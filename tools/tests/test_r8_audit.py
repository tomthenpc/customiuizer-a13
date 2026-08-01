#!/usr/bin/env python3
"""Tests for the static R8/ProGuard audit."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from r8_audit import _parse_build_types, _parse_dependencies, _parse_proguard, audit

REPO = Path(__file__).resolve().parent.parent.parent
APP = REPO / "app"


class R8AuditTest(unittest.TestCase):
    def test_proguard_has_xposed_module_keep(self):
        rules = _parse_proguard(APP / "proguard-rules.pro")
        self.assertTrue(any("XposedModule" in r for r in rules))

    def test_proguard_has_hooker_keep(self):
        rules = _parse_proguard(APP / "proguard-rules.pro")
        self.assertTrue(any("Hooker" in r for r in rules))

    def test_build_types_differ_by_variant(self):
        text = (APP / "build.gradle.kts").read_text(encoding="utf-8")
        types = _parse_build_types(text)
        self.assertIn("debug", types)
        self.assertIn("release", types)
        self.assertFalse(types["debug"]["minify"])
        self.assertTrue(types["release"]["minify"])

    def test_dependencies_include_commons_lang3(self):
        text = (APP / "build.gradle.kts").read_text(encoding="utf-8")
        deps = _parse_dependencies(text)
        self.assertTrue(any("commons" in d for d in deps))

    def test_full_audit_runs(self):
        report = audit(APP / "build.gradle.kts", APP / "proguard-rules.pro")
        self.assertTrue(report.xposed_entry_retained)
        self.assertTrue(report.hooker_retained)
        self.assertIn("release", report.build_types)


if __name__ == "__main__":
    unittest.main()
