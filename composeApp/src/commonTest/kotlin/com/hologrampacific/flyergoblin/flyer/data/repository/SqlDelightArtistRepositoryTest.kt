package com.hologrampacific.flyergoblin.flyer.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.db.createAppDatabase
import com.hologrampacific.flyergoblin.db.createTestDriver
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfile
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileSearchResults
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudTrack
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class SqlDelightArtistRepositoryTest : AppTest() {

  private lateinit var driver: SqlDriver
  private lateinit var repository: SqlDelightArtistRepository

  @BeforeTest
  fun setup() {
    driver = createTestDriver()
    repository = SqlDelightArtistRepository(createAppDatabase(driver))
  }

  @AfterTest
  fun teardown() {
    driver.close()
  }

  private fun testArtist(
    name: String = "Test Artist",
    soundCloudProfile: SoundCloudProfileInfo? = null,
    soundCloudProfiles: List<SoundCloudProfileInfo> = emptyList(),
    soundCloudTracks: List<SoundCloudTrack> = emptyList(),
    lastSearched: Instant? = null,
  ): Artist {
    val hasInfo =
      soundCloudProfile != null ||
        soundCloudProfiles.isNotEmpty() ||
        soundCloudTracks.isNotEmpty() ||
        lastSearched != null
    return Artist(
      name = name,
      soundCloudInfo =
        if (hasInfo)
          SoundCloudInfo(
            profile =
              soundCloudProfile?.let {
                SoundCloudProfile(
                  username = it.username,
                  profileUrl = it.profileUrl,
                  followersCount = it.followersCount,
                  trackCount = it.trackCount,
                  city = it.city,
                  countryCode = it.countryCode,
                  tracks = soundCloudTracks,
                )
              },
            profileSearchResults =
              if (soundCloudProfiles.isNotEmpty() || lastSearched != null)
                SoundCloudProfileSearchResults(
                  results = soundCloudProfiles,
                  lastUpdated = lastSearched ?: Instant.fromEpochMilliseconds(0),
                )
              else null,
          )
        else null,
    )
  }

  @Test
  fun `getArtistByName returns null when artist does not exist`() = runTest {
    assertNull(repository.getArtistByName("Unknown Artist"))
  }

  @Test
  fun `saveArtist and getArtistByName round-trip preserves artist name`() = runTest {
    val artist = testArtist(name = "Test Artist")
    repository.saveArtist(artist)

    val saved = repository.getArtistByName("Test Artist")
    assertNotNull(saved)
    assertEquals("Test Artist", saved.name)
  }

  @Test
  fun `saveArtist and getArtistByName round-trip preserves all fields`() = runTest {
    val profile =
      SoundCloudProfileInfo(
        username = "testuser",
        profileUrl = "https://soundcloud.com/testuser",
        followersCount = 100,
      )
    val profiles =
      listOf(
        profile,
        SoundCloudProfileInfo(
          username = "testuser2",
          profileUrl = "https://soundcloud.com/testuser2",
        ),
      )
    val tracks =
      listOf(
        SoundCloudTrack(id = 1L, title = "Track 1", url = "https://soundcloud.com/track1"),
        SoundCloudTrack(id = 2L, title = "Track 2", url = "https://soundcloud.com/track2"),
      )
    val lastSearched = Instant.fromEpochMilliseconds(1706832000000)
    val artist =
      testArtist(
        name = "Full Artist",
        soundCloudProfile = profile,
        soundCloudProfiles = profiles,
        soundCloudTracks = tracks,
        lastSearched = lastSearched,
      )

    repository.saveArtist(artist)

    val saved = repository.getArtistByName("Full Artist")
    assertNotNull(saved)
    assertEquals(artist.name, saved.name)
    assertEquals(artist.soundCloudInfo?.profile, saved.soundCloudInfo?.profile)
    assertEquals(artist.soundCloudInfo?.profile?.tracks, saved.soundCloudInfo?.profile?.tracks)
    assertEquals(
      artist.soundCloudInfo?.profileSearchResults,
      saved.soundCloudInfo?.profileSearchResults,
    )
  }

  @Test
  fun `saveArtist with null optional fields round-trips correctly`() = runTest {
    val artist = testArtist(name = "Minimal Artist")
    repository.saveArtist(artist)

    val saved = repository.getArtistByName("Minimal Artist")
    assertNotNull(saved)
    assertNull(saved.soundCloudInfo)
  }

  @Test
  fun `saveArtist acts as upsert when name already exists`() = runTest {
    repository.saveArtist(testArtist(name = "Artist"))

    val profile =
      SoundCloudProfileInfo(username = "sc_user", profileUrl = "https://soundcloud.com/sc_user")
    repository.saveArtist(testArtist(name = "Artist", soundCloudProfile = profile))

    val saved = repository.getArtistByName("Artist")
    assertNotNull(saved)
    assertEquals(profile.username, saved.soundCloudInfo?.profile?.username)
    assertEquals(profile.profileUrl, saved.soundCloudInfo?.profile?.profileUrl)
  }

  @Test
  fun `updateArtist replaces the existing artist`() = runTest {
    repository.saveArtist(testArtist(name = "Artist"))

    val profile =
      SoundCloudProfileInfo(username = "sc_user", profileUrl = "https://soundcloud.com/sc_user")
    repository.updateArtist(testArtist(name = "Artist", soundCloudProfile = profile))

    val updated = repository.getArtistByName("Artist")
    assertNotNull(updated)
    assertEquals(profile.username, updated.soundCloudInfo?.profile?.username)
    assertEquals(profile.profileUrl, updated.soundCloudInfo?.profile?.profileUrl)
  }

  @Test
  fun `getArtistByName returns null after artist is replaced with different name`() = runTest {
    repository.saveArtist(testArtist(name = "Artist A"))
    repository.saveArtist(testArtist(name = "Artist B"))

    assertNotNull(repository.getArtistByName("Artist A"))
    assertNotNull(repository.getArtistByName("Artist B"))
    assertNull(repository.getArtistByName("Artist C"))
  }
}
