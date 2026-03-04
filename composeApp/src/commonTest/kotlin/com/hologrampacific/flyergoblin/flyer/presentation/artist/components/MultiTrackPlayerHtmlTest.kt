package com.hologrampacific.flyergoblin.flyer.presentation.artist.components

import com.hologrampacific.flyergoblin.AppTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultiTrackPlayerHtmlTest : AppTest() {

  @Test
  fun `test buildMultiTrackPlayerHtml produces valid HTML structure`() {
    val html = buildMultiTrackPlayerHtml(
      trackUrls = listOf("https://example.com/track1"),
      trackHeight = 120,
      trackGap = 8,
      backgroundColor = "#000000",
    )

    assertTrue(html.startsWith("<!DOCTYPE html>"))
    assertContains(html, "<html>")
    assertContains(html, "<head>")
    assertContains(html, "</head>")
    assertContains(html, "<body>")
    assertContains(html, "</body>")
    assertTrue(html.endsWith("</html>"))
  }

  @Test
  fun `test buildMultiTrackPlayerHtml renders one iframe per track URL`() {
    val html = buildMultiTrackPlayerHtml(
      trackUrls = listOf("https://example.com/track1", "https://example.com/track2"),
      trackHeight = 120,
      trackGap = 8,
      backgroundColor = "#000000",
    )

    assertEquals(2, html.split("<iframe").size - 1)
    assertContains(html, "src=\"https://example.com/track1\"")
    assertContains(html, "src=\"https://example.com/track2\"")
  }

  @Test
  fun `test buildMultiTrackPlayerHtml renders no iframes for empty track list`() {
    val html = buildMultiTrackPlayerHtml(
      trackUrls = emptyList(),
      trackHeight = 120,
      trackGap = 8,
      backgroundColor = "#000000",
    )

    assertFalse(html.contains("<iframe"))
  }

  @Test
  fun `test buildMultiTrackPlayerHtml applies trackHeight to iframes`() {
    val html = buildMultiTrackPlayerHtml(
      trackUrls = listOf("https://example.com/track1"),
      trackHeight = 150,
      trackGap = 8,
      backgroundColor = "#000000",
    )

    assertContains(html, "height=\"150\"")
  }

  @Test
  fun `test buildMultiTrackPlayerHtml applies trackGap to iframe style`() {
    val html = buildMultiTrackPlayerHtml(
      trackUrls = listOf("https://example.com/track1"),
      trackHeight = 120,
      trackGap = 16,
      backgroundColor = "#000000",
    )

    assertContains(html, "margin-bottom: 16px")
  }

  @Test
  fun `test buildMultiTrackPlayerHtml applies backgroundColor to body style`() {
    val html = buildMultiTrackPlayerHtml(
      trackUrls = listOf("https://example.com/track1"),
      trackHeight = 120,
      trackGap = 8,
      backgroundColor = "#1A2B3C",
    )

    assertContains(html, "background-color: #1A2B3C")
  }

  @Test
  fun `test buildMultiTrackPlayerHtml injects headScript when provided`() {
    val script = "<script src=\"https://example.com/api.js\"></script>"
    val html = buildMultiTrackPlayerHtml(
      trackUrls = listOf("https://example.com/track1"),
      trackHeight = 120,
      trackGap = 8,
      backgroundColor = "#000000",
      headScript = script,
    )

    assertContains(html, script)
    // head script should appear before </head>
    assertTrue(html.indexOf(script) < html.indexOf("</head>"))
  }

  @Test
  fun `test buildMultiTrackPlayerHtml omits headScript when null`() {
    val html = buildMultiTrackPlayerHtml(
      trackUrls = listOf("https://example.com/track1"),
      trackHeight = 120,
      trackGap = 8,
      backgroundColor = "#000000",
      headScript = null,
    )

    assertFalse(html.contains("<script"))
  }

  @Test
  fun `test buildMultiTrackPlayerHtml injects bodyScript when provided`() {
    val script = "<script>console.log('loaded');</script>"
    val html = buildMultiTrackPlayerHtml(
      trackUrls = listOf("https://example.com/track1"),
      trackHeight = 120,
      trackGap = 8,
      backgroundColor = "#000000",
      bodyScript = script,
    )

    assertContains(html, script)
    // body script should appear before </body>
    assertTrue(html.indexOf(script) < html.indexOf("</body>"))
  }

  @Test
  fun `test buildMultiTrackPlayerHtml omits bodyScript when null`() {
    val html = buildMultiTrackPlayerHtml(
      trackUrls = listOf("https://example.com/track1"),
      trackHeight = 120,
      trackGap = 8,
      backgroundColor = "#000000",
      bodyScript = null,
    )

    assertFalse(html.contains("<script"))
  }

  @Test
  fun `test buildMultiTrackPlayerHtml iframes include autoplay allow attribute`() {
    val html = buildMultiTrackPlayerHtml(
      trackUrls = listOf("https://example.com/track1"),
      trackHeight = 120,
      trackGap = 8,
      backgroundColor = "#000000",
    )

    assertContains(html, "allow=\"autoplay\"")
  }

  @Test
  fun `test buildMultiTrackPlayerHtml includes viewport meta tag`() {
    val html = buildMultiTrackPlayerHtml(
      trackUrls = listOf("https://example.com/track1"),
      trackHeight = 120,
      trackGap = 8,
      backgroundColor = "#000000",
    )

    assertContains(html, "<meta name=\"viewport\"")
  }
}
