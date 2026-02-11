package com.hologrampacific.learnkmp.flyer.presentation.artist.profileselection

import com.hologrampacific.learnkmp.flyer.domain.model.SoundCloudProfileInfo

data class SoundCloudProfileSelectionUiState(
  val profiles: List<SoundCloudProfileInfo> = emptyList(),
  val currentProfileUrl: String? = null,
  val selectedProfileUrl: String? = null,
  val isLoading: Boolean = true,
)
