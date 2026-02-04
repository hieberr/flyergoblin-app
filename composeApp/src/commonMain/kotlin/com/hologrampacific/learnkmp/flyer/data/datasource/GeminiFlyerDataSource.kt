package com.hologrampacific.learnkmp.flyer.data.datasource

import com.hologrampacific.learnkmp.flyer.data.remote.GeminiApiClient
import com.hologrampacific.learnkmp.flyer.data.remote.JsonAiResponseParser
import com.hologrampacific.learnkmp.flyer.domain.datasource.ExtractedFlyerData
import com.hologrampacific.learnkmp.flyer.domain.datasource.FlyerExtractionResult
import com.hologrampacific.learnkmp.flyer.domain.datasource.FlyerProcessingDataSource
import com.hologrampacific.learnkmp.util.AppLogger
import io.ktor.client.network.sockets.*
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

/**
 * Gemini-based implementation of FlyerProcessingDataSource that uses Google's Gemini model to
 * extract event details from flyer images.
 *
 * @param geminiApiClient The API client for making Gemini requests
 */
class GeminiFlyerDataSource(private val geminiApiClient: GeminiApiClient) :
  FlyerProcessingDataSource {

  private val json = JsonAiResponseParser.json

  companion object {
    /** Lower temperature (0.0-1.0) for more consistent, factual extraction from flyer images */
    private const val EXTRACTION_TEMPERATURE = 0.3

    /** Maximum output tokens - sufficient for event JSON response with all fields */
    private const val MAX_OUTPUT_TOKENS = 2000

    /** The Gemini model to use */
    private const val MODEL = "gemini-2.5-flash"
  }

  @Serializable
  private data class GeminiExtractedEventData(
    val name: String = "Unknown",
    val startDate: String = "Unknown",
    val startTime: String? = null,
    val venue: String? = null,
    val eventUrl: String? = null,
    val artists: List<String> = emptyList(),
  )

  override suspend fun extractEventFromFlyer(
    imageBase64: String,
    mimeType: String,
  ): FlyerExtractionResult {
    return try {
      val prompt = buildPrompt()

      val geminiResponse =
        geminiApiClient.llmPromptWithImage(
          prompt = prompt,
          imageBase64 = imageBase64,
          mimeType = mimeType,
          model = MODEL,
          temperature = EXTRACTION_TEMPERATURE,
          maxOutputTokens = MAX_OUTPUT_TOKENS,
        )

      val responseText =
        geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
          ?: run {
            AppLogger.e("GeminiFlyerDataSource", "No response text from LLM service")
            return FlyerExtractionResult.Error("Unable to extract event details from the flyer.")
          }

      parseEventData(responseText)
    } catch (e: ConnectTimeoutException) {
      AppLogger.e("GeminiFlyerDataSource", "Connection timeout while processing flyer", e)
      FlyerExtractionResult.Error("Connection timeout. Please check your internet connection.")
    } catch (e: SocketTimeoutException) {
      AppLogger.e("GeminiFlyerDataSource", "Request timeout while processing flyer", e)
      FlyerExtractionResult.Error("Request timeout. The server took too long to respond.")
    } catch (e: SerializationException) {
      AppLogger.e("GeminiFlyerDataSource", "Failed to parse server response: ${e.message}", e)
      FlyerExtractionResult.Error("Unable to process the server response. Please try again.")
    } catch (e: Exception) {
      AppLogger.e("GeminiFlyerDataSource", "Failed to process flyer: ${e.message}", e)
      FlyerExtractionResult.Error("Unable to process flyer. Please try again.")
    }
  }

  private fun buildPrompt(): String {
    return """
    Extract event details from this flyer image and return them in JSON format.

    Fields to extract:
    - name: The event name
    - startDate: Event start date in YYYY-MM-DD format
      - If year is unknown assume it to be the year that places the event date nearest to the current date.
    - startTime: Event start time in HH:MM format (24-hour)
    - venue: Venue name
    - eventUrl: Event URL if provided
    - artists: Array of artist/band/DJ names performing

    Return ONLY valid JSON in this exact format:
    {
      "name": "Event Name",
      "startDate": "2026-03-15",
      "startTime": "20:00",
      "venue": "Venue Name",
      "eventUrl": "https://example.com",
      "artists": ["Artist 1", "Artist 2"]
    }

    If a field is not found on the flyer, omit it from the JSON.
    Do not include any explanation or additional text, only the JSON object.
    """
      .trimIndent()
  }

  private fun parseEventData(responseText: String): FlyerExtractionResult {
    return try {
      val jsonText =
        JsonAiResponseParser.extractJsonFromResponse(responseText, "GeminiFlyerDataSource")

      val extractedData = json.decodeFromString<GeminiExtractedEventData>(jsonText)

      val eventName = extractedData.name.takeIf { it.isNotBlank() } ?: "Unknown"

      val startDate =
        if (
          extractedData.startDate.isBlank() ||
            extractedData.startDate.equals("Unknown", ignoreCase = true)
        ) {
          null
        } else {
          try {
            kotlinx.datetime.LocalDate.parse(extractedData.startDate)
          } catch (e: Exception) {
            AppLogger.w(
              "GeminiFlyerDataSource",
              "Could not parse date: ${extractedData.startDate}",
              e,
            )
            null
          }
        }

      val startTime =
        extractedData.startTime?.let {
          try {
            LocalTime.parse(it)
          } catch (e: Exception) {
            AppLogger.w("GeminiFlyerDataSource", "Could not parse time: $it", e)
            null
          }
        }

      val data =
        ExtractedFlyerData(
          name = eventName,
          startDate = startDate,
          startTime = startTime,
          venue = extractedData.venue,
          eventUrl = extractedData.eventUrl,
          artists = extractedData.artists,
        )

      AppLogger.i(
        "GeminiFlyerDataSource",
        "Successfully parsed event: ${data.name} on ${data.startDate}",
      )
      FlyerExtractionResult.Success(data)
    } catch (e: SerializationException) {
      AppLogger.e(
        "GeminiFlyerDataSource",
        "Failed to parse event data: ${e.message}\nResponse: $responseText",
        e,
      )
      FlyerExtractionResult.Error(
        "Unable to extract event details. The flyer format may not be recognized."
      )
    } catch (e: Exception) {
      AppLogger.e(
        "GeminiFlyerDataSource",
        "Error processing event data: ${e.message}\nResponse: $responseText",
        e,
      )
      FlyerExtractionResult.Error("Unable to process event details. Please try again.")
    }
  }
}
