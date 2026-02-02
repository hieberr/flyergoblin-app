package com.hologrampacific.learnkmp.flyer.presentation.addevent

import io.github.vinceglb.filekit.core.PlatformFile

data class AddEventUiState(
  val selectedImageFile: PlatformFile? = null,
  val isProcessing: Boolean = false,
  val errorMessage: String? = null,
)
