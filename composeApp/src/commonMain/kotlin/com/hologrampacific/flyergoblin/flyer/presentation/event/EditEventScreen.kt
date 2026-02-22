package com.hologrampacific.flyergoblin.flyer.presentation.event

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hologrampacific.flyergoblin.presentation.Navigator
import com.hologrampacific.flyergoblin.presentation.Ui
import com.hologrampacific.flyergoblin.presentation.components.ScreenButtonConfig
import com.hologrampacific.flyergoblin.presentation.components.TopAppBarScreenWithCenteredContent
import com.hologrampacific.flyergoblin.presentation.components.rememberLottieLoopProgress
import com.hologrampacific.flyergoblin.util.decodeImageBitmap
import flyergoblin.composeapp.generated.resources.Res
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import network.chaintech.kmp_date_time_picker.ui.datepicker.WheelDatePickerView
import network.chaintech.kmp_date_time_picker.utils.DateTimePickerView
import network.chaintech.kmp_date_time_picker.utils.WheelPickerDefaults
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// Toggle between the wheel picker (kmp-date-time-picker) and the standard Material3 DatePicker.
private const val USE_WHEEL_DATE_PICKER = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
  navigator: Navigator,
  eventId: Long?,
  viewModel: EditEventViewModel = koinViewModel { parametersOf(eventId) },
) {
  val uiState by viewModel.uiState.collectAsState()

  // Hoisted here so the Material3 DatePickerDialog can be placed outside the scroll container,
  // which is required for it to appear correctly on iOS.
  var showDatePicker by remember { mutableStateOf(false) }

  val imagePickerLauncher =
    rememberFilePickerLauncher(
      type = PickerType.Image,
      mode = PickerMode.Single,
      title = "Select Flyer Image",
    ) { file ->
      file?.let { viewModel.onImageSelected(it) }
    }

  LaunchedEffect(Unit) {
    viewModel.effects.collect { effect ->
      when (effect) {
        EditEventEffect.NavigateBack -> navigator.goBack()
        is EditEventEffect.NavigateToEventDetail ->
          navigator.popAndGoTo(
            com.hologrampacific.flyergoblin.flyer.presentation.EventDetail(effect.eventId)
          )
      }
    }
  }

  // Validation logic
  val isFormValid =
    remember(uiState.editedEvent) {
      val edited = uiState.editedEvent ?: return@remember false
      edited.name.isNotBlank() &&
        edited.startDate != null &&
        (edited.startTime.isEmpty() || edited.startTime.matches(Regex("""\d{2}:\d{2}""")))
    }

  // Button configurations
  val primaryButtonConfig =
    remember(isFormValid, uiState.isSaving) {
      ScreenButtonConfig(
        text = "Save",
        onClick = { viewModel.saveEvent() },
        enabled = isFormValid && !uiState.isSaving,
      )
    }

  val secondaryButtonConfig = remember {
    ScreenButtonConfig(text = "Cancel", onClick = { navigator.goBack() })
  }

  TopAppBarScreenWithCenteredContent(
    appBarTitle = if (eventId == null) "Add Event" else "Edit Event",
    onBackClicked = { navigator.goBack() },
    primaryButtonConfig = primaryButtonConfig,
    secondaryButtonConfig = secondaryButtonConfig,
    overlay = { if (uiState.isProcessingFlyer) ProcessingFlyerOverlay() },
  ) {
    uiState.editedEvent?.let { editedEvent ->
      EditEventContent(
        editedEvent = editedEvent,
        errorMessage = uiState.errorMessage,
        hasSelectedImage = uiState.selectedImageFile != null,
        showDatePicker = showDatePicker,
        onEventChange = { viewModel.updateEditedEvent(it) },
        onSelectImage = { imagePickerLauncher.launch() },
        onProcessFlyer = { viewModel.processFlyer() },
        onReplaceImage = { viewModel.replaceImage() },
        onDateFieldClick = { showDatePicker = true },
        onDatePickerDismiss = { showDatePicker = false },
      )
    }
  }

  // Material3 DatePickerDialog is placed here, outside the scroll container, so it renders
  // correctly on iOS.
  if (!USE_WHEEL_DATE_PICKER && showDatePicker) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val initialMillis = (uiState.editedEvent?.startDate ?: today).toEpochDays() * 86_400_000L
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            val millis = datePickerState.selectedDateMillis
            if (millis != null) {
              val days = (millis / 86_400_000L).toInt()
              uiState.editedEvent?.let {
                viewModel.updateEditedEvent(it.copy(startDate = LocalDate.fromEpochDays(days)))
              }
            }
            showDatePicker = false
          }
        ) { Text("OK") }
      },
      dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
    ) {
      DatePicker(state = datePickerState)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventContent(
  editedEvent: EditedEventData,
  errorMessage: String?,
  hasSelectedImage: Boolean,
  showDatePicker: Boolean,
  onEventChange: (EditedEventData) -> Unit,
  onSelectImage: () -> Unit,
  onProcessFlyer: () -> Unit,
  onReplaceImage: () -> Unit,
  onDateFieldClick: () -> Unit,
  onDatePickerDismiss: () -> Unit,
) {
  val isTimeValid =
    editedEvent.startTime.isEmpty() || editedEvent.startTime.matches(Regex("""\d{2}:\d{2}"""))

  Column(verticalArrangement = Arrangement.spacedBy(Ui.halfUnit)) {
    // Display flyer image section with click support for selection
    if (editedEvent.flyerImageBytes != null) {
      // Image exists - show it with replace button
      FlyerImage(imageBytes = editedEvent.flyerImageBytes, modifier = Modifier.fillMaxWidth())
      OutlinedButton(onClick = onReplaceImage, modifier = Modifier.fillMaxWidth()) {
        Text("Replace Image")
      }
    } else {
      // No image - show clickable placeholder
      ClickableFlyerImageSection(onClick = onSelectImage, modifier = Modifier.fillMaxWidth())
    }

    // Show "Process Flyer" button if image is selected
    if (hasSelectedImage) {
      Button(onClick = onProcessFlyer, modifier = Modifier.fillMaxWidth()) {
        Text("Get event details from flyer")
      }
    }

    OutlinedTextField(
      value = editedEvent.name,
      onValueChange = { onEventChange(editedEvent.copy(name = it)) },
      label = { Text("Event Name *") },
      modifier = Modifier.fillMaxWidth(),
    )

    // OutlinedTextField doesn't produce PressInteraction on iOS/Desktop (CMP issue #4087),
    // so a transparent Box overlay is used to capture taps instead.
    Box(modifier = Modifier.fillMaxWidth()) {
      OutlinedTextField(
        value = editedEvent.startDate?.let {
          // toString() is ISO "YYYY-MM-DD"; rearrange to "DD-MM-YYYY"
          val iso = it.toString()
          "${iso.substring(8, 10)}-${iso.substring(5, 7)}-${iso.substring(0, 4)}"
        } ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text("Date *") },
        isError = editedEvent.startDate == null,
        supportingText =
          if (editedEvent.startDate == null) {
            { Text("Date is required.") }
          } else null,
        modifier = Modifier.fillMaxWidth(),
      )
      Box(modifier = Modifier.matchParentSize().clickable(onClick = onDateFieldClick))
    }

    // WheelDatePickerView handles its own iOS dialog presentation so it stays here in the column.
    if (USE_WHEEL_DATE_PICKER && showDatePicker) {
      val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
      WheelDatePickerView(
        showDatePicker = true,
        dateTimePickerView = DateTimePickerView.DIALOG_VIEW,
        startDate = editedEvent.startDate ?: today,
        yearsRange = IntRange(2020, 2040),
        height = Ui.unit * 10,
        title = "Start Date",
        onDoneClick = { date ->
          onEventChange(editedEvent.copy(startDate = date))
          onDatePickerDismiss()
        },
        onDismiss = onDatePickerDismiss,
        selectorProperties = WheelPickerDefaults.selectorProperties(borderColor = Color.Red),
      )
    }

    OutlinedTextField(
      value = editedEvent.startTime,
      onValueChange = { onEventChange(editedEvent.copy(startTime = it)) },
      label = { Text("Time (HH:MM format)") },
      isError = !isTimeValid && editedEvent.startTime.isNotBlank(),
      supportingText =
        if (!isTimeValid && editedEvent.startTime.isNotBlank()) {
          { Text("Format must be HH:MM. Time validity checked on save.") }
        } else null,
      modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
      value = editedEvent.venue,
      onValueChange = { onEventChange(editedEvent.copy(venue = it)) },
      label = { Text("Venue") },
      modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
      value = editedEvent.eventUrl,
      onValueChange = { onEventChange(editedEvent.copy(eventUrl = it)) },
      label = { Text("Event URL") },
      modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
      value = editedEvent.artists,
      onValueChange = { onEventChange(editedEvent.copy(artists = it)) },
      label = { Text("Artists (comma-separated)") },
      modifier = Modifier.fillMaxWidth(),
      minLines = 3,
    )

    errorMessage?.let { error ->
      Text(
        text = error,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

@Composable
private fun ProcessingFlyerOverlay() {
  val composition by rememberLottieComposition {
    LottieCompositionSpec.DotLottie(
      Res.readBytes("files/lottie/flyer-goblin-eat-flyer-black.lottie")
    )
  }
  val progress = rememberLottieLoopProgress(composition, loopStartSeconds = 2.04f)
  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Image(
        painter = rememberLottiePainter(composition = composition, progress = progress),
        contentDescription = null,
        modifier =
          composition?.let { Modifier.size((it.width * 2).dp, (it.height * 2).dp) } ?: Modifier,
      )
      Ui.SpacerUnitHeight()
      Text(
        text = "Gobbling flyer...",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}

@Composable
private fun FlyerImage(imageBytes: ByteArray, modifier: Modifier = Modifier) {
  val imageBitmap = remember(imageBytes) { decodeImageBitmap(imageBytes) }

  if (imageBitmap != null) {
    Image(
      bitmap = imageBitmap,
      contentDescription = "Event flyer",
      modifier = modifier,
      contentScale = ContentScale.FillWidth,
    )
  } else {
    // Show error state when image decoding fails
    Surface(
      modifier = modifier.height(Ui.unit * 12),
      color = MaterialTheme.colorScheme.errorContainer,
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(Ui.unit),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text("Unable to display flyer image", color = MaterialTheme.colorScheme.onErrorContainer)
      }
    }
  }
}

@Composable
fun ClickableFlyerImageSection(onClick: () -> Unit, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth().height(Ui.unit * 12).clickable(onClick = onClick),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(Ui.unit),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(text = "📷", style = MaterialTheme.typography.displayLarge)
      Spacer(modifier = Modifier.height(Ui.halfUnit))
      Text(
        text = "Tap to select flyer image",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}
