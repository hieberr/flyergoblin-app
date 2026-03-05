package com.hologrampacific.flyergoblin.flyer.data.remote

import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudShow
import com.hologrampacific.flyergoblin.util.AppLogger
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class MixcloudApiClientImpl(private val httpClient: HttpClient) : MixcloudApiClient {

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  /** Time until which all API requests are blocked due to rate limiting. */
  private var rateLimitBlockedUntil: Instant? = null

  /** Mutex for thread-safe access to rate limit state. */
  private val rateLimitMutex = Mutex()

  companion object {
    private const val BASE_URL = "https://api.mixcloud.com"
    private const val MAX_SHOWS_TO_FETCH = 10
  }

  /**
   * Checks the rate limit window before an attempt. Throws [ApiRateLimitException] if the window is
   * still active, otherwise clears it. Thread-safe.
   */
  private suspend fun checkRateLimit() {
    rateLimitMutex.withLock {
      rateLimitBlockedUntil?.let { blockedUntil ->
        if (Clock.System.now() < blockedUntil) {
          AppLogger.w("MixcloudApiClient", "Rate limited until: $blockedUntil")
          throw ApiRateLimitException(
            blockedUntil = blockedUntil,
            message = "Mixcloud API rate limit exceeded",
          )
        } else {
          rateLimitBlockedUntil = null
        }
      }
    }
  }

  /**
   * Throws a typed [MixcloudApiException] subtype for any non-success response. Reads and logs the
   * error body. No-op for success responses.
   */
  private suspend fun HttpResponse.throwIfNotSuccess(operationDescription: String) {
    if (status.isSuccess()) return
    val statusCode = status.value
    val errorBody = bodyAsText()
    AppLogger.w("MixcloudApiClient", "$operationDescription ($statusCode): $errorBody")
    when {
      statusCode in 500..599 ->
        throw MixcloudServerErrorException(statusCode, "Mixcloud server error: $statusCode")

      statusCode in 400..499 ->
        throw MixcloudClientErrorException(statusCode, "Mixcloud client error: $statusCode")

      else -> throw MixcloudApiException("Unexpected status code: $statusCode")
    }
  }

  /**
   * Handles 403 responses that may indicate rate limiting. Parses the error body and throws
   * [ApiRateLimitException] if it is a rate limit error.
   */
  private suspend fun handleRateLimitResponse(response: HttpResponse) {
    if (response.status.value != 403) return

    val errorBody = response.bodyAsText()
    try {
      val rateLimitError = json.decodeFromString<MixcloudRateLimitError>(errorBody)
      if (rateLimitError.error.type == "RateLimitException") {
        val retryAfter = rateLimitError.error.retryAfter
        val blockedUntil = Clock.System.now() + retryAfter.seconds
        rateLimitMutex.withLock { rateLimitBlockedUntil = blockedUntil }
        AppLogger.w("MixcloudApiClient", "Rate limit exceeded (403). Blocked for ${retryAfter}s.")
        throw ApiRateLimitException(
          blockedUntil = blockedUntil,
          message = rateLimitError.error.message,
        )
      }
    } catch (e: ApiRateLimitException) {
      throw e
    } catch (e: Exception) {
      AppLogger.w(
        "MixcloudApiClient",
        "403 response with unexpected body (not a rate limit). Body: $errorBody. Error: ${e.message}",
      )
    }
  }

  override suspend fun searchUsers(query: String): List<MixcloudUserResult> {
    try {
      AppLogger.d("MixcloudApiClient", "Searching users for: $query")

      val response =
        withHttpRetry(
          logTag = "MixcloudApiClient",
          onBeforeAttempt = ::checkRateLimit,
          onNonRetryableResponse = ::handleRateLimitResponse,
        ) {
          httpClient.get("$BASE_URL/search/") {
            parameter("q", query)
            parameter("type", "user")
          }
        }

      response.throwIfNotSuccess("Failed to search users")

      val responseBody = response.bodyAsText()
      AppLogger.d("MixcloudApiClient", "User search response: $responseBody")

      val searchResponse = json.decodeFromString<MixcloudSearchResponse>(responseBody)
      return searchResponse.data
    } catch (e: MixcloudApiException) {
      throw e
    } catch (e: SerializationException) {
      AppLogger.e("MixcloudApiClient", "Error parsing user search response: ${e.message}", e)
      throw MixcloudApiException("Failed to parse search response", e)
    } catch (e: Exception) {
      AppLogger.e("MixcloudApiClient", "Network error searching users: ${e.message}", e)
      throw MixcloudApiException("Network error", e)
    }
  }

  override suspend fun getUserProfile(userKey: String): MixcloudUserProfile {
    try {
      AppLogger.d("MixcloudApiClient", "Fetching user profile for: $userKey")

      val response =
        withHttpRetry(
          logTag = "MixcloudApiClient",
          onBeforeAttempt = ::checkRateLimit,
          onNonRetryableResponse = ::handleRateLimitResponse,
        ) {
          httpClient.get("$BASE_URL$userKey")
        }

      response.throwIfNotSuccess("Failed to fetch profile")

      val responseBody = response.bodyAsText()
      AppLogger.d("MixcloudApiClient", "User profile response: $responseBody")

      return json.decodeFromString<MixcloudUserProfile>(responseBody)
    } catch (e: MixcloudApiException) {
      throw e
    } catch (e: SerializationException) {
      AppLogger.e("MixcloudApiClient", "Error parsing profile response: ${e.message}", e)
      throw MixcloudApiException("Failed to parse profile response", e)
    } catch (e: Exception) {
      AppLogger.e("MixcloudApiClient", "Network error fetching profile: ${e.message}", e)
      throw MixcloudApiException("Network error", e)
    }
  }

  override suspend fun getCloudcasts(userKey: String): List<MixcloudShow> {
    return try {
      AppLogger.d("MixcloudApiClient", "Fetching cloudcasts for: $userKey")

      val response =
        withHttpRetry(
          logTag = "MixcloudApiClient",
          onBeforeAttempt = ::checkRateLimit,
          onNonRetryableResponse = ::handleRateLimitResponse,
        ) {
          httpClient.get("$BASE_URL${userKey}cloudcasts/") {
            parameter("limit", MAX_SHOWS_TO_FETCH)
          }
        }

      response.throwIfNotSuccess("Failed to fetch cloudcasts")

      val responseBody = response.bodyAsText()
      AppLogger.d("MixcloudApiClient", "Cloudcasts response: $responseBody")

      val cloudcastsResponse = json.decodeFromString<MixcloudCloudcastsResponse>(responseBody)
      val shows =
        cloudcastsResponse.data.map { cloudcast ->
          MixcloudShow(key = cloudcast.key, name = cloudcast.name, url = cloudcast.url)
        }

      AppLogger.d("MixcloudApiClient", "Fetched ${shows.size} cloudcasts")
      shows
    } catch (e: ApiRateLimitException) {
      throw e
    } catch (e: MixcloudApiException) {
      AppLogger.e("MixcloudApiClient", "API error fetching cloudcasts: ${e.message}", e)
      emptyList()
    } catch (e: SerializationException) {
      AppLogger.e("MixcloudApiClient", "Error parsing cloudcasts response: ${e.message}", e)
      emptyList()
    } catch (e: Exception) {
      AppLogger.e("MixcloudApiClient", "Error fetching cloudcasts: ${e.message}", e)
      emptyList()
    }
  }
}
