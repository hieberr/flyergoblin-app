package com.hologrampacific.flyergoblin.flyer.data.remote

import com.hologrampacific.flyergoblin.util.AppLogger
import io.ktor.client.*
import io.ktor.client.call.*
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

/**
 * Structured error body returned by the flyer processing API for non-2xx responses. Both fields are
 * optional because a failure can also come from API Gateway's own integration timeout (e.g.
 * `{"message": "Endpoint request timed out"}`) rather than our Lambda's own `{code, message}`
 * response, which never runs if the gateway's timeout fires first.
 */
@Serializable
private data class FlyerApiErrorBody(val code: String? = null, val message: String? = null)

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

      throw FlyerApiException(
        parsedError?.code?.let { FlyerApiErrorCode.fromWireValue(it) }
          ?: FlyerApiErrorCode.fromStatusCode(response.status.value),
        parsedError?.message ?: "Unable to process flyer. Please try again.",
        response.status.value,
      )
    }

    return response.body<FlyerApiResponse>()
  }
}
