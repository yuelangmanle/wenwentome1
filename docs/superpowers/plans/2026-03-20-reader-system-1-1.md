# Reader System 1.1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade WenwenToMe from the `1.0` MVP reader into a unified `1.1` reading system that fixes EPUB正文入口, adds modern bookshelf/detail/reader UX, and completes the web-novel reading loop.

**Architecture:** Keep the existing multi-module Android app and do a half-refactor rather than a rebuild. The core move is to unify chapter catalog, logical locator, progress state, cover asset handling, and remote update metadata so local TXT, EPUB, and web novels can share one bookshelf, one detail screen, and one reader shell with mode-specific presentation.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose, Room, DataStore, Coroutines/Flow, epublib, OkHttp, jsoup, GitHub Actions

---

## Scope Note

This plan intentionally keeps `1.1` in a single execution plan because the “bookshelf redesign”, “reader upgrade”, and “web novel completion” are coupled by the same model changes:

- chapter catalog / locator semantics
- reading progress persistence
- cover asset override strategy
- remote update metadata

Do **not** split these into separate implementation branches unless you also split the shared model/database work first.

Implementation guidance:

- Use `@superpowers:subagent-driven-development` when executing the plan
- Use `@superpowers:test-driven-development` inside each task before implementation
- Use `@superpowers:verification-before-completion` before claiming a task is done
- If local Java remains unavailable, treat GitHub Actions `android-ci` as the final verification source

## File Structure

Lock these responsibilities before writing code.

- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReaderChapter.kt`
  Purpose: Shared chapter catalog entry model for TXT / EPUB / WEB readers.
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReaderMode.kt`
  Purpose: Shared reader mode enum so preferences, UI state, and tests use the same type.
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReaderPresentationPrefs.kt`
  Purpose: Shared reader typography / theme / brightness preference model for all reader modes.
- Modify: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReadingState.kt`
  Purpose: Persist logical locator semantics instead of UI-page-specific state.
- Modify: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/RemoteBinding.kt`
  Purpose: Store latest known remote chapter metadata and catalog refresh timestamps.
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ReadingStateEntity.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/RemoteBindingEntity.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/DatabaseMappers.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/ReaderDatabase.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseMigrations.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/datastore/ReaderPreferencesStore.kt`
  Purpose: Save the global default reader mode plus typography/theme/brightness defaults.
- Test: `core/database/src/test/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseTest.kt`
- Test: `core/database/src/test/kotlin/com/wenwentome/reader/core/database/datastore/ReaderPreferencesStoreTest.kt`
- Create: `core/database/src/androidTest/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseMigrationTest.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/di/AppContainer.kt`
  Purpose: Register the explicit `2 -> 3` Room migration instead of relying on destructive fallback for the known release upgrade path.

- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/EpubCatalogParser.kt`
  Purpose: Parse TOC/nav, identify first readable chapter, and keep cover parsing separate from正文入口.
- Create: `data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepositoryTest.kt`
- Create: `data/localbooks/src/test/resources/fixtures/sample-cover-first.epub`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookModels.kt`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/EpubBookParser.kt`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepository.kt`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepository.kt`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookFileStore.kt`
- Test: `data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepositoryTest.kt`
  Purpose: Ensure EPUB imports extract cover asset and reader entry chooses正文而不是 Cover.

- Modify: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/model/RemoteBookModels.kt`
- Modify: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/SourceBridgeRepository.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/RefreshRemoteBookUseCase.kt`
- Create: `feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/RefreshRemoteBookUseCaseTest.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/AddRemoteBookToShelfUseCase.kt`
- Modify: `feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/AddRemoteBookToShelfUseCaseTest.kt`
  Purpose: Add latest-chapter metadata, manual refresh semantics, and “has updates” calculation inputs.

- Create: `app/src/main/java/com/wenwentome/reader/navigation/ReaderContentFlows.kt`
  Purpose: Keep local/web reader content resolution out of `AppNavHost.kt`.
- Modify: `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/di/AppContainer.kt`
  Purpose: Wire unified reader content, detail screen state, cover repository, and refresh use cases.

- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderUiState.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModel.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderModePicker.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderSettingsSheet.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderTocSheet.kt`
- Create: `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt`
- Modify: `feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModelTest.kt`
  Purpose: Support simulated page turn, horizontal paging, vertical scroll, TOC jump/highlight, typography controls, theme/brightness controls, and progress restore.

- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailUiState.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailViewModel.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookCoverActionSheet.kt`
- Create: `feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/BookDetailViewModelTest.kt`
- Create: `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/BookDetailScreenTest.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailScreen.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/BookAssetDao.kt`
  Purpose: Create one shared detail screen for local and web books, and hang cover operations off it.

- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ContinueReadingCard.kt`
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt`
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookActionsMenu.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryUiState.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ObserveBookshelfUseCase.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryViewModel.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt`
- Create: `feature/library/src/testDebug/java/com/wenwentome/reader/feature/library/LibraryScreenTest.kt`
- Modify: `feature/library/src/test/kotlin/com/wenwentome/reader/feature/library/LibraryViewModelTest.kt`
  Purpose: Turn the library into a real bookshelf with rectangular book covers, continue-reading, update badges, and a long-press entry for cover/catalog actions.

- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverUiState.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModel.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverScreen.kt`
- Modify: `feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModelTest.kt`
  Purpose: Move from “search + add” into “search + preview + refresh + read latest”.

- Modify: `app/src/testDebug/java/com/wenwentome/reader/AppNavigationSmokeTest.kt`
- Modify: `app/src/testDebug/java/com/wenwentome/reader/AppSmokeFlowTest.kt`
- Create: `app/src/testDebug/java/com/wenwentome/reader/AppReaderFlowTest.kt`
- Modify: `docs/development-progress.md`
  Purpose: Add end-to-end smoke coverage and keep the development-progress doc aligned with the implementation stage.

## Task 1: Define Unified Reader Anchors, Reader Preferences, And Safe Database Upgrade

