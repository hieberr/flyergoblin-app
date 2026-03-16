package com.hologrampacific.flyergoblin.flyer.presentation.events

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hologrampacific.flyergoblin.flyer.domain.model.Event
import com.hologrampacific.flyergoblin.flyer.presentation.EditEvent
import com.hologrampacific.flyergoblin.flyer.presentation.EventDetail
import com.hologrampacific.flyergoblin.presentation.Navigator
import com.hologrampacific.flyergoblin.presentation.Ui
import com.hologrampacific.flyergoblin.presentation.components.DevMenu
import com.hologrampacific.flyergoblin.presentation.components.DevMenuTestSnackbarErrorText
import com.hologrampacific.flyergoblin.presentation.components.SwipeToDeleteBox
import com.hologrampacific.flyergoblin.presentation.theme.AppTheme
import com.hologrampacific.flyergoblin.presentation.util.decodeImageBitmap
import com.hologrampacific.flyergoblin.presentation.util.formattedString
import flyergoblin.composeapp.generated.resources.Res
import flyergoblin.composeapp.generated.resources.add_24px
import flyergoblin.composeapp.generated.resources.goblin_black
import flyergoblin.composeapp.generated.resources.sort_24px
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EventsScreen(navigator: Navigator, viewModel: EventsViewModel = koinViewModel()) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { errorMessage ->
      snackbarHostState.showSnackbar(message = errorMessage, withDismissAction = true)
      viewModel.clearError()
    }
  }

  EventsScreenContent(
    uiState = uiState,
    snackbarHostState = snackbarHostState,
    onSortOptionChange = viewModel::setSortOption,
    onShowPastEventsChange = viewModel::setShowPastEvents,
    isPastEvent = viewModel::isPastEvent,
    onEventClick = { eventId -> navigator.goTo(EventDetail(eventId)) },
    onAddEventClick = { navigator.goTo(EditEvent(null)) },
    onDeleteEvent = viewModel::deleteEvent,
    onTriggerTestError = viewModel::triggerTestError,
  )
}

@Composable
fun EventsScreenContent(
  uiState: EventsUiState,
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
  onSortOptionChange: (SortOption) -> Unit,
  onShowPastEventsChange: (Boolean) -> Unit,
  isPastEvent: (Event) -> Boolean = { false },
  onEventClick: (Long) -> Unit,
  onAddEventClick: () -> Unit,
  onDeleteEvent: (Long) -> Unit,
  onTriggerTestError: () -> Unit = {},
  modifier: Modifier = Modifier,
) {

  Surface(modifier = modifier.fillMaxSize()) {
    Box(modifier = Modifier.fillMaxSize()) {
      Column(modifier = Modifier.fillMaxSize().padding(horizontal = Ui.unit)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Events",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
          )
          Spacer(Modifier.weight(1f))
          Icon(
            painter = painterResource(Res.drawable.goblin_black),
            contentDescription = null,
            modifier = Modifier.size(Ui.unit * 3),
            tint = MaterialTheme.colorScheme.onSurface,
          )
          DevMenu {
            DropdownMenuItem(
              text = { DevMenuTestSnackbarErrorText() },
              onClick = {
                dismiss()
                onTriggerTestError()
              },
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(Ui.halfUnit),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            painter = painterResource(Res.drawable.sort_24px),
            contentDescription = "Sort",
            modifier = Modifier.size(Ui.standardIconSize),
            tint = MaterialTheme.colorScheme.onSurface,
          )
          FilterChip(
            selected = uiState.sortOption == SortOption.BY_EVENT_DATE,
            onClick = { onSortOptionChange(SortOption.BY_EVENT_DATE) },
            label = { Text(text = "By Event Date", style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.weight(1f),
          )
          FilterChip(
            selected = uiState.sortOption == SortOption.BY_DATE_ADDED,
            onClick = { onSortOptionChange(SortOption.BY_DATE_ADDED) },
            label = { Text(text = "By Date Added", style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.weight(1f),
          )
        }

        FilterChip(
          selected = uiState.showPastEvents,
          onClick = { onShowPastEventsChange(!uiState.showPastEvents) },
          label = { Text(text = "Show Past Events", style = MaterialTheme.typography.labelMedium) },
        )

        // Tracks which item (if any) has its delete button revealed.
        var openItemId by remember { mutableStateOf<Long?>(null) }
        LaunchedEffect(uiState.sortOption, uiState.showPastEvents) { openItemId = null }

        if (uiState.events.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
              text = "No events yet. Tap + to add one!",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Ui.unit),
            // give a bunch of extra padding so we can scroll past the bottom item.
            // It feels constraining to be unable to just scroll enough to get the bottom item on
            // the screen.
            contentPadding = PaddingValues(bottom = Ui.unit * 4),
          ) {
            items(uiState.events, key = { it.id }) { event ->
              val isOpen = openItemId == event.id
              SwipeToDeleteBox(
                isOpen = isOpen,
                onSwipeOpen = { openItemId = event.id },
                onDelete = {
                  openItemId = null
                  onDeleteEvent(event.id)
                },
                modifier = Modifier.animateItem(),
              ) {
                EventCard(
                  event = event,
                  isPast = isPastEvent(event),
                  onClick = {
                    if (openItemId != null) openItemId = null else onEventClick(event.id)
                  },
                )
              }
            }
          }
        }
      }

      SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))

      FloatingActionButton(
        onClick = onAddEventClick,
        modifier = Modifier.align(Alignment.BottomEnd).padding(Ui.unit),
        containerColor = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary,
      ) {
        Icon(
          painter = painterResource(Res.drawable.add_24px),
          contentDescription = "Add Event",
          modifier = Modifier.size(Ui.standardIconSize),
        )
      }
    }
  }
}

