package com.hologrampacific.learnkmp.util

import platform.Foundation.NSBundle

internal actual fun isDebugBuild(): Boolean {
  // Check if the app is running in debug mode on iOS
  // This checks for the DEBUG preprocessor macro that Xcode sets
  return NSBundle.mainBundle.objectForInfoDictionaryKey("IS_DEBUG") as? Boolean ?: false
}
