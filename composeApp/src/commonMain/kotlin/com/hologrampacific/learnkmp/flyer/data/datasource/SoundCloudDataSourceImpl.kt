package com.hologrampacific.learnkmp.flyer.data.datasource

import com.hologrampacific.learnkmp.flyer.data.remote.SoundCloudApiClient
import com.hologrampacific.learnkmp.flyer.domain.datasource.ArtistProfileResult
import com.hologrampacific.learnkmp.flyer.domain.datasource.ArtistResearchDataSource
import com.hologrampacific.learnkmp.flyer.domain.datasource.SoundCloudDataSource
import com.hologrampacific.learnkmp.flyer.domain.model.SoundCloudTrack
import io.ktor.http.*

/**
 * Implementation of SoundCloudDataSource and ArtistResearchDataSource that wraps the
 * SoundCloudApiClient.
 *
 * @param soundCloudApiClient The API client for SoundCloud requests
 */
class SoundCloudDataSourceImpl(private val soundCloudApiClient: SoundCloudApiClient) :
  SoundCloudDataSource, ArtistResearchDataSource {

  override suspend fun fetchPopularTracks(profileUrl: String): List<SoundCloudTrack> {
    return soundCloudApiClient.fetchPopularTracks(profileUrl)
  }

  override suspend fun findSoundCloudProfile(artistName: String): ArtistProfileResult {
    val trimmedName = artistName.trim()
    if (trimmedName.isBlank()) {
      return ArtistProfileResult.Error("Artist name cannot be empty")
    }
    if (trimmedName.length > 200) {
      return ArtistProfileResult.Error("Artist name is too long")
    }

    val users = soundCloudApiClient.searchUsers(trimmedName)

    if (users.isEmpty()) {
      return ArtistProfileResult.Error("No SoundCloud profile found for \"$trimmedName\"")
    }

    // Return the first matching user's profile URL
    val usersByFollowersCount = users.sortedByDescending { it.followersCount ?: 0 }
    val topResult = usersByFollowersCount.first()
    val profileUrl =
      topResult.permalinkUrl ?: "https://soundcloud.com/${topResult.permalink.encodeURLPathPart()}"

    return ArtistProfileResult.Success(profileUrl)
  }
}
