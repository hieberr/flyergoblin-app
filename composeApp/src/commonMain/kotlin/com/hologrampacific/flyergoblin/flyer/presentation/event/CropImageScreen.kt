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
 * Returns the crop selection rect in canvas coordinates from [ir] (image display rect) and [cs].
 */
private fun cropRect(ir: Rect, cs: CropState) =
  Rect(
    left = ir.left + cs.topLeftX * ir.width,
    top = ir.top + cs.topLeftY * ir.height,
    right = ir.left + cs.bottomRightX * ir.width,
    bottom = ir.top + cs.bottomRightY * ir.height,
  )

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
              val cr = cropRect(ir, cropState.value)
              listOf(
                  Offset(cr.left, cr.top),
                  Offset(cr.right, cr.top),
                  Offset(cr.left, cr.bottom),
                  Offset(cr.right, cr.bottom),
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
                  val cr = cropRect(ir, cropState.value)

                  fun dist(ax: Float, ay: Float): Float {
                    val dx = offset.x - ax
                    val dy = offset.y - ay
                    return sqrt(dx * dx + dy * dy)
                  }

                  activeHandle.value =
                    when {
                      dist(cr.left, cr.top) <= touchRadiusPx -> Handle.TopLeft
                      dist(cr.right, cr.top) <= touchRadiusPx -> Handle.TopRight
                      dist(cr.left, cr.bottom) <= touchRadiusPx -> Handle.BottomLeft
                      dist(cr.right, cr.bottom) <= touchRadiusPx -> Handle.BottomRight
                      else -> Handle.None
                    }
                },
                onDrag = { change, dragAmount ->
                  change.consume()
                  if (activeHandle.value != Handle.None) {
                    val cs = cropState.value
                    val imageRect = dragImageRect
                    val dx = dragAmount.x / imageRect.width
                    val dy = dragAmount.y / imageRect.height
                    cropState.value =
                      when (activeHandle.value) {
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
        val displayRect =
          imageDisplayRect(size.width, size.height, imageBitmap.width, imageBitmap.height, hPadPx)

        // Draw image scaled to fit
        drawImage(
          image = imageBitmap,
          dstOffset = IntOffset(displayRect.left.toInt(), displayRect.top.toInt()),
          dstSize = IntSize(displayRect.width.toInt(), displayRect.height.toInt()),
        )

        // Crop rect in canvas coordinates
        val cropRect = cropRect(displayRect, cropState.value)

        val darkOverlay = Color.Black.copy(alpha = 0.6f)

        // Dark overlay outside the crop region (within the image display rect)
        drawRect(
          darkOverlay,
          topLeft = Offset(displayRect.left, displayRect.top),
          size = Size(displayRect.width, cropRect.top - displayRect.top),
        )
        drawRect(
          darkOverlay,
          topLeft = Offset(displayRect.left, cropRect.bottom),
          size = Size(displayRect.width, displayRect.bottom - cropRect.bottom),
        )
        drawRect(
          darkOverlay,
          topLeft = Offset(displayRect.left, cropRect.top),
          size = Size(cropRect.left - displayRect.left, cropRect.height),
        )
        drawRect(
          darkOverlay,
          topLeft = Offset(cropRect.right, cropRect.top),
          size = Size(displayRect.right - cropRect.right, cropRect.height),
        )

        // White crop border
        drawRect(
          color = Color.White,
          topLeft = Offset(cropRect.left, cropRect.top),
          size = Size(cropRect.width, cropRect.height),
          style = Stroke(width = 2.dp.toPx()),
        )

        // Outlined box handles at all 4 corners
        val handleSize = Ui.unit.toPx()
        val half = handleSize / 2f
        listOf(
            Offset(cropRect.left, cropRect.top),
            Offset(cropRect.right, cropRect.top),
            Offset(cropRect.left, cropRect.bottom),
            Offset(cropRect.right, cropRect.bottom),
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
    SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
  }
}
