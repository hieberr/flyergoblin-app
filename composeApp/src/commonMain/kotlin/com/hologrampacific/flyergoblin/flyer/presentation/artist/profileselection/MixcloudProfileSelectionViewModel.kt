package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SetMixcloudProfileResult
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SetMixcloudProfileUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class MixcloudProfileSelectionEffect {
  data object NavigateBack : MixcloudProfileSelectionEffect()
}

class MixcloudProfileSelectionViewModel(
  private val artistName: String,
  private val artistRepository: ArtistRepository,
  private val setMixcloudProfileUseCase: SetMixcloudProfileUseCase,
) : ViewModel() {

  private val _uiState = MutableStateFlow(MixcloudProfileSelectionUiState())
  val uiState: StateFlow<MixcloudProfileSelectionUiState> = _uiState.asStateFlow()

  private val _effects = Channel<MixcloudProfileSelectionEffect>(Channel.BUFFERED)
  val effects = _effects.receiveAsFlow()

  init {
    loadProfiles()
  }

  private fun loadProfiles() {
    viewModelScope.launch {
      val artist = artistRepository.getArtistByName(artistName)
      val currentProfileKey = artist?.mixcloudInfo?.profile?.key
      _uiState.value =
        MixcloudProfileSelectionUiState(
          profiles = artist?.mixcloudInfo?.profileSearchResults?.results ?: emptyList(),
          currentProfileKey = currentProfileKey,
          selectedProfileKey = currentProfileKey,
          isNoneSelected = currentProfileKey == null,
          isLoading = false,
        )
    }
  }

  fun selectProfile(profileKey: String) {
    _uiState.value = _uiState.value.copy(selectedProfileKey = profileKey, isNoneSelected = false)
  }

  fun selectNone() {
    _uiState.value = _uiState.value.copy(selectedProfileKey = null, isNoneSelected = true)
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  fun confirmSelection() {
    viewModelScope.launch {
      val state = _uiState.value

      val hasChange =
        if (state.isNoneSelected) {
          state.currentProfileKey != null
        } else {
          state.selectedProfileKey != state.currentProfileKey
        }

      if (!hasChange) {
        _effects.send(MixcloudProfileSelectionEffect.NavigateBack)
        return@launch
      }

      _uiState.value = _uiState.value.copy(isConfirming = true)

      val profileKey = if (state.isNoneSelected) null else state.selectedProfileKey

      when (val result = setMixcloudProfileUseCase(artistName, profileKey)) {
        is SetMixcloudProfileResult.Success -> {
          _uiState.value = _uiState.value.copy(isConfirming = false)
          _effects.send(MixcloudProfileSelectionEffect.NavigateBack)
        }

        is SetMixcloudProfileResult.Error -> {
          _uiState.value = _uiState.value.copy(isConfirming = false, errorMessage = result.message)
        }

        is SetMixcloudProfileResult.RateLimited -> {
          _uiState.value =
            _uiState.value.copy(
              isConfirming = false,
              errorMessage =
                "Mixcloud rate limit hit. Try again in ${result.retryAfterSeconds} seconds.",
            )
        }
      }
    }
  }
}
