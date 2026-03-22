package com.wenwentome.reader.core.database

import androidx.room.TypeConverter
import com.wenwentome.reader.core.model.ApiModelCostLevel
import com.wenwentome.reader.core.model.ApiModelValidationState
import com.wenwentome.reader.core.model.ApiOverBudgetAction
import com.wenwentome.reader.core.model.AssetRole
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookshelfState
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.ProviderAuthScheme
import com.wenwentome.reader.core.model.ProviderKind
import com.wenwentome.reader.core.model.ProviderModelSource
import com.wenwentome.reader.core.model.ProviderSecretSyncMode
import com.wenwentome.reader.core.model.ProviderTransportStyle
import com.wenwentome.reader.core.model.ReadingBookmark
import com.wenwentome.reader.core.model.RemoteSyncMode
import com.wenwentome.reader.core.model.RuleFormat
import com.wenwentome.reader.core.model.SourceType
import org.json.JSONArray
import org.json.JSONObject

class DatabaseConverters {
    @TypeConverter
    fun originTypeToString(value: OriginType): String = value.name

    @TypeConverter
    fun stringToOriginType(value: String): OriginType = OriginType.valueOf(value)

    @TypeConverter
    fun bookFormatToString(value: BookFormat): String = value.name

    @TypeConverter
    fun stringToBookFormat(value: String): BookFormat = BookFormat.valueOf(value)

    @TypeConverter
    fun bookshelfStateToString(value: BookshelfState): String = value.name

    @TypeConverter
    fun stringToBookshelfState(value: String): BookshelfState = BookshelfState.valueOf(value)

    @TypeConverter
    fun assetRoleToString(value: AssetRole): String = value.name

    @TypeConverter
    fun stringToAssetRole(value: String): AssetRole = AssetRole.valueOf(value)

    @TypeConverter
    fun sourceTypeToString(value: SourceType): String = value.name

    @TypeConverter
    fun stringToSourceType(value: String): SourceType = SourceType.valueOf(value)

    @TypeConverter
    fun ruleFormatToString(value: RuleFormat): String = value.name

    @TypeConverter
    fun stringToRuleFormat(value: String): RuleFormat = RuleFormat.valueOf(value)

    @TypeConverter
    fun remoteSyncModeToString(value: RemoteSyncMode): String = value.name

    @TypeConverter
    fun stringToRemoteSyncMode(value: String): RemoteSyncMode = RemoteSyncMode.valueOf(value)

    @TypeConverter
    fun providerKindToString(value: ProviderKind): String = value.name

    @TypeConverter
    fun stringToProviderKind(value: String): ProviderKind = ProviderKind.valueOf(value)

    @TypeConverter
    fun providerTransportStyleToString(value: ProviderTransportStyle): String = value.name

    @TypeConverter
    fun stringToProviderTransportStyle(value: String): ProviderTransportStyle =
        ProviderTransportStyle.valueOf(value)

    @TypeConverter
    fun providerAuthSchemeToString(value: ProviderAuthScheme): String = value.name

    @TypeConverter
    fun stringToProviderAuthScheme(value: String): ProviderAuthScheme = ProviderAuthScheme.valueOf(value)

    @TypeConverter
    fun providerSecretSyncModeToString(value: ProviderSecretSyncMode): String = value.name

    @TypeConverter
    fun stringToProviderSecretSyncMode(value: String): ProviderSecretSyncMode =
        ProviderSecretSyncMode.valueOf(value)

    @TypeConverter
    fun providerModelSourceToString(value: ProviderModelSource): String = value.name

    @TypeConverter
    fun stringToProviderModelSource(value: String): ProviderModelSource = ProviderModelSource.valueOf(value)

    @TypeConverter
    fun apiModelCostLevelToString(value: ApiModelCostLevel): String = value.name

    @TypeConverter
    fun stringToApiModelCostLevel(value: String): ApiModelCostLevel = ApiModelCostLevel.valueOf(value)

    @TypeConverter
    fun apiModelValidationStateToString(value: ApiModelValidationState): String = value.name

    @TypeConverter
    fun stringToApiModelValidationState(value: String): ApiModelValidationState =
        ApiModelValidationState.valueOf(value)

    @TypeConverter
    fun apiOverBudgetActionToString(value: ApiOverBudgetAction): String = value.name

    @TypeConverter
    fun stringToApiOverBudgetAction(value: String): ApiOverBudgetAction = ApiOverBudgetAction.valueOf(value)

    @TypeConverter
    fun readingBookmarksToJson(value: List<ReadingBookmark>): String {
        if (value.isEmpty()) return ""
        val array = JSONArray()
        value.forEach { bookmark ->
            val obj = JSONObject()
            obj.put("chapterRef", bookmark.chapterRef ?: JSONObject.NULL)
            obj.put("locator", bookmark.locator)
            obj.put("label", bookmark.label)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun jsonToReadingBookmarks(value: String): List<ReadingBookmark> {
        if (value.isBlank()) return emptyList()
        val array = JSONArray(value)
        return List(array.length()) { i ->
            val obj = array.optJSONObject(i) ?: JSONObject()
            val chapterRef =
                if (obj.has("chapterRef") && !obj.isNull("chapterRef")) obj.optString("chapterRef") else null
            ReadingBookmark(
                chapterRef = chapterRef,
                locator = obj.optString("locator"),
                label = obj.optString("label"),
            )
        }
    }

    @TypeConverter
    fun notesToJson(value: List<String>): String {
        if (value.isEmpty()) return ""
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun jsonToNotes(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        val array = JSONArray(value)
        return List(array.length()) { i -> array.optString(i) }
    }

}
