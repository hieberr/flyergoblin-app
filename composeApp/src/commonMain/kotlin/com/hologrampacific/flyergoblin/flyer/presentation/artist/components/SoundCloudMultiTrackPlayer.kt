package com.hologrampacific.flyergoblin.flyer.presentation.artist.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.flyergoblin.presentation.util.htmlHexString
import io.ktor.http.*

/** Height of the SoundCloud player widgets in dp */
const val SOUNDCLOUD_TRACK_HEIGHT = 132

/**
 * Gap between SoundCloud track player widgets in dp. Note: intentionally matches
 * [com.hologrampacific.flyergoblin.presentation.Ui.halfUnit] (8.dp). Update together if spacing
 * changes.
 */
const val SOUNDCLOUD_TRACK_GAP = 8

/**
 * Multi-track SoundCloud player that embeds all tracks in a single WebView.
 *
 * @param tracks List of SoundCloud tracks to display
 * @param modifier Modifier for the composable
 */
@Composable
fun SoundCloudMultiTrackPlayer(tracks: List<SoundCloudTrack>, modifier: Modifier = Modifier) {
  val backgroundColor = MaterialTheme.colorScheme.background
  val html =
    remember(tracks, backgroundColor) {
      buildMultiTrackPlayerHtml(
        trackUrls = tracks.map { buildSoundCloudWidgetUrl(it.url, "ff5500") },
        trackHeight = SOUNDCLOUD_TRACK_HEIGHT,
        trackGap = SOUNDCLOUD_TRACK_GAP,
        backgroundColor = backgroundColor.htmlHexString,
        scrollable = webViewScrollsInternally,
        headScript = SC_HEAD_SCRIPT,
        bodyScript = SC_BODY_SCRIPT,
      )
    }

  EmbeddedHtmlMultiTrackPlayer(
    html = html,
    itemCount = tracks.size,
    itemHeight = SOUNDCLOUD_TRACK_HEIGHT,
    itemGap = SOUNDCLOUD_TRACK_GAP,
    loadingText = "Loading tracks...",
    errorText = "Failed to load tracks",
    baseUrl = "https://w.soundcloud.com",
    modifier = modifier,
  )
}

private const val SC_HEAD_SCRIPT =
  "<script src=\"https://w.soundcloud.com/player/api.js\"></script>"

private const val SC_BODY_SCRIPT =
  "<script>(function() {" +
    "  var widgets = [];" +
    "  var iframes = document.querySelectorAll('iframe');" +
    "  iframes.forEach(function(iframe, index) {" +
    "    var widget = SC.Widget(iframe);" +
    "    widgets.push(widget);" +
    "    widget.bind(SC.Widget.Events.PLAY, function() {" +
    "      widgets.forEach(function(w, i) {" +
    "        if (i !== index) {" +
    "          w.pause();" +
    "        }" +
    "      });" +
    "    });" +
    "  });" +
    "})();</script>"

/**
 * Builds a SoundCloud widget embed URL for a given track URL.
 *
 * @param trackUrl The SoundCloud track URL to embed
 * @param color The widget accent color (hex without #), defaults to SoundCloud orange
 * @return The complete widget URL for embedding in a WebView
 */
internal fun buildSoundCloudWidgetUrl(trackUrl: String, color: String = "ff5500"): String {
  val encodedUrl = trackUrl.encodeURLQueryComponent(encodeFull = true)

  return buildString {
    append("https://w.soundcloud.com/player/?url=")
    append(encodedUrl)
    append("&color=%23")
    append(color)
    append("&auto_play=false")
    append("&hide_related=true")
    append("&show_comments=false")
    append("&show_user=true")
    append("&show_reposts=false")
    append("&show_teaser=false")
    append("&visual=true")
    append("&single_active=true")
    append("&download=false")
    append("&buying=false")
    append("&sharing=false")
  }
}
