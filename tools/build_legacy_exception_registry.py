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

ALLOWED_ACTIVATION_MODES = {
    "UNCONDITIONAL",
    "ANY_OF",
}

ALLOWED_PREDICATE_KINDS = {
    "BOOLEAN_KEY_TRUE",
    "INT_KEY_GT",
    "DYNAMIC_SUFFIX_INT_GT",
    "FIXED_INT_ANY_GT_AND_NONEMPTY_SET",
}

PREDICATE_REQUIRED_FIELDS: dict[str, set[str]] = {
    "BOOLEAN_KEY_TRUE": {"key"},
    "INT_KEY_GT": {"key", "thresholdExclusive"},
    "DYNAMIC_SUFFIX_INT_GT": {"keySuffix", "thresholdExclusive", "valueType"},
    "FIXED_INT_ANY_GT_AND_NONEMPTY_SET": {"integerKeys", "thresholdExclusive", "requiredNonEmptySetKey"},
}

PREDICATE_ALLOWED_FIELDS: dict[str, set[str]] = {
    "BOOLEAN_KEY_TRUE": {"kind", "key"},
    "INT_KEY_GT": {"kind", "key", "thresholdExclusive"},
    "DYNAMIC_SUFFIX_INT_GT": {"kind", "keySuffix", "thresholdExclusive", "valueType"},
    "FIXED_INT_ANY_GT_AND_NONEMPTY_SET": {"kind", "integerKeys", "thresholdExclusive", "requiredNonEmptySetKey"},
}

ALLOWED_VALUE_TYPES = {"INTEGER"}

