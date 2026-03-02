package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

/** Returns a string formatted as "DayOfWeek M/DD/YYYY" e.g. "Monday 7/15/2024" */
fun LocalDate.formattedString(): String {
  val dateFormat =
    LocalDate.Format {
      dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
      char(' ')
      monthNumber(padding = Padding.NONE)
      char('/')
      this@Format.day(padding = Padding.NONE)
      char('/')
      year()
    }
  return dateFormat.format(this)
}

/** Returns a string formatted as h:mm AM/PM */
fun LocalTime.formattedString(): String {
  val timeFormat =
    LocalTime.Format {
      amPmHour(padding = Padding.NONE)
      char(':')
      minute()
      char(' ')
      amPmMarker("AM", "PM")
    }
  return timeFormat.format(this)
}

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

/** Converts a color to a HTML hex color string eg: "#FFFFFF */
val Color.htmlHexString: String
  get() {
    val argb = toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${
      b.toString(16).padStart(2, '0')
    }"
  }
