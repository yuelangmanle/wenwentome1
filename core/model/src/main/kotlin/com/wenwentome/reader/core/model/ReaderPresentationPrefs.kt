package com.wenwentome.reader.core.model

enum class ReaderTheme { PAPER, SEPIA, NIGHT }

data class ReaderPresentationPrefs(
    val theme: ReaderTheme = ReaderTheme.PAPER,
    val fontSizeSp: Int = 18,
    val lineHeightMultiplier: Float = 1.55f,
    val brightnessPercent: Int = 0,
)
