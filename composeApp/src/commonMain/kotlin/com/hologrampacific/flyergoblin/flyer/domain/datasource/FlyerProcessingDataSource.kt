package com.hologrampacific.flyergoblin.flyer.domain.datasource

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

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
   */
  data class Error(val message: String) : FlyerExtractionResult()
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
