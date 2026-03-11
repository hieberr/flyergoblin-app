package com.hologrampacific.flyergoblin.flyer.presentation.artist.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.hologrampacific.flyergoblin.JcefManager
import java.awt.BorderLayout
import javax.swing.JPanel
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter

actual val webViewScrollsInternally: Boolean = true

@Composable
actual fun PlatformWebView(
  html: String,
  baseUrl: String?, // Unused on JVM: content is served via a data: URL; no base URL is needed.
  onLoadingStateChange: (PlayerLoadingState) -> Unit,
  modifier: Modifier,
) {
  val dataUrl =
    remember(html) {
      val encodedHtml = java.util.Base64.getEncoder().encodeToString(html.encodeToByteArray())
      "data:text/html;base64,$encodedHtml"
    }

  val client = remember {
    JcefManager.createClient().also { client ->
      client.addLoadHandler(
        object : CefLoadHandlerAdapter() {
          override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
            if (frame.isMain) {
              onLoadingStateChange(PlayerLoadingState.LOADED)
            }
          }

          override fun onLoadError(
            browser: CefBrowser,
            frame: CefFrame,
            errorCode: CefLoadHandler.ErrorCode,
            errorText: String,
            failedUrl: String,
          ) {
            if (frame.isMain) {
              onLoadingStateChange(PlayerLoadingState.ERROR)
            }
          }
        }
      )
    }
  }

  val browser = remember { client.createBrowser(dataUrl, false, false) }

  LaunchedEffect(dataUrl) { browser.loadURL(dataUrl) }

  DisposableEffect(Unit) {
    onDispose {
      // Skip all CEF cleanup if already shutting down: CefApp.dispose() handles all browser
      // cleanup, and calling browser.close() or client.dispose() (which internally calls
      // browser.close) from the AWT EDT during shutdown causes a macOS main-thread assertion
      // crash inside JCEF's native TempWindowMac destructor.
      if (!JcefManager.isDisposing) {
        browser.close(true)
        client.dispose()
      }
    }
  }

  SwingPanel(
    modifier = modifier.fillMaxSize(),
    factory = { JPanel(BorderLayout()).apply { add(browser.uiComponent, BorderLayout.CENTER) } },
  )
}
