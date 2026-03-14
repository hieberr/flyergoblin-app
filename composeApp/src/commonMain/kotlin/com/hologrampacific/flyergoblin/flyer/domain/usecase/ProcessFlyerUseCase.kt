package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerExtractionResult
import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerProcessingDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.Event
import com.hologrampacific.flyergoblin.util.BYTES_PER_KB
import com.hologrampacific.flyergoblin.util.ImageBytes
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
    /** Maximum image size in kilobytes */
    private const val MAX_IMAGE_SIZE_KB = 200

    /** Minimum image size in bytes */
    private const val MIN_IMAGE_SIZE_BYTES = 1024
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
    imageBytes: ImageBytes,
    mimeType: String = "image/jpeg",
  ): ProcessFlyerResult {
    // Validate image size
    if (imageBytes.bytes.size > MAX_IMAGE_SIZE_KB * BYTES_PER_KB) {
      val sizeKB = imageBytes.bytes.size / BYTES_PER_KB
      return ProcessFlyerResult.Error(
        "Image too large (${sizeKB}KB). Maximum size is ${MAX_IMAGE_SIZE_KB}KB."
      )
    }

    if (imageBytes.bytes.size < MIN_IMAGE_SIZE_BYTES) {
      return ProcessFlyerResult.Error(
        "Image too small (${imageBytes.bytes.size} bytes). Please provide a clear flyer image."
      )
    }

    // Encode image and call data source
    val base64Image = Base64.encode(imageBytes.bytes)

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
