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
import hashlib
import json
import sys
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path

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


def build_registry(sites: list[dict]) -> dict:
    legacy = [s for s in sites if s["category"] == "LEGACY_EXCEPTION"]

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
                "preferenceKeys": seed["preferenceKeys"],
                "reasonCode": seed["reasonCode"],
                "reason": seed["reason"],
                "coveredCallSites": sorted(
                    covered, key=lambda c: (c.split(":")[0], int(c.split(":")[1]), c.split(":")[2])
                ),
                "hookTargets": seed["hookTargets"],
                "testEvidence": seed["testEvidence"],
                "exitCondition": seed["exitCondition"],
            }
        )

    records.sort(key=lambda r: r["id"])

    return {
        "schemaVersion": 1,
        "generatedAt": None,
        "sourceCommit": None,
        "totalLegacyCallSites": len(legacy),
        "totalLegacyGroups": len(groups),
        "firstBatchSize": len(records),
        "records": records,
    }


def validate(registry: dict, strict: bool = True) -> list[str]:
    errors: list[str] = []

    records = registry.get("records", [])
    seen_ids: set[str] = set()
    seen_calls: set[str] = set()
    typed_owned: set[str] = set()
    infra_calls: set[str] = set()

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


def _git_head() -> str | None:
    import subprocess
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
        print(f"Registry valid: {len(registry.get('records', []))} records")
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
