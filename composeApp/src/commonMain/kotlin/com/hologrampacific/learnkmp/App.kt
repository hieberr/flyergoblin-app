package com.hologrampacific.learnkmp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hologrampacific.learnkmp.di.flyerModule
import com.hologrampacific.learnkmp.presentation.MainScreen
import org.koin.compose.KoinApplication

@Composable
@Preview
fun App() {
  KoinApplication(application = { modules(flyerModule) }) { MaterialTheme { MainScreen() } }
}
