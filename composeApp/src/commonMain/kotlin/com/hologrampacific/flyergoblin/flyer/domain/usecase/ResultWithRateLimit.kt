package com.hologrampacific.flyergoblin.flyer.domain.usecase

import kotlin.time.Instant

/** Results of an operation that could have rate limiting. */
sealed class ResultWithRateLimit {
  /** Search completed successfully. */
  data object Success : ResultWithRateLimit()

  /**
   * Rate limit exceeded.
   *
   * @property blockedUntil The instant until which requests are blocked
   */
  data class RateLimited(val blockedUntil: Instant) : ResultWithRateLimit()

  /**
   * Failed with an error.
   *
   * @property message User-friendly error message
   */
  data class Error(val message: String) : ResultWithRateLimit()
}
