package com.hologrampacific.learnkmp.flyer.data.remote

import com.hologrampacific.learnkmp.flyer.domain.model.SoundCloudTrack

/**
 * API client interface for interacting with SoundCloud's public API. Handles authentication, rate
 * limiting, and data fetching operations.
 */
interface SoundCloudApiClient {
  /**
   * Searches for SoundCloud users by username or display name.
   *
   * @param query The search query string
   * @return List of SoundCloud users matching the query
   * @throws RateLimitException if rate limit is exceeded (includes reset time)
   * @throws ServerErrorException if server returns 5xx error
   * @throws ClientErrorException if client error occurs (4xx)
   * @throws SoundCloudApiException for other API errors
   */
  suspend fun searchUsers(query: String): List<SoundCloudUser>

  /**
   * Gets tracks for a given SoundCloud user profile.
   *
   * @param profileUrl The full SoundCloud profile URL (e.g., "https://soundcloud.com/username")
   * @return List of tracks, or empty list if none found or on error
   * @throws RateLimitException if rate limit is exceeded (includes reset time)
   */
  suspend fun getTracks(profileUrl: String): List<SoundCloudTrack>
}
