package io.legado.app.ui.wenwen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.wenwentome.reader.core.database.datastore.ReaderPreferencesStore
import com.wenwentome.reader.core.model.BrowserFindPreferences
import com.wenwentome.reader.core.model.BrowserMode
import com.wenwentome.reader.core.model.BrowserSearchEnginePreset
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WenwenDiscoverSettingsActivity : AppCompatActivity() {

    private val preferencesStore by lazy {
        ReaderPreferencesStore(applicationContext)
    }
    private var uiState = mutableStateOf(WenwenBrowserSettingsUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialSection = intent.getStringExtra(EXTRA_SECTION) ?: SECTION_ENGINES
        uiState.value = uiState.value.copy(section = initialSection)
        lifecycleScope.launch {
            val prefs = preferencesStore.browserFindPrefs.first()
            uiState.value = WenwenBrowserSettingsUiState.fromPreferences(prefs, initialSection)
        }
        setContent {
            MaterialTheme {
                WenwenDiscoverSettingsScreen(
                    state = uiState.value,
                    onBackClick = ::finish,
                    onSectionChange = { section ->
                        uiState.value = uiState.value.copy(section = section)
                    },
                    onEngineSelected = { engineId ->
                        uiState.value = uiState.value.copy(defaultSearchEngineId = engineId)
                    },
                    onCustomNameChange = { value ->
                        uiState.value = uiState.value.copy(customSearchEngineName = value)
                    },
                    onCustomUrlChange = { value ->
                        uiState.value = uiState.value.copy(customSearchUrlTemplate = value)
                    },
                    onBrowserModeChange = { mode ->
                        uiState.value = uiState.value.copy(browserMode = mode)
                    },
                    onAutoOptimizeChange = { value ->
                        uiState.value = uiState.value.copy(autoOptimizeReading = value)
                    },
                    onManualOptimizeChange = { value ->
                        uiState.value = uiState.value.copy(showManualOptimizeFloatingButton = value)
                    },
                    onSave = {
                        val currentState = uiState.value
                        if (
                            currentState.defaultSearchEngineId == BrowserSearchEnginePreset.CUSTOM.id &&
                            currentState.customSearchUrlTemplate.isBlank()
                        ) {
                            toastOnUi("自定义搜索引擎至少要填写搜索 URL 模板。")
                        } else if (
                            currentState.defaultSearchEngineId == BrowserSearchEnginePreset.CUSTOM.id &&
                            !currentState.customSearchUrlTemplate.contains("{query}")
                        ) {
                            toastOnUi("自定义搜索 URL 模板里需要包含 `{query}` 占位符。")
                        } else {
                            lifecycleScope.launch {
                                preferencesStore.saveBrowserFindPrefs(currentState.toPreferences())
                                toastOnUi("浏览器找书设置已保存。")
                            }
                        }
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_SECTION = "section"
        const val SECTION_ENGINES = "engines"
        const val SECTION_BROWSER = "browser"

        fun open(context: Context, section: String) {
            context.startActivity(
                Intent(context, WenwenDiscoverSettingsActivity::class.java).apply {
                    putExtra(EXTRA_SECTION, section)
                }
            )
        }
    }
}

private enum class DiscoverSettingsSection {
    ENGINES,
    BROWSER,
}

private data class WenwenBrowserSettingsUiState(
    val section: String = WenwenDiscoverSettingsActivity.SECTION_ENGINES,
    val defaultSearchEngineId: String = BrowserSearchEnginePreset.BING.id,
    val customSearchEngineName: String = "",
    val customSearchUrlTemplate: String = "",
    val browserMode: BrowserMode = BrowserMode.READER,
    val autoOptimizeReading: Boolean = true,
    val showManualOptimizeFloatingButton: Boolean = true,
) {
    fun toPreferences(): BrowserFindPreferences =
        BrowserFindPreferences(
            defaultSearchEngineId = defaultSearchEngineId,
            customSearchEngineName = customSearchEngineName,
            customSearchUrlTemplate = customSearchUrlTemplate,
            browserMode = browserMode,
            autoOptimizeReading = autoOptimizeReading,
            showManualOptimizeFloatingButton = showManualOptimizeFloatingButton,
        )

    companion object {
        fun fromPreferences(
            prefs: BrowserFindPreferences,
            section: String,
        ): WenwenBrowserSettingsUiState =
            WenwenBrowserSettingsUiState(
                section = section,
                defaultSearchEngineId = prefs.defaultSearchEngineId,
                customSearchEngineName = prefs.customSearchEngineName,
                customSearchUrlTemplate = prefs.customSearchUrlTemplate,
                browserMode = prefs.browserMode,
                autoOptimizeReading = prefs.autoOptimizeReading,
                showManualOptimizeFloatingButton = prefs.showManualOptimizeFloatingButton,
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WenwenDiscoverSettingsScreen(
    state: WenwenBrowserSettingsUiState,
    onBackClick: () -> Unit,
    onSectionChange: (String) -> Unit,
    onEngineSelected: (String) -> Unit,
    onCustomNameChange: (String) -> Unit,
    onCustomUrlChange: (String) -> Unit,
    onBrowserModeChange: (BrowserMode) -> Unit,
    onAutoOptimizeChange: (Boolean) -> Unit,
    onManualOptimizeChange: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    val section =
        if (state.section == WenwenDiscoverSettingsActivity.SECTION_BROWSER) {
            DiscoverSettingsSection.BROWSER
        } else {
            DiscoverSettingsSection.ENGINES
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("浏览器找书设置") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("返回")
                    }
                },
                actions = {
                    TextButton(onClick = onSave) {
                        Text("保存")
                    }
                },
            )
        },
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = section == DiscoverSettingsSection.ENGINES,
                        onClick = { onSectionChange(WenwenDiscoverSettingsActivity.SECTION_ENGINES) },
                        label = { Text("搜索引擎") },
                    )
                    FilterChip(
                        selected = section == DiscoverSettingsSection.BROWSER,
                        onClick = { onSectionChange(WenwenDiscoverSettingsActivity.SECTION_BROWSER) },
                        label = { Text("浏览器模式") },
                    )
                }
                when (section) {
                    DiscoverSettingsSection.ENGINES -> EngineSection(
                        state = state,
                        onSelectEngine = onEngineSelected,
                        onCustomNameChange = onCustomNameChange,
                        onCustomUrlChange = onCustomUrlChange,
                    )
                    DiscoverSettingsSection.BROWSER -> BrowserSection(
                        browserMode = state.browserMode,
                        onSelectMode = onBrowserModeChange,
                        autoOptimize = state.autoOptimizeReading,
                        onAutoOptimizeChange = onAutoOptimizeChange,
                        manualOptimize = state.showManualOptimizeFloatingButton,
                        onManualOptimizeChange = onManualOptimizeChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun EngineSection(
    state: WenwenBrowserSettingsUiState,
    onSelectEngine: (String) -> Unit,
    onCustomNameChange: (String) -> Unit,
    onCustomUrlChange: (String) -> Unit,
) {
    val engines = BrowserSearchEnginePreset.entries
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsInfoCard(
            title = "搜索引擎管理",
            body = "发现页搜索框左侧的小标签走快速切换。这里保留完整管理入口，可切换内置引擎，也可保存自定义搜索模板。",
        )
        engines.forEach { engine ->
            SelectableCard(
                title = engine.label,
                summary =
                    if (engine.id == state.defaultSearchEngineId) {
                        "当前默认搜索引擎"
                    } else {
                        "点击切换为默认引擎"
                    },
                selected = engine.id == state.defaultSearchEngineId,
                onClick = { onSelectEngine(engine.id) },
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("自定义引擎", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "使用 `{query}` 作为搜索词占位符，例如 `https://example.com/search?q={query}`。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.customSearchEngineName,
                    onValueChange = onCustomNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("自定义引擎名称") },
                )
                OutlinedTextField(
                    value = state.customSearchUrlTemplate,
                    onValueChange = onCustomUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索 URL 模板") },
                )
            }
        }
    }
}

@Composable
private fun BrowserSection(
    browserMode: BrowserMode,
    onSelectMode: (BrowserMode) -> Unit,
    autoOptimize: Boolean,
    onAutoOptimizeChange: (Boolean) -> Unit,
    manualOptimize: Boolean,
    onManualOptimizeChange: (Boolean) -> Unit,
) {
    val modes = BrowserMode.entries
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsInfoCard(
            title = "浏览器模式",
            body = "标准网页保留原站点样式，沉浸网页简化 UI，智能阅读会优先抽取正文并切入阅读视图。",
        )
        modes.forEach { mode ->
            SelectableCard(
                title = mode.label,
                summary = if (mode == browserMode) "当前浏览器模式" else "点击切换此模式",
                selected = mode == browserMode,
                onClick = { onSelectMode(mode) },
            )
        }
        ToggleCard(
            title = "自动优化阅读",
            summary = "进入网页后自动检测正文成分，命中后直接切换优化阅读。",
            checked = autoOptimize,
            onCheckedChange = onAutoOptimizeChange,
        )
        ToggleCard(
            title = "保留悬浮手动优化",
            summary = "关闭自动模式时，允许长按悬浮入口手动触发正文优化。",
            checked = manualOptimize,
            onCheckedChange = onManualOptimizeChange,
        )
    }
}

@Composable
private fun SelectableCard(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingsInfoCard(
    title: String,
    body: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
