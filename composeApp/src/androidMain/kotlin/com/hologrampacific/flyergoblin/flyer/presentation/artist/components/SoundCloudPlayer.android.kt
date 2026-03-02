package com.hologrampacific.flyergoblin.flyer.presentation.artist.components

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.flyergoblin.presentation.Ui
import com.hologrampacific.flyergoblin.presentation.util.buildMultiTrackWidgetHtml
import com.hologrampacific.flyergoblin.presentation.util.htmlHexString
import com.hologrampacific.flyergoblin.util.AppLogger
import kotlin.math.abs

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
            // todo: The height of this box looks a little strange
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
            modifier = Modifier.fillMaxWidth().padding(Ui.unit),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(text = "Failed to load tracks", color = MaterialTheme.colorScheme.error)
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
        modifier =
          Modifier.fillMaxWidth()
            .height((tracks.size * (SOUNDCLOUD_TRACK_HEIGHT + SOUNDCLOUD_TRACK_GAP)).dp),
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
  val backgroundColor = MaterialTheme.colorScheme.background

  val html =
    remember(tracks, backgroundColor) {
      buildMultiTrackWidgetHtml(
        tracks.map { it.url },
        SOUNDCLOUD_TRACK_HEIGHT,
        SOUNDCLOUD_TRACK_GAP,
        backgroundColor = backgroundColor.htmlHexString,
      )
    }

  AndroidView(
    factory = { context ->
      object : WebView(context) {
          private var touchDownX = 0f
          private var touchDownY = 0f
          private var storedDownEvent: MotionEvent? = null
          private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

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

          override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
              MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                // Store a copy of the DOWN event to replay later if it's a tap
                storedDownEvent?.recycle()
                storedDownEvent = MotionEvent.obtain(event)
                // Return true to claim the event series, but DON'T pass to WebView yet
                return true
              }

              MotionEvent.ACTION_MOVE -> {
                val dy = abs(event.y - touchDownY)
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

              MotionEvent.ACTION_UP -> {
                val dx = abs(event.x - touchDownX)
                val dy = abs(event.y - touchDownY)
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

              MotionEvent.ACTION_CANCEL -> {
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
          setLayerType(View.LAYER_TYPE_HARDWARE, null)
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
