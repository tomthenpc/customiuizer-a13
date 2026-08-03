#!/usr/bin/env python3
"""Focused tests for A13-P3.3B GlobalActions and AlarmCompat LEGACY_EXCEPTION registration.

Covers:
- P3.3A record canonical preservation
- P3.3B installer route evidence
- P3.3B process/phase/preference/hookTargets correctness
- Multi-batch schema invariants (batch, batchCounts, registeredRecordCount)
- P3.3B mutation cases
- Whole-file and all-legacy gates remain active
"""

from __future__ import annotations

import copy
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


def load_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def dump_json(obj: dict, path: Path) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


class P3_3B_LegacyExceptionRouteTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.registry = load_json(REGISTRY_FILE)
        cls.records = {r["id"]: r for r in cls.registry.get("records", [])}
        import build_legacy_exception_registry as builder
        cls.builder = builder

    # ---- P3.3A preservation ----

    def test_01_p3_3a_records_preserved(self) -> None:
        p3_3a = [r for r in self.registry["records"] if r["batch"] == "P3.3A"]
        self.assertEqual(len(p3_3a), 4, "P3.3A must still have 4 records")

    def test_02_p3_3a_batch_count_is_four(self) -> None:
        counts = self.registry.get("batchCounts", {})
        self.assertEqual(counts.get("P3.3A"), 4, "batchCounts.P3.3A must be 4")

    def test_03_p3_3a_covered_calls_still_eleven(self) -> None:
        p3_3a = [r for r in self.registry["records"] if r["batch"] == "P3.3A"]
        total = sum(len(r["coveredCallSites"]) for r in p3_3a)
        self.assertEqual(total, 11, "P3.3A records must still cover exactly 11 call sites")

    def test_04_p3_3a_fields_unchanged(self) -> None:
        # P3.3A record owners and entrypoints must match the original approved set.
        p3_3a = {r["owner"]: r["entrypoint"] for r in self.registry["records"] if r["batch"] == "P3.3A"}
        expected = {
            "MIUIVolumeDialogHook": "MIUIVolumeDialogHook",
            "NotificationVolumeSettingsHook": "NotificationVolumeSettingsHook",
            "USBConfigHook": "USBConfigHook",
            "USBConfigSettingsHook": "USBConfigSettingsHook",
        }
        self.assertEqual(p3_3a, expected, "P3.3A record owner/entrypoint mapping must not drift")

    # ---- P3.3B record presence ----

    def test_05_p3_3b_records_present(self) -> None:
        p3_3b = [r for r in self.registry["records"] if r["batch"] == "P3.3B"]
        self.assertEqual(len(p3_3b), 4, "P3.3B must add 4 records")

    def test_06_p3_3b_expected_owners(self) -> None:
        owners = {r["owner"] for r in self.registry["records"] if r["batch"] == "P3.3B"}
        expected = {
            "GlobalActions.setupGlobalActions",
            "GlobalActions.setupStatusBar",
            "GlobalActions.setupForegroundMonitor",
            "AlarmCompatServiceHook",
        }
        self.assertEqual(owners, expected, "P3.3B must cover the four approved owners")

    def test_07_p3_3b_covered_call_count(self) -> None:
        p3_3b = [r for r in self.registry["records"] if r["batch"] == "P3.3B"]
        total = sum(len(r["coveredCallSites"]) for r in p3_3b)
        self.assertEqual(total, 8, "P3.3B must cover exactly 8 call sites")

    # ---- batch schema invariants ----

    def test_08_schema_version_is_two(self) -> None:
        self.assertEqual(self.registry.get("schemaVersion"), 2, "Schema must be version 2")

    def test_09_first_batch_size_unchanged(self) -> None:
        self.assertEqual(self.registry.get("firstBatchSize"), 4, "firstBatchSize must remain 4")

    def test_10_registered_record_count_matches_records(self) -> None:
        self.assertEqual(
            self.registry.get("registeredRecordCount"),
            len(self.registry["records"]),
            "registeredRecordCount must equal len(records)",
        )

    def test_11_batch_counts_match_records(self) -> None:
        from collections import Counter
        actual = Counter(r["batch"] for r in self.registry["records"])
        expected = dict(actual)
        self.assertEqual(self.registry.get("batchCounts"), expected, "batchCounts must be derived from records")

    def test_12_all_records_have_valid_batch(self) -> None:
        for rec in self.registry["records"]:
            self.assertIn(rec.get("batch"), ("P3.3A", "P3.3B"), f"Record {rec['id']} has invalid batch")

    # ---- installer route and preference evidence ----

    def test_13_globalactions_systemserver_installer_route(self) -> None:
        rec = self._p33b_record_by_owner("GlobalActions.setupGlobalActions")
        installer = (REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "installers" / "SystemServerInstaller.java").read_text(encoding="utf-8")
        self.assertIn("GlobalActions.setupGlobalActions(lpparam)", installer)
        self.assertIn("needGlobalActions()", installer)
        self.assertIn("_action", installer)
        self.assertEqual(rec["process"], "system_server")
        self.assertEqual(rec["phase"], "SYSTEM_SERVER_STARTING")

    def test_14_globalactions_statusbar_installer_route(self) -> None:
        rec = self._p33b_record_by_owner("GlobalActions.setupStatusBar")
        installer = (REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "installers" / "SystemUiInstaller.java").read_text(encoding="utf-8")
        self.assertIn("GlobalActions.setupStatusBar(lpparam)", installer)
        self.assertEqual(rec["process"], "system_ui")
        self.assertEqual(rec["phase"], "PACKAGE_READY")

    def test_15_globalactions_foreground_installer_route(self) -> None:
        rec = self._p33b_record_by_owner("GlobalActions.setupForegroundMonitor")
        installer = (REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "installers" / "SystemUiInstaller.java").read_text(encoding="utf-8")
        self.assertIn("GlobalActions.setupForegroundMonitor(lpparam)", installer)
        self.assertIn("various_showcallui", installer)
        self.assertEqual(rec["process"], "system_ui")
        self.assertEqual(rec["phase"], "PACKAGE_READY")

    def test_16_alarmcompat_installer_route(self) -> None:
        rec = self._p33b_record_by_owner("AlarmCompatServiceHook")
        installer = (REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "installers" / "SystemServerInstaller.java").read_text(encoding="utf-8")
        self.assertIn("Various.AlarmCompatServiceHook(lpparam)", installer)
        self.assertIn("various_alarmcompat", installer)
        self.assertEqual(rec["process"], "system_server")
        self.assertEqual(rec["phase"], "SYSTEM_SERVER_STARTING")

    def test_17_globalactions_systemserver_preference_condition(self) -> None:
        rec = self._p33b_record_by_owner("GlobalActions.setupGlobalActions")
        self.assertTrue(rec["preferenceKeys"], "setupGlobalActions must list action preference keys")
        for key in rec["preferenceKeys"]:
            self.assertTrue(
                key.endswith("_action") or key in ("controls_volumemedia_up", "controls_volumemedia_down"),
                f"Unexpected preference key {key!r}",
            )

    def test_18_globalactions_foreground_preference_condition(self) -> None:
        rec = self._p33b_record_by_owner("GlobalActions.setupForegroundMonitor")
        self.assertIn("various_showcallui", rec["preferenceKeys"])
        self.assertIn("controls_volumecursor", rec["preferenceKeys"])

    def test_19_alarmcompat_preference_condition(self) -> None:
        rec = self._p33b_record_by_owner("AlarmCompatServiceHook")
        self.assertIn("various_alarmcompat", rec["preferenceKeys"])
        self.assertIn("various_alarmcompat_apps", rec["preferenceKeys"])

    # ---- hook targets ----

    def test_20_globalactions_systemserver_hook_targets(self) -> None:
        rec = self._p33b_record_by_owner("GlobalActions.setupGlobalActions")
        self.assertEqual(
            set(rec["hookTargets"]),
            {
                "com.android.server.accessibility.AccessibilityManagerService#<init>",
                "com.android.server.policy.BaseMiuiPhoneWindowManager#initInternal",
            },
        )

    def test_21_globalactions_statusbar_hook_targets(self) -> None:
        rec = self._p33b_record_by_owner("GlobalActions.setupStatusBar")
        self.assertEqual(
            rec["hookTargets"],
            ["com.android.systemui.statusbar.phone.CentralSurfacesImpl#start"],
        )

    def test_22_globalactions_foreground_hook_targets(self) -> None:
        rec = self._p33b_record_by_owner("GlobalActions.setupForegroundMonitor")
        self.assertEqual(
            set(rec["hookTargets"]),
            {
                "com.android.systemui.statusbar.policy.NetworkSpeedController#<init>",
                "com.miui.systemui.util.MiuiActivityUtil#updateTopActivity",
                "com.android.systemui.statusbar.StatusBarStateControllerImpl#setSystemBarAttributes",
            },
        )

    def test_23_alarmcompat_hook_targets(self) -> None:
        rec = self._p33b_record_by_owner("AlarmCompatServiceHook")
        self.assertEqual(
            set(rec["hookTargets"]),
            {
                "com.android.server.alarm.AlarmManagerService#onBootPhase",
                "com.android.server.alarm.AlarmManagerService#getNextAlarmClockImpl",
            },
        )

    # ---- source evidence ----

    def test_24_entrypoints_exist_in_source(self) -> None:
        for rec in self.registry["records"]:
            path = REPO_ROOT / "app" / "src" / "main" / "java" / rec["sourceFile"]
            text = path.read_text(encoding="utf-8")
            self.assertIn(rec["entrypoint"], text, f"entrypoint {rec['entrypoint']} not in {rec['sourceFile']}")

    def test_25_globalactions_has_multiple_legacy_functions(self) -> None:
        # Whole-file/whole-function gate only matters when there is more than one legacy function.
        sites = self.builder.scan_legacy_call_sites()
        file_funcs = {s["function"] for s in sites if s["rel"] == "tv/withaibuild/customiuizer/mods/GlobalActions.kt" and s["category"] == "LEGACY_EXCEPTION"}
        self.assertGreater(len(file_funcs), 1, "GlobalActions.kt must have more than one legacy function")

    # ---- helpers ----

    def _p33b_record_by_owner(self, owner: str) -> dict:
        for rec in self.registry["records"]:
            if rec["batch"] == "P3.3B" and rec["owner"] == owner:
                return rec
        raise AssertionError(f"P3.3B record not found for owner {owner}")


