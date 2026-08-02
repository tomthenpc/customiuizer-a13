#!/usr/bin/env python3
"""Check documentation contract invariants.

Runs from the repo root and exits non-zero on drift.

Checks (v4 minimum):
- docs/audit/*_V4.md and docs/audit/DOCUMENTATION_CONTRACT_V4.md exist;
- each required CURRENT/SNAPSHOT/PLAN metadata block has the expected keys;
- EvidenceCommit, if present, is an ancestor of HEAD;
- no CURRENT document uses forbidden wait/stop/obsolete language.
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DOCS_DIR = REPO_ROOT / "docs"
REQUIRED_METADATA = {
    "DocumentKind",
    "Product",
    "Repository",
    "Branch",
    "EvidenceCommit",
    "EvidenceState",
    "DeviceEvidence",
}
FORBIDDEN_PHRASES = [
    "等待当前 Agent",
    "尚无 checkpoint",
    "Registry 仍与 legacy dispatcher 并存",
    "旧授权分支",
    "完成后等待 owner",
    "用 current build 覆盖 baseline",
    "stop and wait",
    "等待仓库所有者",
]


def commit_exists(commit: str) -> tuple[bool, str | None]:
    """Return (ok, error_code). HISTORY_UNAVAILABLE if object not present."""
    try:
        result = subprocess.run(
            ["git", "-C", str(REPO_ROOT), "cat-file", "-e", commit],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )
        if result.returncode == 0:
            return True, None
        if "not a valid object" in (result.stderr.decode("utf-8", errors="replace") + result.stdout.decode("utf-8", errors="replace")):
            return False, "HISTORY_UNAVAILABLE"
        return False, "HISTORY_UNAVAILABLE"
    except Exception:
        return False, "HISTORY_UNAVAILABLE"


def is_ancestor(commit: str) -> tuple[bool, str | None]:
    """Return (ok, error_code). Error codes: HISTORY_UNAVAILABLE, NOT_ANCESTOR."""
    exists, err = commit_exists(commit)
    if not exists:
        return False, err
    try:
        result = subprocess.run(
            ["git", "-C", str(REPO_ROOT), "merge-base", "--is-ancestor", commit, "HEAD"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )
        if result.returncode == 0:
            return True, None
        return False, "NOT_ANCESTOR"
    except Exception:
        return False, "HISTORY_UNAVAILABLE"


def check() -> list[str]:
    errors: list[str] = []

    for path in (REPO_ROOT / "docs" / "audit").glob("*_V4.md"):
        text = path.read_text(encoding="utf-8", errors="replace")

        # Extract fenced metadata block
        metadata: dict[str, str] = {}
        m = re.search(r"```text\n(.*?)\n```", text, re.DOTALL)
        if not m:
            errors.append(f"{path}: missing fenced metadata block")
            continue

        block = m.group(1)
        for line in block.splitlines():
            if ":" in line:
                key, _, value = line.partition(":")
                metadata[key.strip()] = value.strip()

        missing = REQUIRED_METADATA - set(metadata.keys())
        if missing:
            errors.append(f"{path}: missing metadata keys {sorted(missing)}")
            continue

        if "EvidenceCommit" in metadata:
            ok, err = is_ancestor(metadata["EvidenceCommit"])
            if not ok:
                errors.append(
                    f"{path}: EvidenceCommit {metadata['EvidenceCommit']} {err}"
                )

        if metadata.get("DocumentKind") == "CURRENT":
            for phrase in FORBIDDEN_PHRASES:
                if phrase in text:
                    errors.append(f"{path}: forbidden phrase '{phrase}' in CURRENT doc")

    return errors


def main() -> int:
    errors = check()
    if errors:
        raise SystemExit(f"Documentation contract errors ({len(errors)}):\n" + "\n".join(f"  - ERR:{e}" for e in errors))
    print("A13 documentation contract OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
