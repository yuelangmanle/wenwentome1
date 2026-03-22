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
            activeSourceId = "",
            activeRemoteBookId = "",
            activeRemoteBookUrl = "",
            autoSwitched = false,
            primarySourceId = "",
            primarySourceFailed = false,
        )
    },
    private val loadReadingState: suspend (String) -> ReadingState? = { null },
    private val updateReadingState: suspend (ReadingState) -> Unit = {},
    private val healthTracker: SourceHealthTracker = SourceHealthTracker(),
    private val boostedSourceIdsProvider: () -> Set<String> = { emptySet() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
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
                selectedResult = null,
                selectedPreview = null,
                refreshingResultIds = emptySet(),
                enhancementHint = null,
                lastRefreshHint = null,
            )
        }
        searchJob?.cancel()
        previewJob?.cancel()
        if (query.isBlank()) {
            return
        }
        val currentToken = ++searchToken
        searchJob = viewModelScope.launch {
            val results = withContext(ioDispatcher) {
                sourceBridgeRepository.search(query, emptyList())
            }
            val state = mutableUiState.value
            if (currentToken != searchToken || state.query != query) return@launch
            val boostedSourceIds = boostedSourceIdsProvider()
            val enhanced =
                healthTracker.enhanceResults(
                    results = results,
                    boostedSourceIds = boostedSourceIds,
                    preferredTitle = query,
                )
            val hint =
                if (enhanced.isNotEmpty()) {
                    if (boostedSourceIds.isNotEmpty()) "已按书源健康分与增强信号排序" else "已按书源健康分排序"
                } else {
                    null
                }
            mutableUiState.update { it.copy(results = enhanced, enhancementHint = hint) }
        }
    }

    fun selectResult(resultId: String) {
        val result = mutableUiState.value.results.firstOrNull { it.id == resultId } ?: return
        mutableUiState.update {
            it.copy(
                selectedResultId = resultId,
                selectedResult = result,
                selectedPreview = null,
            )
        }
        previewJob?.cancel()
        val currentToken = ++previewToken
        previewJob = viewModelScope.launch {
            val start = nowProvider()
            try {
                val detail = withContext(ioDispatcher) {
                    sourceBridgeRepository.fetchBookDetail(result.sourceId, result.id)
                }
                healthTracker.recordResult(
                    sourceId = result.sourceId,
                    success = true,
                    latencyMs = nowProvider() - start,
                )
                val state = mutableUiState.value
                if (currentToken != previewToken || state.selectedResultId != resultId) return@launch
                mutableUiState.update {
                    val updatedResults = reEnhanceResults(preferredTitle = it.query)
                    it.copy(
                        selectedPreview = detail,
                        results = updatedResults,
                        selectedResult = updatedResults.firstOrNull { item -> item.id == resultId } ?: it.selectedResult,
                    )
                }
            } catch (error: Throwable) {
                healthTracker.recordResult(
                    sourceId = result.sourceId,
                    success = false,
                    latencyMs = nowProvider() - start,
                )
                mutableUiState.update {
                    val updatedResults = reEnhanceResults(preferredTitle = it.query)
                    it.copy(
                        results = updatedResults,
                        selectedResult = updatedResults.firstOrNull { item -> item.id == resultId } ?: it.selectedResult,
                        lastRefreshHint = "当前书源预览失败，请稍后重试",
                    )
                }
            }
        }
    }

    fun addToShelf(result: RemoteSearchResult) {
        if (result.id in mutableUiState.value.addingResultIds) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(addingResultIds = it.addingResultIds + result.id) }
            try {
                withContext(ioDispatcher) {
                    addRemoteBookToShelf(result)
                }
                mutableUiState.update {
                    it.copy(
                        addingResultIds = it.addingResultIds - result.id,
                        lastAddedTitle = result.title,
                    )
                }
            } catch (error: Throwable) {
                mutableUiState.update { it.copy(addingResultIds = it.addingResultIds - result.id) }
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
                    ensureBookOnShelf(result.result)
                }
                val refreshResult = withContext(ioDispatcher) {
                    refreshRemoteBook(ensured.bookId)
                }
                val activeSource = resolveActiveSource(refreshResult, result.result)
                val detail = withContext(ioDispatcher) {
                    sourceBridgeRepository.fetchBookDetail(activeSource.sourceId, activeSource.remoteBookId)
                }
                val updatedResults = reEnhanceResults(preferredTitle = mutableUiState.value.query)
                mutableUiState.update {
                    it.copy(
                        selectedPreview = detail,
                        refreshingResultIds = it.refreshingResultIds - result.id,
                        lastAddedTitle = result.title.takeIf { ensured.wasAdded } ?: it.lastAddedTitle,
                        results = updatedResults,
                        selectedResult = updateSelectedResult(result, activeSource, updatedResults),
                        selectedResultId = if (refreshResult.autoSwitched) activeSource.remoteBookId else it.selectedResultId,
                        lastRefreshHint = refreshHint(refreshResult, activeSource.sourceId),
                    )
                }
            } catch (error: Throwable) {
                mutableUiState.update {
                    val updatedResults = reEnhanceResults(preferredTitle = it.query)
                    it.copy(
                        refreshingResultIds = it.refreshingResultIds - result.id,
                        results = updatedResults,
                        selectedResult = updatedResults.firstOrNull { item -> item.id == result.id } ?: it.selectedResult,
                        lastRefreshHint = "刷新目录失败，请稍后再试",
                    )
                }
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
                    ensureBookOnShelf(result.result)
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
                val activeSource = resolveActiveSource(refreshResult, result.result)
                val detail = withContext(ioDispatcher) {
                    sourceBridgeRepository.fetchBookDetail(activeSource.sourceId, activeSource.remoteBookId)
                }
                val updatedResults = reEnhanceResults(preferredTitle = mutableUiState.value.query)
                mutableUiState.update {
                    it.copy(
                        selectedPreview = detail,
                        refreshingResultIds = it.refreshingResultIds - result.id,
                        lastAddedTitle = result.title.takeIf { ensured.wasAdded } ?: it.lastAddedTitle,
                        results = updatedResults,
                        selectedResult = updateSelectedResult(result, activeSource, updatedResults),
                        selectedResultId = if (refreshResult.autoSwitched) activeSource.remoteBookId else it.selectedResultId,
                        lastRefreshHint = refreshHint(refreshResult, activeSource.sourceId),
                    )
                }
            } catch (error: Throwable) {
                mutableUiState.update {
                    val updatedResults = reEnhanceResults(preferredTitle = it.query)
                    it.copy(
                        refreshingResultIds = it.refreshingResultIds - result.id,
                        results = updatedResults,
                        selectedResult = updatedResults.firstOrNull { item -> item.id == result.id } ?: it.selectedResult,
                        lastRefreshHint = "阅读最新失败，请稍后再试",
                    )
                }
            }
        }
    }

    private fun selectedResult(): DiscoverSearchResult? {
        return mutableUiState.value.selectedResult
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

    private data class ActiveSource(
        val sourceId: String,
        val remoteBookId: String,
        val remoteBookUrl: String,
    )

    private fun resolveActiveSource(
        refreshResult: RefreshRemoteBookResult,
        fallback: RemoteSearchResult,
    ): ActiveSource {
        val sourceId = refreshResult.activeSourceId.ifBlank { fallback.sourceId }
        val remoteBookId = refreshResult.activeRemoteBookId.ifBlank { fallback.id }
        val remoteBookUrl = refreshResult.activeRemoteBookUrl.ifBlank { fallback.detailUrl }
        return ActiveSource(
            sourceId = sourceId,
            remoteBookId = remoteBookId,
            remoteBookUrl = remoteBookUrl,
        )
    }

    private fun reEnhanceResults(preferredTitle: String? = null): List<DiscoverSearchResult> {
        val boostedSourceIds = boostedSourceIdsProvider()
        return healthTracker.enhanceResults(
            results = mutableUiState.value.results.map { it.result },
            boostedSourceIds = boostedSourceIds,
            preferredTitle = preferredTitle,
        )
    }

    private fun updateSelectedResult(
        current: DiscoverSearchResult,
        activeSource: ActiveSource,
        updatedResults: List<DiscoverSearchResult>,
    ): DiscoverSearchResult {
        if (activeSource.sourceId == current.sourceId && activeSource.remoteBookId == current.id) {
            return updatedResults.firstOrNull { it.id == current.id } ?: current
        }
        val updatedRemote =
            current.result.copy(
                id = activeSource.remoteBookId,
                sourceId = activeSource.sourceId,
                detailUrl = activeSource.remoteBookUrl,
            )
        val score = healthTracker.healthScore(updatedRemote.sourceId)
        return current.copy(
            result = updatedRemote,
            healthScore = score,
            healthLabel = healthTracker.healthLabel(score),
        )
    }

    private fun refreshHint(
        refreshResult: RefreshRemoteBookResult,
        activeSourceId: String,
    ): String? =
        when {
            refreshResult.autoSwitched -> "已自动切换到书源：$activeSourceId"
            refreshResult.hasUpdates -> "已检测到更新"
            else -> null
        }
}
