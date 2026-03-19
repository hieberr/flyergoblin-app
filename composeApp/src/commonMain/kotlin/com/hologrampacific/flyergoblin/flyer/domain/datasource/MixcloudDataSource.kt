package com.hologrampacific.flyergoblin.flyer.domain.datasource

import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfile
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfileInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudShow
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ResultWithRateLimitData
import kotlin.time.Instant

/** Result of searching for Mixcloud profiles. */
sealed class MixcloudProfileSearchResult {
  /**
   * Profiles found successfully.
   *
   * @property profiles All matching Mixcloud profiles. Result list order is the same as returned by
   *   the api call.
   */
  data class Success(val profiles: List<MixcloudProfileInfo>) : MixcloudProfileSearchResult()

  /**
   * Profile lookup failed with an error.
   *
   * @property message User-friendly error message
   */
  data class Error(val message: String) : MixcloudProfileSearchResult()

  /**
   * Request was blocked due to Mixcloud rate limiting.
   *
   * @property blockedUntil The instant until which requests are blocked
   */
  data class RateLimited(val blockedUntil: Instant) : MixcloudProfileSearchResult()
}

/** DataSource for fetching data from Mixcloud. */
interface MixcloudDataSource {
  /**
   * Search for Mixcloud profiles matching an artist name.
   *
   * Input validation (blank name, length limits, etc.) is the responsibility of this data source.
   * Callers do not need to duplicate those checks.
   *
   * @param artistName The name of the artist to search for
   * @return A MixcloudProfileSearchResult containing either matching profiles or an error
   */
  suspend fun searchMixcloudProfiles(artistName: String): MixcloudProfileSearchResult

  /**
   * Fetches the full Mixcloud profile including shows for a given user key.
   *
   * @param userKey The Mixcloud user key/path (e.g. `/username/`)
   * @return [ResultWithRateLimitData.Success] containing the [MixcloudProfile] on success,
   *   [ResultWithRateLimitData.RateLimited] if rate limited, or [ResultWithRateLimitData.Error] on
   *   failure.
   */
  suspend fun getFullProfile(userKey: String): ResultWithRateLimitData<MixcloudProfile>

  /**
   * Fetches shows (cloudcasts) for a Mixcloud user.
   *
   * @param userKey The Mixcloud user key/path (e.g. `/username/`)
   * @return List of shows (empty list if fetching fails)
   */
  suspend fun getShowsForProfile(userKey: String): List<MixcloudShow>
}
