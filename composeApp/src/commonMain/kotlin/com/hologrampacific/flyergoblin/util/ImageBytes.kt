package com.hologrampacific.flyergoblin.util

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class ImageBytes(val bytes: ByteArray) {
  override fun equals(other: Any?): Boolean =
    other is ImageBytes && bytes.contentEquals(other.bytes)

  override fun hashCode(): Int = bytes.contentHashCode()

  /** Returns true if the bytes appear to be a valid image (JPEG, PNG, GIF, BMP, or WebP). */
  val isValidImage: Boolean
    get() {
      if (bytes.isEmpty()) return false
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
}

/** Serializes [ImageBytes] as a Base64 string. */
object ImageBytesSerializer : KSerializer<ImageBytes> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("ImageBytes", PrimitiveKind.STRING)

  @OptIn(ExperimentalEncodingApi::class)
  override fun serialize(encoder: Encoder, value: ImageBytes) {
    encoder.encodeString(Base64.Default.encode(value.bytes))
  }

  @OptIn(ExperimentalEncodingApi::class)
  override fun deserialize(decoder: Decoder): ImageBytes {
    return ImageBytes(Base64.Default.decode(decoder.decodeString()))
  }
}
