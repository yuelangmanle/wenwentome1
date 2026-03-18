# Android Reader MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Phase 1 Android MVP that delivers a unified bookshelf for local TXT/EPUB books and web novels, with a single reader experience and GitHub private-repo sync for files and metadata.

**Architecture:** Follow Android's recommended layered app architecture with a Compose UI layer and isolated data modules, while keeping the web-novel compatibility logic inside a dedicated `Source Bridge` so legacy rule complexity does not leak into the app shell or bookshelf core. The product is intentionally local-first: the bookshelf model, reader state, and sync manifest revolve around `BookRecord`, while web sources are attached through `RemoteBinding` and `SourceDefinition` instead of becoming top-level product objects.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose, Room, DataStore, Coroutines/Flow, OkHttp, jsoup, JsonPath, JsoupXpath, Rhino, GitHub Actions, GitHub Codespaces

---

## Scope Note

This plan intentionally covers only **Phase 1 MVP** from the approved spec at [`docs/superpowers/specs/2026-03-18-android-reader-design.md`](../specs/2026-03-18-android-reader-design.md).

Out of scope for this plan:

- PDF reader support
- Deep login-dependent source flows
- Advanced discovery pages and complex aggregation sources
- Public distribution hardening

Implementation guidance:

- Use `@superpowers:subagent-driven-development` to execute task-by-task
- Use `@superpowers:verification-before-completion` before claiming any task is done

## File Structure

Lock these module boundaries before writing code.

- Create: `.devcontainer/devcontainer.json`
- Create: `.devcontainer/Dockerfile`
  Purpose: Define a repeatable Codespaces environment with JDK, Android SDK command-line tools, and Gradle cache mounts.
- Create: `.github/workflows/android-ci.yml`
  Purpose: Run unit tests, assemble debug APK, and upload build artifacts on pushes and pull requests.
- Create: `settings.gradle.kts`
  Purpose: Register the root project and all planned Gradle modules.
- Create: `build.gradle.kts`
  Purpose: Root plugin configuration and shared Gradle conventions.
- Create: `gradle/libs.versions.toml`
  Purpose: Centralize dependency coordinates and plugin aliases.
- Create: `gradle.properties`
  Purpose: Enable AndroidX, non-transitive R classes, Kotlin incremental settings, and JVM memory tuning.

- Create: `app/build.gradle.kts`
  Purpose: Android application module with Compose, manifest, top-level navigation, and module wiring.
- Create: `app/src/main/AndroidManifest.xml`
  Purpose: Declare the launcher activity, storage pickers, and app-level configuration.
- Create: `app/src/main/java/com/wenwentome/reader/MainActivity.kt`
  Purpose: Host `ReaderApp()` and the app lifecycle entry point.
- Create: `app/src/main/java/com/wenwentome/reader/ReaderApp.kt`
  Purpose: Build the `Scaffold`, top-level destinations, and root state holders.
- Create: `app/src/main/java/com/wenwentome/reader/di/AppContainer.kt`
  Purpose: Centralize application-scope construction for `ReaderDatabase`, DAOs, repositories, file storage, and sync services.
- Create: `app/src/main/java/com/wenwentome/reader/navigation/TopLevelDestination.kt`
  Purpose: Define `Bookshelf`, `Discover`, and `Settings` navigation contracts.
- Create: `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`
  Purpose: Wire top-level Compose destinations and deep-link-ready routes.
- Test: `app/src/test/java/com/wenwentome/reader/AppNavigationSmokeTest.kt`

- Create: `core/model/build.gradle.kts`
  Purpose: Shared product model module without Android UI dependencies.
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/BookRecord.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/BookAsset.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReadingState.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/SourceDefinition.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/RemoteBinding.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/SyncSnapshot.kt`
- Test: `core/model/src/test/kotlin/com/wenwentome/reader/core/model/BookRecordTest.kt`

- Create: `core/database/build.gradle.kts`
  Purpose: Room entities, DAOs, and DataStore-backed preferences.
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/ReaderDatabase.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/DatabaseConverters.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/DatabaseMappers.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/BookRecordEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/BookAssetEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ReadingStateEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/SourceDefinitionEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/RemoteBindingEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/BookRecordDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/BookAssetDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ReadingStateDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/RemoteBindingDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/SourceDefinitionDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/datastore/ReaderPreferencesStore.kt`
- Test: `core/database/src/test/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseTest.kt`

- Create: `data/localbooks/build.gradle.kts`
  Purpose: TXT/EPUB parsing, import pipeline, and local repository logic.
- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/TxtBookParser.kt`
- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/EpubBookParser.kt`
- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookModels.kt`
- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookFileStore.kt`
- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepository.kt`
- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepository.kt`
- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/ImportLocalBookUseCase.kt`
- Create: `data/localbooks/src/test/resources/fixtures/sample.txt`
- Create: `data/localbooks/src/test/resources/fixtures/sample.epub`
- Test: `data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepositoryTest.kt`

- Create: `feature/library/build.gradle.kts`
  Purpose: Unified bookshelf UI and bookshelf-facing state holders.
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt`
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryViewModel.kt`
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryFilter.kt`
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ObserveBookshelfUseCase.kt`
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryUiState.kt`
- Test: `feature/library/src/test/kotlin/com/wenwentome/reader/feature/library/LibraryViewModelTest.kt`

- Create: `feature/reader/build.gradle.kts`
  Purpose: Book detail + reader UI, locator persistence, bookmarks, and reading settings.
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailScreen.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModel.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderUiState.kt`
- Test: `feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModelTest.kt`

- Create: `sync/github/build.gradle.kts`
  Purpose: GitHub private-repo sync client, manifest serializer, and file transport.
- Create: `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/GitHubAuthConfig.kt`
- Create: `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/GitHubContentApi.kt`
- Create: `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/GitHubSyncRepository.kt`
- Create: `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/SyncManifest.kt`
- Create: `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/SyncManifestSerializer.kt`
- Create: `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/RestoredSnapshot.kt`
- Test: `sync/github/src/test/kotlin/com/wenwentome/reader/sync/github/GitHubSyncRepositoryTest.kt`

- Create: `bridge/source/build.gradle.kts`
  Purpose: Legacy/new rule parsing, rule execution, and remote source bridging.
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/SourceRuleParser.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/LegacySourceMapper.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/model/LegacySourceDefinition.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/model/NormalizedSourceDefinition.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/model/RemoteBookModels.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/JsoupRuleExecutor.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/XPathRuleExecutor.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/JsonPathRuleExecutor.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/RhinoScriptEvaluator.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/SourceBridgeRepository.kt`
- Create: `bridge/source/src/test/resources/fixtures/legacy-source.json`
- Create: `bridge/source/src/test/resources/fixtures/search-result.html`
- Create: `bridge/source/src/test/resources/fixtures/book-detail.html`
- Create: `bridge/source/src/test/resources/fixtures/chapter-list.html`
- Create: `bridge/source/src/test/resources/fixtures/chapter-content.html`
- Test: `bridge/source/src/test/kotlin/com/wenwentome/reader/bridge/source/SourceRuleParserTest.kt`

- Create: `feature/discover/build.gradle.kts`
  Purpose: Discover/search/source-management UI that adds remote books into the unified bookshelf.
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverScreen.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModel.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverUiState.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/AddRemoteBookToShelfUseCase.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/ImportSourcesUseCase.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/SourceManagementScreen.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/SourceManagementViewModel.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/SourceManagementUiState.kt`
- Test: `feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModelTest.kt`

- Create: `feature/settings/build.gradle.kts`
  Purpose: Sync settings, token configuration, and app settings screens.
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SettingsScreen.kt`
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsScreen.kt`
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsUiState.kt`
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsViewModel.kt`
- Test: `feature/settings/src/test/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsViewModelTest.kt`

- Create: `app/src/test/java/com/wenwentome/reader/AppSmokeFlowTest.kt`
  Purpose: End-to-end Robolectric/Compose smoke coverage for the app shell and top-level route stability.

## Task 1: Scaffold The Multi-Module Project

**Files:**
- Create: `.devcontainer/devcontainer.json`
- Create: `.devcontainer/Dockerfile`
- Create: `.github/workflows/android-ci.yml`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/wenwentome/reader/MainActivity.kt`
- Create: `app/src/main/java/com/wenwentome/reader/ReaderApp.kt`
- Create: `app/src/main/java/com/wenwentome/reader/di/AppContainer.kt`
- Create: `app/src/main/java/com/wenwentome/reader/navigation/TopLevelDestination.kt`
- Create: `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`
- Test: `app/src/test/java/com/wenwentome/reader/AppNavigationSmokeTest.kt`

- [ ] **Step 1: Create the root Gradle settings and module registration**

```kotlin
rootProject.name = "wenwentome-reader"
include(
    ":app",
    ":core:model",
    ":core:database",
    ":data:localbooks",
    ":feature:library",
    ":feature:reader",
    ":feature:discover",
    ":feature:settings",
    ":sync:github",
    ":bridge:source",
)
```

- [ ] **Step 2: Add Codespaces, pinned dependency versions, and CI bootstrap files**

```dockerfile
FROM mcr.microsoft.com/devcontainers/base:ubuntu

RUN apt-get update \
    && apt-get install -y curl openjdk-21-jdk unzip \
    && rm -rf /var/lib/apt/lists/*

ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools

RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools /tmp/android-sdk \
    && curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/android-sdk/cmdline-tools.zip \
    && unzip -q /tmp/android-sdk/cmdline-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools \
    && mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
    && yes | sdkmanager --licenses \
    && sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

```json
{
  "name": "wenwentome-reader",
  "build": {
    "dockerfile": "Dockerfile"
  },
  "remoteEnv": {
    "ANDROID_SDK_ROOT": "/opt/android-sdk"
  },
  "mounts": [
    "source=gradle-cache,target=/home/vscode/.gradle,type=volume"
  ],
  "customizations": {
    "codespaces": {
      "openFiles": [
        "docs/superpowers/specs/2026-03-18-android-reader-design.md",
        "docs/superpowers/plans/2026-03-18-android-reader-mvp.md"
      ]
    }
  }
}
```

```yaml
name: android-ci
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - uses: android-actions/setup-android@v3
      - run: ./gradlew test testDebugUnitTest assembleDebug
