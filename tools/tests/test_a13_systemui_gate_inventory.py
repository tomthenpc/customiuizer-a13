#!/usr/bin/env python3
"""Unit tests for a13_systemui_gate_inventory.

Tests cover both synthetic fixtures and the real repository sources.
"""

import json
import shutil
import sys
import tempfile
import unittest
from pathlib import Path
from typing import Any

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import a13_systemui_gate_inventory as inv


REPO_ROOT = Path(__file__).resolve().parent.parent.parent


def _make_feature_catalog_text(condition: str, declared_keys: str = 'setOf("key_a")') -> str:
    return f"""package tv.withaibuild.customiuizer.mods.catalog

import tv.withaibuild.customiuizer.utils.PrefMap
import tv.withaibuild.customiuizer.mods.utils.ProcessScope

object FeatureCatalog {{
    private val specs = listOf(
        FeatureSpec(
            id = "testFeature",
            processScope = ProcessScope.SYSTEM_UI,
            preferenceKeys = {declared_keys},
            condition = {condition},
            installer = {{ runtime, compatResult -> }}
        )
    )
}}
"""


def _make_repo(tmp: Path, installer_text: str, catalog_text: str | None = None) -> Path:
    (tmp / "app/src/main/java/tv/withaibuild/customiuizer/installers").mkdir(parents=True, exist_ok=True)
    (tmp / "app/src/main/java/tv/withaibuild/customiuizer/mods/catalog").mkdir(parents=True, exist_ok=True)
    (tmp / "app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java").write_text(
        installer_text, encoding="utf-8"
    )
    if catalog_text is None:
        catalog_text = _make_feature_catalog_text("{ prefs -> prefs.getBoolean(\"key_a\") }")
    (tmp / "app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt").write_text(
        catalog_text, encoding="utf-8"
    )
    return tmp


def _all_entries(data: dict[str, Any]) -> list[dict[str, Any]]:
    entries: list[dict[str, Any]] = []
    for value in data.values():
        if isinstance(value, list) and value and isinstance(value[0], dict):
            entries.extend(value)
    return entries


class SourceFileAndLineNumberTests(unittest.TestCase):
    def test_source_file_is_relative_posix(self) -> None:
        data = inv.to_json(inv.inventory_from_sources(REPO_ROOT))
        for entry in _all_entries(data):
            self.assertNotIn("\\", entry["source_file"])
            self.assertFalse(Path(entry["source_file"]).is_absolute())
            self.assertTrue(entry["source_file"].startswith("app/src/main/java/"))

    def test_absolute_line_numbers_after_leading_blank_lines(self) -> None:
        java = (
            "\n" * 50
            + """public class Foo {
    public static void install() {
        if (prefs.getBoolean("x")) { A(); }
    }
}
"""
        )
        body = inv.find_method_body(java, "public static void install")
        self.assertIsNotNone(body)
        conds = inv.extract_conditions(
            body.text,
            "Foo.java",
            "install",
            "TEST",
            full_text=java,
            body_start=body.start_offset,
        )
        self.assertEqual(len(conds), 1)
        # 50 blank lines + class line (51) + method line (52) + if line (53)
        self.assertEqual(conds[0].start_line, 53)
        self.assertGreater(conds[0].start_line, 1)


class DeterminismTests(unittest.TestCase):
    def test_two_temp_repo_roots_produce_identical_outputs(self) -> None:
        installer_src = REPO_ROOT / inv.INSTALLER_REL
        catalog_src = REPO_ROOT / inv.CATALOG_REL
        with tempfile.TemporaryDirectory() as t1, tempfile.TemporaryDirectory() as t2:
            r1 = Path(t1)
            r2 = Path(t2)
            for r in (r1, r2):
                (r / "app/src/main/java/tv/withaibuild/customiuizer/installers").mkdir(parents=True)
                (r / "app/src/main/java/tv/withaibuild/customiuizer/mods/catalog").mkdir(parents=True)
                shutil.copy(installer_src, r / inv.INSTALLER_REL)
                shutil.copy(catalog_src, r / inv.CATALOG_REL)
            _, _, json1, md1 = inv.write_inventory(r1)
            _, _, json2, md2 = inv.write_inventory(r2)
            self.assertEqual(json1, json2)
            self.assertEqual(md1, md2)

    def test_deterministic_byte_output(self) -> None:
        _, _, json1, md1 = inv.write_inventory(REPO_ROOT)
        _, _, json2, md2 = inv.write_inventory(REPO_ROOT)
        self.assertEqual(json1, json2)
        self.assertEqual(md1, md2)


