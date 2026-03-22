# API Hub 与阅读体验增强 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付 WenwenToMe `1.2`：上线 API 中心底座、阅读体验收口、首批阅读器 AI 能力、封面/元数据增强、网文增强，并把版本文档与云端发布链路一起收口。

**Architecture:** 新增 `data:apihub` 作为统一 API runtime，负责 provider、secret、model、budget、fallback、usage log 与能力调度；新增 `feature:apihub` 作为“我的 -> API 中心”入口与配置 UI。阅读器、详情页、发现页、书架页只通过 `ApiAbilityDispatcher` 请求能力，不直接拼 provider 细节；GitHub 同步继续保持 snapshot 模式，但 secret 改成 `bootstrap secret + SecretEnvelopeV1 + sync password` 的双层恢复流程。

**Tech Stack:** Kotlin、Jetpack Compose、Room、DataStore、OkHttp、kotlinx.serialization、AndroidX Security Crypto、Robolectric、GitHub Actions、GitHub Pages

---

## Execution Notes

- 本计划不再拆成多个子计划，因为 `API Hub / secret 同步 / 阅读体验收口 / AI 能力入口 / 网文增强` 共用同一套模型、导航和发布链路；拆开会造成反复改表和重复接线。
- 所有编码任务先按 `@superpowers:test-driven-development` 执行；遇到非预期红测先切到 `@superpowers:systematic-debugging`；对外宣称完成前必须走 `@superpowers:verification-before-completion`。
- `./gradlew` 命令默认在 GitHub Codespaces 或 GitHub Actions 的 JDK 17 环境执行；当前本地若仍缺 Java，不要把本地未跑过的命令标成通过。
- 正式发版目标是 `1.2 / versionCode 120`，只有在 Tasks 1-10 全部落地并且云端验证通过后才更新正式 release metadata。
- 用户要求“后期功能不要丢”，因此本轮除了实现代码，还要把后续 API 能力路线写进仓库文档，避免上下文压缩后遗失。

## File Map

- `settings.gradle.kts`
  Purpose: 注册 `:data:apihub` 与 `:feature:apihub` 模块。
- `gradle/libs.versions.toml`
  Purpose: 增加 API Hub 所需的安全存储、浏览器/网络、Compose 辅助依赖。
- `data/apihub/build.gradle.kts`
  Purpose: API runtime 模块依赖声明。
- `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/...`
  Purpose: provider 仓储、secret 存储、模型发现、价格估算、能力缓存、冲突合并、预算保护、能力调度、usage log。
- `feature/apihub/build.gradle.kts`
  Purpose: API 中心 UI 模块依赖声明。
- `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/...`
  Purpose: API 中心总览、provider 管理、模型/能力绑定、预算与回退、价格覆盖、冲突处理、调用记录页面。
- `core/model/src/main/kotlin/com/wenwentome/reader/core/model/...`
  Purpose: API provider、model、capability binding、budget、usage log 等跨模块模型。
- `core/database/src/main/kotlin/com/wenwentome/reader/core/database/...`
  Purpose: Room entity/dao/migration/mappers，持久化 API Hub 数据、价格覆盖和能力缓存。
- `core/database/src/main/kotlin/com/wenwentome/reader/core/database/datastore/ReaderPreferencesStore.kt`
  Purpose: 保留阅读展示偏好，同时迁出 GitHub token 明文存储并接入 bootstrap secret。
- `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/...`
  Purpose: GitHub snapshot schema、secret envelope 同步、恢复流程与 `updatedAt` 合并。
- `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/...`
  Purpose: “我的”页入口、同步设置、bootstrap token 与 sync password 配置 UI。
- `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/...`
  Purpose: 阅读体验收口、AI 面板、目录/进度保位、章节总结/翻译/TTS 入口。
- `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/...`
  Purpose: 书架筛选/排序 UI、美化后的状态分组、封面增强入口与 AI 分类建议占位。
- `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/...`
  Purpose: 网文智能搜索增强、书源健康评分、自动切源与更新提醒增强。
- `app/src/main/java/com/wenwentome/reader/di/AppContainer.kt`
  Purpose: 模块装配、repository/runtime 注入与页面接线。
- `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`
  Purpose: 新页面路由、阅读器 AI 与 API 中心入口。
- `app/src/main/assets/changelog.json`, `CHANGELOG.md`, `README.md`, `site/index.html`, `docs/*.md`
  Purpose: `1.2` 版本元数据、开发文档、路线图、发布页与更新日志对齐。

## Task 1: API Hub 模块骨架与导航接线

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/wenwentome/reader/ReaderApp.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/di/AppContainer.kt`
- Modify: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SettingsScreen.kt`
- Create: `data/apihub/build.gradle.kts`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/ApiHubModule.kt`
- Create: `feature/apihub/build.gradle.kts`
- Create: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubUiState.kt`
- Create: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubViewModel.kt`
- Create: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubOverviewScreen.kt`
- Test: `app/src/testDebug/java/com/wenwentome/reader/AppNavigationSmokeTest.kt`
- Test: `feature/apihub/src/testDebug/java/com/wenwentome/reader/feature/apihub/ApiHubOverviewScreenTest.kt`

- [ ] **Step 1: 先写失败测试，锁定“我的 -> API 中心”导航和占位总览页**

```kotlin
@Test
fun settings_canOpenApiHubOverview() {
    composeTestRule.setContent { TestApp() }

    composeTestRule.onNodeWithText("API 中心").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("api-hub-overview-screen").assertExists()
    composeTestRule.onNodeWithText("今日调用").assertExists()
}
```

- [ ] **Step 2: 运行导航与占位 UI 测试，确认当前仓库还没有 API Hub 入口**

Run: `./gradlew :app:testDebugUnitTest --tests "*AppNavigationSmokeTest" :feature:apihub:testDebugUnitTest --tests "*ApiHubOverviewScreenTest"`
Expected: FAIL，报错指向缺少 `:feature:apihub` 模块、路由或 `api-hub-overview-screen` selector。

- [ ] **Step 3: 先补模块骨架与最小 ViewModel/UI**

```kotlin
data class ApiHubUiState(
    val enabledProviderCount: Int = 0,
    val boundCapabilityCount: Int = 0,
    val todayCallCount: Int = 0,
    val budgetUsageRatio: Float = 0f,
    val latestError: String? = null,
)
```

