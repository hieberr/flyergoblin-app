package com.hologrampacific.learnkmp.flyer.domain.service

import com.hologrampacific.learnkmp.flyer.domain.model.Event

sealed class AiProcessingResult {
  data class Success(val event: Event) : AiProcessingResult()

  data class Error(val message: String) : AiProcessingResult()
}

interface FlyerAiService {
  suspend fun processFlyer(imageBytes: ByteArray): AiProcessingResult
}
