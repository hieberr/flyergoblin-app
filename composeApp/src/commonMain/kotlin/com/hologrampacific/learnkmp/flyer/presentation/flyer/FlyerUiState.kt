package com.hologrampacific.learnkmp.flyer.presentation.flyer

import com.hologrampacific.learnkmp.flyer.domain.model.Event

data class FlyerUiState(
  val events: List<Event> = emptyList(),
  val sortOption: SortOption = SortOption.BY_DATE_ADDED,
)

enum class SortOption {
  BY_DATE_ADDED,
  BY_EVENT_DATE,
}