class P3_3B_RegistryMutationTest(unittest.TestCase):
    """Mutation tests using a temporary copy of the committed registry."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.registry = load_json(REGISTRY_FILE)
        import build_legacy_exception_registry as builder
        cls.builder = builder
        cls.sites = cls.builder.scan_legacy_call_sites()
        cls.expected = cls.builder.build_registry(cls.sites)

    def _diff_after(self, mutator) -> list[str]:
        reg = copy.deepcopy(self.registry)
        mutator(reg)
        return self.builder.canonical_diff(self.expected, reg)

    def _validate_after(self, mutator) -> list[str]:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            mutated_file = tmp_path / "registry.json"
            reg = copy.deepcopy(self.registry)
            mutator(reg)
            dump_json(reg, mutated_file)
            return self.builder.validate(load_json(mutated_file))

    # ---- batch invariants ----

    def test_30_unknown_batch_fails(self) -> None:
        def mutate(reg):
            reg["records"][0]["batch"] = "P3.3C"
        errors = self._validate_after(mutate)
        self.assertTrue(any("batch" in e.lower() for e in errors), "Unknown batch must fail")

    def test_31_missing_batch_fails(self) -> None:
        def mutate(reg):
            del reg["records"][0]["batch"]
        errors = self._validate_after(mutate)
        self.assertTrue(any("missing field 'batch'" in e.lower() for e in errors), "Missing batch must fail")

    def test_32_stale_batch_counts_fails(self) -> None:
        def mutate(reg):
            reg["batchCounts"]["P3.3B"] = 99
        diffs = self._diff_after(mutate)
        self.assertTrue(any("batchCounts" in d for d in diffs), "Stale batchCounts must fail canonical check")

    def test_33_stale_registered_record_count_fails(self) -> None:
        def mutate(reg):
            reg["registeredRecordCount"] = 99
        diffs = self._diff_after(mutate)
        self.assertTrue(any("registeredRecordCount" in d for d in diffs), "Stale registeredRecordCount must fail")

    def test_34_remove_p33b_seed_fails(self) -> None:
        def mutate(reg):
            reg["records"] = [r for r in reg["records"] if r["owner"] != "AlarmCompatServiceHook"]
            reg["registeredRecordCount"] = len(reg["records"])
            reg["batchCounts"]["P3.3B"] -= 1
        diffs = self._diff_after(mutate)
        self.assertTrue(diffs, "Removing a P3.3B record must make --check fail")

    def test_35_change_p33b_batch_fails(self) -> None:
        def mutate(reg):
            for rec in reg["records"]:
                if rec["owner"] == "AlarmCompatServiceHook":
                    rec["batch"] = "P3.3A"
        diffs = self._diff_after(mutate)
        self.assertTrue(any("batch" in d for d in diffs), "Changing a P3.3B record's batch must fail")

    # ---- route/owner invariants ----

    def test_36_merge_globalactions_routes_fails(self) -> None:
        # Single record covering all GlobalActions legacy call sites.
        sites = self.builder.scan_legacy_call_sites()
        file_rel = "tv/withaibuild/customiuizer/mods/GlobalActions.kt"
        all_calls = sorted(
            self.builder._stable_call_id(s["rel"], s["line"], s["function"])
            for s in sites
            if s["category"] == "LEGACY_EXCEPTION" and s["rel"] == file_rel
        )
        reg = {
            "records": [
                {
                    "id": "test-whole-globalactions",
                    "batch": "P3.3B",
                    "status": "ACTIVE",
                    "owner": "GlobalActionsWholeFile",
                    "sourceFile": file_rel,
                    "entrypoint": "setupGlobalActions",
                    "process": "system_server",
                    "phase": "SYSTEM_SERVER_STARTING",
                    "preferenceKeys": [],
                    "reasonCode": "CROSS_PROCESS",
                    "reason": "test whole GlobalActions.kt record",
                    "coveredCallSites": all_calls,
                    "hookTargets": ["com.android.server.accessibility.AccessibilityManagerService#<init>"],
                    "testEvidence": ["tools/tests/test_p33b_legacy_exception_routes.py"],
                    "exitCondition": "test",
                }
            ]
        }
        errors = self.builder.validate(reg)
        self.assertTrue(
            any("WHOLE_FILE_LEGACY_EXCEPTION_FORBIDDEN" in e for e in errors),
            f"Whole-file GlobalActions record must fail: {errors}",
        )

    def test_37_whole_file_various_fails(self) -> None:
        sites = self.builder.scan_legacy_call_sites()
        file_rel = "tv/withaibuild/customiuizer/mods/Various.kt"
        all_calls = sorted(
            self.builder._stable_call_id(s["rel"], s["line"], s["function"])
            for s in sites
            if s["category"] == "LEGACY_EXCEPTION" and s["rel"] == file_rel
        )
        reg = {
            "records": [
                {
                    "id": "test-whole-various",
                    "batch": "P3.3B",
                    "status": "ACTIVE",
                    "owner": "VariousWholeFile",
                    "sourceFile": file_rel,
                    "entrypoint": "AlarmCompatServiceHook",
                    "process": "system_server",
                    "phase": "SYSTEM_SERVER_STARTING",
                    "preferenceKeys": ["various_alarmcompat"],
                    "reasonCode": "LIFECYCLE_BOOTSTRAP",
                    "reason": "test whole Various.kt record",
                    "coveredCallSites": all_calls,
                    "hookTargets": ["com.android.server.alarm.AlarmManagerService#onBootPhase"],
                    "testEvidence": ["tools/tests/test_p33b_legacy_exception_routes.py"],
                    "exitCondition": "test",
                }
            ]
        }
        errors = self.builder.validate(reg)
        self.assertTrue(
            any("WHOLE_FILE_LEGACY_EXCEPTION_FORBIDDEN" in e for e in errors),
            f"Whole-file Various record must fail: {errors}",
        )

    def test_38_systemui_call_in_system_server_record_fails(self) -> None:
        # Move a setupForegroundMonitor call into setupGlobalActions record.
        def mutate(reg):
            global_rec = next(r for r in reg["records"] if r["owner"] == "GlobalActions.setupGlobalActions")
            foreground_rec = next(r for r in reg["records"] if r["owner"] == "GlobalActions.setupForegroundMonitor")
            global_rec["coveredCallSites"].append(foreground_rec["coveredCallSites"][0])
        errors = self._validate_after(mutate)
        self.assertTrue(
            any("more than one" in e.lower() for e in errors),
            "SystemUI call in system_server record must fail",
        )

    def test_39_system_server_call_in_systemui_record_fails(self) -> None:
        def mutate(reg):
            global_rec = next(r for r in reg["records"] if r["owner"] == "GlobalActions.setupGlobalActions")
            status_rec = next(r for r in reg["records"] if r["owner"] == "GlobalActions.setupStatusBar")
            status_rec["coveredCallSites"].append(global_rec["coveredCallSites"][0])
        errors = self._validate_after(mutate)
        self.assertTrue(
            any("more than one" in e.lower() for e in errors),
            "system_server call in SystemUI record must fail",
        )

    def test_40_duplicate_globalactions_call_owner_fails(self) -> None:
        # Put the same setupGlobalActions call on both setupGlobalActions and setupStatusBar records.
        def mutate(reg):
            global_rec = next(r for r in reg["records"] if r["owner"] == "GlobalActions.setupGlobalActions")
            status_rec = next(r for r in reg["records"] if r["owner"] == "GlobalActions.setupStatusBar")
            status_rec["coveredCallSites"].append(global_rec["coveredCallSites"][0])
        errors = self._validate_after(mutate)
        self.assertTrue(
            any("more than one" in e.lower() for e in errors),
            "Duplicate call owner must fail",
        )

    def test_41_include_globalactions_unrelated_helper_fails(self) -> None:
        # Add a call from miuizerSettingsHook (unrelated GlobalActions legacy function) to setupGlobalActions record.
        sites = self.builder.scan_legacy_call_sites()
        helper_call = next(
            self.builder._stable_call_id(s["rel"], s["line"], s["function"])
            for s in sites
            if s["rel"] == "tv/withaibuild/customiuizer/mods/GlobalActions.kt"
            and s["category"] == "LEGACY_EXCEPTION"
            and s["function"] == "miuizerSettingsHook"
        )

        def mutate(reg):
            global_rec = next(r for r in reg["records"] if r["owner"] == "GlobalActions.setupGlobalActions")
            global_rec["coveredCallSites"].append(helper_call)
        diffs = self._diff_after(mutate)
        self.assertTrue(diffs, "Including an unrelated helper call must make canonical check fail")

    def test_42_change_alarmcompat_preference_fails(self) -> None:
        def mutate(reg):
            for rec in reg["records"]:
                if rec["owner"] == "AlarmCompatServiceHook":
                    rec["preferenceKeys"] = ["wrong_preference"]
        diffs = self._diff_after(mutate)
        self.assertTrue(diffs, "Changing AlarmCompat preference keys must fail canonical check")

    def test_43_change_alarmcompat_phase_fails(self) -> None:
        def mutate(reg):
            for rec in reg["records"]:
                if rec["owner"] == "AlarmCompatServiceHook":
                    rec["phase"] = "PACKAGE_READY"
        diffs = self._diff_after(mutate)
        self.assertTrue(diffs, "Changing AlarmCompat phase must fail canonical check")

    def test_44_remove_alarmcompat_installer_route_fails(self) -> None:
        def mutate(reg):
            for rec in reg["records"]:
                if rec["owner"] == "AlarmCompatServiceHook":
                    rec["sourceFile"] = "tv/withaibuild/customiuizer/mods/Controls.kt"
        errors = self._validate_after(mutate)
        self.assertTrue(
            any("entrypoint" in e.lower() for e in errors),
            "AlarmCompat sourceFile without entrypoint must fail",
        )

    def test_45_empty_p33b_hook_targets_fails(self) -> None:
        def mutate(reg):
            for rec in reg["records"]:
                if rec["owner"] == "AlarmCompatServiceHook":
                    rec["hookTargets"] = []
        errors = self._validate_after(mutate)
        self.assertTrue(any("hookTargets is empty" in e for e in errors), "Empty hookTargets must fail")

    def test_46_duplicate_p33b_record_id_fails(self) -> None:
        def mutate(reg):
            p3_3b = [r for r in reg["records"] if r["batch"] == "P3.3B"]
            if len(p3_3b) > 1:
                p3_3b[0]["id"] = p3_3b[1]["id"]
        errors = self._validate_after(mutate)
        self.assertTrue(any("duplicate id" in e.lower() for e in errors), "Duplicate P3.3B id must fail")

    def test_47_p33b_process_mismatch_fails(self) -> None:
        def mutate(reg):
            for rec in reg["records"]:
                if rec["owner"] == "GlobalActions.setupGlobalActions":
                    rec["process"] = "system_ui"
        diffs = self._diff_after(mutate)
        self.assertTrue(diffs, "GlobalActions system_server record assigned system_ui must fail")

    def test_48_modify_p33a_record_id_fails(self) -> None:
        def mutate(reg):
            for rec in reg["records"]:
                if rec["batch"] == "P3.3A":
                    rec["id"] = "deadbeef00000000"
        diffs = self._diff_after(mutate)
        self.assertTrue(diffs, "Modifying a P3.3A record id must fail canonical check")

    def test_49_remove_p33a_batch_tag_fails(self) -> None:
        def mutate(reg):
            for rec in reg["records"]:
                if rec["batch"] == "P3.3A":
                    del rec["batch"]
        errors = self._validate_after(mutate)
        self.assertTrue(any("missing field 'batch'" in e.lower() for e in errors), "Removing P3.3A batch must fail")


if __name__ == "__main__":
    unittest.main()
