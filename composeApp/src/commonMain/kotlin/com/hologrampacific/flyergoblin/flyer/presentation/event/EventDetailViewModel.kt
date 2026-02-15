package com.hologrampacific.flyergoblin.flyer.presentation.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hologrampacific.flyergoblin.flyer.domain.repository.EventRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class EventDetailEffect {
  data object NavigateBack : EventDetailEffect()
}

class EventDetailViewModel(private val eventId: String?, private val repository: EventRepository) :
  ViewModel() {
  private val _uiState = MutableStateFlow(EventDetailUiState())
  val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

  private val _effects = Channel<EventDetailEffect>(Channel.BUFFERED)
  val effects = _effects.receiveAsFlow()

  init {
    if (eventId != null) {
      loadEvent()
    } else {
      // Should never happen - create mode goes to EditEventScreen
      _uiState.update { it.copy(isLoading = false) }
    }
  }

  private fun loadEvent() {
    viewModelScope.launch {
      val event = repository.getEventById(eventId!!)
      _uiState.update { it.copy(event = event, isLoading = false) }
    }
  }

  fun refreshEvent() {
    if (eventId != null) {
      _uiState.update { it.copy(isLoading = true) }
      loadEvent()
    }
  }

  /**
   * Deletes the event from the repository.
   *
   * Note: Event flyer images are stored inline with the event as serialized byte arrays. When the
   * event is deleted, the image bytes are automatically cleaned up by the GC as part of the event's
   * serialized data. No explicit image cleanup is required.
   */
  fun deleteEvent() {
    viewModelScope.launch {
      repository.deleteEvent(eventId!!)
      _effects.send(EventDetailEffect.NavigateBack)
    }
  }
}
