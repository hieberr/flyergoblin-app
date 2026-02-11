package com.hologrampacific.learnkmp.flyer.data.remote

/**
 * Base exception for SoundCloud API errors.
 *
 * @param message Error message
 * @param cause The underlying cause
 */
open class SoundCloudApiException(message: String, cause: Throwable? = null) :
  Exception(message, cause)

/**
 * Exception thrown when SoundCloud API rate limit is exceeded.
 *
 * @param message Error message
 * @param resetTime The time when the rate limit will reset (format: yyyy/MM/dd HH:mm:ss Z)
 * @param maxRequests Maximum number of requests allowed
 * @param timeWindow The time window duration as ISO 8601
 */
class RateLimitException(
  message: String,
  val resetTime: String,
  val maxRequests: Int,
  val timeWindow: String,
) : SoundCloudApiException(message)

/**
 * Exception thrown when the SoundCloud API returns a server error (5xx).
 *
 * @param statusCode The HTTP status code
 * @param message Error message
 */
class ServerErrorException(val statusCode: Int, message: String) : SoundCloudApiException(message)

/**
 * Exception thrown when the SoundCloud API returns a client error (4xx) other than 401 or 429.
 *
 * @param statusCode The HTTP status code
 * @param message Error message
 */
class ClientErrorException(val statusCode: Int, message: String) : SoundCloudApiException(message)
