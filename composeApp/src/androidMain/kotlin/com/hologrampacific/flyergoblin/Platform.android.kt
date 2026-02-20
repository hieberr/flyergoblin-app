package com.hologrampacific.flyergoblin

import android.os.Build

class AndroidPlatform : Platform {
  override val version: String = "${Build.VERSION.SDK_INT}"
  override val type = PlatformType.ANDROID
}

actual fun getPlatform(): Platform = AndroidPlatform()
