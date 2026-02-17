package com.hologrampacific.flyergoblin.flyer.domain.model

import com.hologrampacific.flyergoblin.AppTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class EventTest : AppTest() {

  private val baseEvent =
    Event(
      id = "test-id",
      name = "Test Event",
      startDate = LocalDate(2026, 3, 15),
      startTime = LocalTime(20, 0),
      venue = "Test Venue",
      eventUrl = "https://example.com",
      artists = listOf("Artist 1", "Artist 2"),
      dateAdded = Instant.fromEpochMilliseconds(1706832000000),
      flyerImageBytes = byteArrayOf(1, 2, 3, 4, 5),
    )

  @Test
  fun testEqualsWithSameValues() {
    val event1 = baseEvent.copy()
    val event2 = baseEvent.copy()

    assertEquals(event1, event2)
    assertEquals(event1.hashCode(), event2.hashCode())
  }

  @Test
  fun testEqualsWithSameReference() {
    val event = baseEvent.copy()

    assertEquals(event, event)
  }

  @Test
  fun testNotEqualsWithDifferentId() {
    val event1 = baseEvent.copy(id = "id-1")
    val event2 = baseEvent.copy(id = "id-2")

    assertNotEquals(event1, event2)
  }

  @Test
  fun testNotEqualsWithDifferentName() {
    val event1 = baseEvent.copy(name = "Event 1")
    val event2 = baseEvent.copy(name = "Event 2")

    assertNotEquals(event1, event2)
  }

  @Test
  fun testNotEqualsWithDifferentDate() {
    val event1 = baseEvent.copy(startDate = LocalDate(2026, 3, 15))
    val event2 = baseEvent.copy(startDate = LocalDate(2026, 3, 16))

    assertNotEquals(event1, event2)
  }

  @Test
  fun testNotEqualsWithDifferentTime() {
    val event1 = baseEvent.copy(startTime = LocalTime(20, 0))
    val event2 = baseEvent.copy(startTime = LocalTime(21, 0))

    assertNotEquals(event1, event2)
  }

  @Test
  fun testNotEqualsWithDifferentVenue() {
    val event1 = baseEvent.copy(venue = "Venue 1")
    val event2 = baseEvent.copy(venue = "Venue 2")

    assertNotEquals(event1, event2)
  }

  @Test
  fun testNotEqualsWithDifferentUrl() {
    val event1 = baseEvent.copy(eventUrl = "https://example1.com")
    val event2 = baseEvent.copy(eventUrl = "https://example2.com")

    assertNotEquals(event1, event2)
  }

  @Test
  fun testNotEqualsWithDifferentArtists() {
    val event1 = baseEvent.copy(artists = listOf("Artist 1"))
    val event2 = baseEvent.copy(artists = listOf("Artist 2"))

    assertNotEquals(event1, event2)
  }

  @Test
  fun testNotEqualsWithDifferentDateAdded() {
    val event1 = baseEvent.copy(dateAdded = Instant.fromEpochMilliseconds(1706832000000))
    val event2 = baseEvent.copy(dateAdded = Instant.fromEpochMilliseconds(1706832001000))

    assertNotEquals(event1, event2)
  }

  @Test
  fun testEqualsWithSameFlyerImageBytes() {
    val bytes = byteArrayOf(1, 2, 3, 4, 5)
    val event1 = baseEvent.copy(flyerImageBytes = bytes)
    val event2 = baseEvent.copy(flyerImageBytes = bytes.copyOf())

    assertEquals(event1, event2)
    assertEquals(event1.hashCode(), event2.hashCode())
  }

  @Test
  fun testNotEqualsWithDifferentFlyerImageBytes() {
    val event1 = baseEvent.copy(flyerImageBytes = byteArrayOf(1, 2, 3))
    val event2 = baseEvent.copy(flyerImageBytes = byteArrayOf(4, 5, 6))

    assertNotEquals(event1, event2)
  }

  @Test
  fun testEqualsWithNullFlyerImageBytes() {
    val event1 = baseEvent.copy(flyerImageBytes = null)
    val event2 = baseEvent.copy(flyerImageBytes = null)

    assertEquals(event1, event2)
  }

  @Test
  fun testNotEqualsWithNullAndNonNullFlyerImageBytes() {
    val event1 = baseEvent.copy(flyerImageBytes = null)
    val event2 = baseEvent.copy(flyerImageBytes = byteArrayOf(1, 2, 3))

    assertNotEquals(event1, event2)
  }

  @Test
  fun testEqualsWithNullOptionalFields() {
    val event1 =
      baseEvent.copy(
        startDate = null,
        startTime = null,
        venue = null,
        eventUrl = null,
        flyerImageBytes = null,
      )
    val event2 =
      baseEvent.copy(
        startDate = null,
        startTime = null,
        venue = null,
        eventUrl = null,
        flyerImageBytes = null,
      )

    assertEquals(event1, event2)
  }

  @Test
  fun testNotEqualsWithNull() {
    val event = baseEvent.copy()

    assertFalse(event.equals(null))
  }

  @Test
  fun testNotEqualsWithDifferentType() {
    val event = baseEvent.copy()

    assertFalse(event.equals("Not an Event"))
  }

  @Test
  fun testHashCodeConsistency() {
    val event = baseEvent.copy()

    assertEquals(event.hashCode(), event.hashCode())
  }
}
