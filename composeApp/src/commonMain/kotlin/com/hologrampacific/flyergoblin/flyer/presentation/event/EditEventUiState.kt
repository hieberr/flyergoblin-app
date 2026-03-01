package com.hologrampacific.flyergoblin.flyer.presentation.event

data class EditEventUiState(
  val editedEvent: EditedEventData? = null,
  val originalEventId: Long? = null,
  val isLoading: Boolean = true,
  val errorMessage: String? = null,
  val isProcessingFlyer: Boolean = false,
  val isSaving: Boolean = false,
)
