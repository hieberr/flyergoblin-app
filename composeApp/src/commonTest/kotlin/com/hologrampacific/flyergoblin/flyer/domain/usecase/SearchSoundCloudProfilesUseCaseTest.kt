package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistResearchDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class SearchSoundCloudProfilesUseCaseTest : AppTest() {

  private lateinit var cache: ProfileSearchCache
  private lateinit var dataSource: ArtistResearchDataSource
  private lateinit var useCase: SearchSoundCloudProfilesUseCase

  override fun suppressLogs() {
    super.suppressLogs()
    cache = ProfileSearchCache()
    dataSource = mock()
    useCase = SearchSoundCloudProfilesUseCase(dataSource, cache)
  }

  private fun profile(username: String, followersCount: Int? = null, fullName: String? = null) =
    SoundCloudProfileInfo(
      id = username.hashCode().toLong(),
      username = username,
      profileUrl = "https://soundcloud.com/$username",
      followersCount = followersCount,
      fullName = fullName,
    )

  @Test
  fun `test invoke stores empty list in cache on Success with no profiles`() = runTest {
    everySuspend { dataSource.searchSoundCloudProfiles("DJ Artist") } returns
      ArtistProfileSearchResult.Success(emptyList())

    val result = useCase("DJ Artist")

    assertEquals(ResultWithRateLimit.Success, result)
    assertEquals(emptyList(), cache.getSoundCloudResults("DJ Artist"))
  }

  @Test
  fun `test invoke stores results in cache`() = runTest {
    val profiles = listOf(profile("artist1", 100), profile("artist2", 200))
    everySuspend { dataSource.searchSoundCloudProfiles("Artist One") } returns
      ArtistProfileSearchResult.Success(profiles)

    useCase("Artist One")

    val cached = cache.getSoundCloudResults("Artist One")
    assertEquals(2, cached?.size)
  }

  @Test
  fun `test invoke exact username match is moved to top`() = runTest {
    val exactMatch = profile("DJ Artist", followersCount = 50)
    val highFollowerOther = profile("other_dj", followersCount = 10000)
    everySuspend { dataSource.searchSoundCloudProfiles("DJ Artist") } returns
      ArtistProfileSearchResult.Success(listOf(highFollowerOther, exactMatch))

    useCase("DJ Artist")

    val cached = cache.getSoundCloudResults("DJ Artist")!!
    assertEquals(exactMatch, cached[0])
    assertEquals(highFollowerOther, cached[1])
  }

  @Test
  fun `test invoke exact username match is case insensitive`() = runTest {
    val exactMatch = profile("dj artist", followersCount = 50)
    val other = profile("other_dj", followersCount = 10000)
    everySuspend { dataSource.searchSoundCloudProfiles("DJ Artist") } returns
      ArtistProfileSearchResult.Success(listOf(other, exactMatch))

    useCase("DJ Artist")

    val cached = cache.getSoundCloudResults("DJ Artist")!!
    assertEquals(exactMatch, cached[0])
  }

  @Test
  fun `test invoke exact fullName match is moved to top`() = runTest {
    val exactMatch = profile("some_username", followersCount = 50, fullName = "DJ Artist")
    val highFollowerOther = profile("other_dj", followersCount = 10000)
    everySuspend { dataSource.searchSoundCloudProfiles("DJ Artist") } returns
      ArtistProfileSearchResult.Success(listOf(highFollowerOther, exactMatch))

    useCase("DJ Artist")

    val cached = cache.getSoundCloudResults("DJ Artist")!!
    assertEquals(exactMatch, cached[0])
    assertEquals(highFollowerOther, cached[1])
  }

  @Test
  fun `test invoke exact fullName match is case insensitive`() = runTest {
    val exactMatch = profile("some_username", followersCount = 50, fullName = "dj artist")
    val other = profile("other_dj", followersCount = 10000)
    everySuspend { dataSource.searchSoundCloudProfiles("DJ Artist") } returns
      ArtistProfileSearchResult.Success(listOf(other, exactMatch))

    useCase("DJ Artist")

    val cached = cache.getSoundCloudResults("DJ Artist")!!
    assertEquals(exactMatch, cached[0])
  }

  @Test
  fun `test invoke profile with null fullName is not treated as exact match via fullName`() = runTest {
    val nullFullName = profile("some_username", followersCount = 50, fullName = null)
    val other = profile("other_dj", followersCount = 10000)
    everySuspend { dataSource.searchSoundCloudProfiles("DJ Artist") } returns
      ArtistProfileSearchResult.Success(listOf(nullFullName, other))

    useCase("DJ Artist")

    val cached = cache.getSoundCloudResults("DJ Artist")!!
    assertEquals(other, cached[0])
    assertEquals(nullFullName, cached[1])
  }

  @Test
  fun `test invoke non-matching profiles are sorted by follower count descending`() = runTest {
    val low = profile("low_artist", followersCount = 100)
    val high = profile("high_artist", followersCount = 9000)
    val mid = profile("mid_artist", followersCount = 500)
    everySuspend { dataSource.searchSoundCloudProfiles("DJ Artist") } returns
      ArtistProfileSearchResult.Success(listOf(low, high, mid))

    useCase("DJ Artist")

    val cached = cache.getSoundCloudResults("DJ Artist")!!
    assertEquals(high, cached[0])
    assertEquals(mid, cached[1])
    assertEquals(low, cached[2])
  }

  @Test
  fun `test invoke exact match comes before higher-follower non-matching profiles`() = runTest {
    val exactMatch = profile("DJ Artist", followersCount = 10)
    val highFollower = profile("popular_dj", followersCount = 100000)
    val midFollower = profile("mid_dj", followersCount = 5000)
    everySuspend { dataSource.searchSoundCloudProfiles("DJ Artist") } returns
      ArtistProfileSearchResult.Success(listOf(highFollower, midFollower, exactMatch))

    useCase("DJ Artist")

    val cached = cache.getSoundCloudResults("DJ Artist")!!
    assertEquals(exactMatch, cached[0])
    assertEquals(highFollower, cached[1])
    assertEquals(midFollower, cached[2])
  }

  @Test
  fun `test invoke returns Error on datasource error`() = runTest {
    everySuspend { dataSource.searchSoundCloudProfiles("DJ Artist") } returns
      ArtistProfileSearchResult.Error("Network error")

    val result = useCase("DJ Artist")

    assertEquals(ResultWithRateLimit.Error("Network error"), result)
  }

  @Test
  fun `test invoke trims whitespace before searching and caching`() = runTest {
    val exactMatch = profile("DJ Artist", followersCount = 100, fullName = "DJ Artist")
    everySuspend { dataSource.searchSoundCloudProfiles("DJ Artist") } returns
      ArtistProfileSearchResult.Success(listOf(exactMatch))

    useCase("  DJ Artist  ")

    val cached = cache.getSoundCloudResults("DJ Artist")
    assertEquals(1, cached?.size)
  }

  @Test
  fun `test invoke returns RateLimited on datasource rate limit`() = runTest {
    val blockedUntil = Instant.parse("2026-06-01T00:00:00Z")
    everySuspend { dataSource.searchSoundCloudProfiles("DJ Artist") } returns
      ArtistProfileSearchResult.RateLimited(blockedUntil)

    val result = useCase("DJ Artist")

    assertEquals(ResultWithRateLimit.RateLimited(blockedUntil), result)
  }
}