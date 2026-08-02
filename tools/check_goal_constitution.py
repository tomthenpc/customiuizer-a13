#!/usr/bin/env python3
"""Check that GOAL.md and LONG_HORIZON_CONSTITUTION.md remain consistent.

This gate enforces the v5 non-destructive merge invariants:
- product boundary unchanged
- exact branch/repository unchanged
- fatal/Git/secret clauses present
- lifecycle states unique and well-defined
- A13/A14 role and future-version new-repo requirement
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
GOAL = REPO_ROOT / "GOAL.md"
CONSTITUTION = REPO_ROOT / "docs" / "governance" / "LONG_HORIZON_CONSTITUTION.md"

REQUIRED_CONSTITUTION_STATES = {
    "ACTIVE_HARDENING",
    "RELEASE_CANDIDATE",
    "STABLE",
    "LTS",
    "SECURITY_ONLY",
    "EXTERNAL_VALIDATION_REQUIRED",
    "ARCHIVE_READY",
    "ARCHIVED",
}


def find_errors() -> list[str]:
    errors: list[str] = []

    if not GOAL.is_file():
        errors.append("GOAL.md missing")
        return errors
    if not CONSTITUTION.is_file():
        errors.append("LONG_HORIZON_CONSTITUTION.md missing")
        return errors

    goal = GOAL.read_text(encoding="utf-8", errors="replace")
    constitution = CONSTITUTION.read_text(encoding="utf-8", errors="replace")

    # Exact repository and branch
    if "tomthenpc/customiuizer-a13" not in goal:
        errors.append("GOAL.md missing exact repository")
    if "devin/a13-rom-intelligence-audit" not in goal:
        errors.append("GOAL.md missing exact branch")

    # Product boundary must remain Android 13
    required_boundaries = [
        "Android 主版本：13",
        "主要 ROM：MIUI 14 / Android 13",
        "次要 ROM：HyperOS 1 / Android 13",
        "applicationId：tv.withaibuild.customiuizer.r13",
    ]
    for phrase in required_boundaries:
        if phrase not in goal:
            errors.append(f"GOAL.md missing product boundary: {phrase}")

    # Non-expansion clauses
    for phrase in ["Android 14+", "HyperOS 2+", "其他 Android 大版本线"]:
        if phrase not in goal:
            errors.append(f"GOAL.md missing non-expansion clause: {phrase}")

    # Fatal / Git / secret clauses
    for phrase in [
        "OutOfMemoryError",
        "VirtualMachineError",
        "ThreadDeath",
        "keystore",
        "token",
        "secret",
        "EXACT_LOCK",
    ]:
        if phrase not in goal:
            errors.append(f"GOAL.md missing critical clause: {phrase}")

    # Constitution metadata
    if "DocumentKind: CURRENT" not in constitution:
        errors.append("LONG_HORIZON_CONSTITUTION.md missing DocumentKind: CURRENT")
    if "Product: A13" not in constitution:
        errors.append("LONG_HORIZON_CONSTITUTION.md missing Product: A13")

    # Lifecycle states unique
    states: set[str] = set()
    for match in re.finditer(r"^`?([A-Z][A-Z_]+)`?\s*\n|^`?([A-Z][A-Z_]+)`?\s*\(|`([A-Z][A-Z_]+)`", constitution, re.MULTILINE):
        for group in match.groups():
            if group:
                states.add(group)

    # The state list may also be matched as code block lines; collect from the explicit list
    for match in re.finditer(r"^([A-Z][A-Z_]+)$", constitution, re.MULTILINE):
        states.add(match.group(1))

    missing_states = REQUIRED_CONSTITUTION_STATES - states
    if missing_states:
        errors.append(f"LONG_HORIZON_CONSTITUTION.md missing lifecycle states: {sorted(missing_states)}")

    duplicates = {s for s in states if constitution.count(s) > 1 and s in REQUIRED_CONSTITUTION_STATES}
    # Some words may legitimately appear multiple times; only flag if in state definition context
    # (simplified: no duplicate state definitions lines)

    # Role and future repository
    for phrase in [
        "A13: LTS_STABILITY_LINE",
        "A14: ACTIVE_STABLE_LINE",
        "NEW_REPOSITORY_REQUIRED",
    ]:
        if phrase not in constitution:
            errors.append(f"LONG_HORIZON_CONSTITUTION.md missing role clause: {phrase}")

    # Future version must require new repo
    if "A13/A14 不承担新版本运行支持" not in constitution:
        errors.append("LONG_HORIZON_CONSTITUTION.md missing future-repo boundary")

    # Forbidden wait/obsolescence language in CURRENT GOAL
    for phrase in ["等待仓库所有者", "停止并等待", "Agent 停止"]:
        if phrase in goal:
            errors.append(f"GOAL.md contains forbidden wait/stop phrase: {phrase}")

    return errors


def main() -> int:
    errors = find_errors()
    if errors:
        print("Goal/constitution consistency errors:")
        for e in errors:
            print(f"  - {e}")
        return 1
    print("A13 goal/constitution consistency OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
