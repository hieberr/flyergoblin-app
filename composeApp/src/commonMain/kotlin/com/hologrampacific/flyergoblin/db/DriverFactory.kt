package com.hologrampacific.flyergoblin.db

import app.cash.sqldelight.db.SqlDriver

const val DATABASE_NAME = "flyergoblin.db"

/**
 * Platform-specific factory for creating the SQLDelight [SqlDriver].
 *
 * Each platform's `actual` implementation requires different constructor parameters:
 * - **Android**: `actual class DriverFactory(context: Context)` — a `Context` is required to
 *   locate the database file on device storage.
 * - **iOS / JVM**: `actual class DriverFactory()` — no-arg constructor.
 *
 * Because the constructors differ, `DriverFactory` cannot be instantiated from `commonMain` code.
 * It must be constructed at the platform DI boundary and injected into shared code as a dependency.
 * Do not attempt to create a `DriverFactory` from shared code.
 */
expect class DriverFactory {
  fun createDriver(): SqlDriver
}
