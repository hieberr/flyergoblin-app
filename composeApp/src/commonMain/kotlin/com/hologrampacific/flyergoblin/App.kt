package com.hologrampacific.flyergoblin

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hologrampacific.flyergoblin.db.DriverFactory
import com.hologrampacific.flyergoblin.di.flyerModule
import com.hologrampacific.flyergoblin.presentation.MainScreen
import org.koin.compose.KoinApplication

@Composable
fun App(driverFactory: DriverFactory) {
  val module = remember { flyerModule(driverFactory) }
  KoinApplication(application = { modules(module) }) {
    MaterialTheme { MainScreen() }
  }
}
