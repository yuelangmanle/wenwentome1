package com.wenwentome.reader.core.database

import com.wenwentome.reader.core.database.entity.ApiAbilityCacheEntity
import com.wenwentome.reader.core.database.entity.ApiBudgetPolicyEntity
import com.wenwentome.reader.core.database.entity.ApiCapabilityBindingEntity
import com.wenwentome.reader.core.database.entity.ApiModelEntity
import com.wenwentome.reader.core.database.entity.ApiPriceOverrideEntity
import com.wenwentome.reader.core.database.entity.ApiProviderEntity
import com.wenwentome.reader.core.database.entity.ApiUsageLogEntity
import com.wenwentome.reader.core.database.entity.BookAssetEntity
import com.wenwentome.reader.core.database.entity.BookRecordEntity
import com.wenwentome.reader.core.database.entity.ReadingStateEntity
import com.wenwentome.reader.core.database.entity.RemoteBindingEntity
import com.wenwentome.reader.core.database.entity.SourceDefinitionEntity
import com.wenwentome.reader.core.model.ApiAbilityCacheEntry
import com.wenwentome.reader.core.model.ApiBudgetPolicy
import com.wenwentome.reader.core.model.ApiCapabilityBinding
import com.wenwentome.reader.core.model.ApiModelProfile
import com.wenwentome.reader.core.model.ApiPriceOverride
import com.wenwentome.reader.core.model.ApiProviderProfile
import com.wenwentome.reader.core.model.ApiUsageLog
import com.wenwentome.reader.core.model.BookAsset
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.ReadingState
import com.wenwentome.reader.core.model.RemoteBinding
import com.wenwentome.reader.core.model.SourceDefinition

fun ApiProviderEntity.toModel(): ApiProviderProfile =
    ApiProviderProfile(
        providerId = providerId,
        displayName = displayName,
        providerKind = providerKind,
        baseUrl = baseUrl,
        apiStyle = apiStyle,
        authScheme = authScheme,
        enabled = enabled,
        region = region,
        organizationId = organizationId,
        defaultHeadersTemplate = defaultHeadersTemplate,
        secretSyncMode = secretSyncMode,
        modelSource = modelSource,
        supportsChat = supportsChat,
        supportsTts = supportsTts,
        supportsEmbeddings = supportsEmbeddings,
        supportsImage = supportsImage,
        updatedAt = updatedAt,
    )

fun ApiProviderProfile.toEntity(): ApiProviderEntity =
    ApiProviderEntity(
        providerId = providerId,
        displayName = displayName,
        providerKind = providerKind,
        baseUrl = baseUrl,
        apiStyle = apiStyle,
        authScheme = authScheme,
        enabled = enabled,
        region = region,
        organizationId = organizationId,
        defaultHeadersTemplate = defaultHeadersTemplate,
        secretSyncMode = secretSyncMode,
        modelSource = modelSource,
        supportsChat = supportsChat,
        supportsTts = supportsTts,
        supportsEmbeddings = supportsEmbeddings,
        supportsImage = supportsImage,
        updatedAt = updatedAt,
    )

fun ApiModelEntity.toModel(): ApiModelProfile =
    ApiModelProfile(
        providerId = providerId,
        modelId = modelId,
        label = label,
        capabilities = capabilities,
        costLevel = costLevel,
        contextWindow = contextWindow,
        supportsStreaming = supportsStreaming,
        inputPricePer1kMicros = inputPricePer1kMicros,
        outputPricePer1kMicros = outputPricePer1kMicros,
        requestPricePerCallMicros = requestPricePerCallMicros,
        voiceOptions = voiceOptions,
        maxRequestsPerMinute = maxRequestsPerMinute,
        source = source,
        validationState = validationState,
        updatedAt = updatedAt,
    )

