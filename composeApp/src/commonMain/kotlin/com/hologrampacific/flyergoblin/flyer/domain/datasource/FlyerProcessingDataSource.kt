package com.hologrampacific.flyergoblin.flyer.domain.datasource

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Category of failure when extracting event data from a flyer, used to tailor how the failure is
 * presented to the user (e.g. offering a retry for a timeout vs. a rate limit).
 */
enum class FlyerExtractionErrorType {
  /** The server, or the upstream AI service, took too long to process the image. */
  TIMEOUT,
  /** The upstream AI service (Gemini) is rate limiting requests from our backend. */
  UPSTREAM_RATE_LIMITED,
  /** Our own API is rate limiting requests from this client to prevent abuse. */
  CLIENT_RATE_LIMITED,
  /** The request was rejected as invalid (e.g. unsupported image). */
  INVALID_REQUEST,
  /** The server encountered an error while processing the image. */
  SERVER_ERROR,
  /** A local network problem prevented the request from completing. */
  NETWORK,
  /** An unclassified failure. */
  UNKNOWN,
}

/** Result of extracting event data from a flyer image. */
sealed class FlyerExtractionResult {
  /**
   * Extraction completed successfully.
   *
   * @property data The extracted event data
   */
  data class Success(val data: ExtractedFlyerData) : FlyerExtractionResult()

  /**
   * Extraction failed with an error.
   *
   * @property message User-friendly error message
   * @property type Category of the failure
   */
  data class Error(
    val message: String,
    val type: FlyerExtractionErrorType = FlyerExtractionErrorType.UNKNOWN,
  ) : FlyerExtractionResult()
}

/** Raw data extracted from a flyer image by the AI. */
data class ExtractedFlyerData(
  val name: String,
  val startDate: LocalDate?,
  val startTime: LocalTime?,
  val venue: String?,
  val eventUrl: String?,
  val artists: List<String>,
)

/** DataSource for extracting event information from flyer images using AI. */
interface FlyerProcessingDataSource {
  /**
   * Extracts event details from a flyer image.
   *
   * @param imageBase64 The flyer image encoded as Base64
   * @param mimeType The MIME type of the image (e.g., "image/jpeg")
   * @return A FlyerExtractionResult containing either the extracted data or an error
   */
  suspend fun extractEventFromFlyer(imageBase64: String, mimeType: String): FlyerExtractionResult
}
