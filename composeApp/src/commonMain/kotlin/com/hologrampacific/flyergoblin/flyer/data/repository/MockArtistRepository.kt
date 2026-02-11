package com.hologrampacific.flyergoblin.flyer.data.repository

import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory implementation of ArtistRepository for storing artist data. Uses a StateFlow to allow
 * observing changes to the artist collection.
 */
class MockArtistRepository : ArtistRepository {
  private val _artists = MutableStateFlow<Map<String, Artist>>(emptyMap())
  override val artists: StateFlow<Map<String, Artist>> = _artists.asStateFlow()

  override suspend fun getArtistByName(name: String): Artist? {
    return _artists.value[name]
  }

  override suspend fun saveArtist(artist: Artist) {
    _artists.value = _artists.value + (artist.name to artist)
  }

  override suspend fun updateArtist(artist: Artist) {
    _artists.value = _artists.value + (artist.name to artist)
  }
}
