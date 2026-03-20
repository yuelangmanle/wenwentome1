package com.wenwentome.reader.data.localbooks

import java.io.File
import java.io.InputStream
import java.net.URI

class LocalBookFileStore(
    private val filesDir: File,
) {
    fun persistOriginal(bookId: String, extension: String, bytes: ByteArray): String {
        return persistOriginal(
            bookId = bookId,
            baseName = "source",
            extension = extension,
            bytes = bytes,
        )
    }

    fun persistOriginal(bookId: String, baseName: String, extension: String, bytes: ByteArray): String {
        val bookDir = File(filesDir, "books/$bookId").apply { mkdirs() }
        val sanitizedBaseName = baseName
            .trim()
            .ifBlank { "source" }
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val target = File(bookDir, "$sanitizedBaseName.$extension")
        target.writeBytes(bytes)
        return target.toURI().toString()
    }

    fun open(storageUri: String): InputStream =
        File(URI(storageUri)).inputStream()

    fun deleteBook(bookId: String) {
        File(filesDir, "books/$bookId").deleteRecursively()
    }
}
