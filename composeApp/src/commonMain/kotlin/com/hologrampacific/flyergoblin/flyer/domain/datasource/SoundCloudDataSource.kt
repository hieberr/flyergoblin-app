package com.hologrampacific.flyergoblin.flyer.domain.datasource

import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudTrack
import kotlin.time.Instant

/** Result of searching for a profile. */
sealed class SoundcloudProfileSearchResult {
  /**
   * Profile found successfully.
   *
   * @property profiles All matching SoundCloud profiles.
   */
  data class Success(val profiles: List<SoundCloudProfileInfo>) : SoundcloudProfileSearchResult()

  /**
   * Rate limit exceeded.
   *
   * @property blockedUntil The instant until which requests are blocked
   */
  data class RateLimited(val blockedUntil: Instant) : SoundcloudProfileSearchResult()

  /**
   * Profile lookup failed with an error.
   *
   * @property message User-friendly error message
   */
  data class Error(val message: String) : SoundcloudProfileSearchResult()
}

/** DataSource for fetching data from SoundCloud. */
interface SoundCloudDataSource {
  /**
   * Fetches popular tracks from a SoundCloud artist profile.
   *
   * @param soundCloudUserId The artist's SoundCloud profile user id
   * @return List of popular tracks (empty list if fetching fails). Result list is order is the same
   *   as what the api call returns.
   */
  suspend fun getTracksForProfile(soundCloudUserId: Long): List<SoundCloudTrack>

  /**
   * Find the SoundCloud profile URL for an artist.
   *
   * Input validation (blank name, length limits, etc.) is the responsibility of this data source.
   * Callers do not need to duplicate those checks.
   *
   * @param artistName The name of the artist to research
   * @return An ArtistProfileResult containing either the profile URL or an error
   */
  suspend fun searchSoundCloudProfiles(artistName: String): SoundcloudProfileSearchResult
}
