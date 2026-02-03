package com.hologrampacific.learnkmp.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.math.sqrt

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
  return try {
    Image.makeFromEncoded(bytes).toComposeImageBitmap()
  } catch (e: Exception) {
    null
  }
}

actual fun processImageForStorage(bytes: ByteArray, maxSizeBytes: Int): ByteArray? {
  return try {
    // Decode the original image
    var image = Image.makeFromEncoded(bytes) ?: return null

    // Try encoding with high quality first
    var quality = 90
    var result = image.encodeToData(EncodedImageFormat.JPEG, quality)?.bytes ?: return null

    // If still too large, gradually reduce quality
    while (result.size > maxSizeBytes && quality > 50) {
      quality -= 10
      result = image.encodeToData(EncodedImageFormat.JPEG, quality)?.bytes ?: return null
    }

    // If still too large after reducing quality, resize the image
    if (result.size > maxSizeBytes) {
      val scaleFactor = sqrt(maxSizeBytes.toDouble() / result.size.toDouble())
      val newWidth = (image.width * scaleFactor).toInt()
      val newHeight = (image.height * scaleFactor).toInt()

      // Create a bitmap with the new dimensions
      val bitmap = Bitmap()
      bitmap.allocPixels(ImageInfo.makeS32(newWidth, newHeight, ColorAlphaType.PREMUL))

      // Create a scaled image
      val surface = org.jetbrains.skia.Surface.makeRasterN32Premul(newWidth, newHeight)
      val canvas = surface.canvas
      val srcRect = org.jetbrains.skia.Rect.makeWH(image.width.toFloat(), image.height.toFloat())
      val dstRect = org.jetbrains.skia.Rect.makeWH(newWidth.toFloat(), newHeight.toFloat())
      canvas.drawImageRect(image, srcRect, dstRect)

      image = surface.makeImageSnapshot()

      // Encode the resized image
      result = image.encodeToData(EncodedImageFormat.JPEG, quality)?.bytes ?: return null
    }

    result
  } catch (e: Exception) {
    null
  }
}
