package com.hologrampacific.learnkmp.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: run {
        AppLogger.e("UrlOpener", "Invalid URL: $url")
        return
    }

    UIApplication.sharedApplication.openURL(nsUrl)
}
