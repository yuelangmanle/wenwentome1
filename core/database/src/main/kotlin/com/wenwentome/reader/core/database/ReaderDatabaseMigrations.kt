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
