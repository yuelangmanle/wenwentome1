# WenwenToMe V2 Master Execution Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `文文tome` 从“原型 + 局部桥接”推进到“正式 Android App 可运行主链 + 云端构建可验证”的状态，覆盖书架、发现、我的、阅读、详情、下载缓存、浏览器找书、优化阅读、换源与云端构建。

**Architecture:** 继续采用“双轨并行”策略。底层阅读、书源、TXT/EPUB、缓存、换源继续复用 `Legado` 成熟能力；上层交互与视觉逐步切换到 `文文tome` 新 UI。先做主工程接线与入口接管，再逐步把书架、发现、阅读、浏览器找书替换为新壳，最后收口云端验证与发版链路。

**Tech Stack:** Kotlin, Android Views, Jetpack Compose, Room, DataStore, Coroutines, JUnit, GitHub Actions

---

## 当前进度（2026-03-26）

- 已推进：主工程模块接线、三栏主入口桥接、`我的` 页 AI / 同步入口、发现页双模式框架、浏览器找书设置持久化、浏览器找书真实 WebView 路由。
- 本轮新增：浏览器找书 `WebView -> 优化阅读` MVP，包含悬浮“优化阅读”入口、自动/手动正文提取、最小浏览器阅读桥接页、对应纯单测。
- 本轮继续推进：新阅读页顶部在线书直达“下载缓存”快捷动作；书籍详情页补上“在线书刷新当前来源 / 本地书按书名作者跨已启用书源精确补全简介与封面”的桥接逻辑。
- 本轮继续推进：浏览器优化阅读页已经可以把正文保存成一条真实的 `Book + Chapter + 正文缓存` 记录，并支持“加入书架”或“直接用统一阅读器打开”；浏览器捕获书在新阅读页中默认关闭换源与下载入口，避免出现假能力。
- 仍待完成：浏览器优化阅读与正式新阅读器的深度融合、统一下载缓存链路、阅读页详情/换源菜单收口、共享元素转场、GitHub Actions 最终云端编译验证。

---

### Task 1: 主工程接线与模块入场

**Files:**
- Modify: `settings.gradle`
- Modify: `app/build.gradle`
- Verify: `build.gradle`

- [ ] 将 `core:model`、`core:database`、`data:localbooks`、`data:apihub`、`bridge:source`、`feature:library`、`feature:discover`、`feature:reader`、`feature:apihub`、`feature:settings`、`sync:github` 纳入主工程。
- [ ] 补齐 `app` 对需要落地主链的模块依赖，避免新 UI 只存在于仓库而不在正式工程图谱里。
- [ ] 静态检查 Gradle 结构，确认命名、路径与插件体系一致。

### Task 2: 正式 App 一级信息架构接管

**Files:**
- Modify: `app/src/main/java/io/legado/app/ui/main/MainActivity.kt`
- Modify: `app/src/main/res/menu/menu_bottom_navigation.xml`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverScreen.kt`

- [ ] 将底部导航收口为 `书架 / 发现 / 我的` 三栏。
- [ ] 去掉独立 `导入` 底栏入口，改为书架页右上角三点菜单承接。
- [ ] 保证 `我的` 页保留 `AI / 同步` 入口，但不再和书架、发现混排。

### Task 3: 书架页主链落地

**Files:**
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ContinueReadingCard.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt`

- [ ] 首页只保留一个继续阅读区。
- [ ] 书封改为更大的双列主书架布局。
- [ ] 右上角菜单收纳 `导入书籍 / 下载缓存管理 / 批量管理 / 排序筛选`。
- [ ] 导入书籍的两个入口定义为 `扫描本地书籍 / 按路径手动导入`。

### Task 4: 发现页双模式落地