```

```toml
[versions]
agp = "8.9.0"
kotlin = "2.1.10"
composeBom = "2026.03.00"
robolectric = "4.14.1"

[libraries]
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

```kotlin
// Apply the same testOptions/testImplementation block in `app` and every Compose-based feature module.
android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test.junit4)
}
```

- [ ] **Step 3: Write the failing shell smoke test**

```kotlin
@Test
fun bottomBar_showsThreeTopLevelDestinations() {
    val appContainer = AppContainer(ApplicationProvider.getApplicationContext())
    composeTestRule.setContent { ReaderApp(appContainer = appContainer) }
    composeTestRule.onNodeWithText("书库").assertExists()
    composeTestRule.onNodeWithText("发现").assertExists()
    composeTestRule.onNodeWithText("我的").assertExists()
}
```

- [ ] **Step 4: Run the test to verify the shell is missing**

Run: `./gradlew :app:testDebugUnitTest --tests "com.wenwentome.reader.AppNavigationSmokeTest.bottomBar_showsThreeTopLevelDestinations"`

Expected: FAIL with unresolved references or missing `ReaderApp` / navigation classes.

- [ ] **Step 5: Implement the minimal Compose shell**

```kotlin
enum class TopLevelDestination(val label: String) {
    BOOKSHELF("书库"),
    DISCOVER("发现"),
    SETTINGS("我的"),
}

@Composable
fun ReaderApp(appContainer: AppContainer) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate(destination.name) },
                        icon = {},
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { paddingValues ->
        AppNavHost(
            navController = navController,
            paddingValues = paddingValues,
            appContainer = appContainer,
        )
    }
}

class AppContainer(private val application: Application) {
    val appContext: Context = application
}

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReaderApp(appContainer = appContainer) }
    }
}
```

- [ ] **Step 6: Re-run the shell test**

Run: `./gradlew :app:testDebugUnitTest --tests "com.wenwentome.reader.AppNavigationSmokeTest.bottomBar_showsThreeTopLevelDestinations"`

Expected: PASS

- [ ] **Step 7: Commit the scaffold**

```bash
git add .devcontainer .github settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml app
git commit -m "chore: scaffold android reader workspace"
```

### Task 2: Define The Shared Product Model

**Files:**
- Create: `core/model/build.gradle.kts`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/BookRecord.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/BookAsset.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReadingState.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/SourceDefinition.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/RemoteBinding.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/SyncSnapshot.kt`
- Test: `core/model/src/test/kotlin/com/wenwentome/reader/core/model/BookRecordTest.kt`

- [ ] **Step 1: Write the failing model normalization test**

```kotlin
@Test
fun newLocalBook_defaultsToInShelfAndLocalOrigin() {
    val book = BookRecord.newLocal(
        title = "三体",
        author = "刘慈欣",
        format = BookFormat.EPUB,
    )

    assertEquals(OriginType.LOCAL, book.originType)
    assertEquals(BookshelfState.IN_SHELF, book.bookshelfState)
    assertEquals(BookFormat.EPUB, book.primaryFormat)
}
```

- [ ] **Step 2: Run the model test**

Run: `./gradlew :core:model:test --tests "com.wenwentome.reader.core.model.BookRecordTest.newLocalBook_defaultsToInShelfAndLocalOrigin"`

Expected: FAIL with `BookRecord`, `OriginType`, or `BookFormat` not found.

- [ ] **Step 3: Implement the core model types**

```kotlin
enum class OriginType { LOCAL, WEB, MIXED }
enum class BookFormat { TXT, EPUB, WEB }
enum class BookshelfState { IN_SHELF, ARCHIVED, FAVORITE }

data class BookRecord(
    val id: String,
    val title: String,
    val author: String? = null,
    val originType: OriginType,
    val primaryFormat: BookFormat,
    val cover: String? = null,
    val summary: String? = null,
    val bookshelfState: BookshelfState = BookshelfState.IN_SHELF,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
) {
    companion object {
        fun newLocal(title: String, author: String?, format: BookFormat) = BookRecord(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            author = author,
            originType = OriginType.LOCAL,
            primaryFormat = format,
        )
    }
}
```

- [ ] **Step 4: Add the remaining shared data classes**

```kotlin
enum class AssetRole { PRIMARY_TEXT, COVER, CACHE_PACKAGE }
enum class SourceType { BUILT_IN, IMPORTED, COMPAT }
enum class RuleFormat { LEGACY, LEGADO3, CUSTOM }
enum class RemoteSyncMode { LATEST_ONLY, PINNED_TOC }

data class BookAsset(
    val bookId: String,
    val assetRole: AssetRole,
    val storageUri: String,
    val mime: String,
    val size: Long,
    val hash: String,
    val syncPath: String,
)

data class ReadingBookmark(val chapterRef: String?, val locator: String, val label: String)

data class ReadingState(
    val bookId: String,
    val locator: String? = null,
    val chapterRef: String? = null,
    val progressPercent: Float = 0f,
    val bookmarks: List<ReadingBookmark> = emptyList(),
    val notes: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        fun initial(bookId: String) = ReadingState(bookId = bookId)
    }
}
data class SourceDefinition(
    val sourceId: String,
    val sourceName: String,
    val sourceType: SourceType,
    val ruleFormat: RuleFormat,
    val authState: String? = null,
    val enabled: Boolean = true,
    val group: String? = null,
)
data class RemoteBinding(
    val bookId: String,
    val sourceId: String,
    val remoteBookId: String,
    val remoteBookUrl: String,
    val tocRef: String? = null,
    val syncMode: RemoteSyncMode = RemoteSyncMode.LATEST_ONLY,
    val lastFetchedAt: Long? = null,
)
data class SyncSnapshot(
    val snapshotId: String,
    val revision: String,
    val deviceId: String,
    val mergedAt: Long,
    val manifestJson: String,
)
```

- [ ] **Step 5: Run the shared model tests**

Run: `./gradlew :core:model:test`

Expected: PASS

- [ ] **Step 6: Commit the model module**

```bash
git add core/model settings.gradle.kts
git commit -m "feat: add shared reader model"
```

### Task 3: Add Room Persistence And Preferences

**Files:**
- Create: `core/database/build.gradle.kts`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/ReaderDatabase.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/DatabaseConverters.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/DatabaseMappers.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/BookRecordEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/BookAssetEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ReadingStateEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/SourceDefinitionEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/RemoteBindingEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/BookRecordDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/BookAssetDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ReadingStateDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/RemoteBindingDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/SourceDefinitionDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/datastore/ReaderPreferencesStore.kt`
- Test: `core/database/src/test/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseTest.kt`

- [ ] **Step 1: Write the failing DAO test**

```kotlin
@Test
fun upsertBookAndReadingState_areReturnedTogether() = runTest {
    val database = testDatabase()
    database.bookRecordDao().upsert(BookRecordEntity(id = "book-1", title = "三体"))
    database.readingStateDao().upsert(ReadingStateEntity(bookId = "book-1", locator = "cfi(/6/2)", progressPercent = 0.42f))

    val book = database.bookRecordDao().observeById("book-1").first()
    val state = database.readingStateDao().observeByBookId("book-1").first()

    assertEquals("三体", book?.title)
    assertEquals(0.42f, state?.progressPercent)
}
```

- [ ] **Step 2: Run the DAO test**

Run: `./gradlew :core:database:testDebugUnitTest --tests "com.wenwentome.reader.core.database.ReaderDatabaseTest.upsertBookAndReadingState_areReturnedTogether"`

Expected: FAIL because the database, DAOs, or entities do not exist.

- [ ] **Step 3: Implement the entities and DAOs**

