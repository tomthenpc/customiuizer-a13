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
        self.assertIn("ProcessScope.LOCATION_FUSED", text)
        self.assertIn("ProcessScope.NETWORK_STACK", text)
        self.assertIn("isRejected", text)

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
        # The KNOWN_PACKAGES Set is a compile-time singleton, not rebuilt on call.
        self.assertIn("private val KNOWN_PACKAGES: Set<String>", text)


if __name__ == "__main__":
    unittest.main()
