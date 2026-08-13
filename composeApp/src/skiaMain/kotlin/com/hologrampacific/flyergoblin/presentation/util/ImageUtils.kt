package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.hologrampacific.flyergoblin.util.AppLogger
import com.hologrampacific.flyergoblin.util.ImageBytes
import kotlin.math.sqrt
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

// JPEG quantization degrades sharp edges first, which is exactly where flyer text lives, and
// lowering quality doesn't shrink Gemini's processing time the way fewer pixels does. So we hold
// quality fixed and let dimensions absorb the size budget instead.
private const val JPEG_QUALITY = 90

// Floor on the longest side so the size budget can't scale text down to illegibility. If this
// floor is hit, the result may exceed maxSizeBytes.
private const val MIN_LONGEST_SIDE_PX = 640

actual fun decodeImageBitmap(imageBytes: ImageBytes): ImageBitmap? {
  return try {
    Image.makeFromEncoded(imageBytes.bytes).toComposeImageBitmap()
  } catch (e: Exception) {
    null
  }
}

actual fun cropImage(
  imageBytes: ImageBytes,
  x: Float,
  y: Float,
  width: Float,
  height: Float,
): ImageBytes? {
  return try {
    val image = Image.makeFromEncoded(imageBytes.bytes)
    val imgWidth = image.width
    val imgHeight = image.height

    val cropX = (x * imgWidth).toInt().coerceIn(0, imgWidth - 1)
    val cropY = (y * imgHeight).toInt().coerceIn(0, imgHeight - 1)
    val cropWidth = (width * imgWidth).toInt().coerceIn(1, imgWidth - cropX)
    val cropHeight = (height * imgHeight).toInt().coerceIn(1, imgHeight - cropY)

    val surface = org.jetbrains.skia.Surface.makeRasterN32Premul(cropWidth, cropHeight)
    val srcRect =
      org.jetbrains.skia.Rect.makeLTRB(
        cropX.toFloat(),
        cropY.toFloat(),
        (cropX + cropWidth).toFloat(),
        (cropY + cropHeight).toFloat(),
      )
    val dstRect = org.jetbrains.skia.Rect.makeWH(cropWidth.toFloat(), cropHeight.toFloat())
    surface.canvas.drawImageRect(image, srcRect, dstRect)
    surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, 90)?.bytes?.let {
      ImageBytes(it)
    }
  } catch (e: Exception) {
    AppLogger.e("ImageUtils", "Error cropping image", e)
    null
  }
}

actual fun reencodeImageToFitSize(imageBytes: ImageBytes, maxSizeBytes: Int): ImageBytes? {
  return try {
    // Decode the original image
    var image = Image.makeFromEncoded(imageBytes.bytes)

    var result = image.encodeToData(EncodedImageFormat.JPEG, JPEG_QUALITY)?.bytes ?: return null

    // If still too large, scale dimensions down toward the target size, without going below the
    // legible floor
    var wasResized = false
    if (result.size > maxSizeBytes) {
      val longestSide = maxOf(image.width, image.height)
      val scaleFactor =
        sqrt(maxSizeBytes.toDouble() / result.size.toDouble())
          .coerceAtLeast(MIN_LONGEST_SIDE_PX.toDouble() / longestSide)

      if (scaleFactor < 1.0) {
        wasResized = true
        val newWidth = (image.width * scaleFactor).toInt().coerceAtLeast(1)
        val newHeight = (image.height * scaleFactor).toInt().coerceAtLeast(1)

        // Create a scaled image
        val surface = org.jetbrains.skia.Surface.makeRasterN32Premul(newWidth, newHeight)
        val canvas = surface.canvas
        val srcRect = org.jetbrains.skia.Rect.makeWH(image.width.toFloat(), image.height.toFloat())
        val dstRect = org.jetbrains.skia.Rect.makeWH(newWidth.toFloat(), newHeight.toFloat())
        canvas.drawImageRect(image, srcRect, dstRect)

        image = surface.makeImageSnapshot()

        // Encode the resized image
        result = image.encodeToData(EncodedImageFormat.JPEG, JPEG_QUALITY)?.bytes ?: return null
      }
    }

    AppLogger.i(
      "ImageUtils",
      "reencodeImageToFitSize: quality=$JPEG_QUALITY, resized=$wasResized, " +
        "finalDimensions=${image.width}x${image.height}, finalSize=${result.size}B",
    )

    ImageBytes(result)
  } catch (e: Exception) {
    null
  }
}
