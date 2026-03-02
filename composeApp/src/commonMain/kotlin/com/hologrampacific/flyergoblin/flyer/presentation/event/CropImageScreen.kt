package com.hologrampacific.flyergoblin.flyer.presentation.event

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hologrampacific.flyergoblin.presentation.Ui
import com.hologrampacific.flyergoblin.presentation.components.CtaButtons
import com.hologrampacific.flyergoblin.presentation.components.ScreenButtonConfig
import com.hologrampacific.flyergoblin.presentation.util.cropImage
import com.hologrampacific.flyergoblin.presentation.util.decodeImageBitmap
import com.hologrampacific.flyergoblin.presentation.util.platformSystemGestureExclusion
import kotlin.math.sqrt
import kotlinx.coroutines.launch

private data class CropState(
  val topLeftX: Float = 0f,
  val topLeftY: Float = 0f,
  val bottomRightX: Float = 1f,
  val bottomRightY: Float = 1f,
)

private enum class Handle {
  TopLeft,
  TopRight,
  BottomLeft,
  BottomRight,
  None,
}

/**
 * Computes the rect in which the image is displayed within the canvas, fitting the image within the
 * available area while maintaining aspect ratio. [hPadPx] is the horizontal padding to leave on
 * each side.
 */
private fun imageDisplayRect(
  canvasWidth: Float,
  canvasHeight: Float,
  imageWidth: Int,
  imageHeight: Int,
  hPadPx: Float,
): Rect {
  val availableWidth = canvasWidth - 2f * hPadPx
  val scale = minOf(availableWidth / imageWidth, canvasHeight / imageHeight)
  val displayWidth = imageWidth * scale
  val displayHeight = imageHeight * scale
  val left = hPadPx + (availableWidth - displayWidth) / 2f
  val top = (canvasHeight - displayHeight) / 2f
  return Rect(left, top, left + displayWidth, top + displayHeight)
}

