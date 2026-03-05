package com.hologrampacific.flyergoblin.flyer.data.remote

import com.hologrampacific.flyergoblin.BuildKonfig
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.flyergoblin.util.AppLogger
import io.ktor.client.*
import io.ktor.client.network.sockets.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.math.max
import kotlin.math.pow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class SoundCloudApiClientImpl(private val httpClient: HttpClient) : SoundCloudApiClient {

  /** Cached OAuth access token */
  private var cachedAccessToken: String? = null

  /** Timestamp (in milliseconds) when the cached token expires */
  private var tokenExpirationTime: Long = 0

  /** Mutex to ensure thread-safe access to the cached token */
  private val tokenMutex = Mutex()

  /** In-flight token request. Allows concurrent callers to share one network request. */
  private var pendingTokenDeferred: CompletableDeferred<String?>? = null

  /** Cached rate limit state. Non-null while a rate limit window is active. */
  private var rateLimitState: RateLimitState? = null

  /** Mutex for thread-safe access to rate limit state */
  private val rateLimitMutex = Mutex()

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  private data class RateLimitState(
    val blockedUntil: Instant,
    val resetTime: String,
    val maxRequests: Int,
    val timeWindow: String,
  )

  companion object {
    private const val MAX_RETRIES = 3
    private const val INITIAL_BACKOFF_MS = 1000L

    /** Of the tracks fetched load and this many */
    private const val MAX_TRACKS_TO_SHOW = 5

    /** Fallback block duration when the reset time string cannot be parsed. */
    private val UNKNOWN_RATE_LIMIT_FALLBACK = 60.minutes

    /**
     * Parses a SoundCloud reset time string (format: "yyyy/MM/dd HH:mm:ss +HHMM") into an
     * [Instant]. Returns null if the string cannot be parsed.
     */
    internal fun parseResetTime(resetTimeStr: String): Instant? {
      return try {
        // "2026/02/08 14:30:00 +0000" → "2026-02-08T14:30:00+00:00"
        val parts = resetTimeStr.replace('/', '-').split(" ")
        if (parts.size != 3) return null
        val (date, time, tz) = parts
        // "+0000" → "+00:00"
        val tzFormatted = if (tz.length == 5) "${tz.substring(0, 3)}:${tz.substring(3)}" else tz
        Instant.parse("${date}T${time}${tzFormatted}")
      } catch (e: Exception) {
        null
      }
    }
  }

  /**
   * Executes an HTTP request with exponential backoff retry logic. Retries on server errors (5xx)
   * and transient network failures. Does NOT retry on 429 (rate limiting) to avoid wasting quota.
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
          // Handle 429 rate limit errors by parsing details and throwing exception
          if (response.status.value == 429) {
            val errorBody = response.bodyAsText()

            // Try parsing play request format first (has detailed rate limit info)
            try {
              val playError = json.decodeFromString<SoundCloudPlayRateLimitError>(errorBody)
              val meta = playError.errors.firstOrNull()?.meta
              if (meta != null) {
                AppLogger.w(
                  "SoundCloudApiClient",
                  "Rate limit exceeded (429) - Play requests. " +
                    "Group: ${meta.rateLimit.group}, " +
                    "Max: ${meta.rateLimit.maxRequests}, " +
                    "Remaining: ${meta.remainingRequests}, " +
                    "Window: ${meta.rateLimit.timeWindow}, " +
                    "Resets at: ${meta.resetTime}",
                )
                throw RateLimitException(
                  message = "SoundCloud API rate limit exceeded",
                  resetTime = meta.resetTime,
                  maxRequests = meta.rateLimit.maxRequests,
                  timeWindow = meta.rateLimit.timeWindow,
                )
              }
            } catch (e: RateLimitException) {
              throw e
            } catch (e: Exception) {
              // Not play request format, try general format
              AppLogger.d(
                "SoundCloudApiClient",
                "Failed to parse as play request rate limit: ${e.message}",
              )
            }

            // Try parsing general rate limit format (token/search endpoints)
            try {
              val generalError = json.decodeFromString<SoundCloudGeneralRateLimitError>(errorBody)
              AppLogger.w(
                "SoundCloudApiClient",
                "Rate limit exceeded (429) - General request. " +
                  "Message: ${generalError.message}, " +
                  "Status: ${generalError.status}. " +
                  "No reset time available.",
              )
              throw RateLimitException(
                message = "SoundCloud API rate limit exceeded: ${generalError.message}",
                resetTime = "Unknown - please wait and try again later",
                maxRequests = 0,
                timeWindow = "Unknown",
              )
            } catch (e: RateLimitException) {
              throw e
            } catch (e: Exception) {
              // Couldn't parse either format
              AppLogger.w(
                "SoundCloudApiClient",
                "Rate limit exceeded (429), but could not parse error response: ${e.message}. " +
                  "Raw body: $errorBody",
              )
              throw RateLimitException(
                message = "SoundCloud API rate limit exceeded",
                resetTime = "Unknown - please wait and try again later",
                maxRequests = 0,
                timeWindow = "Unknown",
              )
            }
          }
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
   * Checks if an HTTP response should trigger a retry. Only retries on 5xx server errors. Does NOT
   * retry on 429 (Too Many Requests) as this indicates rate limiting and retrying will just waste
   * API quota.
   *
   * @param response The HTTP response to check
   * @return true if the request should be retried
   */
  private fun shouldRetryResponse(response: HttpResponse): Boolean {
    return response.status.value in 500..599
  }

  /**
   * Throws [RateLimitException] if the rate limit window is still active. If the window has
   * expired, clears the cached state and allows the request to proceed. Thread-safe.
   */
  private suspend fun checkRateLimit() {
    rateLimitMutex.withLock {
      val state = rateLimitState ?: return@withLock
      if (Clock.System.now() >= state.blockedUntil) {
        rateLimitState = null
        return@withLock
      }
      AppLogger.w("SoundCloudApiClient", "Rate limited. Blocking request until: ${state.resetTime}")
      throw RateLimitException(
        message = "SoundCloud API rate limit exceeded",
        resetTime = state.resetTime,
        maxRequests = state.maxRequests,
        timeWindow = state.timeWindow,
      )
    }
  }

  /** Stores rate limit info from an exception. Thread-safe. */
  private suspend fun storeRateLimitState(e: RateLimitException) {
    val blockedUntil =
      parseResetTime(e.resetTime) ?: (Clock.System.now() + UNKNOWN_RATE_LIMIT_FALLBACK)
    rateLimitMutex.withLock {
      rateLimitState =
        RateLimitState(
          blockedUntil = blockedUntil,
          resetTime = e.resetTime,
          maxRequests = e.maxRequests,
          timeWindow = e.timeWindow,
        )
    }
  }

  /** Clears cached rate limit state on a successful request. Thread-safe. */
  private suspend fun clearRateLimitState() {
    rateLimitMutex.withLock { rateLimitState = null }
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
    var accessToken = getAccessToken() ?: throw Exception("Could not obtain access token")

    val response = block(accessToken)

    // If we get a 401, clear the cached token and retry once
    if (response.status.value == 401) {
      AppLogger.w("SoundCloudApiClient", "Received 401, clearing token cache and retrying")
      clearCachedToken()

      accessToken = getAccessToken() ?: throw Exception("Could not obtain access token on retry")
      return block(accessToken)
    }

    return response
  }

  /**
   * Gets a valid OAuth access token for SoundCloud API. Uses cached token if available and not
   * expired, otherwise requests a new one. Thread-safe: the mutex is held only briefly to check the
   * cache or register an in-flight request — the network call itself runs outside the lock so
   * concurrent callers share one request rather than serialising behind it.
   *
   * @return Access token, or null if authentication fails
   */
  private suspend fun getAccessToken(): String? {
    // Phase 1: Acquire lock only to check cache or get/create the in-flight deferred (no I/O).
    val (deferred, isOwner) =
      tokenMutex.withLock {
        cachedAccessToken?.let { token ->
          val currentTime = Clock.System.now().toEpochMilliseconds()
          if (currentTime < tokenExpirationTime) {
            AppLogger.d("SoundCloudApiClient", "Using cached access token")
            return token
          }
          AppLogger.d("SoundCloudApiClient", "Cached token expired, requesting new token")
          cachedAccessToken = null
          tokenExpirationTime = 0
        }
        val existing = pendingTokenDeferred
        if (existing != null) existing to false
        else {
          val new = CompletableDeferred<String?>()
          pendingTokenDeferred = new
          new to true
        }
      }

    // Phase 2: Owner coroutine performs the network call (mutex is not held).
    if (isOwner) {
      try {
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
          deferred.complete(null)
        } else {
          val tokenResponse = json.decodeFromString<SoundCloudTokenResponse>(responseBody)
          val expiresInMs = (tokenResponse.expiresIn ?: 3600) * 1000L
          tokenMutex.withLock {
            cachedAccessToken = tokenResponse.accessToken
            tokenExpirationTime = Clock.System.now().toEpochMilliseconds() + expiresInMs - 60000L
          }
          AppLogger.d(
            "SoundCloudApiClient",
            "Successfully obtained access token (expires in ${tokenResponse.expiresIn ?: 3600}s)",
          )
          deferred.complete(tokenResponse.accessToken)
        }
      } catch (e: CancellationException) {
        deferred.complete(null)
        throw e
      } catch (e: Exception) {
        AppLogger.e("SoundCloudApiClient", "Error getting access token: ${e.message}", e)
        deferred.complete(null)
      } finally {
        tokenMutex.withLock { if (pendingTokenDeferred === deferred) pendingTokenDeferred = null }
      }
    }

    // Phase 3: All callers (owner and waiters) converge here to get the result.
    return deferred.await()
  }

  /**
   * Fetches popular tracks from a SoundCloud artist profile using the official SoundCloud API.
   *
   * @param soundCloudUserId The artist's SoundCloud profile id
   * @return List of popular tracks
   */
  override suspend fun getTracks(soundCloudUserId: Long): List<SoundCloudTrack> {
    checkRateLimit()
    return try {
      AppLogger.d("SoundCloudApiClient", "Fetching tracks for: $soundCloudUserId")

      val apiUrl = "https://api.soundcloud.com/users/$soundCloudUserId/tracks"
      val response = withAuthRetry { accessToken ->
        withRetry { _ ->
          httpClient.get(apiUrl) {
            header("Authorization", "Bearer $accessToken")
            parameter("limit", max(MAX_TRACKS_TO_SHOW * 2, 10))
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

      // HTTP success — the API is reachable, clear any cached rate limit state
      clearRateLimitState()

      val tracksResponseJson = response.bodyAsText()
      AppLogger.d("SoundCloudApiClient", "Tracks API response: $tracksResponseJson")

      val tracks = json.decodeFromString<List<SoundCloudTrackResponse>>(tracksResponseJson)

      val popularTracks =
        tracks
          .sortedByDescending { it.playbackCount ?: 0 }
          .take(MAX_TRACKS_TO_SHOW)
          .map { track ->
            SoundCloudTrack(id = track.id, title = track.title, url = track.permalinkUrl)
          }

      AppLogger.d("SoundCloudApiClient", "Fetched ${popularTracks.size} tracks from API")
      popularTracks
    } catch (e: SerializationException) {
      AppLogger.e("SoundCloudApiClient", "Error parsing SoundCloud API response: ${e.message}", e)
      emptyList()
    } catch (e: RateLimitException) {
      storeRateLimitState(e)
      throw e
    } catch (e: Exception) {
      AppLogger.e("SoundCloudApiClient", "Error fetching popular tracks: ${e.message}", e)
      emptyList()
    }
  }

  /**
   * Searches for users on SoundCloud by name.
   *
   * @param query The search query (artist/user name)
   * @return List of matching users
   * @throws RateLimitException if rate limit is exceeded
   * @throws ServerErrorException if server returns 5xx error
   * @throws ClientErrorException if client error occurs (4xx)
   * @throws SoundCloudApiException for other API errors
   */
  override suspend fun searchUsers(query: String): List<SoundCloudUser> {
    checkRateLimit()
    try {
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
        val statusCode = response.status.value
        AppLogger.w("SoundCloudApiClient", "Failed to search users ($statusCode): $errorBody")

        when {
          statusCode in 500..599 ->
            throw ServerErrorException(statusCode, "SoundCloud server error: $statusCode")
          statusCode in 400..499 ->
            throw ClientErrorException(statusCode, "SoundCloud client error: $statusCode")
          else -> throw SoundCloudApiException("Unexpected status code: $statusCode")
        }
      }

      val responseBody = response.bodyAsText()
      AppLogger.d("SoundCloudApiClient", "User search response: $responseBody")

      val result = json.decodeFromString<List<SoundCloudUser>>(responseBody)
      clearRateLimitState()
      return result
    } catch (e: RateLimitException) {
      storeRateLimitState(e)
      throw e
    } catch (e: SoundCloudApiException) {
      throw e
    } catch (e: SerializationException) {
      AppLogger.e("SoundCloudApiClient", "Error parsing user search response: ${e.message}", e)
      throw SoundCloudApiException("Failed to parse search response", e)
    } catch (e: Exception) {
      AppLogger.e("SoundCloudApiClient", "Network error searching users: ${e.message}", e)
      throw SoundCloudApiException("Network error", e)
    }
  }
}
