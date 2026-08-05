#!/usr/bin/env python3
"""Static contract tests for remaining controlled diagnostics baseline-zero migration."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
FILES = {
    "SeekBarPreference": REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "prefs" / "SeekBarPreference.kt",
    "SpinnerEx": REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "prefs" / "SpinnerEx.kt",
    "AutoRotateService": REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "qs" / "AutoRotateService.kt",
    "AppHelper": REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "utils" / "AppHelper.kt",
    "LockedAppAdapter": REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "utils" / "LockedAppAdapter.kt",
    "PreferenceAdapter": REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "utils" / "PreferenceAdapter.kt",
    "PrivacyAppAdapter": REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "utils" / "PrivacyAppAdapter.kt",
}
MAIN_JAVA = REPO / "app" / "src" / "main" / "java"
SETTINGS_DIAGNOSTICS = (
    REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "utils" / "SettingsDiagnostics.kt"
)


class RemainingDiagnosticsContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.texts = {name: path.read_text(encoding="utf-8") for name, path in FILES.items()}
        cls.sd_text = SETTINGS_DIAGNOSTICS.read_text(encoding="utf-8")

    def test_no_printStackTrace_globally(self):
        for path in MAIN_JAVA.rglob("*"):
            if path.is_file() and path.suffix in (".kt", ".java"):
                with self.subTest(path=path.relative_to(REPO)):
                    text = path.read_text(encoding="utf-8")
                    self.assertIsNone(
                        re.search(r"\.printStackTrace\s*\(", text),
                        f"{path} still contains printStackTrace",
                    )

    def test_no_system_out_or_err_in_targets(self):
        for name, text in self.texts.items():
            with self.subTest(file=name):
                for ref in ("System.out", "System.err"):
                    with self.subTest(ref=ref):
                        self.assertNotIn(ref, text)

    def test_no_new_log_calls_in_target_catches(self):
        operations = [
            "SeekBarPreference.formatDisplayValue",
            "SpinnerEx.configurePopupHeight",
            "AutoRotateService.switchState",
            "AutoRotateService.readState",
            "AppHelper.resolveActionName",
            "LockedAppAdapter.initializeSecurityManager",
            "PreferenceAdapter.bindActionIcon",
            "PrivacyAppAdapter.initializeSecurityManager",
        ]
        for op in operations:
            with self.subTest(operation=op):
                body = self._extract_catch_body_after_call(op)
                self.assertIsNotNone(body)
                for call in ("Log.e(", "Log.w(", "Log.d(", "Log.i(", "Log.v("):
                    with self.subTest(call=call):
                        self.assertNotIn(call, body)

    def test_settings_diagnostics_unchanged(self):
        self.assertIn("internal object SettingsDiagnostics", self.sd_text)
        self.assertIn('private const val TAG = "CustoMIUIzer-Settings"', self.sd_text)
        self.assertIn("Log.e(TAG, operation, throwable)", self.sd_text)

    def test_all_operations_present_and_unique(self):
        operations = [
            "SeekBarPreference.formatDisplayValue",
            "SpinnerEx.configurePopupHeight",
            "AutoRotateService.switchState",
            "AutoRotateService.readState",
            "AppHelper.resolveActionName",
            "LockedAppAdapter.initializeSecurityManager",
            "LockedAppAdapter.readChecked",
            "PreferenceAdapter.bindActionIcon",
            "PrivacyAppAdapter.initializeSecurityManager",
            "PrivacyAppAdapter.readChecked",
        ]
        for op in operations:
            with self.subTest(operation=op):
                found = any(f'SettingsDiagnostics.failure("{op}",' in text for text in self.texts.values())
                self.assertTrue(found, f"operation {op} not found")

        all_found = []
        for text in self.texts.values():
            all_found.extend(re.findall(r'SettingsDiagnostics\.failure\("([^"]+)",', text))
        self.assertEqual(10, len(all_found))
        self.assertEqual(10, len(set(all_found)), "operation names must be unique")

    def test_seekbar_only_illegalformat(self):
        text = self.texts["SeekBarPreference"]
        call_match = re.search(r'SettingsDiagnostics\.failure\("SeekBarPreference\.formatDisplayValue"[^)]*\)', text)
        self.assertIsNotNone(call_match)
        body = self._extract_catch_body_after_call("SeekBarPreference.formatDisplayValue")
        self.assertIsNotNone(body)
        # No Throwable catch in the operation catch (it must be IllegalFormatException).
        self.assertNotIn("catch (t: Throwable)", body)
        self.assertNotIn("is OutOfMemoryError", body)
        # But must still fallback to display.toString().
        self.assertIn("display.toString()", body)

    def test_seven_throwable_catches_have_full_fatal_guard(self):
        operations = [
            "SpinnerEx.configurePopupHeight",
            "AutoRotateService.switchState",
            "AutoRotateService.readState",
            "AppHelper.resolveActionName",
            "LockedAppAdapter.initializeSecurityManager",
            "PreferenceAdapter.bindActionIcon",
            "PrivacyAppAdapter.initializeSecurityManager",
        ]
        for op in operations:
            with self.subTest(operation=op):
                body = self._extract_catch_body_after_call(op)
                self.assertIsNotNone(body, f"could not find catch body for {op}")
                self.assertIn("is OutOfMemoryError", body)
                self.assertIn("is ThreadDeath", body)
                self.assertIn("is VirtualMachineError", body)
                self.assertIn("throw", body)

    def test_spinner_ex_popup(self):
        text = self.texts["SpinnerEx"]
        try_body = self._extract_try_body_before_call("SpinnerEx.configurePopupHeight")
        self.assertIsNotNone(try_body)
        self.assertIn("mPopup", try_body)
        self.assertIn("(40 * 10 * scale).toInt()", try_body)

    def test_auto_rotate_service(self):
        text = self.texts["AutoRotateService"]
        switch_try = self._extract_try_body_before_call("AutoRotateService.switchState")
        self.assertIsNotNone(switch_try)
        self.assertIn('"pref_key_qs_autorotate_state"', switch_try)
        self.assertIn("if (state >= 2) 0 else state + 1", switch_try)

        read_try = self._extract_try_body_before_call("AutoRotateService.readState")
        self.assertIsNotNone(read_try)
        self.assertIn('"pref_key_qs_autorotate_state"', read_try)

        read_body = self._extract_catch_body_after_call("AutoRotateService.readState")
        self.assertIsNotNone(read_body)
        self.assertIn("0", read_body)

    def test_apphelper_returns_null(self):
        text = self.texts["AppHelper"]
        body = self._extract_catch_body_after_call("AppHelper.resolveActionName")
        self.assertIsNotNone(body)
        self.assertIn("null", body)

    def test_preference_adapter_continue_and_return(self):
        text = self.texts["PreferenceAdapter"]
        body = self._extract_catch_body_after_call("PreferenceAdapter.bindActionIcon")
        self.assertIsNotNone(body)
        self.assertNotIn("return", body)
        self.assertIn("row.setPadding", text)
        self.assertIn("return row", text)

    def test_locked_adapter_no_return(self):
        text = self.texts["LockedAppAdapter"]
        body = self._extract_catch_body_after_call("LockedAppAdapter.initializeSecurityManager")
        self.assertIsNotNone(body)
        self.assertNotIn("return", body)
        # Method name and signature must be unchanged.
        try_body = self._extract_try_body_before_call("LockedAppAdapter.initializeSecurityManager")
        self.assertIsNotNone(try_body)
        self.assertIn('"getApplicationAccessControlEnabledAsUser"', try_body)
        self.assertIn("String::class.java", try_body)
        self.assertIn("Int::class.javaPrimitiveType", try_body)

    def test_privacy_adapter_no_return(self):
        text = self.texts["PrivacyAppAdapter"]
        body = self._extract_catch_body_after_call("PrivacyAppAdapter.initializeSecurityManager")
        self.assertIsNotNone(body)
        self.assertNotIn("return", body)
        try_body = self._extract_try_body_before_call("PrivacyAppAdapter.initializeSecurityManager")
        self.assertIsNotNone(try_body)
        self.assertIn('"isPrivacyApp"', try_body)
        self.assertIn("String::class.java", try_body)
        self.assertIn("Int::class.javaPrimitiveType", try_body)

    def _extract_catch_body_after_call(self, operation):
        call_pattern = re.compile(rf'SettingsDiagnostics\.failure\("{re.escape(operation)}"[^)]*\)')
        text = "".join(self.texts.values())
        call_match = call_pattern.search(text)
        if not call_match:
            return None

        text_before = text[: call_match.start()]
        catch_matches = list(re.finditer(r'catch\s*\([^)]*\)\s*\{', text_before))
        if not catch_matches:
            return None
        catch_match = catch_matches[-1]

        brace_start = text_before.find("{", catch_match.start())
        if brace_start == -1:
            return None

        return self._brace_body(text, brace_start)

    def _extract_try_body_before_call(self, operation):
        call_pattern = re.compile(rf'SettingsDiagnostics\.failure\("{re.escape(operation)}"[^)]*\)')
        text = "".join(self.texts.values())
        call_match = call_pattern.search(text)
        if not call_match:
            return None

        text_before = text[: call_match.start()]
        catch_matches = list(re.finditer(r'catch\s*\([^)]*\)\s*\{', text_before))
        if not catch_matches:
            return None
        catch_match = catch_matches[-1]

        try_matches = list(re.finditer(r'try\s*\{', text_before[: catch_match.start()]))
        if not try_matches:
            return None
        try_match = try_matches[-1]

        brace_start = text_before.find("{", try_match.start())
        if brace_start == -1:
            return None

        return self._brace_body(text, brace_start)

    def _brace_body(self, text, brace_start):
        depth = 0
        i = brace_start
        while i < len(text):
            if text[i] == "{":
                depth += 1
            elif text[i] == "}":
                depth -= 1
                if depth == 0:
                    return text[brace_start + 1 : i]
            i += 1
        return None


if __name__ == "__main__":
    unittest.main()
