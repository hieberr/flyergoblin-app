package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.AppTest
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
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class RefreshMixcloudProfileUseCaseTest : AppTest() {

  private val mixcloudDataSource: MixcloudDataSource = mock()
  private val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
  private val useCase = RefreshMixcloudProfileUseCase(mixcloudDataSource, artistRepository)

  private val profile =
    MixcloudProfile(
      key = "/testartist/",
      username = "testartist",
      profileUrl = "https://mixcloud.com/testartist",
    )

  private val artistWithProfile =
    Artist(name = "Test Artist", mixcloudInfo = MixcloudInfo(profile = profile))

  private val artistWithoutProfile = Artist(name = "Test Artist")

  @Test
  fun `test invoke with no mixcloud profile returns Error without calling datasource`() = runTest {
    val result = useCase(artistWithoutProfile)

    assertIs<ResultWithRateLimitData.Error>(result)
    verifySuspend(mode = VerifyMode.exactly(0)) { mixcloudDataSource.getFullProfile(any()) }
  }

  @Test
  fun `test invoke on success calls upsertArtist and returns Success`() = runTest {
    everySuspend { mixcloudDataSource.getFullProfile(profile.key) } returns
      ResultWithRateLimitData.Success(profile)

    val result = useCase(artistWithProfile)

    assertIs<ResultWithRateLimitData.Success<Unit>>(result)
    verifySuspend { artistRepository.upsertArtist(any()) }
  }

  @Test
  fun `test invoke propagates RateLimited and does not call upsertArtist`() = runTest {
    val blockedUntil = Instant.parse("2026-06-01T00:00:00Z")
    everySuspend { mixcloudDataSource.getFullProfile(profile.key) } returns
      ResultWithRateLimitData.RateLimited(blockedUntil)

    val result = useCase(artistWithProfile)

    assertEquals(ResultWithRateLimitData.RateLimited(blockedUntil), result)
    verifySuspend(mode = VerifyMode.exactly(0)) { artistRepository.upsertArtist(any()) }
  }

  @Test
  fun `test invoke propagates Error and does not call upsertArtist`() = runTest {
    everySuspend { mixcloudDataSource.getFullProfile(profile.key) } returns
      ResultWithRateLimitData.Error("network error")

    val result = useCase(artistWithProfile)

    assertEquals(ResultWithRateLimitData.Error("network error"), result)
    verifySuspend(mode = VerifyMode.exactly(0)) { artistRepository.upsertArtist(any()) }
  }
}
