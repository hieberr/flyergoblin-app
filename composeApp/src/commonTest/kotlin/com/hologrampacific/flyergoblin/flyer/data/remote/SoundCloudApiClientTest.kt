package com.hologrampacific.flyergoblin.flyer.data.remote

import com.hologrampacific.flyergoblin.AppTest
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
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class SoundCloudApiClientTest : AppTest() {

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
    // Given: Mock that returns 401 on tracks endpoint, then succeeds on retry
    var tokenRequestCount = 0
    var tracksAttempts = 0

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
        request.url.toString().contains("/users/12345/tracks") -> {
          tracksAttempts++
          if (tracksAttempts == 1) {
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

    // When: Fetch tracks which auto-retries on 401 with refreshed token
    val tracks = apiClient.getTracks(12345L)

    // Then: Should refresh token and retry after 401
    assertEquals(2, tokenRequestCount, "Should refresh token after 401 on tracks endpoint")
    assertEquals(2, tracksAttempts, "Should retry tracks fetch after token refresh")
    assertTrue(tracks.isEmpty(), "Should successfully fetch tracks after token refresh")
  }

  @Test
  fun `test searchUsers throws ApiRateLimitException on 429 with play format`() = runTest {
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

    // When/Then: Should throw ApiRateLimitException with parsed blockedUntil
    val exception = assertFailsWith<ApiRateLimitException> { apiClient.searchUsers("test") }
    assertEquals(Instant.parse("2026-02-08T14:30:00Z"), exception.blockedUntil)
  }

  @Test
  fun `test searchUsers throws ApiRateLimitException on 429 with general format`() = runTest {
    // Given: Mock that returns 429 with the general rate limit format (no code field)
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

    // When/Then: Should throw ApiRateLimitException via SoundCloudGeneralRateLimitError parse path
    val exception = assertFailsWith<ApiRateLimitException> { apiClient.searchUsers("test") }
    assertTrue(exception.blockedUntil > Clock.System.now())
  }

  @Test
  fun `test getTracks throws ApiRateLimitException on 429`() = runTest {
    // Given: Mock that returns 429 on tracks endpoint
    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          respond(
            content = ByteReadChannel("""{"access_token":"test_token","expires_in":3600}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        request.url.toString().contains("/users/12345/tracks") -> {
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

    // When/Then: Should throw ApiRateLimitException when fetching tracks
    val exception = assertFailsWith<ApiRateLimitException> { apiClient.getTracks(12345L) }
    assertTrue(exception.blockedUntil > Clock.System.now())
  }

  @Test
  fun `test getTracks throws ApiRateLimitException on 429 with play format during tracks fetch`() =
    runTest {
      // Given: Mock that returns 429 with play rate limit format on tracks fetch
      val mockEngine = MockEngine { request ->
        when {
          request.url.toString().contains("/oauth2/token") -> {
            respond(
              content = ByteReadChannel("""{"access_token":"test_token","expires_in":3600}"""),
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

      // When/Then: Should throw ApiRateLimitException when fetching tracks
      val exception = assertFailsWith<ApiRateLimitException> { apiClient.getTracks(12345L) }
      assertEquals(Instant.parse("2026-02-08T15:00:00Z"), exception.blockedUntil)
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
    assertFailsWith<ApiRateLimitException> { apiClient.searchUsers("test") }

    // Then: Should not retry (only one request made)
    assertEquals(1, searchRequestCount, "Should not retry on 429")
  }

  @Test
  fun `test getTracks does not retry on 429`() = runTest {
    // Given: Mock that tracks number of tracks requests
    var tracksRequestCount = 0
    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          respond(
            content = ByteReadChannel("""{"access_token":"test_token","expires_in":3600}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        request.url.toString().contains("/users/12345/tracks") -> {
          tracksRequestCount++
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

    // When: getTracks hits rate limit
    assertFailsWith<ApiRateLimitException> { apiClient.getTracks(12345L) }

    // Then: Should not retry (only one request made)
    assertEquals(1, tracksRequestCount, "Should not retry on 429")
  }

  @Test
  fun `test searchUsers blocks subsequent calls when rate limited`() = runTest {
    // Given: Mock that returns 429 on first call
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
    val apiClient = SoundCloudApiClientImpl(createTestHttpClient(mockEngine))

    // When: First call hits rate limit
    assertFailsWith<ApiRateLimitException> { apiClient.searchUsers("test") }
    assertEquals(1, searchRequestCount, "First call should make HTTP request")

    // When: Second call should be blocked without making HTTP request
    assertFailsWith<ApiRateLimitException> { apiClient.searchUsers("test") }
    assertEquals(1, searchRequestCount, "Second call should be blocked, no HTTP request")
  }

  @Test
  fun `test getTracks blocks subsequent calls when rate limited`() = runTest {
    // Given: Mock that returns 429 on first call
    var tracksRequestCount = 0
    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          respond(
            content = ByteReadChannel("""{"access_token":"test_token","expires_in":3600}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        request.url.toString().contains("/users/12345/tracks") -> {
          tracksRequestCount++
          respond(
            content = ByteReadChannel("""{"message":"Too many requests","status":"429"}"""),
            status = HttpStatusCode.TooManyRequests,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
      }
    }
    val apiClient = SoundCloudApiClientImpl(createTestHttpClient(mockEngine))

    // When: First call hits rate limit
    assertFailsWith<ApiRateLimitException> { apiClient.getTracks(12345L) }
    assertEquals(1, tracksRequestCount, "First call should make HTTP request")

    // When: Second call should be blocked without making HTTP request
    assertFailsWith<ApiRateLimitException> { apiClient.getTracks(12345L) }
    assertEquals(1, tracksRequestCount, "Second call should be blocked, no HTTP request")
  }

  @Test
  fun `test getTracks shares rate limit state with searchUsers`() = runTest {
    // Given: getTracks hits rate limit
    var searchRequestCount = 0
    var tracksRequestCount = 0
    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          respond(
            content = ByteReadChannel("""{"access_token":"test_token","expires_in":3600}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        request.url.toString().contains("/users/12345/tracks") -> {
          tracksRequestCount++
          respond(
            content =
              ByteReadChannel(
                """{"errors":[{"meta":{"rate_limit":{"group":"plays","max_nr_of_requests":15000,"time_window":"PT24H"},"remaining_requests":0,"reset_time":"2099/01/01 00:00:00 +0000"}}]}"""
              ),
            status = HttpStatusCode.TooManyRequests,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        request.url.toString().contains("/users") -> {
          searchRequestCount++
          respond(
            content = ByteReadChannel("""[]"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
      }
    }
    val apiClient = SoundCloudApiClientImpl(createTestHttpClient(mockEngine))

    // When: getTracks hits rate limit
    val exception = assertFailsWith<ApiRateLimitException> { apiClient.getTracks(12345L) }
    assertEquals(Instant.parse("2099-01-01T00:00:00Z"), exception.blockedUntil)
    assertEquals(1, tracksRequestCount)

    // Then: searchUsers should be blocked without making HTTP request
    assertFailsWith<ApiRateLimitException> { apiClient.searchUsers("test") }
    assertEquals(
      0,
      searchRequestCount,
      "searchUsers should be blocked by rate limit from getTracks",
    )
  }

  @Test
  fun `test searchUsers shares rate limit state with getTracks`() = runTest {
    // Given: searchUsers hits rate limit
    var searchRequestCount = 0
    var tracksRequestCount = 0
    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          respond(
            content = ByteReadChannel("""{"access_token":"test_token","expires_in":3600}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        request.url.toString().contains("/users/12345/tracks") -> {
          tracksRequestCount++
          respond(
            content = ByteReadChannel("""[]"""),
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
    val apiClient = SoundCloudApiClientImpl(createTestHttpClient(mockEngine))

    // When: searchUsers hits rate limit
    assertFailsWith<ApiRateLimitException> { apiClient.searchUsers("test") }
    assertEquals(1, searchRequestCount)

    // Then: getTracks should be blocked without making HTTP request
    assertFailsWith<ApiRateLimitException> { apiClient.getTracks(12345L) }
    assertEquals(
      0,
      tracksRequestCount,
      "getTracks should be blocked by rate limit from searchUsers",
    )
  }

  @Test
  fun `test searchUsers auto-clears rate limit when reset time is in the past`() = runTest {
    // Given: Mock returns 429 with a reset time that is already in the past
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
          when (searchRequestCount) {
            1 ->
              respond(
                // Reset time in the past — should auto-clear on next check
                content =
                  ByteReadChannel(
                    """{"errors":[{"meta":{"rate_limit":{"group":"plays","max_nr_of_requests":15000,"time_window":"PT24H"},"remaining_requests":0,"reset_time":"2020/01/01 00:00:00 +0000"}}]}"""
                  ),
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
              )

            else ->
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
    val apiClient = SoundCloudApiClientImpl(createTestHttpClient(mockEngine))

    // When: First call hits rate limit with a past reset time
    assertFailsWith<ApiRateLimitException> { apiClient.searchUsers("test") }
    assertEquals(1, searchRequestCount)

    // Then: Second call should proceed because the reset time has already passed
    val result = apiClient.searchUsers("test")
    assertTrue(result.isEmpty())
    assertEquals(2, searchRequestCount, "Second call should make HTTP request after auto-expiry")
  }

  @Test
  fun `test getTracks success clears rate limit state`() = runTest {
    // Given: First call returns 429, then success (new client with no prior state)
    var tracksRequestCount = 0
    val mockEngine = MockEngine { request ->
      when {
        request.url.toString().contains("/oauth2/token") -> {
          respond(
            content = ByteReadChannel("""{"access_token":"test_token","expires_in":3600}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        request.url.toString().contains("/users/12345/tracks") -> {
          tracksRequestCount++
          respond(
            content = ByteReadChannel("""[]"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
          )
        }

        else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
      }
    }
    val apiClient = SoundCloudApiClientImpl(createTestHttpClient(mockEngine))

    // When: Successful call
    val tracks = apiClient.getTracks(12345L)

    // Then: Should succeed and subsequent calls should also succeed (no rate limit state set)
    assertTrue(tracks.isEmpty())
    val tracks2 = apiClient.getTracks(12345L)
    assertTrue(tracks2.isEmpty())
    assertEquals(2, tracksRequestCount, "Both calls should make HTTP requests")
  }

  @Test
  fun `test parseResetTime parses valid SoundCloud date string`() {
    val instant = SoundCloudApiClientImpl.parseResetTime("2026/02/08 14:30:00 +0000")
    assertEquals(
      "2026-02-08T14:30:00Z",
      instant?.toString(),
      "Should parse SoundCloud date format to ISO 8601 instant",
    )
  }

  @Test
  fun `test parseResetTime returns null for unparseable string`() {
    val instant =
      SoundCloudApiClientImpl.parseResetTime("Unknown - please wait and try again later")
    assertEquals(null, instant, "Should return null for unrecognised format")
  }

  @Test
  fun `test parseResetTime handles non-UTC timezone offset`() {
    val instant = SoundCloudApiClientImpl.parseResetTime("2026/06/15 09:00:00 +0530")
    assertEquals(
      "2026-06-15T03:30:00Z",
      instant?.toString(),
      "Should correctly convert +0530 offset to UTC",
    )
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
