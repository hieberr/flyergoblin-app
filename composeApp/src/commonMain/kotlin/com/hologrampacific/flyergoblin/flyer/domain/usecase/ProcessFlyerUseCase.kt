package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerExtractionResult
import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerProcessingDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.Event
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock

/** Result of processing a flyer image. */
sealed class ProcessFlyerResult {
  /**
   * Processing completed successfully.
   *
   * @property event The extracted event with all details
   */
  data class Success(val event: Event) : ProcessFlyerResult()

  /**
   * Processing failed with an error.
   *
   * @property message User-friendly error message
   */
  data class Error(val message: String) : ProcessFlyerResult()
}

/**
 * Use case for processing flyer images to extract event information.
 *
 * @param flyerDataSource The data source for AI-based flyer extraction
 */
class ProcessFlyerUseCase(private val flyerDataSource: FlyerProcessingDataSource) {

  companion object {
    /** Maximum image size in megabytes */
    private const val MAX_IMAGE_SIZE_MB = 20

    /** Minimum image size in bytes */
    private const val MIN_IMAGE_SIZE_BYTES = 1024

    /** Bytes per megabyte */
    private const val BYTES_PER_MB = 1024 * 1024
  }

  /**
   * Processes a flyer image to extract event details.
   *
   * @param imageBytes The flyer image as a byte array
   * @param mimeType The MIME type of the image (e.g., "image/jpeg")
   * @return A ProcessFlyerResult containing either the created Event or an error
   */
  @OptIn(ExperimentalEncodingApi::class)
  suspend operator fun invoke(
    imageBytes: ByteArray,
    mimeType: String = "image/jpeg",
  ): ProcessFlyerResult {
    // Validate image size
    if (imageBytes.size > MAX_IMAGE_SIZE_MB * BYTES_PER_MB) {
      val sizeMB = imageBytes.size.toFloat() / BYTES_PER_MB
      val sizeFormatted = ((sizeMB * 100).toInt() / 100f).toString()
      return ProcessFlyerResult.Error(
        "Image too large ($sizeFormatted MB). Maximum size is ${MAX_IMAGE_SIZE_MB}MB."
      )
    }

    if (imageBytes.size < MIN_IMAGE_SIZE_BYTES) {
      return ProcessFlyerResult.Error(
        "Image too small (${imageBytes.size} bytes). Please provide a clear flyer image."
      )
    }

    // Encode image and call data source
    val base64Image = Base64.encode(imageBytes)

    return when (val result = flyerDataSource.extractEventFromFlyer(base64Image, mimeType)) {
      is FlyerExtractionResult.Success -> {
        val data = result.data
        val event =
          Event(
            id = 0L,
            name = data.name.ifBlank { "Unknown" },
            startDate = data.startDate,
            startTime = data.startTime,
            venue = data.venue,
            eventUrl = data.eventUrl,
            artists = data.artists,
            dateAdded = Clock.System.now(),
            flyerImageBytes = imageBytes,
          )
        ProcessFlyerResult.Success(event)
      }
      is FlyerExtractionResult.Error -> {
        ProcessFlyerResult.Error(result.message)
      }
    }
  }
}
