#!/usr/bin/env python3
"""Static contract tests for HookerClassHelper fatal propagation."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
FILE = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils" / "HookerClassHelper.java"


class HookerClassHelperFatalityContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = FILE.read_text(encoding="utf-8")

    def test_helper_exists_unique(self):
        matches = list(re.finditer(
            r'private\s+static\s+void\s+throwIfFatal\s*\(\s*Throwable\s+\w+\s*\)',
            self.text
        ))
        self.assertEqual(1, len(matches), "throwIfFatal must exist exactly once")

    def test_old_helper_name_removed(self):
        self.assertNotIn("throwIfOutOfMemory", self.text)

    def test_helper_checks_three_fatals(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertIn("instanceof OutOfMemoryError", body)
        self.assertIn("instanceof ThreadDeath", body)
        self.assertIn("instanceof VirtualMachineError", body)

    def test_helper_rethrows_current(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertIn("throw (OutOfMemoryError) current", body)
        self.assertIn("throw (ThreadDeath) current", body)
        self.assertIn("throw (VirtualMachineError) current", body)

    def test_helper_does_not_check_all_errors(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertNotIn("instanceof Error", body)

    def test_helper_depth_limit_is_eight(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertIn("depth < 8", body)

    def test_helper_null_check(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertIn("current != null", body)

    def test_helper_uses_get_cause(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertIn("current.getCause()", body)

    def test_helper_self_reference_guard(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertIn("next == current", body)

    def test_helper_no_logging(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertNotIn("XposedHelpers.log", body)
        self.assertNotIn("Log.", body)

    def test_helper_no_cause_modification(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertNotIn("setCause", body)
        self.assertNotIn("initCause", body)

    def test_before_hook_calls_throw_if_fatal(self):
        body = self._extract_method_body("beforeHook")
        self.assertIsNotNone(body)
        self.assertIn("throwIfFatal(t)", body)

    def test_before_helper_before_log(self):
        body = self._extract_method_body("beforeHook")
        self.assertIsNotNone(body)
        fatal_pos = body.find("throwIfFatal")
        log_pos = body.find("XposedHelpers.log")
        self.assertGreater(fatal_pos, -1)
        self.assertGreater(log_pos, -1)
        self.assertLess(fatal_pos, log_pos)

    def test_after_hook_calls_throw_if_fatal(self):
        body = self._extract_method_body("afterHook")
        self.assertIsNotNone(body)
        self.assertIn("throwIfFatal(t)", body)

    def test_after_helper_before_log(self):
        body = self._extract_method_body("afterHook")
        self.assertIsNotNone(body)
        fatal_pos = body.find("throwIfFatal")
        log_pos = body.find("XposedHelpers.log")
        self.assertGreater(fatal_pos, -1)
        self.assertGreater(log_pos, -1)
        self.assertLess(fatal_pos, log_pos)

    def test_host_path_calls_throw_if_fatal(self):
        body = self._extract_method_body("intercept")
        self.assertIsNotNone(body)
        self.assertIn("throwIfFatal(throwable)", body)

    def test_host_helper_before_has_after(self):
        body = self._extract_method_body("intercept")
        self.assertIsNotNone(body)
        fatal_pos = body.find("throwIfFatal(throwable)")
        has_after_pos = body.find("if (hasAfter)")
        self.assertGreater(fatal_pos, -1)
        self.assertGreater(has_after_pos, -1)
        self.assertLess(fatal_pos, has_after_pos)

    def test_three_call_sites(self):
        call_count = self.text.count("throwIfFatal(") - 1
        self.assertEqual(3, call_count, "throwIfFatal must be called in three places")

    def test_ordinary_before_after_logs(self):
        for method in ("beforeHook", "afterHook"):
            body = self._extract_method_body(method)
            self.assertIsNotNone(body)
            self.assertIn("XposedHelpers.log(t)", body)

    def test_chain_proceed_logic_unchanged(self):
        body = self._extract_method_body("intercept")
        self.assertIsNotNone(body)
        self.assertIn("chain.proceed()", body)
        self.assertIn("chain.proceed(before.getArgs())", body)

    def test_do_nothing_and_return_constant_present(self):
        self.assertIn("public static final MethodHook DO_NOTHING", self.text)
        self.assertIn("public static MethodHook returnConstant", self.text)

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
            r'private\s+static\s+void\s+throwIfFatal\s*\(\s*Throwable\s+\w+\s*\)',
            self.text
        )
        if not match:
            return None
        brace_start = self.text.find("{", match.start())
        return self._brace_body(self.text, brace_start)

    def _extract_method_body(self, method_name):
        match = re.search(rf'\s+(?:public|private|protected)\s+(?:final\s+)?(?:\w+\s+)?{re.escape(method_name)}\s*\(', self.text)
        if not match:
            return None
        # Find opening brace after the method signature, not after a parameter type.
        brace_start = self.text.find("{", match.start())
        return self._brace_body(self.text, brace_start)

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
