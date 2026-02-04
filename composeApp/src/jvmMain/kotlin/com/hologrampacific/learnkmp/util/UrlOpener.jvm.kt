package com.hologrampacific.learnkmp.util

import java.awt.Desktop
import java.net.URI

actual fun openUrl(url: String) {
    try {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI(url))
            } else {
                AppLogger.e("UrlOpener", "Desktop BROWSE action not supported")
            }
        } else {
            AppLogger.e("UrlOpener", "Desktop not supported on this platform")
        }
    } catch (e: Exception) {
        AppLogger.e("UrlOpener", "Failed to open URL: $url", e)
    }
}
