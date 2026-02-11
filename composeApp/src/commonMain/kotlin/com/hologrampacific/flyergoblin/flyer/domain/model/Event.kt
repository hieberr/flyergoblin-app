package com.hologrampacific.flyergoblin.flyer.domain.model

import com.benasher44.uuid.uuid4
import com.hologrampacific.flyergoblin.util.ByteArraySerializer
import com.hologrampacific.flyergoblin.util.InstantSerializer
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class Event(
  val id: String,
  val name: String,
  val startDate: LocalDate? = null,
  val startTime: LocalTime? = null,
  val venue: String? = null,
  val eventUrl: String? = null,
  val artists: List<String> = emptyList(),
  @Serializable(with = InstantSerializer::class) val dateAdded: Instant,
  @Serializable(with = ByteArraySerializer::class) val flyerImageBytes: ByteArray? = null,
) {
  companion object {
    fun generateId(): String = uuid4().toString()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as Event

    if (id != other.id) return false
    if (name != other.name) return false
    if (startDate != other.startDate) return false
    if (startTime != other.startTime) return false
    if (venue != other.venue) return false
    if (eventUrl != other.eventUrl) return false
    if (artists != other.artists) return false
    if (dateAdded != other.dateAdded) return false
    if (flyerImageBytes != null) {
      if (other.flyerImageBytes == null) return false
      if (!flyerImageBytes.contentEquals(other.flyerImageBytes)) return false
    } else if (other.flyerImageBytes != null) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + (startDate?.hashCode() ?: 0)
    result = 31 * result + (startTime?.hashCode() ?: 0)
    result = 31 * result + (venue?.hashCode() ?: 0)
    result = 31 * result + (eventUrl?.hashCode() ?: 0)
    result = 31 * result + artists.hashCode()
    result = 31 * result + dateAdded.hashCode()
    result = 31 * result + (flyerImageBytes?.contentHashCode() ?: 0)
    return result
  }
}
