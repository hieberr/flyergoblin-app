package com.hologrampacific.flyergoblin.db

import app.cash.sqldelight.ColumnAdapter
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudInfo
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object LocalDateAdapter : ColumnAdapter<LocalDate, String> {
  override fun decode(databaseValue: String): LocalDate = LocalDate.parse(databaseValue)

  override fun encode(value: LocalDate): String = value.toString()
}

object LocalTimeAdapter : ColumnAdapter<LocalTime, String> {
  override fun decode(databaseValue: String): LocalTime = LocalTime.parse(databaseValue)

  override fun encode(value: LocalTime): String = value.toString()
}

object InstantColumnAdapter : ColumnAdapter<Instant, String> {
  override fun decode(databaseValue: String): Instant = Instant.parse(databaseValue)

  override fun encode(value: Instant): String = value.toString()
}

object StringListAdapter : ColumnAdapter<List<String>, String> {
  private val serializer = ListSerializer(String.serializer())

  override fun decode(databaseValue: String): List<String> =
    json.decodeFromString(serializer, databaseValue)

  override fun encode(value: List<String>): String = json.encodeToString(serializer, value)
}

object SoundCloudInfoAdapter : ColumnAdapter<SoundCloudInfo, String> {
  override fun decode(databaseValue: String): SoundCloudInfo =
    json.decodeFromString(SoundCloudInfo.serializer(), databaseValue)

  override fun encode(value: SoundCloudInfo): String =
    json.encodeToString(SoundCloudInfo.serializer(), value)
}
