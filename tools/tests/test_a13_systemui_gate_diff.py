"""R1-B2 proof hardening tests for a13_systemui_gate_diff.py."""

from __future__ import annotations

import importlib
import io
import json
import os
import shutil
import sys
import tempfile
import unittest
from pathlib import Path
from typing import Any

# The module name starts with a digit, so use importlib.
_tools_dir = Path(__file__).resolve().parent.parent
if str(_tools_dir) not in sys.path:
    sys.path.insert(0, str(_tools_dir))

REPO_ROOT = Path(__file__).resolve().parent.parent.parent

diff_mod = importlib.import_module("a13_systemui_gate_diff")
inv_mod = importlib.import_module("a13_systemui_gate_inventory")


class AstMatchingTests(unittest.TestCase):
    """Canonical AST matching and mismatch classification."""

    def _parse(self, expr: str, defaults: list[Any] | None = None, kinds: list[str] | None = None) -> Any:
        return diff_mod.parse_expression(expr, defaults or [], kinds or [])

    def test_identical_pref_match(self):
        a = self._parse('MainModule.mPrefs.getBoolean("system_foo")')
        b = self._parse('prefs.getBoolean("system_foo")')
        self.assertEqual(diff_mod.canonical(a), diff_mod.canonical(b))

    def test_prefs_vs_mainmodule_match(self):
        a = self._parse('MainModule.mPrefs.getBoolean("system_foo")')
        b = self._parse('prefs.getBoolean("system_foo")')
        self.assertEqual(diff_mod.canonical(a), diff_mod.canonical(b))

    def test_implicit_false_vs_explicit_false_match(self):
        a = self._parse('MainModule.mPrefs.getBoolean("system_foo")')
        b = self._parse('MainModule.mPrefs.getBoolean("system_foo", false)')
        self.assertEqual(diff_mod.canonical(a), diff_mod.canonical(b))

    def test_different_default_is_default_mismatch(self):
        a = self._parse('MainModule.mPrefs.getBoolean("system_foo")')
        b = self._parse('MainModule.mPrefs.getBoolean("system_foo", true)')
        mtype = diff_mod.classify_mismatch(diff_mod.canonical(a), diff_mod.canonical(b))
        self.assertEqual(mtype, "DEFAULT_MISMATCH")

    def test_greater_vs_ge_comparator_mismatch(self):
        a = self._parse('MainModule.mPrefs.getInt("system_bar", 0) > 0')
        b = self._parse('prefs.getInt("system_bar", 0) >= 0')
        mtype = diff_mod.classify_mismatch(diff_mod.canonical(a), diff_mod.canonical(b))
        self.assertEqual(mtype, "COMPARATOR_MISMATCH")

    def test_and_vs_or_composite_mismatch(self):
        a = self._parse('MainModule.mPrefs.getBoolean("a") && MainModule.mPrefs.getBoolean("b")')
        b = self._parse('prefs.getBoolean("a") || prefs.getBoolean("b")')
        mtype = diff_mod.classify_mismatch(diff_mod.canonical(a), diff_mod.canonical(b))
        self.assertEqual(mtype, "COMPOSITE_CONDITION_MISMATCH")

    def test_or_commutative_match(self):
        a = self._parse('prefs.getBoolean("a") || prefs.getBoolean("b")')
        b = self._parse('MainModule.mPrefs.getBoolean("b") || MainModule.mPrefs.getBoolean("a")')
        self.assertEqual(diff_mod.canonical(a), diff_mod.canonical(b))

    def test_missing_not_composite_mismatch(self):
        a = self._parse('!prefs.getBoolean("a")')
        b = self._parse('prefs.getBoolean("a")')
        mtype = diff_mod.classify_mismatch(diff_mod.canonical(a), diff_mod.canonical(b))
        self.assertEqual(mtype, "COMPOSITE_CONDITION_MISMATCH")

    def test_or_atomic_units_split(self):
        ast = self._parse('prefs.getBoolean("a") || prefs.getBoolean("b")')
        units = diff_mod.atomic_units(ast)
        self.assertEqual(len(units), 2)

    def test_and_remains_composite(self):
        ast = self._parse('prefs.getBoolean("a") && prefs.getBoolean("b")')
        units = diff_mod.atomic_units(ast)
        self.assertEqual(len(units), 1)


