package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.data.remote.ApiRateLimitException
import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfile
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.util.AppLogger
import kotlin.time.Clock

/**
 * Sets the Mixcloud profile to use for an artist.
 *
 * @param artistRepository Repository for loading and saving artist data
 * @param mixcloudDataSource DataSource for Mixcloud information
 */
class SetMixcloudProfileUseCase(
  private val artistRepository: ArtistRepository,
  private val mixcloudDataSource: MixcloudDataSource,
  private val profileSearchCache: ProfileSearchCache,
) {
  /**
   * Sets the selected Mixcloud profile for an artist.
   *
   * @param artistName The name of the artist
   * @param mixcloudUserKey The Mixcloud user key to set, or null to clear the profile
   */
  suspend operator fun invoke(artistName: String, mixcloudUserKey: String?): ResultWithRateLimit {
    val artist = artistRepository.getArtistByName(artistName) ?: Artist(artistName)

    if (artist.mixcloudInfo?.profile?.key == mixcloudUserKey) {
      // Selected profile didn't change. Nothing to do.
      return ResultWithRateLimit.Success
    }

    // Handle "None" selection - clear the profile
    if (mixcloudUserKey == null) {
      val updatedArtist = artist.copy(mixcloudInfo = artist.mixcloudInfo?.copy(profile = null))
      artistRepository.upsertArtist(updatedArtist)
      return ResultWithRateLimit.Success
    }

    val newProfile =
      profileSearchCache.getMixcloudResults(artistName)?.find { it.key == mixcloudUserKey }
    if (newProfile == null) {
      AppLogger.e(
        "SetMixcloudProfileUseCase",
        "Mixcloud profile with key $mixcloudUserKey not found in cache.",
      )
      return ResultWithRateLimit.Error("Could not find Mixcloud profile with key $mixcloudUserKey")
    }

    val shows =
      try {
        mixcloudDataSource.getShowsForProfile(newProfile.key)
      } catch (e: ApiRateLimitException) {
        return ResultWithRateLimit.RateLimited(blockedUntil = e.blockedUntil)
      } catch (e: Exception) {
        AppLogger.e("SetMixcloudProfileUseCase", "Failed to fetch shows for profile", e)
        // Still set the profile but with no tracks. This allows the user to at least see the
        // profile
        emptyList()
      }

    val existingOrNewInfo = artist.mixcloudInfo ?: MixcloudInfo()
    val profile =
      MixcloudProfile(
        key = newProfile.key,
        username = newProfile.username,
        profileUrl = newProfile.profileUrl,
        followerCount = newProfile.followerCount,
        cloudcastCount = newProfile.cloudcastCount,
        city = newProfile.city,
        countryCode = newProfile.countryCode,
        avatarUrl = newProfile.avatarUrl,
        name = newProfile.name,
        shows = shows,
        lastUpdated = Clock.System.now(),
      )
    val updatedArtist = artist.copy(mixcloudInfo = existingOrNewInfo.copy(profile = profile))
    artistRepository.upsertArtist(updatedArtist)

    return ResultWithRateLimit.Success
  }
}
