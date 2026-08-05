#!/usr/bin/env python3
"""Static contract tests for Helpers controlled diagnostics migration."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
HELPERS = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "utils" / "Helpers.kt"
DIAGNOSTICS = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "utils" / "SettingsDiagnostics.kt"


class HelpersDiagnosticsContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.helpers_text = HELPERS.read_text(encoding="utf-8")
        cls.diagnostics_text = DIAGNOSTICS.read_text(encoding="utf-8")

    def test_helpers_no_printStackTrace(self):
        self.assertIsNone(
            re.search(r"\.printStackTrace\s*\(", self.helpers_text),
            "Helpers.kt still contains printStackTrace calls",
        )

    def test_settings_diagnostics_has_fixed_tag(self):
        self.assertIn('private const val TAG = "CustoMIUIzer-Settings"', self.diagnostics_text)

    def test_settings_diagnostics_uses_log_e(self):
        self.assertIn("Log.e(TAG, operation, throwable)", self.diagnostics_text)

    def test_settings_diagnostics_no_unwanted_references(self):
        # Strip block and line comments before checking for prohibited symbols.
        code = re.sub(r"/\*.*?\*/", "", self.diagnostics_text, flags=re.S)
        code = re.sub(r"//.*", "", code)
        for ref in ("Context", "Helpers", "HookUtils", "Xposed", "IBinder"):
            with self.subTest(ref=ref):
                self.assertNotIn(ref, code)

    def test_all_fifteen_operations_present_and_unique(self):
        operations = [
            "Helpers.hideKeyboard",
            "Helpers.updateNewModsMarking",
            "Helpers.getAnimationScale",
            "Helpers.setAnimationScale",
            "Helpers.getPackageInfoAsUser",
            "Helpers.getInstalledApps.item",
            "Helpers.getLaunchableApps.item",
            "Helpers.getShareApps.item",
            "Helpers.getOpenWithApps.item",
            "Helpers.getAppName.application",
            "Helpers.getAppName.activity",
            "Helpers.getAppIcon.application",
            "Helpers.getAppIcon.activity",
            "Helpers.parsePrefXml.item",
            "Helpers.copyFile",
        ]
        for op in operations:
            with self.subTest(operation=op):
                self.assertIn(f'SettingsDiagnostics.failure("{op}",', self.helpers_text)

        found = re.findall(r'SettingsDiagnostics\.failure\("([^"]+)",', self.helpers_text)
        self.assertEqual(15, len(found))
        self.assertEqual(len(found), len(set(found)), "operation names must be unique")

    def test_fatal_guards_preserved(self):
        matches = re.findall(
            r"is OutOfMemoryError \|\| .*? is ThreadDeath \|\| .*? is VirtualMachineError",
            self.helpers_text,
        )
        # At least the 15 changed catch blocks, plus the existing ignore-style guards.
        self.assertGreaterEqual(len(matches), 15)

    def test_getAnimationScale_returns_fallback(self):
        self._assert_call_followed_by("Helpers.getAnimationScale", r"1\.0f")

    def test_getPackageInfoAsUser_returns_fallback(self):
        self._assert_call_followed_by("Helpers.getPackageInfoAsUser", r"null")

    def test_getAppName_application_returns_fallback(self):
        self._assert_call_followed_by("Helpers.getAppName.application", r"null")

    def test_getAppName_activity_returns_fallback(self):
        self._assert_call_followed_by("Helpers.getAppName.activity", r"null")

    def test_getAppIcon_application_returns_fallback(self):
        self._assert_call_followed_by("Helpers.getAppIcon.application", r"null")

    def test_getAppIcon_activity_returns_fallback(self):
        self._assert_call_followed_by("Helpers.getAppIcon.activity", r"null")

    def test_copyFile_returns_fallback(self):
        self._assert_call_followed_by("Helpers.copyFile", r"false")

    def test_app_list_item_catches_no_return(self):
        for op in (
            "Helpers.getInstalledApps.item",
            "Helpers.getLaunchableApps.item",
            "Helpers.getShareApps.item",
            "Helpers.getOpenWithApps.item",
        ):
            with self.subTest(operation=op):
                body = self._extract_catch_body_after_call(op)
                self.assertIsNotNone(body, f"could not find catch for {op}")
                self.assertNotIn("return", body, f"catch for {op} must not return")

    def test_parsePrefXml_loop_continues(self):
        op = "Helpers.parsePrefXml.item"
        body = self._extract_catch_body_after_call(op)
        self.assertIsNotNone(body, f"could not find catch for {op}")
        self.assertNotIn("return", body)
        self.assertNotIn("break", body)

        # Ensure the catch block is followed by the next XML event read.
        call_match = re.search(
            rf'SettingsDiagnostics\.failure\("{re.escape(op)}",\s*\w+\)',
            self.helpers_text,
        )
        self.assertIsNotNone(call_match)
        after = self.helpers_text[call_match.end(): call_match.end() + 200]
        self.assertIn("eventType = xml.next()", after)

    def test_no_system_out_or_err(self):
        self.assertIsNone(re.search(r"\bSystem\.(?:out|err)\b", self.helpers_text))

    def _extract_catch_body_after_call(self, operation: str) -> str | None:
        pattern = (
            r'catch\s*\(\s*[a-zA-Z]+\s*:\s*Throwable\s*\)\s*\{\s*'
            r'if\s*\(\s*[a-zA-Z]+\s+is\s+OutOfMemoryError\s+\|\|\s+[a-zA-Z]+\s+is\s+ThreadDeath\s+\|\|\s+[a-zA-Z]+\s+is\s+VirtualMachineError\s*\)\s*throw\s+[a-zA-Z]+\s*'
            rf'SettingsDiagnostics\.failure\("{re.escape(operation)}",\s*[a-zA-Z]+\)'
        )
        m = re.search(pattern, self.helpers_text)
        if not m:
            return None
        start = m.start()
        # The catch body opened at the '{' matched just before the if; find its matching '}'.
        brace = self.helpers_text.find("{", start)
        depth = 1
        i = brace + 1
        while i < len(self.helpers_text) and depth > 0:
            if self.helpers_text[i] == "{":
                depth += 1
            elif self.helpers_text[i] == "}":
                depth -= 1
            i += 1
        return self.helpers_text[brace:i]

    def _assert_call_followed_by(self, operation: str, value_pattern: str) -> None:
        pattern = (
            r'catch\s*\(\s*[a-zA-Z]+\s*:\s*Throwable\s*\)\s*\{[^}]*'
            rf'SettingsDiagnostics\.failure\("{re.escape(operation)}",\s*[a-zA-Z]+\)'
            r'[^}]*'
            + value_pattern
            + r'[^}]*\}'
        )
        self.assertRegex(self.helpers_text, pattern, f"{operation} catch should return {value_pattern}")


if __name__ == "__main__":
    unittest.main()
