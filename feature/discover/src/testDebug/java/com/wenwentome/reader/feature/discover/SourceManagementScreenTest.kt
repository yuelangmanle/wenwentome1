package com.wenwentome.reader.feature.discover

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.wenwentome.reader.core.model.RuleFormat
import com.wenwentome.reader.core.model.SourceDefinition
import com.wenwentome.reader.core.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SourceManagementScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deleteButton_invokesDeleteCallback() {
        var deletedSourceId: String? = null

        composeTestRule.setContent {
            SourceManagementScreen(
                state = SourceManagementUiState(sources = listOf(sampleSourceDefinitionForScreen())),
                onImportJson = {},
                onToggleSource = {},
                onDeleteSource = { deletedSourceId = it },
            )
        }

        composeTestRule.onNodeWithTag("source-delete-source-1").performClick()

        assertEquals("source-1", deletedSourceId)
    }
}

private fun sampleSourceDefinitionForScreen() =
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
