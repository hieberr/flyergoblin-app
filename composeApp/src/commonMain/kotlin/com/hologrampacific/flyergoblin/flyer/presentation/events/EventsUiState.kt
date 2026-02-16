package com.hologrampacific.flyergoblin.flyer.presentation.events

import com.hologrampacific.flyergoblin.flyer.domain.model.Event

data class EventsUiState(
  val events: List<Event> = emptyList(),
  val sortOption: SortOption = SortOption.BY_DATE_ADDED,
)

enum class SortOption {
  BY_DATE_ADDED,
  BY_EVENT_DATE,
}
