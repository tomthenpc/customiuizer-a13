#!/usr/bin/env python3
"""A13-P3.3B-R1 activation contract and route evidence tests.

Expected values are derived from:
- the committed registry JSON;
- production Java/Kotlin installer and hook function source;
- hard-coded source-of-truth contracts.

This test module MUST NOT use `build_legacy_exception_registry.LEGACY_EXCEPTION_SEEDS`
or `build_legacy_exception_registry.build_registry(sites)` to generate route,
preference, hook-target or call-site expected values.  Build tool helpers may only
be used for canonical/mutation checks, source scanning, validation, and stable ID
formatting.
"""

from __future__ import annotations

import copy
import json
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


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def load_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def read_source(rel: str) -> str:
    return (SOURCE_ROOT / rel).read_text(encoding="utf-8")


def find_function_body(text: str, func_name: str, language: str = "java") -> str | None:
    if language == "kt":
        # Match a Kotlin `fun` declaration.  Avoid calls like `GlobalActions.setupForegroundMonitor(...)`.
        pattern = re.compile(rf"\bfun\s+{re.escape(func_name)}\s*\(")
    else:
        # Match a Java method definition.  Avoid calls like `if (needGlobalActions())`.
        pattern = re.compile(rf"(?<![(])\b{re.escape(func_name)}\s*\(")
    # Pick the first match that is immediately followed by a balanced `{` on the same line.
    for match in pattern.finditer(text):
        brace = text.find("{", match.end())
        if brace == -1:
            continue
        # The next `{` should not cross another function definition; a declaration line
        # has `) {` before any statement separator.  Tolerate whitespace/newlines.
        snippet = text[match.end() : brace]
        if ")" in snippet and ";" not in snippet:
            return _balanced_body(text, brace)
    return None


def _balanced_body(text: str, start: int) -> str | None:
    i = start
    depth = 0
    in_string: str | None = None
    in_line_comment = False
    in_block_comment = False
    n = len(text)
    while i < n:
        ch = text[i]
        if in_line_comment:
            if ch == "\n":
                in_line_comment = False
            i += 1
            continue
        if in_block_comment:
            if ch == "*" and i + 1 < n and text[i + 1] == "/":
                in_block_comment = False
                i += 2
                continue
            i += 1
            continue
        if in_string:
            if ch == "\\" and i + 1 < n:
                i += 2
                continue
            if ch == in_string:
                in_string = None
            i += 1
            continue
        if ch == "/" and i + 1 < n:
            if text[i + 1] == "/":
                in_line_comment = True
                i += 2
                continue
            if text[i + 1] == "*":
                in_block_comment = True
                i += 2
                continue
        if ch == '"':
            in_string = '"'
        elif ch == "'":
            in_string = "'"
        elif ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return text[start : i + 1]
        i += 1
    return None


def find_if_block(body: str, cond_regex: str) -> str | None:
    pattern = re.compile(rf"if\s*\(\s*{cond_regex}\s*\)\s*{{")
    match = pattern.search(body)
    if not match:
        return None
    brace = body.find("{", match.start())
    return _balanced_body(body, brace)


def get_need_global_actions_body() -> str:
    text = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
    body = find_function_body(text, "needGlobalActions", "java")
    assert body is not None, "needGlobalActions body must be extractable"
    return body


def get_setup_foreground_monitor_body() -> str:
    text = read_source("tv/withaibuild/customiuizer/mods/GlobalActions.kt")
    body = find_function_body(text, "setupForegroundMonitor", "kt")
    assert body is not None, "setupForegroundMonitor body must be extractable"
    return body


