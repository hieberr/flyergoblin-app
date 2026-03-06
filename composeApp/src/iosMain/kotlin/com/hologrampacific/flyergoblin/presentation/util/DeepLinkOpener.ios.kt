package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberDeepLinkOpener(): (deepLinkUri: String, fallbackUrl: String) -> Unit {
  val uriHandler = LocalUriHandler.current
  return remember(uriHandler) {
    { deepLinkUri, fallbackUrl ->
      val url = NSURL.URLWithString(deepLinkUri)
      if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
      } else {
        uriHandler.openUri(fallbackUrl)
      }
    }
  }
}
