#!/usr/bin/env python3
"""Hardened regression tests for upstream #660 and #624 patterns.
These are static tests. They do not require a device. They fail if the A13
source relaxes the safeguards that prevent:
* #660 — `IndexOutOfBoundsException` from `ViewGroup.addView` with an un-clamped
  index, or duplicate attach of a View that already has a parent.
* #624 — clock seconds becoming static because a REQUIRED update target is
  optional or missing, or the install result is mis-reported.
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
def block_for_opener(text: str, start: int, open_ch: str = "(", close_ch: str = ")") -> str:
    """Return the balanced block starting at `start` (or first opener at/after)."""
    i = text.find(open_ch, start)
    if i == -1:
        return ""
    depth = 0
    while i < len(text):
        c = text[i]
        if c == open_ch:
            depth += 1
        elif c == close_ch:
            depth -= 1
            if depth == 0:
                return text[text.find(open_ch, start) : i + 1]
        i += 1
    return ""
def parse_spec_call(call: str) -> dict[str, str]:
    inner = call[1:-1]
    parts: list[str] = []
    depth = 0
    in_str = False
    part = ""
    for c in inner:
        if c == '"' and depth == 0:
            in_str = not in_str
            part += c
            continue
        if c == "(" and not in_str:
            depth += 1
        elif c == ")" and not in_str:
            depth -= 1
        if c == "," and depth == 0 and not in_str:
            parts.append(part.strip())
            part = ""
            continue
        part += c
    if part.strip():
        parts.append(part.strip())
    args: dict[str, str] = {}
    for p in parts:
        if "=" in p:
            k, _, v = p.partition("=")
            args[k.strip()] = v.strip()
    return args
class ClockContractTarget:
    __slots__ = ("id", "class_name", "member_name", "operation", "params", "criticality")
    def __init__(self, id_: str, class_name: str, member_name: str, operation: str, params: str, criticality: str) -> None:
        self.id = id_
        self.class_name = class_name
        self.member_name = member_name
        self.operation = operation
        self.params = params
        self.criticality = criticality
    def __repr__(self) -> str:
        return f"ClockContractTarget({self.id}, {self.criticality})"
def extract_clock_contract_targets() -> list[ClockContractTarget]:
    text = read("tv/withaibuild/customiuizer/mods/catalog/CanaryContracts.kt")
    m = re.search(r"val statusBarClockTweak: HookTargetContract by lazy", text)
    if not m:
        raise AssertionError("statusBarClockTweak contract not found")
    contract_text = block_for_opener(text, m.end(), "{", "}")
    targets: list[ClockContractTarget] = []
    for m in re.finditer(r"\bHookTargetSpec\b", contract_text):
        call = find_balanced_call(contract_text, m.start())
        if not call:
            continue
        args = parse_spec_call(call)
        criticality = (
            re.search(r"criticality\s*=\s*Criticality\.([A-Z_]+)", call).group(1)
            if re.search(r"criticality\s*=\s*Criticality\.([A-Z_]+)", call)
            else "REQUIRED"
        )
        targets.append(
            ClockContractTarget(
                id_=args.get("id", "").strip('"'),
                class_name=args.get("className", "").strip('"'),
                member_name=args.get("memberName", "").strip('"'),
                operation=args.get("operation", "").split(".")[-1].strip() if args.get("operation") else "",
                params=args.get("parameterTypes", ""),
                criticality=criticality,
            )
        )
    return targets
def add_view_calls(text: str) -> list[tuple[int, str, str | None, str | None]]:
    """Return a list of (line_no, full_line, view_expr, index_expr) for addView calls."""
    out: list[tuple[int, str, str | None, str | None]] = []
    for line_no, line in enumerate(text.splitlines(), start=1):
        m = re.search(r"\.addView\(([^)]+)\)", line)
        if not m:
            continue
        full = m.group(1)
        parts = [p.strip() for p in full.split(",")]
        view_expr = parts[0]
        index_expr = parts[1] if len(parts) > 1 else None
        out.append((line_no, line, view_expr, index_expr))
    return out
def enclosing_function(text: str, line_no: int) -> str | None:
    """Return the name of the Kotlin/Java function that contains `line_no`."""
    lines = text.splitlines()
    # Scan upward for a function header.
    for i in range(line_no - 1, -1, -1):
        m = re.match(r"\s*(?:override\s+|private\s+|internal\s+|public\s+|@JvmStatic\s+)*fun\s+(\w+).*", lines[i])
        if m:
            return m.group(1)
    return None
def function_body(text: str, func_name: str) -> str:
    """Return the full text of a function by name (naïve brace balance)."""
    m = re.search(rf"\bfun\s+{re.escape(func_name)}\b", text)
    if not m:
        return ""
    block = block_for_opener(text, m.end(), "{", "}")
    return block
class UpstreamIssueRegressionTests(unittest.TestCase):
    def test_audit_files_exist(self):
        for name in (
            "docs/audit/A13_UPSTREAM_ISSUE_REGRESSION.md",
            "docs/audit/A13_DEVICE_REGRESSION_CHECKLIST.md",
        ):
            self.assertTrue((REPO / name).is_file(), f"{name} missing")
    # --------------------------------------------------------------------- #660
    def test_battery_indicator_removes_existing_view_before_add(self):
        """`BatteryIndicatorHook` must not attach a second View if the old one is
        still present in the same parent."""
        text = read("tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt")
        body = function_body(text, "BatteryIndicatorHook")
        self.assertIn("addView(indicator", body, "expected addView(indicator, ...) in BatteryIndicatorHook")
        add_line = next(
            (ln for ln, line, _, _ in add_view_calls(body) if "indicator" in line),
            0,
        )
        self.assertGreater(add_line, 0, "addView(indicator, ...) not found")
        # The fix: existing indicator must be removed from its parent before the new one is added.
        self.assertIn(
            "removeView",
            body[: body.index("addView(indicator")],
            "BatteryIndicatorHook adds a new indicator but does not remove the old one first; risk of duplicate attach",
        )
    def test_battery_indicator_add_index_is_clamped(self):
        """`BatteryIndicatorHook` insert index must be clamped to the parent's child count."""
        text = read("tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt")
        body = function_body(text, "BatteryIndicatorHook")
        add_match = re.search(r"mStatusBarWindow\.addView\(indicator,\s*(\w+)\)", body, re.S)
        self.assertIsNotNone(add_match)
        index_name = add_match.group(1)
        assign = re.search(rf"val\s+{re.escape(index_name)}\s*=\s*(.+)", body)
        self.assertIsNotNone(assign, f"{index_name} assignment not found")
        index_def = assign.group(1)
        self.assertTrue(
            any(k in index_def for k in ("coerceIn", "coerceAtMost", "minOf", "Math.min")),
            f"BatteryIndicator add index is not clamped: {index_def}",
        )
    def test_monitor_device_info_add_index_is_clamped(self):
        """#660 crash pattern: `mGroup.addView(iconView, i)` with the raw `i` from
        `addHolder` must be clamped."""
        text = read("tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt")
        body = function_body(text, "MonitorDeviceInfoHook")
        # Find the `addHolder` `before` callback.
        add_holder_match = re.search(
            r'hookAllMethods\("com\.android\.systemui\.statusbar\.phone\.StatusBarIconController\\\$IconManager".*?,\s*"addHolder".*?\{.*?\n\s*\}\n\s*\}\)\s*\}',
            body,
            re.S,
        )
        if not add_holder_match:
            self.fail("MonitorDeviceInfoHook addHolder hook not found")
        block = add_holder_match.group(0)
        m = re.search(r"mGroup\.addView\(iconView,\s*(.+?)\)", block)
        self.assertIsNotNone(m, "mGroup.addView(iconView, i) not found")
        index_expr = m.group(1)
        self.assertTrue(
            any(k in index_expr for k in ("coerceIn", "coerceAtMost", "minOf", "Math.min")),
            f"MonitorDeviceInfo add index is not clamped: {index_expr}",
        )
    def test_monitor_device_info_left_icon_add_index_is_clamped(self):
        text = read("tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt")
        body = function_body(text, "MonitorDeviceInfoHook")
        m = re.search(r"leftIconsContainer\.addView\(iconView,\s*(\w+)\)", body)
        self.assertIsNotNone(m, "leftIconsContainer.addView(iconView, ...) not found")
        index_name = m.group(1)
        assign = re.search(rf"val\s+{re.escape(index_name)}\s*=\s*(.+)", body)
        self.assertIsNotNone(assign, f"{index_name} assignment not found")
        index_def = assign.group(1)
        self.assertTrue(
            any(k in index_def for k in ("coerceIn", "coerceAtMost", "minOf", "Math.min")),
            f"left icon add index is not clamped: {index_def}",
        )
    # --------------------------------------------------------------------- #624
    def test_clock_contract_required_targets_present_and_not_optional(self):
        targets = {t.id: t for t in extract_clock_contract_targets()}
        required_ids = [
            "MiuiStatusBarClockController.constructors",
            "MiuiStatusBarClockController.fireTimeChange",
            "MiuiClock.constructors",
            "MiuiClock.updateTime",
            "MiuiPhoneStatusBarView.onAttachedToWindow",
        ]
        for rid in required_ids:
            self.assertIn(rid, targets, f"REQUIRED target {rid} is missing from statusBarClockTweak")
            self.assertEqual(
                targets[rid].criticality,
                "REQUIRED",
                f"Target {rid} must stay REQUIRED; found {targets[rid].criticality}",
            )
        # The control-center date visibility setter must remain part of the contract.
        self.assertIn("MiuiClock.setClockVisibility", targets)
        # The update source (the actual seconds behaviour) must not be optional.
        self.assertEqual(targets["MiuiClock.updateTime"].criticality, "REQUIRED")
    def test_clock_contract_member_names_are_exact(self):
        """No REQUIRED target has been renamed to a different member to make a test pass."""
        targets = {t.id: t for t in extract_clock_contract_targets()}
        self.assertEqual(targets["MiuiStatusBarClockController.fireTimeChange"].member_name, "fireTimeChange")
        self.assertEqual(targets["MiuiClock.updateTime"].member_name, "updateTime")
    def test_clock_feature_catalog_keeps_systemui_restart_and_partial_reload(self):
        text = read("tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt")
        self.assertIn("statusBarClockTweakContract", text)
        self.assertIn("CanaryContracts.statusBarClockTweakForInstall", text)
        self.assertIn("activationRestartTarget = RestartTarget.SYSTEMUI_RESTART", text)
        self.assertIn("configReloadMode = ConfigReloadMode.PARTIAL", text)
    def test_feature_dispatcher_reports_failed_not_dispatched(self):
        text = read("tv/withaibuild/customiuizer/mods/catalog/FeatureDispatcher.kt")
        self.assertIn("InstallOutcome.FAILED", text)
        self.assertNotIn("InstallOutcome.DISPATCHED", text,
                         "FeatureDispatcher must not report legacy DISPATCHED state after P2 registry migration")
if __name__ == "__main__":
    unittest.main()
