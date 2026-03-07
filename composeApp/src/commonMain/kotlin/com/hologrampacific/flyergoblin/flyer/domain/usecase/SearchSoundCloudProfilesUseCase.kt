package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistResearchDataSource

/**
 * Searches SoundCloud for artist profiles and stores results in the in-memory cache.
 *
 * Does not persist anything to disk and does not select a profile.
 */
class SearchSoundCloudProfilesUseCase(
  private val artistDataSource: ArtistResearchDataSource,
  private val profileSearchCache: ProfileSearchCache,
) {
  suspend operator fun invoke(artistName: String): ResultWithRateLimit {
    return when (val result = artistDataSource.searchSoundCloudProfiles(artistName)) {
      is ArtistProfileSearchResult.Success -> {
        profileSearchCache.putSoundCloudResults(artistName, result.profiles)
        ResultWithRateLimit.Success
      }

      is ArtistProfileSearchResult.RateLimited ->
        ResultWithRateLimit.RateLimited(result.blockedUntil)

      is ArtistProfileSearchResult.Error -> ResultWithRateLimit.Error(result.message)
    }
  }
}
