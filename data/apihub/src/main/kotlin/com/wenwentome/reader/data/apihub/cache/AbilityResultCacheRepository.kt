package com.wenwentome.reader.data.apihub.cache

import com.wenwentome.reader.core.model.ApiAbilityCacheEntry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

data class AbilityCacheLookup<T>(
    val value: T,
    val hit: Boolean,
)

interface AbilityCacheStore {
    suspend fun find(cacheKey: String): ApiAbilityCacheEntry?

    suspend fun upsert(entry: ApiAbilityCacheEntry)

    suspend fun deleteExpired(expiredAt: Long)
}

class AbilityResultCacheRepository(
    private val store: AbilityCacheStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun <T> getOrPut(
        cacheKey: String,
        serializer: KSerializer<T>,
        ttlMs: Long = DEFAULT_ABILITY_CACHE_TTL_MS,
        producer: suspend () -> T,
    ): AbilityCacheLookup<T> {
        val now = nowProvider()
        store.deleteExpired(now)
        val cached = store.find(cacheKey)
        if (cached != null && cached.expiresAt > now) {
            return AbilityCacheLookup(
                value = json.decodeFromString(serializer, cached.payloadJson),
                hit = true,
            )
        }

        val value = producer()
        store.upsert(
            ApiAbilityCacheEntry(
                cacheKey = cacheKey,
                payloadJson = json.encodeToString(serializer, value),
                expiresAt = now + ttlMs,
                updatedAt = now,
            )
        )
        return AbilityCacheLookup(value = value, hit = false)
    }

    companion object {
        const val DEFAULT_ABILITY_CACHE_TTL_MS: Long = 6 * 60 * 60 * 1000L
    }
}