fun ApiModelProfile.toEntity(): ApiModelEntity =
    ApiModelEntity(
        providerId = providerId,
        modelId = modelId,
        label = label,
        capabilities = capabilities,
        costLevel = costLevel,
        contextWindow = contextWindow,
        supportsStreaming = supportsStreaming,
        inputPricePer1kMicros = inputPricePer1kMicros,
        outputPricePer1kMicros = outputPricePer1kMicros,
        requestPricePerCallMicros = requestPricePerCallMicros,
        voiceOptions = voiceOptions,
        maxRequestsPerMinute = maxRequestsPerMinute,
        source = source,
        validationState = validationState,
        updatedAt = updatedAt,
    )

fun ApiCapabilityBindingEntity.toModel(): ApiCapabilityBinding =
    ApiCapabilityBinding(
        capabilityId = capabilityId,
        primaryProviderId = primaryProviderId,
        primaryModelId = primaryModelId,
        fallbackProviderId = fallbackProviderId,
        fallbackModelId = fallbackModelId,
        enabled = enabled,
        updatedAt = updatedAt,
    )

fun ApiCapabilityBinding.toEntity(): ApiCapabilityBindingEntity =
    ApiCapabilityBindingEntity(
        capabilityId = capabilityId,
        primaryProviderId = primaryProviderId,
        primaryModelId = primaryModelId,
        fallbackProviderId = fallbackProviderId,
        fallbackModelId = fallbackModelId,
        enabled = enabled,
        updatedAt = updatedAt,
    )

fun ApiBudgetPolicyEntity.toModel(): ApiBudgetPolicy =
    ApiBudgetPolicy(
        policyId = policyId,
        dailyLimitMicros = dailyLimitMicros,
        monthlyLimitMicros = monthlyLimitMicros,
        warnThreshold = warnThreshold,
        requireConfirmAboveCostMicros = requireConfirmAboveCostMicros,
        overBudgetAction = overBudgetAction,
        fallbackToLowerCostModel = fallbackToLowerCostModel,
        updatedAt = updatedAt,
    )

fun ApiBudgetPolicy.toEntity(): ApiBudgetPolicyEntity =
    ApiBudgetPolicyEntity(
        policyId = policyId,
        dailyLimitMicros = dailyLimitMicros,
        monthlyLimitMicros = monthlyLimitMicros,
        warnThreshold = warnThreshold,
        requireConfirmAboveCostMicros = requireConfirmAboveCostMicros,
        overBudgetAction = overBudgetAction,
        fallbackToLowerCostModel = fallbackToLowerCostModel,
        updatedAt = updatedAt,
    )

fun ApiUsageLogEntity.toModel(): ApiUsageLog =
    ApiUsageLog(
        callId = callId,
        capabilityId = capabilityId,
        providerId = providerId,
        modelId = modelId,
        success = success,
        usedFallback = usedFallback,
        bookId = bookId,
        chapterRef = chapterRef,
        durationMs = durationMs,
        estimatedCostMicros = estimatedCostMicros,
        errorMessage = errorMessage,
        createdAt = createdAt,
    )

fun ApiUsageLog.toEntity(): ApiUsageLogEntity =
    ApiUsageLogEntity(
        callId = callId,
        capabilityId = capabilityId,
        providerId = providerId,
        modelId = modelId,
        success = success,
        usedFallback = usedFallback,
        bookId = bookId,
        chapterRef = chapterRef,
        durationMs = durationMs,
        estimatedCostMicros = estimatedCostMicros,
        errorMessage = errorMessage,
        createdAt = createdAt,
    )

fun ApiPriceOverrideEntity.toModel(): ApiPriceOverride =
    ApiPriceOverride(
        providerId = providerId,
        modelId = modelId,
        inputPricePer1kMicros = inputPricePer1kMicros,
        outputPricePer1kMicros = outputPricePer1kMicros,
        requestPricePerCallMicros = requestPricePerCallMicros,
        updatedAt = updatedAt,
    )

fun ApiPriceOverride.toEntity(): ApiPriceOverrideEntity =
    ApiPriceOverrideEntity(
        providerId = providerId,
        modelId = modelId,
        inputPricePer1kMicros = inputPricePer1kMicros,
        outputPricePer1kMicros = outputPricePer1kMicros,
        requestPricePerCallMicros = requestPricePerCallMicros,
        updatedAt = updatedAt,
    )

