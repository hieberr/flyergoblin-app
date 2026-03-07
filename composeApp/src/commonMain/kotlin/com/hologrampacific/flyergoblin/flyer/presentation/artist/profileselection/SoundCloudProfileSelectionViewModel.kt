package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import androidx.lifecycle.viewModelScope
import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ResultWithRateLimit
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SearchSoundCloudProfilesUseCase
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
  private val searchSoundCloudProfilesUseCase: SearchSoundCloudProfilesUseCase,
  private val profileSearchCache: ProfileSearchCache,
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
      val cachedResults = profileSearchCache.getSoundCloudResults(artistName)
      _uiState.value =
        SoundCloudProfileSelectionUiState(
          profiles = cachedResults ?: emptyList(),
          searchResultsAvailable = cachedResults != null,
          currentProfileId = currentProfileId,
          selectedProfileId = if (cachedResults != null) currentProfileId else null,
          isNoneSelected = cachedResults != null && currentProfileId == null,
          isLoading = false,
        )
    }
  }

  fun searchProfiles() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isSearching = true)
      when (val result = searchSoundCloudProfilesUseCase(artistName)) {
        is ResultWithRateLimit.Success -> {
          val profiles = profileSearchCache.getSoundCloudResults(artistName) ?: emptyList()
          val currentProfileId = _uiState.value.currentProfileId
          _uiState.value =
            _uiState.value.copy(
              isSearching = false,
              searchResultsAvailable = true,
              profiles = profiles,
              selectedProfileId = currentProfileId,
              isNoneSelected = currentProfileId == null,
            )
        }

        is ResultWithRateLimit.RateLimited -> {
          _uiState.value =
            _uiState.value.copy(
              isSearching = false,
              rateLimitBlockedUntil = result.blockedUntil,
              errorMessage =
                "Rate limit exceeded. Retry after ${result.blockedUntil.formattedString()}.",
            )
        }

        is ResultWithRateLimit.Error -> {
          _uiState.value = _uiState.value.copy(isSearching = false, errorMessage = result.message)
        }
      }
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
      if (!state.searchResultsAvailable) {
        _effects.send(SoundCloudProfileSelectionEffect.NavigateBack)
        return@launch
      }

      val hasChange =
        if (state.isNoneSelected) {
          state.currentProfileId != null
        } else {
          state.selectedProfileId != state.currentProfileId
        }

      if (!hasChange) {
        _effects.send(SoundCloudProfileSelectionEffect.NavigateBack)
        return@launch
      }

      _uiState.value = _uiState.value.copy(isConfirming = true)

      val profileId = if (state.isNoneSelected) null else state.selectedProfileId

      when (val result = setSoundCloudProfileUseCase(artistName, profileId)) {
        is ResultWithRateLimit.Success -> {
          _uiState.value = _uiState.value.copy(isConfirming = false)
          _effects.send(SoundCloudProfileSelectionEffect.NavigateBack)
        }
        is ResultWithRateLimit.Error -> {
          _uiState.value = _uiState.value.copy(isConfirming = false, errorMessage = result.message)
        }
        is ResultWithRateLimit.RateLimited -> {
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
