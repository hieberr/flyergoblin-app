package com.hologrampacific.flyergoblin.presentation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hologrampacific.flyergoblin.presentation.components.TopAppBarScreenWithCenteredContent
import com.hologrampacific.flyergoblin.presentation.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
  val devModeEnabled by viewModel.devModeEnabled.collectAsState()
  SettingsScreenContent(
    devModeEnabled = devModeEnabled,
    onDevModeChanged = viewModel::setDevModeEnabled,
  )
}

@Composable
fun SettingsScreenContent(
  devModeEnabled: Boolean,
  onDevModeChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  TopAppBarScreenWithCenteredContent(
    appBarTitle = "Settings",
    onBackClicked = {},
    showBackButton = false,
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = Ui.halfUnit),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Checkbox(checked = devModeEnabled, onCheckedChange = onDevModeChanged)
      Text(
        text = "Enable dev mode",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(start = Ui.halfUnit),
      )
    }
  }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
  AppTheme { SettingsScreenContent(devModeEnabled = false, onDevModeChanged = {}) }
}