def get_installer_condition(installer_rel: str, call: str) -> str | None:
    text = read_source(installer_rel)
    call_match = re.search(re.escape(call), text)
    assert call_match is not None, f"{call} not found in {installer_rel}"
    call_start = call_match.start()

    # Find the nearest `if (` before the call.
    last: re.Match | None = None
    for m in re.finditer(r"if\s*\(", text[:call_start]):
        last = m
    if last is None:
        return "UNCONDITIONAL"

    cond_start = last.end()
    paren_depth = 1
    i = cond_start
    while i < call_start and paren_depth > 0:
        c = text[i]
        if c == "(":
            paren_depth += 1
        elif c == ")":
            paren_depth -= 1
            if paren_depth == 0:
                gap = text[i + 1 : call_start]
                if "\n" in gap:
                    gap = gap.replace("\n", " ")
                if gap.strip() in ("", "{"):
                    cond = text[cond_start:i].strip()
                    cond = re.sub(r"\s+", " ", cond)
                    return cond
        i += 1
    return "UNCONDITIONAL"


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

    def test_05_schema_version_is_three(self) -> None:
        self.assertEqual(self.registry["schemaVersion"], 3)

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
        cond = get_installer_condition(
            "tv/withaibuild/customiuizer/installers/SystemServerInstaller.java",
            "GlobalActions.setupGlobalActions(lpparam);",
        )
        self.assertEqual(cond, "needGlobalActions()")

    def test_11_setup_global_actions_source_predicates(self) -> None:
        body = get_need_global_actions_body()
        self.assertIn('key.endsWith("_action")', body)
        self.assertIn("value instanceof Integer", body)
        self.assertRegex(body, r"\(Integer\)\s*value\s*>\s*1")
        self.assertIn("controls_volumemedia_up", body)
        self.assertIn("controls_volumemedia_down", body)
        self.assertIn("controls_mediaplayer_apps", body)
        self.assertRegex(body, r"!\s*MainModule\.mPrefs\.getStringSet\s*\(\s*\"controls_mediaplayer_apps\"\s*\)\.isEmpty\s*\(\s*\)")

    def test_12_setup_global_actions_preference_keys_exact(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupGlobalActions")
        self.assertEqual(
            sorted(rec["preferenceKeys"]),
            ["controls_mediaplayer_apps", "controls_volumemedia_down", "controls_volumemedia_up"],
        )

    def test_13_setup_global_actions_no_enumerated_action_keys(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupGlobalActions")
        for pk in rec["preferenceKeys"]:
            self.assertFalse(pk.endswith("_action"), f"{pk} must not be enumerated in preferenceKeys")

    def test_14_setup_global_actions_excluded_keys_absent(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupGlobalActions")
        forbidden = {
            "system_cc_custom_clock_action",
            "launcher_swipedown2_action",
            "launcher_swipeup2_action",
            "controls_backlong_action",
        }
        self.assertTrue(forbidden.isdisjoint(rec["preferenceKeys"]), "forbidden _action keys must not appear")

    def test_15_setup_global_actions_activation_contract(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupGlobalActions")
        ac = rec["activationContract"]
        self.assertEqual(ac["mode"], "ANY_OF")
        kinds = {p["kind"] for p in ac["predicates"]}
        self.assertEqual(kinds, {"DYNAMIC_SUFFIX_INT_GT", "FIXED_INT_ANY_GT_AND_NONEMPTY_SET"})
        dynamic = next(p for p in ac["predicates"] if p["kind"] == "DYNAMIC_SUFFIX_INT_GT")
        self.assertEqual(dynamic["keySuffix"], "_action")
        self.assertEqual(dynamic["thresholdExclusive"], 1)
        self.assertEqual(dynamic["valueType"], "INTEGER")
        fixed = next(p for p in ac["predicates"] if p["kind"] == "FIXED_INT_ANY_GT_AND_NONEMPTY_SET")
        self.assertEqual(sorted(fixed["integerKeys"]), ["controls_volumemedia_down", "controls_volumemedia_up"])
        self.assertEqual(fixed["thresholdExclusive"], 0)
        self.assertEqual(fixed["requiredNonEmptySetKey"], "controls_mediaplayer_apps")

    def test_16_setup_global_actions_reason_distinguishes_value_type(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupGlobalActions")
        self.assertIn("Integer", rec["reason"])
        self.assertIn("_action", rec["reason"])
        self.assertIn("activationContract", rec["reason"])

    # --- setupStatusBar ---

    def test_17_setup_status_bar_covered_call_matches_source(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupStatusBar")
        expected = set(call_ids_in_function("tv/withaibuild/customiuizer/mods/GlobalActions.kt", "setupStatusBar"))
        self.assertEqual(set(rec["coveredCallSites"]), expected)

    def test_18_setup_status_bar_unconditional(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupStatusBar")
        cond = get_installer_condition(
            "tv/withaibuild/customiuizer/installers/SystemUiInstaller.java",
            "GlobalActions.setupStatusBar(lpparam);",
        )
        self.assertEqual(cond, "UNCONDITIONAL")
        self.assertEqual(rec["activationContract"], {"mode": "UNCONDITIONAL"})
        self.assertEqual(rec["preferenceKeys"], [])

    # --- setupForegroundMonitor ---

    def test_19_setup_foreground_monitor_covered_calls_match_source(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupForegroundMonitor")
        expected = set(call_ids_in_function("tv/withaibuild/customiuizer/mods/GlobalActions.kt", "setupForegroundMonitor"))
        self.assertEqual(set(rec["coveredCallSites"]), expected)

    def test_20_setup_foreground_monitor_installer_condition(self) -> None:
        cond = get_installer_condition(
            "tv/withaibuild/customiuizer/installers/SystemUiInstaller.java",
            "GlobalActions.setupForegroundMonitor(lpparam);",
        )
        self.assertIn("various_showcallui", cond)
        self.assertIn("controls_volumecursor", cond)
        self.assertIn("||", cond)

    def test_21_setup_foreground_monitor_third_call_in_show_call_ui_block(self) -> None:
        body = get_setup_foreground_monitor_body()
        block = find_if_block(
            body,
            r"MainModule\.mPrefs\.getStringAsInt\s*\(\s*\"various_showcallui\"\s*,\s*0\s*\)\s*>\s*0",
        )
        self.assertIsNotNone(block, "show-call-ui if block must be extractable")
        self.assertIn("StatusBarStateControllerImpl", block)
        self.assertIn("setSystemBarAttributes", block)

    def test_22_setup_foreground_monitor_activation_contract(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupForegroundMonitor")
        ac = rec["activationContract"]
        self.assertEqual(ac["mode"], "ANY_OF")
        kinds = {p["kind"] for p in ac["predicates"]}
        self.assertEqual(kinds, {"BOOLEAN_KEY_TRUE", "INT_KEY_GT"})

    def test_23_setup_foreground_monitor_call_site_condition(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupForegroundMonitor")
        csc = rec["callSiteConditions"]
        third = "tv/withaibuild/customiuizer/mods/GlobalActions.kt:759:setupForegroundMonitor"
        self.assertIn(third, csc)
        cond = csc[third]
        self.assertEqual(cond, {"kind": "INT_KEY_GT", "key": "various_showcallui", "thresholdExclusive": 0})

    def test_24_setup_foreground_monitor_reason_mentions_call_site_condition(self) -> None:
        rec = record_by_owner(self.registry, "GlobalActions.setupForegroundMonitor")
        self.assertIn("various_showcallui", rec["reason"])
        self.assertIn("third hook", rec["reason"].lower())

    # --- AlarmCompat ---

    def test_25_alarm_compat_covered_calls_match_source(self) -> None:
        rec = record_by_owner(self.registry, "AlarmCompatServiceHook")
        expected = set(call_ids_in_function("tv/withaibuild/customiuizer/mods/Various.kt", "AlarmCompatServiceHook"))
        self.assertEqual(set(rec["coveredCallSites"]), expected)

    def test_26_alarm_compat_installer_condition(self) -> None:
        cond = get_installer_condition(
            "tv/withaibuild/customiuizer/installers/SystemServerInstaller.java",
            "Various.AlarmCompatServiceHook(lpparam);",
        )
        self.assertIn("various_alarmcompat", cond)

    def test_27_alarm_compat_activation_and_config_keys(self) -> None:
        rec = record_by_owner(self.registry, "AlarmCompatServiceHook")
        ac = rec["activationContract"]
        self.assertEqual(ac["mode"], "ANY_OF")
        pred = ac["predicates"][0]
        self.assertEqual(pred["kind"], "BOOLEAN_KEY_TRUE")
        self.assertEqual(pred["key"], "various_alarmcompat")
        self.assertIn("various_alarmcompat", rec["preferenceKeys"])
        self.assertIn("various_alarmcompat_apps", rec["preferenceKeys"])
        self.assertIn("runtime allowlist", rec["reason"])
        self.assertIn("not the installer activation gate", rec["reason"])


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

    def test_30_unknown_activation_mode(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"].update({"mode": "ALL_OF"}),
        )
        self._assert_validation_fails(reg, "unknown activationContract mode must fail")

    def test_31_unknown_predicate_kind(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"].append({"kind": "STRING_CONTAINS", "key": "x"}),
        )
        self._assert_validation_fails(reg, "unknown predicate kind must fail")

    def test_32_predicate_missing_required_field(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][0].__delitem__("thresholdExclusive"),
        )
        self._assert_validation_fails(reg, "predicate missing required field must fail")

    def test_33_dynamic_suffix_empty(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][0].update({"keySuffix": ""}),
        )
        self._assert_validation_fails(reg, "empty keySuffix must fail")

    def test_34_dynamic_suffix_missing_value_type(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][0].pop("valueType"),
        )
        self._assert_validation_fails(reg, "missing valueType must fail")

    def test_35_fixed_int_empty_integer_keys(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][1].update({"integerKeys": []}),
        )
        self._assert_validation_fails(reg, "empty integerKeys must fail")

    def test_36_int_gt_non_integer_threshold(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["activationContract"]["predicates"][1].update({"thresholdExclusive": "0"}),
        )
        self._assert_validation_fails(reg, "string thresholdExclusive must fail")

    def test_37_activation_contract_in_canonical_diff(self) -> None:
        sites = builder.scan_legacy_call_sites()
        expected = builder.build_registry(sites)
        reg = copy.deepcopy(self.registry)
        rec = record_by_owner(reg, "GlobalActions.setupGlobalActions")
        rec["activationContract"]["mode"] = "UNCONDITIONAL"
        diffs = builder.canonical_diff(expected, reg)
        self.assertTrue(any("activationContract" in d or "mode" in d for d in diffs))

    def test_38_call_site_conditions_in_canonical_diff(self) -> None:
        sites = builder.scan_legacy_call_sites()
        expected = builder.build_registry(sites)
        reg = copy.deepcopy(self.registry)
        rec = record_by_owner(reg, "GlobalActions.setupForegroundMonitor")
        rec["callSiteConditions"] = {}
        diffs = builder.canonical_diff(expected, reg)
        self.assertTrue(any("callSiteConditions" in d for d in diffs))


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

    def test_40_call_site_condition_for_non_covered_call(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:999:setupForegroundMonitor": {
                    "kind": "INT_KEY_GT", "key": "various_showcallui", "thresholdExclusive": 0,
                }
            }),
        )
        self._assert_validation_fails(reg, "condition for non-covered call must fail")

    def test_41_call_site_condition_unknown_key(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:759:setupForegroundMonitor": {
                    "kind": "INT_KEY_GT", "key": "unknown_key", "thresholdExclusive": 0,
                }
            }),
        )
        self._assert_validation_fails(reg, "condition key not in preferenceKeys must fail")

    def test_42_call_site_condition_missing_field(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:759:setupForegroundMonitor": {
                    "kind": "INT_KEY_GT",
                }
            }),
        )
        self._assert_validation_fails(reg, "condition missing required field must fail")

    def test_43_call_site_condition_unknown_kind(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:759:setupForegroundMonitor": {
                    "kind": "FLOAT_GT", "key": "various_showcallui", "thresholdExclusive": 0,
                }
            }),
        )
        self._assert_validation_fails(reg, "unknown condition kind must fail")


