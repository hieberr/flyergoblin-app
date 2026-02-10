package com.hologrampacific.learnkmp.flyer.presentation.artist.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.hologrampacific.learnkmp.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.learnkmp.util.buildMultiTrackWidgetHtml
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState

@Composable
actual fun SoundCloudMultiTrackPlayer(tracks: List<SoundCloudTrack>, modifier: Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    MultiTrackDesktopWebView(
      tracks = tracks,
      modifier = Modifier.fillMaxWidth().height((tracks.size * 176).dp),
    )
  }
}

@Composable
private fun MultiTrackDesktopWebView(tracks: List<SoundCloudTrack>, modifier: Modifier = Modifier) {
  val uriHandler = LocalUriHandler.current
  val html = remember(tracks) { buildMultiTrackWidgetHtml(tracks.map { it.url }) }
  // Convert HTML to data URL for desktop WebView
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

  Box(modifier = modifier) {
    // WebView is always rendered
    WebView(state = webViewState, navigator = webViewNavigator, modifier = Modifier.fillMaxSize())

    // Loading overlay
    AnimatedVisibility(
      visible = webViewState.isLoading,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CircularProgressIndicator()
          Text(text = "Loading tracks...", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }

    // Error overlay
    if (webViewState.errorsForCurrentRequest.isNotEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = "Failed to load tracks", color = MaterialTheme.colorScheme.error)
        }
      }
    }
  }
}
