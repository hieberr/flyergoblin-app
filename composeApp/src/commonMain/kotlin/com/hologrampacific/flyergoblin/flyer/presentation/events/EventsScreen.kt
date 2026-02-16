package com.hologrampacific.flyergoblin.flyer.presentation.events

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.hologrampacific.flyergoblin.util.decodeImageBitmap
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EventsScreen(navigator: Navigator, viewModel: EventsViewModel = koinViewModel()) {
  val uiState by viewModel.uiState.collectAsState()

  EventsScreenContent(
    uiState = uiState,
    onSortOptionChange = viewModel::setSortOption,
    onEventClick = { eventId -> navigator.goTo(EventDetail(eventId)) },
    onAddEventClick = { navigator.goTo(EditEvent(null)) },
  )
}

@Composable
fun EventsScreenContent(
  uiState: EventsUiState,
  onSortOptionChange: (SortOption) -> Unit,
  onEventClick: (String) -> Unit,
  onAddEventClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Column(modifier = Modifier.fillMaxSize().padding(Ui.unit)) {
      Text(
        text = "Events",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
      )

      Ui.SpacerUnitHeight()

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Ui.halfUnit),
      ) {
        FilterChip(
          selected = uiState.sortOption == SortOption.BY_DATE_ADDED,
          onClick = { onSortOptionChange(SortOption.BY_DATE_ADDED) },
          label = { Text("By Date Added") },
        )
        FilterChip(
          selected = uiState.sortOption == SortOption.BY_EVENT_DATE,
          onClick = { onSortOptionChange(SortOption.BY_EVENT_DATE) },
          label = { Text("By Event Date") },
        )
      }

      Ui.SpacerUnitHeight()

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
          contentPadding = PaddingValues(bottom = 80.dp),
        ) {
          items(uiState.events, key = { it.id }) { event ->
            EventCard(event = event, onClick = { onEventClick(event.id) })
          }
        }
      }
    }

    FloatingActionButton(
      onClick = onAddEventClick,
      modifier = Modifier.align(Alignment.BottomEnd).padding(Ui.unit),
    ) {
      Text("+", style = MaterialTheme.typography.headlineMedium)
    }
  }
}

@Composable
@Preview
fun EventsScreenPreview() {
  MaterialTheme {
    EventsScreenContent(
      uiState =
        EventsUiState(
          events =
            listOf(
              Event(
                id = "1",
                name = "Summer Music Festival",
                startDate = LocalDate(2024, 7, 15),
                startTime = LocalTime(19, 0),
                venue = "Golden Gate Park",
                artists = listOf("The Headliners", "DJ Sunset", "Acoustic Soul"),
                dateAdded = Clock.System.now(),
              ),
              Event(
                id = "2",
                name = "Jazz Night",
                startDate = kotlinx.datetime.LocalDate(2024, 7, 20),
                startTime = LocalTime(20, 30),
                venue = "Blue Note",
                artists = listOf("Jazz Quartet"),
                dateAdded = Clock.System.now(),
              ),
            ),
          sortOption = SortOption.BY_DATE_ADDED,
        ),
      onSortOptionChange = {},
      onEventClick = {},
      onAddEventClick = {},
    )
  }
}

@Composable
private fun EventCard(event: Event, onClick: () -> Unit) {
  Card(
    modifier = Modifier.clickable(onClick = onClick).fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
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

        Row {
          Text(
            text = event.startDate.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
          )
          if (event.startTime != null) {
            Spacer(modifier = Modifier.width(Ui.halfUnit))
            Text(
              text = event.startTime.toString(),
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
            .background(MaterialTheme.colorScheme.surfaceDim)
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
                  .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)),
              contentScale = ContentScale.Crop,
              alignment = Alignment.TopCenter,
            )
          }
        }
      }
    }
  }
}