class CatalogParsingTests(unittest.TestCase):
    def test_condition_lambda_parsing(self) -> None:
        condition = """{ prefs ->
                prefs.getBoolean("key_a") ||
                prefs.getInt("key_b", 0) > 0
            }"""
        with tempfile.TemporaryDirectory() as t:
            repo = _make_repo(Path(t), "public class SystemUiInstaller {}", _make_feature_catalog_text(condition))
            entries = inv.parse_feature_catalog(repo)
        self.assertEqual(len(entries), 1)
        e = entries[0]
        self.assertEqual(e.feature_id, "testFeature")
        self.assertEqual(e.condition_preference_keys, ["key_a", "key_b"])
        self.assertEqual(e.parse_status, "PARSED")

    def test_declared_preference_keys_extraction(self) -> None:
        with tempfile.TemporaryDirectory() as t:
            repo = _make_repo(
                Path(t),
                "public class SystemUiInstaller {}",
                _make_feature_catalog_text("{ prefs -> prefs.getBoolean(\"key_a\") }", 'setOf("key_a", "key_b")'),
            )
            entries = inv.parse_feature_catalog(repo)
        self.assertEqual(entries[0].declared_preference_keys, ["key_a", "key_b"])

    def test_declared_condition_key_difference(self) -> None:
        condition = '{ prefs -> prefs.getBoolean("key_a") || prefs.getBoolean("key_c") }'
        with tempfile.TemporaryDirectory() as t:
            repo = _make_repo(
                Path(t),
                "public class SystemUiInstaller {}",
                _make_feature_catalog_text(condition, 'setOf("key_a", "key_b")'),
            )
            entries = inv.parse_feature_catalog(repo)
        e = entries[0]
        self.assertEqual(e.condition_preference_keys, ["key_a", "key_c"])
        self.assertEqual(e.preference_key_difference, ["key_b", "key_c"])


class AggregationConsistencyTests(unittest.TestCase):
    def _build_inventory_with_statuses(self) -> inv.Inventory:
        installer = """public class SystemUiInstaller {
    private static PrefMap<String, Object> prefs;
    public static void install() {
        if (prefs.unknownMethod("x")) { A(); }
        if (prefs.getBoolean("y") && prefs.unknownMethod("z")) { B(); }
    }
    public static boolean hasAnySystemUiStartupFeature(PrefMap<String, Object> p) { return false; }
    private static boolean hasAnyGlobalAction(PrefMap<String, Object> p) { return false; }
    static boolean isWithinSystemUiRestartGuard(long a, long b) { return b - a < 10000; }
}"""
        with tempfile.TemporaryDirectory() as t:
            repo = _make_repo(Path(t), installer)
            return inv.inventory_from_sources(repo)

    def test_unparsed_aggregation_consistency(self) -> None:
        inv_obj = self._build_inventory_with_statuses()
        data = inv.to_json(inv_obj)
        unparsed_ids = {e["id"] for e in data["UNPARSED"]}
        functional_ids = set()
        for key in inv_obj.__dict__:
            if key in ("unparsed", "partial"):
                continue
            for e in getattr(inv_obj, key):
                if e.parse_status == "UNPARSED":
                    functional_ids.add(e.id)
        self.assertEqual(unparsed_ids, functional_ids)
        self.assertEqual(len(data["UNPARSED"]), data["parse_status_counts"]["UNPARSED"])

    def test_partial_aggregation_consistency(self) -> None:
        inv_obj = self._build_inventory_with_statuses()
        data = inv.to_json(inv_obj)
        partial_ids = {e["id"] for e in data["PARTIAL"]}
        functional_ids = set()
        for key in inv_obj.__dict__:
            if key in ("unparsed", "partial"):
                continue
            for e in getattr(inv_obj, key):
                if e.parse_status == "PARTIAL":
                    functional_ids.add(e.id)
        self.assertEqual(partial_ids, functional_ids)
        self.assertEqual(len(data["PARTIAL"]), data["parse_status_counts"]["PARTIAL"])


