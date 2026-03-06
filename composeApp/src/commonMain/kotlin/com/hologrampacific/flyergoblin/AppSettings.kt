package com.hologrampacific.flyergoblin

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

// The default Settings() uses platform persistent storage (SharedPreferences/UserDefaults).
// Tests must always inject a test double (e.g., MapSettings from multiplatform-settings)
// rather than constructing AppSettings() directly, to avoid polluting real storage.
class AppSettings(private val settings: Settings = Settings()) {

  var devModeEnabled: Boolean
    get() = settings.getBoolean(KEY_DEV_MODE, false)
    set(value) {
      settings[KEY_DEV_MODE] = value
    }

  companion object {
    private const val KEY_DEV_MODE = "dev_mode_enabled"
  }
}
