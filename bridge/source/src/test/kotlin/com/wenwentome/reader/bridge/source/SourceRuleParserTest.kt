package com.wenwentome.reader.bridge.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SourceRuleParserTest {
    @Test
    fun legacySource_searchDetailTocAndContentRules_areNormalized() {
        val source = fixture("legacy-source.json").decodeToString()
        val parsed = SourceRuleParser().parse(source)

        assertEquals("测试源", parsed.name)
        assertNotNull(parsed.searchRule)
        assertNotNull(parsed.bookInfoRule)
        assertNotNull(parsed.tocRule)
        assertNotNull(parsed.contentRule)
    }
}

private fun fixture(name: String): ByteArray =
    requireNotNull(SourceRuleParserTest::class.java.getResourceAsStream("/fixtures/$name")) {
        "Missing fixture $name"
    }.readBytes()
