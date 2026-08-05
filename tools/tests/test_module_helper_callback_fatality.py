#!/usr/bin/env python3
"""Static contract tests for ModuleHelper callback fatal unwrapping."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
FILE = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils" / "ModuleHelper.java"


CALLBACK_TARGET_METHODS = (
    "public static void handlePreferenceChanged(@Nullable String key)",
    "public static void guarded(Runnable block)",
    "static void guarded(String callbackName, Runnable block, CallbackFailureLogger failureLogger)",
    "public static <T> T guarded(T fallback, Callable<T> block)",
    "static <T> T guarded(String callbackName, T fallback, Callable<T> block, CallbackFailureLogger failureLogger)",
)

GUARDED_FORWARDING_OVERLOADS = (
    "public static void guarded(String callbackName, Runnable block)",
    "public static <T> T guarded(String callbackName, T fallback, Callable<T> block)",
)

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

REFLECTION_FALLBACK_METHODS = (
    "public static Object getStaticObjectFieldSilently(Class <?> clazz, String fieldName)",
    "public static Object getObjectFieldSilently(Object obj, String fieldName)",
    "public static Context findContext()",
    "public static Context findContext(XposedModuleInterface.PackageReadyParam lpparam)",
)

RECEIVER_METHODS = (
    "public static boolean registerModuleReceiver(Context context, String key, BroadcastReceiver receiver, IntentFilter filter, int flags)",
    "public static boolean registerOwnedReceiver(Context context, Object owner, String key, BroadcastReceiver receiver, IntentFilter filter, int flags)",
    "private static boolean tryRelease(ReceiverRegistration registration)",
    "private static void releaseReceiver(ReceiverRegistration registration)",
)

ACTION_ALARM_METHODS = (
    "public static long getNextMIUIAlarmTime(Context context)",
    "public static void openAppInfo(Context context, String pkg, int user)",
    "public static Drawable getActionImage(Context context, String key)",
    "public static String getActionName(Context context, String key)",
)


class ModuleHelperCallbackFatalityContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = FILE.read_text(encoding="utf-8")

    # ------------------------------------------------------------------
    # 1-13. handlePreferenceChanged contract
    # ------------------------------------------------------------------

    def test_handle_preference_changed_method_exists(self):
        body = self._extract_method_body("public static void handlePreferenceChanged(@Nullable String key)")
        self.assertIsNotNone(body)

    def test_handle_preference_changed_has_three_catch_throwable_t(self):
        body = self._extract_method_body("public static void handlePreferenceChanged(@Nullable String key)")
        self.assertIsNotNone(body)
        catches = self._extract_all_catch_bodies_by_var(body, "t")
        self.assertEqual(3, len(catches))

    def test_all_three_observer_catches_call_throw_if_fatal_first(self):
        body = self._extract_method_body("public static void handlePreferenceChanged(@Nullable String key)")
        self.assertIsNotNone(body)
        catches = self._extract_all_catch_bodies_by_var(body, "t")
        self.assertEqual(3, len(catches))
        for i, catch in enumerate(catches):
            with self.subTest(index=i):
                stripped = self._strip_comments(catch)
                self.assertRegex(
                    stripped,
                    r"^\s*throwIfFatal\s*\(\s*t\s*\)\s*;",
                    f"throwIfFatal(t) must be first statement in observer catch {i}",
                )
                self.assertIn("log(t)", catch)

    def test_all_three_observer_catches_call_throw_if_fatal_once(self):
        body = self._extract_method_body("public static void handlePreferenceChanged(@Nullable String key)")
        self.assertIsNotNone(body)
        catches = self._extract_all_catch_bodies_by_var(body, "t")
        for i, catch in enumerate(catches):
            with self.subTest(index=i):
                self.assertEqual(1, catch.count("throwIfFatal(t);"))

    def test_observer_catches_have_no_direct_fatal_instanceof(self):
        body = self._extract_method_body("public static void handlePreferenceChanged(@Nullable String key)")
        self.assertIsNotNone(body)
        catches = self._extract_all_catch_bodies_by_var(body, "t")
        for catch in catches:
            self.assertNotIn("t instanceof OutOfMemoryError", catch)
            self.assertNotIn("t instanceof ThreadDeath", catch)
            self.assertNotIn("t instanceof VirtualMachineError", catch)

    def test_observer_catches_do_not_call_get_cause(self):
        body = self._extract_method_body("public static void handlePreferenceChanged(@Nullable String key)")
        self.assertIsNotNone(body)
        catches = self._extract_all_catch_bodies_by_var(body, "t")
        for catch in catches:
            self.assertNotIn("t.getCause()", catch)

    def test_pref_observers_loop_present(self):
        body = self._extract_method_body("public static void handlePreferenceChanged(@Nullable String key)")
        self.assertIn("for (PreferenceObserver prefObserver : prefObservers)", body)

    def test_keyed_pref_observers_loop_present(self):
        body = self._extract_method_body("public static void handlePreferenceChanged(@Nullable String key)")
        self.assertIn("for (PreferenceObserver prefObserver : keyedPrefObservers.values())", body)

    def test_owned_pref_observers_loop_present(self):
        body = self._extract_method_body("public static void handlePreferenceChanged(@Nullable String key)")
        self.assertIn("for (OwnedPreferenceObserver registration : ownedPrefObservers)", body)

    def test_owned_observer_both_call_paths_present(self):
        body = self._extract_method_body("public static void handlePreferenceChanged(@Nullable String key)")
        self.assertIn("registration.callback.onChange(owner, key)", body)
        self.assertIn("prefObserver.onChange(key)", body)

    def test_saw_cleared_and_final_cleanup_present(self):
        body = self._extract_method_body("public static void handlePreferenceChanged(@Nullable String key)")
        self.assertIn("boolean sawCleared = false", body)
        self.assertIn("if (sawCleared) dropOwnedObserver(null, null)", body)

    def test_observer_ordinary_catches_do_not_throw_or_remove(self):
        body = self._extract_method_body("public static void handlePreferenceChanged(@Nullable String key)")
        self.assertIsNotNone(body)
        catches = self._extract_all_catch_bodies_by_var(body, "t")
        for catch in catches:
            self.assertNotIn("throw t", catch)
            self.assertNotIn("remove", catch)

    # ------------------------------------------------------------------
    # 14-26. guarded execution contract
    # ------------------------------------------------------------------

    def test_four_guarded_execution_methods_exist(self):
        for sig in CALLBACK_TARGET_METHODS[1:]:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body, f"Could not find {sig}")

    def test_each_guarded_execution_catch_calls_throw_if_fatal(self):
        for sig in CALLBACK_TARGET_METHODS[1:]:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch, f"{sig} missing catch")
                self.assertIn("throwIfFatal(t);", catch)

    def test_each_guarded_catch_has_exactly_one_throw_if_fatal(self):
        for sig in CALLBACK_TARGET_METHODS[1:]:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                catch = self._extract_catch_body(body)
                self.assertEqual(1, catch.count("throwIfFatal(t);"))

    def test_guarded_catch_throw_if_fatal_is_first_statement(self):
        for sig in CALLBACK_TARGET_METHODS[1:]:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                catch = self._extract_catch_body(body)
                stripped = self._strip_comments(catch)
                self.assertRegex(
                    stripped,
                    r"^\s*throwIfFatal\s*\(\s*t\s*\)\s*;",
                    f"throwIfFatal(t) must be first statement in {sig}",
                )

    def test_guarded_catches_have_no_direct_fatal_instanceof(self):
        for sig in CALLBACK_TARGET_METHODS[1:]:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                catch = self._extract_catch_body(body)
                self.assertNotIn("t instanceof OutOfMemoryError", catch)
                self.assertNotIn("t instanceof ThreadDeath", catch)
                self.assertNotIn("t instanceof VirtualMachineError", catch)

    def test_guarded_catches_do_not_call_get_cause(self):
        for sig in CALLBACK_TARGET_METHODS[1:]:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                catch = self._extract_catch_body(body)
                self.assertNotIn("t.getCause()", catch)

    def test_unnamed_runnable_guarded_calls_block_run(self):
        body = self._extract_method_body("public static void guarded(Runnable block)")
        self.assertIn("block.run()", body)

    def test_named_runnable_guarded_calls_block_run(self):
        body = self._extract_method_body("static void guarded(String callbackName, Runnable block, CallbackFailureLogger failureLogger)")
        self.assertIn("block.run()", body)

    def test_unnamed_returning_guarded_calls_block_call(self):
        body = self._extract_method_body("public static <T> T guarded(T fallback, Callable<T> block)")
        self.assertIn("block.call()", body)

    def test_named_returning_guarded_calls_block_call(self):
        body = self._extract_method_body("static <T> T guarded(String callbackName, T fallback, Callable<T> block, CallbackFailureLogger failureLogger)")
        self.assertIn("block.call()", body)

    def test_unnamed_void_ordinary_failure_logs(self):
        body = self._extract_method_body("public static void guarded(Runnable block)")
        catch = self._extract_catch_body(body)
        self.assertIn("log(t)", catch)

    def test_named_void_ordinary_failure_calls_log_guarded_failure(self):
        body = self._extract_method_body("static void guarded(String callbackName, Runnable block, CallbackFailureLogger failureLogger)")
        catch = self._extract_catch_body(body)
        self.assertIn("logGuardedFailure(callbackName, t, failureLogger)", catch)

    def test_unnamed_returning_ordinary_failure_returns_fallback(self):
        body = self._extract_method_body("public static <T> T guarded(T fallback, Callable<T> block)")
        catch = self._extract_catch_body(body)
        self.assertIn("return fallback", catch)

    def test_named_returning_ordinary_failure_returns_fallback(self):
        body = self._extract_method_body("static <T> T guarded(String callbackName, T fallback, Callable<T> block, CallbackFailureLogger failureLogger)")
        catch = self._extract_catch_body(body)
        self.assertIn("return fallback", catch)

    # ------------------------------------------------------------------
    # 27-30. Forwarding overloads
    # ------------------------------------------------------------------

    def test_public_runnable_overload_contains_no_try_catch(self):
        body = self._extract_method_body("public static void guarded(String callbackName, Runnable block)")
        self.assertIsNotNone(body)
        self.assertNotIn("try {", body)
        self.assertNotIn("catch (Throwable", body)

    def test_public_runnable_overload_delegates_with_callback_failure_logger(self):
        body = self._extract_method_body("public static void guarded(String callbackName, Runnable block)")
        self.assertIn("CALLBACK_FAILURE_LOGGER", body)

    def test_public_returning_overload_contains_no_try_catch(self):
        body = self._extract_method_body("public static <T> T guarded(String callbackName, T fallback, Callable<T> block)")
        self.assertIsNotNone(body)
        self.assertNotIn("try {", body)
        self.assertNotIn("catch (Throwable", body)

    def test_public_returning_overload_delegates_with_callback_failure_logger(self):
        body = self._extract_method_body("public static <T> T guarded(String callbackName, T fallback, Callable<T> block)")
        self.assertIn("CALLBACK_FAILURE_LOGGER", body)

    # ------------------------------------------------------------------
    # 31-36. Log-once contract
    # ------------------------------------------------------------------

    def test_callback_failure_logger_interface_unchanged(self):
        self.assertIn("interface CallbackFailureLogger", self.text)
        self.assertIn("void log(String callbackName, Throwable failure)", self.text)

    def test_default_callback_failure_logger_unchanged(self):
        self.assertIn("CALLBACK_FAILURE_LOGGER = XposedHelpers::log", self.text)

    def test_logged_callback_failures_still_concurrent_hash_map(self):
        self.assertIn("ConcurrentHashMap<String, Boolean> loggedCallbackFailures", self.text)

    def test_log_guarded_failure_still_uses_put_if_absent(self):
        body = self._extract_method_body("private static void logGuardedFailure(String callbackName, Throwable failure, CallbackFailureLogger failureLogger)")
        self.assertIsNotNone(body)
        self.assertIn("putIfAbsent(callbackName, Boolean.TRUE)", body)

    def test_ordinary_named_logger_receives_callback_name_and_failure(self):
        body = self._extract_method_body("private static void logGuardedFailure(String callbackName, Throwable failure, CallbackFailureLogger failureLogger)")
        self.assertIn("failureLogger.log(callbackName, failure)", body)

    def test_throw_if_fatal_comes_before_log_guarded_failure(self):
        body = self._extract_method_body("static void guarded(String callbackName, Runnable block, CallbackFailureLogger failureLogger)")
        catch = self._extract_catch_body(body)
        fatal_pos = catch.find("throwIfFatal(t)")
        log_pos = catch.find("logGuardedFailure")
        self.assertNotEqual(-1, fatal_pos)
        self.assertNotEqual(-1, log_pos)
        self.assertLess(fatal_pos, log_pos)

    # ------------------------------------------------------------------
    # 37-45. Global convergence
    # ------------------------------------------------------------------

    def test_throw_if_fatal_helper_unchanged(self):
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

    def test_no_direct_fatal_instanceof_outside_helper(self):
        helper_body = self._extract_helper_body()
        rest = self.text
        if helper_body:
            rest = rest.replace(helper_body, "", 1)
        for pattern in (
            "t instanceof OutOfMemoryError",
            "t instanceof ThreadDeath",
            "t instanceof VirtualMachineError",
            "t2 instanceof OutOfMemoryError",
        ):
            with self.subTest(pattern=pattern):
                self.assertNotIn(pattern, rest)

    def test_previous_hook_reflection_receiver_action_alarm_still_use_helper(self):
        all_sigs = list(HOOK_INSTALL_METHODS) + list(REFLECTION_FALLBACK_METHODS) + list(RECEIVER_METHODS) + list(ACTION_ALARM_METHODS)
        for sig in all_sigs:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertIn("throwIfFatal", catch)

    def test_alarm_intent_receiver_state_machine_unchanged(self):
        for sig in ACTION_ALARM_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)

    def test_source_hazard_baseline_empty(self):
        baseline = REPO / "docs" / "audit" / "SOURCE_HAZARD_BASELINE.json"
        text = baseline.read_text(encoding="utf-8")
        self.assertIn('"schema": 1', text)
        self.assertIn('"fingerprints": []', text)
        self.assertIn('"findings": []', text)

    def test_no_print_stack_trace_globally(self):
        for path in (REPO / "app" / "src" / "main" / "java").rglob("*"):
            if path.is_file() and path.suffix in (".kt", ".java"):
                with self.subTest(path=path.relative_to(REPO)):
                    text = path.read_text(encoding="utf-8")
                    self.assertIsNone(
                        re.search(r"\.printStackTrace\s*\(", text),
                        f"{path} contains printStackTrace",
                    )

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
        tokens = re.findall(r"[A-Za-z_][A-Za-z0-9_]*|[^\sA-Za-z0-9_]", sig.strip())
        pattern = r"\s*".join(re.escape(t) for t in tokens) + r"\s*\{"
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

    def _extract_all_catch_bodies_by_var(self, body, var):
        catches = []
        start = 0
        while True:
            match = re.search(rf"catch\s*\(\s*Throwable\s+{re.escape(var)}\s*\)\s*\{{", body[start:])
            if not match:
                break
            brace = body.find("{", start + match.start())
            catch_body = self._brace_body(body, brace)
            catches.append(catch_body)
            start = brace + 1
        return catches

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
