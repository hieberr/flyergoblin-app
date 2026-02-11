package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SetSoundCloudProfileUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class SoundCloudProfileSelectionEffect {
  data object NavigateBack : SoundCloudProfileSelectionEffect()
}

class SoundCloudProfileSelectionViewModel(
  private val artistName: String,
  private val artistRepository: ArtistRepository,
  private val setSoundCloudProfileUseCase: SetSoundCloudProfileUseCase,
) : ViewModel() {

  private val _uiState = MutableStateFlow(SoundCloudProfileSelectionUiState())
  val uiState: StateFlow<SoundCloudProfileSelectionUiState> = _uiState.asStateFlow()

  private val _effects = Channel<SoundCloudProfileSelectionEffect>(Channel.BUFFERED)
  val effects = _effects.receiveAsFlow()

  init {
    loadProfiles()
  }

  private fun loadProfiles() {
    viewModelScope.launch {
      val artist = artistRepository.getArtistByName(artistName)
      _uiState.value =
        SoundCloudProfileSelectionUiState(
          profiles = artist?.soundCloudProfiles ?: emptyList(),
          currentProfileUrl = artist?.soundCloudProfile?.profileUrl,
          selectedProfileUrl = artist?.soundCloudProfile?.profileUrl,
          isLoading = false,
        )
    }
  }

  fun selectProfile(profileUrl: String) {
    _uiState.value = _uiState.value.copy(selectedProfileUrl = profileUrl)
  }

  fun confirmSelection() {
    viewModelScope.launch {
      val state = _uiState.value
      val selectedUrl = state.selectedProfileUrl

      if (selectedUrl != null && selectedUrl != state.currentProfileUrl) {
        setSoundCloudProfileUseCase(artistName, selectedUrl)
      }

      _effects.send(SoundCloudProfileSelectionEffect.NavigateBack)
    }
  }
}
