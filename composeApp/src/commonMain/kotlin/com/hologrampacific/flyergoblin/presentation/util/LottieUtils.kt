package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import goblinColor
import io.github.alexzhirkevich.compottie.ExperimentalCompottieApi
import io.github.alexzhirkevich.compottie.LottieComposition
import io.github.alexzhirkevich.compottie.dynamic.rememberLottieDynamicProperties

/**
 * Returns a progress lambda for use with [rememberLottiePainter] that plays the animation fully
 * once from the beginning, then loops from [loopStartSeconds] to the end indefinitely.
 *
 * @param composition The loaded [LottieComposition], or null while still loading.
 * @param loopStartSeconds The time in seconds to jump back to at the start of each loop.
 */
@Composable
fun rememberLottieLoopProgress(
  composition: LottieComposition?,
  loopStartSeconds: Float,
): () -> Float {
  val progress = remember { Animatable(0f) }
  LaunchedEffect(composition) {
    val c = composition ?: return@LaunchedEffect
    val totalMs = c.duration.inWholeMilliseconds.toInt()
    val loopStartProgress = loopStartSeconds / (totalMs / 1000f)
    val loopMs = ((1f - loopStartProgress) * totalMs).toInt()
    progress.animateTo(1f, animationSpec = tween(totalMs, easing = LinearEasing))
    while (true) {
      progress.snapTo(loopStartProgress)
      progress.animateTo(1f, animationSpec = tween(loopMs, easing = LinearEasing))
    }
  }
  return progress::value
}

/**
 * Returns [LottieDynamicProperties] that recolors all shape fills and strokes to the current
 * theme's goblin color.
 */
@OptIn(ExperimentalCompottieApi::class)
@Composable
fun rememberGoblinDynamicProperties() =
  MaterialTheme.goblinColor.let { goblinColor ->
    rememberLottieDynamicProperties(goblinColor) {
      shapeLayer("**") {
        fill("**") { color { goblinColor } }
        stroke("**") { color { goblinColor } }
      }
    }
  }
