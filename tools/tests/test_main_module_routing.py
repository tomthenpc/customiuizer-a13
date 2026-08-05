#!/usr/bin/env python3
"""Static regression tests for A13 MainModule routing migration."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
MAIN = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "MainModule.java"
REGISTRY = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "prefs" / "PreferenceLoadRegistry.kt"


class MainModuleRoutingTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = MAIN.read_text(encoding="utf-8")
        cls.registry_text = REGISTRY.read_text(encoding="utf-8")

    def test_registry_uses_process_scopes(self):
        self.assertIn("ProcessScopes.isKnownPackage", self.registry_text)

    def test_no_hardcoded_known_package_list(self):
        # The old needLoadPrefs contained a long list of package name checks.
        # After migration it must not contain the duplicated input method list.
        pattern = r'if \"com\.baidu\.input\"\.equals\(pkg\)'
        self.assertIsNone(re.search(pattern, self.text), "MainModule still contains duplicated package list")

    def test_input_method_routed_by_process_scope(self):
        self.assertIn("ProcessScope.INPUT_METHOD", self.text)

    def test_launcher_routed_by_process_scope(self):
        self.assertIn("ProcessScope.LAUNCHER", self.text)

    def test_system_ui_routed_by_process_scope(self):
        self.assertIn("ProcessScope.SYSTEM_UI", self.text)

    def test_process_scope_imported(self):
        self.assertIn("import tv.withaibuild.customiuizer.mods.utils.ProcessScope;", self.text)

    def test_main_module_calls_preference_load_registry(self):
        self.assertIn("PreferenceLoadRegistry.shouldLoad(remote, pkg)", self.text)

    def test_main_module_no_longer_declares_need_load_prefs(self):
        self.assertIsNone(re.search(r"\bneedLoadPrefs\s*\(", self.text), "MainModule still declares needLoadPrefs")

    def test_main_module_no_longer_declares_helper_methods(self):
        for name in ("isPrefEnabled", "isInPrefSet", "isVolumeMediaEnabled"):
            with self.subTest(method=name):
                pattern = re.compile(rf"\b{name}\s*\(")
                self.assertIsNone(pattern.search(self.text), f"MainModule still declares {name}")

    def test_main_module_does_not_contain_preference_keys(self):
        keys = [
            "pref_key_various_alarmcompat",
            "pref_key_various_alarmcompat_apps",
            "pref_key_system_statusbarcolor",
            "pref_key_system_statusbarcolor_apps",
            "pref_key_system_nooverscroll",
            "pref_key_system_nooverscroll_apps",
            "pref_key_controls_volumemedia_up",
            "pref_key_controls_volumemedia_down",
            "pref_key_controls_mediaplayer_apps",
        ]
        for key in keys:
            with self.subTest(key=key):
                self.assertNotIn(key, self.text, f"MainModule still contains preference key literal {key}")

    def test_process_scopes_is_known_package_used_by_registry(self):
        # ProcessScopes.isKnownPackage must still be used, now inside the registry.
        self.assertIn("ProcessScopes.isKnownPackage", self.registry_text)


if __name__ == "__main__":
    unittest.main()
