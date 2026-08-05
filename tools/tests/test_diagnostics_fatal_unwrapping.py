#!/usr/bin/env python3
"""Static contract tests for diagnostics fatal unwrapping."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
RUNTIME_FATALITY = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils" / "RuntimeFatality.kt"
DIAGNOSTIC_RECORDER = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "diagnostics" / "DiagnosticRecorder.kt"
ROM_ENV_DIAG = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "compat" / "RomEnvironmentDiagnostics.kt"
FEATURE_DISPATCHER = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "catalog" / "FeatureDispatcher.kt"
MODULE_HELPER = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils" / "ModuleHelper.java"


class DiagnosticsFatalUnwrappingContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.runtime_text = RUNTIME_FATALITY.read_text(encoding="utf-8")
        cls.recorder_text = DIAGNOSTIC_RECORDER.read_text(encoding="utf-8")
        cls.rom_text = ROM_ENV_DIAG.read_text(encoding="utf-8")
        cls.dispatcher_text = FEATURE_DISPATCHER.read_text(encoding="utf-8")

    # ------------------------------------------------------------------
    # 1-13. RuntimeFatality
    # ------------------------------------------------------------------

    def test_runtime_fatality_file_exists(self):
        self.assertTrue(RUNTIME_FATALITY.exists())

    def test_runtime_fatality_is_internal(self):
        self.assertIn("internal object RuntimeFatality", self.runtime_text)

    def test_throw_if_fatal_signature_exists(self):
        self.assertIn("fun throwIfFatal(throwable: Throwable?)", self.runtime_text)

    def test_runtime_fatality_checks_three_fatals(self):
        self.assertIn("is OutOfMemoryError", self.runtime_text)
        self.assertIn("is ThreadDeath", self.runtime_text)
        self.assertIn("is VirtualMachineError", self.runtime_text)

    def test_runtime_fatality_does_not_check_all_errors(self):
        for keyword in ("is Error", "current is Error", "throwable is Error"):
            with self.subTest(keyword=keyword):
                self.assertNotIn(keyword, self.runtime_text)

    def test_runtime_fatality_rethrows_current_instance(self):
        self.assertIn("throw current", self.runtime_text)

    def test_runtime_fatality_has_depth_limit_of_eight(self):
        pattern = re.compile(r"\b8\b")
        self.assertRegex(self.runtime_text, pattern)

    def test_runtime_fatality_traverses_cause(self):
        self.assertIn(".cause", self.runtime_text)

    def test_runtime_fatality_has_self_reference_guard(self):
        self.assertIn("next === current", self.runtime_text)

    def test_runtime_fatality_null_returns_normally(self):
        body = self._extract_kt_function_body("throwIfFatal")
        self.assertIsNotNone(body)
        self.assertIn("if (current == null) return", body)

    def test_runtime_fatality_has_no_logging(self):
        self.assertNotIn(".log(", self.runtime_text)

    def test_runtime_fatality_has_no_diagnostics_call(self):
        for name in ("DiagnosticRecorder", "record", "recordSafely"):
            with self.subTest(name=name):
                self.assertNotIn(name, self.runtime_text)

    def test_runtime_fatality_does_not_reference_reflection_fatality(self):
        self.assertNotIn("ReflectionFatality", self.runtime_text)

    # ------------------------------------------------------------------
    # 14-31. DiagnosticRecorder
    # ------------------------------------------------------------------

    def test_diagnostic_recorder_imports_runtime_fatality(self):
        self.assertIn("import tv.withaibuild.customiuizer.mods.utils.RuntimeFatality", self.recorder_text)

    def test_diagnostic_recorder_injected_logger_catch_calls_helper(self):
        body = self._extract_kt_function_body("record")
        self.assertIsNotNone(body)
        injected_catch = self._extract_first_catch_body(body)
        self.assertIsNotNone(injected_catch)
        self.assertIn("RuntimeFatality.throwIfFatal(t)", injected_catch)
        self.assertIn("logFallbackSafely", injected_catch)

    def test_diagnostic_recorder_helper_before_fallback_logger(self):
        body = self._extract_kt_function_body("record")
        injected_catch = self._extract_first_catch_body(body)
        fatal_pos = injected_catch.find("RuntimeFatality.throwIfFatal")
        log_pos = injected_catch.find("logFallbackSafely")
        self.assertNotEqual(-1, fatal_pos)
        self.assertNotEqual(-1, log_pos)
        self.assertLess(fatal_pos, log_pos)

    def test_injected_fatal_does_not_call_fallback_before_helper(self):
        body = self._extract_kt_function_body("record")
        injected_catch = self._extract_first_catch_body(body)
        lines = injected_catch.splitlines()
        fatal_line = -1
        fallback_line = -1
        for i, line in enumerate(lines):
            if "RuntimeFatality.throwIfFatal" in line:
                fatal_line = i
            if "logFallbackSafely" in line:
                fallback_line = i
        self.assertNotEqual(-1, fatal_line)
        self.assertNotEqual(-1, fallback_line)
        self.assertLess(fatal_line, fallback_line)

    def test_log_fallback_safely_exists_and_is_unique(self):
        matches = re.findall(r"private fun logFallbackSafely", self.recorder_text)
        self.assertEqual(1, len(matches))

    def test_log_fallback_safely_catch_calls_helper(self):
        body = self._extract_kt_function_body("logFallbackSafely")
        self.assertIsNotNone(body)
        catch = self._extract_first_catch_body(body)
        self.assertIn("RuntimeFatality.throwIfFatal", catch)

    def test_log_fallback_safely_catch_has_no_rethrow(self):
        body = self._extract_kt_function_body("logFallbackSafely")
        catch = self._extract_first_catch_body(body)
        self.assertNotIn("throw t", catch)

    def test_logger_null_path_calls_log_fallback_safely_with_log_line(self):
        body = self._extract_kt_function_body("record")
        self.assertIn("logFallbackSafely(logLine)", body)

    def test_primary_ordinary_failure_uses_original_message(self):
        body = self._extract_kt_function_body("record")
        injected_catch = self._extract_first_catch_body(body)
        self.assertIn('"Diagnostic logger failed: ${t.message}"', injected_catch)

    def test_fallback_logger_is_internal(self):
        self.assertIn("internal var fallbackLogger: (String) -> Unit", self.recorder_text)

    def test_fallback_logger_default_delegates_to_xposed_helpers(self):
        self.assertIn("fallbackLogger: (String) -> Unit = { XposedHelpers.log(it) }", self.recorder_text)

    def test_reset_restores_fallback_logger(self):
        reset_body = self._extract_kt_function_body("reset")
        self.assertIn("fallbackLogger = { XposedHelpers.log(it) }", reset_body)

    def test_diagnostic_recorder_no_direct_fatal_instanceof(self):
        rest = self.recorder_text
        helper = self._extract_kt_function_body("logFallbackSafely") or ""
        rest = rest.replace(helper, "")
        for pattern in ("is OutOfMemoryError", "is ThreadDeath", "is VirtualMachineError"):
            with self.subTest(pattern=pattern):
                self.assertNotIn(pattern, rest)

    def test_diagnostic_recorder_does_not_call_t_cause(self):
        for keyword in ("t.cause", "t.getCause()"):
            with self.subTest(keyword=keyword):
                self.assertNotIn(keyword, self.recorder_text)

    def test_diagnostic_recorder_constants_unchanged(self):
        self.assertIn("MAX_SNAPSHOTS = 32", self.recorder_text)
        self.assertIn("MAX_DETAIL_LENGTH = 512", self.recorder_text)
        self.assertIn("THROTTLE_MS = 60_000L", self.recorder_text)

    def test_snapshot_and_throttle_logic_unchanged(self):
        self.assertIn("logThrottler[id] = now", self.recorder_text)
        self.assertIn("snapshots[id] = snapshot", self.recorder_text)

    # ------------------------------------------------------------------
    # 32-40. RomEnvironmentDiagnostics
    # ------------------------------------------------------------------

    def test_rom_env_diag_imports_runtime_fatality(self):
        self.assertIn("import tv.withaibuild.customiuizer.mods.utils.RuntimeFatality", self.rom_text)

    def test_record_safely_has_single_catch_throwable(self):
        body = self._extract_kt_function_body("recordSafely")
        self.assertIsNotNone(body)
        catches = self._extract_all_catch_blocks(body)
        self.assertEqual(1, len(catches))
        full = self._extract_kt_function_body("recordSafely")
        self.assertIn("catch (t: Throwable)", full)

    def test_record_safely_catch_calls_helper_first(self):
        body = self._extract_kt_function_body("recordSafely")
        catch = self._extract_first_catch_body(body)
        stripped = self._strip_comments(catch)
        self.assertRegex(
            stripped,
            r"^\s*RuntimeFatality\.throwIfFatal\s*\(\s*t\s*\)",
            "RuntimeFatality.throwIfFatal(t) must be first in recordSafely catch",
        )

    def test_record_safely_no_direct_catch_oom(self):
        self.assertNotIn("catch (oom: OutOfMemoryError)", self.rom_text)

    def test_record_safely_no_direct_fatal_is_checks(self):
        for pattern in ("is OutOfMemoryError", "is ThreadDeath", "is VirtualMachineError"):
            with self.subTest(pattern=pattern):
                self.assertNotIn(pattern, self.rom_text)

    def test_record_safely_does_not_call_t_cause(self):
        for keyword in ("t.cause", "t.getCause()"):
            with self.subTest(keyword=keyword):
                self.assertNotIn(keyword, self.rom_text)

    def test_record_safely_ordinary_path_no_rethrow_no_log(self):
        body = self._extract_kt_function_body("recordSafely")
        catch = self._extract_first_catch_body(body)
        self.assertNotIn("throw t", catch)
        self.assertNotIn("log(", catch)

    def test_record_method_body_unchanged(self):
        body = self._extract_kt_function_body("record", source="rom")
        self.assertIn('id = "rom.environment"', body)
        for profile in ("RomProfile.MIUI14_A13", "RomProfile.HYPEROS1_A13", "RomProfile.UNKNOWN_A13", "RomProfile.UNSUPPORTED_ANDROID"):
            with self.subTest(profile=profile):
                self.assertIn(profile, body)

    def test_diagnostic_id_remains_rom_environment(self):
        self.assertIn('id = "rom.environment"', self.rom_text)

    def test_evidence_append_order_unchanged(self):
        body = self._extract_kt_function_body("record", source="rom")
        self.assertIn("appendEvidence(this, environment)", body)

    # ------------------------------------------------------------------
    # 41-46. Scope protection
    # ------------------------------------------------------------------

    def test_feature_dispatcher_not_modified(self):
        self.assertNotIn("RuntimeFatality", self.dispatcher_text)
        self.assertIn("FeatureDispatcher", self.dispatcher_text)

    def test_module_helper_not_modified(self):
        helper_text = MODULE_HELPER.read_text(encoding="utf-8")
        self.assertIn("public class ModuleHelper", helper_text)

    def test_xposed_helpers_not_modified(self):
        xposed = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils" / "XposedHelpers.java"
        self.assertTrue(xposed.exists())

    def test_hook_installer_not_modified(self):
        hooker = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils" / "HookInstaller.kt"
        self.assertTrue(hooker.exists())

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

    def _function_text(self, name, source=None):
        if source:
            return getattr(self, f"{source}_text")
        if name in ("logFallbackSafely", "reset") or (name == "record" and self.recorder_text.count(f"fun {name}") == 1):
            return self.recorder_text
        if name == "throwIfFatal":
            return self.runtime_text
        return self.rom_text

    def _extract_kt_function_body(self, name, source=None):
        text = self._function_text(name, source=source)
        pattern = rf"\bfun\s+{re.escape(name)}\s*\([^)]*\)\s*{{"
        match = re.search(pattern, text)
        if not match:
            return None
        brace = self._find_brace_start(text, match.start())
        return self._brace_body(text, brace)

    def _find_brace_start(self, text, start):
        for i in range(start, len(text)):
            if text[i] == "{":
                return i
        return None

    def _extract_first_catch_body(self, body):
        match = re.search(r"catch\s*\([^)]*\)\s*{", body)
        if not match:
            return None
        brace = body.find("{", match.start())
        return self._brace_body(body, brace)

    def _extract_all_catch_blocks(self, body):
        catches = []
        start = 0
        while True:
            match = re.search(r"catch\s*\([^)]*\)\s*{", body[start:])
            if not match:
                break
            brace = body.find("{", start + match.start())
            catch_body = self._brace_body(body, brace)
            catches.append(catch_body)
            start = brace + 1
        return catches

    def _brace_body(self, text, brace_start):
        if brace_start is None:
            return None
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
