package com.hologrampacific.flyergoblin.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hologrampacific.flyergoblin.presentation.Ui
import com.hologrampacific.flyergoblin.presentation.theme.AppTheme

/**
 * Configuration for a screen action button with text, click handler, and enabled state.
 *
 * Used with [TopAppBarScreenWithCenteredContent] to configure primary and secondary action buttons
 * that appear at the bottom of the screen.
 *
 * @property text The button text to display
 * @property onClick Callback invoked when the button is clicked. Errors should be handled by the
 *   caller as exceptions will propagate to the UI layer.
 * @property enabled Whether the button is enabled and clickable (default: true)
 */
data class ScreenButtonConfig(
  val text: String,
  val onClick: () -> Unit,
  val enabled: Boolean = true,
)

/**
 * Screen layout with a top app bar, horizontally-centered scrollable content, optional bottom
 * action buttons, and snackbar support.
 *
 * The content area is:
 * - Horizontally centered with a maximum width of 600dp
 * - Vertically scrollable
 * - Padded with standard spacing
 *
 * Primary and secondary buttons (if provided) appear pinned below the scrollable content in a
 * horizontal row. The primary button uses filled style, while the secondary button uses outlined
 * style.
 *
 * Snackbars (if a SnackbarHostState is provided) appear at the bottom of the screen, properly
 * positioned outside the scrollable content area.
 *
 * Ideal for forms, selection screens, and other content that benefits from a constrained width on
 * larger screens.
 *
 * ## Error Handling
 * Button onClick handlers are invoked directly without try-catch. The caller is responsible for
 * handling errors within the onClick callback (typically in ViewModels). Unhandled exceptions will
 * propagate and may crash the app.
 *
 * @param appBarTitle The title text displayed in the top app bar
 * @param onBackClicked Callback invoked when the back button is clicked. Should handle its own
 *   errors.
 * @param snackbarHostState Optional SnackbarHostState for showing error messages and notifications.
 *   When provided, a SnackbarHost will be displayed at the bottom of the screen.
 * @param navBarActions Optional composable actions displayed in the app bar's action area
 * @param primaryButtonConfig Optional configuration for the primary action button (filled style).
 *   Appears on the left when both buttons are present. The onClick callback must handle its own
 *   errors.
 * @param secondaryButtonConfig Optional configuration for the secondary action button (outlined
 *   style). Appears on the right when both buttons are present. The onClick callback must handle
 *   its own errors.
 * @param overlay Optional composable rendered on top of the content and buttons, but below the top
 *   app bar. Use this for overlays such as loading indicators.
 * @param content The main scrollable content of the screen, provided as a BoxScope composable
 */
@Composable
fun TopAppBarScreenWithCenteredContent(
  appBarTitle: String,
  onBackClicked: () -> Unit,
  snackbarHostState: SnackbarHostState? = null,
  navBarActions: @Composable (RowScope.() -> Unit) = {},
  primaryButtonConfig: ScreenButtonConfig? = null,
  secondaryButtonConfig: ScreenButtonConfig? = null,
  overlay: (@Composable BoxScope.() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBarStandard(title = appBarTitle, onBackClicked = onBackClicked, actions = navBarActions)
    },
    snackbarHost = { snackbarHostState?.let { SnackbarHost(hostState = it) } },
    containerColor = MaterialTheme.colorScheme.background,
    // The outer MainScreen Scaffold already handles safe-drawing insets, so we zero them out
    // here to avoid applying the status bar top inset a second time.
    contentWindowInsets = WindowInsets(0),
  ) { paddingValues ->
    Box(
      modifier = Modifier.fillMaxSize().padding(paddingValues),
      contentAlignment = Alignment.TopCenter,
    ) {
      val scrollState = rememberScrollState()
      Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxSize()) {
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .padding(horizontal = Ui.unit)
              .verticalScroll(scrollState),
          content = content,
        )
        if (primaryButtonConfig != null || secondaryButtonConfig != null) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Ui.unit, vertical = Ui.unit),
            horizontalArrangement = Arrangement.spacedBy(Ui.unit),
          ) {
            if (secondaryButtonConfig != null) {
              OutlinedButton(
                onClick = secondaryButtonConfig.onClick,
                modifier = Modifier.weight(1f).heightIn(min = Ui.unit * 2),
                enabled = secondaryButtonConfig.enabled,
              ) {
                Text(secondaryButtonConfig.text)
              }
            }
            if (primaryButtonConfig != null) {
              Button(
                onClick = primaryButtonConfig.onClick,
                modifier = Modifier.weight(1f).heightIn(min = Ui.unit * 2),
                enabled = primaryButtonConfig.enabled,
              ) {
                Text(primaryButtonConfig.text)
              }
            }
          }
        }
      }
      overlay?.invoke(this)
    }
  }
}

// MARK: - Previews

@Preview
@Composable
private fun TopAppBarScreenWithCenteredContentPreview() {
  AppTheme {
    TopAppBarScreenWithCenteredContent(
      appBarTitle = "Sample Screen",
      onBackClicked = {},
      primaryButtonConfig = ScreenButtonConfig("Save", {}),
      secondaryButtonConfig = ScreenButtonConfig("Cancel", {}),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(Ui.unit),
        verticalArrangement = Arrangement.spacedBy(Ui.halfUnit),
      ) {
        repeat(20) { Text("Sample content item $it") }
      }
    }
  }
}

@Preview
@Composable
private fun TopAppBarScreenWithCenteredContentNoButtonsPreview() {
  AppTheme {
    TopAppBarScreenWithCenteredContent(appBarTitle = "No Buttons", onBackClicked = {}) {
      Column(
        modifier = Modifier.fillMaxSize().padding(Ui.unit),
        verticalArrangement = Arrangement.spacedBy(Ui.halfUnit),
      ) {
        Text("Content without buttons", style = MaterialTheme.typography.titleMedium)
        repeat(10) { Text("Sample item $it") }
      }
    }
  }
}

@Preview
@Composable
private fun TopAppBarScreenWithCenteredContentPrimaryOnlyPreview() {
  AppTheme {
    TopAppBarScreenWithCenteredContent(
      appBarTitle = "Primary Only",
      onBackClicked = {},
      primaryButtonConfig = ScreenButtonConfig("Confirm", {}, enabled = true),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(Ui.unit),
        verticalArrangement = Arrangement.spacedBy(Ui.halfUnit),
      ) {
        Text("Content with primary button only", style = MaterialTheme.typography.titleMedium)
        repeat(8) { Text("Sample item $it") }
      }
    }
  }
}

@Preview
@Composable
private fun TopAppBarScreenWithCenteredContentDisabledButtonPreview() {
  AppTheme {
    TopAppBarScreenWithCenteredContent(
      appBarTitle = "Disabled State",
      onBackClicked = {},
      primaryButtonConfig = ScreenButtonConfig("Save", {}, enabled = false),
      secondaryButtonConfig = ScreenButtonConfig("Cancel", {}),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(Ui.unit),
        verticalArrangement = Arrangement.spacedBy(Ui.halfUnit),
      ) {
        Text("Content with disabled primary button", style = MaterialTheme.typography.titleMedium)
        repeat(5) { Text("Sample item $it") }
      }
    }
  }
}
