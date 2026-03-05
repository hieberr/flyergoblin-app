package com.hologrampacific.flyergoblin.flyer.data.remote

import com.hologrampacific.flyergoblin.util.AppLogger
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe rate limit state shared across API clients.
 * Encapsulates the blocked-until timestamp and mutex so each client
 * doesn't need to re-implement the same locking pattern.
 */
internal class RateLimitState {
  private var blockedUntil: Instant? = null
  private val mutex = Mutex()

  /**
   * Checks whether requests are currently rate-limited. If a rate limit window is active and has
   * not yet expired, logs a warning and throws [ApiRateLimitException]. If the window has expired,
   * clears it and returns normally.
   *
   * @param logTag Tag used in [AppLogger] log messages (e.g. `"SoundCloudApiClient"`).
   * @param apiName Human-readable API name embedded in the [ApiRateLimitException] message
   *   (e.g. `"SoundCloud API"`).
   * @throws ApiRateLimitException if the rate limit window is still active.
   */
  suspend fun check(logTag: String, apiName: String) {
    mutex.withLock {
      val until = blockedUntil ?: return@withLock
      if (Clock.System.now() < until) {
        AppLogger.w(logTag, "Rate limited until: $until")
        throw ApiRateLimitException(until, "$apiName rate limit exceeded")
      } else {
        blockedUntil = null
      }
    }
  }

  /**
   * Sets the instant until which requests should be blocked. Thread-safe; may be called
   * concurrently from multiple coroutines.
   *
   * @param instant The earliest time at which requests may resume.
   */
  suspend fun setBlockedUntil(instant: Instant) {
    mutex.withLock {
      val current = blockedUntil
      if (current == null || instant > current) {
        blockedUntil = instant
      }
    }
  }
}
