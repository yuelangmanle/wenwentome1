# WenwenToMe 1.0 Release Package Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a `1.0` release package that adds in-app version/project visibility, offline changelog browsing, complete release/developer documentation, and a tag-driven signed GitHub Release workflow.

**Architecture:** Keep the existing Android app shell and settings feature as the user-facing entry point for release metadata. Store changelog history as app assets and render it through the settings flow, while keeping release automation in GitHub Actions plus small repository scripts so version/tag/release-note rules are explicit and testable. Preserve the current module boundaries: app-level constants and navigation stay in `app`, presentation and changelog parsing live in `feature/settings`, and release/doc rules live at the repo root.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose, Robolectric/Compose UI tests, kotlinx.serialization JSON, GitHub Actions, Python 3 standard library scripts, Markdown docs

---

## File Structure

Lock these file responsibilities before implementing.

- Modify: `app/build.gradle.kts`
  Purpose: Set `versionName = "1.0"`, `versionCode = 100`, and add environment-driven release signing config.
- Create: `app/src/main/assets/changelog.json`
  Purpose: Offline, structured changelog data source rendered inside the app.
- Create: `app/src/main/java/com/wenwentome/reader/AppProjectMetadata.kt`
  Purpose: Centralize app name, author name, GitHub URL, and release metadata passed into the settings UI.
- Modify: `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`
  Purpose: Wire the settings screen’s “关于与项目” actions and the changelog route.
- Test: `app/src/test/java/com/wenwentome/reader/AppAboutAndChangelogFlowTest.kt`
  Purpose: Smoke-test the “我的” page card, changelog navigation, and visible version metadata.

- Modify: `feature/settings/build.gradle.kts`
  Purpose: Add JSON parsing dependency for changelog asset loading.
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/AboutProjectModels.kt`
  Purpose: Define lightweight UI models for author/project/version metadata.
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/ChangelogModels.kt`
  Purpose: Define parsed changelog entry models and UI state.
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/ChangelogRepository.kt`
  Purpose: Read and parse `assets/changelog.json` from app context.
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/ChangelogViewModel.kt`
  Purpose: Load changelog entries into state for the changelog screen.
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/AboutProjectCard.kt`
  Purpose: Render the “关于与项目” card inside the settings screen.
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/ChangelogScreen.kt`
  Purpose: Render the complete in-app changelog list.
- Modify: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SettingsScreen.kt`
  Purpose: Append the new about/project card and forward click handlers.
- Test: `feature/settings/src/test/kotlin/com/wenwentome/reader/feature/settings/ChangelogRepositoryTest.kt`
  Purpose: Lock changelog JSON parsing, ordering, and field handling.

- Create: `.github/workflows/android-release.yml`
  Purpose: Build signed `release APK`, create GitHub Release, and upload release artifact on `v*` tags.
- Create: `scripts/release_metadata.py`
  Purpose: Validate tag/version alignment and extract a version’s release notes from `CHANGELOG.md`.
- Create: `scripts/tests/test_release_metadata.py`
  Purpose: Keep release metadata extraction stable without hand-testing workflow shell snippets.

- Create: `README.md`
  Purpose: Public project entry point for installation, features, and release downloads.
- Create: `CHANGELOG.md`
  Purpose: Canonical human-readable version history synced with in-app JSON and GitHub Releases.
- Create: `docs/release-process.md`
  Purpose: Explain signing secrets, tagging, and formal release steps.
- Create: `docs/development-handbook.md`
  Purpose: Capture module boundaries, coding rules, and working conventions.
- Create: `docs/development-progress.md`
  Purpose: Snapshot current completion, active risks, and next milestones.
- Create: `docs/iteration-rules.md`
  Purpose: Document `1.0 -> 1.1 -> ... -> 1.9 -> 2.0` versioning and changelog/update requirements.

### Task 1: Establish Versioning And Changelog Foundations

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/assets/changelog.json`
- Modify: `feature/settings/build.gradle.kts`
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/ChangelogModels.kt`
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/ChangelogRepository.kt`
- Test: `feature/settings/src/test/kotlin/com/wenwentome/reader/feature/settings/ChangelogRepositoryTest.kt`
- Create: `CHANGELOG.md`

- [ ] **Step 1: Write the failing changelog parsing test**

