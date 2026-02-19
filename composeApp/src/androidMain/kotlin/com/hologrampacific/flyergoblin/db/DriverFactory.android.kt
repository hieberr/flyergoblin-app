package com.hologrampacific.flyergoblin.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DriverFactory(private val context: Context) {
  actual fun createDriver(): SqlDriver =
    AndroidSqliteDriver(
      schema = AppDatabase.Schema,
      context = context,
      name = DATABASE_NAME,
      callback =
        object : AndroidSqliteDriver.Callback(AppDatabase.Schema) {
          override fun onConfigure(db: SupportSQLiteDatabase) {
            super.onConfigure(db)
            db.setForeignKeyConstraintsEnabled(true)
          }
        },
    )
}
