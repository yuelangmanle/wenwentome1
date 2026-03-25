#!/usr/bin/env python3

import argparse
import re
import sys
from pathlib import Path
from typing import Optional, Tuple


def read_version_name(gradle_text: str) -> str:
    patterns = [
        r'versionName\s*=\s*"([^"]+)"',
        r'versionName\s+"([^"]+)"',
    ]
    for pattern in patterns:
        match = re.search(pattern, gradle_text)
        if match:
            return match.group(1)

    version_var_match = re.search(r'\bdef\s+version\s*=\s*"([^"]+)"', gradle_text)
    if version_var_match and re.search(r"\bversionName\s*(?:=)?\s*version\b", gradle_text):
        return version_var_match.group(1)

    raise ValueError("versionName not found")


def read_version_code(gradle_text: str) -> Optional[int]:
    patterns = [
        r"versionCode\s*=\s*(\d+)",
        r"versionCode\s+(\d+)",
    ]
    for pattern in patterns:
        match = re.search(pattern, gradle_text)
        if match:
            return int(match.group(1))
    return None


def expected_version_code(version_name: str) -> Optional[int]:
    match = re.fullmatch(r"(\d+)\.(\d+)", version_name)
    if not match:
        return None
    major = int(match.group(1))
    minor = int(match.group(2))
    return major * 100 + minor * 10


def extract_app_notes(app_notes_text: str, version: str) -> str:
    patterns = [
        rf"^\s*## \[{re.escape(version)}\].*?(?=^\s*## |\Z)",
        rf"^\s*## {re.escape(version)}.*?(?=^\s*## |\Z)",
    ]
    for pattern in patterns:
        match = re.search(pattern, app_notes_text, re.MULTILINE | re.DOTALL)
        if match:
            return match.group(0).strip()
    raise ValueError(f"version {version} not found in app release notes")


def version_aliases(version_name: str) -> set[str]:
    aliases = {version_name}
    parts = version_name.split(".")
    if len(parts) >= 2:
        aliases.add(".".join(parts[:2]))
    return aliases


def extract_notes(changelog_text: str, version: str) -> str:
    pattern = rf"^\s*## \[{re.escape(version)}\].*?(?=^\s*## \[|\Z)"
    match = re.search(pattern, changelog_text, re.MULTILINE | re.DOTALL)
    if not match:
        raise ValueError(f"version {version} not found in changelog")
    return match.group(0).strip()


def validate_release_pack(
    gradle_text: str,
    changelog_text: str,
    app_notes_text: str,
    readme_text: str,
    site_text: Optional[str] = None,
) -> str:
    version_name = read_version_name(gradle_text)
    version_code = read_version_code(gradle_text)
    expected_code = expected_version_code(version_name)
    if expected_code is not None and version_code is not None and version_code != expected_code:
        raise ValueError(
            f"versionCode {version_code} does not match iteration rule for versionName {version_name}"
        )

    extract_notes(changelog_text, version_name)
    extract_app_notes(app_notes_text, version_name)

    readme_markers = (
        f"当前正式版本：`{version_name}`",
        f"当前迁移目标：`{version_name}`",
        f"当前主迁移目标版本为 `{version_name}`",
        f"新底座正式版将从 `v{version_name}` 开始",
    )
    if not any(marker in readme_text for marker in readme_markers):
        raise ValueError(f"README missing current release or migration version {version_name}")

    if site_text is not None:
        validate_site_release_page(site_text, version_name)

    return version_name


def validate_site_release_page(site_text: str, version_name: str) -> None:
    if "<title>WenwenToMe" not in site_text:
        raise ValueError("site page title must mention WenwenToMe")
    if "WenwenToMe" not in site_text:
        raise ValueError("site page must mention WenwenToMe")

    aliases = version_aliases(version_name)
    if not any(alias in site_text for alias in aliases):
        raise ValueError(f"site page does not mention versionName {version_name}")


