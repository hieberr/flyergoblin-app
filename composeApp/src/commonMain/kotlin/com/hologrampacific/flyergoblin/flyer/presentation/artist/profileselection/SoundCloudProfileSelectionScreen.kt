package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hologrampacific.flyergoblin.presentation.Navigator
import com.hologrampacific.flyergoblin.presentation.Ui
import com.hologrampacific.flyergoblin.presentation.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SoundCloudProfileSelectionScreen(navigator: Navigator, artistName: String) {
  val viewModel: SoundCloudProfileSelectionViewModel = koinViewModel { parametersOf(artistName) }
  val uiState by viewModel.uiState.collectAsState()

  ProfileSelectionScreen(
    navigator = navigator,
    appBarTitle = "Select SoundCloud Profile",
    artistName = artistName,
    noneDescription = "No SoundCloud profile for this artist",
    uiState =
      ProfileSelectionUiState(
        isLoading = uiState.isLoading,
        isConfirming = uiState.isConfirming,
        isSearching = uiState.isSearching,
        searchResultsAvailable = uiState.searchResultsAvailable,
        hasNoProfiles = uiState.profiles.isEmpty(),
        isNoneSelected = uiState.isNoneSelected,
        hasSelection = uiState.selectedProfileId != null || uiState.isNoneSelected,
        errorMessage = uiState.errorMessage,
      ),
    effects = viewModel.effects,
    onSearchProfiles = { viewModel.searchProfiles() },
    onSelectNone = { viewModel.selectNone() },
    onConfirmSelection = { viewModel.confirmSelection() },
    onClearError = { viewModel.clearError() },
    onTriggerTestError = { viewModel.triggerTestError() },
  ) {
    for (profile in uiState.profiles) {
      ProfileCard(
        avatarUrl = profile.avatarUrl,
        username = profile.username,
        displayName = profile.fullName,
        city = profile.city,
        countryCode = profile.countryCode,
        followerCount = profile.followersCount,
        contentCount = profile.trackCount,
        contentLabel = "tracks",
        isSelected = profile.id == uiState.selectedProfileId,
        onClick = { viewModel.selectProfile(profile.id) },
      )
    }
  }
}

@Composable
@Preview
private fun SoundCloudProfileSelectionPreview() {
  AppTheme {
    Column(
      modifier = Modifier.fillMaxWidth().padding(Ui.unit),
      verticalArrangement = Arrangement.spacedBy(Ui.halfUnit),
    ) {
      NoneCard(
        isSelected = false,
        onClick = {},
        description = "No SoundCloud profile for this artist",
      )
      ProfileCard(
        avatarUrl = null,
        username = "djhorizon",
        displayName = "Alex Horizon",
        city = "Berlin",
        countryCode = "DE",
        followerCount = 12400,
        contentCount = 38,
        contentLabel = "tracks",
        isSelected = true,
        onClick = {},
      )
      ProfileCard(
        avatarUrl = null,
        username = "dj-horizon-official",
        displayName = null,
        city = null,
        countryCode = null,
        followerCount = 530,
        contentCount = 5,
        contentLabel = "tracks",
        isSelected = false,
        onClick = {},
      )
    }
  }
}

@Composable
@Preview
private fun SoundCloudProfileSelectionEmptyPreview() {
  AppTheme {
    Column(
      modifier = Modifier.fillMaxWidth().padding(Ui.unit),
      verticalArrangement = Arrangement.spacedBy(Ui.halfUnit),
    ) {
      Text(
        text = "No search results found for DJ Horizon",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Ui.halfUnit),
      )
      NoneCard(
        isSelected = true,
        onClick = {},
        description = "No SoundCloud profile for this artist",
      )
    }
  }
}
