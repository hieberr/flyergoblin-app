package com.hologrampacific.flyergoblin.flyer.presentation.event

import com.hologrampacific.flyergoblin.flyer.domain.model.Event
import com.hologrampacific.flyergoblin.util.ImageBytes
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
  val flyerImageBytes: ImageBytes? = null,
) {
  /** Returns a copy of this, overwriting only fields that are non-empty in [event]. */
  fun mergeWithProcessedEvent(event: Event, newFlyerImageBytes: ImageBytes): EditedEventData =
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
