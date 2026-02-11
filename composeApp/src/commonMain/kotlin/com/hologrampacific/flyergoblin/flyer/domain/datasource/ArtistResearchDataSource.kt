package com.hologrampacific.flyergoblin.flyer.domain.datasource

import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo

/** Result of searching for a profile. */
sealed class ArtistProfileSearchResult {
  /**
   * Profile found successfully.
   *
   * @property profiles All matching SoundCloud profiles sorted by followers descending
   */
  data class Success(val profiles: List<SoundCloudProfileInfo>) : ArtistProfileSearchResult()

  /**
   * Rate limit exceeded.
   *
   * @property resetTime When the rate limit will reset (format: yyyy/MM/dd HH:mm:ss Z)
   * @property maxRequests Maximum number of requests allowed
   * @property timeWindow The time window duration as ISO 8601
   */
  data class RateLimited(val resetTime: String, val maxRequests: Int, val timeWindow: String) :
    ArtistProfileSearchResult()

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
   * @param artistName The name of the artist to research
   * @return An ArtistProfileResult containing either the profile URL or an error
   */
  suspend fun searchSoundCloudProfiles(artistName: String): ArtistProfileSearchResult
}
