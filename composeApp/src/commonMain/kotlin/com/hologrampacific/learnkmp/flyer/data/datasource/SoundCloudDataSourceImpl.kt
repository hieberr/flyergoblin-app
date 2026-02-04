package com.hologrampacific.learnkmp.flyer.data.datasource

import com.hologrampacific.learnkmp.flyer.data.remote.SoundCloudApiClient
import com.hologrampacific.learnkmp.flyer.domain.datasource.SoundCloudDataSource
import com.hologrampacific.learnkmp.flyer.domain.model.SoundCloudTrack

/**
 * Implementation of SoundCloudDataSource that wraps the SoundCloudApiClient.
 *
 * @param soundCloudApiClient The API client for SoundCloud requests
 */
class SoundCloudDataSourceImpl(private val soundCloudApiClient: SoundCloudApiClient) :
  SoundCloudDataSource {

  override suspend fun fetchPopularTracks(profileUrl: String): List<SoundCloudTrack> {
    return soundCloudApiClient.fetchPopularTracks(profileUrl)
  }
}
