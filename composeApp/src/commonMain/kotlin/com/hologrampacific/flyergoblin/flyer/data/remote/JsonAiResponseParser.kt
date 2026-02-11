package com.hologrampacific.flyergoblin.flyer.data.remote

import com.hologrampacific.flyergoblin.util.AppLogger
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Utility object for parsing JSON responses from LLM services.
 *
 * Handles extraction of JSON objects from text that may contain additional formatting or
 * explanation text around the JSON.
 */
object JsonAiResponseParser {

  /**
   * Shared JSON configuration used for parsing API responses. Configured to be lenient and ignore
   * unknown keys for flexibility with LLM responses.
   */
  val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  /**
   * Extracts the first complete JSON object from a text response.
   *
   * This method handles responses where the JSON may be wrapped in markdown code blocks, surrounded
   * by explanatory text, or otherwise embedded in non-JSON content.
   *
   * The extraction:
   * - Finds the first '{' character
   * - Tracks brace depth to find the matching closing '}'
   * - Properly handles escaped characters and string literals
   * - Validates the extracted JSON by attempting to parse it
   *
   * @param text The response text that may contain JSON along with other text
   * @param logTag Optional tag for logging (defaults to "JsonResponseParser")
   * @return The extracted JSON string, or the original text if no valid JSON found
   */
  fun extractJsonFromResponse(text: String, logTag: String = "JsonResponseParser"): String {
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
              AppLogger.w(logTag, "Extracted JSON failed validation: ${e.message}")
              text
            }
          }
        }
      }
    }

    // If we get here, braces weren't balanced
    AppLogger.w(logTag, "Could not find complete JSON object in response")
    return text
  }
}
