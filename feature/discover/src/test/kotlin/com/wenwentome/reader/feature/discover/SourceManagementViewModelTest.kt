package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.core.model.RuleFormat
import com.wenwentome.reader.core.model.SourceDefinition
import com.wenwentome.reader.core.model.SourceType
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SourceManagementViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun delete_invokesDeleteLambda() = runTest {
        var deletedSourceId: String? = null
        val viewModel = SourceManagementViewModel(
            observeSources = flowOf(listOf(sampleSourceDefinition())),
            toggleSourceEnabled = {},
            deleteSource = { deletedSourceId = it },
        )

        viewModel.delete("source-1")
        advanceUntilIdle()

        assertEquals("source-1", deletedSourceId)
    }
}

internal fun sampleSourceDefinition() =
    SourceDefinition(
        sourceId = "source-1",
        sourceName = "测试书源",
        sourceType = SourceType.IMPORTED,
        ruleFormat = RuleFormat.CUSTOM,
        sourceUrl = "https://example.com/source.json",
        rawDefinition = "{}",
        enabled = true,
        group = "默认分组",
    )
