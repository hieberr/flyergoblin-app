package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo

data class SoundCloudProfileSelectionUiState(
  val profiles: List<SoundCloudProfileInfo> = emptyList(),
  val currentProfileUrl: String? = null,
  val selectedProfileUrl: String? = null,
  val isLoading: Boolean = true,
)
