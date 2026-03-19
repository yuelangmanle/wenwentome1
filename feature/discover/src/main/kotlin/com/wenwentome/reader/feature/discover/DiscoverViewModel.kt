package com.wenwentome.reader.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import kotlinx.coroutines.Job
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
    private var searchJob: Job? = null
    private var searchToken: Long = 0

    fun search(query: String) {
        mutableUiState.update { it.copy(query = query, results = if (query.isBlank()) emptyList() else it.results) }
        searchJob?.cancel()
        if (query.isBlank()) {
            return
        }
        val currentToken = ++searchToken
        searchJob = viewModelScope.launch {
            val results = sourceBridgeRepository.search(query, emptyList())
            val state = mutableUiState.value
            if (currentToken != searchToken || state.query != query) return@launch
            mutableUiState.update { it.copy(results = results) }
        }
    }

    fun addToShelf(resultId: String) {
        val result = mutableUiState.value.results.firstOrNull { it.id == resultId } ?: return
        if (resultId in mutableUiState.value.addingResultIds) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(addingResultIds = it.addingResultIds + resultId) }
            try {
                addRemoteBookToShelf(result)
                mutableUiState.update {
                    it.copy(
                        addingResultIds = it.addingResultIds - resultId,
                        lastAddedTitle = result.title,
                    )
                }
            } catch (error: Throwable) {
                mutableUiState.update { it.copy(addingResultIds = it.addingResultIds - resultId) }
                throw error
            }
        }
    }
}
