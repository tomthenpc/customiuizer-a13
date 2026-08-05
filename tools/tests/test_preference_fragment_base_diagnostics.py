#!/usr/bin/env python3
"""Static contract tests for PreferenceFragmentBase controlled diagnostics migration."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
PREFERENCE_FRAGMENT = (
    REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "PreferenceFragmentBase.kt"
)
SETTINGS_DIAGNOSTICS = (
    REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "utils" / "SettingsDiagnostics.kt"
)


class PreferenceFragmentBaseDiagnosticsContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pf_text = PREFERENCE_FRAGMENT.read_text(encoding="utf-8")
        cls.sd_text = SETTINGS_DIAGNOSTICS.read_text(encoding="utf-8")

    def test_no_printStackTrace(self):
        self.assertIsNone(
            re.search(r"\.printStackTrace\s*\(", self.pf_text),
            "PreferenceFragmentBase.kt still contains printStackTrace calls",
        )

    def test_has_settings_diagnostics_import(self):
        self.assertIn(
            "import tv.withaibuild.customiuizer.utils.SettingsDiagnostics",
            self.pf_text,
        )

    def test_no_system_out_or_err(self):
        for ref in ("System.out", "System.err"):
            with self.subTest(ref=ref):
                self.assertNotIn(ref, self.pf_text)

    def test_no_new_log_tag(self):
        # SettingsDiagnostics uses the fixed tag internally; PreferenceFragmentBase must not declare its own Log or TAG.
        code = re.sub(r"/\*.*?\*/", "", self.pf_text, flags=re.S)
        code = re.sub(r"//.*", "", code)
        self.assertNotIn("private const val TAG", code)
        self.assertNotIn("Log.e(", code)
        self.assertNotIn("Log.w(", code)
        self.assertNotIn("Log.d(", code)

    def test_settings_diagnostics_unchanged(self):
        # The object must remain the generic settings-app helper and not be altered for this file.
        self.assertIn("internal object SettingsDiagnostics", self.sd_text)
        self.assertIn('private const val TAG = "CustoMIUIzer-Settings"', self.sd_text)
        self.assertIn("Log.e(TAG, operation, throwable)", self.sd_text)
        self.assertNotIn("PreferenceFragmentBase", self.sd_text)

    def test_all_five_operations_present_and_unique(self):
        operations = [
            "PreferenceFragmentBase.setDefaultValues",
            "PreferenceFragmentBase.backup.write",
            "PreferenceFragmentBase.backup.close",
            "PreferenceFragmentBase.restore.read",
            "PreferenceFragmentBase.restore.close",
        ]
        for op in operations:
            with self.subTest(operation=op):
                self.assertIn(f'SettingsDiagnostics.failure("{op}",', self.pf_text)

        found = re.findall(r'SettingsDiagnostics\.failure\("([^"]+)",', self.pf_text)
        self.assertEqual(5, len(found))
        self.assertEqual(len(found), len(set(found)), "operation names must be unique")

    def test_fatal_guards_preserved_per_operation(self):
        operations = [
            "PreferenceFragmentBase.setDefaultValues",
            "PreferenceFragmentBase.backup.write",
            "PreferenceFragmentBase.backup.close",
            "PreferenceFragmentBase.restore.read",
            "PreferenceFragmentBase.restore.close",
        ]
        for op in operations:
            with self.subTest(operation=op):
                body = self._extract_catch_body_after_call(op)
                self.assertIsNotNone(body, f"could not find catch body for {op}")
                self.assertIn("is OutOfMemoryError", body)
                self.assertIn("is ThreadDeath", body)
                self.assertIn("is VirtualMachineError", body)
                self.assertIn("throw", body)

    def test_setDefaultValues_no_throw_or_dialog_after_call(self):
        op = "PreferenceFragmentBase.setDefaultValues"
        body = self._extract_catch_body_after_call(op)
        self.assertIsNotNone(body)
        self.assertNotIn("AlertDialog", body)
        # Fatal re-throw is checked by test_fatal_guards_preserved_per_operation.
        # After the fatal guard there must not be another throw for non-fatal t.
        self.assertNotIn('throw t', body.split('throw t')[1] if 'throw t' in body else body)

    def test_backup_write_catch_preserves_ui_and_message(self):
        op = "PreferenceFragmentBase.backup.write"
        body = self._extract_catch_body_after_call(op)
        self.assertIsNotNone(body)
        self.assertIn("R.string.storage_cannot_backup", body)
        self.assertIn("e.message", body)
        self.assertIn("R.string.warning", body)
        self.assertIn("AlertDialog.Builder", body)

    def test_backup_close_finally_preserves_flush_and_close(self):
        # The finally block surrounding the backup.close call must still flush/close output.
        finally_block = self._extract_finally_block_after(
            'SettingsDiagnostics.failure("PreferenceFragmentBase.backup.close"'
        )
        self.assertIsNotNone(finally_block)
        self.assertIn("output?.flush()", finally_block)
        self.assertIn("output?.close()", finally_block)

    def test_restore_read_catch_preserves_ui(self):
        op = "PreferenceFragmentBase.restore.read"
        body = self._extract_catch_body_after_call(op)
        self.assertIsNotNone(body)
        self.assertIn("R.string.storage_cannot_restore", body)
        self.assertIn("R.string.warning", body)
        self.assertIn("AlertDialog.Builder", body)

    def test_restore_close_finally_preserves_close(self):
        finally_block = self._extract_finally_block_after(
            'SettingsDiagnostics.failure("PreferenceFragmentBase.restore.close"'
        )
        self.assertIsNotNone(finally_block)
        self.assertIn("input?.close()", finally_block)

    def _extract_catch_body_after_call(self, operation):
        """Find the catch { ... } block containing a SettingsDiagnostics.failure call."""
        call_pattern = re.compile(rf'SettingsDiagnostics\.failure\("{re.escape(operation)}"[^)]*\)')
        call_match = call_pattern.search(self.pf_text)
        if not call_match:
            return None

        text_before = self.pf_text[: call_match.start()]
        catch_matches = list(re.finditer(r'catch\s*\([^)]*\)\s*\{', text_before))
        if not catch_matches:
            return None
        catch_match = catch_matches[-1]

        brace_start = text_before.find("{", catch_match.start())
        if brace_start == -1:
            return None

        # Find matching closing brace.
        depth = 0
        i = brace_start
        while i < len(self.pf_text):
            if self.pf_text[i] == "{":
                depth += 1
            elif self.pf_text[i] == "}":
                depth -= 1
                if depth == 0:
                    return self.pf_text[brace_start + 1 : i]
            i += 1
        return None

    def _extract_finally_block_after(self, marker):
        """Find the finally { ... } block that contains the given marker."""
        marker_index = self.pf_text.find(marker)
        if marker_index == -1:
            return None

        text_before = self.pf_text[:marker_index]
        finally_matches = list(re.finditer(r'finally\s*\{', text_before))
        if not finally_matches:
            return None
        finally_match = finally_matches[-1]

        brace_start = text_before.find("{", finally_match.start())
        if brace_start == -1:
            return None

        depth = 0
        i = brace_start
        while i < len(self.pf_text):
            if self.pf_text[i] == "{":
                depth += 1
            elif self.pf_text[i] == "}":
                depth -= 1
                if depth == 0:
                    return self.pf_text[brace_start + 1 : i]
            i += 1
        return None


if __name__ == "__main__":
    unittest.main()
