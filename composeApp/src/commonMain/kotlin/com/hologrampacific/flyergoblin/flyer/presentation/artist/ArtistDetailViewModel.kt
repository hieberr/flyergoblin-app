package com.hologrampacific.flyergoblin.flyer.presentation.artist

import androidx.lifecycle.viewModelScope
import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.flyer.domain.usecase.RefreshMixcloudProfileUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ResultWithRateLimit
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ResultWithRateLimitData
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SearchMixcloudProfilesUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SearchSoundCloudProfilesUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SetMixcloudProfileUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SetSoundCloudProfileUseCase
import com.hologrampacific.flyergoblin.presentation.ErrorMessageViewModel
import com.hologrampacific.flyergoblin.util.AppLogger
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.launch

/**
 * ViewModel for the Artist Detail screen. Manages loading artist data from the repository and
 * fetching SoundCloud info via AI.
 *
 * @param artistName The name of the artist to display
 * @param artistRepository Repository for loading and saving artist data
 * @param searchSoundCloudProfilesUseCase Use case for searching SoundCloud profiles
 * @param setSoundCloudProfileUseCase Use case for setting the active SoundCloud profile
 * @param searchMixcloudProfilesUseCase Use case for searching Mixcloud profiles
 * @param setMixcloudProfileUseCase Use case for setting the active Mixcloud profile
 * @param profileSearchCache The cache of profile search results
 */
