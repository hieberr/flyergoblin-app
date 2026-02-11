package com.hologrampacific.flyergoblin

class Greeting {
  private val platform = getPlatform()

  fun greet(): String {
    return "Hello, ${platform.name}!"
  }
}
