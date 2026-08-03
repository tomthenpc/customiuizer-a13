#!/usr/bin/env python3
"""Validate the committed A13_LEGACY_EXCEPTION_REGISTRY.json."""

from __future__ import annotations

import json
import sys
from pathlib import Path

# build_legacy_exception_registry.py performs the actual validation logic.
import build_legacy_exception_registry


def main() -> int:
    if not build_legacy_exception_registry.OUT_FILE.is_file():
        print(f"Missing registry: {build_legacy_exception_registry.OUT_FILE}", file=sys.stderr)
        return 1

    with build_legacy_exception_registry.OUT_FILE.open("r", encoding="utf-8") as f:
        registry = json.load(f)

    errors = build_legacy_exception_registry.validate(registry)
    if errors:
        for e in errors:
            print(f"ERROR: {e}", file=sys.stderr)
        return 1

    print(
        f"A13_LEGACY_EXCEPTION_REGISTRY.json is valid: {len(registry.get('records', []))} records"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
