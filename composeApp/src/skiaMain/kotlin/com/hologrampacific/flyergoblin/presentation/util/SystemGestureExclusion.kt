package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

actual fun Modifier.platformSystemGestureExclusion(
  exclusionRects: (IntSize) -> List<Rect>
): Modifier = this
