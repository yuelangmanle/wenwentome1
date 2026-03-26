# WenwenToMe V2 UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将已确认的 V2 原型落地到真实 Android App，完成三栏信息架构、沉浸式阅读页、目录/书签抽屉、统一缓存入口和可扩展的阅读排版系统。

**Architecture:** 沿用当前 `feature/library`、`feature/discover`、`feature/reader` 的 Compose 模块拆分，不推翻现有模块边界。阅读体验的核心扩展集中在 `core/model` 与 `core/database` 的阅读偏好模型、`feature/reader` 的 UI 与状态管理，以及 `feature/library` / `feature/discover` 的入口重构与共享转场协调。

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX DataStore, Room, Coroutines, Compose UI Test, JUnit

---

### Task 1: 扩展阅读偏好模型与持久化结构

**Files:**
- Modify: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReaderPresentationPrefs.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/datastore/ReaderPreferencesStore.kt`
- Modify: `core/database/src/test/kotlin/com/wenwentome/reader/core/database/datastore/ReaderPreferencesStoreTest.kt`

- [ ] **Step 1: 先写偏好扩展的 failing test**

```kotlin
@Test
fun savePresentationPrefs_persistsContinuousTypographyControls() = runTest {
    val prefs = ReaderPresentationPrefs(
        fontSizeSp = 19.5f,
        lineHeightMultiplier = 1.72f,
        letterSpacingEm = 0.04f,
        paragraphSpacingEm = 0.55f,
        sidePaddingDp = 22,
        autoFitFontSize = true,
        fontFamilyKey = "custom-serif",
        backgroundPaletteKey = "paper-green",
    )
    store.savePresentationPrefs(prefs)
    assertEquals(prefs, store.presentationPrefs.first())
}
```

- [ ] **Step 2: 运行测试确认当前会失败**

Run: `./gradlew :core:database:testDebugUnitTest --tests "*ReaderPreferencesStoreTest*"`
Expected: FAIL，原因是 `ReaderPresentationPrefs` 与 DataStore 尚未支持新增字段与浮点字号。

- [ ] **Step 3: 最小实现模型与存储字段**

```kotlin
data class ReaderPresentationPrefs(
    val theme: ReaderTheme = ReaderTheme.PAPER,
    val fontSizeSp: Float = 18f,
    val autoFitFontSize: Boolean = true,
    val lineHeightMultiplier: Float = 1.55f,
    val letterSpacingEm: Float = 0f,
    val paragraphSpacingEm: Float = 0.35f,
    val sidePaddingDp: Int = 18,
    val brightnessPercent: Int = 0,
    val fontFamilyKey: String = "system-serif",
    val backgroundPaletteKey: String = "paper",
)
```

- [ ] **Step 4: 重新运行测试确认通过**

Run: `./gradlew :core:database:testDebugUnitTest --tests "*ReaderPreferencesStoreTest*"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReaderPresentationPrefs.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/datastore/ReaderPreferencesStore.kt \
  core/database/src/test/kotlin/com/wenwentome/reader/core/database/datastore/ReaderPreferencesStoreTest.kt
git commit -m "feat: extend reader presentation preferences"
```

### Task 2: 重构书架与发现页入口归位

**Files:**
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ContinueReadingCard.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookActionsMenu.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverScreen.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverUiState.kt`
- Test: `feature/library/src/testDebug/java/com/wenwentome/reader/feature/library/LibraryScreenTest.kt`
- Test: `feature/discover/src/testDebug/java/com/wenwentome/reader/feature/discover/DiscoverScreenTest.kt`

- [ ] **Step 1: 先写书架/发现页入口的 failing UI tests**

```kotlin
composeTestRule.onNodeWithText("下载缓存管理").assertDoesNotExist()
composeTestRule.onNodeWithTag("library-overflow-menu").performClick()
composeTestRule.onNodeWithText("下载缓存管理").assertExists()

composeTestRule.onNodeWithText("书源搜索").assertExists()
composeTestRule.onNodeWithText("浏览器找书").assertExists()
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :feature:library:testDebugUnitTest :feature:discover:testDebugUnitTest`
Expected: FAIL，当前页面结构未完全符合新原型入口边界。

- [ ] **Step 3: 最小实现书架/发现页结构**

重点实现：

```text
书架：首页只保留一个继续阅读区 + 双列大书封 + 右上角菜单承接导入/下载缓存管理
发现：顶部双入口，书源搜索与浏览器找书切换
我的：不显示书架/发现式的三点菜单
```

- [ ] **Step 4: 再跑 UI tests**

Run: `./gradlew :feature:library:testDebugUnitTest :feature:discover:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ContinueReadingCard.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookActionsMenu.kt \
  feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverScreen.kt \
  feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverUiState.kt \
  feature/library/src/testDebug/java/com/wenwentome/reader/feature/library/LibraryScreenTest.kt \
  feature/discover/src/testDebug/java/com/wenwentome/reader/feature/discover/DiscoverScreenTest.kt
git commit -m "feat: align library and discover entry architecture"
```

### Task 3: 落地沉浸式阅读页骨架与底部信息条

