package com.wenwentome.reader.data.localbooks

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImportLocalBookUseCase(
    private val contentResolver: ContentResolver,
    private val repository: LocalBookImportRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(uri: Uri) {
        invoke(listOf(uri))
    }

    suspend operator fun invoke(uris: List<Uri>): ImportedLocalBookBatch =
        withContext(ioDispatcher) {
            val requests = uris.map { uri ->
                LocalBookImportRequest(
                    fileName = queryDisplayName(contentResolver, uri),
                    openInputStream = {
                        requireNotNull(contentResolver.openInputStream(uri)) {
                            "Cannot open input stream for $uri"
                        }
                    },
                )
            }
            repository.importBatch(requests)
        }

    private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String =
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) cursor.getString(nameColumn) else (uri.lastPathSegment ?: "imported-book")
        } ?: (uri.lastPathSegment ?: "imported-book")
}
