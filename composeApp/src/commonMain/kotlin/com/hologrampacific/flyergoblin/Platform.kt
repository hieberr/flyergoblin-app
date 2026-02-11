package com.hologrampacific.flyergoblin

interface Platform {
  val name: String
}

expect fun getPlatform(): Platform
