#!/usr/bin/env python3
"""Static contract tests for ModuleHelper action/alarm fatal propagation."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
FILE = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils" / "ModuleHelper.java"


ACTION_ALARM_METHODS = (
    "public static long getNextMIUIAlarmTime(Context context)",
    "public static void openAppInfo(Context context, String pkg, int user)",
    "public static Drawable getActionImage(Context context, String key)",
    "public static String getActionName(Context context, String key)",
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


class ModuleHelperActionAlarmFatalityContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = FILE.read_text(encoding="utf-8")

    # ------------------------------------------------------------------
    # 1-7. Five target catch contract
    # ------------------------------------------------------------------

    def test_four_target_methods_exist(self):
        for sig in ACTION_ALARM_METHODS:
            with self.subTest(sig=sig):
                self.assertIsNotNone(
                    self._extract_method_body(sig),
                    f"Could not find target method: {sig}",
                )

    def test_each_alarm_image_name_catch_calls_throw_if_fatal(self):
        for sig in (
            "public static long getNextMIUIAlarmTime(Context context)",
            "public static Drawable getActionImage(Context context, String key)",
            "public static String getActionName(Context context, String key)",
        ):
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertIn("throwIfFatal(t);", catch)

    def test_open_app_info_both_catches_call_throw_if_fatal(self):
        body = self._extract_method_body(
            "public static void openAppInfo(Context context, String pkg, int user)"
        )
        self.assertIsNotNone(body)
        outer_catch = self._extract_catch_body(body)
        self.assertIsNotNone(outer_catch)
        inner_catch = self._extract_catch_body_by_var(outer_catch, "t2")
        self.assertIsNotNone(inner_catch)
        self.assertIn("throwIfFatal(t);", outer_catch)
        self.assertIn("throwIfFatal(t2);", inner_catch)

    def test_each_target_catch_has_only_one_throw_if_fatal(self):
        for sig in ACTION_ALARM_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                # Count the exact throwIfFatal(t) call; openAppInfo outer catch also contains inner throwIfFatal(t2).
                self.assertEqual(1, catch.count("throwIfFatal(t);"))

    def test_open_app_info_inner_catch_has_only_one_throw_if_fatal_t2(self):
        body = self._extract_method_body(
            "public static void openAppInfo(Context context, String pkg, int user)"
        )
        self.assertIsNotNone(body)
        outer_catch = self._extract_catch_body(body)
        self.assertIsNotNone(outer_catch)
        inner_catch = self._extract_catch_body_by_var(outer_catch, "t2")
        self.assertIsNotNone(inner_catch)
        self.assertEqual(1, inner_catch.count("throwIfFatal"))

    def test_throw_if_fatal_is_first_statement_in_target_catch(self):
        simple_sigs = (
            "public static long getNextMIUIAlarmTime(Context context)",
            "public static Drawable getActionImage(Context context, String key)",
            "public static String getActionName(Context context, String key)",
        )
        for sig in simple_sigs:
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

        # openAppInfo outer and inner
        body = self._extract_method_body(
            "public static void openAppInfo(Context context, String pkg, int user)"
        )
        self.assertIsNotNone(body)
        outer_catch = self._extract_catch_body(body)
        self.assertIsNotNone(outer_catch)
        stripped = self._strip_comments(outer_catch)
        self.assertRegex(stripped, r"^\s*throwIfFatal\s*\(\s*t\s*\)\s*;")

        inner_catch = self._extract_catch_body_by_var(outer_catch, "t2")
        self.assertIsNotNone(inner_catch)
        stripped = self._strip_comments(inner_catch)
        self.assertRegex(stripped, r"^\s*throwIfFatal\s*\(\s*t2\s*\)\s*;")

    def test_target_catches_have_no_direct_fatal_instanceof(self):
        for sig in ACTION_ALARM_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertNotIn("t instanceof OutOfMemoryError", catch)
                self.assertNotIn("t2 instanceof OutOfMemoryError", catch)
                self.assertNotIn("t instanceof ThreadDeath", catch)
                self.assertNotIn("t2 instanceof ThreadDeath", catch)
                self.assertNotIn("t instanceof VirtualMachineError", catch)
                self.assertNotIn("t2 instanceof VirtualMachineError", catch)

    def test_target_methods_have_no_reflection_fatality(self):
        for sig in ACTION_ALARM_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                self.assertNotIn("ReflectionFatality", body)

    def test_throw_if_fatal_before_log_fallback_return(self):
        # Alarm: throwIfFatal before log
        body = self._extract_method_body(
            "public static long getNextMIUIAlarmTime(Context context)"
        )
        self.assertIsNotNone(body)
        catch = self._extract_catch_body(body)
        self.assertIsNotNone(catch)
        fatal_pos = catch.find("throwIfFatal(t)")
        log_pos = catch.find("log(")
        self.assertNotEqual(-1, fatal_pos)
        self.assertNotEqual(-1, log_pos)
        self.assertLess(fatal_pos, log_pos)

        # openAppInfo: outer throwIfFatal before fallback try; inner throwIfFatal before log
        body = self._extract_method_body(
            "public static void openAppInfo(Context context, String pkg, int user)"
        )
        self.assertIsNotNone(body)
        outer_catch = self._extract_catch_body(body)
        self.assertIsNotNone(outer_catch)
        fatal_pos = outer_catch.find("throwIfFatal(t)")
        try_pos = outer_catch.find("try {")
        self.assertNotEqual(-1, fatal_pos)
        self.assertNotEqual(-1, try_pos)
        self.assertLess(fatal_pos, try_pos)

        inner_catch = self._extract_catch_body_by_var(outer_catch, "t2")
        self.assertIsNotNone(inner_catch)
        fatal_pos = inner_catch.find("throwIfFatal(t2)")
        log_pos = inner_catch.find("log(")
        self.assertNotEqual(-1, fatal_pos)
        self.assertNotEqual(-1, log_pos)
        self.assertLess(fatal_pos, log_pos)

        # image/name: throwIfFatal before return null
        for sig in (
            "public static Drawable getActionImage(Context context, String key)",
            "public static String getActionName(Context context, String key)",
        ):
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                fatal_pos = catch.find("throwIfFatal(t)")
                return_pos = catch.find("return null")
                self.assertNotEqual(-1, fatal_pos)
                self.assertNotEqual(-1, return_pos)
                self.assertLess(fatal_pos, return_pos)

    # ------------------------------------------------------------------
    # 8-18. Alarm contract
    # ------------------------------------------------------------------

    def test_alarm_still_uses_original_key_and_keeps_reading_before_parse(self):
        body = self._extract_method_body(
            "public static long getNextMIUIAlarmTime(Context context)"
        )
        self.assertIsNotNone(body)
        self.assertIn(
            'Settings.System.getString(context.getContentResolver(), "next_alarm_clock_formatted")',
            body,
        )
        self.assertIn("long nextTime = 0", body)
        self.assertIn("if (!TextUtils.isEmpty(nextAlarm)) try {", body)

    def test_alarm_uses_utc_timezone_and_monday_first_day(self):
        body = self._extract_method_body(
            "public static long getNextMIUIAlarmTime(Context context)"
        )
        self.assertIsNotNone(body)
        self.assertIn('TimeZone.getTimeZone("UTC")', body)
        self.assertIn("Calendar.MONDAY", body)

    def test_alarm_date_pattern_and_24_hour_choice_unchanged(self):
        body = self._extract_method_body(
            "public static long getNextMIUIAlarmTime(Context context)"
        )
        self.assertIsNotNone(body)
        self.assertIn("DateFormat.getBestDateTimePattern", body)
        self.assertIn("DateFormat.is24HourFormat(context)", body)
        self.assertIn('"EHm"', body)
        self.assertIn('"Ehma"', body)

    def test_alarm_catch_calls_log(self):
        body = self._extract_method_body(
            "public static long getNextMIUIAlarmTime(Context context)"
        )
        self.assertIsNotNone(body)
        catch = self._extract_catch_body(body)
        self.assertIsNotNone(catch)
        self.assertIn("log(t)", catch)

    def test_alarm_returns_current_next_time(self):
        body = self._extract_method_body(
            "public static long getNextMIUIAlarmTime(Context context)"
        )
        self.assertIsNotNone(body)
        self.assertIn("return nextTime", body)

    def test_alarm_catch_does_not_fix_return_value(self):
        body = self._extract_method_body(
            "public static long getNextMIUIAlarmTime(Context context)"
        )
        self.assertIsNotNone(body)
        catch = self._extract_catch_body(body)
        self.assertIsNotNone(catch)
        self.assertNotIn("return 0", catch)
        self.assertNotIn("nextTime =", catch)

    # ------------------------------------------------------------------
    # 19-33. openAppInfo contract
    # ------------------------------------------------------------------

    def test_open_app_info_primary_intent_unchanged(self):
        body = self._extract_method_body(
            "public static void openAppInfo(Context context, String pkg, int user)"
        )
        self.assertIsNotNone(body)
        try_body = self._extract_try_body(body)
        self.assertIsNotNone(try_body)
        self.assertIn('"miui.intent.action.APP_MANAGER_APPLICATION_DETAIL"', try_body)
        self.assertIn('"com.miui.securitycenter"', try_body)
        self.assertIn('"package_name"', try_body)
        self.assertIn('"miui.intent.extra.USER_ID"', try_body)

    def test_open_app_info_outer_ordinary_failure_enters_system_fallback(self):
        body = self._extract_method_body(
            "public static void openAppInfo(Context context, String pkg, int user)"
        )
        self.assertIsNotNone(body)
        outer_catch = self._extract_catch_body(body)
        self.assertIsNotNone(outer_catch)
        self.assertIn("android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS", outer_catch)
        self.assertIn("FLAG_ACTIVITY_NEW_TASK", outer_catch)
        self.assertIn("FLAG_ACTIVITY_RESET_TASK_IF_NEEDED", outer_catch)
        self.assertIn('"package:" + pkg', outer_catch)

    def test_open_app_info_user_0_and_nonzero_branches_unchanged(self):
        body = self._extract_method_body(
            "public static void openAppInfo(Context context, String pkg, int user)"
        )
        self.assertIsNotNone(body)
        outer_catch = self._extract_catch_body(body)
        self.assertIsNotNone(outer_catch)
        inner_try = self._extract_try_body(outer_catch)
        self.assertIsNotNone(inner_try)
        self.assertIn("startActivityAsUser", inner_try)
        self.assertIn("UserHandle.class", inner_try)
        self.assertIn("context.startActivity(intent)", inner_try)

    def test_open_app_info_outer_catch_has_no_log(self):
        body = self._extract_method_body(
            "public static void openAppInfo(Context context, String pkg, int user)"
        )
        self.assertIsNotNone(body)
        outer_catch = self._extract_catch_body(body)
        self.assertIsNotNone(outer_catch)
        # The inner catch contains log(t2). Ensure log does not appear before the inner catch.
        inner_match = re.search(r"catch\s*\(\s*Throwable\s+t2\s*\)\s*\{", outer_catch)
        self.assertIsNotNone(inner_match)
        prefix = outer_catch[:inner_match.start()]
        self.assertNotIn("log(", prefix)

    def test_open_app_info_inner_catch_logs_t2(self):
        body = self._extract_method_body(
            "public static void openAppInfo(Context context, String pkg, int user)"
        )
        self.assertIsNotNone(body)
        outer_catch = self._extract_catch_body(body)
        self.assertIsNotNone(outer_catch)
        inner_catch = self._extract_catch_body_by_var(outer_catch, "t2")
        self.assertIsNotNone(inner_catch)
        self.assertIn("log(t2)", inner_catch)

    def test_open_app_info_inner_catch_does_not_log_outer_t(self):
        body = self._extract_method_body(
            "public static void openAppInfo(Context context, String pkg, int user)"
        )
        self.assertIsNotNone(body)
        outer_catch = self._extract_catch_body(body)
        self.assertIsNotNone(outer_catch)
        inner_catch = self._extract_catch_body_by_var(outer_catch, "t2")
        self.assertIsNotNone(inner_catch)
        self.assertNotIn("log(t)", inner_catch)

    # ------------------------------------------------------------------
    # 34-40. Action image/name contract
    # ------------------------------------------------------------------

    def test_get_action_image_branches_and_keys_unchanged(self):
        body = self._extract_method_body(
            "public static Drawable getActionImage(Context context, String key)"
        )
        self.assertIsNotNone(body)
        try_body = self._extract_try_body(body)
        self.assertIsNotNone(try_body)
        self.assertIn('key + "_action"', try_body)
        self.assertIn("getModuleContext(context)", try_body)
        self.assertIn('key + "_app"', try_body)
        self.assertIn('key + "_activity"', try_body)
        self.assertIn('getAppIcon(modCtx, MainModule.mPrefs.getString(key + "_activity", ""), true)', try_body)

    def test_get_action_image_catch_returns_null(self):
        body = self._extract_method_body(
            "public static Drawable getActionImage(Context context, String key)"
        )
        self.assertIsNotNone(body)
        catch = self._extract_catch_body(body)
        self.assertIsNotNone(catch)
        self.assertIn("return null", catch)

    def test_get_action_name_resid_and_branches_unchanged(self):
        body = self._extract_method_body(
            "public static String getActionName(Context context, String key)"
        )
        self.assertIsNotNone(body)
        try_body = self._extract_try_body(body)
        self.assertIsNotNone(try_body)
        self.assertIn("GlobalActions.getActionResId(action)", try_body)
        self.assertIn("modRes.getString(resId)", try_body)
        self.assertIn('key + "_app"', try_body)
        self.assertIn('key + "_shortcut_name"', try_body)
        self.assertIn('key + "_toggle"', try_body)
        self.assertIn('key + "_activity"', try_body)
        self.assertIn("getAppName(ctx, pref)", try_body)
        self.assertIn("getAppName(ctx, pref, true)", try_body)

    def test_get_action_name_toggle_switch_mapping_unchanged(self):
        body = self._extract_method_body(
            "public static String getActionName(Context context, String key)"
        )
        self.assertIsNotNone(body)
        try_body = self._extract_try_body(body)
        self.assertIsNotNone(try_body)
        toggles = ['wifi', 'bt', 'gps', 'nfc', 'sound', 'brightness', 'rotation', 'torch', 'mobiledata']
        for n, name in enumerate(toggles, 1):
            self.assertIn(
                f"R.string.array_global_toggle_{name}",
                try_body,
            )
        self.assertIn("default: return null", try_body)

    def test_get_action_name_catch_returns_null(self):
        body = self._extract_method_body(
            "public static String getActionName(Context context, String key)"
        )
        self.assertIsNotNone(body)
        catch = self._extract_catch_body(body)
        self.assertIsNotNone(catch)
        self.assertIn("return null", catch)

    def test_action_image_and_name_catch_do_not_log(self):
        for sig in (
            "public static Drawable getActionImage(Context context, String key)",
            "public static String getActionName(Context context, String key)",
        ):
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertNotIn("log(", catch)

    # ------------------------------------------------------------------
    # 41-49. Scope protection
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

    def test_four_receiver_lifecycle_still_call_throw_if_fatal(self):
        for sig in RECEIVER_METHODS:
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                catch = self._extract_catch_body(body)
                self.assertIsNotNone(catch)
                self.assertIn("throwIfFatal(t);", catch)

    def test_add_to_stale_and_drain_stale_unchanged(self):
        for sig in (
            "private static <T extends ReceiverRegistration> void addToStale(ConcurrentHashMap<String, ConcurrentLinkedDeque<T>> map, String key, T reg)",
            "private static <T extends ReceiverRegistration> boolean drainStale(ConcurrentHashMap<String, ConcurrentLinkedDeque<T>> map, String key, int max)",
        ):
            with self.subTest(sig=sig):
                body = self._extract_method_body(sig)
                self.assertIsNotNone(body)
                self.assertNotIn("throwIfFatal", body)

    def test_preference_observers_and_guarded_unchanged(self):
        for marker in (
            "public static void handlePreferenceChanged",
            "public static void guarded(Runnable block)",
            "public static void guarded(String callbackName",
        ):
            with self.subTest(marker=marker):
                self.assertIn(marker, self.text)

    def test_module_context_and_resource_creation_unchanged(self):
        for marker in (
            "public static synchronized Context getModuleContext",
            "public static synchronized Resources getModuleRes",
        ):
            with self.subTest(marker=marker):
                self.assertIn(marker, self.text)

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

    def _extract_catch_body_by_var(self, body, var):
        match = re.search(rf"catch\s*\(\s*Throwable\s+{re.escape(var)}\s*\)\s*\{{", body)
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
