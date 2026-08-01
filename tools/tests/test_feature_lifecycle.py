#!/usr/bin/env python3
"""Static tests for the A13 FeatureLifecycle / FeatureInstallResult model."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
SRC = REPO / "app" / "src" / "main" / "java"


def read(rel: str) -> str:
    return (SRC / rel.replace("/", "\\")).read_text(encoding="utf-8")


class FeatureLifecycleTests(unittest.TestCase):
    def test_lifecycle_enum_has_all_required_states(self):
        text = read("tv/withaibuild/customiuizer/mods/catalog/FeatureLifecycle.kt")
        for state in (
            "DISABLED",
            "UNSUPPORTED_PROCESS",
            "INCOMPATIBLE",
            "READY",
            "INSTALLING",
            "INSTALLED",
            "ALREADY_INSTALLED",
            "FAILED_TRANSIENT",
            "FAILED_PERMANENT",
        ):
            self.assertRegex(text, rf"\b{state}\b")

    def test_lifecycle_exposes_install_result_data_class(self):
        text = read("tv/withaibuild/customiuizer/mods/catalog/FeatureLifecycle.kt")
        self.assertIn("data class FeatureInstallResult(", text)
        self.assertIn("val lifecycle: FeatureLifecycle", text)
        self.assertIn("val diagnostic: DiagnosticState", text)

    def test_lifecycle_mapping_covers_diagnostic_states(self):
        text = read("tv/withaibuild/customiuizer/mods/catalog/FeatureLifecycle.kt")
        self.assertIn("DiagnosticState.DISABLED", text)
        self.assertIn("DiagnosticState.INCOMPATIBLE", text)
        self.assertIn("DiagnosticState.INSTALLED", text)
        self.assertIn("DiagnosticState.FAILED", text)

    def test_lifecycle_mapping_covers_process_scopes(self):
        text = read("tv/withaibuild/customiuizer/mods/catalog/FeatureLifecycle.kt")
        self.assertIn("fromProcessScope", text)
        self.assertIn("ProcessScope.SYSTEM_UI", text)
        self.assertIn("ProcessScope.LAUNCHER", text)

    def test_lifecycle_no_android_runtime_dependency(self):
        text = read("tv/withaibuild/customiuizer/mods/catalog/FeatureLifecycle.kt")
        self.assertNotIn("import android.", text)


if __name__ == "__main__":
    unittest.main()
