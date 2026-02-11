package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistResearchDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import kotlin.time.Clock

/** Result of researching an artist. */
sealed class ResearchArtistResult {
  /** Research completed successfully. */
  data object Success : ResearchArtistResult()

  /**
   * Rate limit exceeded.
   *
   * @property resetTime When the rate limit will reset (format: yyyy/MM/dd HH:mm:ss Z)
   */
  data class RateLimited(val resetTime: String) : ResearchArtistResult()

  /**
   * Research failed with an error.
   *
   * @property message User-friendly error message
   */
  data class Error(val message: String) : ResearchArtistResult()
}

/**
 * Use case for researching artist information from SoundCloud.
 *
 * @param artistDataSource The data source for artist profile lookup
 * @param artistRepository The Repository for artists
 * @param setSoundCloudProfileUseCase The UseCase for setting the SoundCloud profile.
 */
class ResearchArtistUseCase(
  private val artistDataSource: ArtistResearchDataSource,
  private val artistRepository: ArtistRepository,
  private val setSoundCloudProfileUseCase: SetSoundCloudProfileUseCase,
) {

  /**
   * Research an artist to find their SoundCloud profile.
   *
   * @param artistName The name of the artist to research
   * @return A ResearchArtistResult containing the artist with SoundCloud info or an error
   */
  suspend operator fun invoke(artistName: String): ResearchArtistResult {
    val profileResult =
      when (val result = artistDataSource.searchSoundCloudProfiles(artistName)) {
        is ArtistProfileSearchResult.Success -> result
        is ArtistProfileSearchResult.RateLimited -> {
          return ResearchArtistResult.RateLimited(resetTime = result.resetTime)
        }
        is ArtistProfileSearchResult.Error -> {
          return ResearchArtistResult.Error("Failed to find artist: ${result.message}")
        }
      }

    val artist =
      Artist(
        name = artistName,
        soundCloudProfile = null,
        soundCloudProfiles = profileResult.profiles,
        soundCloudTracks = listOf(),
        lastFetched = Clock.System.now(),
      )
    artistRepository.updateArtist(artist)

    val topProfile = profileResult.profiles.first()
    val setProfileResult = setSoundCloudProfileUseCase(artistName, topProfile.profileUrl)
    return when (setProfileResult) {
      is SetSoundCloudProfileResult.Success -> ResearchArtistResult.Success
      is SetSoundCloudProfileResult.Error ->
        ResearchArtistResult.Error("Failed to set soundcloud profile: ${setProfileResult.message} ")
      is SetSoundCloudProfileResult.RateLimited ->
        ResearchArtistResult.RateLimited(resetTime = setProfileResult.resetTime)
    }
  }
}
