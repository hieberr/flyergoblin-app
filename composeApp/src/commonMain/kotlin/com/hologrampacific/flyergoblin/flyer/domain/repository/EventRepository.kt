package com.hologrampacific.flyergoblin.flyer.domain.repository

import com.hologrampacific.flyergoblin.flyer.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
  suspend fun getAllEvents(): List<Event>

  suspend fun getEventById(id: Long): Event?

  suspend fun saveEvent(event: Event): Long

  suspend fun updateEvent(event: Event)

  suspend fun deleteEvent(id: Long)

  fun observeEvents(): Flow<List<Event>>

  fun observeEventById(id: Long): Flow<Event?>
}