# ---------------------------------------------------------------------------
# Mutation tests
# ---------------------------------------------------------------------------


class P3_3B_ActivationContractMutationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.registry = load_json(REGISTRY_FILE)
        self.sites = builder.scan_legacy_call_sites()
        self.expected = builder.build_registry(self.sites)

    def _mutate(self, owner: str, mutate: callable) -> dict:
        reg = copy.deepcopy(self.registry)
        rec = record_by_owner(reg, owner)
        mutate(rec)
        return reg

    def _assert_killed(self, reg: dict, message: str) -> None:
        errors = builder.validate(reg)
        if errors:
            return
        diffs = builder.canonical_diff(self.expected, reg)
        self.assertTrue(diffs, message)

    def test_50_remove_dynamic_activation_contract(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r.pop("activationContract"),
        )
        self._assert_killed(reg, "remove activationContract must fail canonical/validation")

    def test_51_change_dynamic_suffix(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][0].update({"keySuffix": "_click"}),
        )
        self._assert_killed(reg, "changed keySuffix must fail canonical")

    def test_52_remove_integer_type_guard(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][0].pop("valueType"),
        )
        self._assert_killed(reg, "removed valueType must fail validation")

    def test_53_change_action_threshold(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][0].update({"thresholdExclusive": 2}),
        )
        self._assert_killed(reg, "changed action threshold must fail canonical")

    def test_54_remove_media_player_apps(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][1].pop("requiredNonEmptySetKey"),
        )
        self._assert_killed(reg, "removed requiredNonEmptySetKey must fail validation")

    def test_55_change_media_or_to_and(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"].update({"mode": "ALL_OF"}),
        )
        self._assert_killed(reg, "invalid mode must fail validation")

    def test_56_remove_media_up_key(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][1]["integerKeys"].remove("controls_volumemedia_up"),
        )
        self._assert_killed(reg, "removed media up key must fail canonical")

    def test_57_remove_media_down_key(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["activationContract"]["predicates"][1]["integerKeys"].remove("controls_volumemedia_down"),
        )
        self._assert_killed(reg, "removed media down key must fail canonical")

    def test_58_add_static_action_enumeration(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["preferenceKeys"].append("controls_backlong_action"),
        )
        self._assert_killed(reg, "enumerated _action key in preferenceKeys must fail validation")

    def test_59_add_boolean_action_key(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupGlobalActions",
            lambda r: r["preferenceKeys"].append("system_cc_custom_clock_action"),
        )
        self._assert_killed(reg, "boolean _action key in preferenceKeys must fail validation")

    def test_60_remove_foreground_call_condition(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r.update({"callSiteConditions": {}}),
        )
        self._assert_killed(reg, "removed callSiteConditions must fail canonical")

    def test_61_change_foreground_call_condition_key(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:759:setupForegroundMonitor": {
                    "kind": "BOOLEAN_KEY_TRUE", "key": "controls_volumecursor",
                }
            }),
        )
        self._assert_killed(reg, "changed foreground call condition to wrong key must fail canonical")

    def test_62_attach_condition_to_wrong_call(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:743:setupForegroundMonitor": {
                    "kind": "INT_KEY_GT", "key": "various_showcallui", "thresholdExclusive": 0,
                }
            }),
        )
        self._assert_killed(reg, "condition attached to wrong call must fail canonical")

    def test_63_fabricated_call_condition_id(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r["callSiteConditions"].update({
                "tv/withaibuild/customiuizer/mods/GlobalActions.kt:999:fake": {
                    "kind": "INT_KEY_GT", "key": "various_showcallui", "thresholdExclusive": 0,
                }
            }),
        )
        self._assert_killed(reg, "fabricated call condition id must fail validation")

    def test_64_remove_unconditional_statusbar_activation(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupStatusBar",
            lambda r: r.pop("activationContract"),
        )
        self._assert_killed(reg, "removed statusbar activationContract must fail canonical")

    def test_65_change_alarmcompat_activation_key(self) -> None:
        reg = self._mutate(
            "AlarmCompatServiceHook",
            lambda r: r["activationContract"]["predicates"][0].update({"key": "various_alarmcompat_apps"}),
        )
        self._assert_killed(reg, "changed alarmcompat activation to config key must fail canonical")

    def test_66_stale_activation_contract_schema_version(self) -> None:
        reg = copy.deepcopy(self.registry)
        reg["schemaVersion"] = 2
        self._assert_killed(reg, "stale schemaVersion must fail canonical")

    def test_67_stale_call_site_conditions(self) -> None:
        reg = self._mutate(
            "GlobalActions.setupForegroundMonitor",
            lambda r: r.pop("callSiteConditions"),
        )
        self._assert_killed(reg, "removed callSiteConditions must fail canonical")

    def test_68_circular_expected_from_seed_detection(self) -> None:
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
        # Removing activationContract removes the dynamic suffix guard, so validation alone
        # would not fail.  Canonical diff against the golden registry must fail.
        diffs = builder.canonical_diff(self.expected, reg)
        self.assertTrue(diffs, "old seed-enumerated _action keys must not be accepted as expected")


if __name__ == "__main__":
    unittest.main()
