package com.hologrampacific.learnkmp.di

import com.hologrampacific.learnkmp.flyer.data.datasource.GeminiFlyerDataSource
import com.hologrampacific.learnkmp.flyer.data.datasource.SoundCloudDataSourceImpl
import com.hologrampacific.learnkmp.flyer.data.remote.GeminiApiClient
import com.hologrampacific.learnkmp.flyer.data.remote.HttpClientFactory
import com.hologrampacific.learnkmp.flyer.data.remote.SoundCloudApiClient
import com.hologrampacific.learnkmp.flyer.data.remote.SoundCloudApiClientImpl
import com.hologrampacific.learnkmp.flyer.data.repository.MockArtistRepository
import com.hologrampacific.learnkmp.flyer.data.repository.MockEventRepository
import com.hologrampacific.learnkmp.flyer.domain.datasource.ArtistResearchDataSource
import com.hologrampacific.learnkmp.flyer.domain.datasource.FlyerProcessingDataSource
import com.hologrampacific.learnkmp.flyer.domain.datasource.SoundCloudDataSource
import com.hologrampacific.learnkmp.flyer.domain.repository.ArtistRepository
import com.hologrampacific.learnkmp.flyer.domain.repository.EventRepository
import com.hologrampacific.learnkmp.flyer.domain.usecase.ProcessFlyerUseCase
import com.hologrampacific.learnkmp.flyer.domain.usecase.ResearchArtistUseCase
import com.hologrampacific.learnkmp.flyer.domain.usecase.SetSoundCloudProfileUseCase
import com.hologrampacific.learnkmp.flyer.presentation.addevent.AddEventViewModel
import com.hologrampacific.learnkmp.flyer.presentation.artist.ArtistDetailViewModel
import com.hologrampacific.learnkmp.flyer.presentation.artist.profileselection.SoundCloudProfileSelectionViewModel
import com.hologrampacific.learnkmp.flyer.presentation.detail.EventDetailViewModel
import com.hologrampacific.learnkmp.flyer.presentation.flyer.FlyerViewModel
import io.ktor.client.*
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.onClose

val flyerModule = module {
  // Clients
  single<HttpClient> { HttpClientFactory.create() } onClose { it?.close() }
  single<GeminiApiClient> { GeminiApiClient(get()) }
  single<SoundCloudApiClient> { SoundCloudApiClientImpl(get()) }

  // Repositories
  single<EventRepository> { MockEventRepository }
  single<ArtistRepository> { MockArtistRepository() }

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
  viewModel { FlyerViewModel(get()) }

  viewModel { (eventId: String) -> EventDetailViewModel(eventId, get()) }

  viewModel { AddEventViewModel(get(), get<ProcessFlyerUseCase>()) }

  viewModel { (artistName: String) ->
    ArtistDetailViewModel(artistName, get(), get<ResearchArtistUseCase>())
  }

  viewModel { (artistName: String) ->
    SoundCloudProfileSelectionViewModel(artistName, get(), get())
  }
}
