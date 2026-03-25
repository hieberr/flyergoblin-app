package com.hologrampacific.flyergoblin.presentation.util

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

@Composable
actual fun rememberDeepLinkOpener(): (deepLinkUri: String, fallbackUrl: String) -> Unit {
  val context = LocalContext.current
  return remember(context) {
    { deepLinkUri, fallbackUrl ->
      try {
        context.startActivity(Intent(Intent.ACTION_VIEW, deepLinkUri.toUri()))
      } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUrl.toUri()))
      }
    }
  }
}
