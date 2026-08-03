#!/usr/bin/env python3
"""Build a machine-readable LEGACY_EXCEPTION registry from the current source tree.

Usage:
    python tools/build_legacy_exception_registry.py --build
    python tools/build_legacy_exception_registry.py --check

The registry is deliberately conservative: the first batch only contains
legacy call sites that are not in typed-catalog installer functions and are
grouped by (sourceFile, enclosingFunction).  Process/phase and preference keys
are intentionally minimal; later P3.3 batches will refine them with owner
review.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import defaultdict
from pathlib import Path

# Re-use classification constants from the ownership auditor
from audit_hook_ownership import (
    HOOK_RE,
    SOURCE_ROOT,
    classify_file,
    extract_typed_installers,
    file_typed_functions,
    nearest_function,
)

REPO_ROOT = Path(__file__).resolve().parent.parent
OUT_FILE = REPO_ROOT / "docs" / "audit" / "A13_LEGACY_EXCEPTION_REGISTRY.json"

ALLOWED_PROCESS = {
    "system_server",
    "system_ui",
    "launcher",
    "android",
    "per_app",
    "resource",
    "other",
}

ALLOWED_PHASE = {
    "SYSTEM_SERVER_STARTING",
    "PACKAGE_READY",
    "RESOURCE_INIT",
    "BOOTSTRAP",
    "OTHER",
}

ALLOWED_REASON_CODE = {
    "CROSS_PROCESS",
    "LIFECYCLE_BOOTSTRAP",
    "RESOURCE_HOOK",
    "DYNAMIC_TARGET_SET",
    "SHARED_MUTABLE_STATE",
    "ROM_DEPENDENT_DISPATCH",
    "API_BRIDGE_BOUNDARY",
    "INSTALLER_INFRASTRUCTURE",
    "TEMPORARY_MIGRATION_DEFERRED",
    "DEAD_CODE_PENDING_OWNER_APPROVAL",
    "OTHER_REVIEW_REQUIRED",
}


def _stable_call_id(rel: str, line: int, func: str) -> str:
    """Stable call-site identity: repository-relative path, line, enclosing function."""
    return f"{rel}:{line}:{func}"


def _generate_record_id(rel: str, func: str) -> str:
    """Generate a short, unique, filesystem-safe record id."""
    raw = f"{rel}::{func}"
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()[:16]


def _infer_process(rel: str) -> str:
    """Infer process scope from the file path.  Conservative defaults to other."""
    if "SystemUI" in rel or "SystemUi" in rel:
        return "system_ui"
    if "Launcher" in rel:
        return "launcher"
    if rel.startswith("tv/withaibuild/customiuizer/mods/System"):
        return "system_server"
    if "ResourceHooks" in rel:
        return "resource"
    if "Various" in rel:
        return "per_app"
    return "other"


def scan_legacy_call_sites() -> list[dict]:
    typed_funcs = file_typed_functions()
    sites: list[dict] = []

    for path in sorted(SOURCE_ROOT.rglob("*.kt")) + sorted(SOURCE_ROOT.rglob("*.java")):
        rel = path.relative_to(SOURCE_ROOT).as_posix()
        text = path.read_text(encoding="utf-8")
        lines = text.splitlines()

        file_default = classify_file(rel)
        file_typed = typed_funcs.get(path, set())

        for i, line in enumerate(lines, start=1):
            if not HOOK_RE.search(line):
                continue

            func = nearest_function(lines, i - 1)

            if file_default == "API_BRIDGE":
                category = "API_BRIDGE"
            elif file_default == "INSTALLER_INFRASTRUCTURE":
                category = "INSTALLER_INFRASTRUCTURE"
            elif file_default is None and rel.startswith("tv/withaibuild/customiuizer/mods/"):
                category = "REGISTRY_FEATURE" if func in file_typed else "LEGACY_EXCEPTION"
            else:
                category = "UNKNOWN"

            sites.append(
                {
                    "rel": rel,
                    "line": i,
                    "function": func,
                    "category": category,
                    "call": line.strip(),
                }
            )

    return sites


def build_registry(sites: list[dict], first_batch_size: int = 20) -> dict:
    """Group legacy call sites and build a minimal first batch of records."""
    legacy = [s for s in sites if s["category"] == "LEGACY_EXCEPTION"]

    # Group by (sourceFile, function)
    groups: dict[tuple[str, str], list[dict]] = defaultdict(list)
    for s in legacy:
        groups[(s["rel"], s["function"])].append(s)

    # Sort groups deterministically by size then path; take the first batch.
    sorted_groups = sorted(
        groups.items(),
        key=lambda kv: (-len(kv[1]), kv[0][0], kv[0][1]),
    )

    records: list[dict] = []
    for (rel, func), calls in sorted_groups[:first_batch_size]:
        record_id = _generate_record_id(rel, func)
        covered = [_stable_call_id(s["rel"], s["line"], s["function"]) for s in calls]
        records.append(
            {
                "id": record_id,
                "status": "ACTIVE",
                "owner": func,
                "sourceFile": rel,
                "entrypoint": func,
                "process": _infer_process(rel),
                "phase": "OTHER",
                "preferenceKeys": [],
                "reasonCode": "OTHER_REVIEW_REQUIRED",
                "reason": "Legacy hook calls not yet owned by the typed Feature catalog; requires independent review to determine the correct logical owner and migration path.",
                "coveredCallSites": sorted(covered),
                "hookTargets": [],
                "testEvidence": ["tools/tests/test_legacy_exception_registry.py"],
                "exitCondition": "Migrate to typed FeatureSpec or remove after owner review in a subsequent P3.3 batch.",
            }
        )

    records.sort(key=lambda r: r["id"])

    return {
        "schemaVersion": 1,
        "generatedAt": None,  # set by writer
        "sourceCommit": None,
        "totalLegacyCallSites": len(legacy),
        "totalLegacyGroups": len(groups),
        "firstBatchSize": len(records),
        "records": records,
    }


def validate(registry: dict, strict: bool = True) -> list[str]:
    """Validate registry records and return a list of error messages."""
    errors: list[str] = []

    records = registry.get("records", [])
    seen_ids: set[str] = set()
    seen_calls: set[str] = set()
    typed_owned: set[str] = set()
    infra_calls: set[str] = set()

    # First, scan source to know which calls are typed/infra.
    for s in scan_legacy_call_sites():
        call_id = _stable_call_id(s["rel"], s["line"], s["function"])
        if s["category"] == "REGISTRY_FEATURE":
            typed_owned.add(call_id)
        elif s["category"] in ("API_BRIDGE", "INSTALLER_INFRASTRUCTURE"):
            infra_calls.add(call_id)

    for idx, rec in enumerate(records):
        prefix = f"record[{idx}] ({rec.get('id', '?')})"
        for field in (
            "id",
            "status",
            "owner",
            "sourceFile",
            "entrypoint",
            "process",
            "phase",
            "reasonCode",
            "reason",
            "coveredCallSites",
            "testEvidence",
            "exitCondition",
        ):
            if field not in rec:
                errors.append(f"{prefix}: missing field '{field}'")

        if not rec.get("owner"):
            errors.append(f"{prefix}: owner is empty")
        if not rec.get("sourceFile"):
            errors.append(f"{prefix}: sourceFile is empty")
        if not rec.get("exitCondition"):
            errors.append(f"{prefix}: exitCondition is empty")
        if rec.get("exitCondition") == "never":
            errors.append(f"{prefix}: exitCondition must not be 'never'")

        if rec.get("process") not in ALLOWED_PROCESS:
            errors.append(f"{prefix}: unknown process '{rec.get('process')}'")
        if rec.get("phase") not in ALLOWED_PHASE:
            errors.append(f"{prefix}: unknown phase '{rec.get('phase')}'")
        if rec.get("reasonCode") not in ALLOWED_REASON_CODE:
            errors.append(f"{prefix}: unknown reasonCode '{rec.get('reasonCode')}'")

        if rec.get("testEvidence"):
            if not any(isinstance(x, str) for x in rec["testEvidence"]):
                errors.append(f"{prefix}: testEvidence must be a list of strings")
        else:
            errors.append(f"{prefix}: testEvidence is empty")

        if not rec.get("reason"):
            errors.append(f"{prefix}: reason is empty")

        rid = rec.get("id")
        if rid in seen_ids:
            errors.append(f"{prefix}: duplicate id '{rid}'")
        seen_ids.add(rid)

        source_file = rec.get("sourceFile", "")
        if not source_file:
            continue
        path = SOURCE_ROOT / source_file
        if not path.exists():
            errors.append(f"{prefix}: sourceFile does not exist '{source_file}'")
        else:
            text = path.read_text(encoding="utf-8")
            if rec.get("entrypoint") and rec["entrypoint"] not in text:
                errors.append(
                    f"{prefix}: entrypoint function '{rec['entrypoint']}' not found in {source_file}"
                )

        for call_id in rec.get("coveredCallSites", []):
            if call_id in seen_calls:
                errors.append(f"{prefix}: call site {call_id} is covered by more than one record")
            seen_calls.add(call_id)
            if call_id in typed_owned:
                errors.append(f"{prefix}: typed-catalog owned call {call_id} cannot be legacy")
            if call_id in infra_calls:
                errors.append(
                    f"{prefix}: API_BRIDGE/INSTALLER_INFRASTRUCTURE call {call_id} cannot be business exception"
                )
            parts = call_id.split(":")
            if len(parts) != 3:
                errors.append(f"{prefix}: malformed call site id '{call_id}'")

    if strict and errors:
        return errors
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--build", action="store_true", help="write the registry")
    group.add_argument("--check", action="store_true", help="validate the existing registry")
    args = parser.parse_args()

    if args.check:
        if not OUT_FILE.is_file():
            print(f"Missing registry: {OUT_FILE}", file=sys.stderr)
            return 1
        with OUT_FILE.open("r", encoding="utf-8") as f:
            registry = json.load(f)
        errors = validate(registry)
        if errors:
            for e in errors:
                print(f"ERROR: {e}", file=sys.stderr)
            return 1
        print(f"Registry valid: {len(registry.get('records', []))} records")
        return 0

    # build
    sites = scan_legacy_call_sites()
    registry = build_registry(sites, first_batch_size=20)

    git_out = run_git_output(["rev-parse", "HEAD"])
    if git_out:
        registry["sourceCommit"] = git_out.strip()
    from datetime import datetime, timezone
    registry["generatedAt"] = (
        datetime.now(timezone.utc).replace(microsecond=0).isoformat()
    )

    errors = validate(registry)
    if errors:
        for e in errors:
            print(f"ERROR: {e}", file=sys.stderr)
        return 1

    OUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    with OUT_FILE.open("w", encoding="utf-8", newline="\n") as f:
        json.dump(registry, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print(f"Wrote {OUT_FILE}")
    print(f"  total legacy call sites: {registry['totalLegacyCallSites']}")
    print(f"  total legacy groups:     {registry['totalLegacyGroups']}")
    print(f"  first batch records:     {registry['firstBatchSize']}")
    return 0


def run_git_output(args: list[str]) -> str | None:
    import subprocess
    try:
        result = subprocess.run(
            ["git", *args],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
        return result.stdout if result.returncode == 0 else None
    except FileNotFoundError:
        return None


if __name__ == "__main__":
    raise SystemExit(main())
