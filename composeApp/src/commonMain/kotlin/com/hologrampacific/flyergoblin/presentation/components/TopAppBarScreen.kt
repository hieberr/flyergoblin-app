package com.hologrampacific.flyergoblin.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hologrampacific.flyergoblin.presentation.BackIcon
import com.hologrampacific.flyergoblin.presentation.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarStandard(
  title: String,
  onBackClicked: () -> Unit,
  actions: @Composable (RowScope.() -> Unit) = {},
) {
  var backPressed by remember { mutableStateOf(false) }
  CenterAlignedTopAppBar(
    title = { Text(title) },
    navigationIcon = {
      IconButton(
        onClick = {
          if (!backPressed) {
            backPressed = true
            onBackClicked()
          }
        }
      ) {
        BackIcon()
      }
    },
    windowInsets = WindowInsets(0, 0, 0, 0),
    actions = actions,
  )
}

/**
 * Standard screen layout with a top app bar and content area.
 *
 * The content fills the remaining space below the app bar with standard padding. Use this for
 * screens that don't need centered content or bottom action buttons.
 *
 * ## Error Handling
 * The onBackClicked callback is invoked directly without try-catch. The caller is responsible for
 * handling errors within the callback. Unhandled exceptions will propagate and may crash the app.
 *
 * @param appBarTitle The title text displayed in the top app bar
 * @param onBackClicked Callback invoked when the back button is clicked. Should handle its own
 *   errors.
 * @param snackbarHostState Optional SnackbarHostState for showing error messages and notifications.
 *   When provided, a SnackbarHost will be displayed at the bottom of the screen.
 * @param navBarActions Optional composable actions displayed in the app bar's action area
 * @param content The main content of the screen, provided as a BoxScope composable
 */
@Composable
fun TopAppBarScreen(
  appBarTitle: String,
  onBackClicked: () -> Unit,
  snackbarHostState: SnackbarHostState? = null,
  navBarActions: @Composable (RowScope.() -> Unit) = {},
  overlay: (@Composable BoxScope.() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBarStandard(title = appBarTitle, onBackClicked = onBackClicked, actions = navBarActions)
    },
    snackbarHost = { snackbarHostState?.let { SnackbarHost(hostState = it) } },
    containerColor = MaterialTheme.colorScheme.background,
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
      Box(modifier = Modifier.fillMaxSize(), content = content)
      overlay?.invoke(this)
    }
  }
}

// MARK: - Previews

@Preview
@Composable
private fun TopAppBarScreenPreview() {
  AppTheme {
    TopAppBarScreen(appBarTitle = "Standard Screen", onBackClicked = {}) {
      Text("Content", modifier = Modifier.align(Alignment.Center))
    }
  }
}
