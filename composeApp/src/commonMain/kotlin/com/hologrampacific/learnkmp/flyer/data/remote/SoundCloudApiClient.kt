package com.hologrampacific.learnkmp.flyer.data.remote

import com.hologrampacific.learnkmp.BuildKonfig
import com.hologrampacific.learnkmp.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.learnkmp.util.AppLogger
import io.ktor.client.*
import io.ktor.client.network.sockets.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.math.pow
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

open class SoundCloudApiClient(private val httpClient: HttpClient) {

  /** Cached OAuth access token */
  private var cachedAccessToken: String? = null

  /** Timestamp (in milliseconds) when the cached token expires */
  private var tokenExpirationTime: Long = 0

  /** Mutex to ensure thread-safe access to the cached token */
  private val tokenMutex = Mutex()
  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  companion object {
    private const val MAX_RETRIES = 3
    private const val INITIAL_BACKOFF_MS = 1000L
  }

  /**
   * Executes an HTTP request with exponential backoff retry logic. Retries on rate limiting (429),
   * server errors (5xx), and transient network failures.
   *
   * @param maxRetries Maximum number of retry attempts (default: 3)
   * @param block The suspend function that performs the HTTP request
   * @return The HTTP response
   * @throws Exception if all retries are exhausted or non-retryable error occurs
   */
  private suspend fun withRetry(
    maxRetries: Int = MAX_RETRIES,
    block: suspend (attempt: Int) -> HttpResponse,
  ): HttpResponse {
    var lastException: Exception? = null
    var lastResponse: HttpResponse? = null

    repeat(maxRetries + 1) { attempt ->
      try {
        val response = block(attempt)

        // Check if response should trigger a retry
        if (shouldRetryResponse(response)) {
          lastResponse = response
          if (attempt < maxRetries) {
            val backoffMs = INITIAL_BACKOFF_MS * 2.0.pow(attempt.toDouble()).toLong()
            AppLogger.w(
              "SoundCloudApiClient",
              "Request returned ${response.status.value} (attempt ${attempt + 1}/${maxRetries + 1}), retrying in ${backoffMs}ms",
            )
            delay(backoffMs)
            // Continue to next retry attempt
          } else {
            // Max retries exhausted, return the last response
            return response
          }
        } else {
          // Success or non-retryable error
          return response
        }
      } catch (e: Exception) {
        lastException = e

        // Determine if we should retry based on exception type
        val shouldRetry =
          when {
            attempt >= maxRetries -> false
            e is ConnectTimeoutException -> true
            e is SocketTimeoutException -> true
            else -> false
          }

        if (shouldRetry) {
          val backoffMs = INITIAL_BACKOFF_MS * 2.0.pow(attempt.toDouble()).toLong()
          AppLogger.w(
            "SoundCloudApiClient",
            "Request failed (attempt ${attempt + 1}/${maxRetries + 1}), retrying in ${backoffMs}ms: ${e.message}",
          )
          delay(backoffMs)
        } else {
          throw e
        }
      }
    }

    // If we got here, we exhausted retries
    lastException?.let { throw it }
    lastResponse?.let {
      return it
    }
    throw Exception("Unknown error in retry logic")
  }

  /**
   * Checks if an HTTP response should trigger a retry. Retries on 429 (Too Many Requests) and 5xx
   * server errors.
   *
   * @param response The HTTP response to check
   * @return true if the request should be retried
   */
  private fun shouldRetryResponse(response: HttpResponse): Boolean {
    return response.status.value == 429 || response.status.value in 500..599
  }

  /** Clears the cached access token. Thread-safe using mutex. */
  private suspend fun clearCachedToken() =
    tokenMutex.withLock {
      cachedAccessToken = null
      tokenExpirationTime = 0
    }

