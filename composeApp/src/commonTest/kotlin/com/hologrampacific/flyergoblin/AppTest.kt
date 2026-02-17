package com.hologrampacific.flyergoblin

import com.hologrampacific.flyergoblin.util.AppLogger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base class for all tests. Suppresses log output during test runs to keep the console clean, since
 * tests routinely exercise error paths that would otherwise produce expected-but-noisy warnings and
 * errors.
 *
 * To re-enable logging while debugging a specific test, override [suppressLogs] with an empty body
 * in the test class:
 * ```
 * override fun suppressLogs() = Unit
 * ```
 *
 * Search for "suppressLogs() = Unit" to find any tests where logging is currently re-enabled.
 */
abstract class AppTest {

  @BeforeTest open fun suppressLogs() = AppLogger.suppressLogging()

  @AfterTest fun restoreLogs() = AppLogger.resetLogging()
}
