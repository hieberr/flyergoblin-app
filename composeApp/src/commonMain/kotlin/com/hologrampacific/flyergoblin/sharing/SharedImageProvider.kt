package com.hologrampacific.flyergoblin.sharing

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * Holds an image shared into the app from an external source (e.g. the system share sheet).
 *
 * The platform entry point (e.g. [MainActivity]) calls [setSharedImage] when an image intent
 * arrives. The UI layer listens to [navigationEvent] to know when to navigate, then calls
 * [consumePendingImage] to retrieve and clear the bytes exactly once.
 */
class SharedImageProvider {
  private val _pendingImage = MutableStateFlow<ByteArray?>(null)

  private val _navigationEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

  val navigationEvent: SharedFlow<Unit> = _navigationEvent.asSharedFlow()

  /** Sets the pending image to [bytes] and fires a [navigationEvent]. */
  fun setSharedImage(bytes: ByteArray) {
    _pendingImage.value = bytes
    _navigationEvent.tryEmit(Unit)
  }

  /**
   * Returns the pending image and atomically clears it, or `null` if none is pending. Subsequent
   * calls will return `null` until [setSharedImage] is called again.
   */
  fun consumePendingImage(): ByteArray? = _pendingImage.getAndUpdate { null }
}
