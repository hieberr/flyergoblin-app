package com.hologrampacific.flyergoblin.flyer.presentation.artist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.flyergoblin.util.buildMultiTrackWidgetHtml
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@Composable
actual fun SoundCloudMultiTrackPlayer(tracks: List<SoundCloudTrack>, modifier: Modifier) {
  var loadingState by remember { mutableStateOf(PlayerLoadingState.LOADING) }
  val uriHandler = LocalUriHandler.current

  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column {
      when (loadingState) {
        PlayerLoadingState.LOADING -> {
          Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
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
        PlayerLoadingState.ERROR -> {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(text = "Failed to load tracks", color = MaterialTheme.colorScheme.error)
          }
        }
        PlayerLoadingState.LOADED -> {
          // Players are shown in the WKWebView below
        }
      }

      // WKWebView with all tracks
      MultiTrackWKWebView(
        tracks = tracks,
        onLoadingStateChange = { loadingState = it },
        modifier =
          Modifier.fillMaxWidth()
            .height((tracks.size * (SOUNDCLOUD_TRACK_HEIGHT + SOUNDCLOUD_TRACK_GAP)).dp),
      )
    }
  }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun MultiTrackWKWebView(
  tracks: List<SoundCloudTrack>,
  onLoadingStateChange: (PlayerLoadingState) -> Unit,
  modifier: Modifier = Modifier,
) {
  val html =
    remember(tracks) {
      buildMultiTrackWidgetHtml(
        tracks.map { it.url },
        SOUNDCLOUD_TRACK_HEIGHT,
        SOUNDCLOUD_TRACK_GAP,
      )
    }

  // Timeout fallback: if navigation delegate doesn't fire within 3 seconds, mark as loaded
  LaunchedEffect(tracks) {
    kotlinx.coroutines.delay(3000)
    onLoadingStateChange(PlayerLoadingState.LOADED)
  }

  UIKitView(
    factory = {
      val configuration =
        WKWebViewConfiguration().apply {
          allowsInlineMediaPlayback = true
          // Suppress incremental rendering to reduce console noise during load
          suppressesIncrementalRendering = false
        }

      val webView = WKWebView(frame = kotlinx.cinterop.cValue {}, configuration = configuration)

      // Disable user interaction on scrollView to pass touches through to parent
      // Web content (iframes) will still be interactive via the content view
      webView.scrollView.setUserInteractionEnabled(false)

      webView.navigationDelegate =
        object : NSObject(), WKNavigationDelegateProtocol {
          override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
            onLoadingStateChange(PlayerLoadingState.LOADED)
          }

          override fun webView(
            webView: WKWebView,
            didFailNavigation: WKNavigation?,
            withError: platform.Foundation.NSError,
          ) {
            onLoadingStateChange(PlayerLoadingState.ERROR)
          }
        }

      // Set baseURL to SoundCloud domain to give WebKit proper context for privacy features
      val baseURL = NSURL.URLWithString("https://w.soundcloud.com")
      webView.loadHTMLString(html, baseURL = baseURL)

      webView
    },
    modifier = modifier,
  )
}
