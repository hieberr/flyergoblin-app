package com.hologrampacific.learnkmp.flyer.data.service

import com.hologrampacific.learnkmp.flyer.domain.model.Event
import com.hologrampacific.learnkmp.flyer.domain.service.FlyerAiProcessingResult
import com.hologrampacific.learnkmp.flyer.domain.service.FlyerAiService
import com.hologrampacific.learnkmp.util.AppLogger
import io.ktor.client.*
import io.ktor.client.network.sockets.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class GeminiFlyerAiService(
  private val httpClient: HttpClient,
  private val baseUrl: String = "https://api.uedo.net",
  private val model: String = "gemini-2.5-flash",
) : FlyerAiService {

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  companion object {
    // AI Model Configuration
    /** Lower temperature (0.0-1.0) for more consistent, factual extraction from flyer images */
    private const val EXTRACTION_TEMPERATURE = 0.3

    /** Maximum output tokens - sufficient for event JSON response with all fields */
    private const val MAX_OUTPUT_TOKENS = 2000

    // Image Size Limits
    /** Maximum image size in megabytes to prevent memory issues and excessive processing time */
    private const val MAX_IMAGE_SIZE_MB = 10

    /** Minimum image size in bytes to ensure the image has enough data to be processed */
    private const val MIN_IMAGE_SIZE_BYTES = 1024
  }

  @OptIn(ExperimentalEncodingApi::class)
  override suspend fun processFlyer(
    imageBytes: ByteArray,
    mimeType: String,
  ): FlyerAiProcessingResult {

    val mbBytes = 1024 * 1024
    if (imageBytes.size > MAX_IMAGE_SIZE_MB * mbBytes) {
      return FlyerAiProcessingResult.Error(
        "Image too large (${imageBytes.size / mbBytes}MB). Maximum size is ${MAX_IMAGE_SIZE_MB}MB."
      )
    }

    // Warn for very small images
    if (imageBytes.size < MIN_IMAGE_SIZE_BYTES) {
      return FlyerAiProcessingResult.Error(
        "Image too small (${imageBytes.size} bytes). Please provide a clear flyer image."
      )
    }

    return try {
      val base64Image = Base64.encode(imageBytes)

      val prompt = buildPrompt()

      val request =
        GeminiRequest(
          contents =
            listOf(
              Content(
                parts =
                  listOf(
                    Part.image(mimeType = mimeType, base64Data = base64Image),
                    Part.text(prompt),
                  )
              )
            ),
          generationConfig =
            GenerationConfig(
              temperature = EXTRACTION_TEMPERATURE,
              maxOutputTokens = MAX_OUTPUT_TOKENS,
            ),
        )

      val response =
        httpClient.post("$baseUrl/llm?model=$model") {
          contentType(ContentType.Application.Json)
          setBody(request)
        }

      if (!response.status.isSuccess()) {
        val errorBody = response.bodyAsText()
        AppLogger.e("GeminiFlyerAiService", "Server error (${response.status.value}): $errorBody")
        return FlyerAiProcessingResult.Error(
          "Unable to process flyer. The server returned an error."
        )
      }

      val responseBody = response.bodyAsText()
      AppLogger.d("GeminiFlyerAiService", "Gemini API Response: $responseBody")

      val geminiResponse: GeminiResponse =
        try {
          json.decodeFromString<GeminiResponse>(responseBody)
        } catch (e: SerializationException) {
          AppLogger.e(
            "GeminiFlyerAiService",
            "Failed to parse Gemini response: ${e.message}\nResponse: $responseBody",
            e,
          )
          return FlyerAiProcessingResult.Error(
            "Unable to process the server response. Please try again."
          )
        }

      val responseText =
        geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
          ?: run {
            AppLogger.e("GeminiFlyerAiService", "No response text from LLM service")
            return FlyerAiProcessingResult.Error("Unable to extract event details from the flyer.")
          }

      parseEventData(responseText)
    } catch (e: ConnectTimeoutException) {
      AppLogger.e("GeminiFlyerAiService", "Connection timeout while processing flyer", e)
      FlyerAiProcessingResult.Error("Connection timeout. Please check your internet connection.")
    } catch (e: SocketTimeoutException) {
      AppLogger.e("GeminiFlyerAiService", "Request timeout while processing flyer", e)
      FlyerAiProcessingResult.Error("Request timeout. The server took too long to respond.")
    } catch (e: SerializationException) {
      AppLogger.e("GeminiFlyerAiService", "Failed to parse server response: ${e.message}", e)
      FlyerAiProcessingResult.Error("Unable to process the server response. Please try again.")
    } catch (e: Exception) {
      AppLogger.e("GeminiFlyerAiService", "Failed to process flyer: ${e.message}", e)
      FlyerAiProcessingResult.Error("Unable to process flyer. Please try again.")
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

  private fun parseEventData(responseText: String): FlyerAiProcessingResult {
    return try {
      val jsonText = extractJsonFromResponse(responseText)

      val extractedData = json.decodeFromString<ExtractedEventData>(jsonText)

      // Handle "Unknown" or blank values for required fields
      val eventName = extractedData.name.takeIf { it.isNotBlank() } ?: "Unknown"

      // If startDate is "Unknown" or blank, use today's date
      val startDate =
        if (
          extractedData.startDate.isBlank() ||
            extractedData.startDate.equals("Unknown", ignoreCase = true)
        ) {
          null
        } else {
          null // LocalDate.parse(extractedData.startDate)
        }

      val startTime = extractedData.startTime?.let { LocalTime.parse(it) }

      val event =
        Event(
          id = Event.generateId(),
          name = eventName,
          startDate = startDate,
          startTime = startTime,
          venue = extractedData.venue,
          eventUrl = extractedData.eventUrl,
          artists = extractedData.artists,
          dateAdded = Clock.System.now(),
        )

      FlyerAiProcessingResult.Success(event)
    } catch (e: SerializationException) {
      AppLogger.e(
        "GeminiFlyerAiService",
        "Failed to parse event data: ${e.message}\nResponse: $responseText",
        e,
      )
      FlyerAiProcessingResult.Error(
        "Unable to extract event details. The flyer format may not be recognized."
      )
    } catch (e: IllegalArgumentException) {
      AppLogger.e(
        "GeminiFlyerAiService",
        "Invalid date format in response: ${e.message}\nResponse: $responseText",
        e,
      )
      FlyerAiProcessingResult.Error(
        "Unable to extract valid event dates. Please check the flyer content."
      )
    } catch (e: Exception) {
      AppLogger.e(
        "GeminiFlyerAiService",
        "Error processing event data: ${e.message}\nResponse: $responseText",
        e,
      )
      FlyerAiProcessingResult.Error("Unable to process event details. Please try again.")
    }
  }

  /**
   * Extracts the first complete JSON object from a text response. Properly handles nested braces by
   * tracking brace depth.
   *
   * @param text The response text that may contain JSON along with other text
   * @return The extracted JSON string, or the original text if no valid JSON found
   */
  private fun extractJsonFromResponse(text: String): String {
    val jsonStart = text.indexOf("{")
    if (jsonStart == -1) {
      return text
    }

    var braceDepth = 0
    var inString = false
    var escapeNext = false

    for (i in jsonStart until text.length) {
      val char = text[i]

      when {
        escapeNext -> {
          escapeNext = false
        }
        char == '\\' && inString -> {
          escapeNext = true
        }
        char == '"' -> {
          inString = !inString
        }
        !inString && char == '{' -> {
          braceDepth++
        }
        !inString && char == '}' -> {
          braceDepth--
          if (braceDepth == 0) {
            // Found the end of the JSON object
            val extractedJson = text.substring(jsonStart, i + 1)
            // Validate by attempting to parse
            return try {
              json.parseToJsonElement(extractedJson)
              extractedJson
            } catch (e: SerializationException) {
              AppLogger.w("GeminiFlyerAiService", "Extracted JSON failed validation: ${e.message}")
              text
            }
          }
        }
      }
    }

    // If we get here, braces weren't balanced
    AppLogger.w("GeminiFlyerAiService", "Could not find complete JSON object in response")
    return text
  }
}
