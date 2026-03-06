package com.hologrampacific.flyergoblin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UiState types that carry an [errorMessage] field implement this to enable
 * [ErrorMessageViewModel].
 */
interface HasErrorMessage<T> {
  val errorMessage: String?

  fun copyWithErrorMessage(errorMessage: String?): T
}

/**
 * Base ViewModel for screens that display error messages via a snackbar.
 *
 * Subclasses get [clearError] and [triggerTestError] for free. The [_uiState] and [uiState]
 * properties are also provided so subclasses don't need to redeclare them.
 */
abstract class ErrorMessageViewModel<S : HasErrorMessage<S>>(initialState: S) : ViewModel() {

  protected val _uiState = MutableStateFlow(initialState)
  val uiState: StateFlow<S> = _uiState.asStateFlow()

  open fun clearError() {
    _uiState.update { it.copyWithErrorMessage(null) }
  }

  fun triggerTestError() {
    _uiState.update { it.copyWithErrorMessage("Test error message") }
  }
}
