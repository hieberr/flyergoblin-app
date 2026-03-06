package com.hologrampacific.flyergoblin.flyer.presentation.events

import androidx.lifecycle.viewModelScope
import com.hologrampacific.flyergoblin.flyer.domain.model.Event
import com.hologrampacific.flyergoblin.flyer.domain.repository.EventRepository
import com.hologrampacific.flyergoblin.presentation.ErrorMessageViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class EventsViewModel(private val repository: EventRepository) :
  ErrorMessageViewModel<EventsUiState>(EventsUiState()) {

  init {
    observeEvents()
  }

  private fun observeEvents() {
    repository
      .observeEvents()
      .onEach { events ->
        _uiState.update { state -> state.copy(events = sortEvents(events, state.sortOption)) }
      }
      .launchIn(viewModelScope)
  }

  fun deleteEvent(id: Long) {
    viewModelScope.launch {
      try {
        repository.deleteEvent(id)
      } catch (e: Exception) {
        _uiState.update { it.copy(errorMessage = "Failed to delete event") }
      }
    }
  }

  fun setSortOption(option: SortOption) {
    _uiState.update { state ->
      state.copy(sortOption = option, events = sortEvents(state.events, option))
    }
  }

  private fun sortEvents(events: List<Event>, option: SortOption) =
    when (option) {
      SortOption.BY_DATE_ADDED -> events.sortedByDescending { it.dateAdded }
      SortOption.BY_EVENT_DATE -> events.sortedBy { it.startDate ?: LocalDate(9999, 12, 31) }
    }
}
