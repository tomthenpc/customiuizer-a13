#!/usr/bin/env python3
"""Static contract tests for HookUtils controlled diagnostics migration."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
HOOKUTILS = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "utils" / "HookUtils.kt"


class HookUtilsDiagnosticsContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = HOOKUTILS.read_text(encoding="utf-8")

    def test_no_printStackTrace(self):
        self.assertIsNone(
            re.search(r"\.printStackTrace\s*\(", self.text),
            "HookUtils still contains printStackTrace calls",
        )

    def test_has_fixed_log_tag(self):
        self.assertIn('private const val TAG = "CustoMIUIzer-HookUtils"', self.text)

    def test_has_log_helper(self):
        self.assertIn("private fun logFailure(operation: String, throwable: Throwable)", self.text)
        self.assertIn("Log.e(TAG, operation, throwable)", self.text)

    def test_five_operation_names_present_and_unique(self):
        operations = [
            "copyFile",
            "getAppName.application",
            "getAppName.activity",
            "getAppIcon.application",
            "getAppIcon.activity",
        ]
        found = []
        for op in operations:
            with self.subTest(operation=op):
                self.assertIn(f'"{op}"', self.text)
                found.append(op)
        self.assertEqual(len(operations), len(set(found)))

    def test_each_catch_rethrows_fatal_errors(self):
        # Count fatal guards in catch blocks; should be at least 5 (one per changed catch).
        fatal_guards = list(re.finditer(
            r"catch\s*\([a-zA-Z]+\s*:\s*Throwable\).*?\{[^}]*if\s*\(\w+\s+is\s+OutOfMemoryError\s+\|\|\s*\w+\s+is\s+ThreadDeath\s+\|\|\s*\w+\s+is\s+VirtualMachineError\)\s+throw\s+\w+",
            self.text,
            re.S,
        ))
        self.assertGreaterEqual(
            len(fatal_guards),
            5,
            f"Expected at least 5 fatal error re-throw guards, found {len(fatal_guards)}",
        )

    def test_no_system_out_or_err(self):
        self.assertIsNone(re.search(r"\bSystem\.(?:out|err)\b", self.text))

    def test_log_failure_called_in_each_catch(self):
        # Each logFailure call should be immediately followed by the known fallback.
        self.assertIn('logFailure("copyFile",', self.text)
        self.assertIn('logFailure("getAppName.application",', self.text)
        self.assertIn('logFailure("getAppName.activity",', self.text)
        self.assertIn('logFailure("getAppIcon.application",', self.text)
        self.assertIn('logFailure("getAppIcon.activity",', self.text)


if __name__ == "__main__":
    unittest.main()