  /**
   * Executes an authenticated API request with automatic token refresh on 401 errors. If the
   * request fails with 401, clears the cached token and retries once with a fresh token.
   *
   * @param block The suspend function that performs the authenticated HTTP request
   * @return The HTTP response
   */
  private suspend fun withAuthRetry(
    block: suspend (accessToken: String) -> HttpResponse
  ): HttpResponse {
    var accessToken = getAccessToken()
    if (accessToken == null) {
      throw Exception("Could not obtain access token")
    }

    val response = block(accessToken)

    // If we get a 401, clear the cached token and retry once
    if (response.status.value == 401) {
      AppLogger.w("SoundCloudApiClient", "Received 401, clearing token cache and retrying")
      clearCachedToken()

      accessToken = getAccessToken()
      if (accessToken == null) {
        throw Exception("Could not obtain access token on retry")
      }

      return block(accessToken)
    }

    return response
  }

  /**
   * Gets a valid OAuth access token for SoundCloud API. Uses cached token if available and not
   * expired, otherwise requests a new one. Thread-safe using mutex to prevent concurrent token
   * requests.
   *
   * @return Access token, or null if authentication fails
   */
  private suspend fun getAccessToken(): String? =
    tokenMutex.withLock {
      // Check if we have a cached token that hasn't expired
      cachedAccessToken?.let { token ->
        val currentTime = System.currentTimeMillis()
        if (currentTime < tokenExpirationTime) {
          AppLogger.d("SoundCloudApiClient", "Using cached access token")
          return@withLock token
        } else {
          AppLogger.d("SoundCloudApiClient", "Cached token expired, requesting new token")
          cachedAccessToken = null
          tokenExpirationTime = 0
        }
      }

      return@withLock try {
        AppLogger.d("SoundCloudApiClient", "Requesting new OAuth access token")

        val response = withRetry { _ ->
          httpClient.post("https://api.soundcloud.com/oauth2/token") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
              "grant_type=client_credentials&client_id=${BuildKonfig.SOUNDCLOUD_CLIENT_ID}&client_secret=${BuildKonfig.SOUNDCLOUD_CLIENT_SECRET}"
            )
          }
        }

        val responseBody = response.bodyAsText()
        if (!response.status.isSuccess()) {
          AppLogger.e(
            "SoundCloudApiClient",
            "Failed to get access token (${response.status.value}): $responseBody",
          )
          return@withLock null
        }

        val tokenResponse = json.decodeFromString<SoundCloudTokenResponse>(responseBody)
        cachedAccessToken = tokenResponse.accessToken

        // Calculate expiration time with a 60-second buffer to avoid edge cases
        val expiresInMs =
          (tokenResponse.expiresIn ?: 3600) * 1000L // Default to 1 hour if not provided
        tokenExpirationTime = System.currentTimeMillis() + expiresInMs - 60000L

