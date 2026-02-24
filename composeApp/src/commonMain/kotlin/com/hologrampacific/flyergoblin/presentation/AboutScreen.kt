package com.hologrampacific.flyergoblin.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hologrampacific.flyergoblin.presentation.theme.AppTheme

@Composable
@Preview
fun AboutScreen() {
  AppTheme {
    Surface(
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.fillMaxSize(),
    ) {
      Column(
        modifier = Modifier.safeContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text("About Screen")
      }
    }
  }
}
