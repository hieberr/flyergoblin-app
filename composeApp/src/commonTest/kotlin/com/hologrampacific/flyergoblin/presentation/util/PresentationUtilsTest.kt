package com.hologrampacific.flyergoblin.presentation.util

import com.hologrampacific.flyergoblin.AppTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class PresentationUtilsTest : AppTest() {

  // LocalDate.formattedString()

  @Test
  fun `LocalDate formattedString includes full day of week name`() {
    // 2024-07-15 is a Monday
    assertEquals("Monday 7/15/2024", LocalDate(2024, 7, 15).formattedString())
  }

  @Test
  fun `LocalDate formattedString has no leading zero on month`() {
    // 2024-01-01 is a Monday
    assertEquals("Monday 1/1/2024", LocalDate(2024, 1, 1).formattedString())
  }

  @Test
  fun `LocalDate formattedString has no leading zero on day`() {
    // 2024-07-05 is a Friday
    assertEquals("Friday 7/5/2024", LocalDate(2024, 7, 5).formattedString())
  }

  @Test
  fun `LocalDate formattedString produces correct day of week for Saturday`() {
    // 2024-07-20 is a Saturday
    assertEquals("Saturday 7/20/2024", LocalDate(2024, 7, 20).formattedString())
  }

  // LocalTime.formattedString()

  @Test
  fun `LocalTime formattedString formats morning time as AM`() {
    assertEquals("9:30 AM", LocalTime(9, 30).formattedString())
  }

  @Test
  fun `LocalTime formattedString formats afternoon time as PM`() {
    assertEquals("9:00 PM", LocalTime(21, 0).formattedString())
  }

  @Test
  fun `LocalTime formattedString formats noon as 12 PM`() {
    assertEquals("12:00 PM", LocalTime(12, 0).formattedString())
  }

  @Test
  fun `LocalTime formattedString formats midnight as 12 AM`() {
    assertEquals("12:00 AM", LocalTime(0, 0).formattedString())
  }

  @Test
  fun `LocalTime formattedString pads minutes with leading zero`() {
    assertEquals("2:05 PM", LocalTime(14, 5).formattedString())
  }

  @Test
  fun `LocalTime formattedString has no leading zero on hour`() {
    assertEquals("8:00 AM", LocalTime(8, 0).formattedString())
  }
}
