package com.hologrampacific.learnkmp.flyer.presentation.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hologrampacific.learnkmp.flyer.domain.model.Artist
import com.hologrampacific.learnkmp.flyer.domain.repository.ArtistRepository
import com.hologrampacific.learnkmp.flyer.domain.usecase.ResearchArtistResult
import com.hologrampacific.learnkmp.flyer.domain.usecase.ResearchArtistUseCase
import com.hologrampacific.learnkmp.util.AppLogger
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
) : ViewModel() {

  private val _uiState = MutableStateFlow(ArtistDetailUiState())
  val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

  init {
    loadArtist()
  }

  /** Load artist data from repository. */
  private fun loadArtist() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)

      val artist = artistRepository.getArtistByName(artistName)

      _uiState.value =
        _uiState.value.copy(artist = artist ?: Artist(name = artistName), isLoading = false)
    }
  }

  /** Fetch SoundCloud information for the artist using the research use case. */
  fun fetchSoundCloudInfo() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isFetchingSoundCloud = true, errorMessage = null)

      AppLogger.i("ArtistDetailViewModel", "Fetching SoundCloud info for: $artistName")

      when (val result = researchArtistUseCase(artistName)) {
        is ResearchArtistResult.Success -> {
          AppLogger.i("ArtistDetailViewModel", "Successfully fetched SoundCloud info")
          artistRepository.updateArtist(result.artist)
          _uiState.value =
            _uiState.value.copy(
              artist = result.artist,
              isFetchingSoundCloud = false,
              errorMessage = null,
            )
        }
        is ResearchArtistResult.Error -> {
          AppLogger.e("ArtistDetailViewModel", "Failed to fetch SoundCloud info: ${result.message}")
          _uiState.value =
            _uiState.value.copy(isFetchingSoundCloud = false, errorMessage = result.message)
        }
      }
    }
  }

  /** Clear the current error message. */
  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }
}
