package com.hologrampacific.flyergoblin.flyer.presentation.artist.components

/**
 * Builds a generic HTML page containing multiple widget iframes in a single WebView.
 *
 * @param trackUrls List of pre-built widget embed URLs
 * @param trackHeight Height of each iframe in dp
 * @param trackGap Gap between iframes in dp
 * @param backgroundColor Background color as HTML hex string (e.g. "#FFFFFF")
 * @param scrollable When true the page body scrolls (for platforms where the WebView handles
 *   scrolling internally). When false overflow is hidden and the outer Compose container scrolls.
 * @param headScript Optional script tag to inject in the `<head>` (e.g. a widget API loader)
 * @param bodyScript Optional script block to inject at the end of `<body>` (e.g. single-active JS)
 * @return Complete HTML page as a string
 */
fun buildMultiTrackPlayerHtml(
  trackUrls: List<String>,
  trackHeight: Int,
  trackGap: Int,
  backgroundColor: String,
  scrollable: Boolean = false,
  headScript: String? = null,
  bodyScript: String? = null,
): String {
  val overflow = if (scrollable) "auto" else "hidden"
  return buildString {
    append("<!DOCTYPE html>")
    append("<html>")
    append("<head>")
    append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
    if (headScript != null) append(headScript)
    append("<style>")
    append("html, body { margin: 0; padding: 0; overflow: $overflow; background-color: ")
    append(backgroundColor)
    append("; pointer-events: none; }")
    append("iframe { display: block; width: 100%; border: none; margin-bottom: ")
    append(trackGap)
    append("px; pointer-events: auto; }")
    append("</style>")
    append("</head>")
    append("<body>")
    trackUrls.forEach { url ->
      append("<iframe height=\"")
      append(trackHeight)
      append("\" src=\"")
      append(url)
      append("\" allow=\"autoplay\"></iframe>")
    }
    if (bodyScript != null) append(bodyScript)
    append("</body>")
    append("</html>")
  }
}
