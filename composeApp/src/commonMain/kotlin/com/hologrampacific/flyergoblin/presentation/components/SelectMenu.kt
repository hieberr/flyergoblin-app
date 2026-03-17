package com.hologrampacific.flyergoblin.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hologrampacific.flyergoblin.presentation.Ui
import flyergoblin.composeapp.generated.resources.Res
import flyergoblin.composeapp.generated.resources.check_24px
import org.jetbrains.compose.resources.painterResource

/**
 * A dropdown menu displayed as a [FilterChip] that allows the user to select a single value from
 * a list of options.
 *
 * @param options The list of options to display in the dropdown.
 * @param selected The currently selected option. Used to display the current value in the chip and
 *   to show a checkmark next to the matching item in the dropdown.
 * @param onSelectedChange Called when the user selects an option.
 * @param labelForOption Converts an option to its display string.
 * @param leadingIcon Optional icon displayed inside the chip before the selected label.
 * @param modifier Modifier applied to the [ExposedDropdownMenuBox] container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectMenu(
  options: List<T>,
  selected: T,
  onSelectedChange: (T) -> Unit,
  labelForOption: (T) -> String,
  leadingIcon: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(
    modifier = modifier,
    expanded = expanded,
    onExpandedChange = { expanded = it },
  ) {
    FilterChip(
      modifier =
        Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
      selected = expanded,
      onClick = {},
      label = {
        Row(
          horizontalArrangement = Arrangement.spacedBy(Ui.unit),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          leadingIcon?.invoke()
          Text(text = labelForOption(selected), style = MaterialTheme.typography.labelMedium)
        }
      },
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      for (option in options) {
        val label = labelForOption(option)
        DropdownMenuItem(
          text = {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
          },
          trailingIcon = {
            if (selected == option) {
              Icon(
                painter = painterResource(Res.drawable.check_24px),
                contentDescription = "Selected",
                modifier = Modifier.size(Ui.standardIconSize),
              )
            }
          },
          onClick = {
            onSelectedChange(option)
            expanded = false
          },
          contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
        )
      }
    }
  }
}
