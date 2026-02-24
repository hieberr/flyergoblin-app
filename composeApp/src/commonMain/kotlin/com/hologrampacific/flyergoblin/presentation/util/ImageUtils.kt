package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes a byte array into an ImageBitmap for display in Compose UI.
 *
 * @param bytes The image data as a byte array
 * @return The decoded ImageBitmap, or null if decoding fails
 */
expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?

/**
 * Validates that a byte array represents a valid image file by checking magic numbers/signatures.
 * Supports JPEG, PNG, GIF, BMP, and WebP formats.
 *
 * @param bytes The byte array to validate
 * @return true if the bytes appear to be a valid image format, false otherwise
 */
fun isValidImage(bytes: ByteArray): Boolean {
  if (bytes.isEmpty()) return false

  // Check for common image format signatures
  return when {
    // JPEG: FF D8 FF
    bytes.size >= 3 &&
      bytes[0] == 0xFF.toByte() &&
      bytes[1] == 0xD8.toByte() &&
      bytes[2] == 0xFF.toByte() -> true

    // PNG: 89 50 4E 47 0D 0A 1A 0A
    bytes.size >= 8 &&
      bytes[0] == 0x89.toByte() &&
      bytes[1] == 0x50.toByte() &&
      bytes[2] == 0x4E.toByte() &&
      bytes[3] == 0x47.toByte() &&
      bytes[4] == 0x0D.toByte() &&
      bytes[5] == 0x0A.toByte() &&
      bytes[6] == 0x1A.toByte() &&
      bytes[7] == 0x0A.toByte() -> true

    // GIF: 47 49 46 38 (GIF8)
    bytes.size >= 4 &&
      bytes[0] == 0x47.toByte() &&
      bytes[1] == 0x49.toByte() &&
      bytes[2] == 0x46.toByte() &&
      bytes[3] == 0x38.toByte() -> true

    // BMP: 42 4D
    bytes.size >= 2 && bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte() -> true

    // WebP: 52 49 46 46 ... 57 45 42 50
    bytes.size >= 12 &&
      bytes[0] == 0x52.toByte() &&
      bytes[1] == 0x49.toByte() &&
      bytes[2] == 0x46.toByte() &&
      bytes[3] == 0x46.toByte() &&
      bytes[8] == 0x57.toByte() &&
      bytes[9] == 0x45.toByte() &&
      bytes[10] == 0x42.toByte() &&
      bytes[11] == 0x50.toByte() -> true

    else -> false
  }
}

/**
 * Processes an image to ensure it's in JPEG format and under the maximum size. If the image is
 * larger than maxSizeBytes, it will be resized while maintaining aspect ratio.
 *
 * @param bytes The original image bytes
 * @param maxSizeBytes Maximum allowed size in bytes (default 100Kb)
 * @return Processed image as JPEG bytes, or null if processing fails
 */
expect fun reencodeImageToFitSize(bytes: ByteArray, maxSizeBytes: Int = 100 * 1024): ByteArray?