**Files:**
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReaderChapter.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReaderMode.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReaderPresentationPrefs.kt`
- Modify: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReadingState.kt`
- Modify: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/RemoteBinding.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ReadingStateEntity.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/RemoteBindingEntity.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/DatabaseMappers.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/ReaderDatabase.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseMigrations.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/datastore/ReaderPreferencesStore.kt`
- Test: `core/database/src/test/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseTest.kt`
- Test: `core/database/src/test/kotlin/com/wenwentome/reader/core/database/datastore/ReaderPreferencesStoreTest.kt`
- Create: `core/database/src/androidTest/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseMigrationTest.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/di/AppContainer.kt`

- [ ] **Step 1: Write failing persistence and migration tests for reader mode, presentation prefs, and remote refresh metadata**

```kotlin
@Test
fun readerPreferences_roundTripModeAndPresentationPrefs() = runTest {
    val store = ReaderPreferencesStore(context)
    store.saveReaderMode(ReaderMode.SIMULATED_PAGE_TURN)
    store.savePresentationPrefs(
        ReaderPresentationPrefs(
            theme = ReaderTheme.SEPIA,
            fontSizeSp = 20,
            lineHeightMultiplier = 1.7f,
            brightnessPercent = 72,
        )
    )
    assertEquals(ReaderMode.SIMULATED_PAGE_TURN, store.readerMode.first())
    assertEquals(ReaderTheme.SEPIA, store.presentationPrefs.first().theme)
}

@Test
fun remoteBinding_roundTripsLatestChapterMetadata() = runTest {
    dao.upsert(
        RemoteBindingEntity(
            bookId = "book-1",
            sourceId = "src",
            remoteBookId = "remote",
            remoteBookUrl = "https://example.com/book",
            latestKnownChapterRef = "chapter-9",
            lastCatalogRefreshAt = 123L,
        )
    )
    assertEquals("chapter-9", dao.observeByBookId("book-1").first()?.latestKnownChapterRef)
}

@Test
fun migrate2To3_preservesExistingShelfData() {
    helper.createDatabase(TEST_DB, 2).apply {
        execSQL(
            "INSERT INTO remote_bindings (bookId, sourceId, remoteBookId, remoteBookUrl, tocRef, syncMode, lastFetchedAt) " +
                "VALUES ('book-1', 'src-1', 'remote-1', 'https://example.com/book/1', NULL, 'LATEST_ONLY', 11)"
        )
        close()
    }

    helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)
}
```

- [ ] **Step 2: Run the focused tests and confirm they fail for the expected missing fields, prefs, and migration registration**

Run:

```bash
./gradlew :core:database:testDebugUnitTest --tests "com.wenwentome.reader.core.database.ReaderDatabaseTest" --tests "com.wenwentome.reader.core.database.datastore.ReaderPreferencesStoreTest"
./gradlew :core:database:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.wenwentome.reader.core.database.ReaderDatabaseMigrationTest
```

Expected: compile or assertion failures mentioning missing `ReaderPresentationPrefs`, `latestKnownChapterRef`, `savePresentationPrefs`, or unregistered `MIGRATION_2_3`.

- [ ] **Step 3: Add the minimal shared types, preference fields, and explicit Room migration**

```kotlin
enum class ReaderMode { SIMULATED_PAGE_TURN, HORIZONTAL_PAGING, VERTICAL_SCROLL }

enum class ReaderTheme { PAPER, SEPIA, NIGHT }

data class ReaderPresentationPrefs(
    val theme: ReaderTheme = ReaderTheme.PAPER,
    val fontSizeSp: Int = 18,
    val lineHeightMultiplier: Float = 1.55f,
    val brightnessPercent: Int = 0,
)

data class ReaderChapter(
    val chapterRef: String,
    val title: String,
    val orderIndex: Int,
    val sourceType: BookFormat,
    val locatorHint: String? = null,
    val isLatest: Boolean = false,
)
```

Remote update metadata lives in `RemoteBindingEntity`:

```kotlin
val latestKnownChapterRef: String? = null
val lastCatalogRefreshAt: Long? = null
```

Add a non-destructive migration for the known release upgrade path:

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE remote_bindings ADD COLUMN latestKnownChapterRef TEXT"
        )
        database.execSQL(
            "ALTER TABLE remote_bindings ADD COLUMN lastCatalogRefreshAt INTEGER"
        )
    }
}
```

`ReaderDatabase` should move to `version = 3`, and `AppContainer` should register `addMigrations(MIGRATION_2_3)` before any fallback policy so upgrading from `1.0` to `1.1` does not wipe the shelf.

- [ ] **Step 4: Re-run the unit tests and confirm the new prefs / metadata round trips pass**

Run:

```bash
./gradlew :core:database:testDebugUnitTest --tests "com.wenwentome.reader.core.database.ReaderDatabaseTest" --tests "com.wenwentome.reader.core.database.datastore.ReaderPreferencesStoreTest"
```

Expected: PASS for the new round-trip coverage.

- [ ] **Step 5: Run the migration test and confirm the `2 -> 3` upgrade keeps existing records**

Run:

```bash
./gradlew :core:database:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.wenwentome.reader.core.database.ReaderDatabaseMigrationTest
```

Expected: PASS, and the old `remote_bindings` rows survive upgrade with new columns defaulted safely.

- [ ] **Step 6: Commit**

```bash
git add core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReaderChapter.kt core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReaderMode.kt core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReaderPresentationPrefs.kt core/model/src/main/kotlin/com/wenwentome/reader/core/model/ReadingState.kt core/model/src/main/kotlin/com/wenwentome/reader/core/model/RemoteBinding.kt core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ReadingStateEntity.kt core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/RemoteBindingEntity.kt core/database/src/main/kotlin/com/wenwentome/reader/core/database/DatabaseMappers.kt core/database/src/main/kotlin/com/wenwentome/reader/core/database/ReaderDatabase.kt core/database/src/main/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseMigrations.kt core/database/src/main/kotlin/com/wenwentome/reader/core/database/datastore/ReaderPreferencesStore.kt core/database/src/test/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseTest.kt core/database/src/test/kotlin/com/wenwentome/reader/core/database/datastore/ReaderPreferencesStoreTest.kt core/database/src/androidTest/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseMigrationTest.kt app/src/main/java/com/wenwentome/reader/di/AppContainer.kt
git commit -m "feat: add reader prefs and safe db upgrade path"
```

