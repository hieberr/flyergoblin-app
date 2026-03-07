package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.hologrampacific.flyergoblin.presentation.Navigator
import com.hologrampacific.flyergoblin.presentation.components.DevMenu
import com.hologrampacific.flyergoblin.presentation.components.DevMenuTestSnackbarErrorText
import com.hologrampacific.flyergoblin.presentation.components.ScreenButtonConfig
import kotlinx.coroutines.flow.Flow

sealed class ProfileSelectionEffect {
  data object NavigateBack : ProfileSelectionEffect()
}

data class ProfileSelectionUiState(
  val isLoading: Boolean = true,
  val isConfirming: Boolean = false,
  val isSearching: Boolean = false,
  val searchResultsAvailable: Boolean = false,
  val hasNoProfiles: Boolean = false,
  val isNoneSelected: Boolean = false,
  val hasSelection: Boolean = false,
  val errorMessage: String? = null,
)

@Composable
fun ProfileSelectionScreen(
  navigator: Navigator,
  appBarTitle: String,
  artistName: String,
  noneDescription: String,
  uiState: ProfileSelectionUiState,
  effects: Flow<ProfileSelectionEffect>,
  onSearchProfiles: () -> Unit,
  onSelectNone: () -> Unit,
  onConfirmSelection: () -> Unit,
  onClearError: () -> Unit,
  onTriggerTestError: () -> Unit,
  profileCards: @Composable () -> Unit,
) {
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    effects.collect { effect ->
      when (effect) {
        is ProfileSelectionEffect.NavigateBack -> navigator.goBack()
      }
    }
  }

  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { errorMessage ->
      snackbarHostState.showSnackbar(message = errorMessage, withDismissAction = true)
      onClearError()
    }
  }

  val isButtonEnabled =
    uiState.hasSelection && !uiState.isLoading && !uiState.isConfirming && !uiState.isSearching
  val primaryButtonConfig =
    ScreenButtonConfig(
      text = "Use this profile",
      onClick = onConfirmSelection,
      enabled = isButtonEnabled,
    )

  ProfileSelectionScreenLayout(
    appBarTitle = appBarTitle,
    artistName = artistName,
    isLoading = uiState.isLoading,
    isConfirming = uiState.isConfirming,
    hasNoProfiles = uiState.hasNoProfiles,
    isNoneSelected = uiState.isNoneSelected,
    onSelectNone = onSelectNone,
    navigator = navigator,
    snackbarHostState = snackbarHostState,
    primaryButtonConfig = primaryButtonConfig,
    noneDescription = noneDescription,
    searchResultsAvailable = uiState.searchResultsAvailable,
    isSearching = uiState.isSearching,
    onSearchProfiles = onSearchProfiles,
    navBarActions = {
      DevMenu {
        DropdownMenuItem(
          text = { DevMenuTestSnackbarErrorText() },
          onClick = {
            dismiss()
            onTriggerTestError()
          },
        )
      }
    },
    profileCards = profileCards,
  )
}
