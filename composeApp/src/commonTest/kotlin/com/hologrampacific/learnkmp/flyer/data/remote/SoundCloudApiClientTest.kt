package com.hologrampacific.learnkmp.flyer.data.remote

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class SoundCloudApiClientTest {

  @Test
  fun `test token cache uses cache when token not expired`() = runTest {
    // Given: Mock HTTP client that tracks token requests
    var tokenRequestCount = 0
    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          tokenRequestCount++
          respond(
            content = ByteReadChannel("""{"access_token":"test_token","expires_in":3600}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }
        request.url.toString().contains("/users") -> {
          respond(
            content = ByteReadChannel("""[]"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }
        else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
      }
    }

    val httpClient = createTestHttpClient(mockEngine)
    val apiClient = SoundCloudApiClientImpl(httpClient)

    // When: Make two consecutive API calls
    apiClient.searchUsers("test1")
    apiClient.searchUsers("test2")

    // Then: Token should only be requested once (cached for second call)
    assertEquals(1, tokenRequestCount, "Token should be cached and reused")
  }

  @Test
  fun `test token cache requests new token when expired`() = runTest {
    // Given: Mock that returns token with very short expiration
    var tokenRequestCount = 0
    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          tokenRequestCount++
          // Return token that expires in 1 second
          respond(
            content =
              ByteReadChannel("""{"access_token":"token_$tokenRequestCount","expires_in":1}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }
        request.url.toString().contains("/users") -> {
          respond(
            content = ByteReadChannel("""[]"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }
        else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
      }
    }

    val httpClient = createTestHttpClient(mockEngine)
    val apiClient = SoundCloudApiClientImpl(httpClient)

    // When: Make API call, wait for expiration, then make another call
    apiClient.searchUsers("test1")
    assertEquals(1, tokenRequestCount, "First call should request token")

    // Wait for token to expire (1 second + 1 second buffer = should be expired)
    delay(1100)

    apiClient.searchUsers("test2")

    // Then: Should request a new token since the first one expired
    assertEquals(2, tokenRequestCount, "Second call should request new token after expiration")
  }

  @Test
  fun `test token cache refreshes on 401`() = runTest {
    // Given: Mock that returns 401 on first attempt with valid token, then succeeds
    var tokenRequestCount = 0
    var userSearchAttempts = 0

    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          tokenRequestCount++
          respond(
            content =
              ByteReadChannel("""{"access_token":"token_$tokenRequestCount","expires_in":3600}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }
        request.url.toString().contains("/users") -> {
          userSearchAttempts++
          // First attempt returns 401 (simulating expired token not yet detected)
          // Second attempt succeeds (after token refresh)
          if (userSearchAttempts == 1) {
            respond(
              content = ByteReadChannel("""{"error":"Unauthorized"}"""),
              status = HttpStatusCode.Unauthorized,
              headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
          } else {
            respond(
              content = ByteReadChannel("""[]"""),
              status = HttpStatusCode.OK,
              headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
          }
        }
        else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
      }
    }

    val httpClient = createTestHttpClient(mockEngine)
    val apiClient = SoundCloudApiClientImpl(httpClient)

    // When: Make API call that will receive 401 and auto-retry
    val result = apiClient.searchUsers("test")

    // Then: Should have requested token twice (initial + refresh after 401)
    assertEquals(2, tokenRequestCount, "Should refresh token after 401")
    assertEquals(2, userSearchAttempts, "Should retry user search after token refresh")
    assertTrue(result.isEmpty(), "Search should succeed after token refresh")
  }

  @Test
  fun `test token cache defaults to one hour when expires in null`() = runTest {
    // Given: Mock that returns token without expires_in field
    var tokenRequestCount = 0
    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          tokenRequestCount++
          // Return token without expires_in field
          respond(
            content = ByteReadChannel("""{"access_token":"test_token"}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }
        request.url.toString().contains("/users") -> {
          respond(
            content = ByteReadChannel("""[]"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }
        else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
      }
    }

    val httpClient = createTestHttpClient(mockEngine)
    val apiClient = SoundCloudApiClientImpl(httpClient)

    // When: Make multiple API calls within short timeframe
    apiClient.searchUsers("test1")
    apiClient.searchUsers("test2")

    // Then: Token should still be cached (defaulting to 1 hour expiration)
    assertEquals(1, tokenRequestCount, "Token should default to 1 hour expiration and be cached")
  }

  @Test
  fun `test token cache handles 401 on fetchTracks`() = runTest {
    // Given: Mock that returns 401 on resolve endpoint, then succeeds on retry
    var tokenRequestCount = 0
    var resolveAttempts = 0

    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          tokenRequestCount++
          respond(
            content =
              ByteReadChannel("""{"access_token":"token_$tokenRequestCount","expires_in":3600}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }
        request.url.toString().contains("/resolve") -> {
          resolveAttempts++
          if (resolveAttempts == 1) {
            respond(
              content = ByteReadChannel("""{"error":"Unauthorized"}"""),
              status = HttpStatusCode.Unauthorized,
              headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
          } else {
            respond(
              content =
                ByteReadChannel(
                  """{"id":12345,"kind":"user","permalink":"testuser","permalink_url":"https://soundcloud.com/testuser"}"""
                ),
              status = HttpStatusCode.OK,
              headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
          }
        }
        request.url.toString().contains("/users/12345/tracks") -> {
          respond(
            content = ByteReadChannel("""[]"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }
        else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
      }
    }

    val httpClient = createTestHttpClient(mockEngine)
    val apiClient = SoundCloudApiClientImpl(httpClient)

    // When: Fetch popular tracks which internally uses resolve endpoint
    val tracks = apiClient.getTracks("https://soundcloud.com/testuser")

    // Then: Should refresh token and retry after 401
    assertEquals(2, tokenRequestCount, "Should refresh token after 401 on resolve endpoint")
    assertEquals(2, resolveAttempts, "Should retry resolve after token refresh")
    assertTrue(tracks.isEmpty(), "Should successfully fetch tracks after token refresh")
  }

  @Test
  fun `test searchUsers throws RateLimitException on 429 with play format`() = runTest {
    // Given: Mock that returns 429 with play request rate limit format
    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          respond(
            content = ByteReadChannel("""{"access_token":"test_token","expires_in":3600}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        request.url.toString().contains("/users") -> {
          respond(
            content =
              ByteReadChannel(
                """{"errors":[{"meta":{"rate_limit":{"group":"plays","max_nr_of_requests":15000,"time_window":"PT24H"},"remaining_requests":0,"reset_time":"2026/02/08 14:30:00 +0000"}}]}"""
              ),
            status = HttpStatusCode.TooManyRequests,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
      }
    }

    val httpClient = createTestHttpClient(mockEngine)
    val apiClient = SoundCloudApiClientImpl(httpClient)

    // When/Then: Should throw RateLimitException with parsed details
    val exception = assertFailsWith<RateLimitException> { apiClient.searchUsers("test") }
    assertEquals("2026/02/08 14:30:00 +0000", exception.resetTime)
    assertEquals(15000, exception.maxRequests)
    assertEquals("PT24H", exception.timeWindow)
  }

  @Test
  fun `test searchUsers throws RateLimitException on 429 with general format`() = runTest {
    // Given: Mock that returns 429 with general rate limit format
    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          respond(
            content = ByteReadChannel("""{"access_token":"test_token","expires_in":3600}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        request.url.toString().contains("/users") -> {
          respond(
            content = ByteReadChannel("""{"message":"Too many requests","status":"429"}"""),
            status = HttpStatusCode.TooManyRequests,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
      }
    }

    val httpClient = createTestHttpClient(mockEngine)
    val apiClient = SoundCloudApiClientImpl(httpClient)

    // When/Then: Should throw RateLimitException with unknown reset time
    val exception = assertFailsWith<RateLimitException> { apiClient.searchUsers("test") }
    assertEquals("Unknown - please wait and try again later", exception.resetTime)
    assertEquals(0, exception.maxRequests)
    assertEquals("Unknown", exception.timeWindow)
  }

  @Test
  fun `test getTracks throws RateLimitException on 429 during resolve`() = runTest {
    // Given: Mock that returns 429 on resolve endpoint
    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          respond(
            content = ByteReadChannel("""{"access_token":"test_token","expires_in":3600}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        request.url.toString().contains("/resolve") -> {
          respond(
            content = ByteReadChannel("""{"message":"Too many requests","status":"429"}"""),
            status = HttpStatusCode.TooManyRequests,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
      }
    }

    val httpClient = createTestHttpClient(mockEngine)
    val apiClient = SoundCloudApiClientImpl(httpClient)

    // When/Then: Should throw RateLimitException when resolving URL
    val exception =
      assertFailsWith<RateLimitException> { apiClient.getTracks("https://soundcloud.com/testuser") }
    assertEquals("Unknown - please wait and try again later", exception.resetTime)
  }

  @Test
  fun `test getTracks throws RateLimitException on 429 during tracks fetch`() = runTest {
    // Given: Mock that succeeds on resolve but returns 429 on tracks fetch
    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          respond(
            content = ByteReadChannel("""{"access_token":"test_token","expires_in":3600}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        request.url.toString().contains("/resolve") -> {
          respond(
            content =
              ByteReadChannel(
                """{"id":12345,"kind":"user","permalink":"testuser","permalink_url":"https://soundcloud.com/testuser"}"""
              ),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        request.url.toString().contains("/users/12345/tracks") -> {
          respond(
            content =
              ByteReadChannel(
                """{"errors":[{"meta":{"rate_limit":{"group":"plays","max_nr_of_requests":15000,"time_window":"PT24H"},"remaining_requests":0,"reset_time":"2026/02/08 15:00:00 +0000"}}]}"""
              ),
            status = HttpStatusCode.TooManyRequests,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
      }
    }

    val httpClient = createTestHttpClient(mockEngine)
    val apiClient = SoundCloudApiClientImpl(httpClient)

    // When/Then: Should throw RateLimitException when fetching tracks
    val exception =
      assertFailsWith<RateLimitException> { apiClient.getTracks("https://soundcloud.com/testuser") }
    assertEquals("2026/02/08 15:00:00 +0000", exception.resetTime)
    assertEquals(15000, exception.maxRequests)
    assertEquals("PT24H", exception.timeWindow)
  }

  @Test
  fun `test searchUsers does not retry on 429`() = runTest {
    // Given: Mock that tracks number of user search requests
    var searchRequestCount = 0
    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          respond(
            content = ByteReadChannel("""{"access_token":"test_token","expires_in":3600}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        request.url.toString().contains("/users") -> {
          searchRequestCount++
          respond(
            content = ByteReadChannel("""{"message":"Too many requests","status":"429"}"""),
            status = HttpStatusCode.TooManyRequests,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
      }
    }

    val httpClient = createTestHttpClient(mockEngine)
    val apiClient = SoundCloudApiClientImpl(httpClient)

    // When: searchUsers hits rate limit
    assertFailsWith<RateLimitException> { apiClient.searchUsers("test") }

    // Then: Should not retry (only one request made)
    assertEquals(1, searchRequestCount, "Should not retry on 429")
  }

  @Test
  fun `test getTracks does not retry on 429`() = runTest {
    // Given: Mock that tracks number of resolve requests
    var resolveRequestCount = 0
    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          respond(
            content = ByteReadChannel("""{"access_token":"test_token","expires_in":3600}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        request.url.toString().contains("/resolve") -> {
          resolveRequestCount++
          respond(
            content = ByteReadChannel("""{"message":"Too many requests","status":"429"}"""),
            status = HttpStatusCode.TooManyRequests,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
      }
    }

    val httpClient = createTestHttpClient(mockEngine)
    val apiClient = SoundCloudApiClientImpl(httpClient)

    // When: getTracks hits rate limit on resolve
    assertFailsWith<RateLimitException> { apiClient.getTracks("https://soundcloud.com/testuser") }

    // Then: Should not retry (only one request made)
    assertEquals(1, resolveRequestCount, "Should not retry on 429")
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
