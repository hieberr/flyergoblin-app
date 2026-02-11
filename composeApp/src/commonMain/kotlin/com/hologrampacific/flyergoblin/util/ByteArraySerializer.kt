package com.hologrampacific.flyergoblin.util

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Custom serializer for ByteArray that encodes to/from Base64 strings. Used for persisting binary
 * data (like images) in serialized formats.
 */
object ByteArraySerializer : KSerializer<ByteArray> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("ByteArray", PrimitiveKind.STRING)

  @OptIn(ExperimentalEncodingApi::class)
  override fun serialize(encoder: Encoder, value: ByteArray) {
    encoder.encodeString(Base64.Default.encode(value))
  }

  @OptIn(ExperimentalEncodingApi::class)
  override fun deserialize(decoder: Decoder): ByteArray {
    return Base64.Default.decode(decoder.decodeString())
  }
}