```kotlin
@Composable
fun ApiHubOverviewScreen(
    state: ApiHubUiState,
    onOpenProviders: () -> Unit,
    onOpenBindings: () -> Unit,
    onOpenBudgets: () -> Unit,
) { /* 先渲染可测试的占位统计卡 */ }
```

- [ ] **Step 4: 把模块接到 app 和“我的”页**

```kotlin
private const val SettingsApiHubRoute = "settings/api-hub"

SettingsScreen(
    /* ... */
    onOpenApiHub = { navController.navigate(SettingsApiHubRoute) },
)
```

```kotlin
composable(SettingsApiHubRoute) {
    ApiHubOverviewScreen(state = apiHubViewModel.uiState.collectAsState().value, ...)
}
```

- [ ] **Step 5: 跑通 app 与新模块测试**

Run: `./gradlew :feature:apihub:testDebugUnitTest :app:testDebugUnitTest --tests "*AppNavigationSmokeTest"`
Expected: PASS，测试能进入 `API 中心` 占位页且底部导航高亮仍正确。

- [ ] **Step 6: 提交骨架与导航接线**

```bash
git add settings.gradle.kts gradle/libs.versions.toml app/build.gradle.kts \
  app/src/main/java/com/wenwentome/reader/ReaderApp.kt \
  app/src/main/java/com/wenwentome/reader/navigation/AppNavHost.kt \
  app/src/main/java/com/wenwentome/reader/di/AppContainer.kt \
  feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SettingsScreen.kt \
  data/apihub/build.gradle.kts \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/ApiHubModule.kt \
  feature/apihub/build.gradle.kts \
  feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubUiState.kt \
  feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubViewModel.kt \
  feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubOverviewScreen.kt \
  app/src/testDebug/java/com/wenwentome/reader/AppNavigationSmokeTest.kt \
  feature/apihub/src/testDebug/java/com/wenwentome/reader/feature/apihub/ApiHubOverviewScreenTest.kt
git commit -m "feat: scaffold api hub modules and navigation"
```

## Task 2: API 核心模型、Room 表与本地 secret 存储

**Files:**
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ApiProviderProfile.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ApiModelProfile.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ApiCapabilityBinding.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ApiBudgetPolicy.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ApiUsageLog.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ApiPriceOverride.kt`
- Create: `core/model/src/main/kotlin/com/wenwentome/reader/core/model/ApiAbilityCacheEntry.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ApiProviderEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ApiModelEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ApiCapabilityBindingEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ApiBudgetPolicyEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ApiUsageLogEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ApiPriceOverrideEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ApiAbilityCacheEntity.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ApiProviderDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ApiModelDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ApiCapabilityBindingDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ApiBudgetPolicyDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ApiUsageLogDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ApiPriceOverrideDao.kt`
- Create: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ApiAbilityCacheDao.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/ReaderDatabase.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseMigrations.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/DatabaseMappers.kt`
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/DatabaseConverters.kt`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/local/ApiSecretLocalStore.kt`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/ApiHubRepository.kt`
- Test: `core/database/src/test/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseTest.kt`
- Test: `data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/local/ApiSecretLocalStoreTest.kt`

- [ ] **Step 1: 写失败测试，锁定 provider/binding/log 的落库结构与本地 secret 策略**

```kotlin
@Test
fun providerDao_roundTripsSecretModeModelSourceAndUpdatedAt() = runTest {
    dao.upsert(
        ApiProviderEntity(
            providerId = "openai-main",
            displayName = "OpenAI 主线路",
            secretSyncMode = SecretSyncMode.LOCAL_ONLY,
            updatedAt = 1711000000000,
        )
    )

    val stored = dao.observeAll().first().single()
    assertEquals(SecretSyncMode.LOCAL_ONLY, stored.secretSyncMode)
    assertEquals(1711000000000, stored.updatedAt)
}
```

```kotlin
@Test
fun apiSecretLocalStore_readsAndWritesBootstrapSecret() = runTest {
    store.save("provider-1", "sk-test")
    assertEquals("sk-test", store.read("provider-1"))
}
```

```kotlin
@Test
fun abilityCacheDao_roundTripsCacheKeyPayloadAndExpiresAt() = runTest {
    cacheDao.upsert(
        ApiAbilityCacheEntity(
            cacheKey = "reader.summary:book-1:chapter-3:v1",
            payloadJson = """{"summary":"cached"}""",
            expiresAt = 1712000000000,
            updatedAt = 1711000000000,
        )
    )

    val cached = cacheDao.findByKey("reader.summary:book-1:chapter-3:v1")
    assertEquals("""{"summary":"cached"}""", cached?.payloadJson)
}
```

- [ ] **Step 2: 运行数据库与 secret storage 测试，确认当前模型与表还不存在**

Run: `./gradlew :core:database:testDebugUnitTest :data:apihub:test`
Expected: FAIL，报错指向缺少 API Hub entity/dao/model 或 `ApiSecretLocalStore`。

- [ ] **Step 3: 先补 core:model 与 Room schema，包括价格覆盖和能力缓存表**

```kotlin
data class ApiCapabilityBinding(
    val capabilityId: String,
    val primaryProviderId: String,
    val primaryModelId: String,
    val fallbackProviderId: String? = null,
    val fallbackModelId: String? = null,
    val enabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
)
```

```kotlin
@Entity(tableName = "api_usage_logs")
data class ApiUsageLogEntity(
    @PrimaryKey val callId: String,
    val capabilityId: String,
    val providerId: String,
    val modelId: String,
    val success: Boolean,
    val estimatedCostMicros: Long,
    val createdAt: Long,
)
```

```kotlin
@Entity(tableName = "api_ability_cache")
data class ApiAbilityCacheEntity(
    @PrimaryKey val cacheKey: String,
    val payloadJson: String,
    val expiresAt: Long,
    val updatedAt: Long,
)
```

- [ ] **Step 4: 再补最小本地 secret 仓储**

```kotlin
interface ApiSecretLocalStore {
    suspend fun save(secretId: String, plainText: String)
    suspend fun read(secretId: String): String?
    suspend fun delete(secretId: String)
}
```

- [ ] **Step 5: 跑通 core/database 与 data/apihub 测试**

Run: `./gradlew :core:database:testDebugUnitTest :data:apihub:test`
Expected: PASS，API Hub 基础 schema、mapper 与本地 secret store 通过。

- [ ] **Step 6: 提交核心模型和持久化基础**

```bash
git add core/model/src/main/kotlin/com/wenwentome/reader/core/model/ApiProviderProfile.kt \
  core/model/src/main/kotlin/com/wenwentome/reader/core/model/ApiModelProfile.kt \
  core/model/src/main/kotlin/com/wenwentome/reader/core/model/ApiCapabilityBinding.kt \
  core/model/src/main/kotlin/com/wenwentome/reader/core/model/ApiBudgetPolicy.kt \
  core/model/src/main/kotlin/com/wenwentome/reader/core/model/ApiUsageLog.kt \
  core/model/src/main/kotlin/com/wenwentome/reader/core/model/ApiPriceOverride.kt \
  core/model/src/main/kotlin/com/wenwentome/reader/core/model/ApiAbilityCacheEntry.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ApiProviderEntity.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ApiModelEntity.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ApiCapabilityBindingEntity.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ApiBudgetPolicyEntity.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ApiUsageLogEntity.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ApiPriceOverrideEntity.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/entity/ApiAbilityCacheEntity.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ApiProviderDao.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ApiModelDao.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ApiCapabilityBindingDao.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ApiBudgetPolicyDao.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ApiUsageLogDao.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ApiPriceOverrideDao.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/dao/ApiAbilityCacheDao.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/ReaderDatabase.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseMigrations.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/DatabaseMappers.kt \
  core/database/src/main/kotlin/com/wenwentome/reader/core/database/DatabaseConverters.kt \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/local/ApiSecretLocalStore.kt \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/ApiHubRepository.kt \
  core/database/src/test/kotlin/com/wenwentome/reader/core/database/ReaderDatabaseTest.kt \
  data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/local/ApiSecretLocalStoreTest.kt
