package com.wenwentome.reader.core.database

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderDatabaseMigrationTest {
    @Test
    fun migrate2To3_preservesRemoteBindingsRows_andKeepsNewColumnsNullable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB)

        val dbFile = context.getDatabasePath(TEST_DB)
        dbFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).apply {
            execSQL(
                "CREATE TABLE IF NOT EXISTS book_records (" +
                    "id TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "author TEXT, " +
                    "originType TEXT NOT NULL, " +
                    "primaryFormat TEXT NOT NULL, " +
                    "cover TEXT, " +
                    "summary TEXT, " +
                    "bookshelfState TEXT NOT NULL, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL, " +
                    "PRIMARY KEY(id)" +
                    ")",
            )
            execSQL(
                "CREATE TABLE IF NOT EXISTS book_assets (" +
                    "bookId TEXT NOT NULL, " +
                    "assetRole TEXT NOT NULL, " +
                    "storageUri TEXT NOT NULL, " +
                    "mime TEXT NOT NULL, " +
                    "size INTEGER NOT NULL, " +
                    "hash TEXT NOT NULL, " +
                    "syncPath TEXT NOT NULL, " +
                    "PRIMARY KEY(bookId, assetRole)" +
                    ")",
            )
            execSQL(
                "CREATE TABLE IF NOT EXISTS reading_states (" +
                    "bookId TEXT NOT NULL, " +
                    "locator TEXT, " +
                    "chapterRef TEXT, " +
                    "progressPercent REAL NOT NULL, " +
                    "bookmarks TEXT NOT NULL, " +
                    "notes TEXT NOT NULL, " +
                    "updatedAt INTEGER NOT NULL, " +
                    "PRIMARY KEY(bookId)" +
                    ")",
            )
            execSQL(
                "CREATE TABLE IF NOT EXISTS source_definitions (" +
                    "sourceId TEXT NOT NULL, " +
                    "sourceName TEXT NOT NULL, " +
                    "sourceType TEXT NOT NULL, " +
                    "ruleFormat TEXT NOT NULL, " +
                    "sourceUrl TEXT, " +
                    "rawDefinition TEXT, " +
                    "authState TEXT, " +
                    "enabled INTEGER NOT NULL, " +
                    "`group` TEXT, " +
                    "PRIMARY KEY(sourceId)" +
                    ")",
            )
            execSQL(
                "CREATE TABLE IF NOT EXISTS remote_bindings (" +
                    "bookId TEXT NOT NULL, " +
                    "sourceId TEXT NOT NULL, " +
                    "remoteBookId TEXT NOT NULL, " +
                    "remoteBookUrl TEXT NOT NULL, " +
                    "tocRef TEXT, " +
                    "syncMode TEXT NOT NULL, " +
                    "lastFetchedAt INTEGER, " +
                    "PRIMARY KEY(bookId)" +
                    ")",
            )
            execSQL(
                "INSERT INTO remote_bindings (bookId, sourceId, remoteBookId, remoteBookUrl, tocRef, syncMode, lastFetchedAt) " +
                    "VALUES ('book-1', 'src-1', 'remote-1', 'https://example.com/book/1', NULL, 'LATEST_ONLY', 11)",
            )
            version = 2
            close()
        }

        val migratedDb =
            Room.databaseBuilder(context, ReaderDatabase::class.java, TEST_DB)
                .addMigrations(MIGRATION_2_3)
                .build()

        migratedDb.openHelper.writableDatabase.query(
            "SELECT bookId, latestKnownChapterRef, lastCatalogRefreshAt FROM remote_bindings",
        ).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("book-1", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
        }

        migratedDb.close()
        context.deleteDatabase(TEST_DB)
    }

    companion object {
        private const val TEST_DB = "reader-migration-test"
    }
}
