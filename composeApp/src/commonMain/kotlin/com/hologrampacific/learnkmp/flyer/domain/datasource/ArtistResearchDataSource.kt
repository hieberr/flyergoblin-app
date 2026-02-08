package com.hologrampacific.learnkmp.flyer.domain.datasource

/** Result of finding an artist's profile using AI research. */
sealed class ArtistProfileResult {
  /**
   * Profile found successfully.
   *
   * @property soundCloudProfile The artist's SoundCloud profile URL
   */
  data class Success(val soundCloudProfile: String) : ArtistProfileResult()

  /**
   * Rate limit exceeded.
   *
   * @property resetTime When the rate limit will reset (format: yyyy/MM/dd HH:mm:ss Z)
   * @property maxRequests Maximum number of requests allowed
   * @property timeWindow The time window duration as ISO 8601
   */
  data class RateLimited(
    val resetTime: String,
    val maxRequests: Int,
    val timeWindow: String,
  ) : ArtistProfileResult()

  /**
   * Profile lookup failed with an error.
   *
   * @property message User-friendly error message
   */
  data class Error(val message: String) : ArtistProfileResult()
}

/** DataSource for researching artist profiles using AI. */
interface ArtistResearchDataSource {
  /**
   * Find the SoundCloud profile URL for an artist using AI research.
   *
   * @param artistName The name of the artist to research
   * @return An ArtistProfileResult containing either the profile URL or an error
   */
  suspend fun findSoundCloudProfile(artistName: String): ArtistProfileResult
}
