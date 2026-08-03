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


if __name__ == "__main__":
    unittest.main()
