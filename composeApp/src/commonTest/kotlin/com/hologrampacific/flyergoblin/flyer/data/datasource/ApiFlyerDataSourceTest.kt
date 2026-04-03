package com.hologrampacific.flyergoblin.flyer.data.datasource

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.data.remote.FlyerApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.FlyerApiResponse
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
}
