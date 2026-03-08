package com.hologrampacific.flyergoblin.flyer.domain.model

import com.hologrampacific.flyergoblin.util.ImageBytes
import com.hologrampacific.flyergoblin.util.ImageBytesSerializer
import com.hologrampacific.flyergoblin.util.InstantSerializer
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class Event(
  val id: Long = 0L,
  val name: String,
  val startDate: LocalDate? = null,
  val startTime: LocalTime? = null,
  val venue: String? = null,
  val eventUrl: String? = null,
  val artists: List<String> = emptyList(),
  @Serializable(with = InstantSerializer::class) val dateAdded: Instant,
  @Serializable(with = ImageBytesSerializer::class) val flyerImageBytes: ImageBytes? = null,
)
