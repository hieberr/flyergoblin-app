package com.hologrampacific.flyergoblin.flyer.presentation.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ResearchArtistResult
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ResearchArtistUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ResearchMixcloudArtistUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ResearchMixcloudResult
import com.hologrampacific.flyergoblin.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Artist Detail screen. Manages loading artist data from the repository and
 * fetching SoundCloud info via AI.
 *
 * @param artistName The name of the artist to display
 * @param artistRepository Repository for loading and saving artist data
 * @param researchArtistUseCase Use case for researching artist SoundCloud information
 */
class ArtistDetailViewModel(
  private val artistName: String,
  private val artistRepository: ArtistRepository,
  private val researchArtistUseCase: ResearchArtistUseCase,
  private val researchMixcloudArtistUseCase: ResearchMixcloudArtistUseCase,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ArtistDetailUiState())
  val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

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
          rateLimitResetTime = null,
        )

      AppLogger.i("ArtistDetailViewModel", "Fetching SoundCloud info for: $artistName")

      when (val result = researchArtistUseCase(artistName)) {
        is ResearchArtistResult.Success -> {
          AppLogger.i("ArtistDetailViewModel", "Successfully fetched SoundCloud info")
          _uiState.value =
            _uiState.value.copy(
              isFetchingSoundCloud = false,
              errorMessage = null,
              rateLimitResetTime = null,
            )
        }
        is ResearchArtistResult.RateLimited -> {
          AppLogger.w("ArtistDetailViewModel", "Rate limited. Resets at: ${result.resetTime}")
          val errorMessage =
            if (result.resetTime.startsWith("Unknown")) {
              "SoundCloud rate limit exceeded. Please wait and try again later."
            } else {
              "SoundCloud rate limit exceeded. Try again after ${result.resetTime}"
            }
          _uiState.value =
            _uiState.value.copy(
              isFetchingSoundCloud = false,
              errorMessage = errorMessage,
              rateLimitResetTime = result.resetTime,
            )
        }
        is ResearchArtistResult.Error -> {
          AppLogger.e("ArtistDetailViewModel", "Failed to fetch SoundCloud info: ${result.message}")
          _uiState.value =
            _uiState.value.copy(
              isFetchingSoundCloud = false,
              errorMessage = result.message,
              rateLimitResetTime = null,
            )
        }
      }
    }
  }

  /** Fetch Mixcloud information for the artist. */
  fun fetchMixcloudInfo() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isFetchingMixcloud = true, errorMessage = null)

      AppLogger.i("ArtistDetailViewModel", "Fetching Mixcloud info for: $artistName")

      when (val result = researchMixcloudArtistUseCase(artistName)) {
        is ResearchMixcloudResult.Success -> {
          AppLogger.i("ArtistDetailViewModel", "Successfully fetched Mixcloud info")
          _uiState.value = _uiState.value.copy(isFetchingMixcloud = false, errorMessage = null)
        }

        is ResearchMixcloudResult.RateLimited -> {
          AppLogger.w(
            "ArtistDetailViewModel",
            "Mixcloud rate limited. Retry after ${result.retryAfterSeconds}s.",
          )
          val errorMessage =
            "Mixcloud rate limit exceeded. Try again in ${result.retryAfterSeconds} seconds."
          _uiState.value =
            _uiState.value.copy(isFetchingMixcloud = false, errorMessage = errorMessage)
        }

        is ResearchMixcloudResult.Error -> {
          AppLogger.e("ArtistDetailViewModel", "Failed to fetch Mixcloud info: ${result.message}")
          _uiState.value =
            _uiState.value.copy(isFetchingMixcloud = false, errorMessage = result.message)
        }
      }
    }
  }

  /** Clear the current error message. */
  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  /** Set the selected tab. */
  fun selectTab(tab: ArtistTab) {
    _uiState.value = _uiState.value.copy(selectedTab = tab)
  }
}
