package com.hologrampacific.flyergoblin.flyer.data.remote

import com.hologrampacific.flyergoblin.util.AppLogger
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerializationException

/**
 * Ktor-based implementation of [GeminiApiClient] that communicates with the Gemini LLM API.
 *
 * @param httpClient The HTTP client for making API requests
 * @param baseUrl The base URL for the Gemini API endpoint
 */
class GeminiApiClientImpl(
  private val httpClient: HttpClient,
  private val baseUrl: String = "https://api.uedo.net",
) : GeminiApiClient {
  private val json = JsonAiResponseParser.json

  override suspend fun llmPrompt(
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

  override suspend fun llmPromptWithImage(
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
      httpClient.post("$baseUrl/llm") {
        parameter("model", model)
        contentType(ContentType.Application.Json)
        setBody(request)
      }

    val responseBody = response.bodyAsText()

    if (!response.status.isSuccess()) {
      AppLogger.e("GeminiApiClient", "Server error (${response.status.value}): $responseBody")
      throw ResponseException(response, responseBody)
    }

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