**Files:**
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderUiState.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModel.kt`
- Test: `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt`
- Test: `feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModelTest.kt`

- [ ] **Step 1: 写阅读页沉浸式骨架的 failing tests**

```kotlin
composeTestRule.onNodeWithTag("reader-screen").assertExists()
composeTestRule.onNodeWithTag("reader-system-bar").assertExists()
composeTestRule.onNodeWithTag("reader-bottom-meta").assertExists()
composeTestRule.onNodeWithText("全书").assertExists()
composeTestRule.onNodeWithText("章节").assertExists()
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :feature:reader:testDebugUnitTest :feature:reader:testDebugAndroidTest`
Expected: FAIL，当前阅读页仍是旧骨架或缺少新信息层。

- [ ] **Step 3: 最小实现沉浸式阅读页**

实现要求：

```text
正文铺满整个阅读区域
顶部系统信息、标题与操作按钮位于安全区
底部细信息条显示电量、全书百分比、章节百分比
阅读工具默认为弱化态，只在唤起时增强显示
```

- [ ] **Step 4: 重新运行测试**

Run: `./gradlew :feature:reader:testDebugUnitTest :feature:reader:testDebugAndroidTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderUiState.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModel.kt \
  feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt \
  feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModelTest.kt
git commit -m "feat: add immersive reader shell"
```

### Task 4: 落地目录/书签抽屉与阅读设置高级项

**Files:**
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderTocSheet.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderBookmarkDrawer.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderSettingsSheet.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderAssistantUiState.kt`
- Test: `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt`

- [ ] **Step 1: 先写抽屉与高级设置的 failing UI tests**

```kotlin
composeTestRule.onNodeWithTag("reader-toc-drawer").assertExists()
composeTestRule.onNodeWithTag("reader-bookmark-drawer").assertExists()
composeTestRule.onNodeWithText("导入字体").assertExists()
composeTestRule.onNodeWithText("阅读背景调色盘").assertExists()
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :feature:reader:testDebugAndroidTest`
Expected: FAIL，当前目录/书签不是抽屉形态，设置项也不完整。

- [ ] **Step 3: 最小实现抽屉与排版系统**

实现要求：

```text
目录左抽屉、书签右抽屉
目录项可展开查看小节
书签项可展开查看摘录与批注
字号改为连续滑杆，并支持自动适配开关
新增字距、行距、段落间距、页边距调节
新增背景调色盘
新增导入字体入口（MVP 可先只做入口与本地字体注册）
```

- [ ] **Step 4: 重新运行 UI tests**

Run: `./gradlew :feature:reader:testDebugAndroidTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderTocSheet.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderBookmarkDrawer.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderSettingsSheet.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderAssistantUiState.kt \
  feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt
git commit -m "feat: add reader drawers and advanced typography controls"
```

### Task 5: 落地书架到阅读页的共享元素转场

**Files:**
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderDetailShared.kt`
- Test: `feature/library/src/testDebug/java/com/wenwentome/reader/feature/library/LibraryScreenTest.kt`
- Test: `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt`

- [ ] **Step 1: 先写转场标识的 failing tests**

```kotlin
composeTestRule.onNodeWithTag("book-cover-shared-book-1").assertExists()
composeTestRule.onNodeWithTag("reader-shared-container-book-1").assertExists()
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :feature:library:testDebugAndroidTest :feature:reader:testDebugAndroidTest`
Expected: FAIL，当前没有统一的共享元素 tag / 容器。

- [ ] **Step 3: 最小实现共享元素骨架**

实现要求：

```text
书架封面进入阅读时从原封面位置展开
关闭阅读时回收至同一封面位置
详情页进入阅读时使用详情封面或信息卡作为起点
禁止出现来源无关的乱飘转场
```

- [ ] **Step 4: 再跑测试**

Run: `./gradlew :feature:library:testDebugAndroidTest :feature:reader:testDebugAndroidTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderDetailShared.kt \
  feature/library/src/testDebug/java/com/wenwentome/reader/feature/library/LibraryScreenTest.kt \
  feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt
git commit -m "feat: add shared transitions for opening and closing books"
```

### Task 6: 验证整条阅读主链

**Files:**
- Verify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt`
- Verify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverScreen.kt`
- Verify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt`

- [ ] **Step 1: 跑模块测试**

Run: `./gradlew :core:model:testDebugUnitTest :core:database:testDebugUnitTest :data:localbooks:testDebugUnitTest :feature:library:testDebugUnitTest :feature:discover:testDebugUnitTest :feature:reader:testDebugUnitTest`
Expected: PASS

- [ ] **Step 2: 跑关键 Compose UI tests**

Run: `./gradlew :feature:library:connectedDebugAndroidTest :feature:discover:connectedDebugAndroidTest :feature:reader:connectedDebugAndroidTest`
Expected: PASS

- [ ] **Step 3: 跑一次 debug 包确认可编译**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 手工验收清单**

```text
书架：首页不再有重复继续阅读
发现：双入口与右上角联动菜单正常
阅读：沉浸式顶区 + 底部信息条正常
目录/书签：侧边抽屉可展开
排版：字号无级调节、自动适配、字距/行距/背景/字体入口存在
转场：打开/关闭阅读不乱飘
```

- [ ] **Step 5: 最终提交**

```bash
git add feature/library feature/discover feature/reader core/model core/database data/localbooks
git commit -m "feat: implement wenwentome v2 reader architecture"
```
