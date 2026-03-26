package io.legado.app.ui.wenwen

import android.content.SharedPreferences
import androidx.core.content.edit

data class WenwenReaderPresentationPrefs(
    val fontSizeSp: Float = 20f,
    val lineHeightEm: Float = 1.75f,
    val letterSpacingEm: Float = 0.02f,
    val paragraphSpacingDp: Float = 14f,
    val paletteName: String = ReaderPalette.Paper.name,
) {
    fun sanitized(): WenwenReaderPresentationPrefs = copy(
        fontSizeSp = fontSizeSp.coerceIn(12f, 36f),
        lineHeightEm = lineHeightEm.coerceIn(1.2f, 2.4f),
        letterSpacingEm = letterSpacingEm.coerceIn(-0.05f, 0.20f),
        paragraphSpacingDp = paragraphSpacingDp.coerceIn(0f, 32f),
        paletteName = ReaderPalette.entries.firstOrNull { it.name == paletteName }?.name
            ?: ReaderPalette.Paper.name,
    )
}

object WenwenReaderPrefs {

    private const val KEY_FONT_SIZE_SP = "wenwen_reader_font_size_sp"
    private const val KEY_LINE_HEIGHT_EM = "wenwen_reader_line_height_em"
    private const val KEY_LETTER_SPACING_EM = "wenwen_reader_letter_spacing_em"
    private const val KEY_PARAGRAPH_SPACING_DP = "wenwen_reader_paragraph_spacing_dp"
    private const val KEY_PALETTE_NAME = "wenwen_reader_palette_name"

    fun load(prefs: SharedPreferences): WenwenReaderPresentationPrefs {
        return WenwenReaderPresentationPrefs(
            fontSizeSp = prefs.getFloat(KEY_FONT_SIZE_SP, 20f),
            lineHeightEm = prefs.getFloat(KEY_LINE_HEIGHT_EM, 1.75f),
            letterSpacingEm = prefs.getFloat(KEY_LETTER_SPACING_EM, 0.02f),
            paragraphSpacingDp = prefs.getFloat(KEY_PARAGRAPH_SPACING_DP, 14f),
            paletteName = prefs.getString(KEY_PALETTE_NAME, ReaderPalette.Paper.name)
                ?: ReaderPalette.Paper.name,
        ).sanitized()
    }

    fun save(
        prefs: SharedPreferences,
        value: WenwenReaderPresentationPrefs,
    ) {
        val sanitized = value.sanitized()
        prefs.edit {
            putFloat(KEY_FONT_SIZE_SP, sanitized.fontSizeSp)
            putFloat(KEY_LINE_HEIGHT_EM, sanitized.lineHeightEm)
            putFloat(KEY_LETTER_SPACING_EM, sanitized.letterSpacingEm)
            putFloat(KEY_PARAGRAPH_SPACING_DP, sanitized.paragraphSpacingDp)
            putString(KEY_PALETTE_NAME, sanitized.paletteName)
        }
    }
}
