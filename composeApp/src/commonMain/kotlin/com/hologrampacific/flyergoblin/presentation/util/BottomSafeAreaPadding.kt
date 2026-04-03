package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Returns the bottom padding needed to keep content above the navigation bar / home indicator.
 *
 * Uses [WindowInsets.navigationBars] (not `safeDrawing`) so the value excludes the IME — the parent
 * [AppNavDisplay][com.hologrampacific.flyergoblin.presentation.AppNavDisplay] already applies
 * `imePadding()` globally. When the keyboard **is** visible the IME padding alone clears the bottom
 * edge, so this function returns `0.dp` to avoid a redundant gap.
 *
 * The IME check compares against the navigation bar height rather than zero, because on iOS a known
 * Compose Multiplatform issue (JetBrains/compose-multiplatform#4618) can cause the IME bottom inset
 * to briefly return a stale non-zero value after the keyboard dismisses.
 *
 * This is safe to call from sub-screens even though the parent `MainScreen` Scaffold uses
 * `contentWindowInsets = WindowInsets.safeDrawing`, because M3 Scaffold does not consume insets via
 * the Compose `consumeWindowInsets` system — only explicit `consumeWindowInsets` calls do, and the
 * parent consumes `bottom = 0.dp` for sub-screens.
 */
@Composable
fun bottomSafeAreaPadding(): Dp {
  val navBarBottom =
    WindowInsets.navigationBars
      .only(WindowInsetsSides.Bottom)
      .asPaddingValues()
      .calculateBottomPadding()
  // The keyboard is "truly visible" when IME bottom exceeds the navigation bar height.
  // This avoids false positives from stale IME inset values on iOS.
  val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
  return if (imeBottom > navBarBottom) 0.dp else navBarBottom
}
