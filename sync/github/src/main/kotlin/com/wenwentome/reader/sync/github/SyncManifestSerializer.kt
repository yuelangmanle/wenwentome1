package com.wenwentome.reader.sync.github

import com.wenwentome.reader.core.model.BookAsset
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.ReadingBookmark
import com.wenwentome.reader.core.model.ReadingState
import com.wenwentome.reader.core.model.RemoteBinding
import com.wenwentome.reader.core.model.SourceDefinition
import com.wenwentome.reader.core.model.SyncSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SyncManifestSerializer {
    private val json = Json { ignoreUnknownKeys = true }

    fun encodeManifest(manifest: SyncManifest): String =
        buildJsonObject {
            put("snapshotPath", JsonPrimitive(manifest.snapshotPath))
            put("bookRecordsPath", JsonPrimitive(manifest.bookRecordsPath))
            put("readingStatesPath", JsonPrimitive(manifest.readingStatesPath))
            put("remoteBindingsPath", JsonPrimitive(manifest.remoteBindingsPath))
            put("sourceDefinitionsPath", JsonPrimitive(manifest.sourceDefinitionsPath))
            put("preferencesPath", JsonPrimitive(manifest.preferencesPath))
            put("secretEnvelopesPath", JsonPrimitive(manifest.secretEnvelopesPath))
            put("assetIndexPath", JsonPrimitive(manifest.assetIndexPath))
        }.toString()

    fun decodeManifest(raw: String): SyncManifest {
        val root = json.parseToJsonElement(raw).jsonObject
        return SyncManifest(
            snapshotPath = root.requiredString("snapshotPath"),
            bookRecordsPath = root.requiredString("bookRecordsPath"),
            readingStatesPath = root.requiredString("readingStatesPath"),
            remoteBindingsPath = root.requiredString("remoteBindingsPath"),
            sourceDefinitionsPath = root.requiredString("sourceDefinitionsPath"),
            preferencesPath = root.requiredString("preferencesPath"),
            secretEnvelopesPath = root.optionalString("secretEnvelopesPath") ?: SyncManifest().secretEnvelopesPath,
            assetIndexPath = root.requiredString("assetIndexPath"),
        )
    }

    fun encodeSnapshot(snapshot: SyncSnapshot): String =
        buildJsonObject {
            put("snapshotId", JsonPrimitive(snapshot.snapshotId))
            put("revision", JsonPrimitive(snapshot.revision))
            put("deviceId", JsonPrimitive(snapshot.deviceId))
            put("mergedAt", JsonPrimitive(snapshot.mergedAt))
            put("manifestJson", JsonPrimitive(snapshot.manifestJson))
        }.toString()

    fun decodeSnapshot(raw: String): SyncSnapshot {
        val root = json.parseToJsonElement(raw).jsonObject
        return SyncSnapshot(
            snapshotId = root.requiredString("snapshotId"),
            revision = root.requiredString("revision"),
            deviceId = root.requiredString("deviceId"),
            mergedAt = root.requiredLong("mergedAt"),
            manifestJson = root.requiredString("manifestJson"),
        )
    }

    fun encodeBookRecords(records: List<BookRecord>): String =
        records.mapToJsonArray { record ->
            buildJsonObject {
                put("id", JsonPrimitive(record.id))
                put("title", JsonPrimitive(record.title))
                put("author", optionalString(record.author))
                put("originType", JsonPrimitive(record.originType.name))
                put("primaryFormat", JsonPrimitive(record.primaryFormat.name))
                put("cover", optionalString(record.cover))
                put("summary", optionalString(record.summary))
                put("bookshelfState", JsonPrimitive(record.bookshelfState.name))
                put("createdAt", JsonPrimitive(record.createdAt))
                put("updatedAt", JsonPrimitive(record.updatedAt))
            }
        }

    fun decodeBookRecords(raw: String): List<BookRecord> =
        json.parseToJsonElement(raw).jsonArray.map { element ->
            val root = element.jsonObject
            BookRecord(
                id = root.requiredString("id"),
                title = root.requiredString("title"),
                author = root.optionalString("author"),
                originType = enumValueOf(root.requiredString("originType")),
                primaryFormat = enumValueOf(root.requiredString("primaryFormat")),
                cover = root.optionalString("cover"),
                summary = root.optionalString("summary"),
                bookshelfState = enumValueOf(root.requiredString("bookshelfState")),
                createdAt = root.requiredLong("createdAt"),
                updatedAt = root.requiredLong("updatedAt"),
            )
        }

    fun encodeReadingStates(states: List<ReadingState>): String =
        states.mapToJsonArray { state ->
            buildJsonObject {
                put("bookId", JsonPrimitive(state.bookId))
                put("locator", optionalString(state.locator))
                put("chapterRef", optionalString(state.chapterRef))
                put("progressPercent", JsonPrimitive(state.progressPercent))
                put(
                    "bookmarks",
                    buildJsonArray {
                        state.bookmarks.forEach { bookmark ->
                            add(
                                buildJsonObject {
                                    put("chapterRef", optionalString(bookmark.chapterRef))
                                    put("locator", JsonPrimitive(bookmark.locator))
                                    put("label", JsonPrimitive(bookmark.label))
                                }
                            )
                        }
                    }
                )
                put(
                    "notes",
                    buildJsonArray {
                        state.notes.forEach { note ->
                            add(JsonPrimitive(note))
                        }
                    }
                )
                put("updatedAt", JsonPrimitive(state.updatedAt))
            }
        }

    fun decodeReadingStates(raw: String): List<ReadingState> =
        json.parseToJsonElement(raw).jsonArray.map { element ->
            val root = element.jsonObject
            ReadingState(
                bookId = root.requiredString("bookId"),
                locator = root.optionalString("locator"),
                chapterRef = root.optionalString("chapterRef"),
                progressPercent = root.requiredFloat("progressPercent"),
                bookmarks = root["bookmarks"]?.jsonArray.orEmpty().map { bookmarkElement ->
                    val bookmark = bookmarkElement.jsonObject
                    ReadingBookmark(
                        chapterRef = bookmark.optionalString("chapterRef"),
                        locator = bookmark.requiredString("locator"),
                        label = bookmark.requiredString("label"),
                    )
                },
                notes = root["notes"]?.jsonArray.orEmpty().map { note ->
                    requireNotNull(note.jsonPrimitive.contentOrNull) { "notes entry must be a string" }
                },
                updatedAt = root.requiredLong("updatedAt"),
            )
        }

    fun encodeRemoteBindings(bindings: List<RemoteBinding>): String =
        bindings.mapToJsonArray { binding ->
            buildJsonObject {
                put("bookId", JsonPrimitive(binding.bookId))
                put("sourceId", JsonPrimitive(binding.sourceId))
                put("remoteBookId", JsonPrimitive(binding.remoteBookId))
                put("remoteBookUrl", JsonPrimitive(binding.remoteBookUrl))
                put("tocRef", optionalString(binding.tocRef))
                put("syncMode", JsonPrimitive(binding.syncMode.name))
                put("lastFetchedAt", optionalLong(binding.lastFetchedAt))
            }
        }

    fun decodeRemoteBindings(raw: String): List<RemoteBinding> =
        json.parseToJsonElement(raw).jsonArray.map { element ->
            val root = element.jsonObject
            RemoteBinding(
                bookId = root.requiredString("bookId"),
                sourceId = root.requiredString("sourceId"),
                remoteBookId = root.requiredString("remoteBookId"),
                remoteBookUrl = root.requiredString("remoteBookUrl"),
                tocRef = root.optionalString("tocRef"),
                syncMode = enumValueOf(root.requiredString("syncMode")),
                lastFetchedAt = root.optionalLong("lastFetchedAt"),
            )
        }

    fun encodeSourceDefinitions(definitions: List<SourceDefinition>): String =
        definitions.mapToJsonArray { definition ->
            buildJsonObject {
                put("sourceId", JsonPrimitive(definition.sourceId))
                put("sourceName", JsonPrimitive(definition.sourceName))
                put("sourceType", JsonPrimitive(definition.sourceType.name))
                put("ruleFormat", JsonPrimitive(definition.ruleFormat.name))
                put("sourceUrl", optionalString(definition.sourceUrl))
                put("rawDefinition", optionalString(definition.rawDefinition))
                put("authState", optionalString(definition.authState))
                put("enabled", JsonPrimitive(definition.enabled))
                put("group", optionalString(definition.group))
            }
        }

    fun decodeSourceDefinitions(raw: String): List<SourceDefinition> =
        json.parseToJsonElement(raw).jsonArray.map { element ->
            val root = element.jsonObject
            SourceDefinition(
                sourceId = root.requiredString("sourceId"),
                sourceName = root.requiredString("sourceName"),
                sourceType = enumValueOf(root.requiredString("sourceType")),
                ruleFormat = enumValueOf(root.requiredString("ruleFormat")),
                sourceUrl = root.optionalString("sourceUrl"),
                rawDefinition = root.optionalString("rawDefinition"),
                authState = root.optionalString("authState"),
                enabled = root.requiredBoolean("enabled"),
                group = root.optionalString("group"),
            )
        }

    fun encodePreferences(snapshot: PreferencesSnapshot): String =
        buildJsonObject {
            put("owner", JsonPrimitive(snapshot.owner))
            put("repo", JsonPrimitive(snapshot.repo))
            put("branch", JsonPrimitive(snapshot.branch))
            put("deviceId", JsonPrimitive(snapshot.deviceId))
        }.toString()

    fun decodePreferences(raw: String): PreferencesSnapshot {
        val root = json.parseToJsonElement(raw).jsonObject
        return PreferencesSnapshot(
            owner = root.requiredString("owner"),
            repo = root.requiredString("repo"),
            branch = root.requiredString("branch"),
            deviceId = root.requiredString("deviceId"),
            bootstrapToken = root.optionalString("bootstrapToken") ?: root.optionalString("token").orEmpty(),
        )
    }

    fun encodeSecretEnvelopes(secretEnvelopes: List<SecretEnvelopePayload>): String =
        secretEnvelopes.mapToJsonArray { envelope ->
            buildJsonObject {
                put("secretId", JsonPrimitive(envelope.secretId))
                put("scope", JsonPrimitive(envelope.scope.name))
                put("version", JsonPrimitive(envelope.version))
                put("kdf", JsonPrimitive(envelope.kdf))
                put("iterations", JsonPrimitive(envelope.iterations))
                put("saltBase64", JsonPrimitive(envelope.saltBase64))
                put("ivBase64", JsonPrimitive(envelope.ivBase64))
                put("cipherTextBase64", JsonPrimitive(envelope.cipherTextBase64))
                put("checksumBase64", JsonPrimitive(envelope.checksumBase64))
                put("updatedAt", JsonPrimitive(envelope.updatedAt))
            }
        }

    fun decodeSecretEnvelopes(raw: String): List<SecretEnvelopePayload> =
        json.parseToJsonElement(raw).jsonArray.map { element ->
            val root = element.jsonObject
            SecretEnvelopePayload(
                secretId = root.requiredString("secretId"),
                scope = enumValueOf(root.requiredString("scope")),
                version = root.requiredLong("version").toInt(),
                kdf = root.requiredString("kdf"),
                iterations = root.requiredLong("iterations").toInt(),
                saltBase64 = root.requiredString("saltBase64"),
                ivBase64 = root.requiredString("ivBase64"),
                cipherTextBase64 = root.requiredString("cipherTextBase64"),
                checksumBase64 = root.requiredString("checksumBase64"),
                updatedAt = root.requiredLong("updatedAt"),
            )
        }

    fun encodeAssets(assets: List<BookAsset>): String =
        assets.mapToJsonArray { asset ->
            buildJsonObject {
                put("bookId", JsonPrimitive(asset.bookId))
                put("assetRole", JsonPrimitive(asset.assetRole.name))
                put("storageUri", JsonPrimitive(asset.storageUri))
                put("mime", JsonPrimitive(asset.mime))
                put("size", JsonPrimitive(asset.size))
                put("hash", JsonPrimitive(asset.hash))
                put("syncPath", JsonPrimitive(asset.syncPath))
            }
        }

    fun decodeAssets(raw: String): List<BookAsset> =
        json.parseToJsonElement(raw).jsonArray.map { element ->
            val root = element.jsonObject
            BookAsset(
                bookId = root.requiredString("bookId"),
                assetRole = enumValueOf(root.requiredString("assetRole")),
                storageUri = root.requiredString("storageUri"),
                mime = root.requiredString("mime"),
                size = root.requiredLong("size"),
                hash = root.requiredString("hash"),
                syncPath = root.requiredString("syncPath"),
            )
        }

    private fun <T> List<T>.mapToJsonArray(transform: (T) -> JsonObject): String =
        buildJsonArray {
            forEach { item -> add(transform(item)) }
        }.toString()

    private fun optionalString(value: String?): JsonElement =
        value?.let(::JsonPrimitive) ?: JsonPrimitive(null as String?)

    private fun optionalLong(value: Long?): JsonElement =
        value?.let(::JsonPrimitive) ?: JsonPrimitive(null as String?)

    private fun JsonObject.requiredString(key: String): String =
        requireNotNull(getValue(key).jsonPrimitive.contentOrNull) { "$key must be a string" }

    private fun JsonObject.optionalString(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.requiredLong(key: String): Long =
        requiredString(key).toLong()

    private fun JsonObject.optionalLong(key: String): Long? =
        optionalString(key)?.toLongOrNull()

    private fun JsonObject.requiredFloat(key: String): Float =
        requiredString(key).toFloat()

    private fun JsonObject.requiredBoolean(key: String): Boolean =
        getValue(key).jsonPrimitive.boolean
}
