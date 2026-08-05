"""Static contract tests for the low-allocation device-info sampler."""
import re
import subprocess
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent


def read_source(rel: str) -> str:
    return (REPO_ROOT / rel).read_text(encoding="utf-8")


def find_function_block(text: str, name: str) -> str:
    """Extract the body of a Kotlin function with the given name."""
    # Prefer explicitly private functions to avoid collisions (e.g. LifecycleState.start).
    match = re.search(rf"\bprivate\s+fun\s+{re.escape(name)}\s*\(", text)
    if not match:
        match = re.search(rf"\bfun\s+{re.escape(name)}\s*\(", text)
    if not match:
        return ""
    i = match.end()
    # Skip to matching ')' of the parameter list.
    depth = 1
    while i < len(text) and depth > 0:
        if text[i] == "(":
            depth += 1
        elif text[i] == ")":
            depth -= 1
        i += 1
    # Skip optional return type.
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


def find_catch_block(text: str) -> str:
    """Extract the catch (t: Throwable) { ... } block."""
    match = re.search(r"catch\s*\(\s*t\s*:\s*Throwable\s*\)\s*\{", text)
    if not match:
        return ""
    start = match.end() - 1
    depth = 0
    for j in range(start, len(text)):
        c = text[j]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return text[start : j + 1]
    return ""


def changed_files() -> set[str]:
    """Return files changed in the working tree relative to HEAD."""
    result = subprocess.run(
        ["git", "diff", "--name-only", "HEAD"],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
    )
    names = set(result.stdout.splitlines())
    result2 = subprocess.run(
        ["git", "ls-files", "--others", "--exclude-standard"],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
    )
    names.update(result2.stdout.splitlines())
    return names


class DeviceInfoSysfsParserContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.parser_path = REPO_ROOT / (
            "app/src/main/java/tv/withaibuild/customiuizer/mods/utils/"
            "DeviceInfoSysfsParser.kt"
        )
        cls.parser_text = cls.parser_path.read_text(encoding="utf-8")
        cls.parse_battery = find_function_block(cls.parser_text, "parseBatteryUevent")
        cls.parse_cpu = find_function_block(cls.parser_text, "parseCpuTemperature")
        cls.parse_int = find_function_block(cls.parser_text, "parseInt")

    def test_parser_file_exists(self):
        self.assertTrue(self.parser_path.is_file())

    def test_parser_has_no_android_import(self):
        self.assertNotIn("import android", self.parser_text)

    def test_battery_buffer_size_constant(self):
        self.assertIn("BATTERY_BUFFER_BYTES = 8 * 1024", self.parser_text)

    def test_cpu_buffer_size_constant(self):
        self.assertIn("CPU_BUFFER_BYTES = 64", self.parser_text)

    def test_battery_values_has_three_int_fields(self):
        cls_block = find_braced_block(
            self.parser_text, "internal class BatteryValues"
        )
        fields = re.findall(r"var\s+(\w+)\s*:\s*Int", cls_block)
        self.assertEqual({"temperature", "currentNow", "voltageNow"}, set(fields))

    def test_battery_values_has_reset(self):
        cls_block = find_braced_block(
            self.parser_text, "internal class BatteryValues"
        )
        self.assertIn("fun reset()", cls_block)

    def test_parse_battery_resets_output(self):
        self.assertIn("output.reset()", self.parse_battery)

    def test_parse_battery_contains_three_key_constants(self):
        # The constants may be declared at object scope and referenced by name.
        for key in (
            "POWER_SUPPLY_TEMP",
            "POWER_SUPPLY_CURRENT_NOW",
            "POWER_SUPPLY_VOLTAGE_NOW",
        ):
            self.assertIn(key, self.parser_text)

    def test_parse_battery_scans_byte_array(self):
        # No String construction of the whole buffer; loops over ByteArray.
        self.assertNotIn("String(", self.parse_battery)
        self.assertRegex(self.parse_battery, r"buffer\[[^\]]+\]")

    def test_parse_battery_supports_newline(self):
        self.assertIn(r"'\n'.code", self.parse_battery)

    def test_parse_battery_supports_carriage_return(self):
        self.assertIn(r"'\r'.code", self.parse_battery)

    def test_parse_int_supports_plus(self):
        self.assertIn(r"'+'.code", self.parse_int)

    def test_parse_int_supports_minus(self):
        self.assertIn(r"'-'.code", self.parse_int)

    def test_parse_int_has_upper_bound_check(self):
        self.assertIn("Int.MAX_VALUE", self.parse_int)

    def test_parse_int_has_lower_bound_check(self):
        self.assertTrue(
            "Int.MIN_VALUE" in self.parse_int
            or "Int.MAX_VALUE.toLong() + 1" in self.parse_int,
            "parseInt must guard the negative range",
        )

    def test_parse_battery_duplicate_last_wins(self):
        # The parser overwrites output fields on each key match (no break in when).
        for field in ("output.temperature", "output.currentNow", "output.voltageNow"):
            self.assertIn(field, self.parse_battery)
        when_block = find_braced_block(self.parse_battery, "when (keyLen)")
        self.assertNotIn("break", when_block)

    def test_parse_battery_rejects_invalid_length(self):
        self.assertIn("return false", self.parse_battery)

    def test_parse_battery_rejects_length_greater_than_buffer(self):
        self.assertRegex(
            self.parse_battery, r"length\s*>\s*buffer\.size"
        )

    def test_parser_has_no_string_constructor(self):
        for block in (self.parse_battery, self.parse_cpu, self.parse_int):
            self.assertNotIn("String(", block)
            self.assertNotIn("decodeToString", block)

    def test_parser_has_no_decode_to_string(self):
        self.assertNotIn("decodeToString", self.parser_text)

    def test_parser_has_no_split_or_substring(self):
        for token in ("split", "substring"):
            self.assertNotIn(token, self.parser_text)

    def test_parser_has_no_regex(self):
        self.assertNotIn("Regex", self.parser_text)

    def test_parser_has_no_properties(self):
        self.assertNotIn("Properties", self.parser_text)

    def test_parser_has_no_map_or_list_or_sequence(self):
        for token in ("Map", "HashMap", "List", "Sequence"):
            # Avoid matching false positives by requiring word boundary.
            self.assertIsNone(
                re.search(rf"\b{token}\b", self.parser_text),
                f"found forbidden {token} in parser",
            )

    def test_parser_has_no_dynamic_byte_array(self):
        for token in ("ByteArray(", "byteArrayOf("):
            self.assertNotIn(token, self.parse_battery)
            self.assertNotIn(token, self.parse_cpu)


class DeviceInfoMonitorContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.monitor_path = REPO_ROOT / (
            "app/src/main/java/tv/withaibuild/customiuizer/mods/utils/"
            "DeviceInfoMonitor.kt"
        )
        cls.monitor_text = cls.monitor_path.read_text(encoding="utf-8")
        cls.run_tick = find_function_block(cls.monitor_text, "runTick")
        cls.sample = find_function_block(cls.monitor_text, "sampleAndPublish")
        cls.read_sysfs = find_function_block(cls.monitor_text, "readSysfsFile")
        cls.start = find_function_block(cls.monitor_text, "start")
        cls.publish = find_function_block(cls.monitor_text, "publish")
        cls.register = find_function_block(cls.monitor_text, "registerScreenReceiverLocked")

    def test_monitor_has_no_properties_import(self):
        self.assertNotIn("import java.util.Properties", self.monitor_text)

    def test_monitor_has_no_random_access_file_import(self):
        self.assertNotIn("import java.io.RandomAccessFile", self.monitor_text)

    def test_monitor_does_not_call_properties_load(self):
        self.assertNotIn("Properties().load", self.monitor_text)

    def test_monitor_does_not_call_read_line(self):
        self.assertNotIn("readLine", self.monitor_text)

    def test_monitor_has_only_two_fixed_byte_arrays(self):
        matches = list(re.finditer(r"ByteArray\(", self.monitor_text))
        self.assertEqual(
            2,
            len(matches),
            "Only the two top-level buffer constants should create ByteArrays",
        )

    def test_buffer_not_created_inside_functions(self):
        for block in (self.run_tick, self.sample, self.read_sysfs, self.publish):
            self.assertNotIn("ByteArray(", block)
            self.assertNotIn("byteArrayOf(", block)

    def test_sysfs_read_lock_exists(self):
        self.assertIn("private val sysfsReadLock = Any()", self.monitor_text)

    def test_file_read_and_parser_inside_read_lock(self):
        lock_block = find_braced_block(self.sample, "synchronized(sysfsReadLock)")
        self.assertIn("readSysfsFile", lock_block)
        self.assertIn("DeviceInfoSysfsParser.parseBatteryUevent", lock_block)
        self.assertIn("DeviceInfoSysfsParser.parseCpuTemperature", lock_block)

    def test_text_formatting_outside_read_lock(self):
        lock_block = find_braced_block(self.sample, "synchronized(sysfsReadLock)")
        lock_end = self.sample.find(lock_block) + len(lock_block)
        tail = self.sample[lock_end:]
        self.assertIn("buildBatteryInfo", tail)
        self.assertIn("buildDeviceInfo", tail)

    def test_read_sysfs_uses_file_input_stream_use(self):
        self.assertIn("FileInputStream(path).use", self.read_sysfs)

    def test_read_sysfs_is_bounded(self):
        self.assertIn("buffer.size", self.read_sysfs)
        self.assertIn("capacity - offset", self.read_sysfs)

    def test_read_sysfs_detects_buffer_overflow(self):
        self.assertIn("input.read() >= 0", self.read_sysfs)

    def test_read_sysfs_returns_minus_one_on_zero_read(self):
        self.assertIn("if (read == 0) return@use -1", self.read_sysfs)

    def test_read_sysfs_returns_minus_one_on_ordinary_failure(self):
        self.assertIn("return@use -1", self.read_sysfs)

    def test_read_sysfs_catch_first_statement_is_runtime_fatality(self):
        catch = find_catch_block(self.read_sysfs)
        body = catch[catch.find("{") + 1 : catch.rfind("}")].strip()
        self.assertTrue(
            body.startswith("RuntimeFatality.throwIfFatal(t)"),
            f"catch body starts with: {body[:80]!r}",
        )

    def test_read_sysfs_does_not_directly_check_fatal_errors(self):
        catch = find_catch_block(self.read_sysfs)
        for name in ("OutOfMemoryError", "ThreadDeath", "VirtualMachineError"):
            self.assertNotIn(name, catch)

    def test_read_sysfs_has_no_periodic_failure_logging(self):
        for token in ("Log.", "println", "printStackTrace"):
            self.assertNotIn(token, self.read_sysfs)

    def test_run_tick_does_not_call_is_interactive(self):
        self.assertNotIn("isInteractive", self.run_tick)

    def test_start_reads_is_interactive_once(self):
        matches = re.findall(r"isInteractive", self.start)
        self.assertEqual(1, len(matches))

    def test_no_power_manager_field(self):
        self.assertIsNone(
            re.search(r"private\s+(?:var|val)\s+powerManager", self.monitor_text)
        )

    def test_run_tick_gate_before_io(self):
        # The gate is in a synchronized(lock) block before sampleAndPublish.
        self.assertIn("synchronized(lock)", self.run_tick)
        self.assertIn("isCurrentTick(", self.run_tick)
        self.assertIn("sampleAndPublish(", self.run_tick)
        gate_pos = self.run_tick.find("isCurrentTick(")
        io_pos = self.run_tick.find("sampleAndPublish(")
        self.assertLess(gate_pos, io_pos)

    def test_base_delay_unchanged(self):
        self.assertIn("BASE_DELAY_MS = 2_000L", self.monitor_text)

    def test_max_delay_unchanged(self):
        self.assertIn("MAX_DELAY_MS = 60_000L", self.monitor_text)

    def test_screen_off_removes_messages(self):
        block = find_braced_block(self.register, "Intent.ACTION_SCREEN_OFF")
        self.assertIn("removeMessages(MONITOR_MESSAGE)", block)
        self.assertIn("removeMessages(UPDATE_MESSAGE)", block)

    def test_screen_on_sends_immediate_message(self):
        block = find_braced_block(self.register, "Intent.ACTION_SCREEN_ON")
        self.assertIn("removeMessages(MONITOR_MESSAGE)", block)
        self.assertIn("sendEmptyMessage(MONITOR_MESSAGE)", block)

    def test_no_read_result_data_class(self):
        self.assertNotIn("data class ReadResult", self.monitor_text)

    def test_no_icon_update_data_class(self):
        self.assertNotIn("data class IconUpdate", self.monitor_text)

    def test_message_arg1_carries_type(self):
        self.assertIn("UPDATE_MESSAGE,\n                    type,", self.publish)
        self.assertIn("val type = msg.arg1", self.monitor_text)

    def test_message_arg2_carries_show(self):
        self.assertIn("if (show) 1 else 0", self.publish)
        self.assertIn("val show = msg.arg2 != 0", self.monitor_text)

    def test_message_obj_carries_string(self):
        self.assertRegex(self.publish, r"obtainMessage\(\s*UPDATE_MESSAGE\s*,\s*\n\s*type\s*,\s*\n\s*if\s*\(show\)\s*1\s*else\s*0\s*,\s*\n\s*text\s*\)")
        self.assertIn("val text = msg.obj as? String", self.monitor_text)

    def test_message_types_91_and_92_preserved(self):
        # 91 is used for the battery icon, 92 for the temperature icon.
        self.assertIn("91", self.monitor_text)
        self.assertIn("92", self.monitor_text)

    def test_text_state_change_detection_preserved(self):
        self.assertIn(
            "state.show != show || state.text != text", self.publish
        )

    def test_generation_check_preserved_in_publish(self):
        self.assertIn("isCurrentTick(", self.publish)


