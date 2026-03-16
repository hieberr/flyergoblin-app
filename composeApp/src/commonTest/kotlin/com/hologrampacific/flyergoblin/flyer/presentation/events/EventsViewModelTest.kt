package com.hologrampacific.flyergoblin.flyer.presentation.events

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.domain.model.Event
import com.hologrampacific.flyergoblin.flyer.domain.repository.EventRepository
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class EventsViewModelTest : AppTest() {

  private val testDispatcher = UnconfinedTestDispatcher()

  @BeforeTest
  fun setupDispatcher() {
    Dispatchers.setMain(testDispatcher)
  }

  @AfterTest
  fun teardownDispatcher() {
    Dispatchers.resetMain()
  }

  private fun makeRepository(events: List<Event> = emptyList()): EventRepository {
    val repository: EventRepository = mock(MockMode.autoUnit)
    every { repository.observeEvents() } returns flowOf(events)
    return repository
  }

  // Fixed clock representing 2026-03-15 noon in the local timezone
  private val fixedClock =
    object : Clock {
      override fun now(): Instant {
        val localNoon = LocalDateTime(2026, 3, 15, 12, 0, 0)
        return localNoon.toInstant(TimeZone.currentSystemDefault())
      }
    }

  private fun testEvent() =
    Event(
      id = 1L,
      name = "Test Event",
      startDate = LocalDate(2099, 12, 31),
      dateAdded = Instant.fromEpochMilliseconds(0),
    )

  @Test
  fun `deleteEvent calls repository deleteEvent with correct id`() = runTest {
    val repository = makeRepository()
    val viewModel = EventsViewModel(repository)

    viewModel.deleteEvent(42L)

    verifySuspend { repository.deleteEvent(42L) }
  }

  @Test
  fun `deleteEvent sets errorMessage in uiState when repository throws`() = runTest {
    val repository = makeRepository()
    everySuspend { repository.deleteEvent(any()) } throws Exception("DB error")
    val viewModel = EventsViewModel(repository)

    viewModel.deleteEvent(42L)

    assertNotNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `clearError clears errorMessage in uiState`() = runTest {
    val repository = makeRepository()
    everySuspend { repository.deleteEvent(any()) } throws Exception("DB error")
    val viewModel = EventsViewModel(repository)
    viewModel.deleteEvent(42L)

    viewModel.clearError()

    assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `uiState events updates when repository flow emits new list`() = runTest {
    val eventsFlow = MutableStateFlow(listOf(testEvent()))
    val repository: EventRepository = mock(MockMode.autoUnit)
    every { repository.observeEvents() } returns eventsFlow
    val viewModel = EventsViewModel(repository)

    eventsFlow.value = emptyList()

    assertTrue(viewModel.uiState.value.events.isEmpty())
  }

  @Test
  fun `isPastEvent returns true for events before today`() = runTest {
    val repository = makeRepository()
    val viewModel = EventsViewModel(repository, fixedClock)
    val pastEvent = testEvent().copy(startDate = LocalDate(2026, 3, 14))

    assertTrue(viewModel.isPastEvent(pastEvent))
  }

  @Test
  fun `isPastEvent returns false for events today`() = runTest {
    val repository = makeRepository()
    val viewModel = EventsViewModel(repository, fixedClock)
    val todayEvent = testEvent().copy(startDate = LocalDate(2026, 3, 15))

    assertFalse(viewModel.isPastEvent(todayEvent))
  }

  @Test
  fun `isPastEvent returns false for events with null startDate`() = runTest {
    val repository = makeRepository()
    val viewModel = EventsViewModel(repository, fixedClock)
    val noDateEvent = testEvent().copy(startDate = null)

    assertFalse(viewModel.isPastEvent(noDateEvent))
  }

  @Test
  fun `past events are hidden by default`() = runTest {
    val pastEvent = testEvent().copy(id = 1L, startDate = LocalDate(2026, 3, 14))
    val futureEvent = testEvent().copy(id = 2L, startDate = LocalDate(2026, 3, 16))
    val repository = makeRepository(listOf(pastEvent, futureEvent))
    val viewModel = EventsViewModel(repository, fixedClock)

    assertEquals(1, viewModel.uiState.value.events.size)
    assertEquals(2L, viewModel.uiState.value.events[0].id)
  }

  @Test
  fun `setShowPastEvents true includes past events`() = runTest {
    val pastEvent = testEvent().copy(id = 1L, startDate = LocalDate(2026, 3, 14))
    val futureEvent = testEvent().copy(id = 2L, startDate = LocalDate(2026, 3, 16))
    val eventsFlow = MutableStateFlow(listOf(pastEvent, futureEvent))
    val repository: EventRepository = mock(MockMode.autoUnit)
    every { repository.observeEvents() } returns eventsFlow
    val viewModel = EventsViewModel(repository, fixedClock)

    viewModel.setShowPastEvents(true)

    assertEquals(2, viewModel.uiState.value.events.size)
  }

  @Test
  fun `setShowPastEvents false hides past events`() = runTest {
    val pastEvent = testEvent().copy(id = 1L, startDate = LocalDate(2026, 3, 14))
    val futureEvent = testEvent().copy(id = 2L, startDate = LocalDate(2026, 3, 16))
    val eventsFlow = MutableStateFlow(listOf(pastEvent, futureEvent))
    val repository: EventRepository = mock(MockMode.autoUnit)
    every { repository.observeEvents() } returns eventsFlow
    val viewModel = EventsViewModel(repository, fixedClock)
    viewModel.setShowPastEvents(true)

    viewModel.setShowPastEvents(false)

    assertEquals(1, viewModel.uiState.value.events.size)
    assertEquals(2L, viewModel.uiState.value.events[0].id)
  }

  @Test
  fun `events with null startDate are always shown`() = runTest {
    val noDateEvent = testEvent().copy(id = 1L, startDate = null)
    val repository = makeRepository(listOf(noDateEvent))
    val viewModel = EventsViewModel(repository, fixedClock)

    assertEquals(1, viewModel.uiState.value.events.size)
  }
}
