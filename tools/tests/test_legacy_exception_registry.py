#!/usr/bin/env python3
"""Focused tests for the A13 LEGACY_EXCEPTION registry and its validator."""

from __future__ import annotations

import copy
import json
import os
import sys
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
# Make tools/ available for direct imports in tests.
sys.path.insert(0, str(REPO_ROOT / "tools"))
REGISTRY_FILE = REPO_ROOT / "docs" / "audit" / "A13_LEGACY_EXCEPTION_REGISTRY.json"


def load_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def dump_json(obj: dict, path: Path) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


class LegacyExceptionRegistryTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.registry = load_json(REGISTRY_FILE)

    def _validate_against(self, registry_path: Path) -> list[str]:
        import build_legacy_exception_registry as builder
        registry = load_json(registry_path)
        return builder.validate(registry)

    # ---- positive tests ----

    def test_01_registry_exists(self) -> None:
        self.assertTrue(REGISTRY_FILE.is_file())

    def test_02_required_fields_present(self) -> None:
        for rec in self.registry.get("records", []):
            for field in (
                "id",
                "status",
                "owner",
                "sourceFile",
                "entrypoint",
                "process",
                "phase",
                "reasonCode",
                "reason",
                "coveredCallSites",
                "hookTargets",
                "testEvidence",
                "exitCondition",
            ):
                self.assertIn(field, rec, f"Missing field {field} in record {rec.get('id')}")

    def test_03_no_duplicate_ids(self) -> None:
        ids = [r["id"] for r in self.registry.get("records", [])]
        self.assertEqual(len(ids), len(set(ids)), "Duplicate record ids")

    def test_04_no_overlapping_call_sites(self) -> None:
        seen: set[str] = set()
        for rec in self.registry.get("records", []):
            for call in rec["coveredCallSites"]:
                self.assertNotIn(call, seen, f"Call site {call} covered by multiple records")
                seen.add(call)

    def test_05_source_files_exist(self) -> None:
        for rec in self.registry.get("records", []):
            path = REPO_ROOT / "app" / "src" / "main" / "java" / rec["sourceFile"]
            self.assertTrue(path.is_file(), f"sourceFile missing: {rec['sourceFile']}")

    def test_06_entrypoints_exist(self) -> None:
        for rec in self.registry.get("records", []):
            path = REPO_ROOT / "app" / "src" / "main" / "java" / rec["sourceFile"]
            text = path.read_text(encoding="utf-8")
            self.assertIn(rec["entrypoint"], text, f"entrypoint not in {rec['sourceFile']}")

    def test_07_taxonomy_values_known(self) -> None:
        for rec in self.registry.get("records", []):
            self.assertIn(rec["process"], ("system_server", "system_ui", "launcher", "android", "per_app", "resource", "other"))
            self.assertIn(rec["phase"], ("SYSTEM_SERVER_STARTING", "PACKAGE_READY", "RESOURCE_INIT", "BOOTSTRAP", "OTHER"))
            self.assertIn(rec["reasonCode"], (
                "CROSS_PROCESS", "LIFECYCLE_BOOTSTRAP", "RESOURCE_HOOK", "DYNAMIC_TARGET_SET",
                "SHARED_MUTABLE_STATE", "ROM_DEPENDENT_DISPATCH", "API_BRIDGE_BOUNDARY",
                "INSTALLER_INFRASTRUCTURE", "TEMPORARY_MIGRATION_DEFERRED",
                "DEAD_CODE_PENDING_OWNER_APPROVAL", "OTHER_REVIEW_REQUIRED",
            ))

    def test_08_test_evidence_non_empty(self) -> None:
        for rec in self.registry.get("records", []):
            self.assertTrue(rec.get("testEvidence"), f"testEvidence empty for {rec.get('id')}")

    def test_09_exit_condition_not_never(self) -> None:
        for rec in self.registry.get("records", []):
            self.assertNotEqual(rec["exitCondition"].lower(), "never", f"exitCondition is 'never' for {rec.get('id')}")

    def test_10_no_typed_or_infra_calls_in_registry(self) -> None:
        import build_legacy_exception_registry as builder
        sites = builder.scan_legacy_call_sites()
        typed_or_infra = {
            builder._stable_call_id(s["rel"], s["line"], s["function"])
            for s in sites
            if s["category"] in ("REGISTRY_FEATURE", "API_BRIDGE", "INSTALLER_INFRASTRUCTURE")
        }
        for rec in self.registry.get("records", []):
            for call in rec["coveredCallSites"]:
                self.assertNotIn(call, typed_or_infra, f"Invalid call in registry: {call}")

    def test_11_windows_linux_path_normalization(self) -> None:
        for rec in self.registry.get("records", []):
            normalized = rec["sourceFile"].replace("\\", "/")
            self.assertEqual(normalized, rec["sourceFile"], f"sourceFile contains backslash: {rec['sourceFile']}")

    def test_12_registry_output_order_stable(self) -> None:
        # Records are written in deterministic, sorted order. Re-parsing the
        # committed file proves the output is stable and idempotent.
        ids = [r["id"] for r in self.registry["records"]]
        self.assertEqual(ids, sorted(ids), "Record ids must be deterministically sorted")

    # ---- mutation tests ----

    def _with_mutated_registry(self, mutator) -> list[str]:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            mutated_file = tmp_path / "registry.json"
            reg = copy.deepcopy(self.registry)
            mutator(reg)
            dump_json(reg, mutated_file)
            return self._validate_against(mutated_file)

    def test_13_mutation_delete_owner(self) -> None:
        def mutate(reg):
            reg["records"][0]["owner"] = ""
        self.assertTrue(self._with_mutated_registry(mutate), "Missing owner must fail")

    def test_14_mutation_delete_exit_condition(self) -> None:
        def mutate(reg):
            reg["records"][0]["exitCondition"] = ""
        self.assertTrue(self._with_mutated_registry(mutate), "Missing exitCondition must fail")

    def test_15_mutation_delete_test_evidence(self) -> None:
        def mutate(reg):
            reg["records"][0]["testEvidence"] = []
        self.assertTrue(self._with_mutated_registry(mutate), "Empty testEvidence must fail")

    def test_16_mutation_duplicate_id(self) -> None:
        def mutate(reg):
            if len(reg["records"]) > 1:
                reg["records"][0]["id"] = reg["records"][1]["id"]
        errors = self._with_mutated_registry(mutate)
        self.assertTrue(any("duplicate id" in e.lower() for e in errors), "Duplicate id must fail")

    def test_17_mutation_call_site_two_owners(self) -> None:
        def mutate(reg):
            if len(reg["records"]) > 1:
                reg["records"][1]["coveredCallSites"].append(reg["records"][0]["coveredCallSites"][0])
        errors = self._with_mutated_registry(mutate)
        self.assertTrue(any("more than one" in e.lower() for e in errors), "Overlapping call site must fail")

    def test_18_mutation_nonexistent_source_file(self) -> None:
        def mutate(reg):
            reg["records"][0]["sourceFile"] = "tv/withaibuild/customiuizer/mods/NonExistent.kt"
        errors = self._with_mutated_registry(mutate)
        self.assertTrue(any("sourcefile" in e.lower() and "exist" in e.lower() for e in errors), "Missing sourceFile must fail")

    def test_19_mutation_nonexistent_entrypoint(self) -> None:
        def mutate(reg):
            reg["records"][0]["entrypoint"] = "nonExistentFunctionForP3A3"
        errors = self._with_mutated_registry(mutate)
        self.assertTrue(any("entrypoint" in e.lower() for e in errors), "Missing entrypoint must fail")

    def test_20_mutation_typed_owned_call_added(self) -> None:
        import build_legacy_exception_registry as builder
        sites = builder.scan_legacy_call_sites()
        typed = [s for s in sites if s["category"] == "REGISTRY_FEATURE"]
        if not typed:
            self.skipTest("no typed calls to test with")
        def mutate(reg):
            reg["records"][0]["coveredCallSites"].append(
                builder._stable_call_id(typed[0]["rel"], typed[0]["line"], typed[0]["function"])
            )
        errors = self._with_mutated_registry(mutate)
        self.assertTrue(any("typed-catalog" in e.lower() for e in errors), "Typed-owned call in registry must fail")

    def test_21_mutation_api_bridge_disguised(self) -> None:
        import build_legacy_exception_registry as builder
        sites = builder.scan_legacy_call_sites()
        infra = [s for s in sites if s["category"] in ("API_BRIDGE", "INSTALLER_INFRASTRUCTURE")]
        if not infra:
            self.skipTest("no API_BRIDGE/INSTALLER_INFRASTRUCTURE calls to test with")
        def mutate(reg):
            reg["records"][0]["coveredCallSites"].append(
                builder._stable_call_id(infra[0]["rel"], infra[0]["line"], infra[0]["function"])
            )
        errors = self._with_mutated_registry(mutate)
        self.assertTrue(any("api_bridge" in e.lower() or "installer_infrastructure" in e.lower() for e in errors), "Infra call as business exception must fail")

    def test_22_mutation_unknown_process(self) -> None:
        def mutate(reg):
            reg["records"][0]["process"] = "magical_process"
        errors = self._with_mutated_registry(mutate)
        self.assertTrue(any("unknown process" in e.lower() for e in errors), "Unknown process must fail")

    def test_23_mutation_unknown_phase(self) -> None:
        def mutate(reg):
            reg["records"][0]["phase"] = "SOMETHING_ELSE"
        errors = self._with_mutated_registry(mutate)
        self.assertTrue(any("unknown phase" in e.lower() for e in errors), "Unknown phase must fail")

    def test_24_mutation_unknown_reason_code(self) -> None:
        def mutate(reg):
            reg["records"][0]["reasonCode"] = "LEGACY"
        errors = self._with_mutated_registry(mutate)
        self.assertTrue(any("unknown reasoncode" in e.lower() for e in errors), "Unknown reasonCode must fail")


