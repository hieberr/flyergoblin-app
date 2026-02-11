package com.hologrampacific.flyergoblin

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hologrampacific.flyergoblin.di.flyerModule
import com.hologrampacific.flyergoblin.presentation.MainScreen
import org.koin.compose.KoinApplication

@Composable
@Preview
fun App() {
  KoinApplication(application = { modules(flyerModule) }) { MaterialTheme { MainScreen() } }
}
