package com.hologrampacific.learnkmp.flyer.presentation.addevent

data class AddEventUiState(
  val selectedImageBytes: ByteArray? = null,
  val isProcessing: Boolean = false,
  val errorMessage: String? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as AddEventUiState

    if (selectedImageBytes != null) {
      if (other.selectedImageBytes == null) return false
      if (!selectedImageBytes.contentEquals(other.selectedImageBytes)) return false
    } else if (other.selectedImageBytes != null) return false
    if (isProcessing != other.isProcessing) return false
    if (errorMessage != other.errorMessage) return false

    return true
  }

  override fun hashCode(): Int {
    var result = selectedImageBytes?.contentHashCode() ?: 0
    result = 31 * result + isProcessing.hashCode()
    result = 31 * result + (errorMessage?.hashCode() ?: 0)
    return result
  }
}