git commit -m "feat: add api hub models and persistence"
```

## Task 3: GitHub bootstrap secret、SecretEnvelopeV1 与恢复迁移

**Files:**
- Modify: `core/database/src/main/kotlin/com/wenwentome/reader/core/database/datastore/ReaderPreferencesStore.kt`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/secret/SecretEnvelopeV1.kt`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/secret/SecretSyncCrypto.kt`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/sync/ApiHubMergeResolver.kt`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/sync/PendingSyncConflict.kt`
- Modify: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsUiState.kt`
- Modify: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsViewModel.kt`
- Modify: `feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsScreen.kt`
- Modify: `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/SyncManifest.kt`
- Modify: `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/SyncManifestSerializer.kt`
- Modify: `sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/GitHubSyncRepository.kt`
- Modify: `sync/github/src/test/kotlin/com/wenwentome/reader/sync/github/GitHubSyncRepositoryTest.kt`
- Test: `feature/settings/src/test/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsViewModelTest.kt`
- Test: `data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/secret/SecretEnvelopeV1Test.kt`
- Test: `data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/sync/ApiHubMergeResolverTest.kt`
- Test: `sync/github/src/test/kotlin/com/wenwentome/reader/sync/github/GitHubSyncRepositoryTest.kt`

- [ ] **Step 1: 写失败测试，锁定“bootstrap token 不入远端快照 + sync password 解锁远端 secret”**

```kotlin
@Test
fun pullLatestSnapshot_restoresNonSecretFieldsBeforeUnlockingEncryptedSecrets() = runTest {
    val restored = repository.pullLatestSnapshot(auth = auth)

    assertEquals("books", restored.preferences.owner)
    assertTrue(restored.pendingSecretRestore.isNotEmpty())
    assertEquals("", restored.preferences.bootstrapToken)
}
```

```kotlin
@Test
fun pushNow_persistsBootstrapTokenLocallyButSyncPayloadOmitsPlainToken() = runTest {
    viewModel.pushNow(
        SyncSettingsUiState(
            owner = "me",
            repo = "books",
            branch = "main",
            bootstrapToken = "ghp_secret",
            syncPassword = "sync-pass",
        )
    )
    assertEquals("ghp_secret", configStore.localBootstrapToken)
    assertFalse(serializer.lastPreferencesJson.contains("ghp_secret"))
}
```

```kotlin
@Test
fun mergeResolver_returnsConflictWhenTwoBindingsChangeInsideConflictWindow() = runTest {
    val result = mergeResolver.mergeBinding(local = localBinding, remote = remoteBinding)
    assertTrue(result is MergeBindingResult.Conflict)
}
```

- [ ] **Step 2: 跑失败测试，确认当前实现仍在 DataStore 明文保存并同步 GitHub token**

Run: `./gradlew :feature:settings:testDebugUnitTest :data:apihub:test :sync:github:test`
Expected: FAIL，错误指向 `bootstrapToken` 字段、`SecretEnvelopeV1`、远端快照结构和恢复流程不匹配。

- [ ] **Step 3: 先实现 envelope 与 crypto 基础**

```kotlin
@Serializable
data class SecretEnvelopeV1(
    val version: Int = 1,
    val scope: SecretScope,
    val kdf: String = "PBKDF2WithHmacSHA256",
    val iterations: Int,
    val saltBase64: String,
    val ivBase64: String,
    val cipherTextBase64: String,
    val checksumBase64: String,
    val updatedAt: Long,
)
```

- [ ] **Step 4: 把 GitHub token 迁成 bootstrap secret，并落地恢复顺序**

```kotlin
data class SyncSettingsUiState(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val bootstrapToken: String = "",
    val syncPassword: String = "",
)
```

```kotlin
// pullLatestSnapshot 内部顺序
restoreNonSecretConfig(preferences)
cachePendingEncryptedSecrets(secretEnvelopes)
```

- [ ] **Step 5: 实现 `updatedAt` 合并、冲突窗口与按 secret 单元恢复**

```kotlin
fun mergeBinding(local: ApiCapabilityBinding, remote: ApiCapabilityBinding): MergeBindingResult {
    return if (kotlin.math.abs(local.updatedAt - remote.updatedAt) <= conflictWindowMs) {
        MergeBindingResult.Conflict(local, remote)
    } else {
        MergeBindingResult.Resolved(if (local.updatedAt >= remote.updatedAt) local else remote)
    }
}
```

```kotlin
secretEnvelopes.forEach { envelope ->
    restoreOneSecretIfUnlocked(envelope)
}
```

- [ ] **Step 6: 跑通 settings、secret、sync 测试**

Run: `./gradlew :feature:settings:testDebugUnitTest :data:apihub:test :sync:github:test`
Expected: PASS，GitHub token 只保留本地 bootstrap secret，远端只保存密文 envelope 与非 secret 配置。

- [ ] **Step 7: 提交 secret 与同步迁移**

```bash
git add core/database/src/main/kotlin/com/wenwentome/reader/core/database/datastore/ReaderPreferencesStore.kt \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/secret/SecretEnvelopeV1.kt \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/secret/SecretSyncCrypto.kt \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/sync/ApiHubMergeResolver.kt \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/sync/PendingSyncConflict.kt \
  feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsUiState.kt \
  feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsViewModel.kt \
  feature/settings/src/main/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsScreen.kt \
  sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/SyncManifest.kt \
  sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/SyncManifestSerializer.kt \
  sync/github/src/main/kotlin/com/wenwentome/reader/sync/github/GitHubSyncRepository.kt \
  feature/settings/src/test/kotlin/com/wenwentome/reader/feature/settings/SyncSettingsViewModelTest.kt \
  data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/secret/SecretEnvelopeV1Test.kt \
  data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/sync/ApiHubMergeResolverTest.kt \
  sync/github/src/test/kotlin/com/wenwentome/reader/sync/github/GitHubSyncRepositoryTest.kt
