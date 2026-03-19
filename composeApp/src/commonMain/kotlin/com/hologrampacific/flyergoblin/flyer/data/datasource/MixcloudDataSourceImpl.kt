package com.hologrampacific.flyergoblin.flyer.data.datasource

import com.hologrampacific.flyergoblin.flyer.data.remote.ApiRateLimitException
import com.hologrampacific.flyergoblin.flyer.data.remote.MixcloudApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.MixcloudApiException
import com.hologrampacific.flyergoblin.flyer.data.remote.MixcloudClientErrorException
import com.hologrampacific.flyergoblin.flyer.data.remote.MixcloudServerErrorException
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfile
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfileInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudShow
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ResultWithRateLimitData
import com.hologrampacific.flyergoblin.util.AppLogger
import kotlin.time.Clock

/**
 * Implementation of MixcloudDataSource that wraps the MixcloudApiClient.
 *
 * For each search result, fetches the full user profile to get follower/cloudcast counts, then
 * returns enriched MixcloudProfileInfo objects sorted by relevance: The top result should be the
 * best match according to Mixcloud's search.
 *
 * @param mixcloudApiClient The API client for Mixcloud requests
 */
class MixcloudDataSourceImpl(private val mixcloudApiClient: MixcloudApiClient) :
  MixcloudDataSource {

  override suspend fun searchMixcloudProfiles(artistName: String): MixcloudProfileSearchResult {
    val trimmedName = artistName.trim()
    if (trimmedName.isBlank()) {
      return MixcloudProfileSearchResult.Error("Artist name cannot be empty")
    }
    if (trimmedName.length > 200) {
      return MixcloudProfileSearchResult.Error("Artist name is too long")
    }

    return try {
      val users = mixcloudApiClient.searchUsers(trimmedName)

      if (users.isEmpty()) {
        MixcloudProfileSearchResult.Error("No Mixcloud profile found for \"$trimmedName\"")
      } else {
        val profiles: MutableList<MixcloudProfileInfo> = mutableListOf()

        for (user in users) {
          try {
            val fullProfile = mixcloudApiClient.getUserProfile(user.key)

            // Filter out profiles without any shows.
            // Treat null cloudcastCount as 0 (unknown = exclude to avoid showing empty profiles).
            if ((fullProfile.cloudcastCount ?: 0) == 0) {
              continue
            }
            profiles.add(
              MixcloudProfileInfo(
                key = fullProfile.key,
                username = fullProfile.username,
                profileUrl = fullProfile.url,
                followerCount = fullProfile.followerCount,
                cloudcastCount = fullProfile.cloudcastCount,
                city = fullProfile.city,
                countryCode = fullProfile.country,
                avatarUrl = fullProfile.pictures?.large,
                name = fullProfile.name,
              )
            )
          } catch (e: ApiRateLimitException) {
            throw e
          } catch (e: Exception) {
            AppLogger.w(
              "MixcloudDataSource",
              "Failed to fetch profile for ${user.key}: ${e.message}",
            )
          }
        }

        if (profiles.isEmpty()) {
          MixcloudProfileSearchResult.Error(
            "No Mixcloud profiles with shows found for \"$trimmedName\""
          )
        } else {
          MixcloudProfileSearchResult.Success(profiles)
        }
      }
    } catch (e: ApiRateLimitException) {
      AppLogger.w("MixcloudDataSource", "Rate limit hit. Blocked until: ${e.blockedUntil}")
      MixcloudProfileSearchResult.RateLimited(blockedUntil = e.blockedUntil)
    } catch (e: MixcloudServerErrorException) {
      AppLogger.d("MixcloudDataSource", "Server error (${e.statusCode})")
      MixcloudProfileSearchResult.Error("Mixcloud server error. Please try again later.")
    } catch (e: MixcloudClientErrorException) {
      AppLogger.d("MixcloudDataSource", "Client error (${e.statusCode})")
      MixcloudProfileSearchResult.Error("Failed to search Mixcloud. Please try again.")
    } catch (e: MixcloudApiException) {
      AppLogger.d("MixcloudDataSource", "API error: ${e.message}")
      MixcloudProfileSearchResult.Error("Failed to search Mixcloud.")
    } catch (e: Exception) {
      AppLogger.d("MixcloudDataSource", "Unexpected error searching profiles", e)
      MixcloudProfileSearchResult.Error("An unexpected error occurred")
    }
  }

  override suspend fun getFullProfile(userKey: String): ResultWithRateLimitData<MixcloudProfile> {
    return try {
      val apiProfile = mixcloudApiClient.getUserProfile(userKey)
      val shows = mixcloudApiClient.getCloudcasts(userKey)

      val mixcloudProfile =
        MixcloudProfile(
          key = userKey,
          username = apiProfile.username,
          profileUrl = apiProfile.url,
          followerCount = apiProfile.followerCount,
          cloudcastCount = apiProfile.cloudcastCount,
          city = apiProfile.city,
          countryCode = apiProfile.country,
          avatarUrl = apiProfile.pictures?.large,
          name = apiProfile.name,
          shows = shows,
          lastUpdated = Clock.System.now(),
        )

      ResultWithRateLimitData.Success(mixcloudProfile)
    } catch (e: ApiRateLimitException) {
      AppLogger.w("MixcloudDataSource", "Rate limit hit. Blocked until: ${e.blockedUntil}")
      ResultWithRateLimitData.RateLimited(blockedUntil = e.blockedUntil)
    } catch (e: MixcloudServerErrorException) {
      AppLogger.d("MixcloudDataSource", "Server error (${e.statusCode})")
      ResultWithRateLimitData.Error("Mixcloud server error. Please try again later.")
    } catch (e: MixcloudClientErrorException) {
      AppLogger.d("MixcloudDataSource", "Client error (${e.statusCode})")
      ResultWithRateLimitData.Error("Failed to get Mixcloud profile info. Please try again.")
    } catch (e: MixcloudApiException) {
      AppLogger.d("MixcloudDataSource", "API error: ${e.message}")
      ResultWithRateLimitData.Error("Failed to get Mixcloud profile info.")
    } catch (e: Exception) {
      AppLogger.d("MixcloudDataSource", "Unexpected error searching profiles", e)
      ResultWithRateLimitData.Error("An unexpected error occurred")
    }
  }

  override suspend fun getShowsForProfile(userKey: String): List<MixcloudShow> {
    return mixcloudApiClient.getCloudcasts(userKey)
  }
}
