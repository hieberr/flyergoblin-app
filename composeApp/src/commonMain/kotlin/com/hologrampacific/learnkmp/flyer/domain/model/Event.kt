package com.hologrampacific.learnkmp.flyer.domain.model

import com.benasher44.uuid.uuid4
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class Event(
  val id: String,
  val name: String,
  val startDate: LocalDate,
  val startTime: LocalTime? = null,
  val venue: String? = null,
  val eventUrl: String? = null,
  val artists: List<String> = emptyList(),
  @Serializable(with = InstantSerializer::class) val dateAdded: Instant,
) {
  companion object {
    fun generateId(): String = uuid4().toString()
  }
}
