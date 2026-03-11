package com.hologrampacific.flyergoblin.flyer.presentation.artist.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hologrampacific.flyergoblin.presentation.Ui

/** Loading state for embedded player widgets. */
enum class PlayerLoadingState {
  LOADING,
  LOADED,
  ERROR,
}

/**
 * Embedded HTML player that renders content in a platform-specific WebView, wrapped in a Card with
 * loading and error states.
 *
 * @param html The HTML content to render
 * @param itemCount Number of items being displayed (used for height calculation)
 * @param itemHeight Height of each item in dp
 * @param itemGap Gap between items in dp
 * @param loadingText Text shown while loading (e.g., "Loading tracks...")
 * @param errorText Text shown on error (e.g., "Failed to load tracks")
 * @param baseUrl Optional base URL for the WebView context (used on iOS for privacy features)
 * @param modifier Modifier for the composable
 */
@Composable
fun EmbeddedHtmlMultiTrackPlayer(
  html: String,
  itemCount: Int,
  itemHeight: Int,
  itemGap: Int,
  loadingText: String,
  errorText: String,
  baseUrl: String? = null,
  modifier: Modifier = Modifier,
) {
  var loadingState by remember { mutableStateOf(PlayerLoadingState.LOADING) }

  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    // On platforms where the webview handles scrolling internally (desktop/JVM), fill the
    // available space so the native browser can scroll its content. On mobile, use the
    // calculated height so the outer Compose scroll container handles scrolling.
    val boxModifier =
      if (webViewScrollsInternally) Modifier.fillMaxSize()
      else Modifier.fillMaxWidth().height((itemCount * (itemHeight + itemGap)).dp)
    Box(modifier = boxModifier) {
      PlatformWebView(
        html = html,
        baseUrl = baseUrl,
        onLoadingStateChange = { loadingState = it },
        modifier = Modifier.fillMaxSize(),
      )

      when (loadingState) {
        PlayerLoadingState.LOADING -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(Ui.halfUnit),
            ) {
              CircularProgressIndicator()
              Text(text = loadingText, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }

        PlayerLoadingState.ERROR -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = errorText, color = MaterialTheme.colorScheme.error)
          }
        }

        PlayerLoadingState.LOADED -> {}
      }
    }
  }
}

/**
 * Platform-specific WebView that renders HTML content.
 *
 * On Android, uses Android WebView with custom touch handling to allow parent scrolling. On iOS,
 * uses WKWebView with a timeout fallback for loading state. On Desktop/JVM, uses the multiplatform
 * WebView library.
 *
 * @param html The HTML content to render
 * @param baseUrl Optional base URL for the WebView context
 * @param onLoadingStateChange Callback to report loading state changes
 * @param modifier Modifier for the composable
 */
@Composable
expect fun PlatformWebView(
  html: String,
  baseUrl: String?,
  onLoadingStateChange: (PlayerLoadingState) -> Unit,
  modifier: Modifier = Modifier,
)

/**
 * True on platforms where the WebView handles scrolling internally (Desktop/JVM). On such
 * platforms, the webview should be sized to fill available space rather than the total content
 * height, so the native browser scrolls its own content.
 *
 * False on mobile platforms where the outer Compose scroll container handles scrolling and the
 * webview is sized to its full content height.
 */
expect val webViewScrollsInternally: Boolean
