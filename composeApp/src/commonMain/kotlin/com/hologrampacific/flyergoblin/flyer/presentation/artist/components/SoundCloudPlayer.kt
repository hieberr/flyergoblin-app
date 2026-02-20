package com.hologrampacific.flyergoblin.flyer.presentation.artist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudTrack

/** Height of the sound cloud player widgets in dp */
const val SOUNDCLOUD_TRACK_HEIGHT = 132

/** The gap between the sound cloud player widgets in dp */
const val SOUNDCLOUD_TRACK_GAP = 8

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
expect fun SoundCloudMultiTrackPlayer(tracks: List<SoundCloudTrack>, modifier: Modifier = Modifier)
