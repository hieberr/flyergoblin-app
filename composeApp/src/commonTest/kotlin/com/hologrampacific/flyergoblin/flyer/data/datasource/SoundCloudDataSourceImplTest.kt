package com.hologrampacific.flyergoblin.flyer.data.datasource

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.data.remote.ApiRateLimitException
import com.hologrampacific.flyergoblin.flyer.data.remote.ClientErrorException
import com.hologrampacific.flyergoblin.flyer.data.remote.ServerErrorException
import com.hologrampacific.flyergoblin.flyer.data.remote.SoundCloudApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.SoundCloudApiException
import com.hologrampacific.flyergoblin.flyer.data.remote.SoundCloudUser
import com.hologrampacific.flyergoblin.flyer.domain.datasource.SoundcloudProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class SoundCloudDataSourceImplTest : AppTest() {

  @Test
  fun `test searchSoundCloudProfiles empty search results`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    everySuspend { mockClient.searchUsers("NonExistentArtist") } returns emptyList()
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("NonExistentArtist")

    // Then
    assertIs<SoundcloudProfileSearchResult.Error>(result)
    assertEquals("No SoundCloud profile found for \"NonExistentArtist\"", (result).message)
    verifySuspend { mockClient.searchUsers("NonExistentArtist") }
  }

  @Test
  fun `test searchSoundCloudProfiles blank artist name`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("   ")

    // Then
    assertIs<SoundcloudProfileSearchResult.Error>(result)
    assertEquals("Artist name cannot be empty", result.message)
  }

  @Test
  fun `test searchSoundCloudProfiles empty artist name`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("")

    // Then
    assertIs<SoundcloudProfileSearchResult.Error>(result)
    assertEquals("Artist name cannot be empty", result.message)
  }

  @Test
  fun `test searchSoundCloudProfiles artist name too long`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    val dataSource = SoundCloudDataSourceImpl(mockClient)
    val longName = "a".repeat(201)

    // When
    val result = dataSource.searchSoundCloudProfiles(longName)

    // Then
    assertIs<SoundcloudProfileSearchResult.Error>(result)
    assertEquals("Artist name is too long", result.message)
  }

  @Test
  fun `test searchSoundCloudProfiles success with permalink`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    val permalinkUrl = "https://soundcloud.com/testartist"
    val searchResults =
      listOf(
        SoundCloudUser(
          id = 123,
          username = "testartist",
          permalink = "testartist",
          permalinkUrl = permalinkUrl,
          followersCount = 1000,
          trackCount = 42,
          city = "Berlin",
          countryCode = "DE",
        )
      )
    everySuspend { mockClient.searchUsers("Test Artist") } returns searchResults
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("Test Artist")

    // Then
    assertIs<SoundcloudProfileSearchResult.Success>(result)
    assertEquals(permalinkUrl, result.profiles.first().profileUrl)
    assertEquals(1, result.profiles.size)
    assertEquals(
      SoundCloudProfileInfo(
        id = 123,
        username = "testartist",
        profileUrl = permalinkUrl,
        followersCount = 1000,
        trackCount = 42,
        city = "Berlin",
        countryCode = "DE",
      ),
      result.profiles.first(),
    )
    verifySuspend { mockClient.searchUsers("Test Artist") }
  }

  @Test
  fun `test searchSoundCloudProfiles maps avatarUrl and fullName`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    val permalinkUrl = "https://soundcloud.com/testartist"
    val searchResults =
      listOf(
        SoundCloudUser(
          id = 123,
          username = "testartist",
          permalink = "testartist",
          permalinkUrl = permalinkUrl,
          avatarUrl = "https://i1.sndcdn.com/avatars-000051966075-igrx67-large.jpg",
          fullName = "Test Artist Full Name",
          trackCount = 100,
        )
      )
    everySuspend { mockClient.searchUsers("Test Artist") } returns searchResults
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("Test Artist")

    // Then
    assertIs<SoundcloudProfileSearchResult.Success>(result)
    val profile = result.profiles.first()
    assertEquals("https://i1.sndcdn.com/avatars-000051966075-igrx67-large.jpg", profile.avatarUrl)
    assertEquals("Test Artist Full Name", profile.fullName)
  }

  @Test
  fun `test searchSoundCloudProfiles maps null avatarUrl and fullName`() = runTest {
    // Given: User without avatarUrl or fullName
    val mockClient = mock<SoundCloudApiClient>()
    val searchResults =
      listOf(
        SoundCloudUser(
          id = 123,
          username = "testartist",
          permalink = "testartist",
          permalinkUrl = "https://soundcloud.com/testartist",
          trackCount = 100,
        )
      )
    everySuspend { mockClient.searchUsers("Test Artist") } returns searchResults
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("Test Artist")

    // Then
    assertIs<SoundcloudProfileSearchResult.Success>(result)
    val profile = result.profiles.first()
    assertEquals(null, profile.avatarUrl)
    assertEquals(null, profile.fullName)
  }

  @Test
  fun `test searchSoundCloudProfiles url construction fallback`() = runTest {
    // Given: User without permalinkUrl (should fall back to constructing URL from permalink)
    val mockClient = mock<SoundCloudApiClient>()
    val searchResults =
      listOf(
        SoundCloudUser(
          id = 456,
          username = "artist-name",
          permalink = "artist-name",
          permalinkUrl = null,
          followersCount = 500,
          trackCount = 100,
        )
      )
    everySuspend { mockClient.searchUsers("Artist Name") } returns searchResults
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("Artist Name")

    // Then
    assertIs<SoundcloudProfileSearchResult.Success>(result)
    assertEquals("https://soundcloud.com/artist-name", result.profiles.first().profileUrl)
    verifySuspend { mockClient.searchUsers("Artist Name") }
  }

  @Test
  fun `test searchSoundCloudProfiles url construction with special characters`() = runTest {
    // Given: permalink with special characters that need URL encoding
    val mockClient = mock<SoundCloudApiClient>()
    val searchResults =
      listOf(
        SoundCloudUser(
          id = 789,
          username = "dj name",
          permalink = "dj name",
          permalinkUrl = null,
          followersCount = 300,
          trackCount = 100,
        )
      )
    everySuspend { mockClient.searchUsers("DJ Name") } returns searchResults
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("DJ Name")

    // Then
    assertIs<SoundcloudProfileSearchResult.Success>(result)
    val url = (result).profiles.first().profileUrl
    // Should have URL-encoded space as %20
    assertEquals("https://soundcloud.com/dj%20name", url)
    verifySuspend { mockClient.searchUsers("DJ Name") }
  }

  @Test
  fun `test searchSoundCloudProfiles filters out profiles with no tracks`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    val searchResults =
      listOf(
        SoundCloudUser(
          id = 1,
          username = "artist1",
          permalink = "artist1",
          permalinkUrl = "https://soundcloud.com/artist1",
          trackCount = 100,
        ),
        SoundCloudUser(
          id = 2,
          username = "artist2",
          permalink = "artist2",
          permalinkUrl = "https://soundcloud.com/artist2",
          trackCount = 0,
        ),
      )
    everySuspend { mockClient.searchUsers("Test Artist") } returns searchResults
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("  Test Artist  ")

    // Then
    assertIs<SoundcloudProfileSearchResult.Success>(result)
    assertEquals(result.profiles.count(), 1)
    assertEquals(result.profiles.first().username, "artist1")
  }

  @Test
  fun `test searchSoundCloudProfiles trims whitespace`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    val searchResults =
      listOf(
        SoundCloudUser(
          id = 1,
          username = "artist",
          permalink = "artist",
          permalinkUrl = "https://soundcloud.com/artist",
          followersCount = 100,
        )
      )
    everySuspend { mockClient.searchUsers("Test Artist") } returns searchResults
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When: Search with leading/trailing whitespace
    val result = dataSource.searchSoundCloudProfiles("  Test Artist  ")

    // Then: Should still find results (whitespace trimmed)
    assertIs<SoundcloudProfileSearchResult.Success>(result)
    verifySuspend { mockClient.searchUsers("Test Artist") }
  }

  @Test
  fun `test searchSoundCloudProfiles rate limit exception returns RateLimited result`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    val blockedUntil = Clock.System.now() + 60.minutes
    everySuspend { mockClient.searchUsers("Test Artist") } throws
      ApiRateLimitException(blockedUntil = blockedUntil, message = "Rate limit exceeded")
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("Test Artist")

    // Then
    assertIs<SoundcloudProfileSearchResult.RateLimited>(result)
    assertEquals(blockedUntil, result.blockedUntil)
    verifySuspend { mockClient.searchUsers("Test Artist") }
  }

  @Test
  fun `test searchSoundCloudProfiles server error returns Error result`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    everySuspend { mockClient.searchUsers("Test Artist") } throws
      ServerErrorException(statusCode = 503, message = "Service unavailable")
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("Test Artist")

    // Then
    assertIs<SoundcloudProfileSearchResult.Error>(result)
    assertEquals("SoundCloud server error. Please try again later.", result.message)
    verifySuspend { mockClient.searchUsers("Test Artist") }
  }

  @Test
  fun `test searchSoundCloudProfiles client error returns Error result`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    everySuspend { mockClient.searchUsers("Test Artist") } throws
      ClientErrorException(statusCode = 400, message = "Bad request")
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("Test Artist")

    // Then
    assertIs<SoundcloudProfileSearchResult.Error>(result)
    assertEquals("Failed to search SoundCloud. Please try again.", result.message)
    verifySuspend { mockClient.searchUsers("Test Artist") }
  }

  @Test
  fun `test searchSoundCloudProfiles api exception returns Error result`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    everySuspend { mockClient.searchUsers("Test Artist") } throws
      SoundCloudApiException("Unknown API error")
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("Test Artist")

    // Then
    assertIs<SoundcloudProfileSearchResult.Error>(result)
    assertEquals("Failed to search SoundCloud.", result.message)
    verifySuspend { mockClient.searchUsers("Test Artist") }
  }

  @Test
  fun `test searchSoundCloudProfiles returns RateLimited result on each call when client throws ApiRateLimitException`() =
    runTest {
      // Given
      val mockClient = mock<SoundCloudApiClient>()
      val blockedUntil = Instant.parse("2026-02-08T14:30:00Z")
      everySuspend { mockClient.searchUsers(any()) } throws
        ApiRateLimitException(blockedUntil = blockedUntil, message = "Rate limit exceeded")
      val dataSource = SoundCloudDataSourceImpl(mockClient)

      val result1 = dataSource.searchSoundCloudProfiles("Artist1")
      assertIs<SoundcloudProfileSearchResult.RateLimited>(result1)

      // When/Then: Each call returns RateLimited when the client throws ApiRateLimitException
      val result2 = dataSource.searchSoundCloudProfiles("Artist2")
      assertIs<SoundcloudProfileSearchResult.RateLimited>(result2)
      assertEquals(blockedUntil, result2.blockedUntil)
    }

  @Test
  fun `test getTracksForProfile rate limit exception propagates`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    val blockedUntil = Instant.parse("2026-02-08T14:30:00Z")
    val userId = 123L
    everySuspend { mockClient.getTracks(userId) } throws
      ApiRateLimitException(blockedUntil = blockedUntil, message = "Rate limit exceeded")
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When/Then
    val e = assertFailsWith<ApiRateLimitException> { dataSource.getTracksForProfile(userId) }
    assertEquals(blockedUntil, e.blockedUntil)
    verifySuspend { mockClient.getTracks(userId) }
  }
}
