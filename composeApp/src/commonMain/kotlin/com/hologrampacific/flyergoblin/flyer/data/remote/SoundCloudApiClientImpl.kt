package com.hologrampacific.flyergoblin.flyer.data.remote

import com.hologrampacific.flyergoblin.BuildKonfig
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.flyergoblin.util.AppLogger
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class SoundCloudApiClientImpl(private val httpClient: HttpClient) : SoundCloudApiClient {

  /** Cached OAuth access token */
  private var cachedAccessToken: String? = null

  /** Timestamp (in milliseconds) when the cached token expires */
  private var tokenExpirationTime: Long = 0

  /** Mutex to ensure thread-safe access to the cached token */
  private val tokenMutex = Mutex()

  /** In-flight token request. Allows concurrent callers to share one network request. */
  private var pendingTokenDeferred: CompletableDeferred<String?>? = null

  private val rateLimitState = RateLimitState()

  private suspend fun checkRateLimit() =
    rateLimitState.check("SoundCloudApiClient", "SoundCloud API")

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  companion object {
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
   * Throws a typed [SoundCloudApiException] subtype for any non-success response. Reads and logs
   * the error body. No-op for success responses.
   */
  private suspend fun HttpResponse.throwIfNotSuccess(operationDescription: String) {
    if (status.isSuccess()) return
    val statusCode = status.value
    val errorBody = bodyAsText()
    AppLogger.w("SoundCloudApiClient", "$operationDescription ($statusCode): $errorBody")
    when {
      statusCode in 500..599 ->
        throw ServerErrorException(statusCode, "SoundCloud server error: $statusCode")

      statusCode in 400..499 ->
        throw ClientErrorException(statusCode, "SoundCloud client error: $statusCode")

      else -> throw SoundCloudApiException("Unexpected status code: $statusCode")
    }
  }

  /**
   * Handles 429 rate limit responses by parsing the error body and throwing
   * [ApiRateLimitException]. Tries the play request format first (detailed info), then the general
   * format.
   */
  private suspend fun handleRateLimitResponse(response: HttpResponse) {
    if (response.status.value != 429) return

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
        val blockedUntil =
          parseResetTime(meta.resetTime) ?: (Clock.System.now() + UNKNOWN_RATE_LIMIT_FALLBACK)
        rateLimitState.setBlockedUntil(blockedUntil)
        throw ApiRateLimitException(
          blockedUntil = blockedUntil,
          message = "SoundCloud API rate limit exceeded",
        )
      }
    } catch (e: ApiRateLimitException) {
      throw e
    } catch (e: Exception) {
      AppLogger.d("SoundCloudApiClient", "Failed to parse as play request rate limit: ${e.message}")
    }

    // Try parsing general rate limit format (token/search endpoints)
    val blockedUntil = Clock.System.now() + UNKNOWN_RATE_LIMIT_FALLBACK
    rateLimitState.setBlockedUntil(blockedUntil)
    try {
      val generalError = json.decodeFromString<SoundCloudGeneralRateLimitError>(errorBody)
      AppLogger.w(
        "SoundCloudApiClient",
        "Rate limit exceeded (429) - General request. " +
          "Message: ${generalError.message}, " +
          "Status: ${generalError.status}. " +
          "No reset time available.",
      )
      throw ApiRateLimitException(
        blockedUntil = blockedUntil,
        message = "SoundCloud API rate limit exceeded: ${generalError.message}",
      )
    } catch (e: ApiRateLimitException) {
      throw e
    } catch (e: Exception) {
      AppLogger.w(
        "SoundCloudApiClient",
        "Rate limit exceeded (429), but could not parse error response: ${e.message}. " +
          "Raw body: $errorBody",
      )
      throw ApiRateLimitException(
        blockedUntil = blockedUntil,
        message = "SoundCloud API rate limit exceeded",
      )
    }
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
        val response =
          withHttpRetry(logTag = "SoundCloudApiClient") {
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
        // Unblock any coroutines waiting on the deferred, then re-throw so cancellation propagates.
        // Without the re-throw, swallowing CancellationException breaks cooperative cancellation.
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
    return try {
      AppLogger.d("SoundCloudApiClient", "Fetching tracks for: $soundCloudUserId")

      val apiUrl = "https://api.soundcloud.com/users/$soundCloudUserId/tracks"
      val response = withAuthRetry { accessToken ->
        withHttpRetry(
          logTag = "SoundCloudApiClient",
          onBeforeAttempt = ::checkRateLimit,
          onNonRetryableResponse = ::handleRateLimitResponse,
        ) {
          httpClient.get(apiUrl) {
            header("Authorization", "Bearer $accessToken")
            parameter("limit", max(MAX_TRACKS_TO_SHOW * 2, 10))
          }
        }
      }

      response.throwIfNotSuccess("Failed to fetch tracks from API")

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
    } catch (e: ApiRateLimitException) {
      throw e
    } catch (e: SoundCloudApiException) {
      AppLogger.e("SoundCloudApiClient", "API error fetching tracks: ${e.message}", e)
      emptyList()
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
   * @return List of matching users
   * @throws ApiRateLimitException if rate limit is exceeded
   * @throws ServerErrorException if server returns 5xx error
   * @throws ClientErrorException if client error occurs (4xx)
   * @throws SoundCloudApiException for other API errors
   */
  override suspend fun searchUsers(query: String): List<SoundCloudUser> {
    try {
      AppLogger.d("SoundCloudApiClient", "Searching users for: $query")

      val response = withAuthRetry { accessToken ->
        withHttpRetry(
          logTag = "SoundCloudApiClient",
          onBeforeAttempt = ::checkRateLimit,
          onNonRetryableResponse = ::handleRateLimitResponse,
        ) {
          httpClient.get("https://api.soundcloud.com/users") {
            header("Authorization", "Bearer $accessToken")
            parameter("q", query)
            parameter("limit", 10)
          }
        }
      }

      response.throwIfNotSuccess("Failed to search users")

      val responseBody = response.bodyAsText()
      AppLogger.d("SoundCloudApiClient", "User search response: $responseBody")

      val result = json.decodeFromString<List<SoundCloudUser>>(responseBody)
      return result
    } catch (e: ApiRateLimitException) {
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
