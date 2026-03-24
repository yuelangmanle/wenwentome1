# WenwenToMe 可用性抢修与界面收口 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不重做现有导航和数据骨架的前提下，优先修复 `TXT / EPUB` 本地书可读性、阅读页正文占比、发现页搜索提交、书源删除，以及“我的”页与 `API 中心` 的首屏可理解性，让当前版本重新达到“能顺手用、能继续发包验证”的水平。

**Architecture:** 保持现有 `Compose + Room + DataStore + SourceBridge + LocalBookContentRepository` 主结构不变，先在 `data:localbooks` 修正导入与正文解析回退，再在 `feature:reader`、`feature:discover`、`feature:settings`、`feature:apihub` 做最小但明确的交互层级收口。所有行为改动都先用单测或 Robolectric 屏测锁住，再做最小实现，避免再次出现“能安装但不好用”的回归。

**Tech Stack:** Kotlin、Jetpack Compose、Room、DataStore、Robolectric、JUnit4、epublib、Gradle

---

## Execution Notes

- 本计划对应已批准规格文档 [2026-03-24-usability-recovery-design.md](/Users/yueliangmanle/Desktop/codex/wenwentome/.worktrees/main-publish/docs/superpowers/specs/2026-03-24-usability-recovery-design.md)，不再拆成多个子计划，因为 `TXT / EPUB / 阅读页 / 发现页 / 我的 / API 中心` 共用同一套 app shell、数据库和测试入口。
- 实施顺序严格按用户确认的优先级执行：`TXT / EPUB` -> 阅读页 -> 发现页搜索 -> 书源删除 -> “我的”页 -> `API 中心` -> 集成验证。
- 所有编码任务先按 `@superpowers:test-driven-development` 执行；遇到“测试失败原因不明”先切到 `@superpowers:systematic-debugging`；对外宣称完成前必须走 `@superpowers:verification-before-completion`。
- `TXT` 和 `EPUB` 的回归样本优先挂到现有 `data/localbooks` 测试中；除非现有 fixture 无法表达问题，否则不要新建第二套解析链路或预解析数据库。
- `docs/superpowers/plans/2026-03-23-book-source-phase1-implementation.md` 当前是未跟踪旧文件，只能读取参考，不能修改、覆盖或误提交。
- 当前会话受限，若不能启动 reviewer subagent，则写完计划后必须做一次人工自审，逐项核对：文件路径是否真实存在、命令是否对应当前模块、测试是否与需求绑定、提交粒度是否足够小。
- 若本地缺少可用的 Android/JDK 环境，不要把未实际跑过的命令写成“已通过”；最终打包与发包只能在所有定向验证通过后再执行。

## File Map

- `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepository.kt`
  Purpose: 本地书导入事务、解析器分发、异常透传和导入回滚。
- `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/EpubBookParser.kt`
  Purpose: EPUB 导入阶段的可读性校验、标题/作者元数据提取。
- `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepository.kt`
  Purpose: TXT / EPUB 阅读时正文加载、章节解析、locator 回退与窗口裁切。
- `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/TxtBookParser.kt`
  Purpose: TXT 导入阶段的最轻量 metadata 解析；仅在必须调整导入元信息时触碰。
- `data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepositoryTest.kt`
  Purpose: 锁定本地书正文解析与章节定位回归，覆盖 `TXT` 与 `EPUB` 的真实读取行为。
- `data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepositoryTest.kt`
  Purpose: 锁定导入阶段的容错、清理回滚与用户可见错误语义。
- `data/localbooks/src/test/resources/fixtures/sample-no-chapter.txt`
  Purpose: 纯正文 `TXT` 回归样本，验证“无章节标题时也能稳定回退到正文”。
- `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt`
  Purpose: 阅读页正文布局、功能入口显隐、底部面板/浮层结构。
- `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderUiState.kt`
  Purpose: 阅读页 UI 所需状态；仅在正文优先布局确实需要新增显示状态时修改。
- `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt`
  Purpose: 锁定阅读页默认正文优先、控制面板按需展开。
- `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverUiState.kt`
  Purpose: 搜索输入态与已提交搜索态的拆分。
- `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModel.kt`
  Purpose: 搜索请求生命周期、输入态更新、正式提交搜索。
- `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverScreen.kt`
  Purpose: 搜索框、搜索按钮、键盘回车提交，以及书源管理入口。
- `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/SourceManagementScreen.kt`
  Purpose: 书源列表 UI、删除按钮与启用开关并存。
