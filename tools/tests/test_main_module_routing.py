#!/usr/bin/env python3
"""Static regression tests for A13 MainModule routing migration."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
MAIN = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "MainModule.java"


class MainModuleRoutingTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = MAIN.read_text(encoding="utf-8")

    def test_need_load_prefs_uses_process_scopes(self):
        self.assertIn("ProcessScopes.isKnownPackage(pkg)", self.text)

    def test_no_hardcoded_known_package_list(self):
        # The old needLoadPrefs contained a long list of package name checks.
        # After migration it must not contain the duplicated input method list.
        pattern = r'if \(\"com\.baidu\.input\"\.equals\(pkg\)'
        self.assertIsNone(re.search(pattern, self.text), "MainModule still contains duplicated package list")

    def test_input_method_routed_by_process_scope(self):
        self.assertIn("ProcessScope.INPUT_METHOD", self.text)

    def test_launcher_routed_by_process_scope(self):
        self.assertIn("ProcessScope.LAUNCHER", self.text)

    def test_system_ui_routed_by_process_scope(self):
        self.assertIn("ProcessScope.SYSTEM_UI", self.text)

    def test_process_scope_imported(self):
        self.assertIn("import tv.withaibuild.customiuizer.mods.utils.ProcessScope;", self.text)


if __name__ == "__main__":
    unittest.main()
