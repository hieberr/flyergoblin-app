package com.hologrampacific.flyergoblin.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import flyergoblin.composeapp.generated.resources.Jersey15_Regular
import flyergoblin.composeapp.generated.resources.OpenSans_Italic_VariableFont
import flyergoblin.composeapp.generated.resources.OpenSans_VariableFont
import flyergoblin.composeapp.generated.resources.Res
import flyergoblin.composeapp.generated.resources.Roboto_Italic_VariableFont
import flyergoblin.composeapp.generated.resources.Roboto_VariableFont
import flyergoblin.composeapp.generated.resources.Sixtyfour_Regular_VariableFont
import flyergoblin.composeapp.generated.resources.Workbench_Regular_VariableFont
import org.jetbrains.compose.resources.Font

val SixtyfourFontFamily
  @Composable get() = FontFamily(Font(Res.font.Sixtyfour_Regular_VariableFont))

// Sixtyfour renders larger than other fonts at the same sp value. Tune this to compensate.
private fun TextStyle.modifiedForSixtyfour(): TextStyle = copy(fontSize = fontSize * 0.75f)

val WorkBenchFontFamily
  @Composable get() = FontFamily(Font(Res.font.Workbench_Regular_VariableFont))

private fun TextStyle.modifiedForWorkBench(): TextStyle = copy(fontSize = fontSize * 1.0)

// Jersey15 renders smaller than other fonts at the same sp value. Tune this to compensate.
val Jersey15FontFamily
  @Composable get() = FontFamily(Font(Res.font.Jersey15_Regular))

private fun TextStyle.modifiedForJersey(): TextStyle = copy(fontSize = fontSize * 1.3)

val OpenSansFontFamily
  @Composable
  get() =
    FontFamily(
      Font(Res.font.OpenSans_VariableFont, style = FontStyle.Normal),
      Font(Res.font.OpenSans_Italic_VariableFont, style = FontStyle.Italic),
    )

val RobotoFontFamily
  @Composable
  get() =
    FontFamily(
      Font(Res.font.Roboto_VariableFont, style = FontStyle.Normal),
      Font(Res.font.Roboto_Italic_VariableFont, style = FontStyle.Italic),
    )

// Switch typography theme here

// Good but I like the look jersey is more readable for smaller fonts
// @Composable fun rememberAppTypography() = rememberSixtyfourWorkbenchTypography()

// Good mix of the three
@Composable fun rememberAppTypography() = rememberSixtyfourWorkbenchJerseyTypography()

// Good, but it ends up being mostly jersey
// @Composable fun rememberAppTypography() = rememberSixtyfourJerseyTypography()

@Composable
private fun rememberSixtyfourWorkbenchTypography(): Typography {
  val sixtyfour = SixtyfourFontFamily
  val workBench = WorkBenchFontFamily
  val baseline = Typography()
  fun TextStyle.withSixtyfour() = copy(fontFamily = sixtyfour).modifiedForSixtyfour()
  fun TextStyle.withWorkBench() = copy(fontFamily = workBench).modifiedForWorkBench()

  return Typography(
    displayLarge = baseline.displayLarge.withSixtyfour(),
    displayMedium = baseline.displayMedium.withSixtyfour(),
    displaySmall = baseline.displaySmall.withWorkBench(),
    headlineLarge = baseline.headlineLarge.withSixtyfour(),
    headlineMedium = baseline.headlineMedium.withSixtyfour(),
    headlineSmall = baseline.headlineSmall.withSixtyfour(),
    titleLarge = baseline.titleLarge.withWorkBench(),
    titleMedium = baseline.titleMedium.withWorkBench(),
    titleSmall = baseline.titleSmall.withWorkBench(),
    bodyLarge = baseline.bodyLarge.withWorkBench(),
    bodyMedium = baseline.bodyMedium.withWorkBench(),
    bodySmall = baseline.bodySmall.withWorkBench(),
    labelLarge = baseline.labelLarge.withSixtyfour(),
    labelMedium = baseline.labelMedium.withWorkBench(),
    labelSmall = baseline.labelSmall.withWorkBench(),
  )
}

@Composable
private fun rememberSixtyfourJerseyTypography(): Typography {
  val sixtyfour = SixtyfourFontFamily
  val jersey = Jersey15FontFamily
  val baseline = Typography()
  fun TextStyle.withSixtyfour() = copy(fontFamily = sixtyfour).modifiedForSixtyfour()
  fun TextStyle.withJersey() = copy(fontFamily = jersey).modifiedForJersey()

  return Typography(
    displayLarge = baseline.displayLarge.withSixtyfour(),
    displayMedium = baseline.displayMedium.withSixtyfour(),
    displaySmall = baseline.displaySmall.withJersey(),
    headlineLarge = baseline.headlineLarge.withSixtyfour(),
    headlineMedium = baseline.headlineMedium.withSixtyfour(),
    headlineSmall = baseline.headlineSmall.withSixtyfour(),
    titleLarge = baseline.titleLarge.withJersey(),
    titleMedium = baseline.titleMedium.withJersey(),
    titleSmall = baseline.titleSmall.withJersey(),
    bodyLarge = baseline.bodyLarge.withJersey(),
    bodyMedium = baseline.bodyMedium.withJersey(),
    bodySmall = baseline.bodySmall.withJersey(),
    labelLarge = baseline.labelLarge.withSixtyfour(),
    labelMedium = baseline.labelMedium.withJersey(),
    labelSmall = baseline.labelSmall.withJersey(),
  )
}

