package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler

@Composable
actual fun rememberDeepLinkOpener(): (deepLinkUri: String, fallbackUrl: String) -> Unit {
  val uriHandler = LocalUriHandler.current
  return remember(uriHandler) { { _, fallbackUrl -> uriHandler.openUri(fallbackUrl) } }
}
