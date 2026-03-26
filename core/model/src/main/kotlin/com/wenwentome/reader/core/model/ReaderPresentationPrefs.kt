package com.wenwentome.reader.core.model

enum class ReaderTheme { PAPER, SEPIA, NIGHT }

data class ReaderPresentationPrefs(
    val theme: ReaderTheme = ReaderTheme.PAPER,
    val fontSizeSp: Float = 18f,
    val autoFitFontSize: Boolean = true,
    val lineHeightMultiplier: Float = 1.55f,
    val letterSpacingEm: Float = 0f,
    val paragraphSpacingEm: Float = 0.35f,
    val sidePaddingDp: Int = 18,
    val brightnessPercent: Int = 0,
    val fontFamilyKey: String = "system-serif",
    val backgroundPaletteKey: String = "paper",
)
