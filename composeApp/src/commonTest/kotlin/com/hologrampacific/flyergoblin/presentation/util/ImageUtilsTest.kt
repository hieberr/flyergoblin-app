package com.hologrampacific.flyergoblin.presentation.util

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.util.ImageBytes
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageUtilsTest : AppTest() {

  // Image Validation Tests

  @Test
  fun testIsValidImageWithJpeg() {
    val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
    assertTrue(ImageBytes(jpegHeader).isValidImage, "Valid JPEG header should be recognized")
  }

  @Test
  fun testIsValidImageWithPng() {
    val pngHeader =
      byteArrayOf(
        0x89.toByte(),
        0x50.toByte(),
        0x4E.toByte(),
        0x47.toByte(),
        0x0D.toByte(),
        0x0A.toByte(),
        0x1A.toByte(),
        0x0A.toByte(),
      )
    assertTrue(ImageBytes(pngHeader).isValidImage, "Valid PNG header should be recognized")
  }

  @Test
  fun testIsValidImageWithGif() {
    val gifHeader =
      byteArrayOf(
        0x47.toByte(),
        0x49.toByte(),
        0x46.toByte(),
        0x38.toByte(),
        0x39.toByte(),
        0x61.toByte(),
      )
    assertTrue(ImageBytes(gifHeader).isValidImage, "Valid GIF header should be recognized")
  }

  @Test
  fun testIsValidImageWithBmp() {
    val bmpHeader = byteArrayOf(0x42.toByte(), 0x4D.toByte())
    assertTrue(ImageBytes(bmpHeader).isValidImage, "Valid BMP header should be recognized")
  }

  @Test
  fun testIsValidImageWithWebP() {
    val webpHeader =
      byteArrayOf(
        0x52.toByte(),
        0x49.toByte(),
        0x46.toByte(),
        0x46.toByte(), // RIFF
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(), // Size placeholder
        0x57.toByte(),
        0x45.toByte(),
        0x42.toByte(),
        0x50.toByte(), // WEBP
      )
    assertTrue(ImageBytes(webpHeader).isValidImage, "Valid WebP header should be recognized")
  }

  @Test
  fun testIsValidImageWithEmptyArray() {
    assertFalse(ImageBytes(ByteArray(0)).isValidImage, "Empty array should not be valid")
  }

  @Test
  fun testIsValidImageWithRandomBytes() {
    val randomBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
    assertFalse(ImageBytes(randomBytes).isValidImage, "Random bytes should not be valid")
  }

  @Test
  fun testIsValidImageWithPartialHeader() {
    val partialJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
    assertFalse(ImageBytes(partialJpeg).isValidImage, "Partial JPEG header should not be valid")
  }

  @Test
  fun testIsValidImageWithTextData() {
    val textBytes = "This is not an image".encodeToByteArray()
    assertFalse(ImageBytes(textBytes).isValidImage, "Text data should not be valid image")
  }

  // Existing Image Decoding Tests

  @Test
  fun testDecodeImageBitmapWithEmptyByteArray() {
    val emptyBytes = ImageBytes(ByteArray(0))
    val result = decodeImageBitmap(emptyBytes)

    assertNull(result, "Empty byte array should return null")
  }

  @Test
  fun testDecodeImageBitmapWithInvalidData() {
    val invalidBytes = ImageBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
    val result = decodeImageBitmap(invalidBytes)

    assertNull(result, "Invalid image data should return null")
  }

  @Test
  fun testDecodeImageBitmapWithCorruptData() {
    // Simulate corrupt JPEG data (starts with JPEG header but is incomplete)
    val corruptJpeg =
      ImageBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
    val result = decodeImageBitmap(corruptJpeg)

    assertNull(result, "Corrupt image data should return null")
  }

  @Test
  fun testDecodeImageBitmapWithRandomBytes() {
    val randomBytes = ImageBytes(ByteArray(1000) { it.toByte() })
    val result = decodeImageBitmap(randomBytes)

    assertNull(result, "Random bytes should return null")
  }

  @Test
  fun testProcessImageForStorageWithEmptyByteArray() {
    val emptyBytes = ImageBytes(ByteArray(0))
    val result = reencodeImageToFitSize(emptyBytes)

    assertNull(result, "Empty byte array should return null")
  }

  @Test
  fun testProcessImageForStorageWithInvalidData() {
    val invalidBytes = ImageBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
    val result = reencodeImageToFitSize(invalidBytes)

    assertNull(result, "Invalid image data should return null")
  }

  @Test
  fun testProcessImageForStorageWithCorruptData() {
    // Simulate corrupt JPEG data
    val corruptJpeg =
      ImageBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
    val result = reencodeImageToFitSize(corruptJpeg)

    assertNull(result, "Corrupt image data should return null")
  }

  @Test
  fun testProcessImageForStorageWithCustomTargetSize() {
    val invalidBytes = ImageBytes(byteArrayOf(1, 2, 3, 4, 5))
    val result = reencodeImageToFitSize(invalidBytes, targetSizeBytes = 1024)

    assertNull(result, "Invalid image data should return null regardless of target size")
  }

  @Test
  fun testProcessImageForStorageReturnsJpegOnSuccess() {
    // This test requires a valid image to be created in platform-specific tests
    // Here we just verify the function signature and error handling
    val randomBytes = ImageBytes(ByteArray(100) { it.toByte() })
    val result = reencodeImageToFitSize(randomBytes)

    // Result should be null for invalid data
    assertNull(result)
  }

  // cropImage Tests

  @Test
  fun testCropImageWithEmptyByteArray() {
    val result = cropImage(ImageBytes(ByteArray(0)), 0f, 0f, 1f, 1f)
    assertNull(result, "Empty byte array should return null")
  }

  @Test
  fun testCropImageWithInvalidData() {
    val result = cropImage(ImageBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)), 0f, 0f, 1f, 1f)
    assertNull(result, "Invalid image data should return null")
  }

  @Test
  fun testCropImageWithCorruptJpeg() {
    val corruptJpeg =
      ImageBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
    val result = cropImage(corruptJpeg, 0f, 0f, 1f, 1f)
    assertNull(result, "Corrupt JPEG data should return null")
  }

  @Test
  fun testCropImageWithRandomBytesAndPartialRegion() {
    val randomBytes = ImageBytes(ByteArray(1000) { it.toByte() })
    val result = cropImage(randomBytes, 0.25f, 0.25f, 0.5f, 0.5f)
    assertNull(result, "Random bytes should return null regardless of crop region")
  }
}
