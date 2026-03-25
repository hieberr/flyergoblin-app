package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.AppTest
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
import kotlinx.coroutines.test.runTest

class ConfirmMixcloudProfileUseCaseTest : AppTest() {

  private val testProfile =
    MixcloudProfile(
      key = "testartist",
      username = "testartist",
      profileUrl = "https://mixcloud.com/testartist",
    )

  @Test
  fun `invoke sets profileChosen to true when profile exists and profileChosen is false`() =
    runTest {
      val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
      val useCase = ConfirmMixcloudProfileUseCase(artistRepository)
      val artist =
        Artist(
          name = "TestArtist",
          mixcloudInfo = MixcloudInfo(profile = testProfile, profileChosen = false),
        )

      everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

      useCase("TestArtist")

      verifySuspend {
        artistRepository.upsertArtist(
          artist.copy(mixcloudInfo = artist.mixcloudInfo!!.copy(profileChosen = true))
        )
      }
    }

  @Test
  fun `invoke does not upsert when artist is not found`() = runTest {
    val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
    val useCase = ConfirmMixcloudProfileUseCase(artistRepository)

    everySuspend { artistRepository.getArtistByName("TestArtist") } returns null

    useCase("TestArtist")

    verifySuspend(mode = VerifyMode.not) { artistRepository.upsertArtist(any()) }
  }

  @Test
  fun `invoke does not upsert when mixcloudInfo is null`() = runTest {
    val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
    val useCase = ConfirmMixcloudProfileUseCase(artistRepository)
    val artist = Artist(name = "TestArtist", mixcloudInfo = null)

    everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

    useCase("TestArtist")

    verifySuspend(mode = VerifyMode.not) { artistRepository.upsertArtist(any()) }
  }

  @Test
  fun `invoke does not upsert when profile is null`() = runTest {
    val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
    val useCase = ConfirmMixcloudProfileUseCase(artistRepository)
    val artist = Artist(name = "TestArtist", mixcloudInfo = MixcloudInfo(profile = null))

    everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

    useCase("TestArtist")

    verifySuspend(mode = VerifyMode.not) { artistRepository.upsertArtist(any()) }
  }

  @Test
  fun `invoke does not upsert when profileChosen is already true`() = runTest {
    val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
    val useCase = ConfirmMixcloudProfileUseCase(artistRepository)
    val artist =
      Artist(
        name = "TestArtist",
        mixcloudInfo = MixcloudInfo(profile = testProfile, profileChosen = true),
      )

    everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

    useCase("TestArtist")

    verifySuspend(mode = VerifyMode.not) { artistRepository.upsertArtist(any()) }
  }
}
