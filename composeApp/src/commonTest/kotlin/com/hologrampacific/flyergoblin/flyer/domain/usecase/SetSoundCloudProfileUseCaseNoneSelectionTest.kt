package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.datasource.SoundCloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfile
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

/**
 * Tests for the "None" selection feature in SetSoundCloudProfileUseCase.
 *
 * These tests verify that setting a profile to null correctly clears the artist's SoundCloud
 * profile and tracks.
 */
class SetSoundCloudProfileUseCaseNoneSelectionTest : AppTest() {

  private val testProfile1 =
    SoundCloudProfileInfo(
      id = 1L,
      username = "artist1",
      profileUrl = "https://soundcloud.com/artist1",
    )
  private val testProfile2 =
    SoundCloudProfileInfo(
      id = 2L,
      username = "artist2",
      profileUrl = "https://soundcloud.com/artist2",
    )
  private val testTrack1 =
    SoundCloudTrack(id = 1L, title = "Track 1", url = "https://soundcloud.com/track1")
  private val testTrack2 =
    SoundCloudTrack(id = 2L, title = "Track 2", url = "https://soundcloud.com/track2")

  @Test
  fun `invoke with null soundCloudUserId clears profile and tracks`() = runTest {
    val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
    val soundCloudDataSource: SoundCloudDataSource = mock()
    val useCase =
      SetSoundCloudProfileUseCase(artistRepository, soundCloudDataSource, ProfileSearchCache())

    val artist =
      Artist(
        name = "TestArtist",
        soundCloudInfo =
          SoundCloudInfo(
            profile =
              SoundCloudProfile(
                id = testProfile1.id,
                username = testProfile1.username,
                profileUrl = testProfile1.profileUrl,
                tracks = listOf(testTrack1, testTrack2),
              ),
          ),
      )

    everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

    val result = useCase("TestArtist", null)

    assertIs<ResultWithRateLimit.Success>(result)
  }

  @Test
  fun `invoke returns Success when clearing already null profile`() = runTest {
    val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
    val soundCloudDataSource: SoundCloudDataSource = mock()
    val useCase =
      SetSoundCloudProfileUseCase(artistRepository, soundCloudDataSource, ProfileSearchCache())

    val artist =
      Artist(
        name = "TestArtist",
        soundCloudInfo =
          SoundCloudInfo(),
      )

    everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

    val result = useCase("TestArtist", null)

    assertIs<ResultWithRateLimit.Success>(result)
  }
}
