package com.hologrampacific.flyergoblin

class JVMPlatform : Platform {
  override val type = PlatformType.DESKTOP
  override val version: String = "${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()
