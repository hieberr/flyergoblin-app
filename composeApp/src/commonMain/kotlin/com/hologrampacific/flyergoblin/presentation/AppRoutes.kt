package com.hologrampacific.flyergoblin.presentation

import androidx.navigation3.runtime.NavKey
import com.hologrampacific.flyergoblin.email.presentation.EmailRoutes
import com.hologrampacific.flyergoblin.flyer.presentation.flyerSerializationModule
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface TopLevelRoutes {

  @Serializable data object Settings : NavKey

  @Serializable data object Email : NavKey

  @Serializable data object Flyer : NavKey

  companion object Companion {
    val entries: List<NavKey> = listOf(Settings, Email, Flyer)

    /** The initial route to display and the root of the backstack */
    val home = Flyer

    val serializationModule = SerializersModule {
      polymorphic(NavKey::class) {
        subclass(Settings::class)
        subclass(Email::class)
        subclass(Flyer::class)
      }
    }
  }
}

val appSerializersModule = SerializersModule {
  include(EmailRoutes.serializationModule)
  include(flyerSerializationModule)
  include(TopLevelRoutes.serializationModule)
}
