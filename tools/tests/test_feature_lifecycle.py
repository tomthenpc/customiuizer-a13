#!/usr/bin/env python3
"""Static tests for the A13 FeatureInstallResult / FeatureState lifecycle model."""
import re
import unittest
from pathlib import Path, PurePosixPath

REPO = Path(__file__).resolve().parent.parent.parent
SRC = REPO / "app" / "src" / "main" / "java"


def read(rel: str) -> str:
    return SRC.joinpath(*PurePosixPath(rel).parts).read_text(encoding="utf-8")


class FeatureLifecycleTests(unittest.TestCase):
    def test_lifecycle_exposes_all_install_outcomes(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/FeatureInstallResult.kt")
        for state in (
            "Installed",
            "AlreadyInstalled",
            "Disabled",
            "UnsupportedProcess",
            "WrongPhase",
            "Incompatible",
            "FailedTransient",
            "FailedPermanent",
        ):
            self.assertIn(state, text, f"FeatureInstallResult subtype {state} is missing")

    def test_lifecycle_exposes_install_result_data_class(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/FeatureInstallResult.kt")
        self.assertIn("sealed interface FeatureInstallResult", text)
        self.assertIn("val isActive", text)
        self.assertIn("toDiagnosticState", text)

    def test_lifecycle_mapping_covers_diagnostic_states(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/FeatureInstallResult.kt")
        self.assertIn("DiagnosticState.DISABLED", text)
        self.assertIn("DiagnosticState.INCOMPATIBLE", text)
        self.assertIn("DiagnosticState.INSTALLED", text)
        self.assertIn("DiagnosticState.FAILED", text)

    def test_lifecycle_mapping_covers_process_scopes(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        self.assertIn("SYSTEM_UI", text)
        self.assertIn("LAUNCHER", text)

    def test_lifecycle_no_android_runtime_dependency(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/FeatureInstallResult.kt")
        self.assertNotIn("import android.", text)


if __name__ == "__main__":
    unittest.main()
