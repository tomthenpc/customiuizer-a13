#!/usr/bin/env python3
"""Static contract tests for XposedHelpers reflection fatal unwrapping."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
XPOSED_HELPERS = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils" / "XposedHelpers.java"
OLD_TEST = REPO / "app" / "src" / "test" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils" / "XposedHelpersOomTest.java"
NEW_TEST = REPO / "app" / "src" / "test" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils" / "XposedHelpersFatalityTest.java"


class XposedHelpersReflectionFatalityContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = XPOSED_HELPERS.read_text(encoding="utf-8")

    def test_no_old_test_file(self):
        self.assertFalse(OLD_TEST.exists(), "XposedHelpersOomTest.java should not exist")

    def test_new_test_file_exists(self):
        self.assertTrue(NEW_TEST.exists())
        new_test = NEW_TEST.read_text(encoding="utf-8")
        self.assertIn("public class XposedHelpersFatalityTest", new_test)

    def test_helper_exists_unique(self):
        matches = list(re.finditer(
            r'private\s+static\s+InvocationTargetError\s+invocationTargetError\s*\(\s*InvocationTargetException\s+\w+\s*\)',
            self.text
        ))
        self.assertEqual(1, len(matches), "invocationTargetError helper must exist exactly once")

    def test_helper_gets_cause(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertIn("getCause()", body)

    def test_helper_checks_three_fatals(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertIn("instanceof OutOfMemoryError", body)
        self.assertIn("instanceof ThreadDeath", body)
        self.assertIn("instanceof VirtualMachineError", body)

    def test_helper_rethrows_original_cause(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertIn("throw (OutOfMemoryError) cause", body)
        self.assertIn("throw (ThreadDeath) cause", body)
        self.assertIn("throw (VirtualMachineError) cause", body)

    def test_helper_ordinary_wrapper(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertIn("new InvocationTargetError(cause)", body)

    def test_helper_does_not_check_all_errors(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertNotIn("instanceof Error", body)

    def test_helper_does_not_log(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertNotIn("Log.", body)

    def test_no_reflection_fatality_import(self):
        self.assertNotIn("import tv.withaibuild.customiuizer.utils.ReflectionFatality", self.text)

    def test_eight_public_entry_points_call_helper(self):
        # 8 public methods: 3 callMethod overloads + 3 callStaticMethod overloads + 2 newInstance overloads
        entries = [
            ("callMethod", 3),
            ("callStaticMethod", 3),
            ("newInstance", 2),
        ]
        call_helper = 0
        for method, count in entries:
            call_helper += self._count_entry_call_helper(method)
        self.assertEqual(8, call_helper, "eight public entry points must call invocationTargetError")

    def test_entries_do_not_bypass_helper(self):
        # No catch directly constructs InvocationTargetError with e.getCause().
        for match in re.finditer(r'catch\s*\(\s*InvocationTargetException\s+\w+\s*\)', self.text):
            start = match.start()
            body = self._brace_body(self.text, self.text.find("{", start))
            self.assertIsNotNone(body)
            self.assertIn("throw invocationTargetError", body)
            self.assertNotIn("new InvocationTargetError(e.getCause())", body)

    def test_invocation_target_error_extends_error(self):
        self.assertIn("public static final class InvocationTargetError extends Error", self.text)

    def test_serial_version_uid_unchanged(self):
        match = re.search(r'private\s+static\s+final\s+long\s+serialVersionUID\s*=\s*(-?[0-9L]+)', self.text)
        self.assertIsNotNone(match)
        # The existing serialVersionUID in XposedHelpers is -1070936889459514628L.
        self.assertEqual("-1070936889459514628L", match.group(1).strip())

    def test_no_printStackTrace_globally(self):
        for path in (REPO / "app" / "src" / "main" / "java").rglob("*"):
            if path.is_file() and path.suffix in (".kt", ".java"):
                with self.subTest(path=path.relative_to(REPO)):
                    text = path.read_text(encoding="utf-8")
                    self.assertIsNone(
                        re.search(r"\.printStackTrace\s*\(", text),
                        f"{path} still contains printStackTrace",
                    )

    def test_source_hazard_baseline_empty(self):
        baseline = REPO / "docs" / "audit" / "SOURCE_HAZARD_BASELINE.json"
        text = baseline.read_text(encoding="utf-8")
        self.assertIn('"schema": 1', text)
        self.assertIn('"fingerprints": []', text)
        self.assertIn('"findings": []', text)

    def _extract_helper_body(self):
        match = re.search(
            r'private\s+static\s+InvocationTargetError\s+invocationTargetError\s*\(\s*InvocationTargetException\s+\w+\s*\)',
            self.text
        )
        if not match:
            return None
        brace_start = self.text.find("{", match.start())
        return self._brace_body(self.text, brace_start)

    def _count_entry_call_helper(self, method_name):
        pattern = re.compile(rf'public\s+static\s+.*\s+{re.escape(method_name)}\s*\(')
        count = 0
        for match in pattern.finditer(self.text):
            brace_start = self.text.find("{", match.start())
            body = self._brace_body(self.text, brace_start)
            if body and "throw invocationTargetError" in body:
                count += 1
        return count

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
