package com.wenwentome.reader.data.localbooks

import com.wenwentome.reader.core.model.AssetRole
import com.wenwentome.reader.core.model.BookFormat

class TxtBookParser {
    fun parse(name: String, bytes: ByteArray): ParsedLocalBook =
        ParsedLocalBook(
            title = name.removeSuffix(".txt"),
            author = null,
            format = BookFormat.TXT,
            assets = listOf(
                ParsedAsset(
                    assetRole = AssetRole.PRIMARY_TEXT,
                    bytes = bytes,
                    mime = "text/plain",
                    extension = "txt",
                )
            ),
        )
}

