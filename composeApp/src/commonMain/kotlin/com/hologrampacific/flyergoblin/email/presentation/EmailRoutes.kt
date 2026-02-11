package com.hologrampacific.flyergoblin.email.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hologrampacific.flyergoblin.presentation.Navigator
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface EmailRoutes {
  @Serializable data class EmailDetail(val emailText: String) : NavKey

  companion object {
    val serializationModule = SerializersModule {
      polymorphic(NavKey::class) { subclass(EmailDetail::class) }
    }

    fun EntryProviderScope<NavKey>.emailEntryBuilder(navigator: Navigator) {
      entry<EmailDetail> { key -> EmailDetailScreen(navigator, key.emailText) }
    }
  }
}
