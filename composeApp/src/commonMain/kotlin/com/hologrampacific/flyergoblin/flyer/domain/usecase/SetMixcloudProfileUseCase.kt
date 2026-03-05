package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.data.remote.ApiRateLimitException
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfile
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.util.AppLogger
import kotlin.time.Clock
import kotlin.time.Instant

/** Result of setting the Mixcloud profile for an artist. */
sealed class SetMixcloudProfileResult {
  /** Completed successfully. */
  data object Success : SetMixcloudProfileResult()

  /**
   * Failed with an error.
   *
   * @property message User-friendly error message
   */
  data class Error(val message: String) : SetMixcloudProfileResult()

  /**
   * Request was blocked by rate limiting.
   *
   * @property blockedUntil The instant until which requests are blocked
   */
  data class RateLimited(val blockedUntil: Instant) : SetMixcloudProfileResult()
}

/**
 * Sets the Mixcloud profile to use for an artist.
 *
 * @param artistRepository Repository for loading and saving artist data
 * @param mixcloudDataSource DataSource for Mixcloud information
 */
class SetMixcloudProfileUseCase(
  private val artistRepository: ArtistRepository,
  private val mixcloudDataSource: MixcloudDataSource,
) {
  /**
   * Sets the selected Mixcloud profile for an artist.
   *
   * @param artistName The name of the artist
   * @param mixcloudUserKey The Mixcloud user key to set, or null to clear the profile
   */
  suspend operator fun invoke(
    artistName: String,
    mixcloudUserKey: String?,
  ): SetMixcloudProfileResult {
    val artist = artistRepository.getArtistByName(artistName)
    if (artist == null) {
      AppLogger.e("SetMixcloudProfileUseCase", "Artist $artistName not found in repository.")
      return SetMixcloudProfileResult.Error("Could not find artist with name $artistName")
    }
    if (artist.mixcloudInfo?.profile?.key == mixcloudUserKey) {
      // Selected profile didn't change. Nothing to do.
      return SetMixcloudProfileResult.Success
    }

    // Handle "None" selection - clear the profile
    if (mixcloudUserKey == null) {
      val updatedArtist = artist.copy(mixcloudInfo = artist.mixcloudInfo?.copy(profile = null))
      artistRepository.updateArtist(updatedArtist)
      return SetMixcloudProfileResult.Success
    }

    val newProfile =
      artist.mixcloudInfo?.profileSearchResults?.results?.find { it.key == mixcloudUserKey }
    if (newProfile == null) {
      AppLogger.e(
        "SetMixcloudProfileUseCase",
        "Mixcloud profile with key $mixcloudUserKey not found in saved profiles.",
      )
      return SetMixcloudProfileResult.Error(
        "Could not find Mixcloud profile with key $mixcloudUserKey"
      )
    }

    val shows =
      try {
        mixcloudDataSource.getShowsForProfile(newProfile.key)
      } catch (e: ApiRateLimitException) {
        return SetMixcloudProfileResult.RateLimited(blockedUntil = e.blockedUntil)
      } catch (e: Exception) {
        AppLogger.e("SetMixcloudProfileUseCase", "Failed to fetch shows for profile", e)
        emptyList()
      }

    val updatedArtist =
      artist.copy(
        mixcloudInfo =
          artist.mixcloudInfo.copy(
            profile =
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
          )
      )
    artistRepository.updateArtist(updatedArtist)

    return SetMixcloudProfileResult.Success
  }
}
