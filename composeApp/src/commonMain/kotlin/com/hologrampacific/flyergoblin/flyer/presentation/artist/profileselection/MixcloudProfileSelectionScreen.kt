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
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfileInfo
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
fun MixcloudProfileSelectionScreen(navigator: Navigator, artistName: String) {
  val viewModel: MixcloudProfileSelectionViewModel = koinViewModel { parametersOf(artistName) }
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.effects.collect { effect ->
      when (effect) {
        is MixcloudProfileSelectionEffect.NavigateBack -> navigator.goBack()
      }
    }
  }

  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { errorMessage ->
      snackbarHostState.showSnackbar(message = errorMessage, withDismissAction = true)
      viewModel.clearError()
    }
  }

  val hasSelection = uiState.selectedProfileKey != null || uiState.isNoneSelected
  val isButtonEnabled = hasSelection && !uiState.isLoading && !uiState.isConfirming
  val primaryButtonConfig =
    remember(isButtonEnabled) {
      ScreenButtonConfig(
        text = "Use this profile",
        onClick = { viewModel.confirmSelection() },
        enabled = isButtonEnabled,
      )
    }

  ProfileSelectionScreenLayout(
    appBarTitle = "Select Mixcloud Profile",
    artistName = artistName,
    isLoading = uiState.isLoading,
    isConfirming = uiState.isConfirming,
    hasNoProfiles = uiState.profiles.isEmpty(),
    isNoneSelected = uiState.isNoneSelected,
    onSelectNone = { viewModel.selectNone() },
    navigator = navigator,
    snackbarHostState = snackbarHostState,
    primaryButtonConfig = primaryButtonConfig,
    noneDescription = "No Mixcloud profile for this artist",
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
      MixcloudProfileCard(
        profile = profile,
        isSelected = profile.key == uiState.selectedProfileKey,
        onClick = { viewModel.selectProfile(profile.key) },
      )
    }
  }
}

@Composable
private fun MixcloudProfileCard(
  profile: MixcloudProfileInfo,
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
        val nameAndLocation = listOfNotNull(profile.name?.takeIf { it.isNotBlank() }, location)
        if (nameAndLocation.isNotEmpty()) {
          Text(
            text = nameAndLocation.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
          )
        }

        val details = buildList {
          profile.followerCount?.let { add("$it followers") }
          profile.cloudcastCount?.let { add("$it shows") }
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
private fun MixcloudProfileSelectionPreview() {
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
          text = "No Mixcloud profile for this artist",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      MixcloudProfileCard(
        profile =
          MixcloudProfileInfo(
            key = "djhorizon",
            username = "djhorizon",
            profileUrl = "https://www.mixcloud.com/djhorizon/",
            name = "Alex Horizon",
            city = "Berlin",
            countryCode = "DE",
            followerCount = 3200,
            cloudcastCount = 47,
          ),
        isSelected = true,
        onClick = {},
      )
      MixcloudProfileCard(
        profile =
          MixcloudProfileInfo(
            key = "dj-horizon-official",
            username = "dj-horizon-official",
            profileUrl = "https://www.mixcloud.com/dj-horizon-official/",
            followerCount = 120,
            cloudcastCount = 8,
          ),
        isSelected = false,
        onClick = {},
      )
    }
  }
}

@Composable
@Preview
private fun MixcloudProfileSelectionEmptyPreview() {
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
          text = "No Mixcloud profile for this artist",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