@Composable
fun CropImageScreen(imageBytes: ByteArray, onDone: (ByteArray) -> Unit, onCancel: () -> Unit) {
  val imageBitmap = remember(imageBytes) { decodeImageBitmap(imageBytes) }

  if (imageBitmap == null) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
      Column(modifier = Modifier.align(Alignment.Center)) {
        Text(
          text = "Unable to load image",
          color = Color.White,
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(Ui.unit))
        CtaButtons(primaryButtonConfig = ScreenButtonConfig(text = "Cancel", onClick = onCancel))
      }
    }
    return
  }

  val cropState = remember { mutableStateOf(CropState()) }
  val activeHandle = remember { mutableStateOf(Handle.None) }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current
  val hPadPx = with(density) { Ui.unit.toPx() }
  val touchRadiusPx = with(density) { 44.dp.toPx() }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
      Spacer(modifier = Modifier.height(Ui.halfUnit))

      Text(
        text = "Crop",
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        modifier = Modifier.align(Alignment.CenterHorizontally),
      )

      Spacer(modifier = Modifier.height(Ui.halfUnit))

      Canvas(
        modifier =
          Modifier.fillMaxWidth()
            .weight(1f)
            // Only exclude small rects around each handle — stays within Android's 200dp-per-edge
            // limit.
            .platformSystemGestureExclusion { canvasIntSize ->
              val ir =
                imageDisplayRect(
                  canvasIntSize.width.toFloat(),
                  canvasIntSize.height.toFloat(),
                  imageBitmap.width,
                  imageBitmap.height,
                  hPadPx,
                )
              val cs = cropState.value
              listOf(
                  Offset(ir.left + cs.topLeftX * ir.width, ir.top + cs.topLeftY * ir.height),
                  Offset(ir.left + cs.bottomRightX * ir.width, ir.top + cs.topLeftY * ir.height),
                  Offset(ir.left + cs.topLeftX * ir.width, ir.top + cs.bottomRightY * ir.height),
                  Offset(ir.left + cs.bottomRightX * ir.width, ir.top + cs.bottomRightY * ir.height),
                )
                .map { center ->
                  Rect(
                    center.x - touchRadiusPx,
                    center.y - touchRadiusPx,
                    center.x + touchRadiusPx,
                    center.y + touchRadiusPx,
                  )
                }
            }
            .pointerInput(Unit) {
              var dragImageRect = Rect.Zero
              detectDragGestures(
                onDragStart = { offset ->
                  val ir =
                    imageDisplayRect(
                      size.width.toFloat(),
                      size.height.toFloat(),
                      imageBitmap.width,
                      imageBitmap.height,
                      hPadPx,
                    )
                  dragImageRect = ir
                  val cs = cropState.value
                  val tlX = ir.left + cs.topLeftX * ir.width
                  val tlY = ir.top + cs.topLeftY * ir.height
                  val brX = ir.left + cs.bottomRightX * ir.width
                  val brY = ir.top + cs.bottomRightY * ir.height

                  fun dist(ax: Float, ay: Float): Float {
                    val dx = offset.x - ax
                    val dy = offset.y - ay
                    return sqrt(dx * dx + dy * dy)
                  }

                  activeHandle.value =
                    when {
                      dist(tlX, tlY) <= touchRadiusPx -> Handle.TopLeft
                      dist(brX, tlY) <= touchRadiusPx -> Handle.TopRight
                      dist(tlX, brY) <= touchRadiusPx -> Handle.BottomLeft
                      dist(brX, brY) <= touchRadiusPx -> Handle.BottomRight
                      else -> Handle.None
                    }
                },
                onDrag = { change, dragAmount ->
                  change.consume()
                  val ah = activeHandle.value
                  if (ah != Handle.None) {
                    val cs = cropState.value
                    val ir = dragImageRect
                    val dx = dragAmount.x / ir.width
                    val dy = dragAmount.y / ir.height
                    cropState.value =
                      when (ah) {
                        Handle.TopLeft ->
                          cs.copy(
                            topLeftX = (cs.topLeftX + dx).coerceIn(0f, cs.bottomRightX - 0.05f),
                            topLeftY = (cs.topLeftY + dy).coerceIn(0f, cs.bottomRightY - 0.05f),
                          )
                        Handle.TopRight ->
                          cs.copy(
                            bottomRightX = (cs.bottomRightX + dx).coerceIn(cs.topLeftX + 0.05f, 1f),
                            topLeftY = (cs.topLeftY + dy).coerceIn(0f, cs.bottomRightY - 0.05f),
                          )
                        Handle.BottomLeft ->
                          cs.copy(
                            topLeftX = (cs.topLeftX + dx).coerceIn(0f, cs.bottomRightX - 0.05f),
                            bottomRightY = (cs.bottomRightY + dy).coerceIn(cs.topLeftY + 0.05f, 1f),
                          )
                        Handle.BottomRight ->
                          cs.copy(
                            bottomRightX = (cs.bottomRightX + dx).coerceIn(cs.topLeftX + 0.05f, 1f),
                            bottomRightY = (cs.bottomRightY + dy).coerceIn(cs.topLeftY + 0.05f, 1f),
                          )
                        Handle.None -> cs
                      }
                  }
                },
              )
            }
      ) {
        val cs = cropState.value
        val ir =
          imageDisplayRect(size.width, size.height, imageBitmap.width, imageBitmap.height, hPadPx)

        // Draw image scaled to fit
        drawImage(
          image = imageBitmap,
          dstOffset = IntOffset(ir.left.toInt(), ir.top.toInt()),
          dstSize = IntSize(ir.width.toInt(), ir.height.toInt()),
        )

        // Crop rect in canvas coordinates
        val cropLeft = ir.left + cs.topLeftX * ir.width
        val cropTop = ir.top + cs.topLeftY * ir.height
        val cropRight = ir.left + cs.bottomRightX * ir.width
        val cropBottom = ir.top + cs.bottomRightY * ir.height

        val darkOverlay = Color.Black.copy(alpha = 0.6f)

        // Dark overlay outside the crop region (within the image display rect)
        drawRect(
          darkOverlay,
          topLeft = Offset(ir.left, ir.top),
          size = Size(ir.width, cropTop - ir.top),
        )
        drawRect(
          darkOverlay,
          topLeft = Offset(ir.left, cropBottom),
          size = Size(ir.width, ir.bottom - cropBottom),
        )
        drawRect(
          darkOverlay,
          topLeft = Offset(ir.left, cropTop),
          size = Size(cropLeft - ir.left, cropBottom - cropTop),
        )
        drawRect(
          darkOverlay,
          topLeft = Offset(cropRight, cropTop),
          size = Size(ir.right - cropRight, cropBottom - cropTop),
        )

        // White crop border
        drawRect(
          color = Color.White,
          topLeft = Offset(cropLeft, cropTop),
          size = Size(cropRight - cropLeft, cropBottom - cropTop),
          style = Stroke(width = 2.dp.toPx()),
        )

        // Outlined box handles at all 4 corners
        val handleSize = Ui.unit.toPx()
        val half = handleSize / 2f
        listOf(
            Offset(cropLeft, cropTop),
            Offset(cropRight, cropTop),
            Offset(cropLeft, cropBottom),
            Offset(cropRight, cropBottom),
          )
          .forEach { corner ->
            drawRect(
              Color.White,
              topLeft = Offset(corner.x - half, corner.y - half),
              size = Size(handleSize, handleSize),
              style = Stroke(width = 1.dp.toPx()),
            )
          }
      }
      CtaButtons(
        secondaryButtonConfig = ScreenButtonConfig(text = "Cancel", onClick = onCancel),
        primaryButtonConfig =
          ScreenButtonConfig(
            text = "Done",
            onClick = {
              scope.launch {
                val cs = cropState.value
                val croppedBytes =
                  cropImage(
                    imageBytes,
                    cs.topLeftX,
                    cs.topLeftY,
                    cs.bottomRightX - cs.topLeftX,
                    cs.bottomRightY - cs.topLeftY,
                  )
                if (croppedBytes != null) {
                  onDone(croppedBytes)
                } else {
                  snackbarHostState.showSnackbar("Failed to crop image. Please try again.")
                }
              }
            },
          ),
      )
    }
    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.BottomCenter),
    )
  }
}
