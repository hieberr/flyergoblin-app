package com.hologrampacific.flyergoblin.email.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hologrampacific.flyergoblin.presentation.Navigator

@Composable
@Preview
fun EmailDetailScreen() {
  EmailDetailScreen(Navigator.noOpNavigator, emailText = "Email Text")
}

@Composable
fun EmailDetailScreen(navigator: Navigator, emailText: String) {
  Column(
    modifier =
      Modifier.background(MaterialTheme.colorScheme.primaryContainer)
        .safeContentPadding()
        .fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Email Detail Screen")
    Text("Email Text: $emailText")
    Button(onClick = { navigator.goBack() }) { Text("Back") }
  }
}
