package com.hologrampacific.learnkmp.flyer.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hologrampacific.learnkmp.flyer.domain.repository.EventRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

sealed class EventDetailEffect {
  data object NavigateBack : EventDetailEffect()
}

class EventDetailViewModel(private val eventId: String, private val repository: EventRepository) :
  ViewModel() {
  private val _uiState = MutableStateFlow(EventDetailUiState())
  val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

  private val _effects = Channel<EventDetailEffect>(Channel.BUFFERED)
  val effects = _effects.receiveAsFlow()

  init {
    loadEvent()
  }

  private fun loadEvent() {
    viewModelScope.launch {
      val event = repository.getEventById(eventId)
      _uiState.update { it.copy(event = event, isLoading = false) }
    }
  }

  fun startEditing() {
    val event = _uiState.value.event ?: return
    _uiState.update {
      it.copy(
        isEditing = true,
        editedEvent =
          EditedEventData(
            name = event.name,
            startDate = event.startDate.toString(),
            startTime = event.startTime?.toString() ?: "",
            venue = event.venue ?: "",
            eventUrl = event.eventUrl ?: "",
            artists = event.artists.joinToString(", "),
            flyerImageBytes = event.flyerImageBytes,
          ),
      )
    }
  }

  fun cancelEditing() {
    _uiState.update { it.copy(isEditing = false, editedEvent = null) }
  }

  fun updateEditedEvent(editedEvent: EditedEventData) {
    _uiState.update { it.copy(editedEvent = editedEvent, errorMessage = null) }
  }

  fun saveEvent() {
    val currentEvent = _uiState.value.event ?: return
    val editedData = _uiState.value.editedEvent ?: return

    viewModelScope.launch {
      try {
        val updatedEvent =
          currentEvent.copy(
            name = editedData.name,
            startDate = LocalDate.parse(editedData.startDate),
            startTime =
              if (editedData.startTime.isNotBlank()) LocalTime.parse(editedData.startTime)
              else null,
            venue = editedData.venue.ifBlank { null },
            eventUrl = editedData.eventUrl.ifBlank { null },
            artists = editedData.artists.split(",").map { it.trim() }.filter { it.isNotBlank() },
            flyerImageBytes = editedData.flyerImageBytes,
          )
        repository.updateEvent(updatedEvent)
        _uiState.update {
          it.copy(event = updatedEvent, isEditing = false, editedEvent = null, errorMessage = null)
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            errorMessage =
              "Invalid date or time format. Please use YYYY-MM-DD for date and HH:MM for time."
          )
        }
      }
    }
  }

  /**
   * Deletes the event from the repository.
   *
   * Note: Event flyer images are stored inline with the event as serialized byte arrays.
   * When the event is deleted, the image bytes are automatically cleaned up by the GC
   * as part of the event's serialized data. No explicit image cleanup is required.
   */
  fun deleteEvent() {
    viewModelScope.launch {
      repository.deleteEvent(eventId)
      _effects.send(EventDetailEffect.NavigateBack)
    }
  }
}
