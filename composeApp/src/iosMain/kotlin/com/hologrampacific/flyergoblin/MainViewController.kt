package com.hologrampacific.flyergoblin

import androidx.compose.ui.window.ComposeUIViewController
import com.hologrampacific.flyergoblin.db.DriverFactory

fun MainViewController() = ComposeUIViewController {
  App(DriverFactory()) { provider -> SharedImageBridge.sharedImageProvider = provider }
}
