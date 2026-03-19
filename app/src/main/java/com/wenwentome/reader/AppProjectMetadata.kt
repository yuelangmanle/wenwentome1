package com.wenwentome.reader

import com.wenwentome.reader.feature.settings.AboutProjectInfo

private const val ProjectName = "WenwenToMe"
private const val AuthorName = "月亮满了"
private const val ProjectUrl = "https://github.com/yuelangmanle/wenwentome1"

fun appProjectInfo(): AboutProjectInfo =
    AboutProjectInfo(
        appName = ProjectName,
        authorName = AuthorName,
        versionName = BuildConfig.VERSION_NAME,
        projectUrl = ProjectUrl,
    )