```kotlin
@Test
fun parse_returnsNewestEntriesFirstAndPreservesHighlights() {
    val repository = ChangelogRepository(
        loadJson = {
            """
            [
              {
                "version": "1.0",
                "releaseDate": "2026-03-19",
                "title": "首发版本",
                "highlights": ["统一书库"],
                "details": ["支持 TXT / EPUB"]
              },
              {
                "version": "1.1",
                "releaseDate": "2026-03-29",
                "title": "首个迭代",
                "highlights": ["更新日志页优化"],
                "details": ["新增版本排序"]
              }
            ]
            """.trimIndent()
        },
    )

    val entries = repository.getAll()

    assertEquals(listOf("1.1", "1.0"), entries.map { it.version })
    assertEquals(listOf("更新日志页优化"), entries.first().highlights)
}
```

- [ ] **Step 2: Run the settings test to verify it fails**

Run: `./gradlew :feature:settings:testDebugUnitTest --tests "com.wenwentome.reader.feature.settings.ChangelogRepositoryTest.parse_returnsNewestEntriesFirstAndPreservesHighlights"`

Expected: FAIL because no changelog repository/model exists yet.

- [ ] **Step 3: Implement the minimal changelog parser and models**

```kotlin
@Serializable
data class ChangelogEntry(
    val version: String,
    val releaseDate: String,
    val title: String,
    val highlights: List<String>,
    val details: List<String>,
)

class ChangelogRepository(
    private val loadJson: suspend () -> String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun getAll(): List<ChangelogEntry> =
        json.decodeFromString<List<ChangelogEntry>>(loadJson())
            .sortedByDescending { it.version }
}
```

- [ ] **Step 4: Run the settings test to verify it passes**

Run: `./gradlew :feature:settings:testDebugUnitTest --tests "com.wenwentome.reader.feature.settings.ChangelogRepositoryTest.parse_returnsNewestEntriesFirstAndPreservesHighlights"`

Expected: PASS

- [ ] **Step 5: Bump the app to `1.0`, add the canonical changelog files, and wire JSON dependency**

```kotlin
defaultConfig {
    applicationId = "com.wenwentome.reader"
    minSdk = 26
    targetSdk = 35
    versionCode = 100
    versionName = "1.0"
}
```

```json
[
  {
    "version": "1.0",
    "releaseDate": "2026-03-19",
    "title": "首发版本",
    "highlights": [
      "支持 TXT / EPUB 本地导入与阅读",
      "支持发现页搜索网文并加入统一书库",
      "支持 GitHub 私有仓库同步与恢复"
    ],
    "details": [
      "新增统一书库、详情页和阅读态",
      "新增书源导入、启用/禁用和网文发现流程",
      "新增 GitHub 同步设置、备份与恢复入口"
    ]
  }
]
```

```markdown
## [1.0] - 2026-03-19

### Added
- 支持 TXT / EPUB 本地导入与阅读
- 支持发现页搜索网文并加入统一书库
- 支持 GitHub 私有仓库同步与恢复
```

- [ ] **Step 6: Run the focused verification pass**

Run: `./gradlew :feature:settings:testDebugUnitTest`

Expected: PASS

- [ ] **Step 7: Commit the versioning/changelog foundation**

```bash
git add app/build.gradle.kts app/src/main/assets/changelog.json feature/settings/build.gradle.kts feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/ChangelogModels.kt feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/ChangelogRepository.kt feature/settings/src/test/kotlin/com/wenwentome/reader/feature/settings/ChangelogRepositoryTest.kt CHANGELOG.md
git commit -m "feat: add release changelog foundations"
```

### Task 2: Add The Settings “About And Project” Card And In-App Changelog

**Files:**
- Create: `app/src/main/java/com/wenwentome/reader/AppProjectMetadata.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`
- Create: `app/src/test/java/com/wenwentome/reader/AppAboutAndChangelogFlowTest.kt`
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/AboutProjectModels.kt`
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/AboutProjectCard.kt`
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/ChangelogViewModel.kt`
- Create: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/ChangelogScreen.kt`
- Modify: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SettingsScreen.kt`

- [ ] **Step 1: Write the failing app smoke test for the new settings card**

```kotlin
@Test
fun settingsShowsAboutCardAndNavigatesToChangelog() {
    val appContainer = AppContainer(ApplicationProvider.getApplicationContext())

    composeTestRule.setContent {
        ReaderApp(appContainer = appContainer)
    }

    composeTestRule.onNodeWithTag("nav-settings").performClick()
    composeTestRule.onNodeWithText("关于与项目").assertExists()
    composeTestRule.onNodeWithText("作者：月亮满了").assertExists()
    composeTestRule.onNodeWithText("版本 1.0").assertExists()
    composeTestRule.onNodeWithText("查看完整更新日志").performClick()
    composeTestRule.onNodeWithTag("screen").assertTextEquals("更新日志")
    composeTestRule.onNodeWithText("v1.0").assertExists()
}
```

- [ ] **Step 2: Run the app smoke test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.wenwentome.reader.AppAboutAndChangelogFlowTest.settingsShowsAboutCardAndNavigatesToChangelog"`

