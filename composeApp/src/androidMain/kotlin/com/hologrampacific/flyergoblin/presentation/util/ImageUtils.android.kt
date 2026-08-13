package com.hologrampacific.flyergoblin.presentation.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import com.hologrampacific.flyergoblin.util.AppLogger
import com.hologrampacific.flyergoblin.util.ImageBytes
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

// JPEG quantization degrades sharp edges first, which is exactly where flyer text lives, and
// lowering quality doesn't shrink Gemini's processing time the way fewer pixels does. So we hold
// quality fixed and let dimensions absorb the size budget instead.
private const val JPEG_QUALITY = 90

// Floor on the longest side so the size budget can't scale text down to illegibility. If this
// floor is hit, the result may exceed targetSizeBytes.
private const val MIN_LONGEST_SIDE_PX = 640

actual fun decodeImageBitmap(imageBytes: ImageBytes): ImageBitmap? {
  return try {
    val bytes = imageBytes.bytes
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    bitmap?.asImageBitmap()
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
  var bitmap: Bitmap? = null
  var cropped: Bitmap? = null
  return try {
    val bytes = imageBytes.bytes
    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val imgWidth = bitmap.width
    val imgHeight = bitmap.height

    val cropX = (x * imgWidth).toInt().coerceIn(0, imgWidth - 1)
    val cropY = (y * imgHeight).toInt().coerceIn(0, imgHeight - 1)
    val cropWidth = (width * imgWidth).toInt().coerceIn(1, imgWidth - cropX)
    val cropHeight = (height * imgHeight).toInt().coerceIn(1, imgHeight - cropY)

    cropped = Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
    val outputStream = ByteArrayOutputStream()
    cropped.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
    ImageBytes(outputStream.toByteArray())
  } catch (e: Exception) {
    AppLogger.e("ImageUtils", "Error cropping image", e)
    null
  } finally {
    bitmap?.recycle()
    if (cropped != null && cropped != bitmap) cropped.recycle()
  }
}

actual fun reencodeImageToFitSize(imageBytes: ImageBytes, targetSizeBytes: Int): ImageBytes? {
  return try {
    // Decode the original bitmap
    val rawBytes = imageBytes.bytes
    var bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return null

    var outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
    var result = outputStream.toByteArray()

    // If still too large, scale dimensions down toward the target size, without going below the
    // legible floor
    var wasResized = false
    if (result.size > targetSizeBytes) {
      val longestSide = maxOf(bitmap.width, bitmap.height)
      val scaleFactor =
        sqrt(targetSizeBytes.toDouble() / result.size.toDouble())
          .coerceAtLeast(MIN_LONGEST_SIDE_PX.toDouble() / longestSide)

      if (scaleFactor < 1.0) {
        wasResized = true
        val newWidth = (bitmap.width * scaleFactor).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scaleFactor).toInt().coerceAtLeast(1)

        val resizedBitmap = bitmap.scale(newWidth, newHeight)
        if (resizedBitmap != bitmap) {
          bitmap.recycle()
          bitmap = resizedBitmap
        }

        // Compress the resized bitmap
        outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        result = outputStream.toByteArray()
      }
    }

    AppLogger.i(
      "ImageUtils",
      "reencodeImageToFitSize: quality=$JPEG_QUALITY, resized=$wasResized, " +
        "finalDimensions=${bitmap.width}x${bitmap.height}, finalSize=${result.size}B",
    )

    bitmap.recycle()
    ImageBytes(result)
  } catch (e: Exception) {
    null
  }
}
