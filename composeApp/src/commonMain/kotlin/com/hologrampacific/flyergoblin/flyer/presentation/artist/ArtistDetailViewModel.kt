package com.hologrampacific.flyergoblin.flyer.presentation.artist

import androidx.lifecycle.viewModelScope
import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ResultWithRateLimit
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SearchMixcloudProfilesUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SearchSoundCloudProfilesUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SetMixcloudProfileUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SetSoundCloudProfileUseCase
import com.hologrampacific.flyergoblin.presentation.ErrorMessageViewModel
import com.hologrampacific.flyergoblin.presentation.util.formattedString
import com.hologrampacific.flyergoblin.util.AppLogger
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
  private val profileSearchCache: ProfileSearchCache,
) : ErrorMessageViewModel<ArtistDetailUiState>(ArtistDetailUiState()) {

  init {
    observeArtist()
  }

  /** Observe artist data from repository and auto-fetch tracks when profile changes. */
  private fun observeArtist() {
    viewModelScope.launch {
      artistRepository.observeArtistByName(artistName).collect { artist ->
        _uiState.value =
          _uiState.value.copy(artist = artist ?: Artist(name = artistName), isLoading = false)
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
          rateLimitBlockedUntil = null,
        )

      AppLogger.i("ArtistDetailViewModel", "Fetching SoundCloud info for: $artistName")

      when (val result = searchSoundCloudProfilesUseCase(artistName)) {
        is ResultWithRateLimit.Success -> {
          AppLogger.i("ArtistDetailViewModel", "Successfully searched SoundCloud profiles")
        }

        is ResultWithRateLimit.RateLimited -> {
          handleRateLimited("SoundCloud", result.blockedUntil)
          return@launch
        }

        is ResultWithRateLimit.Error -> {
          AppLogger.e("ArtistDetailViewModel", "Failed to fetch SoundCloud info: ${result.message}")
          _uiState.value =
            _uiState.value.copy(
              isFetchingSoundCloud = false,
              errorMessage = result.message,
              rateLimitBlockedUntil = null,
            )
          return@launch
        }
      }
      val topProfile = profileSearchCache.getSoundCloudResults(artistName)?.firstOrNull()
      if (topProfile == null) {
        _uiState.value = _uiState.value.copy(isFetchingSoundCloud = false, errorMessage = null)
        return@launch
      }

      when (val result = setSoundCloudProfileUseCase(artistName, topProfile.id)) {
        is ResultWithRateLimit.Success -> {
          AppLogger.i("ArtistDetailViewModel", "Successfully fetched SoundCloud info")
          _uiState.value =
            _uiState.value.copy(
              isFetchingSoundCloud = false,
              errorMessage = null,
              rateLimitBlockedUntil = null,
            )
        }

        is ResultWithRateLimit.RateLimited -> {
          handleRateLimited("SoundCloud", result.blockedUntil)
        }

        is ResultWithRateLimit.Error -> {
          AppLogger.e("ArtistDetailViewModel", "Failed to fetch SoundCloud info: ${result.message}")
          _uiState.value =
            _uiState.value.copy(
              isFetchingSoundCloud = false,
              errorMessage = result.message,
              rateLimitBlockedUntil = null,
            )
        }
      }
    }
  }

  private fun handleRateLimited(serviceName: String, blockedUntil: Instant) {
    AppLogger.w("ArtistDetailViewModel", "$serviceName rate limited until: $blockedUntil")
    _uiState.value =
      _uiState.value.copy(
        isFetchingSoundCloud = false,
        isFetchingMixcloud = false,
        errorMessage =
          "$serviceName rate limit exceeded. Retry after ${blockedUntil.formattedString()}.",
        rateLimitBlockedUntil = blockedUntil,
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
          handleRateLimited("Mixcloud", result.blockedUntil)
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
              rateLimitBlockedUntil = null,
            )
        }

        is ResultWithRateLimit.RateLimited -> {
          handleRateLimited("Mixcloud", result.blockedUntil)
        }

        is ResultWithRateLimit.Error -> {
          AppLogger.e("ArtistDetailViewModel", "Failed to fetch Mixcloud info: ${result.message}")
          _uiState.value =
            _uiState.value.copy(
              isFetchingMixcloud = false,
              errorMessage = result.message,
              rateLimitBlockedUntil = null,
            )
        }
      }
    }
  }

  /** Delete the artist entirely from the repository. */
  fun deleteArtist() {
    viewModelScope.launch { artistRepository.deleteArtistByName(artistName) }
  }

  /** Set the selected tab. */
  fun selectTab(tab: ArtistTab) {
    _uiState.value = _uiState.value.copy(selectedTab = tab)
  }
}
