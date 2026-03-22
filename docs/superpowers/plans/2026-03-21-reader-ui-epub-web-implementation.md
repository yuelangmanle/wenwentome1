# Reader UI EPUB Web Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把阅读器升级成正式可用的三模式阅读器，同时修复 EPUB 正文入口问题，并补齐网文阅读闭环与书架进度联动。

**Architecture:** 保持现有 `BookRecord + ReadingState + ReaderChapter` 统一模型，不再拆第二套阅读链路。优先增强 `LocalBookContentRepository` 与 `ReaderScreen` 的表现层，再把网文目录/最新章/继续阅读状态接回书架与详情页。所有行为改动先用单测或 Robolectric UI 测试锁住，再做最小实现。

**Tech Stack:** Kotlin、Jetpack Compose、Room、DataStore、Robolectric、GitHub Actions

---

### Task 1: EPUB 正文入口与章节定位修复

**Files:**
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepository.kt`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/EpubCatalogParser.kt`
- Test: `data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepositoryTest.kt`

- [ ] **Step 1: 写失败测试，覆盖 EPUB 不应落到 Cover/nav/toc，且正文段落抽取更稳定**

```kotlin
@Test
fun load_epubStartsFromFirstReadableChapter_notCoverOrNav() = runTest {
    val repository = createRepository(epubFixture = "sample-cover-first.epub")

    val content = repository.load("book-1", locator = null)

    assertEquals("第二章", content.chapterTitle)
    assertTrue(content.paragraphs.first().contains("第二章正文"))
}
```

- [ ] **Step 2: 运行失败测试，确认当前实现仍有 EPUB 正文入口缺陷**

Run: `./gradlew :data:localbooks:testDebugUnitTest --tests "*LocalBookContentRepositoryTest"`
Expected: 目标测试 FAIL，失败原因与正文章节/段落定位相关

- [ ] **Step 3: 最小实现 EPUB 正文修复**

```kotlin
private fun extractParagraphsFromHtml(html: String): List<String> {
    val body = stripHeadAndNoise(html)
    val blocks = splitReadableBlocks(body)
    return blocks.ifEmpty { fallbackSentenceSplit(body) }
}
```

- [ ] **Step 4: 运行本模块测试，确认 EPUB 修复通过且无回归**

Run: `./gradlew :data:localbooks:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepository.kt \
  data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/EpubCatalogParser.kt \
  data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepositoryTest.kt
git commit -m "fix: stabilize epub readable chapter extraction"
```

### Task 2: 阅读器三模式与目录/进度体验升级

**Files:**
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderUiState.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModel.kt`
- Test: `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt`
- Test: `feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModelTest.kt`

- [ ] **Step 1: 写失败测试，锁定三模式呈现差异与目录跳转/进度保存**

```kotlin
@Test
fun readerScreen_horizontalPagingRendersPagerContainer() {
    composeTestRule.setContent { ReaderScreen(state = sampleState(readerMode = ReaderMode.HORIZONTAL_PAGING), ...) }
    composeTestRule.onNodeWithTag("reader-horizontal-pager").assertExistsCompat()
}
```

```kotlin
@Test
fun jumpToChapter_usesLocatorHintAndUpdatesReadingState() = runTest {
    val viewModel = createViewModel(chapters = listOf(sampleChapter(locatorHint = "chapter:ch2#paragraph:0")))
    viewModel.jumpToChapter("ch2")
    assertEquals("chapter:ch2#paragraph:0", persistedState?.locator)
}
```

- [ ] **Step 2: 运行失败测试，确认当前阅读器还只是同一个 `LazyColumn`**

Run: `./gradlew :feature:reader:testDebugUnitTest --tests "*ReaderViewModelTest" --tests "*ReaderScreenTest"`
Expected: FAIL，缺少新的容器 tag 或行为断言

- [ ] **Step 3: 最小实现阅读器模式分化**

```kotlin
when (state.readerMode) {
    ReaderMode.SIMULATED_PAGE_TURN -> SimulatedPageReader(...)
    ReaderMode.HORIZONTAL_PAGING -> HorizontalPagerReader(...)
    ReaderMode.VERTICAL_SCROLL -> VerticalScrollReader(...)
}
```

- [ ] **Step 4: 补目录高亮、章节信息条、页内点击翻页与更细的进度保存**

```kotlin
onPageChanged = { page ->
    onLocatorChanged(page.locator, page.progressPercent)
}
```

- [ ] **Step 5: 运行 reader 模块测试**

Run: `./gradlew :feature:reader:testDebugUnitTest`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderUiState.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModel.kt \
  feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt \
  feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModelTest.kt
git commit -m "feat: add differentiated reader presentation modes"
```

