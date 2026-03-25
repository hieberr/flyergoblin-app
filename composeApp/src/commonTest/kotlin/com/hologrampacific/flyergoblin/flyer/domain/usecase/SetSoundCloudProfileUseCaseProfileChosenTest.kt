package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.datasource.SoundCloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfile
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

/**
 * Tests for the `profileChosen` parameter behaviour of [SetSoundCloudProfileUseCase], covering
 * the same-profile upgrade path and idempotency.
 */
class SetSoundCloudProfileUseCaseProfileChosenTest : AppTest() {

  private val existingProfile =
    SoundCloudProfile(
      id = 1L,
      username = "artist1",
      profileUrl = "https://soundcloud.com/artist1",
    )

  @Test
  fun `invoke same profile with profileChosen true upgrades profileChosen without re-fetching tracks`() =
    runTest {
      val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
      val soundCloudDataSource: SoundCloudDataSource = mock(MockMode.autoUnit)
      val artist =
        Artist(
          name = "TestArtist",
          soundCloudInfo = SoundCloudInfo(profile = existingProfile, profileChosen = false),
        )
      everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

      val result =
        SetSoundCloudProfileUseCase(artistRepository, soundCloudDataSource, ProfileSearchCache())
          .invoke("TestArtist", existingProfile.id, profileChosen = true)

      assertIs<ResultWithRateLimit.Success>(result)
      verifySuspend {
        artistRepository.upsertArtist(
          artist.copy(soundCloudInfo = artist.soundCloudInfo!!.copy(profileChosen = true))
        )
      }
      // Tracks must NOT be re-fetched when upgrading the same profile
      verifySuspend(mode = VerifyMode.not) { soundCloudDataSource.getTracksForProfile(any()) }
    }

  @Test
  fun `invoke same profile with same profileChosen is a no-op`() = runTest {
    val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
    val soundCloudDataSource: SoundCloudDataSource = mock(MockMode.autoUnit)
    val artist =
      Artist(
        name = "TestArtist",
        soundCloudInfo = SoundCloudInfo(profile = existingProfile, profileChosen = true),
      )
    everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

    val result =
      SetSoundCloudProfileUseCase(artistRepository, soundCloudDataSource, ProfileSearchCache())
        .invoke("TestArtist", existingProfile.id, profileChosen = true)

    assertIs<ResultWithRateLimit.Success>(result)
    verifySuspend(mode = VerifyMode.not) { artistRepository.upsertArtist(any()) }
    verifySuspend(mode = VerifyMode.not) { soundCloudDataSource.getTracksForProfile(any()) }
  }
}
