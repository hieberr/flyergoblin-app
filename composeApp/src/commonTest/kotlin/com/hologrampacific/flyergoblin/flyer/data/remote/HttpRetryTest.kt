package com.hologrampacific.flyergoblin.flyer.data.remote

import com.hologrampacific.flyergoblin.AppTest
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class HttpRetryTest : AppTest() {

  @Test
  fun `test withHttpRetry returns response on success`() = runTest {
    val client = createMockClient { respond("OK", HttpStatusCode.OK) }

    val response = withHttpRetry(logTag = "Test") { client.get("https://example.com") }

    assertEquals(200, response.status.value)
  }

  @Test
  fun `test withHttpRetry retries on 5xx and eventually succeeds`() = runTest {
    var attemptCount = 0
    val client = createMockClient {
      attemptCount++
      if (attemptCount < 3) {
        respond("Server Error", HttpStatusCode.InternalServerError)
      } else {
        respond("OK", HttpStatusCode.OK)
      }
    }

    val response =
      withHttpRetry(initialBackoffMs = 1L, logTag = "Test") { client.get("https://example.com") }

    assertEquals(200, response.status.value)
    assertEquals(3, attemptCount)
  }

  @Test
  fun `test withHttpRetry returns last 5xx response when retries exhausted`() = runTest {
    var attemptCount = 0
    val client = createMockClient {
      attemptCount++
      respond("Server Error", HttpStatusCode.InternalServerError)
    }

    val response =
      withHttpRetry(maxRetries = 2, initialBackoffMs = 1L, logTag = "Test") {
        client.get("https://example.com")
      }

    assertEquals(500, response.status.value)
    assertEquals(3, attemptCount) // initial + 2 retries
  }

  @Test
  fun `test withHttpRetry retries on ConnectTimeoutException`() = runTest {
    var attemptCount = 0
    val client = createMockClient { respond("OK", HttpStatusCode.OK) }

    val response =
      withHttpRetry(initialBackoffMs = 1L, logTag = "Test") {
        attemptCount++
        if (attemptCount < 2) {
          throw io.ktor.client.network.sockets.ConnectTimeoutException("Connect timed out")
        }
        client.get("https://example.com")
      }

    assertEquals(200, response.status.value)
    assertEquals(2, attemptCount)
  }

  @Test
  fun `test withHttpRetry retries on SocketTimeoutException`() = runTest {
    var attemptCount = 0
    val client = createMockClient { respond("OK", HttpStatusCode.OK) }

    val response =
      withHttpRetry(initialBackoffMs = 1L, logTag = "Test") {
        attemptCount++
        if (attemptCount < 2) {
          throw io.ktor.client.network.sockets.SocketTimeoutException("Socket timed out")
        }
        client.get("https://example.com")
      }

    assertEquals(200, response.status.value)
    assertEquals(2, attemptCount)
  }

  @Test
  fun `test withHttpRetry does not retry non-timeout exceptions`() = runTest {
    var attemptCount = 0

    assertFailsWith<IllegalStateException> {
      withHttpRetry(initialBackoffMs = 1L, logTag = "Test") {
        attemptCount++
        throw IllegalStateException("not retryable")
      }
    }

    assertEquals(1, attemptCount)
  }

  @Test
  fun `test withHttpRetry calls onBeforeAttempt before each attempt`() = runTest {
    var beforeAttemptCount = 0
    var attemptCount = 0
    val client = createMockClient {
      attemptCount++
      if (attemptCount < 2) {
        respond("Server Error", HttpStatusCode.InternalServerError)
      } else {
        respond("OK", HttpStatusCode.OK)
      }
    }

    withHttpRetry(
      initialBackoffMs = 1L,
      logTag = "Test",
      onBeforeAttempt = { beforeAttemptCount++ },
    ) {
      client.get("https://example.com")
    }

    assertEquals(2, beforeAttemptCount)
    assertEquals(2, attemptCount)
  }

  @Test
  fun `test withHttpRetry onBeforeAttempt exception propagates without retry`() = runTest {
    var attemptCount = 0
    val client = createMockClient { respond("OK", HttpStatusCode.OK) }

    assertFailsWith<IllegalStateException> {
      withHttpRetry(
        initialBackoffMs = 1L,
        logTag = "Test",
        onBeforeAttempt = { throw IllegalStateException("rate limited") },
      ) {
        attemptCount++
        client.get("https://example.com")
      }
    }

    assertEquals(0, attemptCount, "Block should not execute if onBeforeAttempt throws")
  }

  @Test
  fun `test withHttpRetry calls onNonRetryableResponse for non-5xx responses`() = runTest {
    var callbackResponse: HttpResponse? = null
    val client = createMockClient { respond("OK", HttpStatusCode.OK) }

    withHttpRetry(
      logTag = "Test",
      onNonRetryableResponse = { response -> callbackResponse = response },
    ) {
      client.get("https://example.com")
    }

    assertEquals(200, callbackResponse?.status?.value)
  }

  @Test
  fun `test withHttpRetry onNonRetryableResponse exception propagates without retry`() = runTest {
    var attemptCount = 0
    val client = createMockClient {
      attemptCount++
      respond("Rate Limited", HttpStatusCode.TooManyRequests)
    }

    assertFailsWith<IllegalStateException> {
      withHttpRetry(
        initialBackoffMs = 1L,
        logTag = "Test",
        onNonRetryableResponse = { throw IllegalStateException("rate limited") },
      ) {
        client.get("https://example.com")
      }
    }

    assertEquals(1, attemptCount, "Should not retry when onNonRetryableResponse throws")
  }

  @Test
  fun `test withHttpRetry does not call onNonRetryableResponse for 5xx`() = runTest {
    var callbackCalled = false
    val client = createMockClient { respond("Server Error", HttpStatusCode.InternalServerError) }

    withHttpRetry(
      maxRetries = 0,
      logTag = "Test",
      onNonRetryableResponse = { callbackCalled = true },
    ) {
      client.get("https://example.com")
    }

    assertTrue(!callbackCalled, "onNonRetryableResponse should not be called for 5xx responses")
  }

  private fun createMockClient(
    handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
  ): HttpClient {
    return HttpClient(MockEngine { request -> handler(request) })
  }
}