        AppLogger.d(
          "SoundCloudApiClient",
          "Successfully obtained access token (expires in ${tokenResponse.expiresIn ?: 3600}s)",
        )
        tokenResponse.accessToken
      } catch (e: Exception) {
        AppLogger.e("SoundCloudApiClient", "Error getting access token: ${e.message}", e)
        null
      }
    }

  /**
   * Fetches popular tracks from a SoundCloud artist profile using the official SoundCloud API.
   *
   * @param profileUrl The artist's SoundCloud profile URL
   * @return List of popular tracks
   */
  open suspend fun fetchPopularTracks(profileUrl: String): List<SoundCloudTrack> {
    return try {
      AppLogger.d("SoundCloudApiClient", "Fetching tracks for: $profileUrl")

      // Step 1: Resolve the profile URL to get the user ID using official API
      val userId = resolveUserIdFromUrl(profileUrl)
      if (userId == null) {
        AppLogger.w("SoundCloudApiClient", "Could not resolve user ID from profile URL")
        return emptyList()
      }

      AppLogger.d("SoundCloudApiClient", "Resolved user ID: $userId")

      // Step 2: Fetch the user's tracks from the official API
      val apiUrl = "https://api.soundcloud.com/users/$userId/tracks"
      val response = withAuthRetry { accessToken ->
        withRetry { _ ->
          httpClient.get(apiUrl) {
            header("Authorization", "Bearer $accessToken")
            parameter("limit", 10)
          }
        }
      }

      if (!response.status.isSuccess()) {
        val errorBody = response.bodyAsText()
        AppLogger.w(
          "SoundCloudApiClient",
          "Failed to fetch tracks from API (${response.status.value}): $errorBody",
        )
        return emptyList()
      }

      val tracksResponseJson = response.bodyAsText()
      AppLogger.d("SoundCloudApiClient", "Tracks API response: $tracksResponseJson")

      val tracks = json.decodeFromString<List<SoundCloudTrackResponse>>(tracksResponseJson)

      // Sort by playback count to get popular tracks first, limit to 10
      val popularTracks =
        tracks
          .sortedByDescending { it.playbackCount ?: 0 }
          .take(10)
          .map { track -> SoundCloudTrack(title = track.title, url = track.permalinkUrl) }

      AppLogger.d("SoundCloudApiClient", "Fetched ${popularTracks.size} tracks from API")
      popularTracks
    } catch (e: SerializationException) {
      AppLogger.e("SoundCloudApiClient", "Error parsing SoundCloud API response: ${e.message}", e)
      emptyList()
    } catch (e: Exception) {
      AppLogger.e("SoundCloudApiClient", "Error fetching popular tracks: ${e.message}", e)
      emptyList()
    }
  }

  /**
   * Searches for users on SoundCloud by name.
   *
   * @param query The search query (artist/user name)
   * @return List of matching users, or empty list if search fails
   */
  open suspend fun searchUsers(query: String): List<SoundCloudUser> {
    return try {
      AppLogger.d("SoundCloudApiClient", "Searching users for: $query")

      val response = withAuthRetry { accessToken ->
        withRetry { _ ->
          httpClient.get("https://api.soundcloud.com/users") {
            header("Authorization", "Bearer $accessToken")
            parameter("q", query)
            parameter("limit", 10)
          }
        }
      }

      if (!response.status.isSuccess()) {
        val errorBody = response.bodyAsText()
        AppLogger.w(
          "SoundCloudApiClient",
          "Failed to search users (${response.status.value}): $errorBody",
        )
        return emptyList()
      }

      val responseBody = response.bodyAsText()
      AppLogger.d("SoundCloudApiClient", "User search response: $responseBody")

      json.decodeFromString<List<SoundCloudUser>>(responseBody)
    } catch (e: SerializationException) {
      AppLogger.e("SoundCloudApiClient", "Error parsing user search response: ${e.message}", e)
      emptyList()
    } catch (e: Exception) {
      AppLogger.e("SoundCloudApiClient", "Error searching users: ${e.message}", e)
      emptyList()
    }
  }

  /**
   * Resolves a SoundCloud profile URL to get the user ID using the official resolve API.
   *
   * @param profileUrl The artist's SoundCloud profile URL
   * @return The user ID, or null if resolution fails
   */
  private suspend fun resolveUserIdFromUrl(profileUrl: String): Long? {
    return try {
      val resolveUrl = "https://api.soundcloud.com/resolve"
      AppLogger.d("SoundCloudApiClient", "Resolving URL: $profileUrl")

      val response = withAuthRetry { accessToken ->
        withRetry { _ ->
          httpClient.get(resolveUrl) {
            header("Authorization", "Bearer $accessToken")
            parameter("url", profileUrl)
          }
        }
      }

      if (!response.status.isSuccess()) {
        val errorBody = response.bodyAsText()
        AppLogger.w(
          "SoundCloudApiClient",
          "Failed to resolve user ID (${response.status.value}): $errorBody",
        )
        return null
      }

      val responseBody = response.bodyAsText()
      AppLogger.d("SoundCloudApiClient", "Resolve API response: $responseBody")

      val resolveResponse = json.decodeFromString<SoundCloudResolveResponse>(responseBody)
      resolveResponse.id
    } catch (e: Exception) {
      AppLogger.e("SoundCloudApiClient", "Error resolving user ID: ${e.message}", e)
      null
    }
  }
}
