package com.hologrampacific.flyergoblin.flyer.presentation.events

import com.hologrampacific.flyergoblin.flyer.domain.model.Event
import com.hologrampacific.flyergoblin.presentation.HasErrorMessage

data class EventsUiState(
  val events: List<Event> = emptyList(),
  val sortOption: SortOption = SortOption.BY_EVENT_DATE,
  val showPastEvents: Boolean = false,
  override val errorMessage: String? = null,
) : HasErrorMessage<EventsUiState> {
  override fun copyWithErrorMessage(errorMessage: String?) = copy(errorMessage = errorMessage)
}

enum class SortOption {
  BY_DATE_ADDED,
  BY_EVENT_DATE,
}