class LegacyExceptionIntegrityTest(unittest.TestCase):
    """R1 focused tests for canonical stale detection, whole-file/all-legacy gates,
    hookTargets/coveredCallSites/sourceFile validation and stable provenance."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.registry = load_json(REGISTRY_FILE)

    def _validate_dict(self, reg: dict) -> list[str]:
        import build_legacy_exception_registry as builder
        return builder.validate(reg)

    def _bad_source_file(self, bad_path: str, needle: str) -> None:
        reg = copy.deepcopy(self.registry)
        reg["records"][0]["sourceFile"] = bad_path
        errors = self._validate_dict(reg)
        self.assertTrue(
            any(needle.lower() in e.lower() for e in errors),
            f"Bad sourceFile {bad_path!r} must fail: {errors}",
        )

    def test_25_current_seeds_match_committed(self) -> None:
        import build_legacy_exception_registry as builder
        sites = builder.scan_legacy_call_sites()
        expected = builder.build_registry(sites)
        diffs = builder.canonical_diff(expected, self.registry)
        self.assertEqual(diffs, [], f"Committed registry must be canonical-identical to expected: {diffs}")

    def test_26_delete_seed_fails_check(self) -> None:
        import build_legacy_exception_registry as builder
        orig = copy.deepcopy(builder.LEGACY_EXCEPTION_SEEDS)
        try:
            builder.LEGACY_EXCEPTION_SEEDS = copy.deepcopy(builder.LEGACY_EXCEPTION_SEEDS[:-1])
            sites = builder.scan_legacy_call_sites()
            expected = builder.build_registry(sites)
            diffs = builder.canonical_diff(expected, self.registry)
            self.assertTrue(diffs, "Deleting a seed must make --check fail")
        finally:
            builder.LEGACY_EXCEPTION_SEEDS = orig

    def test_27_modify_seed_owner_fails_check(self) -> None:
        import build_legacy_exception_registry as builder
        orig = copy.deepcopy(builder.LEGACY_EXCEPTION_SEEDS)
        try:
            builder.LEGACY_EXCEPTION_SEEDS = copy.deepcopy(builder.LEGACY_EXCEPTION_SEEDS)
            builder.LEGACY_EXCEPTION_SEEDS[0]["owner"] = "ModifiedOwner"
            sites = builder.scan_legacy_call_sites()
            expected = builder.build_registry(sites)
            diffs = builder.canonical_diff(expected, self.registry)
            self.assertTrue(diffs, "Modifying seed owner must make --check fail")
        finally:
            builder.LEGACY_EXCEPTION_SEEDS = orig

    def test_28_modify_seed_owned_functions_fails_check(self) -> None:
        import build_legacy_exception_registry as builder
        orig = copy.deepcopy(builder.LEGACY_EXCEPTION_SEEDS)
        try:
            builder.LEGACY_EXCEPTION_SEEDS = copy.deepcopy(builder.LEGACY_EXCEPTION_SEEDS)
            builder.LEGACY_EXCEPTION_SEEDS[0]["ownedFunctions"] = {"MIUIVolumeDialogHook"}
            sites = builder.scan_legacy_call_sites()
            expected = builder.build_registry(sites)
            diffs = builder.canonical_diff(expected, self.registry)
            self.assertTrue(diffs, "Modifying seed ownedFunctions must make --check fail")
        finally:
            builder.LEGACY_EXCEPTION_SEEDS = orig

    def test_29_add_seed_fails_check(self) -> None:
        import build_legacy_exception_registry as builder
        orig = copy.deepcopy(builder.LEGACY_EXCEPTION_SEEDS)
        try:
            extra = {
                "id": "legacy-test-added",
                "owner": "VolumeDialogAutohideDelayHook",
                "sourceFile": "tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt",
                "entrypoint": "VolumeDialogAutohideDelayHook",
                "process": "system_ui",
                "phase": "PACKAGE_READY",
                "preferenceKeys": ["system_volumedialogdelay_expanded"],
                "reasonCode": "CROSS_PROCESS",
                "reason": "Test-only extra seed to prove --check detects additions.",
                "ownedFunctions": {"VolumeDialogAutohideDelayHook"},
                "hookTargets": ["com.android.systemui.miui.volume.MiuiVolumeDialogImpl#computeTimeoutH"],
                "testEvidence": ["tools/tests/test_legacy_exception_registry.py"],
                "exitCondition": "test",
            }
            builder.LEGACY_EXCEPTION_SEEDS = copy.deepcopy(builder.LEGACY_EXCEPTION_SEEDS) + [extra]
            sites = builder.scan_legacy_call_sites()
            expected = builder.build_registry(sites)
            diffs = builder.canonical_diff(expected, self.registry)
            self.assertTrue(diffs, "Adding a seed must make --check fail")
        finally:
            builder.LEGACY_EXCEPTION_SEEDS = orig

    def test_30_seed_order_does_not_change_canonical(self) -> None:
        import build_legacy_exception_registry as builder
        orig = copy.deepcopy(builder.LEGACY_EXCEPTION_SEEDS)
        try:
            sites = builder.scan_legacy_call_sites()
            expected_normal = builder.build_registry(sites)
            builder.LEGACY_EXCEPTION_SEEDS = list(reversed(copy.deepcopy(builder.LEGACY_EXCEPTION_SEEDS)))
            expected_reversed = builder.build_registry(sites)
            diffs = builder.canonical_diff(expected_normal, expected_reversed)
            self.assertEqual(diffs, [], "Reversing seed order must not change canonical registry")
        finally:
            builder.LEGACY_EXCEPTION_SEEDS = orig

    def test_31_single_record_whole_file_calls_fails(self) -> None:
        import build_legacy_exception_registry as builder
        sites = builder.scan_legacy_call_sites()
        file_rel = "tv/withaibuild/customiuizer/mods/SystemSettingsMoreHooks.kt"
        all_calls = sorted(
            builder._stable_call_id(s["rel"], s["line"], s["function"])
            for s in sites
            if s["category"] == "LEGACY_EXCEPTION" and s["rel"] == file_rel
        )
        reg = {
            "records": [
                {
                    "id": "test-whole-file-calls",
                    "status": "ACTIVE",
                    "owner": "WholeFile",
                    "sourceFile": file_rel,
                    "entrypoint": "USBConfigHook",
                    "process": "system_server",
                    "phase": "SYSTEM_SERVER_STARTING",
                    "preferenceKeys": ["system_defaultusb"],
                    "reasonCode": "CROSS_PROCESS",
                    "reason": "test whole-file coverage",
                    "coveredCallSites": all_calls,
                    "hookTargets": ["com.android.server.power.PowerManagerService#systemReady"],
                    "testEvidence": ["tools/tests/test_legacy_exception_registry.py"],
                    "exitCondition": "test",
                }
            ]
        }
        errors = self._validate_dict(reg)
        self.assertTrue(
            any("WHOLE_FILE_LEGACY_EXCEPTION_FORBIDDEN" in e for e in errors),
            f"Whole-file call coverage must fail: {errors}",
        )

    def test_32_single_record_whole_file_functions_fails(self) -> None:
        import build_legacy_exception_registry as builder
        sites = builder.scan_legacy_call_sites()
        file_rel = "tv/withaibuild/customiuizer/mods/SystemSettingsMoreHooks.kt"
        all_calls = sorted(
            builder._stable_call_id(s["rel"], s["line"], s["function"])
            for s in sites
            if s["category"] == "LEGACY_EXCEPTION" and s["rel"] == file_rel
        )
        # Pick the first and last call of the file: one from each legacy function.
        call_ids = [all_calls[0], all_calls[-1]]
        reg = {
            "records": [
                {
                    "id": "test-whole-file-functions",
                    "status": "ACTIVE",
                    "owner": "WholeFunctions",
                    "sourceFile": file_rel,
                    "entrypoint": "USBConfigHook",
                    "process": "system_server",
                    "phase": "SYSTEM_SERVER_STARTING",
                    "preferenceKeys": ["system_defaultusb"],
                    "reasonCode": "CROSS_PROCESS",
                    "reason": "test whole-function coverage",
                    "coveredCallSites": call_ids,
                    "hookTargets": ["com.android.server.power.PowerManagerService#systemReady"],
                    "testEvidence": ["tools/tests/test_legacy_exception_registry.py"],
                    "exitCondition": "test",
                }
            ]
        }
        errors = self._validate_dict(reg)
        self.assertTrue(
            any("WHOLE_FILE_LEGACY_EXCEPTION_FORBIDDEN" in e for e in errors),
            f"Whole-file function coverage must fail: {errors}",
        )

    def test_33_multiple_records_all_legacy_calls_fails(self) -> None:
        import build_legacy_exception_registry as builder
        sites = builder.scan_legacy_call_sites()
        all_legacy = sorted(
            builder._stable_call_id(s["rel"], s["line"], s["function"])
            for s in sites
            if s["category"] == "LEGACY_EXCEPTION"
        )
        half = len(all_legacy) // 2
        file_rel = "tv/withaibuild/customiuizer/mods/SystemAudioAndVolumeHooks.kt"
        rec = {
            "status": "ACTIVE",
            "owner": "AllLegacyBatch",
            "sourceFile": file_rel,
            "entrypoint": "NotificationVolumeSettingsHook",
            "process": "per_app",
            "phase": "PACKAGE_READY",
            "preferenceKeys": ["system_separatevolume"],
            "reasonCode": "CROSS_PROCESS",
            "reason": "test all-legacy batch",
            "hookTargets": ["com.android.settings.MiuiSoundSettings#onCreate"],
            "testEvidence": ["tools/tests/test_legacy_exception_registry.py"],
            "exitCondition": "test",
        }
        rec1 = dict(rec, id="test-all-1", coveredCallSites=all_legacy[:half])
        rec2 = dict(rec, id="test-all-2", coveredCallSites=all_legacy[half:])
        reg = {"records": [rec1, rec2]}
        errors = self._validate_dict(reg)
        self.assertTrue(
            any("ALL_LEGACY_CALLS_BATCH_FORBIDDEN" in e for e in errors),
            f"Batch covering all legacy calls must fail: {errors}",
        )

    def test_34_hook_targets_empty_fails(self) -> None:
        reg = copy.deepcopy(self.registry)
        reg["records"][0]["hookTargets"] = []
        errors = self._validate_dict(reg)
        self.assertTrue(any("hookTargets is empty" in e for e in errors), "Empty hookTargets must fail")

    def test_35_hook_targets_blank_fails(self) -> None:
        reg = copy.deepcopy(self.registry)
        reg["records"][0]["hookTargets"] = ["  "]
        errors = self._validate_dict(reg)
        self.assertTrue(
            any("empty or whitespace" in e.lower() for e in errors),
            "Blank hookTargets must fail",
        )

    def test_36_hook_targets_duplicate_fails(self) -> None:
        reg = copy.deepcopy(self.registry)
        reg["records"][0]["hookTargets"] = ["com.foo.Bar#method", "com.foo.Bar#method"]
        errors = self._validate_dict(reg)
        self.assertTrue(any("duplicate" in e.lower() for e in errors), "Duplicate hookTargets must fail")

    def test_37_covered_call_sites_empty_fails(self) -> None:
        reg = copy.deepcopy(self.registry)
        reg["records"][0]["coveredCallSites"] = []
        errors = self._validate_dict(reg)
        self.assertTrue(any("coveredCallSites is empty" in e for e in errors), "Empty coveredCallSites must fail")

    def test_38_covered_call_nonexistent_fails(self) -> None:
        reg = copy.deepcopy(self.registry)
        reg["records"][0]["coveredCallSites"].append(
            "tv/withaibuild/customiuizer/mods/NonExistent.kt:1:FakeFunction"
        )
        errors = self._validate_dict(reg)
        self.assertTrue(
            any("not present in the current LEGACY_EXCEPTION census" in e for e in errors),
            "Nonexistent covered call must fail",
        )

    def test_39_covered_call_typed_fails(self) -> None:
        import build_legacy_exception_registry as builder
        sites = builder.scan_legacy_call_sites()
        typed = [s for s in sites if s["category"] == "REGISTRY_FEATURE"]
        if not typed:
            self.skipTest("no typed calls to test with")
        reg = copy.deepcopy(self.registry)
        reg["records"][0]["coveredCallSites"].append(
            builder._stable_call_id(typed[0]["rel"], typed[0]["line"], typed[0]["function"])
        )
        errors = self._validate_dict(reg)
        self.assertTrue(
            any("typed-catalog owned call" in e for e in errors),
            "Typed-catalog call in registry must fail",
        )

    def test_40_covered_call_api_bridge_fails(self) -> None:
        import build_legacy_exception_registry as builder
        sites = builder.scan_legacy_call_sites()
        infra = [s for s in sites if s["category"] in ("API_BRIDGE", "INSTALLER_INFRASTRUCTURE")]
        if not infra:
            self.skipTest("no API_BRIDGE/INSTALLER_INFRASTRUCTURE calls to test with")
        reg = copy.deepcopy(self.registry)
        reg["records"][0]["coveredCallSites"].append(
            builder._stable_call_id(infra[0]["rel"], infra[0]["line"], infra[0]["function"])
        )
        errors = self._validate_dict(reg)
        self.assertTrue(
            any("API_BRIDGE" in e or "INSTALLER_INFRASTRUCTURE" in e for e in errors),
            "Infra call as business exception must fail",
        )

    def test_41_source_file_backslash_fails(self) -> None:
        self._bad_source_file(
            "tv\\withaibuild\\customiuizer\\mods\\SystemSettingsMoreHooks.kt",
            "backslash",
        )

    def test_42_source_file_mixed_separator_fails(self) -> None:
        self._bad_source_file(
            "tv/withaibuild//customiuizer/mods/SystemSettingsMoreHooks.kt",
            "not normalized",
        )

    def test_43_source_file_absolute_fails(self) -> None:
        self._bad_source_file(
            "/tv/withaibuild/customiuizer/mods/SystemSettingsMoreHooks.kt",
            "absolute",
        )

    def test_44_source_file_dotdot_fails(self) -> None:
        self._bad_source_file(
            "tv/withaibuild/../customiuizer/mods/SystemSettingsMoreHooks.kt",
            "'..'",
        )

    def test_45_provenance_input_digest_mismatch_fails(self) -> None:
        import build_legacy_exception_registry as builder
        sites = builder.scan_legacy_call_sites()
        expected = builder.build_registry(sites)
        reg = copy.deepcopy(self.registry)
        reg["inputDigest"] = "0" * 64
        diffs = builder.canonical_diff(expected, reg)
        self.assertTrue(
            any("inputDigest" in d for d in diffs),
            "Mismatched inputDigest must fail canonical check",
        )

    def test_46_generated_at_not_canonical(self) -> None:
        import build_legacy_exception_registry as builder
        sites = builder.scan_legacy_call_sites()
        expected = builder.build_registry(sites)
        reg = copy.deepcopy(expected)
        reg["generatedAt"] = "2100-01-01T00:00:00+00:00"
        reg["sourceCommit"] = "deadbeef00000000000000000000000000000000"
        diffs = builder.canonical_diff(expected, reg)
        self.assertEqual(diffs, [], "generatedAt and sourceCommit must not affect canonical comparison")

    def test_47_p3_3a_batch_covers_exactly_eleven_calls(self) -> None:
        p3_3a = [r for r in self.registry["records"] if r["batch"] == "P3.3A"]
        total = sum(len(r["coveredCallSites"]) for r in p3_3a)
        self.assertEqual(total, 11, "P3.3A batch must cover exactly 11 call sites")

    def test_48_first_batch_size_is_four(self) -> None:
        self.assertEqual(self.registry.get("firstBatchSize"), 4, "firstBatchSize must remain 4")

    def test_49_all_records_have_batch(self) -> None:
        for rec in self.registry["records"]:
            self.assertIn(rec.get("batch"), ("P3.3A", "P3.3B"), f"Record {rec.get('id')} has invalid batch")

    def test_48_p3_2_1b_python_test_not_regressed(self) -> None:
        import subprocess
        result = subprocess.run(
            [sys.executable, "-m", "unittest", "tools.tests.test_check_hook_contract_parity"],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
        )
        self.assertEqual(
            result.returncode,
            0,
            f"P3.2.1B contract parity tests must not regress: {result.stdout}\n{result.stderr}",
        )


if __name__ == "__main__":
    unittest.main()
