import tempfile
import unittest
from pathlib import Path
import sys
import csv

from tools import parity_inventory
from tools.parity_inventory import (
    classify_ui_node,
    derive_batch_counts,
    evidence_for_row,
    extract_pref_reads,
    implementation_mode_for,
    missing_semantic_aliases,
    parity_accounting_invariant,
    parse_a14_specs,
    parse_ui_nodes,
    route_phase_e_batch,
)


class ParityInventoryTest(unittest.TestCase):
    def _write(self, root: Path, rel: str, content: str) -> None:
        p = root / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(content, encoding="utf-8")

    def test_category_and_navigation_not_product_feature(self):
        self.assertEqual(classify_ui_node("PreferenceCategory", "system_cat"), "CATEGORY")
        self.assertEqual(classify_ui_node("PreferenceScreen", "system"), "NAVIGATION_ENTRY")

    def test_same_key_stays_insufficient_without_semantic_proof(self):
        level = evidence_for_row("k", True, {"k"}, {"k"})
        self.assertEqual(level, "IMPLEMENTATION_PRESENCE")

    def test_kotlin_and_java_preference_discovery(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            self._write(
                root,
                "app/src/main/java/demo/A.kt",
                'fun x(p: Any){ prefs.getBoolean("kt_key"); val s = "ignore" }',
            )
            self._write(
                root,
                "app/src/main/java/demo/B.java",
                'class B { void x(){ prefs.getString("java_key"); } }',
            )
            keys = extract_pref_reads(root)
            self.assertIn("kt_key", keys)
            self.assertIn("java_key", keys)

    def test_parse_ui_nodes_with_fixture_typing(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            self._write(
                root,
                "app/src/main/res/xml/prefs_demo.xml",
                """<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android">
                    <PreferenceCategory android:key="pref_key_demo_cat" />
                    <PreferenceScreen android:key="pref_key_system" />
                    <CheckBoxPreferenceEx android:key="pref_key_demo_toggle" />
                </PreferenceScreen>""",
            )
            self._write(root, "app/src/main/res/values/strings.xml", "<resources/>")
            nodes, _ = parse_ui_nodes(root)
            self.assertEqual(nodes["demo_cat"].node_type, "CATEGORY")
            self.assertEqual(nodes["system"].node_type, "NAVIGATION_ENTRY")
            self.assertEqual(nodes["demo_toggle"].node_type, "ACTIONABLE_FEATURE")

    def test_multi_key_feature_spec_supported(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            self._write(
                root,
                "app/src/main/java/demo/F.kt",
                """
                val x = LazyFeatureSpec(
                    id = DemoFeatureId,
                    name = "Demo",
                    preferenceKey = "demo_main",
                    preferenceKeys = listOf("demo_main", "demo_extra"),
                    target = FeatureTarget.SYSTEM_UI
                ),
                """,
            )
            specs, discovered, unknown = parse_a14_specs(root)
            self.assertIn("demo_main", specs)
            self.assertIn("demo_extra", specs)
            self.assertEqual(discovered, 1)
            self.assertEqual(unknown, 0)

    def test_phase_e_routing_by_host_process(self):
        self.assertEqual(route_phase_e_batch("SYSTEM_UI", "com.android.systemui", "k", "n", "MISSING_IN_A13"), "E3")
        self.assertEqual(route_phase_e_batch("LAUNCHER", "com.miui.home", "k", "n", "MISSING_IN_A13"), "E3")
        self.assertEqual(route_phase_e_batch("SECURITY_CENTER", "com.miui.securitycenter", "k", "n", "MISSING_IN_A13"), "E4")
        self.assertEqual(route_phase_e_batch("PACKAGE_INSTALLER", "com.google.android.packageinstaller", "k", "n", "MISSING_IN_A13"), "E4")
        self.assertEqual(route_phase_e_batch("SYSTEM_PACKAGE", "android.system.package", "various_installer_purify", "Package Installer Purify", "MISSING_IN_A13"), "E4")
        self.assertEqual(route_phase_e_batch("SYSTEM_SERVER", "android", "k", "n", "MISSING_IN_A13"), "E5")
        self.assertEqual(route_phase_e_batch("SETTINGS", "com.android.settings", "infra.backup_restore", "Backup / Restore", "MISSING_IN_A13"), "E1")
        self.assertEqual(route_phase_e_batch("SETTINGS", "com.android.settings", "generic_low_risk", "Generic", "MISSING_IN_A13"), "E2")
        self.assertEqual(route_phase_e_batch("SYSTEM_UI", "com.android.systemui", "controls_hide_ime_dismiss_button", "Hide IME", "MISSING_IN_A13"), "E3")

    def test_batch_count_and_invariant_and_dynamic_once(self):
        rows = [
            {"a14_feature_id": "f1", "parity_state": "MISSING_IN_A13", "phase_e_batch": "E1"},
            {"a14_feature_id": "f2", "parity_state": "PARTIAL_PARITY", "phase_e_batch": "E4"},
            {"a14_feature_id": "f3", "parity_state": "HOLD_EVIDENCE", "phase_e_batch": "HOLD_EVIDENCE"},
            {"a14_feature_id": "", "parity_state": "A13_ONLY_KEEP", "phase_e_batch": ""},
            {"a14_feature_id": "f4", "parity_state": "INTENTIONAL_EXCLUDED", "phase_e_batch": ""},
        ]
        c = derive_batch_counts(rows)
        self.assertEqual(c["E1"], 1)
        self.assertEqual(c["E4"], 1)
        self.assertTrue(parity_accounting_invariant(rows))
        self.assertEqual(sum(1 for r in rows if r["parity_state"] == "INTENTIONAL_EXCLUDED"), 1)

    def test_implementation_mode_values(self):
        self.assertEqual(implementation_mode_for("MISSING_IN_A13", "E3", False), "NEW_PORT")
        self.assertEqual(implementation_mode_for("PARTIAL_PARITY", "E5", True), "UPGRADE_EXISTING_A13")
        self.assertEqual(implementation_mode_for("MISSING_IN_A13", "HOLD_EVIDENCE", False), "EVIDENCE_HOLD")
        self.assertEqual(implementation_mode_for("PRESENT_A13_VARIANT", "", False), "NO_IMPLEMENTATION")

    def test_missing_alias_map_has_usb_regression(self):
        aliases = missing_semantic_aliases()
        self.assertIn("system_usb_default_function", aliases)
        self.assertIn("system_defaultusb", aliases["system_usb_default_function"]["a13_keys"])
        self.assertEqual(aliases["system_usb_default_function"]["parity_state"], "PRESENT_A13_VARIANT")

    def test_d_final_aliases_cover_known_false_missing(self):
        aliases = missing_semantic_aliases()
        self.assertEqual(aliases["launcher_folderblur_disable"]["parity_state"], "PARTIAL_PARITY")
        self.assertEqual(aliases["system_netspeed_boldfont"]["parity_state"], "PRESENT_A13_VARIANT")
        self.assertEqual(aliases["system_statusbarcontrols_dt_left"]["parity_state"], "HOLD_EVIDENCE")
        self.assertEqual(aliases["system_statusbarcontrols_dt_right"]["parity_state"], "HOLD_EVIDENCE")
        self.assertEqual(aliases["system_charginginfo_fontsize"]["parity_state"], "PARTIAL_PARITY")
        self.assertEqual(aliases["system_charginginfo_fontsize"]["host_package"], "SYSTEM_UI")
        self.assertEqual(aliases["system_strong_toast_island_offset"]["phase_e_batch"], "HOLD_EVIDENCE")

    def test_absence_proof_is_feature_specific(self):
        index = {
            "app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt":
                'val opt = MainModule.mPrefs.getInt("launcher_dock_topmargin", 0)',
            "app/src/main/res/xml/prefs_launcher.xml":
                'android:key="pref_key_launcher_dock_topmargin"',
        }
        proof = parity_inventory.build_absence_proof(
            "launcher_dock_height",
            "LauncherDockHeightFeatureId",
            "Launcher Dock Height",
            index,
            {"launcher_dock_topmargin": None},
            {"launcher_dock_topmargin"},
            ["launcher_dock_topmargin"],
        )
        self.assertIn("launcher_dock_height", proof)
        self.assertIn("hotseat/dock height", proof)
        self.assertNotIn("Checked A13 UI keys, PreferenceSchema-linked keys", proof)

    def test_partial_counts_as_gap_present_does_not(self):
        rows = [
            {"parity_state": "PARTIAL_PARITY", "phase_e_batch": "E3"},
            {"parity_state": "PRESENT_A13_VARIANT", "phase_e_batch": "E3"},
            {"parity_state": "MISSING_IN_A13", "phase_e_batch": "HOLD_EVIDENCE"},
        ]
        c = derive_batch_counts(rows)
        self.assertEqual(c["E3"], 1)

    def test_hold_evidence_excluded_from_ready_gaps(self):
        rows = [
            {"parity_state": "MISSING_IN_A13", "phase_e_batch": "HOLD_EVIDENCE"},
            {"parity_state": "PARTIAL_PARITY", "phase_e_batch": "E4"},
        ]
        c = derive_batch_counts(rows)
        ready = sum(c.get(k, 0) for k in ["E1", "E2", "E3", "E4", "E5"])
        self.assertEqual(ready, 1)

    def test_usb_alias_regression_not_missing(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            a13 = root / "a13"
            a14 = root / "a14"
            out = root / "out"
            self._write(a14, "app/src/main/res/values/strings.xml", "<resources/>")
            self._write(a13, "app/src/main/res/values/strings.xml", "<resources/>")
            self._write(
                a14,
                "app/src/main/res/xml/prefs_system.xml",
                """<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android">
                    <CheckBoxPreferenceEx android:key="pref_key_system_usb_default_function" />
                </PreferenceScreen>""",
            )
            self._write(
                a13,
                "app/src/main/res/xml/prefs_system.xml",
                """<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android">
                    <CheckBoxPreferenceEx android:key="pref_key_system_defaultusb" />
                </PreferenceScreen>""",
            )
            self._write(
                a14,
                "app/src/main/java/demo/F.kt",
                """
                val f = LazyFeatureSpec(
                    id = UsbDefaultFunctionFeatureId,
                    name = "USB Default Function",
                    preferenceKey = "system_usb_default_function",
                    target = FeatureTarget.SYSTEM_SERVER
                )
                """,
            )
            self._write(
                a13,
                "app/src/main/java/demo/Usb.kt",
                'class U { fun x(){ prefs.getString("system_defaultusb") } }',
            )
            old = sys.argv
            try:
                sys.argv = [
                    "parity_inventory.py",
                    "--a13-repo", str(a13),
                    "--a14-repo", str(a14),
                    "--out-dir", str(out),
                ]
                parity_inventory.main()
            finally:
                sys.argv = old
            rows = list(csv.DictReader((out / "A13_A14_FEATURE_MATRIX.csv").open(encoding="utf-8")))
            usb = next(r for r in rows if r["a14_pref_keys"] == "system_usb_default_function")
            self.assertEqual(usb["parity_state"], "PRESENT_A13_VARIANT")
            self.assertEqual(usb["implementation_mode"], "NO_IMPLEMENTATION")


if __name__ == "__main__":
    unittest.main()