- `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/SourceManagementViewModel.kt`
  Purpose: 书源启用/删除动作桥接。
- `feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModelTest.kt`
  Purpose: 锁定“输入不等于搜索”“只有提交才发请求”。
- `feature/discover/src/testDebug/java/com/wenwentome/reader/feature/discover/DiscoverScreenTest.kt`
  Purpose: 锁定搜索按钮、IME Search 与预览区行为。
- `feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/SourceManagementViewModelTest.kt`
  Purpose: 新增书源删除 ViewModel 回归测试。
- `feature/discover/src/testDebug/java/com/wenwentome/reader/feature/discover/SourceManagementScreenTest.kt`
  Purpose: 新增书源删除按钮屏测。
- `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/SourceDefinitionDao.kt`
  Purpose: 书源本地持久化删除能力。
- `core/database/src/test/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseTest.kt`
  Purpose: 锁定 `SourceDefinitionDao` 删除行为不影响其他源。
- `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`
  Purpose: 发现页与书源管理路由接线，把 DAO 的删除动作接进 UI。
- `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SettingsScreen.kt`
  Purpose: “我的”页首页结构，控制云同步入口折叠与展开。
- `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsScreen.kt`
  Purpose: 同步表单文案与按钮区域。
- `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsUiState.kt`
  Purpose: 云同步表单状态；仅在文案或交互需要额外状态时修改。
- `feature/settings/src/test/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsViewModelTest.kt`
  Purpose: 保底验证同步配置保存逻辑不因 UI 收口被破坏。
- `feature/settings/src/testDebug/java/com/wenwentome/reader/feature/settings/SettingsScreenTest.kt`
  Purpose: 新增“我的”页首屏折叠与展开回归。
- `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubOverviewScreen.kt`
  Purpose: `API 中心` 首页新手化总览、状态摘要、二级入口分层。
- `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubUiState.kt`
  Purpose: 若首页需要更人话的摘要字段，可在这里补最小 UI state。
- `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubViewModel.kt`
  Purpose: 若首页摘要需要 ViewModel 预先组装文案，则在这里做派生。
- `feature/apihub/src/testDebug/java/com/wenwentome/reader/feature/apihub/ApiHubOverviewScreenTest.kt`
  Purpose: 锁定首页首屏文案与入口层级。
- `feature/apihub/src/test/kotlin/com/wenwentome/reader/feature/apihub/ApiHubViewModelTest.kt`
  Purpose: 若 UI state 派生发生变化，补对等单测。
- `app/src/testDebug/java/com/wenwentome/reader/AppSmokeFlowTest.kt`
  Purpose: 顶层路由烟测，覆盖“我的”页和 `API 中心` 的首屏行为没有被改坏。

### Task 1: TXT 首开慢与空正文回归修复

**Files:**
- Create: `data/localbooks/src/test/resources/fixtures/sample-no-chapter.txt`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepository.kt`
- Test: `data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepositoryTest.kt`

- [ ] **Step 1: 先写失败测试，锁定“无章节 TXT 也必须有正文”的回退语义**

```kotlin
@Test
fun load_txtWithoutChapterHeadings_fallsBackToSingleBodyChapter() = runTest {
    val repository = createTxtRepository(txtFixture = "sample-no-chapter.txt")

    val content = repository.load(bookId = "txt-book", locator = null)

    assertEquals("正文", content.chapterTitle)
    assertTrue(content.paragraphs.isNotEmpty())
    assertTrue(content.paragraphs.first().contains("第一段"))
}
```

```kotlin
@Test
fun load_txtUtf8AndGbk_neverReturnsEmptyParagraphWindowWhenTextExists() = runTest {
    listOf("sample-utf8.txt", "sample-gbk.txt").forEach { fixture ->
        val repository = createTxtRepository(txtFixture = fixture)
        val content = repository.load(bookId = "txt-book", locator = null)
        assertTrue("fixture=$fixture", content.paragraphs.isNotEmpty())
    }
}
```

- [ ] **Step 2: 运行 `localbooks` 定向测试，确认当前实现对纯正文 TXT 没有稳定回退**

Run: `./gradlew :data:localbooks:testDebugUnitTest --tests "*LocalBookContentRepositoryTest"`
Expected: FAIL，新增 `sample-no-chapter.txt` 用例会暴露“章节切分失败后正文窗口为空”或标题/段落回退错误。

- [ ] **Step 3: 在 `LocalBookContentRepository` 里实现最小 TXT 回退修复**

```kotlin
private fun buildTxtChapters(decodedText: String): List<TxtParsedChapter> {
    val paragraphs = normalizeTxtParagraphs(decodedText)
    val chapters = splitTxtChapters(paragraphs)
    if (chapters.isNotEmpty()) return chapters

    return listOf(
        TxtParsedChapter(
            title = "正文",
            chapterRef = "body",
            orderIndex = 0,
            paragraphs = paragraphs.ifEmpty { listOf("正文解析失败，请检查文件编码") },
        )
    )
}
```

```kotlin
private fun normalizeTxtParagraphs(raw: String): List<String> =
    raw.replace("\r\n", "\n")
        .replace('\r', '\n')
        .split('\n')
        .map { it.trim() }
        .filter { it.isNotBlank() }
