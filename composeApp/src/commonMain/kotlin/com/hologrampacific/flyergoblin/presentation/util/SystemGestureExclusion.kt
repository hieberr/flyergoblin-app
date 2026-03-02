package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

/**
 * Excludes the provided rects from Android's system gesture detection (e.g. edge back-swipe).
 * The lambda receives the canvas [IntSize] so callers can compute rects in pixel coordinates.
 * On iOS and Desktop this is a no-op.
 *
 * **Android limit**: at most 4 exclusion rects are honoured per view, to stay within Android's
 * 200dp-per-edge restriction. Any rects beyond index 3 in the returned list will be ignored.
 */
expect fun Modifier.platformSystemGestureExclusion(exclusionRects: (IntSize) -> List<Rect>): Modifier
