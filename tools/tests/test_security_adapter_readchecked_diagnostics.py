#!/usr/bin/env python3
"""Static contract tests for security adapter readChecked diagnostics."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
FILES = {
    "LockedAppAdapter": REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "utils" / "LockedAppAdapter.kt",
    "PrivacyAppAdapter": REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "utils" / "PrivacyAppAdapter.kt",
}
SETTINGS_DIAGNOSTICS = (
    REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "utils" / "SettingsDiagnostics.kt"
)


class SecurityAdapterReadCheckedContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.texts = {name: path.read_text(encoding="utf-8") for name, path in FILES.items()}
        cls.sd_text = SETTINGS_DIAGNOSTICS.read_text(encoding="utf-8")

    def test_no_printStackTrace_globally(self):
        for path in (REPO / "app" / "src" / "main" / "java").rglob("*"):
            if path.is_file() and path.suffix in (".kt", ".java"):
                with self.subTest(path=path.relative_to(REPO)):
                    text = path.read_text(encoding="utf-8")
                    self.assertIsNone(
                        re.search(r"\.printStackTrace\s*\(", text),
                        f"{path} still contains printStackTrace",
                    )

    def test_readChecked_returns_boolean(self):
        for name in ("LockedAppAdapter", "PrivacyAppAdapter"):
            with self.subTest(name=name):
                text = self.texts[name]
                match = re.search(r'fun readChecked\(app: AppData\): Boolean', text)
                self.assertIsNotNone(match, f"{name}.readChecked must return Boolean")

    def test_method_invoke_signature(self):
        for name, method in (("LockedAppAdapter", "getApplicationAccessControlEnabledAsUser"), ("PrivacyAppAdapter", "isPrivacyApp")):
            with self.subTest(name=name):
                text = self.texts[name]
                body = self._extract_method_body_with_call(text, "readChecked", method)
                self.assertIsNotNone(body)
                self.assertIn(f"method.invoke(sm, app.pkgName.orEmpty(), app.user)", body)
                self.assertIn("as? Boolean ?: false", body)

    def test_catch_has_full_fatal_guard(self):
        for op in ("LockedAppAdapter.readChecked", "PrivacyAppAdapter.readChecked"):
            with self.subTest(operation=op):
                body = self._extract_catch_body_after_call(op)
                self.assertIsNotNone(body)
                for fatal in ("is OutOfMemoryError", "is ThreadDeath", "is VirtualMachineError"):
                    with self.subTest(fatal=fatal):
                        self.assertIn(fatal, body)
                self.assertIn("throw", body)

    def test_fallback_is_false(self):
        for op in ("LockedAppAdapter.readChecked", "PrivacyAppAdapter.readChecked"):
            with self.subTest(operation=op):
                body = self._extract_catch_body_after_call(op)
                self.assertIsNotNone(body)
                self.assertIn("false", body)
                self.assertNotIn("return true", body)

    def test_log_once_flag_exists(self):
        for name in ("LockedAppAdapter", "PrivacyAppAdapter"):
            with self.subTest(name=name):
                text = self.texts[name]
                self.assertIn("private var readCheckedFailureLogged = false", text)

    def test_log_call_guarded(self):
        for op in ("LockedAppAdapter.readChecked", "PrivacyAppAdapter.readChecked"):
            with self.subTest(operation=op):
                body = self._extract_catch_body_after_call(op)
                self.assertIsNotNone(body)
                self.assertIn("if (!readCheckedFailureLogged)", body)
                # Flag should be set in the same guarded block or before SettingsDiagnostics.failure.
                self.assertRegex(body, r'readCheckedFailureLogged\s*=\s*true')

    def test_no_nullify_manager_or_method(self):
        for op in ("LockedAppAdapter.readChecked", "PrivacyAppAdapter.readChecked"):
            with self.subTest(operation=op):
                body = self._extract_catch_body_after_call(op)
                self.assertIsNotNone(body)
                self.assertNotIn("mSecurityManager = null", body)
                self.assertNotIn("getApplicationAccessControlEnabledAsUser = null", body)
                self.assertNotIn("isPrivacyApp = null", body)

    def test_no_notifyDataSetChanged(self):
        for op in ("LockedAppAdapter.readChecked", "PrivacyAppAdapter.readChecked"):
            with self.subTest(operation=op):
                body = self._extract_catch_body_after_call(op)
                self.assertIsNotNone(body)
                self.assertNotIn("notifyDataSetChanged", body)

    def test_refresh_not_rewritten(self):
        for name in ("LockedAppAdapter", "PrivacyAppAdapter"):
            with self.subTest(name=name):
                text = self.texts[name]
                self.assertIn("fun refresh(app: AppData)", text)
                self.assertIn("notifyDataSetChanged()", text)

    def test_init_operations_still_exist_once(self):
        for name, op in (
            ("LockedAppAdapter", "LockedAppAdapter.initializeSecurityManager"),
            ("PrivacyAppAdapter", "PrivacyAppAdapter.initializeSecurityManager"),
        ):
            with self.subTest(operation=op):
                text = self.texts[name]
                calls = text.count(f'SettingsDiagnostics.failure("{op}",')
                self.assertEqual(1, calls, f"{op} must exist exactly once")

    def test_readChecked_operations_unique(self):
        for op in ("LockedAppAdapter.readChecked", "PrivacyAppAdapter.readChecked"):
            with self.subTest(operation=op):
                calls = sum(text.count(f'SettingsDiagnostics.failure("{op}",') for text in self.texts.values())
                self.assertEqual(1, calls)

    def test_source_hazard_baseline_empty(self):
        baseline = REPO / "docs" / "audit" / "SOURCE_HAZARD_BASELINE.json"
        text = baseline.read_text(encoding="utf-8")
        self.assertIn('"schema": 1', text)
        self.assertIn('"fingerprints": []', text)
        self.assertIn('"findings": []', text)

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

    def _extract_method_body_with_call(self, text, method_name, _method_var):
        match = re.search(rf'fun {re.escape(method_name)}\(app: AppData\): Boolean', text)
        if not match:
            return None
        return self._brace_body(text, text.find("{", match.start()))

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
