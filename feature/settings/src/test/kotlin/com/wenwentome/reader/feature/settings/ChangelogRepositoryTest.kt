package com.wenwentome.reader.feature.settings

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChangelogRepositoryTest {
    @Test
    fun parse_returnsNewestEntriesFirstAndPreservesHighlights() = runTest {
        val repository = ChangelogRepository(
            loadJson = {
                """
                [
                  {
                    "version": "1.0",
                    "releaseDate": "2026-03-19",
                    "title": "首发版本",
                    "highlights": ["统一书库"],
                    "details": ["支持 TXT / EPUB"]
                  },
                  {
                    "version": "1.1",
                    "releaseDate": "2026-03-29",
                    "title": "首个迭代",
                    "highlights": ["更新日志页优化"],
                    "details": ["新增版本排序"]
                  }
                ]
                """.trimIndent()
            },
        )

        val entries = repository.getAll()

        assertEquals(listOf("1.1", "1.0"), entries.map { it.version })
        assertEquals(listOf("更新日志页优化"), entries.first().highlights)
    }
}
