package com.hologrampacific.learnkmp.flyer.data.datasource

import com.hologrampacific.learnkmp.flyer.data.remote.SoundCloudApiClient
import com.hologrampacific.learnkmp.flyer.data.remote.SoundCloudUser
import com.hologrampacific.learnkmp.flyer.domain.datasource.ArtistProfileResult
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SoundCloudDataSourceImplTest {

  @Test
  fun `test findSoundCloudProfile empty search results`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    everySuspend { mockClient.searchUsers("NonExistentArtist") } returns emptyList()
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.findSoundCloudProfile("NonExistentArtist")

    // Then
    assertTrue(result is ArtistProfileResult.Error)
    assertEquals("No SoundCloud profile found for \"NonExistentArtist\"", (result).message)
    verifySuspend { mockClient.searchUsers("NonExistentArtist") }
  }

  @Test
  fun `test findSoundCloudProfile blank artist name`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.findSoundCloudProfile("   ")

    // Then
    assertTrue(result is ArtistProfileResult.Error)
    assertEquals("Artist name cannot be empty", (result as ArtistProfileResult.Error).message)
  }

  @Test
  fun `test findSoundCloudProfile empty artist name`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.findSoundCloudProfile("")

    // Then
    assertTrue(result is ArtistProfileResult.Error)
    assertEquals("Artist name cannot be empty", (result as ArtistProfileResult.Error).message)
  }

  @Test
  fun `test findSoundCloudProfile artist name too long`() = runTest {
    // Given
    val mockClient = mock<SoundCloudApiClient>()
    val dataSource = SoundCloudDataSourceImpl(mockClient)
    val longName = "a".repeat(201)

    // When
    val result = dataSource.findSoundCloudProfile(longName)

    // Then
    assertTrue(result is ArtistProfileResult.Error)
    assertEquals("Artist name is too long", (result as ArtistProfileResult.Error).message)
  }

  @Test
  fun `test findSoundCloudProfile success with permalink`() = runTest {
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
        )
      )
    everySuspend { mockClient.searchUsers("Test Artist") } returns searchResults
    val dataSource = SoundCloudDataSourceImpl(mockClient)

    // When
    val result = dataSource.findSoundCloudProfile("Test Artist")

    // Then
    assertTrue(result is ArtistProfileResult.Success)
    assertEquals(permalinkUrl, (result).soundCloudProfile)
    verifySuspend { mockClient.searchUsers("Test Artist") }
  }

  @Test
  fun `test findSoundCloudProfile url construction fallback`() = runTest {
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
    val result = dataSource.findSoundCloudProfile("Artist Name")

    // Then
    assertTrue(result is ArtistProfileResult.Success)
    assertEquals("https://soundcloud.com/artist-name", (result).soundCloudProfile)
    verifySuspend { mockClient.searchUsers("Artist Name") }
  }

  @Test
  fun `test findSoundCloudProfile url construction with special characters`() = runTest {
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
    val result = dataSource.findSoundCloudProfile("DJ Name")

    // Then
    assertTrue(result is ArtistProfileResult.Success)
    val url = (result).soundCloudProfile
    // Should have URL-encoded space as %20
    assertEquals("https://soundcloud.com/dj%20name", url)
    verifySuspend { mockClient.searchUsers("DJ Name") }
  }

  @Test
  fun `test findSoundCloudProfile sorts by followers descending`() = runTest {
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
    val result = dataSource.findSoundCloudProfile("Artist")

    // Then: Should return the artist with most followers (5000)
    assertTrue(result is ArtistProfileResult.Success)
    assertEquals("https://soundcloud.com/artist2", (result).soundCloudProfile)
    verifySuspend { mockClient.searchUsers("Artist") }
  }

  @Test
  fun `test findSoundCloudProfile null follower counts treated as zero`() = runTest {
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
    val result = dataSource.findSoundCloudProfile("Artist")

    // Then: Should return the artist with actual follower count, not the null one
    assertTrue(result is ArtistProfileResult.Success)
    assertEquals("https://soundcloud.com/artist-with-followers", (result).soundCloudProfile)
    verifySuspend { mockClient.searchUsers("Artist") }
  }

  @Test
  fun `test findSoundCloudProfile all null follower counts`() = runTest {
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
    val result = dataSource.findSoundCloudProfile("Artist")

    // Then: Should still return a result (the first one after sorting)
    assertTrue(result is ArtistProfileResult.Success)
    assertTrue((result).soundCloudProfile.contains("soundcloud.com"))
    verifySuspend { mockClient.searchUsers("Artist") }
  }

  @Test
  fun `test findSoundCloudProfile trims whitespace`() = runTest {
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
    val result = dataSource.findSoundCloudProfile("  Test Artist  ")

    // Then: Should still find results (whitespace trimmed)
    assertTrue(result is ArtistProfileResult.Success)
    verifySuspend { mockClient.searchUsers("Test Artist") }
  }
}
