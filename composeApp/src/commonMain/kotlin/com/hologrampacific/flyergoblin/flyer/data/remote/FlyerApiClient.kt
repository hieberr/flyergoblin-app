package com.hologrampacific.flyergoblin.flyer.data.remote

import com.hologrampacific.flyergoblin.util.AppLogger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/** Response from the flyer processing API. */
@Serializable
data class FlyerApiResponse(
  val name: String? = null,
  val startDate: String? = null,
  val startTime: String? = null,
  val venue: String? = null,
  val eventUrl: String? = null,
  val artists: List<String> = emptyList(),
)

/** Client interface for the flyer processing API. */
interface FlyerApiClient {
  suspend fun processFlyer(imageBase64: String, mimeType: String): FlyerApiResponse
}

@Serializable
private data class FlyerProcessRequestBody(val imageBase64: String, val mimeType: String)

/** Structured error body returned by the flyer processing API for non-2xx responses. */
@Serializable private data class FlyerApiErrorBody(val code: String, val message: String)

/** Ktor-based implementation of [FlyerApiClient]. */
class FlyerApiClientImpl(
  private val httpClient: HttpClient,
  private val baseUrl: String = "https://api.uedo.net",
) : FlyerApiClient {

  override suspend fun processFlyer(imageBase64: String, mimeType: String): FlyerApiResponse {
    val response =
      httpClient.post("$baseUrl/v1/flyer/process") {
        contentType(ContentType.Application.Json)
        setBody(FlyerProcessRequestBody(imageBase64, mimeType))
      }

    if (!response.status.isSuccess()) {
      val errorBody = response.bodyAsText()
      AppLogger.e("FlyerApiClient", "Server error (${response.status.value}): $errorBody")

      val parsedError =
        try {
          HttpClientFactory.json.decodeFromString<FlyerApiErrorBody>(errorBody)
        } catch (e: Exception) {
          AppLogger.e("FlyerApiClient", "Failed to parse error body: $errorBody", e)
          null
        }

      if (parsedError != null) {
        throw FlyerApiException(
          FlyerApiErrorCode.fromWireValue(parsedError.code),
          parsedError.message,
          response.status.value,
        )
      }
      throw ResponseException(response, errorBody)
    }

    return response.body<FlyerApiResponse>()
  }
}