class InstallByIdBindingTests(unittest.TestCase):
    def _install_conditions(self, installer: str) -> list[inv.ConditionEntry]:
        with tempfile.TemporaryDirectory() as t:
            repo = _make_repo(Path(t), installer)
            return inv.inventory_from_sources(repo).install_conditions

    def test_sibling_if_does_not_steal_install_by_id(self) -> None:
        installer = """public class SystemUiInstaller {
    public static void install() {
        if (MainModule.mPrefs.getBoolean("a")) FeatureDispatcher.installById("feat_a", runtime);
        if (MainModule.mPrefs.getBoolean("b")) FeatureDispatcher.installById("feat_b", runtime);
    }
    public static boolean hasAnySystemUiStartupFeature(PrefMap<String, Object> p) { return false; }
    private static boolean hasAnyGlobalAction(PrefMap<String, Object> p) { return false; }
    static boolean isWithinSystemUiRestartGuard(long a, long b) { return false; }
}"""
        conds = self._install_conditions(installer)
        by_key = {c.preference_keys[0]: c for c in conds if c.preference_keys}
        self.assertEqual(by_key["a"].feature_id, "feat_a")
        self.assertEqual(by_key["b"].feature_id, "feat_b")

    def test_block_scoped_install_by_id_binding(self) -> None:
        installer = """public class SystemUiInstaller {
    public static void install() {
        if (MainModule.mPrefs.getBoolean("a")) {
            FeatureDispatcher.installById("feat_a", runtime);
        }
    }
    public static boolean hasAnySystemUiStartupFeature(PrefMap<String, Object> p) { return false; }
    private static boolean hasAnyGlobalAction(PrefMap<String, Object> p) { return false; }
    static boolean isWithinSystemUiRestartGuard(long a, long b) { return false; }
}"""
        conds = self._install_conditions(installer)
        self.assertEqual(conds[0].feature_id, "feat_a")

    def test_unconditional_install_by_id_recorded_in_feature_dispatch_calls(self) -> None:
        installer = """public class SystemUiInstaller {
    public static void install() {
        FeatureDispatcher.installById("unconditional_feat", runtime);
        if (MainModule.mPrefs.getBoolean("a")) { A(); }
    }
    public static boolean hasAnySystemUiStartupFeature(PrefMap<String, Object> p) { return false; }
    private static boolean hasAnyGlobalAction(PrefMap<String, Object> p) { return false; }
    static boolean isWithinSystemUiRestartGuard(long a, long b) { return false; }
}"""
        with tempfile.TemporaryDirectory() as t:
            repo = _make_repo(Path(t), installer)
            inv_obj = inv.inventory_from_sources(repo)
        self.assertEqual(len(inv_obj.feature_dispatch_calls), 1)
        self.assertEqual(inv_obj.feature_dispatch_calls[0].feature_id, "unconditional_feat")
        self.assertEqual(inv_obj.install_conditions[0].feature_id, "")

    def test_nested_condition_depth_parent_branch(self) -> None:
        installer = """public class SystemUiInstaller {
    public static void install() {
        if (MainModule.mPrefs.getBoolean("a")) {
            if (MainModule.mPrefs.getBoolean("b")) {
                FeatureDispatcher.installById("feat_b", runtime);
            }
        }
    }
    public static boolean hasAnySystemUiStartupFeature(PrefMap<String, Object> p) { return false; }
    private static boolean hasAnyGlobalAction(PrefMap<String, Object> p) { return false; }
    static boolean isWithinSystemUiRestartGuard(long a, long b) { return false; }
}"""
        conds = self._install_conditions(installer)
        self.assertEqual(len(conds), 2)
        outer = [c for c in conds if c.preference_keys == ["a"]][0]
        inner = [c for c in conds if c.preference_keys == ["b"]][0]
        self.assertEqual(outer.nesting_depth, 0)
        self.assertEqual(inner.nesting_depth, 1)
        self.assertEqual(inner.parent_condition_id, outer.id)
        self.assertEqual(inner.branch_kind, "IF")
        self.assertEqual(outer.feature_id, "")
        self.assertEqual(inner.feature_id, "feat_b")


