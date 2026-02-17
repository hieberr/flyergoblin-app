package com.hologrampacific.flyergoblin.flyer.data.datasource

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.data.remote.ClientErrorException
import com.hologrampacific.flyergoblin.flyer.data.remote.RateLimitException
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
import kotlin.test.assertTrue
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
    val resetTime = "2026/02/08 14:30:00 +0000"
    everySuspend { mockClient.searchUsers("Test Artist") } throws
      RateLimitException(
        message = "Rate limit exceeded",
        resetTime = resetTime,
        maxRequests = 15000,
        timeWindow = "PT24H",
      )
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.searchSoundCloudProfiles("Test Artist")

    // Then
    assertTrue(result is ArtistProfileSearchResult.RateLimited)
    assertEquals(resetTime, result.resetTime)
    assertEquals(15000, result.maxRequests)
    assertEquals("PT24H", result.timeWindow)
    verifySuspend { mockClient.searchUsers("Test Artist") }
  }

  @Test
  fun `test searchSoundCloudProfiles blocks subsequent calls when rate limited`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    val resetTime = "2026/02/08 14:30:00 +0000"
    everySuspend { mockClient.searchUsers(any()) } throws
      RateLimitException(
        message = "Rate limit exceeded",
        resetTime = resetTime,
        maxRequests = 15000,
        timeWindow = "PT24H",
      )
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When: First call hits rate limit
    val result1 = dataSource.searchSoundCloudProfiles("Test Artist")

    // Then: First call returns RateLimited
    assertTrue(result1 is ArtistProfileSearchResult.RateLimited)

    // When: Second call should be blocked without calling API
    val result2 = dataSource.searchSoundCloudProfiles("Test Artist")

    // Then: Second call also returns RateLimited with same reset time
    assertTrue(result2 is ArtistProfileSearchResult.RateLimited)
    assertEquals(resetTime, result2.resetTime)
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
  fun `test searchSoundCloudProfiles with different artists when rate limited`() = runTest {
    // Given: Rate limit hit for first artist
    val mockClient = mock<SoundCloudApiClient>()
    everySuspend { mockClient.searchUsers(any()) } throws
      RateLimitException(
        message = "Rate limit exceeded",
        resetTime = "2026/02/08 14:30:00 +0000",
        maxRequests = 15000,
        timeWindow = "PT24H",
      )
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    val result1 = dataSource.searchSoundCloudProfiles("Artist1")
    assertTrue(result1 is ArtistProfileSearchResult.RateLimited)

    // When: Try searching for a different artist
    val result2 = dataSource.searchSoundCloudProfiles("Artist2")

    // Then: Should still be rate limited (rate limit is global, not per-artist)
    assertTrue(result2 is ArtistProfileSearchResult.RateLimited)
    assertEquals("2026/02/08 14:30:00 +0000", result2.resetTime)
  }

  @Test
  fun `test getTracksForProfile rate limit exception propagates`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    val resetTime = "2026/02/08 14:30:00 +0000"
    val profileUrl = "https://soundcloud.com/testartist"
    everySuspend { mockClient.getTracks(profileUrl) } throws
      RateLimitException(
        message = "Rate limit exceeded",
        resetTime = resetTime,
        maxRequests = 15000,
        timeWindow = "PT24H",
      )
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When/Then
    try {
      dataSource.getTracksForProfile(profileUrl)
      throw AssertionError("Expected RateLimitException to be thrown")
    } catch (e: RateLimitException) {
      assertEquals(resetTime, e.resetTime)
      assertEquals(15000, e.maxRequests)
      assertEquals("PT24H", e.timeWindow)
    }
    verifySuspend { mockClient.getTracks(profileUrl) }
  }

  @Test
  fun `test getTracksForProfile blocks subsequent calls when rate limited`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    val resetTime = "2026/02/08 14:30:00 +0000"
    val profileUrl = "https://soundcloud.com/testartist"
    everySuspend { mockClient.getTracks(any()) } throws
      RateLimitException(
        message = "Rate limit exceeded",
        resetTime = resetTime,
        maxRequests = 15000,
        timeWindow = "PT24H",
      )
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When: First call hits rate limit
    try {
      dataSource.getTracksForProfile(profileUrl)
      throw AssertionError("Expected RateLimitException to be thrown")
    } catch (e: RateLimitException) {
      assertEquals(resetTime, e.resetTime)
    }

    // When: Second call should be blocked without calling API
    try {
      dataSource.getTracksForProfile(profileUrl)
      throw AssertionError("Expected RateLimitException to be thrown")
    } catch (e: RateLimitException) {
      // Then: Second call also throws RateLimitException with same reset time
      assertEquals(resetTime, e.resetTime)
      assertEquals(15000, e.maxRequests)
      assertEquals("PT24H", e.timeWindow)
    }
  }

  @Test
  fun `test getTracksForProfile shares rate limit state with searchSoundCloudProfiles`() = runTest {
    // Given: searchSoundCloudProfiles hits rate limit first
    val mockClient = mock<SoundCloudApiClient>()
    val resetTime = "2026/02/08 14:30:00 +0000"
    everySuspend { mockClient.searchUsers(any()) } throws
      RateLimitException(
        message = "Rate limit exceeded",
        resetTime = resetTime,
        maxRequests = 15000,
        timeWindow = "PT24H",
      )
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    val searchResult = dataSource.searchSoundCloudProfiles("Test Artist")
    assertTrue(searchResult is ArtistProfileSearchResult.RateLimited)

    // When: Try to fetch tracks for a profile (should be blocked without calling API)
    val profileUrl = "https://soundcloud.com/testartist"
    try {
      dataSource.getTracksForProfile(profileUrl)
      throw AssertionError("Expected RateLimitException to be thrown")
    } catch (e: RateLimitException) {
      // Then: Should be blocked by cached rate limit state from searchSoundCloudProfiles
      assertEquals(resetTime, e.resetTime)
      assertEquals(15000, e.maxRequests)
      assertEquals("PT24H", e.timeWindow)
    }
  }

  @Test
  fun `test searchSoundCloudProfiles shares rate limit state with getTracksForProfile`() = runTest {
    // Given: getTracksForProfile hits rate limit first
    val mockClient = mock<SoundCloudApiClient>()
    val resetTime = "2026/02/08 14:30:00 +0000"
    val profileUrl = "https://soundcloud.com/testartist"
    everySuspend { mockClient.getTracks(any()) } throws
      RateLimitException(
        message = "Rate limit exceeded",
        resetTime = resetTime,
        maxRequests = 15000,
        timeWindow = "PT24H",
      )
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    try {
      dataSource.getTracksForProfile(profileUrl)
      throw AssertionError("Expected RateLimitException to be thrown")
    } catch (e: RateLimitException) {
      assertEquals(resetTime, e.resetTime)
    }

    // When: Try to search for profiles (should be blocked without calling API)
    val searchResult = dataSource.searchSoundCloudProfiles("Test Artist")

    // Then: Should be blocked by cached rate limit state from getTracksForProfile
    assertTrue(searchResult is ArtistProfileSearchResult.RateLimited)
    assertEquals(resetTime, searchResult.resetTime)
    assertEquals(15000, searchResult.maxRequests)
    assertEquals("PT24H", searchResult.timeWindow)
  }

  @Test
  fun `test getTracksForProfile success clears cached rate limit state`() = runTest {
    // Given: Fresh data source with successful getTracks call
    val mockClient = mock<SoundCloudApiClient>()
    val profileUrl = "https://soundcloud.com/testartist"
    everySuspend { mockClient.getTracks(profileUrl) } returns emptyList()
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When: Successful call to getTracks
    val tracks = dataSource.getTracksForProfile(profileUrl)

    // Then: Should succeed
    assertEquals(emptyList(), tracks)

    // And subsequent searchSoundCloudProfiles should work (not blocked by rate limit)
    everySuspend { mockClient.searchUsers(any()) } returns emptyList()
    val searchResult = dataSource.searchSoundCloudProfiles("Test Artist")
    // Should return Error (no results), not RateLimited
    assertTrue(searchResult is ArtistProfileSearchResult.Error)
    assertEquals("No SoundCloud profile found for \"Test Artist\"", searchResult.message)
  }
}
