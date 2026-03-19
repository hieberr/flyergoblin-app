package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository

class RefreshMixcloudProfileUseCase(
  private val mixcloudDataSource: MixcloudDataSource,
  private val artistRepository: ArtistRepository,
) {
  suspend operator fun invoke(artist: Artist): ResultWithRateLimitData<Unit> {
    val userKey =
      artist.mixcloudInfo?.profile?.key
        ?: return ResultWithRateLimitData.Error("Mixcloud profile not found")

    return when (val result = mixcloudDataSource.getFullProfile(userKey)) {
      is ResultWithRateLimitData.Success -> {
        artistRepository.upsertArtist(
          artist.copy(mixcloudInfo = artist.mixcloudInfo.copy(profile = result.value))
        )
        ResultWithRateLimitData.Success(Unit)
      }

      is ResultWithRateLimitData.Error -> result
      is ResultWithRateLimitData.RateLimited -> result
    }
  }
}
