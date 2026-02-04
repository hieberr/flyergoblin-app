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