class ArtistDetailViewModel(
  private val artistName: String,
  private val artistRepository: ArtistRepository,
  private val searchSoundCloudProfilesUseCase: SearchSoundCloudProfilesUseCase,
  private val setSoundCloudProfileUseCase: SetSoundCloudProfileUseCase,
  private val searchMixcloudProfilesUseCase: SearchMixcloudProfilesUseCase,
  private val setMixcloudProfileUseCase: SetMixcloudProfileUseCase,
  private val refreshMixcloudProfileUseCase: RefreshMixcloudProfileUseCase,
  private val profileSearchCache: ProfileSearchCache,
) : ErrorMessageViewModel<ArtistDetailUiState>(ArtistDetailUiState()) {

  /** Guards against triggering an auto-refresh more than once per ViewModel instance. */
  private var hasAutoRefreshedMixcloud = false

  init {
    observeArtist()
  }

  /**
   * Observe artist data from repository. On first emission, automatically triggers
   * [refreshMixcloudInfo] if the Mixcloud profile's `lastUpdated` is older than 14 days or has
   * never been set.
   */
  private fun observeArtist() {
    viewModelScope.launch {
      artistRepository.observeArtistByName(artistName).collect { artist ->
        _uiState.value =
          _uiState.value.copy(artist = artist ?: Artist(name = artistName), isLoading = false)
        if (!hasAutoRefreshedMixcloud) {
          hasAutoRefreshedMixcloud = true
          val resolvedArtist = artist ?: Artist(name = artistName)
          val profile = resolvedArtist.mixcloudInfo?.profile
          if (profile != null) {
            val staleThreshold = Clock.System.now() - 14.days
            if (profile.lastUpdated == null || profile.lastUpdated < staleThreshold) {
              refreshMixcloudInfo(resolvedArtist)
            }
          }
        }
      }
    }
  }

  /** Fetch SoundCloud information for the artist. */
  fun fetchSoundCloudInfo() {
    viewModelScope.launch {
      _uiState.value =
        _uiState.value.copy(
          isFetchingSoundCloud = true,
          errorMessage = null,
          soundCloudRateLimitBlockedUntil = null,
        )

      AppLogger.i("ArtistDetailViewModel", "Fetching SoundCloud info for: $artistName")

      when (val result = searchSoundCloudProfilesUseCase(artistName)) {
        is ResultWithRateLimit.Success -> {
          AppLogger.i("ArtistDetailViewModel", "Successfully searched SoundCloud profiles")
        }

        is ResultWithRateLimit.RateLimited -> {
          handleRateLimited(ArtistAudioPlatform.SoundCloud, result.blockedUntil)
          return@launch
        }

        is ResultWithRateLimit.Error -> {
          AppLogger.e("ArtistDetailViewModel", "Failed to fetch SoundCloud info: ${result.message}")
          _uiState.value =
            _uiState.value.copy(
              isFetchingSoundCloud = false,
              errorMessage = result.message,
              soundCloudRateLimitBlockedUntil = null,
            )
          return@launch
        }
      }
      val topProfile = profileSearchCache.getSoundCloudResults(artistName)?.firstOrNull()
      if (topProfile == null) {
        _uiState.value =
          _uiState.value.copy(
            isFetchingSoundCloud = false,
            errorMessage = "No profiles found for $artistName",
          )
        return@launch
      }

      when (val result = setSoundCloudProfileUseCase(artistName, topProfile.id)) {
        is ResultWithRateLimit.Success -> {
          AppLogger.i("ArtistDetailViewModel", "Successfully fetched SoundCloud info")
          _uiState.value =
            _uiState.value.copy(
              isFetchingSoundCloud = false,
              errorMessage = null,
              soundCloudRateLimitBlockedUntil = null,
            )
        }

        is ResultWithRateLimit.RateLimited -> {
          handleRateLimited(ArtistAudioPlatform.SoundCloud, result.blockedUntil)
        }

        is ResultWithRateLimit.Error -> {
          AppLogger.e("ArtistDetailViewModel", "Failed to fetch SoundCloud info: ${result.message}")
          _uiState.value =
            _uiState.value.copy(
              isFetchingSoundCloud = false,
              errorMessage = result.message,
              soundCloudRateLimitBlockedUntil = null,
            )
        }
      }
    }
  }

  private fun handleRateLimited(service: ArtistAudioPlatform, blockedUntil: Instant) {
    AppLogger.w("ArtistDetailViewModel", "$service rate limited until: $blockedUntil")
    _uiState.value =
      _uiState.value.copy(
        isFetchingSoundCloud =
          if (service == ArtistAudioPlatform.SoundCloud) false
          else _uiState.value.isFetchingSoundCloud,
        isFetchingMixcloud =
          if (service == ArtistAudioPlatform.Mixcloud) false else _uiState.value.isFetchingMixcloud,
        errorMessage = null,
        soundCloudRateLimitBlockedUntil =
          if (service == ArtistAudioPlatform.SoundCloud) blockedUntil
          else _uiState.value.soundCloudRateLimitBlockedUntil,
        mixcloudRateLimitBlockedUntil =
          if (service == ArtistAudioPlatform.Mixcloud) blockedUntil
          else _uiState.value.mixcloudRateLimitBlockedUntil,
      )
  }

  /** Searches Mixcloud profiles for artist name and sets the selected profile to the top result. */
  fun fetchMixcloudInfo() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isFetchingMixcloud = true, errorMessage = null)

      AppLogger.i("ArtistDetailViewModel", "Fetching Mixcloud info for: $artistName")
      when (val result = searchMixcloudProfilesUseCase(artistName)) {
        is ResultWithRateLimit.Success -> {
          AppLogger.i("ArtistDetailViewModel", "Successfully searched Mixcloud profiles")
        }

        is ResultWithRateLimit.RateLimited -> {
          handleRateLimited(ArtistAudioPlatform.Mixcloud, result.blockedUntil)
          return@launch
        }

        is ResultWithRateLimit.Error -> {
          AppLogger.e("ArtistDetailViewModel", "Failed to fetch Mixcloud info: ${result.message}")
          _uiState.value =
            _uiState.value.copy(isFetchingMixcloud = false, errorMessage = result.message)
          return@launch
        }
      }

      val topProfile = profileSearchCache.getMixcloudResults(artistName)?.firstOrNull()
      if (topProfile == null) {
        _uiState.value =
          _uiState.value.copy(
            isFetchingMixcloud = false,
            errorMessage = "No profiles found for $artistName",
          )
        return@launch
      }

      when (val result = setMixcloudProfileUseCase(artistName, topProfile.key)) {
        is ResultWithRateLimit.Success -> {
          AppLogger.i("ArtistDetailViewModel", "Successfully set Mixcloud profile")
          _uiState.value =
            _uiState.value.copy(
              isFetchingMixcloud = false,
              errorMessage = null,
              mixcloudRateLimitBlockedUntil = null,
            )
        }

        is ResultWithRateLimit.RateLimited -> {
          handleRateLimited(ArtistAudioPlatform.Mixcloud, result.blockedUntil)
        }

        is ResultWithRateLimit.Error -> {
          AppLogger.e("ArtistDetailViewModel", "Failed to fetch Mixcloud info: ${result.message}")
          _uiState.value =
            _uiState.value.copy(
              isFetchingMixcloud = false,
              errorMessage = result.message,
              mixcloudRateLimitBlockedUntil = null,
            )
        }
      }
    }
  }

  /**
   * Re-fetches the Mixcloud profile and shows from the API and saves the result. No-ops if no
   * Mixcloud profile is currently selected.
   */
  private fun refreshMixcloudInfo(artist: Artist) {
    if (artist.mixcloudInfo?.profile == null) return
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isFetchingMixcloud = true, errorMessage = null)
      when (val result = refreshMixcloudProfileUseCase(artist)) {
        is ResultWithRateLimitData.Success ->
          _uiState.value = _uiState.value.copy(isFetchingMixcloud = false)

        is ResultWithRateLimitData.RateLimited ->
          handleRateLimited(ArtistAudioPlatform.Mixcloud, result.blockedUntil)

        is ResultWithRateLimitData.Error ->
          _uiState.value =
            _uiState.value.copy(isFetchingMixcloud = false, errorMessage = result.message)
      }
    }
  }

  /** Delete the artist entirely from the repository. */
  fun deleteArtist() {
    viewModelScope.launch { artistRepository.deleteArtistByName(artistName) }
  }

  /** Set the selected tab. */
  fun selectTab(tab: ArtistAudioPlatform) {
    _uiState.value = _uiState.value.copy(selectedTab = tab)
  }

  // region Dev menu test helpers

  /** FOR DEV MENU USE ONLY. Sets soundCloudRateLimitBlockedUntil to now + 2 minutes. */
  fun testSoundCloudRateLimit() {
    _uiState.value =
      _uiState.value.copy(soundCloudRateLimitBlockedUntil = Clock.System.now() + 2.minutes)
  }

  /** FOR DEV MENU USE ONLY. Sets mixcloudRateLimitBlockedUntil to now + 2 minutes. */
  fun testMixcloudRateLimit() {
    _uiState.value =
      _uiState.value.copy(mixcloudRateLimitBlockedUntil = Clock.System.now() + 2.minutes)
  }

  // endregion
}
