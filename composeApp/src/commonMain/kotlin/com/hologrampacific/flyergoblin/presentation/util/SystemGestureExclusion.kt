package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect

/**
 * Excludes the provided rects from Android's system gesture detection (e.g. edge back-swipe). On
 * iOS and Desktop this is a no-op.
 *
 * Callers must recompute [exclusionRects] reactively (e.g. via `remember(key1, key2) { ... }`) so
 * that the list updates as its inputs change — a stale list left over from a previous composition
 * will keep excluding the wrong area. Check [isSystemGestureExclusionSupported] before doing that
 * work, since it's discarded on platforms where this is a no-op.
 *
 * **Android limit**: at most 4 exclusion rects are honoured per view, to stay within Android's
 * 200dp-per-edge restriction. Any rects beyond index 3 in the list will be ignored.
 */
expect fun Modifier.platformSystemGestureExclusion(exclusionRects: List<Rect>): Modifier

/** Whether [platformSystemGestureExclusion] does anything on this platform. */
expect val isSystemGestureExclusionSupported: Boolean