```kotlin
@Entity(tableName = "book_records")
data class BookRecordEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String? = null,
    val originType: String = "LOCAL",
    val primaryFormat: String = "TXT",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Dao
interface BookRecordDao {
    @Upsert suspend fun upsert(entity: BookRecordEntity)
    @Upsert suspend fun upsertAll(entities: List<BookRecordEntity>)
    @Query("SELECT * FROM book_records WHERE id = :id")
    fun observeById(id: String): Flow<BookRecordEntity?>
    @Query("SELECT * FROM book_records ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<BookRecordEntity>>
    @Query("SELECT * FROM book_records ORDER BY updatedAt DESC")
    suspend fun getAll(): List<BookRecordEntity>
    @Query("DELETE FROM book_records")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entities: List<BookRecordEntity>) {
        clearAll()
        upsertAll(entities)
    }
}

@Dao
interface BookAssetDao {
    @Upsert suspend fun upsertAll(entities: List<BookAssetEntity>)
    @Query("SELECT * FROM book_assets WHERE bookId = :bookId AND assetRole = 'PRIMARY_TEXT' LIMIT 1")
    suspend fun findPrimaryAsset(bookId: String): BookAssetEntity?
    @Query("SELECT * FROM book_assets")
    suspend fun getAll(): List<BookAssetEntity>
    @Query("DELETE FROM book_assets")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entities: List<BookAssetEntity>) {
        clearAll()
        upsertAll(entities)
    }
}

@Dao
interface ReadingStateDao {
    @Upsert suspend fun upsert(entity: ReadingStateEntity)
    @Upsert suspend fun upsertAll(entities: List<ReadingStateEntity>)
    @Query("SELECT * FROM reading_states WHERE bookId = :bookId")
    fun observeByBookId(bookId: String): Flow<ReadingStateEntity?>
    @Query("SELECT * FROM reading_states")
    suspend fun getAll(): List<ReadingStateEntity>
    @Query("DELETE FROM reading_states")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entities: List<ReadingStateEntity>) {
        clearAll()
        upsertAll(entities)
    }
}

@Dao
interface RemoteBindingDao {
    @Upsert suspend fun upsert(entity: RemoteBindingEntity)
    @Upsert suspend fun upsertAll(entities: List<RemoteBindingEntity>)
    @Query("SELECT * FROM remote_bindings")
    suspend fun getAll(): List<RemoteBindingEntity>
    @Query("DELETE FROM remote_bindings")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entities: List<RemoteBindingEntity>) {
        clearAll()
        upsertAll(entities)
    }
}

@Dao
interface SourceDefinitionDao {
    @Upsert suspend fun upsert(entity: SourceDefinitionEntity)
    @Upsert suspend fun upsertAll(entities: List<SourceDefinitionEntity>)
    @Query("SELECT * FROM source_definitions ORDER BY sourceName")
    fun observeAll(): Flow<List<SourceDefinitionEntity>>
    @Query("SELECT * FROM source_definitions")
    suspend fun getAll(): List<SourceDefinitionEntity>
    @Query("UPDATE source_definitions SET enabled = NOT enabled WHERE sourceId = :sourceId")
    suspend fun toggleEnabled(sourceId: String)
    @Query("DELETE FROM source_definitions")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entities: List<SourceDefinitionEntity>) {
        clearAll()
        upsertAll(entities)
    }
}

fun BookRecord.toEntity() = BookRecordEntity(
    id = id,
    title = title,
    author = author,
    originType = originType.name,
    primaryFormat = primaryFormat.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun BookRecordEntity.asExternalModel() = BookRecord(
    id = id,
    title = title,
    author = author,
    originType = OriginType.valueOf(originType),
    primaryFormat = BookFormat.valueOf(primaryFormat),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun BookAsset.toEntity() = BookAssetEntity(
    bookId = bookId,
    assetRole = assetRole.name,
    storageUri = storageUri,
    mime = mime,
    size = size,
    hash = hash,
    syncPath = syncPath,
)

fun BookAssetEntity.asExternalModel() = BookAsset(
    bookId = bookId,
    assetRole = AssetRole.valueOf(assetRole),
    storageUri = storageUri,
    mime = mime,
    size = size,
    hash = hash,
    syncPath = syncPath,
)

fun ReadingState.toEntity() = ReadingStateEntity(
    bookId = bookId,
    locator = locator,
    chapterRef = chapterRef,
    progressPercent = progressPercent,
    bookmarks = bookmarks,
    notes = notes,
    updatedAt = updatedAt,
)

fun ReadingStateEntity.asExternalModel() = ReadingState(
    bookId = bookId,
    locator = locator,
    chapterRef = chapterRef,
    progressPercent = progressPercent,
    bookmarks = bookmarks,
    notes = notes,
    updatedAt = updatedAt,
)

fun RemoteBinding.toEntity() = RemoteBindingEntity(
    bookId = bookId,
    sourceId = sourceId,
    remoteBookId = remoteBookId,
    remoteBookUrl = remoteBookUrl,
    tocRef = tocRef,
    syncMode = syncMode.name,
    lastFetchedAt = lastFetchedAt,
)

fun RemoteBindingEntity.asExternalModel() = RemoteBinding(
    bookId = bookId,
    sourceId = sourceId,
    remoteBookId = remoteBookId,
    remoteBookUrl = remoteBookUrl,
    tocRef = tocRef,
    syncMode = RemoteSyncMode.valueOf(syncMode),
    lastFetchedAt = lastFetchedAt,
)

fun SourceDefinition.toEntity() = SourceDefinitionEntity(
    sourceId = sourceId,
    sourceName = sourceName,
    sourceType = sourceType.name,
    ruleFormat = ruleFormat.name,
    authState = authState,
    enabled = enabled,
    group = group,
)

fun SourceDefinitionEntity.asExternalModel() = SourceDefinition(
    sourceId = sourceId,
    sourceName = sourceName,
    sourceType = SourceType.valueOf(sourceType),
    ruleFormat = RuleFormat.valueOf(ruleFormat),
    authState = authState,
    enabled = enabled,
    group = group,
)
```

- [ ] **Step 4: Implement the Room database and DataStore wrapper**

```kotlin
class DatabaseConverters {
    @TypeConverter
    fun readingBookmarksToJson(value: List<ReadingBookmark>): String = Json.encodeToString(value)

    @TypeConverter
    fun jsonToReadingBookmarks(value: String): List<ReadingBookmark> =
        if (value.isBlank()) emptyList() else Json.decodeFromString(value)

    @TypeConverter
    fun notesToJson(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun jsonToNotes(value: String): List<String> =
        if (value.isBlank()) emptyList() else Json.decodeFromString(value)
}

@Database(
    entities = [
        BookRecordEntity::class,
        BookAssetEntity::class,
        ReadingStateEntity::class,
        SourceDefinitionEntity::class,
        RemoteBindingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class ReaderDatabase : RoomDatabase() {
    abstract fun bookRecordDao(): BookRecordDao
    abstract fun bookAssetDao(): BookAssetDao
    abstract fun readingStateDao(): ReadingStateDao
    abstract fun remoteBindingDao(): RemoteBindingDao
    abstract fun sourceDefinitionDao(): SourceDefinitionDao
}

// Add `replaceAll(...)` helpers on the DAOs used during sync restore so pull operations can
// clear stale rows and then upsert the downloaded snapshot in a single transaction.

data class PreferencesSnapshot(
    val owner: String,
    val repo: String,
    val branch: String,
    val token: String,
    val deviceId: String,
)

private val Context.dataStore by preferencesDataStore(name = "reader_prefs")
private val OWNER = stringPreferencesKey("github_owner")
private val REPO = stringPreferencesKey("github_repo")
private val BRANCH = stringPreferencesKey("github_branch")
private val TOKEN = stringPreferencesKey("github_token")
private val DEVICE_ID = stringPreferencesKey("device_id")

class ReaderPreferencesStore(private val context: Context) {
    val syncConfig: Flow<GitHubAuthConfig> = context.dataStore.data.map { prefs ->
        GitHubAuthConfig(
            owner = prefs[OWNER].orEmpty(),
            repo = prefs[REPO].orEmpty(),
            branch = prefs[BRANCH] ?: "main",
            token = prefs[TOKEN].orEmpty(),
        )
    }

    suspend fun saveGitHubConfig(owner: String, repo: String, branch: String, token: String) {
        context.dataStore.edit { prefs ->
            prefs[OWNER] = owner
            prefs[REPO] = repo
            prefs[BRANCH] = branch
            prefs[TOKEN] = token
        }
    }

    suspend fun getOrCreateDeviceId(): String {
        val existing = context.dataStore.data.first()[DEVICE_ID]
        if (existing != null) return existing
        val generated = UUID.randomUUID().toString()
        context.dataStore.edit { it[DEVICE_ID] = generated }
        return generated
    }

    suspend fun exportSnapshot(): PreferencesSnapshot = PreferencesSnapshot(
        owner = syncConfig.first().owner,
        repo = syncConfig.first().repo,
        branch = syncConfig.first().branch,
        token = syncConfig.first().token,
        deviceId = getOrCreateDeviceId(),
    )

    suspend fun importSnapshot(snapshot: PreferencesSnapshot) {
        saveGitHubConfig(snapshot.owner, snapshot.repo, snapshot.branch, snapshot.token)
        context.dataStore.edit { it[DEVICE_ID] = snapshot.deviceId }
    }
}

// After Task 3, extend `AppContainer` with:
// `val database by lazy { Room.databaseBuilder(appContext, ReaderDatabase::class.java, "reader.db").build() }`
// `val preferencesStore by lazy { ReaderPreferencesStore(appContext) }`
```

- [ ] **Step 5: Re-run database tests**

Run: `./gradlew :core:database:testDebugUnitTest`

Expected: PASS

- [ ] **Step 6: Commit the persistence layer**

```bash
git add core/database settings.gradle.kts
git commit -m "feat: add room persistence for reader data"
```

### Task 4: Build TXT/EPUB Import And Local Book Repositories

**Files:**
- Create: `data/localbooks/build.gradle.kts`
- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/TxtBookParser.kt`
- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/EpubBookParser.kt`
- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookModels.kt`
- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookFileStore.kt`
- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepository.kt`
- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepository.kt`
- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/ImportLocalBookUseCase.kt`
- Create: `data/localbooks/src/test/resources/fixtures/sample.txt`
- Create: `data/localbooks/src/test/resources/fixtures/sample.epub`
- Test: `data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepositoryTest.kt`

- [ ] **Step 1: Write the failing import test**

```kotlin
@Test
fun importEpub_createsBookRecordAssetAndInitialReadingState() = runTest {
    val repository = LocalBookImportRepository(
        txtParser = TxtBookParser(),
        epubParser = EpubBookParser(),
        fileStore = fakeLocalBookFileStore(),
        bookRecordDao = fakeBookRecordDao(),
        readingStateDao = fakeReadingStateDao(),
        bookAssetDao = fakeBookAssetDao(),
    )

    val result = repository.import(fileName = "sample.epub", inputStream = fixture("sample.epub"))

    assertEquals(BookFormat.EPUB, result.book.primaryFormat)
    assertTrue(result.assets.isNotEmpty())
    assertEquals(0f, result.readingState.progressPercent)
}
```

- [ ] **Step 2: Run the import test**

Run: `./gradlew :data:localbooks:test --tests "com.wenwentome.reader.data.localbooks.LocalBookImportRepositoryTest.importEpub_createsBookRecordAssetAndInitialReadingState"`

Expected: FAIL because the repository and parsers do not exist.

- [ ] **Step 3: Implement the TXT and EPUB parsers**

```kotlin
data class ParsedLocalBook(
    val title: String,
    val author: String?,
    val format: BookFormat,
    val assets: List<ParsedAsset>,
)

data class ParsedAsset(
    val assetRole: AssetRole,
    val bytes: ByteArray,
    val mime: String,
    val extension: String,
)

data class ImportedLocalBook(
    val book: BookRecord,
    val assets: List<BookAsset>,
    val readingState: ReadingState,
)

class TxtBookParser {
    fun parse(name: String, bytes: ByteArray): ParsedLocalBook = ParsedLocalBook(
        title = name.removeSuffix(".txt"),
        author = null,
        format = BookFormat.TXT,
        assets = listOf(
            ParsedAsset(
                assetRole = AssetRole.PRIMARY_TEXT,
                bytes = bytes,
                mime = "text/plain",
                extension = "txt",
            )
        ),
    )
}

