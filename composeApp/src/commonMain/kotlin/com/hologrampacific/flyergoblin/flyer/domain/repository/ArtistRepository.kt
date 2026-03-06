package com.hologrampacific.flyergoblin.flyer.domain.repository

import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import kotlinx.coroutines.flow.Flow

/** Repository for managing artist data. */
interface ArtistRepository {
  /**
   * Observe a single artist by name. Emits null if the artist does not exist, and re-emits whenever
   * the artist's data changes in the database.
   */
  fun observeArtistByName(name: String): Flow<Artist?>

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

  /**
   * Delete an artist by name.
   *
   * @param name The artist's name
   */
  suspend fun deleteArtistByName(name: String)
}
