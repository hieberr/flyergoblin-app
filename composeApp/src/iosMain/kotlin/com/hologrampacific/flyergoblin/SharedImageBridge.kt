package com.hologrampacific.flyergoblin

import com.hologrampacific.flyergoblin.sharing.SharedImageProvider

object SharedImageBridge {
  var sharedImageProvider: SharedImageProvider? = null

  fun setSharedImageFromNative(imageBytes: ByteArray) {
    sharedImageProvider?.setSharedImage(imageBytes)
  }
}

// Top-level function called from Swift via the generated Kotlin/Native header.
fun setSharedImageFromNative(imageBytes: ByteArray) {
  SharedImageBridge.setSharedImageFromNative(imageBytes)
}
