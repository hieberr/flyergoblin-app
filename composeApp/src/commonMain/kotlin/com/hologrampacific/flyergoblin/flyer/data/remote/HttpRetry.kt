package com.hologrampacific.flyergoblin.flyer.data.remote

import com.hologrampacific.flyergoblin.util.AppLogger
import io.ktor.client.network.sockets.*
import io.ktor.client.statement.*
import kotlin.math.pow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Executes an HTTP request with exponential backoff retry logic. Retries on server errors (5xx) and
 * transient network failures (connect/socket timeouts).
 *
 * @param maxRetries Maximum number of retry attempts (default: 3)
 * @param initialBackoffMs Base delay in milliseconds for exponential backoff (default: 1000)
 * @param maxBackoffMs Maximum delay in milliseconds between retries (default: 30000)
 * @param logTag Tag used for log messages
 * @param onBeforeAttempt Called before each attempt (e.g. to check a rate limit window)
 * @param onNonRetryableResponse Called for non-5xx responses before returning (e.g. to detect rate
 *   limiting). If it throws, the exception propagates normally through the catch block.
 * @param block The suspend function that performs the HTTP request
 * @return The HTTP response
 * @throws Exception if all retries are exhausted or a non-retryable error occurs
 */
internal suspend fun withHttpRetry(
  maxRetries: Int = 3,
  initialBackoffMs: Long = 1000L,
  maxBackoffMs: Long = 30_000L,
  logTag: String,
  onBeforeAttempt: suspend () -> Unit = {},
  onNonRetryableResponse: suspend (HttpResponse) -> Unit = {},
  block: suspend () -> HttpResponse,
): HttpResponse {
  require(maxRetries >= 0) { "maxRetries must be non-negative, got $maxRetries" }
  var lastException: Exception? = null
  var lastResponse: HttpResponse? = null

  repeat(maxRetries + 1) { attempt ->
    try {
      onBeforeAttempt()

      val response = block()

      if (response.status.value in 500..599) {
        lastResponse = response
        if (attempt < maxRetries) {
          val backoffMs =
            minOf(initialBackoffMs * 2.0.pow(attempt.toDouble()).toLong(), maxBackoffMs)
          AppLogger.w(
            logTag,
            "Request returned ${response.status.value} (attempt ${attempt + 1}/${maxRetries + 1}), retrying in ${backoffMs}ms",
          )
          delay(backoffMs)
        } else {
          return response
        }
      } else {
        onNonRetryableResponse(response)
        return response
      }
    } catch (e: CancellationException) {
      throw e
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
        val backoffMs = minOf(initialBackoffMs * 2.0.pow(attempt.toDouble()).toLong(), maxBackoffMs)
        AppLogger.w(
          logTag,
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
