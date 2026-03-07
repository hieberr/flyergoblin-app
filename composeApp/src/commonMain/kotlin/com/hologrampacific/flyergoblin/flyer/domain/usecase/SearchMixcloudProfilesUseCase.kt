package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudProfileSearchResult

/**
 * Searches Mixcloud for artist profiles and stores results in the in-memory cache.
 *
 * Does not persist anything to disk and does not select a profile.
 */
class SearchMixcloudProfilesUseCase(
  private val mixcloudDataSource: MixcloudDataSource,
  private val profileSearchCache: ProfileSearchCache,
) {
  suspend operator fun invoke(artistName: String): ResultWithRateLimit {
    return when (val result = mixcloudDataSource.searchMixcloudProfiles(artistName)) {
      is MixcloudProfileSearchResult.Success -> {
        profileSearchCache.putMixcloudResults(artistName, result.profiles)
        ResultWithRateLimit.Success
      }

      is MixcloudProfileSearchResult.Error -> ResultWithRateLimit.Error(result.message)
      is MixcloudProfileSearchResult.RateLimited ->
        ResultWithRateLimit.RateLimited(result.blockedUntil)
    }
  }
}
