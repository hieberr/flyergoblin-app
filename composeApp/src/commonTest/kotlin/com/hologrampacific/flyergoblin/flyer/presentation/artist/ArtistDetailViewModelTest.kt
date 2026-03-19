package com.hologrampacific.flyergoblin.flyer.presentation.artist

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.data.remote.ApiRateLimitException
import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.datasource.SoundCloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.datasource.SoundcloudProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfile
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfileInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.flyer.domain.usecase.RefreshMixcloudProfileUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ResultWithRateLimitData
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
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
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
    stubArtistRepository: (ArtistRepository) -> Unit = {},
    stubSoundCloud: (SoundCloudDataSource) -> Unit = {},
    stubMixcloud: (MixcloudDataSource) -> Unit = {},
  ): ArtistDetailViewModel {
    val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
    every { artistRepository.observeArtistByName(any()) } returns flowOf(null)
    everySuspend { artistRepository.getArtistByName(any()) } returns null
    stubArtistRepository(artistRepository)

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
      refreshMixcloudProfileUseCase =
        RefreshMixcloudProfileUseCase(mixcloudDataSource, artistRepository),
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

  // region auto-refresh mixcloud

  private fun mixcloudProfileLastUpdated(lastUpdated: Instant?) =
    MixcloudProfile(
      key = "/testartist/",
      username = "testartist",
      profileUrl = "https://mixcloud.com/testartist",
      lastUpdated = lastUpdated,
    )

  private fun artistWithMixcloudProfile(lastUpdated: Instant?) =
    Artist(
      name = "Test Artist",
      mixcloudInfo = MixcloudInfo(profile = mixcloudProfileLastUpdated(lastUpdated)),
    )

  @Test
  fun `auto-refresh fires when mixcloud profile lastUpdated is null`() = runTest {
    val viewModel =
      makeViewModel(
        stubArtistRepository = { repo ->
          every { repo.observeArtistByName(any()) } returns
            flowOf(artistWithMixcloudProfile(lastUpdated = null))
        },
        stubMixcloud = { ds ->
          everySuspend { ds.getFullProfile(any()) } returns
            ResultWithRateLimitData.Error("refreshed")
        },
      )

    assertEquals("refreshed", viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `auto-refresh fires when mixcloud profile lastUpdated is older than 14 days`() = runTest {
    val viewModel =
      makeViewModel(
        stubArtistRepository = { repo ->
          every { repo.observeArtistByName(any()) } returns
            flowOf(artistWithMixcloudProfile(lastUpdated = Clock.System.now() - 15.days))
        },
        stubMixcloud = { ds ->
          everySuspend { ds.getFullProfile(any()) } returns
            ResultWithRateLimitData.Error("refreshed")
        },
      )

    assertEquals("refreshed", viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `auto-refresh does not fire when mixcloud profile lastUpdated is within 14 days`() = runTest {
    val viewModel =
      makeViewModel(
        stubArtistRepository = { repo ->
          every { repo.observeArtistByName(any()) } returns
            flowOf(artistWithMixcloudProfile(lastUpdated = Clock.System.now() - 13.days))
        },
        stubMixcloud = { ds ->
          everySuspend { ds.getFullProfile(any()) } returns
            ResultWithRateLimitData.Error("refreshed")
        },
      )

    assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `auto-refresh does not fire when artist has no mixcloud profile`() = runTest {
    val viewModel =
      makeViewModel(
        stubArtistRepository = { repo ->
          every { repo.observeArtistByName(any()) } returns flowOf(Artist(name = "Test Artist"))
        }
      )

    assertNull(viewModel.uiState.value.errorMessage)
    assertFalse(viewModel.uiState.value.isFetchingMixcloud)
  }

  @Test
  fun `auto-refresh fires at most once when repository emits stale artist multiple times`() =
    runTest {
      val staleArtist = artistWithMixcloudProfile(lastUpdated = null)

      val artistRepository: ArtistRepository = mock(MockMode.autoUnit)
      every { artistRepository.observeArtistByName(any()) } returns
        flow {
          emit(staleArtist)
          emit(staleArtist)
        }

      val mixcloudDataSource: MixcloudDataSource = mock(MockMode.autoUnit)
      everySuspend { mixcloudDataSource.getShowsForProfile(any()) } returns emptyList()
      everySuspend { mixcloudDataSource.getFullProfile(any()) } returns
        ResultWithRateLimitData.Error("test")

      val soundCloudDataSource: SoundCloudDataSource = mock(MockMode.autoUnit)
      everySuspend { soundCloudDataSource.getTracksForProfile(any()) } returns emptyList()

      val cache = ProfileSearchCache()
      ArtistDetailViewModel(
        artistName = "Test Artist",
        artistRepository = artistRepository,
        searchSoundCloudProfilesUseCase =
          SearchSoundCloudProfilesUseCase(soundCloudDataSource, cache),
        setSoundCloudProfileUseCase =
          SetSoundCloudProfileUseCase(artistRepository, soundCloudDataSource, cache),
        searchMixcloudProfilesUseCase = SearchMixcloudProfilesUseCase(mixcloudDataSource, cache),
        setMixcloudProfileUseCase =
          SetMixcloudProfileUseCase(artistRepository, mixcloudDataSource, cache),
        refreshMixcloudProfileUseCase =
          RefreshMixcloudProfileUseCase(mixcloudDataSource, artistRepository),
        profileSearchCache = cache,
      )

      verifySuspend(mode = VerifyMode.exactly(1)) { mixcloudDataSource.getFullProfile(any()) }
    }

  @Test
  fun `auto-refresh rate limited sets mixcloudRateLimitBlockedUntil`() = runTest {
    val viewModel =
      makeViewModel(
        stubArtistRepository = { repo ->
          every { repo.observeArtistByName(any()) } returns
            flowOf(artistWithMixcloudProfile(lastUpdated = null))
        },
        stubMixcloud = { ds ->
          everySuspend { ds.getFullProfile(any()) } returns
            ResultWithRateLimitData.RateLimited(blockedUntil)
        },
      )

    assertEquals(blockedUntil, viewModel.uiState.value.mixcloudRateLimitBlockedUntil)
  }

  @Test
  fun `auto-refresh error sets errorMessage`() = runTest {
    val viewModel =
      makeViewModel(
        stubArtistRepository = { repo ->
          every { repo.observeArtistByName(any()) } returns
            flowOf(artistWithMixcloudProfile(lastUpdated = null))
        },
        stubMixcloud = { ds ->
          everySuspend { ds.getFullProfile(any()) } returns
            ResultWithRateLimitData.Error("network failure")
        },
      )

    assertEquals("network failure", viewModel.uiState.value.errorMessage)
  }

  // endregion
}