```

- [ ] **Step 4: 运行 `data:localbooks` 全量单测，确认 TXT 回归通过且不破坏现有 EPUB 测试**

Run: `./gradlew :data:localbooks:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: 提交 TXT 回退修复**

```bash
git add data/localbooks/src/test/resources/fixtures/sample-no-chapter.txt \
  data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepository.kt \
  data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepositoryTest.kt
git commit -m "fix: stabilize txt local book fallback rendering"
```

### Task 2: EPUB 导入容错与错误语义收口

**Files:**
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/EpubBookParser.kt`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepository.kt`
- Test: `data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepositoryTest.kt`

- [ ] **Step 1: 先写失败测试，锁定“结构脏但可读”与“真坏文件”两种不同结果**

```kotlin
@Test
fun importEpub_whenValidationMetadataIsIncompleteButHtmlIsReadable_stillImports() = runTest {
    val context = createRepositoryContext()

    val result = context.repository.import(
        fileName = "loose.epub",
        inputStream = ByteArrayInputStream(createReadableLooseEpubBytes()),
    )

    assertEquals(BookFormat.EPUB, result.book.primaryFormat)
    assertTrue(context.bookAssetDao.getAll().any { it.bookId == result.book.id })
}
```

```kotlin
@Test
fun import_whenEpubIsInvalid_surfacesFriendlyMessage() = runTest {
    val context = createRepositoryContext()

    val error = runCatching {
        context.repository.import(
            fileName = "invalid.epub",
            inputStream = ByteArrayInputStream("not-a-valid-epub".encodeToByteArray()),
        )
    }.exceptionOrNull()

    assertEquals("EPUB 文件无效或已损坏", error?.message)
}
```

- [ ] **Step 2: 运行导入测试，确认当前 `validateReadableEpub` 过严或错误语义不清楚**

Run: `./gradlew :data:localbooks:testDebugUnitTest --tests "*LocalBookImportRepositoryTest"`
Expected: FAIL，新增“结构脏但可读”用例会在导入阶段被拦截，或无效文件仍然只返回技术化异常文案。

- [ ] **Step 3: 放宽 `EpubBookParser` 的导入校验，并在 import 仓储里统一对外错误文案**

```kotlin
private fun validateReadableEpub(name: String, book: Book) {
    val hasReadableSpineResource = book.spine?.spineReferences.orEmpty().any { ref ->
        ref.resource?.let(::isHtmlLike) == true
    }
    val hasReadableResourceAnywhere = book.resources?.all?.any(::isHtmlLike) == true

    if (!hasReadableSpineResource && !hasReadableResourceAnywhere) {
        throw IllegalStateException("EPUB 文件无效或已损坏")
    }
}
```

```kotlin
private fun normalizeImportError(fileName: String, error: Throwable): Nothing {
    if (fileName.endsWith(".epub", ignoreCase = true)) {
        throw IllegalStateException("EPUB 文件无效或已损坏", error)
    }
    throw error
}
```

- [ ] **Step 4: 运行 `localbooks` 全量单测，确认 EPUB 导入容错与回滚行为同时成立**

Run: `./gradlew :data:localbooks:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: 提交 EPUB 导入容错修复**

```bash
git add data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/EpubBookParser.kt \
  data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepository.kt \
  data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepositoryTest.kt
git commit -m "fix: relax epub import validation and clarify errors"
```

### Task 3: 阅读页改成正文优先、功能按需调出

**Files:**
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt`
- Test: `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt`

- [ ] **Step 1: 先写失败测试，锁定“默认不展开功能面板”和“正文区域持续存在”**

