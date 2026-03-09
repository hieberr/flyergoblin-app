package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistResearchDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo

/**
 * Searches SoundCloud for artist profiles and stores results in the in-memory cache. Results are
 * stored in order with the best match on top. Order:
 * - Profiles are sorted by follower count. Then profiles where username or full name exactly
 *   matches the artist name are moved to the top.
 */
class SearchSoundCloudProfilesUseCase(
  private val artistDataSource: ArtistResearchDataSource,
  private val profileSearchCache: ProfileSearchCache,
) {
  suspend operator fun invoke(artistName: String): ResultWithRateLimit {
    val trimmedName = artistName.trim()
    return when (val result = artistDataSource.searchSoundCloudProfiles(trimmedName)) {
      is ArtistProfileSearchResult.Success -> {
        val profilesMatchingName: MutableList<SoundCloudProfileInfo> = mutableListOf()
        val otherProfiles: MutableList<SoundCloudProfileInfo> = mutableListOf()

        val profilesByFollowerCount = result.profiles.sortedByDescending { it.followersCount }
        for (profile in profilesByFollowerCount) {
          val usernameMatches = profile.username.compareTo(trimmedName, ignoreCase = true) == 0
          val nameMatches = profile.fullName?.compareTo(trimmedName, ignoreCase = true) == 0
          if (usernameMatches || nameMatches) {
            profilesMatchingName.add(profile)
          } else {
            otherProfiles.add(profile)
          }
        }
        profileSearchCache.putSoundCloudResults(trimmedName, profilesMatchingName + otherProfiles)
        ResultWithRateLimit.Success
      }

      is ArtistProfileSearchResult.RateLimited ->
        ResultWithRateLimit.RateLimited(result.blockedUntil)

      is ArtistProfileSearchResult.Error -> ResultWithRateLimit.Error(result.message)
    }
  }
}
