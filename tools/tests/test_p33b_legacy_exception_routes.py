#!/usr/bin/env python3
"""A13-P3.3B-R2 source logic, validator and route evidence tests.

Expected values are derived from:
- the committed registry JSON;
- production Java/Kotlin installer and hook function source;
- the read-only `legacy_exception_source_contract` parser.

The `build_legacy_exception_registry` generator is only used in the dedicated
`GeneratorConsistencyTest` to prove that `build_registry(LEGACY_EXCEPTION_SEEDS)`
matches the committed canonical output. It is not used as the source of truth
for route, preference, hook-target or call-site expected values.
"""

from __future__ import annotations

import ast
import contextlib
import copy
import json
import re
import sys
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "tools"))
sys.path.insert(0, str(REPO_ROOT / "tools" / "tests"))
REGISTRY_FILE = REPO_ROOT / "docs" / "audit" / "A13_LEGACY_EXCEPTION_REGISTRY.json"
SOURCE_ROOT = REPO_ROOT / "app" / "src" / "main" / "java"

import build_legacy_exception_registry as builder
import legacy_exception_source_contract as sc
import p33b_ast_policy as ast_policy


def load_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def read_source(rel: str) -> str:
    return (SOURCE_ROOT / rel).read_text(encoding="utf-8")


def record_by_owner(registry: dict, owner: str) -> dict:
    for rec in registry["records"]:
        if rec["owner"] == owner:
            return rec
    raise AssertionError(f"record for owner {owner!r} not found")


def call_ids_in_function(rel: str, func: str) -> list[str]:
    sites = builder.scan_legacy_call_sites()
    return [
        builder._stable_call_id(s["rel"], s["line"], s["function"])
        for s in sites
        if s["rel"] == rel and s["function"] == func and s["category"] == "LEGACY_EXCEPTION"
    ]


@contextlib.contextmanager
def _override_source(rel: str, text: str):
    """Temporarily point the source contract parser at an in-memory file."""
    original_root = sc.SOURCE_ROOT
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp) / "app" / "src" / "main" / "java"
        path = root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        sc.SOURCE_ROOT = root
        try:
            yield
        finally:
            sc.SOURCE_ROOT = original_root


# ---------------------------------------------------------------------------
# Positive route tests
# ---------------------------------------------------------------------------


