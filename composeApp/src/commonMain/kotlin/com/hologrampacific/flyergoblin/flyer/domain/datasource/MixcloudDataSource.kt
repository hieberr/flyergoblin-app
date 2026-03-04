package com.hologrampacific.flyergoblin.flyer.domain.datasource

import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfileInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudShow

/** Result of searching for Mixcloud profiles. */
sealed class MixcloudProfileSearchResult {
  /**
   * Profiles found successfully.
   *
   * @property profiles All matching Mixcloud profiles sorted by followers descending
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
   * @property retryAfterSeconds Number of seconds to wait before retrying
   */
  data class RateLimited(val retryAfterSeconds: Int) : MixcloudProfileSearchResult()
}

/** DataSource for fetching data from Mixcloud. */
interface MixcloudDataSource {
  /**
   * Search for Mixcloud profiles matching an artist name.
   *
   * @param artistName The name of the artist to search for
   * @return A MixcloudProfileSearchResult containing either matching profiles or an error
   */
  suspend fun searchMixcloudProfiles(artistName: String): MixcloudProfileSearchResult

  /**
   * Fetches shows (cloudcasts) for a Mixcloud user.
   *
   * @param userKey The Mixcloud user key/path (e.g. `/username/`)
   * @return List of shows (empty list if fetching fails)
   */
  suspend fun getShowsForProfile(userKey: String): List<MixcloudShow>
}
