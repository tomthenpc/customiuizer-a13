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
BASELINE_REF = "origin/devin/r13.8-catalog-expansion-batch-2"


def extract_catalog_tokens(text: str) -> list[str]:
    """Return ordered tokens for all catalog feature calls (direct or via catalog)."""
    catalog_feature_ids = {
        "packagePermissions",
        "autoBrightnessRange",
        "muffledVibration",
        "statusBarClockTweak",
        "noMoreIcon",
        "batteryIndicator",
        "noClockHide",
        "noWidgetOnly",
        "screenDimTime",
        "firstVolumePress",
        "networkIndicatorWifi",
        "muteVisibleNotifications",
        "hideLauncherTitles",
        "fixAppInfoLaunch",
        "hideProximityWarning",
        "clearAllTasks",
        "hideDismissView",
        "hideLockScreenHint",
        "folderColumns",
        "titleTopMargin",
        "noLightUpOnCharge",
        "allRotations",
        "noNetworkSpeedSeparator",
        "hideIconsClock",
        "noUnlockAnimation",
    }
    direct_to_id = {
        "PackagePermissions.hook": "packagePermissions",
        "SystemStatusBarClockAndMoreHooks.StatusBarClockTweakHook": "statusBarClockTweak",
        "SystemDisplayAndWindowHooks.AutoBrightnessRangeHook": "autoBrightnessRange",
        "SystemAudioAndVisualAndMoreHooks.MuffledVibrationHook": "muffledVibration",
        "SystemNotificationMoreHooks.NoMoreIconHook": "noMoreIcon",
        "SystemUIBatteryHooks.BatteryIndicatorHook": "batteryIndicator",
        "LauncherSystemHooks.NoClockHideHook": "noClockHide",
        "LauncherLayoutHooks.NoWidgetOnlyHook": "noWidgetOnly",
        "SystemAudioAndVisualAndMoreHooks.ScreenDimTimeHook": "screenDimTime",
        "SystemAudioAndVisualAndMoreHooks.FirstVolumePressHook": "firstVolumePress",
        "SystemStatusBarMoreHooks.NetworkIndicatorWifi": "networkIndicatorWifi",
        "SystemNotificationMoreHooks.MuteVisibleNotificationsHook": "muteVisibleNotifications",
        "LauncherIconHooks.HideTitlesHook": "hideLauncherTitles",
        "LauncherSystemHooks.FixAppInfoLaunchHook": "fixAppInfoLaunch",
        "SystemDisplayAndWindowHooks.HideProximityWarningHook": "hideProximityWarning",
        "SystemAudioAndVisualAndMoreHooks.ClearAllTasksHook": "clearAllTasks",
        "SystemUINotificationHooks.HideDismissViewHook": "hideDismissView",
        "SystemLockScreenMoreHooks.HideLockScreenHintHook": "hideLockScreenHint",
        "LauncherFolderHooks.FolderColumnsHook": "folderColumns",
        "LauncherIconHooks.TitleTopMarginHook": "titleTopMargin",
        "SystemAudioAndVisualAndMoreHooks.AllRotationsHook": "allRotations",
        "SystemDisplayAndWindowHooks.NoLightUpOnChargeHook": "noLightUpOnCharge",
        "SystemUIStatusBarHooks.NoNetworkSpeedSeparatorHook": "noNetworkSpeedSeparator",
        "SystemUIStatusBarHooks.HideIconsClockHook": "hideIconsClock",
        "LauncherAnimationHooks.NoUnlockAnimationHook": "noUnlockAnimation",
    }

    tokens = []
    # Single pass: match either a direct hook call or a catalog installById call
    # in the order they appear in MainModule.
    direct_pattern = "|".join(re.escape(k) for k in direct_to_id)
    full_pattern = f"({direct_pattern})|FeatureCatalog\\.installById\\(\"([^\"]+)\",\\s*\\w+Runtime\\)"
    for m in re.finditer(full_pattern, text):
        if m.group(1):
            tokens.append(direct_to_id[m.group(1)])
        elif m.group(2) and m.group(2) in catalog_feature_ids:
            tokens.append(m.group(2))
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

    baseline_tokens = extract_catalog_tokens(baseline_text)
    current_tokens = extract_catalog_tokens(current_text)

    catalog_feature_ids = {
        "packagePermissions",
        "autoBrightnessRange",
        "muffledVibration",
        "statusBarClockTweak",
        "noMoreIcon",
        "batteryIndicator",
        "noClockHide",
        "noWidgetOnly",
        "screenDimTime",
        "firstVolumePress",
        "networkIndicatorWifi",
        "muteVisibleNotifications",
        "hideLauncherTitles",
        "fixAppInfoLaunch",
        "hideProximityWarning",
        "clearAllTasks",
        "hideDismissView",
        "hideLockScreenHint",
        "folderColumns",
        "titleTopMargin",
        "noLightUpOnCharge",
        "allRotations",
        "noNetworkSpeedSeparator",
        "hideIconsClock",
        "noUnlockAnimation",
    }

    if set(baseline_tokens) != catalog_feature_ids:
        print("Baseline MainModule does not contain the expected catalog feature call set.")
        print(f"  missing:  {sorted(catalog_feature_ids - set(baseline_tokens))}")
        print(f"  extra:    {sorted(set(baseline_tokens) - catalog_feature_ids)}")
        print(f"  tokens:   {baseline_tokens}")
        return 1

    if current_tokens != baseline_tokens:
        print("Current MainModule catalog install-by-id sequence is out of order or incomplete.")
        print(f"  baseline: {baseline_tokens}")
        print(f"  current:  {current_tokens}")
        return 1

    # Ensure no bulk FeatureCatalog.installForPackage remains and no extra
    # FeatureCatalog.installById calls were added outside the catalog set.
    extra = set(current_tokens) - catalog_feature_ids
    if extra:
        print(f"Unexpected FeatureCatalog.installById calls: {sorted(extra)}")
        return 1

    if re.search(r'FeatureCatalog\.installForPackage\(', current_text):
        print("FeatureCatalog.installForPackage bulk call still exists in MainModule.")
        return 1

    if re.search(r'FeatureCatalog\.installForSystemServer\(', current_text):
        print("FeatureCatalog.installForSystemServer bulk call still exists in MainModule.")
        return 1

    print("Catalog hook-sequence audit passed:")
    print(f"  - baseline tokens: {baseline_tokens}")
    print(f"  - current tokens:  {current_tokens}")
    print("  - order preserved, conditions moved to catalog, no bulk-install leftovers")
    return 0


if __name__ == "__main__":
    sys.exit(main())
