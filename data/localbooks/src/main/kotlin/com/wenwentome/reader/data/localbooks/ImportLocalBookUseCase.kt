package com.wenwentome.reader.data.localbooks

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

class ImportLocalBookUseCase(
    private val contentResolver: ContentResolver,
    private val repository: LocalBookImportRepository,
) {
    suspend operator fun invoke(uri: Uri) {
        val displayName = queryDisplayName(contentResolver, uri)
        contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "Cannot open input stream for $uri" }
            repository.import(fileName = displayName, inputStream = stream)
        }
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

