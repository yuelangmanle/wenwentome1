package io.legado.app.ui.wenwen

import android.content.Context
import com.wenwentome.reader.core.model.BrowserFindPreferences
import io.legado.app.ui.browser.WebViewActivity
import io.legado.app.utils.startActivity
import io.legado.app.utils.toastOnUi

fun Context.startWenwenBrowserSearch(
    query: String,
    preferences: BrowserFindPreferences,
) {
    val request = WenwenBrowserSearchRequestFactory.create(query, preferences)
    if (request == null) {
        toastOnUi("先输入要搜索的小说或关键词。")
        return
    }
    startActivity<WebViewActivity> {
        putExtra("title", request.title)
        putExtra("url", request.url)
        putExtra("sourceName", request.sourceName)
        putExtra(EXTRA_WENWEN_BROWSER_MODE_ENABLED, request.enableWenwenBrowserMode)
        putExtra(EXTRA_WENWEN_BROWSER_SEARCH_QUERY, request.query)
        putExtra(EXTRA_WENWEN_BROWSER_AUTO_OPTIMIZE, request.autoOptimizeReading)
        putExtra(
            EXTRA_WENWEN_BROWSER_SHOW_MANUAL_BUTTON,
            request.showManualOptimizeFloatingButton,
        )
    }
}
