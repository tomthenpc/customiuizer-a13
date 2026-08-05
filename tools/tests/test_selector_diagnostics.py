#!/usr/bin/env python3
"""Static contract tests for selector controlled diagnostics migration."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
SUBS = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "subs"
FILES = {
    "ActivitySelector": SUBS / "ActivitySelector.kt",
    "AppSelector": SUBS / "AppSelector.kt",
    "ShortcutSelector": SUBS / "ShortcutSelector.kt",
    "SortableList": SUBS / "SortableList.kt",
}
SETTINGS_DIAGNOSTICS = (
    REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "utils" / "SettingsDiagnostics.kt"
)


class SelectorDiagnosticsContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.texts = {name: path.read_text(encoding="utf-8") for name, path in FILES.items()}
        cls.sd_text = SETTINGS_DIAGNOSTICS.read_text(encoding="utf-8")

    def test_no_printStackTrace_in_any_selector(self):
        for name, text in self.texts.items():
            with self.subTest(file=name):
                self.assertIsNone(
                    re.search(r"\.printStackTrace\s*\(", text),
                    f"{name}.kt still contains printStackTrace calls",
                )

    def test_all_selectors_import_settings_diagnostics(self):
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
        for op in (
            "ActivitySelector",
            "AppSelector",
            "ShortcutSelector",
            "SortableList",
        ):
            with self.subTest(op=op):
                self.assertNotIn(op, self.sd_text)

    def test_all_operations_present_and_unique(self):
        operations = [
            "ActivitySelector.loadActivities",
            "AppSelector.privacy.toggle",
            "AppSelector.applock.toggle",
            "AppSelector.loadApps",
            "ShortcutSelector.loadIconResource",
            "ShortcutSelector.persistIcon",
            "SortableList.loadDragShadow",
        ]
        for op in operations:
            with self.subTest(operation=op):
                found = False
                for name, text in self.texts.items():
                    if f'SettingsDiagnostics.failure("{op}",' in text:
                        found = True
                self.assertTrue(found, f"operation {op} not found")

        all_found = []
        for text in self.texts.values():
            all_found.extend(re.findall(r'SettingsDiagnostics\.failure\("([^"]+)",', text))
        self.assertEqual(7, len(all_found))
        self.assertEqual(7, len(set(all_found)), "operation names must be unique")

    def test_all_catch_blocks_have_full_fatal_guard(self):
        operations = [
            "ActivitySelector.loadActivities",
            "AppSelector.privacy.toggle",
            "AppSelector.applock.toggle",
            "AppSelector.loadApps",
            "ShortcutSelector.loadIconResource",
            "ShortcutSelector.persistIcon",
            "SortableList.loadDragShadow",
        ]
        for op in operations:
            with self.subTest(operation=op):
                body = self._extract_catch_body_after_call(op)
                self.assertIsNotNone(body, f"could not find catch body for {op}")
                self.assertIn("is OutOfMemoryError", body)
                self.assertIn("is ThreadDeath", body)
                self.assertIn("is VirtualMachineError", body)
                self.assertIn("throw", body)

    def test_activity_selector_catch_does_not_call_process(self):
        body = self._extract_catch_body_after_call("ActivitySelector.loadActivities")
        self.assertIsNotNone(body)
        self.assertNotIn("process", body)

    def test_activity_selector_success_path_has_run_on_ui_thread(self):
        text = self.texts["ActivitySelector"]
        self.assertIn("act.runOnUiThread(process)", text)
        try_body = self._extract_try_body_before_call("ActivitySelector.loadActivities")
        self.assertIsNotNone(try_body)
        self.assertIn("act.runOnUiThread(process)", try_body)

    def test_app_selector_privacy_success(self):
        text = self.texts["AppSelector"]
        try_body = self._extract_try_body_before_call("AppSelector.privacy.toggle")
        self.assertIsNotNone(try_body)
        self.assertIn("PrivacyAppAdapter", try_body)
        self.assertIn("update_privacyapps_icon", try_body)

    def test_app_selector_applock_success(self):
        text = self.texts["AppSelector"]
        try_body = self._extract_try_body_before_call("AppSelector.applock.toggle")
        self.assertIsNotNone(try_body)
        self.assertIn("LockedAppAdapter", try_body)

    def test_app_selector_load_apps_success_sets_initialized(self):
        text = self.texts["AppSelector"]
        try_body = self._extract_try_body_before_call("AppSelector.loadApps")
        self.assertIsNotNone(try_body)
        self.assertIn("initialized = true", try_body)
        self.assertIn("act.runOnUiThread(process)", try_body)

        catch_body = self._extract_catch_body_after_call("AppSelector.loadApps")
        self.assertIsNotNone(catch_body)
        self.assertNotIn("initialized = true", catch_body)

    def test_shortcut_load_icon_resource_falls_back_to_bitmap(self):
        text = self.texts["ShortcutSelector"]
        catch_body = self._extract_catch_body_after_call("ShortcutSelector.loadIconResource")
        self.assertIsNotNone(catch_body)
        self.assertNotIn("EXTRA_SHORTCUT_ICON", catch_body)
        self.assertIn("Intent.EXTRA_SHORTCUT_ICON", text)

    def test_shortcut_persist_icon_format_unchanged(self):
        text = self.texts["ShortcutSelector"]
        try_body = self._extract_try_body_before_call("ShortcutSelector.persistIcon")
        self.assertIsNotNone(try_body)
        self.assertIn("Bitmap.CompressFormat.PNG", try_body)
        self.assertIn("100", try_body)
        self.assertIn("FileOutputStream", try_body)
        self.assertIn('"shortcut_icon"', try_body)

        catch_body = self._extract_catch_body_after_call("ShortcutSelector.persistIcon")
        self.assertIsNotNone(catch_body)
        self.assertNotIn("shortcut_icon", catch_body)

    def test_shortcut_persist_icon_continues_after_catch(self):
        text = self.texts["ShortcutSelector"]
        self.assertIn('"shortcut_contents"', text)
        self.assertIn('"shortcut_name"', text)
        self.assertIn('"shortcut_intent"', text)

    def test_sortable_list_drag_shadow_corresponds_to_snapshot(self):
        try_body = self._extract_try_body_before_call("SortableList.loadDragShadow")
        self.assertIsNotNone(try_body)
        self.assertIn("mSnapshotShadow", try_body)

    def test_sortable_list_drag_shadow_failure_continues(self):
        text = self.texts["SortableList"]
        catch_body = self._extract_catch_body_after_call("SortableList.loadDragShadow")
        self.assertIsNotNone(catch_body)
        self.assertNotIn("return", catch_body)
        self.assertIn("PreferenceAdapter", text)

    def _extract_catch_body_after_call(self, operation):
        """Find the catch { ... } block containing a SettingsDiagnostics.failure call."""
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
        """Find the try { ... } block immediately preceding the catch containing the call."""
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
