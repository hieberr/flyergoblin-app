package com.hologrampacific.learnkmp.flyer.presentation.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hologrampacific.learnkmp.flyer.domain.model.Event
import com.hologrampacific.learnkmp.presentation.Navigator
import com.hologrampacific.learnkmp.util.decodeImageBitmap
import kotlin.time.Instant
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
  navigator: Navigator,
  eventId: String,
  viewModel: EventDetailViewModel = koinViewModel { parametersOf(eventId) },
) {
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(Unit) {
    viewModel.effects.collect { effect ->
      when (effect) {
        EventDetailEffect.NavigateBack -> navigator.goBack()
      }
    }
  }

  EventDetailScreenContent(
    uiState = uiState,
    onBackClick = { navigator.goBack() },
    onStartEditing = { viewModel.startEditing() },
    onCancelEditing = { viewModel.cancelEditing() },
    onEventChange = { viewModel.updateEditedEvent(it) },
    onSaveEvent = { viewModel.saveEvent() },
    onDeleteEvent = { viewModel.deleteEvent() },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreenContent(
  uiState: EventDetailUiState,
  onBackClick: () -> Unit,
  onStartEditing: () -> Unit,
  onCancelEditing: () -> Unit,
  onEventChange: (EditedEventData) -> Unit,
  onSaveEvent: () -> Unit,
  onDeleteEvent: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showDeleteDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(if (uiState.isEditing) "Edit Event" else "Event Details") },
        navigationIcon = { TextButton(onClick = onBackClick) { Text("< Back") } },
        actions = {
          if (!uiState.isEditing && uiState.event != null) {
            TextButton(onClick = onStartEditing) { Text("Edit") }
            TextButton(onClick = { showDeleteDialog = true }) { Text("Delete") }
          }
        },
      )
    },
  ) { padding ->
    if (uiState.isLoading) {
      Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        CircularProgressIndicator()
      }
    } else if (uiState.event == null) {
      Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text("Event not found")
      }
    } else {
      if (uiState.isEditing) {
        uiState.editedEvent?.let { editedEvent ->
          EditEventContent(
            editedEvent = editedEvent,
            errorMessage = uiState.errorMessage,
            onEventChange = onEventChange,
            onSave = onSaveEvent,
            onCancel = onCancelEditing,
            modifier = Modifier.fillMaxSize().padding(padding),
          )
        }
          ?: run {
            // Error state: editing mode but no edited event data
            Column(
              modifier = Modifier.fillMaxSize().padding(padding),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
            ) {
              Text("Error: Unable to load event for editing")
              Spacer(modifier = Modifier.height(16.dp))
              TextButton(onClick = onBackClick) { Text("Go Back") }
            }
          }
      } else {
        ReadOnlyEventContent(
          event = uiState.event,
          modifier = Modifier.fillMaxSize().padding(padding),
        )
      }
    }
  }

  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("Delete Event") },
      text = { Text("Are you sure you want to delete this event?") },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteDialog = false
            onDeleteEvent()
          }
        ) {
          Text("Delete")
        }
      },
      dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
    )
  }
}

