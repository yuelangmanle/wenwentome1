package com.wenwentome.reader.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.core.model.ReadingState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DiscoverEvent {
    data class OpenBookDetail(
        val bookId: String,
    ) : DiscoverEvent

    data class OpenReader(
        val bookId: String,
    ) : DiscoverEvent
}

class DiscoverViewModel(
    private val sourceBridgeRepository: SourceBridgeRepository,
    private val addRemoteBookToShelf: AddRemoteBookToShelf,
    private val resolveShelfBookId: suspend (RemoteSearchResult) -> String? = { null },
    private val refreshRemoteBook: suspend (String) -> RefreshRemoteBookResult = {
        RefreshRemoteBookResult(
            latestKnownChapterRef = null,
            hasUpdates = false,
        )
    },
    private val loadReadingState: suspend (String) -> ReadingState? = { null },
    private val updateReadingState: suspend (ReadingState) -> Unit = {},
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = mutableUiState.asStateFlow()
    private val mutableEvents = MutableSharedFlow<DiscoverEvent>(extraBufferCapacity = 1)
    val events = mutableEvents.asSharedFlow()
    private var searchJob: Job? = null
    private var previewJob: Job? = null
    private var searchToken: Long = 0
    private var previewToken: Long = 0

    fun search(query: String) {
        mutableUiState.update {
            it.copy(
                query = query,
                results = if (query.isBlank()) emptyList() else it.results,
                selectedResultId = null,
                selectedPreview = null,
                refreshingResultIds = emptySet(),
            )
        }
        searchJob?.cancel()
        previewJob?.cancel()
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

    fun selectResult(resultId: String) {
        val result = mutableUiState.value.results.firstOrNull { it.id == resultId } ?: return
        mutableUiState.update {
            it.copy(
                selectedResultId = resultId,
                selectedPreview = null,
            )
        }
        previewJob?.cancel()
        val currentToken = ++previewToken
        previewJob = viewModelScope.launch {
            val detail = sourceBridgeRepository.fetchBookDetail(result.sourceId, result.id)
            val state = mutableUiState.value
            if (currentToken != previewToken || state.selectedResultId != resultId) return@launch
            mutableUiState.update { it.copy(selectedPreview = detail) }
        }
    }

    fun addToShelf(resultId: String) {
        val result = mutableUiState.value.results.firstOrNull { it.id == resultId } ?: return
        if (resultId in mutableUiState.value.addingResultIds) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(addingResultIds = it.addingResultIds + resultId) }
            try {
                withContext(ioDispatcher) {
                    ensureBookOnShelf(result)
                }
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

    fun refreshSelected() {
        val result = selectedResult() ?: return
        if (result.id in mutableUiState.value.refreshingResultIds) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(refreshingResultIds = it.refreshingResultIds + result.id) }
            try {
                val ensured = withContext(ioDispatcher) {
                    ensureBookOnShelf(result)
                }
                withContext(ioDispatcher) {
                    refreshRemoteBook(ensured.bookId)
                }
                val detail = withContext(ioDispatcher) {
                    sourceBridgeRepository.fetchBookDetail(result.sourceId, result.id)
                }
                mutableUiState.update {
                    it.copy(
                        selectedPreview = detail,
                        refreshingResultIds = it.refreshingResultIds - result.id,
                        lastAddedTitle = result.title.takeIf { ensured.wasAdded } ?: it.lastAddedTitle,
                    )
                }
            } catch (error: Throwable) {
                mutableUiState.update { it.copy(refreshingResultIds = it.refreshingResultIds - result.id) }
                throw error
            }
        }
    }

    fun readLatest() {
        val result = selectedResult() ?: return
        if (result.id in mutableUiState.value.refreshingResultIds) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(refreshingResultIds = it.refreshingResultIds + result.id) }
            try {
                val ensured = withContext(ioDispatcher) {
                    ensureBookOnShelf(result)
                }
                val refreshResult = withContext(ioDispatcher) {
                    refreshRemoteBook(ensured.bookId)
                }
                val latestChapterRef = refreshResult.latestKnownChapterRef
                if (latestChapterRef.isNullOrBlank()) {
                    mutableEvents.tryEmit(DiscoverEvent.OpenBookDetail(bookId = ensured.bookId))
                } else {
                    val currentReadingState = withContext(ioDispatcher) {
                        loadReadingState(ensured.bookId)
                    }
                    withContext(ioDispatcher) {
                        updateReadingState(
                            ReadingState(
                                bookId = ensured.bookId,
                                locator = latestChapterRef,
                                chapterRef = latestChapterRef,
                                progressPercent = currentReadingState?.progressPercent ?: 0f,
                                bookmarks = currentReadingState?.bookmarks.orEmpty(),
                                notes = currentReadingState?.notes.orEmpty(),
                                updatedAt = System.currentTimeMillis(),
                            )
                        )
                    }
                    mutableEvents.tryEmit(DiscoverEvent.OpenReader(bookId = ensured.bookId))
                }
                val detail = withContext(ioDispatcher) {
                    sourceBridgeRepository.fetchBookDetail(result.sourceId, result.id)
                }
                mutableUiState.update {
                    it.copy(
                        selectedPreview = detail,
                        refreshingResultIds = it.refreshingResultIds - result.id,
                        lastAddedTitle = result.title.takeIf { ensured.wasAdded } ?: it.lastAddedTitle,
                    )
                }
            } catch (error: Throwable) {
                mutableUiState.update { it.copy(refreshingResultIds = it.refreshingResultIds - result.id) }
                throw error
            }
        }
    }

    private fun selectedResult(): RemoteSearchResult? {
        val state = mutableUiState.value
        val selectedId = state.selectedResultId ?: return null
        return state.results.firstOrNull { it.id == selectedId }
    }

    private suspend fun ensureBookOnShelf(result: RemoteSearchResult): EnsuredShelfBook {
        val existingBookId = resolveShelfBookId(result)
        if (existingBookId != null) {
            return EnsuredShelfBook(bookId = existingBookId, wasAdded = false)
        }
        val addedBookId = addRemoteBookToShelf(result)
        return EnsuredShelfBook(bookId = addedBookId, wasAdded = true)
    }

    private data class EnsuredShelfBook(
        val bookId: String,
        val wasAdded: Boolean,
    )
}