```kotlin
@Test
fun readerScreen_defaultDoesNotRenderOverlayPanels() {
    composeTestRule.setContent {
        ReaderScreen(
            state = sampleState(),
            onLocatorChanged = { _, _ -> },
            onReaderModeChange = {},
            onThemeChange = {},
            onFontSizeChange = {},
            onLineHeightChange = {},
            onBrightnessChange = {},
            onChapterSelected = {},
            onSummarizeChapter = {},
            onExplainParagraph = {},
            onTranslateParagraph = {},
            onSpeakChapter = {},
        )
    }

    composeTestRule.onNodeWithTag("reader-mode-picker").assertDoesNotExist()
    composeTestRule.onNodeWithTag("reader-settings-sheet").assertDoesNotExist()
    composeTestRule.onNodeWithTag("reader-body").assertExists()
}
```

```kotlin
@Test
fun readerScreen_openingSettingsShowsBottomPanelWithoutReplacingBody() {
    composeTestRule.setContent {
        ReaderScreen(
            state = sampleState(),
            onLocatorChanged = { _, _ -> },
            onReaderModeChange = {},
            onThemeChange = {},
            onFontSizeChange = {},
            onLineHeightChange = {},
            onBrightnessChange = {},
            onChapterSelected = {},
            onSummarizeChapter = {},
            onExplainParagraph = {},
            onTranslateParagraph = {},
            onSpeakChapter = {},
        )
    }

    composeTestRule.onNodeWithText("设置").performClick()
    composeTestRule.onNodeWithTag("reader-overlay-panel").assertExists()
    composeTestRule.onNodeWithTag("reader-body").assertExists()
}
```

- [ ] **Step 2: 运行阅读页屏测，确认当前结构把按钮和展开面板直接堆在正文前面**

Run: `./gradlew :feature:reader:testDebugUnitTest --tests "*ReaderScreenTest"`
Expected: FAIL，新增断言会证明默认结构仍会直接渲染控制面板容器，或缺少统一 overlay host。

- [ ] **Step 3: 在 `ReaderScreen` 里实现“正文满屏 + 底部面板”的最小重排**

```kotlin
private enum class ReaderOverlayPanel { MODE, TOC, SETTINGS, ASSISTANT }

var activePanel by remember { mutableStateOf<ReaderOverlayPanel?>(null) }

Box(modifier = modifier.fillMaxSize().testTag("reader-screen")) {
    ReaderBodyContainer(
        state = state,
        palette = palette,
        pagerState = pagerState,
        verticalListState = verticalListState,
        onLocatorChanged = onLocatorChanged,
        modifier = Modifier.fillMaxSize().testTag("reader-body"),
    )
    ReaderQuickActions(onOpenPanel = { activePanel = it })

    if (activePanel != null) {
        ReaderOverlayPanelHost(
            panel = activePanel!!,
            onDismiss = { activePanel = null },
            modifier = Modifier.testTag("reader-overlay-panel"),
        )
    }
}
```

- [ ] **Step 4: 运行阅读页全量屏测，确认三种阅读模式和目录/设置入口无回归**

Run: `./gradlew :feature:reader:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: 提交阅读页正文优先改造**

```bash
git add feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt \
  feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt
git commit -m "refactor: prioritize reader body over persistent panels"
```

### Task 4: 发现页增加明确搜索按钮与回车提交

**Files:**
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverUiState.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModel.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverScreen.kt`
- Test: `feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModelTest.kt`
- Test: `feature/discover/src/testDebug/java/com/wenwentome/reader/feature/discover/DiscoverScreenTest.kt`

- [ ] **Step 1: 先写失败测试，锁定“输入不等于搜索，提交才发请求”**

```kotlin
@Test
fun updateDraftQuery_doesNotTriggerBridgeSearchUntilSubmit() = runTest {
    val bridge = FakeBridgeRepository(searchResults = listOf(sampleSearchResult()))
    val viewModel = DiscoverViewModel(
        sourceBridgeRepository = bridge,
        addRemoteBookToShelf = FakeAddRemoteBookToShelfUseCase(),
        ioDispatcher = Dispatchers.Main,
    )

    viewModel.updateDraftQuery("雪中悍刀行")
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.results.isEmpty())

    viewModel.submitSearch()
    advanceUntilIdle()
    assertEquals("雪中悍刀行", viewModel.uiState.value.query)
    assertEquals(1, viewModel.uiState.value.results.size)
}
```

