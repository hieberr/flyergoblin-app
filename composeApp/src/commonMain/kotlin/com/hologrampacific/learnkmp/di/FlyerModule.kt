package com.hologrampacific.learnkmp.di

import com.hologrampacific.learnkmp.flyer.data.repository.MockEventRepository
import com.hologrampacific.learnkmp.flyer.data.service.MockFlyerAiService
import com.hologrampacific.learnkmp.flyer.domain.repository.EventRepository
import com.hologrampacific.learnkmp.flyer.domain.service.FlyerAiService
import com.hologrampacific.learnkmp.flyer.presentation.addevent.AddEventViewModel
import com.hologrampacific.learnkmp.flyer.presentation.detail.EventDetailViewModel
import com.hologrampacific.learnkmp.flyer.presentation.flyer.FlyerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val flyerModule = module {
  // Repositories
  single<EventRepository> { MockEventRepository }

  // Services
  single<FlyerAiService> { MockFlyerAiService() }

  // ViewModels
  viewModel { FlyerViewModel(get()) }

  viewModel { (eventId: String) -> EventDetailViewModel(eventId, get()) }

  viewModel { AddEventViewModel(get(), get()) }
}
