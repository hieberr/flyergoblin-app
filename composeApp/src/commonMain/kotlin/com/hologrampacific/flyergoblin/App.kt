package com.hologrampacific.flyergoblin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.hologrampacific.flyergoblin.db.DriverFactory
import com.hologrampacific.flyergoblin.di.flyerModule
import com.hologrampacific.flyergoblin.presentation.MainScreen
import com.hologrampacific.flyergoblin.presentation.theme.AppTheme
import com.hologrampacific.flyergoblin.sharing.SharedImageProvider
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

@Composable
fun App(driverFactory: DriverFactory, onKoinReady: (SharedImageProvider) -> Unit = {}) {
  val module = remember { flyerModule(driverFactory) }
  KoinApplication(application = { modules(module) }) {
    val provider: SharedImageProvider = koinInject()
    LaunchedEffect(provider) { onKoinReady(provider) }
    AppTheme { MainScreen() }
  }
}
