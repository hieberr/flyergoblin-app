package com.hologrampacific.flyergoblin

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.hologrampacific.flyergoblin.db.DriverFactory
import com.hologrampacific.flyergoblin.sharing.SharedImageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
  private var sharedImageProvider: SharedImageProvider? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    // Capture the launch intent once. Cleared after first handling so that if
    // onKoinReady re-fires (e.g. due to recomposition) the share is not re-processed.
    var launchIntent: Intent? = intent

    setContent {
      App(DriverFactory(applicationContext)) { provider ->
        sharedImageProvider = provider
        launchIntent?.let {
          handleShareIntent(it)
          launchIntent = null
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleShareIntent(intent)
  }

  private fun handleShareIntent(intent: Intent) {
    if (intent.action != Intent.ACTION_SEND) return
    if (intent.type?.startsWith("image/") != true) return

    val uri =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
      } else {
        @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
      } ?: return

    lifecycleScope.launch {
      val bytes =
        withContext(Dispatchers.IO) {
          runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        } ?: return@launch
      sharedImageProvider?.setSharedImage(bytes)
    }
  }
}
