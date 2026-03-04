package com.hologrampacific.flyergoblin.flyer.presentation.artist.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import platform.Foundation.NSURL
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

/**
 * Holds a strong reference to the WKWebView navigation delegate and tracks loading state. WKWebView
 * .navigationDelegate is declared as `weak` in Objective-C, so without an explicit holder the
 * delegate object would be immediately deallocated after the factory lambda exits, causing
 * navigation callbacks to never fire.
 */
private class WebViewDelegateHolder {
  var delegate: NSObject? = null
  var definitiveStateSeen = false
  /** Last HTML content loaded, used to detect changes in the update lambda. */
  var lastHtml: String = ""
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformWebView(
  html: String,
  baseUrl: String?,
  onLoadingStateChange: (PlayerLoadingState) -> Unit,
  modifier: Modifier,
) {
  val holder = remember { WebViewDelegateHolder() }

  // Timeout fallback: if the navigation delegate doesn't fire within 3 seconds, mark as loaded.
  // Guard with definitiveStateSeen so an error state set by the delegate is not overwritten.
  LaunchedEffect(html) {
    holder.definitiveStateSeen = false
    delay(3000)
    if (!holder.definitiveStateSeen) {
      onLoadingStateChange(PlayerLoadingState.LOADED)
    }
  }

  UIKitView(
    factory = {
      val configuration =
        WKWebViewConfiguration().apply {
          allowsInlineMediaPlayback = true
          suppressesIncrementalRendering = false
        }

      val webView = WKWebView(frame = kotlinx.cinterop.cValue {}, configuration = configuration)

      // iOS Scrolling Notes
      // Touch down events seem to always be processed by the track iFrames even though the KMP docs
      // say that there should be a delay to detect scrolls for UIKitViews. During the delay KMP is
      // supposed to detect that scrolling has begun and then no events are passed to the UiKitView.
      // But, this doesn't seem to be happening. Possibly this is because we are using a WKWebView
      // which maybe doesn't respect this. I'm not sure how to solve this but, it's not the worst
      // bug.

      // Disabling the scroll in the webview makes sense since we handle scrolling in KMP. But, when
      // I disable scrollView scrolling touches that begin between the iframes (the track players)
      // don't register and scrolling doesn't start. Since this webview scrolling doesn't do
      // anything we can just leave it enabled and with bounces turned off.
      // webView.scrollView.setScrollEnabled(false)
      webView.scrollView.bounces = false

      val delegate =
        object : NSObject(), WKNavigationDelegateProtocol {
          override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
            holder.definitiveStateSeen = true
            onLoadingStateChange(PlayerLoadingState.LOADED)
          }

          override fun webView(
            webView: WKWebView,
            didFailNavigation: WKNavigation?,
            withError: platform.Foundation.NSError,
          ) {
            holder.definitiveStateSeen = true
            onLoadingStateChange(PlayerLoadingState.ERROR)
          }
        }

      // Retain the delegate in the holder; without this the weak navigationDelegate reference
      // would be the only reference and the delegate could be GC'd immediately.
      holder.delegate = delegate
      webView.navigationDelegate = delegate

      webView
    },
    update = { webView ->
      // Load (or reload) HTML content when it changes. Using update instead of factory ensures
      // the WKWebView instance is reused across recompositions that change the HTML, avoiding
      // delegate reference races and unnecessary view recreation.
      if (holder.lastHtml != html) {
        holder.lastHtml = html
        val nsBaseUrl = baseUrl?.let { NSURL.URLWithString(it) }
        webView.loadHTMLString(html, baseURL = nsBaseUrl)
      }
    },
    modifier = modifier,
    properties = UIKitInteropProperties(isInteractive = true, isNativeAccessibilityEnabled = true),
  )
}
