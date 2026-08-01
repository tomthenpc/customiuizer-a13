#!/usr/bin/env python3
"""Static tests for the A13 ProcessScope / ProcessScopes pure routing layer."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
SRC = REPO / "app" / "src" / "main" / "java"


def read(rel: str) -> str:
    return (SRC / rel.replace("/", "\\")).read_text(encoding="utf-8")


class ProcessScopeTests(unittest.TestCase):
    def test_main_module_delegates_to_process_scopes(self):
        main = read("tv/withaibuild/customiuizer/MainModule.java")
        self.assertIn("ProcessScopes.isRejected(pkg, processName)", main)

    def test_process_scopes_has_when_expression(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        self.assertIn("when (packageName) {", text)
        for pkg in (
            "com.android.systemui",
            "com.miui.home",
            "com.mi.android.globallauncher",
            "com.android.settings",
            "com.miui.securitycenter",
            "com.miui.powerkeeper",
        ):
            self.assertIn(pkg, text)

    def test_process_scopes_rejects_known_auxiliary_processes(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        self.assertIn("ProcessScope.SETTINGS_REMOTE", text)
        self.assertIn("ProcessScope.SECURITY_CENTER_BOOTAWARE", text)
        self.assertIn("ProcessScope.NETWORK_STACK", text)
        self.assertIn("isRejected", text)

    def test_process_scopes_distinguishes_main_and_auxiliary(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        self.assertIn("ProcessScope.SETTINGS_MAIN", text)
        self.assertIn("ProcessScope.SECURITY_CENTER_MAIN", text)
        self.assertIn("ProcessScope.SECURITY_CENTER_REMOTE", text)
        self.assertIn("ProcessScope.SYSTEM_UI_PLUGIN", text)

    def test_process_scopes_bootaware_accepts_colon_and_dot(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        self.assertIn('processName.endsWith(":bootaware")', text)
        self.assertIn('processName.endsWith(".bootaware")', text)

    def test_process_scopes_has_no_runtime_android_imports(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        self.assertNotIn("import android.", text)

    def test_process_scopes_no_collections_at_runtime(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        self.assertNotIn("mapOf", text)
        self.assertNotIn("listOf", text)
        self.assertIn("private val KNOWN_PACKAGES: Set<String>", text)

    def test_android_package_is_system_server(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        self.assertIn('PKG_ANDROID -> ProcessScope.SYSTEM_SERVER', text)

    def test_wallpaper_package_classified(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        self.assertIn('"com.miui.miwallpaper" -> ProcessScope.WALLPAPER', text)

    def test_process_scopes_default_rejects_unknown_auxiliary(self):
        """Any package with an auxiliary process name that is not the main package
        should resolve to a non-MAIN scope."""
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        # The default branch returns GENERIC_APP for unknown packages.
        self.assertIn("ProcessScope.GENERIC_APP", text)

    def test_process_scopes_system_ui_plugin_non_installable(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        self.assertIn("this != SYSTEM_UI_PLUGIN", text)
        self.assertIn("!resolve(packageName, processName).isInstallable", text)


if __name__ == "__main__":
    unittest.main()
