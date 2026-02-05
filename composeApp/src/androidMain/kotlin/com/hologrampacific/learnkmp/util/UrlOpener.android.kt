package com.hologrampacific.learnkmp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Holder for Android context to enable URL opening from non-Composable code. */
object UrlOpenerAndroid {
  var context: Context? = null
}

/**
 * Composable that initializes the URL opener with the current Android context. Should be called at
 * the top level of your Android app.
 */
@Composable
fun InitUrlOpener() {
  val context = LocalContext.current
  remember(context) {
    UrlOpenerAndroid.context = context
    null
  }
}

actual fun openUrl(url: String) {
  val context =
    UrlOpenerAndroid.context
      ?: run {
        AppLogger.e("UrlOpener", "Android context not set. Cannot open URL: $url")
        return
      }

  try {
    val intent =
      Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    context.startActivity(intent)
  } catch (e: Exception) {
    AppLogger.e("UrlOpener", "Failed to open URL: $url", e)
  }
}