## Task 2: Fix EPUB TOC, Cover Separation, And First Readable Chapter Resolution

**Files:**
- Create: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/EpubCatalogParser.kt`
- Create: `data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepositoryTest.kt`
- Create: `data/localbooks/src/test/resources/fixtures/sample-cover-first.epub`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookModels.kt`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/EpubBookParser.kt`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepository.kt`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepository.kt`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookFileStore.kt`
- Test: `data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepositoryTest.kt`

- [ ] **Step 1: Write failing EPUB regression tests for “Cover” and TOC-backed chapter selection**

```kotlin
@Test
fun load_epubSkipsCoverAndOpensFirstReadableChapter() = runTest {
    val content = repository.load(bookId = "epub-book", locator = null)
    assertEquals("第一章", content.chapterTitle)
    assertFalse(content.paragraphs.first().contains("Cover"))
}

@Test
fun import_epubPersistsCoverAssetSeparatelyFromPrimaryText() = runTest {
    val result = repository.import("sample-cover-first.epub", fixture("sample-cover-first.epub"))
    assertTrue(result.assets.any { it.assetRole == AssetRole.COVER })
    assertTrue(result.assets.any { it.assetRole == AssetRole.PRIMARY_TEXT })
}

@Test
fun load_epubRestoresLegacySpineLocatorFrom10WithoutLosingProgress() = runTest {
    val content = repository.load(bookId = "epub-book", locator = "0:3")
    assertEquals("第一章", content.chapterTitle)
    assertFalse(content.paragraphs.first().contains("Cover"))
}
```

- [ ] **Step 2: Run EPUB-focused tests to verify they fail**

Run:

```bash
./gradlew :data:localbooks:testDebugUnitTest --tests "com.wenwentome.reader.data.localbooks.LocalBookContentRepositoryTest" --tests "com.wenwentome.reader.data.localbooks.LocalBookImportRepositoryTest"
```

Expected: FAIL because the current repository resolves `spine[0]` and has no explicit cover asset handling.

- [ ] **Step 3: Implement TOC-first catalog parsing and separate cover extraction**

```kotlin
class EpubCatalogParser {
    fun firstReadableChapter(book: nl.siegmann.epublib.domain.Book): ReaderChapter =
        catalog(book).first()

    fun catalog(book: nl.siegmann.epublib.domain.Book): List<ReaderChapter> =
        resolveReadableResources(book).mapIndexed { index, resource ->
            ReaderChapter(
                chapterRef = resource.href,
                title = resource.title ?: "章节 ${index + 1}",
                orderIndex = index,
                sourceType = BookFormat.EPUB,
                locatorHint = "chapter:${resource.href}#paragraph:0",
            )
        }
}
```

And keep cover separate:

```kotlin
val coverResource = epubBook.coverImage
val coverAsset = coverResource?.let {
    ParsedAsset(
        assetRole = AssetRole.COVER,
        bytes = it.data,
        mime = it.mediaType?.name ?: "image/jpeg",
        extension = "jpg",
    )
}
```

`LocalBookContentRepository` should accept logical locators like:

```kotlin
"chapter:<chapterRef>#paragraph:<index>"
```

instead of raw `spineIndex:paragraphIndex`.

For upgrade safety, it must also accept the legacy `1.0` EPUB locator format:

```kotlin
"<legacySpineIndex>:<paragraphIndex>"
```

When a legacy locator is encountered, map the old `spineIndex` to the filtered readable chapter list before resolving the paragraph offset. This avoids losing existing EPUB reading progress after the `1.0 -> 1.1` upgrade even if the stored locator is never rewritten.

- [ ] **Step 4: Re-run the focused EPUB tests and confirm they pass**

Run:

```bash
./gradlew :data:localbooks:testDebugUnitTest --tests "com.wenwentome.reader.data.localbooks.LocalBookContentRepositoryTest" --tests "com.wenwentome.reader.data.localbooks.LocalBookImportRepositoryTest"
```

Expected: PASS, and the first chapter resolves to readable正文.

- [ ] **Step 5: Commit**

```bash
git add data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/EpubCatalogParser.kt data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookModels.kt data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/EpubBookParser.kt data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepository.kt data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepository.kt data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookFileStore.kt data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepositoryTest.kt data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookImportRepositoryTest.kt data/localbooks/src/test/resources/fixtures/sample-cover-first.epub
git commit -m "fix: resolve epub content from first readable chapter"
```

## Task 3: Add Web-Novel Refresh Metadata And Latest-Chapter Detection

**Files:**
- Modify: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/model/RemoteBookModels.kt`
- Modify: `bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/SourceBridgeRepository.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/RefreshRemoteBookUseCase.kt`
- Create: `feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/RefreshRemoteBookUseCaseTest.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/AddRemoteBookToShelfUseCase.kt`
- Modify: `feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/AddRemoteBookToShelfUseCaseTest.kt`

- [ ] **Step 1: Write failing tests for latest chapter persistence and manual refresh**

```kotlin
@Test
fun addRemoteBookToShelfStoresLatestKnownChapterRef() = runTest {
    useCase(sampleSearchResult())
    assertEquals("chapter-12", bindingDao.observeByBookId(createdBookId).first()?.latestKnownChapterRef)
}

