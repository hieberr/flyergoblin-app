package com.hologrampacific.flyergoblin

enum class PlatformType {
  ANDROID,
  IOS,
  DESKTOP,
}

interface Platform {
  val version: String
  val type: PlatformType
}

expect fun getPlatform(): Platform
