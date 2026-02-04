package com.hologrampacific.learnkmp.flyer.data.remote

import com.hologrampacific.learnkmp.util.AppLogger
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerializationException

/**
 * Client for making requests to the Gemini LLM API.
 *
 * Provides methods for text-only and image+text prompts, handling request construction, error
 * handling, and response parsing.
 *
 * @param httpClient The HTTP client for making API requests
 * @param baseUrl The base URL for the Gemini API endpoint
 */
class GeminiApiClient(
  private val httpClient: HttpClient,
  private val baseUrl: String = "https://api.uedo.net",
) {
  private val json = JsonAiResponseParser.json

  suspend fun llmPrompt(
    prompt: String,
    model: String,
    temperature: Double,
    maxOutputTokens: Int,
  ): GeminiResponse {
    val request =
      GeminiRequest(
        contents = listOf(Content(parts = listOf(Part.text(prompt)))),
        generationConfig = GenerationConfig(temperature, maxOutputTokens),
      )

    return sendRequest(request, model)
  }

  suspend fun llmPromptWithImage(
    prompt: String,
    imageBase64: String,
    mimeType: String,
    model: String,
    temperature: Double,
    maxOutputTokens: Int,
  ): GeminiResponse {
    val request =
      GeminiRequest(
        contents =
          listOf(
            Content(
              parts =
                listOf(Part.image(mimeType = mimeType, base64Data = imageBase64), Part.text(prompt))
            )
          ),
        generationConfig = GenerationConfig(temperature, maxOutputTokens),
      )

    return sendRequest(request, model)
  }

  private suspend fun sendRequest(request: GeminiRequest, model: String): GeminiResponse {
    val response =
      httpClient.post("$baseUrl/llm?model=$model") {
        contentType(ContentType.Application.Json)
        setBody(request)
      }

    if (!response.status.isSuccess()) {
      val errorBody = response.bodyAsText()
      AppLogger.e("GeminiApiClient", "Server error (${response.status.value}): $errorBody")
      throw ResponseException(response, errorBody)
    }

    val responseBody = response.bodyAsText()

    val geminiResponse: GeminiResponse =
      try {
        json.decodeFromString<GeminiResponse>(responseBody)
      } catch (e: SerializationException) {
        AppLogger.e(
          "GeminiApiClient",
          "Failed to parse Gemini response: ${e.message}\nResponse: $responseBody",
          e,
        )
        throw e
      }
    return geminiResponse
  }
}
