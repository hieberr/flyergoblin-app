package com.hologrampacific.flyergoblin.presentation.theme

import LocalGoblinColor
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * The entire color definition for the app. Defines all colors that change when the theme changes
 */
interface AppColorTheme {

  /** Dark/Light Mode */
  enum class LightMode {
    Light,
    Dark,
  }

  /**
   * Each AppColorTheme has a low, medium, and high contrast mode which slightly changes the color
   * pallet
   */
  enum class Contrast {
    Low,
    Medium,
    High,
  }

  // material color scheme
  val colorScheme: ColorScheme

  // Custom goblin color
  val goblinColor: Color

  companion object {
    // Switch app color themes here.
    /** Singleton setting the current AppColorTheme */
    val colorTheme = HotPinkTheme(LightMode.Dark, Contrast.Low)
  }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalGoblinColor provides AppColorTheme.colorTheme.goblinColor) {
    MaterialTheme(
      colorScheme = AppColorTheme.colorTheme.colorScheme,
      typography = rememberAppTypography(),
      content = content,
    )
  }
}