```kotlin
@Test
fun discoverScreen_searchButtonInvokesSubmitCallback() {
    var submitted = 0
    composeTestRule.setContent {
        DiscoverScreen(
            state = DiscoverUiState(draftQuery = "雪中"),
            onQueryChange = {},
            onSubmitSearch = { submitted++ },
            onPreview = {},
            onAddToShelf = {},
            onRefreshSelected = {},
            onReadLatest = {},
            onManageSources = {},
        )
    }

    composeTestRule.onNodeWithTag("discover-search-submit").performClick()
    assertEquals(1, submitted)
}
```

- [ ] **Step 2: 运行发现页测试，确认当前实现仍是 `onValueChange = onSearch` 的即时搜索**

Run: `./gradlew :feature:discover:testDebugUnitTest --tests "*DiscoverViewModelTest" --tests "*DiscoverScreenTest"`
Expected: FAIL，新增测试会因为 `draftQuery` / `onSubmitSearch` / 搜索按钮缺失而失败。

- [ ] **Step 3: 在状态、ViewModel 和 Screen 里做最小搜索提交拆分**

```kotlin
data class DiscoverUiState(
    val draftQuery: String = "",
    val query: String = "",
    val results: List<DiscoverSearchResult> = emptyList(),
    val selectedResultId: String? = null,
    val selectedResult: DiscoverSearchResult? = null,
    val selectedPreview: RemoteBookDetail? = null,
    val addingResultIds: Set<String> = emptySet(),
    val refreshingResultIds: Set<String> = emptySet(),
    val lastAddedTitle: String? = null,
    val enhancementHint: String? = null,
    val lastRefreshHint: String? = null,
)
```

```kotlin
fun updateDraftQuery(value: String) {
    mutableUiState.update { it.copy(draftQuery = value) }
}

fun submitSearch() {
    val query = mutableUiState.value.draftQuery.trim()
    if (query.isBlank()) {
        mutableUiState.update { it.copy(query = "", results = emptyList()) }
        return
    }
    search(query)
}
```

```kotlin
OutlinedTextField(
    value = state.draftQuery,
    onValueChange = onQueryChange,
    singleLine = true,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(onSearch = { onSubmitSearch() }),
)
```

- [ ] **Step 4: 运行发现页模块测试，确认按钮点击和回车提交都已可预测**

Run: `./gradlew :feature:discover:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: 提交发现页搜索交互修复**

```bash
git add feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverUiState.kt \
  feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModel.kt \
  feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverScreen.kt \
  feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModelTest.kt \
  feature/discover/src/testDebug/java/com/wenwentome/reader/feature/discover/DiscoverScreenTest.kt
git commit -m "fix: require explicit discover search submission"
```

### Task 5: 书源管理增加删除链路

**Files:**
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/SourceDefinitionDao.kt`
- Modify: `core/database/src/test/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseTest.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/SourceManagementScreen.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/SourceManagementViewModel.kt`
- Create: `feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/SourceManagementViewModelTest.kt`
- Create: `feature/discover/src/testDebug/java/com/wenwentome/reader/feature/discover/SourceManagementScreenTest.kt`

- [ ] **Step 1: 先写失败测试，锁定 DAO 删除、ViewModel 动作和 UI 按钮**

```kotlin
@Test
fun sourceDefinitionDao_deleteById_removesOnlyTargetSource() = runTest {
    val database = testDatabase()
    database.sourceDefinitionDao().upsertAll(
        listOf(
            SourceDefinitionEntity(sourceId = "s1", sourceName = "Alpha", enabled = true),
            SourceDefinitionEntity(sourceId = "s2", sourceName = "Beta", enabled = true),
        )
    )

    database.sourceDefinitionDao().deleteById("s1")

    val remaining = database.sourceDefinitionDao().getAll()
    assertEquals(listOf("s2"), remaining.map { it.sourceId })
}
```

```kotlin
@Test
fun deleteSource_invokesDeleteLambda() = runTest {
    var deleted: String? = null
    val viewModel = SourceManagementViewModel(
        observeSources = flowOf(listOf(sampleSourceDefinition())),
        toggleSourceEnabled = {},
        deleteSource = { deleted = it },
    )

    viewModel.delete("source-1")
    advanceUntilIdle()
    assertEquals("source-1", deleted)
}
```

