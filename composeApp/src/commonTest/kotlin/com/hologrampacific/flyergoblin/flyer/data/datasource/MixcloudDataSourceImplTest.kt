package com.hologrampacific.flyergoblin.flyer.data.datasource

import com.hologrampacific.flyergoblin.AppTest
import com.hologrampacific.flyergoblin.flyer.data.remote.ApiRateLimitException
import com.hologrampacific.flyergoblin.flyer.data.remote.MixcloudApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.MixcloudApiException
import com.hologrampacific.flyergoblin.flyer.data.remote.MixcloudClientErrorException
import com.hologrampacific.flyergoblin.flyer.data.remote.MixcloudPictures
import com.hologrampacific.flyergoblin.flyer.data.remote.MixcloudServerErrorException
import com.hologrampacific.flyergoblin.flyer.data.remote.MixcloudUserProfile
import com.hologrampacific.flyergoblin.flyer.data.remote.MixcloudUserResult
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudProfileSearchResult
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudShow
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest

class MixcloudDataSourceImplTest : AppTest() {

  // Helper to create a minimal MixcloudUserResult
  private fun userResult(key: String, username: String = key.trim('/')) =
    MixcloudUserResult(
      key = key,
      url = "https://www.mixcloud.com$key",
      name = username,
      username = username,
    )

  // Helper to create a minimal MixcloudUserProfile with shows
  private fun userProfile(key: String, cloudcastCount: Int? = 5, username: String = key.trim('/')) =
    MixcloudUserProfile(
      key = key,
      url = "https://www.mixcloud.com$key",
      name = username,
      username = username,
      cloudcastCount = cloudcastCount,
    )

  // ----- searchMixcloudProfiles validation -----

  @Test
  fun `test searchMixcloudProfiles blank artist name returns Error`() = runTest {
    val mockClient = mock<MixcloudApiClient>()
    val dataSource = MixcloudDataSourceImpl(mockClient)

    val result = dataSource.searchMixcloudProfiles("   ")

    assertIs<MixcloudProfileSearchResult.Error>(result)
    assertEquals("Artist name cannot be empty", result.message)
  }

  @Test
  fun `test searchMixcloudProfiles empty artist name returns Error`() = runTest {
    val mockClient = mock<MixcloudApiClient>()
    val dataSource = MixcloudDataSourceImpl(mockClient)

    val result = dataSource.searchMixcloudProfiles("")

    assertIs<MixcloudProfileSearchResult.Error>(result)
    assertEquals("Artist name cannot be empty", result.message)
  }

  @Test
  fun `test searchMixcloudProfiles artist name too long returns Error`() = runTest {
    val mockClient = mock<MixcloudApiClient>()
    val dataSource = MixcloudDataSourceImpl(mockClient)
    val longName = "a".repeat(201)

    val result = dataSource.searchMixcloudProfiles(longName)

    assertIs<MixcloudProfileSearchResult.Error>(result)
    assertEquals("Artist name is too long", result.message)
  }

  @Test
  fun `test searchMixcloudProfiles trims whitespace before searching`() = runTest {
    val mockClient = mock<MixcloudApiClient>()
    everySuspend { mockClient.searchUsers("Test Artist") } returns emptyList()
    val dataSource = MixcloudDataSourceImpl(mockClient)

    dataSource.searchMixcloudProfiles("  Test Artist  ")

    verifySuspend { mockClient.searchUsers("Test Artist") }
  }

  // ----- empty / no-results cases -----

  @Test
  fun `test searchMixcloudProfiles empty search results returns Error`() = runTest {
    val mockClient = mock<MixcloudApiClient>()
    everySuspend { mockClient.searchUsers("Unknown Artist") } returns emptyList()
    val dataSource = MixcloudDataSourceImpl(mockClient)

    val result = dataSource.searchMixcloudProfiles("Unknown Artist")

    assertIs<MixcloudProfileSearchResult.Error>(result)
    assertEquals("No Mixcloud profile found for \"Unknown Artist\"", result.message)
  }

