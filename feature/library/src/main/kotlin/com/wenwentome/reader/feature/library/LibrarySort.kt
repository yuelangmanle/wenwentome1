package com.wenwentome.reader.feature.library

enum class LibrarySort(
    val label: String,
) {
    LAST_READ_DESC("最近阅读"),
    TITLE_ASC("书名"),
    ;

    fun apply(items: List<LibraryBookItem>): List<LibraryBookItem> =
        when (this) {
            LAST_READ_DESC ->
                items.sortedWith(
                    compareByDescending<LibraryBookItem> { it.lastReadAt }
                        .thenBy { it.book.title.lowercase() }
                )

            TITLE_ASC -> items.sortedBy { it.book.title.lowercase() }
        }
}
