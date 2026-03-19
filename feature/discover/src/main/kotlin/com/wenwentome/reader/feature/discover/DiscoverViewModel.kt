package com.wenwentome.reader.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DiscoverViewModel(
    private val sourceBridgeRepository: SourceBridgeRepository,
    private val addRemoteBookToShelf: AddRemoteBookToShelf,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = mutableUiState.asStateFlow()

    fun search(query: String) {
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    query = query,
                    results = if (query.isBlank()) emptyList() else sourceBridgeRepository.search(query, emptyList()),
                )
            }
        }
    }

    fun addToShelf(resultId: String) {
        viewModelScope.launch {
            val result = mutableUiState.value.results.first { it.id == resultId }
            addRemoteBookToShelf(result)
            mutableUiState.update { it.copy(lastAddedTitle = result.title) }
        }
    }
}