```kotlin
@Test
fun sourceManagementScreen_deleteButtonInvokesCallback() {
    var deleted: String? = null
    composeTestRule.setContent {
        SourceManagementScreen(
            state = SourceManagementUiState(sources = listOf(sampleSourceDefinition())),
            onImportJson = {},
            onToggleSource = {},
            onDeleteSource = { deleted = it },
        )
    }

    composeTestRule.onNodeWithTag("source-delete-source-1").performClick()
    assertEquals("source-1", deleted)
}
```

- [ ] **Step 2: 运行数据库与发现页定向测试，确认当前链路只有开关没有删除**

Run: `./gradlew :core:database:testDebugUnitTest --tests "*ReaderDatabaseTest" :feature:discover:testDebugUnitTest --tests "*SourceManagementViewModelTest" --tests "*SourceManagementScreenTest"`
Expected: FAIL，当前缺少 `deleteById`、ViewModel 删除动作和屏幕删除按钮。

- [ ] **Step 3: 在 DAO、导航和 UI 上接通最小删除能力**

```kotlin
@Query("DELETE FROM source_definitions WHERE sourceId = :sourceId")
suspend fun deleteById(sourceId: String)
```

```kotlin
class SourceManagementViewModel(
    observeSources: Flow<List<SourceDefinition>>,
    private val toggleSourceEnabled: suspend (String) -> Unit,
    private val deleteSource: suspend (String) -> Unit,
) : ViewModel() {
    fun delete(sourceId: String) {
        viewModelScope.launch { deleteSource(sourceId) }
    }
}
```

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    TextButton(
        onClick = { onDeleteSource(source.sourceId) },
        modifier = Modifier.testTag("source-delete-${source.sourceId}"),
    ) { Text("删除") }
    Switch(checked = source.enabled, onCheckedChange = { onToggleSource(source.sourceId) })
}
```

- [ ] **Step 4: 运行数据库与发现页全量测试，确认删除后列表可刷新、开关仍可用**

Run: `./gradlew :core:database:testDebugUnitTest :feature:discover:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: 提交书源删除链路**

```bash
git add core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/SourceDefinitionDao.kt \
  core/database/src/test/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseTest.kt \
  app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt \
  feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/SourceManagementScreen.kt \
  feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/SourceManagementViewModel.kt \
  feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/SourceManagementViewModelTest.kt \
  feature/discover/src/testDebug/java/com/wenwentome/reader/feature/discover/SourceManagementScreenTest.kt
git commit -m "feat: allow deleting imported book sources"
```

### Task 6: “我的”页折叠云同步表单并改成人话文案

**Files:**
- Modify: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SettingsScreen.kt`
- Modify: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsScreen.kt`
- Create: `feature/settings/src/testDebug/java/com/wenwentome/reader/feature/settings/SettingsScreenTest.kt`
- Modify: `app/src/testDebug/java/com/wenwentome/reader/AppSmokeFlowTest.kt`

- [ ] **Step 1: 先写失败测试，锁定“我的”页首屏只显示入口，不默认展开整块表单**

```kotlin
@Test
fun settingsScreen_defaultHidesSyncForm_untilCloudSyncEntryClicked() {
    composeTestRule.setContent {
        SettingsScreen(
            state = SyncSettingsUiState(),
            projectInfo = AboutProjectInfo(
                appName = "WenwenToMe",
                authorName = "月亮慢了",
                versionName = "1.5.0",
                projectUrl = "https://github.com/example/wenwentome",
            ),
            onStateChange = {},
            onSaveConfig = {},
            onPush = {},
            onPull = {},
            onOpenApiHub = {},
            onOpenProject = {},
            onOpenChangelog = {},
        )
    }

    composeTestRule.onNodeWithTag("settings-cloud-sync-entry").assertExists()
    composeTestRule.onNodeWithText("立即备份").assertDoesNotExist()
}
```

```kotlin
@Test
fun appShell_myScreenShowsCloudSyncEntryInsteadOfImmediateForm() {
    // 进入“我的”页后，首屏应先看到“云同步”入口；点击后再出现“立即备份”
}
```

- [ ] **Step 2: 运行设置页和 app 烟测，确认当前首页仍直接渲染同步大表单**

Run: `./gradlew :feature:settings:testDebugUnitTest --tests "*SettingsScreenTest" :app:testDebugUnitTest --tests "*AppSmokeFlowTest"`
Expected: FAIL，现有 `SettingsScreen` 会直接显示 `SyncSettingsScreen` 和“立即备份”按钮。

