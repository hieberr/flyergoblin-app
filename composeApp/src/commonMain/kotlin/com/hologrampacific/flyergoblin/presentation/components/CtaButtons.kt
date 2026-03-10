package com.hologrampacific.flyergoblin.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hologrampacific.flyergoblin.presentation.Ui

/**
 * Configuration for a call-to-action button.
 *
 * @property text The button text to display
 * @property onClick Callback invoked when the button is clicked
 * @property enabled Whether the button is enabled and clickable (default: true)
 */
data class ScreenButtonConfig(
  val text: String,
  val onClick: () -> Unit,
  val enabled: Boolean = true,
)

/**
 * Standard call-to-action button row used throughout the app. Renders an optional secondary
 * (outlined) button and an optional primary (filled) button side-by-side, matching the style used
 * by [TopAppBarScreenWithCenteredContent].
 *
 * @param primaryButtonConfig Configuration for the primary (filled) button, shown on the right.
 * @param secondaryButtonConfig Configuration for the secondary (outlined) button, shown on the
 *   left.
 * @param modifier Modifier applied to the outer Row.
 */
@Composable
fun CtaButtons(
  primaryButtonConfig: ScreenButtonConfig? = null,
  secondaryButtonConfig: ScreenButtonConfig? = null,
  modifier: Modifier = Modifier,
) {
  if (primaryButtonConfig == null && secondaryButtonConfig == null) return
  Row(
    modifier = modifier.fillMaxWidth().padding(horizontal = Ui.unit, vertical = Ui.unit),
    horizontalArrangement = Arrangement.spacedBy(Ui.unit),
  ) {
    val buttonModifier = Modifier.weight(1f)
    if (secondaryButtonConfig != null) {
      OutlinedButton(
        onClick = secondaryButtonConfig.onClick,
        modifier = buttonModifier.heightIn(min = Ui.unit * 2),
        enabled = secondaryButtonConfig.enabled,
      ) {
        Text(secondaryButtonConfig.text)
      }
    }
    if (primaryButtonConfig != null) {
      Button(
        onClick = primaryButtonConfig.onClick,
        modifier = buttonModifier.heightIn(min = Ui.unit * 2),
        enabled = primaryButtonConfig.enabled,
      ) {
        Text(primaryButtonConfig.text)
      }
    }
  }
}
