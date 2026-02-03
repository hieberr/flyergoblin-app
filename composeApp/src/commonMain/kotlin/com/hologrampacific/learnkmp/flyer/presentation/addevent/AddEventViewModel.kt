package com.hologrampacific.learnkmp.flyer.presentation.addevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hologrampacific.learnkmp.flyer.domain.model.Event
import com.hologrampacific.learnkmp.flyer.domain.repository.EventRepository
import com.hologrampacific.learnkmp.flyer.domain.service.FlyerAiProcessingResult
import com.hologrampacific.learnkmp.flyer.domain.service.FlyerAiService
import com.hologrampacific.learnkmp.util.isValidImage
import com.hologrampacific.learnkmp.util.processImageForStorage
import io.github.vinceglb.filekit.core.PlatformFile
import kotlin.time.Clock
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

  fun onImageSelected(imageFile: PlatformFile) {
    _uiState.update { it.copy(selectedImageFile = imageFile, errorMessage = null) }
  }

  fun processFlyer(isManual: Boolean) {
    val imageFile = _uiState.value.selectedImageFile ?: return

    viewModelScope.launch {
      _uiState.update { it.copy(errorMessage = null) }
      val originalBytes = imageFile.readBytes()

      // Validate that the file is actually an image
      if (!isValidImage(originalBytes)) {
        _uiState.update {
          it.copy(
            isProcessing = false,
            selectedImageFile = null,
            errorMessage =
              "Invalid image file. Please select a valid image (JPEG, PNG, GIF, BMP, or WebP).",
          )
        }
        return@launch
      }

      // Process image: convert to JPEG and resize if needed
      val processedBytes = processImageForStorage(originalBytes)
      if (processedBytes == null) {
        _uiState.update {
          it.copy(
            isProcessing = false,
            selectedImageFile = null,
            errorMessage = "Failed to process image. Please try a different image.",
          )
        }
        return@launch
      }

      if (isManual) {
        val event =
          Event(
            Event.generateId(),
            name = "Unknown",
            startDate = null,
            startTime = null,
            venue = null,
            eventUrl = null,
            artists = emptyList(),
            dateAdded = Clock.System.now(),
            flyerImageBytes = processedBytes,
          )

        repository.saveEvent(event)
        _uiState.update { it.copy(isProcessing = false, selectedImageFile = null) }
        _effects.send(AddEventEffect.NavigateToEventDetail(event.id))
      } else {
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

        when (val result = aiService.processFlyer(processedBytes)) {
          is FlyerAiProcessingResult.Success -> {
            repository.saveEvent(result.event)
            _uiState.update { it.copy(isProcessing = false, selectedImageFile = null) }
            _effects.send(AddEventEffect.NavigateToEventDetail(result.event.id))
          }

          is FlyerAiProcessingResult.Error -> {
            _uiState.update {
              it.copy(isProcessing = false, selectedImageFile = null, errorMessage = result.message)
            }
          }
        }
      }
    }
  }
}