@Test
fun refreshRemoteBookMarksHasUpdatesWhenLatestChapterAdvanced() = runTest {
    val result = refreshRemoteBook(bookId = "book-1")
    assertTrue(result.hasUpdates)
}
```

- [ ] **Step 2: Run discover-focused tests and confirm they fail**

Run:

```bash
./gradlew :feature:discover:testDebugUnitTest --tests "com.wenwentome.reader.feature.discover.AddRemoteBookToShelfUseCaseTest" --tests "com.wenwentome.reader.feature.discover.RefreshRemoteBookUseCaseTest"
```

Expected: FAIL because `RemoteBinding` has no latest-chapter refresh metadata and no refresh use case exists.

- [ ] **Step 3: Implement manual-refresh-only update semantics**

```kotlin
data class RefreshRemoteBookResult(
    val latestKnownChapterRef: String?,
    val hasUpdates: Boolean,
)
```

Manual refresh updates binding metadata:

```kotlin
remoteBindingDao.upsert(
    binding.copy(
        latestKnownChapterRef = latest.chapterRef,
        lastCatalogRefreshAt = clock.now(),
    ).toEntity()
)
```

Derive `hasUpdates` by comparing `latestKnownChapterRef` with `ReadingState.chapterRef`.

- [ ] **Step 4: Re-run discover tests and confirm they pass**

Run:

```bash
./gradlew :feature:discover:testDebugUnitTest --tests "com.wenwentome.reader.feature.discover.AddRemoteBookToShelfUseCaseTest" --tests "com.wenwentome.reader.feature.discover.RefreshRemoteBookUseCaseTest"
```

Expected: PASS with deterministic latest-chapter metadata behavior.

- [ ] **Step 5: Commit**

```bash
git add bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/model/RemoteBookModels.kt bridge/source/src/main/kotlin/com/wenwentome/reader/bridge/source/SourceBridgeRepository.kt feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/RefreshRemoteBookUseCase.kt feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/RefreshRemoteBookUseCaseTest.kt feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/AddRemoteBookToShelfUseCase.kt feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/AddRemoteBookToShelfUseCaseTest.kt
git commit -m "feat: persist remote chapter refresh metadata"
```

## Task 4: Wire Unified Local/Web Reader Content Flows

**Files:**
- Create: `app/src/main/java/com/wenwentome/reader/navigation/ReaderContentFlows.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/di/AppContainer.kt`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookModels.kt`
- Test: `app/src/testDebug/java/com/wenwentome/reader/AppSmokeFlowTest.kt`

- [ ] **Step 1: Write a failing smoke test that opens a web-origin book in the reader route**

```kotlin
@Test
fun readerFlow_opensWebBookContentInsteadOfSummaryPlaceholder() {
    composeTestRule.onNodeWithText("开始阅读").performClick()
    composeTestRule.onNodeWithText("网文正文桥接将在后续任务接入。").assertDoesNotExist()
}
```

- [ ] **Step 2: Run the smoke test to verify it fails on the current placeholder**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.wenwentome.reader.AppSmokeFlowTest"
```

Expected: FAIL because `OriginType.WEB` still emits the placeholder summary text.

- [ ] **Step 3: Move reader content selection into a dedicated flow helper**

```kotlin
private suspend fun loadRemoteReaderContent(
    book: BookRecord,
    bookId: String,
    readingState: ReadingState?,
    appContainer: AppContainer,
): ReaderContent {
    val binding = appContainer.database.remoteBindingDao()
        .observeByBookId(bookId)
        .first()
        ?.toModel()
        ?: return ReaderContent(
            chapterTitle = book.title,
            paragraphs = listOf("未找到网文绑定信息"),
        )

    val chapterRef = readingState?.chapterRef
        ?: binding.latestKnownChapterRef
        ?: appContainer.sourceBridgeRepository
            .fetchToc(binding.sourceId, binding.remoteBookId)
            .firstOrNull()
            ?.chapterRef
        ?: return ReaderContent(
            chapterTitle = book.title,
            paragraphs = listOf("目录为空"),
        )

    val chapter = appContainer.sourceBridgeRepository.fetchChapterContent(
        sourceId = binding.sourceId,
        chapterRef = chapterRef,
    )

    return ReaderContent(
        chapterTitle = chapter.title,
        paragraphs = chapter.content
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList(),
    )
}

fun readerContentFlow(
    book: BookRecord?,
    bookId: String,
    appContainer: AppContainer,
    observeReadingState: Flow<ReadingState?>,
): Flow<ReaderContent> =
    when (book?.originType) {
        OriginType.LOCAL -> observeReadingState.flatMapLatest { state ->
            flow { emit(appContainer.localBookContentRepository.load(bookId, state?.locator)) }
        }
        OriginType.WEB, OriginType.MIXED -> observeReadingState.flatMapLatest { state ->
            flow {
                emit(
                    loadRemoteReaderContent(
                        book = requireNotNull(book),
                        bookId = bookId,
                        readingState = state,
                        appContainer = appContainer,
                    )
                )
            }
        }
        else -> flowOf(ReaderContent(chapterTitle = "", paragraphs = emptyList()))
    }
```

`AppContainer` should continue exposing `sourceBridgeRepository`, `database.remoteBindingDao()`, and `localBookContentRepository`, so `AppNavHost` only wires flows and does not re-embed parsing logic.

- [ ] **Step 4: Re-run the smoke test and any touched reader tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.wenwentome.reader.AppSmokeFlowTest" :feature:reader:testDebugUnitTest --tests "com.wenwentome.reader.feature.reader.ReaderViewModelTest"
```

Expected: PASS, and no placeholder text remains on the web-reader path.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/wenwentome/reader/navigation/ReaderContentFlows.kt app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt app/src/main/java/com/wenwentome/reader/di/AppContainer.kt data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookModels.kt app/src/testDebug/java/com/wenwentome/reader/AppSmokeFlowTest.kt
git commit -m "feat: unify local and web reader content flows"
```

## Task 5: Upgrade Reader UI For Modes, TOC, And Stable Progress Restore

**Files:**
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderUiState.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModel.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderModePicker.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderSettingsSheet.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderTocSheet.kt`
- Create: `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt`
- Modify: `feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModelTest.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`

- [ ] **Step 1: Write failing tests for mode switching, reader settings, and TOC highlighter behavior**

