package com.hologrampacific.flyergoblin.flyer.data.datasource

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.data.remote.ApiRateLimitException
import com.hologrampacific.flyergoblin.flyer.data.remote.ClientErrorException
import com.hologrampacific.flyergoblin.flyer.data.remote.ServerErrorException
import com.hologrampacific.flyergoblin.flyer.data.remote.SoundCloudApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.SoundCloudApiException
import com.hologrampacific.flyergoblin.flyer.data.remote.SoundCloudUser
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistProfileSearchResult
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
import kotlin.test.assertTrue
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
    assertTrue(result is ArtistProfileSearchResult.Error)
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
    assertTrue(result is ArtistProfileSearchResult.Error)
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
    assertTrue(result is ArtistProfileSearchResult.Error)
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
    assertTrue(result is ArtistProfileSearchResult.Error)
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
    assertTrue(result is ArtistProfileSearchResult.Success)
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
        )
      )
    everySuspend { mockClient.searchUsers("Test Artist") } returns searchResults
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("Test Artist")

    // Then
    assertTrue(result is ArtistProfileSearchResult.Success)
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
        )
      )
    everySuspend { mockClient.searchUsers("Test Artist") } returns searchResults
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("Test Artist")

    // Then
    assertTrue(result is ArtistProfileSearchResult.Success)
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
        )
      )
    everySuspend { mockClient.searchUsers("Artist Name") } returns searchResults
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("Artist Name")

    // Then
    assertTrue(result is ArtistProfileSearchResult.Success)
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
        )
      )
    everySuspend { mockClient.searchUsers("DJ Name") } returns searchResults
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("DJ Name")

    // Then
    assertTrue(result is ArtistProfileSearchResult.Success)
    val url = (result).profiles.first().profileUrl
    // Should have URL-encoded space as %20
    assertEquals("https://soundcloud.com/dj%20name", url)
    verifySuspend { mockClient.searchUsers("DJ Name") }
  }

  @Test
  fun `test searchSoundCloudProfiles sorts by followers descending`() = runTest {
    // Given: Multiple users with different follower counts
    val mockClient = mock<SoundCloudApiClient>()
    val searchResults =
      listOf(
        SoundCloudUser(
          id = 1,
          username = "artist1",
          permalink = "artist1",
          permalinkUrl = "https://soundcloud.com/artist1",
          followersCount = 100,
        ),
        SoundCloudUser(
          id = 2,
          username = "artist2",
          permalink = "artist2",
          permalinkUrl = "https://soundcloud.com/artist2",
          followersCount = 5000,
        ),
        SoundCloudUser(
          id = 3,
          username = "artist3",
          permalink = "artist3",
          permalinkUrl = "https://soundcloud.com/artist3",
          followersCount = 1000,
        ),
      )
    everySuspend { mockClient.searchUsers("Artist") } returns searchResults
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("Artist")

    // Then: Should return the artist with most followers (5000)
    assertTrue(result is ArtistProfileSearchResult.Success)
    assertEquals("https://soundcloud.com/artist2", (result).profiles.first().profileUrl)
    // Profiles should be sorted by followers descending
    assertEquals(3, result.profiles.size)
    assertEquals("artist2", result.profiles[0].username)
    assertEquals(5000, result.profiles[0].followersCount)
    assertEquals("artist3", result.profiles[1].username)
    assertEquals(1000, result.profiles[1].followersCount)
    assertEquals("artist1", result.profiles[2].username)
    assertEquals(100, result.profiles[2].followersCount)
    verifySuspend { mockClient.searchUsers("Artist") }
  }

  @Test
  fun `test searchSoundCloudProfiles null follower counts treated as zero`() = runTest {
    // Given: Users with null follower counts mixed with valid counts
    val mockClient = mock<SoundCloudApiClient>()
    val searchResults =
      listOf(
        SoundCloudUser(
          id = 1,
          username = "artist-no-followers",
          permalink = "artist-no-followers",
          permalinkUrl = "https://soundcloud.com/artist-no-followers",
          followersCount = null,
        ),
        SoundCloudUser(
          id = 2,
          username = "artist-with-followers",
          permalink = "artist-with-followers",
          permalinkUrl = "https://soundcloud.com/artist-with-followers",
          followersCount = 100,
        ),
      )
    everySuspend { mockClient.searchUsers("Artist") } returns searchResults
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("Artist")

    // Then: Should return the artist with actual follower count, not the null one
    assertTrue(result is ArtistProfileSearchResult.Success)
    assertEquals(
      "https://soundcloud.com/artist-with-followers",
      (result).profiles.first().profileUrl,
    )
    verifySuspend { mockClient.searchUsers("Artist") }
  }

  @Test
  fun `test searchSoundCloudProfiles all null follower counts`() = runTest {
    // Given: All users have null follower counts
    val mockClient = mock<SoundCloudApiClient>()
    val searchResults =
      listOf(
        SoundCloudUser(
          id = 1,
          username = "artist1",
          permalink = "artist1",
          permalinkUrl = "https://soundcloud.com/artist1",
          followersCount = null,
        ),
        SoundCloudUser(
          id = 2,
          username = "artist2",
          permalink = "artist2",
          permalinkUrl = "https://soundcloud.com/artist2",
          followersCount = null,
        ),
      )
    everySuspend { mockClient.searchUsers("Artist") } returns searchResults
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("Artist")

    // Then: Should still return a result (the first one after sorting)
    assertTrue(result is ArtistProfileSearchResult.Success)
    assertTrue((result).profiles.first().profileUrl.contains("soundcloud.com"))
    verifySuspend { mockClient.searchUsers("Artist") }
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
    assertTrue(result is ArtistProfileSearchResult.Success)
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
    assertTrue(result is ArtistProfileSearchResult.RateLimited)
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
    assertTrue(result is ArtistProfileSearchResult.Error)
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
    assertTrue(result is ArtistProfileSearchResult.Error)
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
    assertTrue(result is ArtistProfileSearchResult.Error)
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
      assertTrue(result1 is ArtistProfileSearchResult.RateLimited)

      // When/Then: Each call returns RateLimited when the client throws ApiRateLimitException
      val result2 = dataSource.searchSoundCloudProfiles("Artist2")
      assertTrue(result2 is ArtistProfileSearchResult.RateLimited)
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
