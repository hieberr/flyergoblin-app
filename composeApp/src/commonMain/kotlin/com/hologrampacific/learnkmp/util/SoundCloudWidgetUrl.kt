package com.hologrampacific.learnkmp.util

/**
 * Builds a SoundCloud widget embed URL for a given track URL.
 *
 * @param trackUrl The SoundCloud track URL to embed
 * @param color The widget accent color (hex without #), defaults to SoundCloud orange
 * @return The complete widget URL for embedding in a WebView
 */
fun buildSoundCloudWidgetUrl(trackUrl: String, color: String = "ff5500"): String {
  val encodedUrl = urlEncode(trackUrl)

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

/**
 * Builds an HTML page containing multiple SoundCloud widget iframes.
 *
 * @param trackUrls List of SoundCloud track URLs to embed
 * @param color The widget accent color (hex without #), defaults to SoundCloud orange
 * @return Complete HTML page as a string with all track widgets embedded
 */
fun buildMultiTrackWidgetHtml(trackUrls: List<String>, color: String = "ff5500"): String {
  return buildString {
    append("<!DOCTYPE html>")
    append("<html>")
    append("<head>")
    append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
    append("<script src=\"https://w.soundcloud.com/player/api.js\"></script>")
    append("<style>")
    append("body { margin: 0; padding: 0; background-color: #f3f3f3; pointer-events: none; }")
    append("iframe { display: block; width: 100%; border: none; margin-bottom: 10px; pointer-events: auto; }")
    append("</style>")
    append("</head>")
    append("<body>")

    // Add an iframe for each track with unique ID
    trackUrls.forEachIndexed { index, trackUrl ->
      val widgetUrl = buildSoundCloudWidgetUrl(trackUrl, color)
      append("<iframe id=\"sc-widget-")
      append(index)
      append("\" height=\"166\" src=\"")
      append(widgetUrl)
      append("\"></iframe>")
    }

    append("<script>")
    append("(function() {")
    append("  var widgets = [];")
    append("  var iframes = document.querySelectorAll('iframe');")
    append("  ")
    append("  iframes.forEach(function(iframe, index) {")
    append("    var widget = SC.Widget(iframe);")
    append("    widgets.push(widget);")
    append("    ")
    append("    widget.bind(SC.Widget.Events.PLAY, function() {")
    append("      widgets.forEach(function(w, i) {")
    append("        if (i !== index) {")
    append("          w.pause();")
    append("        }")
    append("      });")
    append("    });")
    append("  });")
    append("})();")
    append("</script>")
    append("</body>")
    append("</html>")
  }
}

/**
 * URL encodes a string for use in query parameters.
 *
 * This is a simple implementation that encodes the most common special characters.
 */
private fun urlEncode(value: String): String {
  return buildString {
    for (char in value) {
      when (char) {
        ' ' -> append("%20")
        '!' -> append("%21")
        '#' -> append("%23")
        '$' -> append("%24")
        '&' -> append("%26")
        '\'' -> append("%27")
        '(' -> append("%28")
        ')' -> append("%29")
        '*' -> append("%2A")
        '+' -> append("%2B")
        ',' -> append("%2C")
        '/' -> append("%2F")
        ':' -> append("%3A")
        ';' -> append("%3B")
        '=' -> append("%3D")
        '?' -> append("%3F")
        '@' -> append("%40")
        '[' -> append("%5B")
        ']' -> append("%5D")
        else -> append(char)
      }
    }
  }
}