git commit -m "feat: secure github bootstrap secret and sync envelopes"
```

## Task 4: Provider 管理、模型来源发现与绑定前校验

**Files:**
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/provider/ProviderTemplate.kt`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/provider/ProviderCatalogRepository.kt`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/provider/ModelDiscoveryService.kt`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/provider/ProviderValidationService.kt`
- Modify: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubUiState.kt`
- Modify: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubViewModel.kt`
- Create: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ProviderManagementScreen.kt`
- Create: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ModelBindingScreen.kt`
- Create: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/BindingConflictDialog.kt`
- Test: `data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/provider/ProviderCatalogRepositoryTest.kt`
- Test: `data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/provider/ProviderValidationServiceTest.kt`
- Test: `feature/apihub/src/test/kotlin/com/wenwentome/reader/feature/apihub/ApiHubViewModelTest.kt`
- Test: `feature/apihub/src/testDebug/java/com/wenwentome/reader/feature/apihub/ProviderManagementScreenTest.kt`
- Test: `feature/apihub/src/testDebug/java/com/wenwentome/reader/feature/apihub/BindingConflictDialogTest.kt`

- [ ] **Step 1: 写失败测试，锁定“远端发现 / 预设模型 / 手动模型”三种来源与绑定前校验**

```kotlin
@Test
fun discoverModels_mergesRemotePresetAndManualEntries() = runTest {
    val models = repository.modelsFor("deepseek-main")

    assertEquals(
        listOf("deepseek-chat", "deepseek-reasoner", "custom-manual-model"),
        models.map { it.modelId }
    )
}
```

```kotlin
@Test
fun bindCapability_requiresSuccessfulProbeOrPresetMatch() = runTest {
    val result = validator.validateBinding(providerId = "custom", modelId = "bad-model")
    assertFalse(result.allowed)
    assertTrue(result.reason.contains("probe"))
}
```

```kotlin
@Test
fun savingBinding_surfacesConflictForUserDecision() = runTest {
    viewModel.saveBinding(conflictingDraft)
    assertTrue(viewModel.uiState.value.pendingConflict != null)
}
```

- [ ] **Step 2: 运行 provider/model 测试，确认当前还没有发现与校验能力**

Run: `./gradlew :data:apihub:test :feature:apihub:testDebugUnitTest --tests "*ApiHubViewModelTest"`
Expected: FAIL，缺少 provider template、远端发现、手动录入和 probe 校验逻辑。

- [ ] **Step 3: 实现 provider 模板与模型来源聚合**

```kotlin
sealed interface ModelSource {
    data object RemoteDiscovery : ModelSource
    data object PresetCatalog : ModelSource
    data object ManualEntry : ModelSource
}
```

```kotlin
suspend fun modelsFor(providerId: String): List<ApiModelProfile> =
    mergeModels(
        remote = modelDiscoveryService.fetch(providerId),
        preset = templateCatalog.models(providerId),
        manual = modelDao.observeManual(providerId).first(),
    )
```

- [ ] **Step 4: 实现 UI：Provider 管理页、模型绑定页与冲突选择弹窗**

```kotlin
ProviderCard(
    templateLabel = provider.templateLabel,
    validationState = provider.validationState,
    onValidate = { viewModel.validateProvider(provider.providerId) },
)
```

```kotlin
BindingConflictDialog(
    conflict = state.pendingConflict,
    onKeepLocal = viewModel::keepLocalBinding,
    onUseRemote = viewModel::useRemoteBinding,
)
```

- [ ] **Step 5: 跑通 data + feature 测试**

Run: `./gradlew :data:apihub:test :feature:apihub:testDebugUnitTest`
Expected: PASS，provider 创建、模型来源聚合、probe 校验、绑定冲突提示和管理页 UI 全部通过。

- [ ] **Step 6: 提交 provider 与模型管理**

```bash
git add data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/provider/ProviderTemplate.kt \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/provider/ProviderCatalogRepository.kt \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/provider/ModelDiscoveryService.kt \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/provider/ProviderValidationService.kt \
  feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubUiState.kt \
  feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubViewModel.kt \
  feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ProviderManagementScreen.kt \
  feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ModelBindingScreen.kt \
  feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/BindingConflictDialog.kt \
  data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/provider/ProviderCatalogRepositoryTest.kt \
  data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/provider/ProviderValidationServiceTest.kt \
  feature/apihub/src/test/kotlin/com/wenwentome/reader/feature/apihub/ApiHubViewModelTest.kt \
  feature/apihub/src/testDebug/java/com/wenwentome/reader/feature/apihub/ProviderManagementScreenTest.kt \
  feature/apihub/src/testDebug/java/com/wenwentome/reader/feature/apihub/BindingConflictDialogTest.kt
git commit -m "feat: add provider management and model validation"
```