class P3_3B_LegacyExceptionRouteTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.registry = load_json(REGISTRY_FILE)

    # --- P3.3A preservation ---

    def test_01_p3_3a_has_four_records(self) -> None:
        p3_3a = [r for r in self.registry["records"] if r["batch"] == "P3.3A"]
        self.assertEqual(len(p3_3a), 4)

    def test_02_p3_3a_covers_eleven_calls(self) -> None:
        p3_3a = [r for r in self.registry["records"] if r["batch"] == "P3.3A"]
        total = sum(len(r["coveredCallSites"]) for r in p3_3a)
        self.assertEqual(total, 11)

    def test_03_p3_3a_records_unchanged(self) -> None:
        for rec in self.registry["records"]:
            if rec["batch"] == "P3.3A":
                self.assertNotIn("activationContract", rec)
                self.assertNotIn("callSiteConditions", rec)
                self.assertNotIn("runtimeConfigKeys", rec)

    def test_04_p3_3a_snapshot_matches_expected(self) -> None:
        expected_ids = {
            "0f39ad5a6b4ea752",
            "1676c40a75d45cc9",
            "2dab6505e43f17aa",
            "d0b55fb7aa83c53a",
        }
        actual = {r["id"] for r in self.registry["records"] if r["batch"] == "P3.3A"}
        self.assertEqual(actual, expected_ids)

    # --- schema and counts ---

    def test_05_schema_version_is_four(self) -> None:
        self.assertEqual(self.registry["schemaVersion"], 4)

    def test_06_record_counts(self) -> None:
        self.assertEqual(self.registry["firstBatchSize"], 4)
        self.assertEqual(self.registry["registeredRecordCount"], 8)
        self.assertEqual(self.registry["batchCounts"], {"P3.3A": 4, "P3.3B": 4})

    def test_07_total_covered_calls(self) -> None:
        total = sum(len(r["coveredCallSites"]) for r in self.registry["records"])
        self.assertEqual(total, 19)

    def test_08_p3_3b_covers_eight_calls(self) -> None:
        p3_3b = [r for r in self.registry["records"] if r["batch"] == "P3.3B"]
        total = sum(len(r["coveredCallSites"]) for r in p3_3b)
        self.assertEqual(total, 8)

    # --- setupGlobalActions ---

    def test_09_setup_global_actions_covered_calls_match_source(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupGlobalActions")
        expected = set(call_ids_in_function("tv/withaibuild/customiuizer/mods/GlobalActions.kt", "setupGlobalActions"))
        self.assertEqual(set(rec["coveredCallSites"]), expected)

    def test_10_setup_global_actions_installer_condition(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
        call_pos = text.find("GlobalActions.setupGlobalActions(lpparam);")
        self.assertGreater(call_pos, 0)
        condition = sc.get_enclosing_if_condition(text, call_pos)
        self.assertEqual(condition, "needGlobalActions()")

    def test_11_setup_global_actions_source_activation_matches_registry(self) -> None:
        derived = sc.derive_setup_global_actions_activation()
        rec = record_by_owner(self.registry, "GlobalActions.setupGlobalActions")
        self.assertEqual(builder._canonical_activation_contract(derived), rec["activationContract"])

    def test_12_setup_global_actions_source_predicates(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
        body = sc.extract_function_body(text, "needGlobalActions", "java")
        blocks = sc._extract_all_if_blocks(body)
        # First if: dynamic _action chain.
        cond = blocks[0]["condition"]
        operands, operators = sc.parse_boolean_expression(cond)
        self.assertEqual(operators, ["&&", "&&", "&&"])
        self.assertEqual(len(operands), 4)
        # Second if: media OR.
        cond = blocks[1]["condition"]
        operands, operators = sc.parse_boolean_expression(cond)
        self.assertEqual(operators, ["||"])
        self.assertIn("controls_volumemedia_up", operands[0])
        self.assertIn("controls_volumemedia_down", operands[1])

    def test_13_setup_global_actions_preference_keys_exact(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupGlobalActions")
        self.assertEqual(
            rec["preferenceKeys"],
            ["controls_mediaplayer_apps", "controls_volumemedia_down", "controls_volumemedia_up"],
        )

    def test_14_setup_global_actions_runtime_config_empty(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupGlobalActions")
        self.assertEqual(rec.get("runtimeConfigKeys"), [])

    def test_15_setup_global_actions_no_enumerated_action_keys(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupGlobalActions")
        for pk in rec["preferenceKeys"]:
            self.assertFalse(pk.endswith("_action"), f"{pk} must not be enumerated in preferenceKeys")

    def test_16_setup_global_actions_excluded_keys_absent(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupGlobalActions")
        forbidden = {
            "system_cc_custom_clock_action",
            "launcher_swipedown2_action",
            "launcher_swipeup2_action",
            "controls_backlong_action",
        }
        self.assertTrue(forbidden.isdisjoint(rec["preferenceKeys"]), "forbidden _action keys must not appear")

    # --- setupStatusBar ---

    def test_17_setup_status_bar_covered_call_matches_source(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupStatusBar")
        expected = set(call_ids_in_function("tv/withaibuild/customiuizer/mods/GlobalActions.kt", "setupStatusBar"))
        self.assertEqual(set(rec["coveredCallSites"]), expected)

    def test_18_setup_status_bar_unconditional(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/installers/SystemUiInstaller.java")
        call_pos = text.find("GlobalActions.setupStatusBar(lpparam);")
        self.assertGreater(call_pos, 0)
        condition = sc.get_enclosing_if_condition(text, call_pos)
        self.assertIsNone(condition)
        rec = record_by_owner(self.registry, "GlobalActions.setupStatusBar")
        self.assertEqual(rec["activationContract"], {"mode": "UNCONDITIONAL"})
        self.assertEqual(rec["preferenceKeys"], [])

    # --- setupForegroundMonitor ---

    def test_19_setup_foreground_monitor_covered_calls_match_source(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupForegroundMonitor")
        expected = set(call_ids_in_function("tv/withaibuild/customiuizer/mods/GlobalActions.kt", "setupForegroundMonitor"))
        self.assertEqual(set(rec["coveredCallSites"]), expected)

    def test_20_setup_foreground_monitor_installer_condition(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/installers/SystemUiInstaller.java")
        call_pos = text.find("GlobalActions.setupForegroundMonitor(lpparam);")
        condition = sc.get_enclosing_if_condition(text, call_pos)
        self.assertIsNotNone(condition)
        operands, operators = sc.parse_boolean_expression(condition)
        self.assertEqual(operators, ["||"])
        self.assertIn("various_showcallui", condition)
        self.assertIn("controls_volumecursor", condition)

    def test_21_setup_foreground_monitor_source_activation_matches_registry(self) -> None:
        derived = sc.derive_setup_foreground_monitor_activation()
        rec = record_by_owner(self.registry, "GlobalActions.setupForegroundMonitor")
        self.assertEqual(builder._canonical_activation_contract(derived), rec["activationContract"])

    def test_22_setup_foreground_monitor_call_site_conditions_match_source(self) -> None:
        derived = sc.derive_setup_foreground_monitor_call_site_conditions()
        rec = record_by_owner(self.registry, "GlobalActions.setupForegroundMonitor")
        self.assertEqual(derived, {int(k.split(":")[1]): v for k, v in rec.get("callSiteConditions", {}).items()})

    def test_23_setup_foreground_monitor_third_call_in_show_call_ui_block(self) -> None:
        body = sc.extract_function_body(
            read_source("tv/withaibuild/customiuizer/mods/GlobalActions.kt"),
            "setupForegroundMonitor",
            "kt",
        )
        calls = sc.find_hook_calls_in_function(
            "tv/withaibuild/customiuizer/mods/GlobalActions.kt",
            "setupForegroundMonitor",
        )
        status_call = [c for c in calls if "StatusBarStateControllerImpl" in (c.get("target") or "")]
        self.assertEqual(len(status_call), 1)
        status = status_call[0]
        # Find innermost if containing the StatusBarStateControllerImpl call.
        enclosing = None
        for block in sc._extract_all_if_blocks(body):
            if block["body_start"] < body.find(status["text"]) < block["body_end"]:
                if enclosing is None or block["body_start"] > enclosing["body_start"]:
                    enclosing = block
        self.assertIsNotNone(enclosing)
        ok, cond = sc._is_showcallui_condition(enclosing["condition"])
        self.assertTrue(ok)
        self.assertEqual(cond, {"kind": "INT_KEY_GT", "key": "various_showcallui", "thresholdExclusive": 0})

    def test_24_setup_foreground_monitor_preference_keys_exact(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupForegroundMonitor")
        self.assertEqual(rec["preferenceKeys"], ["controls_volumecursor", "various_showcallui"])

    def test_25_setup_foreground_monitor_runtime_config_empty(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupForegroundMonitor")
        self.assertEqual(rec.get("runtimeConfigKeys"), [])

    # --- AlarmCompat ---

    def test_26_alarm_compat_covered_calls_match_source(self) -> None:
        rec = record_by_owner(self.registry, "AlarmCompatServiceHook")
        expected = set(call_ids_in_function("tv/withaibuild/customiuizer/mods/Various.kt", "AlarmCompatServiceHook"))
        self.assertEqual(set(rec["coveredCallSites"]), expected)

    def test_27_alarm_compat_installer_condition(self) -> None:
        text = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
        call_pos = text.find("Various.AlarmCompatServiceHook(lpparam);")
        condition = sc.get_enclosing_if_condition(text, call_pos)
        self.assertIsNotNone(condition)
        self.assertIn("various_alarmcompat", condition)

    def test_28_alarm_compat_source_activation_matches_registry(self) -> None:
        derived = sc.derive_alarm_compat_activation()
        rec = record_by_owner(self.registry, "AlarmCompatServiceHook")
        self.assertEqual(builder._canonical_activation_contract(derived), rec["activationContract"])

    def test_29_alarm_compat_runtime_config_key(self) -> None:
        derived = sc.derive_runtime_config_keys("AlarmCompatServiceHook")
        rec = record_by_owner(self.registry, "AlarmCompatServiceHook")
        self.assertEqual(derived, rec.get("runtimeConfigKeys"))
        self.assertEqual(derived, ["various_alarmcompat_apps"])

    def test_30_alarm_compat_preference_keys(self) -> None:
        rec = record_by_owner(self.registry, "AlarmCompatServiceHook")
        self.assertEqual(rec["preferenceKeys"], ["various_alarmcompat", "various_alarmcompat_apps"])


# ---------------------------------------------------------------------------
# Generator consistency tests
# ---------------------------------------------------------------------------


class P3_3B_GeneratorConsistencyTest(unittest.TestCase):
    """Tests that LEGACY_EXCEPTION_SEEDS can reproduce the committed registry.

    This is the ONLY test class in this module that may call build_registry().
    Route, activation, preference and call-site expected values must be derived
    from production source and the committed JSON, not from the generator.
    """

    def setUp(self) -> None:
        self.registry = load_json(REGISTRY_FILE)

    def test_build_registry_matches_committed(self) -> None:
        """The generator must reproduce the committed registry exactly."""
        sites = builder.scan_legacy_call_sites()
        expected = builder.build_registry(sites)
        diffs = builder.canonical_diff(expected, self.registry)
        self.assertEqual(diffs, [])

    def test_build_registry_ast_policy(self) -> None:
        """AST gate: only this class may reference build_registry in this module."""
        source = Path(__file__).read_text(encoding="utf-8")
        allowed = {"P3_3B_GeneratorConsistencyTest"}
        real = ast_policy.find_build_registry_violations(source, allowed)
        self.assertEqual(real, [], f"real module has build_registry violations: {real}")

        target_classes = [
            "P3_3B_LegacyExceptionRouteTest",
            "P3_3B_SourceMutationTest",
            "P3_3B_ActivationContractValidationTest",
            "P3_3B_CallSiteConditionValidationTest",
            "P3_3B_ActivationContractMutationTest",
        ]

        mutations = [
            ("direct call", "def test_x(self):\n        build_registry([])\n"),
            ("builder attribute", "def test_x(self):\n        builder.build_registry([])\n"),
            (
                "import alias",
                "def test_x(self):\n        from tools.build_legacy_exception_registry import build_registry as br\n        br([])\n",
            ),
            (
                "module alias",
                "def test_x(self):\n        import tools.build_legacy_exception_registry as reg\n        reg.build_registry([])\n",
            ),
            (
                "assignment alias",
                "def test_x(self):\n        fn = builder.build_registry\n        fn([])\n",
            ),
            (
                "getattr",
                "def test_x(self):\n        getattr(builder, \"build_registry\")([])\n",
            ),
            (
                "partial/callback",
                "def test_x(self):\n        import functools\n        functools.partial(builder.build_registry, [])([])\n",
            ),
            (
                "lambda reference",
                "def test_x(self):\n        some_call(lambda: builder.build_registry)\n",
            ),
        ]

        for target in target_classes:
            for label, method in mutations:
                with self.subTest(class_name=target, mutation=label):
                    mutated = ast_policy.inject_method(source, target, method)
                    violations = ast_policy.find_build_registry_violations(mutated, allowed)
                    reported = [v for v in violations if v.class_name == target]
                    self.assertTrue(
                        reported,
                        f"mutation {label} in {target} should be rejected",
                    )

        with self.subTest(mutation="future unknown TestCase"):
            future = source + "\n\nclass FutureEvilTest(unittest.TestCase):\n    def test_x(self):\n        builder.build_registry([])\n"
            violations = ast_policy.find_build_registry_violations(future, allowed)
            self.assertTrue(
                any(v.class_name == "FutureEvilTest" for v in violations),
                "future unknown TestCase must be rejected",
            )

        with self.subTest(mutation="module-level helper"):
            helper = (
                source
                + "\n\ndef _module_helper():\n    return builder.build_registry\n"
            )
            violations = ast_policy.find_build_registry_violations(helper, allowed)
            self.assertTrue(
                any(v.class_name is None for v in violations),
                "module-level helper must be rejected",
            )

        with self.subTest(false_positive="docstring"):
            mutated = ast_policy.inject_method(
                source,
                "P3_3B_LegacyExceptionRouteTest",
                'def test_x(self):\n        """build_registry docstring"""\n        self.assertTrue(True)\n',
            )
            violations = ast_policy.find_build_registry_violations(mutated, allowed)
            self.assertEqual(violations, [], f"docstring must not trigger: {violations}")

        with self.subTest(false_positive="string literal"):
            mutated = ast_policy.inject_method(
                source,
                "P3_3B_LegacyExceptionRouteTest",
                'def test_x(self):\n        x = "build_registry"\n        self.assertIsNotNone(x)\n',
            )
            violations = ast_policy.find_build_registry_violations(mutated, allowed)
            self.assertEqual(violations, [], f"string literal must not trigger: {violations}")

        with self.subTest(false_positive="builder.validate"):
            mutated = ast_policy.inject_method(
                source,
                "P3_3B_LegacyExceptionRouteTest",
                "def test_x(self):\n        builder.validate([])\n",
            )
            violations = ast_policy.find_build_registry_violations(mutated, allowed)
            self.assertEqual(violations, [], f"builder.validate must not trigger: {violations}")


# ---------------------------------------------------------------------------
# Activation contract / callSiteConditions validation tests
# ---------------------------------------------------------------------------


class P3_3B_ActivationContractValidationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.registry = load_json(REGISTRY_FILE)

    def _mutate(self, owner: str, mutate: callable) -> dict:
        reg = copy.deepcopy(self.registry)
        rec = record_by_owner(reg, owner)
        mutate(rec)
        return reg

    def _assert_validation_fails(self, reg: dict, message: str) -> None:
        errors = builder.validate(reg)
        self.assertTrue(errors, message)

    def test_31_unknown_activation_mode(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"].update({"mode": "ALL_OF"}),
        )
        self._assert_validation_fails(reg, "unknown activationContract mode must fail")

    def test_32_unknown_predicate_kind(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"].append({"kind": "STRING_CONTAINS", "key": "x"}),
        )
        self._assert_validation_fails(reg, "unknown predicate kind must fail")

    def test_33_predicate_missing_required_field(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][0].__delitem__("thresholdExclusive"),
        )
        self._assert_validation_fails(reg, "predicate missing required field must fail")

    def test_34_dynamic_suffix_empty(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][0].update({"keySuffix": ""}),
        )
        self._assert_validation_fails(reg, "empty keySuffix must fail")

    def test_35_dynamic_suffix_missing_value_type(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][0].pop("valueType"),
        )
        self._assert_validation_fails(reg, "missing valueType must fail")

    def test_36_int_gt_non_integer_threshold(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["activationContract"]["predicates"][1].update({"thresholdExclusive": "0"}),
        )
        self._assert_validation_fails(reg, "string thresholdExclusive must fail")

    def test_37_activation_contract_not_object(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r.update({"activationContract": "UNCONDITIONAL"}),
        )
        errors = builder.validate(reg)
        self.assertTrue(any("INVALID_ACTIVATION_CONTRACT" in e for e in errors))

    def test_38_threshold_bool_rejected(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["activationContract"]["predicates"][1].update({"thresholdExclusive": True}),
        )
        errors = builder.validate(reg)
        self.assertTrue(any("INVALID_ACTIVATION_THRESHOLD" in e for e in errors))

    def test_39_threshold_float_rejected(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["activationContract"]["predicates"][1].update({"thresholdExclusive": 0.0}),
        )
        errors = builder.validate(reg)
        self.assertTrue(any("INVALID_ACTIVATION_THRESHOLD" in e for e in errors))

    def test_40_threshold_null_rejected(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["activationContract"]["predicates"][1].update({"thresholdExclusive": None}),
        )
        errors = builder.validate(reg)
        self.assertTrue(any("INVALID_ACTIVATION_THRESHOLD" in e for e in errors))

    def test_41_unknown_activation_field(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"].update({"extra": 1}),
        )
        errors = builder.validate(reg)
        self.assertTrue(any("unknown field" in e.lower() for e in errors))

    def test_42_unconditional_with_predicates(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupStatusBar",
            lambda r: r.update({
                "activationContract": {"mode": "UNCONDITIONAL", "predicates": [{"kind": "BOOLEAN_KEY_TRUE", "key": "x"}]}
            }),
        )
        errors = builder.validate(reg)
        self.assertTrue(any("UNCONDITIONAL" in e for e in errors))

    def test_43_empty_predicates(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"].update({"predicates": []}),
        )
        errors = builder.validate(reg)
        self.assertTrue(errors, "empty predicates must fail")

    def test_44_duplicate_predicate(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["activationContract"]["predicates"].append(r["activationContract"]["predicates"][0]),
        )
        errors = builder.validate(reg)
        self.assertTrue(any("duplicate" in e.lower() for e in errors))

    def test_45_activation_contract_in_canonical_diff(self) -> None:
        reg = copy.deepcopy(self.registry)
        rec = record_by_owner(reg, "GlobalActions.setupGlobalActions")
        rec["activationContract"]["mode"] = "UNCONDITIONAL"
        diffs = builder.canonical_diff(self.registry, reg)
        self.assertTrue(any("activationContract" in d or "mode" in d for d in diffs))

    def test_46_call_site_conditions_in_canonical_diff(self) -> None:
        reg = copy.deepcopy(self.registry)
        rec = record_by_owner(reg, "GlobalActions.setupForegroundMonitor")
        rec["callSiteConditions"] = {}
        diffs = builder.canonical_diff(self.registry, reg)
        self.assertTrue(any("callSiteConditions" in d for d in diffs))

    def test_47_runtime_config_keys_in_canonical_diff(self) -> None:
        reg = copy.deepcopy(self.registry)
        rec = record_by_owner(reg, "AlarmCompatServiceHook")
        rec["runtimeConfigKeys"] = []
        diffs = builder.canonical_diff(self.registry, reg)
        self.assertTrue(any("runtimeConfigKeys" in d for d in diffs))


class P3_3B_CallSiteConditionValidationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.registry = load_json(REGISTRY_FILE)

    def _mutate(self, owner: str, mutate: callable) -> dict:
        reg = copy.deepcopy(self.registry)
        rec = record_by_owner(reg, owner)
        mutate(rec)
        return reg

    def _assert_validation_fails(self, reg: dict, message: str) -> None:
        errors = builder.validate(reg)
        self.assertTrue(errors, message)

    def test_50_call_site_condition_for_non_covered_call(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:999:setupForegroundMonitor": {
                    "kind": "INT_KEY_GT", "key": "various_showcallui", "thresholdExclusive": 0,
                }
            }),
        )
        self._assert_validation_fails(reg, "condition for non-covered call must fail")

    def test_51_call_site_condition_unknown_key(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:765:setupForegroundMonitor": {
                    "kind": "INT_KEY_GT", "key": "unknown_key", "thresholdExclusive": 0,
                }
            }),
        )
        self._assert_validation_fails(reg, "condition key not in preferenceKeys must fail")

    def test_52_call_site_condition_missing_field(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:765:setupForegroundMonitor": {
                    "kind": "INT_KEY_GT",
                }
            }),
        )
        self._assert_validation_fails(reg, "condition missing required field must fail")

    def test_53_call_site_condition_unknown_kind(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:765:setupForegroundMonitor": {
                    "kind": "FLOAT_GT", "key": "various_showcallui", "thresholdExclusive": 0,
                }
            }),
        )
        self._assert_validation_fails(reg, "unknown condition kind must fail")

    def test_54_call_site_condition_not_activation_branch(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:765:setupForegroundMonitor": {
                    "kind": "BOOLEAN_KEY_TRUE", "key": "various_showcallui",
                }
            }),
        )
        errors = builder.validate(reg)
        self.assertTrue(any("CALL_SITE_CONDITION_NOT_ACTIVATION_BRANCH" in e for e in errors))

    def test_55_call_site_condition_threshold_bool(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:765:setupForegroundMonitor": {
                    "kind": "INT_KEY_GT", "key": "various_showcallui", "thresholdExclusive": True,
                }
            }),
        )
        errors = builder.validate(reg)
        self.assertTrue(any("INVALID_ACTIVATION_THRESHOLD" in e for e in errors))


