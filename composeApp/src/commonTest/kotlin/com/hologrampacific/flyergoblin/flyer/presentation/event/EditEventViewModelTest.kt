package com.hologrampacific.flyergoblin.flyer.presentation.event

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerExtractionResult
import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerProcessingDataSource
import com.hologrampacific.flyergoblin.flyer.domain.repository.EventRepository
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ProcessFlyerUseCase
import com.hologrampacific.flyergoblin.sharing.SharedImageProvider
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

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
  ): EditEventViewModel {
    val repository: EventRepository = mock(MockMode.autoUnit)
    val dataSource: FlyerProcessingDataSource = mock(MockMode.autoUnit)
    stubDataSource(dataSource)
    return EditEventViewModel(
      eventId,
      repository,
      ProcessFlyerUseCase(dataSource),
      sharedImageProvider,
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
  fun `when pending shared image has valid header but cannot be decoded, errorMessage is set`() =
    runTest {
      val sharedImageProvider = SharedImageProvider()
      // Bytes that pass isValidImage (JPEG magic numbers) but fail reencodeImageToFitSize.
      val fakeJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + ByteArray(100)
      sharedImageProvider.setSharedImage(fakeJpeg)
      val viewModel = makeViewModel(sharedImageProvider = sharedImageProvider)

      assertNotNull(viewModel.uiState.value.errorMessage)
    }

  @Test
  fun `when pending shared image is valid, flyerImageBytes is populated in uiState`() = runTest {
    val sharedImageProvider = SharedImageProvider()
    sharedImageProvider.setSharedImage(minimalPng)
    // Stub the data source so processFlyer() completes without hitting the network.
    val viewModel =
      makeViewModel(
        sharedImageProvider = sharedImageProvider,
        stubDataSource = { dataSource ->
          everySuspend { dataSource.extractEventFromFlyer(any(), any()) } returns
            FlyerExtractionResult.Error("stubbed")
        },
      )

    assertNotNull(viewModel.uiState.value.editedEvent?.flyerImageBytes)
  }
}