### Task 3: 网文阅读闭环与书架进度联动

**Files:**
- Modify: `app/src/main/java/com/wenwentome/reader/navigation/ReaderContentFlows.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ObserveBookshelfUseCase.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryUiState.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryViewModel.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt`
- Test: `app/src/testDebug/java/com/wenwentome/reader/AppReaderFlowTest.kt`
- Test: `feature/library/src/test/kotlin/com/wenwentome/reader/feature/library/LibraryViewModelTest.kt`

- [ ] **Step 1: 写失败测试，锁定网文“继续阅读/最新章/书架更新”闭环**

```kotlin
@Test
fun appReaderFlow_remoteBookProgressFlowsBackToBookshelf() {
    // 进入阅读器 -> 跳章节/保存进度 -> 回书架 -> 断言 continue reading 与更新状态
}
```

```kotlin
@Test
fun observeBookshelf_marksRemoteBookAsUpdatedWhenLatestChapterAdvances() = runTest {
    val items = useCase().first()
    assertTrue(items.single().hasUpdates)
}
```

- [ ] **Step 2: 运行失败测试，确认书架还没有完整消费 readingState + remoteBinding**

Run: `./gradlew :feature:library:testDebugUnitTest --tests "*LibraryViewModelTest" :app:testDebugUnitTest --tests "*AppReaderFlowTest"`
Expected: FAIL

- [ ] **Step 3: 最小实现统一书架项模型**

```kotlin
data class LibraryBookItem(
    val book: BookRecord,
    val effectiveCover: String?,
    val progressPercent: Float,
    val progressLabel: String,
    val hasUpdates: Boolean,
)
```

- [ ] **Step 4: 接入 readingState / remoteBinding / cover 数据，补 continue reading 和更新徽标**

```kotlin
combine(observeBooks, observeReadingStates, observeBindings) { books, states, bindings ->
    buildLibraryItems(books, states, bindings)
}
```

- [ ] **Step 5: 运行 library + app 测试**

Run: `./gradlew :feature:library:testDebugUnitTest :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/wenwentome/reader/navigation/ReaderContentFlows.kt \
  app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ObserveBookshelfUseCase.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryUiState.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryViewModel.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt \
  app/src/testDebug/java/com/wenwentome/reader/AppReaderFlowTest.kt \
  feature/library/src/test/kotlin/com/wenwentome/reader/feature/library/LibraryViewModelTest.kt
git commit -m "feat: complete web reading loop and bookshelf progress states"
```

### Task 4: 云端验证与收尾

**Files:**
- Verify: `.github/workflows/android-ci.yml`
- Verify: `README.md`

- [ ] **Step 1: 运行 git 静态检查**

Run: `git diff --check`
Expected: 无输出

- [ ] **Step 2: 推送分支并触发云端构建**

Run: `git push origin release/android-reader-v1`
Expected: push 成功

- [ ] **Step 3: 观察 GitHub Actions**

Run: `gh run list --repo yuelangmanle/wenwentome1 --branch release/android-reader-v1 --limit 3`
Expected: 最新 `android-ci` run 进入 `in_progress`

- [ ] **Step 4: 下载 debug APK artifact**

Run: `gh run download <run-id> --repo yuelangmanle/wenwentome1 --name debug-apk --dir .artifacts/run-<run-id>`
Expected: 获得 `debug-apk` 产物目录

- [ ] **Step 5: 提交最终收尾**

```bash
git add README.md .github/workflows/android-ci.yml
git commit -m "chore: finalize reader 1.1 delivery notes"
```
