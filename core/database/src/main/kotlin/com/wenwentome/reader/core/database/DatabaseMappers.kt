package com.wenwentome.reader.core.database

import com.wenwentome.reader.core.database.entity.BookAssetEntity
import com.wenwentome.reader.core.database.entity.BookRecordEntity
import com.wenwentome.reader.core.database.entity.ReadingStateEntity
import com.wenwentome.reader.core.database.entity.RemoteBindingEntity
import com.wenwentome.reader.core.database.entity.SourceDefinitionEntity
import com.wenwentome.reader.core.model.BookAsset
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.ReadingState
import com.wenwentome.reader.core.model.RemoteBinding
import com.wenwentome.reader.core.model.SourceDefinition

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
    )

