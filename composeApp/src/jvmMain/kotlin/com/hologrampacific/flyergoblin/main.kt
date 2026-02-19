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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.hologrampacific.flyergoblin.db.DriverFactory
import dev.datlag.kcef.KCEF
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun main() = application {
  val driverFactory = remember { DriverFactory() }
  var kcefInitialized by remember { mutableStateOf(false) }
  var kcefError by remember { mutableStateOf<String?>(null) }
  var statusMessage by remember { mutableStateOf("") }
  // -1 = indeterminate, 0f–1f = download progress
  var downloadProgress by remember { mutableFloatStateOf(-1f) }

  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      val desktopAppFiles = File(".app-files-desktop").also { it.mkdirs() }
      val kcefBundleDir = File(desktopAppFiles, "kcef-bundle")
      val initMarker = File(desktopAppFiles, "kcef-init-in-progress")

      // If the marker exists, the previous launch crashed mid-initialization (e.g. a native JVM
      // crash from a corrupted bundle). Delete the bundle so KCEF re-downloads a fresh copy.
      if (initMarker.exists()) {
        kcefBundleDir.deleteRecursively()
        initMarker.delete()
      }

      initMarker.createNewFile()
      try {
        KCEF.init(
          builder = {
            installDir(kcefBundleDir)
            progress {
              onDownloading {
                downloadProgress = it / 100f
                statusMessage = "Downloading browser engine..."
              }
              onExtracting {
                downloadProgress = -1f
                statusMessage = "Extracting browser engine..."
              }
              onInitialized { statusMessage = "Ready" }
            }
          },
          onError = { error -> kcefError = error?.message ?: "Unknown KCEF error" },
          onRestartRequired = { statusMessage = "Restart required" },
        )
        kcefInitialized = true
        initMarker.delete()
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
        if (statusMessage.isNotEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              if (downloadProgress >= 0f) {
                LinearProgressIndicator(
                  progress = { downloadProgress },
                  modifier = Modifier.width(240.dp),
                )
              } else {
                LinearProgressIndicator(modifier = Modifier.width(240.dp))
              }
              Text(statusMessage, modifier = Modifier.padding(top = 8.dp))
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
