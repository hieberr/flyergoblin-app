package com.hologrampacific.learnkmp.flyer.domain.usecase

import com.hologrampacific.learnkmp.flyer.domain.datasource.ArtistProfileResult
import com.hologrampacific.learnkmp.flyer.domain.datasource.ArtistResearchDataSource
import com.hologrampacific.learnkmp.flyer.domain.datasource.SoundCloudDataSource
import com.hologrampacific.learnkmp.flyer.domain.model.Artist
import kotlin.time.Clock

/** Result of researching an artist. */
sealed class ResearchArtistResult {
  /**
   * Research completed successfully.
   *
   * @property artist The artist with populated SoundCloud information
   */
  data class Success(val artist: Artist) : ResearchArtistResult()

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
 * @param artistDataSource The data source for AI-based artist profile lookup
 * @param soundCloudDataSource The data source for fetching SoundCloud tracks
 */
class ResearchArtistUseCase(
  private val artistDataSource: ArtistResearchDataSource,
  private val soundCloudDataSource: SoundCloudDataSource,
) {

  /**
   * Research an artist to find their SoundCloud profile and top tracks.
   *
   * @param artistName The name of the artist to research
   * @return A ResearchArtistResult containing the artist with SoundCloud info or an error
   */
  suspend operator fun invoke(artistName: String): ResearchArtistResult {
    // Step 1: Find the artist's SoundCloud profile.
    val profileUrl =
      when (val profileResult = artistDataSource.findSoundCloudProfile(artistName)) {
        is ArtistProfileResult.Success -> profileResult.soundCloudProfile
        is ArtistProfileResult.Error -> {
          return ResearchArtistResult.Error("Failed to research artist: ${profileResult.message}")
        }
      }

    // Step 2: Fetch popular tracks from SoundCloud
    val tracks = soundCloudDataSource.fetchPopularTracks(profileUrl)

    // Step 3: Create the Artist model
    val artist =
      Artist(
        name = artistName,
        soundCloudProfile = profileUrl,
        soundCloudTracks = tracks,
        lastFetched = Clock.System.now(),
      )

    return ResearchArtistResult.Success(artist)
  }
}
