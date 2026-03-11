package com.hologrampacific.flyergoblin

import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.EnumProgress
import me.friwi.jcefmaven.IProgressHandler
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter
import org.cef.CefApp
import org.cef.CefClient

/**
 * Singleton that owns the JCEF lifecycle: initialization, client creation, and shutdown.
 * All direct calls to [CefApp] are centralized here.
 */
object JcefManager {

  private val appDataDir: File
    get() = File(System.getProperty("user.home"), ".flyergoblin").also { it.mkdirs() }

  private var onTerminated: (() -> Unit)? = null

  /**
   * True once [dispose] has been called. Individual browsers must not call [CefBrowser.close]
   * or [CefClient.dispose] after this point — those calls trigger TempWindowMac destruction from
   * the AWT EDT, which causes a macOS main-thread assertion crash in AppKit.
   */
  @Volatile var isDisposing: Boolean = false
    private set

  /**
   * Initializes JCEF. Blocks the calling thread (must be called from a background dispatcher).
   * [onProgress] is dispatched to the Main dispatcher before being invoked.
   * [onTerminated] is invoked by [dispose] to exit the application.
   * Throws [Exception] on recoverable failure; the init marker is cleaned up before rethrowing.
   * Fatal [Error]s are not caught, leaving the marker for crash-detection on the next launch.
   */
  fun initialize(
    onProgress: (state: EnumProgress, percent: Float) -> Unit,
    onTerminated: () -> Unit,
  ) {
    this.onTerminated = onTerminated

    val bundleDir = File(appDataDir, "jcef-bundle")
    val initMarker = File(appDataDir, "jcef-init-in-progress")

    if (initMarker.exists()) {
      bundleDir.deleteRecursively()
      initMarker.delete()
    }

    try {
      initMarker.createNewFile()
    } catch (_: IOException) {
      // Non-fatal: proceed without crash detection for this launch.
    }

    try {
      val builder = CefAppBuilder()
      builder.setInstallDir(bundleDir)
      builder.setProgressHandler(
        IProgressHandler { state, percent ->
          // IProgressHandler is called from jcefmaven's internal thread — dispatch to Main.
          GlobalScope.launch(Dispatchers.Main) { onProgress(state, percent) }
        }
      )
      // Register the app handler via the builder — do NOT use CefApp.addAppHandler() directly,
      // as jcefmaven's adapter overrides onBeforeCommandLineProcessing in a way required for
      // correct macOS operation.
      builder.setAppHandler(object : MavenCefAppHandlerAdapter() {})
      // Windowed rendering (false) is required for Swing-based embedding via SwingPanel.
      // OSR (off-screen rendering) mode is not used in this app.
      builder.cefSettings.windowless_rendering_enabled = false
      builder.cefSettings.root_cache_path = File(appDataDir, "jcef-cache").absolutePath
      builder.build()
      initMarker.delete()
    } catch (e: Exception) {
      // Delete the marker for recoverable failures so the next launch does not unnecessarily
      // re-download the bundle. Fatal Errors are not caught here, so the marker remains for
      // crash-detection on the next launch.
      initMarker.delete()
      throw e
    }
  }

  /** Creates a new [CefClient]. The caller is responsible for calling [CefClient.dispose]. */
  fun createClient(): CefClient = CefApp.getInstance().createClient()

  /**
   * Initiates shutdown. Calls [onTerminated] immediately so the caller can exit cleanly.
   *
   * We intentionally skip [CefApp.dispose] here. On macOS, [CefApp.dispose] synchronously
   * destroys windowed browser contexts from the calling thread, which triggers
   * [TempWindowMac::~TempWindowMac] → [NSWindow._close] — an AppKit operation that requires
   * Thread 0 (the AppKit main thread). Since [dispose] is called from the AWT EDT (which is a
   * separate thread on macOS in Compose Desktop), this causes an EXC_BREAKPOINT crash.
   *
   * Skipping [CefApp.dispose] is safe: Chromium's child processes (renderer, GPU, utility) are
   * designed to detect when the browser process exits and terminate themselves.
   */
  fun dispose() {
    isDisposing = true
    onTerminated?.invoke()
  }
}
