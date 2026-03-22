package com.wenwentome.reader.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE remote_bindings ADD COLUMN latestKnownChapterRef TEXT",
            )
            database.execSQL(
                "ALTER TABLE remote_bindings ADD COLUMN lastCatalogRefreshAt INTEGER",
            )
        }
    }

val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS api_providers (
                    providerId TEXT NOT NULL,
                    displayName TEXT NOT NULL,
                    providerKind TEXT NOT NULL,
                    baseUrl TEXT,
                    apiStyle TEXT NOT NULL,
                    authScheme TEXT NOT NULL,
                    enabled INTEGER NOT NULL,
                    region TEXT,
                    organizationId TEXT,
                    defaultHeadersTemplate TEXT,
                    secretSyncMode TEXT NOT NULL,
                    modelSource TEXT NOT NULL,
                    supportsChat INTEGER NOT NULL,
                    supportsTts INTEGER NOT NULL,
                    supportsEmbeddings INTEGER NOT NULL,
                    supportsImage INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(providerId)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS api_models (
                    providerId TEXT NOT NULL,
                    modelId TEXT NOT NULL,
                    label TEXT NOT NULL,
                    capabilities TEXT NOT NULL,
                    costLevel TEXT NOT NULL,
                    contextWindow INTEGER,
                    supportsStreaming INTEGER NOT NULL,
                    inputPricePer1kMicros INTEGER,
                    outputPricePer1kMicros INTEGER,
                    requestPricePerCallMicros INTEGER,
                    voiceOptions TEXT NOT NULL,
                    maxRequestsPerMinute INTEGER,
                    source TEXT NOT NULL,
                    validationState TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(providerId, modelId)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS api_capability_bindings (
                    capabilityId TEXT NOT NULL,
                    primaryProviderId TEXT NOT NULL,
                    primaryModelId TEXT NOT NULL,
                    fallbackProviderId TEXT,
                    fallbackModelId TEXT,
                    enabled INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(capabilityId)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS api_budget_policies (
                    policyId TEXT NOT NULL,
                    dailyLimitMicros INTEGER,
                    monthlyLimitMicros INTEGER,
                    warnThreshold REAL NOT NULL,
                    requireConfirmAboveCostMicros INTEGER,
                    overBudgetAction TEXT NOT NULL,
                    fallbackToLowerCostModel INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(policyId)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS api_usage_logs (
                    callId TEXT NOT NULL,
                    capabilityId TEXT NOT NULL,
                    providerId TEXT NOT NULL,
                    modelId TEXT NOT NULL,
                    success INTEGER NOT NULL,
                    bookId TEXT,
                    chapterRef TEXT,
                    durationMs INTEGER,
                    estimatedCostMicros INTEGER NOT NULL,
                    errorMessage TEXT,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(callId)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS api_price_overrides (
                    providerId TEXT NOT NULL,
                    modelId TEXT NOT NULL,
                    inputPricePer1kMicros INTEGER,
                    outputPricePer1kMicros INTEGER,
                    requestPricePerCallMicros INTEGER,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(providerId, modelId)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS api_ability_cache (
                    cacheKey TEXT NOT NULL,
                    payloadJson TEXT NOT NULL,
                    expiresAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(cacheKey)
                )
                """.trimIndent(),
            )
        }
    }
