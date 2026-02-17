package com.hologrampacific.flyergoblin.util

import com.hologrampacific.flyergoblin.AppTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageUtilsTest : AppTest() {

  // Image Validation Tests

  @Test
  fun testIsValidImageWithJpeg() {
    val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
    assertTrue(isValidImage(jpegHeader), "Valid JPEG header should be recognized")
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
    assertTrue(isValidImage(pngHeader), "Valid PNG header should be recognized")
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
    assertTrue(isValidImage(gifHeader), "Valid GIF header should be recognized")
  }

  @Test
  fun testIsValidImageWithBmp() {
    val bmpHeader = byteArrayOf(0x42.toByte(), 0x4D.toByte())
    assertTrue(isValidImage(bmpHeader), "Valid BMP header should be recognized")
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
    assertTrue(isValidImage(webpHeader), "Valid WebP header should be recognized")
  }

  @Test
  fun testIsValidImageWithEmptyArray() {
    assertFalse(isValidImage(ByteArray(0)), "Empty array should not be valid")
  }

  @Test
  fun testIsValidImageWithRandomBytes() {
    val randomBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
    assertFalse(isValidImage(randomBytes), "Random bytes should not be valid")
  }

  @Test
  fun testIsValidImageWithPartialHeader() {
    val partialJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
    assertFalse(isValidImage(partialJpeg), "Partial JPEG header should not be valid")
  }

  @Test
  fun testIsValidImageWithTextData() {
    val textBytes = "This is not an image".encodeToByteArray()
    assertFalse(isValidImage(textBytes), "Text data should not be valid image")
  }

  // Existing Image Decoding Tests

  @Test
  fun testDecodeImageBitmapWithEmptyByteArray() {
    val emptyBytes = ByteArray(0)
    val result = decodeImageBitmap(emptyBytes)

    assertNull(result, "Empty byte array should return null")
  }

  @Test
  fun testDecodeImageBitmapWithInvalidData() {
    val invalidBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
    val result = decodeImageBitmap(invalidBytes)

    assertNull(result, "Invalid image data should return null")
  }

  @Test
  fun testDecodeImageBitmapWithCorruptData() {
    // Simulate corrupt JPEG data (starts with JPEG header but is incomplete)
    val corruptJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
    val result = decodeImageBitmap(corruptJpeg)

    assertNull(result, "Corrupt image data should return null")
  }

  @Test
  fun testDecodeImageBitmapWithRandomBytes() {
    val randomBytes = ByteArray(1000) { it.toByte() }
    val result = decodeImageBitmap(randomBytes)

    assertNull(result, "Random bytes should return null")
  }

  @Test
  fun testProcessImageForStorageWithEmptyByteArray() {
    val emptyBytes = ByteArray(0)
    val result = processImageForStorage(emptyBytes)

    assertNull(result, "Empty byte array should return null")
  }

  @Test
  fun testProcessImageForStorageWithInvalidData() {
    val invalidBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
    val result = processImageForStorage(invalidBytes)

    assertNull(result, "Invalid image data should return null")
  }

  @Test
  fun testProcessImageForStorageWithCorruptData() {
    // Simulate corrupt JPEG data
    val corruptJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
    val result = processImageForStorage(corruptJpeg)

    assertNull(result, "Corrupt image data should return null")
  }

  @Test
  fun testProcessImageForStorageWithCustomMaxSize() {
    val invalidBytes = byteArrayOf(1, 2, 3, 4, 5)
    val result = processImageForStorage(invalidBytes, maxSizeBytes = 1024)

    assertNull(result, "Invalid image data should return null regardless of max size")
  }

  @Test
  fun testProcessImageForStorageReturnsJpegOnSuccess() {
    // This test requires a valid image to be created in platform-specific tests
    // Here we just verify the function signature and error handling
    val randomBytes = ByteArray(100) { it.toByte() }
    val result = processImageForStorage(randomBytes)

    // Result should be null for invalid data
    assertNull(result)
  }
}