```kotlin
@Test
fun updateReaderMode_persistsSelectedModeWithoutResettingChapter() = runTest {
    viewModel.setReaderMode(ReaderMode.HORIZONTAL_PAGING)
    assertEquals("chapter-3", viewModel.uiState.value.chapterRef)
    assertEquals(ReaderMode.HORIZONTAL_PAGING, viewModel.uiState.value.readerMode)
}

@Test
fun updatePresentationPrefs_persistsThemeFontAndLineHeight() = runTest {
    viewModel.updatePresentation(
        ReaderPresentationPrefs(
            theme = ReaderTheme.NIGHT,
            fontSizeSp = 22,
            lineHeightMultiplier = 1.8f,
            brightnessPercent = 60,
        )
    )
    assertEquals(ReaderTheme.NIGHT, viewModel.uiState.value.presentation.theme)
    assertEquals(22, viewModel.uiState.value.presentation.fontSizeSp)
}

@Test
fun jumpToChapter_updatesLocatorUsingChapterLocatorHint() = runTest {
    viewModel.jumpToChapter(chapterRef = "chapter-8")
    assertEquals("chapter:chapter-8#paragraph:0", persistedState?.locator)
}

@Test
fun tocSheet_highlightsCurrentChapter_andLatestChapter_withoutResettingLocatorUntilSelect() {
    composeTestRule.onNodeWithText("目录").performClick()
    composeTestRule.onNodeWithTag("toc-current-chapter").assertExists()
    composeTestRule.onNodeWithTag("toc-latest-chapter").assertExists()
    composeTestRule.onNodeWithTag("reader-progress-label").assertTextContains("42%")
}
```

- [ ] **Step 2: Run reader tests to verify they fail**

Run:

```bash
./gradlew :feature:reader:testDebugUnitTest --tests "com.wenwentome.reader.feature.reader.ReaderViewModelTest" :feature:reader:testDebugUnitTest --tests "com.wenwentome.reader.feature.reader.ReaderScreenTest"
```

Expected: FAIL because reader mode persistence, reader settings, and TOC highlight/latest behavior do not exist yet.

- [ ] **Step 3: Implement the minimal reader state, menu, TOC semantics, and settings chrome**

```kotlin
data class ReaderUiState(
    val readerMode: ReaderMode = ReaderMode.SIMULATED_PAGE_TURN,
    val presentation: ReaderPresentationPrefs = ReaderPresentationPrefs(),
    val chapters: List<ReaderChapter> = emptyList(),
    val chapterRef: String? = null,
    val latestChapterRef: String? = null,
    val tocHighlightedChapterRef: String? = null,
    val locator: String? = null,
    val progressPercent: Float = 0f,
    val progressLabel: String = "0%",
    val chapterTitle: String? = null,
    val paragraphs: List<String> = emptyList(),
)
```

`ReaderScreen` should:

```kotlin
when (state.readerMode) {
    ReaderMode.SIMULATED_PAGE_TURN ->
        SimulatedPageTurnBody(state = state, onLocatorChanged = onLocatorChanged)
    ReaderMode.HORIZONTAL_PAGING ->
        HorizontalPagerBody(state = state, onLocatorChanged = onLocatorChanged)
    ReaderMode.VERTICAL_SCROLL ->
        VerticalScrollBody(state = state, onLocatorChanged = onLocatorChanged)
}
```

All three branches must call the same logical `onLocatorChanged`.

`ReaderTocSheet` must receive both current and latest anchors so it can highlight the current chapter and scroll to the best starting target when opened:

```kotlin
ReaderTocSheet(
    chapters = state.chapters,
    currentChapterRef = state.chapterRef,
    latestChapterRef = state.latestChapterRef,
    initialScrollChapterRef = state.chapterRef ?: state.latestChapterRef,
    onChapterClick = onChapterSelected,
)
```

Closing the TOC must not mutate `locator`; only tapping a chapter is allowed to change reading position.

`ReaderSettingsSheet` must expose the minimum spec-required controls:

```kotlin
ReaderSettingsSheet(
    presentation = state.presentation,
    progressLabel = state.progressLabel,
    onThemeChange = onThemeChange,
    onFontSizeChange = onFontSizeChange,
    onLineHeightChange = onLineHeightChange,
    onBrightnessChange = onBrightnessChange,
)
```

Theme / font size / line height persist through `ReaderPreferencesStore`; brightness may be applied as a reader-only session overlay or window brightness value, but it still needs a visible slider and test coverage.

- [ ] **Step 4: Re-run focused reader tests and confirm they pass**

Run:

```bash
./gradlew :feature:reader:testDebugUnitTest --tests "com.wenwentome.reader.feature.reader.ReaderViewModelTest" :feature:reader:testDebugUnitTest --tests "com.wenwentome.reader.feature.reader.ReaderScreenTest"
```

Expected: PASS, with stable TOC jump semantics, visible current/latest highlights, persistent typography/theme settings, and unchanged locator when the TOC closes without selection.

- [ ] **Step 5: Commit**

```bash
git add feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderUiState.kt feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModel.kt feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderModePicker.kt feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderSettingsSheet.kt feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderTocSheet.kt feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModelTest.kt app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt
git commit -m "feat: add reader modes toc and reading settings"
```

## Task 6: Build The Unified Book Detail Screen And Cover Actions

**Files:**
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailUiState.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailViewModel.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookCoverActionSheet.kt`
- Create: `feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/BookDetailViewModelTest.kt`
- Create: `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/BookDetailScreenTest.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailScreen.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/BookAssetDao.kt`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookFileStore.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/di/AppContainer.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`

- [ ] **Step 1: Write failing tests for cover override actions, picker entry, and unified detail state**

