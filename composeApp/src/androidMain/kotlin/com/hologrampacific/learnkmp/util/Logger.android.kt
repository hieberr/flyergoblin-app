package com.hologrampacific.learnkmp.util

internal actual fun isDebugBuild(): Boolean {
  // For Android, default to true for development
  // Can be overridden via system property: -Dlearnkmp.debug=false
  return System.getProperty("learnkmp.debug", "true").toBoolean()
}
