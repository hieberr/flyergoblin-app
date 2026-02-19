package com.hologrampacific.flyergoblin.flyer.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.hologrampacific.flyergoblin.db.AppDatabase
import com.hologrampacific.flyergoblin.db.EventEntity
import com.hologrampacific.flyergoblin.flyer.domain.model.Event
import com.hologrampacific.flyergoblin.flyer.domain.repository.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SqlDelightEventRepository(private val database: AppDatabase) : EventRepository {

  private val queries = database.eventEntityQueries

  override suspend fun getAllEvents(): List<Event> =
    queries.observeAllEvents().executeAsList().map { it.toEvent() }

  override suspend fun getEventById(id: Long): Event? =
    queries.getEventById(id).executeAsOneOrNull()?.toEvent()

  override suspend fun saveEvent(event: Event): Long =
    // The transaction guarantees that last_insert_rowid() returns the rowid assigned to
    // our INSERT — no other insert can interleave on the same connection within a transaction.
    database.transactionWithResult {
      queries.insertEvent(
        name = event.name,
        startDate = event.startDate,
        startTime = event.startTime,
        venue = event.venue,
        eventUrl = event.eventUrl,
        artists = event.artists,
        dateAdded = event.dateAdded,
        flyerImageBytes = event.flyerImageBytes,
      )
      queries.lastInsertRowId().executeAsOne()
    }

  override suspend fun updateEvent(event: Event) {
    queries.updateEvent(
      name = event.name,
      startDate = event.startDate,
      startTime = event.startTime,
      venue = event.venue,
      eventUrl = event.eventUrl,
      artists = event.artists,
      flyerImageBytes = event.flyerImageBytes,
      id = event.id,
    )
  }

  override suspend fun deleteEvent(id: Long) {
    queries.deleteEvent(id)
  }

  override fun observeEvents(): Flow<List<Event>> =
    queries.observeAllEvents().asFlow().mapToList(Dispatchers.IO).map { entities ->
      entities.map { it.toEvent() }
    }

  private fun EventEntity.toEvent(): Event =
    Event(
      id = id,
      name = name,
      startDate = startDate,
      startTime = startTime,
      venue = venue,
      eventUrl = eventUrl,
      artists = artists,
      dateAdded = dateAdded,
      flyerImageBytes = flyerImageBytes,
    )
}
