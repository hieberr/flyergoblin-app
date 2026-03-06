package com.hologrampacific.flyergoblin.flyer.presentation.event

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hologrampacific.flyergoblin.FeatureFlags
import com.hologrampacific.flyergoblin.PlatformType
import com.hologrampacific.flyergoblin.flyer.presentation.EventDetail
import com.hologrampacific.flyergoblin.getPlatform
import com.hologrampacific.flyergoblin.presentation.NavTransition
import com.hologrampacific.flyergoblin.presentation.Navigator
import com.hologrampacific.flyergoblin.presentation.Ui
import com.hologrampacific.flyergoblin.presentation.components.FlyerImage
import com.hologrampacific.flyergoblin.presentation.components.ScreenButtonConfig
import com.hologrampacific.flyergoblin.presentation.components.TopAppBarScreenWithCenteredContent
import com.hologrampacific.flyergoblin.presentation.theme.AppTheme
import com.hologrampacific.flyergoblin.presentation.util.formattedString
import com.hologrampacific.flyergoblin.presentation.util.rememberGoblinDynamicProperties
import com.hologrampacific.flyergoblin.presentation.util.rememberLottieLoopProgress
import com.hologrampacific.flyergoblin.util.MILLIS_PER_DAY
import flyergoblin.composeapp.generated.resources.Res
import flyergoblin.composeapp.generated.resources.arrow_cool_down_24px
import flyergoblin.composeapp.generated.resources.image_24px
import flyergoblin.composeapp.generated.resources.more_vert_24px
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.ExperimentalCompottieApi
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import network.chaintech.kmp_date_time_picker.ui.datepicker.WheelDatePickerView
import network.chaintech.kmp_date_time_picker.utils.DateTimePickerView
import network.chaintech.kmp_date_time_picker.utils.WheelPickerDefaults
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
  navigator: Navigator,
  eventId: Long?,
  viewModel: EditEventViewModel = koinViewModel { parametersOf(eventId) },
) {
  val uiState by viewModel.uiState.collectAsState()

  // Hoisted here so the Material3 pickers can be placed outside the scroll container,
  // which is required for them to appear correctly on iOS.
  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }
  var showCropImage by remember { mutableStateOf(false) }

  val imagePickerLauncher =
    rememberFilePickerLauncher(
      type = PickerType.Image,
      mode = PickerMode.Single,
      title = "Select Flyer Image",
    ) { file ->
      file?.let { viewModel.onImageSelected(it) }
    }

  LaunchedEffect(viewModel) {
    viewModel.effects.collect { effect ->
      when (effect) {
        EditEventEffect.NavigateBack -> navigator.goBack(NavTransition.Fade)
        is EditEventEffect.NavigateToEventDetail ->
          navigator.popAndGoTo(EventDetail(effect.eventId), NavTransition.Fade)
      }
    }
  }

  // Validation logic
  val isFormValid =
    remember(uiState.editedEvent) {
      val edited = uiState.editedEvent ?: return@remember false
      edited.name.isNotBlank() && edited.startDate != null
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
    ScreenButtonConfig(text = "Cancel", onClick = { navigator.goBack(NavTransition.Fade) })
  }

  Box(modifier = Modifier.fillMaxSize()) {
    TopAppBarScreenWithCenteredContent(
      appBarTitle = if (eventId == null) "Add Event" else "Edit Event",
      onBackClicked = { navigator.goBack(NavTransition.Fade) },
      primaryButtonConfig = primaryButtonConfig,
      secondaryButtonConfig = secondaryButtonConfig,
      overlay = { if (uiState.isProcessingFlyer) ProcessingFlyerOverlay() },
    ) {
      uiState.editedEvent?.let { editedEvent ->
        EditEventContent(
          editedEvent = editedEvent,
          errorMessage = uiState.errorMessage,
          hasSelectedImage = uiState.editedEvent?.flyerImageBytes != null,
          onEventChange = { viewModel.updateEditedEvent(it) },
          onSelectImage = { imagePickerLauncher.launch() },
          onProcessFlyer = { viewModel.processFlyer() },
          onReplaceImage = { viewModel.replaceImage() },
          onEditImage = { showCropImage = true },
          onDateFieldClick = { showDatePicker = true },
          onTimeFieldClick = { showTimePicker = true },
        )
      }
    }

    if (showCropImage) {
      val imageBytes = uiState.editedEvent?.flyerImageBytes
      if (imageBytes != null) {
        CropImageScreen(
          imageBytes = imageBytes,
          onDone = { croppedBytes ->
            uiState.editedEvent?.let {
              viewModel.updateEditedEvent(it.copy(flyerImageBytes = croppedBytes))
            }
            showCropImage = false
          },
          onCancel = { showCropImage = false },
        )
      }
    }
  }

  // Material3 DatePickerDialog is placed here, outside the scroll container, so it renders
  // correctly on iOS.
  if (!FeatureFlags.USE_WHEEL_DATE_PICKER && showDatePicker) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val initialMillis = (uiState.editedEvent?.startDate ?: today).toEpochDays() * MILLIS_PER_DAY
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            val millis = datePickerState.selectedDateMillis
            if (millis != null) {
              val days = (millis / MILLIS_PER_DAY).toInt()
              uiState.editedEvent?.let {
                viewModel.updateEditedEvent(it.copy(startDate = LocalDate.fromEpochDays(days)))
              }
            }
            showDatePicker = false
          }
        ) {
          Text("OK")
        }
      },
      dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  // TimePickerDialog placed here, outside the scroll container, so it renders correctly on iOS.
  if (showTimePicker) {
    val currentTime = uiState.editedEvent?.startTime
    val timePickerState =
      rememberTimePickerState(
        initialHour = currentTime?.hour ?: 21,
        initialMinute = currentTime?.minute ?: 0,
        is24Hour = false,
      )
    AlertDialog(
      onDismissRequest = { showTimePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            uiState.editedEvent?.let {
              viewModel.updateEditedEvent(
                it.copy(startTime = LocalTime(timePickerState.hour, timePickerState.minute))
              )
            }
            showTimePicker = false
          }
        ) {
          Text("OK")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            uiState.editedEvent?.let { viewModel.updateEditedEvent(it.copy(startTime = null)) }
            showTimePicker = false
          }
        ) {
          Text("Clear")
        }
      },
      title = { Text("Start Time") },
      text = {
        if (getPlatform().type == PlatformType.DESKTOP) {
          // At default scale the time picker appears too large on desktop. So, scale it down.
          val density = LocalDensity.current
          CompositionLocalProvider(
            LocalDensity provides Density(density.density * 0.45f, density.fontScale)
          ) {
            TimePicker(state = timePickerState)
          }
        } else {
          TimePicker(state = timePickerState)
        }
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventContent(
  editedEvent: EditedEventData,
  errorMessage: String?,
  hasSelectedImage: Boolean,
  onEventChange: (EditedEventData) -> Unit,
  onSelectImage: () -> Unit,
  onProcessFlyer: () -> Unit,
  onReplaceImage: () -> Unit,
  onEditImage: () -> Unit,
  onDateFieldClick: () -> Unit,
  onTimeFieldClick: () -> Unit,
) {
  // Internal state for the wheel date picker (only used when FeatureFlags.USE_WHEEL_DATE_PICKER =
  // true).
  var showWheelDatePicker by remember { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(Ui.halfUnit)) {
    // Display flyer image section with click support for selection
    if (editedEvent.flyerImageBytes != null) {
      // Image exists - show it with a "..." menu in the top-right corner
      var showImageMenu by remember { mutableStateOf(false) }
      Box(modifier = Modifier.fillMaxWidth()) {
        FlyerImage(imageBytes = editedEvent.flyerImageBytes, modifier = Modifier.fillMaxWidth())
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
          IconButton(onClick = { showImageMenu = true }) {
            Icon(
              painter = painterResource(Res.drawable.more_vert_24px),
              contentDescription = "Image options",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier =
                Modifier.size(Ui.unit * 2f)
                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
            )
          }
          DropdownMenu(expanded = showImageMenu, onDismissRequest = { showImageMenu = false }) {
            DropdownMenuItem(
              text = { Text("Clear Image") },
              onClick = {
                showImageMenu = false
                onReplaceImage()
              },
            )
            DropdownMenuItem(
              text = { Text("Crop Image") },
              onClick = {
                showImageMenu = false
                onEditImage()
              },
            )
          }
        }
      }
    } else {
      // No image - show clickable placeholder
      ClickableFlyerImageSection(onClick = onSelectImage, modifier = Modifier.fillMaxWidth())
    }

    // Show "Get Details" button if image is selected
    if (hasSelectedImage) {
      val hasEventData =
        editedEvent.name.isNotBlank() ||
          editedEvent.startDate != null

      val getDetailsButtonContent: @Composable RowScope.() -> Unit = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(Ui.halfUnit),
        ) {
          Icon(
            painter = painterResource(Res.drawable.arrow_cool_down_24px),
            contentDescription = null,
            modifier = Modifier.size(Ui.standardIconSize),
          )
          Text("Get Details")
          Icon(
            painter = painterResource(Res.drawable.arrow_cool_down_24px),
            contentDescription = null,
            modifier = Modifier.size(Ui.standardIconSize),
          )
        }
      }
      if (hasEventData) {
        OutlinedButton(
          onClick = onProcessFlyer,
          modifier = Modifier.fillMaxWidth(),
          content = getDetailsButtonContent,
        )
      } else {
        Button(
          onClick = onProcessFlyer,
          modifier = Modifier.fillMaxWidth(),
          content = getDetailsButtonContent,
        )
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
        value = editedEvent.startDate?.formattedString() ?: "",
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
      Box(
        modifier =
          Modifier.matchParentSize().clickable {
            if (FeatureFlags.USE_WHEEL_DATE_PICKER) showWheelDatePicker = true
            else onDateFieldClick()
          }
      )
    }

    // WheelDatePickerView handles its own iOS dialog presentation so it stays here in the column.
    if (FeatureFlags.USE_WHEEL_DATE_PICKER && showWheelDatePicker) {
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
          showWheelDatePicker = false
        },
        onDismiss = { showWheelDatePicker = false },
        selectorProperties = WheelPickerDefaults.selectorProperties(borderColor = Color.Red),
      )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
      OutlinedTextField(
        value = editedEvent.startTime?.formattedString() ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text("Start Time") },
        modifier = Modifier.fillMaxWidth(),
      )
      Box(modifier = Modifier.matchParentSize().clickable(onClick = onTimeFieldClick))
    }

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

