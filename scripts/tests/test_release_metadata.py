import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from release_metadata import extract_notes, read_version_name  # noqa: E402


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


if __name__ == "__main__":
    unittest.main()
