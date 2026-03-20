#!/usr/bin/env python3

import argparse
import re
import sys
from pathlib import Path
from typing import Optional, Tuple


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
