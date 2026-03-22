import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from release_metadata import extract_notes, read_version_name, resolve_release_tag, validate_release_pack  # noqa: E402


class ReleaseMetadataTest(unittest.TestCase):
    def test_extract_notes_returns_requested_version_section(self):
        notes = extract_notes(
            changelog_text="""## [1.0] - 2026-03-19
- 首发

## [1.1] - 2026-03-29
- 修复""",
            version="1.0",
        )
        self.assertIn("首发", notes)
        self.assertNotIn("修复", notes)

    def test_read_gradle_version_name_matches_tag(self):
        version_name = read_version_name(
            """
            defaultConfig {
                versionCode = 100
                versionName = "1.0"
            }
            """
        )
        self.assertEqual("1.0", version_name)

    def test_resolve_release_tag_uses_git_tag_context_on_push(self):
        tag_name, version = resolve_release_tag(
            event_name="push",
            ref_type="tag",
            ref_name="v1.0",
            manual_tag=None,
        )
        self.assertEqual("v1.0", tag_name)
        self.assertEqual("1.0", version)

    def test_resolve_release_tag_uses_manual_tag_on_workflow_dispatch(self):
        tag_name, version = resolve_release_tag(
            event_name="workflow_dispatch",
            ref_type="branch",
            ref_name="main",
            manual_tag="v1.0",
        )
        self.assertEqual("v1.0", tag_name)
        self.assertEqual("1.0", version)

    def test_validate_release_pack_accepts_matching_metadata(self):
        validate_release_pack(
            gradle_text="""
            defaultConfig {
                versionCode = 110
                versionName = "1.1"
            }
            """,
            changelog_text="""## [1.1] - 2026-03-21
            - 视觉改版

            ## [1.0] - 2026-03-19
            - 首发""",
            changelog_json_text="""
            [
              {
                "version": "1.1",
                "releaseDate": "2026-03-21",
                "title": "阅读体验升级",
                "highlights": ["详情页四段结构"],
                "details": ["新增目录当前章标识"]
              },
              {
                "version": "1.0",
                "releaseDate": "2026-03-19",
                "title": "首发版本",
                "highlights": ["支持 TXT / EPUB"],
                "details": ["统一书库上线"]
              }
            ]
            """,
            readme_text="""
            ## 版本信息
            - 当前正式版本：`1.1`
            """,
            site_text="""
            <title>WenwenToMe 1.1 发布页</title>
            <meta name="description" content="WenwenToMe 1.1 发布页：查看版本亮点" />
            <p>1.1 版本聚焦阅读稳定性</p>
            <p class="panel-version">1.1</p>
            <p class="section-kicker">1.1 亮点</p>
            """,
        )

    def test_validate_release_pack_rejects_mismatched_app_changelog_version(self):
        with self.assertRaisesRegex(ValueError, "first changelog entry"):
            validate_release_pack(
                gradle_text="""
                defaultConfig {
                    versionCode = 110
                    versionName = "1.1"
                }
                """,
                changelog_text="""## [1.1] - 2026-03-21
                - 视觉改版""",
                changelog_json_text="""
                [
                  {
                    "version": "1.0",
                    "releaseDate": "2026-03-19",
                    "title": "首发版本",
                    "highlights": ["支持 TXT / EPUB"],
                    "details": ["统一书库上线"]
                  }
                ]
                """,
                readme_text="""
                ## 版本信息
                - 当前正式版本：`1.1`
                """,
            )

    def test_validate_release_pack_rejects_mismatched_site_release_page_version(self):
        with self.assertRaisesRegex(ValueError, "site release page"):
            validate_release_pack(
                gradle_text="""
                defaultConfig {
                    versionCode = 110
                    versionName = "1.1"
                }
                """,
                changelog_text="""## [1.1] - 2026-03-21
                - 视觉改版""",
                changelog_json_text="""
                [
                  {
                    "version": "1.1",
                    "releaseDate": "2026-03-21",
                    "title": "阅读体验升级",
                    "highlights": ["详情页四段结构"],
                    "details": ["新增目录当前章标识"]
                  }
                ]
                """,
                readme_text="""
                ## 版本信息
                - 当前正式版本：`1.1`
                """,
                site_text="""
                <title>WenwenToMe 1.0 发布页</title>
                <meta name="description" content="WenwenToMe 1.0 发布页：查看版本亮点" />
                <p>1.0 版本聚焦阅读稳定性</p>
                <p class="panel-version">1.0</p>
                <p class="section-kicker">1.0 亮点</p>
                """,
            )

    def test_validate_release_pack_rejects_mismatched_site_release_page_body_version(self):
        with self.assertRaisesRegex(ValueError, "hero copy"):
            validate_release_pack(
                gradle_text="""
                defaultConfig {
                    versionCode = 110
                    versionName = "1.1"
                }
                """,
                changelog_text="""## [1.1] - 2026-03-21
                - 视觉改版""",
                changelog_json_text="""
                [
                  {
                    "version": "1.1",
                    "releaseDate": "2026-03-21",
                    "title": "阅读体验升级",
                    "highlights": ["详情页四段结构"],
                    "details": ["新增目录当前章标识"]
                  }
                ]
                """,
                readme_text="""
                ## 版本信息
                - 当前正式版本：`1.1`
                """,
                site_text="""
                <title>WenwenToMe 1.1 发布页</title>
                <meta name="description" content="WenwenToMe 1.1 发布页：查看版本亮点" />
                <p>1.0 版本聚焦阅读稳定性</p>
                <p class="panel-version">1.1</p>
                <p class="section-kicker">1.1 亮点</p>
                """,
            )


if __name__ == "__main__":
    unittest.main()
