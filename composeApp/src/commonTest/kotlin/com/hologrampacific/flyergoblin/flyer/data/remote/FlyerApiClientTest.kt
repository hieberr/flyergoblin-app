package com.hologrampacific.flyergoblin.flyer.data.remote

import com.hologrampacific.flyergoblin.AppTest
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class FlyerApiClientTest : AppTest() {

  @Test
  fun `test processFlyer returns parsed response on success`() = runTest {
    val mockEngine = MockEngine {
      respond(
        content =
          ByteReadChannel(
            """{"name":"Summer Beats","startDate":"2026-07-04","artists":["DJ Alpha"]}"""
          ),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }

    val apiClient = FlyerApiClientImpl(createTestHttpClient(mockEngine))
    val response = apiClient.processFlyer("base64", "image/jpeg")

    assertEquals("Summer Beats", response.name)
    assertEquals("2026-07-04", response.startDate)
    assertEquals(listOf("DJ Alpha"), response.artists)
  }

  @Test
  fun `test processFlyer throws FlyerApiException with TIMEOUT code on 504`() = runTest {
    val mockEngine = MockEngine {
      respond(
        content =
          ByteReadChannel(
            """{"code":"TIMEOUT","message":"The flyer took too long to process. Try a smaller or simpler image, or try again."}"""
          ),
        status = HttpStatusCode.GatewayTimeout,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }

    val apiClient = FlyerApiClientImpl(createTestHttpClient(mockEngine))
    val exception =
      assertFailsWith<FlyerApiException> { apiClient.processFlyer("base64", "image/jpeg") }

    assertEquals(FlyerApiErrorCode.TIMEOUT, exception.errorCode)
    assertEquals(
      "The flyer took too long to process. Try a smaller or simpler image, or try again.",
      exception.message,
    )
    assertEquals(504, exception.statusCode)
  }

  @Test
  fun `test processFlyer throws FlyerApiException with UPSTREAM_RATE_LIMITED code on 429`() =
    runTest {
      val mockEngine = MockEngine {
        respond(
          content =
            ByteReadChannel("""{"code":"UPSTREAM_RATE_LIMITED","message":"Too many requests."}"""),
          status = HttpStatusCode.TooManyRequests,
          headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
      }

      val apiClient = FlyerApiClientImpl(createTestHttpClient(mockEngine))
      val exception =
        assertFailsWith<FlyerApiException> { apiClient.processFlyer("base64", "image/jpeg") }

      assertEquals(FlyerApiErrorCode.UPSTREAM_RATE_LIMITED, exception.errorCode)
    }

  @Test
  fun `test processFlyer maps unrecognized code to UNKNOWN`() = runTest {
    val mockEngine = MockEngine {
      respond(
        content = ByteReadChannel("""{"code":"SOME_NEW_CODE","message":"Something went wrong."}"""),
        status = HttpStatusCode.InternalServerError,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }

    val apiClient = FlyerApiClientImpl(createTestHttpClient(mockEngine))
    val exception =
      assertFailsWith<FlyerApiException> { apiClient.processFlyer("base64", "image/jpeg") }

    assertEquals(FlyerApiErrorCode.UNKNOWN, exception.errorCode)
    assertEquals("Something went wrong.", exception.message)
  }

  @Test
  fun `test processFlyer falls back to ResponseException when error body is not structured`() =
    runTest {
      val mockEngine = MockEngine {
        respond(
          content = ByteReadChannel("Internal Server Error"),
          status = HttpStatusCode.InternalServerError,
          headers = headersOf(HttpHeaders.ContentType, "text/plain"),
        )
      }

      val apiClient = FlyerApiClientImpl(createTestHttpClient(mockEngine))
      assertFailsWith<ResponseException> { apiClient.processFlyer("base64", "image/jpeg") }
    }

  /** Helper function to create an HTTP client with mock engine and JSON support. */
  private fun createTestHttpClient(mockEngine: MockEngine): HttpClient {
    return HttpClient(mockEngine) {
      install(ContentNegotiation) {
        json(
          Json {
            ignoreUnknownKeys = true
            isLenient = true
          }
        )
      }
    }
  }
}
