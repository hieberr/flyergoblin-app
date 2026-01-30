package com.hologrampacific.learnkmp.flyer.presentation.detail

import com.hologrampacific.learnkmp.flyer.domain.model.Event

data class EventDetailUiState(
  val event: Event? = null,
  val isEditing: Boolean = false,
  val editedEvent: EditedEventData? = null,
  val isLoading: Boolean = true,
  val errorMessage: String? = null,
)

data class EditedEventData(
  val name: String,
  val startDate: String,
  val startTime: String,
  val venue: String,
  val eventUrl: String,
  val artists: String,
)