## Task 5: 能力绑定、预算保护、失败回退与调用记录

**Files:**
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/runtime/ApiAbilityDispatcher.kt`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/runtime/ApiBudgetGuard.kt`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/runtime/ApiFallbackExecutor.kt`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/runtime/ApiCallLogger.kt`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/pricing/ApiPricingRepository.kt`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/pricing/PriceEstimator.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/di/AppContainer.kt`
- Modify: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubOverviewScreen.kt`
- Modify: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubViewModel.kt`
- Create: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/BudgetFallbackScreen.kt`
- Create: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/PriceCatalogScreen.kt`
- Create: `feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/UsageLogScreen.kt`
- Test: `data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/runtime/ApiAbilityDispatcherTest.kt`
- Test: `data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/runtime/ApiBudgetGuardTest.kt`
- Test: `data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/pricing/ApiPricingRepositoryTest.kt`
- Test: `feature/apihub/src/testDebug/java/com/wenwentome/reader/feature/apihub/ApiHubOverviewScreenTest.kt`

- [ ] **Step 1: 写失败测试，锁定 429/5xx 回退、预算拦截和 usage log**

```kotlin
@Test
fun dispatch_fallsBackToSecondaryModelOn429_andWritesUsageLog() = runTest {
    val result = dispatcher.invoke(
        capabilityId = "reader.summary",
        input = ReaderAbilityInput("第三章正文"),
    )

    assertEquals("fallback-provider", result.providerId)
    assertTrue(logDao.observeLatest().first().single().usedFallback)
}
```

```kotlin
@Test
fun budgetGuard_blocksHighCostRequestWhenDailyBudgetExceeded() = runTest {
    val decision = guard.evaluate(estimatedCostMicros = 2_000_000)
    assertEquals(BudgetDecision.Blocked("daily_limit"), decision)
}
```

```kotlin
@Test
fun priceRepository_prefersManualOverrideOverCatalogPrice() = runTest {
    priceRepository.saveOverride(modelId = "gpt-5-mini", inputPricePer1k = 1.23, outputPricePer1k = 4.56)
    val price = priceRepository.resolve("gpt-5-mini")
    assertEquals(1.23, price.inputPricePer1k, 0.0)
}
```

- [ ] **Step 2: 运行 runtime 测试，确认当前没有统一调度器**

Run: `./gradlew :data:apihub:test :feature:apihub:testDebugUnitTest --tests "*ApiHubOverviewScreenTest"`
Expected: FAIL，缺少 dispatcher、budget guard、fallback executor 或 UI 状态。

- [ ] **Step 3: 先实现 runtime 核心**

```kotlin
suspend fun invoke(capabilityId: String, input: AbilityInput): AbilityResult {
    val route = routeResolver.resolve(capabilityId)
    val pricedRoute = priceEstimator.attachPrice(route, input)
    budgetGuard.ensureAllowed(pricedRoute, input)
    return fallbackExecutor.execute(pricedRoute, input).also { outcome ->
        callLogger.record(route, outcome)
    }
}
```

- [ ] **Step 4: 把预算/回退/调用记录页接入 API 中心 UI**

```kotlin
ApiHubOverviewScreen(
    state = state,
    onOpenBudgets = { navController.navigate("settings/api-hub/budget") },
    onOpenPrices = { navController.navigate("settings/api-hub/prices") },
    onOpenUsageLogs = { navController.navigate("settings/api-hub/logs") },
)
```

- [ ] **Step 5: 跑通 runtime 与 API Hub UI 测试**

Run: `./gradlew :data:apihub:test :feature:apihub:testDebugUnitTest`
Expected: PASS，预算保护、手动价格覆盖、失败回退、usage log 和总览统计卡全部通过。

- [ ] **Step 6: 提交 API runtime**

```bash
git add data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/runtime/ApiAbilityDispatcher.kt \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/runtime/ApiBudgetGuard.kt \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/runtime/ApiFallbackExecutor.kt \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/runtime/ApiCallLogger.kt \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/pricing/ApiPricingRepository.kt \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/pricing/PriceEstimator.kt \
  app/src/main/java/com/wenwentome/reader/di/AppContainer.kt \
  feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubOverviewScreen.kt \
  feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/ApiHubViewModel.kt \
  feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/BudgetFallbackScreen.kt \
  feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/PriceCatalogScreen.kt \
  feature/apihub/src/main/kotlin/com/wenwentome/reader/feature/apihub/UsageLogScreen.kt \
  data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/runtime/ApiAbilityDispatcherTest.kt \
  data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/runtime/ApiBudgetGuardTest.kt \
  data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/pricing/ApiPricingRepositoryTest.kt \
  feature/apihub/src/testDebug/java/com/wenwentome/reader/feature/apihub/ApiHubOverviewScreenTest.kt
git commit -m "feat: add api hub runtime with budget fallback and logs"
```

## Task 6: 阅读体验收口与三条阅读链路回归

**Files:**
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepository.kt`
- Modify: `data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/EpubCatalogParser.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModel.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryUiState.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryViewModel.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryFilter.kt`
- Create: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibrarySort.kt`
- Test: `data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepositoryTest.kt`
- Test: `feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModelTest.kt`
- Test: `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt`
- Test: `feature/library/src/test/kotlin/com/wenwentome/reader/feature/library/LibraryViewModelTest.kt`
- Test: `app/src/testDebug/java/com/wenwentome/reader/AppReaderFlowTest.kt`
- Test: `app/src/testDebug/java/com/wenwentome/reader/AppSmokeFlowTest.kt`

- [ ] **Step 1: 写失败测试，锁定阅读模式切换保位、EPUB 正文入口与书架筛选/排序 UI**

```kotlin
@Test
fun switchingReaderMode_keepsCurrentLocator() = runTest {
    viewModel.updateLocator("chapter:ch2#paragraph:7", "ch2", 0.42f)
    viewModel.setReaderMode(ReaderMode.HORIZONTAL_PAGING)
    advanceUntilIdle()
    assertEquals("chapter:ch2#paragraph:7", persistedState?.locator)
}
```

