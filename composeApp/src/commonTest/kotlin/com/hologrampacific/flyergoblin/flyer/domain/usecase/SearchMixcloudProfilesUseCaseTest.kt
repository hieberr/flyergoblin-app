package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfileInfo
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class SearchMixcloudProfilesUseCaseTest : AppTest() {

  private lateinit var cache: ProfileSearchCache
  private lateinit var dataSource: MixcloudDataSource
  private lateinit var useCase: SearchMixcloudProfilesUseCase

  override fun suppressLogs() {
    super.suppressLogs()
    cache = ProfileSearchCache()
    dataSource = mock()
    useCase = SearchMixcloudProfilesUseCase(dataSource, cache)
  }

  private fun profile(username: String, name: String?, followerCount: Int? = null) =
    MixcloudProfileInfo(
      key = "/$username/",
      username = username,
      profileUrl = "https://mixcloud.com/$username",
      name = name,
      followerCount = followerCount,
    )

  @Test
  fun `test invoke stores results in cache`() = runTest {
    val profiles = listOf(profile("artist1", "Artist One"), profile("artist2", "Artist Two"))
    everySuspend { dataSource.searchMixcloudProfiles("Artist One") } returns
      MixcloudProfileSearchResult.Success(profiles)

    useCase("Artist One")

    val cached = cache.getMixcloudResults("Artist One")
    assertEquals(2, cached?.size)
  }

  @Test
  fun `test invoke exact name match is moved to top`() = runTest {
    val exactMatch = profile("artist1", "DJ Artist")
    val otherProfile = profile("other", "Some Other DJ")
    everySuspend { dataSource.searchMixcloudProfiles("DJ Artist") } returns
      MixcloudProfileSearchResult.Success(listOf(otherProfile, exactMatch))

    useCase("DJ Artist")

    val cached = cache.getMixcloudResults("DJ Artist")!!
    assertEquals(exactMatch, cached[0])
    assertEquals(otherProfile, cached[1])
  }

  @Test
  fun `test invoke exact username match is moved to top`() = runTest {
    val exactMatch = profile("DJ Artist", "Some Display Name")
    val otherProfile = profile("other", "Some Other DJ")
    everySuspend { dataSource.searchMixcloudProfiles("DJ Artist") } returns
      MixcloudProfileSearchResult.Success(listOf(otherProfile, exactMatch))

    useCase("DJ Artist")

    val cached = cache.getMixcloudResults("DJ Artist")!!
    assertEquals(exactMatch, cached[0])
    assertEquals(otherProfile, cached[1])
  }

  @Test
  fun `test invoke exact name match is case insensitive`() = runTest {
    val exactMatch = profile("artist1", "dj artist")
    val otherProfile = profile("other", "Some Other DJ")
    everySuspend { dataSource.searchMixcloudProfiles("DJ Artist") } returns
      MixcloudProfileSearchResult.Success(listOf(otherProfile, exactMatch))

    useCase("DJ Artist")

    val cached = cache.getMixcloudResults("DJ Artist")!!
    assertEquals(exactMatch, cached[0])
  }

  @Test
  fun `test invoke exact username match is case insensitive`() = runTest {
    val exactMatch = profile("dj artist", "Some Display Name")
    val otherProfile = profile("other", "Some Other DJ")
    everySuspend { dataSource.searchMixcloudProfiles("DJ Artist") } returns
      MixcloudProfileSearchResult.Success(listOf(otherProfile, exactMatch))

    useCase("DJ Artist")

    val cached = cache.getMixcloudResults("DJ Artist")!!
    assertEquals(exactMatch, cached[0])
  }

  @Test
  fun `test invoke multiple exact matches sorted by follower count descending`() = runTest {
    val exactLow = profile("artist_low", "DJ Artist", followerCount = 100)
    val exactHigh = profile("artist_high", "DJ Artist", followerCount = 5000)
    val exactMid = profile("artist_mid", "DJ Artist", followerCount = 500)
    val other = profile("other", "Other Artist")
    everySuspend { dataSource.searchMixcloudProfiles("DJ Artist") } returns
      MixcloudProfileSearchResult.Success(listOf(exactLow, other, exactMid, exactHigh))

    useCase("DJ Artist")

    val cached = cache.getMixcloudResults("DJ Artist")!!
    assertEquals(exactHigh, cached[0])
    assertEquals(exactMid, cached[1])
    assertEquals(exactLow, cached[2])
    assertEquals(other, cached[3])
  }

  @Test
  fun `test invoke non-matching profiles maintain original order`() = runTest {
    val first = profile("first", "Almost DJ Artist")
    val second = profile("second", "DJ Artist Fan")
    val third = profile("third", "Some DJ")
    everySuspend { dataSource.searchMixcloudProfiles("DJ Artist") } returns
      MixcloudProfileSearchResult.Success(listOf(first, second, third))

    useCase("DJ Artist")

    val cached = cache.getMixcloudResults("DJ Artist")!!
    assertEquals(listOf(first, second, third), cached)
  }

  @Test
  fun `test invoke profile with null name is not treated as exact match`() = runTest {
    val nullName = profile("artist1", null)
    val other = profile("other", "DJ Artist")
    everySuspend { dataSource.searchMixcloudProfiles("DJ Artist") } returns
      MixcloudProfileSearchResult.Success(listOf(nullName, other))

    useCase("DJ Artist")

    val cached = cache.getMixcloudResults("DJ Artist")!!
    assertEquals(other, cached[0])
    assertEquals(nullName, cached[1])
  }

  @Test
  fun `test invoke returns Error on datasource error`() = runTest {
    everySuspend { dataSource.searchMixcloudProfiles("DJ Artist") } returns
      MixcloudProfileSearchResult.Error("Network error")

    val result = useCase("DJ Artist")

    assertEquals(ResultWithRateLimit.Error("Network error"), result)
  }

  @Test
  fun `test invoke trims whitespace before searching and caching`() = runTest {
    val exactMatch = profile("DJ Artist", "DJ Artist")
    everySuspend { dataSource.searchMixcloudProfiles("DJ Artist") } returns
      MixcloudProfileSearchResult.Success(listOf(exactMatch))

    useCase("  DJ Artist  ")

    val cached = cache.getMixcloudResults("DJ Artist")
    assertEquals(1, cached?.size)
  }

  @Test
  fun `test invoke returns RateLimited on datasource rate limit`() = runTest {
    val blockedUntil = Instant.parse("2026-06-01T00:00:00Z")
    everySuspend { dataSource.searchMixcloudProfiles("DJ Artist") } returns
      MixcloudProfileSearchResult.RateLimited(blockedUntil)

    val result = useCase("DJ Artist")

    assertEquals(ResultWithRateLimit.RateLimited(blockedUntil), result)
  }
}
