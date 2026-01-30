package com.hologrampacific.learnkmp.flyer.domain.repository

import com.hologrampacific.learnkmp.flyer.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
  suspend fun getAllEvents(): List<Event>

  suspend fun getEventById(id: String): Event?

  suspend fun saveEvent(event: Event)

  suspend fun updateEvent(event: Event)

  suspend fun deleteEvent(id: String)

  fun observeEvents(): Flow<List<Event>>
}