  @Test
  fun `test searchMixcloudProfiles all profiles have zero cloudcasts returns Error`() = runTest {
    val mockClient = mock<MixcloudApiClient>()
    val userKey = "/artistname/"
    everySuspend { mockClient.searchUsers("Artist") } returns listOf(userResult(userKey))
    everySuspend { mockClient.getUserProfile(userKey) } returns
      userProfile(userKey, cloudcastCount = 0)
    val dataSource = MixcloudDataSourceImpl(mockClient)

    val result = dataSource.searchMixcloudProfiles("Artist")

    assertIs<MixcloudProfileSearchResult.Error>(result)
    assertTrue(result.message.contains("no Mixcloud profiles with shows", ignoreCase = true))
  }

  @Test
  fun `test searchMixcloudProfiles null cloudcastCount is treated as zero and filtered out`() =
    runTest {
      val mockClient = mock<MixcloudApiClient>()
      val userKey = "/artistname/"
      everySuspend { mockClient.searchUsers("Artist") } returns listOf(userResult(userKey))
      everySuspend { mockClient.getUserProfile(userKey) } returns
        userProfile(userKey, cloudcastCount = null)
      val dataSource = MixcloudDataSourceImpl(mockClient)

      val result = dataSource.searchMixcloudProfiles("Artist")

      assertIs<MixcloudProfileSearchResult.Error>(result)
    }

  // ----- success cases -----

  @Test
  fun `test searchMixcloudProfiles success returns profiles with cloudcasts`() = runTest {
    val mockClient = mock<MixcloudApiClient>()
    val userKey = "/djtest/"
    everySuspend { mockClient.searchUsers("DJ Test") } returns
      listOf(userResult(userKey, "DJ Test"))
    everySuspend { mockClient.getUserProfile(userKey) } returns
      userProfile(userKey, cloudcastCount = 10, username = "djtest")
    val dataSource = MixcloudDataSourceImpl(mockClient)

    val result = dataSource.searchMixcloudProfiles("DJ Test")

    assertIs<MixcloudProfileSearchResult.Success>(result)
    assertEquals(1, result.profiles.size)
    assertEquals(userKey, result.profiles.first().key)
    assertEquals("djtest", result.profiles.first().username)
    assertEquals(10, result.profiles.first().cloudcastCount)
  }

  @Test
  fun `test searchMixcloudProfiles maps avatarUrl from large picture`() = runTest {
    val mockClient = mock<MixcloudApiClient>()
    val userKey = "/djtest/"
    val avatarUrl = "https://thumbnailer.mixcloud.com/unsafe/large.jpg"
    everySuspend { mockClient.searchUsers("DJ Test") } returns listOf(userResult(userKey))
    everySuspend { mockClient.getUserProfile(userKey) } returns
      userProfile(userKey).copy(pictures = MixcloudPictures(large = avatarUrl))
    val dataSource = MixcloudDataSourceImpl(mockClient)

    val result = dataSource.searchMixcloudProfiles("DJ Test")

    assertIs<MixcloudProfileSearchResult.Success>(result)
    assertEquals(avatarUrl, result.profiles.first().avatarUrl)
  }

  @Test
  fun `test searchMixcloudProfiles partial failures still return remaining profiles`() = runTest {
    val mockClient = mock<MixcloudApiClient>()
    val key1 = "/artist1/"
    val key2 = "/artist2/"
    everySuspend { mockClient.searchUsers("Artist") } returns
      listOf(userResult(key1), userResult(key2))
    everySuspend { mockClient.getUserProfile(key1) } throws MixcloudApiException("Network error")
    everySuspend { mockClient.getUserProfile(key2) } returns userProfile(key2, cloudcastCount = 3)
    val dataSource = MixcloudDataSourceImpl(mockClient)

    val result = dataSource.searchMixcloudProfiles("Artist")

    assertIs<MixcloudProfileSearchResult.Success>(result)
    assertEquals(1, result.profiles.size)
    assertEquals(key2, result.profiles.first().key)
  }

