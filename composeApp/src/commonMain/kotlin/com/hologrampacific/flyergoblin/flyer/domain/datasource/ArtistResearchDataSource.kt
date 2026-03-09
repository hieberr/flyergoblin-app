package com.hologrampacific.flyergoblin.flyer.domain.datasource

import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import kotlin.time.Instant

/** Result of searching for a profile. */
sealed class ArtistProfileSearchResult {
  /**
   * Profile found successfully.
   *
   * @property profiles All matching SoundCloud profiles.
   */
  data class Success(val profiles: List<SoundCloudProfileInfo>) : ArtistProfileSearchResult()

  /**
   * Rate limit exceeded.
   *
   * @property blockedUntil The instant until which requests are blocked
   */
  data class RateLimited(val blockedUntil: Instant) : ArtistProfileSearchResult()

  /**
   * Profile lookup failed with an error.
   *
   * @property message User-friendly error message
   */
  data class Error(val message: String) : ArtistProfileSearchResult()
}

/** DataSource for researching artist profiles using AI. */
interface ArtistResearchDataSource {
  /**
   * Find the SoundCloud profile URL for an artist using AI research.
   *
   * Input validation (blank name, length limits, etc.) is the responsibility of this data source.
   * Callers do not need to duplicate those checks.
   *
   * @param artistName The name of the artist to research
   * @return An ArtistProfileResult containing either the profile URL or an error
   */
  suspend fun searchSoundCloudProfiles(artistName: String): ArtistProfileSearchResult
}
