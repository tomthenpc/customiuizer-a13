#!/usr/bin/env python3
"""Static contract tests for launcher animation scale fastpath."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
LAUNCHER = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "LauncherAnimationHooks.kt"
HOOK_UTILS = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "utils" / "HookUtils.kt"
BASELINE = REPO / "docs" / "audit" / "SOURCE_HAZARD_BASELINE.json"
MAIN = REPO / "app" / "src" / "main" / "java"


class LauncherAnimationScaleFastpathTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.launcher = LAUNCHER.read_text(encoding="utf-8")
        cls.hook_utils = HOOK_UTILS.read_text(encoding="utf-8")

    def test_imports_value_animator(self):
        self.assertIn("import android.animation.ValueAnimator", self.launcher)

    def test_effective_animator_scale_exists(self):
        pattern = re.compile(r"internal fun effectiveAnimatorScale\s*\(")
        self.assertRegex(self.launcher, pattern)

    def test_zero_maps_to_min_non_zero(self):
        body = self._extract_kt_function_body("effectiveAnimatorScale")
        self.assertIsNotNone(body)
        self.assertIn("MIN_NON_ZERO_SCALE", body)

    def test_non_zero_returns_raw(self):
        body = self._extract_kt_function_body("effectiveAnimatorScale")
        self.assertRegex(body, r"(return\s+rawScale|else\s+rawScale)")

    def test_two_callbacks_use_fastpath(self):
        fix_body = self._extract_kt_function_body("FixAnimHook")
        self.assertEqual(2, fix_body.count("currentAnimatorScale()"))
        self.assertEqual(0, fix_body.count("HookUtils.getAnimationScale"))

    def test_each_callback_reads_scale_once(self):
        fix_body = self._extract_kt_function_body("FixAnimHook")
        # split by callback before bodies
        callbacks = re.findall(r"override fun before\s*\([^)]*\)\s*{", fix_body)
        self.assertEqual(2, len(callbacks))
        for body in self._extract_callback_bodies(fix_body):
            self.assertEqual(1, body.count("currentAnimatorScale()"))

    def test_scale_one_earlies_return(self):
        for body in self._extract_callback_bodies(self._extract_kt_function_body("FixAnimHook")):
            self.assertIn("if (scale == 1.0f) return", body)

    def test_scale_stiffness_preserved(self):
        self.assertIn("private fun scaleStiffness", self.launcher)
        body = self._extract_kt_function_body("scaleStiffness")
        self.assertIn("if (scale < 1.0f)", body)

    def test_stiffness_fields_preserved(self):
        for field in (
            "mCenterXStiffness",
            "mCenterYStiffness",
            "mWidthStiffness",
            "mRadiusStiffness",
            "mAlphaStiffness",
        ):
            with self.subTest(field=field):
                self.assertIn(field, self.launcher)

    def test_ratio_stiffness_preserved(self):
        self.assertIn("mRatioStiffness", self.launcher)

    def test_radio_stiffness_fallback_preserved(self):
        fix_body = self._extract_kt_function_body("FixAnimHook")
        self.assertIn("mRadioStiffness", fix_body)

    def test_no_class_for_name(self):
        self.assertNotIn("Class.forName", self.launcher)

    def test_no_get_declared_method(self):
        self.assertNotIn("getDeclaredMethod", self.launcher)

    def test_no_service_manager(self):
        self.assertNotIn("ServiceManager", self.launcher)

    def test_no_iwindow_manager(self):
        self.assertNotIn("IWindowManager", self.launcher)

    def test_no_ibinder(self):
        self.assertNotIn("IBinder", self.launcher)

    def test_no_settings_global(self):
        self.assertNotIn("Settings.Global", self.launcher)

    def test_hookutils_no_longer_declares_get_animation_scale(self):
        pattern = re.compile(r"fun getAnimationScale\s*\(")
        self.assertNotRegex(self.hook_utils, pattern)

    def test_hookutils_no_reflection_strings(self):
        for keyword in ("ServiceManager", "IWindowManager", "getDeclaredMethod"):
            with self.subTest(keyword=keyword):
                self.assertNotIn(keyword, self.hook_utils)

    def test_no_cached_scale_field(self):
        for kw in ("mAnimatorScale", "cachedScale", "lastDurationScale"):
            with self.subTest(kw=kw):
                self.assertNotIn(kw, self.launcher)

    def test_no_lambda_reader_seam(self):
        pattern = re.compile(r"var\s+.*=\s*\{\s*ValueAnimator\.getDurationScale")
        self.assertNotRegex(self.launcher, pattern)

    def test_other_launcher_hook_files_not_modified(self):
        for name in ("LauncherGestureHooks.kt", "LauncherLayoutHooks.kt", "LauncherFolderHooks.kt", "LauncherIconHooks.kt"):
            with self.subTest(name=name):
                path = REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / name
                text = path.read_text(encoding="utf-8")
                self.assertNotIn("ValueAnimator.getDurationScale", text)

    def test_baseline_empty(self):
        text = BASELINE.read_text(encoding="utf-8")
        self.assertIn('"fingerprints": []', text)
        self.assertIn('"findings": []', text)

    def test_no_print_stack_trace(self):
        for path in MAIN.rglob("*"):
            if path.is_file() and path.suffix in (".kt", ".java"):
                with self.subTest(path=path.relative_to(REPO)):
                    text = path.read_text(encoding="utf-8")
                    self.assertIsNone(
                        re.search(r"\.printStackTrace\s*\(", text),
                        f"{path} contains .printStackTrace(",
                    )

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------

    def _extract_kt_function_body(self, name):
        pattern = rf"\bfun\s+{re.escape(name)}\s*\([^)]*\)(?:\s*:\s*[^{{]+)?\s*{{"
        match = re.search(pattern, self.launcher)
        if not match:
            return None
        brace = self.launcher.find("{", match.start())
        return self._brace_body(self.launcher, brace)

    def _extract_callback_bodies(self, body):
        bodies = []
        start = 0
        while True:
            match = re.search(r"override fun before\s*\([^)]*\)\s*{", body[start:])
            if not match:
                break
            brace = body.find("{", start + match.start())
            bodies.append(self._brace_body(body, brace))
            start = brace + 1
        return bodies

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
            if c == '"' or c == "'" or c == '`':
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
