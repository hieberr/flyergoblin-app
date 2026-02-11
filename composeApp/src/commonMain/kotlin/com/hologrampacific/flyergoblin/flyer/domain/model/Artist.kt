package com.hologrampacific.flyergoblin.flyer.domain.model

import com.hologrampacific.flyergoblin.util.InstantSerializer
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * SoundCloud profile search result info.
 *
 * @property username The SoundCloud username
 * @property profileUrl The SoundCloud profile URL
 * @property followersCount Number of followers (null if unknown)
 * @property trackCount Number of tracks (null if unknown)
 * @property city The city from the profile (null if not set)
 * @property countryCode The country code from the profile (null if not set)
 */
@Serializable
data class SoundCloudProfileInfo(
  val username: String,
  val profileUrl: String,
  val followersCount: Int? = null,
  val trackCount: Int? = null,
  val city: String? = null,
  val countryCode: String? = null,
)

/**
 * Represents a music artist with SoundCloud information.
 *
 * @property name The artist's name (used as primary identifier)
 * @property soundCloudProfile The currently selected profile for this artist (null if not found)
 * @property soundCloudProfiles All SoundCloud profile search results for artist name
 * @property soundCloudTracks List of the artist's top tracks on SoundCloud
 * @property lastFetched Timestamp of when the SoundCloud info was last fetched
 */
@Serializable
data class Artist(
  val name: String,
  val soundCloudProfile: SoundCloudProfileInfo? = null,
  val soundCloudProfiles: List<SoundCloudProfileInfo> = emptyList(),
  val soundCloudTracks: List<SoundCloudTrack> = emptyList(),
  @Serializable(with = InstantSerializer::class) val lastFetched: Instant? = null,
)

/**
 * Represents a track on SoundCloud.
 *
 * @property id The SoundCloud track ID (used for streaming API)
 * @property title The title of the track
 * @property url The URL to the track on SoundCloud (permalink)
 */
@Serializable
data class SoundCloudTrack(val id: Long, val title: String, val url: String)
