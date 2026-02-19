package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.data.remote.RateLimitException
import com.hologrampacific.flyergoblin.flyer.domain.datasource.SoundCloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfile
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.util.AppLogger

/** Result of setting the SoundCloud profile for an artist. */
sealed class SetSoundCloudProfileResult {
  /** Completed successfully. */
  data object Success : SetSoundCloudProfileResult()

  /**
   * Rate limit exceeded.
   *
   * @property resetTime When the rate limit will reset (format: yyyy/MM/dd HH:mm:ss Z)
   */
  data class RateLimited(val resetTime: String) : SetSoundCloudProfileResult()

  /**
   * Research failed with an error.
   *
   * @property message User-friendly error message
   */
  data class Error(val message: String) : SetSoundCloudProfileResult()
}

/**
 * Sets the SoundCloud profile to use for an artist.
 *
 * @param artistRepository Repository for loading and saving artist data
 * @param soundCloudDataSource DataSource for SoundCloud information
 */
class SetSoundCloudProfileUseCase(
  private val artistRepository: ArtistRepository,
  private val soundCloudDataSource: SoundCloudDataSource,
) {
  suspend operator fun invoke(
    artistName: String,
    soundCloudProfileUrl: String?,
  ): SetSoundCloudProfileResult {
    val artist = artistRepository.getArtistByName(artistName)
    if (artist == null) {
      AppLogger.e("SetSoundCloudProfileUseCase", "Artist $artistName not found in repository.")
      return SetSoundCloudProfileResult.Error("Could not find artist with name $artistName")
    }
    if (artist.soundCloudInfo?.profile?.profileUrl == soundCloudProfileUrl) {
      // Selected profile didn't change. Nothing to do.
      return SetSoundCloudProfileResult.Success
    }

    // Handle "None" selection - clear the profile (tracks clear automatically with the profile)
    if (soundCloudProfileUrl == null) {
      val updatedArtist = artist.copy(soundCloudInfo = artist.soundCloudInfo?.copy(profile = null))
      artistRepository.updateArtist(updatedArtist)
      return SetSoundCloudProfileResult.Success
    }

    val newProfile =
      artist.soundCloudInfo?.profileSearchResults?.results?.find {
        it.profileUrl == soundCloudProfileUrl
      }
    if (newProfile == null) {
      AppLogger.e(
        "SetSoundCloudProfileUseCase",
        "SoundCloud profile $soundCloudProfileUrl not found in saved profiles.",
      )
      return SetSoundCloudProfileResult.Error(
        "Could not find SoundCloud profile $soundCloudProfileUrl"
      )
    }

    val newTracks =
      try {
        soundCloudDataSource.getTracksForProfile(newProfile.profileUrl)
      } catch (e: RateLimitException) {
        return SetSoundCloudProfileResult.RateLimited(resetTime = e.resetTime)
      } catch (e: Exception) {
        AppLogger.e("SetSoundCloudProfileUseCase", "Failed to fetch tracks for profile", e)
        // Still set the profile but with no tracks
        // This allows the user to at least see the profile
        emptyList()
      }
    val updatedArtist =
      artist.copy(
        soundCloudInfo =
          artist.soundCloudInfo.copy(
            profile =
              SoundCloudProfile(
                username = newProfile.username,
                profileUrl = newProfile.profileUrl,
                followersCount = newProfile.followersCount,
                trackCount = newProfile.trackCount,
                city = newProfile.city,
                countryCode = newProfile.countryCode,
                avatarUrl = newProfile.avatarUrl,
                fullName = newProfile.fullName,
                tracks = newTracks,
              )
          )
      )
    artistRepository.updateArtist(updatedArtist)

    return SetSoundCloudProfileResult.Success
  }
}
