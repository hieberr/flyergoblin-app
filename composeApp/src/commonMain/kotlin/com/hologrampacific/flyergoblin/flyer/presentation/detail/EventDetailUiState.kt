package com.hologrampacific.flyergoblin.flyer.presentation.detail

import com.hologrampacific.flyergoblin.flyer.domain.model.Event

data class EventDetailUiState(
  val event: Event? = null,
  val isEditing: Boolean = false,
  val editedEvent: EditedEventData? = null,
  val isLoading: Boolean = true,
  val errorMessage: String? = null,
)

data class EditedEventData(
  val name: String,
  val startDate: String,
  val startTime: String,
  val venue: String,
  val eventUrl: String,
  val artists: String,
  val flyerImageBytes: ByteArray? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as EditedEventData

    if (name != other.name) return false
    if (startDate != other.startDate) return false
    if (startTime != other.startTime) return false
    if (venue != other.venue) return false
    if (eventUrl != other.eventUrl) return false
    if (artists != other.artists) return false
    if (flyerImageBytes != null) {
      if (other.flyerImageBytes == null) return false
      if (!flyerImageBytes.contentEquals(other.flyerImageBytes)) return false
    } else if (other.flyerImageBytes != null) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + startDate.hashCode()
    result = 31 * result + startTime.hashCode()
    result = 31 * result + venue.hashCode()
    result = 31 * result + eventUrl.hashCode()
    result = 31 * result + artists.hashCode()
    result = 31 * result + (flyerImageBytes?.contentHashCode() ?: 0)
    return result
  }
}
