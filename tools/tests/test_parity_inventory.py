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
from tools.parity_phase_f import (
    DeadPathProof,
    PhaseFTransitionInput,
    ProofManifest,
    SourceOwner,
    classify_phase_f_transition,
    classify_unproven_bucket,
    fingerprint_proof_for_key,
    source_review_variant_for_pair,
)


class ParityInventoryTest(unittest.TestCase):
    def _write(self, root: Path, rel: str, content: str) -> None:
        p = root / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(content, encoding="utf-8")

    def test_category_and_navigation_not_product_feature(self):
        self.assertEqual(classify_ui_node("PreferenceCategory", "system_cat"), "CATEGORY")
        self.assertEqual(classify_ui_node("PreferenceScreen", "system"), "NAVIGATION")

    def test_hidden_warning_is_not_product_feature(self):
        self.assertEqual(
            classify_ui_node("PreferenceEx", "warning", visible="false", warning="true"),
            "HIDDEN_HELPER",
        )
        decision = classify_phase_f_transition(PhaseFTransitionInput(
            key="warning",
            node_type="HIDDEN_HELPER",
            a14_read=False,
            a13_read=False,
        ))
        self.assertFalse(decision.product_feature)
        self.assertEqual(decision.parity_state, "NOT_PRODUCT_FEATURE")

    def test_dynamic_island_helper_is_not_product_feature(self):
        self.assertEqual(
            classify_ui_node("SeekBarPreference", "system_strong_toast_island_offset"),
            "DYNAMIC_ISLAND_HELPER",
        )
        decision = classify_phase_f_transition(PhaseFTransitionInput(
            key="system_strong_toast_island_offset",
            node_type="DYNAMIC_ISLAND_HELPER",
        ))
        self.assertFalse(decision.product_feature)
        self.assertNotEqual(decision.parity_state, "HOLD_EVIDENCE")

    def test_same_key_without_source_proof_is_not_present(self):
        decision = classify_phase_f_transition(PhaseFTransitionInput(
            key="system_demo",
            node_type="PRODUCT_ACTION",
            a14_read=True,
            a13_read=True,
            host_package="SYSTEM_UI",
            hook_behavior_match=True,
            source_proof=None,
        ))
        self.assertNotIn(decision.parity_state, {"PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT"})
        self.assertEqual(decision.evidence_level, "IMPLEMENTATION_PRESENCE")

    def test_same_key_different_hook_behavior_is_not_present(self):
        decision = classify_phase_f_transition(PhaseFTransitionInput(
            key="system_demo",
            node_type="PRODUCT_ACTION",
            a14_read=True,
            a13_read=True,
            host_package="SYSTEM_UI",
            hook_behavior_match=False,
        ))
        self.assertNotIn(decision.parity_state, {"PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT"})

    def test_verified_source_family_proof_is_present_variant(self):
        proof = ProofManifest(
            proof_id="PROOF_TEST_FAMILY",
            a14_owner_path="mods/A.kt",
            a14_symbol="FooHook",
            a14_installer="A14Installer",
            a14_hook_targets="Bar#baz",
            a14_callback_phase="after",
            a13_owner_path="mods/A.kt",
            a13_symbol="FooHook",
            a13_installer="A13Installer",
            a13_hook_targets="Bar#baz",
            a13_callback_phase="after",
            preference_keys=("system_demo",),
            value_domain="boolean",
            default_semantics="false",
            result_argument_behavior="skip",
            api33_variant_reason="same member",
            proof_conclusion="PRESENT_A13_VARIANT",
        )
        decision = classify_phase_f_transition(PhaseFTransitionInput(
            key="system_demo",
            node_type="PRODUCT_ACTION",
            a14_read=True,
            a13_read=True,
            host_package="SYSTEM_UI",
            source_proof=proof,
        ))
        self.assertEqual(decision.parity_state, "PRESENT_A13_VARIANT")
        self.assertEqual(decision.proof_id, "PROOF_TEST_FAMILY")

    def test_regex_read_miss_is_not_dead_upstream(self):
        decision = classify_phase_f_transition(PhaseFTransitionInput(
            key="system_orphan",
            node_type="PRODUCT_ACTION",
            a14_read=False,
            a13_read=False,
            dead_proof=None,
        ))
        self.assertNotEqual(decision.parity_state, "DEAD_UPSTREAM_PATH")

    def test_explicit_dead_proof_is_dead_upstream(self):
        dead = DeadPathProof(
            key="system_orphan",
            a14_ui_reference="prefs_system.xml",
            a14_search_references="xml/strings only",
            a14_nearest_candidate="system_related",
            why_not_reachable="UI exists; no FeatureSpec/installer/hook/alias",
        )
        decision = classify_phase_f_transition(PhaseFTransitionInput(
            key="system_orphan",
            node_type="PRODUCT_ACTION",
            a14_read=False,
            dead_proof=dead,
        ))
        self.assertEqual(decision.parity_state, "DEAD_UPSTREAM_PATH")

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
                """<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android"
                    xmlns:app="http://schemas.android.com/apk/res-auto"
                    xmlns:miuizer="http://schemas.android.com/apk/res-auto">
                    <PreferenceCategory android:key="pref_key_demo_cat" />
                    <PreferenceScreen android:key="pref_key_system" />
                    <CheckBoxPreferenceEx android:key="pref_key_demo_toggle" />
                    <PreferenceEx android:key="pref_key_warning" app:isPreferenceVisible="false" miuizer:warning="true" />
                    <SeekBarPreference android:key="pref_key_system_strong_toast_island_offset" />
                    <PreferenceEx android:key="pref_key_controls_fsg_horiz_apps" android:title="@string/apps" miuizer:countAsSummary="true" />
                    <PreferenceEx android:key="pref_key_system_netspeed_prerequisite" android:title="@string/note" android:selectable="false" android:persistent="false" />
                </PreferenceScreen>""",
            )
            self._write(root, "app/src/main/res/values/strings.xml", "<resources/>")
            nodes, _ = parse_ui_nodes(root)
            self.assertEqual(nodes["demo_cat"].node_type, "CATEGORY")
            self.assertEqual(nodes["system"].node_type, "NAVIGATION")
            self.assertEqual(nodes["demo_toggle"].node_type, "PRODUCT_ACTION")
            self.assertEqual(nodes["warning"].node_type, "HIDDEN_HELPER")
            self.assertEqual(nodes["system_strong_toast_island_offset"].node_type, "DYNAMIC_ISLAND_HELPER")
            self.assertEqual(nodes["controls_fsg_horiz_apps"].node_type, "PRODUCT_SUBOPTION")
            self.assertEqual(nodes["system_netspeed_prerequisite"].node_type, "DEPENDENCY_HELPER")

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
        self.assertEqual(aliases["system_netspeed_boldfont"]["parity_state"], "PRESENT_A13_VARIANT")
        self.assertEqual(aliases["system_statusbarcontrols_dt_left"]["parity_state"], "HOLD_EVIDENCE")
        self.assertEqual(aliases["system_statusbarcontrols_dt_right"]["parity_state"], "HOLD_EVIDENCE")
        self.assertNotIn("system_strong_toast_island_offset", aliases)
        self.assertNotIn("launcher_folderblur_disable", aliases)

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


