#!/usr/bin/env python3
"""Unit tests for a13_systemui_gate_inventory.

Tests use small synthetic fixtures so the parser is not coupled to the full
production source tree.
"""

import json
import sys
import tempfile
import unittest
from pathlib import Path

# Allow importing the tool script from the parent tools/ directory.
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import a13_systemui_gate_inventory as inv


class GateInventoryParserTest(unittest.TestCase):
    def test_single_get_boolean(self) -> None:
        text = 'if (prefs.getBoolean("system_statusbar_mobiletype_single")) { A(); }'
        entries = inv.extract_top_level_ifs(text, "Test.java", "test", "TEST")
        self.assertEqual(len(entries), 1)
        e = entries[0]
        self.assertEqual(e.accessors, ["getBoolean"])
        self.assertEqual(e.preference_keys, ["system_statusbar_mobiletype_single"])
        self.assertEqual(e.default_values, [None])

    def test_get_int_with_default_and_gt(self) -> None:
        text = 'if (mPrefs.getInt("system_statusbarheight", 19) > 19) { A(); }'
        entries = inv.extract_top_level_ifs(text, "Test.java", "test", "TEST")
        self.assertEqual(len(entries), 1)
        e = entries[0]
        self.assertEqual(e.accessors, ["getInt"])
        self.assertEqual(e.preference_keys, ["system_statusbarheight"])
        self.assertEqual(e.default_values, [19])
        self.assertIn(">", e.comparators)

    def test_and_combination(self) -> None:
        text = '''
            if (prefs.getInt("a", 0) > 0 && !prefs.getBoolean("b")) { A(); }
        '''
        entries = inv.extract_top_level_ifs(text, "Test.java", "test", "TEST")
        self.assertEqual(len(entries), 1)
        e = entries[0]
        self.assertIn("AND", e.boolean_operators)
        self.assertIn("NOT", e.boolean_operators)
        self.assertEqual(e.preference_keys, ["a", "b"])

    def test_or_combination(self) -> None:
        text = 'if (prefs.getBoolean("x") || prefs.getStringAsInt("y", 1) > 1) { A(); }'
        entries = inv.extract_top_level_ifs(text, "Test.java", "test", "TEST")
        self.assertEqual(len(entries), 1)
        e = entries[0]
        self.assertIn("OR", e.boolean_operators)
        self.assertEqual(e.accessors, ["getBoolean", "getStringAsInt"])

    def test_nested_parens(self) -> None:
        text = '''
            if ((prefs.getInt("a", 0) > 0) && (prefs.getInt("b", 0) > 0 || prefs.getBoolean("c"))) { A(); }
        '''
        entries = inv.extract_top_level_ifs(text, "Test.java", "test", "TEST")
        self.assertEqual(len(entries), 1)
        e = entries[0]
        self.assertEqual(e.preference_keys, ["a", "b", "c"])

    def test_multiline_if(self) -> None:
        text = '''
            if (
                prefs.getBoolean("system_fivegtile")
                || prefs.getBoolean("system_cc_fpstile")
            ) { A(); }
        '''
        entries = inv.extract_top_level_ifs(text, "Test.java", "test", "TEST")
        self.assertEqual(len(entries), 1)
        e = entries[0]
        self.assertEqual(e.preference_keys, ["system_fivegtile", "system_cc_fpstile"])

    def test_install_by_id_detected(self) -> None:
        text = 'if (true) { FeatureDispatcher.installById("batteryIndicator"); }'
        entries = inv.extract_top_level_ifs(text, "Test.java", "test", "TEST")
        self.assertEqual(entries[0].feature_id, "batteryIndicator")

    def test_feature_catalog_gate_parsed(self) -> None:
        text = '''
            FeatureSpec(
                id = "batteryIndicator",
                processScope = ProcessScope.SYSTEM_UI,
                isEnabled = { prefs -> prefs.getBoolean("system_batteryindicator") },
            ),
        '''
        with tempfile.TemporaryDirectory() as tmp:
            catalog = Path(tmp) / "FeatureCatalog.kt"
            catalog.write_text(text, encoding="utf-8")
            entries = inv.parse_feature_catalog(catalog)
        self.assertEqual(len(entries), 1)
        e = entries[0]
        self.assertEqual(e.feature_id, "batteryIndicator")
        self.assertEqual(e.preference_keys, ["system_batteryindicator"])

    def test_comments_ignored(self) -> None:
        text = '''
            // if (prefs.getBoolean("fake")) { A(); }
            if (prefs.getBoolean("real")) { A(); }
        '''
        entries = inv.extract_top_level_ifs(text, "Test.java", "test", "TEST")
        self.assertEqual(len(entries), 1)
        self.assertEqual(entries[0].preference_keys, ["real"])

    def test_strings_ignored(self) -> None:
        text = 'if (someMethod("if (prefs.getBoolean(\\"fake\\"))")) { A(); }'
        # Should not extract the fake condition inside the string.
        entries = inv.extract_top_level_ifs(text, "Test.java", "test", "TEST")
        self.assertEqual(len(entries), 1)
        self.assertEqual(entries[0].preference_keys, [])

    def test_unparsed_marked(self) -> None:
        text = 'if (prefs.unknownMethod("a")) { A(); }'
        entries = inv.extract_top_level_ifs(text, "Test.java", "test", "TEST")
        self.assertEqual(len(entries), 1)
        # Unknown PrefMap method call should be UNPARSED.
        self.assertEqual(entries[0].parse_status, "UNPARSED")

    def test_restart_guard_separate(self) -> None:
        text = '''
            class Foo {
                static boolean isWithinSystemUiRestartGuard(long restartTime, long currentTime) {
                    return currentTime - restartTime < 10000;
                }
            }
        '''
        body = inv.find_method_body(text, "static boolean isWithinSystemUiRestartGuard")
        self.assertIsNotNone(body)

    def test_resource_phase_condition(self) -> None:
        text = 'if (mPrefs.getInt("system_statusbarheight", 19) > 19) SystemStatusBarAndClockHooks.StatusBarHeightRes();'
        entries = inv.extract_top_level_ifs(text, "Test.java", "test", "PACKAGE_READY_RESOURCE")
        self.assertEqual(entries[0].preference_keys, ["system_statusbarheight"])


class GateInventoryIntegrationTest(unittest.TestCase):
    def test_inventory_runs_against_repo(self) -> None:
        repo_root = Path(__file__).resolve().parent.parent.parent
        inventory = inv.inventory_from_sources(repo_root)
        data = inv.to_json(inventory)

        self.assertGreater(len(data["INSTALL_CONDITIONS"]), 0)
        self.assertGreater(len(data["STARTUP_GATE_CONDITIONS"]), 0)
        self.assertGreater(len(data["FEATURE_DISPATCH_CALLS"]), 0)
        self.assertGreater(len(data["FEATURE_CATALOG_GATES"]), 0)
        self.assertGreater(len(data["RESTART_GUARD"]), 0)

    def test_inventory_deterministic(self) -> None:
        repo_root = Path(__file__).resolve().parent.parent.parent
        _, _, json1, _ = inv.write_inventory(repo_root)
        _, _, json2, _ = inv.write_inventory(repo_root)
        self.assertEqual(json1, json2)


if __name__ == "__main__":
    unittest.main()
