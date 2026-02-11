package com.hologrampacific.flyergoblin.util

internal actual fun isDebugBuild(): Boolean {
  // For Android, default to true for development
  // Can be overridden via system property: -Dflyergoblin.debug=false
  return System.getProperty("flyergoblin.debug", "true").toBoolean()
}
