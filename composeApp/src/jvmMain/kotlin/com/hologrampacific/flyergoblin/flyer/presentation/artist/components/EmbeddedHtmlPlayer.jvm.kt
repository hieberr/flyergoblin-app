package com.hologrampacific.flyergoblin.flyer.presentation.artist.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState

@Composable
actual fun PlatformWebView(
  html: String,
  baseUrl: String?,
  onLoadingStateChange: (PlayerLoadingState) -> Unit,
  modifier: Modifier,
) {
  val dataUrl =
    remember(html) {
      val encodedHtml = java.util.Base64.getEncoder().encodeToString(html.encodeToByteArray())
      "data:text/html;base64,$encodedHtml"
    }
  val webViewState =
    rememberWebViewState(dataUrl) {
      isJavaScriptEnabled = true
      customUserAgentString =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
  val webViewNavigator = rememberWebViewNavigator()

  LaunchedEffect(webViewState.isLoading) {
    if (!webViewState.isLoading) {
      onLoadingStateChange(PlayerLoadingState.LOADED)
    }
  }

  LaunchedEffect(webViewState.errorsForCurrentRequest.size) {
    if (webViewState.errorsForCurrentRequest.isNotEmpty()) {
      onLoadingStateChange(PlayerLoadingState.ERROR)
    }
  }

  WebView(state = webViewState, navigator = webViewNavigator, modifier = modifier.fillMaxSize())
}
