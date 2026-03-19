package com.hologrampacific.flyergoblin.db

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfile
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudShow
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfile
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
  fun `SoundCloudInfoAdapter encodes and decodes SoundCloudInfo with profileChosen true`() {
    val info = SoundCloudInfo(profileChosen = true)
    assertEquals(info, SoundCloudInfoAdapter.decode(SoundCloudInfoAdapter.encode(info)))
  }

  @Test
  fun `SoundCloudInfoAdapter decodes old JSON without profileChosen field as false`() {
    // Simulate old serialized data that doesn't include profileChosen
    val oldJson = """{"profile":null}"""
    val decoded = SoundCloudInfoAdapter.decode(oldJson)
    assertEquals(false, decoded.profileChosen)
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
          )
      )
    assertEquals(info, SoundCloudInfoAdapter.decode(SoundCloudInfoAdapter.encode(info)))
  }

  @Test
  fun `SoundCloudInfoAdapter encodes and decodes SoundCloudInfo with lastUpdated`() {
    val info =
      SoundCloudInfo(
        profile =
          SoundCloudProfile(
            id = 96064L,
            username = "testuser",
            profileUrl = "https://soundcloud.com/testuser",
            lastUpdated = Instant.fromEpochMilliseconds(1706832000000),
          )
      )
    assertEquals(info, SoundCloudInfoAdapter.decode(SoundCloudInfoAdapter.encode(info)))
  }

  @Test
  fun `MixcloudInfoAdapter encodes and decodes null MixcloudInfo`() {
    val info = MixcloudInfo()
    assertEquals(info, MixcloudInfoAdapter.decode(MixcloudInfoAdapter.encode(info)))
  }

  @Test
  fun `MixcloudInfoAdapter encodes and decodes fully populated MixcloudInfo`() {
    val info =
      MixcloudInfo(
        profile =
          MixcloudProfile(
            key = "/testuser/",
            username = "testuser",
            profileUrl = "https://www.mixcloud.com/testuser/",
            followerCount = 50,
            cloudcastCount = 10,
            city = "London",
            countryCode = "GB",
            name = "Test User",
            shows =
              listOf(
                MixcloudShow(
                  key = "/testuser/show1/",
                  name = "Show 1",
                  url = "https://www.mixcloud.com/testuser/show1/",
                )
              ),
            lastUpdated = Instant.fromEpochMilliseconds(1706832000000),
          ),
        profileChosen = true,
      )
    assertEquals(info, MixcloudInfoAdapter.decode(MixcloudInfoAdapter.encode(info)))
  }

  @Test
  fun `MixcloudInfoAdapter encodes and decodes MixcloudInfo with lastUpdated`() {
    val info =
      MixcloudInfo(
        profile =
          MixcloudProfile(
            key = "/testuser/",
            username = "testuser",
            profileUrl = "https://www.mixcloud.com/testuser/",
            lastUpdated = Instant.fromEpochSeconds(1706832000L, 123456789),
          )
      )
    assertEquals(info, MixcloudInfoAdapter.decode(MixcloudInfoAdapter.encode(info)))
  }
}
