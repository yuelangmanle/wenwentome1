package com.wenwentome.reader.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.wenwentome.reader.core.database.ReaderDatabase
import com.wenwentome.reader.data.localbooks.EpubBookParser
import com.wenwentome.reader.data.localbooks.ImportLocalBookUseCase
import com.wenwentome.reader.data.localbooks.LocalBookFileStore
import com.wenwentome.reader.data.localbooks.LocalBookImportRepository
import com.wenwentome.reader.data.localbooks.TxtBookParser

class AppContainer(private val application: Application) {
    val appContext: Context = application

    val database: ReaderDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            ReaderDatabase::class.java,
            "reader.db",
        ).build()
    }

    val fileStore: LocalBookFileStore by lazy {
        LocalBookFileStore(filesDir = appContext.filesDir)
    }

    private val localBookImportRepository: LocalBookImportRepository by lazy {
        LocalBookImportRepository(
            txtParser = TxtBookParser(),
            epubParser = EpubBookParser(),
            fileStore = fileStore,
            bookRecordDao = database.bookRecordDao(),
            readingStateDao = database.readingStateDao(),
            bookAssetDao = database.bookAssetDao(),
        )
    }

    val importLocalBook: ImportLocalBookUseCase by lazy {
        ImportLocalBookUseCase(
            contentResolver = appContext.contentResolver,
            repository = localBookImportRepository,
        )
    }
}
