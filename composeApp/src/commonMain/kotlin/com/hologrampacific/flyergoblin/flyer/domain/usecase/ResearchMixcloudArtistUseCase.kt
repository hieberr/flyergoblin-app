package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.data.remote.ApiRateLimitException
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfile
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfileSearchResults
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.util.AppLogger
import kotlin.time.Clock
import kotlin.time.Instant

/** Result of researching a Mixcloud artist. */
sealed class ResearchMixcloudResult {
  /** Research completed successfully. */
  data object Success : ResearchMixcloudResult()

  /**
   * Research failed with an error.
   *
   * @property message User-friendly error message
   */
  data class Error(val message: String) : ResearchMixcloudResult()

  /**
   * Request was blocked by rate limiting.
   *
   * @property blockedUntil The instant until which requests are blocked
   */
  data class RateLimited(val blockedUntil: Instant) : ResearchMixcloudResult()
}

/**
 * Use case for researching artist information from Mixcloud.
 *
 * @param mixcloudDataSource The data source for Mixcloud profile lookup
 * @param artistRepository The Repository for artists
 */
class ResearchMixcloudArtistUseCase(
  private val mixcloudDataSource: MixcloudDataSource,
  private val artistRepository: ArtistRepository,
) {

  /**
   * Research an artist to find their Mixcloud profile.
   *
   * Performs the search, selects the top result, fetches its shows, and persists everything in a
   * single atomic write.
   *
   * @param artistName The name of the artist to research
   * @return A ResearchMixcloudResult containing the result or an error
   */
  suspend operator fun invoke(artistName: String): ResearchMixcloudResult {
    val profileResult =
      when (val result = mixcloudDataSource.searchMixcloudProfiles(artistName)) {
        is MixcloudProfileSearchResult.Success -> result
        is MixcloudProfileSearchResult.Error -> {
          return ResearchMixcloudResult.Error(result.message)
        }

        is MixcloudProfileSearchResult.RateLimited -> {
          return ResearchMixcloudResult.RateLimited(blockedUntil = result.blockedUntil)
        }
      }

    val topProfile =
      profileResult.profiles.firstOrNull()
        ?: return ResearchMixcloudResult.Error("No Mixcloud profiles found for $artistName")

    // Fetch shows for the top profile before writing anything to the database.
    val shows =
      try {
        mixcloudDataSource.getShowsForProfile(topProfile.key)
      } catch (e: ApiRateLimitException) {
        return ResearchMixcloudResult.RateLimited(blockedUntil = e.blockedUntil)
      } catch (e: Exception) {
        AppLogger.e("ResearchMixcloudArtistUseCase", "Failed to fetch shows for top profile", e)
        emptyList()
      }

    // Build the complete MixcloudInfo (search results + selected profile) and write once.
    val now = Clock.System.now()
    val existingArtist = artistRepository.getArtistByName(artistName)
    val artist =
      (existingArtist ?: Artist(name = artistName)).let {
        it.copy(
          mixcloudInfo =
            (it.mixcloudInfo ?: MixcloudInfo()).copy(
              profileSearchResults =
                MixcloudProfileSearchResults(results = profileResult.profiles, lastUpdated = now),
              profile =
                MixcloudProfile(
                  key = topProfile.key,
                  username = topProfile.username,
                  profileUrl = topProfile.profileUrl,
                  followerCount = topProfile.followerCount,
                  cloudcastCount = topProfile.cloudcastCount,
                  city = topProfile.city,
                  countryCode = topProfile.countryCode,
                  avatarUrl = topProfile.avatarUrl,
                  name = topProfile.name,
                  shows = shows,
                  lastUpdated = now,
                ),
            )
        )
      }

    if (existingArtist != null) {
      artistRepository.updateArtist(artist)
    } else {
      artistRepository.saveArtist(artist)
    }

    return ResearchMixcloudResult.Success
  }
}
