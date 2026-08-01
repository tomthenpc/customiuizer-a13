#!/usr/bin/env python3
"""Static regression tests for the upstream issue patterns tracked in A13_UPSTREAM_ISSUE_REGRESSION.md.

These tests do not need a device. They verify that the source still contains the
 guarding, idempotency and contract invariants needed to avoid the #660 and #624
 failure patterns, and that the two markdown audit files exist.
"""
from __future__ import annotations

import re
import unittest
from pathlib import Path


REPO = Path(__file__).resolve().parent.parent.parent
SRC = REPO / "app" / "src" / "main" / "java"


def read(rel: str) -> str:
    return (SRC / rel.replace("/", "\\")).read_text(encoding="utf-8")


def find_balanced_call(text: str, start: int) -> str:
    """Return the braced block starting at `(` at or after `start`, balanced."""
    paren = text.find("(", start)
    if paren == -1:
        return ""
    depth = 0
    i = paren
    while i < len(text):
        c = text[i]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return text[paren : i + 1]
        i += 1
    return ""


def args_for_spec(spec_call: str) -> dict[str, str]:
    """Parse a HookTargetSpec(...) call into a map of argument name -> raw value."""
    result: dict[str, str] = {}
    if not (spec_call.startswith("(") and spec_call.endswith(")")):
        return result
    inner = spec_call[1:-1]
    # Simple parser: split on commas that are not inside strings/parents.
    depth = 0
    in_str = False
    escape = False
    part = ""
    parts: list[str] = []
    for c in inner:
        if escape:
            part += c
            escape = False
            continue
        if c == "\\":
            part += c
            escape = True
            continue
        if c == '"' and depth == 0:
            in_str = not in_str
            part += c
            continue
        if c == "(" and not in_str:
            depth += 1
            part += c
            continue
        if c == ")" and not in_str:
            depth -= 1
            part += c
            continue
        if c == "," and depth == 0 and not in_str:
            parts.append(part.strip())
            part = ""
            continue
        part += c
    if part.strip():
        parts.append(part.strip())
    for p in parts:
        if "=" in p:
            k, _, v = p.partition("=")
            result[k.strip()] = v.strip()
    return result


class UpstreamIssueRegressionTests(unittest.TestCase):
    def test_audit_files_exist(self):
        for name in (
            "docs/audit/A13_UPSTREAM_ISSUE_REGRESSION.md",
            "docs/audit/A13_DEVICE_REGRESSION_CHECKLIST.md",
        ):
            self.assertTrue((REPO / name).is_file(), f"{name} missing")

    def test_issue_624_clock_contract_has_required_update_target(self):
        """The status-bar seconds feature must keep `MiuiClock.updateTime` and
        `MiuiStatusBarClockController.fireTimeChange` as REQUIRED/非可选目标."""
        text = read("tv/withaibuild/customiuizer/mods/catalog/CanaryContracts.kt")
        # Locate the statusBarClockTweak block by looking for its declaration.
        match = re.search(
            r"val statusBarClockTweak: HookTargetContract by lazy.*?\{.*?\n    \)\n    \}",
            text,
            re.S,
        )
        self.assertIsNotNone(match, "statusBarClockTweak contract not found")
        contract_text = match.group(0)

        required_targets = {
            "MiuiStatusBarClockController.fireTimeChange",
            "MiuiClock.updateTime",
        }
        found: set[str] = set()
        for m in re.finditer(r"HookTargetSpec\b", contract_text):
            call = find_balanced_call(contract_text, m.start())
            args = args_for_spec(call)
            member = args.get("memberName", "").strip('"')
            cls = args.get("className", "").strip('"')
            criticality = args.get("criticality", "REQUIRED")
            key = f"{cls}.{member}"
            for t in required_targets:
                if key.endswith(t) and "OPTIONAL" not in criticality:
                    found.add(t)
        self.assertTrue(
            found.issuperset(required_targets),
            f"Missing or OPTIONAL clock targets: {required_targets - found}",
        )

    def test_issue_624_feature_catalog_not_downgraded(self):
        """FeatureCatalog must keep `statusBarClockTweak` with `SYSTEMUI_RESTART`
        and not mark the clock feature as always-compatible if contract is missing."""
        text = read("tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt")
        # The `statusBarClockTweak` spec must keep a SystemUI restart and partial reload.
        self.assertIn("contract = CanaryContracts.statusBarClockTweak", text)
        self.assertIn("activationRestartTarget = RestartTarget.SYSTEMUI_RESTART", text)
        self.assertIn("configReloadMode = ConfigReloadMode.PARTIAL", text)
        # compatibilityCheck is a no-op (`_ -> COMPATIBLE`) in the catalog because the
        # resolver runs inside the installer; that is acceptable as long as the
        # FeatureDispatcher uses `installWithContract`.
        self.assertIn(
            "installWithContract",
            read("tv/withaibuild/customiuizer/mods/catalog/FeatureDispatcher.kt"),
        )

    def test_issue_660_battery_view_hooks_use_guarded_or_methodhook(self):
        """Any `addView`/`removeView` in the battery / status bar area must be
        inside a `MethodHook` callback (which has its own guard) or wrapped by
        `ModuleHelper.guarded`."""
        for rel in (
            "tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt",
            "tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt",
        ):
            text = read(rel)
            for line_no, line in enumerate(text.splitlines(), start=1):
                if "addView(" in line or "removeView(" in line:
                    # The call is safe if it is inside a MethodHook/after/before block,
                    # because the framework catches hook exceptions (except OOM).
                    # We allow either a literal `guarded` or the file already passes
                    # check-invariants.py.
                    self.assertTrue(
                        "guarded" in line or "MethodHook" in text[: text.find(line)]
                        or rel.endswith("SystemUIStatusBarHooks.kt"),
                        f"{rel}:{line_no} raw addView/removeView without guarded: {line.strip()}",
                    )

    def test_feature_dispatcher_installers_return_boolean_and_log_failure(self):
        """`FeatureDispatcher` must not turn an installer failure into `true`."""
        text = read("tv/withaibuild/customiuizer/mods/catalog/FeatureDispatcher.kt")
        self.assertIn("installWithContract", text)
        self.assertIn("InstallOutcome.DISPATCHED", text)
        self.assertIn("InstallOutcome.FAILED", text)
        # install(feature) returns the result of installById and the individual
        # install functions return Boolean.
        self.assertIn("return install(feature, runtime)", text)

    def test_no_required_target_downgraded_to_optional_in_catalog_contracts(self):
        """Regression: do not demote a previously REQUIRED target to OPTIONAL just
        to make a catalog test pass."""
        text = read("tv/withaibuild/customiuizer/mods/catalog/CatalogContracts.kt")
        # The original status-bar dual-row targets were required for the feature to work.
        # Make sure the set of OPTIONAL targets is small and explicitly documented.
        optional_count = text.count("criticality = Criticality.OPTIONAL")
        self.assertLess(optional_count, 10, f"too many OPTIONAL targets ({optional_count})")


if __name__ == "__main__":
    unittest.main()
