package com.hologrampacific.flyergoblin.flyer.data.datasource

import com.hologrampacific.flyergoblin.flyer.data.remote.GeminiApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.JsonAiResponseParser
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistResearchDataSource
import com.hologrampacific.flyergoblin.util.AppLogger
import io.ktor.client.network.sockets.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

// TODO: We can delete this whole class. We don't use gemini to search for profile.
/**
 * Gemini-based implementation of ArtistResearchDataSource that uses Google's Gemini model to find
 * artist SoundCloud profiles.
 *
 * @param geminiApiClient The API client for making Gemini requests
 */
class GeminiArtistDataSource(private val geminiApiClient: GeminiApiClient) :
  ArtistResearchDataSource {

  private val json = JsonAiResponseParser.json

  companion object {
    /** Lower temperature for more factual research results */
    private const val RESEARCH_TEMPERATURE = 0.3

    /** Maximum output tokens for artist research response */
    private const val MAX_OUTPUT_TOKENS = 2000

    /** The Gemini model to use */
    private const val MODEL = "gemini-2.5-flash"
  }

  @Serializable private data class ArtistProfileData(val soundCloudProfile: String?)

  override suspend fun searchSoundCloudProfiles(artistName: String): ArtistProfileSearchResult {
    val trimmedName = artistName.trim()
    if (trimmedName.isBlank()) {
      return ArtistProfileSearchResult.Error("Artist name cannot be empty")
    }
    if (trimmedName.length > 200) {
      return ArtistProfileSearchResult.Error("Artist name is too long")
    }
    return try {
      val prompt = buildPrompt(artistName)
      val geminiResponse =
        geminiApiClient.llmPrompt(prompt, MODEL, RESEARCH_TEMPERATURE, MAX_OUTPUT_TOKENS)

      val responseText =
        geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
          ?: run {
            AppLogger.d("GeminiArtistDataSource", "No response text from LLM service")
            return ArtistProfileSearchResult.Error("Unable to research artist information.")
          }

      parseResponse(responseText)
    } catch (e: ConnectTimeoutException) {
      AppLogger.d("GeminiArtistDataSource", "Connection timeout while researching artist")
      ArtistProfileSearchResult.Error("Connection timeout. Please check your internet connection.")
    } catch (e: SocketTimeoutException) {
      AppLogger.d("GeminiArtistDataSource", "Request timeout while researching artist")
      ArtistProfileSearchResult.Error("Request timeout. The server took too long to respond.")
    } catch (e: SerializationException) {
      AppLogger.d("GeminiArtistDataSource", "Failed to parse server response", e)
      ArtistProfileSearchResult.Error("Unable to process the server response. Please try again.")
    } catch (e: Exception) {
      AppLogger.d("GeminiArtistDataSource", "Failed to research artist", e)
      ArtistProfileSearchResult.Error("Unable to research artist. Please try again.")
    }
  }

  private fun buildPrompt(artistName: String): String {
    return """
        Research the music artist "$artistName" on SoundCloud.

        Find their SoundCloud profile URL.

        Return ONLY valid JSON in this exact format:
        {
          "soundCloudProfile": "https://soundcloud.com/artist-name"
        }

        If the artist is not found on SoundCloud, return:
        {"soundCloudProfile": null}

        Do not include any explanation or additional text, only the JSON object.
        """
      .trimIndent()
  }

  private fun parseResponse(responseText: String): ArtistProfileSearchResult {
    return try {
      val jsonText =
        JsonAiResponseParser.extractJsonFromResponse(responseText, "GeminiArtistDataSource")

      val profileData = json.decodeFromString<ArtistProfileData>(jsonText)

      if (profileData.soundCloudProfile.isNullOrBlank()) {
        ArtistProfileSearchResult.Error("Artist not found on SoundCloud.")
      } else {
        ArtistProfileSearchResult.Success(listOf())
      }
    } catch (e: SerializationException) {
      AppLogger.d(
        "GeminiArtistDataSource",
        "Failed to parse artist data\nResponse: $responseText",
        e,
      )
      ArtistProfileSearchResult.Error(
        "Unable to extract artist information. The response format may not be recognized."
      )
    } catch (e: Exception) {
      AppLogger.d(
        "GeminiArtistDataSource",
        "Error processing artist data\nResponse: $responseText",
        e,
      )
      ArtistProfileSearchResult.Error("Unable to process artist information. Please try again.")
    }
  }
}