class DiffAuditTests(unittest.TestCase):
    """End-to-end diff_from_inventory cases."""

    def _inventory(self, install: list[dict[str, Any]], startup: list[dict[str, Any]] | None = None,
                   catalog: list[dict[str, Any]] | None = None, dispatch: list[dict[str, Any]] | None = None) -> dict[str, Any]:
        return {
            "schema_version": "1.0",
            "generated_from": [],
            "INSTALL_CONDITIONS": install,
            "STARTUP_GATE_CONDITIONS": startup or [],
            "GLOBAL_ACTION_DOMAIN_RULES": [],
            "FEATURE_DISPATCH_CALLS": dispatch or [],
            "FEATURE_CATALOG_GATES": catalog or [],
        }

    def _cond(self, cond_id: str, expr: str, keys: list[str], phase: str = "POST_RESTART_GUARD_RUNTIME",
              feature_id: str = "", target: str = "") -> dict[str, Any]:
        return {
            "id": cond_id,
            "phase": phase,
            "feature_id": feature_id,
            "install_target": target,
            "normalized_expression": expr,
            "raw_expression": expr,
            "preference_keys": keys,
            "accessors": ["getBoolean"] * len([k for k in keys if "getBoolean" in expr]) or ["getBoolean"],
            "comparators": [],
            "boolean_operators": [],
            "default_values": [False] * len(keys),
            "default_kinds": ["IMPLICIT_PREFMAP_DEFAULT"] * len(keys),
            "start_line": 1,
            "end_line": 1,
            "source_file": "install.java",
            "source_method": "install",
        }

    def _startup(self, cond_id: str, expr: str, keys: list[str]) -> dict[str, Any]:
        c = self._cond(cond_id, expr, keys, phase="STARTUP_GATE")
        c["source_method"] = "hasAnySystemUiStartupFeature"
        return c

    def _catalog(self, feature_id: str, expr: str, keys: list[str]) -> dict[str, Any]:
        return {
            "id": f"FeatureCatalog_{feature_id}",
            "feature_id": feature_id,
            "phase": "FEATURE_CATALOG_GATE",
            "condition_expression": expr,
            "normalized_expression": expr,
            "declared_preference_keys": [],
            "condition_preference_keys": keys,
            "default_values": [False] * len(keys),
            "default_kinds": ["IMPLICIT_PREFMAP_DEFAULT"] * len(keys),
        }

    def _dispatch(self, feature_id: str) -> dict[str, Any]:
        return {
            "id": f"install_installById_{feature_id}",
            "feature_id": feature_id,
            "call_expression": f'FeatureDispatcher.installById("{feature_id}", lpparam)',
            "start_line": 1,
            "end_line": 1,
        }

    def test_installer_missing_from_startup_is_installer_only(self):
        inv = self._inventory(
            install=[self._cond("install_if_1", 'MainModule.mPrefs.getBoolean("system_foo")', ["system_foo"])],
        )
        installer = "public class X { static void install() { if (MainModule.mPrefs.getBoolean(\"system_foo\")); } }"
        result = diff_mod.diff_from_inventory(inv, installer)
        self.assertEqual(result.counts["INSTALLER_ONLY"], 1)

    def test_startup_extra_is_gate_only(self):
        inv = self._inventory(
            install=[self._cond("install_if_1", 'MainModule.mPrefs.getBoolean("system_foo")', ["system_foo"])],
            startup=[self._startup("startup_if_1", 'prefs.getBoolean("system_foo")', ["system_foo"]),
                     self._startup("startup_if_2", 'prefs.getBoolean("system_bar")', ["system_bar"])],
        )
        installer = "public class X { static void install() { if (MainModule.mPrefs.getBoolean(\"system_foo\")); } }"
        result = diff_mod.diff_from_inventory(inv, installer)
        self.assertEqual(result.counts["GATE_ONLY_UNEXPLAINED"], 1)

    def test_unconditional_dispatcher_with_catalog_and_startup_match(self):
        inv = self._inventory(
            install=[],
            startup=[self._startup("startup_if_1", 'prefs.getBoolean("system_hidemoreicon")', ["system_hidemoreicon"])],
            catalog=[self._catalog("noMoreIcon", 'prefs.getBoolean("system_hidemoreicon")', ["system_hidemoreicon"])],
            dispatch=[self._dispatch("noMoreIcon")],
        )
        installer = "public class X { static void install() { } }"
        result = diff_mod.diff_from_inventory(inv, installer)
        self.assertEqual(result.counts["INSTALLER_CATALOG_MATCH"], 1)

    def test_missing_catalog_is_feature_catalog_gate_unknown(self):
        inv = self._inventory(
            install=[self._cond("install_if_1", 'MainModule.mPrefs.getBoolean("system_hidemoreicon")', ["system_hidemoreicon"])],
            startup=[self._startup("startup_if_1", 'prefs.getBoolean("system_hidemoreicon")', ["system_hidemoreicon"])],
            catalog=[],
            dispatch=[self._dispatch("noMoreIcon")],
        )
        installer = "public class X { static void install() { if (MainModule.mPrefs.getBoolean(\"system_hidemoreicon\")); } }"
        result = diff_mod.diff_from_inventory(inv, installer)
        self.assertEqual(result.counts["FEATURE_CATALOG_GATE_UNKNOWN"], 1)

    def test_conditional_dispatcher_catalog_mismatch(self):
        inv = self._inventory(
            install=[self._cond("install_if_1", 'MainModule.mPrefs.getBoolean("system_hidemoreicon")', ["system_hidemoreicon"], feature_id="noMoreIcon")],
            startup=[self._startup("startup_if_1", 'prefs.getBoolean("system_hidemoreicon")', ["system_hidemoreicon"])],
            catalog=[self._catalog("noMoreIcon", 'prefs.getInt("system_hidemoreicon", 0) > 0', ["system_hidemoreicon"])],
            dispatch=[self._dispatch("noMoreIcon")],
        )
        installer = "public class X { static void install() { if (MainModule.mPrefs.getBoolean(\"system_hidemoreicon\")); } }"
        result = diff_mod.diff_from_inventory(inv, installer)
        self.assertEqual(result.counts["INSTALLER_CATALOG_MISMATCH"], 1)

    def test_package_guard_exclusion(self):
        inv = self._inventory(
            install=[self._cond("install_if_pkg", 'pkgName.equals("com.android.systemui")', [], phase="PACKAGE_GUARD")],
        )
        result = diff_mod.diff_from_inventory(inv, "")
        self.assertEqual(result.counts["UNMATCHED_INFRASTRUCTURE"], 1)

    def test_restart_guard_exclusion(self):
        inv = self._inventory(
            install=[self._cond("install_if_restart", 'isWithinSystemUiRestartGuard(restartTime, currentTime)', [], phase="RESTART_GUARD")],
        )
        result = diff_mod.diff_from_inventory(inv, "")
        self.assertEqual(result.counts["UNMATCHED_INFRASTRUCTURE"], 1)

    def test_pre_restart_guard_infrastructure_exclusion(self):
        inv = self._inventory(
            install=[self._cond("install_if_pre", 'MainModule.mPrefs.getBoolean("system_screenshot_overlay")', ["system_screenshot_overlay"], phase="PRE_RESTART_GUARD_INFRASTRUCTURE")],
        )
        result = diff_mod.diff_from_inventory(inv, "")
        self.assertEqual(result.counts["UNMATCHED_INFRASTRUCTURE"], 1)

    def test_resource_phase_participates(self):
        inv = self._inventory(
            install=[],
            startup=[self._startup("startup_if_1", 'prefs.getBoolean("system_statusbar_topmargin")', ["system_statusbar_topmargin"])],
        )
        resource = '''class X { fun setupStatusBar() { if (MainModule.mPrefs.getBoolean("system_statusbar_topmargin")); } }'''
        result = diff_mod.diff_from_inventory(inv, "", resource_text=resource, resource_source_file="X.kt")
        self.assertEqual(result.counts["MATCH"], 1)

    def test_contaminated_global_action_domain(self):
        installer = '''class X {
            static boolean isSystemUiGlobalActionKey(String key) { return key != null && key.endsWith("_action"); }
            static boolean hasAnyGlobalAction(PrefMap prefs) { return false; }
            static void install() { if (MainModule.mPrefs.getBoolean("launcher_swipedown_action")); }
        }'''
        inv = self._inventory(
            install=[self._cond("install_if_1", 'MainModule.mPrefs.getBoolean("launcher_swipedown_action")', ["launcher_swipedown_action"])],
            startup=[self._startup("startup_if_1", 'hasAnyGlobalAction(prefs)', [])],
        )
        result = diff_mod.diff_from_inventory(inv, installer)
        self.assertEqual(result.global_action_domain.get("status"), "PARSED_CONTAMINATED")
        self.assertGreater(result.counts["DOMAIN_CONTAMINATION"], 0)

    def test_unknown_global_action_parser_is_blocker(self):
        installer = '''class X {
            static boolean isSystemUiGlobalActionKey(String key) {
                if (key == null) return false;
                String tail = key.substring(key.length() - 7);
                return tail.equals("_action");
            }
            static boolean hasAnyGlobalAction(PrefMap prefs) { return false; }
            static void install() { if (MainModule.mPrefs.getBoolean("controls_backlong_action")); }
        }'''
        inv = self._inventory(
            install=[self._cond("install_if_1", 'MainModule.mPrefs.getBoolean("controls_backlong_action")', ["controls_backlong_action"])],
            startup=[self._startup("startup_if_1", 'hasAnyGlobalAction(prefs)', [])],
        )
        result = diff_mod.diff_from_inventory(inv, installer)
        self.assertEqual(result.global_action_domain.get("status"), "UNKNOWN")
        self.assertGreater(result.counts["SEMANTIC_REVIEW_REQUIRED"], 0)