- [ ] **Step 3: 在“我的”页实现折叠入口，并把表单标签改成人话**

```kotlin
var syncExpanded by rememberSaveable { mutableStateOf(false) }

CloudSyncEntryCard(
    expanded = syncExpanded,
    onToggle = { syncExpanded = !syncExpanded },
    modifier = Modifier.testTag("settings-cloud-sync-entry"),
)

AnimatedVisibility(visible = syncExpanded) {
    SyncSettingsScreen(
        state = state,
        onStateChange = onStateChange,
        onSaveConfig = onSaveConfig,
        onPush = onPush,
        onPull = onPull,
    )
}
```

```kotlin
OutlinedTextField(
    value = state.owner,
    onValueChange = { onStateChange(state.updateOwner(it)) },
    label = { Text("GitHub 用户名") },
)
OutlinedTextField(
    value = state.repo,
    onValueChange = { onStateChange(state.updateRepo(it)) },
    label = { Text("仓库名") },
)
OutlinedTextField(
    value = state.branch,
    onValueChange = { onStateChange(state.updateBranch(it)) },
    label = { Text("分支") },
)
OutlinedTextField(
    value = state.bootstrapToken,
    onValueChange = { onStateChange(state.updateBootstrapToken(it)) },
    label = { Text("GitHub Token") },
)
OutlinedTextField(
    value = state.syncPassword,
    onValueChange = { onStateChange(state.updateSyncPassword(it)) },
    label = { Text("同步密码") },
)
```

- [ ] **Step 4: 运行设置页与 app 烟测，确认首屏更干净但展开后功能没丢**

Run: `./gradlew :feature:settings:testDebugUnitTest :app:testDebugUnitTest --tests "*AppSmokeFlowTest"`
Expected: PASS

- [ ] **Step 5: 提交“我的”页收口**

```bash
git add feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SettingsScreen.kt \
  feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsScreen.kt \
  feature/settings/src/testDebug/java/com/wenwentome/reader/feature/settings/SettingsScreenTest.kt \
  app/src/testDebug/java/com/wenwentome/reader/AppSmokeFlowTest.kt
git commit -m "refactor: collapse sync settings behind cloud sync entry"
```

### Task 7: API 中心首页改成新手可理解的总览页

**Files:**
- Modify: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubOverviewScreen.kt`
- Modify: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubUiState.kt`
- Modify: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubViewModel.kt`
- Test: `feature/apihub/src/testDebug/java/com/wenwentome/reader/feature/apihub/ApiHubOverviewScreenTest.kt`
- Test: `feature/apihub/src/test/kotlin/com/wenwentome/reader/feature/apihub/ApiHubViewModelTest.kt`

- [ ] **Step 1: 先写失败测试，锁定首屏应该先回答“有没有配好、下一步去哪配”**

```kotlin
@Test
fun rendersBeginnerSummaryInsteadOfOnlyMetricCards() {
    composeTestRule.setContent {
        ApiHubOverviewScreen(
            state = ApiHubUiState(
                enabledProviderCount = 0,
                boundCapabilityCount = 0,
                todayCallCount = 0,
                latestError = null,
            ),
            onOpenProviders = {},
            onOpenBindings = {},
            onOpenBudgets = {},
            onOpenPrices = {},
            onOpenUsageLogs = {},
        )
    }

    composeTestRule.onNodeWithText("配置 AI").assertExists()
    composeTestRule.onNodeWithText("还没有可用接口").assertExists()
}
```

```kotlin
@Test
fun refreshState_buildsHumanReadableStatusSummary() = runTest {
    val viewModel = ApiHubViewModel(repository = FakeApiHubStore())
    advanceUntilIdle()
    assertEquals("还没有可用接口", viewModel.uiState.value.providerStatusLabel)
}
```

- [ ] **Step 2: 运行 API 中心测试，确认当前首页仍是“指标卡 + 五个按钮”的后台视图**

Run: `./gradlew :feature:apihub:testDebugUnitTest --tests "*ApiHubOverviewScreenTest" --tests "*ApiHubViewModelTest"`
Expected: FAIL，当前 `ApiHubUiState` 没有人话摘要字段，首页也没有新手引导文案。

- [ ] **Step 3: 为 `ApiHubUiState` 增加最小摘要字段，并重写首页首屏层级**

```kotlin
val providerStatusLabel: String = "还没有可用接口"
val bindingStatusLabel: String = "还没有能力绑定"
val usageStatusLabel: String = "今天还没有调用记录"
```

```kotlin
private suspend fun refreshState(selectedProviderId: String? = mutableUiState.value.selectedProviderId) {
    mutableUiState.value = mutableUiState.value.copy(
        providerStatusLabel =
            if (providers.any { it.enabled }) "已配置 ${providers.count { it.enabled }} 个可用接口"
            else "还没有可用接口",
        bindingStatusLabel =
            if (bindings.any { it.enabled }) "已绑定 ${bindings.count { it.enabled }} 项能力"
            else "还没有能力绑定",
        usageStatusLabel =
            if (todayLogs.isEmpty()) "今天还没有调用记录"
            else "今天已调用 ${todayLogs.size} 次",
    )
}
```

```kotlin
QuickStartCard(
    title = "配置 AI",
    body = state.providerStatusLabel,
    actionLabel = "去配置 Provider",
    onAction = onOpenProviders,
)
```

- [ ] **Step 4: 运行 API 中心全量测试，确认新首页更清楚且旧的二级入口仍可达**

Run: `./gradlew :feature:apihub:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: 提交 API 中心首页收口**

