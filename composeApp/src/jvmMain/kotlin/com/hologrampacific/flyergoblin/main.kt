package com.hologrampacific.flyergoblin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.datlag.kcef.KCEF
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun main() = application {
  var kcefInitialized by remember { mutableStateOf(false) }
  var kcefError by remember { mutableStateOf<String?>(null) }
  var statusMessage by remember { mutableStateOf("Initializing...") }

  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      try {
        KCEF.init(
          builder = {
            installDir(File("kcef-bundle"))
            progress {
              onDownloading { statusMessage = "Downloading browser engine: $it%" }
              onExtracting { statusMessage = "Extracting browser engine..." }
              onInitialized { statusMessage = "Ready" }
            }
          },
          onError = { error -> kcefError = error?.message ?: "Unknown KCEF error" },
          onRestartRequired = { statusMessage = "Restart required" },
        )
        kcefInitialized = true
      } catch (e: Exception) {
        kcefError = e.message ?: "Failed to initialize browser engine"
      }
    }
  }

  Window(onCloseRequest = ::exitApplication, title = "Flyer Goblin") {
    when {
      kcefError != null -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Browser engine initialization failed:")
            Text(kcefError ?: "Unknown error")
          }
        }
      }
      !kcefInitialized -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(statusMessage)
          }
        }
      }
      else -> {
        App()
      }
    }
  }
}
