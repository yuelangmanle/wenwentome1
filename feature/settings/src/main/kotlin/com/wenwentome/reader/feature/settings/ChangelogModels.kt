package com.wenwentome.reader.feature.settings

data class ChangelogEntry(
    val version: String,
    val releaseDate: String,
    val title: String,
    val highlights: List<String>,
    val details: List<String>,
)

data class ChangelogUiState(
    val entries: List<ChangelogEntry> = emptyList(),
    val errorMessage: String? = null,
)
