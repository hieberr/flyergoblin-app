package com.hologrampacific.learnkmp.flyer.data.datasource

import com.hologrampacific.learnkmp.flyer.data.remote.SoundCloudApiClient
import com.hologrampacific.learnkmp.flyer.data.remote.SoundCloudUser
import com.hologrampacific.learnkmp.flyer.domain.datasource.ArtistProfileResult
import com.hologrampacific.learnkmp.flyer.domain.model.SoundCloudTrack
import io.ktor.client.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Fake implementation of SoundCloudApiClient for testing purposes. Allows control over search
 * results and error conditions.
 */
class FakeSoundCloudApiClient : SoundCloudApiClient(httpClient = HttpClient()) {
  var searchUsersResult: List<SoundCloudUser> = emptyList()
  var shouldThrowException = false
  var exceptionMessage = "Test exception"

  override suspend fun searchUsers(query: String): List<SoundCloudUser> {
    if (shouldThrowException) {
      throw Exception(exceptionMessage)
    }
    return searchUsersResult
  }

  override suspend fun fetchPopularTracks(profileUrl: String): List<SoundCloudTrack> {
    return emptyList()
  }
}

class SoundCloudDataSourceImplTest {

  @Test
  fun `test findSoundCloudProfile empty search results`() {
    // Given
    val fakeClient = FakeSoundCloudApiClient()
    fakeClient.searchUsersResult = emptyList()
    val dataSource = SoundCloudDataSourceImpl(fakeClient)

    // When
    val result = runTest { dataSource.findSoundCloudProfile("NonExistentArtist") }

    // Then
    assertTrue(result is ArtistProfileResult.Error)
    assertEquals(
      "No SoundCloud profile found for \"NonExistentArtist\"",
      (result as ArtistProfileResult.Error).message,
    )
  }

  @Test
  fun `test findSoundCloudProfile blank artist name`() {
    // Given
    val fakeClient = FakeSoundCloudApiClient()
    val dataSource = SoundCloudDataSourceImpl(fakeClient)

    // When
    val result = runTest { dataSource.findSoundCloudProfile("   ") }

    // Then
    assertTrue(result is ArtistProfileResult.Error)
    assertEquals("Artist name cannot be empty", (result as ArtistProfileResult.Error).message)
  }

  @Test
  fun `test findSoundCloudProfile empty artist name`() {
    // Given
    val fakeClient = FakeSoundCloudApiClient()
    val dataSource = SoundCloudDataSourceImpl(fakeClient)

    // When
    val result = runTest { dataSource.findSoundCloudProfile("") }

    // Then
    assertTrue(result is ArtistProfileResult.Error)
    assertEquals("Artist name cannot be empty", (result as ArtistProfileResult.Error).message)
  }

  @Test
  fun `test findSoundCloudProfile artist name too long`() {
    // Given
    val fakeClient = FakeSoundCloudApiClient()
    val dataSource = SoundCloudDataSourceImpl(fakeClient)
    val longName = "a".repeat(201)

    // When
    val result = runTest { dataSource.findSoundCloudProfile(longName) }

    // Then
    assertTrue(result is ArtistProfileResult.Error)
    assertEquals("Artist name is too long", (result as ArtistProfileResult.Error).message)
  }

  @Test
  fun `test findSoundCloudProfile success with permalink`() {
    // Given
    val fakeClient = FakeSoundCloudApiClient()
    val permalinkUrl = "https://soundcloud.com/testartist"
    fakeClient.searchUsersResult =
      listOf(
        SoundCloudUser(
          id = 123,
          username = "testartist",
          permalink = "testartist",
          permalinkUrl = permalinkUrl,
          followersCount = 1000,
        )
      )
    val dataSource = SoundCloudDataSourceImpl(fakeClient)

    // When
    val result = runTest { dataSource.findSoundCloudProfile("Test Artist") }

    // Then
    assertTrue(result is ArtistProfileResult.Success)
    assertEquals(permalinkUrl, (result as ArtistProfileResult.Success).soundCloudProfile)
  }

