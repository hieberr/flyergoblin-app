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

actual fun reencodeImageToFitSize(imageBytes: ImageBytes, maxSizeBytes: Int): ImageBytes? {
  return try {
    // Decode the original bitmap
    val rawBytes = imageBytes.bytes
    var bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return null

    // Try compressing with high quality first
    var quality = 90
    var outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    var result = outputStream.toByteArray()

    // If still too large, gradually reduce quality
    while (result.size > maxSizeBytes && quality > 50) {
      quality -= 10
      outputStream = ByteArrayOutputStream()
      bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
      result = outputStream.toByteArray()
    }

    // If still too large after reducing quality, resize the bitmap
    if (result.size > maxSizeBytes) {
      val scaleFactor = sqrt(maxSizeBytes.toDouble() / result.size.toDouble())
      val newWidth = (bitmap.width * scaleFactor).toInt()
      val newHeight = (bitmap.height * scaleFactor).toInt()

      val resizedBitmap = bitmap.scale(newWidth, newHeight)
      if (resizedBitmap != bitmap) {
        bitmap.recycle()
        bitmap = resizedBitmap
      }

      // Compress the resized bitmap
      outputStream = ByteArrayOutputStream()
      bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
      result = outputStream.toByteArray()
    }

    bitmap.recycle()
    ImageBytes(result)
  } catch (e: Exception) {
    null
  }
}
