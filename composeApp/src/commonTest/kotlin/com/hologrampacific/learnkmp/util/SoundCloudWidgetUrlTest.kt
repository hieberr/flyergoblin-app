package com.hologrampacific.learnkmp.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SoundCloudWidgetUrlTest {

  @Test
  fun `buildSoundCloudWidgetUrl encodes track URL correctly`() {
    val trackUrl = "https://soundcloud.com/artist/track-name"
    val widgetUrl = buildSoundCloudWidgetUrl(trackUrl)

    assertTrue(widgetUrl.startsWith("https://w.soundcloud.com/player/?url="))
    assertTrue(widgetUrl.contains("https%3A%2F%2Fsoundcloud.com%2Fartist%2Ftrack-name"))
  }

  @Test
  fun `buildSoundCloudWidgetUrl uses default color`() {
    val trackUrl = "https://soundcloud.com/artist/track"
    val widgetUrl = buildSoundCloudWidgetUrl(trackUrl)

    assertTrue(widgetUrl.contains("&color=%23ff5500"))
  }

  @Test
  fun `buildSoundCloudWidgetUrl uses custom color`() {
    val trackUrl = "https://soundcloud.com/artist/track"
    val widgetUrl = buildSoundCloudWidgetUrl(trackUrl, color = "00ff00")

    assertTrue(widgetUrl.contains("&color=%2300ff00"))
  }

  @Test
  fun `buildSoundCloudWidgetUrl includes required widget parameters`() {
    val trackUrl = "https://soundcloud.com/artist/track"
    val widgetUrl = buildSoundCloudWidgetUrl(trackUrl)

    assertTrue(widgetUrl.contains("&auto_play=false"))
    assertTrue(widgetUrl.contains("&hide_related=true"))
    assertTrue(widgetUrl.contains("&show_comments=false"))
    assertTrue(widgetUrl.contains("&show_user=true"))
    assertTrue(widgetUrl.contains("&show_reposts=false"))
    assertTrue(widgetUrl.contains("&show_teaser=false"))
    assertTrue(widgetUrl.contains("&visual=true"))
    assertTrue(widgetUrl.contains("&single_active=true"))
    assertTrue(widgetUrl.contains("&download=false"))
    assertTrue(widgetUrl.contains("&buying=false"))
    assertTrue(widgetUrl.contains("&sharing=false"))
  }

  @Test
  fun `buildSoundCloudWidgetUrl handles track URL with special characters`() {
    val trackUrl = "https://soundcloud.com/artist/track with spaces & symbols"
    val widgetUrl = buildSoundCloudWidgetUrl(trackUrl)

    assertFalse(widgetUrl.contains(" "))
    assertTrue(widgetUrl.contains("%20"))
    assertTrue(widgetUrl.contains("%26"))
  }
}
