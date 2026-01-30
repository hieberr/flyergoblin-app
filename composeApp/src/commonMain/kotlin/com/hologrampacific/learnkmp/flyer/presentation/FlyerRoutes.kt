package com.hologrampacific.learnkmp.flyer.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hologrampacific.learnkmp.flyer.presentation.addevent.AddEventScreen
import com.hologrampacific.learnkmp.flyer.presentation.detail.EventDetailScreen
import com.hologrampacific.learnkmp.presentation.Navigator
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable data object AddEvent : NavKey

@Serializable data class EventDetail(val eventId: String) : NavKey

fun EntryProviderScope<NavKey>.flyerEntryBuilder(navigator: Navigator) {
  entry<AddEvent> { AddEventScreen(navigator) }
  entry<EventDetail> { key -> EventDetailScreen(navigator, key.eventId) }
}

val flyerSerializationModule = SerializersModule {
  polymorphic(NavKey::class) {
    subclass(AddEvent::class)
    subclass(EventDetail::class)
  }
}