  // ----- rate limit cases -----

  @Test
  fun `test searchMixcloudProfiles rate limit on searchUsers returns RateLimited`() = runTest {
    val mockClient = mock<MixcloudApiClient>()
    val blockedUntil = Clock.System.now() + 60.seconds
    everySuspend { mockClient.searchUsers("Artist") } throws
      ApiRateLimitException(blockedUntil = blockedUntil, message = "Rate limited")
    val dataSource = MixcloudDataSourceImpl(mockClient)

    val result = dataSource.searchMixcloudProfiles("Artist")

    assertIs<MixcloudProfileSearchResult.RateLimited>(result)
    assertEquals(blockedUntil, result.blockedUntil)
  }

  @Test
  fun `test searchMixcloudProfiles rate limit on getUserProfile propagates`() = runTest {
    val mockClient = mock<MixcloudApiClient>()
    val userKey = "/artist1/"
    val blockedUntil = Clock.System.now() + 30.seconds
    everySuspend { mockClient.searchUsers("Artist") } returns listOf(userResult(userKey))
    everySuspend { mockClient.getUserProfile(userKey) } throws
      ApiRateLimitException(blockedUntil = blockedUntil, message = "Rate limited")
    val dataSource = MixcloudDataSourceImpl(mockClient)

    val result = dataSource.searchMixcloudProfiles("Artist")

    assertIs<MixcloudProfileSearchResult.RateLimited>(result)
    assertEquals(blockedUntil, result.blockedUntil)
  }

  // ----- API error cases -----

  @Test
  fun `test searchMixcloudProfiles server error returns Error`() = runTest {
    val mockClient = mock<MixcloudApiClient>()
    everySuspend { mockClient.searchUsers("Artist") } throws
      MixcloudServerErrorException(503, "Service unavailable")
    val dataSource = MixcloudDataSourceImpl(mockClient)

    val result = dataSource.searchMixcloudProfiles("Artist")

    assertIs<MixcloudProfileSearchResult.Error>(result)
    assertEquals("Mixcloud server error. Please try again later.", result.message)
  }

  @Test
  fun `test searchMixcloudProfiles client error returns Error`() = runTest {
    val mockClient = mock<MixcloudApiClient>()
    everySuspend { mockClient.searchUsers("Artist") } throws
      MixcloudClientErrorException(400, "Bad request")
    val dataSource = MixcloudDataSourceImpl(mockClient)

    val result = dataSource.searchMixcloudProfiles("Artist")

    assertIs<MixcloudProfileSearchResult.Error>(result)
    assertEquals("Failed to search Mixcloud. Please try again.", result.message)
  }

  // ----- getShowsForProfile -----

  @Test
  fun `test getShowsForProfile returns shows from client`() = runTest {
    val mockClient = mock<MixcloudApiClient>()
    val shows =
      listOf(
        MixcloudShow(
          key = "/artist/show-1/",
          name = "Show 1",
          url = "https://www.mixcloud.com/artist/show-1/",
        )
      )
    everySuspend { mockClient.getCloudcasts("/artist/") } returns shows
    val dataSource = MixcloudDataSourceImpl(mockClient)

    val result = dataSource.getShowsForProfile("/artist/")

    assertEquals(shows, result)
    verifySuspend { mockClient.getCloudcasts("/artist/") }
  }

  @Test
  fun `test getShowsForProfile rate limit exception propagates`() = runTest {
    val mockClient = mock<MixcloudApiClient>()
    val blockedUntil = Clock.System.now() + 45.seconds
    everySuspend { mockClient.getCloudcasts(any()) } throws
      ApiRateLimitException(blockedUntil = blockedUntil, message = "Rate limited")
    val dataSource = MixcloudDataSourceImpl(mockClient)

    val e = assertFailsWith<ApiRateLimitException> { dataSource.getShowsForProfile("/artist/") }
    assertEquals(blockedUntil, e.blockedUntil)
  }
}
