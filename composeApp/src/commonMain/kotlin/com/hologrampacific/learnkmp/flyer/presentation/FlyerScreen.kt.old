package com.hologrampacific.learnkmp.flyer.presentation

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
import com.hologrampacific.learnkmp.presentation.Navigator

@Composable
@Preview
fun FlyerScreenPreview() {
  FlyerScreen(Navigator.noOpNavigator)
}

@Composable
fun FlyerScreen(navigator: Navigator) {
  Column(
    modifier =
      Modifier.background(MaterialTheme.colorScheme.primaryContainer)
        .safeContentPadding()
        .fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Flyer Screen")
    Button(onClick = { navigator.goTo(EventDetail("flyer_id")) }) { Text("Click") }
  }
}
