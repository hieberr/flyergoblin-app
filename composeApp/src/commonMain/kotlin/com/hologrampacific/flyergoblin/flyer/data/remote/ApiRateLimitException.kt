package com.hologrampacific.flyergoblin.flyer.data.remote

import kotlin.time.Instant

/**
 * Exception thrown when an API rate limit is exceeded.
 *
 * @param blockedUntil The instant until which requests are blocked
 * @param message Error message
 */
class ApiRateLimitException(val blockedUntil: Instant, message: String) : Exception(message)
