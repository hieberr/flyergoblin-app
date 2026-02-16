package com.hologrampacific.flyergoblin.flyer.data.datasource

import com.hologrampacific.flyergoblin.flyer.data.remote.ClientErrorException
import com.hologrampacific.flyergoblin.flyer.data.remote.RateLimitException
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
    // Check if we're currently rate limited
    rateLimitMutex.withLock {
      rateLimitResetTime?.let { resetTime ->
        AppLogger.w("SoundCloudDataSource", "Rate limited. Cannot fetch tracks until: $resetTime")
        throw RateLimitException(
          message = "SoundCloud API rate limit exceeded",
          resetTime = resetTime,
          maxRequests = rateLimitMaxRequests,
          timeWindow = rateLimitTimeWindow,
        )
      }
    }

    return try {
      val tracks = soundCloudApiClient.getTracks(profileUrl)

      // Clear rate limit state on successful request
      rateLimitMutex.withLock {
        rateLimitResetTime = null
        rateLimitMaxRequests = 0
        rateLimitTimeWindow = ""
      }

      tracks
    } catch (e: RateLimitException) {
      // Cache rate limit info
      rateLimitMutex.withLock {
        rateLimitResetTime = e.resetTime
        rateLimitMaxRequests = e.maxRequests
        rateLimitTimeWindow = e.timeWindow
      }

      AppLogger.w(
        "SoundCloudDataSource",
        "Rate limit hit fetching tracks. Resets at: ${e.resetTime}",
      )
      throw e
    }
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

      AppLogger.d("SoundCloudDataSource", "Rate limit hit. Resets at: ${e.resetTime}")

      ArtistProfileSearchResult.RateLimited(
        resetTime = e.resetTime,
        maxRequests = e.maxRequests,
        timeWindow = e.timeWindow,
      )
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
