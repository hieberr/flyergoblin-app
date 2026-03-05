package com.hologrampacific.flyergoblin.flyer.presentation.artist

import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import kotlin.time.Instant

/**
 * UI state for the Artist Detail screen.
 *
 * @property artist The artist being displayed (null if not yet loaded)
 * @property isLoading True when initially loading artist data from repository
 * @property isFetchingSoundCloud True when fetching SoundCloud info from AI service
 * @property isFetchingMixcloud True when fetching Mixcloud info
 * @property errorMessage Error message to display, or null if no error
 * @property rateLimitBlockedUntil When the SoundCloud rate limit ends, or null if not rate limited
 */
data class ArtistDetailUiState(
  val artist: Artist? = null,
  val isLoading: Boolean = true,
  val isFetchingSoundCloud: Boolean = false,
  val isFetchingMixcloud: Boolean = false,
  val errorMessage: String? = null,
  val rateLimitBlockedUntil: Instant? = null,
  val selectedTab: ArtistTab = ArtistTab.SoundCloud,
)
