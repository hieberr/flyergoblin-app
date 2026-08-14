package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect

actual fun Modifier.platformSystemGestureExclusion(exclusionRects: List<Rect>): Modifier =
  // Android allows at most 4 exclusion rects (200dp-per-edge limit).
  systemGestureExclusion { exclusionRects.getOrElse(0) { Rect.Zero } }
    .systemGestureExclusion { exclusionRects.getOrElse(1) { Rect.Zero } }
    .systemGestureExclusion { exclusionRects.getOrElse(2) { Rect.Zero } }
    .systemGestureExclusion { exclusionRects.getOrElse(3) { Rect.Zero } }

actual val isSystemGestureExclusionSupported: Boolean = true