fun ApiAbilityCacheEntity.toModel(): ApiAbilityCacheEntry =
    ApiAbilityCacheEntry(
        cacheKey = cacheKey,
        payloadJson = payloadJson,
        expiresAt = expiresAt,
        updatedAt = updatedAt,
    )

fun ApiAbilityCacheEntry.toEntity(): ApiAbilityCacheEntity =
    ApiAbilityCacheEntity(
        cacheKey = cacheKey,
        payloadJson = payloadJson,
        expiresAt = expiresAt,
        updatedAt = updatedAt,
    )

fun BookRecordEntity.toModel(): BookRecord =
    BookRecord(
        id = id,
        title = title,
        author = author,
        originType = originType,
        primaryFormat = primaryFormat,
        cover = cover,
        summary = summary,
        bookshelfState = bookshelfState,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun BookRecord.toEntity(): BookRecordEntity =
    BookRecordEntity(
        id = id,
        title = title,
        author = author,
        originType = originType,
        primaryFormat = primaryFormat,
        cover = cover,
        summary = summary,
        bookshelfState = bookshelfState,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun BookAssetEntity.toModel(): BookAsset =
    BookAsset(
        bookId = bookId,
        assetRole = assetRole,
        storageUri = storageUri,
        mime = mime,
        size = size,
        hash = hash,
        syncPath = syncPath,
    )

fun BookAsset.toEntity(): BookAssetEntity =
    BookAssetEntity(
        bookId = bookId,
        assetRole = assetRole,
        storageUri = storageUri,
        mime = mime,
        size = size,
        hash = hash,
        syncPath = syncPath,
    )

fun ReadingStateEntity.toModel(): ReadingState =
    ReadingState(
        bookId = bookId,
        locator = locator,
        chapterRef = chapterRef,
        progressPercent = progressPercent,
        bookmarks = bookmarks,
        notes = notes,
        updatedAt = updatedAt,
    )

fun ReadingState.toEntity(): ReadingStateEntity =
    ReadingStateEntity(
        bookId = bookId,
        locator = locator,
        chapterRef = chapterRef,
        progressPercent = progressPercent,
        bookmarks = bookmarks,
        notes = notes,
        updatedAt = updatedAt,
    )

fun SourceDefinitionEntity.toModel(): SourceDefinition =
    SourceDefinition(
        sourceId = sourceId,
        sourceName = sourceName,
        sourceType = sourceType,
        ruleFormat = ruleFormat,
        sourceUrl = sourceUrl,
        rawDefinition = rawDefinition,
        authState = authState,
        enabled = enabled,
        group = group,
    )

fun SourceDefinition.toEntity(): SourceDefinitionEntity =
    SourceDefinitionEntity(
        sourceId = sourceId,
        sourceName = sourceName,
        sourceType = sourceType,
        ruleFormat = ruleFormat,
        sourceUrl = sourceUrl,
        rawDefinition = rawDefinition,
        authState = authState,
        enabled = enabled,
        group = group,
    )

fun RemoteBindingEntity.toModel(): RemoteBinding =
    RemoteBinding(
        bookId = bookId,
        sourceId = sourceId,
        remoteBookId = remoteBookId,
        remoteBookUrl = remoteBookUrl,
        tocRef = tocRef,
        syncMode = syncMode,
        lastFetchedAt = lastFetchedAt,
        latestKnownChapterRef = latestKnownChapterRef,
        lastCatalogRefreshAt = lastCatalogRefreshAt,
    )

fun RemoteBinding.toEntity(): RemoteBindingEntity =
    RemoteBindingEntity(
        bookId = bookId,
        sourceId = sourceId,
        remoteBookId = remoteBookId,
        remoteBookUrl = remoteBookUrl,
        tocRef = tocRef,
        syncMode = syncMode,
        lastFetchedAt = lastFetchedAt,
        latestKnownChapterRef = latestKnownChapterRef,
        lastCatalogRefreshAt = lastCatalogRefreshAt,
    )