# ---------------------------------------------------------------------------
# Mutation tests against committed registry
# ---------------------------------------------------------------------------


class P3_3B_ActivationContractMutationTest(unittest.TestCase):
    """Schema mutations against a deep copy of the committed registry.

    This class must not use build_registry() as the expected baseline; it uses
    the committed JSON and canonical_diff() to detect drift.
    """

    def setUp(self) -> None:
        self.registry = load_json(REGISTRY_FILE)
        self.committed = copy.deepcopy(self.registry)

    def _mutate(self, owner: str, mutate: callable) -> dict:
        reg = copy.deepcopy(self.registry)
        rec = record_by_owner(reg, owner)
        mutate(rec)
        return reg

    def _assert_killed(self, reg: dict, message: str) -> None:
        errors = builder.validate(reg)
        if errors:
            return
        diffs = builder.canonical_diff(self.committed, reg)
        self.assertTrue(diffs, message)

    def test_60_remove_dynamic_activation_contract(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r.pop("activationContract"),
        )
        self._assert_killed(reg, "remove activationContract must fail canonical/validation")

    def test_61_change_dynamic_suffix(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][0].update({"keySuffix": "_click"}),
        )
        self._assert_killed(reg, "changed keySuffix must fail canonical")

    def test_62_remove_integer_type_guard(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][0].pop("valueType"),
        )
        self._assert_killed(reg, "removed valueType must fail validation")

    def test_63_change_action_threshold(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][0].update({"thresholdExclusive": 2}),
        )
        self._assert_killed(reg, "changed action threshold must fail canonical")

    def test_64_remove_media_player_apps(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][1].pop("requiredNonEmptySetKey"),
        )
        self._assert_killed(reg, "removed requiredNonEmptySetKey must fail validation")

    def test_65_change_media_or_to_and(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"].update({"mode": "ALL_OF"}),
        )
        self._assert_killed(reg, "invalid mode must fail validation")

    def test_66_remove_media_up_key(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][1]["integerKeys"].remove("controls_volumemedia_up"),
        )
        self._assert_killed(reg, "removed media up key must fail canonical")

    def test_67_remove_media_down_key(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][1]["integerKeys"].remove("controls_volumemedia_down"),
        )
        self._assert_killed(reg, "removed media down key must fail canonical")

    def test_68_add_static_action_enumeration(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["preferenceKeys"].append("controls_backlong_action"),
        )
        self._assert_killed(reg, "enumerated _action key in preferenceKeys must fail validation")

    def test_69_add_boolean_action_key(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["preferenceKeys"].append("system_cc_custom_clock_action"),
        )
        self._assert_killed(reg, "boolean _action key in preferenceKeys must fail validation")

    def test_70_remove_foreground_call_condition(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r.update({"callSiteConditions": {}}),
        )
        self._assert_killed(reg, "removed callSiteConditions must fail canonical")

    def test_71_change_foreground_call_condition_key(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:765:setupForegroundMonitor": {
                    "kind": "BOOLEAN_KEY_TRUE", "key": "controls_volumecursor",
                }
            }),
        )
        self._assert_killed(reg, "changed foreground call condition to wrong key must fail canonical/validation")

    def test_72_attach_condition_to_wrong_call(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:743:setupForegroundMonitor": {
                    "kind": "INT_KEY_GT", "key": "various_showcallui", "thresholdExclusive": 0,
                }
            }),
        )
        self._assert_killed(reg, "condition attached to wrong call must fail canonical/validation")

    def test_73_fabricated_call_condition_id(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:999:fake": {
                    "kind": "INT_KEY_GT", "key": "various_showcallui", "thresholdExclusive": 0,
                }
            }),
        )
        self._assert_killed(reg, "fabricated call condition id must fail validation")

    def test_74_remove_unconditional_statusbar_activation(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupStatusBar",
            lambda r: r.pop("activationContract"),
        )
        self._assert_killed(reg, "removed statusbar activationContract must fail canonical")

    def test_75_change_alarmcompat_activation_key(self) -> None:
        reg = self._mutate(
            "AlarmCompatServiceHook",
            lambda r: r["activationContract"]["predicates"][0].update({"key": "various_alarmcompat_apps"}),
        )
        self._assert_killed(reg, "changed alarmcompat activation key must fail canonical/validation")

    def test_76_runtime_config_key_misclassified(self) -> None:
        reg = self._mutate(
            "AlarmCompatServiceHook",
            lambda r: r["runtimeConfigKeys"].append("various_alarmcompat"),
        )
        errors = builder.validate(reg)
        self.assertTrue(errors, "runtime key overlapping activation must fail")

    def test_77_remove_runtime_config_key(self) -> None:
        reg = self._mutate(
            "AlarmCompatServiceHook",
            lambda r: r.pop("runtimeConfigKeys"),
        )
        self._assert_killed(reg, "removed runtimeConfigKeys must fail canonical")

    def test_78_stale_activation_contract_schema_version(self) -> None:
        reg = copy.deepcopy(self.registry)
        reg["schemaVersion"] = 3
        self._assert_killed(reg, "stale schemaVersion must fail canonical")

    def test_79_circular_expected_from_seed_detection(self) -> None:
        # Simulate the old hand-enumerated seed expectation that the R1 repair removed.
        old_bogus_keys = [
            "controls_backlong_action",
            "controls_fingerprint2_action",
            "system_cc_custom_clock_action",
            "launcher_swipedown2_action",
        ]
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: (r.pop("activationContract", None), r.__setitem__("preferenceKeys", sorted(old_bogus_keys))),
        )
        self._assert_killed(reg, "old seed-enumerated _action keys must not be accepted as expected")


