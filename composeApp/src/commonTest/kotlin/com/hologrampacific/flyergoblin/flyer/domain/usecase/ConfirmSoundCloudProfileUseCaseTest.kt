package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.AppTest
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
import kotlinx.coroutines.test.runTest

class ConfirmSoundCloudProfileUseCaseTest : AppTest() {

  private val testProfile =
    SoundCloudProfile(
      id = 1L,
      username = "testartist",
      profileUrl = "https://soundcloud.com/testartist",
    )

  @Test
  fun `invoke sets profileChosen to true when profile exists and profileChosen is false`() =
    runTest {
      val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
      val useCase = ConfirmSoundCloudProfileUseCase(artistRepository)
      val artist =
        Artist(
          name = "TestArtist",
          soundCloudInfo = SoundCloudInfo(profile = testProfile, profileChosen = false),
        )

      everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

      useCase("TestArtist")

      verifySuspend {
        artistRepository.upsertArtist(
          artist.copy(soundCloudInfo = artist.soundCloudInfo!!.copy(profileChosen = true))
        )
      }
    }

  @Test
  fun `invoke does not upsert when artist is not found`() = runTest {
    val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
    val useCase = ConfirmSoundCloudProfileUseCase(artistRepository)

    everySuspend { artistRepository.getArtistByName("TestArtist") } returns null

    useCase("TestArtist")

    verifySuspend(mode = VerifyMode.not) { artistRepository.upsertArtist(any()) }
  }

  @Test
  fun `invoke does not upsert when soundCloudInfo is null`() = runTest {
    val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
    val useCase = ConfirmSoundCloudProfileUseCase(artistRepository)
    val artist = Artist(name = "TestArtist", soundCloudInfo = null)

    everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

    useCase("TestArtist")

    verifySuspend(mode = VerifyMode.not) { artistRepository.upsertArtist(any()) }
  }

  @Test
  fun `invoke does not upsert when profile is null`() = runTest {
    val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
    val useCase = ConfirmSoundCloudProfileUseCase(artistRepository)
    val artist = Artist(name = "TestArtist", soundCloudInfo = SoundCloudInfo(profile = null))

    everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

    useCase("TestArtist")

    verifySuspend(mode = VerifyMode.not) { artistRepository.upsertArtist(any()) }
  }

  @Test
  fun `invoke does not upsert when profileChosen is already true`() = runTest {
    val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
    val useCase = ConfirmSoundCloudProfileUseCase(artistRepository)
    val artist =
      Artist(
        name = "TestArtist",
        soundCloudInfo = SoundCloudInfo(profile = testProfile, profileChosen = true),
      )

    everySuspend { artistRepository.getArtistByName("TestArtist") } returns artist

    useCase("TestArtist")

    verifySuspend(mode = VerifyMode.not) { artistRepository.upsertArtist(any()) }
  }
}