class EpubBookParser {
    fun parse(name: String, inputStream: InputStream): ParsedLocalBook {
        val bytes = inputStream.readBytes()
        val book = EpubReader().readEpub(ByteArrayInputStream(bytes))
        return ParsedLocalBook(
            title = book.title ?: name.removeSuffix(".epub"),
            author = book.metadata.authors.firstOrNull()?.toString(),
            format = BookFormat.EPUB,
            assets = listOf(
                ParsedAsset(
                    assetRole = AssetRole.PRIMARY_TEXT,
                    bytes = bytes,
                    mime = "application/epub+zip",
                    extension = "epub",
                )
            ),
        )
    }
}
```

- [ ] **Step 4: Persist imported files and implement the import/content repositories**

```kotlin
class LocalBookFileStore(private val filesDir: File) {
    fun persistOriginal(bookId: String, extension: String, bytes: ByteArray): String {
        val bookDir = File(filesDir, "books/$bookId").apply { mkdirs() }
        val target = File(bookDir, "source.$extension")
        target.writeBytes(bytes)
        return target.toURI().toString()
    }

    fun open(storageUri: String): InputStream = File(URI(storageUri)).inputStream()
}

data class ReaderContent(val chapterTitle: String, val paragraphs: List<String>)

class LocalBookContentRepository(
    private val bookAssetDao: BookAssetDao,
    private val fileStore: LocalBookFileStore,
) {
    // Locator semantics for Phase 1:
    // - TXT: paragraph index as string, for example "0", "18"
    // - EPUB: "<spineIndex>:<paragraphIndex>", for example "3:12"
    suspend fun load(bookId: String, locator: String?): ReaderContent {
        val asset = requireNotNull(bookAssetDao.findPrimaryAsset(bookId)) { "Primary asset missing for $bookId" }
        return when (asset.mime) {
            "text/plain" -> renderTxt(fileStore.open(asset.storageUri), locator)
            "application/epub+zip" -> renderEpub(fileStore.open(asset.storageUri), locator)
            else -> error("Unsupported asset mime: ${asset.mime}")
        }
    }

    private fun renderTxt(inputStream: InputStream, locator: String?): ReaderContent {
        val paragraphs = inputStream.bufferedReader().readText()
            .split("\n")
            .map(String::trim)
            .filter(String::isNotBlank)
        val startIndex = locator?.toIntOrNull() ?: 0
        return ReaderContent(
            chapterTitle = "正文",
            paragraphs = paragraphs.drop(startIndex).take(60),
        )
    }

    private fun renderEpub(inputStream: InputStream, locator: String?): ReaderContent {
        val parts = locator?.split(":").orEmpty()
        val spineIndex = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val paragraphIndex = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val book = EpubReader().readEpub(inputStream)
        val resource = book.spine.spineReferences[spineIndex].resource
        val paragraphs = Jsoup.parse(resource.inputStream.readBytes().decodeToString())
            .text()
            .split("。")
            .map(String::trim)
            .filter(String::isNotBlank)
        return ReaderContent(
            chapterTitle = resource.title ?: "章节 ${spineIndex + 1}",
            paragraphs = paragraphs.drop(paragraphIndex).take(60),
        )
    }
}

suspend fun import(fileName: String, inputStream: InputStream): ImportedLocalBook {
    val parsed = when {
        fileName.endsWith(".txt", ignoreCase = true) -> txtParser.parse(fileName, inputStream.readBytes())
        fileName.endsWith(".epub", ignoreCase = true) -> epubParser.parse(fileName, inputStream)
        else -> error("Unsupported file format: $fileName")
    }

    val book = BookRecord.newLocal(parsed.title, parsed.author, parsed.format)
    val assets = parsed.assets.mapIndexed { index, asset ->
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(asset.bytes)
            .joinToString("") { byte -> "%02x".format(byte) }
        val storageUri = fileStore.persistOriginal(book.id, asset.extension, asset.bytes)
        BookAsset(
            bookId = book.id,
            assetRole = asset.assetRole,
            storageUri = storageUri,
            mime = asset.mime,
            size = asset.bytes.size.toLong(),
            hash = digest,
            syncPath = "books/${book.id}/asset-$index.${asset.extension}",
        )
    }
    val readingState = ReadingState.initial(book.id)

    bookRecordDao.upsert(book.toEntity())
    bookAssetDao.upsertAll(assets.map(BookAsset::toEntity))
    readingStateDao.upsert(readingState.toEntity())
    return ImportedLocalBook(book = book, assets = assets, readingState = readingState)
}

class ImportLocalBookUseCase(
    private val contentResolver: ContentResolver,
    private val repository: LocalBookImportRepository,
) {
    suspend operator fun invoke(uri: Uri) {
        val displayName = queryDisplayName(contentResolver, uri)
        contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "Cannot open input stream for $uri" }
            repository.import(displayName, stream)
        }
    }

    private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) cursor.getString(nameColumn) else uri.lastPathSegment ?: "imported-book"
        } ?: (uri.lastPathSegment ?: "imported-book")
}

// After Task 4, extend `AppContainer` with:
// `val fileStore by lazy { LocalBookFileStore(appContext.filesDir) }`
// `val localBookContentRepository by lazy { LocalBookContentRepository(database.bookAssetDao(), fileStore) }`
```

- [ ] **Step 5: Run the local book tests**

Run: `./gradlew :data:localbooks:test`

Expected: PASS

- [ ] **Step 6: Commit the local import pipeline**

```bash
git add data/localbooks settings.gradle.kts
git commit -m "feat: import local txt and epub books"
```

### Task 5: Implement The Unified Bookshelf Feature

**Files:**
- Create: `feature/library/build.gradle.kts`
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt`
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryViewModel.kt`
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryFilter.kt`
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ObserveBookshelfUseCase.kt`
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryUiState.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`
- Test: `feature/library/src/test/kotlin/com/wenwentome/reader/feature/library/LibraryViewModelTest.kt`

- [ ] **Step 1: Write the failing bookshelf state test**

```kotlin
@Test
fun defaultShelf_mergesLocalAndRemoteBooksIntoSingleList() = runTest {
    val viewModel = LibraryViewModel(
        observeBookshelf = fakeObserveBookshelfUseCase(
            flowOf(
            listOf(
                BookRecord.newLocal("悉达多", "黑塞", BookFormat.EPUB),
                BookRecord(id = "web-1", title = "雪中悍刀行", author = "烽火戏诸侯", originType = OriginType.WEB, primaryFormat = BookFormat.WEB),
            )
        )),
        importLocalBook = { _ -> },
    )

    val state = viewModel.uiState.first()
    assertEquals(2, state.visibleBooks.size)
}
```

- [ ] **Step 2: Run the bookshelf test**

Run: `./gradlew :feature:library:testDebugUnitTest --tests "com.wenwentome.reader.feature.library.LibraryViewModelTest.defaultShelf_mergesLocalAndRemoteBooksIntoSingleList"`

Expected: FAIL because `LibraryViewModel` and `LibraryUiState` do not exist.

- [ ] **Step 3: Implement the bookshelf state holder**

```kotlin
enum class LibraryFilter { All, LocalOnly, WebOnly }

class ObserveBookshelfUseCase(
    private val bookRecordDao: BookRecordDao,
) {
    operator fun invoke(): Flow<List<BookRecord>> =
        bookRecordDao.observeAll().map { entities -> entities.map(BookRecordEntity::asExternalModel) }
}

data class LibraryUiState(
    val selectedFilter: LibraryFilter = LibraryFilter.All,
    val visibleBooks: List<BookRecord> = emptyList(),
)

class LibraryViewModel(
    observeBookshelf: ObserveBookshelfUseCase,
    private val importLocalBook: suspend (Uri) -> Unit,
) : ViewModel() {
    val uiState: StateFlow<LibraryUiState> = observeBookshelf()
        .map { books -> LibraryUiState(visibleBooks = books.sortedByDescending { it.updatedAt }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun importFromUri(uri: Uri) = viewModelScope.launch {
        importLocalBook(uri)
    }
}
```

- [ ] **Step 4: Implement the bookshelf Compose screen**

```kotlin
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onImportClick: () -> Unit,
    onBookClick: (String) -> Unit,
) {
    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = onImportClick) { Text("+") } },
    ) { paddingValues ->
        LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = paddingValues) {
            items(state.visibleBooks, key = { it.id }) { book ->
                BookCard(book = book, onClick = { onBookClick(book.id) })
            }
        }
    }
}

@Composable
private fun BookCard(book: BookRecord, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = book.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text = book.author ?: "未知作者", style = MaterialTheme.typography.labelMedium)
        }
    }
}
```

- [ ] **Step 5: Wire the bookshelf route into the app shell**

```kotlin
// Extend `AppContainer` in Task 3 and Task 4 with `database` and `fileStore` before adding this route.
val context = LocalContext.current
val bookRecordDao = appContainer.database.bookRecordDao()
val importLocalBookUseCase = ImportLocalBookUseCase(
    contentResolver = context.contentResolver,
    repository = LocalBookImportRepository(
        txtParser = TxtBookParser(),
        epubParser = EpubBookParser(),
        fileStore = appContainer.fileStore,
        bookRecordDao = bookRecordDao,
        readingStateDao = appContainer.database.readingStateDao(),
        bookAssetDao = appContainer.database.bookAssetDao(),
    ),
)
val libraryViewModel = remember {
    LibraryViewModel(
        observeBookshelf = ObserveBookshelfUseCase(bookRecordDao),
        importLocalBook = importLocalBookUseCase::invoke,
    )
}
val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let(libraryViewModel::importFromUri)
}

composable(TopLevelDestination.BOOKSHELF.name) {
    LibraryScreen(
        state = libraryViewModel.uiState.collectAsStateWithLifecycle().value,
        onImportClick = { importLauncher.launch(arrayOf("text/plain", "application/epub+zip")) },
        onBookClick = { bookId -> navController.navigate("book/$bookId") },
    )
}
```

Run: `./gradlew :feature:library:testDebugUnitTest :app:testDebugUnitTest`

Expected: PASS

- [ ] **Step 6: Commit the bookshelf feature**

```bash
git add feature/library app/src/main/java/com/wenwentome/reader/navigation
git commit -m "feat: add unified bookshelf feature"
```

### Task 6: Implement Book Detail And Reader State

