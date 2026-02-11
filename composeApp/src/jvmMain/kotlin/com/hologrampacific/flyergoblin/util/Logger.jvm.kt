package com.hologrampacific.flyergoblin.util

internal actual fun isDebugBuild(): Boolean {
  // For JVM/Desktop, check system property or default to true for development
  return System.getProperty("flyergoblin.debug", "true").toBoolean()
}
