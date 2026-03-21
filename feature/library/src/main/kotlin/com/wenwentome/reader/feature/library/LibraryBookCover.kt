package com.wenwentome.reader.feature.library

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Library feature 内部使用的封面 helper。
 * 暂时明确留在 feature/library，避免过早抽成跨模块图片层。
 */
@Composable
internal fun LibraryBookCover(
    title: String,
    coverUri: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    elevation: Dp = 12.dp,
    realCoverTag: String? = null,
    placeholderTag: String? = null,
    placeholderContent: @Composable BoxScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val coverBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, coverUri) {
        value = loadReadableCoverBitmap(context, coverUri)
    }

    Box(
        modifier = modifier.shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color(0x3328150A),
            spotColor = Color(0x3328150A),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Color(0xFFF3E7DA)),
        ) {
            if (coverBitmap != null) {
                Image(
                    bitmap = requireNotNull(coverBitmap),
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (realCoverTag != null) Modifier.testTag(realCoverTag) else Modifier),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (placeholderTag != null) Modifier.testTag(placeholderTag) else Modifier)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFC98C59),
                                    Color(0xFF8D5732),
                                    Color(0xFF5E351E),
                                ),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    content = placeholderContent,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(16.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color(0xFFE1C3A4).copy(alpha = 0.65f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.Transparent,
                                Color(0xFF2A1509).copy(alpha = 0.18f),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.18f),
                        shape = shape,
                    ),
            )
        }
    }
}

private suspend fun loadReadableCoverBitmap(
    context: Context,
    coverUri: String?,
): androidx.compose.ui.graphics.ImageBitmap? =
    withContext(Dispatchers.IO) {
        if (coverUri.isNullOrBlank()) return@withContext null
        LibraryBookCoverBitmapCache.get(coverUri)?.let { return@withContext it }

        val decodeLock = LibraryBookCoverBitmapCache.lockFor(coverUri)
        try {
            synchronized(decodeLock) {
                LibraryBookCoverBitmapCache.get(coverUri)?.let { return@synchronized it }

                val stream = when {
                    coverUri.startsWith("content://") ->
                        context.contentResolver.openInputStream(Uri.parse(coverUri))

                    coverUri.startsWith("file:") ->
                        runCatching { File(URI(coverUri)).inputStream() }.getOrNull()

                    else ->
                        runCatching { File(coverUri).takeIf { it.canRead() }?.inputStream() }.getOrNull()
                } ?: return@synchronized null

                val bitmap = stream.use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
                if (bitmap != null) {
                    LibraryBookCoverBitmapCache.put(coverUri, bitmap)
                }
                bitmap
            }
        } finally {
            LibraryBookCoverBitmapCache.unlock(coverUri, decodeLock)
        }
    }

private object LibraryBookCoverBitmapCache {
    private const val maxEntries = 24
    private val bitmaps =
        object : LinkedHashMap<String, androidx.compose.ui.graphics.ImageBitmap>(maxEntries, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, androidx.compose.ui.graphics.ImageBitmap>?,
            ): Boolean = size > maxEntries
        }
    private val decodeLocks = ConcurrentHashMap<String, Any>()

    fun get(coverUri: String): androidx.compose.ui.graphics.ImageBitmap? =
        synchronized(bitmaps) { bitmaps[coverUri] }

    fun put(
        coverUri: String,
        bitmap: androidx.compose.ui.graphics.ImageBitmap,
    ) {
        synchronized(bitmaps) {
            bitmaps[coverUri] = bitmap
        }
    }

    fun lockFor(coverUri: String): Any = decodeLocks.computeIfAbsent(coverUri) { Any() }

    fun unlock(
        coverUri: String,
        lock: Any,
    ) {
        decodeLocks.remove(coverUri, lock)
    }
}
