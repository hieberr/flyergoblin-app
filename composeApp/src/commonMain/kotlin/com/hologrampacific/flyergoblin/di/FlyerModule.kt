package com.hologrampacific.flyergoblin.di

import app.cash.sqldelight.db.SqlDriver
import com.hologrampacific.flyergoblin.AppSettings
import com.hologrampacific.flyergoblin.db.AppDatabase
import com.hologrampacific.flyergoblin.db.DriverFactory
import com.hologrampacific.flyergoblin.db.createAppDatabase
import com.hologrampacific.flyergoblin.flyer.data.datasource.ApiFlyerDataSource
import com.hologrampacific.flyergoblin.flyer.data.datasource.MixcloudDataSourceImpl
import com.hologrampacific.flyergoblin.flyer.data.datasource.SoundCloudDataSourceImpl
import com.hologrampacific.flyergoblin.flyer.data.remote.FlyerApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.FlyerApiClientImpl
import com.hologrampacific.flyergoblin.flyer.data.remote.HttpClientFactory
import com.hologrampacific.flyergoblin.flyer.data.remote.MixcloudApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.MixcloudApiClientImpl
import com.hologrampacific.flyergoblin.flyer.data.remote.SoundCloudApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.SoundCloudApiClientImpl
import com.hologrampacific.flyergoblin.flyer.data.repository.SqlDelightArtistRepository
import com.hologrampacific.flyergoblin.flyer.data.repository.SqlDelightEventRepository
import com.hologrampacific.flyergoblin.flyer.domain.ProfileSearchCache
import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerProcessingDataSource
import com.hologrampacific.flyergoblin.flyer.domain.datasource.MixcloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.datasource.SoundCloudDataSource
import com.hologrampacific.flyergoblin.flyer.domain.repository.ArtistRepository
import com.hologrampacific.flyergoblin.flyer.domain.repository.EventRepository
import com.hologrampacific.flyergoblin.flyer.domain.usecase.ProcessFlyerUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SearchMixcloudProfilesUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SearchSoundCloudProfilesUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SetMixcloudProfileUseCase
import com.hologrampacific.flyergoblin.flyer.domain.usecase.SetSoundCloudProfileUseCase
import com.hologrampacific.flyergoblin.flyer.presentation.artist.ArtistDetailViewModel
import com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection.MixcloudProfileSelectionViewModel
import com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection.SoundCloudProfileSelectionViewModel
import com.hologrampacific.flyergoblin.flyer.presentation.event.EditEventViewModel
import com.hologrampacific.flyergoblin.flyer.presentation.event.EventDetailViewModel
import com.hologrampacific.flyergoblin.flyer.presentation.events.EventsViewModel
import com.hologrampacific.flyergoblin.presentation.SettingsViewModel
import com.hologrampacific.flyergoblin.sharing.SharedImageProvider
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
  single<FlyerApiClient> { FlyerApiClientImpl(get()) }
  single<SoundCloudApiClient> { SoundCloudApiClientImpl(get()) }
  single<MixcloudApiClient> { MixcloudApiClientImpl(get()) }

  // Repositories
  single<EventRepository> { SqlDelightEventRepository(get()) }
  single<ArtistRepository> { SqlDelightArtistRepository(get()) }

  // DataSources
  single<FlyerProcessingDataSource> { ApiFlyerDataSource(get()) }
  single { SoundCloudDataSourceImpl(get()) }
  single<SoundCloudDataSource> { get<SoundCloudDataSourceImpl>() }
  single<MixcloudDataSource> { MixcloudDataSourceImpl(get()) }

  // In-memory cache (singleton, lives for app lifetime)
  single { ProfileSearchCache() }

  // SharedImageProvider singleton for cross-platform share-sheet integration
  single { SharedImageProvider() }

  // App settings
  single { AppSettings() }

  // Use Cases (factory = new instance each time)
  factory { ProcessFlyerUseCase(get()) }
  factory {
    SetSoundCloudProfileUseCase(
      artistRepository = get(),
      soundCloudDataSource = get(),
      profileSearchCache = get(),
    )
  }
  factory {
    SetMixcloudProfileUseCase(
      artistRepository = get(),
      mixcloudDataSource = get(),
      profileSearchCache = get(),
    )
  }
  factory { SearchSoundCloudProfilesUseCase(artistDataSource = get(), profileSearchCache = get()) }
  factory { SearchMixcloudProfilesUseCase(mixcloudDataSource = get(), profileSearchCache = get()) }

  // ViewModels
  viewModel { SettingsViewModel(get()) }
  viewModel { EventsViewModel(get()) }

  viewModel { (eventId: Long?) -> EventDetailViewModel(eventId, get()) }

  viewModel { (eventId: Long?) ->
    EditEventViewModel(eventId, get(), get<ProcessFlyerUseCase>(), get())
  }

  viewModel { (artistName: String) ->
    ArtistDetailViewModel(
      artistName = artistName,
      artistRepository = get(),
      searchSoundCloudProfilesUseCase = get(),
      setSoundCloudProfileUseCase = get(),
      searchMixcloudProfilesUseCase = get(),
      setMixcloudProfileUseCase = get(),
      profileSearchCache = get(),
    )
  }

  viewModel { (artistName: String) ->
    SoundCloudProfileSelectionViewModel(
      artistName = artistName,
      artistRepository = get(),
      setSoundCloudProfileUseCase = get(),
      searchSoundCloudProfilesUseCase = get(),
      profileSearchCache = get(),
    )
  }

  viewModel { (artistName: String) ->
    MixcloudProfileSelectionViewModel(
      artistName = artistName,
      artistRepository = get(),
      setMixcloudProfileUseCase = get(),
      searchMixcloudProfilesUseCase = get(),
      profileSearchCache = get(),
    )
  }
}
