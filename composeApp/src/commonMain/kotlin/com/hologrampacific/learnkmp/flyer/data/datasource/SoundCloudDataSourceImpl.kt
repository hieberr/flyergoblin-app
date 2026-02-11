package com.hologrampacific.learnkmp.flyer.data.datasource

import com.hologrampacific.learnkmp.flyer.data.remote.ClientErrorException
import com.hologrampacific.learnkmp.flyer.data.remote.RateLimitException
import com.hologrampacific.learnkmp.flyer.data.remote.ServerErrorException
import com.hologrampacific.learnkmp.flyer.data.remote.SoundCloudApiClient
import com.hologrampacific.learnkmp.flyer.data.remote.SoundCloudApiException
import com.hologrampacific.learnkmp.flyer.domain.datasource.ArtistProfileSearchResult
import com.hologrampacific.learnkmp.flyer.domain.datasource.ArtistResearchDataSource
import com.hologrampacific.learnkmp.flyer.domain.datasource.SoundCloudDataSource
import com.hologrampacific.learnkmp.flyer.domain.model.SoundCloudProfileInfo
import com.hologrampacific.learnkmp.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.learnkmp.util.AppLogger
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implementation of SoundCloudDataSource and ArtistResearchDataSource that wraps the
 * SoundCloudApiClient.
 *
 * Tracks rate limit state and blocks requests when rate limited.
 *
 * @param soundCloudApiClient The API client for SoundCloud requests
 */
class SoundCloudDataSourceImpl(private val soundCloudApiClient: SoundCloudApiClient) :
  SoundCloudDataSource, ArtistResearchDataSource {

  /** Cached rate limit information */
  private var rateLimitResetTime: String? = null
  private var rateLimitMaxRequests: Int = 0
  private var rateLimitTimeWindow: String = ""

  /** Mutex for thread-safe access to rate limit state */
  private val rateLimitMutex = Mutex()

  override suspend fun getTracksForProfile(profileUrl: String): List<SoundCloudTrack> {
    return soundCloudApiClient.getTracks(profileUrl)
  }

  override suspend fun searchSoundCloudProfiles(artistName: String): ArtistProfileSearchResult {
    val trimmedName = artistName.trim()
    if (trimmedName.isBlank()) {
      return ArtistProfileSearchResult.Error("Artist name cannot be empty")
    }
    if (trimmedName.length > 200) {
      return ArtistProfileSearchResult.Error("Artist name is too long")
    }

    // Check if we're currently rate limited
    rateLimitMutex.withLock {
      rateLimitResetTime?.let { resetTime ->
        AppLogger.w("SoundCloudDataSource", "Rate limited. Cannot search until: $resetTime")
        return ArtistProfileSearchResult.RateLimited(
          resetTime = resetTime,
          maxRequests = rateLimitMaxRequests,
          timeWindow = rateLimitTimeWindow,
        )
      }
    }

    return try {
      val users = soundCloudApiClient.searchUsers(trimmedName)

      // Clear rate limit state on successful request
      rateLimitMutex.withLock {
        rateLimitResetTime = null
        rateLimitMaxRequests = 0
        rateLimitTimeWindow = ""
      }

      if (users.isEmpty()) {
        ArtistProfileSearchResult.Error("No SoundCloud profile found for \"$trimmedName\"")
      } else {
        val usersByFollowersCount = users.sortedByDescending { it.followersCount ?: 0 }
        val profiles =
          usersByFollowersCount.map { user ->
            val url =
              user.permalinkUrl ?: "https://soundcloud.com/${user.permalink.encodeURLPathPart()}"
            SoundCloudProfileInfo(
              username = user.username,
              profileUrl = url,
              followersCount = user.followersCount,
              trackCount = user.trackCount,
              city = user.city,
              countryCode = user.countryCode,
            )
          }
        ArtistProfileSearchResult.Success(profiles)
      }
    } catch (e: RateLimitException) {
      // Cache rate limit info
      rateLimitMutex.withLock {
        rateLimitResetTime = e.resetTime
        rateLimitMaxRequests = e.maxRequests
        rateLimitTimeWindow = e.timeWindow
      }

      AppLogger.w("SoundCloudDataSource", "Rate limit hit. Resets at: ${e.resetTime}")

      ArtistProfileSearchResult.RateLimited(
        resetTime = e.resetTime,
        maxRequests = e.maxRequests,
        timeWindow = e.timeWindow,
      )
    } catch (e: ServerErrorException) {
      AppLogger.e("SoundCloudDataSource", "Server error (${e.statusCode}): ${e.message}")
      ArtistProfileSearchResult.Error("SoundCloud server error. Please try again later.")
    } catch (e: ClientErrorException) {
      AppLogger.e("SoundCloudDataSource", "Client error (${e.statusCode}): ${e.message}")
      ArtistProfileSearchResult.Error("Failed to search SoundCloud. Please try again.")
    } catch (e: SoundCloudApiException) {
      AppLogger.e("SoundCloudDataSource", "API error: ${e.message}")
      ArtistProfileSearchResult.Error("Failed to search SoundCloud: ${e.message}")
    } catch (e: Exception) {
      AppLogger.e("SoundCloudDataSource", "Unexpected error: ${e.message}", e)
      ArtistProfileSearchResult.Error("An unexpected error occurred")
    }
  }
}
