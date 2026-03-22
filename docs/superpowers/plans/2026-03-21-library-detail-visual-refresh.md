# Library & Detail Visual Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将书架页和书籍详情页升级为“轻拟物精装书城版”视觉，同时保持当前 `TXT / EPUB / WEB` 阅读主链、继续阅读入口和关键 UI 测试稳定。

**Architecture:** 本轮只重排 `feature/library` 与 `feature/reader` 的 Compose UI 结构，不改阅读协议、导航协议或持久化模型。书架页重构为“顶部陈列区 + 下方藏书墙”，详情页重构为“作品展示区 / 阅读状态区 / 封面管理区 / 目录区”，并通过稳定的 test tag 把 UI 改版锚定到可验证边界上。

**Tech Stack:** Kotlin、Jetpack Compose Material 3、Robolectric Compose UI tests、GitHub Actions `android-ci`

---

## 实施前提与约束

- 当前批准的规格文档：
  - `docs/superpowers/specs/2026-03-21-library-detail-visual-refresh-design.md`
- 当前必须保持稳定的已有入口：
  - `continue-reading-card`
  - `book-cover-card-<bookId>`
  - `book-detail`
  - `detail-read-button`
  - `detail-progress-label`
  - `cover-import-photo-button`
- 当前环境仍可能缺少本地 Java；如果 `./gradlew` 被 `Unable to locate a Java Runtime` 阻塞，则把模块测试改为 GitHub Actions 云端验证，不要在本地环境问题上卡住实现。

## 文件结构与职责

### 主要修改文件

- `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt`
  - 负责书架页整体骨架、顶部陈列区、下方藏书墙区和菜单职责边界
- `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ContinueReadingCard.kt`
  - 负责继续阅读主卡的“书桌陈列感”布局与状态呈现
- `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt`
  - 负责单本书卡片的书本比例、书脊暗示、状态角标和进度文案
- `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookActionsMenu.kt`
  - 负责书架长按菜单的二级快捷入口，只保留详情与网文章节刷新
- `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailScreen.kt`
  - 负责详情页四段结构、封面优先首屏、状态区、封面管理区和目录区

### 主要测试文件

- `feature/library/src/testDebug/java/com/wenwentome/reader/feature/library/LibraryScreenTest.kt`
  - 锁定书架页新的结构 test tag 与长按菜单职责边界
- `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/BookDetailScreenTest.kt`
  - 锁定详情页四段结构、封面管理入口和主按钮存在性
- `app/src/testDebug/java/com/wenwentome/reader/AppSmokeFlowTest.kt`
  - 校验从书架/详情页进入阅读器的 app 级主链未回退
- `app/src/testDebug/java/com/wenwentome/reader/AppReaderFlowTest.kt`
  - 校验继续阅读与网文详情主链未因 UI 重排被打断

### 建议新增的结构锚点

以下 test tag 应作为本轮 UI 重构的稳定边界：

- `library-hero-section`
- `library-grid-section`
- `detail-hero-section`
- `detail-reading-status-section`
- `detail-cover-management-section`
- `detail-catalog-section`

## Task 1: 先用测试锁定 UI 合同

**Files:**
- Modify: `feature/library/src/testDebug/java/com/wenwentome/reader/feature/library/LibraryScreenTest.kt`
- Modify: `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/BookDetailScreenTest.kt`

- [ ] **Step 1: 为书架页写失败中的结构与菜单边界测试**

```kotlin
@Test
fun rendersHeroAndGridSections_andLongPressMenuIsNowLimited() {
    composeTestRule.setContent {
        LibraryScreen(
            state = sampleState(),
            onImportClick = {},
            onContinueReadingClick = {},
            onBookClick = {},
            onImportPhoto = {},
            onRefreshCover = {},
            onRestoreAutomaticCover = {},
            onRefreshCatalog = {},
        )
    }

    composeTestRule.onNodeWithTag("library-hero-section").assertExistsCompat()
    composeTestRule.onNodeWithTag("library-grid-section").assertExistsCompat()
    composeTestRule.onNodeWithTag("book-cover-card-book-1").performTouchInput { longClick() }
    composeTestRule.onNodeWithText("打开详情").assertExistsCompat()
    composeTestRule.onNodeWithText("刷新目录").assertExistsCompat()
    composeTestRule.onNodeWithText("导入照片").assertDoesNotExist()
    composeTestRule.onNodeWithText("刷新封面").assertDoesNotExist()
    composeTestRule.onNodeWithText("恢复自动封面").assertDoesNotExist()
}
```