class ScopeProtectionTest(unittest.TestCase):
    EXCLUDED_FILES = {
        "LauncherAnimationHooks.kt",
        "LauncherGestureHooks.kt",
        "LauncherLayoutHooks.kt",
        "HookUtils.kt",
        "LockScreenAlbumArtController.kt",
        "BatteryIndicator.kt",
        "SystemAudioAndVisualAndMoreHooks.kt",
        "FeatureDispatcher.kt",
        "HookInstaller.kt",
        "HookTargetResolver.kt",
        "ModuleHelper.java",
        "RuntimeFatality.kt",
        "DiagnosticRecorder.kt",
        "app/build.gradle.kts",
        "CHANGELOG.md",
        "README.md",
        "README_EN.md",
        "docs/release-manifest.json",
        "gradle/verification-metadata.xml",
        "docs/audit/SOURCE_HAZARD_BASELINE.json",
    }

    def test_excluded_source_files_not_modified(self):
        for path in changed_files():
            self.assertNotIn(
                Path(path).name,
                self.EXCLUDED_FILES,
                f"excluded file {path} was modified",
            )

    def test_no_print_stack_trace_in_production_source(self):
        for kt in (REPO_ROOT / "app/src/main/java").rglob("*.kt"):
            text = kt.read_text(encoding="utf-8")
            for line in text.splitlines():
                self.assertNotIn(".printStackTrace(", line)
        for java in (REPO_ROOT / "app/src/main/java").rglob("*.java"):
            text = java.read_text(encoding="utf-8")
            for line in text.splitlines():
                self.assertNotIn(".printStackTrace(", line)


if __name__ == "__main__":
    unittest.main()
