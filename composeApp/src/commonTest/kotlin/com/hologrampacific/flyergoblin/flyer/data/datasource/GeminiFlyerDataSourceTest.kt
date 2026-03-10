package com.hologrampacific.flyergoblin.flyer.data.datasource

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.data.remote.Candidate
import com.hologrampacific.flyergoblin.flyer.data.remote.CandidateContent
import com.hologrampacific.flyergoblin.flyer.data.remote.GeminiApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.GeminiResponse
import com.hologrampacific.flyergoblin.flyer.data.remote.TextPart
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
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class GeminiFlyerDataSourceTest : AppTest() {

  // Fixed to 2026-03-10 noon UTC so date-resolution tests are deterministic regardless of when they
  // run.
  private val fixedClock =
    object : Clock {
      override fun now(): Instant = Instant.parse("2026-03-10T12:00:00Z")
    }

  private val mockClient = mock<GeminiApiClient>()
  private val dataSource = GeminiFlyerDataSource(mockClient, fixedClock)

  private fun geminiResponse(json: String): GeminiResponse =
    GeminiResponse(
      candidates =
        listOf(
          Candidate(
            content = CandidateContent(parts = listOf(TextPart(json)), role = "model"),
            finishReason = "STOP",
          )
        )
    )

  private suspend fun extract(json: String): FlyerExtractionResult {
    everySuspend { mockClient.llmPromptWithImage(any(), any(), any(), any(), any(), any()) } returns
      geminiResponse(json)
    return dataSource.extractEventFromFlyer("base64", "image/jpeg")
  }

  // ----- year present -----

  @Test
  fun `test extractEventFromFlyer uses explicit startYear when provided`() = runTest {
    val result =
      extract("""{"name":"Festival","startMonth":6,"startDay":15,"startYear":2027,"artists":[]}""")

    assertIs<FlyerExtractionResult.Success>(result)
    assertEquals(LocalDate(2027, 6, 15), result.data.startDate)
  }

  // ----- year absent, date not yet passed -----

  @Test
  fun `test extractEventFromFlyer resolveStartDate uses current year when day and month not yet passed`() =
    runTest {
      // fixedClock = 2026-03-10. December 31 has not yet passed this year.
      val result = extract("""{"name":"NYE","startMonth":12,"startDay":31,"artists":[]}""")

      assertIs<FlyerExtractionResult.Success>(result)
      assertEquals(LocalDate(2026, 12, 31), result.data.startDate)
    }

  // ----- year absent, date already passed -----

  @Test
  fun `test extractEventFromFlyer resolveStartDate uses next year when day and month already passed`() =
    runTest {
      // fixedClock = 2026-03-10. January 1 has already passed this year → should resolve to 2027.
      val result = extract("""{"name":"New Year","startMonth":1,"startDay":1,"artists":[]}""")

      assertIs<FlyerExtractionResult.Success>(result)
      assertEquals(LocalDate(2027, 1, 1), result.data.startDate)
    }

  // ----- year absent, date is today -----

  @Test
  fun `test extractEventFromFlyer resolveStartDate uses current year when day and month is today`() =
    runTest {
      // fixedClock = 2026-03-10. An event on March 10 should not be bumped to next year.
      val result = extract("""{"name":"Tonight","startMonth":3,"startDay":10,"artists":[]}""")

      assertIs<FlyerExtractionResult.Success>(result)
      assertEquals(LocalDate(2026, 3, 10), result.data.startDate)
    }

  // ----- missing month or day -----

  @Test
  fun `test extractEventFromFlyer startDate is null when startMonth is missing`() = runTest {
    val result = extract("""{"name":"Mystery","startDay":15,"artists":[]}""")

    assertIs<FlyerExtractionResult.Success>(result)
    assertNull(result.data.startDate)
  }

  @Test
  fun `test extractEventFromFlyer startDate is null when startDay is missing`() = runTest {
    val result = extract("""{"name":"Mystery","startMonth":6,"artists":[]}""")

    assertIs<FlyerExtractionResult.Success>(result)
    assertNull(result.data.startDate)
  }

  @Test
  fun `test extractEventFromFlyer startDate is null when startMonth and startDay are both missing`() =
    runTest {
      val result = extract("""{"name":"Mystery","artists":[]}""")

      assertIs<FlyerExtractionResult.Success>(result)
      assertNull(result.data.startDate)
    }

  // ----- invalid date values -----

  @Test
  fun `test extractEventFromFlyer startDate is null when explicit startYear produces invalid date`() =
    runTest {
      val result =
        extract(
          """{"name":"Bad Date","startMonth":2,"startDay":30,"startYear":2026,"artists":[]}"""
        )

      assertIs<FlyerExtractionResult.Success>(result)
      assertNull(result.data.startDate)
    }

  @Test
  fun `test extractEventFromFlyer startDate is null when day and month produce invalid date`() =
    runTest {
      // February 30 does not exist.
      val result = extract("""{"name":"Bad Date","startMonth":2,"startDay":30,"artists":[]}""")

      assertIs<FlyerExtractionResult.Success>(result)
      assertNull(result.data.startDate)
    }

  // ----- other fields -----

  @Test
  fun `test extractEventFromFlyer extracts all fields correctly`() = runTest {
    val result =
      extract(
        """
        {
          "name": "Summer Beats",
          "startMonth": 7,
          "startDay": 4,
          "startYear": 2026,
          "startTime": "21:00",
          "venue": "The Venue",
          "eventUrl": "https://example.com",
          "artists": ["DJ Alpha", "MC Beta"]
        }
        """
          .trimIndent()
      )

    assertIs<FlyerExtractionResult.Success>(result)
    with(result.data) {
      assertEquals("Summer Beats", name)
      assertEquals(LocalDate(2026, 7, 4), startDate)
      assertEquals(21, startTime?.hour)
      assertEquals(0, startTime?.minute)
      assertEquals("The Venue", venue)
      assertEquals("https://example.com", eventUrl)
      assertEquals(listOf("DJ Alpha", "MC Beta"), artists)
    }
  }

  // ----- API errors -----

  @Test
  fun `test extractEventFromFlyer returns Error when API throws exception`() = runTest {
    everySuspend { mockClient.llmPromptWithImage(any(), any(), any(), any(), any(), any()) } throws
      RuntimeException("Network error")

    val result = dataSource.extractEventFromFlyer("base64", "image/jpeg")

    assertIs<FlyerExtractionResult.Error>(result)
  }

  @Test
  fun `test extractEventFromFlyer returns Error when response has no candidates`() = runTest {
    everySuspend { mockClient.llmPromptWithImage(any(), any(), any(), any(), any(), any()) } returns
      GeminiResponse(candidates = emptyList())

    val result = dataSource.extractEventFromFlyer("base64", "image/jpeg")

    assertIs<FlyerExtractionResult.Error>(result)
  }

  @Test
  fun `test extractEventFromFlyer returns Error when response JSON is invalid`() = runTest {
    val result = extract("this is not json")

    assertIs<FlyerExtractionResult.Error>(result)
  }
}
