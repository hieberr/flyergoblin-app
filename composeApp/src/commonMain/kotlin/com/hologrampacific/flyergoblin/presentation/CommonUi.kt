package com.hologrampacific.flyergoblin.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Standard UI constants and presets */
data object Ui {
  /**
   * Standard size/space unit. All sizes in the app should be a multiple or fraction of this if
   * possible.
   */
  val unit = 16.dp

  /** Half of a unit for convenience. */
  val halfUnit = 8.dp

  /** Spacer with a height set to unit. */
  @Composable
  fun SpacerUnitHeight() {
    Spacer(modifier = Modifier.height(unit))
  }
}

@Composable
fun BackIcon() {
  Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
}
