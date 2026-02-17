package com.hologrampacific.flyergoblin.util

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig

/**
 * Centralized logger for the application.
 *
 * Usage:
 * - AppLogger.d("tag", "Debug message")
 * - AppLogger.i("tag", "Info message")
 * - AppLogger.w("tag", "Warning message")
 * - AppLogger.e("tag", "Error message", throwable)
 */
object AppLogger {

  private val defaultSeverity
    get() = if (isDebugBuild()) Severity.Verbose else Severity.Info

  private fun createLogger(severity: Severity = defaultSeverity) =
    Logger(
      config =
        StaticConfig(
          // In production builds, you can set minSeverity to Severity.Info or Severity.Warn
          // to filter out debug logs
          minSeverity = severity
        ),
      tag = "Flyer Goblin",
    )

  private var logger = createLogger()

  /** Suppress all log output. Intended for use in tests only. */
  internal fun suppressLogging() {
    logger = createLogger(Severity.Assert)
  }

  /** Restore default log output. Intended for use in tests only. */
  internal fun resetLogging() {
    logger = createLogger()
  }

  fun v(tag: String, message: String, throwable: Throwable? = null) {
    logger.v(throwable) { "[$tag] $message" }
  }

  fun d(tag: String, message: String, throwable: Throwable? = null) {
    logger.d(throwable) { "[$tag] $message" }
  }

  fun i(tag: String, message: String, throwable: Throwable? = null) {
    logger.i(throwable) { "[$tag] $message" }
  }

  fun w(tag: String, message: String, throwable: Throwable? = null) {
    logger.w(throwable) { "[$tag] $message" }
  }

  fun e(tag: String, message: String, throwable: Throwable? = null) {
    logger.e(throwable) { "[$tag] $message" }
  }
}

/**
 * Determines if this is a debug build. Override this in platform-specific implementations if
 * needed.
 */
internal expect fun isDebugBuild(): Boolean
