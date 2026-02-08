package com.hologrampacific.learnkmp.flyer.presentation.artist.components

import android.annotation.SuppressLint
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hologrampacific.learnkmp.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.learnkmp.util.AppLogger
import com.hologrampacific.learnkmp.util.buildMultiTrackWidgetHtml
import com.hologrampacific.learnkmp.util.openUrl

@Composable
actual fun SoundCloudMultiTrackPlayer(tracks: List<SoundCloudTrack>, modifier: Modifier) {
  var loadingState by remember { mutableStateOf(PlayerLoadingState.LOADING) }

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
            Text(text = "Failed to load players", color = MaterialTheme.colorScheme.error)
            Text(
              text = "Open tracks individually:",
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(top = 8.dp),
            )
            tracks.forEach { track ->
              TextButton(onClick = { openUrl(track.url) }) { Text(track.title) }
            }
          }
        }
        PlayerLoadingState.LOADED -> {
          // Players are shown in the WebView below
        }
      }

      // WebView with all tracks
      MultiTrackWebView(
        tracks = tracks,
        onLoadingStateChange = { loadingState = it },
        modifier = Modifier.fillMaxWidth().height((tracks.size * 176).dp),
      )
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MultiTrackWebView(
  tracks: List<SoundCloudTrack>,
  onLoadingStateChange: (PlayerLoadingState) -> Unit,
  modifier: Modifier = Modifier,
) {
  val html = remember(tracks) { buildMultiTrackWidgetHtml(tracks.map { it.url }) }

  AndroidView(
    factory = { context ->
      object : WebView(context) {
        // Prevent WebView from scrolling internally
        override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
          super.onScrollChanged(l, t, oldl, oldt)
          // Always reset scroll to top-left to prevent internal scrolling
          scrollTo(0, 0)
        }
      }.apply {
        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true

        // Disable scroll bars
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        webViewClient =
          object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
              onLoadingStateChange(PlayerLoadingState.LOADED)
            }

            override fun onReceivedError(
              view: WebView?,
              request: WebResourceRequest?,
              error: WebResourceError?,
            ) {
              AppLogger.e(
                "MultiTrackWebView",
                "Error loading ${request?.url}: ${error?.description}",
              )
              if (request?.isForMainFrame == true) {
                onLoadingStateChange(PlayerLoadingState.ERROR)
              }
            }
          }

        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
      }
    },
    modifier = modifier,
  )
}
