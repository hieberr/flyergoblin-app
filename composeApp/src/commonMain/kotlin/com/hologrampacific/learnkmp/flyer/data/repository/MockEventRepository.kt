package com.hologrampacific.learnkmp.flyer.data.repository

import com.hologrampacific.learnkmp.flyer.domain.model.Event
import com.hologrampacific.learnkmp.flyer.domain.repository.EventRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object MockEventRepository : EventRepository {
  private val _events = MutableStateFlow(createSampleEvents())
  private val eventsFlow = _events.asStateFlow()

  override suspend fun getAllEvents(): List<Event> {
    return _events.value
  }

  override suspend fun getEventById(id: String): Event? {
    return _events.value.find { it.id == id }
  }

  override suspend fun saveEvent(event: Event) {
    _events.update { it + event }
  }

  override suspend fun updateEvent(event: Event) {
    _events.update { it.map { existing -> if (existing.id == event.id) event else existing } }
  }

  override suspend fun deleteEvent(id: String) {
    _events.update { it.filter { event -> event.id != id } }
  }

  override fun observeEvents(): Flow<List<Event>> {
    return eventsFlow
  }

  private fun createSampleEvents(): List<Event> {
    return listOf(
      Event(
        id = Event.generateId(),
        name = "Summer Jazz Festival",
        startDate = kotlinx.datetime.LocalDate(2026, 7, 15),
        startTime = kotlinx.datetime.LocalTime(19, 0),
        venue = "Blue Note Jazz Club",
        eventUrl = "https://example.com/summer-jazz",
        artists = listOf("The Jazz Trio", "Sarah Vocals", "Mike on Sax"),
        dateAdded = Clock.System.now(),
      ),
      Event(
        id = Event.generateId(),
        name = "Electronic Music Night",
        startDate = kotlinx.datetime.LocalDate(2026, 8, 20),
        startTime = kotlinx.datetime.LocalTime(22, 0),
        venue = "The Warehouse",
        eventUrl = "https://example.com/electronic-night",
        artists = listOf("Hologram Pacific", "goth grandpa", "opiuo", "Neon Lights"),
        dateAdded = Clock.System.now() - 2.days,
      ),
      Event(
        id = Event.generateId(),
        name = "Acoustic Showcase",
        startDate = kotlinx.datetime.LocalDate(2026, 6, 10),
        startTime = null,
        venue = "Coffee House Stage",
        eventUrl = null,
        artists = listOf("Emma String", "Tom Guitar"),
        dateAdded = Clock.System.now() - 5.days,
      ),
    )
  }
}
