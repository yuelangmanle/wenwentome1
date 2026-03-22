package com.wenwentome.reader.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wenwentome.reader.core.database.dao.ApiAbilityCacheDao
import com.wenwentome.reader.core.database.dao.ApiBudgetPolicyDao
import com.wenwentome.reader.core.database.dao.ApiCapabilityBindingDao
import com.wenwentome.reader.core.database.dao.ApiModelDao
import com.wenwentome.reader.core.database.dao.ApiPriceOverrideDao
import com.wenwentome.reader.core.database.dao.ApiProviderDao
import com.wenwentome.reader.core.database.dao.ApiUsageLogDao
import com.wenwentome.reader.core.database.dao.BookAssetDao
import com.wenwentome.reader.core.database.dao.BookRecordDao
import com.wenwentome.reader.core.database.dao.ReadingStateDao
import com.wenwentome.reader.core.database.dao.RemoteBindingDao
import com.wenwentome.reader.core.database.dao.SourceDefinitionDao
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

@Database(
    entities = [
        BookRecordEntity::class,
        BookAssetEntity::class,
        ReadingStateEntity::class,
        SourceDefinitionEntity::class,
        RemoteBindingEntity::class,
        ApiProviderEntity::class,
        ApiModelEntity::class,
        ApiCapabilityBindingEntity::class,
        ApiBudgetPolicyEntity::class,
        ApiUsageLogEntity::class,
        ApiPriceOverrideEntity::class,
        ApiAbilityCacheEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class ReaderDatabase : RoomDatabase() {
    abstract fun apiProviderDao(): ApiProviderDao
    abstract fun apiModelDao(): ApiModelDao
    abstract fun apiCapabilityBindingDao(): ApiCapabilityBindingDao
    abstract fun apiBudgetPolicyDao(): ApiBudgetPolicyDao
    abstract fun apiUsageLogDao(): ApiUsageLogDao
    abstract fun apiPriceOverrideDao(): ApiPriceOverrideDao
    abstract fun apiAbilityCacheDao(): ApiAbilityCacheDao
    abstract fun bookRecordDao(): BookRecordDao
    abstract fun bookAssetDao(): BookAssetDao
    abstract fun readingStateDao(): ReadingStateDao
    abstract fun remoteBindingDao(): RemoteBindingDao
    abstract fun sourceDefinitionDao(): SourceDefinitionDao
}