Expected: FAIL because the settings card and changelog route do not exist.

- [ ] **Step 3: Implement the minimal UI models, about card, changelog screen, and settings navigation**

```kotlin
data class AboutProjectInfo(
    val appName: String,
    val authorName: String,
    val versionName: String,
    val projectUrl: String,
)

@Composable
fun AboutProjectCard(
    info: AboutProjectInfo,
    onOpenProject: () -> Unit,
    onOpenChangelog: () -> Unit,
) {
    ElevatedCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("关于与项目")
            Text("作者：${info.authorName}")
            Text("版本 ${info.versionName}")
            OutlinedButton(onClick = onOpenProject) { Text("打开 GitHub 项目") }
            Button(onClick = onOpenChangelog) { Text("查看完整更新日志") }
        }
    }
}
```

```kotlin
private const val SettingsChangelogRoute = "settings/changelog"

composable(SettingsChangelogRoute) {
    val viewModel = remember(appContainer) {
        ChangelogViewModel(repository = appContainer.changelogRepository)
    }
    val state by viewModel.uiState.collectAsState()
    ChangelogScreen(state = state)
}
```

- [ ] **Step 4: Run the app smoke test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.wenwentome.reader.AppAboutAndChangelogFlowTest.settingsShowsAboutCardAndNavigatesToChangelog"`

Expected: PASS

- [ ] **Step 5: Run the broader UI regression pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.wenwentome.reader.AppNavigationSmokeTest" --tests "com.wenwentome.reader.AppSmokeFlowTest"`

Expected: PASS

- [ ] **Step 6: Commit the settings/about/changelog UI**

```bash
git add app/src/main/java/com/wenwentome/reader/AppProjectMetadata.kt app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt app/src/test/java/com/wenwentome/reader/AppAboutAndChangelogFlowTest.kt feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/AboutProjectModels.kt feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/AboutProjectCard.kt feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/ChangelogViewModel.kt feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/ChangelogScreen.kt feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SettingsScreen.kt
git commit -m "feat: add settings project card and changelog"
```

### Task 3: Automate Signed GitHub Releases From `main` Tags

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `.github/workflows/android-release.yml`
- Create: `scripts/release_metadata.py`
- Create: `scripts/tests/test_release_metadata.py`

- [ ] **Step 1: Write the failing release metadata tests**

```python
class ReleaseMetadataTest(unittest.TestCase):
    def test_extract_notes_returns_requested_version_section(self):
        notes = extract_notes(
            changelog_text=\"\"\"## [1.0] - 2026-03-19\n- 首发\n\n## [1.1] - 2026-03-29\n- 修复\"\"\",
            version=\"1.0\",
        )
        self.assertIn("首发", notes)
        self.assertNotIn("修复", notes)

    def test_read_gradle_version_name_matches_tag(self):
        version_name = read_version_name(
            '''
            defaultConfig {
                versionCode = 100
                versionName = "1.0"
            }
            '''
        )
        self.assertEqual("1.0", version_name)
```

- [ ] **Step 2: Run the release metadata tests to verify they fail**

Run: `python3 -m unittest scripts/tests/test_release_metadata.py -v`

Expected: FAIL because the script does not exist yet.

- [ ] **Step 3: Implement the metadata script and the release workflow**

```python
def read_version_name(gradle_text: str) -> str:
    match = re.search(r'versionName\\s*=\\s*"([^"]+)"', gradle_text)
    if not match:
        raise ValueError("versionName not found")
    return match.group(1)

def extract_notes(changelog_text: str, version: str) -> str:
    pattern = rf"^## \\[{re.escape(version)}\\].*?(?=^## \\[|\\Z)"
    match = re.search(pattern, changelog_text, re.MULTILINE | re.DOTALL)
    if not match:
        raise ValueError(f"version {version} not found in changelog")
    return match.group(0).strip()
```

