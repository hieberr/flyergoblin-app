package com.hologrampacific.flyergoblin.flyer.presentation.event

import com.hologrampacific.flyergoblin.flyer.domain.model.Event
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class EventDetailUiState(val event: Event? = null, val isLoading: Boolean = true)

data class EditedEventData(
  val name: String = "",
  val startDate: LocalDate? = null,
  val startTime: LocalTime? = null,
  val venue: String = "",
  val eventUrl: String = "",
  val artists: String = "",
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
    result = 31 * result + (startDate?.hashCode() ?: 0)
    result = 31 * result + (startTime?.hashCode() ?: 0)
    result = 31 * result + venue.hashCode()
    result = 31 * result + eventUrl.hashCode()
    result = 31 * result + artists.hashCode()
    result = 31 * result + (flyerImageBytes?.contentHashCode() ?: 0)
    return result
  }

  /** Returns a copy of this, overwriting only fields that are non-empty in [event]. */
  fun mergeWithProcessedEvent(event: Event, newFlyerImageBytes: ByteArray): EditedEventData =
    copy(
      name = event.name.takeIf { it.isNotBlank() } ?: name,
      startDate = event.startDate ?: startDate,
      startTime = event.startTime ?: startTime,
      venue = event.venue?.takeIf { it.isNotBlank() } ?: venue,
      eventUrl = event.eventUrl?.takeIf { it.isNotBlank() } ?: eventUrl,
      artists = event.artists.joinToString(", ").takeIf { it.isNotBlank() } ?: artists,
      flyerImageBytes = newFlyerImageBytes,
    )
}
