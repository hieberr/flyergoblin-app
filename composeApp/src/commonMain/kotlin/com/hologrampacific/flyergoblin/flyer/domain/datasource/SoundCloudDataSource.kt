package com.hologrampacific.flyergoblin.flyer.domain.datasource

import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudTrack

/** DataSource for fetching data from SoundCloud. */
interface SoundCloudDataSource {
  /**
   * Fetches popular tracks from a SoundCloud artist profile.
   *
   * @param soundCloudUserId The artist's SoundCloud profile user id
   * @return List of popular tracks (empty list if fetching fails)
   */
  suspend fun getTracksForProfile(soundCloudUserId: Long): List<SoundCloudTrack>
}
