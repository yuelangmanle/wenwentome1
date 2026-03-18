package com.wenwentome.reader.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.core.database.entity.BookRecordEntity
import com.wenwentome.reader.core.database.entity.ReadingStateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ReaderDatabaseTest {
    @Test
    fun upsertBookAndReadingState_areReturnedTogether() = runTest {
        val database = testDatabase()
        database.bookRecordDao().upsert(BookRecordEntity(id = "book-1", title = "三体"))
        database.readingStateDao()
            .upsert(
                ReadingStateEntity(
                    bookId = "book-1",
                    locator = "cfi(/6/2)",
                    progressPercent = 0.42f,
                ),
            )

        val book = database.bookRecordDao().observeById("book-1").first()
        val state = database.readingStateDao().observeByBookId("book-1").first()

        assertEquals("三体", book?.title)
        assertEquals(0.42f, state?.progressPercent)
    }

    private fun testDatabase(): ReaderDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReaderDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
}

