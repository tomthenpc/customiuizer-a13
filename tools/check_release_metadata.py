#!/usr/bin/env python3
from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--require-tag", action="store_true")
    args = p.parse_args()

    gradle = (REPO / "app" / "build.gradle.kts").read_text(encoding="utf-8")
    m_code = re.search(r"val\s+lastVersion\s*=\s*(\d+)", gradle)
    m_name = re.search(r"val\s+lastVersionName\s*=\s*\"(r\d+\.\d+\.\d+)\"", gradle)
    if not m_code or not m_name:
        print("missing lastVersion/lastVersionName in app/build.gradle.kts")
        return 1
    version_code = int(m_code.group(1))
    version_name = m_name.group(1)
    if version_code <= 0:
        print("invalid versionCode")
        return 1

    changelog = (REPO / "CHANGELOG.md").read_text(encoding="utf-8", errors="replace")
    if f"## {version_name}" not in changelog:
        print(f"CHANGELOG.md missing top entry for {version_name}")
        return 1

    if args.require_tag:
        tag = os.environ.get("GITHUB_REF_NAME", "")
        if tag and tag != version_name:
            print(f"tag/version mismatch: tag={tag} version={version_name}")
            return 1

    print(f"release metadata OK: {version_name} ({version_code})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
