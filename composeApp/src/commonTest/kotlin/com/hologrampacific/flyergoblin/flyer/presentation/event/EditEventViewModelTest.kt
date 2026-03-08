package com.hologrampacific.flyergoblin.flyer.presentation.event

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerProcessingDataSource
import com.hologrampacific.flyergoblin.flyer.domain.repository.EventRepository
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ProcessFlyerUseCase
import com.hologrampacific.flyergoblin.sharing.SharedImageProvider
import com.hologrampacific.flyergoblin.util.ImageBytes
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import io.github.vinceglb.filekit.core.PlatformFile
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EditEventViewModelTest : AppTest() {

  private val testDispatcher = UnconfinedTestDispatcher()

  @BeforeTest
  fun setupDispatcher() {
    Dispatchers.setMain(testDispatcher)
  }

  @AfterTest
  fun teardownDispatcher() {
    Dispatchers.resetMain()
  }

  // Minimal 1×1 red pixel PNG — valid image for exercising the happy-path through
  // reencodeImageToFitSize on all platforms.
  @OptIn(ExperimentalEncodingApi::class)
  private val minimalPng: ByteArray by lazy {
    Base64.decode(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI6QAAAABJRU5ErkJggg=="
    )
  }

  private fun makeViewModel(
    eventId: Long? = null,
    sharedImageProvider: SharedImageProvider = SharedImageProvider(),
    stubDataSource: (FlyerProcessingDataSource) -> Unit = {},
    encodeImage: (ImageBytes, Int) -> ImageBytes? = { _, _ -> null },
  ): EditEventViewModel {
    val repository: EventRepository = mock(MockMode.autoUnit)
    val dataSource: FlyerProcessingDataSource = mock(MockMode.autoUnit)
    stubDataSource(dataSource)
    return EditEventViewModel(
      eventId,
      repository,
      ProcessFlyerUseCase(dataSource),
      sharedImageProvider,
      encodeImage,
    )
  }

  @Test
  fun `when no pending shared image, flyerImageBytes starts as null`() = runTest {
    val viewModel = makeViewModel()

    assertNull(viewModel.uiState.value.editedEvent?.flyerImageBytes)
  }

  @Test
  fun `consumePendingImage is called in init when eventId is null`() = runTest {
    val sharedImageProvider = SharedImageProvider()
    sharedImageProvider.setSharedImage(byteArrayOf(0x01, 0x02, 0x03))

    makeViewModel(sharedImageProvider = sharedImageProvider)

    // The ViewModel consumed the image during init; nothing should remain for subsequent callers.
    assertNull(sharedImageProvider.consumePendingImage())
  }

  @Test
  fun `when pending shared image fails validation, errorMessage is set`() = runTest {
    val sharedImageProvider = SharedImageProvider()
    sharedImageProvider.setSharedImage(byteArrayOf(0x01, 0x02, 0x03))
    val viewModel = makeViewModel(sharedImageProvider = sharedImageProvider)

    assertNotNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `when pending shared image has valid header, cropMode is set to NewImage (re-encode deferred to after crop)`() =
    runTest {
      val sharedImageProvider = SharedImageProvider()
      // Bytes that pass isValidImage (JPEG magic numbers) — re-encoding is deferred until after
      // crop.
      val fakeJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + ByteArray(100)
      sharedImageProvider.setSharedImage(fakeJpeg)
      val viewModel = makeViewModel(sharedImageProvider = sharedImageProvider)

      assertNotNull(viewModel.uiState.value.cropMode)
      assertNull(viewModel.uiState.value.errorMessage)
    }

  @Test
  fun `when pending shared image is valid, cropMode is set to NewImage in uiState`() = runTest {
    val sharedImageProvider = SharedImageProvider()
    sharedImageProvider.setSharedImage(minimalPng)
    val viewModel = makeViewModel(sharedImageProvider = sharedImageProvider)
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.cropMode)
    assertNull(viewModel.uiState.value.editedEvent?.flyerImageBytes)
  }

  @Test
  fun `onImageSelected with invalid bytes sets errorMessage`() = runTest {
    val file: PlatformFile = mock(MockMode.autoUnit)
    everySuspend { file.readBytes() } returns byteArrayOf(0x00, 0x01, 0x02)
    val viewModel = makeViewModel()

    viewModel.onImageSelected(file)
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMessage)
    assertNull(viewModel.uiState.value.cropMode)
  }

  @Test
  fun `onImageSelected with valid image sets cropMode to NewImage`() = runTest {
    val file: PlatformFile = mock(MockMode.autoUnit)
    everySuspend { file.readBytes() } returns minimalPng
    val viewModel = makeViewModel()

    viewModel.onImageSelected(file)
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.cropMode)
    assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `onCropDone re-encodes, moves image to flyerImageBytes, and clears cropMode`() = runTest {
    val sharedImageProvider = SharedImageProvider()
    sharedImageProvider.setSharedImage(minimalPng)
    val viewModel =
      makeViewModel(sharedImageProvider = sharedImageProvider, encodeImage = { bytes, _ -> bytes })
    advanceUntilIdle()

    viewModel.onCropDone(ImageBytes(minimalPng))
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.cropMode)
    assertNotNull(viewModel.uiState.value.editedEvent?.flyerImageBytes)
  }

  @Test
  fun `onCropDone sets errorMessage when re-encode fails`() = runTest {
    val sharedImageProvider = SharedImageProvider()
    sharedImageProvider.setSharedImage(minimalPng)
    val viewModel =
      makeViewModel(sharedImageProvider = sharedImageProvider, encodeImage = { _, _ -> null })
    advanceUntilIdle()

    viewModel.onCropDone(ImageBytes(minimalPng))
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMessage)
    assertNull(viewModel.uiState.value.editedEvent?.flyerImageBytes)
  }

  @Test
  fun `onCropCancelled clears cropMode and errorMessage`() = runTest {
    val file: PlatformFile = mock(MockMode.autoUnit)
    everySuspend { file.readBytes() } returns minimalPng
    val viewModel = makeViewModel()
    viewModel.onImageSelected(file)
    advanceUntilIdle()

    viewModel.onCropCancelled()

    assertNull(viewModel.uiState.value.cropMode)
    assertNull(viewModel.uiState.value.errorMessage)
  }
}
