#!/usr/bin/env python3
"""Static contract tests for ModuleHelper hook install fatal propagation."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
FILE = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils" / "ModuleHelper.java"


TARGET_METHODS = [
    (
        "public static CustomMethodUnhooker hookMethod(Method method, MethodHook callback)",
        "null",
    ),
    (
        "public static CustomMethodUnhooker findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback)",
        "null",
    ),
    (
        "public static CustomMethodUnhooker findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback)",
        "null",
    ),
    (
        "public static boolean findAndHookMethodSilently(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback)",
        "false",
    ),
    (
        "public static boolean findAndHookMethodSilently(Class<?> clazz, String methodName, Object... parameterTypesAndCallback)",
        "false",
    ),
    (
        "public static CustomMethodUnhooker findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback)",
        "null",
    ),
    (
        "public static void hookAllConstructors(String className, ClassLoader classLoader, MethodHook callback)",
        "void",
    ),
    (
        "public static void hookAllConstructors(Class<?> hookClass, MethodHook callback)",
        "void",
    ),
    (
        "public static void hookAllMethods(String className, ClassLoader classLoader, String methodName, MethodHook callback)",
        "void",
    ),
    (
        "public static void hookAllMethods(Class<?> hookClass, String methodName, MethodHook callback)",
        "void",
    ),
    (
        "public static boolean hookAllMethodsSilently(String className, ClassLoader classLoader, String methodName, MethodHook callback)",
        "false",
    ),
    (
        "public static boolean hookAllMethodsSilently(Class<?> hookClass, String methodName, MethodHook callback)",
        "false",
    ),
]


class ModuleHelperHookInstallFatalityContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = FILE.read_text(encoding="utf-8")

    # ------------------------------------------------------------------
    # Helper contract
    # ------------------------------------------------------------------

    def test_helper_exists_unique(self):
        matches = list(re.finditer(
            r'(?<!\w)\s+static\s+void\s+throwIfFatal\s*\(\s*Throwable\s+\w+\s*\)',
            self.text,
        ))
        self.assertEqual(1, len(matches), "throwIfFatal must exist exactly once in ModuleHelper")

    def test_helper_is_package_private(self):
        match = re.search(
            r'(?<!\w)\s+static\s+void\s+throwIfFatal\s*\(\s*Throwable\s+\w+\s*\)',
            self.text,
        )
        self.assertIsNotNone(match)
        line = None
        for ln in self.text.splitlines():
            if "static void throwIfFatal" in ln:
                line = ln
                break
        self.assertIsNotNone(line)
        self.assertIn("static void throwIfFatal", line)
        self.assertNotIn("public", line)
        self.assertNotIn("private", line)

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
        self.assertNotIn("log(", body)

    def test_helper_no_hook_installer(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertNotIn("HookInstaller", body)

    def test_helper_no_reflection_fatality(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertNotIn("ReflectionFatality", body)

    def test_helper_no_new_exceptions(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertNotIn("new OutOfMemoryError", body)
        self.assertNotIn("new ThreadDeath", body)
        self.assertNotIn("new VirtualMachineError", body)

    # ------------------------------------------------------------------
    # 12 target method contract
    # ------------------------------------------------------------------

    def test_twelve_target_methods_exist(self):
        for sig, _ in TARGET_METHODS:
            with self.subTest(sig=sig):
                self.assertIsNotNone(
                    self._extract_method_body(sig),
                    f"Could not find target method: {sig}",
                )

    def test_each_target_catch_calls_throw_if_fatal(self):
        for sig, _ in TARGET_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertIn("throwIfFatal(t);", catch)

    def test_each_target_has_only_one_throw_if_fatal(self):
        for sig, _ in TARGET_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertEqual(1, catch.count("throwIfFatal(t)"))

    def test_no_direct_out_of_memory_check_in_target_catches(self):
        for sig, _ in TARGET_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertNotIn("t instanceof OutOfMemoryError", catch)
                self.assertNotIn("t instanceof ThreadDeath", catch)
                self.assertNotIn("t instanceof VirtualMachineError", catch)

    def test_throw_if_fatal_before_record(self):
        for sig, _ in TARGET_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                fatal_pos = catch.find("throwIfFatal(t)")
                record_pos = catch.find("recordHookFailure")
                hook_installer_pos = catch.find("HookInstaller.record")
                if record_pos != -1:
                    self.assertLess(fatal_pos, record_pos)
                if hook_installer_pos != -1:
                    self.assertLess(fatal_pos, hook_installer_pos)

    def test_throw_if_fatal_before_log(self):
        for sig, _ in TARGET_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                fatal_pos = catch.find("throwIfFatal(t)")
                log_pos = catch.find("log(")
                if log_pos != -1:
                    self.assertLess(fatal_pos, log_pos)

    def test_throw_if_fatal_before_return(self):
        for sig, _ in TARGET_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                fatal_pos = catch.find("throwIfFatal(t)")
                return_pos = catch.find("return ")
                if return_pos != -1:
                    self.assertLess(fatal_pos, return_pos)

    def test_target_fallback_types_unchanged(self):
        for sig, expected in TARGET_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                if expected == "void":
                    self.assertNotIn("return ", catch, f"{sig} must remain void")
                elif expected == "null":
                    self.assertIn("return null", catch)
                elif expected == "false":
                    self.assertIn("return false", catch)

    # ------------------------------------------------------------------
    # Scope protection
    # ------------------------------------------------------------------

    def test_record_hook_failure_unchanged(self):
        body = self._extract_method_body(
            "private static void recordHookFailure(String className, String memberName, HookOperation operation, Class<?>[] parameterTypes, Throwable t)"
        )
        self.assertIsNotNone(body)
        self.assertIn("HookInstaller.recordFailure", body)

    def test_extract_parameter_types_unchanged(self):
        body = self._extract_method_body(
            "private static Class<?>[] extractParameterTypes(Object... parameterTypesAndCallback)"
        )
        self.assertIsNotNone(body)
        self.assertIn("parameterTypesAndCallback", body)

    def test_non_target_field_helpers_unchanged(self):
        for sig in (
            "public static Object getStaticObjectFieldSilently(Class <?> clazz, String fieldName)",
            "public static Object getObjectFieldSilently(Object obj, String fieldName)",
        ):
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertIn("if (t instanceof OutOfMemoryError)", catch)
                self.assertIn("return NOT_EXIST_SYMBOL", catch)

    def test_no_print_stack_trace_globally(self):
        for path in (REPO / "app" / "src" / "main" / "java").rglob("*"):
            if path.is_file() and path.suffix in (".kt", ".java"):
                with self.subTest(path=path.relative_to(REPO)):
                    text = path.read_text(encoding="utf-8")
                    self.assertIsNone(
                        re.search(r"\.printStackTrace\s*\(", text),
                        f"{path} contains printStackTrace",
                    )

    def test_source_hazard_baseline_empty(self):
        baseline = REPO / "docs" / "audit" / "SOURCE_HAZARD_BASELINE.json"
        text = baseline.read_text(encoding="utf-8")
        self.assertIn('"schema": 1', text)
        self.assertIn('"fingerprints": []', text)
        self.assertIn('"findings": []', text)

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------

    def _extract_helper_body(self):
        match = re.search(
            r'(?<!\w)\s+static\s+void\s+throwIfFatal\s*\(\s*Throwable\s+\w+\s*\)',
            self.text,
        )
        if not match:
            return None
        brace = self.text.find("{", match.start())
        return self._brace_body(self.text, brace)

    def _extract_method_body(self, sig):
        # Match the signature line.  We expect it on its own line with leading whitespace.
        pattern = re.escape(sig.strip()) + r"\s*\{"
        match = re.search(pattern, self.text)
        if not match:
            return None
        brace = self.text.find("{", match.start())
        return self._brace_body(self.text, brace)

    def _extract_catch_body(self, body):
        match = re.search(r"catch\s*\(\s*Throwable\s+t\s*\)\s*\{", body)
        if not match:
            return None
        brace = body.find("{", match.start())
        return self._brace_body(body, brace)

    def _brace_body(self, text, brace_start):
        depth = 0
        in_string = None
        in_line_comment = False
        in_block_comment = False
        i = brace_start
        while i < len(text):
            c = text[i]
            if in_line_comment:
                if c == "\n":
                    in_line_comment = False
                i += 1
                continue
            if in_block_comment:
                if c == "*" and i + 1 < len(text) and text[i + 1] == "/":
                    in_block_comment = False
                    i += 2
                    continue
                i += 1
                continue
            if in_string:
                if c == "\\" and i + 1 < len(text):
                    i += 2
                    continue
                if c == in_string:
                    in_string = None
                i += 1
                continue
            if c == '"' or c == "'":
                in_string = c
                i += 1
                continue
            if c == "/" and i + 1 < len(text):
                nxt = text[i + 1]
                if nxt == "/":
                    in_line_comment = True
                    i += 2
                    continue
                if nxt == "*":
                    in_block_comment = True
                    i += 2
                    continue
            if c == "{":
                depth += 1
            elif c == "}":
                depth -= 1
                if depth == 0:
                    return text[brace_start + 1 : i]
            i += 1
        return None


if __name__ == "__main__":
    unittest.main()
