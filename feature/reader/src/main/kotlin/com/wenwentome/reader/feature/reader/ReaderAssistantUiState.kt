package com.wenwentome.reader.feature.reader

data class ReaderAssistantUiState(
    val isLoading: Boolean = false,
    val summary: String? = null,
    val explanation: String? = null,
    val translation: String? = null,
    val ttsScript: String? = null,
    val errorMessage: String? = null,
)
