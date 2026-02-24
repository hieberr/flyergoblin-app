package com.hologrampacific.flyergoblin.email.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hologrampacific.flyergoblin.presentation.Navigator
import com.hologrampacific.flyergoblin.presentation.theme.AppTheme

@Composable
@Preview
fun EmailDetailScreen() {
  AppTheme { EmailDetailScreen(Navigator.noOpNavigator, emailText = "Email Text") }
}

@Composable
fun EmailDetailScreen(navigator: Navigator, emailText: String) {
  Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.safeContentPadding(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text("Email Detail Screen")
      Text("Email Text: $emailText")
      Button(onClick = { navigator.goBack() }) { Text("Back") }
    }
  }
}
