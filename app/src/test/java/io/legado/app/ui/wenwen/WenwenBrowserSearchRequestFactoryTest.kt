package io.legado.app.ui.wenwen

import com.wenwentome.reader.core.model.BrowserFindPreferences
import com.wenwentome.reader.core.model.BrowserMode
import com.wenwentome.reader.core.model.BrowserSearchEnginePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WenwenBrowserSearchRequestFactoryTest {

    @Test
    fun create_returnsNullWhenQueryIsBlank() {
        val request = WenwenBrowserSearchRequestFactory.create("   ", BrowserFindPreferences())

        assertNull(request)
    }

    @Test
    fun create_buildsSearchRequestWithWenwenBrowserFlags() {
        val request =
            WenwenBrowserSearchRequestFactory.create(
                query = "雪中悍刀行 最新章节",
                preferences =
                    BrowserFindPreferences(
                        defaultSearchEngineId = BrowserSearchEnginePreset.BAIDU.id,
                        browserMode = BrowserMode.READER,
                        autoOptimizeReading = true,
                        showManualOptimizeFloatingButton = false,
                    ),
            )

        requireNotNull(request)
        assertEquals("百度 搜索", request.title)
        assertEquals("浏览器找书 · 智能阅读", request.sourceName)
        assertTrue(request.enableWenwenBrowserMode)
        assertTrue(request.autoOptimizeReading)
        assertFalse(request.showManualOptimizeFloatingButton)
        assertTrue(request.url.startsWith("https://www.baidu.com/s?wd="))
        assertEquals("雪中悍刀行 最新章节", request.query)
    }
}
