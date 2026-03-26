package com.wenwentome.reader.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserFindPreferencesTest {

    @Test
    fun buildSearchUrl_usesBuiltinEngineTemplate() {
        val prefs = BrowserFindPreferences(defaultSearchEngineId = BrowserSearchEnginePreset.BING.id)

        val url = prefs.buildSearchUrl("雪中悍刀行 最新章节")

        assertEquals(
            "https://www.bing.com/search?q=%E9%9B%AA%E4%B8%AD%E6%82%8D%E5%88%80%E8%A1%8C+%E6%9C%80%E6%96%B0%E7%AB%A0%E8%8A%82",
            url,
        )
    }

    @Test
    fun activeSearchEngine_prefersCustomEngineWhenConfigured() {
        val prefs =
            BrowserFindPreferences(
                defaultSearchEngineId = BrowserSearchEnginePreset.CUSTOM.id,
                customSearchEngineName = "起点站内",
                customSearchUrlTemplate = "https://example.com/search?keyword={query}",
            )

        val engine = prefs.activeSearchEngine()

        assertEquals("custom", engine.id)
        assertEquals("起点站内", engine.label)
        assertEquals(
            "https://example.com/search?keyword=%E9%9B%AA%E4%B8%AD",
            prefs.buildSearchUrl("雪中"),
        )
        assertTrue(prefs.availableSearchEngines().any { it.id == BrowserSearchEnginePreset.CUSTOM.id })
    }
}
