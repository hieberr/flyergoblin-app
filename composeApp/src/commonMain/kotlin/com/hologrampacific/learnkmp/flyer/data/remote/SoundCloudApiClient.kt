package com.hologrampacific.learnkmp.flyer.data.remote

import com.hologrampacific.learnkmp.flyer.domain.model.SoundCloudTrack

/**
 * API client interface for interacting with SoundCloud's public API.
 * Handles authentication, rate limiting, and data fetching operations.
 */
interface SoundCloudApiClient {
  /**
   * Searches for SoundCloud users by username or display name.
   *
   * @param query The search query string
   * @return List of SoundCloud users matching the query, or empty list if none found
   * @throws Exception if API request fails or authentication issues occur
   */
  suspend fun searchUsers(query: String): List<SoundCloudUser>

  /**
   * Fetches the most popular tracks for a given SoundCloud user profile.
   *
   * @param profileUrl The full SoundCloud profile URL (e.g., "https://soundcloud.com/username")
   * @return List of popular tracks, or empty list if none found or on error
   */
  suspend fun fetchPopularTracks(profileUrl: String): List<SoundCloudTrack>
}
