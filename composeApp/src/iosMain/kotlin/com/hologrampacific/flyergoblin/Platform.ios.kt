package com.hologrampacific.flyergoblin

import platform.UIKit.UIDevice

class IOSPlatform : Platform {
  override val type = PlatformType.IOS

  override val version: String =
    UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()
