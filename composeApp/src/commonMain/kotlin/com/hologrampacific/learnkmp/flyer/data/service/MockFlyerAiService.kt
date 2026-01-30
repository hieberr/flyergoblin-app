package com.hologrampacific.learnkmp.flyer.data.service

import com.hologrampacific.learnkmp.flyer.domain.model.Event
import com.hologrampacific.learnkmp.flyer.domain.service.AiProcessingResult
import com.hologrampacific.learnkmp.flyer.domain.service.FlyerAiService
import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.coroutines.delay

class MockFlyerAiService : FlyerAiService {
  override suspend fun processFlyer(imageBytes: ByteArray): AiProcessingResult {
    delay(2000)

    val success = Random.nextFloat() < 0.8f

    return if (success) {
      val randomMonth = Random.nextInt(3, 9)
      val randomDay = Random.nextInt(1, 28)

      val event =
        Event(
          id = Event.generateId(),
          name = generateRandomEventName(),
          startDate = kotlinx.datetime.LocalDate(2026, randomMonth, randomDay),
          startTime = kotlinx.datetime.LocalTime(Random.nextInt(18, 23), Random.nextInt(0, 60)),
          venue = generateRandomVenue(),
          eventUrl = "https://example.com/event-${Random.nextInt(1000, 9999)}",
          artists = generateRandomArtists(),
          dateAdded = Clock.System.now(),
        )
      AiProcessingResult.Success(event)
    } else {
      AiProcessingResult.Error("Failed to process flyer. Please try again with a clearer image.")
    }
  }

  private fun generateRandomEventName(): String {
    val prefixes =
      listOf("Spring", "Summer", "Autumn", "Winter", "Night", "Day", "Sunset", "Midnight")
    val types =
      listOf("Music Festival", "Concert", "Jam Session", "Live Show", "Performance", "Showcase")
    return "${prefixes.random()} ${types.random()}"
  }

  private fun generateRandomVenue(): String {
    val venues =
      listOf(
        "The Blue Room",
        "Downtown Arena",
        "City Hall",
        "The Underground",
        "Skyline Theater",
        "Riverside Stage",
      )
    return venues.random()
  }

  private fun generateRandomArtists(): List<String> {
    val artists =
      listOf(
        "The Rockers",
        "DJ Spin",
        "Electric Dreams",
        "Sarah & The Band",
        "Jazz Masters",
        "Acoustic Soul",
        "Beat Collective",
        "Neon Nights",
      )
    return artists.shuffled().take(Random.nextInt(2, 5))
  }
}
