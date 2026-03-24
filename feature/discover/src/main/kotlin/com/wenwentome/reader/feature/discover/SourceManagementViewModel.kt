package com.wenwentome.reader.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenwentome.reader.core.model.SourceDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SourceManagementViewModel(
    observeSources: Flow<List<SourceDefinition>>,
    private val toggleSourceEnabled: suspend (String) -> Unit,
    private val deleteSource: suspend (String) -> Unit,
) : ViewModel() {
    val uiState: StateFlow<SourceManagementUiState> =
        observeSources
            .map(::SourceManagementUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SourceManagementUiState(),
            )

    fun toggleEnabled(sourceId: String) {
        viewModelScope.launch {
            toggleSourceEnabled(sourceId)
        }
    }

    fun delete(sourceId: String) {
        viewModelScope.launch {
            deleteSource(sourceId)
        }
    }
}
