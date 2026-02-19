package com.hologrampacific.flyergoblin.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

actual class DriverFactory {
  actual fun createDriver(): SqlDriver {
    val appDataDir = File(System.getProperty("user.home"), ".flyergoblin").also { it.mkdirs() }
    return JdbcSqliteDriver(
      url = "jdbc:sqlite:${File(appDataDir, DATABASE_NAME).absolutePath}",
      properties = Properties().apply { put("foreign_keys", "true") },
      schema = AppDatabase.Schema,
    )
  }
}
