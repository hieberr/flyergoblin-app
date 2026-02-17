package com.hologrampacific.flyergoblin.flyer.presentation.event

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.hologrampacific.flyergoblin.flyer.domain.model.Event
import com.hologrampacific.flyergoblin.presentation.Navigator
import com.hologrampacific.flyergoblin.presentation.Ui
import com.hologrampacific.flyergoblin.presentation.components.TopAppBarScreenWithCenteredContent
import com.hologrampacific.flyergoblin.util.decodeImageBitmap
import kotlin.time.Instant
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
  navigator: Navigator,
  eventId: String?,
  viewModel: EventDetailViewModel = koinViewModel { parametersOf(eventId) },
) {
  val uiState by viewModel.uiState.collectAsState()
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

  // Refresh event when screen resumes (e.g., when returning from edit screen)
  DisposableEffect(lifecycleOwner, eventId) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshEvent()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(Unit) {
    viewModel.effects.collect { effect ->
      when (effect) {
        EventDetailEffect.NavigateBack -> navigator.goBack()
      }
    }
  }

  EventDetailScreenContent(
    uiState = uiState,
    navigator = navigator,
    eventId = eventId,
    onBackClicked = { navigator.goBack() },
    onDeleteEvent = { viewModel.deleteEvent() },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreenContent(
  uiState: EventDetailUiState,
  navigator: Navigator,
  eventId: String?,
  onBackClicked: () -> Unit,
  onDeleteEvent: () -> Unit,
) {
  val showDeleteDialog = remember { mutableStateOf(false) }

  TopAppBarScreenWithCenteredContent(
    appBarTitle = "Event Details",
    onBackClicked = onBackClicked,
    navBarActions = {
      if (uiState.event != null) {
        TextButton(
          onClick = {
            navigator.goTo(
              com.hologrampacific.flyergoblin.flyer.presentation.EditEvent(eventId = eventId)
            )
          }
        ) {
          Text("Edit")
        }
        TextButton(onClick = { showDeleteDialog.value = true }) { Text("Delete") }
      }
    },
  ) {
    if (uiState.isLoading) {
      CircularProgressIndicator()
    } else if (uiState.event == null) {
      Text("Event not found")
    } else {
      ReadOnlyEventContent(event = uiState.event, navigator = navigator)
    }
  }

  if (showDeleteDialog.value) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog.value = false },
      title = { Text("Delete Event") },
      text = { Text("Are you sure you want to delete this event?") },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteDialog.value = false
            onDeleteEvent()
          }
        ) {
          Text("Delete")
        }
      },
      dismissButton = { TextButton(onClick = { showDeleteDialog.value = false }) { Text("Cancel") } },
    )
  }
}

@Composable
private fun FlyerImageOrPlaceholder(imageBytes: ByteArray?, modifier: Modifier = Modifier) {
  if (imageBytes != null) {
    FlyerImage(imageBytes = imageBytes, modifier = modifier)
  } else {
    // Show placeholder when no image is available
    Surface(
      modifier = modifier.fillMaxWidth().height(Ui.unit * 12),
      color = MaterialTheme.colorScheme.surfaceVariant,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(Ui.unit),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "No flyer image",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
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
      modifier = modifier,
      contentScale = ContentScale.FillWidth,
    )
  } else {
    // Show error state when image decoding fails
    Surface(
      modifier = modifier.height(Ui.unit * 12),
      color = MaterialTheme.colorScheme.errorContainer,
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(Ui.unit),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text("Unable to display flyer image", color = MaterialTheme.colorScheme.onErrorContainer)
      }
    }
  }
}

@Composable
private fun ReadOnlyEventContent(event: Event, navigator: Navigator) {
  Column(verticalArrangement = Arrangement.spacedBy(Ui.unit)) {
    FlyerImageOrPlaceholder(imageBytes = event.flyerImageBytes, modifier = Modifier.fillMaxWidth())

    DetailField(label = "Event Name", value = event.name)

    DetailField(label = "Date", value = event.startDate.toString())

    if (event.startTime != null) {
      DetailField(label = "Time", value = event.startTime.toString())
    }

    if (event.venue != null) {
      DetailField(label = "Venue", value = event.venue)
    }

    if (event.eventUrl != null) {
      DetailField(label = "Event URL", value = event.eventUrl)
    }

    if (event.artists.isNotEmpty()) {
      ClickableArtistsList(
        artists = event.artists,
        onArtistClick = { artistName ->
          navigator.goTo(
            com.hologrampacific.flyergoblin.flyer.presentation.ArtistDetail(artistName)
          )
        },
      )
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
    Spacer(modifier = Modifier.height(Ui.unit / 4))
    Text(text = value, style = MaterialTheme.typography.bodyLarge)
  }
}

@Composable
private fun ClickableArtistsList(artists: List<String>, onArtistClick: (String) -> Unit) {
  Column {
    Text(
      text = "Artists",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(Ui.unit / 4))
    artists.forEach { artistName ->
      Text(
        text = artistName,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable { onArtistClick(artistName) }.padding(vertical = Ui.unit / 4),
      )
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
          isLoading = false,
        ),
      navigator = Navigator.noOpNavigator,
      eventId = "preview-id",
      onBackClicked = {},
      onDeleteEvent = {},
    )
  }
}
