package com.hologrampacific.learnkmp.flyer.presentation.flyer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hologrampacific.learnkmp.flyer.domain.model.Event
import org.koin.compose.viewmodel.koinViewModel
import com.hologrampacific.learnkmp.flyer.presentation.AddEvent
import com.hologrampacific.learnkmp.flyer.presentation.EventDetail
import com.hologrampacific.learnkmp.presentation.Navigator
import kotlin.time.Clock

@Composable
fun FlyerScreen(
  navigator: Navigator,
  viewModel: FlyerViewModel = koinViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()

  FlyerScreenContent(
    uiState = uiState,
    onSortOptionChange = viewModel::setSortOption,
    onEventClick = { eventId -> navigator.goTo(EventDetail(eventId)) },
    onAddEventClick = { navigator.goTo(AddEvent) },
  )
}

@Composable
fun FlyerScreenContent(
  uiState: FlyerUiState,
  onSortOptionChange: (SortOption) -> Unit,
  onEventClick: (String) -> Unit,
  onAddEventClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
      Text(
        text = "Events",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
      )

      Spacer(modifier = Modifier.height(16.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

      Spacer(modifier = Modifier.height(16.dp))

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
          verticalArrangement = Arrangement.spacedBy(12.dp),
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
      modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
    ) {
      Text("+", style = MaterialTheme.typography.headlineMedium)
    }
  }
}

@Composable
@Preview
fun FlyerScreenPreview() {
  MaterialTheme {
    FlyerScreenContent(
      uiState =
        FlyerUiState(
          events =
            listOf(
              Event(
                id = "1",
                name = "Summer Music Festival",
                startDate = kotlinx.datetime.LocalDate(2024, 7, 15),
                startTime = kotlinx.datetime.LocalTime(19, 0),
                venue = "Golden Gate Park",
                artists = listOf("The Headliners", "DJ Sunset", "Acoustic Soul"),
                dateAdded = Clock.System.now(),
              ),
              Event(
                id = "2",
                name = "Jazz Night",
                startDate = kotlinx.datetime.LocalDate(2024, 7, 20),
                startTime = kotlinx.datetime.LocalTime(20, 30),
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
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = event.name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row {
        Text(
          text = event.startDate.toString(),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        if (event.startTime != null) {
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = event.startTime.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      if (event.venue != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = event.venue,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (event.artists.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = event.artists.joinToString(", "),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