@OptIn(ExperimentalCompottieApi::class)
@Composable
private fun ProcessingFlyerOverlay() {
  val composition by rememberLottieComposition {
    LottieCompositionSpec.DotLottie(
      Res.readBytes("files/lottie/flyer-goblin-eat-flyer-black.lottie")
    )
  }
  val progress = rememberLottieLoopProgress(composition, loopStartSeconds = 2.04f)
  val dynamicProperties = rememberGoblinDynamicProperties()
  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
  ) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
      Spacer(modifier = Modifier.weight(0.2f))
      Image(
        painter =
          rememberLottiePainter(
            composition = composition,
            progress = progress,
            dynamicProperties = dynamicProperties,
          ),
        contentDescription = null,
        modifier =
          composition?.let { Modifier.size((it.width * 2).dp, (it.height * 2).dp) } ?: Modifier,
      )
      Ui.SpacerUnitHeight()
      Text(
        text = "Gobblin' Flyer...",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Medium,
      )
      Spacer(modifier = Modifier.weight(0.8f))
    }
  }
}

@Composable
private fun ClickableFlyerImageSection(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
      Icon(
        painter = painterResource(Res.drawable.image_24px),
        contentDescription = "Image options",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(Ui.unit * 4),
      )
      Spacer(modifier = Modifier.height(Ui.halfUnit))
      Text(
        text = "Select Flyer Image",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}

@Composable
@Preview
private fun EditEventContentEmptyPreview() {
  AppTheme {
    EditEventContent(
      editedEvent =
        EditedEventData(
          name = "",
          startDate = null,
          startTime = null,
          venue = "",
          eventUrl = "",
          artists = "",
          flyerImageBytes = null,
        ),
      errorMessage = null,
      hasSelectedImage = false,
      onEventChange = {},
      onSelectImage = {},
      onProcessFlyer = {},
      onReplaceImage = {},
      onEditImage = {},
      onDateFieldClick = {},
      onTimeFieldClick = {},
    )
  }
}

@Composable
@Preview
private fun EditEventContentFilledPreview() {
  AppTheme {
    EditEventContent(
      editedEvent =
        EditedEventData(
          name = "Summer Beats",
          startDate = LocalDate(2025, 8, 10),
          startTime = LocalTime(21, 0),
          venue = "The Fillmore",
          eventUrl = "https://example.com/event",
          artists = "DJ Sunset, The Headliners, Acoustic Soul",
          flyerImageBytes = null,
        ),
      errorMessage = null,
      hasSelectedImage = true,
      onEventChange = {},
      onSelectImage = {},
      onProcessFlyer = {},
      onReplaceImage = {},
      onEditImage = {},
      onDateFieldClick = {},
      onTimeFieldClick = {},
    )
  }
}