class PhaseClassificationTests(unittest.TestCase):
    def test_restart_guard_phase_classification(self) -> None:
        data = inv.to_json(inv.inventory_from_sources(REPO_ROOT))
        phases = {c["phase"] for c in data["INSTALL_CONDITIONS"]}
        self.assertIn("PACKAGE_GUARD", phases)
        self.assertIn("PRE_RESTART_GUARD_RESOURCE", phases)
        self.assertIn("PRE_RESTART_GUARD_INFRASTRUCTURE", phases)
        self.assertIn("RESTART_GUARD", phases)
        self.assertIn("POST_RESTART_GUARD_RUNTIME", phases)

    def test_resource_phase_conditions_not_empty(self) -> None:
        data = inv.to_json(inv.inventory_from_sources(REPO_ROOT))
        self.assertGreater(len(data["RESOURCE_PHASE_CONDITIONS"]), 0)
        for entry in data["RESOURCE_PHASE_CONDITIONS"]:
            self.assertEqual(entry["phase"], "PRE_RESTART_GUARD_RESOURCE")

    def test_guard_predicate_in_restart_guard(self) -> None:
        data = inv.to_json(inv.inventory_from_sources(REPO_ROOT))
        self.assertEqual(len(data["RESTART_GUARD"]), 1)
        self.assertIn("currentTime - restartTime < 10000", data["RESTART_GUARD"][0]["raw_expression"])


class DefaultValueTests(unittest.TestCase):
    def test_implicit_default_values_and_kinds(self) -> None:
        cases = [
            ('prefs.getBoolean("x")', "x", False, "IMPLICIT_PREFMAP_DEFAULT"),
            ('prefs.getBoolean("x", true)', "x", True, "EXPLICIT"),
            ('prefs.getBoolean("x", false)', "x", False, "EXPLICIT"),
            ('prefs.getInt("x", 19)', "x", 19, "EXPLICIT"),
            ('prefs.getStringSet("x")', "x", [], "IMPLICIT_PREFMAP_DEFAULT"),
        ]
        for expr, key, value, kind in cases:
            with self.subTest(expr=expr):
                parsed = inv.parse_expression(expr)
                self.assertEqual(parsed.preference_keys, [key])
                self.assertEqual(parsed.default_values, [value])
                self.assertEqual(parsed.default_kinds, [kind])

    def test_unknown_default(self) -> None:
        parsed = inv.parse_expression('prefs.unknown("x")')
        self.assertEqual(parsed.preference_keys, [])
        self.assertEqual(parsed.default_values, [])
        self.assertEqual(parsed.parse_status, "UNPARSED")


class CommentAndStringTests(unittest.TestCase):
    def test_comments_and_string_embedded_if_ignored(self) -> None:
        java = r"""public class Foo {
    public static void install() {
        // if (prefs.getBoolean("fake")) { A(); }
        if (prefs.getBoolean("real")) { B(); }
        String s = "if (prefs.getBoolean(\"fake\"))";
    }
}"""
        body = inv.find_method_body(java, "public static void install")
        self.assertIsNotNone(body)
        conds = inv.extract_conditions(
            body.text,
            "Foo.java",
            "install",
            "TEST",
            full_text=java,
            body_start=body.start_offset,
        )
        self.assertEqual(len(conds), 1)
        self.assertEqual(conds[0].preference_keys, ["real"])


