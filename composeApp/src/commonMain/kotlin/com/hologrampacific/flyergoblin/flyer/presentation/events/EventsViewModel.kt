package com.hologrampacific.flyergoblin.flyer.presentation.events

import androidx.lifecycle.viewModelScope
import com.hologrampacific.flyergoblin.flyer.domain.model.Event
import com.hologrampacific.flyergoblin.flyer.domain.repository.EventRepository
import com.hologrampacific.flyergoblin.presentation.ErrorMessageViewModel
import kotlin.time.Clock
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class EventsViewModel(
  private val repository: EventRepository,
  private val clock: Clock = Clock.System,
) : ErrorMessageViewModel<EventsUiState>(EventsUiState()) {

  private var allEvents: List<Event> = emptyList()

  init {
    observeEvents()
  }

  private fun observeEvents() {
    repository
      .observeEvents()
      .onEach { events ->
        allEvents = events
        _uiState.update { state ->
          state.copy(events = filterAndSortEvents(events, state.sortOption, state.showPastEvents))
        }
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
      state.copy(
        sortOption = option,
        events = filterAndSortEvents(allEvents, option, state.showPastEvents),
      )
    }
  }

  fun setShowPastEvents(show: Boolean) {
    _uiState.update { state ->
      state.copy(
        showPastEvents = show,
        events = filterAndSortEvents(allEvents, state.sortOption, show),
      )
    }
  }

  fun isPastEvent(event: Event): Boolean {
    val startDate = event.startDate ?: return false
    val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return startDate < today
  }

  private fun filterAndSortEvents(
    events: List<Event>,
    option: SortOption,
    showPastEvents: Boolean,
  ): List<Event> {
    val filtered = events.filter { showPastEvents || !isPastEvent(it) }

    return when (option) {
      SortOption.BY_DATE_ADDED -> filtered.sortedByDescending { it.dateAdded }
      SortOption.BY_EVENT_DATE -> filtered.sortedBy { it.startDate ?: LocalDate(9999, 12, 31) }
    }
  }
}
