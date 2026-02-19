package com.hologrampacific.flyergoblin.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration

actual class DriverFactory {
  actual fun createDriver(): SqlDriver =
    NativeSqliteDriver(
      DatabaseConfiguration(
        name = DATABASE_NAME,
        version = AppDatabase.Schema.version.toInt(),
        create = { connection -> wrapConnection(connection) { AppDatabase.Schema.create(it) } },
        upgrade = { connection, oldVersion, newVersion ->
          wrapConnection(connection) {
            AppDatabase.Schema.migrate(it, oldVersion.toLong(), newVersion.toLong())
          }
        },
        extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true),
      )
    )
}
