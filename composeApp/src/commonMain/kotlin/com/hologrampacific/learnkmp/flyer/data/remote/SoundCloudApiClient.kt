package com.hologrampacific.learnkmp.flyer.data.remote

import com.hologrampacific.learnkmp.BuildKonfig
import com.hologrampacific.learnkmp.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.learnkmp.util.AppLogger
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class SoundCloudApiClient(private val httpClient: HttpClient) {

  /** Cached OAuth access token */
  private var cachedAccessToken: String? = null
  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  /**
   * Gets a valid OAuth access token for SoundCloud API. Uses cached token if available, otherwise
   * requests a new one.
   *
   * @return Access token, or null if authentication fails
   */
  private suspend fun getAccessToken(): String? {
    // Return cached token if we have one
    cachedAccessToken?.let {
      return it
    }

    return try {
      AppLogger.d("SoundCloudApiClient", "Requesting new OAuth access token")

      val response =
        httpClient.post("https://api.soundcloud.com/oauth2/token") {
          contentType(ContentType.Application.FormUrlEncoded)
          setBody(
            "grant_type=client_credentials&client_id=${BuildKonfig.SOUNDCLOUD_CLIENT_ID}&client_secret=${BuildKonfig.SOUNDCLOUD_CLIENT_SECRET}"
          )
        }

      if (!response.status.isSuccess()) {
        val errorBody = response.bodyAsText()
        AppLogger.e(
          "SoundCloudApiClient",
          "Failed to get access token (${response.status.value}): $errorBody",
        )
        return null
      }

      val tokenResponse = json.decodeFromString<SoundCloudTokenResponse>(response.bodyAsText())
      cachedAccessToken = tokenResponse.accessToken

      AppLogger.d("SoundCloudApiClient", "Successfully obtained access token")
      tokenResponse.accessToken
    } catch (e: Exception) {
      AppLogger.e("SoundCloudApiClient", "Error getting access token: ${e.message}", e)
      null
    }
  }

  /**
   * Fetches popular tracks from a SoundCloud artist profile using the official SoundCloud API.
   *
   * @param profileUrl The artist's SoundCloud profile URL
   * @return List of popular tracks
   */
  suspend fun fetchPopularTracks(profileUrl: String): List<SoundCloudTrack> {
    return try {
      AppLogger.d("SoundCloudApiClient", "Fetching tracks for: $profileUrl")

      // Get OAuth access token
      val accessToken = getAccessToken()
      if (accessToken == null) {
        AppLogger.w("SoundCloudApiClient", "Could not obtain access token")
        return emptyList()
      }

      // Step 1: Resolve the profile URL to get the user ID using official API
      val userId = resolveUserIdFromUrl(profileUrl, accessToken)
      if (userId == null) {
        AppLogger.w("SoundCloudApiClient", "Could not resolve user ID from profile URL")
        return emptyList()
      }

      AppLogger.d("SoundCloudApiClient", "Resolved user ID: $userId")

      // Step 2: Fetch the user's tracks from the official API
      val apiUrl = "https://api.soundcloud.com/users/$userId/tracks"
      val response =
        httpClient.get(apiUrl) {
          header("Authorization", "Bearer $accessToken")
          parameter("limit", 10)
        }

      if (!response.status.isSuccess()) {
        val errorBody = response.bodyAsText()
        AppLogger.w(
          "SoundCloudApiClient",
          "Failed to fetch tracks from API (${response.status.value}): $errorBody",
        )
        return emptyList()
      }

      val tracksResponseJson = response.bodyAsText()
      AppLogger.d("SoundCloudApiClient", "Tracks API response: $tracksResponseJson")

      val tracks = json.decodeFromString<List<SoundCloudTrackResponse>>(tracksResponseJson)

      // Sort by playback count to get popular tracks first, limit to 10
      val popularTracks =
        tracks
          .sortedByDescending { it.playbackCount ?: 0 }
          .take(10)
          .map { track -> SoundCloudTrack(title = track.title, url = track.permalinkUrl) }

      AppLogger.d("SoundCloudApiClient", "Fetched ${popularTracks.size} tracks from API")
      popularTracks
    } catch (e: SerializationException) {
      AppLogger.e("SoundCloudApiClient", "Error parsing SoundCloud API response: ${e.message}", e)
      emptyList()
    } catch (e: Exception) {
      AppLogger.e("SoundCloudApiClient", "Error fetching popular tracks: ${e.message}", e)
      emptyList()
    }
  }

  /**
   * Resolves a SoundCloud profile URL to get the user ID using the official resolve API.
   *
   * @param profileUrl The artist's SoundCloud profile URL
   * @param accessToken OAuth access token for authentication
   * @return The user ID, or null if resolution fails
   */
  private suspend fun resolveUserIdFromUrl(profileUrl: String, accessToken: String): Long? {
    return try {
      val resolveUrl = "https://api.soundcloud.com/resolve"
      AppLogger.d("SoundCloudApiClient", "Resolving URL: $profileUrl")

      val response =
        httpClient.get(resolveUrl) {
          header("Authorization", "Bearer $accessToken")
          parameter("url", profileUrl)
        }

      if (!response.status.isSuccess()) {
        val errorBody = response.bodyAsText()
        AppLogger.w(
          "SoundCloudApiClient",
          "Failed to resolve user ID (${response.status.value}): $errorBody",
        )
        return null
      }

      val responseBody = response.bodyAsText()
      AppLogger.d("SoundCloudApiClient", "Resolve API response: $responseBody")

      val resolveResponse = json.decodeFromString<SoundCloudResolveResponse>(responseBody)
      resolveResponse.id
    } catch (e: Exception) {
      AppLogger.e("SoundCloudApiClient", "Error resolving user ID: ${e.message}", e)
      null
    }
  }
}