class RepoIsolationTests(unittest.TestCase):
    """Repo-root isolation and temp-repo provenance."""

    def _make_minimal_repo(self, name: str, with_resource: bool) -> Path:
        temp = Path(tempfile.mkdtemp(prefix=f"a13_diff_iso_{name}_"))
        inv_path = temp / "docs/audit/A13_SYSTEMUI_GATE_INVENTORY.json"
        inv_path.parent.mkdir(parents=True, exist_ok=True)
        inventory = {
            "schema_version": "1.0",
            "generated_from": [],
            "INSTALL_CONDITIONS": [],
            "STARTUP_GATE_CONDITIONS": [{
                "id": "startup_if_1",
                "phase": "STARTUP_GATE",
                "feature_id": "",
                "install_target": "",
                "normalized_expression": 'prefs.getBoolean("system_extra_resource")',
                "raw_expression": 'prefs.getBoolean("system_extra_resource")',
                "preference_keys": ["system_extra_resource"],
                "accessors": ["getBoolean"],
                "comparators": [],
                "boolean_operators": [],
                "default_values": [False],
                "default_kinds": ["IMPLICIT_PREFMAP_DEFAULT"],
                "start_line": 1,
                "end_line": 1,
                "source_file": "install.java",
                "source_method": "hasAnySystemUiStartupFeature",
            }],
            "GLOBAL_ACTION_DOMAIN_RULES": [],
            "FEATURE_DISPATCH_CALLS": [],
            "FEATURE_CATALOG_GATES": [],
        }
        inv_path.write_text(json.dumps(inventory, indent=2), encoding="utf-8")
        installer = "public class X { static void install() { } }"
        installer_path = temp / "app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java"
        installer_path.parent.mkdir(parents=True, exist_ok=True)
        installer_path.write_text(installer, encoding="utf-8")
        if with_resource:
            hook = temp / "app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt"
            hook.parent.mkdir(parents=True, exist_ok=True)
            hook.write_text(
                'class X { fun setupStatusBar() { if (MainModule.mPrefs.getBoolean("system_extra_resource")); } }',
                encoding="utf-8",
            )
        return temp

    def test_repo_root_isolation_resource_differs(self):
        repo_a = self._make_minimal_repo("A", with_resource=True)
        repo_b = self._make_minimal_repo("B", with_resource=False)
        try:
            result_a = diff_mod.diff_from_repo(repo_a)
            result_b = diff_mod.diff_from_repo(repo_b)
            # A has a resource unit that matches the lone startup gate; B does not,
            # so B reports INSTALLER_ONLY and A reports MATCH.
            self.assertNotEqual(result_a.counts, result_b.counts)
            self.assertEqual(result_a.counts["MATCH"], 1)
            self.assertEqual(result_b.counts["GATE_ONLY_UNEXPLAINED"], 1)
            # Provenance must not contain an absolute repo_root or any temp path.
            for result in (result_a, result_b):
                self.assertNotIn("repo_root", result.provenance)
                text = json.dumps(result.to_dict(), sort_keys=True)
                self.assertNotIn(str(repo_a.resolve()), text)
                self.assertNotIn(str(repo_b.resolve()), text)
        finally:
            shutil.rmtree(repo_a)
            shutil.rmtree(repo_b)

    def test_deterministic_json_and_markdown(self):
        repo = Path(__file__).resolve().parent.parent.parent
        result1 = diff_mod.diff_from_repo(repo)
        result2 = diff_mod.diff_from_repo(repo)
        self.assertEqual(
            json.dumps(result1.to_dict(), sort_keys=True),
            json.dumps(result2.to_dict(), sort_keys=True),
        )
        md1 = diff_mod.render_markdown(result1)
        md2 = diff_mod.render_markdown(result2)
        self.assertEqual(md1, md2)


