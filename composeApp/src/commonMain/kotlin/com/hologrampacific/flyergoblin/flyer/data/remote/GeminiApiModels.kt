package com.hologrampacific.flyergoblin.flyer.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
  val contents: List<Content>,
  val generationConfig: GenerationConfig? = null,
)

@Serializable data class Content(val parts: List<Part>)

@Serializable
data class Part(
  val text: String? = null,
  @SerialName("inline_data") val inlineData: InlineData? = null,
) {
  companion object {
    fun text(text: String) = Part(text = text)

    fun image(mimeType: String, base64Data: String) =
      Part(inlineData = InlineData(mimeType, base64Data))
  }
}

@Serializable data class InlineData(@SerialName("mime_type") val mimeType: String, val data: String)

@Serializable
data class GenerationConfig(val temperature: Double = 0.7, val maxOutputTokens: Int = 2000)

@Serializable
data class GeminiResponse(val candidates: List<Candidate>, val usageMetadata: UsageMetadata? = null)

@Serializable
data class Candidate(
  val content: CandidateContent,
  val finishReason: String,
  val index: Int? = null,
)

@Serializable data class CandidateContent(val parts: List<TextPart>, val role: String)

@Serializable data class TextPart(val text: String)

@Serializable
data class UsageMetadata(
  val promptTokenCount: Int,
  val candidatesTokenCount: Int,
  val totalTokenCount: Int,
)