@Composable
@Preview
fun EventsScreenPreview() {
  AppTheme {
    EventsScreenContent(
      uiState =
        EventsUiState(
          events =
            listOf(
              Event(
                id = 1L,
                name = "Summer Music Festival",
                startDate = LocalDate(2024, 7, 15),
                startTime = LocalTime(19, 0),
                venue = "Golden Gate Park",
                artists = listOf("The Headliners", "DJ Sunset", "Acoustic Soul"),
                dateAdded = Clock.System.now(),
              ),
              Event(
                id = 2L,
                name = "Jazz Night",
                startDate = LocalDate(2024, 7, 20),
                startTime = LocalTime(20, 30),
                venue = "Blue Note",
                artists = listOf("Jazz Quartet"),
                dateAdded = Clock.System.now(),
              ),
            ),
          sortOption = SortOption.BY_DATE_ADDED,
        ),
      onSortOptionChange = {},
      onShowPastEventsChange = {},
      onEventClick = {},
      onAddEventClick = {},
      onDeleteEvent = {},
    )
  }
}

@Composable
private fun EventCard(event: Event, isPast: Boolean = false, onClick: () -> Unit) {
  Card(
    modifier = Modifier.clickable(onClick = onClick).fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Box {
      Row(modifier = Modifier.height(IntrinsicSize.Max)) {
        Column(modifier = Modifier.weight(1f).padding(Ui.unit)) {
          Text(
            text = event.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )

          Spacer(modifier = Modifier.height(Ui.halfUnit))

          FlowRow(horizontalArrangement = Arrangement.spacedBy(Ui.halfUnit)) {
            if (event.startDate != null) {
              Text(
                text = event.startDate.formattedString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
              )
            }
            if (event.startTime != null) {
              Text(
                text = event.startTime.formattedString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
              )
            }
          }

          if (event.venue != null) {
            Spacer(modifier = Modifier.height(Ui.unit / 4))
            Text(
              text = event.venue,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          if (event.artists.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Ui.unit / 4))
            Text(
              text = event.artists.joinToString(", "),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Box(
          modifier =
            Modifier.width(Ui.unit * 7)
              .fillMaxHeight()
              .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
          if (event.flyerImageBytes != null) {
            val imageBitmap =
              remember(event.flyerImageBytes) { decodeImageBitmap(event.flyerImageBytes) }
            if (imageBitmap != null) {
              Image(
                bitmap = imageBitmap,
                contentDescription = "Flyer for ${event.name}",
                modifier =
                  Modifier.matchParentSize()
                    .clip(RoundedCornerShape(topEnd = Ui.unit, bottomEnd = Ui.unit)),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
              )
            }
          }
        }
      }
      if (isPast) {
        Box(
          modifier =
            Modifier.matchParentSize()
              .clip(CardDefaults.shape)
              .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        )
      }
    }
  }
}
