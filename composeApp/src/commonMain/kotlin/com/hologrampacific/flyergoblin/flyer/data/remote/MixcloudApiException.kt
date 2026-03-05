package com.hologrampacific.flyergoblin.flyer.data.remote

/**
 * Base exception for Mixcloud API errors.
 *
 * @param message Error message
 * @param cause The underlying cause
 */
open class MixcloudApiException(message: String, cause: Throwable? = null) :
  Exception(message, cause)

/**
 * Exception thrown when the Mixcloud API returns a server error (5xx).
 *
 * @param statusCode The HTTP status code
 * @param message Error message
 */
class MixcloudServerErrorException(val statusCode: Int, message: String) :
  MixcloudApiException(message)

/**
 * Exception thrown when the Mixcloud API returns a client error (4xx).
 *
 * @param statusCode The HTTP status code
 * @param message Error message
 */
class MixcloudClientErrorException(val statusCode: Int, message: String) :
  MixcloudApiException(message)

