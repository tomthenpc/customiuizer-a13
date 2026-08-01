#!/usr/bin/env python3
"""Static checks for A13-H1.2G variant execution invariants.

- AutoBrightnessRangeHook must receive the selected variant explicitly.
- It must branch on the variant and install only one target class.
- FeatureDispatcher must pass the selected variant to it.
- MethodHook callbacks must not read the variant or ROM profile.
- FeatureDispatcher must rethrow OutOfMemoryError in install boundaries.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
SRC = REPO_ROOT / "app" / "src" / "main" / "java"


def read(rel: str) -> str:
    return (SRC / rel.replace("/", "\\")).read_text(encoding="utf-8")


def fail(msg: str, errors: list[str]) -> None:
    print(f"  {msg}")
    errors.append(msg)


def check_auto_brightness(errors: list[str]) -> None:
    text = read("tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt")
    if "fun AutoBrightnessRangeHook(" not in text:
        fail("AutoBrightnessRangeHook not found", errors)
        return
    if "variant: AutoBrightnessVariant" not in text:
        fail("AutoBrightnessRangeHook does not declare variant parameter", errors)
    if "when (variant)" not in text:
        fail("AutoBrightnessRangeHook does not branch on variant", errors)
    if "AUTOMATIC_BRIGHTNESS_CONTROLLER" not in text:
        fail("ABC variant missing", errors)
    if "DISPLAY_POWER_CONTROLLER" not in text:
        fail("DPC variant missing", errors)

    # The legacy single-function body installed both clamp methods unconditionally.
    # After the split, the top-level function should only dispatch to one variant.
    func_match = re.search(
        r"fun AutoBrightnessRangeHook\(.*?(?=\n    @JvmStatic|\n    private fun|\Z)",
        text,
        re.S,
    )
    if func_match:
        top = func_match.group(0)
        if 'ModuleHelper.findAndHookMethod("com.android.server.display.AutomaticBrightnessController"' in top:
            fail("AutoBrightnessRangeHook still calls ModuleHelper for ABC at top level", errors)
        if 'ModuleHelper.findAndHookMethod("com.android.server.display.DisplayPowerController"' in top:
            fail("AutoBrightnessRangeHook still calls ModuleHelper for DPC at top level", errors)


def check_dispatcher(errors: list[str]) -> None:
    text = read("tv/withaibuild/customiuizer/mods/catalog/FeatureDispatcher.kt")
    if "installWithContractVariant(" not in text:
        fail("FeatureDispatcher.installWithContractVariant missing", errors)
    if not re.search(
        r"AutoBrightnessRangeHook\([^)]*runtime\.lpparam[^)]*variant[^)]*\)",
        text,
        re.S,
    ):
        fail("AutoBrightnessRangeHook is not called with a variant argument", errors)
    if "catch (oom: OutOfMemoryError)" not in text:
        fail("FeatureDispatcher does not catch and rethrow OutOfMemoryError", errors)
    if text.count("catch (oom: OutOfMemoryError)") < 2:
        fail("FeatureDispatcher should rethrow OOM in at least two boundaries", errors)


def check_callback_independence(errors: list[str]) -> None:
    text = read("tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt")
    for match in re.finditer(
        r"override fun (?:before|after)\(param: (?:Before|After)HookCallback\) \{(.*?)\n\s*\}",
        text,
        re.S,
    ):
        body = match.group(1)
        if "AutoBrightnessVariant" in body:
            fail("MethodHook callback reads AutoBrightnessVariant", errors)
        if "selectedVariant" in body:
            fail("MethodHook callback reads selectedVariant", errors)
        if "RomProfile" in body:
            fail("MethodHook callback reads RomProfile", errors)


def check_hook_installer(errors: list[str]) -> None:
    text = read("tv/withaibuild/customiuizer/mods/utils/HookInstaller.kt")
    if "compatibilityResult.selectedVariant" not in text:
        fail("HookInstaller does not read selectedVariant from compatibility result", errors)
    if "Contract ${contract.featureId} requires selected variant" not in text:
        fail("HookInstaller does not enforce selected variant for multi-variant contracts", errors)


def check_diagnostic_recorder(errors: list[str]) -> None:
    text = read("tv/withaibuild/customiuizer/mods/diagnostics/DiagnosticRecorder.kt")
    if text.count("catch (oom: OutOfMemoryError)") < 2:
        fail("DiagnosticRecorder fallback logger does not protect OOM boundaries", errors)


def main() -> int:
    errors: list[str] = []
    check_auto_brightness(errors)
    check_dispatcher(errors)
    check_callback_independence(errors)
    check_hook_installer(errors)
    check_diagnostic_recorder(errors)
    if errors:
        print("check-compat-contracts FAILED")
        for e in errors:
            print(f"  - {e}")
        return 1
    print("check-compat-contracts OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
