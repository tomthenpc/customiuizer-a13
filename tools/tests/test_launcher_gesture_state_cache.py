"""Static contract tests for the launcher gesture state cache."""
import re
import subprocess
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent


def read_source(rel: str) -> str:
    return (REPO_ROOT / rel).read_text(encoding="utf-8")


def find_function_block(text: str, name: str) -> str:
    """Extract the body of a Kotlin function with the given name."""
    match = re.search(rf"\b(?:private|internal|public|protected)?\s*fun\s+{re.escape(name)}\s*\(", text)
    if not match:
        return ""
    i = match.end()
    depth = 1
    while i < len(text) and depth > 0:
        if text[i] == "(":
            depth += 1
        elif text[i] == ")":
            depth -= 1
        i += 1
    while i < len(text) and text[i] in " \t\r\n":
        i += 1
    if i < len(text) and text[i] == ":":
        i += 1
        while i < len(text) and text[i] not in "{\r\n":
            i += 1
    while i < len(text) and text[i] in " \t\r\n":
        i += 1
    if i >= len(text) or text[i] != "{":
        return ""

    brace_depth = 0
    for j in range(i, len(text)):
        c = text[j]
        if c == "{":
            brace_depth += 1
        elif c == "}":
            brace_depth -= 1
            if brace_depth == 0:
                return text[i : j + 1]
    return ""


def find_braced_block(text: str, keyword: str) -> str:
    """Extract the nearest braced block after keyword."""
    idx = text.find(keyword)
    if idx == -1:
        return ""
    brace = text.find("{", idx)
    if brace == -1:
        return ""
    depth = 0
    for j in range(brace, len(text)):
        c = text[j]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return text[brace : j + 1]
    return ""


class LauncherGestureStateCacheTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = read_source(
            "app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt"
        )
        cls.hot_seat_swipes = find_function_block(cls.source, "HotSeatSwipesHook")
        cls.fs_gestures = find_function_block(cls.source, "FSGesturesHook")
        cls.state_class = find_braced_block(cls.source, "internal class HotSeatGestureState")
        cls.dispatch_block = find_braced_block(
            cls.hot_seat_swipes, "override fun before(param: BeforeHookCallback)"
        )
        cls.threshold_update_block = find_braced_block(
            cls.dispatch_block, "if (state.densityDpi != densityDpi)"
        )

    def test_object_level_down_fields_removed(self):
        for field in ("mHotSeatDownX", "mHotSeatDownY", "mHotSeatDownTime"):
            self.assertNotIn(f"private var {field}", self.source)

    def test_hot_seat_gesture_state_class_exists(self):
        self.assertIn("internal class HotSeatGestureState", self.source)

    def test_state_is_not_data_class(self):
        self.assertNotIn("data class HotSeatGestureState", self.source)

    def test_state_has_three_thresholds(self):
        for field in ("minDistance", "velocityThreshold", "touchSlop"):
            self.assertIn(f"var {field}: Int", self.state_class)

    def test_state_has_down_fields(self):
        for field in ("downX", "downY", "downTime"):
            self.assertIn(f"var {field}:", self.state_class)

    def test_state_no_context_view_activity_classloader_fields(self):
        forbidden = (
            "Context",
            "View",
            "Activity",
            "ClassLoader",
            "ViewGroup",
        )
        for f in re.findall(r"var\s+(\w+)\s*:\s*(\S+)", self.state_class):
            for bad in forbidden:
                self.assertNotIn(bad, f[1], f"state field {f[0]} must not hold {bad}")

    def test_uses_additional_instance_field(self):
        self.assertIn("getAdditionalInstanceField(hotSeat, HOTSEAT_GESTURE_STATE_KEY)", self.source)
        self.assertIn("setAdditionalInstanceField(hotSeat, HOTSEAT_GESTURE_STATE_KEY, state)", self.source)

    def test_no_static_map_for_state(self):
        for pat in ("HashMap", "WeakHashMap", "Map<", "mutableMapOf", "hashMapOf"):
            self.assertNotIn(pat, self.dispatch_block)

    def test_no_view_set_tag(self):
        self.assertNotIn("setTag", self.dispatch_block)

    def test_state_helper_creates_once(self):
        self.assertRegex(
            self.source,
            r"var\s+state\s*=\s*XposedHelpers\.getAdditionalInstanceField\(hotSeat,\s*HOTSEAT_GESTURE_STATE_KEY\)\s*as\?\s*HotSeatGestureState",
        )
        self.assertRegex(
            self.source,
            r"if\s*\(\s*state\s*==\s*null\s*\)\s*\{\s*state\s*=\s*HotSeatGestureState\(\)",
        )

    def test_helper_returns_same_instance(self):
        # The helper always returns after get-or-create.
        helper = find_function_block(self.source, "hotSeatGestureState")
        self.assertIn("return state", helper)

    def test_dispatch_uses_helper(self):
        self.assertIn("val state = hotSeatGestureState(hotSeat)", self.dispatch_block)

    def test_distance_and_velocity_constants_unchanged(self):
        self.assertIn("Math.round(75f * density)", self.source)
        self.assertIn("Math.round(33f * density)", self.source)

    def test_densityDpi_gate_exists(self):
        self.assertIn("if (state.densityDpi != densityDpi)", self.dispatch_block)

    def test_view_configuration_only_on_density_change(self):
        # ViewConfiguration.get must be inside the densityDpi-change branch.
        self.assertIn("ViewConfiguration.get", self.threshold_update_block)
        else_block = find_braced_block(self.dispatch_block, "} else {")
        self.assertNotIn("ViewConfiguration.get", else_block)

    def test_steady_path_does_not_read_display_metrics(self):
        else_block = find_braced_block(self.dispatch_block, "} else {")
        self.assertIn("density = 0f", else_block)
        self.assertIn("touchSlop = 0", else_block)

    def test_update_thresholds_if_needed_called(self):
        self.assertIn(
            "state.updateThresholdsIfNeeded(densityDpi, density, touchSlop)",
            self.dispatch_block,
        )

    def test_down_uses_instance_state(self):
        self.assertIn("state.downX = ev.x", self.dispatch_block)
        self.assertIn("state.downY = ev.y", self.dispatch_block)
        self.assertIn("state.downTime = SystemClock.uptimeMillis()", self.dispatch_block)

    def test_up_uses_instance_state(self):
        self.assertIn("val dx = ev.x - state.downX", self.dispatch_block)
        self.assertIn("val dy = ev.y - state.downY", self.dispatch_block)
        self.assertIn("val dt = SystemClock.uptimeMillis() - state.downTime", self.dispatch_block)

    def test_dt_zero_guard_kept(self):
        self.assertIn("if (dt == 0L) return", self.dispatch_block)

    def test_velocity_formula_unchanged(self):
        self.assertIn("kotlin.math.abs(dx) * 1000 / dt", self.dispatch_block)

    def test_left_right_action_keys_unchanged(self):
        self.assertIn("\"launcher_swiperight\"", self.dispatch_block)
        self.assertIn("\"launcher_swipeleft\"", self.dispatch_block)

    def test_up_uses_state_thresholds(self):
        self.assertIn("kotlin.math.abs(dy) <= state.touchSlop", self.dispatch_block)
        self.assertIn("velocity > state.velocityThreshold", self.dispatch_block)
        self.assertIn("dx > state.minDistance", self.dispatch_block)
        self.assertIn("-dx > state.minDistance", self.dispatch_block)

    def test_base_recents_class_parsed_once(self):
        matches = re.findall(
            r'XposedHelpers\.findClass\(\s*"com\.miui\.home\.recents\.BaseRecentsImpl"',
            self.source,
        )
        self.assertEqual(1, len(matches), "BaseRecentsImpl class must be resolved once")

    def test_fs_gestures_callbacks_use_captured_class(self):
        for method in ("createAndAddNavStubView", "updateFsgWindowState", "getGlobalBoolean"):
            block = find_braced_block(self.fs_gestures, f'"{method}"')
            self.assertIn("baseRecentsClass", block)
            self.assertNotIn(
                'XposedHelpers.findClass("com.miui.home.recents.BaseRecentsImpl"', block
            )

    def test_real_force_fsg_nav_bar_unchanged(self):
        self.assertIn("\"REAL_FORCE_FSG_NAV_BAR\"", self.source)

    def test_stack_trace_call_preserved(self):
        self.assertIn("Thread.currentThread().stackTrace", self.source)

    def test_stack_class_name_condition_preserved(self):
        self.assertIn('el.className == "com.miui.home.recents.BaseRecentsImpl"', self.source)

    def test_no_stack_walker_or_throwable_stack(self):
        for bad in ("StackWalker", "Throwable().stackTrace", "VMStack"):
            self.assertNotIn(bad, self.source)

    def test_global_force_fsg_not_always_true(self):
        # The contract records the value on the stack; it must not be set to true for all callers.
        block = find_braced_block(self.fs_gestures, '"getGlobalBoolean"')
        self.assertIn("if (el.className ==", block)

    def test_no_print_stack_trace_in_production_source(self):
        self.assertNotIn(".printStackTrace(", self.source)


class ScopeProtectionTest(unittest.TestCase):
    def test_no_other_launcher_files_modified(self):
        result = subprocess.run(
            ["git", "diff", "--name-only", "HEAD", "--", "app/src/main/java"],
            capture_output=True,
            text=True,
            cwd=REPO_ROOT,
        )
        changed = [p for p in result.stdout.splitlines() if p]
        allowed = {
            "app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt"
        }
        for path in changed:
            if path in allowed:
                continue
            self.fail(
                f"Unexpected app/src/main/java change: {path}; only {allowed} are permitted"
            )


if __name__ == "__main__":
    unittest.main()
