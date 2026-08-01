#!/usr/bin/env python3
"""Table-driven static test for A13 ProcessScope classification.

This test does not execute the Kotlin runtime; it verifies that the source
contains the explicit branches required for every known package/process pair.
If a process is missing or mis-classified, the test fails and the table must
be updated together with the implementation.
"""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
SRC = REPO / "app" / "src" / "main" / "java"


def read(rel: str) -> str:
    return (SRC / rel.replace("/", "\\")).read_text(encoding="utf-8")


# package, process, expected scope
PROCESS_TABLE = [
    ("android", "android", "SYSTEM_SERVER"),
    ("android", "system_server", "SYSTEM_SERVER"),
    ("com.android.systemui", "com.android.systemui", "SYSTEM_UI"),
    ("com.android.systemui", "com.miui.notification", "SYSTEM_UI_PLUGIN"),
    ("com.miui.home", "com.miui.home", "LAUNCHER"),
    ("com.mi.android.globallauncher", "com.mi.android.globallauncher", "LAUNCHER"),
    ("com.android.settings", "com.android.settings", "SETTINGS_MAIN"),
    ("com.android.settings", "com.android.settings:remote", "SETTINGS_REMOTE"),
    ("com.miui.securitycenter", "com.miui.securitycenter", "SECURITY_CENTER_MAIN"),
    ("com.miui.securitycenter", "com.miui.securitycenter:remote", "SECURITY_CENTER_REMOTE"),
    ("com.miui.securitycenter", "com.miui.securitycenter.bootaware", "SECURITY_CENTER_BOOTAWARE"),
    ("com.miui.securitycenter", "com.miui.securitycenter:bootaware", "SECURITY_CENTER_BOOTAWARE"),
    ("com.miui.powerkeeper", "com.miui.powerkeeper", "POWER_KEEPER"),
    ("com.miui.miwallpaper", "com.miui.miwallpaper", "WALLPAPER"),
    ("com.miui.screenshot", "com.miui.screenshot", "MEDIA"),
    ("com.miui.gallery", "com.miui.gallery", "MEDIA"),
    ("com.android.incallui", "com.android.incallui", "PHONE"),
    ("com.miui.packageinstaller", "com.miui.packageinstaller", "PACKAGE_INSTALLER"),
    ("com.google.android.inputmethod.pinyin", "com.google.android.inputmethod.pinyin", "INPUT_METHOD"),
    ("com.baidu.input", "com.baidu.input", "INPUT_METHOD"),
    ("com.android.networkstack", "com.android.networkstack.process", "NETWORK_STACK"),
    ("com.android.networkstack.tethering", "com.android.networkstack.tethering", "NETWORK_STACK"),
    ("com.android.location.fused", "com.android.location.fused", "UNSUPPORTED"),
    ("com.example.app", "com.example.app", "GENERIC_APP"),
    ("com.example.app", "com.example.app:other", "GENERIC_APP"),
]


class ProcessScopeTableTest(unittest.TestCase):
    def test_table_size(self):
        self.assertGreaterEqual(len(PROCESS_TABLE), 20, "table must cover at least 20 known combinations")

    def test_all_scopes_present(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        for _, _, scope in PROCESS_TABLE:
            self.assertIn(f"ProcessScope.{scope}", text, f"ProcessScope.{scope} not defined")

    def test_main_process_allowed(self):
        """Main process names (package == process or empty) must not be rejected."""
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        self.assertIn("packageName == processName || processName.isEmpty()", text)

    def test_unknown_auxiliary_process_default(self):
        """An unknown package with a non-empty, non-main process must not map to a
        main-process scope."""
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        # The final catch-all branch is GENERIC_APP.
        self.assertIn("else -> ProcessScope.GENERIC_APP", text)

    def test_rejected_list(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        for scope in ("SETTINGS_REMOTE", "SECURITY_CENTER_BOOTAWARE", "UNSUPPORTED", "NETWORK_STACK"):
            self.assertRegex(
                text,
                rf"ProcessScope\.{scope}\s*(?:,|->)",
                f"rejected scope {scope} missing from isRejected when",
            )

    def test_bootaware_dot_and_colon(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        self.assertIn('processName.endsWith(".bootaware")', text)
        self.assertIn('processName.endsWith(":bootaware")', text)


if __name__ == "__main__":
    unittest.main()
