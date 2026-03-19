package com.wenwentome.reader.sync.github

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Base64

data class GitHubContentEnvelope(
    val content: String? = null,
    val sha: String? = null,
    val downloadUrl: String? = null,
)

class GitHubContentApi(
    baseUrl: String,
    private val client: OkHttpClient = OkHttpClient(),
) {
    private val baseUrl = baseUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun putJson(auth: GitHubAuthConfig, path: String, json: String, sha: String? = null): String =
        putFile(auth, path, json.encodeToByteArray(), sha)

    suspend fun putBinary(auth: GitHubAuthConfig, path: String, bytes: ByteArray, sha: String? = null): String =
        putFile(auth, path, bytes, sha)

    suspend fun getJson(auth: GitHubAuthConfig, path: String): Pair<String, String?> {
        val envelope = getEnvelope(auth, path)
        val decoded = Base64.getDecoder()
            .decode(requireNotNull(envelope.content).replace("\n", ""))
            .decodeToString()
        return decoded to envelope.sha
    }

    suspend fun getBinary(auth: GitHubAuthConfig, path: String): Pair<ByteArray, String?> {
        val envelope = getEnvelope(auth, path)
        val bytes = envelope.content?.let {
            Base64.getDecoder().decode(it.replace("\n", ""))
        } ?: executeBinaryGet(requireNotNull(envelope.downloadUrl), auth.token)
        return bytes to envelope.sha
    }

    suspend fun findShaOrNull(auth: GitHubAuthConfig, path: String): String? =
        try {
            getEnvelope(auth, path).sha
        } catch (error: GitHubHttpException) {
            if (error.code == 404) {
                null
            } else {
                throw error
            }
        }

    private suspend fun putFile(auth: GitHubAuthConfig, path: String, bytes: ByteArray, sha: String?): String {
        val body = buildJsonObject {
            put("message", JsonPrimitive("sync: update $path"))
            put("content", JsonPrimitive(Base64.getEncoder().encodeToString(bytes)))
            sha?.let { put("sha", JsonPrimitive(it)) }
            put("branch", JsonPrimitive(auth.branch))
        }
        return executePut(contentsUrl(auth, path), auth.token, body).sha
            ?: error("GitHub put response missing sha for $path")
    }

    private suspend fun getEnvelope(auth: GitHubAuthConfig, path: String): GitHubContentEnvelope =
        executeGetEnvelope(contentsUrl(auth, path), auth.token)

    private fun contentsUrl(auth: GitHubAuthConfig, path: String): String =
        "$baseUrl/repos/${auth.owner}/${auth.repo}/contents/$path"

    private suspend fun executePut(url: String, token: String, body: JsonObject): GitHubContentEnvelope {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .put(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return request.executeEnvelope()
    }

    private suspend fun executeGetEnvelope(url: String, token: String): GitHubContentEnvelope {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .get()
            .build()
        return request.executeEnvelope()
    }

    private suspend fun executeBinaryGet(url: String, token: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw GitHubHttpException(response.code, response.body?.string().orEmpty())
            }
            return requireNotNull(response.body).bytes()
        }
    }

    private fun Request.executeEnvelope(): GitHubContentEnvelope =
        client.newCall(this).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw GitHubHttpException(response.code, body)
            }
            parseEnvelope(body)
        }

    private fun parseEnvelope(rawBody: String): GitHubContentEnvelope {
        val root = json.parseToJsonElement(rawBody).jsonObject
        val nestedContent = root["content"]?.let { element ->
            runCatching { element.jsonObject }.getOrNull()
        }
        return GitHubContentEnvelope(
            content = root["content"]?.let { element ->
                runCatching { element.jsonPrimitive.contentOrNull }.getOrNull()
            }
                ?: nestedContent?.get("content")?.jsonPrimitive?.contentOrNull,
            sha = root["sha"]?.jsonPrimitive?.contentOrNull
                ?: nestedContent?.get("sha")?.jsonPrimitive?.contentOrNull,
            downloadUrl = root["download_url"]?.jsonPrimitive?.contentOrNull
                ?: nestedContent?.get("download_url")?.jsonPrimitive?.contentOrNull,
        )
    }
}

class GitHubHttpException(
    val code: Int,
    body: String,
) : IOException("GitHub API error $code: $body")
