package com.hologrampacific.learnkmp.flyer.presentation.artist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hologrampacific.learnkmp.flyer.domain.model.SoundCloudTrack

/** Loading state for the SoundCloud player widget. */
enum class PlayerLoadingState {
  LOADING,
  LOADED,
  ERROR,
}

/**
 * Platform-specific multi-track SoundCloud player that embeds all tracks in a single WebView.
 *
 * On Android and iOS, this displays all tracks in a single embedded player using WebView/WKWebView.
 * On Desktop/JVM, this shows a list of tracks with "Open in SoundCloud" buttons.
 *
 * @param tracks List of SoundCloud tracks to display
 * @param modifier Modifier for the composable
 */
@Composable
expect fun SoundCloudMultiTrackPlayer(
  tracks: List<SoundCloudTrack>,
  modifier: Modifier = Modifier,
)
