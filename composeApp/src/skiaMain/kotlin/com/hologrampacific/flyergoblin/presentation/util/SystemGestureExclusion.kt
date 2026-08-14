package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect

actual fun Modifier.platformSystemGestureExclusion(exclusionRects: List<Rect>): Modifier = this

actual val isSystemGestureExclusionSupported: Boolean = false
