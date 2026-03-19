#!/usr/bin/env python3

import argparse
import re
import sys
from pathlib import Path


def read_version_name(gradle_text: str) -> str:
    match = re.search(r'versionName\s*=\s*"([^"]+)"', gradle_text)
    if not match:
        raise ValueError("versionName not found")
    return match.group(1)


def extract_notes(changelog_text: str, version: str) -> str:
    pattern = rf"^## \[{re.escape(version)}\].*?(?=^## \[|\Z)"
    match = re.search(pattern, changelog_text, re.MULTILINE | re.DOTALL)
    if not match:
        raise ValueError(f"version {version} not found in changelog")
    return match.group(0).strip()


def check_tag(tag: str, gradle_path: Path) -> None:
    if not tag.startswith("v"):
        raise ValueError("tag must start with 'v'")
    version_name = read_version_name(gradle_path.read_text(encoding="utf-8"))
    expected_tag = f"v{version_name}"
    if tag != expected_tag:
        raise ValueError(f"tag {tag} does not match versionName {version_name}")


def print_notes(version: str, changelog_path: Path) -> None:
    notes = extract_notes(changelog_path.read_text(encoding="utf-8"), version)
    print(notes)


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate release metadata.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    check_tag_parser = subparsers.add_parser("check-tag")
    check_tag_parser.add_argument("tag")
    check_tag_parser.add_argument("gradle_path")

    notes_parser = subparsers.add_parser("notes")
    notes_parser.add_argument("version")
    notes_parser.add_argument("changelog_path")

    args = parser.parse_args()

    try:
        if args.command == "check-tag":
            check_tag(args.tag, Path(args.gradle_path))
        elif args.command == "notes":
            print_notes(args.version, Path(args.changelog_path))
    except ValueError as error:
        print(str(error), file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
