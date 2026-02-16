package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.domain.datasource.SoundCloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Tests for the "None" selection feature in SetSoundCloudProfileUseCase.
 *
 * These tests verify that setting a profile to null correctly clears the artist's SoundCloud
 * profile and tracks.
 */
class SetSoundCloudProfileUseCaseNoneSelectionTest {

  private val testProfile1 =
    SoundCloudProfileInfo(username = "artist1", profileUrl = "https://soundcloud.com/artist1")
  private val testProfile2 =
    SoundCloudProfileInfo(username = "artist2", profileUrl = "https://soundcloud.com/artist2")
  private val testTrack1 =
    SoundCloudTrack(id = 1L, title = "Track 1", url = "https://soundcloud.com/track1")
  private val testTrack2 =
    SoundCloudTrack(id = 2L, title = "Track 2", url = "https://soundcloud.com/track2")

  @Test
  fun `invoke with null profileUrl clears profile and tracks`() = runTest {
    val artistRepository: ArtistRepository = mock<ArtistRepository>(MockMode.autoUnit)
    val soundCloudDataSource: SoundCloudDataSource = mock<SoundCloudDataSource>()
    val useCase = SetSoundCloudProfileUseCase(artistRepository, soundCloudDataSource)

    val artist =
      Artist(
        name = "TestArtist",
        soundCloudProfile = testProfile1,
        soundCloudProfiles = listOf(testProfile1, testProfile2),
        soundCloudTracks = listOf(testTrack1, testTrack2),
      )

    everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

    val result = useCase("TestArtist", null)

    assertTrue(result is SetSoundCloudProfileResult.Success)
  }

  @Test
  fun `invoke returns Success when clearing already null profile`() = runTest {
    val artistRepository: ArtistRepository = mock<ArtistRepository>(MockMode.autoUnit)
    val soundCloudDataSource: SoundCloudDataSource = mock<SoundCloudDataSource>()
    val useCase = SetSoundCloudProfileUseCase(artistRepository, soundCloudDataSource)

    val artist =
      Artist(
        name = "TestArtist",
        soundCloudProfile = null,
        soundCloudProfiles = listOf(testProfile1, testProfile2),
      )

    everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

    val result = useCase("TestArtist", null)

    assertTrue(result is SetSoundCloudProfileResult.Success)
  }

  @Test
  fun `invoke returns Error when artist not found`() = runTest {
    val artistRepository: ArtistRepository = mock<ArtistRepository>()
    val soundCloudDataSource: SoundCloudDataSource = mock<SoundCloudDataSource>()
    val useCase = SetSoundCloudProfileUseCase(artistRepository, soundCloudDataSource)

    everySuspend { artistRepository.getArtistByName("TestArtist") } returns null

    val result = useCase("TestArtist", null)

    assertTrue(result is SetSoundCloudProfileResult.Error)
    assertTrue(
      (result as SetSoundCloudProfileResult.Error).message.contains("Could not find artist")
    )
  }
}
