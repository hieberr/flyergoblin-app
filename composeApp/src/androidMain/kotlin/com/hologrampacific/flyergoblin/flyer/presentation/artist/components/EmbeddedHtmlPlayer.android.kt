package com.hologrampacific.flyergoblin.flyer.presentation.artist.components

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.hologrampacific.flyergoblin.util.AppLogger
import kotlin.math.abs

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
actual fun PlatformWebView(
  html: String,
  baseUrl: String?,
  onLoadingStateChange: (PlayerLoadingState) -> Unit,
  modifier: Modifier,
) {
  AndroidView(
    factory = { context ->
      object : WebView(context) {
          private var touchDownX = 0f
          private var touchDownY = 0f
          private var storedDownEvent: MotionEvent? = null
          private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

          // Prevent WebView from scrolling internally
          override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            storedDownEvent?.recycle()
            storedDownEvent = null
          }

          override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
            super.onScrollChanged(l, t, oldl, oldt)
            scrollTo(0, 0)
          }

          // Prevent WebView from blocking parent scroll interception
          override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            super.requestDisallowInterceptTouchEvent(false)
          }

          override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
              MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                storedDownEvent?.recycle()
                storedDownEvent = MotionEvent.obtain(event)
                return true
              }

              MotionEvent.ACTION_MOVE -> {
                val dy = abs(event.y - touchDownY)
                if (dy > touchSlop) {
                  storedDownEvent?.recycle()
                  storedDownEvent = null
                  return false
                }
                return true
              }

              MotionEvent.ACTION_UP -> {
                val dx = abs(event.x - touchDownX)
                val dy = abs(event.y - touchDownY)
                if (dx < touchSlop && dy < touchSlop && storedDownEvent != null) {
                  super.onTouchEvent(storedDownEvent)
                  val result = super.onTouchEvent(event)
                  storedDownEvent?.recycle()
                  storedDownEvent = null
                  return result
                }
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
                  "PlatformWebView",
                  "Error loading ${request?.url}: ${error?.description}",
                )
                if (request?.isForMainFrame == true) {
                  onLoadingStateChange(PlayerLoadingState.ERROR)
                }
              }
            }

          loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        }
    },
    modifier = modifier,
  )
}
