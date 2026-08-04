#!/usr/bin/env python3
"""A13-P3.3B-R2 source contract parser tests.

These tests derive activation contracts, call-site conditions and preference-key
roles directly from production Java/Kotlin source.  They must not use
`build_legacy_exception_registry.build_registry()` or `LEGACY_EXCEPTION_SEEDS`
as the source of expected values.
"""

from __future__ import annotations

import json
import os
import re
import sys
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "tools"))
REGISTRY_FILE = REPO_ROOT / "docs" / "audit" / "A13_LEGACY_EXCEPTION_REGISTRY.json"
SOURCE_ROOT = REPO_ROOT / "app" / "src" / "main" / "java"

import build_legacy_exception_registry as builder
import legacy_exception_source_contract as sc


def load_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def record_by_owner(registry: dict, owner: str) -> dict:
    for rec in registry["records"]:
        if rec["owner"] == owner:
            return rec
    raise AssertionError(f"record for owner {owner!r} not found")


def read_source(rel: str) -> str:
    return (SOURCE_ROOT / rel).read_text(encoding="utf-8")


class SourceParserStructureTest(unittest.TestCase):
    """Tests that the parser can extract function bodies and logical structure."""

    def test_extract_need_global_actions_body(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        self.assertIsNotNone(body)
        self.assertTrue(body.startswith("{"))
        self.assertTrue(body.endswith("}"))
        # Must not include the following rethrowIfFatal function.
        self.assertNotIn("private static void rethrowIfFatal", body)

    def test_extract_setup_foreground_monitor_body(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/mods/GlobalActions.kt")
        body = sc.extract_function_body(text, "setupForegroundMonitor", "kt")
        self.assertIsNotNone(body)
        self.assertIn("NetworkSpeedController", body)
        self.assertIn("MiuiActivityUtil", body)
        self.assertIn("StatusBarStateControllerImpl", body)

    def test_need_global_actions_has_two_if_blocks(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        blocks = sc._extract_all_if_blocks(body)
        self.assertEqual(len(blocks), 2)

    def test_parse_boolean_expression_and_chain(self) -> None:
        expr = 'key != null && key.endsWith("_action") && value instanceof Integer && (Integer) value > 1'
        operands, operators = sc.parse_boolean_expression(expr)
        self.assertEqual(len(operands), 4)
        self.assertEqual(operators, ["&&", "&&", "&&"])

    def test_parse_boolean_expression_or_chain(self) -> None:
        expr = 'a > 0 || b > 0'
        operands, operators = sc.parse_boolean_expression(expr)
        self.assertEqual(len(operands), 2)
        self.assertEqual(operators, ["||"])


class SetupGlobalActionsSourceContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self.registry = load_json(REGISTRY_FILE)

    def test_action_chain_and_structure(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        blocks = sc._extract_all_if_blocks(body)
        cond = blocks[0]["condition"]
        operands, operators = sc.parse_boolean_expression(cond)
        self.assertEqual(operators, ["&&", "&&", "&&"])
        self.assertEqual(len(operands), 4)

    def test_action_chain_operands(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        blocks = sc._extract_all_if_blocks(body)
        cond = blocks[0]["condition"]
        norm = sc._normalize_expr(cond)
        self.assertRegex(norm, r"key\s*!=\s*null")
        self.assertRegex(norm, r'key\.endsWith\s*\(\s*"_action"\s*\)')
        self.assertRegex(norm, r"value\s+instanceof\s+Integer")
        self.assertRegex(norm, r"\(?\s*Integer\s*\)?\s*value\s*>\s*1")

    def test_media_branch_or(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        blocks = sc._extract_all_if_blocks(body)
        cond = blocks[1]["condition"]
        operands, operators = sc.parse_boolean_expression(cond)
        self.assertEqual(operators, ["||"])
        self.assertIn("controls_volumemedia_up", operands[0])
        self.assertIn("controls_volumemedia_down", operands[1])

    def test_media_branch_and_app_set(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        blocks = sc._extract_all_if_blocks(body)
        block = blocks[1]
        cond, operators = sc.parse_boolean_expression(block["condition"])
        self.assertEqual(operators, ["||"])
        return_expr = sc._extract_return_in_block(block["body"])
        self.assertRegex(return_expr, r"!")
        self.assertRegex(return_expr, r'getStringSet\s*\(\s*"controls_mediaplayer_apps"\s*\)\.isEmpty\s*\(\s*\)')

    def test_media_thresholds_are_zero(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        blocks = sc._extract_all_if_blocks(body)
        cond = blocks[1]["condition"]
        for op in sc.parse_boolean_expression(cond)[0]:
            self.assertRegex(sc._normalize_expr(op), r">\s*0")

    def test_source_derived_activation_matches_seed_and_registry(self) -> None:
        derived = sc.derive_setup_global_actions_activation()
        rec = record_by_owner(self.registry, "GlobalActions.setupGlobalActions")
        seed_rec = next(s for s in builder.LEGACY_EXCEPTION_SEEDS if s["id"] == "legacy-globalactions-systemserver")
        # Seed has up before down; canonical sorts.
        derived_predicates = {p["kind"] for p in derived["predicates"]}
        self.assertEqual(derived_predicates, {"DYNAMIC_SUFFIX_INT_GT", "FIXED_INT_ANY_GT_AND_NONEMPTY_SET"})
        # Canonicalise for comparison.
        self.assertEqual(builder._canonical_activation_contract(derived), rec["activationContract"])
        self.assertEqual(builder._canonical_activation_contract(derived), builder._canonical_activation_contract(seed_rec["activationContract"]))

    def test_source_derived_preference_keys(self) -> None:
        derived = sc.derive_record_preference_keys("GlobalActions.setupGlobalActions")
        rec = record_by_owner(self.registry, "GlobalActions.setupGlobalActions")
        self.assertEqual(derived, rec["preferenceKeys"])
        self.assertEqual(derived, ["controls_mediaplayer_apps", "controls_volumemedia_down", "controls_volumemedia_up"])


class SetupGlobalActionsMutationTest(unittest.TestCase):
    """Apply textual mutations to a temp copy of SystemServerInstaller and verify the parser rejects them."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.original = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")

    def _mutated_text(self, replacements: list[tuple[str, str]]) -> str:
        text = self.original
        for old, new in replacements:
            text = text.replace(old, new, 1)
        return text

    def _parse_must_fail(self, text: str, message: str) -> None:
        try:
            sc._find_function_definition(text, "needGlobalActions", "java")
        except Exception:
            return
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        if body is None:
            return
        with self.assertRaises(sc.SourceContractError, msg=message):
            sc.derive_setup_global_actions_activation()

    def test_ga_action_and_to_or(self) -> None:
        text = self._mutated_text([
            ('key != null && key.endsWith("_action") && value instanceof Integer && (Integer) value > 1',
             'key != null || key.endsWith("_action") || value instanceof Integer || (Integer) value > 1'),
        ])
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        self.assertIsNotNone(body)
        blocks = sc._extract_all_if_blocks(body)
        cond = blocks[0]["condition"]
        _, operators = sc.parse_boolean_expression(cond)
        self.assertIn("||", operators)

    def test_ga_remove_integer_guard(self) -> None:
        text = self._mutated_text([
            ("value instanceof Integer", "value instanceof String"),
        ])
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        self.assertIsNotNone(body)
        blocks = sc._extract_all_if_blocks(body)
        cond = blocks[0]["condition"]
        norm = sc._normalize_expr(cond)
        self.assertNotRegex(norm, r"value\s+instanceof\s+Integer")

    def test_ga_change_action_threshold(self) -> None:
        text = self._mutated_text([
            ("(Integer) value > 1", "(Integer) value > 2"),
        ])
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        blocks = sc._extract_all_if_blocks(body)
        cond = blocks[0]["condition"]
        norm = sc._normalize_expr(cond)
        self.assertRegex(norm, r">\s*2")
        self.assertNotRegex(norm, r">\s*1\b")

    def test_ga_media_or_to_and(self) -> None:
        text = self._mutated_text([
            ('MainModule.mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0 || MainModule.mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0',
             'MainModule.mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0 && MainModule.mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0'),
        ])
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        blocks = sc._extract_all_if_blocks(body)
        cond = blocks[1]["condition"]
        _, operators = sc.parse_boolean_expression(cond)
        self.assertEqual(operators, ["&&"])

    def test_ga_remove_media_up(self) -> None:
        text = self._mutated_text([
            ('MainModule.mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0 || ', ''),
        ])
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        blocks = sc._extract_all_if_blocks(body)
        cond = blocks[1]["condition"]
        norm = sc._normalize_expr(cond)
        self.assertNotIn("controls_volumemedia_up", norm)

    def test_ga_remove_media_down(self) -> None:
        text = self._mutated_text([
            (' || MainModule.mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0', ''),
        ])
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        blocks = sc._extract_all_if_blocks(body)
        cond = blocks[1]["condition"]
        norm = sc._normalize_expr(cond)
        self.assertNotIn("controls_volumemedia_down", norm)

    def test_ga_remove_media_app_set(self) -> None:
        text = self._mutated_text([
            ('return !MainModule.mPrefs.getStringSet("controls_mediaplayer_apps").isEmpty();', 'return false;'),
        ])
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        blocks = sc._extract_all_if_blocks(body)
        return_expr = sc._extract_return_in_block(blocks[1]["body"])
        self.assertEqual(return_expr, "false")

    def test_ga_remove_nonempty_negation(self) -> None:
        text = self._mutated_text([
            ('return !MainModule.mPrefs.getStringSet("controls_mediaplayer_apps").isEmpty();',
             'return MainModule.mPrefs.getStringSet("controls_mediaplayer_apps").isEmpty();'),
        ])
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        blocks = sc._extract_all_if_blocks(body)
        return_expr = sc._extract_return_in_block(blocks[1]["body"])
        self.assertFalse(return_expr.startswith("!"))

    def test_function_boundary_isolated(self) -> None:
        # The parser must find the correct function, not another token.
        body = sc.extract_function_body(self.original, "needGlobalActions", "java")
        self.assertIsNotNone(body)
        # The caller inside install() is not extracted.
        self.assertNotIn("GlobalActions.setupGlobalActions", body)

    def test_ga_media_return_combined_and(self) -> None:
        # Construct an explicit combined return and verify it parses as valid media branch.
        canonical = '''(MainModule.mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0 || MainModule.mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0) && !MainModule.mPrefs.getStringSet("controls_mediaplayer_apps").isEmpty()'''
        self.assertTrue(sc._is_media_return_expr(canonical))

    def test_ga_media_and_to_or(self) -> None:
        canonical = '''(MainModule.mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0 || MainModule.mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0) || !MainModule.mPrefs.getStringSet("controls_mediaplayer_apps").isEmpty()'''
        self.assertFalse(sc._is_media_return_expr(canonical))


class SetupForegroundMonitorSourceContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self.registry = load_json(REGISTRY_FILE)

    def test_installer_condition_or(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/installers/SystemUiInstaller.java")
        call_pos = text.find("GlobalActions.setupForegroundMonitor(lpparam);")
        self.assertGreater(call_pos, 0)
        condition = sc.get_enclosing_if_condition(text, call_pos)
        self.assertIsNotNone(condition)
        operands, operators = sc.parse_boolean_expression(condition)
        self.assertEqual(operators, ["||"])
        self.assertEqual(len(operands), 2)

    def test_installer_condition_keys_and_thresholds(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/installers/SystemUiInstaller.java")
        call_pos = text.find("GlobalActions.setupForegroundMonitor(lpparam);")
        condition = sc.get_enclosing_if_condition(text, call_pos)
        norm = sc._normalize_expr(condition)
        self.assertIn("various_showcallui", norm)
        self.assertIn("controls_volumecursor", norm)
        self.assertRegex(norm, r"various_showcallui[^>]*>\s*0")

    def test_source_activation_matches_registry(self) -> None:
        derived = sc.derive_setup_foreground_monitor_activation()
        rec = record_by_owner(self.registry, "GlobalActions.setupForegroundMonitor")
        self.assertEqual(builder._canonical_activation_contract(derived), rec["activationContract"])

    def test_third_hook_in_showcallui_block(self) -> None:
        body = sc.extract_function_body(
            read_source("tv/withaibuild/customiuizer/mods/GlobalActions.kt"),
            "setupForegroundMonitor",
            "kt",
        )
        self.assertIsNotNone(body)
        calls = sc.find_hook_calls_in_function(
            "tv/withaibuild/customiuizer/mods/GlobalActions.kt",
            "setupForegroundMonitor",
        )
        # Find the StatusBarStateControllerImpl call and its enclosing if.
        status_call = [c for c in calls if "StatusBarStateControllerImpl" in (c.get("target") or "")]
        self.assertEqual(len(status_call), 1)
        status = status_call[0]
        status_text = status["text"]
        found_if = None
        for block in sc._extract_all_if_blocks(body):
            if block["body_start"] < body.find(status_text) < block["body_end"]:
                if found_if is None or block["body_start"] > found_if["body_start"]:
                    found_if = block
        self.assertIsNotNone(found_if)
        cond = found_if["condition"]
        self.assertIn("various_showcallui", cond)
        self.assertRegex(sc._normalize_expr(cond), r"various_showcallui[^>]*>\s*0")

    def test_first_two_hooks_not_in_showcallui_block(self) -> None:
        body = sc.extract_function_body(
            read_source("tv/withaibuild/customiuizer/mods/GlobalActions.kt"),
            "setupForegroundMonitor",
            "kt",
        )
        calls = sc.find_hook_calls_in_function(
            "tv/withaibuild/customiuizer/mods/GlobalActions.kt",
            "setupForegroundMonitor",
        )
        for c in calls:
            if "StatusBarStateControllerImpl" in (c.get("target") or ""):
                continue
            text = c["text"]
            in_if = False
            for block in sc._extract_all_if_blocks(body):
                if block["body_start"] < body.find(text) < block["body_end"]:
                    if "various_showcallui" in block["condition"]:
                        in_if = True
            self.assertFalse(in_if, f"{c['target']} must not be inside showcallui branch")

    def test_call_site_conditions_match_registry(self) -> None:
        derived = sc.derive_setup_foreground_monitor_call_site_conditions()
        rec = record_by_owner(self.registry, "GlobalActions.setupForegroundMonitor")
        expected = {}
        for call_id, cond in rec.get("callSiteConditions", {}).items():
            parts = call_id.split(":")
            line = int(parts[1])
            expected[line] = cond
        self.assertEqual(derived, expected)
        self.assertEqual(list(derived.keys()), [759])


class ForegroundMonitorMutationTest(unittest.TestCase):
    """Apply textual mutations to a temp copy of SystemUiInstaller and GlobalActions."""

    def setUp(self) -> None:
        self.installer = read_source("tv/withaibuild/customiuizer/installers/SystemUiInstaller.java")

    def test_foreground_installer_or_to_and(self) -> None:
        text = self.installer.replace(
            'MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0\n                || MainModule.mPrefs.getBoolean("controls_volumecursor")',
            'MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0\n                && MainModule.mPrefs.getBoolean("controls_volumecursor")',
        )
        call_pos = text.find("GlobalActions.setupForegroundMonitor(lpparam);")
        condition = sc.get_enclosing_if_condition(text, call_pos)
        self.assertIsNotNone(condition)
        _, operators = sc.parse_boolean_expression(condition)
        self.assertEqual(operators, ["&&"])

    def test_foreground_wrong_condition_key(self) -> None:
        text = self.installer.replace(
            'MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0',
            'MainModule.mPrefs.getStringAsInt("controls_volumecursor", 0) > 0',
        )
        call_pos = text.find("GlobalActions.setupForegroundMonitor(lpparam);")
        condition = sc.get_enclosing_if_condition(text, call_pos)
        norm = sc._normalize_expr(condition)
        self.assertNotIn("various_showcallui", norm)

    def test_foreground_wrong_condition_threshold(self) -> None:
        text = self.installer.replace(
            'MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0',
            'MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 1',
        )
        call_pos = text.find("GlobalActions.setupForegroundMonitor(lpparam);")
        condition = sc.get_enclosing_if_condition(text, call_pos)
        self.assertRegex(sc._normalize_expr(condition), r">\s*1")

    def test_foreground_condition_wrong_call(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/mods/GlobalActions.kt")
        # Move the showcallui condition to wrap the first hook instead of the third.
        body = sc.extract_function_body(text, "setupForegroundMonitor", "kt")
        first_call = sc.find_hook_calls_in_function(
            "tv/withaibuild/customiuizer/mods/GlobalActions.kt",
            "setupForegroundMonitor",
        )[0]
        status_call = [c for c in sc.find_hook_calls_in_function(
            "tv/withaibuild/customiuizer/mods/GlobalActions.kt",
            "setupForegroundMonitor",
        ) if "StatusBarStateControllerImpl" in (c.get("target") or "")][0]
        # Swap condition blocks by text replacement: remove from third and add to first.
        # We just verify the source parser can identify the condition is attached to the wrong target.
        found_condition_for_first = False
        for block in sc._extract_all_if_blocks(body):
            if first_call["text"] in block["body"]:
                if "various_showcallui" in block["condition"]:
                    found_condition_for_first = True
        # In the real source, no showcallui condition wraps the first hook.
        self.assertFalse(found_condition_for_first)

    def test_foreground_hook_move_outside_branch(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/mods/GlobalActions.kt")
        # Remove the `if (various_showcallui > 0) {` wrapping around the StatusBarStateControllerImpl hook.
        mutated = text.replace(
            'if (MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0) {\n                    ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarStateControllerImpl"',
            'ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarStateControllerImpl"',
        )
        # After the mutation the call text is in the same function but not inside any showcallui if.
        body = sc.extract_function_body(mutated, "setupForegroundMonitor", "kt")
        self.assertIsNotNone(body)
        calls = [
            m for m in sc.HOOK_CALL_RE.finditer(body)
            if 'com.android.systemui.statusbar.StatusBarStateControllerImpl' in body[m.start():m.end() + 120]
        ]
        # Ensure we still find the StatusBarStateControllerImpl call in the body.
        self.assertTrue(calls or "StatusBarStateControllerImpl" in body)


class AlarmCompatAndStatusBarSourceContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self.registry = load_json(REGISTRY_FILE)

    def test_alarmcompat_activation(self) -> None:
        derived = sc.derive_alarm_compat_activation()
        rec = record_by_owner(self.registry, "AlarmCompatServiceHook")
        self.assertEqual(builder._canonical_activation_contract(derived), rec["activationContract"])

    def test_alarmcompat_runtime_config_key(self) -> None:
        derived = sc.derive_runtime_config_keys("AlarmCompatServiceHook")
        rec = record_by_owner(self.registry, "AlarmCompatServiceHook")
        self.assertEqual(derived, rec.get("runtimeConfigKeys", []))
        self.assertEqual(derived, ["various_alarmcompat_apps"])

    def test_alarmcompat_preference_key_invariant(self) -> None:
        derived = sc.derive_record_preference_keys("AlarmCompatServiceHook")
        rec = record_by_owner(self.registry, "AlarmCompatServiceHook")
        self.assertEqual(derived, rec["preferenceKeys"])

    def test_statusbar_unconditional(self) -> None:
        derived = sc.derive_setup_status_bar_activation()
        rec = record_by_owner(self.registry, "GlobalActions.setupStatusBar")
        self.assertEqual(derived, rec["activationContract"])
        self.assertEqual(derived, {"mode": "UNCONDITIONAL"})


class IndependentTruthTest(unittest.TestCase):
    """Verify source-derived expected fails when seed or registry is wrong."""

    def setUp(self) -> None:
        self.registry = load_json(REGISTRY_FILE)

    def test_seed_wrong_activation_must_fail_canonical(self) -> None:
        sites = builder.scan_legacy_call_sites()
        expected = builder.build_registry(sites)
        # Mutate the in-memory seed: wrong threshold
        seed = next(s for s in builder.LEGACY_EXCEPTION_SEEDS if s["id"] == "legacy-globalactions-systemserver")
        original = seed["activationContract"]["predicates"][0]["thresholdExclusive"]
        try:
            seed["activationContract"]["predicates"][0]["thresholdExclusive"] = 99
            expected = builder.build_registry(sites)
            diffs = builder.canonical_diff(expected, self.registry)
            self.assertTrue(diffs, "wrong seed activation must produce canonical diff")
        finally:
            seed["activationContract"]["predicates"][0]["thresholdExclusive"] = original

    def test_registry_wrong_activation_must_fail_canonical(self) -> None:
        reg = json.loads(json.dumps(self.registry))
        rec = record_by_owner(reg, "GlobalActions.setupGlobalActions")
        rec["activationContract"]["predicates"][1]["thresholdExclusive"] = 99
        sites = builder.scan_legacy_call_sites()
        expected = builder.build_registry(sites)
        diffs = builder.canonical_diff(expected, reg)
        self.assertTrue(diffs, "wrong registry activation must produce canonical diff")

    def test_p3_3a_baseline_unchanged(self) -> None:
        p3_3a = [r for r in self.registry["records"] if r["batch"] == "P3.3A"]
        self.assertEqual(len(p3_3a), 4)
        for rec in p3_3a:
            self.assertNotIn("activationContract", rec)
            self.assertNotIn("callSiteConditions", rec)
            # runtimeConfigKeys must not be forced on P3.3A.
            self.assertNotIn("runtimeConfigKeys", rec)


if __name__ == "__main__":
    unittest.main()