LEGACY_EXCEPTION_SEEDS: list[dict] = [
    {
        "id": "legacy-separatevolume-systemui",
        "batch": "P3.3A",
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
        "batch": "P3.3A",
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
        "batch": "P3.3A",
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
        "batch": "P3.3A",
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
    {
        "id": "legacy-globalactions-systemserver",
        "batch": "P3.3B",
        "owner": "GlobalActions.setupGlobalActions",
        "sourceFile": "tv/withaibuild/customiuizer/mods/GlobalActions.kt",
        "entrypoint": "setupGlobalActions",
        "process": "system_server",
        "phase": "SYSTEM_SERVER_STARTING",
        "preferenceKeys": [
            "controls_volumemedia_down",
            "controls_volumemedia_up",
            "controls_mediaplayer_apps",
        ],
        "activationContract": {
            "mode": "ANY_OF",
            "predicates": [
                {
                    "kind": "DYNAMIC_SUFFIX_INT_GT",
                    "keySuffix": "_action",
                    "thresholdExclusive": 1,
                    "valueType": "INTEGER",
                },
                {
                    "kind": "FIXED_INT_ANY_GT_AND_NONEMPTY_SET",
                    "integerKeys": ["controls_volumemedia_up", "controls_volumemedia_down"],
                    "thresholdExclusive": 0,
                    "requiredNonEmptySetKey": "controls_mediaplayer_apps",
                },
            ],
        },
        "reasonCode": "LIFECYCLE_BOOTSTRAP",
        "reason": (
            "GlobalActions.setupGlobalActions is installed at SYSTEM_SERVER_STARTING "
            "when the installer activation predicate is satisfied. The predicate is a "
            "disjunction: (A) any SharedPreferences entry whose key ends with '_action' "
            "and whose runtime value is an Integer greater than 1, or (B) any of the "
            "media volume shortcut keys (controls_volumemedia_up / "
            "controls_volumemedia_down) is greater than 0 and the media player app set "
            "(controls_mediaplayer_apps) is non-empty. The fixed literal keys needed by "
            "the activation predicate are listed in preferenceKeys; the dynamic "
            "'_action' key domain is expressed by the DYNAMIC_SUFFIX_INT_GT "
            "activationContract and is not enumerated in preferenceKeys. It hooks "
            "AccessibilityManagerService construction and "
            "BaseMiuiPhoneWindowManager#initInternal to register cross-process "
            "broadcast receivers before the system finishes booting. There is no single "
            "typed FeatureSpec that owns both the lifecycle bootstrap and the "
            "cross-process action dispatch surface today."
        ),
        "ownedFunctions": {"setupGlobalActions"},
        "hookTargets": [
            "com.android.server.accessibility.AccessibilityManagerService#<init>",
            "com.android.server.policy.BaseMiuiPhoneWindowManager#initInternal",
        ],
        "testEvidence": [
            "tools/tests/test_legacy_exception_registry.py",
            "tools/tests/test_p33b_legacy_exception_routes.py",
        ],
        "exitCondition": (
            "Migrate each global action to a typed FeatureSpec that declares its "
            "source process, broadcast contract, and the exact system service hook "
            "it needs; remove this exception when all actions are installed by typed "
            "dispatchers and the global AccessibilityManagerService / "
            "BaseMiuiPhoneWindowManager hooks are no longer needed (P3.3C+)."
        ),
    },
    {
        "id": "legacy-globalactions-statusbar",
        "batch": "P3.3B",
        "owner": "GlobalActions.setupStatusBar",
        "sourceFile": "tv/withaibuild/customiuizer/mods/GlobalActions.kt",
        "entrypoint": "setupStatusBar",
        "process": "system_ui",
        "phase": "PACKAGE_READY",
        "preferenceKeys": [],
        "activationContract": {
            "mode": "UNCONDITIONAL",
        },
        "reasonCode": "CROSS_PROCESS",
        "reason": (
            "GlobalActions.setupStatusBar is installed unconditionally when the "
            "SystemUI package is ready (com.android.systemui). It hooks "
            "CentralSurfacesImpl#start to register a status-bar broadcast receiver "
            "that handles cross-process actions (expand notifications, toggle GPS, "
            "etc.). It cannot be a single typed Feature today because the receiver "
            "covers multiple unrelated actions and spans the SystemUI lifecycle. "
            "The activationContract is UNCONDITIONAL and preferenceKeys is empty "
            "because there is no per-user preference gate."
        ),
        "ownedFunctions": {"setupStatusBar"},
        "hookTargets": [
            "com.android.systemui.statusbar.phone.CentralSurfacesImpl#start",
        ],
        "testEvidence": ["tools/tests/test_p33b_legacy_exception_routes.py"],
        "exitCondition": (
            "Migrate the status-bar global action receiver to a typed SystemUI "
            "FeatureSpec that owns CentralSurfacesImpl#start and declares the "
            "cross-process broadcast contract (P3.3C+)."
        ),
    },
    {
        "id": "legacy-globalactions-foreground-monitor",
        "batch": "P3.3B",
        "owner": "GlobalActions.setupForegroundMonitor",
        "sourceFile": "tv/withaibuild/customiuizer/mods/GlobalActions.kt",
        "entrypoint": "setupForegroundMonitor",
        "process": "system_ui",
        "phase": "PACKAGE_READY",
        "preferenceKeys": ["various_showcallui", "controls_volumecursor"],
        "activationContract": {
            "mode": "ANY_OF",
            "predicates": [
                {
                    "kind": "INT_KEY_GT",
                    "key": "various_showcallui",
                    "thresholdExclusive": 0,
                },
                {
                    "kind": "BOOLEAN_KEY_TRUE",
                    "key": "controls_volumecursor",
                },
            ],
        },
        "callSiteConditions": {
            "tv/withaibuild/customiuizer/mods/GlobalActions.kt:759:setupForegroundMonitor": {
                "kind": "INT_KEY_GT",
                "key": "various_showcallui",
                "thresholdExclusive": 0,
            },
        },
        "reasonCode": "CROSS_PROCESS",
        "reason": (
            "GlobalActions.setupForegroundMonitor is installed in SystemUI when "
            "various_showcallui is greater than 0 or controls_volumecursor is true. "
            "It observes the foreground package and fullscreen state and writes them "
            "into Settings.Global for cross-process consumption. The first two hooks "
            "(NetworkSpeedController construction and MiuiActivityUtil#updateTopActivity) "
            "are installed whenever the entrypoint is called; the third hook "
            "(StatusBarStateControllerImpl#setSystemBarAttributes) is installed only "
            "inside the various_showcallui > 0 branch and therefore has a per-call-site "
            "condition."
        ),
        "ownedFunctions": {"setupForegroundMonitor"},
        "hookTargets": [
            "com.android.systemui.statusbar.policy.NetworkSpeedController#<init>",
            "com.miui.systemui.util.MiuiActivityUtil#updateTopActivity",
            "com.android.systemui.statusbar.StatusBarStateControllerImpl#setSystemBarAttributes",
        ],
        "testEvidence": ["tools/tests/test_p33b_legacy_exception_routes.py"],
        "exitCondition": (
            "Migrate foreground package and fullscreen monitoring to a typed "
            "SystemUI FeatureSpec that owns NetworkSpeedController, MiuiActivityUtil, "
            "and StatusBarStateControllerImpl with explicit lifecycle boundaries "
            "(P3.3C+)."
        ),
    },
    {
        "id": "legacy-alarmcompat-service",
        "batch": "P3.3B",
        "owner": "AlarmCompatServiceHook",
        "sourceFile": "tv/withaibuild/customiuizer/mods/Various.kt",
        "entrypoint": "AlarmCompatServiceHook",
        "process": "system_server",
        "phase": "SYSTEM_SERVER_STARTING",
        "preferenceKeys": ["various_alarmcompat", "various_alarmcompat_apps"],
        "activationContract": {
            "mode": "ANY_OF",
            "predicates": [
                {
                    "kind": "BOOLEAN_KEY_TRUE",
                    "key": "various_alarmcompat",
                },
            ],
        },
        "reasonCode": "LIFECYCLE_BOOTSTRAP",
        "reason": (
            "AlarmCompatServiceHook is installed at SYSTEM_SERVER_STARTING when "
            "various_alarmcompat is true. It hooks AlarmManagerService#onBootPhase "
            "(phase 500) to register a ContentObserver for next_alarm_clock_formatted "
            "and AlarmManagerService#getNextAlarmClockImpl to return a synthetic "
            "alarm for selected apps. The various_alarmcompat_apps key is a runtime "
            "allowlist configuration used inside the hooked functions, not the "
            "installer activation gate. The feature is tied to the ROM-specific "
            "AlarmManagerService lifecycle and cannot be expressed as a typed "
            "Feature today."
        ),
        "ownedFunctions": {"AlarmCompatServiceHook"},
        "hookTargets": [
            "com.android.server.alarm.AlarmManagerService#onBootPhase",
            "com.android.server.alarm.AlarmManagerService#getNextAlarmClockImpl",
        ],
        "testEvidence": ["tools/tests/test_p33b_legacy_exception_routes.py"],
        "exitCondition": (
            "Migrate alarm compatibility to a typed system_server FeatureSpec that "
            "declares the AlarmManagerService lifecycle hooks and a per-app allowlist "
            "contract (P3.3C+)."
        ),
    },
]


def _stable_call_id(rel: str, line: int, func: str) -> str:
    return f"{rel}:{line}:{func}"


def _generate_record_id(seed_id: str) -> str:
    return hashlib.sha256(seed_id.encode("utf-8")).hexdigest()[:16]


def _canonical_activation_contract(contract: dict) -> dict:
    """Return a deterministic, deep copy of an activation contract."""
    if not isinstance(contract, dict):
        return contract
    canonical = copy.deepcopy(contract)
    if canonical.get("mode") == "UNCONDITIONAL":
        canonical.pop("predicates", None)
        return canonical
    predicates = canonical.get("predicates", [])
    if isinstance(predicates, list):
        for p in predicates:
            if isinstance(p, dict) and "integerKeys" in p and isinstance(p["integerKeys"], list):
                p["integerKeys"] = sorted(p["integerKeys"])
        canonical["predicates"] = sorted(predicates, key=lambda p: (p.get("kind", ""), json.dumps(p, sort_keys=True, default=str)))
    return canonical


def _canonical_call_site_conditions(conditions: dict) -> dict:
    """Return a deterministic, deep copy of call-site conditions."""
    if not isinstance(conditions, dict):
        return conditions
    canonical = copy.deepcopy(conditions)
    return dict(sorted(canonical.items()))


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
    batch_counts: dict[str, int] = defaultdict(int)
    for seed in LEGACY_EXCEPTION_SEEDS:
        rel = seed["sourceFile"]
        funcs = seed.get("ownedFunctions", {seed["entrypoint"]})
        covered: list[str] = []
        for func in funcs:
            for s in site_index.get((rel, func), []):
                covered.append(_stable_call_id(s["rel"], s["line"], s["function"]))

        record_id = _generate_record_id(seed["id"])
        batch = seed.get("batch", "P3.3A")
        batch_counts[batch] += 1
        record: dict[str, Any] = {
            "id": record_id,
            "batch": batch,
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
        if "activationContract" in seed:
            record["activationContract"] = _canonical_activation_contract(seed["activationContract"])
        if "callSiteConditions" in seed:
            record["callSiteConditions"] = _canonical_call_site_conditions(seed["callSiteConditions"])
        records.append(record)

    records.sort(key=lambda r: r["id"])

    first_batch_size = batch_counts.get("P3.3A", 0)

    return {
        "schemaVersion": 3,
        "inputDigest": _input_digest(all_legacy_ids),
        "totalLegacyCallSites": len(legacy),
        "totalLegacyGroups": len(groups),
        "firstBatchSize": first_batch_size,
        "registeredRecordCount": len(records),
        "batchCounts": dict(sorted(batch_counts.items())),
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


def _validate_activation_contract(prefix: str, contract: object) -> list[str]:
    errors: list[str] = []
    if not isinstance(contract, dict):
        errors.append(f"{prefix}: activationContract is not a dict")
        return errors

    mode = contract.get("mode")
    if mode not in ALLOWED_ACTIVATION_MODES:
        errors.append(f"{prefix}: unknown activationContract mode '{mode}'")

    if mode == "UNCONDITIONAL":
        if "predicates" in contract:
            errors.append(f"{prefix}: UNCONDITIONAL activationContract must not contain predicates")
        return errors

    predicates = contract.get("predicates")
    if not isinstance(predicates, list) or not predicates:
        errors.append(f"{prefix}: activationContract predicates must be a non-empty list")
        return errors

    seen_predicates: set[str] = set()
    for i, p in enumerate(predicates):
        if not isinstance(p, dict):
            errors.append(f"{prefix}: activationContract predicate[{i}] is not a dict")
            continue
        kind = p.get("kind")
        if kind not in ALLOWED_PREDICATE_KINDS:
            errors.append(f"{prefix}: unknown predicate kind '{kind}'")
            continue

        required = PREDICATE_REQUIRED_FIELDS[kind]
        for r in required:
            if r not in p:
                errors.append(f"{prefix}: activationContract predicate[{i}] missing required field '{r}'")

        allowed = PREDICATE_ALLOWED_FIELDS[kind]
        for k in p.keys():
            if k not in allowed:
                errors.append(f"{prefix}: activationContract predicate[{i}] has unknown field '{k}'")

        if kind == "DYNAMIC_SUFFIX_INT_GT":
            suffix = p.get("keySuffix")
            if not isinstance(suffix, str) or not suffix:
                errors.append(f"{prefix}: DYNAMIC_SUFFIX_INT_GT keySuffix must be a non-empty string")
            value_type = p.get("valueType")
            if value_type not in ALLOWED_VALUE_TYPES:
                errors.append(f"{prefix}: DYNAMIC_SUFFIX_INT_GT valueType must be one of {ALLOWED_VALUE_TYPES}, got {value_type!r}")
        elif kind == "FIXED_INT_ANY_GT_AND_NONEMPTY_SET":
            int_keys = p.get("integerKeys")
            if not isinstance(int_keys, list) or not int_keys:
                errors.append(f"{prefix}: FIXED_INT_ANY_GT_AND_NONEMPTY_SET integerKeys must be a non-empty list")
            else:
                for k in int_keys:
                    if not isinstance(k, str) or not k:
                        errors.append(f"{prefix}: FIXED_INT_ANY_GT_AND_NONEMPTY_SET integerKeys must be non-empty strings")
            if not isinstance(p.get("requiredNonEmptySetKey"), str) or not p.get("requiredNonEmptySetKey"):
                errors.append(f"{prefix}: FIXED_INT_ANY_GT_AND_NONEMPTY_SET requiredNonEmptySetKey must be a non-empty string")
        elif kind == "INT_KEY_GT":
            if not isinstance(p.get("key"), str) or not p.get("key"):
                errors.append(f"{prefix}: INT_KEY_GT key must be a non-empty string")
            if not isinstance(p.get("thresholdExclusive"), int):
                errors.append(f"{prefix}: INT_KEY_GT thresholdExclusive must be an integer")
        elif kind == "BOOLEAN_KEY_TRUE":
            if not isinstance(p.get("key"), str) or not p.get("key"):
                errors.append(f"{prefix}: BOOLEAN_KEY_TRUE key must be a non-empty string")

        pred_id = f"{kind}:{json.dumps(p, sort_keys=True, default=str)}"
        if pred_id in seen_predicates:
            errors.append(f"{prefix}: duplicate activationContract predicate")
        seen_predicates.add(pred_id)

    return errors


def _validate_call_site_conditions(
    prefix: str,
    conditions: object,
    covered: list[str],
    preference_keys: list[str],
) -> list[str]:
    errors: list[str] = []
    if not isinstance(conditions, dict):
        errors.append(f"{prefix}: callSiteConditions is not a dict")
        return errors

    covered_set = set(covered) if isinstance(covered, list) else set()
    preference_set = set(preference_keys) if isinstance(preference_keys, list) else set()

    for call_id, cond in conditions.items():
        if not isinstance(call_id, str):
            errors.append(f"{prefix}: callSiteConditions key is not a string")
            continue
        if call_id not in covered_set:
            errors.append(f"{prefix}: callSiteConditions key '{call_id}' is not in coveredCallSites")
            continue

        if not isinstance(cond, dict):
            errors.append(f"{prefix}: callSiteConditions '{call_id}' condition is not a dict")
            continue

        kind = cond.get("kind")
        if kind not in ALLOWED_PREDICATE_KINDS:
            errors.append(f"{prefix}: callSiteConditions '{call_id}' unknown kind '{kind}'")
            continue

        required = PREDICATE_REQUIRED_FIELDS[kind]
        for r in required:
            if r not in cond:
                errors.append(f"{prefix}: callSiteConditions '{call_id}' missing required field '{r}'")

        allowed = PREDICATE_ALLOWED_FIELDS[kind]
        for k in cond.keys():
            if k not in allowed:
                errors.append(f"{prefix}: callSiteConditions '{call_id}' has unknown field '{k}'")

        if kind == "INT_KEY_GT":
            key = cond.get("key")
            if not isinstance(key, str) or not key:
                errors.append(f"{prefix}: callSiteConditions '{call_id}' INT_KEY_GT key must be a non-empty string")
            elif key not in preference_set:
                errors.append(f"{prefix}: callSiteConditions '{call_id}' uses key '{key}' not in preferenceKeys")
            if not isinstance(cond.get("thresholdExclusive"), int):
                errors.append(f"{prefix}: callSiteConditions '{call_id}' INT_KEY_GT thresholdExclusive must be an integer")
        elif kind == "BOOLEAN_KEY_TRUE":
            key = cond.get("key")
            if not isinstance(key, str) or not key:
                errors.append(f"{prefix}: callSiteConditions '{call_id}' BOOLEAN_KEY_TRUE key must be a non-empty string")
            elif key not in preference_set:
                errors.append(f"{prefix}: callSiteConditions '{call_id}' uses key '{key}' not in preferenceKeys")

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
            "batch",
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

        if rec.get("batch") not in ("P3.3A", "P3.3B"):
            errors.append(f"{prefix}: batch must be 'P3.3A' or 'P3.3B', got {rec.get('batch')!r}")

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

        # activationContract / callSiteConditions validation
        if "activationContract" in rec:
            errors.extend(_validate_activation_contract(prefix, rec["activationContract"]))

            # preferenceKeys must not enumerate dynamic suffix keys
            ac = rec.get("activationContract") or {}
            pred_kinds = {p.get("kind") for p in ac.get("predicates", []) if isinstance(p, dict)}
            if "DYNAMIC_SUFFIX_INT_GT" in pred_kinds:
                for pk in rec.get("preferenceKeys", []):
                    if isinstance(pk, str) and pk.endswith(ac.get("predicates", [{}])[0].get("keySuffix", "")):
                        # Check if this specific key suffix is the dynamic one
                        for p in ac.get("predicates", []):
                            if p.get("kind") == "DYNAMIC_SUFFIX_INT_GT" and isinstance(p.get("keySuffix"), str):
                                if pk.endswith(p["keySuffix"]):
                                    errors.append(
                                        f"{prefix}: preferenceKeys must not enumerate dynamic suffix key '{pk}'; "
                                        "it is already covered by activationContract DYNAMIC_SUFFIX_INT_GT"
                                    )
                                    break

        if "callSiteConditions" in rec:
            errors.extend(_validate_call_site_conditions(
                prefix,
                rec["callSiteConditions"],
                rec.get("coveredCallSites", []),
                rec.get("preferenceKeys", []),
            ))

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
        if isinstance(rec.get("activationContract"), dict):
            rec["activationContract"] = _canonical_activation_contract(rec["activationContract"])
        if isinstance(rec.get("callSiteConditions"), dict):
            rec["callSiteConditions"] = _canonical_call_site_conditions(rec["callSiteConditions"])
    reg["records"] = sorted(records, key=lambda r: r.get("id", ""))
    return reg


def canonical_diff(expected: dict, actual: dict) -> list[str]:
    """Return human-readable differences between two canonical registries."""
    e = _canonical(expected)
    a = _canonical(actual)
    errors: list[str] = []

    top_fields = ("schemaVersion", "totalLegacyCallSites", "totalLegacyGroups", "firstBatchSize", "registeredRecordCount", "batchCounts", "inputDigest")
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
    print(f"  schema version:          {registry['schemaVersion']}")
    print(f"  total legacy call sites: {registry['totalLegacyCallSites']}")
    print(f"  total legacy groups:     {registry['totalLegacyGroups']}")
    print(f"  first batch records:     {registry['firstBatchSize']}")
    print(f"  registered records:      {registry['registeredRecordCount']}")
    print(f"  batch counts:            {registry['batchCounts']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
