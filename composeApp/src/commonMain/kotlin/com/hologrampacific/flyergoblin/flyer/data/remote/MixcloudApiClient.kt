package com.hologrampacific.flyergoblin.flyer.data.remote

import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudShow

/**
 * API client interface for interacting with Mixcloud's public API. Handles retries and data
 * fetching operations.
 */
interface MixcloudApiClient {
  /**
   * Searches for Mixcloud users by name.
   *
   * @param query The search query string
   * @return List of Mixcloud users matching the query
   * @throws MixcloudServerErrorException if server returns 5xx error
   * @throws MixcloudClientErrorException if client error occurs (4xx)
   * @throws MixcloudApiException for other API errors
   */
  suspend fun searchUsers(query: String): List<MixcloudUserResult>

  /**
   * Gets the full profile for a Mixcloud user.
   *
   * @param userKey The Mixcloud user key/path (e.g. `/username/`)
   * @return The user profile
   * @throws MixcloudServerErrorException if server returns 5xx error
   * @throws MixcloudClientErrorException if client error occurs (4xx)
   * @throws MixcloudApiException for other API errors
   */
  suspend fun getUserProfile(userKey: String): MixcloudUserProfile

  /**
   * Gets cloudcasts (shows/mixes) for a Mixcloud user.
   *
   * @param userKey The Mixcloud user key/path (e.g. `/username/`)
   * @return List of cloudcasts, or empty list on network or serialization errors (non-fatal)
   * @throws ApiRateLimitException if rate limit is exceeded
   */
  suspend fun getCloudcasts(userKey: String): List<MixcloudShow>
}