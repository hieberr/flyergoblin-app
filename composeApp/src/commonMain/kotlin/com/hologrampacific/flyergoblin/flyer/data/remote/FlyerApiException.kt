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

    /**
     * Fallback classification from the HTTP status code, used when the response body doesn't carry
     * a recognizable `code` field — e.g. API Gateway's own integration-timeout body (`{"message":
     * "Endpoint request timed out"}`) rather than our Lambda's own `TIMEOUT` response, which never
     * gets a chance to run once the gateway's own timeout fires first.
     */
    fun fromStatusCode(statusCode: Int): FlyerApiErrorCode =
      when (statusCode) {
        400 -> INVALID_REQUEST
        429 -> UPSTREAM_RATE_LIMITED
        502 -> UPSTREAM_ERROR
        504 -> TIMEOUT
        in 500..599 -> INTERNAL_ERROR
        else -> UNKNOWN
      }
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