def check_tag(tag: str, gradle_path: Path) -> None:
    if not tag.startswith("v"):
        raise ValueError("tag must start with 'v'")
    version_name = read_version_name(gradle_path.read_text(encoding="utf-8"))
    expected_tag = f"v{version_name}"
    if tag != expected_tag:
        raise ValueError(f"tag {tag} does not match versionName {version_name}")


def resolve_release_tag(
    event_name: str,
    ref_type: str,
    ref_name: str,
    manual_tag: Optional[str],
) -> Tuple[str, str]:
    if event_name == "workflow_dispatch":
        tag_name = (manual_tag or "").strip()
        if not tag_name:
            raise ValueError("workflow_dispatch requires tag_name input")
    elif ref_type == "tag":
        tag_name = ref_name
    else:
        raise ValueError("release workflow requires a tag ref or workflow_dispatch with tag_name")

    if not tag_name.startswith("v"):
        raise ValueError("tag must start with 'v'")

    return tag_name, tag_name.removeprefix("v")


def print_notes(version: str, changelog_path: Path) -> None:
    notes = extract_notes(changelog_path.read_text(encoding="utf-8"), version)
    print(notes)


def check_release_pack(
    gradle_path: Path,
    changelog_path: Path,
    app_notes_path: Path,
    readme_path: Path,
    site_path: Optional[Path] = None,
) -> None:
    validate_release_pack(
        gradle_text=gradle_path.read_text(encoding="utf-8"),
        changelog_text=changelog_path.read_text(encoding="utf-8"),
        app_notes_text=app_notes_path.read_text(encoding="utf-8"),
        readme_text=readme_path.read_text(encoding="utf-8"),
        site_text=site_path.read_text(encoding="utf-8") if site_path else None,
    )


def write_release_context(
    event_name: str,
    ref_type: str,
    ref_name: str,
    manual_tag: Optional[str],
    output_path: Path,
) -> None:
    tag_name, version = resolve_release_tag(
        event_name=event_name,
        ref_type=ref_type,
        ref_name=ref_name,
        manual_tag=manual_tag,
    )
    with output_path.open("a", encoding="utf-8") as output_file:
        output_file.write(f"tag_name={tag_name}\n")
        output_file.write(f"version={version}\n")


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate release metadata.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    check_tag_parser = subparsers.add_parser("check-tag")
    check_tag_parser.add_argument("tag")
    check_tag_parser.add_argument("gradle_path")

    notes_parser = subparsers.add_parser("notes")
    notes_parser.add_argument("version")
    notes_parser.add_argument("changelog_path")

    validate_pack_parser = subparsers.add_parser("validate-pack")
    validate_pack_parser.add_argument("gradle_path")
    validate_pack_parser.add_argument("changelog_path")
    validate_pack_parser.add_argument("app_notes_path")
    validate_pack_parser.add_argument("readme_path")
    validate_pack_parser.add_argument("--site-path")

    release_context_parser = subparsers.add_parser("release-context")
    release_context_parser.add_argument("--event-name", required=True)
    release_context_parser.add_argument("--ref-type", required=True)
    release_context_parser.add_argument("--ref-name", required=True)
    release_context_parser.add_argument("--manual-tag")
    release_context_parser.add_argument("--output", required=True)

    args = parser.parse_args()

    try:
        if args.command == "check-tag":
            check_tag(args.tag, Path(args.gradle_path))
        elif args.command == "notes":
            print_notes(args.version, Path(args.changelog_path))
        elif args.command == "validate-pack":
            check_release_pack(
                gradle_path=Path(args.gradle_path),
                changelog_path=Path(args.changelog_path),
                app_notes_path=Path(args.app_notes_path),
                readme_path=Path(args.readme_path),
                site_path=Path(args.site_path) if args.site_path else None,
            )
        elif args.command == "release-context":
            write_release_context(
                event_name=args.event_name,
                ref_type=args.ref_type,
                ref_name=args.ref_name,
                manual_tag=args.manual_tag,
                output_path=Path(args.output),
            )
    except ValueError as error:
        print(str(error), file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
