package com.hologrampacific.flyergoblin.flyer.data.datasource

import com.hologrampacific.flyergoblin.flyer.data.remote.FlyerApiClient
import com.hologrampacific.flyergoblin.flyer.data.remote.FlyerApiErrorCode
import com.hologrampacific.flyergoblin.flyer.data.remote.FlyerApiException
import com.hologrampacific.flyergoblin.flyer.domain.datasource.ExtractedFlyerData
import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerExtractionErrorType
import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerExtractionResult
import com.hologrampacific.flyergoblin.flyer.domain.datasource.FlyerProcessingDataSource
import com.hologrampacific.flyergoblin.util.AppLogger
import io.ktor.client.network.sockets.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerializationException

/**
 * Implementation of [FlyerProcessingDataSource] that delegates flyer processing to the backend API.
 * The backend handles the LLM prompt and parsing; this class maps the API response to domain
 * models.
 */
class ApiFlyerDataSource(private val flyerApiClient: FlyerApiClient) : FlyerProcessingDataSource {

  override suspend fun extractEventFromFlyer(
    imageBase64: String,
    mimeType: String,
  ): FlyerExtractionResult {
    return try {
      val response = flyerApiClient.processFlyer(imageBase64, mimeType)

      val startDate =
        response.startDate?.let {
          try {
            LocalDate.parse(it)
          } catch (e: Exception) {
            AppLogger.w("ApiFlyerDataSource", "Could not parse date: $it", e)
            null
          }
        }

      val startTime =
        response.startTime?.let {
          try {
            LocalTime.parse(it)
          } catch (e: Exception) {
            AppLogger.w("ApiFlyerDataSource", "Could not parse time: $it", e)
            null
          }
        }

      val data =
        ExtractedFlyerData(
          name = response.name?.takeIf { it.isNotBlank() } ?: "Unknown",
          startDate = startDate,
          startTime = startTime,
          venue = response.venue,
          eventUrl = response.eventUrl,
          artists = response.artists,
        )

      AppLogger.i(
        "ApiFlyerDataSource",
        "Successfully parsed event: ${data.name} on ${data.startDate}",
      )
      FlyerExtractionResult.Success(data)
    } catch (e: ConnectTimeoutException) {
      AppLogger.d("ApiFlyerDataSource", "Connection timeout while processing flyer")
      FlyerExtractionResult.Error(
        "Connection timeout. Please check your internet connection.",
        FlyerExtractionErrorType.NETWORK,
      )
    } catch (e: SocketTimeoutException) {
      AppLogger.d("ApiFlyerDataSource", "Request timeout while processing flyer")
      FlyerExtractionResult.Error(
        "Request timeout. The server took too long to respond.",
        FlyerExtractionErrorType.TIMEOUT,
      )
    } catch (e: FlyerApiException) {
      AppLogger.d("ApiFlyerDataSource", "Flyer API returned error: ${e.errorCode}", e)
      FlyerExtractionResult.Error(
        e.message ?: "Unable to process flyer. Please try again.",
        e.errorCode.toExtractionErrorType(),
      )
    } catch (e: SerializationException) {
      AppLogger.d("ApiFlyerDataSource", "Failed to parse server response", e)
      FlyerExtractionResult.Error("Unable to process the server response. Please try again.")
    } catch (e: Exception) {
      AppLogger.d("ApiFlyerDataSource", "Failed to process flyer", e)
      FlyerExtractionResult.Error("Unable to process flyer. Please try again.")
    }
  }
}

private fun FlyerApiErrorCode.toExtractionErrorType(): FlyerExtractionErrorType =
  when (this) {
    FlyerApiErrorCode.TIMEOUT -> FlyerExtractionErrorType.TIMEOUT
    FlyerApiErrorCode.UPSTREAM_RATE_LIMITED -> FlyerExtractionErrorType.UPSTREAM_RATE_LIMITED
    FlyerApiErrorCode.UPSTREAM_ERROR,
    FlyerApiErrorCode.INTERNAL_ERROR -> FlyerExtractionErrorType.SERVER_ERROR
    FlyerApiErrorCode.INVALID_REQUEST -> FlyerExtractionErrorType.INVALID_REQUEST
    FlyerApiErrorCode.UNKNOWN -> FlyerExtractionErrorType.UNKNOWN
  }
