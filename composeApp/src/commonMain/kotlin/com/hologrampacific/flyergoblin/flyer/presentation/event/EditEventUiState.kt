package com.hologrampacific.flyergoblin.flyer.presentation.event

import com.hologrampacific.flyergoblin.util.ImageBytes

/** Describes which crop operation is currently active. */
sealed class CropMode {
  /** A newly selected image that needs to be cropped before being applied to the event. */
  data class NewImage(val bytes: ImageBytes) : CropMode()

  /** The existing flyer image is being re-cropped. */
  data object EditExisting : CropMode()
}

data class EditEventUiState(
  val editedEvent: EditedEventData? = null,
  val originalEventId: Long? = null,
  val isLoading: Boolean = true,
  val errorMessage: String? = null,
  val isProcessingFlyer: Boolean = false,
  val isSaving: Boolean = false,
  /** Non-null while a crop operation is in progress. */
  val cropMode: CropMode? = null,
)
