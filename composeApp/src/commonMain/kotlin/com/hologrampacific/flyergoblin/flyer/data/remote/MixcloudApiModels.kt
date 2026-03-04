package com.hologrampacific.flyergoblin.flyer.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mixcloud API search response wrapper. */
@Serializable
data class MixcloudSearchResponse(
  val data: List<MixcloudUserResult>,
  val paging: MixcloudPaging? = null,
)

/** Mixcloud API user result from search. */
@Serializable
data class MixcloudUserResult(
  val key: String,
  val url: String,
  val name: String,
  val username: String,
  val pictures: MixcloudPictures? = null,
)

/** Mixcloud API user profile response (from /{username}/ endpoint). */
@Serializable
data class MixcloudUserProfile(
  val key: String,
  val url: String,
  val name: String,
  val username: String,
  val city: String? = null,
  val country: String? = null,
  @SerialName("follower_count") val followerCount: Int? = null,
  @SerialName("following_count") val followingCount: Int? = null,
  @SerialName("cloudcast_count") val cloudcastCount: Int? = null,
  @SerialName("favorite_count") val favoriteCount: Int? = null,
  val pictures: MixcloudPictures? = null,
)

/** Mixcloud API cloudcasts response wrapper. */
@Serializable
data class MixcloudCloudcastsResponse(
  val data: List<MixcloudCloudcast>,
  val paging: MixcloudPaging? = null,
)

/** Mixcloud API cloudcast (show/mix). */
@Serializable
data class MixcloudCloudcast(
  val key: String,
  val url: String,
  val name: String,
  val slug: String? = null,
  @SerialName("play_count") val playCount: Int? = null,
  @SerialName("favorite_count") val favoriteCount: Int? = null,
  @SerialName("listener_count") val listenerCount: Int? = null,
  @SerialName("audio_length") val audioLength: Int? = null,
)

/** Mixcloud profile pictures at various resolutions. */
@Serializable
data class MixcloudPictures(
  val small: String? = null,
  val thumbnail: String? = null,
  @SerialName("medium_mobile") val mediumMobile: String? = null,
  val medium: String? = null,
  val large: String? = null,
  @SerialName("extra_large") val extraLarge: String? = null,
)

/** Mixcloud API pagination info. */
@Serializable data class MixcloudPaging(val next: String? = null)

/** Mixcloud API rate limit error response (returned on 403 rate limit). */
@Serializable data class MixcloudRateLimitError(val error: MixcloudRateLimitErrorDetail)

/** Detail inside a Mixcloud rate limit error response. */
@Serializable
data class MixcloudRateLimitErrorDetail(
  val message: String,
  val type: String,
  @SerialName("retry_after") val retryAfter: Int,
)
