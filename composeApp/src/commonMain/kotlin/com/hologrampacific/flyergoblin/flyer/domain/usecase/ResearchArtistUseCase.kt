package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistResearchDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileSearchResults
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import kotlin.time.Clock
import kotlin.time.Instant

/** Result of researching an artist. */
sealed class ResearchArtistResult {
  /** Research completed successfully. */
  data object Success : ResearchArtistResult()

  /**
   * Rate limit exceeded.
   *
   * @property blockedUntil The instant until which requests are blocked
   */
  data class RateLimited(val blockedUntil: Instant) : ResearchArtistResult()

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
          return ResearchArtistResult.RateLimited(blockedUntil = result.blockedUntil)
        }
        is ArtistProfileSearchResult.Error -> {
          return ResearchArtistResult.Error(result.message)
        }
      }

    val artist =
      Artist(
        name = artistName,
        soundCloudInfo =
          SoundCloudInfo(
            profileSearchResults =
              SoundCloudProfileSearchResults(
                results = profileResult.profiles,
                lastUpdated = Clock.System.now(),
              )
          ),
      )
    artistRepository.saveArtist(artist)

    val topProfile =
      profileResult.profiles.firstOrNull()
        ?: return ResearchArtistResult.Error("No SoundCloud profiles found for $artistName")
    return when (val setProfileResult = setSoundCloudProfileUseCase(artistName, topProfile.id)) {
      is SetSoundCloudProfileResult.Success -> ResearchArtistResult.Success
      is SetSoundCloudProfileResult.Error -> ResearchArtistResult.Error(setProfileResult.message)
      is SetSoundCloudProfileResult.RateLimited ->
        ResearchArtistResult.RateLimited(blockedUntil = setProfileResult.blockedUntil)
    }
  }
}
