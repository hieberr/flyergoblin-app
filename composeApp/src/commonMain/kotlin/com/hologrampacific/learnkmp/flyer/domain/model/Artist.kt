package com.hologrampacific.learnkmp.flyer.domain.model

import com.hologrampacific.learnkmp.util.InstantSerializer
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a music artist with SoundCloud information.
 *
 * @property name The artist's name (used as primary identifier)
 * @property soundCloudProfile The artist's SoundCloud profile URL (null if not found)
 * @property soundCloudTracks List of the artist's top tracks on SoundCloud
 * @property lastFetched Timestamp of when the SoundCloud info was last fetched
 */
@Serializable
data class Artist(
  val name: String,
  val soundCloudProfile: String? = null,
  val soundCloudTracks: List<SoundCloudTrack> = emptyList(),
  @Serializable(with = InstantSerializer::class) val lastFetched: Instant? = null,
)

/**
 * Represents a track on SoundCloud.
 *
 * @property id The SoundCloud track ID (used for streaming API)
 * @property title The title of the track
 * @property url The URL to the track on SoundCloud (permalink)
 * @property streamUrl The direct streaming URL (null if track is not streamable)
 */
@Serializable
data class SoundCloudTrack(
  val id: Long,
  val title: String,
  val url: String,
  val streamUrl: String? = null,
)