class IntegrationTests(unittest.TestCase):
    def test_inventory_runs_against_repo(self) -> None:
        data = inv.to_json(inv.inventory_from_sources(REPO_ROOT))
        self.assertGreater(len(data["INSTALL_CONDITIONS"]), 0)
        self.assertGreater(len(data["STARTUP_GATE_CONDITIONS"]), 0)
        self.assertGreater(len(data["GLOBAL_ACTION_DOMAIN_RULES"]), 0)
        self.assertGreater(len(data["FEATURE_DISPATCH_CALLS"]), 0)
        self.assertGreater(len(data["FEATURE_CATALOG_GATES"]), 0)
        self.assertGreater(len(data["RESTART_GUARD"]), 0)
        self.assertEqual(data["category_counts"]["RESOURCE_PHASE_CONDITIONS"], 2)

    def test_metadata_fields(self) -> None:
        data = inv.to_json(inv.inventory_from_sources(REPO_ROOT))
        self.assertEqual(data["schema_version"], "1.0")
        self.assertEqual(data["generated_from"], [inv.INSTALLER_REL, inv.CATALOG_REL])
        for status in ("PARSED", "PARTIAL", "UNPARSED"):
            self.assertIn(status, data["parse_status_counts"])
        for key in data["category_counts"]:
            self.assertIn(key, data)


def _line_of(text: str, needle: str) -> int:
    """Return the 1-based line number of the first line containing needle."""
    for i, line in enumerate(text.splitlines(), 1):
        if needle in line:
            return i
    return 0


class DispatcherLineNumberTests(unittest.TestCase):
    def test_unconditional_install_by_id_uses_absolute_file_line(self) -> None:
        """A call inside a method body must report the real file line, not a body-local offset."""
        prefix = "\n" * 100
        installer = prefix + '''public class SystemUiInstaller {
    public static void install(PackageReadyParam lpparam, Runnable watchPreferences) {
        if (!lpparam.getPackageName().equals("com.android.systemui")) return;
        FeatureDispatcher.installById("fixture_feature", runtime);
    }
    public static boolean hasAnySystemUiStartupFeature(PrefMap<String, Object> p) { return false; }
    private static boolean hasAnyGlobalAction(PrefMap<String, Object> p) { return false; }
    static boolean isWithinSystemUiRestartGuard(long a, long b) { return false; }
}'''
        with tempfile.TemporaryDirectory() as t:
            repo = _make_repo(Path(t), installer)
            inv_obj = inv.inventory_from_sources(repo)
        self.assertEqual(len(inv_obj.feature_dispatch_calls), 1)
        entry = inv_obj.feature_dispatch_calls[0]
        self.assertEqual(entry.feature_id, "fixture_feature")
        expected = _line_of(installer, 'FeatureDispatcher.installById("fixture_feature"')
        self.assertGreater(expected, 100)
        self.assertEqual(entry.start_line, expected)
        self.assertEqual(entry.end_line, expected)

    def test_real_feature_dispatcher_calls_have_absolute_source_lines(self) -> None:
        data = inv.to_json(inv.inventory_from_sources(REPO_ROOT))
        installer_text = (REPO_ROOT / inv.INSTALLER_REL).read_text(encoding="utf-8")
        dispatchers = {e["feature_id"]: e for e in data["FEATURE_DISPATCH_CALLS"]}
        for feature_id in (
            "tempHideOverlaySystemUI",
            "hideStatusBarBeforeScreenshot",
            "statusBarClockTweak",
            "noMoreIcon",
            "batteryIndicator",
        ):
            with self.subTest(feature_id=feature_id):
                self.assertIn(feature_id, dispatchers, f"missing dispatcher for {feature_id}")
                entry = dispatchers[feature_id]
                expected = _line_of(installer_text, f'FeatureDispatcher.installById("{feature_id}"')
                self.assertGreater(expected, 0)
                self.assertEqual(entry["start_line"], expected)
                self.assertEqual(entry["end_line"], expected)


if __name__ == "__main__":
    unittest.main()
