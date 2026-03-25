package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import com.hologrampacific.flyergoblin.presentation.HasErrorMessage
import kotlin.time.Instant

data class SoundCloudProfileSelectionUiState(
  val profiles: List<SoundCloudProfileInfo> = emptyList(),
  val searchResultsAvailable: Boolean = false,
  val isSearching: Boolean = false,
  val currentProfileId: Long? = null,
  val selectedProfileId: Long? = null,
  val isNoneSelected: Boolean = false,
  val isCurrentProfileNone: Boolean = false,
  val isCurrentProfileChosen: Boolean = false,
  val isLoading: Boolean = true,
  val isConfirming: Boolean = false,
  override val errorMessage: String? = null,
  val rateLimitBlockedUntil: Instant? = null,
) : HasErrorMessage<SoundCloudProfileSelectionUiState> {
  override fun copyWithErrorMessage(errorMessage: String?) = copy(errorMessage = errorMessage)
}
