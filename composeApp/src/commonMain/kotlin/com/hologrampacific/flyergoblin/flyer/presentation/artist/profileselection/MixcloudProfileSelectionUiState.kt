package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfileInfo

data class MixcloudProfileSelectionUiState(
  val profiles: List<MixcloudProfileInfo> = emptyList(),
  val currentProfileKey: String? = null,
  val selectedProfileKey: String? = null,
  val isNoneSelected: Boolean = false,
  val isLoading: Boolean = true,
  val isConfirming: Boolean = false,
  val errorMessage: String? = null,
)
