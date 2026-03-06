package com.hologrampacific.flyergoblin.flyer.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.hologrampacific.flyergoblin.db.AppDatabase
import com.hologrampacific.flyergoblin.db.ArtistEntity
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SqlDelightArtistRepository(private val database: AppDatabase) : ArtistRepository {

  private val queries = database.artistEntityQueries

  override fun observeArtistByName(name: String): Flow<Artist?> =
    queries.getArtistByName(name).asFlow().mapToOneOrNull(Dispatchers.IO).map { it?.toArtist() }

  override suspend fun getArtistByName(name: String): Artist? =
    queries.getArtistByName(name).executeAsOneOrNull()?.toArtist()

  override suspend fun saveArtist(artist: Artist) {
    queries.upsertArtist(
      name = artist.name,
      soundCloudInfo = artist.soundCloudInfo,
      mixcloudInfo = artist.mixcloudInfo,
    )
  }

  override suspend fun updateArtist(artist: Artist) {
    queries.upsertArtist(
      name = artist.name,
      soundCloudInfo = artist.soundCloudInfo,
      mixcloudInfo = artist.mixcloudInfo,
    )
  }

  override suspend fun deleteArtistByName(name: String) {
    queries.deleteArtistByName(name)
  }

  private fun ArtistEntity.toArtist(): Artist =
    Artist(name = name, soundCloudInfo = soundCloudInfo, mixcloudInfo = mixcloudInfo)
}