- [ ] **Step 2: 运行书架测试，确认它因缺少新结构或菜单边界而失败**

Run:

```bash
./gradlew :feature:library:testDebugUnitTest --tests "com.wenwentome.reader.feature.library.LibraryScreenTest" --stacktrace
```

Expected:

```text
FAILURE: ... LibraryScreenTest ... could not find node with tag library-hero-section
```

- [ ] **Step 3: 为详情页写失败中的四段结构测试**

```kotlin
@Test
fun detailScreen_rendersHeroStatusCoverManagementAndCatalogSections() {
    composeTestRule.setContent {
        BookDetailScreen(
            state = sampleState(),
            onReadClick = {},
            onToggleCatalog = {},
            onChapterClick = {},
            onRefreshCatalogClick = {},
            onJumpToLatestClick = {},
            onRefreshCoverClick = {},
            onImportPhotoClick = {},
            onRestoreAutomaticCoverClick = {},
        )
    }

    composeTestRule.onNodeWithTag("detail-hero-section").assertExistsCompat()
    composeTestRule.onNodeWithTag("detail-reading-status-section").assertExistsCompat()
    composeTestRule.onNodeWithTag("detail-cover-management-section").assertExistsCompat()
    composeTestRule.onNodeWithTag("detail-catalog-section").assertExistsCompat()
    composeTestRule.onNodeWithTag("detail-read-button").assertExistsCompat()
    composeTestRule.onNodeWithTag("detail-progress-label").assertExistsCompat()
}
```

- [ ] **Step 4: 运行详情页测试，确认它先失败**

Run:

```bash
./gradlew :feature:reader:testDebugUnitTest --tests "com.wenwentome.reader.feature.reader.BookDetailScreenTest" --stacktrace
```

Expected:

```text
FAILURE: ... BookDetailScreenTest ... could not find node with tag detail-hero-section
```

- [ ] **Step 5: 提交只包含测试合同的变更**

```bash
git add \
  feature/library/src/testDebug/java/com/wenwentome/reader/feature/library/LibraryScreenTest.kt \
  feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/BookDetailScreenTest.kt
git commit -m "test: lock visual refresh ui contracts"
```

## Task 2: 收紧书架长按菜单边界并搭起顶部陈列区骨架

**Files:**
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ContinueReadingCard.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookActionsMenu.kt`
- Test: `feature/library/src/testDebug/java/com/wenwentome/reader/feature/library/LibraryScreenTest.kt`

- [ ] **Step 1: 在 `LibraryScreen` 中引入新的页面分区与 test tag**

```kotlin
item(span = { GridItemSpan(maxLineSpan) }) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("library-hero-section")
    ) {
        LibraryHeroHeader(...)
        ContinueReadingCard(
            item = item,
            onClick = { onContinueReadingClick(item.book.id) },
            modifier = Modifier.testTag("continue-reading-card"),
        )
    }
}

item(span = { GridItemSpan(maxLineSpan) }) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("library-grid-section")
    ) {
        Text("藏书墙")
    }
}
```

- [ ] **Step 2: 把 `ContinueReadingCard` 改成“主书封面 + 状态列”的陈列卡**

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(18.dp),
) {
    BookshelfHeroCover(...)
    Column(modifier = Modifier.weight(1f)) {
        Text("继续阅读")
        Text(item.book.title, maxLines = 2)
        Text(item.book.author ?: "未知作者")
        Text(item.progressLabel)
        LinearProgressIndicator(progress = { item.progressPercent.coerceIn(0f, 1f) })
        FilledTonalButton(onClick = onClick) { Text("继续阅读") }
    }
}
```

- [ ] **Step 3: 把 `BookActionsMenu` 缩成真正的二级快捷菜单**

```kotlin
ActionButton("打开详情") { ... }
onRefreshCatalog?.let { refresh ->
    ActionButton("刷新目录") {
        onDismiss()
        refresh()
    }
}
```

删除：

```kotlin
ActionButton("刷新封面") { ... }
ActionButton("导入照片") { ... }
ActionButton("恢复自动封面") { ... }
```

- [ ] **Step 4: 运行书架测试，确认新结构和菜单边界通过**

Run:

```bash
./gradlew :feature:library:testDebugUnitTest --tests "com.wenwentome.reader.feature.library.LibraryScreenTest" --stacktrace
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 5: 提交书架骨架与菜单职责边界**

```bash
git add \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ContinueReadingCard.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookActionsMenu.kt \
  feature/library/src/testDebug/java/com/wenwentome/reader/feature/library/LibraryScreenTest.kt
git commit -m "feat: reshape library hero and menu boundaries"
```

## Task 3: 落地“像一本书”的藏书墙卡片

**Files:**
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt`
- Test: `feature/library/src/testDebug/java/com/wenwentome/reader/feature/library/LibraryScreenTest.kt`

- [ ] **Step 1: 在 `BookCoverCard` 中加入书脊、高光、状态角标和进度层，并保留真实封面优先显示**

```kotlin
Box(modifier = Modifier.aspectRatio(0.68f)) {
    if (coverBitmap != null) {
        RealCoverSurface(bitmap = requireNotNull(coverBitmap), title = item.book.title)
    } else {
        PlaceholderCoverSurface(
            modifier = Modifier.testTag("book-cover-placeholder-${item.book.id}"),
            title = item.book.title,
            sourceLabel = if (item.book.originType == OriginType.LOCAL) "本地" else "网文",
        )
    }

    if (item.hasUpdates) {
        UpdateBadge(
            modifier = Modifier.align(Alignment.TopEnd),
            text = "更新",
        )
    }
}
```

其中占位封面至少要包含：

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(brush = Brush.verticalGradient(...))
) {
    Box(modifier = Modifier.fillMaxHeight().width(16.dp).align(Alignment.CenterStart))
    Text(item.book.title, maxLines = 4)
}
```

- [ ] **Step 2: 扩充测试样本，验证真实封面路径不会退化成占位封面**

```kotlin
@Test
fun prefersRealCoverOverPlaceholderWhenFileCoverExists() {
    val coverUri = createTempCoverUri()

    composeTestRule.setContent {
        BookCoverCard(
            item = sampleBookWithFileCover(coverUri),
            onClick = {},
            onLongClick = {},
            modifier = Modifier.testTag("book-cover-card-book-covered"),
        )
    }

    composeTestRule.onNodeWithTag("book-cover-placeholder-book-covered").assertDoesNotExist()
}
```

- [ ] **Step 3: 调整 `LibraryScreen` 栅格密度，保证卡片仍是“藏书墙”而不是内容瀑布流**

```kotlin
LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 156.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
)
```

- [ ] **Step 4: 重新运行书架测试，确认真实封面优先、card tag 与继续阅读入口都还在**

Run:

```bash
./gradlew :feature:library:testDebugUnitTest --tests "com.wenwentome.reader.feature.library.LibraryScreenTest" --stacktrace
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 5: 提交藏书墙卡片视觉落地**

```bash
git add \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt \
  feature/library/src/testDebug/java/com/wenwentome/reader/feature/library/LibraryScreenTest.kt
git commit -m "feat: style library cards as bookshelf books"
```

## Task 4: 把详情页改成四段结构并保留封面管理闭环

