package com.hologrampacific.flyergoblin.flyer.data.datasource

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.data.remote.FlyerApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.FlyerApiErrorCode
import com.hologrampacific.flyergoblin.flyer.data.remote.FlyerApiException
import com.hologrampacific.flyergoblin.flyer.data.remote.FlyerApiResponse
import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerExtractionErrorType
import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerExtractionResult
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class ApiFlyerDataSourceTest : AppTest() {

  private val mockClient = mock<FlyerApiClient>()
  private val dataSource = ApiFlyerDataSource(mockClient)

  private suspend fun extract(response: FlyerApiResponse): FlyerExtractionResult {
    everySuspend { mockClient.processFlyer(any(), any()) } returns response
    return dataSource.extractEventFromFlyer("base64", "image/jpeg")
  }

  @Test
  fun `test extractEventFromFlyer maps all fields correctly`() = runTest {
    val result =
      extract(
        FlyerApiResponse(
          name = "Summer Beats",
          startDate = "2026-07-04",
          startTime = "21:00",
          venue = "The Venue",
          eventUrl = "https://example.com",
          artists = listOf("DJ Alpha", "MC Beta"),
        )
      )

    assertIs<FlyerExtractionResult.Success>(result)
    with(result.data) {
      assertEquals("Summer Beats", name)
      assertEquals(LocalDate(2026, 7, 4), startDate)
      assertEquals(LocalTime(21, 0), startTime)
      assertEquals("The Venue", venue)
      assertEquals("https://example.com", eventUrl)
      assertEquals(listOf("DJ Alpha", "MC Beta"), artists)
    }
  }

  @Test
  fun `test extractEventFromFlyer handles null date and time`() = runTest {
    val result = extract(FlyerApiResponse(name = "Mystery Event", artists = listOf("Artist")))

    assertIs<FlyerExtractionResult.Success>(result)
    assertNull(result.data.startDate)
    assertNull(result.data.startTime)
  }

  @Test
  fun `test extractEventFromFlyer handles invalid date string gracefully`() = runTest {
    val result =
      extract(
        FlyerApiResponse(name = "Bad Date Event", startDate = "not-a-date", artists = emptyList())
      )

    assertIs<FlyerExtractionResult.Success>(result)
    assertNull(result.data.startDate)
  }

  @Test
  fun `test extractEventFromFlyer handles invalid time string gracefully`() = runTest {
    val result =
      extract(
        FlyerApiResponse(name = "Bad Time Event", startTime = "not-a-time", artists = emptyList())
      )

    assertIs<FlyerExtractionResult.Success>(result)
    assertNull(result.data.startTime)
  }

  @Test
  fun `test extractEventFromFlyer uses Unknown when name is blank`() = runTest {
    val result = extract(FlyerApiResponse(name = "", artists = emptyList()))

    assertIs<FlyerExtractionResult.Success>(result)
    assertEquals("Unknown", result.data.name)
  }

  @Test
  fun `test extractEventFromFlyer returns Error when API throws exception`() = runTest {
    everySuspend { mockClient.processFlyer(any(), any()) } throws RuntimeException("Network error")

    val result = dataSource.extractEventFromFlyer("base64", "image/jpeg")

    assertIs<FlyerExtractionResult.Error>(result)
  }

  @Test
  fun `test extractEventFromFlyer maps FlyerApiException TIMEOUT code to TIMEOUT error type`() =
    runTest {
      everySuspend { mockClient.processFlyer(any(), any()) } throws
        FlyerApiException(FlyerApiErrorCode.TIMEOUT, "The flyer took too long to process.", 504)

      val result = dataSource.extractEventFromFlyer("base64", "image/jpeg")

      assertIs<FlyerExtractionResult.Error>(result)
      assertEquals("The flyer took too long to process.", result.message)
      assertEquals(FlyerExtractionErrorType.TIMEOUT, result.type)
    }

  @Test
  fun `test extractEventFromFlyer maps FlyerApiException UPSTREAM_RATE_LIMITED code to UPSTREAM_RATE_LIMITED error type`() =
    runTest {
      everySuspend { mockClient.processFlyer(any(), any()) } throws
        FlyerApiException(FlyerApiErrorCode.UPSTREAM_RATE_LIMITED, "Too many requests.", 429)

      val result = dataSource.extractEventFromFlyer("base64", "image/jpeg")

      assertIs<FlyerExtractionResult.Error>(result)
      assertEquals(FlyerExtractionErrorType.UPSTREAM_RATE_LIMITED, result.type)
    }

  @Test
  fun `test extractEventFromFlyer maps FlyerApiException UPSTREAM_ERROR and INTERNAL_ERROR codes to SERVER_ERROR error type`() =
    runTest {
      everySuspend { mockClient.processFlyer(any(), any()) } throws
        FlyerApiException(FlyerApiErrorCode.UPSTREAM_ERROR, "Upstream error.", 502)

      val result = dataSource.extractEventFromFlyer("base64", "image/jpeg")

      assertIs<FlyerExtractionResult.Error>(result)
      assertEquals(FlyerExtractionErrorType.SERVER_ERROR, result.type)
    }

  @Test
  fun `test extractEventFromFlyer maps FlyerApiException UNKNOWN code to UNKNOWN error type`() =
    runTest {
      everySuspend { mockClient.processFlyer(any(), any()) } throws
        FlyerApiException(FlyerApiErrorCode.UNKNOWN, "Something went wrong.", 500)

      val result = dataSource.extractEventFromFlyer("base64", "image/jpeg")

      assertIs<FlyerExtractionResult.Error>(result)
      assertEquals(FlyerExtractionErrorType.UNKNOWN, result.type)
    }
}
