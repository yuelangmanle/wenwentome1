package com.wenwentome.reader.core.model

import java.net.URLEncoder

enum class BrowserMode(val label: String) {
    STANDARD("标准网页"),
    IMMERSIVE("沉浸网页"),
    READER("智能阅读"),
}

enum class BrowserSearchEnginePreset(
    val id: String,
    val label: String,
    val queryUrlTemplate: String?,
) {
    BING("bing", "必应", "https://www.bing.com/search?q={query}"),
    BAIDU("baidu", "百度", "https://www.baidu.com/s?wd={query}"),
    SOGOU("sogou", "搜狗", "https://www.sogou.com/web?query={query}"),
    SOHU("sohu", "搜狐", "https://so.toutiao.com/search?dvpf=pc&keyword={query}"),
    SHENMA("shenma", "神马", "https://yz.m.sm.cn/s?q={query}"),
    GOOGLE("google", "谷歌", "https://www.google.com/search?q={query}"),
    CUSTOM("custom", "自定义", null),
    ;

    companion object {
        fun fromId(id: String?): BrowserSearchEnginePreset? = entries.firstOrNull { it.id == id }
    }
}

data class BrowserSearchEngine(
    val id: String,
    val label: String,
    val queryUrlTemplate: String,
    val builtIn: Boolean,
)

data class BrowserFindPreferences(
    val defaultSearchEngineId: String = BrowserSearchEnginePreset.BING.id,
    val customSearchEngineName: String = "",
    val customSearchUrlTemplate: String = "",
    val browserMode: BrowserMode = BrowserMode.READER,
    val autoOptimizeReading: Boolean = true,
    val showManualOptimizeFloatingButton: Boolean = true,
) {
    fun availableSearchEngines(): List<BrowserSearchEngine> {
        val builtIns =
            BrowserSearchEnginePreset.entries
                .filter { it != BrowserSearchEnginePreset.CUSTOM }
                .map { preset ->
                    BrowserSearchEngine(
                        id = preset.id,
                        label = preset.label,
                        queryUrlTemplate = preset.queryUrlTemplate.orEmpty(),
                        builtIn = true,
                    )
                }
        val custom = customEngineOrNull()
        return if (custom != null) builtIns + custom else builtIns
    }

    fun activeSearchEngine(): BrowserSearchEngine {
        val custom = customEngineOrNull()
        return when (defaultSearchEngineId) {
            BrowserSearchEnginePreset.CUSTOM.id -> custom ?: builtInEngine(BrowserSearchEnginePreset.BING)
            else -> {
                val preset =
                    BrowserSearchEnginePreset.fromId(defaultSearchEngineId)
                        ?.takeIf { it != BrowserSearchEnginePreset.CUSTOM }
                        ?: BrowserSearchEnginePreset.BING
                builtInEngine(preset)
            }
        }
    }

    fun buildSearchUrl(query: String): String {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        return activeSearchEngine().queryUrlTemplate.replace("{query}", encodedQuery)
    }

    private fun customEngineOrNull(): BrowserSearchEngine? {
        if (customSearchUrlTemplate.isBlank()) return null
        return BrowserSearchEngine(
            id = BrowserSearchEnginePreset.CUSTOM.id,
            label = customSearchEngineName.ifBlank { BrowserSearchEnginePreset.CUSTOM.label },
            queryUrlTemplate = customSearchUrlTemplate,
            builtIn = false,
        )
    }

    private fun builtInEngine(preset: BrowserSearchEnginePreset): BrowserSearchEngine {
        return BrowserSearchEngine(
            id = preset.id,
            label = preset.label,
            queryUrlTemplate = preset.queryUrlTemplate.orEmpty(),
            builtIn = true,
        )
    }
}