**Files:**
- Create: `feature/reader/build.gradle.kts`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailScreen.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModel.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderUiState.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`
- Test: `feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModelTest.kt`

- [ ] **Step 1: Write the failing reader-state restoration test**

```kotlin
@Test
fun openingBook_restoresSavedLocatorAndBookmarks() = runTest {
    val viewModel = ReaderViewModel(
        bookId = "book-1",
        observeReadingState = flowOf(
            ReadingState(
                bookId = "book-1",
                locator = "chapter-3",
                progressPercent = 0.31f,
                bookmarks = listOf(ReadingBookmark(chapterRef = "chapter-3", locator = "chapter-3", label = "重读这里")),
            )
        ),
        observeBook = flowOf(BookRecord.newLocal("悉达多", "黑塞", BookFormat.EPUB)),
        observeContent = flowOf(ReaderContent(chapterTitle = "第三章", paragraphs = listOf("正文第一段"))),
        updateReadingState = {},
    )

    val state = viewModel.uiState.first()
    assertEquals("chapter-3", state.locator)
    assertEquals(0.31f, state.progressPercent)
    assertEquals(1, state.bookmarks.size)
    assertEquals("第三章", state.chapterTitle)
}
```

- [ ] **Step 2: Run the reader-state test**

Run: `./gradlew :feature:reader:testDebugUnitTest --tests "com.wenwentome.reader.feature.reader.ReaderViewModelTest.openingBook_restoresSavedLocatorAndBookmarks"`

Expected: FAIL because reader classes do not exist.

- [ ] **Step 3: Implement the reader state holder**

```kotlin
data class ReaderUiState(
    val book: BookRecord? = null,
    val locator: String? = null,
    val progressPercent: Float = 0f,
    val chapterTitle: String? = null,
    val paragraphs: List<String> = emptyList(),
    val bookmarks: List<ReadingBookmark> = emptyList(),
)

class ReaderViewModel(
    bookId: String,
    observeBook: Flow<BookRecord?>,
    observeReadingState: Flow<ReadingState?>,
    // For local books this comes from `LocalBookContentRepository`; for remote books it can later
    // be backed by `SourceBridgeRepository.fetchChapterContent(...)` without changing reader state.
    observeContent: Flow<ReaderContent>,
    private val updateReadingState: suspend (ReadingState) -> Unit,
) : ViewModel() {
    val uiState: StateFlow<ReaderUiState> = combine(observeBook, observeReadingState, observeContent) { book, state, content ->
        ReaderUiState(
            book = book,
            locator = state?.locator,
            progressPercent = state?.progressPercent ?: 0f,
            chapterTitle = content.chapterTitle,
            paragraphs = content.paragraphs,
            bookmarks = state?.bookmarks.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())
}
```

- [ ] **Step 4: Implement the detail and reader screens**

```kotlin
@Composable
fun BookDetailScreen(book: BookRecord, onReadClick: () -> Unit, onSyncClick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = book.title, style = MaterialTheme.typography.headlineSmall)
        Text(text = book.author ?: "未知作者", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onReadClick) { Text("开始阅读") }
            OutlinedButton(onClick = onSyncClick) { Text("立即同步") }
        }
    }
}

@Composable
fun ReaderScreen(state: ReaderUiState, onLocatorChanged: (String, Float) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text(
            text = state.chapterTitle ?: state.book?.title.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )
        LinearProgressIndicator(progress = { state.progressPercent }, modifier = Modifier.fillMaxWidth())
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(state.paragraphs) { paragraph ->
                SelectionContainer {
                    Text(text = paragraph, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Button(
            onClick = { onLocatorChanged(state.locator ?: state.chapterTitle ?: "chapter-1", state.progressPercent) },
            modifier = Modifier.padding(16.dp),
        ) {
            Text("保存进度")
        }
    }
}

composable("book/{bookId}") { backStackEntry ->
    val bookId = requireNotNull(backStackEntry.arguments?.getString("bookId"))
    // By Task 6, `AppContainer` exposes `database` and `localBookContentRepository`.
    val bookRecordDao = appContainer.database.bookRecordDao()
    val readingStateDao = appContainer.database.readingStateDao()
    val localBookContentRepository = appContainer.localBookContentRepository
    val observeBook = remember(bookId) { bookRecordDao.observeById(bookId).map { it?.asExternalModel() } }
    val observeReadingState = remember(bookId) { readingStateDao.observeByBookId(bookId).map { it?.asExternalModel() } }
    val observeContent = remember(bookId) {
        observeReadingState.flatMapLatest { state ->
            flow { emit(localBookContentRepository.load(bookId, state?.locator)) }
        }
    }
    val viewModel = remember(bookId) {
        ReaderViewModel(
            bookId = bookId,
            observeBook = observeBook,
            observeReadingState = observeReadingState,
            observeContent = observeContent,
            updateReadingState = { state -> readingStateDao.upsert(state.toEntity()) },
        )
    }
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val book = state.book ?: return@composable
    BookDetailScreen(
        book = book,
        onReadClick = { navController.navigate("reader/$bookId") },
        onSyncClick = { navController.navigate(TopLevelDestination.SETTINGS.name) },
    )
}

composable("reader/{bookId}") { backStackEntry ->
    val bookId = requireNotNull(backStackEntry.arguments?.getString("bookId"))
    // By Task 6, `AppContainer` exposes `database` and `localBookContentRepository`.
    val bookRecordDao = appContainer.database.bookRecordDao()
    val readingStateDao = appContainer.database.readingStateDao()
    val localBookContentRepository = appContainer.localBookContentRepository
    val observeBook = remember(bookId) { bookRecordDao.observeById(bookId).map { it?.asExternalModel() } }
    val observeReadingState = remember(bookId) { readingStateDao.observeByBookId(bookId).map { it?.asExternalModel() } }
    val observeContent = remember(bookId) {
        observeReadingState.flatMapLatest { state ->
            flow { emit(localBookContentRepository.load(bookId, state?.locator)) }
        }
    }
    val viewModel = remember(bookId) {
        ReaderViewModel(
            bookId = bookId,
            observeBook = observeBook,
            observeReadingState = observeReadingState,
            observeContent = observeContent,
            updateReadingState = { state -> readingStateDao.upsert(state.toEntity()) },
        )
    }
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    ReaderScreen(
        state = state,
        onLocatorChanged = { locator, progress -> viewModel.updateLocator(locator, null, progress) },
    )
}
```

- [ ] **Step 5: Persist locator updates when the user reads**

```kotlin
fun updateLocator(locator: String, chapterRef: String?, progressPercent: Float) = viewModelScope.launch {
    updateReadingState(
        ReadingState(
            bookId = requireNotNull(uiState.value.book?.id),
            locator = locator,
            chapterRef = chapterRef,
            progressPercent = progressPercent,
            bookmarks = uiState.value.bookmarks,
            updatedAt = System.currentTimeMillis(),
        )
    )
}
```

Run: `./gradlew :feature:reader:testDebugUnitTest`

Expected: PASS

- [ ] **Step 6: Commit the reader feature**

```bash
git add feature/reader app/src/main/java/com/wenwentome/reader/navigation
git commit -m "feat: add reader detail and reading state"
```

### Task 7: Add GitHub Private-Repo Sync

**Files:**
- Create: `sync/github/build.gradle.kts`
- Create: `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/GitHubAuthConfig.kt`
- Create: `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/GitHubContentApi.kt`
- Create: `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/GitHubSyncRepository.kt`
- Create: `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/SyncManifest.kt`
- Create: `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/SyncManifestSerializer.kt`
- Create: `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/RestoredSnapshot.kt`
- Create: `feature/settings/build.gradle.kts`
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SettingsScreen.kt`
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsScreen.kt`
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsUiState.kt`
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsViewModel.kt`
- Test: `sync/github/src/test/kotlin/com/wenwentome/reader/sync/github/GitHubSyncRepositoryTest.kt`
- Test: `feature/settings/src/test/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsViewModelTest.kt`

- [ ] **Step 1: Write the failing sync round-trip test**

```kotlin
@Test
fun pushAndPullSnapshot_roundTripsManifestAndAssets() = runTest {
    val server = MockWebServer()
    val repository = GitHubSyncRepository(
        api = GitHubContentApi(server.url("/").toString()),
        serializer = SyncManifestSerializer(),
        bookRecordDao = fakeBookRecordDao(existing = listOf(sampleBookRecord())),
        readingStateDao = fakeReadingStateDao(existing = listOf(sampleReadingState())),
        remoteBindingDao = fakeRemoteBindingDao(),
        sourceDefinitionDao = fakeSourceDefinitionDao(),
        bookAssetDao = fakeBookAssetDao(existing = listOf(sampleAsset())),
        preferencesStore = fakePreferencesStore(samplePreferences()),
        fileStore = fakeLocalBookFileStore(existing = listOf(sampleAsset())),
    )

    repository.pushSnapshot(
        auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "token"),
    )
    val restored = repository.pullLatestSnapshot(
        auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "token"),
    )

    assertTrue(server.requestCount >= 8)
    assertEquals("snapshot-1", restored.snapshot.snapshotId)
    assertEquals(1, restored.assets.size)
}
```

- [ ] **Step 2: Run the failing sync test**

Run: `./gradlew :sync:github:test --tests "com.wenwentome.reader.sync.github.GitHubSyncRepositoryTest.pushAndPullSnapshot_roundTripsManifestAndAssets"`

Expected: FAIL because sync client classes do not exist.

- [ ] **Step 3: Implement the GitHub content API wrapper**

```kotlin
@Serializable
data class GitHubContentEnvelope(val content: String? = null, val sha: String? = null, @SerialName("download_url") val downloadUrl: String? = null)

class GitHubContentApi(private val baseUrl: String, private val client: OkHttpClient = OkHttpClient()) {
    suspend fun putJson(auth: GitHubAuthConfig, path: String, json: String, sha: String? = null): String =
        putFile(auth, path, json.toByteArray(), sha)

    suspend fun putBinary(auth: GitHubAuthConfig, path: String, bytes: ByteArray, sha: String? = null): String =
        putFile(auth, path, bytes, sha)

