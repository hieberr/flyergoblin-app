package com.hologrampacific.learnkmp.flyer.presentation.flyer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hologrampacific.learnkmp.flyer.domain.model.Event
import com.hologrampacific.learnkmp.flyer.domain.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class FlyerViewModel(private val repository: EventRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(FlyerUiState())
  val uiState: StateFlow<FlyerUiState> = _uiState.asStateFlow()

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

  fun setSortOption(option: SortOption) {
    _uiState.update { state ->
      state.copy(sortOption = option, events = sortEvents(state.events, option))
    }
  }

  private fun sortEvents(events: List<Event>, option: SortOption) =
    when (option) {
      SortOption.BY_DATE_ADDED -> events.sortedByDescending { it.dateAdded }
      SortOption.BY_EVENT_DATE -> events.sortedBy { it.startDate }
    }
}
