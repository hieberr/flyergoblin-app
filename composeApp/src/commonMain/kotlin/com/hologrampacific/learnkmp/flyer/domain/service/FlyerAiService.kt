package com.hologrampacific.learnkmp.flyer.domain.service

import com.hologrampacific.learnkmp.flyer.domain.model.Event

sealed class FlyerAiProcessingResult {
  data class Success(val event: Event) : FlyerAiProcessingResult()

  data class Error(val message: String) : FlyerAiProcessingResult()
}

interface FlyerAiService {
  suspend fun processFlyer(
    imageBytes: ByteArray,
    mimeType: String = "image/jpeg",
  ): FlyerAiProcessingResult
}
