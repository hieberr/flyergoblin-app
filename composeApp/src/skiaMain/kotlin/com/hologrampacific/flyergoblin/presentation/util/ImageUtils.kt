package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.hologrampacific.flyergoblin.util.AppLogger
import com.hologrampacific.flyergoblin.util.ImageBytes
import kotlin.math.sqrt
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

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

    ImageBytes(result)
  } catch (e: Exception) {
    null
  }
}
