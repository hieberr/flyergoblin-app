import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** This file contains custom theme color extensions to MaterialTheme color schemes. */

/** Goblin color for coloring goblin character in animations and images * */
val LocalGoblinColor = compositionLocalOf { Color.Unspecified }

val MaterialTheme.goblinColor: Color
  @Composable @ReadOnlyComposable get() = LocalGoblinColor.current