    suspend fun getJson(auth: GitHubAuthConfig, path: String): Pair<String, String?> {
        val envelope = getEnvelope(auth, path)
        val decoded = Base64.getDecoder().decode(requireNotNull(envelope.content).replace("\n", "")).decodeToString()
        return decoded to envelope.sha
    }

    suspend fun getBinary(auth: GitHubAuthConfig, path: String): Pair<ByteArray, String?> {
        val envelope = getEnvelope(auth, path)
        val bytes = if (envelope.content != null) {
            Base64.getDecoder().decode(envelope.content.replace("\n", ""))
        } else {
            executeBinaryGet(requireNotNull(envelope.downloadUrl), auth.token)
        }
        return bytes to envelope.sha
    }

    suspend fun findShaOrNull(auth: GitHubAuthConfig, path: String): String? =
        runCatching { getEnvelope(auth, path).sha }.getOrNull()

    private suspend fun putFile(auth: GitHubAuthConfig, path: String, bytes: ByteArray, sha: String?): String {
        val body = buildJsonObject {
            put("message", "sync: update $path")
            put("content", Base64.getEncoder().encodeToString(bytes))
            sha?.let { put("sha", it) }
            put("branch", auth.branch)
        }
        return executePut("$baseUrl/repos/${auth.owner}/${auth.repo}/contents/$path", auth.token, body).sha
    }

    private suspend fun getEnvelope(auth: GitHubAuthConfig, path: String): GitHubContentEnvelope =
        executeGetEnvelope("$baseUrl/repos/${auth.owner}/${auth.repo}/contents/$path", auth.token)

    private suspend fun executePut(url: String, token: String, body: JsonObject): GitHubContentEnvelope {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .put(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return client.newCall(request).awaitEnvelope()
    }

    private suspend fun executeGetEnvelope(url: String, token: String): GitHubContentEnvelope {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .get()
            .build()
        return client.newCall(request).awaitEnvelope()
    }

    private suspend fun executeBinaryGet(url: String, token: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        return client.newCall(request).awaitBytes()
    }
}

private suspend fun Call.awaitEnvelope(): GitHubContentEnvelope =
    suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = continuation.resumeWithException(e)
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = requireNotNull(it.body).string()
                    continuation.resume(Json.decodeFromString(body))
                }
            }
        })
    }

private suspend fun Call.awaitBytes(): ByteArray =
    suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = continuation.resumeWithException(e)
            override fun onResponse(call: Call, response: Response) {
                response.use { continuation.resume(requireNotNull(it.body).bytes()) }
            }
        })
    }
```

- [ ] **Step 4: Implement snapshot serialization and sync orchestration**

```kotlin
data class SyncManifest(
    val snapshotPath: String = "manifests/latest.json",
    val bookRecordsPath: String = "data/book-records.json",
    val readingStatesPath: String = "data/reading-states.json",
    val remoteBindingsPath: String = "data/remote-bindings.json",
    val sourceDefinitionsPath: String = "data/source-definitions.json",
    val preferencesPath: String = "data/preferences.json",
    val assetIndexPath: String = "data/assets.json",
)

suspend fun pushSnapshot(auth: GitHubAuthConfig) {
    val manifest = SyncManifest()
    val bookRecords = bookRecordDao.getAll().map(BookRecordEntity::asExternalModel)
    val readingStates = readingStateDao.getAll().map(ReadingStateEntity::asExternalModel)
    val remoteBindings = remoteBindingDao.getAll().map(RemoteBindingEntity::asExternalModel)
    val sourceDefinitions = sourceDefinitionDao.getAll().map(SourceDefinitionEntity::asExternalModel)
    val preferences = preferencesStore.exportSnapshot()
    val assets = bookAssetDao.getAll().map(BookAssetEntity::asExternalModel)
    val existingSha = listOf(
        manifest.bookRecordsPath,
        manifest.readingStatesPath,
        manifest.remoteBindingsPath,
        manifest.sourceDefinitionsPath,
        manifest.preferencesPath,
        manifest.assetIndexPath,
        manifest.snapshotPath,
        *assets.map(BookAsset::syncPath).toTypedArray(),
    ).associateWith { path -> api.findShaOrNull(auth, path) }

    api.putJson(auth, manifest.bookRecordsPath, serializer.encodeBookRecords(bookRecords), existingSha[manifest.bookRecordsPath])
    api.putJson(auth, manifest.readingStatesPath, serializer.encodeReadingStates(readingStates), existingSha[manifest.readingStatesPath])
    api.putJson(auth, manifest.remoteBindingsPath, serializer.encodeRemoteBindings(remoteBindings), existingSha[manifest.remoteBindingsPath])
    api.putJson(auth, manifest.sourceDefinitionsPath, serializer.encodeSourceDefinitions(sourceDefinitions), existingSha[manifest.sourceDefinitionsPath])
    api.putJson(auth, manifest.preferencesPath, serializer.encodePreferences(preferences), existingSha[manifest.preferencesPath])
    api.putJson(auth, manifest.assetIndexPath, serializer.encodeAssets(assets), existingSha[manifest.assetIndexPath])

    assets.forEach { asset ->
        val bytes = fileStore.open(asset.storageUri).readBytes()
        api.putBinary(auth, asset.syncPath, bytes, existingSha[asset.syncPath])
    }

    val snapshot = SyncSnapshot(
        snapshotId = UUID.randomUUID().toString(),
        revision = Instant.now().toString(),
        deviceId = preferencesStore.getOrCreateDeviceId(),
        mergedAt = System.currentTimeMillis(),
        manifestJson = serializer.encodeManifest(manifest),
    )
    api.putJson(auth, manifest.snapshotPath, serializer.encodeSnapshot(snapshot), existingSha[manifest.snapshotPath])
}

suspend fun pullLatestSnapshot(auth: GitHubAuthConfig): RestoredSnapshot {
    val (snapshotJson, _) = api.getJson(auth, "manifests/latest.json")
    val snapshot = serializer.decodeSnapshot(snapshotJson)
    val manifest = serializer.decodeManifest(snapshot.manifestJson)

    val bookRecords = serializer.decodeBookRecords(api.getJson(auth, manifest.bookRecordsPath).first)
    val readingStates = serializer.decodeReadingStates(api.getJson(auth, manifest.readingStatesPath).first)
    val remoteBindings = serializer.decodeRemoteBindings(api.getJson(auth, manifest.remoteBindingsPath).first)
    val sourceDefinitions = serializer.decodeSourceDefinitions(api.getJson(auth, manifest.sourceDefinitionsPath).first)
    val preferences = serializer.decodePreferences(api.getJson(auth, manifest.preferencesPath).first)
    val assetIndex = serializer.decodeAssets(api.getJson(auth, manifest.assetIndexPath).first)

    bookRecordDao.replaceAll(bookRecords.map(BookRecord::toEntity))
    readingStateDao.replaceAll(readingStates.map(ReadingState::toEntity))
    remoteBindingDao.replaceAll(remoteBindings.map(RemoteBinding::toEntity))
    sourceDefinitionDao.replaceAll(sourceDefinitions.map(SourceDefinition::toEntity))
    preferencesStore.importSnapshot(preferences)

    val restoredAssets = assetIndex.map { asset ->
        val (bytes, _) = api.getBinary(auth, asset.syncPath)
        val storageUri = fileStore.persistOriginal(asset.bookId, asset.syncPath.substringAfterLast('.'), bytes)
        asset.copy(storageUri = storageUri)
    }
    bookAssetDao.replaceAll(restoredAssets.map(BookAsset::toEntity))

    return RestoredSnapshot(snapshot = snapshot, assets = restoredAssets)
}

// After Task 7, extend `AppContainer` with:
// `val gitHubSyncRepository by lazy { GitHubSyncRepository(..., preferencesStore = preferencesStore, fileStore = fileStore) }`
```

- [ ] **Step 5: Add the sync settings state holder and screen**

```kotlin
data class SyncSettingsUiState(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val token: String = "",
) {
    fun updateOwner(value: String) = copy(owner = value)
    fun updateRepo(value: String) = copy(repo = value)
    fun updateBranch(value: String) = copy(branch = value)
    fun updateToken(value: String) = copy(token = value)
    fun toConfig() = GitHubAuthConfig(owner = owner, repo = repo, branch = branch, token = token)
}

class SyncSettingsViewModel(
    private val preferencesStore: ReaderPreferencesStore,
    private val syncRepository: GitHubSyncRepository,
) : ViewModel() {
    private val draft = MutableStateFlow(SyncSettingsUiState())

    init {
        viewModelScope.launch {
            preferencesStore.syncConfig.collect { config ->
                draft.value = SyncSettingsUiState(config.owner, config.repo, config.branch, config.token)
            }
        }
    }

    val uiState: StateFlow<SyncSettingsUiState> = draft.asStateFlow()

    fun setDraft(state: SyncSettingsUiState) {
        draft.value = state
    }

    fun saveConfig(state: SyncSettingsUiState) = viewModelScope.launch {
        preferencesStore.saveGitHubConfig(state.owner, state.repo, state.branch, state.token)
    }

    fun pushNow(state: SyncSettingsUiState) = viewModelScope.launch {
        preferencesStore.saveGitHubConfig(state.owner, state.repo, state.branch, state.token)
        syncRepository.pushSnapshot(state.toConfig())
    }

    fun pullNow(state: SyncSettingsUiState) = viewModelScope.launch {
        preferencesStore.saveGitHubConfig(state.owner, state.repo, state.branch, state.token)
        syncRepository.pullLatestSnapshot(state.toConfig())
    }
}

@Composable
fun SyncSettingsScreen(
    state: SyncSettingsUiState,
    onStateChange: (SyncSettingsUiState) -> Unit,
    onSaveConfig: (SyncSettingsUiState) -> Unit,
    onPush: (SyncSettingsUiState) -> Unit,
    onPull: (SyncSettingsUiState) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = state.owner, onValueChange = { onStateChange(state.updateOwner(it)) }, label = { Text("Owner") })
        OutlinedTextField(value = state.repo, onValueChange = { onStateChange(state.updateRepo(it)) }, label = { Text("Repo") })
        OutlinedTextField(value = state.branch, onValueChange = { onStateChange(state.updateBranch(it)) }, label = { Text("Branch") })
        OutlinedTextField(value = state.token, onValueChange = { onStateChange(state.updateToken(it)) }, label = { Text("Token") })
        Button(onClick = { onSaveConfig(state) }) { Text("保存配置") }
        Button(onClick = { onPush(state) }) { Text("立即备份") }
        OutlinedButton(onClick = { onPull(state) }) { Text("恢复到本机") }
    }
}

