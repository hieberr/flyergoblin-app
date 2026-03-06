package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import androidx.lifecycle.viewModelScope
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SetSoundCloudProfileResult
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SetSoundCloudProfileUseCase
import com.hologrampacific.flyergoblin.presentation.ErrorMessageViewModel
import com.hologrampacific.flyergoblin.presentation.util.formattedString
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class SoundCloudProfileSelectionEffect {
  data object NavigateBack : SoundCloudProfileSelectionEffect()
}

class SoundCloudProfileSelectionViewModel(
  private val artistName: String,
  private val artistRepository: ArtistRepository,
  private val setSoundCloudProfileUseCase: SetSoundCloudProfileUseCase,
) : ErrorMessageViewModel<SoundCloudProfileSelectionUiState>(SoundCloudProfileSelectionUiState()) {

  private val _effects = Channel<SoundCloudProfileSelectionEffect>(Channel.BUFFERED)
  val effects = _effects.receiveAsFlow()

  init {
    loadProfiles()
  }

  private fun loadProfiles() {
    viewModelScope.launch {
      val artist = artistRepository.getArtistByName(artistName)
      val currentProfileId = artist?.soundCloudInfo?.profile?.id
      _uiState.value =
        SoundCloudProfileSelectionUiState(
          profiles = artist?.soundCloudInfo?.profileSearchResults?.results ?: emptyList(),
          currentProfileId = currentProfileId,
          selectedProfileId = currentProfileId,
          isNoneSelected = currentProfileId == null,
          isLoading = false,
        )
    }
  }

  fun selectProfile(profileId: Long) {
    _uiState.value = _uiState.value.copy(selectedProfileId = profileId, isNoneSelected = false)
  }

  fun selectNone() {
    _uiState.value = _uiState.value.copy(selectedProfileId = null, isNoneSelected = true)
  }

  override fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null, rateLimitBlockedUntil = null)
  }

  fun confirmSelection() {
    viewModelScope.launch {
      val state = _uiState.value

      // Check if there's actually a change
      val hasChange =
        if (state.isNoneSelected) {
          state.currentProfileId != null
        } else {
          state.selectedProfileId != state.currentProfileId
        }

      if (!hasChange) {
        // No change made, just navigate back
        _effects.send(SoundCloudProfileSelectionEffect.NavigateBack)
        return@launch
      }

      _uiState.value = _uiState.value.copy(isConfirming = true)

      val profileId = if (state.isNoneSelected) null else state.selectedProfileId

      when (val result = setSoundCloudProfileUseCase(artistName, profileId)) {
        is SetSoundCloudProfileResult.Success -> {
          _uiState.value = _uiState.value.copy(isConfirming = false)
          _effects.send(SoundCloudProfileSelectionEffect.NavigateBack)
        }
        is SetSoundCloudProfileResult.Error -> {
          _uiState.value = _uiState.value.copy(isConfirming = false, errorMessage = result.message)
        }
        is SetSoundCloudProfileResult.RateLimited -> {
          _uiState.value =
            _uiState.value.copy(
              isConfirming = false,
              rateLimitBlockedUntil = result.blockedUntil,
              errorMessage =
                "Rate limit exceeded. Retry after ${result.blockedUntil.formattedString()}.",
            )
        }
      }
    }
  }
}
