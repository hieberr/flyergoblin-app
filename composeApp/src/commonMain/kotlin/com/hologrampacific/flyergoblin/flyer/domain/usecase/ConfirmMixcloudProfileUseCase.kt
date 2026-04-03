package com.hologrampacific.flyergoblin.flyer.domain.usecase

import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository

/**
 * Confirms that the auto-selected Mixcloud profile is correct by setting `profileChosen` to true.
 * Fetches the artist fresh from the repository to avoid overwriting concurrent updates. No-ops if
 * the artist has no profile, or if the profile was already confirmed.
 *
 * @param artistRepository Repository for loading and saving artist data
 */
class ConfirmMixcloudProfileUseCase(private val artistRepository: ArtistRepository) {
  suspend operator fun invoke(artistName: String) {
    val artist = artistRepository.getArtistByName(artistName) ?: return
    val info = artist.mixcloudInfo ?: return
    if (info.profile == null || info.profileChosen) return
    artistRepository.upsertArtist(artist.copy(mixcloudInfo = info.copy(profileChosen = true)))
  }
}
