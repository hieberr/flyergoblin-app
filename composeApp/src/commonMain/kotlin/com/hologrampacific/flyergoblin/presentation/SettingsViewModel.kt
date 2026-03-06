package com.hologrampacific.flyergoblin.presentation

import androidx.lifecycle.ViewModel
import com.hologrampacific.flyergoblin.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(private val appSettings: AppSettings) : ViewModel() {

  // Reads the initial value once. This is safe because AppSettings is only
  // mutated through this ViewModel, so the StateFlow never goes stale.
  private val _devModeEnabled = MutableStateFlow(appSettings.devModeEnabled)
  val devModeEnabled: StateFlow<Boolean> = _devModeEnabled.asStateFlow()

  fun setDevModeEnabled(enabled: Boolean) {
    appSettings.devModeEnabled = enabled
    _devModeEnabled.value = enabled
  }
}
