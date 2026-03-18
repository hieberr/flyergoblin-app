package com.hologrampacific.flyergoblin.flyer.presentation.artist

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.data.remote.ApiRateLimitException
import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.datasource.SoundCloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.datasource.SoundcloudProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfileInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SearchMixcloudProfilesUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SearchSoundCloudProfilesUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SetMixcloudProfileUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SetSoundCloudProfileUseCase
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ArtistDetailViewModelTest : AppTest() {

  private val testDispatcher = UnconfinedTestDispatcher()

  @BeforeTest
  fun setupDispatcher() {
    Dispatchers.setMain(testDispatcher)
  }

  @AfterTest
  fun teardownDispatcher() {
    Dispatchers.resetMain()
  }

  private val blockedUntil = Instant.fromEpochMilliseconds(9_999_999_999_000L)

  private val soundCloudProfile =
    SoundCloudProfileInfo(
      id = 1L,
      username = "testartist",
      profileUrl = "https://soundcloud.com/testartist",
    )

  private val mixcloudProfile =
    MixcloudProfileInfo(
      key = "/testartist/",
      username = "testartist",
      profileUrl = "https://mixcloud.com/testartist",
    )

  private fun makeViewModel(
    artistName: String = "Test Artist",
    stubSoundCloud: (SoundCloudDataSource) -> Unit = {},
    stubMixcloud: (MixcloudDataSource) -> Unit = {},
  ): ArtistDetailViewModel {
    val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
    every { artistRepository.observeArtistByName(any()) } returns flowOf(null)
    everySuspend { artistRepository.getArtistByName(any()) } returns null

    val soundCloudDataSource: SoundCloudDataSource = mock(MockMode.autoUnit)
    everySuspend { soundCloudDataSource.getTracksForProfile(any()) } returns emptyList()
    stubSoundCloud(soundCloudDataSource)

    val mixcloudDataSource: MixcloudDataSource = mock(MockMode.autoUnit)
    everySuspend { mixcloudDataSource.getShowsForProfile(any()) } returns emptyList()
    stubMixcloud(mixcloudDataSource)

    val cache = ProfileSearchCache()

    return ArtistDetailViewModel(
      artistName = artistName,
      artistRepository = artistRepository,
      searchSoundCloudProfilesUseCase =
        SearchSoundCloudProfilesUseCase(soundCloudDataSource, cache),
      setSoundCloudProfileUseCase =
        SetSoundCloudProfileUseCase(artistRepository, soundCloudDataSource, cache),
      searchMixcloudProfilesUseCase = SearchMixcloudProfilesUseCase(mixcloudDataSource, cache),
      setMixcloudProfileUseCase =
        SetMixcloudProfileUseCase(artistRepository, mixcloudDataSource, cache),
      profileSearchCache = cache,
    )
  }

  // region fetchSoundCloudInfo rate limiting

  @Test
  fun `fetchSoundCloudInfo rate limited during search sets soundCloudRateLimitBlockedUntil`() =
    runTest {
      val viewModel =
        makeViewModel(
          stubSoundCloud = { ds ->
            everySuspend { ds.searchSoundCloudProfiles(any()) } returns
              SoundcloudProfileSearchResult.RateLimited(blockedUntil)
          }
        )

      viewModel.fetchSoundCloudInfo()

      assertEquals(blockedUntil, viewModel.uiState.value.soundCloudRateLimitBlockedUntil)
    }

  @Test
  fun `fetchSoundCloudInfo rate limited during search does not set mixcloudRateLimitBlockedUntil`() =
    runTest {
      val viewModel =
        makeViewModel(
          stubSoundCloud = { ds ->
            everySuspend { ds.searchSoundCloudProfiles(any()) } returns
              SoundcloudProfileSearchResult.RateLimited(blockedUntil)
          }
        )

      viewModel.fetchSoundCloudInfo()

      assertNull(viewModel.uiState.value.mixcloudRateLimitBlockedUntil)
    }

  @Test
  fun `fetchSoundCloudInfo rate limited during search does not set errorMessage`() = runTest {
    val viewModel =
      makeViewModel(
        stubSoundCloud = { ds ->
          everySuspend { ds.searchSoundCloudProfiles(any()) } returns
            SoundcloudProfileSearchResult.RateLimited(blockedUntil)
        }
      )

    viewModel.fetchSoundCloudInfo()

    assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `fetchSoundCloudInfo rate limited during set profile sets soundCloudRateLimitBlockedUntil`() =
    runTest {
      val viewModel =
        makeViewModel(
          stubSoundCloud = { ds ->
            everySuspend { ds.searchSoundCloudProfiles(any()) } returns
              SoundcloudProfileSearchResult.Success(listOf(soundCloudProfile))
            everySuspend { ds.getTracksForProfile(any()) } throws
              ApiRateLimitException(blockedUntil, "Rate limited")
          }
        )

      viewModel.fetchSoundCloudInfo()

      assertEquals(blockedUntil, viewModel.uiState.value.soundCloudRateLimitBlockedUntil)
    }

  @Test
  fun `fetchSoundCloudInfo rate limited does not clear pre-existing mixcloudRateLimitBlockedUntil`() =
    runTest {
      val viewModel =
        makeViewModel(
          stubSoundCloud = { ds ->
            everySuspend { ds.searchSoundCloudProfiles(any()) } returns
              SoundcloudProfileSearchResult.RateLimited(blockedUntil)
          }
        )
      viewModel.testMixcloudRateLimit()

      viewModel.fetchSoundCloudInfo()

      assertFalse(viewModel.uiState.value.mixcloudRateLimitBlockedUntil == null)
    }

  // endregion

  // region fetchMixcloudInfo rate limiting

  @Test
  fun `fetchMixcloudInfo rate limited during search sets mixcloudRateLimitBlockedUntil`() =
    runTest {
      val viewModel =
        makeViewModel(
          stubMixcloud = { ds ->
            everySuspend { ds.searchMixcloudProfiles(any()) } returns
              MixcloudProfileSearchResult.RateLimited(blockedUntil)
          }
        )

      viewModel.fetchMixcloudInfo()

      assertEquals(blockedUntil, viewModel.uiState.value.mixcloudRateLimitBlockedUntil)
    }

  @Test
  fun `fetchMixcloudInfo rate limited during search does not set soundCloudRateLimitBlockedUntil`() =
    runTest {
      val viewModel =
        makeViewModel(
          stubMixcloud = { ds ->
            everySuspend { ds.searchMixcloudProfiles(any()) } returns
              MixcloudProfileSearchResult.RateLimited(blockedUntil)
          }
        )

      viewModel.fetchMixcloudInfo()

      assertNull(viewModel.uiState.value.soundCloudRateLimitBlockedUntil)
    }

  @Test
  fun `fetchMixcloudInfo rate limited during search does not set errorMessage`() = runTest {
    val viewModel =
      makeViewModel(
        stubMixcloud = { ds ->
          everySuspend { ds.searchMixcloudProfiles(any()) } returns
            MixcloudProfileSearchResult.RateLimited(blockedUntil)
        }
      )

    viewModel.fetchMixcloudInfo()

    assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `fetchMixcloudInfo rate limited during set profile sets mixcloudRateLimitBlockedUntil`() =
    runTest {
      val viewModel =
        makeViewModel(
          stubMixcloud = { ds ->
            everySuspend { ds.searchMixcloudProfiles(any()) } returns
              MixcloudProfileSearchResult.Success(listOf(mixcloudProfile))
            everySuspend { ds.getShowsForProfile(any()) } throws
              ApiRateLimitException(blockedUntil, "Rate limited")
          }
        )

      viewModel.fetchMixcloudInfo()

      assertEquals(blockedUntil, viewModel.uiState.value.mixcloudRateLimitBlockedUntil)
    }

  @Test
  fun `fetchMixcloudInfo rate limited does not clear pre-existing soundCloudRateLimitBlockedUntil`() =
    runTest {
      val viewModel =
        makeViewModel(
          stubMixcloud = { ds ->
            everySuspend { ds.searchMixcloudProfiles(any()) } returns
              MixcloudProfileSearchResult.RateLimited(blockedUntil)
          }
        )
      viewModel.testSoundCloudRateLimit()

      viewModel.fetchMixcloudInfo()

      assertFalse(viewModel.uiState.value.soundCloudRateLimitBlockedUntil == null)
    }

  // endregion
}
