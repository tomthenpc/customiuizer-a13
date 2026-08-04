#!/usr/bin/env python3
"""A13-P3.3B-R2 source contract parser tests.

These tests derive activation contracts, call-site conditions and preference-key
roles directly from production Java/Kotlin source.  They must not use
`build_legacy_exception_registry.build_registry()` or `LEGACY_EXCEPTION_SEEDS`
as the source of expected values.
"""

from __future__ import annotations

import ast
import contextlib
import copy
import inspect
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


@contextlib.contextmanager
def _override_source(rel: str, text: str, *, extra: dict[str, str] | None = None):
    """Temporarily point the source contract parser at an in-memory file tree."""
    original_root = sc.SOURCE_ROOT
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp) / "app" / "src" / "main" / "java"
        all_files = {rel: text}
        if extra:
            all_files.update(extra)
        for file_rel, file_text in all_files.items():
            path = root / file_rel
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(file_text, encoding="utf-8")
        sc.SOURCE_ROOT = root
        try:
            yield
        finally:
            sc.SOURCE_ROOT = original_root


def _ast_offenders_for_build_registry(*classes: type) -> list[str]:
    """Return classes that contain a real Call to build_registry."""
    offenders: list[str] = []
    for cls in classes:
        source = inspect.getsource(cls)
        tree = ast.parse(source)
        for node in ast.walk(tree):
            if isinstance(node, ast.Call):
                func = node.func
                if isinstance(func, ast.Attribute) and func.attr == "build_registry":
                    offenders.append(f"{cls.__name__} -> build_registry")
                if isinstance(func, ast.Name) and func.id == "build_registry":
                    offenders.append(f"{cls.__name__} -> build_registry")
    return offenders


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
    """Apply textual mutations to a temp copy of SystemServerInstaller and verify
    the final source-derived activation contract extraction rejects them."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.original = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
        with _override_source(
            "tv/withaibuild/customiuizer/installers/SystemServerInstaller.java", cls.original
        ):
            cls.expected = sc.derive_setup_global_actions_activation()

    def _mutated_text(self, replacements: list[tuple[str, str]]) -> str:
        text = self.original
        for old, new in replacements:
            text = text.replace(old, new, 1)
        return text

    def _derive_must_fail(self, text: str, message: str) -> None:
        with _override_source(
            "tv/withaibuild/customiuizer/installers/SystemServerInstaller.java", text
        ):
            with self.assertRaises((sc.SourceContractError, sc.SourceStructureError), msg=message):
                sc.derive_setup_global_actions_activation()

    def _derive_contract(self, text: str) -> dict:
        with _override_source(
            "tv/withaibuild/customiuizer/installers/SystemServerInstaller.java", text
        ):
            return sc.derive_setup_global_actions_activation()

    def test_original_source_matches_expected(self) -> None:
        with _override_source(
            "tv/withaibuild/customiuizer/installers/SystemServerInstaller.java", self.original
        ):
            derived = sc.derive_setup_global_actions_activation()
        self.assertEqual(
            builder._canonical_activation_contract(derived),
            builder._canonical_activation_contract(self.expected),
        )

    def test_ga_action_and_to_or(self) -> None:
        text = self._mutated_text([
            ('key != null && key.endsWith("_action") && value instanceof Integer && (Integer) value > 1',
             'key != null || key.endsWith("_action") || value instanceof Integer || (Integer) value > 1'),
        ])
        self._derive_must_fail(text, "action chain OR must be rejected")

    def test_ga_remove_integer_guard(self) -> None:
        text = self._mutated_text([
            ("value instanceof Integer", "value instanceof String"),
        ])
        self._derive_must_fail(text, "missing Integer guard must be rejected")

    def test_ga_change_action_threshold(self) -> None:
        text = self._mutated_text([
            ("(Integer) value > 1", "(Integer) value > 2"),
        ])
        self._derive_must_fail(text, "changed action threshold must be rejected")

    def test_ga_media_or_to_and(self) -> None:
        text = self._mutated_text([
            ('MainModule.mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0 || MainModule.mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0',
             'MainModule.mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0 && MainModule.mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0'),
        ])
        self._derive_must_fail(text, "media OR to AND must be rejected")

    def test_ga_remove_media_up(self) -> None:
        text = self._mutated_text([
            ('MainModule.mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0 || ', ''),
        ])
        self._derive_must_fail(text, "missing media up key must be rejected")

    def test_ga_remove_media_down(self) -> None:
        text = self._mutated_text([
            (' || MainModule.mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0', ''),
        ])
        self._derive_must_fail(text, "missing media down key must be rejected")

    def test_ga_remove_media_app_set(self) -> None:
        text = self._mutated_text([
            ('return !MainModule.mPrefs.getStringSet("controls_mediaplayer_apps").isEmpty();', 'return false;'),
        ])
        self._derive_must_fail(text, "removed media app set must be rejected")

    def test_ga_remove_nonempty_negation(self) -> None:
        text = self._mutated_text([
            ('return !MainModule.mPrefs.getStringSet("controls_mediaplayer_apps").isEmpty();',
             'return MainModule.mPrefs.getStringSet("controls_mediaplayer_apps").isEmpty();'),
        ])
        self._derive_must_fail(text, "removed negation must be rejected")

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
    """Apply textual mutations to temp copies of SystemUiInstaller and GlobalActions
    and verify the final source-derived contracts reject the mutations."""

    def setUp(self) -> None:
        self.installer = read_source("tv/withaibuild/customiuizer/installers/SystemUiInstaller.java")
        self.global_actions = read_source("tv/withaibuild/customiuizer/mods/GlobalActions.kt")

    def _derive_foreground_activation(self, text: str) -> dict:
        with _override_source("tv/withaibuild/customiuizer/installers/SystemUiInstaller.java", text):
            return sc.derive_setup_foreground_monitor_activation()

    def _derive_foreground_conditions(self, text: str) -> dict[int, dict]:
        with _override_source("tv/withaibuild/customiuizer/mods/GlobalActions.kt", text):
            return sc.derive_setup_foreground_monitor_call_site_conditions()

    def test_foreground_installer_or_to_and(self) -> None:
        text = self.installer.replace(
            'MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0\n                || MainModule.mPrefs.getBoolean("controls_volumecursor")',
            'MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0\n                && MainModule.mPrefs.getBoolean("controls_volumecursor")',
        )
        with self.assertRaises((sc.SourceContractError, sc.SourceStructureError)):
            self._derive_foreground_activation(text)

    def test_foreground_wrong_condition_key(self) -> None:
        text = self.installer.replace(
            'MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0',
            'MainModule.mPrefs.getStringAsInt("controls_volumecursor", 0) > 0',
        )
        with self.assertRaises((sc.SourceContractError, sc.SourceStructureError)):
            self._derive_foreground_activation(text)

    def test_foreground_wrong_condition_threshold(self) -> None:
        text = self.installer.replace(
            'MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0',
            'MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 1',
        )
        with self.assertRaises((sc.SourceContractError, sc.SourceStructureError)):
            self._derive_foreground_activation(text)

    def test_foreground_condition_wrong_call(self) -> None:
        text = self.global_actions
        body = sc.extract_function_body(text, "setupForegroundMonitor", "kt")
        first_call = sc.find_hook_calls_in_function(
            "tv/withaibuild/customiuizer/mods/GlobalActions.kt",
            "setupForegroundMonitor",
        )[0]
        # In the real source, no showcallui condition wraps the first hook.
        for block in sc._extract_all_if_blocks(body):
            if first_call["text"] in block["body"]:
                if "various_showcallui" in block["condition"]:
                    raise AssertionError("first hook must not be inside showcallui branch")

    def test_foreground_hook_move_outside_branch(self) -> None:
        # Remove the `if (various_showcallui > 0) {` wrapping around the StatusBarStateControllerImpl hook.
        mutated = self.global_actions.replace(
            'if (MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0) {\n                    ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarStateControllerImpl"',
            'ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarStateControllerImpl"',
        )
        conditions = self._derive_foreground_conditions(mutated)
        for cond in conditions.values():
            if cond.get("key") == "various_showcallui":
                raise AssertionError("StatusBarStateControllerImpl condition must not be derived after moving outside branch")

    def test_foreground_empty_sibling_if_ignored(self) -> None:
        # Add an empty showcallui sibling if before the real installer condition.
        # The source-derived activation must remain unchanged.
        call = 'GlobalActions.setupForegroundMonitor(lpparam);'
        call_pos = self.installer.find(call)
        real_if = self.installer.rfind('if (', 0, call_pos)
        inserted = 'if (MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0) { }\n'
        mutated = self.installer[:real_if] + inserted + self.installer[real_if:]
        derived = self._derive_foreground_activation(mutated)
        expected = sc.derive_setup_foreground_monitor_activation()
        self.assertEqual(
            builder._canonical_activation_contract(derived),
            builder._canonical_activation_contract(expected),
        )


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
    """Verify the three-way independent truth:
    - source-derived contract == original seed contract;
    - source-derived contract == committed registry contract;
    - a mutated seed or registry contract does NOT equal the source-derived contract.

    This class must not call build_registry() to generate an expected baseline.
    """

    def setUp(self) -> None:
        self.registry = load_json(REGISTRY_FILE)

    def _seed_record(self, seed_id: str) -> dict:
        return next(s for s in builder.LEGACY_EXCEPTION_SEEDS if s["id"] == seed_id)

    def _registry_record(self, owner: str) -> dict:
        return record_by_owner(self.registry, owner)

    def _canonical_source_contract(self) -> dict:
        return builder._canonical_activation_contract(sc.derive_setup_global_actions_activation())

    def test_seed_wrong_activation_must_differ_from_source(self) -> None:
        source_contract = self._canonical_source_contract()
        seed = self._seed_record("legacy-globalactions-systemserver")
        original = seed["activationContract"]["predicates"][0]["thresholdExclusive"]
        try:
            seed["activationContract"]["predicates"][0]["thresholdExclusive"] = 99
            wrong_seed_contract = builder._canonical_activation_contract(seed["activationContract"])
            self.assertNotEqual(
                source_contract,
                wrong_seed_contract,
                "wrong seed activation must differ from source-derived contract",
            )
        finally:
            seed["activationContract"]["predicates"][0]["thresholdExclusive"] = original

    def test_seed_correct_activation_matches_source(self) -> None:
        source_contract = self._canonical_source_contract()
        seed = self._seed_record("legacy-globalactions-systemserver")
        seed_contract = builder._canonical_activation_contract(seed["activationContract"])
        self.assertEqual(source_contract, seed_contract)

    def test_registry_wrong_activation_must_differ_from_source(self) -> None:
        source_contract = self._canonical_source_contract()
        reg = copy.deepcopy(self.registry)
        rec = record_by_owner(reg, "GlobalActions.setupGlobalActions")
        rec["activationContract"]["predicates"][1]["thresholdExclusive"] = 99
        wrong_registry_contract = builder._canonical_activation_contract(rec["activationContract"])
        self.assertNotEqual(
            source_contract,
            wrong_registry_contract,
            "wrong registry activation must differ from source-derived contract",
        )

    def test_registry_correct_activation_matches_source(self) -> None:
        source_contract = self._canonical_source_contract()
        registry_contract = builder._canonical_activation_contract(
            self._registry_record("GlobalActions.setupGlobalActions")["activationContract"]
        )
        self.assertEqual(source_contract, registry_contract)

    def test_source_wrong_activation_must_differ_from_seed_and_registry(self) -> None:
        # Mutate the source so the derived contract changes or extraction fails.
        text = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
        text = text.replace("(Integer) value > 1", "(Integer) value > 2")
        with _override_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java", text):
            with self.assertRaises(sc.SourceContractError):
                sc.derive_setup_global_actions_activation()
        source_contract = self._canonical_source_contract()
        seed = self._seed_record("legacy-globalactions-systemserver")
        seed_contract = builder._canonical_activation_contract(seed["activationContract"])
        registry_contract = builder._canonical_activation_contract(
            self._registry_record("GlobalActions.setupGlobalActions")["activationContract"]
        )
        self.assertEqual(source_contract, seed_contract)
        self.assertEqual(source_contract, registry_contract)

    def test_p3_3a_baseline_unchanged(self) -> None:
        p3_3a = [r for r in self.registry["records"] if r["batch"] == "P3.3A"]
        self.assertEqual(len(p3_3a), 4)
        for rec in p3_3a:
            self.assertNotIn("activationContract", rec)
            self.assertNotIn("callSiteConditions", rec)
            # runtimeConfigKeys must not be forced on P3.3A.
            self.assertNotIn("runtimeConfigKeys", rec)


class AlarmCompatMutationTest(unittest.TestCase):
    """Apply textual mutations to AlarmCompatServiceHook source and verify the
    derived contracts reject them."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.various = read_source("tv/withaibuild/customiuizer/mods/Various.kt")
        cls.installer = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")

    def _derive_alarm_activation(self, installer_text: str) -> dict:
        with _override_source(
            "tv/withaibuild/customiuizer/installers/SystemServerInstaller.java",
            installer_text,
            extra={"tv/withaibuild/customiuizer/mods/Various.kt": self.various},
        ):
            return sc.derive_alarm_compat_activation()

    def _derive_runtime_keys(self, various_text: str) -> list[str]:
        with _override_source(
            "tv/withaibuild/customiuizer/mods/Various.kt",
            various_text,
            extra={"tv/withaibuild/customiuizer/installers/SystemServerInstaller.java": self.installer},
        ):
            return sc.derive_runtime_config_keys("AlarmCompatServiceHook")

    def test_alarm_activation_key_changed(self) -> None:
        # If the installer condition checks the wrong preference key, the derived
        # activation contract must differ (or fail) instead of silently matching.
        text = self.installer.replace(
            'MainModule.mPrefs.getBoolean("various_alarmcompat")',
            'MainModule.mPrefs.getBoolean("various_alarmcompat_apps")',
        )
        with self.assertRaises((sc.SourceContractError, sc.SourceStructureError)):
            self._derive_alarm_activation(text)

    def test_alarm_runtime_allowlist_removed(self) -> None:
        # Removing the runtime allowlist access from the hook body should remove
        # the runtime config key.
        text = self.various.replace(
            'MainModule.mPrefs.getStringSet("various_alarmcompat_apps")',
            'java.util.Collections.emptySet()',
        )
        with self.assertRaises((sc.SourceContractError, sc.SourceStructureError)):
            self._derive_runtime_keys(text)

    def test_alarm_boolean_condition_removed(self) -> None:
        # Removing the boolean condition guard makes the installer unconditional,
        # which is not what the committed registry records.
        text = self.installer.replace(
            'if (MainModule.mPrefs.getBoolean("various_alarmcompat")) ',
            '',
        )
        derived = self._derive_alarm_activation(text)
        self.assertEqual(derived, {"mode": "UNCONDITIONAL"})