@Composable
fun SettingsScreen(
    state: SyncSettingsUiState,
    onStateChange: (SyncSettingsUiState) -> Unit,
    onSaveConfig: (SyncSettingsUiState) -> Unit,
    onPush: (SyncSettingsUiState) -> Unit,
    onPull: (SyncSettingsUiState) -> Unit,
) {
    SyncSettingsScreen(
        state = state,
        onStateChange = onStateChange,
        onSaveConfig = onSaveConfig,
        onPush = onPush,
        onPull = onPull,
    )
}
```

- [ ] **Step 6: Run sync and settings tests**

Run: `./gradlew :sync:github:test :feature:settings:testDebugUnitTest`

Expected: PASS

- [ ] **Step 7: Commit the sync feature**

```bash
git add sync/github feature/settings settings.gradle.kts
git commit -m "feat: add github private repo sync"
```

### Task 8: Build The Source Bridge Core

**Files:**
- Create: `bridge/source/build.gradle.kts`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/SourceRuleParser.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/LegacySourceMapper.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/model/LegacySourceDefinition.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/model/NormalizedSourceDefinition.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/model/RemoteBookModels.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/JsoupRuleExecutor.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/XPathRuleExecutor.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/JsonPathRuleExecutor.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/RhinoScriptEvaluator.kt`
- Create: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/SourceBridgeRepository.kt`
- Create: `bridge/source/src/test/resources/fixtures/legacy-source.json`
- Create: `bridge/source/src/test/resources/fixtures/search-result.html`
- Create: `bridge/source/src/test/resources/fixtures/book-detail.html`
- Create: `bridge/source/src/test/resources/fixtures/chapter-list.html`
- Create: `bridge/source/src/test/resources/fixtures/chapter-content.html`
- Test: `bridge/source/src/test/kotlin/com/wenwentome/reader/bridge/source/SourceRuleParserTest.kt`

- [ ] **Step 1: Write the failing rule parser test**

```kotlin
@Test
fun legacySource_searchDetailTocAndContentRules_areNormalized() {
    val source = fixture("legacy-source.json").decodeToString()
    val parsed = SourceRuleParser().parse(source)

    assertEquals("测试源", parsed.name)
    assertNotNull(parsed.searchRule)
    assertNotNull(parsed.bookInfoRule)
    assertNotNull(parsed.tocRule)
    assertNotNull(parsed.contentRule)
}
```

- [ ] **Step 2: Run the source parser test**

Run: `./gradlew :bridge:source:test --tests "com.wenwentome.reader.bridge.source.SourceRuleParserTest.legacySource_searchDetailTocAndContentRules_areNormalized"`

Expected: FAIL because parser and normalized rule models do not exist.

- [ ] **Step 3: Implement normalized rule parsing**

```kotlin
class SourceRuleParser {
    fun parse(rawJson: String): NormalizedSourceDefinition {
        val legacy = Json.decodeFromString<LegacySourceDefinition>(rawJson)
        return LegacySourceMapper().map(legacy)
    }
}
```

- [ ] **Step 4: Implement selector executors for HTML and JSON**

```kotlin
class JsoupRuleExecutor {
    fun select(document: Document, expression: String): List<String> {
        val selector = expression.substringBefore("@")
        val extractor = expression.substringAfter("@", "text")
        return document.select(selector).map { element ->
            when {
                extractor == "text" -> element.text()
                extractor == "html" -> element.html()
                extractor.startsWith("attr:") -> element.attr(extractor.removePrefix("attr:"))
                else -> element.text()
            }
        }
    }
}

class XPathRuleExecutor {
    fun select(html: String, expression: String): List<String> =
        Jsoup.parse(html).selectXpath(expression).map { it.text() }
}

class JsonPathRuleExecutor {
    fun select(json: String, expression: String): List<String> =
        JsonPath.read<List<Any?>>(json, expression).mapNotNull { it?.toString() }
}
```

- [ ] **Step 5: Add Rhino-backed JS evaluation for rule fragments**

```kotlin
class RhinoScriptEvaluator {
    fun eval(script: String, bindings: Map<String, Any?>): String {
        val context = Context.enter()
        return try {
            val scope = context.initSafeStandardObjects()
            bindings.forEach { (key, value) -> ScriptableObject.putProperty(scope, key, value) }
            Context.toString(context.evaluateString(scope, script, "rule.js", 1, null))
        } finally {
            Context.exit()
        }
    }
}
```

- [ ] **Step 6: Implement the source bridge repository contract**

```kotlin
data class RemoteSearchResult(val id: String, val sourceId: String, val title: String, val author: String?, val detailUrl: String)
data class RemoteBookDetail(val title: String, val author: String?, val summary: String?, val coverUrl: String?)
data class RemoteChapter(val chapterRef: String, val title: String)
data class RemoteChapterContent(val chapterRef: String, val title: String, val content: String)

interface SourceBridgeRepository {
    suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult>
    suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail
    suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter>
    suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent
}

class RealSourceBridgeRepository(
    private val sourceDefinitionDao: SourceDefinitionDao,
    private val httpClient: OkHttpClient,
    private val parser: SourceRuleParser,
) : SourceBridgeRepository {
    override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> {
        val sources = sourceDefinitionDao.getAll()
            .map(SourceDefinitionEntity::asExternalModel)
            .filter { it.enabled && (sourceIds.isEmpty() || it.sourceId in sourceIds) }
        return sources.flatMap { source ->
            val requestUrl = buildSearchUrl(source, query)
            val document = httpClient.newCall(Request.Builder().url(requestUrl).build()).awaitDocument()
            executeSearch(source, document)
        }
    }

    override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail {
        val source = requireSource(sourceId)
        val document = httpClient.newCall(Request.Builder().url(buildDetailUrl(source, remoteBookId)).build()).awaitDocument()
        return executeDetail(source, document)
    }

    override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> {
        val source = requireSource(sourceId)
        val document = httpClient.newCall(Request.Builder().url(buildTocUrl(source, remoteBookId)).build()).awaitDocument()
        return executeToc(source, document)
    }

    override suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent {
        val source = requireSource(sourceId)
        val document = httpClient.newCall(Request.Builder().url(chapterRef).build()).awaitDocument()
        return executeContent(source, document, chapterRef)
    }

    private suspend fun requireSource(sourceId: String): SourceDefinition =
        sourceDefinitionDao.getAll()
            .map(SourceDefinitionEntity::asExternalModel)
            .first { it.sourceId == sourceId }

    private fun buildSearchUrl(source: SourceDefinition, query: String): String = "${source.sourceId}/search?q=${query.encodeURLParameter()}"
    private fun buildDetailUrl(source: SourceDefinition, remoteBookId: String): String = "${source.sourceId}/detail/$remoteBookId"
    private fun buildTocUrl(source: SourceDefinition, remoteBookId: String): String = "${source.sourceId}/toc/$remoteBookId"

    private fun executeSearch(source: SourceDefinition, document: Document): List<RemoteSearchResult> = emptyList()
    private fun executeDetail(source: SourceDefinition, document: Document): RemoteBookDetail = RemoteBookDetail("", null, null, null)
    private fun executeToc(source: SourceDefinition, document: Document): List<RemoteChapter> = emptyList()
    private fun executeContent(source: SourceDefinition, document: Document, chapterRef: String): RemoteChapterContent =
        RemoteChapterContent(chapterRef = chapterRef, title = document.title(), content = document.text())
}

private suspend fun Call.awaitDocument(): Document =
    suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = continuation.resumeWithException(e)
            override fun onResponse(call: Call, response: Response) {
                response.use { continuation.resume(Jsoup.parse(requireNotNull(it.body).string())) }
            }
        })
    }

