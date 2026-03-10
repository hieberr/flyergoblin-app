package com.hologrampacific.flyergoblin.flyer.data.datasource

import com.hologrampacific.flyergoblin.flyer.data.remote.GeminiApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.JsonAiResponseParser
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ExtractedFlyerData
import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerExtractionResult
import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerProcessingDataSource
import com.hologrampacific.flyergoblin.util.AppLogger
import io.ktor.client.network.sockets.*
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

/**
 * Gemini-based implementation of FlyerProcessingDataSource that uses Google's Gemini model to
 * extract event details from flyer images.
 *
 * @param geminiApiClient The API client for making Gemini requests
 * @param clock Clock used to determine the current date for year inference
 */
class GeminiFlyerDataSource(
  private val geminiApiClient: GeminiApiClient,
  private val clock: Clock = Clock.System,
) : FlyerProcessingDataSource {

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
    val startMonth: Int? = null,
    val startDay: Int? = null,
    val startYear: Int? = null,
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
            AppLogger.d("GeminiFlyerDataSource", "No response text from LLM service")
            return FlyerExtractionResult.Error("Unable to extract event details from the flyer.")
          }

      parseEventData(responseText)
    } catch (e: ConnectTimeoutException) {
      AppLogger.d("GeminiFlyerDataSource", "Connection timeout while processing flyer")
      FlyerExtractionResult.Error("Connection timeout. Please check your internet connection.")
    } catch (e: SocketTimeoutException) {
      AppLogger.d("GeminiFlyerDataSource", "Request timeout while processing flyer")
      FlyerExtractionResult.Error("Request timeout. The server took too long to respond.")
    } catch (e: SerializationException) {
      AppLogger.d("GeminiFlyerDataSource", "Failed to parse server response", e)
      FlyerExtractionResult.Error("Unable to process the server response. Please try again.")
    } catch (e: Exception) {
      AppLogger.d("GeminiFlyerDataSource", "Failed to process flyer", e)
      FlyerExtractionResult.Error("Unable to process flyer. Please try again.")
    }
  }

  private fun buildPrompt(): String {
    return """
    Extract event details from this flyer image and return them in JSON format.

    Fields to extract:
    - name: The event name
    - startMonth: Event start month as an integer (1-12) if provided. Don't guess.
    - startDay: Event start day of month as an integer (1-31) if provided. Don't guess.
    - startYear: Event start year as an integer (e.g. 2026) if provided. Don't guess.
    - startTime: Event start time in HH:MM format (24-hour)
    - venue: Venue name
    - eventUrl: Event URL
    - artists: Array of artist/band/DJ names performing.
      - On some flyer images where there isn't a list of artists the name of the event may be the name of the main artist that playing.
      - Note: If you see "artist1 B2B artist2" this means that two artists are playing together. Add both artists to the list.

    Return ONLY valid JSON in this exact format:
    {
      "name": "Event Name",
      "startMonth": 3,
      "startDay": 15,
      "startYear": 2026,
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
        resolveStartDate(extractedData.startMonth, extractedData.startDay, extractedData.startYear)

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
      AppLogger.d("GeminiFlyerDataSource", "Failed to parse event data\nResponse: $responseText", e)
      FlyerExtractionResult.Error(
        "Unable to extract event details. The flyer format may not be recognized."
      )
    } catch (e: Exception) {
      AppLogger.d(
        "GeminiFlyerDataSource",
        "Error processing event data\nResponse: $responseText",
        e,
      )
      FlyerExtractionResult.Error("Unable to process event details. Please try again.")
    }
  }

  private fun resolveStartDate(month: Int?, day: Int?, year: Int?): LocalDate? {
    if (month == null || day == null) return null
    return try {
      if (year != null) {
        LocalDate(year, month, day)
      } else {
        val today = clock.now().toLocalDateTime(TimeZone.UTC).date
        val thisYear = LocalDate(today.year, month, day)
        if (thisYear >= today) thisYear else LocalDate(today.year + 1, month, day)
      }
    } catch (e: Exception) {
      AppLogger.w(
        "GeminiFlyerDataSource",
        "Could not resolve date from month=$month day=$day year=$year",
        e,
      )
      null
    }
  }
}
