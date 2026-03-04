package com.hologrampacific.flyergoblin.flyer.presentation.artist.components

import com.hologrampacific.flyergoblin.AppTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MixcloudWidgetUrlTest : AppTest() {

  @Test
  fun `test buildMixcloudWidgetUrl constructs correct URL for show key`() {
    val url = buildMixcloudWidgetUrl("/artistname/show-title/")
    assertEquals(
      "https://www.mixcloud.com/widget/iframe/?feed=%2Fartistname%2Fshow%2Dtitle%2F&hide_cover=1",
      url,
    )
  }

  @Test
  fun `test buildMixcloudWidgetUrl encodes ampersand in show key`() {
    val url = buildMixcloudWidgetUrl("/artist/show&more/")
    assertTrue(url.contains("%2Fartist%2Fshow%26more%2F"))
  }
}
