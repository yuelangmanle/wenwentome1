package com.wenwentome.reader.data.apihub.local

import android.content.SharedPreferences

interface ApiSecretLocalStore {
    suspend fun save(secretId: String, plainText: String)
    suspend fun read(secretId: String): String?
    suspend fun delete(secretId: String)
}

// Task 2 的本地占位实现；Task 3 再切到最终 secure backing。
class SharedPreferencesApiSecretLocalStore(
    private val preferences: SharedPreferences,
) : ApiSecretLocalStore {
    override suspend fun save(secretId: String, plainText: String) {
        check(preferences.edit().putString(secretId, plainText).commit()) {
            "Failed to persist api secret for key=$secretId"
        }
    }

    override suspend fun read(secretId: String): String? = preferences.getString(secretId, null)

    override suspend fun delete(secretId: String) {
        check(preferences.edit().remove(secretId).commit()) {
            "Failed to delete api secret for key=$secretId"
        }
    }
}
