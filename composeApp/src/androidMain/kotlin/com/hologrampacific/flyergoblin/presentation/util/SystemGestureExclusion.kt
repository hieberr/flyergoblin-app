package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

actual fun Modifier.platformSystemGestureExclusion(
  exclusionRects: (IntSize) -> List<Rect>
): Modifier = composed {
  // Cache the last computed list so the lambda is invoked once per layout pass,
  // not once per chained systemGestureExclusion modifier.
  val cache = remember {
    object {
      var size = IntSize.Zero
      var rects: List<Rect> = emptyList()
    }
  }

  fun rectsFor(size: IntSize): List<Rect> {
    if (size != cache.size) {
      cache.rects = exclusionRects(size)
      cache.size = size
    }
    return cache.rects
  }

  // Android allows at most 4 exclusion rects (200dp-per-edge limit).
  systemGestureExclusion { layoutCoords -> rectsFor(layoutCoords.size).getOrElse(0) { Rect.Zero } }
    .systemGestureExclusion { layoutCoords -> rectsFor(layoutCoords.size).getOrElse(1) { Rect.Zero } }
    .systemGestureExclusion { layoutCoords -> rectsFor(layoutCoords.size).getOrElse(2) { Rect.Zero } }
    .systemGestureExclusion { layoutCoords -> rectsFor(layoutCoords.size).getOrElse(3) { Rect.Zero } }
}
