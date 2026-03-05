package com.hologrampacific.flyergoblin.flyer.data.datasource

import com.hologrampacific.flyergoblin.flyer.data.remote.ApiRateLimitException
import com.hologrampacific.flyergoblin.flyer.data.remote.ClientErrorException
import com.hologrampacific.flyergoblin.flyer.data.remote.ServerErrorException
import com.hologrampacific.flyergoblin.flyer.data.remote.SoundCloudApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.SoundCloudApiException
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistResearchDataSource
import com.hologrampacific.flyergoblin.flyer.domain.datasource.SoundCloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.flyergoblin.util.AppLogger
import io.ktor.http.*

/**
 * Implementation of SoundCloudDataSource and ArtistResearchDataSource that wraps the
 * SoundCloudApiClient.
 *
 * @param soundCloudApiClient The API client for SoundCloud requests
 */
class SoundCloudDataSourceImpl(private val soundCloudApiClient: SoundCloudApiClient) :
  SoundCloudDataSource, ArtistResearchDataSource {

  override suspend fun getTracksForProfile(soundCloudUserId: Long): List<SoundCloudTrack> {
    return soundCloudApiClient.getTracks(soundCloudUserId)
  }

  override suspend fun searchSoundCloudProfiles(artistName: String): ArtistProfileSearchResult {
    val trimmedName = artistName.trim()
    if (trimmedName.isBlank()) {
      return ArtistProfileSearchResult.Error("Artist name cannot be empty")
    }
    if (trimmedName.length > 200) {
      return ArtistProfileSearchResult.Error("Artist name is too long")
    }

    return try {
      val users = soundCloudApiClient.searchUsers(trimmedName)

      if (users.isEmpty()) {
        ArtistProfileSearchResult.Error("No SoundCloud profile found for \"$trimmedName\"")
      } else {
        val usersByFollowersCount = users.sortedByDescending { it.followersCount ?: 0 }
        val profiles =
          usersByFollowersCount.map { user ->
            val url =
              user.permalinkUrl ?: "https://soundcloud.com/${user.permalink.encodeURLPathPart()}"
            SoundCloudProfileInfo(
              id = user.id,
              username = user.username,
              profileUrl = url,
              followersCount = user.followersCount,
              trackCount = user.trackCount,
              city = user.city,
              countryCode = user.countryCode,
              avatarUrl = user.avatarUrl,
              fullName = user.fullName,
            )
          }
        ArtistProfileSearchResult.Success(profiles)
      }
    } catch (e: ApiRateLimitException) {
      AppLogger.d("SoundCloudDataSource", "Rate limit hit. Blocked until: ${e.blockedUntil}")
      ArtistProfileSearchResult.RateLimited(blockedUntil = e.blockedUntil)
    } catch (e: ServerErrorException) {
      AppLogger.d("SoundCloudDataSource", "Server error (${e.statusCode})")
      ArtistProfileSearchResult.Error("SoundCloud server error. Please try again later.")
    } catch (e: ClientErrorException) {
      AppLogger.d("SoundCloudDataSource", "Client error (${e.statusCode})")
      ArtistProfileSearchResult.Error("Failed to search SoundCloud. Please try again.")
    } catch (e: SoundCloudApiException) {
      AppLogger.d("SoundCloudDataSource", "API error: ${e.message}")
      ArtistProfileSearchResult.Error("Failed to search SoundCloud.")
    } catch (e: Exception) {
      AppLogger.d("SoundCloudDataSource", "Unexpected error searching profiles", e)
      ArtistProfileSearchResult.Error("An unexpected error occurred")
    }
  }
}