```kotlin
@Test
fun importCover_setsManualCoverAssetAndExposesRestoreAction() = runTest {
    viewModel.importCover(sampleImageBytes)
    assertEquals(true, viewModel.uiState.value.canRestoreAutomaticCover)
}

@Test
fun webBookDetail_showsRefreshAndJumpToLatestActions() = runTest {
    assertTrue(viewModel.uiState.value.showRefreshCatalogAction)
    assertTrue(viewModel.uiState.value.showJumpToLatestAction)
}

@Test
fun jumpToLatest_persistsLatestChapterLocatorBeforeOpeningReader() = runTest {
    viewModel.jumpToLatest()
    assertEquals("chapter-12", persistedState?.chapterRef)
    assertEquals("chapter:chapter-12#paragraph:0", persistedState?.locator)
}

@Test
fun jumpToLatest_withoutLatestRef_fallsBackToCurrentChapterWithoutCrash() = runTest {
    givenLatestChapterRef(null)
    givenCurrentChapterRef("chapter-5")
    viewModel.jumpToLatest()
    assertEquals("chapter-5", persistedState?.chapterRef)
}

@Test
fun detailUiState_exposesReadTocAndProgressSummary() = runTest {
    assertEquals("继续阅读", viewModel.uiState.value.readActionLabel)
    assertTrue(viewModel.uiState.value.showTocAction)
    assertEquals("42%", viewModel.uiState.value.progressLabel)
}

@Test
fun detailScreen_clickImportPhoto_requestsImagePicker() {
    composeTestRule.onNodeWithText("导入照片").performClick()
    composeTestRule.onNodeWithTag("book-cover-picker-requested").assertExists()
}

@Test
fun detailScreen_showsReadContinueTocAndProgress() {
    composeTestRule.onNodeWithText("继续阅读").assertExists()
    composeTestRule.onNodeWithText("查看目录").assertExists()
    composeTestRule.onNodeWithTag("detail-progress-label").assertExists()
}
```

- [ ] **Step 2: Run detail-viewmodel and detail-screen tests and verify they fail**

Run:

```bash
./gradlew :feature:reader:testDebugUnitTest --tests "com.wenwentome.reader.feature.reader.BookDetailViewModelTest"
./gradlew :feature:reader:testDebugUnitTest --tests "com.wenwentome.reader.feature.reader.BookDetailScreenTest"
```

Expected: FAIL because there is no detail view model, image-picker entry, progress summary, or read / TOC action path yet.

- [ ] **Step 3: Implement one shared detail view model, shared picker-driven cover actions, and prefer `AssetRole.COVER` over auto cover**

```kotlin
val effectiveCover = manualCoverAsset?.storageUri ?: book.cover
```

And expose actions:

```kotlin
fun refreshCover()
fun importCover(bytes: ByteArray, mime: String)
fun restoreAutomaticCover()
```

Persist manual images via `LocalBookFileStore` under the existing book directory.

`BookDetailUiState` must cover the full minimum detail-page loop from the spec, not only cover management:

```kotlin
data class BookDetailUiState(
    val readActionLabel: String = "开始阅读",
    val progressLabel: String = "0%",
    val lastReadLabel: String? = null,
    val showTocAction: Boolean = true,
    val showRefreshCatalogAction: Boolean = false,
    val showJumpToLatestAction: Boolean = false, // true only when latestChapterRef != null
)
```

`BookDetailScreen` must visibly render:

- `开始阅读 / 继续阅读`
- `查看目录`
- 当前阅读进度与最近阅读信息
- 网文的更新提示、刷新目录、跳转最新章
- 封面四态动作入口

`jumpToLatest()` must be behavior-complete, not just visible: when the user taps it, open the reader using `RemoteBinding.latestKnownChapterRef`, and fall back to the current reading chapter only if the latest ref is unavailable.

```kotlin
suspend fun jumpToLatest() {
    val latestRef = uiState.value.latestChapterRef ?: currentState.chapterRef ?: return
    updateReadingState(
        currentState.copy(
            chapterRef = latestRef,
            locator = "chapter:$latestRef#paragraph:0",
        )
    )
    navigator.openReader(bookId)
}
```

`AppNavHost` should own the shared image picker callback so the same import flow can be reused by both the detail screen and the bookshelf long-press menu from Task 7:

```kotlin
val pickCoverLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.PickVisualMedia()
) { uri ->
    uri ?: return@rememberLauncherForActivityResult
    coverScope.launch { viewModel.importCover(readBytes(uri), resolveMime(uri)) }
}
```

`BookDetailScreen` should expose explicit UI actions:

```kotlin
BookCoverActionSheet(
    onRefreshCover = viewModel::refreshCover,
    onImportPhoto = {
        pickCoverLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    },
    onRestoreAutomaticCover = viewModel::restoreAutomaticCover,
)
```

`refreshCover()` semantics must be type-aware:

- `EPUB`: re-parse the embedded cover resource and overwrite the automatic cover asset
- `WEB`: re-fetch detail metadata and overwrite the automatic cover asset
- `TXT`: re-run local metadata/default cover resolution without creating a fake network dependency

- [ ] **Step 4: Re-run detail tests and the touched DAO tests**

Run:

```bash
./gradlew :feature:reader:testDebugUnitTest --tests "com.wenwentome.reader.feature.reader.BookDetailViewModelTest" --tests "com.wenwentome.reader.feature.reader.BookDetailScreenTest" :core:database:testDebugUnitTest --tests "com.wenwentome.reader.core.database.ReaderDatabaseTest"
```

Expected: PASS, and the detail page exposes read / TOC / progress actions, `跳转最新章` really opens the latest chapter target while `latestChapterRef` 为空时也不会崩溃, and cover override remains reachable from a real picker entry.

- [ ] **Step 5: Commit**

```bash
git add feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailUiState.kt feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailViewModel.kt feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookCoverActionSheet.kt feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/BookDetailViewModelTest.kt feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/BookDetailScreenTest.kt feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailScreen.kt core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/BookAssetDao.kt data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookFileStore.kt app/src/main/java/com/wenwentome/reader/di/AppContainer.kt app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt
git commit -m "feat: add unified book detail and cover actions"
```

## Task 7: Redesign The Bookshelf As A Real Bookshelf

