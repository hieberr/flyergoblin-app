package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.data.remote.ApiRateLimitException
import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.datasource.SoundCloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfile
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.util.AppLogger
import kotlin.time.Clock

/**
 * Sets the SoundCloud profile to use for an artist.
 *
 * @param artistRepository Repository for loading and saving artist data
 * @param soundCloudDataSource DataSource for SoundCloud information
 */
class SetSoundCloudProfileUseCase(
  private val artistRepository: ArtistRepository,
  private val soundCloudDataSource: SoundCloudDataSource,
  private val profileSearchCache: ProfileSearchCache,
) {
  /**
   * Sets the selected SoundCloud profile for an artist.
   *
   * @param artistName The name of the artist
   * @param soundCloudUserId The SoundCloud user ID to set, or null to clear the profile
   */
  suspend operator fun invoke(artistName: String, soundCloudUserId: Long?): ResultWithRateLimit {
    val artist = artistRepository.getArtistByName(artistName) ?: Artist(artistName)

    // Handle "None" selection - clear the profile (tracks clear automatically with the profile)
    if (soundCloudUserId == null) {
      if (artist.soundCloudInfo?.profile == null && artist.soundCloudInfo?.profileChosen == true) {
        // Already set to None. Nothing to do.
        return ResultWithRateLimit.Success
      }
      val existingOrNewInfo = artist.soundCloudInfo ?: SoundCloudInfo()
      val updatedArtist =
        artist.copy(soundCloudInfo = existingOrNewInfo.copy(profile = null, profileChosen = true))
      artistRepository.upsertArtist(updatedArtist)
      return ResultWithRateLimit.Success
    }

    if (artist.soundCloudInfo?.profile?.id == soundCloudUserId) {
      // Selected profile didn't change. Nothing to do.
      return ResultWithRateLimit.Success
    }

    val newProfile =
      profileSearchCache.getSoundCloudResults(artistName)?.find { it.id == soundCloudUserId }
    if (newProfile == null) {
      AppLogger.e(
        "SetSoundCloudProfileUseCase",
        "SoundCloud profile with id $soundCloudUserId not found in cache.",
      )
      return ResultWithRateLimit.Error(
        "Could not find SoundCloud profile with id $soundCloudUserId"
      )
    }

    val newTracks =
      try {
        soundCloudDataSource.getTracksForProfile(newProfile.id)
      } catch (e: ApiRateLimitException) {
        return ResultWithRateLimit.RateLimited(blockedUntil = e.blockedUntil)
      } catch (e: Exception) {
        AppLogger.e("SetSoundCloudProfileUseCase", "Failed to fetch tracks for profile", e)
        // Still set the profile but with no tracks. This allows the user to at least see the
        // profile
        emptyList()
      }

    val existingOrNewInfo = artist.soundCloudInfo ?: SoundCloudInfo()
    val profile =
      SoundCloudProfile(
        id = newProfile.id,
        username = newProfile.username,
        profileUrl = newProfile.profileUrl,
        followersCount = newProfile.followersCount,
        trackCount = newProfile.trackCount,
        city = newProfile.city,
        countryCode = newProfile.countryCode,
        avatarUrl = newProfile.avatarUrl,
        fullName = newProfile.fullName,
        tracks = newTracks,
        lastUpdated = Clock.System.now(),
      )
    val updatedArtist =
      artist.copy(soundCloudInfo = existingOrNewInfo.copy(profile = profile, profileChosen = true))
    artistRepository.upsertArtist(updatedArtist)

    return ResultWithRateLimit.Success
  }
}
