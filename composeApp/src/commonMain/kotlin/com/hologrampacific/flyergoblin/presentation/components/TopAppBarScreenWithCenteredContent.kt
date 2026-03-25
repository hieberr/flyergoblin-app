package com.hologrampacific.flyergoblin.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
  showBackButton: Boolean = true,
  snackbarHostState: SnackbarHostState? = null,
  navBarActions: @Composable (RowScope.() -> Unit) = {},
  primaryButtonConfig: ScreenButtonConfig? = null,
  secondaryButtonConfig: ScreenButtonConfig? = null,
  overlay: (@Composable BoxScope.() -> Unit)? = null,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBarStandard(
        title = appBarTitle,
        onBackClicked = onBackClicked,
        showBackButton = showBackButton,
        actions = navBarActions,
      )
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
          // Keep buttons above the home-indicator / nav bar. Use navigationBars (not
          // safeDrawing) to avoid including IME — AppNavDisplay already applies imePadding().
          // When the keyboard is visible the IME padding already clears the bottom edge, so
          // skip the extra navigation bar padding to avoid a redundant gap.
          val imeVisible =
            WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
          val safeBottomPadding =
            if (imeVisible) {
              0.dp
            } else {
              WindowInsets.navigationBars
                .only(WindowInsetsSides.Bottom)
                .asPaddingValues()
                .calculateBottomPadding()
            }
          CtaButtons(
            primaryButtonConfig = primaryButtonConfig,
            secondaryButtonConfig = secondaryButtonConfig,
            modifier = Modifier.padding(top = Ui.halfUnit, bottom = safeBottomPadding),
          )
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
