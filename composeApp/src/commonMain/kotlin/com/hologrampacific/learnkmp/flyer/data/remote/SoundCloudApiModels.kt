package com.hologrampacific.learnkmp.flyer.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** SoundCloud API response for user information. */
@Serializable
data class SoundCloudUser(
  val id: Long,
  val permalink: String,
  val username: String,
  @SerialName("permalink_url") val permalinkUrl: String? = null,
  @SerialName("track_count") val trackCount: Int? = null,
  @SerialName("followers_count") val followersCount: Int? = null,
) {}

/** SoundCloud API response for track information. */
@Serializable
data class SoundCloudTrackResponse(
  val id: Long,
  val title: String,
  val permalink: String? = null,
  @SerialName("permalink_url") val permalinkUrl: String,
  val duration: Long? = null,
  @SerialName("playback_count") val playbackCount: Long? = null,
  @SerialName("likes_count") val likesCount: Long? = null,
)

/** SoundCloud API response for a collection of tracks. */
@Serializable
data class SoundCloudTracksResponse(
  val collection: List<SoundCloudTrackResponse>,
  @SerialName("next_href") val nextHref: String? = null,
)

/** SoundCloud API response for resolving URLs. */
@Serializable
data class SoundCloudResolveResponse(
  val id: Long,
  val kind: String,
  val permalink: String,
  @SerialName("permalink_url") val permalinkUrl: String,
)

/** SoundCloud OAuth token response. */
@Serializable
data class SoundCloudTokenResponse(
  @SerialName("access_token") val accessToken: String,
  @SerialName("token_type") val tokenType: String = "bearer",
  @SerialName("expires_in") val expiresIn: Long? = null,
  val scope: String? = null,
)

/**
 * SoundCloud API response for track streaming URLs.
 *
 * Contains URLs for various streaming formats. Prefer HLS AAC formats as MP3/Opus formats are
 * deprecated.
 */
@Serializable
data class SoundCloudStreamsResponse(
  @SerialName("hls_aac_160_url") val hlsAac160Url: String? = null,
  @SerialName("hls_aac_96_url") val hlsAac96Url: String? = null,
  @SerialName("http_mp3_128_url") val httpMp3128Url: String? = null,
  @SerialName("hls_mp3_128_url") val hlsMp3128Url: String? = null,
  @SerialName("preview_mp3_128_url") val previewMp3128Url: String? = null,
)
