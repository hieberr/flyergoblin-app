package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo

data class SoundCloudProfileSelectionUiState(
  val profiles: List<SoundCloudProfileInfo> = emptyList(),
  val currentProfileId: Long? = null,
  val selectedProfileId: Long? = null,
  val isNoneSelected: Boolean = false,
  val isLoading: Boolean = true,
  val isConfirming: Boolean = false,
  val errorMessage: String? = null,
  val rateLimitResetTime: String? = null,
)