```kotlin
@Test
fun load_epubSkipsCoverAndNavDocuments() = runTest {
    val content = repository.load("fixture-epub", locator = null)
    assertTrue(content.paragraphs.first().contains("正文"))
    assertFalse(content.chapterTitle.orEmpty().contains("Cover"))
}
```

- [ ] **Step 2: 跑 reader/localbooks/library/app 回归，确认阅读体验仍有缺口**

Run: `./gradlew :data:localbooks:testDebugUnitTest :feature:reader:testDebugUnitTest :feature:library:testDebugUnitTest :app:testDebugUnitTest --tests "*AppReaderFlowTest" --tests "*AppSmokeFlowTest"`
Expected: FAIL，至少命中阅读模式切换丢位置、EPUB 落到封面/目录页、书架缺筛选排序 UI 三类问题之一。

- [ ] **Step 3: 最小实现阅读模式切换保位与 EPUB 正文过滤**

```kotlin
fun setReaderMode(mode: ReaderMode) {
    viewModelScope.launch {
        val current = uiState.value.locatorForSave()
        if (current != null) {
            updateReadingState(uiState.value.asReadingState(locator = current))
        }
        saveReaderMode(mode)
    }
}
```

```kotlin
private fun isReadableEpubDocument(title: String?, href: String): Boolean =
    !href.contains("cover", ignoreCase = true) &&
        !href.contains("nav", ignoreCase = true) &&
        !href.contains("toc", ignoreCase = true)
```

- [ ] **Step 4: 补书架筛选/排序 UI，并扩宽 `TXT / EPUB / WEB` smoke 覆盖**

```kotlin
data class LibraryUiState(
    val filter: LibraryFilter = LibraryFilter.DEFAULT,
    val sort: LibrarySort = LibrarySort.LAST_READ_DESC,
    /* ... */
)
```

- [ ] **Step 5: 跑通阅读器、书架和 app 级回归**

Run: `./gradlew :data:localbooks:testDebugUnitTest :feature:reader:testDebugUnitTest :feature:library:testDebugUnitTest :app:testDebugUnitTest`
Expected: PASS，TXT/EPUB/WEB 三条链路都能继续阅读、切模式不丢位、书架可筛选排序。

- [ ] **Step 6: 提交阅读体验收口**

```bash
git add data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepository.kt \
  data/localbooks/src/main/kotlin/com/wenwentome/reader/data/localbooks/EpubCatalogParser.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModel.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryUiState.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryViewModel.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryScreen.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibraryFilter.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/LibrarySort.kt \
  data/localbooks/src/test/kotlin/com/wenwentome/reader/data/localbooks/LocalBookContentRepositoryTest.kt \
  feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModelTest.kt \
  feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt \
  feature/library/src/test/kotlin/com/wenwentome/reader/feature/library/LibraryViewModelTest.kt \
  app/src/testDebug/java/com/wenwentome/reader/AppReaderFlowTest.kt \
  app/src/testDebug/java/com/wenwentome/reader/AppSmokeFlowTest.kt
git commit -m "feat: complete reader experience consolidation"
```

## Task 7: 阅读器第一批 AI 能力

**Files:**
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/ability/ReaderAbilityFacade.kt`
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/cache/AbilityResultCacheRepository.kt`
- Modify: `app/src/main/java/com/wenwentome/reader/di/AppContainer.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderAssistantUiState.kt`
- Create: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderAssistantSheet.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModel.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt`
- Test: `data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/ability/ReaderAbilityFacadeTest.kt`
- Test: `data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/cache/AbilityResultCacheRepositoryTest.kt`
- Test: `feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModelTest.kt`
- Test: `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt`

- [ ] **Step 1: 写失败测试，锁定章节总结、段落解释、翻译、TTS 四个入口**

```kotlin
@Test
fun requestChapterSummary_usesCapabilityBindingAndReturnsAssistantCard() = runTest {
    viewModel.generateChapterSummary()
    advanceUntilIdle()

    assertEquals("reader.summary", facade.lastCapabilityId)
    assertTrue(viewModel.uiState.value.assistant.summary.orEmpty().isNotBlank())
}
```

```kotlin
@Test
fun requestChapterSummary_usesCacheOnSecondCall() = runTest {
    viewModel.generateChapterSummary()
    viewModel.generateChapterSummary()
    advanceUntilIdle()
    assertEquals(1, facade.remoteInvocationCount)
}
```

```kotlin
@Test
fun readerScreen_showsAssistantSheetActions() {
    composeTestRule.setContent { ReaderScreen(state = state, /* ... */) }
    composeTestRule.onNodeWithText("AI").performClick()
    composeTestRule.onNodeWithText("章节总结").assertExists()
    composeTestRule.onNodeWithText("AI 翻译").assertExists()
    composeTestRule.onNodeWithText("TTS 朗读").assertExists()
}
```

- [ ] **Step 2: 运行 reader AI 测试，确认当前阅读器还没有接 API Hub**

Run: `./gradlew :data:apihub:test :feature:reader:testDebugUnitTest --tests "*ReaderScreenTest" --tests "*ReaderViewModelTest"`
Expected: FAIL，缺少 AI 面板、facade、调用状态或结果展示。

- [ ] **Step 3: 最小实现阅读器能力 facade**

```kotlin
class ReaderAbilityFacade(
    private val dispatcher: ApiAbilityDispatcher,
    private val cache: AbilityResultCacheRepository,
) {
    suspend fun summarizeChapter(input: ReaderAbilityInput) =
        cache.getOrPut("reader.summary:${input.bookId}:${input.chapterRef}:v1") {
            dispatcher.invoke("reader.summary", input)
        }

    suspend fun translateSelection(input: ReaderAbilityInput) =
        cache.getOrPut("reader.translate:${input.paragraphHash}:${input.targetLanguage}:${input.bindingKey}") {
            dispatcher.invoke("reader.translate", input)
        }
}
```

- [ ] **Step 4: 把 AI 面板接到 ReaderScreen/ViewModel**

```kotlin
TextButton(onClick = { showAssistant = !showAssistant }) { Text("AI") }

