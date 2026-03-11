package com.hologrampacific.flyergoblin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.hologrampacific.flyergoblin.db.DriverFactory
import com.hologrampacific.flyergoblin.presentation.Ui
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.friwi.jcefmaven.EnumProgress

fun main() = application {
  val driverFactory = remember { DriverFactory() }
  var jcefInitialized by remember { mutableStateOf(false) }
  var jcefError by remember { mutableStateOf<String?>(null) }
  var statusMessage by remember { mutableStateOf("") }
  // -1 = indeterminate, 0f–1f = download progress
  var downloadProgress by remember { mutableFloatStateOf(-1f) }

  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      try {
        JcefManager.initialize(
          onProgress = { state, percent ->
            // Already dispatched to Main by JcefManager.
            when (state) {
              EnumProgress.DOWNLOADING -> {
                downloadProgress = percent / 100f
                statusMessage = "Downloading browser engine..."
              }

              EnumProgress.EXTRACTING -> {
                downloadProgress = -1f
                statusMessage = "Extracting browser engine..."
              }

              EnumProgress.INSTALL -> {
                downloadProgress = -1f
                statusMessage = "Installing browser engine..."
              }

              EnumProgress.INITIALIZING -> {
                statusMessage = "Initializing browser engine..."
              }

              EnumProgress.INITIALIZED -> {
                statusMessage = "Ready"
              }

              else -> {}
            }
          },
          onTerminated = { exitApplication() },
        )
        withContext(Dispatchers.Main) { jcefInitialized = true }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          jcefError = e.message ?: "Failed to initialize browser engine"
        }
      }
    }
  }

  Window(
    // Don't call exitApplication() here. JcefManager.dispose() initiates async CEF shutdown;
    // exitApplication() is called from stateHasChanged(TERMINATED) once native cleanup is done.
    onCloseRequest = { JcefManager.dispose() },
    title = "Flyer Goblin",
  ) {
    when {
      jcefError != null -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Browser engine initialization failed:")
            Text(jcefError ?: "Unknown error")
          }
        }
      }

      !jcefInitialized -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (downloadProgress >= 0f) {
              LinearProgressIndicator(
                progress = { downloadProgress },
                modifier = Modifier.width(Ui.unit * 15),
              )
            } else {
              LinearProgressIndicator(modifier = Modifier.width(Ui.unit * 15))
            }
            if (statusMessage.isNotEmpty()) {
              Text(statusMessage, modifier = Modifier.padding(top = Ui.halfUnit))
            }
          }
        }
      }

      else -> {
        App(driverFactory)
      }
    }
  }
}
