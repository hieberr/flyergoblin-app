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
  val id: Long,
  val username: String,
  val profileUrl: String,
  val followersCount: Int? = null,
  val trackCount: Int? = null,
  val city: String? = null,
  val countryCode: String? = null,
  val avatarUrl: String? = null,
  val fullName: String? = null,
)

/**
 * Our domain representation of a selected artist profile, bundling profile details with tracks.
 *
 * @property username The SoundCloud username
 * @property profileUrl The SoundCloud profile URL
 * @property followersCount Number of followers (null if unknown)
 * @property trackCount Number of tracks (null if unknown)
 * @property city The city from the profile (null if not set)
 * @property countryCode The country code from the profile (null if not set)
 * @property tracks List of the artist's tracks on SoundCloud for this profile
 * @property lastUpdated Timestamp of when this profile's data was last fetched (null if unknown)
 */
@Serializable
data class SoundCloudProfile(
  val id: Long,
  val username: String,
  val profileUrl: String,
  val followersCount: Int? = null,
  val trackCount: Int? = null,
  val city: String? = null,
  val countryCode: String? = null,
  val avatarUrl: String? = null,
  val fullName: String? = null,
  val tracks: List<SoundCloudTrack> = emptyList(),
  @Serializable(with = InstantSerializer::class) val lastUpdated: Instant? = null,
)

/**
 * The results of a SoundCloud profile search.
 *
 * @property results The list of SoundCloud profiles returned by the search
 * @property lastUpdated Timestamp of when this search was performed
 */
@Serializable
data class SoundCloudProfileSearchResults(
  val results: List<SoundCloudProfileInfo>,
  @Serializable(with = InstantSerializer::class) val lastUpdated: Instant,
)

/**
 * SoundCloud-specific data for an artist.
 *
 * @property profile The currently selected SoundCloud profile (null if not selected)
 * @property profileSearchResults The most recent profile search results (null if never searched)
 * @property profileSearchAlias Custom search name to use instead of the artist name when searching
 *   SoundCloud (null if not set)
 */
@Serializable
data class SoundCloudInfo(
  val profile: SoundCloudProfile? = null,
  val profileSearchResults: SoundCloudProfileSearchResults? = null,
  val profileSearchAlias: String? = null,
)

/**
 * Represents a music artist.
 *
 * @property name The artist's name as seen on an event flyer (used as primary identifier)
 * @property soundCloudInfo SoundCloud-specific data for this artist (null if not yet fetched)
 */
@Serializable data class Artist(val name: String, val soundCloudInfo: SoundCloudInfo? = null)

/**
 * Represents a track on SoundCloud.
 *
 * @property id The SoundCloud track ID (used for streaming API)
 * @property title The title of the track
 * @property url The URL to the track on SoundCloud (permalink)
 */
@Serializable data class SoundCloudTrack(val id: Long, val title: String, val url: String)
