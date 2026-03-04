package com.hologrampacific.flyergoblin.flyer.presentation.artist.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudShow
import com.hologrampacific.flyergoblin.presentation.util.htmlHexString
import io.ktor.http.*

/**
 * Height of the Mixcloud show player widgets in dp.
 *
 * NOTE: This value is dictated by the Mixcloud embed widget's rendered height and intentionally
 * does not conform to the `Ui.unit` convention (multiples of 16.dp). Do not change without
 * verifying the actual embed widget height in the app.
 */
const val MIXCLOUD_SHOW_HEIGHT = 120

/**
 * Gap between Mixcloud show player widgets in dp. Note: intentionally matches
 * [com.hologrampacific.flyergoblin.presentation.Ui.halfUnit] (8.dp). Update together if spacing
 * changes.
 */
const val MIXCLOUD_SHOW_GAP = 8

/**
 * Multi-show Mixcloud player that embeds all shows in a single WebView.
 *
 * @param shows List of Mixcloud shows to display
 * @param modifier Modifier for the composable
 */
@Composable
fun MixcloudMultiTrackPlayer(shows: List<MixcloudShow>, modifier: Modifier = Modifier) {
  val backgroundColor = MaterialTheme.colorScheme.background
  val html =
    remember(shows, backgroundColor) {
      buildMultiTrackPlayerHtml(
        trackUrls = shows.map { buildMixcloudWidgetUrl(it.key) },
        trackHeight = MIXCLOUD_SHOW_HEIGHT,
        trackGap = MIXCLOUD_SHOW_GAP,
        backgroundColor = backgroundColor.htmlHexString,
        headScript = MIXCLOUD_HEAD_SCRIPT,
        bodyScript = MIXCLOUD_BODY_SCRIPT,
      )
    }

  EmbeddedHtmlMultiTrackPlayer(
    html = html,
    itemCount = shows.size,
    itemHeight = MIXCLOUD_SHOW_HEIGHT,
    itemGap = MIXCLOUD_SHOW_GAP,
    loadingText = "Loading shows...",
    errorText = "Failed to load shows",
    baseUrl = "https://www.mixcloud.com",
    modifier = modifier,
  )
}

private const val MIXCLOUD_HEAD_SCRIPT =
  "<script src=\"//widget.mixcloud.com/media/js/widgetApi.js\" type=\"text/javascript\"></script>"

private const val MIXCLOUD_BODY_SCRIPT =
  "<script>(function() {" +
    "  var widgets = [];" +
    "  var iframes = document.querySelectorAll('iframe');" +
    "  iframes.forEach(function(iframe, index) {" +
    "    var widget = Mixcloud.PlayerWidget(iframe);" +
    "    widgets.push(widget);" +
    "    widget.ready.then(function() {" +
    "      widget.events.play.on(function() {" +
    "        widgets.forEach(function(w, i) {" +
    "          if (i !== index) {" +
    "            w.pause();" +
    "          }" +
    "        });" +
    "      });" +
    "    });" +
    "  });" +
    "})();</script>"

/**
 * Builds a Mixcloud widget embed URL for a given show key.
 *
 * @param showKey The Mixcloud show feed path (e.g. `/username/show-name/`)
 * @return The complete widget URL for embedding in a WebView
 */
internal fun buildMixcloudWidgetUrl(showKey: String): String {
  val encodedKey = showKey.encodeURLQueryComponent(encodeFull = true)
  return "https://www.mixcloud.com/widget/iframe/?feed=$encodedKey&hide_cover=1"
}
