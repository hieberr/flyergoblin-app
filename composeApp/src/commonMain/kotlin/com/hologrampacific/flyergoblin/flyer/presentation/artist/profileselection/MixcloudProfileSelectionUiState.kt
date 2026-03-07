package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfileInfo
import com.hologrampacific.flyergoblin.presentation.HasErrorMessage

data class MixcloudProfileSelectionUiState(
  val profiles: List<MixcloudProfileInfo> = emptyList(),
  val searchResultsAvailable: Boolean = false,
  val isSearching: Boolean = false,
  val currentProfileKey: String? = null,
  val selectedProfileKey: String? = null,
  val isNoneSelected: Boolean = false,
  val isLoading: Boolean = true,
  val isConfirming: Boolean = false,
  override val errorMessage: String? = null,
) : HasErrorMessage<MixcloudProfileSelectionUiState> {
  override fun copyWithErrorMessage(errorMessage: String?) = copy(errorMessage = errorMessage)
}