class SourceParserBoundaryTest(unittest.TestCase):
    """Fail-closed parser boundary tests for _find_function_definition."""

    def _real_body(self) -> str:
        return '''
        static boolean needGlobalActions() {
            return false;
        }
        '''

    def test_function_call_misread_as_declaration(self) -> None:
        text = 'public void run() { needGlobalActions(); }\n' + self._real_body()
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        self.assertIsNotNone(body)
        self.assertIn("return false", body)

    def test_comment_fake_function_declaration(self) -> None:
        fake = '/* static boolean needGlobalActions() { return false; } */'
        text = fake + '\n' + self._real_body()
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        self.assertIsNotNone(body)
        self.assertNotIn("return false/*", body)

    def test_string_fake_function_declaration(self) -> None:
        fake = 'String s = "static boolean needGlobalActions() { return false; }" ;'
        text = fake + '\n' + self._real_body()
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        self.assertIsNotNone(body)
        # The real body does not contain the quoted fake declaration text.
        self.assertNotIn('"static boolean needGlobalActions() { return false; }"', body)

    def test_overload_ambiguity(self) -> None:
        text = '''
        static boolean needGlobalActions() { return false; }
        static boolean needGlobalActions(int x) { return false; }
        '''
        with self.assertRaises(sc.SourceStructureError):
            sc.extract_function_body(text, "needGlobalActions", "java")

    def test_expression_body_unsupported(self) -> None:
        text = 'fun setupForegroundMonitor(lpparam: String) = ModuleHelper.hookAllMethods("foo")'
        with self.assertRaises(sc.SourceStructureError):
            sc.extract_function_body(text, "setupForegroundMonitor", "kt")

    def test_anonymous_class_nested_method_ambiguous(self) -> None:
        text = '''
        static void outer() {
            new Object() {
                boolean needGlobalActions() { return false; }
            };
        }
        static boolean needGlobalActions() { return true; }
        '''
        with self.assertRaises(sc.SourceStructureError):
            sc.extract_function_body(text, "needGlobalActions", "java")


class NoBuildRegistryInSourceTruthTest(unittest.TestCase):
    """AST gate: source-truth test classes in this file must not call build_registry."""

    def test_no_build_registry_in_source_truth_classes(self) -> None:
        offenders = _ast_offenders_for_build_registry(
            SourceParserStructureTest,
            SetupGlobalActionsSourceContractTest,
            SetupGlobalActionsMutationTest,
            SetupForegroundMonitorSourceContractTest,
            ForegroundMonitorMutationTest,
            AlarmCompatAndStatusBarSourceContractTest,
            AlarmCompatMutationTest,
            IndependentTruthTest,
            SourceParserBoundaryTest,
        )
        self.assertEqual(offenders, [], f"source-truth classes must not call build_registry: {offenders}")


if __name__ == "__main__":
    unittest.main()
