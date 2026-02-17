package com.hologrampacific.flyergoblin.flyer.domain.model

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.util.InstantSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.serialization.json.Json

class InstantSerializerTest : AppTest() {

  private val json = Json

  @Test
  fun testSerializeCurrentInstant() {
    val now = Instant.fromEpochMilliseconds(1706832000000) // Fixed timestamp for testing
    val serialized = json.encodeToString(InstantSerializer, now)
    val deserialized = json.decodeFromString(InstantSerializer, serialized)

    assertEquals(now, deserialized)
  }

  @Test
  fun testSerializeEpochInstant() {
    val epoch = Instant.fromEpochMilliseconds(0)
    val serialized = json.encodeToString(InstantSerializer, epoch)
    val deserialized = json.decodeFromString(InstantSerializer, serialized)

    assertEquals(epoch, deserialized)
  }

  @Test
  fun testSerializeFutureInstant() {
    val future = Instant.fromEpochMilliseconds(2000000000000) // Year 2033
    val serialized = json.encodeToString(InstantSerializer, future)
    val deserialized = json.decodeFromString(InstantSerializer, serialized)

    assertEquals(future, deserialized)
  }

  @Test
  fun testRoundTripSerialization() {
    val original = Instant.fromEpochMilliseconds(1706832000000)
    val serialized = json.encodeToString(InstantSerializer, original)
    val deserialized = json.decodeFromString(InstantSerializer, serialized)
    val reSerialized = json.encodeToString(InstantSerializer, deserialized)

    assertEquals(original, deserialized)
    assertEquals(serialized, reSerialized)
  }

  @Test
  fun testSerializeInstantWithNanoseconds() {
    // Test Instant with nanosecond precision
    val instantWithNanos = Instant.fromEpochSeconds(1706832000, 123456789)
    val serialized = json.encodeToString(InstantSerializer, instantWithNanos)
    val deserialized = json.decodeFromString(InstantSerializer, serialized)

    assertEquals(instantWithNanos, deserialized)
  }
}