  @Test
  fun `test findSoundCloudProfile url construction fallback`() {
    // Given: User without permalinkUrl (should fall back to constructing URL from permalink)
    val fakeClient = FakeSoundCloudApiClient()
    fakeClient.searchUsersResult =
      listOf(
        SoundCloudUser(
          id = 456,
          username = "artist-name",
          permalink = "artist-name",
          permalinkUrl = null,
          followersCount = 500,
        )
      )
    val dataSource = SoundCloudDataSourceImpl(fakeClient)

    // When
    val result = runTest { dataSource.findSoundCloudProfile("Artist Name") }

    // Then
    assertTrue(result is ArtistProfileResult.Success)
    assertEquals(
      "https://soundcloud.com/artist-name",
      (result as ArtistProfileResult.Success).soundCloudProfile,
    )
  }

  @Test
  fun `test findSoundCloudProfile url construction with special characters`() {
    // Given: permalink with special characters that need URL encoding
    val fakeClient = FakeSoundCloudApiClient()
    fakeClient.searchUsersResult =
      listOf(
        SoundCloudUser(
          id = 789,
          username = "dj name",
          permalink = "dj name",
          permalinkUrl = null,
          followersCount = 300,
        )
      )
    val dataSource = SoundCloudDataSourceImpl(fakeClient)

    // When
    val result = runTest { dataSource.findSoundCloudProfile("DJ Name") }

    // Then
    assertTrue(result is ArtistProfileResult.Success)
    val url = (result as ArtistProfileResult.Success).soundCloudProfile
    // Should have URL-encoded space as %20
    assertEquals("https://soundcloud.com/dj%20name", url)
  }

  @Test
  fun `test findSoundCloudProfile sorts by followers descending`() {
    // Given: Multiple users with different follower counts
    val fakeClient = FakeSoundCloudApiClient()
    fakeClient.searchUsersResult =
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
    val dataSource = SoundCloudDataSourceImpl(fakeClient)

    // When
    val result = runTest { dataSource.findSoundCloudProfile("Artist") }

    // Then: Should return the artist with most followers (5000)
    assertTrue(result is ArtistProfileResult.Success)
    assertEquals(
      "https://soundcloud.com/artist2",
      (result as ArtistProfileResult.Success).soundCloudProfile,
    )
  }

  @Test
  fun `test findSoundCloudProfile null follower counts treated as zero`() {
    // Given: Users with null follower counts mixed with valid counts
    val fakeClient = FakeSoundCloudApiClient()
    fakeClient.searchUsersResult =
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
    val dataSource = SoundCloudDataSourceImpl(fakeClient)

    // When
    val result = runTest { dataSource.findSoundCloudProfile("Artist") }

    // Then: Should return the artist with actual follower count, not the null one
    assertTrue(result is ArtistProfileResult.Success)
    assertEquals(
      "https://soundcloud.com/artist-with-followers",
      (result as ArtistProfileResult.Success).soundCloudProfile,
    )
  }

  @Test
  fun `test findSoundCloudProfile all null follower counts`() {
    // Given: All users have null follower counts
    val fakeClient = FakeSoundCloudApiClient()
    fakeClient.searchUsersResult =
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
    val dataSource = SoundCloudDataSourceImpl(fakeClient)

    // When
    val result = runTest { dataSource.findSoundCloudProfile("Artist") }

    // Then: Should still return a result (the first one after sorting)
    assertTrue(result is ArtistProfileResult.Success)
    assertTrue((result as ArtistProfileResult.Success).soundCloudProfile.contains("soundcloud.com"))
  }

  @Test
  fun `test findSoundCloudProfile trims whitespace`() {
    // Given
    val fakeClient = FakeSoundCloudApiClient()
    fakeClient.searchUsersResult =
      listOf(
        SoundCloudUser(
          id = 1,
          username = "artist",
          permalink = "artist",
          permalinkUrl = "https://soundcloud.com/artist",
          followersCount = 100,
        )
      )
    val dataSource = SoundCloudDataSourceImpl(fakeClient)

    // When: Search with leading/trailing whitespace
    val result = runTest { dataSource.findSoundCloudProfile("  Test Artist  ") }

    // Then: Should still find results (whitespace trimmed)
    assertTrue(result is ArtistProfileResult.Success)
  }

  /**
   * Helper function to run suspend functions in tests. In a real test environment, you'd use
   * kotlinx.coroutines.test.runTest, but for simplicity in commonTest without additional
   * dependencies, we use a simple blocking approach.
   */
  private fun <T> runTest(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
  }
}
