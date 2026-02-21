package com.hologrampacific.flyergoblin.flyer.presentation.artist

/** Formats city and country code into a single location string, or null if neither is present. */
internal fun buildLocationString(city: String?, countryCode: String?): String? =
  when {
    city != null && countryCode != null -> "$city, $countryCode"
    city != null -> city
    countryCode != null -> countryCode
    else -> null
  }
