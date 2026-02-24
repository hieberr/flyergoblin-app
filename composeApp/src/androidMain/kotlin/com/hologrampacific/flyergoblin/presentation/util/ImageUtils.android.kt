package com.hologrampacific.flyergoblin.presentation.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
  return try {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    bitmap?.asImageBitmap()
  } catch (e: Exception) {
    null
  }
}

actual fun reencodeImageToFitSize(bytes: ByteArray, maxSizeBytes: Int): ByteArray? {
  return try {
    // Decode the original bitmap
    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

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

      val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
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
    result
  } catch (e: Exception) {
    null
  }
}