class SummaryStatTests(unittest.TestCase):
    """Summary statistics are computed correctly."""

    def test_atomic_match_count_is_record_count(self):
        inv = {
            "schema_version": "1.0",
            "generated_from": [],
            "INSTALL_CONDITIONS": [{
                "id": "install_if_1",
                "phase": "POST_RESTART_GUARD_RUNTIME",
                "feature_id": "",
                "install_target": "",
                "normalized_expression": 'MainModule.mPrefs.getBoolean("system_foo")',
                "raw_expression": 'MainModule.mPrefs.getBoolean("system_foo")',
                "preference_keys": ["system_foo"],
                "accessors": ["getBoolean"],
                "comparators": [],
                "boolean_operators": [],
                "default_values": [False],
                "default_kinds": ["IMPLICIT_PREFMAP_DEFAULT"],
                "start_line": 1,
                "end_line": 1,
                "source_file": "install.java",
                "source_method": "install",
            }],
            "STARTUP_GATE_CONDITIONS": [{
                "id": "startup_if_1",
                "phase": "STARTUP_GATE",
                "feature_id": "",
                "install_target": "",
                "normalized_expression": 'prefs.getBoolean("system_foo")',
                "raw_expression": 'prefs.getBoolean("system_foo")',
                "preference_keys": ["system_foo"],
                "accessors": ["getBoolean"],
                "comparators": [],
                "boolean_operators": [],
                "default_values": [False],
                "default_kinds": ["IMPLICIT_PREFMAP_DEFAULT"],
                "start_line": 1,
                "end_line": 1,
                "source_file": "install.java",
                "source_method": "hasAnySystemUiStartupFeature",
            }],
            "GLOBAL_ACTION_DOMAIN_RULES": [],
            "FEATURE_DISPATCH_CALLS": [],
            "FEATURE_CATALOG_GATES": [],
        }
        installer = "public class X { static void install() { if (MainModule.mPrefs.getBoolean(\"system_foo\")); } }"
        result = diff_mod.diff_from_inventory(inv, installer)
        self.assertEqual(result.counts["MATCH"], 1)
        self.assertEqual(result.matched_atomic_units, 1)
        self.assertEqual(result.matched_unique_installer_conditions, 1)
        self.assertEqual(result.matched_unique_startup_conditions, 1)
        self.assertEqual(result.total_installer_atomic_units, 1)
        self.assertEqual(result.total_startup_atomic_units, 1)

    def test_duplicate_atomic_unit_does_not_inflate_unique_condition_count(self):
        inv = {
            "schema_version": "1.0",
            "generated_from": [],
            "INSTALL_CONDITIONS": [{
                "id": "install_if_1",
                "phase": "POST_RESTART_GUARD_RUNTIME",
                "feature_id": "",
                "install_target": "",
                "normalized_expression": 'MainModule.mPrefs.getBoolean("system_foo") || MainModule.mPrefs.getBoolean("system_foo")',
                "raw_expression": 'MainModule.mPrefs.getBoolean("system_foo") || MainModule.mPrefs.getBoolean("system_foo")',
                "preference_keys": ["system_foo", "system_foo"],
                "accessors": ["getBoolean", "getBoolean"],
                "comparators": [],
                "boolean_operators": ["OR"],
                "default_values": [False, False],
                "default_kinds": ["IMPLICIT_PREFMAP_DEFAULT", "IMPLICIT_PREFMAP_DEFAULT"],
                "start_line": 1,
                "end_line": 1,
                "source_file": "install.java",
                "source_method": "install",
            }],
            "STARTUP_GATE_CONDITIONS": [{
                "id": "startup_if_1",
                "phase": "STARTUP_GATE",
                "feature_id": "",
                "install_target": "",
                "normalized_expression": 'prefs.getBoolean("system_foo") || prefs.getBoolean("system_foo")',
                "raw_expression": 'prefs.getBoolean("system_foo") || prefs.getBoolean("system_foo")',
                "preference_keys": ["system_foo", "system_foo"],
                "accessors": ["getBoolean", "getBoolean"],
                "comparators": [],
                "boolean_operators": ["OR"],
                "default_values": [False, False],
                "default_kinds": ["IMPLICIT_PREFMAP_DEFAULT", "IMPLICIT_PREFMAP_DEFAULT"],
                "start_line": 1,
                "end_line": 1,
                "source_file": "install.java",
                "source_method": "hasAnySystemUiStartupFeature",
            }],
            "GLOBAL_ACTION_DOMAIN_RULES": [],
            "FEATURE_DISPATCH_CALLS": [],
            "FEATURE_CATALOG_GATES": [],
        }
        installer = "public class X { static void install() { if (MainModule.mPrefs.getBoolean(\"system_foo\")); } }"
        result = diff_mod.diff_from_inventory(inv, installer)
        # OR/AND canonicalization deduplicates identical children, so the
        # duplicate atomic units collapse to a single match.  Unique condition
        # counts stay at 1 and do not inflate.
        self.assertEqual(result.matched_atomic_units, 1)
        self.assertEqual(result.matched_unique_installer_conditions, 1)
        self.assertEqual(result.matched_unique_startup_conditions, 1)