ReaderAssistantSheet(
    state = state.assistant,
    onSummarizeChapter = viewModel::generateChapterSummary,
    onTranslateSelection = viewModel::translateCurrentParagraph,
    onExplainSelection = viewModel::explainCurrentParagraph,
    onSpeakCurrentChapter = viewModel::speakCurrentChapter,
)
```

- [ ] **Step 5: 跑通 data + reader 测试**

Run: `./gradlew :data:apihub:test :feature:reader:testDebugUnitTest`
Expected: PASS，阅读器能显示 AI 面板、发起能力调用、命中缓存并渲染结果与错误态。

- [ ] **Step 6: 提交阅读器 AI 能力**

```bash
git add data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/ability/ReaderAbilityFacade.kt \
  data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/cache/AbilityResultCacheRepository.kt \
  app/src/main/java/com/wenwentome/reader/di/AppContainer.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderAssistantUiState.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderAssistantSheet.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModel.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/ReaderScreen.kt \
  data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/ability/ReaderAbilityFacadeTest.kt \
  data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/cache/AbilityResultCacheRepositoryTest.kt \
  feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/ReaderViewModelTest.kt \
  feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/ReaderScreenTest.kt
git commit -m "feat: add reader ai assistant actions"
```

## Task 8: 封面与元数据增强

**Files:**
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/ability/BookMetadataEnhancementFacade.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailUiState.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailViewModel.kt`
- Modify: `feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailScreen.kt`
- Modify: `feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt`
- Test: `data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/ability/BookMetadataEnhancementFacadeTest.kt`
- Test: `feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/BookDetailViewModelTest.kt`
- Test: `feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/BookDetailScreenTest.kt`

- [ ] **Step 1: 写失败测试，锁定自动封面刷新、AI 元数据补全和手动应用**

```kotlin
@Test
fun enhanceMetadata_updatesSummaryAndSuggestedCover() = runTest {
    viewModel.enhanceMetadata()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.aiSummary.orEmpty().isNotBlank())
    assertTrue(viewModel.uiState.value.suggestedCoverUri.orEmpty().isNotBlank())
}
```

```kotlin
@Test
fun enhanceMetadata_reusesCacheByBookIdAndSourceSnapshot() = runTest {
    viewModel.enhanceMetadata()
    viewModel.enhanceMetadata()
    advanceUntilIdle()
    assertEquals(1, facade.remoteInvocationCount)
}
```

```kotlin
@Test
fun detailScreen_showsAutoRefreshManualImportAndRestoreOptions() {
    composeTestRule.setContent { BookDetailScreen(state = state, /* ... */) }
    composeTestRule.onNodeWithText("刷新封面").assertExists()
    composeTestRule.onNodeWithText("导入照片").assertExists()
    composeTestRule.onNodeWithText("恢复自动封面").assertExists()
    composeTestRule.onNodeWithText("AI 补全信息").assertExists()
}
```

- [ ] **Step 2: 运行详情页与 metadata 测试，确认当前只有基础封面管理**

Run: `./gradlew :data:apihub:test :feature:reader:testDebugUnitTest --tests "*BookDetailScreenTest" --tests "*BookDetailViewModelTest"`
Expected: FAIL，缺少 AI 元数据状态、推荐封面结果和“应用到书籍”行为。

- [ ] **Step 3: 实现 metadata facade 与 ViewModel 状态**

```kotlin
data class BookEnhancementResult(
    val improvedSummary: String?,
    val suggestedCoverUri: String?,
    val authorIntro: String?,
    val tags: List<String>,
)
```

```kotlin
suspend fun enhance(bookId: String, sourceSnapshot: String): BookEnhancementResult =
    cache.getOrPut("book.metadata:$bookId:$sourceSnapshot") {
        remoteEnhancer.enhance(bookId, sourceSnapshot)
    }
```

- [ ] **Step 4: 在详情页和书架卡片接入增强入口**

```kotlin
OutlinedButton(onClick = onEnhanceMetadataClick) {
    Text("AI 补全信息")
}
```

- [ ] **Step 5: 跑通 detail/library 相关测试**

Run: `./gradlew :data:apihub:test :feature:reader:testDebugUnitTest :feature:library:testDebugUnitTest`
Expected: PASS，详情页可触发 AI 补全、命中缓存并展示结果，书架卡片保留自动/手动/恢复三种封面操作。

- [ ] **Step 6: 提交封面与元数据增强**

```bash
git add data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/ability/BookMetadataEnhancementFacade.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailUiState.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailViewModel.kt \
  feature/reader/src/main/kotlin/com/wenwentome/reader/feature/reader/BookDetailScreen.kt \
  feature/library/src/main/kotlin/com/wenwentome/reader/feature/library/BookCoverCard.kt \
  data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/ability/BookMetadataEnhancementFacadeTest.kt \
  feature/reader/src/test/kotlin/com/wenwentome/reader/feature/reader/BookDetailViewModelTest.kt \
  feature/reader/src/testDebug/java/com/wenwentome/reader/feature/reader/BookDetailScreenTest.kt
git commit -m "feat: add cover and metadata enhancement flows"
```

## Task 9: 网文增强与书源健康策略

**Files:**
- Create: `data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/web/WebNovelEnhancementFacade.kt`
- Create: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/SourceHealthBadge.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverUiState.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModel.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverScreen.kt`
- Modify: `feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/RefreshRemoteBookUseCase.kt`
- Test: `data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/web/WebNovelEnhancementFacadeTest.kt`
- Test: `feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModelTest.kt`
- Test: `feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/RefreshRemoteBookUseCaseTest.kt`
- Test: `feature/discover/src/testDebug/java/com/wenwentome/reader/feature/discover/DiscoverScreenTest.kt`

- [ ] **Step 1: 写失败测试，锁定智能搜索增强、书源健康分和自动切源**

```kotlin
@Test
fun searchRanksResultsByHealthScoreAndApiBoost() = runTest {
    viewModel.search("雪中悍刀行")
    advanceUntilIdle()

    assertEquals("source-fast", viewModel.uiState.value.results.first().sourceId)
    assertTrue(viewModel.uiState.value.results.first().healthScore > 0.8f)
}
```

```kotlin
@Test
fun refreshRemoteBook_switchesSourceWhenPrimarySourceFails() = runTest {
    val result = useCase("book-1")
    assertEquals("backup-source", result.activeSourceId)
    assertTrue(result.hasUpdates)
}
```

- [ ] **Step 2: 运行 discover/web 测试，确认当前没有健康评分和切源策略**

Run: `./gradlew :data:apihub:test :feature:discover:testDebugUnitTest`
Expected: FAIL，缺少健康分、自动切源、更新提醒增强或 UI badge。

- [ ] **Step 3: 实现网文增强 facade 与切源策略**

```kotlin
data class SourceHealthSnapshot(
    val sourceId: String,
    val successRate: Float,
    val medianLatencyMs: Long,
    val lastFailureAt: Long?,
)
```

```kotlin
fun rank(results: List<RemoteSearchResult>, health: Map<String, SourceHealthSnapshot>) =
    results.sortedByDescending { health[it.sourceId]?.successRate ?: 0f }
