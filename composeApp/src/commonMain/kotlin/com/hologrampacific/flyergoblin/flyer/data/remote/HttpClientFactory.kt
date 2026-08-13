package com.hologrampacific.flyergoblin.flyer.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object HttpClientFactory {
  /** JSON config shared by the HTTP client's content negotiation and any manual body parsing. */
  val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
  }

  fun create(): HttpClient {
    return HttpClient {
      install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 60_000
      }
      install(ContentNegotiation) { json(json) }

      install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.INFO
      }
    }
  }
}
