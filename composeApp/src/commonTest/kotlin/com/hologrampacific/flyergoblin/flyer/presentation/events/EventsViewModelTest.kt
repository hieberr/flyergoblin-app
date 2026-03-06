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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate

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

  private fun testEvent() =
    Event(
      id = 1L,
      name = "Test Event",
      startDate = LocalDate(2026, 3, 15),
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
}
