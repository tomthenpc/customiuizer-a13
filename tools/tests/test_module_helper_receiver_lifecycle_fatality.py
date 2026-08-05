#!/usr/bin/env python3
"""Static contract tests for ModuleHelper receiver lifecycle fatal propagation."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
FILE = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils" / "ModuleHelper.java"


RECEIVER_METHODS = (
    "public static boolean registerModuleReceiver(Context context, String key, BroadcastReceiver receiver, IntentFilter filter, int flags)",
    "public static boolean registerOwnedReceiver(Context context, Object owner, String key, BroadcastReceiver receiver, IntentFilter filter, int flags)",
    "private static boolean tryRelease(ReceiverRegistration registration)",
    "private static void releaseReceiver(ReceiverRegistration registration)",
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


class ModuleHelperReceiverLifecycleFatalityContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = FILE.read_text(encoding="utf-8")

    # ------------------------------------------------------------------
    # 1-6. Four target catch contract
    # ------------------------------------------------------------------

    def test_four_receiver_methods_exist(self):
        for sig in RECEIVER_METHODS:
            with self.subTest(sig=sig):
                self.assertIsNotNone(
                    self._extract_method_body(sig),
                    f"Could not find target method: {sig}",
                )

    def test_each_receiver_catch_calls_throw_if_fatal(self):
        for sig in RECEIVER_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertIn("throwIfFatal(t);", catch)

    def test_each_receiver_catch_has_only_one_throw_if_fatal(self):
        for sig in RECEIVER_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertEqual(1, catch.count("throwIfFatal(t)"))

    def test_throw_if_fatal_is_first_statement_in_receiver_catch(self):
        for sig in RECEIVER_METHODS:
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

    def test_receiver_catches_have_no_direct_fatal_instanceof(self):
        for sig in RECEIVER_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertNotIn("t instanceof OutOfMemoryError", catch)
                self.assertNotIn("t instanceof ThreadDeath", catch)
                self.assertNotIn("t instanceof VirtualMachineError", catch)

    def test_receiver_methods_have_no_reflection_fatality(self):
        for sig in RECEIVER_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                self.assertNotIn("ReflectionFatality", body)

    def test_throw_if_fatal_before_state_log_stale_return(self):
        for sig in RECEIVER_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                fatal_pos = catch.find("throwIfFatal(t)")
                for marker in ("state =", "log(", "addToStale(", "return false"):
                    pos = catch.find(marker)
                    if pos != -1:
                        self.assertLess(
                            fatal_pos,
                            pos,
                            f"throwIfFatal(t) must precede {marker} in {sig}",
                        )

    # ------------------------------------------------------------------
    # 7-15. Registration behavior
    # ------------------------------------------------------------------

    def test_both_register_methods_call_framework_register(self):
        for sig in RECEIVER_METHODS[:2]:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                try_body = self._extract_try_body(body)
                self.assertIsNotNone(try_body)
                self.assertIn(
                    "registrationContext.registerReceiver(receiver, filter, flags)",
                    try_body,
                )

    def test_both_register_catches_set_register_failed(self):
        for sig in RECEIVER_METHODS[:2]:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertIn(
                    "newRegistration.state = RegistrationState.REGISTER_FAILED",
                    catch,
                )

    def test_both_register_caches_call_log(self):
        for sig in RECEIVER_METHODS[:2]:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertIn("log(key, t)", catch)

    def test_both_register_catches_return_false(self):
        for sig in RECEIVER_METHODS[:2]:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertIn("return false", catch)

    def test_both_register_success_sets_active(self):
        for sig in RECEIVER_METHODS[:2]:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                self.assertIn(
                    "newRegistration.state = RegistrationState.ACTIVE",
                    body,
                )

    def test_register_module_drain_module_stale_first(self):
        body = self._extract_method_body(RECEIVER_METHODS[0])
        self.assertIsNotNone(body)
        drain_pos = body.find("drainModuleStale(key)")
        register_pos = body.find("registrationContext.registerReceiver")
        self.assertNotEqual(-1, drain_pos)
        self.assertNotEqual(-1, register_pos)
        self.assertLess(drain_pos, register_pos)

    def test_register_owned_drain_owned_stale_first(self):
        body = self._extract_method_body(RECEIVER_METHODS[1])
        self.assertIsNotNone(body)
        drain_pos = body.find("drainOwnedStale(key)")
        register_pos = body.find("registrationContext.registerReceiver")
        self.assertNotEqual(-1, drain_pos)
        self.assertNotEqual(-1, register_pos)
        self.assertLess(drain_pos, register_pos)

    def test_register_module_map_replacement_and_identity_check(self):
        body = self._extract_method_body(RECEIVER_METHODS[0])
        self.assertIsNotNone(body)
        self.assertIn("moduleReceivers.put(key, newRegistration)", body)
        self.assertIn("moduleReceivers.get(key)", body)
        self.assertIn("releaseReceiver(newRegistration)", body)

    def test_register_owned_owner_replacement_and_coexistence(self):
        body = self._extract_method_body(RECEIVER_METHODS[1])
        self.assertIsNotNone(body)
        self.assertIn("bucket.registrations.get(i)", body)
        self.assertIn("r.ownerRef.get()", body)
        self.assertIn("releaseReceiver(r)", body)
        self.assertIn("bucket.registrations.remove(i)", body)
        self.assertIn("bucket.registrations.add(newRegistration)", body)
        self.assertIn("bucket.registrations.contains(newRegistration)", body)

    # ------------------------------------------------------------------
    # 16-24. Release behavior
    # ------------------------------------------------------------------

    def test_try_release_success_sets_released_and_returns_true(self):
        body = self._extract_method_body(RECEIVER_METHODS[2])
        self.assertIsNotNone(body)
        try_body = self._extract_try_body(body)
        self.assertIsNotNone(try_body)
        self.assertIn("registration.context.unregisterReceiver(registration.receiver)", try_body)
        self.assertIn("registration.state = RegistrationState.RELEASED", try_body)
        self.assertIn("return true", try_body)

    def test_try_release_catch_sets_stale_and_returns_false(self):
        body = self._extract_method_body(RECEIVER_METHODS[2])
        self.assertIsNotNone(body)
        catch = self._extract_catch_body(body)
        self.assertIsNotNone(catch)
        self.assertIn("registration.state = RegistrationState.STALE", catch)
        self.assertIn("return false", catch)

    def test_try_release_catch_no_log_or_add_to_stale(self):
        body = self._extract_method_body(RECEIVER_METHODS[2])
        self.assertIsNotNone(body)
        catch = self._extract_catch_body(body)
        self.assertIsNotNone(catch)
        self.assertNotIn("log(", catch)
        self.assertNotIn("addToStale(", catch)

    def test_release_receiver_success_sets_released(self):
        body = self._extract_method_body(RECEIVER_METHODS[3])
        self.assertIsNotNone(body)
        try_body = self._extract_try_body(body)
        self.assertIsNotNone(try_body)
        self.assertIn("registration.context.unregisterReceiver(registration.receiver)", try_body)
        self.assertIn("registration.state = RegistrationState.RELEASED", try_body)

    def test_release_receiver_catch_sets_stale(self):
        body = self._extract_method_body(RECEIVER_METHODS[3])
        self.assertIsNotNone(body)
        catch = self._extract_catch_body(body)
        self.assertIsNotNone(catch)
        self.assertIn("registration.state = RegistrationState.STALE", catch)

    def test_release_receiver_owned_branch_to_owned_stale(self):
        body = self._extract_method_body(RECEIVER_METHODS[3])
        self.assertIsNotNone(body)
        catch = self._extract_catch_body(body)
        self.assertIsNotNone(catch)
        self.assertIn(
            "addToStale(staleOwnedReceivers, registration.key, (OwnedReceiverRegistration) registration)",
            catch,
        )

    def test_release_receiver_module_branch_to_module_stale(self):
        body = self._extract_method_body(RECEIVER_METHODS[3])
        self.assertIsNotNone(body)
        catch = self._extract_catch_body(body)
        self.assertIsNotNone(catch)
        self.assertIn(
            "addToStale(staleModuleReceivers, registration.key, registration)",
            catch,
        )

    def test_release_receiver_stale_branches_use_registration_key(self):
        body = self._extract_method_body(RECEIVER_METHODS[3])
        self.assertIsNotNone(body)
        catch = self._extract_catch_body(body)
        self.assertIsNotNone(catch)
        self.assertIn("registration.key", catch)

    def test_release_receiver_catch_no_log(self):
        body = self._extract_method_body(RECEIVER_METHODS[3])
        self.assertIsNotNone(body)
        catch = self._extract_catch_body(body)
        self.assertIsNotNone(catch)
        self.assertNotIn("log(", catch)

    # ------------------------------------------------------------------
    # 25-29. Synchronization and stale queue contract
    # ------------------------------------------------------------------

    def test_register_module_uses_module_receivers_lock(self):
        body = self._extract_method_body(RECEIVER_METHODS[0])
        self.assertIsNotNone(body)
        self.assertIn("synchronized (moduleReceivers)", body)

    def test_register_owned_uses_bucket_lock(self):
        body = self._extract_method_body(RECEIVER_METHODS[1])
        self.assertIsNotNone(body)
        self.assertIn("synchronized (bucket)", body)

    def test_try_release_synchronizes_registration(self):
        body = self._extract_method_body(RECEIVER_METHODS[2])
        self.assertIsNotNone(body)
        self.assertIn("synchronized (registration)", body)

    def test_release_receiver_synchronizes_registration(self):
        body = self._extract_method_body(RECEIVER_METHODS[3])
        self.assertIsNotNone(body)
        self.assertIn("synchronized (registration)", body)

    def test_add_to_stale_body_unchanged(self):
        body = self._extract_method_body(
            "private static <T extends ReceiverRegistration> void addToStale(ConcurrentHashMap<String, ConcurrentLinkedDeque<T>> map, String key, T reg)"
        )
        self.assertIsNotNone(body)
        self.assertIn("map.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<T>())", body)
        self.assertIn("synchronized (deque)", body)
        self.assertIn("deque.add(reg)", body)

    def test_drain_stale_body_unchanged(self):
        body = self._extract_method_body(
            "private static <T extends ReceiverRegistration> boolean drainStale(ConcurrentHashMap<String, ConcurrentLinkedDeque<T>> map, String key, int max)"
        )
        self.assertIsNotNone(body)
        self.assertIn("map.get(key)", body)
        self.assertIn("tryRelease(reg)", body)
        self.assertIn("it.remove()", body)

    def test_stale_maps_separate(self):
        self.assertIn(
            "private static final ConcurrentHashMap<String, ConcurrentLinkedDeque<ReceiverRegistration>> staleModuleReceivers",
            self.text,
        )
        self.assertIn(
            "private static final ConcurrentHashMap<String, ConcurrentLinkedDeque<OwnedReceiverRegistration>> staleOwnedReceivers",
            self.text,
        )

    def test_registration_state_enum_unchanged(self):
        match = re.search(
            r"private enum RegistrationState \{([^}]+)\}",
            self.text,
        )
        self.assertIsNotNone(match)
        body = match.group(1)
        self.assertIn("PENDING_REGISTER", body)
        self.assertIn("ACTIVE", body)
        self.assertIn("STALE", body)
        self.assertIn("RELEASED", body)
        self.assertIn("REGISTER_FAILED", body)

    # ------------------------------------------------------------------
    # 30-33. Scope protection
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

    def test_four_reflection_fallbacks_still_call_throw_if_fatal(self):
        for sig in REFLECTION_FALLBACK_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertIn("throwIfFatal(t);", catch)

    def test_non_target_methods_unchanged(self):
        # These methods must not be converted to the shared helper by this task.
        for sig in (
            "public static long getNextMIUIAlarmTime(Context context)",
            "public static void openAppInfo(Context context, String pkg, int user)",
            "public static Drawable getActionImage(Context context, String key)",
            "public static String getActionName(Context context, String key)",
        ):
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                self.assertNotIn("throwIfFatal(t);", body)
                self.assertIn("if (t instanceof OutOfMemoryError)", body)

    # ------------------------------------------------------------------
    # 34-35. Baseline and global printStackTrace
    # ------------------------------------------------------------------

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
        # Allow the signature to span multiple lines and arbitrary whitespace.
        # Split into words and punctuation tokens, escape each, then re-join
        # with \s* so any whitespace (including newlines/indentation) is allowed.
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
