package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import com.hologrampacific.flyergoblin.presentation.Navigator
import com.hologrampacific.flyergoblin.presentation.Ui
import com.hologrampacific.flyergoblin.presentation.components.ScreenButtonConfig
import com.hologrampacific.flyergoblin.presentation.components.TopAppBarScreenWithCenteredContent
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Screen that allows users to select a SoundCloud profile for an artist. Displays a list of
 * SoundCloud profile search results and a "None" option.
 *
 * @param navigator Navigation controller for handling back navigation
 * @param artistName The name of the artist to select a profile for
 */
@OptIn(ExperimentalMaterial3Api::class)
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
      snackbarHostState.showSnackbar(errorMessage)
      viewModel.clearError()
      // Navigate back after showing error so user doesn't get stuck
      navigator.goBack()
    }
  }
  val hasSelection = uiState.selectedProfileUrl != null || uiState.isNoneSelected
  val isButtonEnabled = hasSelection && !uiState.isLoading
  val primaryButtonConfig =
    remember(isButtonEnabled) {
      ScreenButtonConfig(
        text = "Use this profile",
        onClick = { viewModel.confirmSelection() },
        enabled = isButtonEnabled,
      )
    }

  TopAppBarScreenWithCenteredContent(
    appBarTitle = "Select SoundCloud Profile",
    onBackClicked = { navigator.goBack() },
    snackbarHostState = snackbarHostState,
    primaryButtonConfig = primaryButtonConfig,
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      if (uiState.isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.align(Alignment.CenterHorizontally).padding(Ui.unit * 2)
        )
      } else {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(Ui.halfUnit),
        ) {
          NoneCard(isSelected = uiState.isNoneSelected, onClick = { viewModel.selectNone() })
          for (profile in uiState.profiles) {
            ProfileCard(
              profile,
              isSelected = profile.profileUrl == uiState.selectedProfileUrl,
              onClick = { viewModel.selectProfile(profile.profileUrl) },
            )
          }
        }
      }
    }
  }
}

/**
 * Base composable for profile selection cards.
 *
 * Provides the common layout, styling, and selection state handling for both profile cards and the
 * "None" card. When selected, displays a primary-colored border and checkmark.
 *
 * @param isSelected Whether this card is currently selected
 * @param onClick Callback invoked when the card is clicked
 * @param content The content to display inside the card (title, description, etc.)
 */
@Composable
private fun ProfileCardBase(
  isSelected: Boolean,
  onClick: () -> Unit,
  content: @Composable () -> Unit,
) {
  Card(
    onClick = onClick,
    modifier =
      Modifier.fillMaxWidth().semantics {
        stateDescription = if (isSelected) "Selected" else "Not selected"
      },
    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isSelected) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surfaceVariant
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(Ui.unit),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(Ui.unit / 4),
      ) {
        content()
      }

      if (isSelected) {
        Text(
          text = "\u2713",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

/**
 * Card displaying a SoundCloud profile option.
 *
 * Shows the profile username and additional details such as follower count, track count, and
 * location (if available). Details are displayed in a single line separated by middot characters.
 *
 * @param profile The SoundCloud profile information to display
 * @param isSelected Whether this profile is currently selected
 * @param onClick Callback invoked when the card is clicked
 */
@Composable
private fun ProfileCard(profile: SoundCloudProfileInfo, isSelected: Boolean, onClick: () -> Unit) {
  ProfileCardBase(isSelected = isSelected, onClick = onClick) {
    Text(
      text = profile.username,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )

    val details = buildList {
      profile.followersCount?.let { add("$it followers") }
      profile.trackCount?.let { add("$it tracks") }
      val location = buildLocationString(profile.city, profile.countryCode)
      if (location != null) add(location)
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

/**
 * Card for the "None" option.
 *
 * Allows users to indicate that no SoundCloud profile matches the artist. When selected, this sets
 * the artist's soundCloudProfile to null.
 *
 * @param isSelected Whether the "None" option is currently selected
 * @param onClick Callback invoked when the card is clicked
 */
@Composable
private fun NoneCard(isSelected: Boolean, onClick: () -> Unit) {
  ProfileCardBase(isSelected = isSelected, onClick = onClick) {
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

/**
 * Builds a location string from city and country code.
 *
 * If both city and country code are available, returns "City, CC". If only one is available,
 * returns that value. If neither is available, returns null.
 *
 * @param city The city name (nullable)
 * @param countryCode The country code (nullable)
 * @return A formatted location string, or null if no location data is available
 */
private fun buildLocationString(city: String?, countryCode: String?): String? {
  return when {
    city != null && countryCode != null -> "$city, $countryCode"
    city != null -> city
    countryCode != null -> countryCode
    else -> null
  }
}
