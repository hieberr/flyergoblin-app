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
 * Processes an image to ensure it's in JPEG format and under the maximum size. If the image is
 * larger than maxSizeBytes, it will be resized while maintaining aspect ratio.
 *
 * @param bytes The original image bytes
 * @param maxSizeBytes Maximum allowed size in bytes (default 100Kb)
 * @return Processed image as JPEG bytes, or null if processing fails
 */
expect fun reencodeImageToFitSize(bytes: ByteArray, maxSizeBytes: Int = 100 * 1024): ByteArray?

/**
 * Crops an image to the specified region defined by normalized ratios.
 *
 * @param bytes The original image bytes
 * @param x Left edge of the crop region as a ratio (0.0 to 1.0)
 * @param y Top edge of the crop region as a ratio (0.0 to 1.0)
 * @param width Width of the crop region as a ratio (0.0 to 1.0)
 * @param height Height of the crop region as a ratio (0.0 to 1.0)
 * @return Cropped image as JPEG bytes, or null if processing fails
 */
expect fun cropImage(bytes: ByteArray, x: Float, y: Float, width: Float, height: Float): ByteArray?
