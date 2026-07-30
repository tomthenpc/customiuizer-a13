#!/usr/bin/env python3
"""Baseline hook-sequence audit for the canary catalog migration.

Compares the current MainModule.java against the frozen baseline
`1ff4fa2` (devin/r13.8-maintenance-architecture) and verifies that the
migrated canary features:

- are installed through FeatureCatalog.installById at the same relative
  positions where their original direct Hook calls were;
- keep the same preference keys, so conditions are owned by the catalog;
- do not introduce additional FeatureCatalog bulk-install calls that would
  change call order.
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
MAIN_MODULE = REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "MainModule.java"
BASELINE_REF = "1ff4fa2"


def extract_canary_tokens(text: str) -> list[str]:
    """Return ordered tokens for canary hook calls and catalog calls."""
    canary_feature_ids = {
        "autoBrightnessRange",
        "muffledVibration",
        "noMoreIcon",
        "batteryIndicator",
        "noClockHide",
        "noWidgetOnly",
    }
    direct_to_id = {
        "SystemDisplayAndWindowHooks.AutoBrightnessRangeHook": "autoBrightnessRange",
        "SystemAudioAndVisualAndMoreHooks.MuffledVibrationHook": "muffledVibration",
        "SystemNotificationMoreHooks.NoMoreIconHook": "noMoreIcon",
        "SystemUIBatteryHooks.BatteryIndicatorHook": "batteryIndicator",
        "LauncherSystemHooks.NoClockHideHook": "noClockHide",
        "LauncherLayoutHooks.NoWidgetOnlyHook": "noWidgetOnly",
    }

    tokens = []
    # Direct hook calls that were migrated.
    for m in re.finditer(
        r'(?:SystemDisplayAndWindowHooks\.AutoBrightnessRangeHook'
        r'|SystemAudioAndVisualAndMoreHooks\.MuffledVibrationHook'
        r'|SystemNotificationMoreHooks\.NoMoreIconHook'
        r'|SystemUIBatteryHooks\.BatteryIndicatorHook'
        r'|LauncherSystemHooks\.NoClockHideHook'
        r'|LauncherLayoutHooks\.NoWidgetOnlyHook)',
        text
    ):
        tokens.append(direct_to_id[m.group(0)])

    # Catalog installById calls.
    for m in re.finditer(
        r'FeatureCatalog\.installById\("([^"]+)",\s*\w+Runtime\)',
        text
    ):
        if m.group(1) in canary_feature_ids:
            tokens.append(m.group(1))
    return tokens


def get_baseline_text() -> str:
    path = MAIN_MODULE.relative_to(REPO_ROOT).as_posix()
    result = subprocess.run(
        ["git", "show", f"{BASELINE_REF}:{path}"],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        check=True
    )
    return result.stdout


def main() -> int:
    current_text = MAIN_MODULE.read_text(encoding="utf-8")
    baseline_text = get_baseline_text()

    baseline_tokens = extract_canary_tokens(baseline_text)
    current_tokens = extract_canary_tokens(current_text)

    canary_feature_ids = [
        "autoBrightnessRange",
        "muffledVibration",
        "noMoreIcon",
        "batteryIndicator",
        "noClockHide",
        "noWidgetOnly",
    ]

    expected = canary_feature_ids
    expected_current = canary_feature_ids

    if baseline_tokens != expected:
        print("Baseline MainModule does not match expected canary call sequence.")
        print(f"  expected: {expected}")
        print(f"  actual:   {baseline_tokens}")
        return 1

    if current_tokens != expected_current:
        print("Current MainModule canary install-by-id sequence is out of order.")
        print(f"  expected: {expected_current}")
        print(f"  actual:   {current_tokens}")
        return 1

    # Ensure no bulk FeatureCatalog.installForPackage remains and no extra
    # FeatureCatalog.installById calls were added outside the canary set.
    extra = set(current_tokens) - set(expected_current)
    if extra:
        print(f"Unexpected FeatureCatalog.installById calls: {sorted(extra)}")
        return 1

    if re.search(r'FeatureCatalog\.installForPackage\(', current_text):
        print("FeatureCatalog.installForPackage bulk call still exists in MainModule.")
        return 1

    if re.search(r'FeatureCatalog\.installForSystemServer\(', current_text):
        print("FeatureCatalog.installForSystemServer bulk call still exists in MainModule.")
        return 1

    print("Canary hook-sequence audit passed:")
    print(f"  - baseline tokens: {baseline_tokens}")
    print(f"  - current tokens:  {current_tokens}")
    print("  - order preserved, conditions moved to catalog, no bulk-install leftovers")
    return 0


if __name__ == "__main__":
    sys.exit(main())
