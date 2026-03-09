package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfileInfo

/**
 * Searches Mixcloud for artist profiles and stores results in the in-memory cache. Results are
 * stored in order with the best match on top. Order:
 * - Profiles exactly matching name or username are moved to the top and sorted by follower count.
 * - The remaining profiles maintain the order returned by Mixcloud since it seems to do a very
 *   fuzzy search and attempts to put names that nearly match towards the top.
 */
class SearchMixcloudProfilesUseCase(
  private val mixcloudDataSource: MixcloudDataSource,
  private val profileSearchCache: ProfileSearchCache,
) {
  suspend operator fun invoke(artistName: String): ResultWithRateLimit {
    val trimmedName = artistName.trim()
    return when (val result = mixcloudDataSource.searchMixcloudProfiles(trimmedName)) {
      is MixcloudProfileSearchResult.Success -> {
        val profilesMatchingName: MutableList<MixcloudProfileInfo> = mutableListOf()
        val otherProfiles: MutableList<MixcloudProfileInfo> = mutableListOf()

        for (profile in result.profiles) {
          val usernameMatches = profile.username.compareTo(trimmedName, ignoreCase = true) == 0
          val nameMatches = profile.name?.compareTo(trimmedName, ignoreCase = true) == 0
          if (usernameMatches || nameMatches) {
            profilesMatchingName.add(profile)
          } else {
            otherProfiles.add(profile)
          }
        }
        val combined = profilesMatchingName.sortedByDescending { it.followerCount } + otherProfiles
        profileSearchCache.putMixcloudResults(trimmedName, combined)

        ResultWithRateLimit.Success
      }

      is MixcloudProfileSearchResult.Error -> ResultWithRateLimit.Error(result.message)
      is MixcloudProfileSearchResult.RateLimited ->
        ResultWithRateLimit.RateLimited(result.blockedUntil)
    }
  }
}
