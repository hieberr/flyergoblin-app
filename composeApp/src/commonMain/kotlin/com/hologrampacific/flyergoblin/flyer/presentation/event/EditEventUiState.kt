package com.hologrampacific.flyergoblin.flyer.presentation.event

import io.github.vinceglb.filekit.core.PlatformFile

data class EditEventUiState(
  val editedEvent: EditedEventData? = null,
  val originalEventId: Long? = null,
  val isLoading: Boolean = true,
  val errorMessage: String? = null,
  val selectedImageFile: PlatformFile? = null,
  val isProcessingFlyer: Boolean = false,
  val isSaving: Boolean = false,
)
