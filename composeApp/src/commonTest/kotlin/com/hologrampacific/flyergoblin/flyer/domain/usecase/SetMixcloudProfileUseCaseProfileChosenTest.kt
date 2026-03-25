package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfile
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
 * Tests for the `profileChosen` parameter behaviour of [SetMixcloudProfileUseCase], covering
 * the same-profile upgrade path and idempotency.
 */
class SetMixcloudProfileUseCaseProfileChosenTest : AppTest() {

  private val existingProfile =
    MixcloudProfile(
      key = "artist1",
      username = "artist1",
      profileUrl = "https://mixcloud.com/artist1",
    )

  @Test
  fun `invoke same profile with profileChosen true upgrades profileChosen without re-fetching shows`() =
    runTest {
      val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
      val mixcloudDataSource: MixcloudDataSource = mock(MockMode.autoUnit)
      val artist =
        Artist(
          name = "TestArtist",
          mixcloudInfo = MixcloudInfo(profile = existingProfile, profileChosen = false),
        )
      everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

      val result =
        SetMixcloudProfileUseCase(artistRepository, mixcloudDataSource, ProfileSearchCache())
          .invoke("TestArtist", existingProfile.key, profileChosen = true)

      assertIs<ResultWithRateLimit.Success>(result)
      verifySuspend {
        artistRepository.upsertArtist(
          artist.copy(mixcloudInfo = artist.mixcloudInfo!!.copy(profileChosen = true))
        )
      }
      // Shows must NOT be re-fetched when upgrading the same profile
      verifySuspend(mode = VerifyMode.not) { mixcloudDataSource.getShowsForProfile(any()) }
    }

  @Test
  fun `invoke same profile with same profileChosen is a no-op`() = runTest {
    val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
    val mixcloudDataSource: MixcloudDataSource = mock(MockMode.autoUnit)
    val artist =
      Artist(
        name = "TestArtist",
        mixcloudInfo = MixcloudInfo(profile = existingProfile, profileChosen = true),
      )
    everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

    val result =
      SetMixcloudProfileUseCase(artistRepository, mixcloudDataSource, ProfileSearchCache())
        .invoke("TestArtist", existingProfile.key, profileChosen = true)

    assertIs<ResultWithRateLimit.Success>(result)
    verifySuspend(mode = VerifyMode.not) { artistRepository.upsertArtist(any()) }
    verifySuspend(mode = VerifyMode.not) { mixcloudDataSource.getShowsForProfile(any()) }
  }
}
