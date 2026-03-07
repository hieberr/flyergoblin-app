package com.hologrampacific.flyergoblin.flyer.domain

import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudProfileInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** In-memory cache for profile search results. Not persisted to disk. */
class ProfileSearchCache {
  private val mutex = Mutex()
  private val soundCloudResults = mutableMapOf<String, List<SoundCloudProfileInfo>>()
  private val mixcloudResults = mutableMapOf<String, List<MixcloudProfileInfo>>()

  suspend fun getSoundCloudResults(artistName: String): List<SoundCloudProfileInfo>? =
    mutex.withLock { soundCloudResults[artistName] }

  suspend fun putSoundCloudResults(artistName: String, results: List<SoundCloudProfileInfo>) =
    mutex.withLock { soundCloudResults[artistName] = results }

  suspend fun getMixcloudResults(artistName: String): List<MixcloudProfileInfo>? =
    mutex.withLock { mixcloudResults[artistName] }

  suspend fun putMixcloudResults(artistName: String, results: List<MixcloudProfileInfo>) =
    mutex.withLock { mixcloudResults[artistName] = results }
}
