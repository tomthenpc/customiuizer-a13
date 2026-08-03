#!/usr/bin/env python3
"""Build a machine-readable LEGACY_EXCEPTION registry from the current source tree.

Usage:
    python tools/build_legacy_exception_registry.py --build
    python tools/build_legacy_exception_registry.py --check
    python tools/build_legacy_exception_registry.py --census

The registry is intentionally conservative.  It is composed of a stable
per-call census plus a curated first batch of LEGACY_EXCEPTION records.
Each record corresponds to a logical owner (process + preference + entrypoint)
and lists the exact legacy call sites it covers.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import re
import subprocess
import sys
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath

from audit_hook_ownership import (
    HOOK_RE,
    SOURCE_ROOT,
    classify_file,
    file_typed_functions,
    nearest_function,
)

REPO_ROOT = Path(__file__).resolve().parent.parent
OUT_FILE = REPO_ROOT / "docs" / "audit" / "A13_LEGACY_EXCEPTION_REGISTRY.json"
CENSUS_FILE = REPO_ROOT / "docs" / "audit" / "A13_HOOK_CALL_SITE_CENSUS.json"

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

FIRST_BATCH_SEEDS: list[dict] = [
    {
        "id": "legacy-separatevolume-systemui",
        "owner": "MIUIVolumeDialogHook",
        "sourceFile": "tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt",
        "entrypoint": "MIUIVolumeDialogHook",
        "process": "system_ui",
        "phase": "PACKAGE_READY",
        "preferenceKeys": ["system_separatevolume", "system_separatevolume_slider"],
        "reasonCode": "CROSS_PROCESS",
        "reason": (
            "The separate volume stream feature routes to SystemUI "
            "(MiuiVolumeDialog / volume plugin) as well as to system_server audio "
            "(NotificationVolumeServiceHook is already typed in FeatureCatalog). "
            "There is currently no typed cross-process owner that covers the "
            "SystemUI side of the split."
        ),
        "ownedFunctions": {"MIUIVolumeDialogHook", "SingleNotificationSliderHook"},
        "hookTargets": [
            "com.android.systemui.shared.plugins.PluginInstance$Factory#getClassLoader",
            "com.android.systemui.miui.volume.MiuiVolumeDialogImpl#addColumn",
            "com.android.systemui.miui.volume.Util#isNotificationSingle",
        ],
        "testEvidence": ["tools/tests/test_legacy_exception_registry.py"],
        "exitCondition": (
            "Migrate to a typed FeatureSpec with cross-process SystemUI routing "
            "when the installer can dispatch to both system_server and SystemUI "
            "(P3.3C)."
        ),
    },
    {
        "id": "legacy-separatevolume-settings",
        "owner": "NotificationVolumeSettingsHook",
        "sourceFile": "tv/withaibuild/customiuizer/mods/SystemAudioAndVolumeHooks.kt",
        "entrypoint": "NotificationVolumeSettingsHook",
        "process": "per_app",
        "phase": "PACKAGE_READY",
        "preferenceKeys": ["system_separatevolume"],
        "reasonCode": "CROSS_PROCESS",
        "reason": (
            "The Settings app variant of the separate volume stream feature is "
            "installed from the Settings installer and is not yet represented as a "
            "typed FeatureSpec, while the system_server audio side is already in the "
            "catalog."
        ),
        "ownedFunctions": {"NotificationVolumeSettingsHook"},
        "hookTargets": [
            "com.android.settings.MiuiSoundSettings#onCreate",
        ],
        "testEvidence": ["tools/tests/test_legacy_exception_registry.py"],
        "exitCondition": (
            "Merge into a per-app FeatureSpec for com.android.settings when the "
            "Settings installer is replaced by the typed registry (P3.3C)."
        ),
    },
    {
        "id": "legacy-usbconfig-system",
        "owner": "USBConfigHook",
        "sourceFile": "tv/withaibuild/customiuizer/mods/SystemSettingsMoreHooks.kt",
        "entrypoint": "USBConfigHook",
        "process": "system_server",
        "phase": "SYSTEM_SERVER_STARTING",
        "preferenceKeys": ["system_defaultusb", "system_defaultusb_unsecure"],
        "reasonCode": "CROSS_PROCESS",
        "reason": (
            "USB default configuration must run in system_server (PowerManager / "
            "UsbDeviceManager) and has no typed Feature owner. The feature spans "
            "both system_server and the Settings app, so the split cannot be "
            "expressed as a single catalog entry today."
        ),
        "ownedFunctions": {"USBConfigHook"},
        "hookTargets": [
            "com.android.server.power.PowerManagerService#systemReady",
            "com.android.server.usb.UsbDeviceManager$UsbHandler#isUsbDataTransferActive",
            "com.android.server.usb.UsbDeviceManager$UsbHandler#handleMessage",
        ],
        "testEvidence": ["tools/tests/test_legacy_exception_registry.py"],
        "exitCondition": (
            "Create a typed system_server FeatureSpec for USB default mode and "
            "expose a cross-process contract for the Settings variant (P3.3C)."
        ),
    },
    {
        "id": "legacy-usbconfig-settings",
        "owner": "USBConfigSettingsHook",
        "sourceFile": "tv/withaibuild/customiuizer/mods/SystemSettingsMoreHooks.kt",
        "entrypoint": "USBConfigSettingsHook",
        "process": "per_app",
        "phase": "PACKAGE_READY",
        "preferenceKeys": ["system_defaultusb"],
        "reasonCode": "CROSS_PROCESS",
        "reason": (
            "The Settings app side of USB default configuration is installed from "
            "the Settings installer and lacks a typed per-app Feature owner. It "
            "shares the system_defaultusb preference with the system_server hook."
        ),
        "ownedFunctions": {"USBConfigSettingsHook"},
        "hookTargets": [
            "com.android.settings.connecteddevice.usb.UsbModeChooserReceiver#onReceive",
        ],
        "testEvidence": ["tools/tests/test_legacy_exception_registry.py"],
        "exitCondition": (
            "Merge into a per-app FeatureSpec for com.android.settings when the "
            "Settings installer is replaced by the typed registry (P3.3C)."
        ),
    },
]


def _stable_call_id(rel: str, line: int, func: str) -> str:
    return f"{rel}:{line}:{func}"


def _generate_record_id(seed_id: str) -> str:
    return hashlib.sha256(seed_id.encode("utf-8")).hexdigest()[:16]


def _git_head() -> str | None:
    try:
        result = subprocess.run(
            ["git", "rev-parse", "HEAD"],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
        return result.stdout.strip() if result.returncode == 0 else None
    except FileNotFoundError:
        return None


def _git_tree(tree_path: str) -> str | None:
    try:
        result = subprocess.run(
            ["git", "rev-parse", f"HEAD:{tree_path}"],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
        return result.stdout.strip() if result.returncode == 0 else None
    except FileNotFoundError:
        return None


def _source_tree_digest() -> str | None:
    return _git_tree("app/src/main/java")


def _generator_version() -> str:
    src = Path(__file__).read_bytes()
    return hashlib.sha256(src).hexdigest()[:16]


def _legacy_call_ids(sites: list[dict]) -> set[str]:
    return {
        _stable_call_id(s["rel"], s["line"], s["function"])
        for s in sites
        if s["category"] == "LEGACY_EXCEPTION"
    }


def _input_digest(legacy_call_ids: set[str]) -> str:
    payload = "\n".join(sorted(legacy_call_ids)) + "\n"
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


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


def build_census(sites: list[dict]) -> dict:
    categories = ["REGISTRY_FEATURE", "INSTALLER_INFRASTRUCTURE", "API_BRIDGE", "LEGACY_EXCEPTION", "UNKNOWN"]
    counts = {cat: 0 for cat in categories}
    for s in sites:
        counts[s["category"]] += 1

    entries = [
        {
            "callId": _stable_call_id(s["rel"], s["line"], s["function"]),
            "sourceFile": s["rel"],
            "line": s["line"],
            "function": s["function"],
            "category": s["category"],
        }
        for s in sorted(sites, key=lambda x: (x["rel"], x["line"], x["function"]))
    ]

    groups: dict[tuple[str, str], int] = defaultdict(int)
    for s in sites:
        if s["category"] == "LEGACY_EXCEPTION":
            groups[(s["rel"], s["function"])] += 1

    return {
        "schemaVersion": 1,
        "generatedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "sourceCommit": _git_head(),
        "totalCallSites": len(sites),
        "categoryCounts": counts,
        "distinctLegacyGroups": len(groups),
        "entries": entries,
    }


def _census_sets(sites: list[dict]) -> tuple[set[str], dict[str, set[str]], dict[str, set[str]], set[str], set[str]]:
    """Return (all_legacy_call_ids, file_calls, file_functions, typed_owned, infra_calls)."""
    all_legacy: set[str] = set()
    file_calls: dict[str, set[str]] = defaultdict(set)
    file_functions: dict[str, set[str]] = defaultdict(set)
    typed_owned: set[str] = set()
    infra_calls: set[str] = set()

    for s in sites:
        call_id = _stable_call_id(s["rel"], s["line"], s["function"])
        if s["category"] == "LEGACY_EXCEPTION":
            all_legacy.add(call_id)
            file_calls[s["rel"]].add(call_id)
            file_functions[s["rel"]].add(s["function"])
        elif s["category"] == "REGISTRY_FEATURE":
            typed_owned.add(call_id)
        elif s["category"] in ("API_BRIDGE", "INSTALLER_INFRASTRUCTURE"):
            infra_calls.add(call_id)

    return all_legacy, file_calls, file_functions, typed_owned, infra_calls


def build_registry(sites: list[dict]) -> dict:
    legacy = [s for s in sites if s["category"] == "LEGACY_EXCEPTION"]
    all_legacy_ids = _legacy_call_ids(sites)

    groups: dict[tuple[str, str], list[dict]] = defaultdict(list)
    for s in legacy:
        groups[(s["rel"], s["function"])].append(s)

    site_index: dict[tuple[str, str], list[dict]] = defaultdict(list)
    for s in legacy:
        site_index[(s["rel"], s["function"])].append(s)

    records: list[dict] = []
    for seed in FIRST_BATCH_SEEDS:
        rel = seed["sourceFile"]
        funcs = seed.get("ownedFunctions", {seed["entrypoint"]})
        covered: list[str] = []
        for func in funcs:
            for s in site_index.get((rel, func), []):
                covered.append(_stable_call_id(s["rel"], s["line"], s["function"]))

        record_id = _generate_record_id(seed["id"])
        records.append(
            {
                "id": record_id,
                "status": "ACTIVE",
                "owner": seed["owner"],
                "sourceFile": seed["sourceFile"],
                "entrypoint": seed["entrypoint"],
                "process": seed["process"],
                "phase": seed["phase"],
                "preferenceKeys": sorted(seed["preferenceKeys"]),
                "reasonCode": seed["reasonCode"],
                "reason": seed["reason"],
                "coveredCallSites": sorted(
                    covered, key=lambda c: (c.split(":")[0], int(c.split(":")[1]), c.split(":")[2])
                ),
                "hookTargets": sorted(seed["hookTargets"]),
                "testEvidence": sorted(seed["testEvidence"]),
                "exitCondition": seed["exitCondition"],
            }
        )

    records.sort(key=lambda r: r["id"])

    return {
        "schemaVersion": 1,
        "inputDigest": _input_digest(all_legacy_ids),
        "totalLegacyCallSites": len(legacy),
        "totalLegacyGroups": len(groups),
        "firstBatchSize": len(records),
        "records": records,
    }


def validate_repo_relative_posix_path(value: str) -> list[str]:
    """Validate a repository-relative POSIX path. Return a list of error messages."""
    errors: list[str] = []
    if not isinstance(value, str):
        return ["INVALID_SOURCE_FILE_PATH: sourceFile is not a string"]
    if not value:
        return ["INVALID_SOURCE_FILE_PATH: sourceFile is empty"]

    if "\\" in value:
        errors.append("INVALID_SOURCE_FILE_PATH: sourceFile contains backslash (must use POSIX '/')")
    if re.match(r"^[A-Za-z]:", value):
        errors.append("INVALID_SOURCE_FILE_PATH: sourceFile contains a Windows drive letter")
    if value.startswith("//") or value.startswith("\\\\"):
        errors.append("INVALID_SOURCE_FILE_PATH: sourceFile is a UNC path")
    if value.startswith("/"):
        errors.append("INVALID_SOURCE_FILE_PATH: sourceFile is an absolute POSIX path")

    try:
        p = PurePosixPath(value)
    except Exception as exc:
        errors.append(f"INVALID_SOURCE_FILE_PATH: cannot parse as POSIX path: {exc}")
        return errors

    if p.as_posix() != value:
        errors.append("INVALID_SOURCE_FILE_PATH: sourceFile is not normalized (duplicate or dangling separators)")
    if "." in p.parts:
        errors.append("INVALID_SOURCE_FILE_PATH: sourceFile contains '.' segment")
    if ".." in p.parts:
        errors.append("INVALID_SOURCE_FILE_PATH: sourceFile contains '..' segment")
    if p.is_absolute():
        errors.append("INVALID_SOURCE_FILE_PATH: sourceFile is absolute after normalization")

    return errors


def _validate_hook_targets(prefix: str, hook_targets: object) -> list[str]:
    errors: list[str] = []
    if hook_targets is None:
        return [f"{prefix}: missing hookTargets"]
    if not isinstance(hook_targets, list):
        return [f"{prefix}: hookTargets is not a list"]
    if len(hook_targets) == 0:
        errors.append(f"{prefix}: hookTargets is empty")

    seen: set[str] = set()
    for t in hook_targets:
        if not isinstance(t, str):
            errors.append(f"{prefix}: hookTargets contains non-string element")
            continue
        trimmed = t.strip()
        if not trimmed:
            errors.append(f"{prefix}: hookTargets contains empty or whitespace-only target")
        elif t != trimmed:
            errors.append(f"{prefix}: hookTargets target '{t}' has leading or trailing whitespace")
        if trimmed in seen:
            errors.append(f"{prefix}: hookTargets contains duplicate target '{trimmed}'")
        seen.add(trimmed)

    return errors


def _validate_covered_call_sites(
    prefix: str,
    covered: object,
    legacy_call_ids: set[str],
    typed_owned: set[str],
    infra_calls: set[str],
    seen_calls: set[str],
) -> list[str]:
    errors: list[str] = []
    if covered is None:
        return [f"{prefix}: missing coveredCallSites"]
    if not isinstance(covered, list):
        return [f"{prefix}: coveredCallSites is not a list"]
    if len(covered) == 0:
        errors.append(f"{prefix}: coveredCallSites is empty")

    for call_id in covered:
        if not isinstance(call_id, str):
            errors.append(f"{prefix}: coveredCallSites contains non-string element")
            continue
        if not call_id.strip():
            errors.append(f"{prefix}: coveredCallSites contains empty or whitespace-only call id")
            continue

        parts = call_id.split(":")
        if len(parts) != 3 or not parts[0] or not parts[2] or not parts[1].isdigit():
            errors.append(f"{prefix}: coveredCallSites contains malformed call id '{call_id}'")
            continue

        if call_id in seen_calls:
            errors.append(f"{prefix}: call site {call_id} is covered by more than one record")
            continue
        seen_calls.add(call_id)

        if call_id in typed_owned:
            errors.append(f"{prefix}: typed-catalog owned call {call_id} cannot be legacy")
        elif call_id in infra_calls:
            errors.append(
                f"{prefix}: API_BRIDGE/INSTALLER_INFRASTRUCTURE call {call_id} cannot be business exception"
            )
        elif call_id not in legacy_call_ids:
            errors.append(
                f"{prefix}: covered call site {call_id} is not present in the current LEGACY_EXCEPTION census"
            )

    return errors


def validate(registry: dict, strict: bool = True) -> list[str]:
    errors: list[str] = []

    records = registry.get("records", [])
    seen_ids: set[str] = set()
    seen_calls: set[str] = set()

    sites = scan_legacy_call_sites()
    all_legacy, file_legacy_calls, file_legacy_functions, typed_owned, infra_calls = _census_sets(sites)

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
            "hookTargets",
        ):
            if field not in rec:
                errors.append(f"{prefix}: missing field '{field}'")

        if not rec.get("owner"):
            errors.append(f"{prefix}: owner is empty")
        if not rec.get("sourceFile"):
            errors.append(f"{prefix}: sourceFile is empty")
        if not rec.get("exitCondition"):
            errors.append(f"{prefix}: exitCondition is empty")
        if rec.get("exitCondition", "").lower() == "never":
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

        # sourceFile path validation
        source_file = rec.get("sourceFile", "")
        if source_file:
            path_errors = validate_repo_relative_posix_path(source_file)
            for pe in path_errors:
                errors.append(f"{prefix}: {pe}")

            if not path_errors:
                path = SOURCE_ROOT / source_file
                if not path.exists():
                    errors.append(f"{prefix}: sourceFile does not exist '{source_file}'")
                else:
                    text = path.read_text(encoding="utf-8")
                    if rec.get("entrypoint") and rec["entrypoint"] not in text:
                        errors.append(
                            f"{prefix}: entrypoint function '{rec['entrypoint']}' not found in {source_file}"
                        )

        # hookTargets validation
        hook_errors = _validate_hook_targets(prefix, rec.get("hookTargets"))
        errors.extend(hook_errors)

        # coveredCallSites validation
        covered_errors = _validate_covered_call_sites(
            prefix,
            rec.get("coveredCallSites"),
            all_legacy,
            typed_owned,
            infra_calls,
            seen_calls,
        )
        errors.extend(covered_errors)

        # whole-file / whole-function gate
        covered = rec.get("coveredCallSites")
        if isinstance(covered, list) and source_file and source_file not in (None, ""):
            record_calls: set[str] = set()
            for call_id in covered:
                if isinstance(call_id, str) and len(call_id.split(":")) == 3:
                    record_calls.add(call_id)

            file_calls = file_legacy_calls.get(source_file, set())
            file_funcs = file_legacy_functions.get(source_file, set())
            # Whole-file/whole-function is only abusive when the file has more than
            # one legacy logical owner. A file with a single legacy function is
            # allowed to be covered by one record.
            if len(file_funcs) > 1:
                if file_calls and record_calls == file_calls:
                    errors.append(
                        f"{prefix}: WHOLE_FILE_LEGACY_EXCEPTION_FORBIDDEN: record covers all "
                        f"{len(file_calls)} legacy call sites in {source_file}"
                    )
                else:
                    record_funcs = {cid.split(":")[2] for cid in record_calls}
                    if file_funcs and record_funcs == file_funcs:
                        errors.append(
                            f"{prefix}: WHOLE_FILE_LEGACY_EXCEPTION_FORBIDDEN: record covers all "
                            f"legacy functions in {source_file}"
                        )

    # all-legacy batch gate
    if all_legacy:
        union_covered: set[str] = set()
        for rec in records:
            covered = rec.get("coveredCallSites")
            if isinstance(covered, list):
                for call_id in covered:
                    if isinstance(call_id, str):
                        union_covered.add(call_id)
        if union_covered == all_legacy:
            errors.append(
                f"ALL_LEGACY_CALLS_BATCH_FORBIDDEN: first batch covers all "
                f"{len(all_legacy)} legacy call sites"
            )

    if strict and errors:
        return errors
    return errors


def _canonical(registry: dict) -> dict:
    """Return a canonical copy of the registry with volatile provenance removed."""
    reg = copy.deepcopy(registry)
    for key in ("generatedAt", "sourceCommit", "sourceTree", "generatorVersion"):
        reg.pop(key, None)

    records = reg.get("records", [])
    for rec in records:
        for field in ("preferenceKeys", "hookTargets", "testEvidence", "coveredCallSites"):
            if isinstance(rec.get(field), list):
                rec[field] = sorted(rec[field])
    reg["records"] = sorted(records, key=lambda r: r.get("id", ""))
    return reg


def canonical_diff(expected: dict, actual: dict) -> list[str]:
    """Return human-readable differences between two canonical registries."""
    e = _canonical(expected)
    a = _canonical(actual)
    errors: list[str] = []

    top_fields = ("schemaVersion", "totalLegacyCallSites", "totalLegacyGroups", "firstBatchSize", "inputDigest")
    for f in top_fields:
        if a.get(f) != e.get(f):
            errors.append(
                f"REGISTRY_STALE: top-level '{f}' differs: expected {e.get(f)!r}, actual {a.get(f)!r}"
            )

    e_records = {r["id"]: r for r in e.get("records", [])}
    a_records = {r["id"]: r for r in a.get("records", [])}
    e_ids = set(e_records)
    a_ids = set(a_records)

    for rid in sorted(e_ids - a_ids):
        errors.append(f"REGISTRY_STALE: expected record {rid} is missing in committed registry")
    for rid in sorted(a_ids - e_ids):
        errors.append(f"REGISTRY_STALE: unexpected record {rid} in committed registry")

    for rid in sorted(e_ids & a_ids):
        er = e_records[rid]
        ar = a_records[rid]
        for f in sorted(er.keys()):
            if f not in ar:
                errors.append(f"REGISTRY_STALE: record {rid}: missing field '{f}'")
                continue
            if er[f] != ar[f]:
                # Keep the diff readable: stringify a short preview.
                ev = er[f]
                av = ar[f]
                if isinstance(ev, list) and isinstance(av, list):
                    errors.append(
                        f"REGISTRY_STALE: record {rid}: field '{f}' differs "
                        f"(expected length {len(ev)}, actual length {len(av)})"
                    )
                else:
                    errors.append(
                        f"REGISTRY_STALE: record {rid}: field '{f}' differs: expected {ev!r}, actual {av!r}"
                    )
        for f in sorted(ar.keys()):
            if f not in er:
                errors.append(f"REGISTRY_STALE: record {rid}: extra field '{f}'")

    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--build", action="store_true", help="write the registry")
    group.add_argument("--check", action="store_true", help="validate the existing registry")
    group.add_argument("--census", action="store_true", help="write the call site census")
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

        sites = scan_legacy_call_sites()
        expected = build_registry(sites)
        diffs = canonical_diff(expected, registry)
        if diffs:
            for d in diffs:
                print(f"STALE: {d}", file=sys.stderr)
            return 1

        print(f"Registry canonical and up-to-date: {len(registry.get('records', []))} records")
        return 0

    if args.census:
        census = build_census(scan_legacy_call_sites())
        CENSUS_FILE.parent.mkdir(parents=True, exist_ok=True)
        with CENSUS_FILE.open("w", encoding="utf-8", newline="\n") as f:
            json.dump(census, f, indent=2, ensure_ascii=False)
            f.write("\n")
        print(f"Wrote {CENSUS_FILE}")
        return 0

    sites = scan_legacy_call_sites()
    registry = build_registry(sites)

    head = _git_head()
    if head:
        registry["sourceCommit"] = head
    source_tree = _source_tree_digest()
    if source_tree:
        registry["sourceTree"] = source_tree
    registry["generatorVersion"] = _generator_version()
    registry["generatedAt"] = datetime.now(timezone.utc).replace(microsecond=0).isoformat()

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


if __name__ == "__main__":
    raise SystemExit(main())