```

- [ ] **Step 4: 在发现页展示健康分、增强提示与自动切源结果**

```kotlin
SourceHealthBadge(
    score = result.healthScore,
    status = result.healthLabel,
)
```

- [ ] **Step 5: 跑通 web 增强相关测试**

Run: `./gradlew :data:apihub:test :feature:discover:testDebugUnitTest`
Expected: PASS，发现页能展示健康分，刷新目录失败时可自动切到候选书源并记录更新状态。

- [ ] **Step 6: 提交网文增强**

```bash
git add data/apihub/src/main/kotlin/com/wenwentome/reader/data/apihub/web/WebNovelEnhancementFacade.kt \
  feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/SourceHealthBadge.kt \
  feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverUiState.kt \
  feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModel.kt \
  feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/DiscoverScreen.kt \
  feature/discover/src/main/kotlin/com/wenwentome/reader/feature/discover/RefreshRemoteBookUseCase.kt \
  data/apihub/src/test/kotlin/com/wenwentome/reader/data/apihub/web/WebNovelEnhancementFacadeTest.kt \
  feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/DiscoverViewModelTest.kt \
  feature/discover/src/test/kotlin/com/wenwentome/reader/feature/discover/RefreshRemoteBookUseCaseTest.kt \
  feature/discover/src/testDebug/java/com/wenwentome/reader/feature/discover/DiscoverScreenTest.kt
git commit -m "feat: add web novel enhancement and source health flows"
```

## Task 10: 云端验证、1.2 元数据与后续路线文档

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `CHANGELOG.md`
- Modify: `app/src/main/assets/changelog.json`
- Modify: `README.md`
- Modify: `site/index.html`
- Modify: `site/styles.css`
- Modify: `docs/development-progress.md`
- Modify: `docs/development-handbook.md`
- Modify: `docs/release-process.md`
- Modify: `docs/iteration-rules.md`
- Create: `docs/api-hub-roadmap.md`
- Verify: `.github/workflows/android-ci.yml`
- Verify: `.github/workflows/android-release.yml`

- [ ] **Step 1: 更新版本元数据到 `1.2 / 120`，并把本轮能力写入 changelog 与 README**

```kotlin
versionCode = 120
versionName = "1.2"
```

- [ ] **Step 2: 补开发文档与后续路线**

```markdown
# API Hub Roadmap

## 已锁定的后续能力
- 阅读问答
- 多语言翻译批处理
- 远端 TTS 缓存与离线回放
- 封面风格生成
- 元数据批量整理
- 书源健康仪表板
- 自动切源策略面板
```

- [ ] **Step 3: 跑 release metadata 校验脚本**

Run: `python3 scripts/release_metadata.py validate-pack app/build.gradle.kts CHANGELOG.md app/src/main/assets/changelog.json README.md`
Expected: PASS，`versionName`、`versionCode`、README、CHANGELOG、App 内 changelog 全部一致。

- [ ] **Step 4: 触发云端 CI 并观察 run**

Run: `gh run list --repo yuelangmanle/wenwentome1 --branch release/android-reader-v1 --limit 5`
Expected: 能看到当前分支最近的 `android-ci` 记录；若最新提交尚未推上 GitHub，先在已登录的 GitHub 环境完成推送后再继续这一步。

- [ ] **Step 5: 下载最新 debug APK artifact 进行留档**

Run: `gh run download <run-id> --repo yuelangmanle/wenwentome1 --name debug-apk --dir .artifacts/run-<run-id>`
Expected: 得到 `.artifacts/run-<run-id>/app-debug.apk`。

- [ ] **Step 6: 提交版本、文档与发布页**

```bash
git add app/build.gradle.kts CHANGELOG.md app/src/main/assets/changelog.json README.md \
  site/index.html site/styles.css \
  docs/development-progress.md docs/development-handbook.md docs/release-process.md \
  docs/iteration-rules.md docs/api-hub-roadmap.md
git commit -m "chore: prepare api hub reader enhancements release 1.2"
```

## Verification Checklist

- [ ] `:data:apihub:test`
- [ ] `:feature:apihub:testDebugUnitTest`
- [ ] `:feature:reader:testDebugUnitTest`
- [ ] `:feature:library:testDebugUnitTest`
- [ ] `:feature:discover:testDebugUnitTest`
- [ ] `:feature:settings:testDebugUnitTest`
- [ ] `:sync:github:test`
- [ ] `:app:testDebugUnitTest`
- [ ] `python3 scripts/release_metadata.py validate-pack app/build.gradle.kts CHANGELOG.md app/src/main/assets/changelog.json README.md`
- [ ] GitHub Actions `android-ci` 绿色

## Notes For Implementers

- 不要把 GitHub bootstrap token 再写回远端 snapshot 或普通 DataStore 明文字段。
- `SYNC_ENCRYPTED` secret 的恢复必须允许“先恢复非 secret，再输入同步密码解锁剩余 secret”的流程。
- 预算判断依赖价格元数据，价格表需要允许手动覆盖；不要把 provider 价格硬编码成不可更新常量。
- 若 provider 返回 `429 / 5xx / model_not_found`，fallback 日志必须落库，避免后续排障时只有 UI toast 没有记录。
- 阅读体验改动必须优先保证 `TXT / EPUB / WEB` 三条链路继续阅读不回退。
- 后续能力列表以 `docs/api-hub-roadmap.md` 为主，不要把产品记忆只留在对话上下文里。
