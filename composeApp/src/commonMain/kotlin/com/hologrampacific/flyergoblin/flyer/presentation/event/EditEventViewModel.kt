package com.hologrampacific.flyergoblin.flyer.presentation.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hologrampacific.flyergoblin.flyer.domain.model.Event
import com.hologrampacific.flyergoblin.flyer.domain.repository.EventRepository
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ProcessFlyerResult
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ProcessFlyerUseCase
import com.hologrampacific.flyergoblin.presentation.util.reencodeImageToFitSize
import com.hologrampacific.flyergoblin.sharing.SharedImageProvider
import com.hologrampacific.flyergoblin.util.BYTES_PER_KB
import com.hologrampacific.flyergoblin.util.ImageBytes
import io.github.vinceglb.filekit.core.PlatformFile
import kotlin.time.Clock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class EditEventEffect {
  data object NavigateBack : EditEventEffect()

  data class NavigateToEventDetail(val eventId: Long) : EditEventEffect()
}

class EditEventViewModel(
  private val eventId: Long?,
  private val repository: EventRepository,
  private val processFlyerUseCase: ProcessFlyerUseCase,
  private val sharedImageProvider: SharedImageProvider,
  private val encodeImage: (ImageBytes, Int) -> ImageBytes? = { imageBytes, maxSize ->
    reencodeImageToFitSize(imageBytes, maxSize)
  },
) : ViewModel() {
  private val _uiState = MutableStateFlow(EditEventUiState())
  val uiState: StateFlow<EditEventUiState> = _uiState.asStateFlow()

  private val _effects = Channel<EditEventEffect>(Channel.BUFFERED)
  val effects = _effects.receiveAsFlow()

  init {
    if (eventId == null) {
      // Create mode: initialize with empty editable form
      _uiState.update {
        it.copy(
          originalEventId = null,
          isLoading = false,
          editedEvent =
            EditedEventData(
              name = "",
              startDate = null,
              startTime = null,
              venue = "",
              eventUrl = "",
              artists = "",
              flyerImageBytes = null,
            ),
        )
      }
      // If launched from OS share sheet, validate then show crop screen (re-encode happens after
      // crop)
      sharedImageProvider.consumePendingImage()?.let { bytes ->
        viewModelScope.launch {
          val imageBytes = ImageBytes(bytes)
          if (!validateImage(imageBytes)) return@launch
          _uiState.update { it.copy(cropMode = CropMode.NewImage(imageBytes)) }
        }
      }
    } else {
      // Edit mode: load existing event
      loadEvent()
    }
  }

  private fun loadEvent() {
    viewModelScope.launch {
      val event = repository.getEventById(eventId!!)
      if (event != null) {
        _uiState.update {
          it.copy(
            originalEventId = eventId,
            isLoading = false,
            editedEvent =
              EditedEventData(
                name = event.name,
                startDate = event.startDate,
                startTime = event.startTime,
                venue = event.venue ?: "",
                eventUrl = event.eventUrl ?: "",
                artists = event.artists.joinToString(", "),
                flyerImageBytes = event.flyerImageBytes,
              ),
          )
        }
      } else {
        _uiState.update { it.copy(isLoading = false, errorMessage = "Event not found") }
      }
    }
  }

  fun updateEditedEvent(editedEvent: EditedEventData) {
    _uiState.update { it.copy(editedEvent = editedEvent, errorMessage = null) }
  }

  fun saveEvent() {
    val editedData = _uiState.value.editedEvent ?: return
    val currentEventId = _uiState.value.originalEventId

    viewModelScope.launch {
      _uiState.update { it.copy(isSaving = true) }
      val startDate =
        editedData.startDate
          ?: run {
            _uiState.update { it.copy(isSaving = false, errorMessage = "Date is required.") }
            return@launch
          }
      try {
        if (currentEventId == null) {
          // Create new event — DB assigns the id, navigate to the new EventDetail
          val newEvent =
            Event(
              id = 0L,
              name = editedData.name,
              startDate = startDate,
              startTime = editedData.startTime,
              venue = editedData.venue.ifBlank { null },
              eventUrl = editedData.eventUrl.ifBlank { null },
              artists = editedData.artists.split(",").map { it.trim() }.filter { it.isNotBlank() },
              dateAdded = Clock.System.now(),
              flyerImageBytes = editedData.flyerImageBytes,
            )
          val newId = repository.saveEvent(newEvent)
          _effects.send(EditEventEffect.NavigateToEventDetail(newId))
        } else {
          // Update existing event
          val existingEvent = repository.getEventById(currentEventId)
          if (existingEvent != null) {
            val updatedEvent =
              existingEvent.copy(
                name = editedData.name,
                startDate = startDate,
                startTime = editedData.startTime,
                venue = editedData.venue.ifBlank { null },
                eventUrl = editedData.eventUrl.ifBlank { null },
                artists =
                  editedData.artists.split(",").map { it.trim() }.filter { it.isNotBlank() },
                flyerImageBytes = editedData.flyerImageBytes,
              )
            repository.updateEvent(updatedEvent)
            _effects.send(EditEventEffect.NavigateBack)
          } else {
            _uiState.update { it.copy(errorMessage = "Event no longer exists.") }
          }
        }
      } finally {
        _uiState.update { it.copy(isSaving = false) }
      }
    }
  }

  fun onImageSelected(imageFile: PlatformFile) {
    viewModelScope.launch {
      val imageBytes = ImageBytes(imageFile.readBytes())
      if (!validateImage(imageBytes)) return@launch
      _uiState.update { it.copy(cropMode = CropMode.NewImage(imageBytes), errorMessage = null) }
    }
  }

  fun onEditImageCrop() {
    _uiState.update { it.copy(cropMode = CropMode.EditExisting) }
  }

  fun onCropDone(croppedBytes: ImageBytes) {
    viewModelScope.launch {
      val reencoded =
        encodeImage(croppedBytes, 100 * 1024)
          ?: run {
            _uiState.update {
              it.copy(errorMessage = "Failed to encode image. Please try a different image.")
            }
            return@launch
          }
      _uiState.update {
        it.copy(editedEvent = it.editedEvent?.copy(flyerImageBytes = reencoded), cropMode = null)
      }
    }
  }

  fun onCropCancelled() {
    _uiState.update { it.copy(cropMode = null, errorMessage = null) }
  }

  /**
   * Checks [bytes] is a valid image format. Updates [_uiState] with an error and returns false if
   * not.
   */
  private fun validateImage(bytes: ImageBytes): Boolean {
    if (!bytes.isValidImage) {
      _uiState.update {
        it.copy(
          errorMessage =
            "Invalid image file. Please select a valid image (JPEG, PNG, GIF, BMP, or WebP)."
        )
      }
      return false
    }
    return true
  }

  /** Process the image to extract event details from it. */
  fun processFlyer() {
    val processedBytes = _uiState.value.editedEvent?.flyerImageBytes ?: return

    viewModelScope.launch {
      _uiState.update { it.copy(isProcessingFlyer = true, errorMessage = null) }

      // Re-encode to a smaller size for the Gemini request to reduce bandwidth and tokens
      val geminiBytes = encodeImage(processedBytes, 50 * BYTES_PER_KB) ?: processedBytes

      // Call AI to extract event details
      when (val result = processFlyerUseCase(geminiBytes)) {
        is ProcessFlyerResult.Success -> {
          val processedEvent = result.event
          _uiState.update { state ->
            val existingEvent = state.editedEvent ?: EditedEventData()
            state.copy(
              isProcessingFlyer = false,
              // Update the edited event with extracted data, only overwriting fields that have data
              editedEvent = existingEvent.mergeWithProcessedEvent(processedEvent, processedBytes),
              errorMessage = null,
            )
          }
        }
        is ProcessFlyerResult.Error -> {
          _uiState.update { it.copy(isProcessingFlyer = false, errorMessage = result.message) }
        }
      }
    }
  }

  fun replaceImage() {
    // Clear the current image so user can select a new one
    val currentEditedEvent = _uiState.value.editedEvent ?: return
    _uiState.update { it.copy(editedEvent = currentEditedEvent.copy(flyerImageBytes = null)) }
  }
}
