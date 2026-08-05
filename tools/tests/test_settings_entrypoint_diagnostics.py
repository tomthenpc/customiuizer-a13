#!/usr/bin/env python3
"""Static contract tests for settings entrypoint controlled diagnostics migration."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
FILES = {
    "AboutFragment": REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "AboutFragment.kt",
    "Credentials": REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "Credentials.kt",
    "MainActivity": REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "MainActivity.kt",
    "PrefsProvider": REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "PrefsProvider.kt",
    "SubFragmentWithSearch": REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "SubFragmentWithSearch.kt",
    "System": REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "subs" / "System.kt",
}
SETTINGS_DIAGNOSTICS = (
    REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "utils" / "SettingsDiagnostics.kt"
)


class SettingsEntrypointDiagnosticsContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.texts = {name: path.read_text(encoding="utf-8") for name, path in FILES.items()}
        cls.sd_text = SETTINGS_DIAGNOSTICS.read_text(encoding="utf-8")

    def test_no_printStackTrace_in_any_target(self):
        for name, text in self.texts.items():
            with self.subTest(file=name):
                self.assertIsNone(
                    re.search(r"\.printStackTrace\s*\(", text),
                    f"{name}.kt still contains printStackTrace calls",
                )

    def test_all_targets_import_settings_diagnostics(self):
        for name, text in self.texts.items():
            with self.subTest(file=name):
                self.assertIn(
                    "import tv.withaibuild.customiuizer.utils.SettingsDiagnostics",
                    text,
                )

    def test_no_system_out_or_err(self):
        for name, text in self.texts.items():
            with self.subTest(file=name):
                for ref in ("System.out", "System.err"):
                    with self.subTest(ref=ref):
                        self.assertNotIn(ref, text)

    def test_no_new_log_calls(self):
        for name, text in self.texts.items():
            with self.subTest(file=name):
                code = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
                code = re.sub(r"//.*", "", code)
                for call in ("Log.e(", "Log.w(", "Log.d(", "Log.i(", "Log.v("):
                    with self.subTest(call=call):
                        self.assertNotIn(call, code)

    def test_settings_diagnostics_unchanged(self):
        self.assertIn("internal object SettingsDiagnostics", self.sd_text)
        self.assertIn('private const val TAG = "CustoMIUIzer-Settings"', self.sd_text)
        self.assertIn("Log.e(TAG, operation, throwable)", self.sd_text)
        for name in FILES:
            with self.subTest(name=name):
                self.assertNotIn(name, self.sd_text)

    def test_all_six_operations_present_and_unique(self):
        operations = [
            "AboutFragment.bindVersionText",
            "Credentials.initializeCredentialFlow",
            "MainActivity.unregisterPreferenceListener",
            "PrefsProvider.openTestAsset",
            "SubFragmentWithSearch.styleSearchView",
            "System.writeQqsCount",
        ]
        for op in operations:
            with self.subTest(operation=op):
                found = any(f'SettingsDiagnostics.failure("{op}",' in text for text in self.texts.values())
                self.assertTrue(found, f"operation {op} not found")

        all_found = []
        for text in self.texts.values():
            all_found.extend(re.findall(r'SettingsDiagnostics\.failure\("([^"]+)",', text))
        self.assertEqual(6, len(all_found))
        self.assertEqual(6, len(set(all_found)), "operation names must be unique")

    def test_all_catch_blocks_have_full_fatal_guard(self):
        operations = [
            "AboutFragment.bindVersionText",
            "Credentials.initializeCredentialFlow",
            "MainActivity.unregisterPreferenceListener",
            "PrefsProvider.openTestAsset",
            "SubFragmentWithSearch.styleSearchView",
            "System.writeQqsCount",
        ]
        for op in operations:
            with self.subTest(operation=op):
                body = self._extract_catch_body_after_call(op)
                self.assertIsNotNone(body, f"could not find catch body for {op}")
                self.assertIn("is OutOfMemoryError", body)
                self.assertIn("is ThreadDeath", body)
                self.assertIn("is VirtualMachineError", body)
                self.assertIn("throw", body)

    def test_about_fragment_version_text(self):
        text = self.texts["AboutFragment"]
        try_body = self._extract_try_body_before_call("AboutFragment.bindVersionText")
        self.assertIsNotNone(try_body)
        self.assertIn("BuildConfig.VERSION_NAME", try_body)
        self.assertIn("BuildConfig.BUILD_TIME", try_body)
        self.assertIn('"yy.MM.dd"', try_body)
        self.assertIn('"-test"', try_body)
        self.assertIn("R.id.about_version", try_body)

        catch_body = self._extract_catch_body_after_call("AboutFragment.bindVersionText")
        self.assertIsNotNone(catch_body)
        self.assertNotIn("AlertDialog", catch_body)

    def test_credentials_outer_catch_only(self):
        text = self.texts["Credentials"]
        call_pattern = re.compile(r'SettingsDiagnostics\.failure\("Credentials\.initializeCredentialFlow"[^)]*\)')
        call_match = call_pattern.search(text)
        self.assertIsNotNone(call_match)

        catch_body = self._extract_catch_body_after_call("Credentials.initializeCredentialFlow")
        self.assertIsNotNone(catch_body)
        # Outer catch is at onCreate root; it must not contain the inner KeyStore fallback.
        self.assertNotIn("createConfirmDeviceCredentialIntent", catch_body)
        self.assertNotIn("startActivityForResult", catch_body)

    def test_credentials_inner_fallback_preserved(self):
        text = self.texts["Credentials"]
        # The inner catch contains the KeyStore fallback and must not call SettingsDiagnostics.
        inner_catch_match = re.search(
            r'} catch \(e: Throwable\) \{(.*?\n(?:.*\n)*?)\}',
            text,
        )
        # Find the inner catch (the one with createConfirmDeviceCredentialIntent).
        inner_matches = list(re.finditer(
            r'} catch \(e: Throwable\) \{',
            text,
        ))
        self.assertTrue(len(inner_matches) >= 1)
        for m in inner_matches:
            body = self._brace_body(text, text.find("{", m.start()))
            if "createConfirmDeviceCredentialIntent" in body:
                self.assertIn("createConfirmDeviceCredentialIntent", body)
                self.assertIn("startActivityForResult", body)
                self.assertNotIn("SettingsDiagnostics", body)

    def test_main_activity_ondestroy_preserved(self):
        text = self.texts["MainActivity"]
        catch_body = self._extract_catch_body_after_call("MainActivity.unregisterPreferenceListener")
        self.assertIsNotNone(catch_body)
        self.assertNotIn("super.onDestroy()", catch_body)
        # super.onDestroy() must follow the catch.
        self.assertIn("super.onDestroy()", text)

    def test_prefs_provider_mappings_and_open_fd(self):
        text = self.texts["PrefsProvider"]
        try_body = self._extract_try_body_before_call("PrefsProvider.openTestAsset")
        self.assertIsNotNone(try_body)
        self.assertIn("ctx.assets.openFd(filename)", try_body)
        for mapping in ('"test0.png"', '"test1.mp3"', '"test2.mp4"', '"test3.txt"', '"test4.zip"'):
            with self.subTest(mapping=mapping):
                self.assertIn(mapping, text)

    def test_prefs_provider_returns_null_after_catch(self):
        text = self.texts["PrefsProvider"]
        catch_body = self._extract_catch_body_after_call("PrefsProvider.openTestAsset")
        self.assertIsNotNone(catch_body)
        self.assertNotIn("return", catch_body)
        # The final method-level return null still exists.
        self.assertIn("return null", text)

    def test_sub_fragment_style_search_view_no_return(self):
        text = self.texts["SubFragmentWithSearch"]
        catch_body = self._extract_catch_body_after_call("SubFragmentWithSearch.styleSearchView")
        self.assertIsNotNone(catch_body)
        self.assertNotIn("return", catch_body)

    def test_sub_fragment_search_ui_preserved(self):
        text = self.texts["SubFragmentWithSearch"]
        self.assertIn("setSaveFromParentEnabled(false)", text)
        self.assertIn("addTextChangedListener", text)
        self.assertIn("ListView", text)
        self.assertIn("filter", text)

    def test_system_write_qqs_count(self):
        text = self.texts["System"]
        try_body = self._extract_try_body_before_call("System.writeQqsCount")
        self.assertIsNotNone(try_body)
        self.assertIn('"sysui_qqs_count"', try_body)
        self.assertIn("Settings.Secure.putInt", try_body)

        # The surrounding logic must be preserved.
        self.assertIn("fromUser", text)
        self.assertIn("value = 5", text)

        catch_body = self._extract_catch_body_after_call("System.writeQqsCount")
        self.assertIsNotNone(catch_body)
        self.assertNotIn("return", catch_body)
        self.assertNotIn("Toast", catch_body)

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
