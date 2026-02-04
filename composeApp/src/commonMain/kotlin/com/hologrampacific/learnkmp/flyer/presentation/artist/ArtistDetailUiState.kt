package com.hologrampacific.learnkmp.flyer.presentation.artist

import com.hologrampacific.learnkmp.flyer.domain.model.Artist

/**
 * UI state for the Artist Detail screen.
 *
 * @property artist The artist being displayed (null if not yet loaded)
 * @property isLoading True when initially loading artist data from repository
 * @property isFetchingSoundCloud True when fetching SoundCloud info from AI service
 * @property errorMessage Error message to display, or null if no error
 */
data class ArtistDetailUiState(
  val artist: Artist? = null,
  val isLoading: Boolean = true,
  val isFetchingSoundCloud: Boolean = false,
  val errorMessage: String? = null,
)
