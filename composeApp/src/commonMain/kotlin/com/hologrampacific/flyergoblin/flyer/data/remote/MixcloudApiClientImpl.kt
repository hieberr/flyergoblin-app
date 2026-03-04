package com.hologrampacific.flyergoblin.flyer.data.remote

import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudShow
import com.hologrampacific.flyergoblin.util.AppLogger
import io.ktor.client.*
import io.ktor.client.network.sockets.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.math.pow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.delay
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
    private const val MAX_RETRIES = 3
    private const val INITIAL_BACKOFF_MS = 1000L
    private const val MAX_SHOWS_TO_FETCH = 10
  }

  /**
   * Executes an HTTP request with exponential backoff retry logic. Retries on server errors (5xx)
   * and transient network failures. Does NOT retry on rate limit (403 RateLimitException).
   *
   * Throws [MixcloudRateLimitException] immediately if currently rate limited or if the response
   * indicates a rate limit.
   */
  private suspend fun withRetry(
    maxRetries: Int = MAX_RETRIES,
    block: suspend (attempt: Int) -> HttpResponse,
  ): HttpResponse {
    var lastException: Exception? = null
    var lastResponse: HttpResponse? = null

    repeat(maxRetries + 1) { attempt ->
      // Check the rate limit window before every attempt so that a limit set by a concurrent
      // coroutine between the previous check and now is always honoured (eliminates TOCTOU race).
      rateLimitMutex.withLock {
        rateLimitBlockedUntil?.let { blockedUntil ->
          val now = Clock.System.now()
          if (now < blockedUntil) {
            val remainingSeconds = maxOf(1, (blockedUntil - now).inWholeSeconds.toInt())
            AppLogger.w("MixcloudApiClient", "Rate limited. Retry after ${remainingSeconds}s.")
            throw MixcloudRateLimitException(
              remainingSeconds,
              "Rate limited. Retry after ${remainingSeconds}s.",
            )
          } else {
            rateLimitBlockedUntil = null
          }
        }
      }
      try {
        val response = block(attempt)

        if (response.status.value in 500..599) {
          lastResponse = response
          if (attempt < maxRetries) {
            val backoffMs = INITIAL_BACKOFF_MS * 2.0.pow(attempt.toDouble()).toLong()
            AppLogger.w(
              "MixcloudApiClient",
              "Request returned ${response.status.value} (attempt ${attempt + 1}/${maxRetries + 1}), retrying in ${backoffMs}ms",
            )
            delay(backoffMs)
          } else {
            return response
          }
        } else {
          if (response.status.value == 403) {
            val errorBody = response.bodyAsText()
            try {
              val rateLimitError = json.decodeFromString<MixcloudRateLimitError>(errorBody)
              if (rateLimitError.error.type == "RateLimitException") {
                val retryAfter = rateLimitError.error.retryAfter
                rateLimitMutex.withLock {
                  rateLimitBlockedUntil = Clock.System.now() + retryAfter.seconds
                }
                AppLogger.w(
                  "MixcloudApiClient",
                  "Rate limit exceeded (403). Blocked for ${retryAfter}s.",
                )
                throw MixcloudRateLimitException(
                  retryAfterSeconds = retryAfter,
                  message = rateLimitError.error.message,
                )
              }
            } catch (e: MixcloudRateLimitException) {
              throw e
            } catch (e: Exception) {
              AppLogger.d("MixcloudApiClient", "403 but not a rate limit response: ${e.message}")
            }
          }
          return response
        }
      } catch (e: Exception) {
        lastException = e

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
            "MixcloudApiClient",
            "Request failed (attempt ${attempt + 1}/${maxRetries + 1}), retrying in ${backoffMs}ms: ${e.message}",
          )
          delay(backoffMs)
        } else {
          throw e
        }
      }
    }

    lastException?.let { throw it }
    lastResponse?.let {
      return it
    }
    throw Exception("Unknown error in retry logic")
  }

  override suspend fun searchUsers(query: String): List<MixcloudUserResult> {
    try {
      AppLogger.d("MixcloudApiClient", "Searching users for: $query")

      val response = withRetry { _ ->
        httpClient.get("$BASE_URL/search/") {
          parameter("q", query)
          parameter("type", "user")
        }
      }

      if (!response.status.isSuccess()) {
        val errorBody = response.bodyAsText()
        val statusCode = response.status.value
        AppLogger.w("MixcloudApiClient", "Failed to search users ($statusCode): $errorBody")

        when {
          statusCode in 500..599 ->
            throw MixcloudServerErrorException(statusCode, "Mixcloud server error: $statusCode")

          statusCode in 400..499 ->
            throw MixcloudClientErrorException(statusCode, "Mixcloud client error: $statusCode")

          else -> throw MixcloudApiException("Unexpected status code: $statusCode")
        }
      }

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

      val response = withRetry { _ -> httpClient.get("$BASE_URL$userKey") }

      if (!response.status.isSuccess()) {
        val errorBody = response.bodyAsText()
        val statusCode = response.status.value
        AppLogger.w("MixcloudApiClient", "Failed to fetch profile ($statusCode): $errorBody")

        when {
          statusCode in 500..599 ->
            throw MixcloudServerErrorException(statusCode, "Mixcloud server error: $statusCode")

          statusCode in 400..499 ->
            throw MixcloudClientErrorException(statusCode, "Mixcloud client error: $statusCode")

          else -> throw MixcloudApiException("Unexpected status code: $statusCode")
        }
      }

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

      val response = withRetry { _ ->
        httpClient.get("$BASE_URL${userKey}cloudcasts/") { parameter("limit", MAX_SHOWS_TO_FETCH) }
      }

      if (!response.status.isSuccess()) {
        val errorBody = response.bodyAsText()
        AppLogger.w(
          "MixcloudApiClient",
          "Failed to fetch cloudcasts (${response.status.value}): $errorBody",
        )
        return emptyList()
      }

      val responseBody = response.bodyAsText()
      AppLogger.d("MixcloudApiClient", "Cloudcasts response: $responseBody")

      val cloudcastsResponse = json.decodeFromString<MixcloudCloudcastsResponse>(responseBody)
      val shows =
        cloudcastsResponse.data.map { cloudcast ->
          MixcloudShow(key = cloudcast.key, name = cloudcast.name, url = cloudcast.url)
        }

      AppLogger.d("MixcloudApiClient", "Fetched ${shows.size} cloudcasts")
      shows
    } catch (e: MixcloudRateLimitException) {
      throw e
    } catch (e: SerializationException) {
      AppLogger.e("MixcloudApiClient", "Error parsing cloudcasts response: ${e.message}", e)
      emptyList()
    } catch (e: Exception) {
      AppLogger.e("MixcloudApiClient", "Error fetching cloudcasts: ${e.message}", e)
      emptyList()
    }
  }
}
