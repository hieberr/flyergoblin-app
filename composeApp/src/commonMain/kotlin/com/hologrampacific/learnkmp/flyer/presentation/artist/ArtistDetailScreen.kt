package com.hologrampacific.learnkmp.flyer.presentation.artist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hologrampacific.learnkmp.flyer.domain.model.Artist
import com.hologrampacific.learnkmp.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.learnkmp.flyer.presentation.artist.components.SoundCloudMultiTrackPlayer
import com.hologrampacific.learnkmp.presentation.Navigator
import com.hologrampacific.learnkmp.util.openUrl
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Screen displaying detailed information about an artist including their SoundCloud profile and top
 * tracks.
 *
 * @param navigator Navigator for handling navigation events
 * @param artistName The name of the artist to display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(navigator: Navigator, artistName: String) {
  val viewModel: ArtistDetailViewModel = koinViewModel { parametersOf(artistName) }
  val uiState by viewModel.uiState.collectAsState()

  Column(modifier = Modifier.fillMaxSize()) {
    TopAppBar(
      title = { Text(artistName) },
      navigationIcon = { TextButton(onClick = { navigator.goBack() }) { Text("< Back") } },
      windowInsets = WindowInsets(0, 0, 0, 0),
    )

    Box(modifier = Modifier.fillMaxSize()) {
      when {
        uiState.isLoading -> {
          CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        else -> {
          ArtistDetailContent(
            artist = uiState.artist,
            isFetchingSoundCloud = uiState.isFetchingSoundCloud,
            errorMessage = uiState.errorMessage,
            rateLimitResetTime = uiState.rateLimitResetTime,
            onFetchSoundCloud = { viewModel.fetchSoundCloudInfo() },
            onClearError = { viewModel.clearError() },
          )
        }
      }
    }
  }
}

/** Content section of the Artist Detail screen. */
@Composable
private fun ArtistDetailContent(
  artist: Artist?,
  isFetchingSoundCloud: Boolean,
  errorMessage: String?,
  rateLimitResetTime: String?,
  onFetchSoundCloud: () -> Unit,
  onClearError: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    HorizontalDivider()

    // SoundCloud Profile Section
    if (artist?.soundCloudProfile != null) {
      SoundCloudProfileSection(artist.soundCloudProfile)
    }

    // Top Tracks Section
    if (artist?.soundCloudTracks?.isNotEmpty() == true) {
      TopTracksSection(artist.soundCloudTracks)
    }

    // Fetch button or loading indicator
    if (artist?.soundCloudProfile == null && artist?.soundCloudTracks.isNullOrEmpty()) {
      if (isFetchingSoundCloud) {
        Box(
          modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CircularProgressIndicator()
            Text(
              text = "Researching artist on SoundCloud...",
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      } else {
        Button(
          onClick = onFetchSoundCloud,
          modifier = Modifier.fillMaxWidth(),
          enabled = rateLimitResetTime == null,
        ) {
          Text(
            if (rateLimitResetTime != null) "Rate Limited - Try Later" else "Fetch SoundCloud Info"
          )
        }
      }
    }

    // Error message
    if (errorMessage != null) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
          )
          TextButton(onClick = onClearError) { Text("Dismiss") }
        }
      }
    }

    // Last fetched timestamp
    if (artist?.lastFetched != null) {
      Text(
        text = "Last updated: ${artist.lastFetched}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** Section displaying the SoundCloud profile link. */
@Composable
private fun SoundCloudProfileSection(profileUrl: String) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(text = "SoundCloud Profile", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    Text(
      text = profileUrl,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.clickable { openUrl(profileUrl) },
    )
  }
}

/** Section displaying the list of popular tracks from SoundCloud with embedded players. */
@Composable
private fun TopTracksSection(tracks: List<SoundCloudTrack>) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(text = "Popular Tracks", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    SoundCloudMultiTrackPlayer(tracks = tracks)
  }
}
