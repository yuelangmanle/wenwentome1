package com.wenwentome.reader.data.localbooks

import java.io.File
import java.io.InputStream
import java.net.URI

class LocalBookFileStore(
    private val filesDir: File,
) {
    fun persistOriginal(bookId: String, extension: String, bytes: ByteArray): String {
        val bookDir = File(filesDir, "books/$bookId").apply { mkdirs() }
        val target = File(bookDir, "source.$extension")
        target.writeBytes(bytes)
        return target.toURI().toString()
    }

    fun open(storageUri: String): InputStream =
        File(URI(storageUri)).inputStream()
}

