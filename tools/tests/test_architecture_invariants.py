#!/usr/bin/env python3
"""Architecture invariants for the A13/A14 alignment."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
SRC = REPO / "app" / "src" / "main" / "java"
MAIN = SRC / "tv" / "withaibuild" / "customiuizer" / "MainModule.java"
INPUT = SRC / "tv" / "withaibuild" / "customiuizer" / "installers" / "InputMethodInstaller.java"
UTILS = SRC / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils"


def read(rel: str) -> str:
    return (SRC / rel.replace("/", "\\")).read_text(encoding="utf-8")


class ArchitectureInvariantTest(unittest.TestCase):
    def test_main_module_delegates_input_method_to_installer(self):
        text = MAIN.read_text(encoding="utf-8")
        self.assertIn("InputMethodInstaller.install(lpparam, pkg);", text)
        self.assertNotIn("VolumeCursorHook(lpparam);", text)
        self.assertNotIn("GboardPaddingHook(lpparam);", text)

    def test_input_method_installer_contains_real_hooks(self):
        text = INPUT.read_text(encoding="utf-8")
        self.assertIn("VolumeCursorHook", text)
        self.assertIn("GboardPaddingHook", text)
        self.assertIn("FixInputMethodBottomMarginHook", text)

    def test_feature_state_model_exists(self):
        text = (UTILS / "FeatureState.kt").read_text(encoding="utf-8")
        for state in ("NOT_INSTALLED", "INSTALLING", "INSTALLED", "FAILED_TRANSIENT", "FAILED_PERMANENT"):
            self.assertIn(state, text)

    def test_feature_install_result_exists(self):
        text = (UTILS / "FeatureInstallResult.kt").read_text(encoding="utf-8")
        for result in ("INSTALLED", "ALREADY_INSTALLED", "SKIPPED", "FAILED_TRANSIENT", "FAILED_PERMANENT"):
            self.assertIn(result, text)
        self.assertIn("isActive", text)

    def test_feature_install_state_exists(self):
        text = (UTILS / "FeatureInstallState.kt").read_text(encoding="utf-8")
        self.assertIn("beginInstall", text)
        self.assertIn("FeatureId", text)
        self.assertIn("@JvmStatic", text)

    def test_main_module_no_direct_launcher_hooks(self):
        text = MAIN.read_text(encoding="utf-8")
        for cls in ("LauncherLayoutHooks", "LauncherSystemHooks", "LauncherAnimationHooks", "LauncherFolderHooks", "LauncherIconHooks"):
            self.assertNotIn(cls, text, f"MainModule still directly references {cls}")

    def test_launcher_installer_contains_package_ready_hooks(self):
        text = (REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "installers" / "LauncherInstaller.java").read_text(encoding="utf-8")
        self.assertIn("installPackageReady", text)
        self.assertIn("HorizontalSpacingRes", text)
        self.assertIn("DisableLauncherLogHook", text)

    def test_process_scope_is_single_source(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        self.assertIn("enum class ProcessScope", text)


if __name__ == "__main__":
    unittest.main()