```yaml
on:
  push:
    tags:
      - "v*"

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - uses: actions/setup-java@v4
      - run: |
          git branch -r --contains "$GITHUB_SHA" | grep "origin/main"
      - run: python3 scripts/release_metadata.py check-tag "${GITHUB_REF_NAME}" app/build.gradle.kts
      - run: python3 scripts/release_metadata.py notes "${GITHUB_REF_NAME#v}" CHANGELOG.md > release-notes.md
      - run: echo "$ANDROID_KEYSTORE_BASE64" | base64 --decode > "$RUNNER_TEMP/release.jks"
      - run: ./gradlew assembleRelease
        env:
          ANDROID_KEYSTORE_PATH: ${{ runner.temp }}/release.jks
          ANDROID_KEYSTORE_PASSWORD: ${{ secrets.ANDROID_KEYSTORE_PASSWORD }}
          ANDROID_KEY_ALIAS: ${{ secrets.ANDROID_KEY_ALIAS }}
          ANDROID_KEY_PASSWORD: ${{ secrets.ANDROID_KEY_PASSWORD }}
      - uses: softprops/action-gh-release@v2
        with:
          body_path: release-notes.md
          files: app/build/outputs/apk/release/*.apk
```

- [ ] **Step 4: Wire release signing into Gradle with environment variables**

```kotlin
signingConfigs {
    create("release") {
        val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
        if (keystorePath != null) {
            storeFile = file(keystorePath)
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
        }
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = false
    }
}
```

- [ ] **Step 5: Run the release metadata tests and local dry-run checks**

Run: `python3 -m unittest scripts/tests/test_release_metadata.py -v`

Expected: PASS

Run: `python3 scripts/release_metadata.py check-tag v1.0 app/build.gradle.kts`

Expected: PASS with no output

Run: `python3 scripts/release_metadata.py notes 1.0 CHANGELOG.md`

Expected: prints only the `1.0` release notes block

- [ ] **Step 6: Commit the release automation**

```bash
git add app/build.gradle.kts .github/workflows/android-release.yml scripts/release_metadata.py scripts/tests/test_release_metadata.py
git commit -m "build: add signed github release workflow"
```

### Task 4: Publish The User And Developer Documentation Pack

**Files:**
- Create: `README.md`
- Create: `docs/release-process.md`
- Create: `docs/development-handbook.md`
- Create: `docs/development-progress.md`
- Create: `docs/iteration-rules.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Write the public-facing README**

```markdown
# WenwenToMe

本地书籍阅读器和网文阅读器二合一的 Android 应用。

## 当前能力
- TXT / EPUB 导入与阅读
- 发现页搜索网文并加入统一书库
- GitHub 私有仓库同步
- App 内完整更新日志

## 下载
- 正式版本请前往 GitHub Releases 页面下载 `release APK`
```

- [ ] **Step 2: Write the release and iteration rule documents**

```markdown
## 版本规则

- 首版：`1.0`
- 后续每次正式版 `+0.1`
- `1.9` 后进位为 `2.0`

## 正式发版前必须同步更新

1. `app/build.gradle.kts`
2. `CHANGELOG.md`
3. `app/src/main/assets/changelog.json`
4. GitHub Release notes
```

- [ ] **Step 3: Write the development handbook and progress snapshot**

```markdown
## 当前完成

- 统一书库
- 本地 TXT / EPUB 阅读
- 网文发现、入库、书源导入
- GitHub 同步 MVP
- 1.0 发布链路

## 当前风险

- 旧书源同步兼容仍需继续观察
- Room 迁移仍是 destructive migration
```

- [ ] **Step 4: Run the documentation consistency checks**

Run: `rg -n "1\\.0|v1\\.0|月亮满了|ANDROID_KEYSTORE_BASE64|changelog.json" README.md CHANGELOG.md docs .github/workflows/android-release.yml app/src/main/assets/changelog.json`

Expected: all required release/version identifiers are present in the right files

- [ ] **Step 5: Run the final verification pass**

Run: `python3 -m unittest scripts/tests/test_release_metadata.py -v`

Expected: PASS

Run: `./gradlew :feature:settings:testDebugUnitTest :app:testDebugUnitTest`

Expected: PASS

- [ ] **Step 6: Commit the documentation pack**

```bash
git add README.md CHANGELOG.md docs/release-process.md docs/development-handbook.md docs/development-progress.md docs/iteration-rules.md
git commit -m "docs: add 1.0 release documentation pack"
```

## Execution Notes

- Follow `@superpowers:test-driven-development` for every behavior change in Kotlin and script code.
- Keep the settings screen single-column and append the “关于与项目” card below sync controls; do not redesign the whole page.
- Keep all changelog history in `app/src/main/assets/changelog.json`; do not make the app depend on GitHub APIs for changelog display.
- Reuse `CHANGELOG.md` as the source for GitHub Release notes through `scripts/release_metadata.py`; do not duplicate separate release-note prose.
- Release automation must assume local packaging is unsupported; verification of signed release builds happens in GitHub Actions, not on this machine.
- Preserve the existing `android-ci.yml`; the new `android-release.yml` is additive and tag-driven.
