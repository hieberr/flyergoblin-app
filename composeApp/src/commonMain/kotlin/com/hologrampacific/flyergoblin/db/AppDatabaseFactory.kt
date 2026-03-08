package com.hologrampacific.flyergoblin.db

import app.cash.sqldelight.db.SqlDriver

fun createAppDatabase(driver: SqlDriver): AppDatabase =
  AppDatabase(
    driver = driver,
    EventEntityAdapter =
      EventEntity.Adapter(
        startDateAdapter = LocalDateAdapter,
        startTimeAdapter = LocalTimeAdapter,
        artistsAdapter = StringListAdapter,
        dateAddedAdapter = InstantColumnAdapter,
        flyerImageBytesAdapter = ImageBytesAdapter,
      ),
    ArtistEntityAdapter =
      ArtistEntity.Adapter(
        soundCloudInfoAdapter = SoundCloudInfoAdapter,
        mixcloudInfoAdapter = MixcloudInfoAdapter,
      ),
  )
