package com.hologrampacific.flyergoblin.flyer.domain.model

import com.hologrampacific.flyergoblin.util.ByteArraySerializer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class ByteArraySerializerTest {

  private val json = Json

  @Test
  fun testSerializeEmptyByteArray() {
    val emptyArray = ByteArray(0)
    val serialized = json.encodeToString(ByteArraySerializer, emptyArray)
    val deserialized = json.decodeFromString(ByteArraySerializer, serialized)

    assertContentEquals(emptyArray, deserialized)
  }

  @Test
  fun testSerializeSimpleByteArray() {
    val testArray = byteArrayOf(1, 2, 3, 4, 5)
    val serialized = json.encodeToString(ByteArraySerializer, testArray)
    val deserialized = json.decodeFromString(ByteArraySerializer, serialized)

    assertContentEquals(testArray, deserialized)
  }

  @Test
  fun testSerializeLargeByteArray() {
    val largeArray = ByteArray(1000) { it.toByte() }
    val serialized = json.encodeToString(ByteArraySerializer, largeArray)
    val deserialized = json.decodeFromString(ByteArraySerializer, serialized)

    assertContentEquals(largeArray, deserialized)
  }

  @Test
  fun testSerializeAllByteValues() {
    // Test all possible byte values from -128 to 127
    val allBytes = ByteArray(256) { (it - 128).toByte() }
    val serialized = json.encodeToString(ByteArraySerializer, allBytes)
    val deserialized = json.decodeFromString(ByteArraySerializer, serialized)

    assertContentEquals(allBytes, deserialized)
  }

  @Test
  fun testRoundTripSerialization() {
    val original = byteArrayOf(0x48, 0x65, 0x6C, 0x6C, 0x6F) // "Hello" in ASCII
    val serialized = json.encodeToString(ByteArraySerializer, original)
    val deserialized = json.decodeFromString(ByteArraySerializer, serialized)
    val reSerialized = json.encodeToString(ByteArraySerializer, deserialized)

    assertContentEquals(original, deserialized)
    assertEquals(serialized, reSerialized)
  }
}
