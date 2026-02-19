package com.hologrampacific.flyergoblin.db

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfile
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileSearchResults
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudTrack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class ColumnAdaptersTest : AppTest() {

  @Test
  fun `LocalDateAdapter encodes and decodes round-trip`() {
    val date = LocalDate(2026, 3, 15)
    assertEquals(date, LocalDateAdapter.decode(LocalDateAdapter.encode(date)))
  }

  @Test
  fun `LocalDateAdapter encodes as ISO string`() {
    assertEquals("2026-03-15", LocalDateAdapter.encode(LocalDate(2026, 3, 15)))
  }

  @Test
  fun `LocalTimeAdapter encodes and decodes round-trip`() {
    val time = LocalTime(20, 30, 0)
    assertEquals(time, LocalTimeAdapter.decode(LocalTimeAdapter.encode(time)))
  }

  @Test
  fun `LocalTimeAdapter encodes and decodes round-trip with seconds`() {
    val time = LocalTime(20, 30, 45)
    assertEquals(time, LocalTimeAdapter.decode(LocalTimeAdapter.encode(time)))
  }

  @Test
  fun `InstantColumnAdapter encodes and decodes round-trip`() {
    val instant = Instant.fromEpochMilliseconds(1706832000000)
    assertEquals(instant, InstantColumnAdapter.decode(InstantColumnAdapter.encode(instant)))
  }

  @Test
  fun `InstantColumnAdapter round-trip preserves nanosecond precision`() {
    val instant = Instant.fromEpochSeconds(1706832000L, 123456789)
    assertEquals(instant, InstantColumnAdapter.decode(InstantColumnAdapter.encode(instant)))
  }

  @Test
  fun `StringListAdapter encodes and decodes empty list`() {
    val list = emptyList<String>()
    assertEquals(list, StringListAdapter.decode(StringListAdapter.encode(list)))
  }

  @Test
  fun `StringListAdapter encodes and decodes list with multiple items`() {
    val list = listOf("Artist 1", "Artist 2", "Artist 3")
    assertEquals(list, StringListAdapter.decode(StringListAdapter.encode(list)))
  }

  @Test
  fun `StringListAdapter handles strings with special characters`() {
    val list = listOf("DJ \"Quote\"", "Band & Roll", "Über Artist")
    assertEquals(list, StringListAdapter.decode(StringListAdapter.encode(list)))
  }

  @Test
  fun `SoundCloudInfoAdapter encodes and decodes null soundCloudInfo`() {
    val info = SoundCloudInfo()
    assertEquals(info, SoundCloudInfoAdapter.decode(SoundCloudInfoAdapter.encode(info)))
  }

  @Test
  fun `SoundCloudInfoAdapter encodes and decodes fully populated SoundCloudInfo`() {
    val info =
      SoundCloudInfo(
        profile =
          SoundCloudProfile(
            id = 96064L,
            username = "testuser",
            profileUrl = "https://soundcloud.com/testuser",
            followersCount = 100,
            trackCount = 20,
            city = "Berlin",
            countryCode = "DE",
            avatarUrl = "https://i1.sndcdn.com/avatars-000051966075-igrx67-large.jpg",
            fullName = "Test User Full Name",
            tracks =
              listOf(
                SoundCloudTrack(id = 1L, title = "Track 1", url = "https://soundcloud.com/track1"),
                SoundCloudTrack(id = 2L, title = "Track 2", url = "https://soundcloud.com/track2"),
              ),
          ),
        profileSearchResults =
          SoundCloudProfileSearchResults(
            results =
              listOf(
                SoundCloudProfileInfo(
                  id = 1L,
                  username = "user1",
                  profileUrl = "https://soundcloud.com/user1",
                  avatarUrl = "https://i1.sndcdn.com/avatars-111111111111-large.jpg",
                  fullName = "User One",
                ),
                SoundCloudProfileInfo(
                  id = 2L,
                  username = "user2",
                  profileUrl = "https://soundcloud.com/user2",
                ),
              ),
            lastUpdated = Instant.fromEpochMilliseconds(1706832000000),
          ),
      )
    assertEquals(info, SoundCloudInfoAdapter.decode(SoundCloudInfoAdapter.encode(info)))
  }
}