**Files:**
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ContinueReadingCard.kt`
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt`
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookActionsMenu.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryUiState.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ObserveBookshelfUseCase.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryViewModel.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt`
- Create: `feature/library/src/testDebug/java/com/wenwentome/reader/feature/library/LibraryScreenTest.kt`
- Modify: `feature/library/src/test/kotlin/com/wenwentome/reader/feature/library/LibraryViewModelTest.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`

- [ ] **Step 1: Write failing tests for continue-reading state and rectangular cover rendering**

```kotlin
@Test
fun libraryUiState_exposesContinueReadingBookAndUpdateBadge() = runTest {
    assertEquals("book-2", state.continueReading?.book.id)
    assertTrue(state.visibleBooks.first { it.book.id == "web-1" }.hasUpdates)
}

@Test
fun refreshCatalog_updatesHasUpdatesBadgeAfterLatestChapterChanges() = runTest {
    viewModel.refreshCatalog("web-1")
    assertFalse(viewModel.uiState.value.visibleBooks.first { it.book.id == "web-1" }.hasUpdates)
}
```

And UI smoke:

```kotlin
composeTestRule.onNodeWithTag("continue-reading-card").assertExists()
composeTestRule.onNodeWithTag("book-cover-card-book-1").assertExists()
composeTestRule.onNodeWithTag("book-cover-card-book-1").performTouchInput { longClick() }
composeTestRule.onNodeWithText("导入照片").assertExists()
composeTestRule.onNodeWithText("恢复自动封面").assertExists()
```

- [ ] **Step 2: Run library tests and verify they fail**

Run:

```bash
./gradlew :feature:library:testDebugUnitTest --tests "com.wenwentome.reader.feature.library.LibraryViewModelTest" :feature:library:testDebugUnitTest --tests "com.wenwentome.reader.feature.library.LibraryScreenTest"
```

Expected: FAIL because the current library state is just a plain list of `BookRecord` and has no long-press action entry.

- [ ] **Step 3: Implement bookshelf cards backed by reading state, remote update metadata, and long-press quick actions**

```kotlin
data class LibraryBookItem(
    val book: BookRecord,
    val effectiveCover: String?,
    val progressPercent: Float,
    val progressLabel: String,
    val hasUpdates: Boolean,
)
```

`LibraryScreen` should render:

```kotlin
state.continueReading?.let { item ->
    ContinueReadingCard(
        item = item,
        onClick = { onBookClick(item.book.id) },
        modifier = Modifier.testTag("continue-reading-card"),
    )
}

LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 132.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
) {
    items(
        items = state.visibleBooks,
        key = { it.book.id },
    ) { item ->
        BookCoverCard(
            item = item,
            onClick = { onBookClick(item.book.id) },
            onLongClick = { onOpenBookActions(item.book.id) },
            modifier = Modifier.testTag("book-cover-card-${item.book.id}"),
        )
    }
}
```

The long-press menu is the second spec-required entry point for cover management:

```kotlin
BookActionsMenu(
    onOpenDetail = { onBookClick(item.book.id) },
    onImportPhoto = { onImportPhoto(item.book.id) },
    onRefreshCover = { onRefreshCover(item.book.id) },
    onRestoreAutomaticCover = { onRestoreAutomaticCover(item.book.id) },
    onRefreshCatalog = item.takeIf { it.book.originType != OriginType.LOCAL }?.let {
        { onRefreshCatalog(item.book.id) }
    },
)
```

`onRefreshCatalog(bookId)` must be wired to the real refresh use case, not a UI stub:

```kotlin
val refreshRemoteBookUseCase = RefreshRemoteBookUseCase(
    sourceBridgeRepository = appContainer.sourceBridgeRepository,
    remoteBindingDao = appContainer.database.remoteBindingDao(),
    readingStateDao = appContainer.database.readingStateDao(),
)

LibraryViewModel(
    observeBookshelf = observeBookshelf,
    refreshCatalog = refreshRemoteBookUseCase::invoke,
)
```

After refresh completes, `LibraryUiState.visibleBooks[*].hasUpdates` must be recalculated from the updated `RemoteBinding.latestKnownChapterRef` plus `ReadingState.chapterRef`, so the badge/continue-reading card changes immediately without requiring app restart.

`ObserveBookshelfUseCase` should therefore join all three flows:

```kotlin
combine(
    bookRecordDao.observeAll(),
    readingStateDao.observeAll(),
    remoteBindingDao.observeAll(),
) { books, readingStates, bindings ->
    books.map { book ->
        val state = readingStates.firstOrNull { it.bookId == book.id }
        val binding = bindings.firstOrNull { it.bookId == book.id }
        val hasUpdates = binding?.latestKnownChapterRef != null &&
            binding.latestKnownChapterRef != state?.chapterRef
        LibraryBookItem(book = book.toModel(), hasUpdates = hasUpdates, ...)
    }
}
```

- [ ] **Step 4: Re-run library tests and confirm they pass**

Run:

```bash
./gradlew :feature:library:testDebugUnitTest --tests "com.wenwentome.reader.feature.library.LibraryViewModelTest" :feature:library:testDebugUnitTest --tests "com.wenwentome.reader.feature.library.LibraryScreenTest"
```

Expected: PASS, with rectangular cover cards, a visible continue-reading entry, and a reachable long-press actions menu.

- [ ] **Step 5: Commit**

```bash
git add feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ContinueReadingCard.kt feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookActionsMenu.kt feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryUiState.kt feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/ObserveBookshelfUseCase.kt feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryViewModel.kt feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt feature/library/src/testDebug/java/com/wenwentome/reader/feature/library/LibraryScreenTest.kt feature/library/src/test/kotlin/com/wenwentome/reader/feature/library/LibraryViewModelTest.kt app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt
git commit -m "feat: redesign bookshelf with quick actions"
```

## Task 8: Complete Discover Flow, App Smokes, And Progress Tracking Docs

**Files:**
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverUiState.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModel.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverScreen.kt`
- Modify: `feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModelTest.kt`
- Modify: `app/src/testDebug/java/com/wenwentome/reader/AppNavigationSmokeTest.kt`
- Modify: `app/src/testDebug/java/com/wenwentome/reader/AppSmokeFlowTest.kt`
- Create: `app/src/testDebug/java/com/wenwentome/reader/AppReaderFlowTest.kt`
- Modify: `docs/development-progress.md`