**Files:**
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverScreen.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverUiState.kt`

- [ ] 顶部双入口切换 `书源搜索 / 浏览器找书`。
- [ ] `书源搜索` 模式下，右上角三点菜单承接 `书源管理 / 导入书源 / 启用禁用 / 删除`。
- [ ] `浏览器找书` 模式下，右上角三点菜单承接 `搜索引擎管理 / 自定义搜索引擎 / 浏览器模式 / 优化阅读设置`。

### Task 5: 书源搜索详情、统一阅读菜单与换源

**Files:**
- Modify: `app/src/main/java/io/legado/app/ui/wenwen/WenwenBookDetailActivity.kt`
- Modify: `app/src/main/java/io/legado/app/ui/wenwen/WenwenReaderActivity.kt`
- Modify: `app/src/main/java/io/legado/app/ui/book/info/BookInfoActivity.kt`

- [ ] 书源搜索结果先进入详情页，再进入阅读。
- [ ] 所有来源的书在阅读页右上角统一提供 `书籍详情 / 下载缓存 / 缓存管理 / 换源`。
- [ ] 在线书保持章节级换源能力。
- [ ] 详情页支持尽量补全简介、作者、封面、来源信息。

### Task 6: 沉浸式阅读页与排版系统收口

**Files:**
- Modify: `app/src/main/java/io/legado/app/ui/wenwen/WenwenReaderActivity.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderSettingsSheet.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderTocSheet.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderBookmarkDrawer.kt`

- [ ] 阅读页保持整块铺平，不再出现卡片套卡片的阅读面。
- [ ] 顶部状态区沉浸式但不压系统状态栏。
- [ ] 底部细信息条显示 `手机电量 / 全书百分比 / 章节百分比`。
- [ ] 排版系统补齐 `无极字号 / 自动适配 / 字距 / 行距 / 段距 / 页边距 / 背景调色盘 / 字体导入`。
- [ ] 目录与书签采用抽屉式交互。

### Task 7: 统一下载缓存链路

**Files:**
- Modify: `app/src/main/java/io/legado/app/ui/wenwen/WenwenReaderActivity.kt`
- Modify: `app/src/main/java/io/legado/app/ui/book/cache/CacheActivity.kt`

- [ ] 本地书、书源书、浏览器优化阅读书的下载入口归并到统一缓存管理语义。
- [ ] 支持 `下载后 50 章 / 下载后 100 章 / 下载后全部 / 下载整本书`。
- [ ] 下载管理页保留封面与来源信息。

### Task 8: 浏览器找书 MVP

**Files:**
- Create/Modify: `feature/discover` 相关浏览器模式文件
- Modify: `site/wenwentome-v2-prototype.html`

- [ ] 内置默认搜索引擎为必应。
- [ ] 支持快速切换 `必应 / 百度 / 搜狗 / 搜狐 / 神马 / 谷歌 / 自定义`。
- [ ] 搭建浏览器模式设置骨架与网页识别入口。
- [ ] 加入优化阅读悬浮操作入口与自动/手动两种策略设置。

### Task 9: 转场动画与视觉收口

**Files:**
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderDetailShared.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt`

- [ ] 建立书架封面进入阅读页的共享元素骨架。
- [ ] 为“打开书籍 / 关闭书籍”准备从哪里来回哪里去的过渡钩子。
- [ ] 保持浏览器原型视觉与正式 App 视觉方向一致。

### Task 10: 云端构建与验证

**Files:**
- Modify: `.github/workflows/android-ci.yml`
- Modify: `.github/workflows/android-release.yml`
- Modify: `README.md`

- [ ] 保证 GitHub Actions 可以在无本地 Java 的前提下执行 Android 云端构建。
- [ ] `android-ci` 负责测试与 `debug APK`。
- [ ] `android-release` 负责签名正式包。
- [ ] 整理云端触发说明与当前已知限制。

### Task 11: 验证与收尾

**Files:**
- Verify: `app/src/main/java/io/legado/app/ui/wenwen/*`
- Verify: `feature/library/*`
- Verify: `feature/discover/*`
- Verify: `feature/reader/*`

- [ ] 在本机缺少 Java 的情况下，至少完成静态自检与测试文件补齐。
- [ ] 将 GitHub Actions 作为最终编译与集成验证来源。
- [ ] 输出“已完成 / 未完成 / 云端待验证”的清单，避免假完成。
