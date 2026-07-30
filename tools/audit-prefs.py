#!/usr/bin/env python3
"""Lightweight read-only audit of PreferenceSchema against XML and code."""

import re
import sys
from collections import Counter
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
SCHEMA_FILE = REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "prefs" / "PreferenceSchema.kt"
XML_GLOB = "app/src/main/res/xml*/prefs_*.xml"
CODE_FILES = [
    "app/src/main/java/tv/withaibuild/customiuizer/MainModule.java",
    "app/src/main/java/tv/withaibuild/customiuizer/mods/**/*.kt",
]


def normalize(key: str) -> str:
    """XML uses a `pref_key_` prefix; code can be written with or without it."""
    if key.startswith("pref_key_"):
        return key[len("pref_key_"):]
    return key


def extract_schema_keys(text: str) -> list[str]:
    return re.findall(r'key\s*=\s*"([^"]+)"', text)


def extract_xml_keys(text: str) -> list[str]:
    return re.findall(r'android:key\s*=\s*"([^"]+)"', text)


def extract_code_keys(text: str) -> list[str]:
    pattern = r'(?:MainModule\.)?mPrefs\.(?:get\w+)\s*\(\s*"([^"]+)"\s*[,)]'
    return re.findall(pattern, text)


def scan_files() -> tuple[set[str], set[str], set[str]]:
    schema_text = SCHEMA_FILE.read_text(encoding="utf-8")
    schema_keys = {normalize(k) for k in extract_schema_keys(schema_text)}

    xml_keys: set[str] = set()
    for path in REPO_ROOT.glob(XML_GLOB):
        xml_keys.update(normalize(k) for k in extract_xml_keys(path.read_text(encoding="utf-8")))

    code_keys: set[str] = set()
    for pattern in CODE_FILES:
        if pattern.endswith("**/*.kt"):
            base = REPO_ROOT / pattern.replace("/**/*.kt", "")
            for path in base.rglob("*.kt"):
                code_keys.update(normalize(k) for k in extract_code_keys(path.read_text(encoding="utf-8")))
        else:
            path = REPO_ROOT / pattern
            if path.exists():
                code_keys.update(normalize(k) for k in extract_code_keys(path.read_text(encoding="utf-8")))

    return schema_keys, xml_keys, code_keys


def print_set(title: str, items: set[str], limit: int = 50) -> None:
    sorted_items = sorted(items)
    print(f"\n{title} ({len(items)}):")
    for item in sorted_items[:limit]:
        print(f"  - {item}")
    if len(sorted_items) > limit:
        print(f"  ... and {len(sorted_items) - limit} more")


def main() -> int:
    schema_keys, xml_keys, code_keys = scan_files()

    schema_dupes = [k for k, count in Counter(extract_schema_keys(SCHEMA_FILE.read_text(encoding="utf-8"))).items() if count > 1]

    in_xml_not_schema = xml_keys - schema_keys
    in_schema_not_xml = schema_keys - xml_keys
    in_code_not_schema = code_keys - schema_keys
    in_schema_not_code = schema_keys - code_keys

    print("=== Preference Schema Audit ===")
    print(f"Schema keys: {len(schema_keys)}")
    print(f"XML keys:    {len(xml_keys)}")
    print(f"Code keys:   {len(code_keys)}")
    print(f"Schema & XML:  {len(schema_keys & xml_keys)}")
    print(f"Schema & Code: {len(schema_keys & code_keys)}")
    print(f"XML & Code:    {len(xml_keys & code_keys)}")

    print_set("Keys in XML but not in Schema", in_xml_not_schema)
    print_set("Keys in Schema but not in XML", in_schema_not_xml)
    print_set("Keys in code but not in Schema", in_code_not_schema)
    print_set("Keys in Schema but not in code", in_schema_not_code)

    if schema_dupes:
        print(f"\nDuplicate keys in Schema ({len(schema_dupes)}):")
        for k in schema_dupes:
            print(f"  - {k}")
        return 1

    print("\nNo duplicate keys in Schema.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
