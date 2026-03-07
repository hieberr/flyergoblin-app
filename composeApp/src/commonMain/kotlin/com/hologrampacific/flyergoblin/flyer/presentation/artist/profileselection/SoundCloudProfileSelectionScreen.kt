package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import com.hologrampacific.flyergoblin.flyer.presentation.artist.buildLocationString
import com.hologrampacific.flyergoblin.presentation.Navigator
import com.hologrampacific.flyergoblin.presentation.Ui
import com.hologrampacific.flyergoblin.presentation.components.DevMenu
import com.hologrampacific.flyergoblin.presentation.components.DevMenuTestSnackbarErrorText
import com.hologrampacific.flyergoblin.presentation.components.ScreenButtonConfig
import com.hologrampacific.flyergoblin.presentation.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SoundCloudProfileSelectionScreen(navigator: Navigator, artistName: String) {
  val viewModel: SoundCloudProfileSelectionViewModel = koinViewModel { parametersOf(artistName) }
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.effects.collect { effect ->
      when (effect) {
        is SoundCloudProfileSelectionEffect.NavigateBack -> navigator.goBack()
      }
    }
  }

  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { errorMessage ->
      snackbarHostState.showSnackbar(message = errorMessage, withDismissAction = true)
      viewModel.clearError()
    }
  }

  val hasSelection = uiState.selectedProfileId != null || uiState.isNoneSelected
  val isButtonEnabled =
    hasSelection && !uiState.isLoading && !uiState.isConfirming && !uiState.isSearching
  val primaryButtonConfig =
    remember(isButtonEnabled) {
      ScreenButtonConfig(
        text = "Use this profile",
        onClick = { viewModel.confirmSelection() },
        enabled = isButtonEnabled,
      )
    }

  ProfileSelectionScreenLayout(
    appBarTitle = "Select SoundCloud Profile",
    artistName = artistName,
    isLoading = uiState.isLoading,
    isConfirming = uiState.isConfirming,
    hasNoProfiles = uiState.profiles.isEmpty(),
    isNoneSelected = uiState.isNoneSelected,
    onSelectNone = { viewModel.selectNone() },
    navigator = navigator,
    snackbarHostState = snackbarHostState,
    primaryButtonConfig = primaryButtonConfig,
    noneDescription = "No SoundCloud profile for this artist",
    searchResultsAvailable = uiState.searchResultsAvailable,
    isSearching = uiState.isSearching,
    onSearchProfiles = { viewModel.searchProfiles() },
    navBarActions = {
      DevMenu {
        DropdownMenuItem(
          text = { DevMenuTestSnackbarErrorText() },
          onClick = {
            dismiss()
            viewModel.triggerTestError()
          },
        )
      }
    },
  ) {
    for (profile in uiState.profiles) {
      SoundCloudProfileCard(
        profile = profile,
        isSelected = profile.id == uiState.selectedProfileId,
        onClick = { viewModel.selectProfile(profile.id) },
      )
    }
  }
}

@Composable
private fun SoundCloudProfileCard(
  profile: SoundCloudProfileInfo,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  ProfileCardBase(isSelected = isSelected, onClick = onClick) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(Ui.unit),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (profile.avatarUrl != null) {
        val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHighest
        AsyncImage(
          model = profile.avatarUrl,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          placeholder = remember(placeholderColor) { ColorPainter(placeholderColor) },
          modifier = Modifier.size(Ui.unit * 3).clip(CircleShape),
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(Ui.unit / 4)) {
        Text(
          text = profile.username,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )

        val location = buildLocationString(profile.city, profile.countryCode)
        val fullNameAndLocation =
          listOfNotNull(profile.fullName?.takeIf { it.isNotBlank() }, location)
        if (fullNameAndLocation.isNotEmpty()) {
          Text(
            text = fullNameAndLocation.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
          )
        }

        val details = buildList {
          profile.followersCount?.let { add("$it followers") }
          profile.trackCount?.let { add("$it tracks") }
        }
        if (details.isNotEmpty()) {
          Text(
            text = details.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
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
      ProfileCardBase(isSelected = false, onClick = {}) {
        Text(
          text = "None",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = "No SoundCloud profile for this artist",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      SoundCloudProfileCard(
        profile =
          SoundCloudProfileInfo(
            id = 1L,
            username = "djhorizon",
            profileUrl = "https://soundcloud.com/djhorizon",
            fullName = "Alex Horizon",
            city = "Berlin",
            countryCode = "DE",
            followersCount = 12400,
            trackCount = 38,
          ),
        isSelected = true,
        onClick = {},
      )
      SoundCloudProfileCard(
        profile =
          SoundCloudProfileInfo(
            id = 2L,
            username = "dj-horizon-official",
            profileUrl = "https://soundcloud.com/dj-horizon-official",
            followersCount = 530,
            trackCount = 5,
          ),
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
      ProfileCardBase(isSelected = true, onClick = {}) {
        Text(
          text = "None",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = "No SoundCloud profile for this artist",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
