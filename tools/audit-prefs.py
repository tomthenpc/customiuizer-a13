#!/usr/bin/env python3
"""Lightweight read-only audit of PreferenceSchema against XML and code.

This version classifies the large XML/code gaps so they are not mistaken for
purely manual-entry counts:

- prefix: the key differs only by the `pref_key_` prefix after normalization;
- test: the key only appears in unit tests;
- dynamic: the key is built by string concatenation/format and not a literal;
- real_missing: the key is a literal used in both XML and production code but
  absent from the schema;
- xml_only / code_only: the key is only referenced in XML or only in code.
"""

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
TEST_FILES = [
    "app/src/test/**/*.kt",
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
    pattern = r'(?:MainModule\.)?(?:mPrefs|prefs)\.(?:get\w+)\s*\(\s*"([^"]+)"\s*[,)]'
    return re.findall(pattern, text)


def extract_test_keys(text: str) -> list[str]:
    """Keys mentioned in test files are usually synthetic test data."""
    return re.findall(r'"(pref_key_[^"]+|system_[^"]+|controls_[^"]+|various_[^"]+|launcher_[^"]+)"', text)


def collect_keys(patterns: list[str], extractor, xml_only: bool = False) -> set[str]:
    keys: set[str] = set()
    for pattern in patterns:
        if pattern.endswith("**/*.kt") or pattern.endswith("**/*.java"):
            base = REPO_ROOT / pattern.replace("/**/*.kt", "").replace("/**/*.java", "")
            ext = pattern.split("**/*.")[-1]
            for path in base.rglob(f"*.{ext}"):
                if xml_only and "test" in path.parts:
                    continue
                keys.update(normalize(k) for k in extractor(path.read_text(encoding="utf-8")))
        else:
            path = REPO_ROOT / pattern
            if path.exists():
                keys.update(normalize(k) for k in extractor(path.read_text(encoding="utf-8")))
    return keys


def classify(xml_only: set[str], code_only: set[str], xml_and_code: set[str], test_keys: set[str]) -> dict[str, list[str]]:
    """Classify keys missing from the schema."""
    categories: dict[str, list[str]] = {
        "prefix_diff": [],
        "dynamic_or_concat": [],
        "test_constant": [],
        "real_missing": [],
        "xml_only": [],
        "code_only": [],
    }

    for key in sorted(xml_only):
        if key.startswith("pref_") or "pref_" in key:
            categories["prefix_diff"].append(key)
        elif key in test_keys:
            categories["test_constant"].append(key)
        else:
            categories["xml_only"].append(key)

    for key in sorted(code_only):
        if "+" in key or "%" in key or "{" in key:
            categories["dynamic_or_concat"].append(key)
        elif key in test_keys:
            categories["test_constant"].append(key)
        else:
            categories["code_only"].append(key)

    for key in sorted(xml_and_code):
        if key in test_keys:
            categories["test_constant"].append(key)
        else:
            categories["real_missing"].append(key)

    return categories


def print_set(title: str, items: list[str], limit: int = 20) -> None:
    print(f"\n{title} ({len(items)}):")
    for item in items[:limit]:
        print(f"  - {item}")
    if len(items) > limit:
        print(f"  ... and {len(items) - limit} more")


def main() -> int:
    schema_text = SCHEMA_FILE.read_text(encoding="utf-8")
    schema_keys = {normalize(k) for k in extract_schema_keys(schema_text)}

    xml_keys: set[str] = set()
    for path in REPO_ROOT.glob(XML_GLOB):
        xml_keys.update(normalize(k) for k in extract_xml_keys(path.read_text(encoding="utf-8")))

    code_keys = collect_keys(CODE_FILES, extract_code_keys)
    test_keys = collect_keys(TEST_FILES, extract_test_keys)

    schema_dupes = [k for k, count in Counter(extract_schema_keys(schema_text)).items() if count > 1]

    in_xml_not_schema = xml_keys - schema_keys
    in_schema_not_xml = schema_keys - xml_keys
    in_code_not_schema = code_keys - schema_keys
    in_schema_not_code = schema_keys - code_keys

    # Keys present in both XML and code but not schema are the most likely
    # real missing schema entries.
    xml_and_code_not_schema = (xml_keys & code_keys) - schema_keys

    print("=== Preference Schema Audit ===")
    print(f"Schema keys: {len(schema_keys)}")
    print(f"XML keys:    {len(xml_keys)}")
    print(f"Code keys:   {len(code_keys)}")
    print(f"Schema & XML:  {len(schema_keys & xml_keys)}")
    print(f"Schema & Code: {len(schema_keys & code_keys)}")
    print(f"XML & Code:    {len(xml_keys & code_keys)}")

    categories = classify(
        in_xml_not_schema,
        in_code_not_schema,
        xml_and_code_not_schema,
        test_keys,
    )

    print_set("Keys in XML but not in Schema (xml_only)", categories["xml_only"])
    print_set("Keys in code but not in Schema (code_only)", categories["code_only"])
    print_set("Keys in both XML and code but not Schema (real_missing)", categories["real_missing"])
    print_set("Prefix-only differences", categories["prefix_diff"])
    print_set("Likely dynamic/concatenated keys", categories["dynamic_or_concat"])
    print_set("Test-only constants", categories["test_constant"])
    print_set("Keys in Schema but not in XML", sorted(in_schema_not_xml))
    print_set("Keys in Schema but not in code", sorted(in_schema_not_code))

    if schema_dupes:
        print(f"\nDuplicate keys in Schema ({len(schema_dupes)}):")
        for k in schema_dupes:
            print(f"  - {k}")
        return 1

    print("\nNo duplicate keys in Schema.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