@Composable
private fun ReadOnlyEventContent(event: Event, modifier: Modifier = Modifier) {
  Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
    // Display flyer image or placeholder
    FlyerImageOrPlaceholder(
      imageBytes = event.flyerImageBytes,
      modifier = Modifier.requiredWidthIn(max = 600.dp)
    )
    Spacer(modifier = Modifier.height(24.dp))

    DetailField(label = "Event Name", value = event.name)
    Spacer(modifier = Modifier.height(16.dp))

    DetailField(label = "Date", value = event.startDate.toString())
    Spacer(modifier = Modifier.height(16.dp))

    if (event.startTime != null) {
      DetailField(label = "Time", value = event.startTime.toString())
      Spacer(modifier = Modifier.height(16.dp))
    }

    if (event.venue != null) {
      DetailField(label = "Venue", value = event.venue)
      Spacer(modifier = Modifier.height(16.dp))
    }

    if (event.eventUrl != null) {
      DetailField(label = "Event URL", value = event.eventUrl)
      Spacer(modifier = Modifier.height(16.dp))
    }

    if (event.artists.isNotEmpty()) {
      DetailField(label = "Artists", value = event.artists.joinToString("\n"))
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun FlyerImageOrPlaceholder(imageBytes: ByteArray?, modifier: Modifier = Modifier) {
  if (imageBytes != null) {
    FlyerImage(imageBytes = imageBytes, modifier = modifier)
  } else {
    // Show placeholder when no image is available
    Surface(
      modifier = modifier.fillMaxWidth().height(200.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "No flyer image",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun FlyerImage(imageBytes: ByteArray, modifier: Modifier = Modifier) {
  val imageBitmap = remember(imageBytes) { decodeImageBitmap(imageBytes) }

  if (imageBitmap != null) {
    Image(
      bitmap = imageBitmap,
      contentDescription = "Event flyer",
      modifier =
        modifier
          .fillMaxWidth()
          .aspectRatio(imageBitmap.width.toFloat() / imageBitmap.height.toFloat()),
      contentScale = ContentScale.Fit,
    )
  } else {
    // Show error state when image decoding fails
    Surface(
      modifier = modifier.fillMaxWidth().height(200.dp),
      color = MaterialTheme.colorScheme.errorContainer,
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text("Unable to display flyer image", color = MaterialTheme.colorScheme.onErrorContainer)
      }
    }
  }
}

@Composable
private fun DetailField(label: String, value: String) {
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = value, style = MaterialTheme.typography.bodyLarge)
  }
}

@Composable
private fun EditEventContent(
  editedEvent: EditedEventData,
  errorMessage: String?,
  onEventChange: (EditedEventData) -> Unit,
  onSave: () -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // Validation logic
  val isDateValid = editedEvent.startDate.matches(Regex("""\d{4}-\d{2}-\d{2}"""))
  val isTimeValid =
    editedEvent.startTime.isEmpty() || editedEvent.startTime.matches(Regex("""\d{2}:\d{2}"""))

  Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
    // Display flyer image or placeholder
    FlyerImageOrPlaceholder(
      imageBytes = editedEvent.flyerImageBytes,
      modifier = Modifier.requiredWidthIn(max = 600.dp)
    )
    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
      value = editedEvent.name,
      onValueChange = { onEventChange(editedEvent.copy(name = it)) },
      label = { Text("Event Name *") },
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
      value = editedEvent.startDate,
      onValueChange = { onEventChange(editedEvent.copy(startDate = it)) },
      label = { Text("Date (YYYY-MM-DD format) *") },
      isError = !isDateValid && editedEvent.startDate.isNotBlank(),
      supportingText =
        if (!isDateValid && editedEvent.startDate.isNotBlank()) {
          { Text("Format must be YYYY-MM-DD. Date validity checked on save.") }
        } else null,
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
      value = editedEvent.startTime,
      onValueChange = { onEventChange(editedEvent.copy(startTime = it)) },
      label = { Text("Time (HH:MM format)") },
      isError = !isTimeValid && editedEvent.startTime.isNotBlank(),
      supportingText =
        if (!isTimeValid && editedEvent.startTime.isNotBlank()) {
          { Text("Format must be HH:MM. Time validity checked on save.") }
        } else null,
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
      value = editedEvent.venue,
      onValueChange = { onEventChange(editedEvent.copy(venue = it)) },
      label = { Text("Venue") },
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
      value = editedEvent.eventUrl,
      onValueChange = { onEventChange(editedEvent.copy(eventUrl = it)) },
      label = { Text("Event URL") },
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
      value = editedEvent.artists,
      onValueChange = { onEventChange(editedEvent.copy(artists = it)) },
      label = { Text("Artists (comma-separated)") },
      modifier = Modifier.fillMaxWidth(),
      minLines = 3,
    )
    Spacer(modifier = Modifier.height(24.dp))

    errorMessage?.let { error ->
      Text(
        text = error,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
      )
      Spacer(modifier = Modifier.height(16.dp))
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
      Button(
        onClick = onSave,
        modifier = Modifier.weight(1f),
        enabled =
          editedEvent.name.isNotBlank() &&
            editedEvent.startDate.isNotBlank() &&
            isDateValid &&
            isTimeValid,
      ) {
        Text("Save")
      }
    }
  }
}

@Composable
@Preview
fun EventDetailScreenPreview() {
  MaterialTheme {
    EventDetailScreenContent(
      uiState =
        EventDetailUiState(
          event =
            Event(
              id = "preview-id",
              name = "Summer Music Festival",
              startDate = kotlinx.datetime.LocalDate(2024, 7, 15),
              startTime = kotlinx.datetime.LocalTime(19, 0),
              venue = "Golden Gate Park",
              eventUrl = "https://example.com/event",
              artists = listOf("The Headliners", "DJ Sunset", "Acoustic Soul"),
              dateAdded = Instant.fromEpochMilliseconds(1706832000000),
              flyerImageBytes = null,
            ),
          isEditing = false,
          editedEvent = null,
          isLoading = false,
        ),
      onBackClick = {},
      onStartEditing = {},
      onCancelEditing = {},
      onEventChange = {},
      onSaveEvent = {},
      onDeleteEvent = {},
    )
  }
}

@Composable
@Preview
fun EventDetailScreenEditPreview() {
  MaterialTheme {
    EventDetailScreenContent(
      uiState =
        EventDetailUiState(
          event =
            Event(
              id = "preview-id",
              name = "Summer Music Festival",
              startDate = kotlinx.datetime.LocalDate(2024, 7, 15),
              startTime = kotlinx.datetime.LocalTime(19, 0),
              venue = "Golden Gate Park",
              eventUrl = "https://example.com/event",
              artists = listOf("The Headliners", "DJ Sunset", "Acoustic Soul"),
              dateAdded = Instant.fromEpochMilliseconds(1706832000000),
              flyerImageBytes = null,
            ),
          isEditing = true,
          editedEvent =
            EditedEventData(
              name = "Summer Music Festival",
              startDate = "2024-07-15",
              startTime = "19:00",
              venue = "Golden Gate Park",
              eventUrl = "https://example.com/event",
              artists = "The Headliners, DJ Sunset, Acoustic Soul",
            ),
          isLoading = false,
        ),
      onBackClick = {},
      onStartEditing = {},
      onCancelEditing = {},
      onEventChange = {},
      onSaveEvent = {},
      onDeleteEvent = {},
    )
  }
}
