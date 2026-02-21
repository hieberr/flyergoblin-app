package com.hologrampacific.flyergoblin.flyer.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.db.createAppDatabase
import com.hologrampacific.flyergoblin.db.createTestDriver
import com.hologrampacific.flyergoblin.flyer.domain.model.Event
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class SqlDelightEventRepositoryTest : AppTest() {

  private lateinit var driver: SqlDriver
  private lateinit var repository: SqlDelightEventRepository

  @BeforeTest
  fun setup() {
    driver = createTestDriver()
    repository = SqlDelightEventRepository(createAppDatabase(driver))
  }

  @AfterTest
  fun teardown() {
    driver.close()
  }

  private fun testEvent(
    name: String = "Test Event",
    startDate: LocalDate? = LocalDate(2026, 3, 15),
    startTime: LocalTime? = LocalTime(20, 0),
    venue: String? = "Test Venue",
    eventUrl: String? = "https://example.com",
    artists: List<String> = listOf("Artist 1", "Artist 2"),
    dateAdded: Instant = Instant.fromEpochMilliseconds(1706832000000),
    flyerImageBytes: ByteArray? = null,
  ) =
    Event(
      name = name,
      startDate = startDate,
      startTime = startTime,
      venue = venue,
      eventUrl = eventUrl,
      artists = artists,
      dateAdded = dateAdded,
      flyerImageBytes = flyerImageBytes,
    )

  @Test
  fun `getAllEvents returns empty list when database is empty`() = runTest {
    assertTrue(repository.getAllEvents().isEmpty())
  }

  @Test
  fun `saveEvent returns the auto-assigned ID`() = runTest {
    val id = repository.saveEvent(testEvent())
    assertTrue(id > 0)
  }

  @Test
  fun `saveEvent and getEventById round-trip preserves all fields`() = runTest {
    val event = testEvent()
    val id = repository.saveEvent(event)
    val saved = repository.getEventById(id)

    assertNotNull(saved)
    assertEquals(id, saved.id)
    assertEquals(event.name, saved.name)
    assertEquals(event.startDate, saved.startDate)
    assertEquals(event.startTime, saved.startTime)
    assertEquals(event.venue, saved.venue)
    assertEquals(event.eventUrl, saved.eventUrl)
    assertEquals(event.artists, saved.artists)
    assertEquals(event.dateAdded, saved.dateAdded)
  }

  @Test
  fun `getAllEvents returns all saved events`() = runTest {
    repository.saveEvent(testEvent(name = "Event 1"))
    repository.saveEvent(testEvent(name = "Event 2"))
    repository.saveEvent(testEvent(name = "Event 3"))

    assertEquals(3, repository.getAllEvents().size)
  }

  @Test
  fun `multiple saveEvent calls assign unique IDs`() = runTest {
    val id1 = repository.saveEvent(testEvent(name = "Event 1"))
    val id2 = repository.saveEvent(testEvent(name = "Event 2"))

    assertTrue(id1 != id2)
  }

  @Test
  fun `updateEvent updates the event fields`() = runTest {
    val id = repository.saveEvent(testEvent(name = "Original Name"))
    val original = repository.getEventById(id)!!

    repository.updateEvent(original.copy(name = "Updated Name", venue = "New Venue"))

    val updated = repository.getEventById(id)!!
    assertEquals("Updated Name", updated.name)
    assertEquals("New Venue", updated.venue)
  }

  @Test
  fun `deleteEvent removes the event from the database`() = runTest {
    val id = repository.saveEvent(testEvent())
    assertNotNull(repository.getEventById(id))

    repository.deleteEvent(id)

    assertNull(repository.getEventById(id))
  }

  @Test
  fun `deleteEvent does not affect other events`() = runTest {
    val id1 = repository.saveEvent(testEvent(name = "Event 1"))
    val id2 = repository.saveEvent(testEvent(name = "Event 2"))

    repository.deleteEvent(id1)

    assertNull(repository.getEventById(id1))
    assertNotNull(repository.getEventById(id2))
  }

  @Test
  fun `getEventById returns null for non-existent ID`() = runTest {
    assertNull(repository.getEventById(999L))
  }

  @Test
  fun `saveEvent handles event with all null optional fields`() = runTest {
    val event =
      testEvent(
        startDate = null,
        startTime = null,
        venue = null,
        eventUrl = null,
        artists = emptyList(),
      )
    val id = repository.saveEvent(event)
    val saved = repository.getEventById(id)

    assertNotNull(saved)
    assertNull(saved.startDate)
    assertNull(saved.startTime)
    assertNull(saved.venue)
    assertNull(saved.eventUrl)
    assertTrue(saved.artists.isEmpty())
  }

  @Test
  fun `saveEvent and getEventById round-trip preserves flyerImageBytes`() = runTest {
    val bytes = byteArrayOf(1, 2, 3, 4, 5)
    val id = repository.saveEvent(testEvent(flyerImageBytes = bytes))
    val saved = repository.getEventById(id)

    assertNotNull(saved)
    assertNotNull(saved.flyerImageBytes)
    assertTrue(bytes.contentEquals(saved.flyerImageBytes))
  }
}
