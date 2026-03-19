package com.wenwentome.reader.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChangelogViewModel(
    private val repository: ChangelogRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ChangelogUiState())
    val uiState: StateFlow<ChangelogUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                repository.getAll()
            }.onSuccess { entries ->
                mutableUiState.update { it.copy(entries = entries, errorMessage = null) }
            }.onFailure { error ->
                mutableUiState.update { it.copy(errorMessage = error.message ?: "更新日志加载失败") }
            }
        }
    }
}
