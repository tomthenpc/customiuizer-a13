#!/usr/bin/env python3
"""Static contract tests for ModuleHelper reflection fallback fatal propagation."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
FILE = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils" / "ModuleHelper.java"


FIELD_METHODS = (
    "public static Object getStaticObjectFieldSilently(Class <?> clazz, String fieldName)",
    "public static Object getObjectFieldSilently(Object obj, String fieldName)",
)

CONTEXT_METHODS = (
    "public static Context findContext()",
    "public static Context findContext(XposedModuleInterface.PackageReadyParam lpparam)",
)

TARGET_METHODS = FIELD_METHODS + CONTEXT_METHODS

HOOK_INSTALL_METHODS = [
    "public static CustomMethodUnhooker hookMethod(Method method, MethodHook callback)",
    "public static CustomMethodUnhooker findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback)",
    "public static CustomMethodUnhooker findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback)",
    "public static boolean findAndHookMethodSilently(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback)",
    "public static boolean findAndHookMethodSilently(Class<?> clazz, String methodName, Object... parameterTypesAndCallback)",
    "public static CustomMethodUnhooker findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback)",
    "public static void hookAllConstructors(String className, ClassLoader classLoader, MethodHook callback)",
    "public static void hookAllConstructors(Class<?> hookClass, MethodHook callback)",
    "public static void hookAllMethods(String className, ClassLoader classLoader, String methodName, MethodHook callback)",
    "public static void hookAllMethods(Class<?> hookClass, String methodName, MethodHook callback)",
    "public static boolean hookAllMethodsSilently(String className, ClassLoader classLoader, String methodName, MethodHook callback)",
    "public static boolean hookAllMethodsSilently(Class<?> hookClass, String methodName, MethodHook callback)",
]


class ModuleHelperReflectionFallbackFatalityContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = FILE.read_text(encoding="utf-8")

    # ------------------------------------------------------------------
    # 1. Four target method identification and catch shape
    # ------------------------------------------------------------------

    def test_four_target_methods_exist(self):
        for sig in TARGET_METHODS:
            with self.subTest(sig=sig):
                self.assertIsNotNone(
                    self._extract_method_body(sig),
                    f"Could not find target method: {sig}",
                )

    def test_each_target_has_exactly_one_catch(self):
        for sig in TARGET_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                matches = list(re.finditer(r"catch\s*\(\s*Throwable\s+t\s*\)\s*\{", body))
                self.assertEqual(1, len(matches), f"{sig} must have exactly one catch (Throwable t)")

    def test_each_target_catch_calls_throw_if_fatal(self):
        for sig in TARGET_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertIn("throwIfFatal(t);", catch)

    def test_each_target_catch_has_only_one_throw_if_fatal(self):
        for sig in TARGET_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertEqual(1, catch.count("throwIfFatal(t)"))

    def test_throw_if_fatal_is_first_statement_in_target_catch(self):
        for sig in TARGET_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                stripped = self._strip_comments(catch)
                self.assertRegex(
                    stripped,
                    r"^\s*throwIfFatal\s*\(\s*t\s*\)\s*;",
                    f"throwIfFatal(t) must be the first statement in the catch of {sig}",
                )

    def test_target_catches_have_no_direct_fatal_instanceof(self):
        for sig in TARGET_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertNotIn("t instanceof OutOfMemoryError", catch)
                self.assertNotIn("t instanceof ThreadDeath", catch)
                self.assertNotIn("t instanceof VirtualMachineError", catch)

    def test_target_methods_have_no_reflection_fatality(self):
        for sig in TARGET_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                self.assertNotIn("ReflectionFatality", body)

    def test_target_methods_have_no_logging(self):
        for sig in TARGET_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                self.assertNotIn("log(", body)

    # ------------------------------------------------------------------
    # 8-12. Field helper contract
    # ------------------------------------------------------------------

    def test_static_field_helper_calls_xposed_helpers(self):
        body = self._extract_method_body(FIELD_METHODS[0])
        self.assertIsNotNone(body)
        self.assertIn("XposedHelpers.getStaticObjectField(clazz, fieldName)", body)

    def test_instance_field_helper_calls_xposed_helpers(self):
        body = self._extract_method_body(FIELD_METHODS[1])
        self.assertIsNotNone(body)
        self.assertIn("XposedHelpers.getObjectField(obj, fieldName)", body)

    def test_field_helpers_return_not_exist_symbol(self):
        for sig in FIELD_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertIn("throwIfFatal(t);", catch)
                self.assertIn("return NOT_EXIST_SYMBOL", catch)

    def test_field_helpers_do_not_return_null_false_or_new_sentinel(self):
        for sig in FIELD_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertNotIn("return null", catch)
                self.assertNotIn("return false", catch)
                self.assertNotIn("return true", catch)
                self.assertNotIn("new ", catch)

    def test_not_exist_symbol_value(self):
        match = re.search(
            r'public\s+static\s+final\s+String\s+NOT_EXIST_SYMBOL\s*=\s*"([^"]+)"\s*;',
            self.text,
        )
        self.assertIsNotNone(match)
        self.assertEqual("ObjectFieldNotExist", match.group(1))

    # ------------------------------------------------------------------
    # 13-21. findContext contract
    # ------------------------------------------------------------------

    def test_find_context_zero_arg_uses_main_module_class_loader(self):
        body = self._extract_method_body(CONTEXT_METHODS[0])
        self.assertIsNotNone(body)
        self.assertIn("MainModule.class.getClassLoader()", body)

    def test_find_context_param_uses_lpparam_class_loader(self):
        body = self._extract_method_body(CONTEXT_METHODS[1])
        self.assertIsNotNone(body)
        self.assertIn("lpparam.getClassLoader()", body)

    def test_find_context_zero_arg_does_not_use_lpparam(self):
        body = self._extract_method_body(CONTEXT_METHODS[0])
        self.assertIsNotNone(body)
        self.assertNotIn("lpparam", body)

    def test_find_context_param_does_not_use_main_module_class_loader(self):
        body = self._extract_method_body(CONTEXT_METHODS[1])
        self.assertIsNotNone(body)
        self.assertNotIn("MainModule.class.getClassLoader()", body)

    def test_both_find_context_try_current_application_first(self):
        for sig in CONTEXT_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                try_body = self._extract_try_body(body)
                self.assertIsNotNone(try_body)
                self.assertIn("sCurrentApplicationMethod.invoke(null)", try_body)

    def test_find_context_only_calls_activity_thread_when_context_null(self):
        for sig in CONTEXT_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                try_body = self._extract_try_body(body)
                self.assertIsNotNone(try_body)
                self.assertIn("if (context == null)", try_body)
                app_pos = try_body.find("sCurrentApplicationMethod.invoke(null)")
                activity_pos = try_body.find("sCurrentActivityThreadMethod.invoke(null)")
                self.assertLess(app_pos, activity_pos)

    def test_find_context_keeps_get_system_context(self):
        for sig in CONTEXT_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                try_body = self._extract_try_body(body)
                self.assertIsNotNone(try_body)
                self.assertIn("sGetSystemContextMethod.invoke(currentActivityThread)", try_body)

    def test_both_find_context_return_local_context(self):
        for sig in CONTEXT_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                # The final return statement must return the local variable `context`.
                self.assertIn("return context;", body)

    def test_find_context_catch_does_not_return_null(self):
        for sig in CONTEXT_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertNotIn("return null", catch)

    def test_find_context_catch_does_not_nullify_context(self):
        for sig in CONTEXT_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertNotIn("context = null", catch)

    def test_find_context_catch_does_not_modify_activity_thread_cache(self):
        cache_fields = (
            "sActivityThreadClass",
            "sCurrentApplicationMethod",
            "sCurrentActivityThreadMethod",
            "sGetSystemContextMethod",
        )
        for sig in CONTEXT_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                for field in cache_fields:
                    self.assertNotIn(f"{field} =", catch, f"catch must not modify {field}")

    def test_find_context_catch_no_retry_loop_sync_cache_reset(self):
        for sig in CONTEXT_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertNotIn("for (", catch)
                self.assertNotIn("while (", catch)
                self.assertNotIn("synchronized", catch)
                self.assertNotIn("reset", catch)

    # ------------------------------------------------------------------
    # 22-29. Scope protection
    # ------------------------------------------------------------------

    def test_throw_if_fatal_method_body_unchanged(self):
        body = self._extract_helper_body()
        self.assertIsNotNone(body)
        self.assertIn("instanceof OutOfMemoryError", body)
        self.assertIn("instanceof ThreadDeath", body)
        self.assertIn("instanceof VirtualMachineError", body)
        self.assertIn("throw (OutOfMemoryError) current", body)
        self.assertIn("throw (ThreadDeath) current", body)
        self.assertIn("throw (VirtualMachineError) current", body)
        self.assertIn("depth < 8", body)
        self.assertIn("current != null", body)
        self.assertIn("current.getCause()", body)
        self.assertIn("next == current", body)
        self.assertNotIn("instanceof Error", body)
        self.assertNotIn("ReflectionFatality", body)

    def test_twelve_hook_install_entries_still_call_throw_if_fatal(self):
        for sig in HOOK_INSTALL_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertIn("throwIfFatal(t);", catch)

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

    def test_receiver_registration_and_guards_unchanged(self):
        # Ensure register/clear/release receiver helpers still exist and are not replaced by throwIfFatal.
        for marker in (
            "registerReceiver",
            "unregisterReceiver",
            "moduleRegistrations.put",
            "moduleRegistrations.remove",
            "public static void guarded(Runnable block)",
            "public static String getActionName",
            "public static Drawable getActionImage",
            "public static long getNextMIUIAlarmTime",
        ):
            with self.subTest(marker=marker):
                self.assertIn(marker, self.text)

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
        pattern = re.escape(sig.strip()) + r"\s*\{"
        match = re.search(pattern, self.text)
        if not match:
            return None
        brace = self.text.find("{", match.start())
        return self._brace_body(self.text, brace)

    def _extract_try_body(self, body):
        match = re.search(r"try\s*\{", body)
        if not match:
            return None
        brace = body.find("{", match.start())
        return self._brace_body(body, brace)

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

    def _strip_comments(self, text):
        text = re.sub(r"//[^\n]*", "", text)
        text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
        return text


if __name__ == "__main__":
    unittest.main()
