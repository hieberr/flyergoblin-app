package com.hologrampacific.learnkmp.flyer.data.service

import kotlinx.serialization.Serializable

@Serializable
data class ExtractedEventData(
  val name: String = "Unknown",
  val startDate: String = "Unknown",
  val startTime: String? = null,
  val venue: String? = null,
  val eventUrl: String? = null,
  val artists: List<String> = emptyList(),
)
