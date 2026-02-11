package com.hologrampacific.flyergoblin.flyer.domain.repository

import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import kotlinx.coroutines.flow.StateFlow

/** Repository for managing artist data. */
interface ArtistRepository {
  /** Observable flow of all artists, keyed by artist name. */
  val artists: StateFlow<Map<String, Artist>>

  /**
   * Get an artist by name.
   *
   * @param name The artist's name
   * @return The artist if found, null otherwise
   */
  suspend fun getArtistByName(name: String): Artist?

  /**
   * Save a new artist.
   *
   * @param artist The artist to save
   */
  suspend fun saveArtist(artist: Artist)

  /**
   * Update an existing artist.
   *
   * @param artist The artist with updated information
   */
  suspend fun updateArtist(artist: Artist)
}
