package io.legado.app.ui.wenwen

import org.junit.Assert.assertEquals
import org.junit.Test

class WenwenReaderPrefsTest {

    @Test
    fun sanitized_clampsReaderTypographyValues() {
        val prefs = WenwenReaderPresentationPrefs(
            fontSizeSp = 48f,
            lineHeightEm = 9f,
            letterSpacingEm = -1f,
            paragraphSpacingDp = 80f,
            paletteName = "Unknown",
        ).sanitized()

        assertEquals(36f, prefs.fontSizeSp)
        assertEquals(2.4f, prefs.lineHeightEm)
        assertEquals(-0.05f, prefs.letterSpacingEm)
        assertEquals(32f, prefs.paragraphSpacingDp)
        assertEquals(ReaderPalette.Paper.name, prefs.paletteName)
    }
}
