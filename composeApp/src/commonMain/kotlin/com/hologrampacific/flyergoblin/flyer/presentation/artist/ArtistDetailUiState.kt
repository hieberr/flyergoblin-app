package com.hologrampacific.flyergoblin.flyer.presentation.artist

import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.presentation.HasErrorMessage
import kotlin.time.Instant

/**
 * UI state for the Artist Detail screen.
 *
 * @property artist The artist being displayed (null if not yet loaded)
 * @property isLoading True when initially loading artist data from repository
 * @property isFetchingSoundCloud True when fetching SoundCloud info from AI service
 * @property isFetchingMixcloud True when fetching Mixcloud info
 * @property errorMessage Error message to display, or null if no error
 * @property soundCloudRateLimitBlockedUntil When the SoundCloud rate limit ends, or null if not
 *   rate limited
 * @property mixcloudRateLimitBlockedUntil When the Mixcloud rate limit ends, or null if not rate
 *   limited
 */
data class ArtistDetailUiState(
  val artist: Artist? = null,
  val isLoading: Boolean = true,
  val isFetchingSoundCloud: Boolean = false,
  val isFetchingMixcloud: Boolean = false,
  override val errorMessage: String? = null,
  val soundCloudRateLimitBlockedUntil: Instant? = null,
  val mixcloudRateLimitBlockedUntil: Instant? = null,
  val selectedTab: ArtistAudioPlatform = ArtistAudioPlatform.SoundCloud,
) : HasErrorMessage<ArtistDetailUiState> {
  override fun copyWithErrorMessage(errorMessage: String?) = copy(errorMessage = errorMessage)
}
