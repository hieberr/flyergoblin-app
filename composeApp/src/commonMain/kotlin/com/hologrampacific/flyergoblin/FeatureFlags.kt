package com.hologrampacific.flyergoblin

/**
 * Compile-time feature flags for optional/experimental features.
 *
 * Toggle a flag here to enable or disable the feature across the entire app.
 */
object FeatureFlags {
  /**
   * Use the wheel-style date picker (kmp-date-time-picker library) instead of the standard
   * Material3 DatePickerDialog on the Edit Event screen.
   */
  const val USE_WHEEL_DATE_PICKER = false
}