- [ ] **Step 1: Write failing tests for result preview, refresh, and latest-chapter affordances**

```kotlin
@Test
fun discoverViewModel_selectResultLoadsPreviewState() = runTest {
    viewModel.selectResult("remote-1")
    assertEquals("remote-1", viewModel.uiState.value.selectedResultId)
}

@Test
fun appReaderFlow_canOpenBookDetailThenReaderThenToc() {
    composeTestRule.onNodeWithText("目录").assertExists()
}

@Test
fun appReaderFlow_jumpToLatest_opensReaderAtLatestChapter() {
    composeTestRule.onNodeWithText("跳转最新章").performClick()
    composeTestRule.onNodeWithTag("reader-chapter-title").assertTextContains("最新章")
}
```

- [ ] **Step 2: Run discover/app smoke tests and verify they fail**

Run:

```bash
./gradlew :feature:discover:testDebugUnitTest --tests "com.wenwentome.reader.feature.discover.DiscoverViewModelTest" :app:testDebugUnitTest --tests "com.wenwentome.reader.AppNavigationSmokeTest" --tests "com.wenwentome.reader.AppSmokeFlowTest" --tests "com.wenwentome.reader.AppReaderFlowTest"
```

Expected: FAIL because discover results do not have preview/refresh/latest actions and app smokes do not cover the new flows yet.

- [ ] **Step 3: Implement preview-first discover actions and update the progress doc**

```kotlin
data class DiscoverUiState(
    val selectedResultId: String? = null,
    val selectedPreview: RemoteBookDetail? = null,
    val refreshingResultIds: Set<String> = emptySet(),
    val results: List<RemoteSearchResult> = emptyList(),
    val query: String = "",
)
```

UI actions should include:

```kotlin
onPreview(resultId)
onRefreshSelected()
onReadLatest()
```

`AppReaderFlowTest` must cover at least one end-to-end latest-chapter flow:

```kotlin
详情页 -> 点击“跳转最新章” -> ReaderRoute 打开 -> 标题/正文来自 latestKnownChapterRef
```

Update `docs/development-progress.md` with the active `1.1` execution stage and the remaining verification work.

- [ ] **Step 4: Run the full verification slice for touched modules**

Run:

```bash
./gradlew :core:database:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.wenwentome.reader.core.database.ReaderDatabaseMigrationTest
./gradlew :feature:discover:testDebugUnitTest --tests "com.wenwentome.reader.feature.discover.DiscoverViewModelTest" :feature:reader:testDebugUnitTest --tests "com.wenwentome.reader.feature.reader.ReaderViewModelTest" :feature:library:testDebugUnitTest --tests "com.wenwentome.reader.feature.library.LibraryViewModelTest" :app:testDebugUnitTest --tests "com.wenwentome.reader.AppNavigationSmokeTest" --tests "com.wenwentome.reader.AppSmokeFlowTest" --tests "com.wenwentome.reader.AppReaderFlowTest"
```

Expected: PASS locally when Java is available; otherwise, the same scope must pass in GitHub Actions before the task is called complete.

- [ ] **Step 5: Commit**

```bash
git add feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverUiState.kt feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModel.kt feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverScreen.kt feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModelTest.kt app/src/testDebug/java/com/wenwentome/reader/AppNavigationSmokeTest.kt app/src/testDebug/java/com/wenwentome/reader/AppSmokeFlowTest.kt app/src/testDebug/java/com/wenwentome/reader/AppReaderFlowTest.kt docs/development-progress.md
git commit -m "feat: complete reader 1.1 discover and smoke coverage"
```

## Spec Traceability

| Spec requirement | Plan coverage | Verification |
|---|---|---|
| EPUB 首开进入正文，不落到 `Cover` | Task 2 | `LocalBookContentRepositoryTest` + EPUB 手测 |
| 三种翻页模式 + 目录 + 进度恢复 | Task 5 | `ReaderViewModelTest` + `ReaderScreenTest` |
| 阅读主题 / 字号 / 行距 / 亮度 / 进度显示 | Task 1 + Task 5 | `ReaderPreferencesStoreTest` + `ReaderScreenTest` |
| 封面四态：自动 / 手动更新 / 导入照片 / 恢复自动 | Task 6 + Task 7 | `BookDetailViewModelTest` + `BookDetailScreenTest` + `LibraryScreenTest` |
| 网文闭环：搜索到目录刷新到最新章 | Task 3 + Task 4 + Task 8 | `DiscoverViewModelTest` + `AppReaderFlowTest` |
| `1.0` 升级到 `1.1` 不丢书架与绑定 | Task 1 | `ReaderDatabaseMigrationTest` |

## Final Verification Checklist

- [ ] `TXT` 书籍能继续正常打开
- [ ] `EPUB` 首次打开进入正文，而不是 `Cover`
- [ ] 阅读器支持仿真翻页 / 左右滑动 / 上下滚动三模式
- [ ] 目录可以高亮当前章节并完成跳转
- [ ] 阅读菜单可调整主题 / 字号 / 行距 / 亮度，并显示当前进度
- [ ] 切换翻页模式不会把阅读位置重置到章首
- [ ] 书架首页展示长方形封面、继续阅读和更新提示
- [ ] 书架长按菜单可进入封面管理 / 刷新目录
- [ ] 手动更新封面、手动导入照片、恢复自动封面可用
- [ ] 网文可以完成 搜索 → 详情 → 目录 → 正文 → 保存进度 → 刷新目录 → 跳转最新章
- [ ] `1.0` 安装数据升级到 `1.1` 后保留书架、进度和远端绑定
- [ ] `android-ci` 在 GitHub Actions 上通过
