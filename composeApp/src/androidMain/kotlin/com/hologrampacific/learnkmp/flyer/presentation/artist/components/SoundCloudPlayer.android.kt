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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hologrampacific.learnkmp.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.learnkmp.util.AppLogger
import com.hologrampacific.learnkmp.util.buildMultiTrackWidgetHtml

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
            Text(text = "Failed to load players", color = MaterialTheme.colorScheme.error)
            Text(
              text = "Open tracks individually:",
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(top = 8.dp),
            )
            tracks.forEach { track ->
              TextButton(onClick = { uriHandler.openUri(track.url) }) { Text(track.title) }
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

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
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
          private var touchDownX = 0f
          private var touchDownY = 0f
          private var storedDownEvent: android.view.MotionEvent? = null
          private val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop

          // Prevent WebView from scrolling internally
          override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
            super.onScrollChanged(l, t, oldl, oldt)
            // Always reset scroll to top-left to prevent internal scrolling
            scrollTo(0, 0)
          }

          // Prevent WebView from blocking parent scroll interception
          override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            // Don't prevent parent from intercepting - allow scrolling to take priority
            super.requestDisallowInterceptTouchEvent(false)
          }

          override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
            when (event.action) {
              android.view.MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                // Store a copy of the DOWN event to replay later if it's a tap
                storedDownEvent?.recycle()
                storedDownEvent = android.view.MotionEvent.obtain(event)
                // Return true to claim the event series, but DON'T pass to WebView yet
                return true
              }

              android.view.MotionEvent.ACTION_MOVE -> {
                val dy = kotlin.math.abs(event.y - touchDownY)
                // If vertical movement exceeds touch slop, it's a scroll
                if (dy > touchSlop) {
                  // Clean up stored event
                  storedDownEvent?.recycle()
                  storedDownEvent = null
                  // Return false to let parent scrollable handle it
                  return false
                }
                // Still within tap threshold, continue waiting
                return true
              }

              android.view.MotionEvent.ACTION_UP -> {
                val dx = kotlin.math.abs(event.x - touchDownX)
                val dy = kotlin.math.abs(event.y - touchDownY)
                // If movement was small enough, treat as a tap
                if (dx < touchSlop && dy < touchSlop && storedDownEvent != null) {
                  // Replay the DOWN event, then the UP event to WebView
                  super.onTouchEvent(storedDownEvent)
                  val result = super.onTouchEvent(event)
                  storedDownEvent?.recycle()
                  storedDownEvent = null
                  return result
                }
                // Was a scroll or drag, clean up and don't pass to WebView
                storedDownEvent?.recycle()
                storedDownEvent = null
                return false
              }

              android.view.MotionEvent.ACTION_CANCEL -> {
                storedDownEvent?.recycle()
                storedDownEvent = null
                return super.onTouchEvent(event)
              }

              else -> {
                return false
              }
            }
          }
        }
        .apply {
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