class MutationIntegrationTests(unittest.TestCase):
    """Counter-proof mutation suites against the real repository."""

    def _capture_run(self, runner):
        out = io.StringIO()
        old = sys.stdout
        sys.stdout = out
        try:
            return runner(REPO_ROOT)
        finally:
            sys.stdout = old

    def test_inventory_mutations_pass(self):
        """A-F inventory-level counter-proofs flag the expected categories."""
        self.assertEqual(self._capture_run(diff_mod.run_mutations), 0)

    def test_source_mutations_pass(self):
        """A-G source-level counter-proofs isolate mutations in temp repos."""
        self.assertEqual(self._capture_run(diff_mod.run_source_mutations), 0)


class DeterministicOutputTests(unittest.TestCase):
    """Identical source in different checkout paths must produce identical output."""

    _REQUIRED_FILES = [
        Path("docs/audit/A13_SYSTEMUI_GATE_INVENTORY.json"),
        Path("app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java"),
        Path("app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt"),
        Path("app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt"),
    ]

    def _create_mirror_repo(self, prefix: str) -> Path:
        temp = Path(tempfile.mkdtemp(prefix=prefix))
        for rel in self._REQUIRED_FILES:
            dst = temp / rel
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(REPO_ROOT / rel, dst)
        return temp

    def _stringify_dict(self, data: dict[str, Any]) -> str:
        return json.dumps(data, sort_keys=True, indent=2)

    def test_identical_source_different_checkout_paths(self):
        repo_a = self._create_mirror_repo("a13_diff_det_a_")
        repo_b = self._create_mirror_repo("a13_diff_det_b_")
        try:
            result_a = diff_mod.diff_from_repo(repo_a)
            result_b = diff_mod.diff_from_repo(repo_b)
            self.assertEqual(result_a.to_dict(), result_b.to_dict())
            self.assertEqual(diff_mod.render_markdown(result_a), diff_mod.render_markdown(result_b))

            text = self._stringify_dict(result_a.to_dict())
            self.assertNotIn(str(repo_a.resolve()), text)
            self.assertNotIn(str(repo_b.resolve()), text)
        finally:
            shutil.rmtree(repo_a)
            shutil.rmtree(repo_b)


if __name__ == "__main__":
    unittest.main()
