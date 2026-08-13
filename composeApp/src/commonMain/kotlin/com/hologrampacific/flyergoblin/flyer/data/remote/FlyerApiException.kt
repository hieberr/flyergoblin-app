package com.hologrampacific.flyergoblin.flyer.data.remote

/**
 * Structured error codes returned by the flyer processing API. [UNKNOWN] covers any code this
 * client doesn't recognize yet, keeping it forward-compatible with new backend error codes.
 */
enum class FlyerApiErrorCode {
  INVALID_REQUEST,
  UPSTREAM_RATE_LIMITED,
  UPSTREAM_ERROR,
  TIMEOUT,
  INTERNAL_ERROR,
  UNKNOWN;

  companion object {
    fun fromWireValue(value: String): FlyerApiErrorCode =
      entries.find { it.name == value } ?: UNKNOWN
  }
}

/**
 * Thrown when the flyer processing API returns a structured `{code, message}` error response.
 *
 * @param errorCode The parsed error code, or [FlyerApiErrorCode.UNKNOWN] if not recognized
 * @param message Human-readable message from the backend, safe to show to the user
 * @param statusCode The HTTP status code
 */
class FlyerApiException(val errorCode: FlyerApiErrorCode, message: String, val statusCode: Int) :
  Exception(message)
