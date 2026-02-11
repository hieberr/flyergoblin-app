package com.hologrampacific.learnkmp.flyer.presentation.addevent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hologrampacific.learnkmp.flyer.presentation.EventDetail
import com.hologrampacific.learnkmp.presentation.Navigator
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(navigator: Navigator, viewModel: AddEventViewModel = koinViewModel()) {
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(Unit) {
    viewModel.effects.collect { effect ->
      when (effect) {
        is AddEventEffect.NavigateToEventDetail -> {
          navigator.popAndGoTo(EventDetail(effect.eventId))
        }
      }
    }
  }

  val launcher =
    rememberFilePickerLauncher(
      type = PickerType.Image,
      mode = PickerMode.Single,
      title = "Select Event Flyer Image",
    ) { file ->
      file?.let { viewModel.onImageSelected(it) }
    }

  AddEventScreenContent(
    uiState = uiState,
    onBackClick = { navigator.goBack() },
    onSelectImageClick = { launcher.launch() },
    onProcessClick = { isManual -> viewModel.processFlyer(isManual = isManual) },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreenContent(
  uiState: AddEventUiState,
  onBackClick: () -> Unit,
  onSelectImageClick: () -> Unit,
  onProcessClick: (isManual: Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    TopAppBar(
      title = { Text("Add Event") },
      navigationIcon = { TextButton(onClick = onBackClick) { Text("< Back") } },
      windowInsets = WindowInsets(0, 0, 0, 0),
    )

    Box(modifier = Modifier.fillMaxSize()) {
      if (uiState.isProcessing) {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          CircularProgressIndicator(modifier = Modifier.size(64.dp))
          Spacer(modifier = Modifier.height(16.dp))
          Text(text = "Processing flyer...", style = MaterialTheme.typography.bodyLarge)
        }
      } else {
        Column(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Button(onClick = onSelectImageClick, modifier = Modifier.fillMaxWidth()) {
            Text("Select Flyer Image")
          }

          Spacer(modifier = Modifier.height(16.dp))

          uiState.selectedImageFile?.let {
            Text(
              text = "Image selected: ${it.name}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
          }

          Button(
            onClick = { onProcessClick(false) },
            enabled = uiState.selectedImageFile != null,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Automatically Create Event")
          }

          Button(
            onClick = { onProcessClick(true) },
            enabled = uiState.selectedImageFile != null,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Manually Create Event")
          }

          uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = error,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      }
    }
  }
}

@Composable
@Preview
fun AddEventScreenPreview() {
  MaterialTheme {
    AddEventScreenContent(
      uiState =
        AddEventUiState(selectedImageFile = null, isProcessing = false, errorMessage = null),
      onBackClick = {},
      onSelectImageClick = {},
      onProcessClick = {},
    )
  }
}
