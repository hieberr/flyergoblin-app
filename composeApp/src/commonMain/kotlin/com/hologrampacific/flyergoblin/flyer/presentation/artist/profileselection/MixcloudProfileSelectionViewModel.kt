package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import androidx.lifecycle.viewModelScope
import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ResultWithRateLimit
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SearchMixcloudProfilesUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SetMixcloudProfileUseCase
import com.hologrampacific.flyergoblin.presentation.ErrorMessageViewModel
import com.hologrampacific.flyergoblin.presentation.util.formattedString
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MixcloudProfileSelectionViewModel(
  private val artistName: String,
  private val artistRepository: ArtistRepository,
  private val setMixcloudProfileUseCase: SetMixcloudProfileUseCase,
  private val searchMixcloudProfilesUseCase: SearchMixcloudProfilesUseCase,
  private val profileSearchCache: ProfileSearchCache,
) : ErrorMessageViewModel<MixcloudProfileSelectionUiState>(MixcloudProfileSelectionUiState()) {

  private val _effects = Channel<ProfileSelectionEffect>(Channel.BUFFERED)
  val effects = _effects.receiveAsFlow()

  init {
    loadProfiles()
  }

  private fun loadProfiles() {
    viewModelScope.launch {
      val artist = artistRepository.getArtistByName(artistName)
      val currentProfileKey = artist?.mixcloudInfo?.profile?.key
      val profileChosen = artist?.mixcloudInfo?.profileChosen == true
      val isCurrentNone = profileChosen && currentProfileKey == null
      val cachedResults = profileSearchCache.getMixcloudResults(artistName)
      _uiState.value =
        MixcloudProfileSelectionUiState(
          profiles = cachedResults ?: emptyList(),
          searchResultsAvailable = cachedResults != null,
          currentProfileKey = currentProfileKey,
          selectedProfileKey = if (cachedResults != null) currentProfileKey else null,
          isNoneSelected = cachedResults != null && currentProfileKey == null,
          isCurrentProfileNone = isCurrentNone,
          isLoading = false,
        )
    }
  }

  fun searchProfiles() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isSearching = true)
      when (val result = searchMixcloudProfilesUseCase(artistName)) {
        is ResultWithRateLimit.Success -> {
          val profiles = profileSearchCache.getMixcloudResults(artistName) ?: emptyList()
          val currentProfileKey = _uiState.value.currentProfileKey
          _uiState.value =
            _uiState.value.copy(
              isSearching = false,
              searchResultsAvailable = true,
              profiles = profiles,
              selectedProfileKey = currentProfileKey,
              isNoneSelected = currentProfileKey == null,
            )
        }

        is ResultWithRateLimit.RateLimited -> {
          _uiState.value =
            _uiState.value.copy(
              isSearching = false,
              rateLimitBlockedUntil = result.blockedUntil,
              errorMessage =
                "Mixcloud rate limit hit. Retry after ${result.blockedUntil.formattedString()}.",
            )
        }

        is ResultWithRateLimit.Error -> {
          _uiState.value = _uiState.value.copy(isSearching = false, errorMessage = result.message)
        }
      }
    }
  }

  fun selectProfile(profileKey: String) {
    _uiState.value = _uiState.value.copy(selectedProfileKey = profileKey, isNoneSelected = false)
  }

  fun selectNone() {
    _uiState.value = _uiState.value.copy(selectedProfileKey = null, isNoneSelected = true)
  }

  override fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null, rateLimitBlockedUntil = null)
  }

  fun confirmSelection() {
    viewModelScope.launch {
      val state = _uiState.value
      if (!state.searchResultsAvailable) {
        _effects.send(ProfileSelectionEffect.NavigateBack)
        return@launch
      }

      val hasChange =
        if (state.isNoneSelected) {
          !state.isCurrentProfileNone
        } else {
          state.selectedProfileKey != state.currentProfileKey
        }

      if (!hasChange) {
        _effects.send(ProfileSelectionEffect.NavigateBack)
        return@launch
      }

      _uiState.value = _uiState.value.copy(isConfirming = true)

      val profileKey = if (state.isNoneSelected) null else state.selectedProfileKey

      when (val result = setMixcloudProfileUseCase(artistName, profileKey)) {
        is ResultWithRateLimit.Success -> {
          _uiState.value = _uiState.value.copy(isConfirming = false)
          _effects.send(ProfileSelectionEffect.NavigateBack)
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
                "Mixcloud rate limit hit. Retry after ${result.blockedUntil.formattedString()}.",
            )
        }
      }
    }
  }
}
