package com.wenwentome.reader.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wenwentome.reader.core.database.dao.BookAssetDao
import com.wenwentome.reader.core.database.dao.BookRecordDao
import com.wenwentome.reader.core.database.dao.ReadingStateDao
import com.wenwentome.reader.core.database.dao.RemoteBindingDao
import com.wenwentome.reader.core.database.dao.SourceDefinitionDao
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
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class ReaderDatabase : RoomDatabase() {
    abstract fun bookRecordDao(): BookRecordDao
    abstract fun bookAssetDao(): BookAssetDao
    abstract fun readingStateDao(): ReadingStateDao
    abstract fun remoteBindingDao(): RemoteBindingDao
    abstract fun sourceDefinitionDao(): SourceDefinitionDao
}

