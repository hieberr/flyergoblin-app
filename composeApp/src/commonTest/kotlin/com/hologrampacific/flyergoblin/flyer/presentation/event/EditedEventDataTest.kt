package com.hologrampacific.flyergoblin.flyer.presentation.event

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.domain.model.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class EditedEventDataTest : AppTest() {

  private val flyerBytes = byteArrayOf(1, 2, 3)
  private val newFlyerBytes = byteArrayOf(4, 5, 6)

  private val baseEvent =
    Event(
      name = "",
      dateAdded = Instant.fromEpochMilliseconds(0),
    )

  private fun eventWith(
    name: String = "",
    startDate: LocalDate? = null,
    startTime: LocalTime? = null,
    venue: String? = null,
    eventUrl: String? = null,
    artists: List<String> = emptyList(),
  ) = baseEvent.copy(name = name, startDate = startDate, startTime = startTime, venue = venue, eventUrl = eventUrl, artists = artists)

  @Test
  fun `test mergeWithProcessedEvent overwrites name when event name is non-blank`() {
    val existing = EditedEventData(name = "Old Name")
    val result = existing.mergeWithProcessedEvent(eventWith(name = "New Name"), newFlyerBytes)
    assertEquals("New Name", result.name)
  }

  @Test
  fun `test mergeWithProcessedEvent keeps existing name when event name is blank`() {
    val existing = EditedEventData(name = "Old Name")
    val result = existing.mergeWithProcessedEvent(eventWith(name = ""), newFlyerBytes)
    assertEquals("Old Name", result.name)
  }

  @Test
  fun `test mergeWithProcessedEvent overwrites startDate when event startDate is non-null`() {
    val existing = EditedEventData(startDate = LocalDate(2025, 1, 1))
    val newDate = LocalDate(2026, 6, 15)
    val result = existing.mergeWithProcessedEvent(eventWith(startDate = newDate), newFlyerBytes)
    assertEquals(newDate, result.startDate)
  }

  @Test
  fun `test mergeWithProcessedEvent keeps existing startDate when event startDate is null`() {
    val existingDate = LocalDate(2025, 1, 1)
    val existing = EditedEventData(startDate = existingDate)
    val result = existing.mergeWithProcessedEvent(eventWith(startDate = null), newFlyerBytes)
    assertEquals(existingDate, result.startDate)
  }

  @Test
  fun `test mergeWithProcessedEvent overwrites startTime when event startTime is non-null`() {
    val existing = EditedEventData(startTime = LocalTime(10, 0))
    val newTime = LocalTime(21, 30)
    val result = existing.mergeWithProcessedEvent(eventWith(startTime = newTime), newFlyerBytes)
    assertEquals(newTime, result.startTime)
  }

  @Test
  fun `test mergeWithProcessedEvent keeps existing startTime when event startTime is null`() {
    val existingTime = LocalTime(10, 0)
    val existing = EditedEventData(startTime = existingTime)
    val result = existing.mergeWithProcessedEvent(eventWith(startTime = null), newFlyerBytes)
    assertEquals(existingTime, result.startTime)
  }

  @Test
  fun `test mergeWithProcessedEvent overwrites venue when event venue is non-blank`() {
    val existing = EditedEventData(venue = "Old Venue")
    val result = existing.mergeWithProcessedEvent(eventWith(venue = "New Venue"), newFlyerBytes)
    assertEquals("New Venue", result.venue)
  }

  @Test
  fun `test mergeWithProcessedEvent keeps existing venue when event venue is null`() {
    val existing = EditedEventData(venue = "Old Venue")
    val result = existing.mergeWithProcessedEvent(eventWith(venue = null), newFlyerBytes)
    assertEquals("Old Venue", result.venue)
  }

  @Test
  fun `test mergeWithProcessedEvent keeps existing venue when event venue is blank`() {
    val existing = EditedEventData(venue = "Old Venue")
    val result = existing.mergeWithProcessedEvent(eventWith(venue = ""), newFlyerBytes)
    assertEquals("Old Venue", result.venue)
  }

  @Test
  fun `test mergeWithProcessedEvent overwrites eventUrl when event eventUrl is non-blank`() {
    val existing = EditedEventData(eventUrl = "https://old.com")
    val result = existing.mergeWithProcessedEvent(eventWith(eventUrl = "https://new.com"), newFlyerBytes)
    assertEquals("https://new.com", result.eventUrl)
  }

  @Test
  fun `test mergeWithProcessedEvent keeps existing eventUrl when event eventUrl is null`() {
    val existing = EditedEventData(eventUrl = "https://old.com")
    val result = existing.mergeWithProcessedEvent(eventWith(eventUrl = null), newFlyerBytes)
    assertEquals("https://old.com", result.eventUrl)
  }

  @Test
  fun `test mergeWithProcessedEvent keeps existing eventUrl when event eventUrl is blank`() {
    val existing = EditedEventData(eventUrl = "https://old.com")
    val result = existing.mergeWithProcessedEvent(eventWith(eventUrl = ""), newFlyerBytes)
    assertEquals("https://old.com", result.eventUrl)
  }

  @Test
  fun `test mergeWithProcessedEvent overwrites artists when event artists is non-empty`() {
    val existing = EditedEventData(artists = "Old Artist")
    val result = existing.mergeWithProcessedEvent(eventWith(artists = listOf("Artist A", "Artist B")), newFlyerBytes)
    assertEquals("Artist A, Artist B", result.artists)
  }

  @Test
  fun `test mergeWithProcessedEvent keeps existing artists when event artists is empty`() {
    val existing = EditedEventData(artists = "Old Artist")
    val result = existing.mergeWithProcessedEvent(eventWith(artists = emptyList()), newFlyerBytes)
    assertEquals("Old Artist", result.artists)
  }

  @Test
  fun `test mergeWithProcessedEvent always updates flyerImageBytes`() {
    val existing = EditedEventData(flyerImageBytes = flyerBytes)
    val result = existing.mergeWithProcessedEvent(eventWith(), newFlyerBytes)
    assertContentEquals(newFlyerBytes, result.flyerImageBytes)
  }

  @Test
  fun `test mergeWithProcessedEvent with all empty existing data uses event data`() {
    val existing = EditedEventData()
    val date = LocalDate(2026, 8, 20)
    val time = LocalTime(19, 0)
    val event = eventWith(
      name = "Festival",
      startDate = date,
      startTime = time,
      venue = "The Venue",
      eventUrl = "https://fest.com",
      artists = listOf("DJ One"),
    )
    val result = existing.mergeWithProcessedEvent(event, newFlyerBytes)
    assertEquals("Festival", result.name)
    assertEquals(date, result.startDate)
    assertEquals(time, result.startTime)
    assertEquals("The Venue", result.venue)
    assertEquals("https://fest.com", result.eventUrl)
    assertEquals("DJ One", result.artists)
    assertContentEquals(newFlyerBytes, result.flyerImageBytes)
  }

  @Test
  fun `test mergeWithProcessedEvent with all empty event data keeps existing data`() {
    val date = LocalDate(2025, 5, 10)
    val time = LocalTime(20, 0)
    val existing = EditedEventData(
      name = "Existing Event",
      startDate = date,
      startTime = time,
      venue = "Existing Venue",
      eventUrl = "https://existing.com",
      artists = "Existing Artist",
      flyerImageBytes = flyerBytes,
    )
    val result = existing.mergeWithProcessedEvent(eventWith(), newFlyerBytes)
    assertEquals("Existing Event", result.name)
    assertEquals(date, result.startDate)
    assertEquals(time, result.startTime)
    assertEquals("Existing Venue", result.venue)
    assertEquals("https://existing.com", result.eventUrl)
    assertEquals("Existing Artist", result.artists)
    assertContentEquals(newFlyerBytes, result.flyerImageBytes)
  }
}