```bash
git add feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubOverviewScreen.kt \
  feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubUiState.kt \
  feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubViewModel.kt \
  feature/apihub/src/testDebug/java/com/wenwentome/reader/feature/apihub/ApiHubOverviewScreenTest.kt \
  feature/apihub/src/test/kotlin/com/wenwentome/reader/feature/apihub/ApiHubViewModelTest.kt
git commit -m "refactor: simplify api hub overview for new users"
```

### Task 8: 集成验证与发包前检查

**Files:**
- Verify: `data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepositoryTest.kt`
- Verify: `data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepositoryTest.kt`
- Verify: `core/database/src/test/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseTest.kt`
- Verify: `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt`
- Verify: `feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModelTest.kt`
- Verify: `feature/discover/src/testDebug/java/com/wenwentome/reader/feature/discover/DiscoverScreenTest.kt`
- Verify: `feature/settings/src/testDebug/java/com/wenwentome/reader/feature/settings/SettingsScreenTest.kt`
- Verify: `feature/apihub/src/testDebug/java/com/wenwentome/reader/feature/apihub/ApiHubOverviewScreenTest.kt`
- Verify: `app/src/testDebug/java/com/wenwentome/reader/AppSmokeFlowTest.kt`

- [ ] **Step 1: 运行所有定向测试套件，确认 5 个痛点都有自动化覆盖**

Run: `./gradlew :data:localbooks:testDebugUnitTest :core:database:testDebugUnitTest :feature:reader:testDebugUnitTest :feature:discover:testDebugUnitTest :feature:settings:testDebugUnitTest :feature:apihub:testDebugUnitTest :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 2: 运行 git 静态检查，确认没有格式性问题和残留冲突**

Run: `git diff --check`
Expected: 无输出

- [ ] **Step 3: 若本地环境可用则组装 release 包；否则只推分支触发云端构建**

Run: `./gradlew :app:assembleRelease`
Expected: PASS，生成 `app/build/outputs/apk/release/` 下的 release APK

Fallback Run: `git push origin release/v1.5-reader-recovery`
Expected: push 成功，现有 GitHub Actions / 云端发包链路启动

- [ ] **Step 4: 人工手测清单**

```text
1. 导入 UTF-8 TXT，确认可快速进入正文
2. 导入 GBK/GB18030 TXT，确认不再显示空正文
3. 导入一个真实 EPUB，确认导入成功且可打开阅读
4. 进入阅读页，确认正文占大头，目录/设置/AI 点击后才出现
5. 发现页输入书名，点击搜索按钮和按回车都能搜索
6. 书源管理删除一个无用源，确认列表立即刷新
7. “我的”页首屏不再默认露出整块同步表单
8. API 中心首页先告诉用户“有没有配置好”和“下一步去哪”
```

## Review Checklist

- [ ] 计划中的每个文件路径都在仓库中真实存在，或明确标记为需要新增的测试文件。
- [ ] 每个任务都有明确的红测、实现、绿测与提交动作。
- [ ] `TXT / EPUB`、阅读页、发现页、书源删除、“我的”页、`API 中心` 都被覆盖，没有遗漏用户原始 5 个痛点。
- [ ] 所有测试命令都对应当前 Gradle 模块名。
- [ ] 没有误引用根工作树的脏文件或未跟踪旧计划文件。