private fun String.encodeURLParameter(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

// After Task 8, extend `AppContainer` with:
// `val sourceBridgeRepository by lazy { RealSourceBridgeRepository(database.sourceDefinitionDao(), OkHttpClient(), SourceRuleParser()) }`
```

- [ ] **Step 7: Re-run bridge tests**

Run: `./gradlew :bridge:source:test`

Expected: PASS

- [ ] **Step 8: Commit the source bridge**

```bash
git add bridge/source settings.gradle.kts
git commit -m "feat: add web source bridge core"
```

### Task 9: Build Discover And Add-To-Bookshelf Integration

**Files:**
- Create: `feature/discover/build.gradle.kts`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverScreen.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModel.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverUiState.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/AddRemoteBookToShelfUseCase.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/ImportSourcesUseCase.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/SourceManagementScreen.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/SourceManagementViewModel.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/SourceManagementUiState.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`
- Test: `feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModelTest.kt`

- [ ] **Step 1: Write the failing add-to-bookshelf test**

```kotlin
@Test
fun addingSearchResult_createsBookRecordAndRemoteBinding() = runTest {
    val addRemoteBookUseCase = fakeAddRemoteBookUseCase()
    val viewModel = DiscoverViewModel(
        sourceBridgeRepository = fakeBridgeRepository(searchResults = listOf(sampleSearchResult())),
        addRemoteBookToShelf = addRemoteBookUseCase,
    )

    viewModel.search("雪中悍刀行")
    viewModel.addToShelf(sampleSearchResult().id)

    assertEquals("雪中悍刀行", viewModel.uiState.value.lastAddedTitle)
    assertEquals(1, addRemoteBookUseCase.invocations.size)
}
```

- [ ] **Step 2: Run the discover test**

Run: `./gradlew :feature:discover:testDebugUnitTest --tests "com.wenwentome.reader.feature.discover.DiscoverViewModelTest.addingSearchResult_createsBookRecordAndRemoteBinding"`

Expected: FAIL because the discover feature does not exist.

- [ ] **Step 3: Implement the discover state holder**

```kotlin
data class DiscoverUiState(
    val query: String = "",
    val results: List<RemoteSearchResult> = emptyList(),
    val lastAddedTitle: String? = null,
)

data class SourceManagementUiState(
    val sources: List<SourceDefinition> = emptyList(),
)

class DiscoverViewModel(
    private val sourceBridgeRepository: SourceBridgeRepository,
    private val addRemoteBookToShelf: AddRemoteBookToShelfUseCase,
) : ViewModel() {
    val uiState = MutableStateFlow(DiscoverUiState())

    fun search(query: String) = viewModelScope.launch {
        uiState.update {
            it.copy(query = query, results = sourceBridgeRepository.search(query, emptyList()))
        }
    }

    fun addToShelf(resultId: String) = viewModelScope.launch {
        val result = uiState.value.results.first { it.id == resultId }
        addRemoteBookToShelf(result)
        uiState.update { it.copy(lastAddedTitle = result.title) }
    }
}

class SourceManagementViewModel(
    observeSources: Flow<List<SourceDefinition>>,
    private val toggleSourceEnabled: suspend (String) -> Unit,
) : ViewModel() {
    val uiState: StateFlow<SourceManagementUiState> = observeSources
        .map(::SourceManagementUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SourceManagementUiState())

    fun toggleEnabled(sourceId: String) = viewModelScope.launch {
        toggleSourceEnabled(sourceId)
    }
}
```

- [ ] **Step 4: Implement add-to-shelf and source-import use cases**

```kotlin
class AddRemoteBookToShelfUseCase(
    private val sourceBridgeRepository: SourceBridgeRepository,
    private val bookRecordDao: BookRecordDao,
    private val remoteBindingDao: RemoteBindingDao,
) {
    suspend operator fun invoke(result: RemoteSearchResult) {
        val detail = sourceBridgeRepository.fetchBookDetail(result.sourceId, result.id)
        val toc = sourceBridgeRepository.fetchToc(result.sourceId, result.id)
        val bookId = java.util.UUID.randomUUID().toString()
        val book = BookRecord(
            id = bookId,
            title = detail.title.ifBlank { result.title },
            author = detail.author ?: result.author,
            originType = OriginType.WEB,
            primaryFormat = BookFormat.WEB,
            cover = detail.coverUrl,
            summary = detail.summary,
        )
        bookRecordDao.upsert(book.toEntity())
        remoteBindingDao.upsert(
            RemoteBinding(
                bookId = book.id,
                sourceId = result.sourceId,
                remoteBookId = result.id,
                remoteBookUrl = result.detailUrl,
                tocRef = toc.firstOrNull()?.chapterRef,
            ).toEntity()
        )
    }
}

class ImportSourcesUseCase(
    private val sourceRuleParser: SourceRuleParser,
    private val sourceDefinitionDao: SourceDefinitionDao,
) {
    suspend operator fun invoke(rawJsonArray: String) {
        Json.parseToJsonElement(rawJsonArray).jsonArray.forEach { element ->
            sourceDefinitionDao.upsert(sourceRuleParser.parse(element.toString()).toEntity())
        }
    }
}
```

- [ ] **Step 5: Implement discover and source-management screens**

```kotlin
@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    onSearch: (String) -> Unit,
    onAddToShelf: (String) -> Unit,
    onManageSources: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onManageSources) { Text("书源管理") }
        OutlinedTextField(value = state.query, onValueChange = onSearch, label = { Text("搜索网文") })
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.results, key = { it.id }) { result ->
                ListItem(
                    headlineContent = { Text(result.title) },
                    supportingContent = { Text(result.author ?: "未知作者") },
                    trailingContent = { Button(onClick = { onAddToShelf(result.id) }) { Text("加入书库") } },
                )
            }
        }
    }
}

@Composable
fun SourceManagementScreen(state: SourceManagementUiState, onImportJson: () -> Unit, onToggleSource: (String) -> Unit) {
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = onImportJson) { Text("导入") } }) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            items(state.sources, key = { it.sourceId }) { source ->
                ListItem(
                    headlineContent = { Text(source.sourceName) },
                    supportingContent = { Text(source.group ?: "未分组") },
                    trailingContent = {
                        Switch(checked = source.enabled, onCheckedChange = { onToggleSource(source.sourceId) })
                    },
                )
            }
        }
    }
}
```

- [ ] **Step 6: Wire discover into the root nav graph**

```kotlin
// By Task 7 and Task 8, extend `AppContainer` with `preferencesStore`, `gitHubSyncRepository`,
// and `sourceBridgeRepository`.
val context = LocalContext.current
val scope = rememberCoroutineScope()
val sourceDefinitionDao = appContainer.database.sourceDefinitionDao()
val sourceManagementViewModel = remember {
    SourceManagementViewModel(
        observeSources = sourceDefinitionDao.observeAll().map { list -> list.map(SourceDefinitionEntity::asExternalModel) },
        toggleSourceEnabled = { sourceId -> sourceDefinitionDao.toggleEnabled(sourceId) },
    )
}
val discoverViewModel = remember {
    DiscoverViewModel(
        sourceBridgeRepository = appContainer.sourceBridgeRepository,
        addRemoteBookToShelf = AddRemoteBookToShelfUseCase(
            sourceBridgeRepository = appContainer.sourceBridgeRepository,
            bookRecordDao = appContainer.database.bookRecordDao(),
            remoteBindingDao = appContainer.database.remoteBindingDao(),
        ),
    )
}
val syncSettingsViewModel = remember {
    SyncSettingsViewModel(
        preferencesStore = appContainer.preferencesStore,
        syncRepository = appContainer.gitHubSyncRepository,
    )
}
val importSourcesUseCase = remember {
    ImportSourcesUseCase(
        sourceRuleParser = SourceRuleParser(),
        sourceDefinitionDao = sourceDefinitionDao,
    )
}
val sourceImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let { selectedUri ->
        scope.launch {
            val rawJson = context.contentResolver.openInputStream(selectedUri)!!.bufferedReader().readText()
            importSourcesUseCase(rawJson)
        }
    }
}

composable(TopLevelDestination.DISCOVER.name) {
    DiscoverScreen(
        state = discoverViewModel.uiState.collectAsStateWithLifecycle().value,
        onSearch = discoverViewModel::search,
        onAddToShelf = discoverViewModel::addToShelf,
        onManageSources = { navController.navigate("discover/sources") },
    )
}

composable("discover/sources") {
    SourceManagementScreen(
        state = sourceManagementViewModel.uiState.collectAsStateWithLifecycle().value,
        onImportJson = { sourceImportLauncher.launch(arrayOf("application/json", "text/plain")) },
        onToggleSource = sourceManagementViewModel::toggleEnabled,
    )
}

composable(TopLevelDestination.SETTINGS.name) {
    SettingsScreen(
        state = syncSettingsViewModel.uiState.collectAsStateWithLifecycle().value,
        onStateChange = { syncSettingsViewModel.setDraft(it) },
        onSaveConfig = syncSettingsViewModel::saveConfig,
        onPush = syncSettingsViewModel::pushNow,
        onPull = syncSettingsViewModel::pullNow,
    )
}
```

Run: `./gradlew :feature:discover:testDebugUnitTest :app:testDebugUnitTest`

Expected: PASS

- [ ] **Step 7: Commit discover integration**

```bash
git add feature/discover app/src/main/java/com/wenwentome/reader/navigation
git commit -m "feat: add discover and remote add-to-bookshelf flow"
```

### Task 10: Harden Packaging And Smoke Coverage

**Files:**
- Modify: `.github/workflows/android-ci.yml`
- Create: `app/src/test/java/com/wenwentome/reader/AppSmokeFlowTest.kt`

- [ ] **Step 1: Write the failing end-to-end smoke test**

```kotlin
@Test
fun appShell_topLevelRoutesRemainReachable() {
    val appContainer = AppContainer(ApplicationProvider.getApplicationContext())
    composeTestRule.setContent { ReaderApp(appContainer = appContainer) }
    composeTestRule.onNodeWithText("书库").assertExists()
    composeTestRule.onNodeWithText("发现").performClick()
    composeTestRule.onNodeWithText("我的").performClick()
    composeTestRule.onNodeWithText("立即备份").assertExists()
}
```

- [ ] **Step 2: Run the smoke test**

Run: `./gradlew :app:testDebugUnitTest --tests "com.wenwentome.reader.AppSmokeFlowTest.appShell_topLevelRoutesRemainReachable"`

Expected: FAIL if route labels, shell wiring, or root module dependencies are still unstable.

- [ ] **Step 3: Fix the shell wiring and stabilize the test**

```kotlin
// Keep navigation destinations as a single source of truth.
val topLevelDestinations = listOf(
    TopLevelDestination.BOOKSHELF,
    TopLevelDestination.DISCOVER,
    TopLevelDestination.SETTINGS,
)
```

- [ ] **Step 4: Update CI to publish APK artifacts**

```yaml
- run: ./gradlew test testDebugUnitTest assembleDebug
- uses: actions/upload-artifact@v4
  with:
    name: debug-apk
    path: app/build/outputs/apk/debug/*.apk
```

- [ ] **Step 5: Run the final verification pass**

Run: `./gradlew test testDebugUnitTest assembleDebug`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit the hardening pass**

```bash
git add .github/workflows/android-ci.yml app/src/test/java/com/wenwentome/reader/AppSmokeFlowTest.kt
git commit -m "test: harden shell smoke coverage and apk packaging"
```

## Execution Notes

- Execute tasks in order. Later tasks assume earlier module boundaries and model names.
- Do not fold `SourceDefinition` into `BookRecord`.
- Do not add PDF support in this plan.
- Keep the `Source Bridge` isolated even if that means temporary adapter classes.
- Imported local files must be copied into app-private storage under `files/books/<bookId>/`; do not treat transient SAF URIs as the long-term storage model.
- Phase 1 must ship a complete local-reading loop for TXT/EPUB: import -> private-file persistence -> content load ->正文渲染 -> progress writeback.
- Web books in Phase 1 must support discover -> add to shelf -> detail metadata; remote正文拉取 should reuse the same reader state model once `Source Bridge` content APIs are wired.
- Prefer failing tests in the smallest module that can prove the behavior.
- After each task, stop and verify the project still builds before moving on.
