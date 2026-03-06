package com.hologrampacific.flyergoblin.presentation.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hologrampacific.flyergoblin.AppSettings
import flyergoblin.composeapp.generated.resources.Res
import flyergoblin.composeapp.generated.resources.logo_dev_24px
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

/**
 * A developer-mode icon button that opens a dropdown menu. Renders nothing when dev mode is
 * disabled in [AppSettings].
 *
 * @param content The menu items to display inside the dropdown. Use [DevMenuScope.dismiss] to close
 *   the menu from within a menu item's onClick.
 */
@Composable
fun DevMenu(content: @Composable DevMenuScope.() -> Unit) {
  val appSettings = koinInject<AppSettings>()
  if (!appSettings.devModeEnabled) return

  var expanded by remember { mutableStateOf(false) }
  val scope = remember { DevMenuScope { expanded = false } }

  IconButton(onClick = { expanded = true }) {
    Icon(
      painter = painterResource(Res.drawable.logo_dev_24px),
      contentDescription = "Developer actions",
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
  DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { scope.content() }
}

/** Scope providing access to the [dismiss] callback for closing the [DevMenu]. */
class DevMenuScope(val dismiss: () -> Unit)

/** Standard label for the "Test snackbar error" dev menu item. */
@Composable
fun DevMenuTestSnackbarErrorText() {
  Text("Test snackbar error")
}
