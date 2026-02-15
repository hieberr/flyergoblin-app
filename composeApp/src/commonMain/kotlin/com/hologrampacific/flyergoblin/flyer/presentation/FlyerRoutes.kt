package com.hologrampacific.flyergoblin.flyer.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hologrampacific.flyergoblin.flyer.presentation.artist.ArtistDetailScreen
import com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection.SoundCloudProfileSelectionScreen
import com.hologrampacific.flyergoblin.flyer.presentation.event.EditEventScreen
import com.hologrampacific.flyergoblin.flyer.presentation.event.EventDetailScreen
import com.hologrampacific.flyergoblin.presentation.Navigator
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable data class EventDetail(val eventId: String?) : NavKey

@Serializable data class EditEvent(val eventId: String?) : NavKey

@Serializable data class ArtistDetail(val artistName: String) : NavKey

@Serializable data class SoundCloudProfileSelection(val artistName: String) : NavKey

fun EntryProviderScope<NavKey>.flyerEntryBuilder(navigator: Navigator) {
  entry<EventDetail> { key -> EventDetailScreen(navigator, key.eventId) }
  entry<EditEvent> { key -> EditEventScreen(navigator, key.eventId) }
  entry<ArtistDetail> { key -> ArtistDetailScreen(navigator, key.artistName) }
  entry<SoundCloudProfileSelection> { key ->
    SoundCloudProfileSelectionScreen(navigator, key.artistName)
  }
}

val flyerSerializationModule = SerializersModule {
  polymorphic(NavKey::class) {
    subclass(EventDetail::class)
    subclass(EditEvent::class)
    subclass(ArtistDetail::class)
    subclass(SoundCloudProfileSelection::class)
  }
}
