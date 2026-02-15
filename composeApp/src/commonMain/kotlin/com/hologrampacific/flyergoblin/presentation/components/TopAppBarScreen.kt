package com.hologrampacific.flyergoblin.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hologrampacific.flyergoblin.presentation.BackIcon
import com.hologrampacific.flyergoblin.presentation.Ui

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarStandard(
  title: String,
  onBackClicked: () -> Unit,
  actions: @Composable (RowScope.() -> Unit) = {},
) {
  CenterAlignedTopAppBar(
    title = { Text(title) },
    navigationIcon = { IconButton(onClick = onBackClicked) { BackIcon() } },
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
 * @param navBarActions Optional composable actions displayed in the app bar's action area
 * @param content The main content of the screen, provided as a BoxScope composable
 */
@Composable
fun TopAppBarScreen(
  appBarTitle: String,
  onBackClicked: () -> Unit,
  navBarActions: @Composable (RowScope.() -> Unit) = {},
  content: @Composable BoxScope.() -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    TopAppBarStandard(title = appBarTitle, onBackClicked = onBackClicked, actions = navBarActions)
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(top = 0.dp, bottom = Ui.unit, start = Ui.unit, end = Ui.unit),
      content = content,
    )
  }
}

// MARK: - Previews

@Preview
@Composable
private fun TopAppBarScreenPreview() {
  MaterialTheme {
    TopAppBarScreen(appBarTitle = "Standard Screen", onBackClicked = {}) {
      Text("Content", modifier = Modifier.align(Alignment.Center))
    }
  }
}
