package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

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

/** Returns a human-readable local date/time string, e.g. "Monday 3/2/2026 2:45 PM" */
fun Instant.formattedString(): String {
  val localDt = toLocalDateTime(TimeZone.currentSystemDefault())
  return "${localDt.date.formattedString()} ${localDt.time.formattedString()}"
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
