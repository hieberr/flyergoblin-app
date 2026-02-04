package com.hologrampacific.learnkmp.flyer.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object HttpClientFactory {
  fun create(): HttpClient {
    return HttpClient {
      install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 60_000
      }
      install(ContentNegotiation) {
        json(
          Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
          }
        )
      }

      install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.INFO
      }
    }
  }
}