# ---------------------------------------------------------------------------
# Source-level mutation tests
# ---------------------------------------------------------------------------


class P3_3B_SourceMutationTest(unittest.TestCase):
    """Apply textual mutations to a temp copy of production source and verify
    the final source-derived contract extraction fails or produces a different
    contract.  Intermediate parser assertions are replaced by calls to the
    actual derive_*() entry points used by the registry.
    """

    @classmethod
    def setUpClass(cls) -> None:
        cls.system_server = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
        cls.system_ui = read_source("tv/withaibuild/customiuizer/installers/SystemUiInstaller.java")
        cls.global_actions = read_source("tv/withaibuild/customiuizer/mods/GlobalActions.kt")

    def _original_global_actions_activation(self) -> dict:
        with _override_source(
            "tv/withaibuild/customiuizer/installers/SystemServerInstaller.java",
            self.system_server,
        ):
            return sc.derive_setup_global_actions_activation()

    def _derive_global_actions_activation(self, text: str) -> dict:
        with _override_source(
            "tv/withaibuild/customiuizer/installers/SystemServerInstaller.java", text
        ):
            return sc.derive_setup_global_actions_activation()

    def _derive_foreground_monitor_activation(self, text: str) -> dict:
        with _override_source(
            "tv/withaibuild/customiuizer/installers/SystemUiInstaller.java", text
        ):
            return sc.derive_setup_foreground_monitor_activation()

    def _derive_foreground_call_site_conditions(self, text: str) -> dict[int, dict]:
        with _override_source("tv/withaibuild/customiuizer/mods/GlobalActions.kt", text):
            return sc.derive_setup_foreground_monitor_call_site_conditions()

    def test_ga_action_and_to_or(self) -> None:
        text = self.system_server.replace(
            'key != null && key.endsWith("_action") && value instanceof Integer && (Integer) value > 1',
            'key != null || key.endsWith("_action") || value instanceof Integer || (Integer) value > 1',
        )
        with self.assertRaises(sc.SourceContractError):
            self._derive_global_actions_activation(text)

    def test_ga_remove_integer_guard(self) -> None:
        text = self.system_server.replace("value instanceof Integer", "value instanceof String")
        with self.assertRaises(sc.SourceContractError):
            self._derive_global_actions_activation(text)

    def test_ga_change_action_threshold(self) -> None:
        text = self.system_server.replace("(Integer) value > 1", "(Integer) value > 2")
        with self.assertRaises(sc.SourceContractError):
            self._derive_global_actions_activation(text)

    def test_ga_media_or_to_and(self) -> None:
        text = self.system_server.replace(
            'MainModule.mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0 || MainModule.mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0',
            'MainModule.mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0 && MainModule.mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0',
        )
        with self.assertRaises(sc.SourceContractError):
            self._derive_global_actions_activation(text)

    def test_ga_remove_media_up(self) -> None:
        text = self.system_server.replace(
            'MainModule.mPrefs.getStringAsInt("controls_volumemedia_up", 0) > 0 || ',
            '',
        )
        with self.assertRaises(sc.SourceContractError):
            self._derive_global_actions_activation(text)

    def test_ga_remove_media_down(self) -> None:
        text = self.system_server.replace(
            ' || MainModule.mPrefs.getStringAsInt("controls_volumemedia_down", 0) > 0',
            '',
        )
        with self.assertRaises(sc.SourceContractError):
            self._derive_global_actions_activation(text)

    def test_ga_remove_media_app_set(self) -> None:
        text = self.system_server.replace(
            'return !MainModule.mPrefs.getStringSet("controls_mediaplayer_apps").isEmpty();',
            'return false;',
        )
        with self.assertRaises(sc.SourceContractError):
            self._derive_global_actions_activation(text)

    def test_ga_remove_nonempty_negation(self) -> None:
        text = self.system_server.replace(
            'return !MainModule.mPrefs.getStringSet("controls_mediaplayer_apps").isEmpty();',
            'return MainModule.mPrefs.getStringSet("controls_mediaplayer_apps").isEmpty();',
        )
        with self.assertRaises(sc.SourceContractError):
            self._derive_global_actions_activation(text)

    def test_foreground_installer_or_to_and(self) -> None:
        text = self.system_ui.replace(
            'MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0\n                || MainModule.mPrefs.getBoolean("controls_volumecursor")',
            'MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0\n                && MainModule.mPrefs.getBoolean("controls_volumecursor")',
        )
        with self.assertRaises(sc.SourceContractError):
            self._derive_foreground_monitor_activation(text)

    def test_foreground_wrong_condition_key(self) -> None:
        text = self.system_ui.replace(
            'MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0',
            'MainModule.mPrefs.getStringAsInt("controls_volumecursor", 0) > 0',
        )
        with self.assertRaises(sc.SourceContractError):
            self._derive_foreground_monitor_activation(text)

    def test_foreground_wrong_condition_threshold(self) -> None:
        text = self.system_ui.replace(
            'MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0',
            'MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 1',
        )
        with self.assertRaises(sc.SourceContractError):
            self._derive_foreground_monitor_activation(text)

    def test_foreground_move_hook_outside_branch(self) -> None:
        # Remove the `if (various_showcallui > 0) {` wrapping around the StatusBarStateControllerImpl hook.
        mutated = self.global_actions.replace(
            'if (MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0) {\n                    ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarStateControllerImpl"',
            'ModuleHelper.hookAllMethods("com.android.systemui.statusbar.StatusBarStateControllerImpl"',
        )
        conditions = self._derive_foreground_call_site_conditions(mutated)
        # The StatusBarStateControllerImpl call should no longer have a showcallui condition.
        for cond in conditions.values():
            if cond.get("key") == "various_showcallui":
                raise AssertionError("StatusBarStateControllerImpl call site condition must not be derived after moving outside branch")

    def test_foreground_empty_sibling_if_ignored(self) -> None:
        # Add an empty showcallui sibling if before the real installer condition.
        # The source-derived activation must remain unchanged.
        call = 'GlobalActions.setupForegroundMonitor(lpparam);'
        call_pos = self.system_ui.find(call)
        real_if = self.system_ui.rfind('if (', 0, call_pos)
        inserted = 'if (MainModule.mPrefs.getStringAsInt("various_showcallui", 0) > 0) { }\n'
        mutated = self.system_ui[:real_if] + inserted + self.system_ui[real_if:]
        derived = self._derive_foreground_monitor_activation(mutated)
        expected = sc.derive_setup_foreground_monitor_activation()
        self.assertEqual(
            builder._canonical_activation_contract(derived),
            builder._canonical_activation_contract(expected),
        )


if __name__ == "__main__":
    unittest.main()