class PhaseFR2ClassifierTest(unittest.TestCase):
    def _owner(self, **kwargs) -> SourceOwner:
        base = dict(
            path="app/src/main/java/tv/withaibuild/customiuizer/mods/Demo.kt",
            symbol="DemoHook",
            kind="hook",
            hook_targets=("com.android.systemui.Foo#bar",),
            callback_phases=("after",),
            keys=("system_demo",),
            normalized_body='{ getBoolean("system_demo", false); setResult(true) }',
        )
        base.update(kwargs)
        return SourceOwner(**base)

    def test_visible_preferenceex_apps_is_product_suboption(self):
        self.assertEqual(
            classify_ui_node(
                "tv.withaibuild.customiuizer.prefs.PreferenceEx",
                "controls_fsg_horiz_apps",
                title="Apps",
                count_as_summary="true",
            ),
            "PRODUCT_SUBOPTION",
        )
        self.assertEqual(
            classify_ui_node(
                "PreferenceEx",
                "controls_volumecursor_apps",
                title="@string/controls_volumecursor_apps_title",
            ),
            "PRODUCT_SUBOPTION",
        )
        self.assertEqual(
            classify_ui_node(
                "PreferenceEx",
                "controls_mediaplayer_apps",
                title="Media player apps",
            ),
            "PRODUCT_SUBOPTION",
        )

    def test_hidden_internal_apps_is_helper_only_with_evidence(self):
        self.assertEqual(
            classify_ui_node(
                "PreferenceEx",
                "system_hidden_apps",
                title="",
                selectable="false",
                persistent="false",
            ),
            "DEPENDENCY_HELPER",
        )
        self.assertEqual(
            classify_ui_node(
                "CheckBoxPreferenceEx",
                "various_disable_defraud_apps_detect",
                title="Disable defraud detect",
            ),
            "PRODUCT_ACTION",
        )
        self.assertEqual(
            classify_ui_node(
                "CheckBoxPreferenceEx",
                "controls_fingerprintsuccess_ignore",
                title="Ignore when screen off",
            ),
            "PRODUCT_ACTION",
        )

    def test_same_symbol_different_body_is_not_automatic_present(self):
        a14 = self._owner(normalized_body='{ setResult(true); getBoolean("system_demo", false) }')
        a13 = self._owner(normalized_body='{ setResult(false); getBoolean("system_demo", false) }')
        man = fingerprint_proof_for_key(
            "system_demo",
            {"system_demo": [a14]},
            {"system_demo": [a13]},
        )
        self.assertIsNone(man)

    def test_same_hook_target_different_result_not_present_without_reviewed_manifest(self):
        a14 = self._owner(normalized_body='{ setResult(true) }')
        a13 = self._owner(normalized_body='{ setResult(false) }')
        auto = fingerprint_proof_for_key(
            "system_demo",
            {"system_demo": [a14]},
            {"system_demo": [a13]},
        )
        self.assertIsNone(auto)
        reviewed = source_review_variant_for_pair(
            "system_demo", a14, a13, [a14], [a13], ("system_demo",),
        )
        self.assertIsNone(reviewed)
        decision = classify_phase_f_transition(PhaseFTransitionInput(
            key="system_demo",
            node_type="PRODUCT_ACTION",
            a14_read=True,
            a13_read=True,
            host_package="SYSTEM_UI",
            hook_behavior_match=True,
            source_proof=None,
        ))
        self.assertNotIn(decision.parity_state, {"PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT"})

    def test_identical_normalized_owner_allows_automatic_proof(self):
        body = '{ getBoolean("system_demo", false); setResult(true) }'
        a14 = self._owner(normalized_body=body)
        a13 = self._owner(normalized_body=body, path="app/src/main/java/tv/withaibuild/customiuizer/mods/Demo.kt")
        man = fingerprint_proof_for_key(
            "system_demo",
            {"system_demo": [a14]},
            {"system_demo": [a13]},
        )
        self.assertIsNotNone(man)
        self.assertEqual(man.body_relation, "IDENTICAL")
        self.assertEqual(man.proof_conclusion, "PRESENT_EQUIVALENT")

    def test_reviewed_variant_manifest_allows_present_a13_variant(self):
        proof = ProofManifest(
            proof_id="PROOF_REVIEWED_TEST",
            a14_owner_path="mods/A.kt",
            a14_symbol="FooHook",
            a14_installer="A14Installer",
            a14_hook_targets="Bar#baz",
            a14_callback_phase="intercept",
            a13_owner_path="mods/A.kt",
            a13_symbol="FooHook",
            a13_installer="A13Installer",
            a13_hook_targets="Bar#baz",
            a13_callback_phase="after",
            preference_keys=("system_demo",),
            value_domain="boolean",
            default_semantics="false",
            result_argument_behavior="A14 skip true; A13 setResult true",
            api33_variant_reason="API33 before/after vs intercept, same skip",
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
            body_relation="REVIEWED_VARIANT",
            diff_summary="A14 intercept/proceed vs A13 after setResult; inner getBoolean key identical",
            value_default_comparison="both default false",
            hook_target_comparison="A14=Bar#baz; A13=Bar#baz",
            callback_semantics_comparison="intercept proceed-once vs after setResult",
            arg_result_comparison="both force true on the same member",
            a14_only_branches="none",
            why_user_behavior_is_equivalent="Same host member skip; callback adapter only",
        )
        decision = classify_phase_f_transition(PhaseFTransitionInput(
            key="system_demo",
            node_type="PRODUCT_ACTION",
            a14_read=True,
            a13_read=True,
            host_package="SYSTEM_UI",
            source_proof=proof,
        ))
        self.assertEqual(decision.parity_state, "PRESENT_A13_VARIANT")

    def test_unmatched_module_owned_settings_is_source_review_not_rom_hold(self):
        bucket = classify_unproven_bucket(
            "various_aospnotif",
            host_package="SETTINGS",
            has_a13=True,
            a14_owner_found=False,
            a13_owner_found=False,
            in_rom_hold_map=False,
        )
        self.assertEqual(bucket, "SOURCE_REVIEW_REQUIRED")
        self.assertNotEqual(bucket, "ROM_DEVICE_HOLD")
        decision = classify_phase_f_transition(PhaseFTransitionInput(
            key="various_aospnotif",
            node_type="PRODUCT_ACTION",
            a14_read=True,
            a13_read=True,
            host_package="SETTINGS",
            source_proof=None,
            unproven_bucket="SOURCE_REVIEW_REQUIRED",
        ))
        self.assertEqual(decision.parity_state, "SOURCE_REVIEW_REQUIRED")
        self.assertNotEqual(decision.proof_id, "PROOF_ROM_DEVICE_HOLD")

    def test_miuizer_locale_is_not_rom_device_hold(self):
        bucket = classify_unproven_bucket(
            "miuizer_locale",
            host_package="SETTINGS",
            has_a13=True,
            a14_owner_found=False,
            a13_owner_found=False,
            in_rom_hold_map=False,
        )
        self.assertEqual(bucket, "SOURCE_REVIEW_REQUIRED")
        self.assertNotEqual(bucket, "ROM_DEVICE_HOLD")
        decision = classify_phase_f_transition(PhaseFTransitionInput(
            key="miuizer_locale",
            node_type="PRODUCT_SUBOPTION",
            a14_read=True,
            a13_read=True,
            host_package="SETTINGS",
            unproven_bucket="SOURCE_REVIEW_REQUIRED",
        ))
        self.assertNotEqual(decision.parity_state, "HOLD_EVIDENCE")
        self.assertNotEqual(decision.proof_id, "PROOF_ROM_DEVICE_HOLD")


if __name__ == "__main__":
    unittest.main()

