package com.hologrampacific.flyergoblin.flyer.data.remote

/** Client interface for making requests to the Gemini LLM API. */
interface GeminiApiClient {
  /**
   * Sends a text-only prompt to the Gemini LLM.
   *
   * @param prompt The text prompt
   * @param model The Gemini model identifier
   * @param temperature Sampling temperature (0.0–1.0)
   * @param maxOutputTokens Maximum tokens in the response
   * @return The Gemini API response
   */
  suspend fun llmPrompt(
    prompt: String,
    model: String,
    temperature: Double,
    maxOutputTokens: Int,
  ): GeminiResponse

  /**
   * Sends an image and text prompt to the Gemini LLM.
   *
   * @param prompt The text prompt
   * @param imageBase64 The image encoded as Base64
   * @param mimeType The MIME type of the image (e.g., "image/jpeg")
   * @param model The Gemini model identifier
   * @param temperature Sampling temperature (0.0–1.0)
   * @param maxOutputTokens Maximum tokens in the response
   * @return The Gemini API response
   */
  suspend fun llmPromptWithImage(
    prompt: String,
    imageBase64: String,
    mimeType: String,
    model: String,
    temperature: Double,
    maxOutputTokens: Int,
  ): GeminiResponse
}
