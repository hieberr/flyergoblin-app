package com.hologrampacific.learnkmp.flyer.presentation.addevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hologrampacific.learnkmp.flyer.domain.repository.EventRepository
import com.hologrampacific.learnkmp.flyer.domain.service.AiProcessingResult
import com.hologrampacific.learnkmp.flyer.domain.service.FlyerAiService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AddEventEffect {
  data class NavigateToEventDetail(val eventId: String) : AddEventEffect()
}

class AddEventViewModel(
  private val repository: EventRepository,
  private val aiService: FlyerAiService,
) : ViewModel() {
  private val _uiState = MutableStateFlow(AddEventUiState())
  val uiState: StateFlow<AddEventUiState> = _uiState.asStateFlow()

  private val _effects = Channel<AddEventEffect>(Channel.BUFFERED)
  val effects = _effects.receiveAsFlow()

  fun onImageSelected(imageBytes: ByteArray) {
    _uiState.update { it.copy(selectedImageBytes = imageBytes, errorMessage = null) }
  }

  fun processFlyer() {
    val imageBytes = _uiState.value.selectedImageBytes ?: return

    viewModelScope.launch {
      _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

      when (val result = aiService.processFlyer(imageBytes)) {
        is AiProcessingResult.Success -> {
          repository.saveEvent(result.event)
          _uiState.update { it.copy(isProcessing = false) }
          _effects.send(AddEventEffect.NavigateToEventDetail(result.event.id))
        }
        is AiProcessingResult.Error -> {
          _uiState.update { it.copy(isProcessing = false, errorMessage = result.message) }
        }
      }
    }
  }
}
