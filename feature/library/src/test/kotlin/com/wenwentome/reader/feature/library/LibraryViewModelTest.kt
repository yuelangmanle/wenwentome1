package com.wenwentome.reader.feature.library

import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryViewModelTest {
    @Test
    fun defaultShelf_mergesLocalAndRemoteBooksIntoSingleList() = runTest {
        val viewModel = LibraryViewModel(
            observeBookshelf = fakeObserveBookshelfUseCase(
                flowOf(
                    listOf(
                        BookRecord.newLocal("悉达多", "黑塞", BookFormat.EPUB),
                        BookRecord(
                            id = "web-1",
                            title = "雪中悍刀行",
                            author = "烽火戏诸侯",
                            originType = OriginType.WEB,
                            primaryFormat = BookFormat.WEB,
                        ),
                    )
                )
            ),
            importLocalBook = { _ -> },
        )

        val state = viewModel.uiState.first()
        assertEquals(2, state.visibleBooks.size)
    }

    private fun fakeObserveBookshelfUseCase(flow: Flow<List<BookRecord>>): ObserveBookshelfUseCase =
        ObserveBookshelfUseCase { flow }
}

