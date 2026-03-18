package com.wenwentome.reader.navigation

enum class TopLevelDestination(
    val route: String,
    val label: String,
) {
    BOOKSHELF(route = "bookshelf", label = "书库"),
    DISCOVER(route = "discover", label = "发现"),
    SETTINGS(route = "settings", label = "我的"),
}
