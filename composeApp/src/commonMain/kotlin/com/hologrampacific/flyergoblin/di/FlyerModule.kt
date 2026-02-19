package com.hologrampacific.flyergoblin.di

import com.hologrampacific.flyergoblin.flyer.data.datasource.GeminiFlyerDataSource
import com.hologrampacific.flyergoblin.flyer.data.datasource.SoundCloudDataSourceImpl
import com.hologrampacific.flyergoblin.flyer.data.remote.GeminiApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.HttpClientFactory
import com.hologrampacific.flyergoblin.flyer.data.remote.SoundCloudApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.SoundCloudApiClientImpl
import app.cash.sqldelight.db.SqlDriver
import com.hologrampacific.flyergoblin.db.AppDatabase
import com.hologrampacific.flyergoblin.db.DriverFactory
import com.hologrampacific.flyergoblin.db.createAppDatabase
import com.hologrampacific.flyergoblin.flyer.data.repository.SqlDelightArtistRepository
import com.hologrampacific.flyergoblin.flyer.data.repository.SqlDelightEventRepository
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ArtistResearchDataSource
import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerProcessingDataSource
import com.hologrampacific.flyergoblin.flyer.domain.datasource.SoundCloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.flyer.domain.repository.EventRepository
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ProcessFlyerUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ResearchArtistUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SetSoundCloudProfileUseCase
import com.hologrampacific.flyergoblin.flyer.presentation.artist.ArtistDetailViewModel
import com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection.SoundCloudProfileSelectionViewModel
import com.hologrampacific.flyergoblin.flyer.presentation.event.EditEventViewModel
import com.hologrampacific.flyergoblin.flyer.presentation.event.EventDetailViewModel
import com.hologrampacific.flyergoblin.flyer.presentation.events.EventsViewModel
import io.ktor.client.*
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.onClose

fun flyerModule(driverFactory: DriverFactory) = module {
  // Database
  single<SqlDriver> { driverFactory.createDriver() } onClose { it?.close() }
  single<AppDatabase> { createAppDatabase(get()) }

  // Clients
  single<HttpClient> { HttpClientFactory.create() } onClose { it?.close() }
  single<GeminiApiClient> { GeminiApiClient(get()) }
  single<SoundCloudApiClient> { SoundCloudApiClientImpl(get()) }

  // Repositories
  single<EventRepository> { SqlDelightEventRepository(get()) }
  single<ArtistRepository> { SqlDelightArtistRepository(get()) }

  // DataSources
  single<FlyerProcessingDataSource> { GeminiFlyerDataSource(get()) }
  single { SoundCloudDataSourceImpl(get()) }
  single<SoundCloudDataSource> { get<SoundCloudDataSourceImpl>() }
  single<ArtistResearchDataSource> { get<SoundCloudDataSourceImpl>() }

  // Use Cases (factory = new instance each time)
  factory { ProcessFlyerUseCase(get()) }
  factory { ResearchArtistUseCase(get(), get(), get()) }
  factory { SetSoundCloudProfileUseCase(get(), get()) }

  // ViewModels
  viewModel { EventsViewModel(get()) }

  viewModel { (eventId: Long?) -> EventDetailViewModel(eventId, get()) }

  viewModel { (eventId: Long?) -> EditEventViewModel(eventId, get(), get<ProcessFlyerUseCase>()) }

  viewModel { (artistName: String) ->
    ArtistDetailViewModel(artistName, get(), get<ResearchArtistUseCase>())
  }

  viewModel { (artistName: String) ->
    SoundCloudProfileSelectionViewModel(artistName, get(), get())
  }
}