**Files:**
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailScreen.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailUiState.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailViewModel.kt`
- Test: `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/BookDetailScreenTest.kt`

- [ ] **Step 1: 把详情页首屏重排成封面优先的作品展示区**

```kotlin
item {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("detail-hero-section")
    ) {
        DetailHeroCard(
            cover = state.effectiveCover,
            title = book?.title.orEmpty(),
            author = book?.author,
            summary = book?.summary,
            lastReadLabel = state.lastReadLabel,
        )
    }
}
```

- [ ] **Step 2: 把进度和目录动作集中到阅读状态区**

```kotlin
item {
    Card(modifier = Modifier.testTag("detail-reading-status-section")) {
        Column {
            Text("阅读状态")
            LinearProgressIndicator(
                progress = { state.progressPercent.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = state.progressLabel,
                modifier = Modifier.testTag("detail-progress-label"),
            )
            state.lastReadTimeLabel?.let { Text(it) }
            state.currentChapterTitle?.let { Text("当前章节 $it") }
            Button(
                onClick = onReadClick,
                modifier = Modifier.testTag("detail-read-button"),
            ) { Text(state.readActionLabel) }
            OutlinedButton(onClick = { showCatalog = !showCatalog; onToggleCatalog() }) { Text("查看目录") }
        }
    }
}
```

- [ ] **Step 3: 在 `BookDetailViewModel` / `BookDetailUiState` 中只补轻量展示字段，不改持久化协议**

```kotlin
data class BookDetailUiState(
    ...
    val currentChapterTitle: String? = null,
    val lastReadTimeLabel: String? = null,
)
```

```kotlin
BookDetailUiState(
    ...
    currentChapterTitle = currentChapterTitle,
    lastReadTimeLabel = readingState?.updatedAt?.let(::formatLastReadTimeLabel),
)
```

要求：

- 只使用现有 `ReadingState.updatedAt`、`chapterRef` 和 `chapters`
- 不新增 Room 字段
- 不改阅读状态存储协议
- 如果最终文案需要降级，也必须保留 `currentChapterTitle` 或 `lastReadLabel` 之一，不能把阅读状态区做成纯装饰

- [ ] **Step 4: 把封面操作集中到独立的封面管理区，并放到目录区前面**

```kotlin
item {
    Card(modifier = Modifier.testTag("detail-cover-management-section")) {
        BookCoverActionSheet(
            onRefreshCover = onRefreshCoverClick,
            onImportPhoto = onImportPhotoClick,
            onRestoreAutomaticCover = if (state.canRestoreAutomaticCover) {
                onRestoreAutomaticCoverClick
            } else {
                null
            },
        )
    }
}
```

目录区外层应新增：

```kotlin
modifier = Modifier.testTag("detail-catalog-section")
```

- [ ] **Step 5: 运行详情页测试，确认四段结构、状态字段、主按钮与封面管理入口都通过**

Run:

```bash
./gradlew :feature:reader:testDebugUnitTest --tests "com.wenwentome.reader.feature.reader.BookDetailScreenTest" --stacktrace
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 6: 提交详情页重排**

```bash
git add \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailScreen.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailUiState.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailViewModel.kt \
  feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/BookDetailScreenTest.kt
git commit -m "feat: reorganize book detail into four sections"
```

## Task 5: 做 app 级回归验证并走云端 CI 收口

**Files:**
- Inspect / Modify if needed: `app/src/testDebug/java/com/wenwentome/reader/AppSmokeFlowTest.kt`
- Inspect / Modify if needed: `app/src/testDebug/java/com/wenwentome/reader/AppReaderFlowTest.kt`
- Inspect / Modify if needed: `docs/development-progress.md`

- [ ] **Step 1: 运行 app 级测试，确认从书架与详情页进入阅读器的主链没断**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.wenwentome.reader.AppSmokeFlowTest" --tests "com.wenwentome.reader.AppReaderFlowTest" --stacktrace
```

Expected:

```text
BUILD SUCCESSFUL
```

如果本地仍被 Java 环境阻塞，改用云端：

```bash
git push origin HEAD
gh run watch --exit-status
```

Expected:

```text
completed with 'success'
```

- [ ] **Step 2: 如果 app 测试因滚动目标或布局变动失败，只修测试锚点，不回退既定 UI 结构**

优先保留并继续使用：

```kotlin
onNodeWithTag("book-cover-card-$bookId")
onNodeWithTag("book-detail")
onNodeWithTag("detail-read-button")
onNodeWithTag("continue-reading-card")
```

- [ ] **Step 3: 同步开发进度文档，记录本次视觉改版落地与验证结果**

```markdown
- `1.1 / Task 8` 书架页与详情页视觉改版完成，已切到轻拟物书城风格
- 保持 `continue-reading-card`、`detail-read-button` 等关键测试入口稳定
```

- [ ] **Step 4: 运行最终验证并记录 run id**

Run:

```bash
gh run list --limit 5
```

Expected:

```text
android-ci ... success
```

- [ ] **Step 5: 提交回归修正与文档同步**

```bash
git add \
  docs/development-progress.md \
  app/src/testDebug/java/com/wenwentome/reader/AppSmokeFlowTest.kt \
  app/src/testDebug/java/com/wenwentome/reader/AppReaderFlowTest.kt
git commit -m "test: verify visual refresh app flows"
```

## 执行备注

- 推荐按任务顺序执行，不要同时改书架和详情页大块布局，否则测试失败定位会变慢。
- 执行每个任务前，优先遵守 `@superpowers:test-driven-development`。
- 每个任务收口前，优先遵守 `@superpowers:verification-before-completion`。
- 如果要并行执行，建议拆成两个 worker：
  - Worker A：Task 2 + Task 3
  - Worker B：Task 4
  - 主线程负责 Task 1、Task 5 和集成验证
