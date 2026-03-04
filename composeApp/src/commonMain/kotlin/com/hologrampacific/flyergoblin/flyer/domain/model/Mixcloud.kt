package com.hologrampacific.flyergoblin.flyer.domain.model

import com.hologrampacific.flyergoblin.util.InstantSerializer
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a single Mixcloud show/cloudcast.
 *
 * @property key The feed path (e.g. `/username/show-name/`)
 * @property name The display name of the show
 * @property url The full URL to the show on Mixcloud
 */
@Serializable data class MixcloudShow(val key: String, val name: String, val url: String)

/**
 * Mixcloud profile search result info.
 *
 * @property key The Mixcloud user key/path (e.g. `/username/`)
 * @property username The Mixcloud username
 * @property profileUrl The Mixcloud profile URL
 * @property followerCount Number of followers (null if unknown)
 * @property cloudcastCount Number of cloudcasts/shows (null if unknown)
 * @property city The city from the profile (null if not set)
 * @property countryCode The country code from the profile (null if not set)
 * @property avatarUrl URL of the profile avatar image (null if not set)
 * @property name The display name (null if not set)
 */
@Serializable
data class MixcloudProfileInfo(
  val key: String,
  val username: String,
  val profileUrl: String,
  val followerCount: Int? = null,
  val cloudcastCount: Int? = null,
  val city: String? = null,
  val countryCode: String? = null,
  val avatarUrl: String? = null,
  val name: String? = null,
)

/**
 * Our domain representation of a selected Mixcloud artist profile, bundling profile details with
 * shows.
 *
 * @property key The Mixcloud user key/path
 * @property username The Mixcloud username
 * @property profileUrl The Mixcloud profile URL
 * @property followerCount Number of followers (null if unknown)
 * @property cloudcastCount Number of cloudcasts/shows (null if unknown)
 * @property city The city from the profile (null if not set)
 * @property countryCode The country code from the profile (null if not set)
 * @property avatarUrl URL of the profile avatar image (null if not set)
 * @property name The display name (null if not set)
 * @property shows List of the artist's shows on Mixcloud
 * @property lastUpdated Timestamp of when this profile's data was last fetched (null if unknown)
 */
@Serializable
data class MixcloudProfile(
  val key: String,
  val username: String,
  val profileUrl: String,
  val followerCount: Int? = null,
  val cloudcastCount: Int? = null,
  val city: String? = null,
  val countryCode: String? = null,
  val avatarUrl: String? = null,
  val name: String? = null,
  val shows: List<MixcloudShow> = emptyList(),
  @Serializable(with = InstantSerializer::class) val lastUpdated: Instant? = null,
)

/**
 * The results of a Mixcloud profile search.
 *
 * @property results The list of Mixcloud profiles returned by the search
 * @property lastUpdated Timestamp of when this search was performed
 */
@Serializable
data class MixcloudProfileSearchResults(
  val results: List<MixcloudProfileInfo>,
  @Serializable(with = InstantSerializer::class) val lastUpdated: Instant,
)

/**
 * Mixcloud-specific data for an artist.
 *
 * @property profile The currently selected Mixcloud profile (null if not selected)
 * @property profileSearchResults The most recent profile search results (null if never searched)
 * @property profileSearchAlias Custom search name to use instead of the artist name when searching
 *   Mixcloud (null if not set)
 */
@Serializable
data class MixcloudInfo(
  val profile: MixcloudProfile? = null,
  val profileSearchResults: MixcloudProfileSearchResults? = null,
  val profileSearchAlias: String? = null,
)