@Composable
private fun rememberSixtyfourWorkbenchJerseyTypography(): Typography {
  val sixtyfour = SixtyfourFontFamily
  val workbench = WorkBenchFontFamily
  val jersey = Jersey15FontFamily
  val baseline = Typography()
  fun TextStyle.withSixtyfour() = copy(fontFamily = sixtyfour).modifiedForSixtyfour()
  fun TextStyle.withWorkbench() = copy(fontFamily = workbench).modifiedForWorkBench()
  fun TextStyle.withJersey() = copy(fontFamily = jersey).modifiedForJersey()

  return Typography(
    displayLarge = baseline.displayLarge.withSixtyfour(),
    displayMedium = baseline.displayMedium.withSixtyfour(),
    displaySmall = baseline.displaySmall.withJersey(),
    headlineLarge = baseline.headlineLarge.withSixtyfour(),
    headlineMedium = baseline.headlineMedium.withSixtyfour(),
    headlineSmall = baseline.headlineSmall.withSixtyfour(),
    titleLarge = baseline.titleLarge.withWorkbench(),
    titleMedium = baseline.titleMedium.withJersey(),
    titleSmall = baseline.titleSmall.withJersey(),
    bodyLarge = baseline.bodyLarge.withWorkbench(),
    bodyMedium = baseline.bodyMedium.withJersey(),
    bodySmall = baseline.bodySmall.withJersey(),
    labelLarge = baseline.labelLarge.withSixtyfour(),
    labelMedium = baseline.labelMedium.withWorkbench(),
    labelSmall = baseline.labelSmall.withJersey(),
  )
}

@Composable
private fun rememberWorkbenchTypography(): Typography {
  val workBench = WorkBenchFontFamily
  val baseline = Typography()
  fun TextStyle.withWorkBench() = copy(fontFamily = workBench).modifiedForWorkBench()
  return Typography(
    displayLarge = baseline.displayLarge.withWorkBench(),
    displayMedium = baseline.displayMedium.withWorkBench(),
    displaySmall = baseline.displaySmall.withWorkBench(),
    headlineLarge = baseline.headlineLarge.withWorkBench(),
    headlineMedium = baseline.headlineMedium.withWorkBench(),
    headlineSmall = baseline.headlineSmall.withWorkBench(),
    titleLarge = baseline.titleLarge.withWorkBench(),
    titleMedium = baseline.titleMedium.withWorkBench(),
    titleSmall = baseline.titleSmall.withWorkBench(),
    bodyLarge = baseline.bodyLarge.withWorkBench(),
    bodyMedium = baseline.bodyMedium.withWorkBench(),
    bodySmall = baseline.bodySmall.withWorkBench(),
    labelLarge = baseline.labelLarge.withWorkBench(),
    labelMedium = baseline.labelMedium.withWorkBench(),
    labelSmall = baseline.labelSmall.withWorkBench(),
  )
}

// Nice game style pixel font
@Composable
private fun rememberJerseyTypography(): Typography {
  val jersey = Jersey15FontFamily
  fun TextStyle.withJersey() = copy(fontFamily = jersey).modifiedForJersey()

  val baseline = Typography()
  return Typography(
    displayLarge = baseline.displayLarge.withJersey(),
    displayMedium = baseline.displayMedium.withJersey(),
    displaySmall = baseline.displaySmall.withJersey(),
    headlineLarge = baseline.headlineLarge.withJersey(),
    headlineMedium = baseline.headlineMedium.withJersey(),
    headlineSmall = baseline.headlineSmall.withJersey(),
    titleLarge = baseline.titleLarge.withJersey(),
    titleMedium = baseline.titleMedium.withJersey(),
    titleSmall = baseline.titleSmall.withJersey(),
    bodyLarge = baseline.bodyLarge.withJersey(),
    bodyMedium = baseline.bodyMedium.withJersey(),
    bodySmall = baseline.bodySmall.withJersey(),
    labelLarge = baseline.labelLarge.withJersey(),
    labelMedium = baseline.labelMedium.withJersey(),
    labelSmall = baseline.labelSmall.withJersey(),
  )
}

// Standard sans serif font for reference
@Composable
private fun rememberOpenSansTypography(): Typography {
  val openSans = OpenSansFontFamily
  val baseline = Typography()
  return Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = openSans),
    displayMedium = baseline.displayMedium.copy(fontFamily = openSans),
    displaySmall = baseline.displaySmall.copy(fontFamily = openSans),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = openSans),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = openSans),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = openSans),
    titleLarge = baseline.titleLarge.copy(fontFamily = openSans),
    titleMedium = baseline.titleMedium.copy(fontFamily = openSans),
    titleSmall = baseline.titleSmall.copy(fontFamily = openSans),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = openSans),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = openSans),
    bodySmall = baseline.bodySmall.copy(fontFamily = openSans),
    labelLarge = baseline.labelLarge.copy(fontFamily = openSans),
    labelMedium = baseline.labelMedium.copy(fontFamily = openSans),
    labelSmall = baseline.labelSmall.copy(fontFamily = openSans),
  )
}

// Standard sans serif font for reference
@Composable
private fun rememberRobotoTypography(): Typography {
  val roboto = RobotoFontFamily
  val baseline = Typography()
  return Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = roboto),
    displayMedium = baseline.displayMedium.copy(fontFamily = roboto),
    displaySmall = baseline.displaySmall.copy(fontFamily = roboto),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = roboto),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = roboto),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = roboto),
    titleLarge = baseline.titleLarge.copy(fontFamily = roboto),
    titleMedium = baseline.titleMedium.copy(fontFamily = roboto),
    titleSmall = baseline.titleSmall.copy(fontFamily = roboto),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = roboto),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = roboto),
    bodySmall = baseline.bodySmall.copy(fontFamily = roboto),
    labelLarge = baseline.labelLarge.copy(fontFamily = roboto),
    labelMedium = baseline.labelMedium.copy(fontFamily = roboto),
    labelSmall = baseline.labelSmall.copy(fontFamily = roboto),
  )
}
