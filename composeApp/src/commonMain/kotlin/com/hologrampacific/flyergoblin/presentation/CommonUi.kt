package com.hologrampacific.flyergoblin.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.hologrampacific.flyergoblin.PlatformType
import com.hologrampacific.flyergoblin.getPlatform
import flyergoblin.composeapp.generated.resources.Res
import flyergoblin.composeapp.generated.resources.arrow_back_24px
import flyergoblin.composeapp.generated.resources.arrow_back_ios_new_24px
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import org.jetbrains.compose.resources.painterResource

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
  if (getPlatform().type == PlatformType.ANDROID) {
    Icon(
      painter = painterResource(Res.drawable.arrow_back_24px),
      contentDescription = "Back",
      modifier = Modifier.size(24.dp),
      tint = MaterialTheme.colorScheme.onSurface,
    )
  } else {
    Icon(
      painter = painterResource(Res.drawable.arrow_back_ios_new_24px),
      contentDescription = "Back",
      modifier = Modifier.size(24.dp),
      tint = MaterialTheme.colorScheme.onSurface,
    )
  }
}

/** Returns a string formatted as MM/DD/YYYY */
fun LocalDate.formattedString(): String {

  val dateFormat =
    LocalDate.Format {
      monthNumber(padding = Padding.NONE)
      char('/')
      this@Format.day(padding = Padding.NONE)
      char('/')
      year()
    }
  return dateFormat.format(this)
}

/** Converts a color to a HTML hex color string eg: "#FFFFFF */
val Color.htmlHexString: String
  get() {
    val argb = toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${
      b.toString(16).padStart(2, '0')
    }"
  }
