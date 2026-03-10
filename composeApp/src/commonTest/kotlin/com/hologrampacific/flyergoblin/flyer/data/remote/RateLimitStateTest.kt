package com.hologrampacific.flyergoblin.flyer.data.remote

import com.hologrampacific.flyergoblin.AppTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class RateLimitStateTest : AppTest() {

  @Test
  fun `test check passes when no rate limit is set`() = runTest {
    val state = RateLimitState()
    // Should not throw
    state.check("TestTag", "Test API")
  }

  @Test
  fun `test check throws ApiRateLimitException when blocked window is active`() = runTest {
    val state = RateLimitState()
    val blockedUntil = Clock.System.now() + 5.minutes

    state.setBlockedUntil(blockedUntil)

    val ex = assertFailsWith<ApiRateLimitException> { state.check("TestTag", "Test API") }
    assertEquals(blockedUntil, ex.blockedUntil)
    assertEquals("Test API rate limit exceeded", ex.message)
  }

  @Test
  fun `test check clears expired state and does not throw`() = runTest {
    val state = RateLimitState()
    // Set a block in the past
    val expiredUntil = Clock.System.now() - 1.seconds

    state.setBlockedUntil(expiredUntil)

    // Should not throw and should clear the state
    state.check("TestTag", "Test API")

    // A second check should also pass (state was cleared)
    state.check("TestTag", "Test API")
  }

  @Test
  fun `test setBlockedUntil does not overwrite a later expiry with an earlier one`() = runTest {
    val state = RateLimitState()
    val farFuture = Clock.System.now() + 60.minutes
    val nearFuture = Clock.System.now() + 5.minutes

    state.setBlockedUntil(farFuture)
    state.setBlockedUntil(nearFuture)

    val ex = assertFailsWith<ApiRateLimitException> { state.check("TestTag", "Test API") }
    assertEquals(farFuture, ex.blockedUntil)
  }

  @Test
  fun `test setBlockedUntil updates when later instant is provided`() = runTest {
    val state = RateLimitState()
    val nearFuture = Clock.System.now() + 5.minutes
    val farFuture = Clock.System.now() + 60.minutes

    state.setBlockedUntil(nearFuture)
    state.setBlockedUntil(farFuture)

    val ex = assertFailsWith<ApiRateLimitException> { state.check("TestTag", "Test API") }
    assertEquals(farFuture, ex.blockedUntil)
  }

  @Test
  fun `test check and setBlockedUntil are safe under concurrent access`() = runTest {
    val state = RateLimitState()
    val blockedUntil = Clock.System.now() + 10.minutes
    val exceptions = mutableListOf<ApiRateLimitException>()

    val setJob = launch { state.setBlockedUntil(blockedUntil) }
    val checkJobs =
      (1..10).map {
        launch {
          try {
            state.check("TestTag", "Test API")
          } catch (e: ApiRateLimitException) {
            exceptions.add(e)
          }
        }
      }
    setJob.join()
    checkJobs.forEach { it.join() }

    exceptions.forEach { assertEquals(blockedUntil, it.blockedUntil) }
  }
}
