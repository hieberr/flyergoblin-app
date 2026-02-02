package com.hologrampacific.learnkmp.di

import com.hologrampacific.learnkmp.flyer.data.repository.MockEventRepository
import com.hologrampacific.learnkmp.flyer.data.service.GeminiFlyerAiService
import com.hologrampacific.learnkmp.flyer.data.service.HttpClientFactory
import com.hologrampacific.learnkmp.flyer.domain.repository.EventRepository
import com.hologrampacific.learnkmp.flyer.domain.service.FlyerAiService
import com.hologrampacific.learnkmp.flyer.presentation.addevent.AddEventViewModel
import com.hologrampacific.learnkmp.flyer.presentation.detail.EventDetailViewModel
import com.hologrampacific.learnkmp.flyer.presentation.flyer.FlyerViewModel
import io.ktor.client.*
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.onClose

val flyerModule = module {
  // HTTP Client
  single<HttpClient> { HttpClientFactory.create() } onClose { it?.close() }

  // Repositories
  single<EventRepository> { MockEventRepository }

  // Services
  single<FlyerAiService> { GeminiFlyerAiService(get()) }

  // ViewModels
  viewModel { FlyerViewModel(get()) }

  viewModel { (eventId: String) -> EventDetailViewModel(eventId, get()) }

  viewModel { AddEventViewModel(get(), get()) }
}
