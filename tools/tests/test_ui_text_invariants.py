"""UI text inheritance and About attribution display invariants for A13."""

from __future__ import annotations

import importlib.util
import os
import re
import sys
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
SOURCE_ROOT = REPO_ROOT / "app" / "src" / "main" / "java"
RES_ROOT = REPO_ROOT / "app" / "src" / "main" / "res"
PREFS_DIR = SOURCE_ROOT / "tv" / "withaibuild" / "customiuizer" / "prefs"
INVARIANTS_PATH = REPO_ROOT / "tools" / "check-invariants.py"
ABOUT_HEAD_PATH = RES_ROOT / "layout" / "fragment_about_head.xml"

_spec = importlib.util.spec_from_file_location("check_invariants", INVARIANTS_PATH)
check_invariants = importlib.util.module_from_spec(_spec)
sys.modules["check_invariants"] = check_invariants
_spec.loader.exec_module(check_invariants)

ANDROID_NS = "http://schemas.android.com/apk/res/android"


class AboutTextViewInvariant(unittest.TestCase):
    """About page attribution TextViews must not truncate."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.tree = ET.parse(ABOUT_HEAD_PATH)

    def _attr(self, elem: ET.Element, name: str) -> str | None:
        return elem.attrib.get(f"{{{ANDROID_NS}}}{name}")

    def _find_by_id(self, view_id: str) -> ET.Element | None:
        for elem in self.tree.iter():
            val = self._attr(elem, "id")
            if val is not None and val.endswith(f"/{view_id}"):
                return elem
        return None

    def _assert_no_truncation(self, view_id: str) -> ET.Element:
        elem = self._find_by_id(view_id)
        self.assertIsNotNone(elem, f"TextView @+id/{view_id} not found")

        forbidden = (
            "ellipsize",
            "maxLines",
            "singleLine",
            "horizontallyScrolling",
            "autoSizeTextType",
        )
        for attr in forbidden:
            self.assertIsNone(
                self._attr(elem, attr),
                f"@+id/{view_id} must not set android:{attr}",
            )

        self.assertEqual(
            self._attr(elem, "layout_width"),
            "match_parent",
            f"@+id/{view_id} must have android:layout_width=match_parent",
        )
        self.assertEqual(
            self._attr(elem, "layout_height"),
            "wrap_content",
            f"@+id/{view_id} must have android:layout_height=wrap_content",
        )
        return elem

    def test_about_maintainer_wraps(self) -> None:
        self._assert_no_truncation("about_maintainer")

    def test_about_based_on_wraps(self) -> None:
        self._assert_no_truncation("about_based_on")

    def test_about_version_wraps(self) -> None:
        self._assert_no_truncation("about_version")

    def _assert_has_attribute(
        self,
        view_id: str,
        attr: str,
        value: str,
    ) -> None:
        xml = (
            '<?xml version="1.0" encoding="utf-8"?>'
            f'<LinearLayout xmlns:android="{ANDROID_NS}">'
            f'<TextView android:id="@+id/{view_id}" '
            f'android:{attr}="{value}" '
            'android:layout_width="match_parent" '
            'android:layout_height="wrap_content" />'
            '</LinearLayout>'
        )
        root = ET.fromstring(xml)
        for elem in root.iter():
            if elem.attrib.get(f"{{{ANDROID_NS}}}id", "").endswith(f"/{view_id}"):
                self.assertEqual(
                    elem.attrib.get(f"{{{ANDROID_NS}}}{attr}"),
                    value,
                    f"injected {attr} should be present",
                )
                return
        self.fail(f"{view_id} not found in injected XML")

    def test_about_maintainer_with_max_lines_fails(self) -> None:
        self._assert_has_attribute("about_maintainer", "maxLines", "1")

    def test_about_based_on_with_ellipsize_fails(self) -> None:
        self._assert_has_attribute("about_based_on", "ellipsize", "end")


class PreferenceStyleInvariant(unittest.TestCase):
    """Custom Preference classes must use the correct defStyleAttr."""

    def _check(self, name: str) -> list:
        path = PREFS_DIR / name
        self.assertTrue(path.is_file(), f"{name} must exist")
        text = path.read_text(encoding="utf-8")
        return check_invariants.check_preference_style_attr(path, text)

    def _check_synthetic(self, name: str, text: str) -> list:
        return check_invariants.check_preference_style_attr(Path(name), text)

    def test_expected_files_exist(self) -> None:
        for name in check_invariants.EXPECTED_DEFSTYLE:
            self.assertTrue(
                (PREFS_DIR / name).is_file(),
                f"expected Preference {name} is missing",
            )

    def test_expected_set_is_complete(self) -> None:
        # If someone removes an entry from EXPECTED_DEFSTYLE, this set fails.
        self.assertEqual(
            set(check_invariants.EXPECTED_DEFSTYLE),
            {
                "CheckBoxPreferenceEx.kt",
                "DropDownPreferenceEx.kt",
                "EditTextPreferenceEx.kt",
                "ListPreferenceEx.kt",
                "PreferenceCategoryEx.kt",
                "PreferenceEx.kt",
                "SeekBarPreference.kt",
            },
        )

    def test_seven_preferences_pass(self) -> None:
        for name in check_invariants.EXPECTED_DEFSTYLE:
            findings = self._check(name)
            self.assertEqual(
                findings,
                [],
                f"{name} has preference-style-attr findings: {findings}",
            )

    def test_def_style_zero_fails(self) -> None:
        text = (
            "class PreferenceEx @JvmOverloads constructor(\n"
            "    context: Context,\n"
            "    attrs: AttributeSet? = null,\n"
            "    defStyleAttr: Int = 0\n"
            ") : Preference(context, attrs, defStyleAttr) {\n"
            "}\n"
        )
        findings = self._check_synthetic("PreferenceEx.kt", text)
        self.assertTrue(findings, "defStyleAttr=0 must be rejected")

    def test_super_call_with_zero_fails(self) -> None:
        text = (
            "class SeekBarPreference @JvmOverloads constructor(\n"
            "    context: Context,\n"
            "    attrs: AttributeSet? = null,\n"
            "    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle\n"
            ") : Preference(context, attrs, 0) {\n"
            "}\n"
        )
        findings = self._check_synthetic("SeekBarPreference.kt", text)
        self.assertTrue(findings, "super(..., 0) must be rejected")

    def test_super_call_omitting_defstyle_fails(self) -> None:
        text = (
            "class SeekBarPreference @JvmOverloads constructor(\n"
            "    context: Context,\n"
            "    attrs: AttributeSet? = null,\n"
            "    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle\n"
            ") : Preference(context, attrs) {\n"
            "}\n"
        )
        findings = self._check_synthetic("SeekBarPreference.kt", text)
        self.assertTrue(findings, "super(..., attrs) must be rejected")

    def test_missing_defstyle_fails(self) -> None:
        text = (
            "class ListPreferenceEx @JvmOverloads constructor(\n"
            "    context: Context,\n"
            "    attrs: AttributeSet? = null\n"
            ") : ListPreference(context, attrs) {\n"
            "}\n"
        )
        findings = self._check_synthetic("ListPreferenceEx.kt", text)
        self.assertTrue(findings, "missing defStyleAttr must be rejected")

    def test_duplicate_defstyle_fails(self) -> None:
        text = (
            "class DropDownPreferenceEx @JvmOverloads constructor(\n"
            "    context: Context,\n"
            "    attrs: AttributeSet? = null,\n"
            "    defStyleAttr: Int = androidx.preference.R.attr.dropdownPreferenceStyle,\n"
            "    another: Int = 0\n"
            ") : DropDownPreference(context, attrs, defStyleAttr) {\n"
            "    private val defStyleAttr: Int = 0\n"
            "}\n"
        )
        findings = self._check_synthetic("DropDownPreferenceEx.kt", text)
        self.assertTrue(findings, "duplicate defStyleAttr must be rejected")

    def test_completeness_finds_missing_file(self) -> None:
        original = dict(check_invariants.EXPECTED_DEFSTYLE)
        try:
            check_invariants.EXPECTED_DEFSTYLE = {
                "__MissingPreference.kt": "androidx.preference.R.attr.preferenceStyle"
            }
            findings = check_invariants.check_preference_style_attr_completeness()
            self.assertTrue(findings, "missing file must be reported")
            self.assertTrue(
                any("__MissingPreference.kt" in str(f) for f in findings),
                "finding must name the missing file",
            )
        finally:
            check_invariants.EXPECTED_DEFSTYLE = original


class ResourceFontInvariant(unittest.TestCase):
    """App UI resources must not hard-code a typeface or font family."""

    def test_no_font_directory(self) -> None:
        font_dir = RES_ROOT / "font"
        self.assertFalse(font_dir.is_dir(), "res/font/ must not exist")

    def test_no_font_family_or_typeface_in_resources(self) -> None:
        # Match fontFamily, android:fontFamily, typeface, android:typeface as
        # attributes, style items, or XML values.
        forbidden = re.compile(
            r"(?<![\w:-])"
            r"(?:android:|app:)?"
            r"(?:fontFamily|typeface)"
            r"(?![\w-])"
        )
        for xml_path in RES_ROOT.rglob("*.xml"):
            # Ignore compiled intermediates.
            if "build" in xml_path.parts:
                continue
            text = xml_path.read_text(encoding="utf-8")
            for match in forbidden.finditer(text):
                self.fail(
                    f"{xml_path.relative_to(REPO_ROOT)} uses font family/typeface "
                    f"at {match.start()}: {match.group(0)}"
                )


class AppCodeFontInvariant(unittest.TestCase):
    """App UI code must not force a concrete font family."""

    def _is_allowed_typeface(self, token: str) -> bool:
        # Only style constants are allowed as Typeface references in App UI code.
        return token in {"BOLD", "ITALIC"}

    def test_no_forced_font_family_outside_mods(self) -> None:
        typeface_dot = re.compile(r"Typeface\.([A-Za-z_]\w*)")
        for path in SOURCE_ROOT.rglob("*"):
            if not path.is_file():
                continue
            if path.suffix not in {".kt", ".java"}:
                continue
            # mods/ contains SystemUI/Launcher/system font logic and is covered
            # by hook-specific audits, not by App UI invariants.
            if "mods" in path.parts:
                continue
            text = path.read_text(encoding="utf-8")
            for match in typeface_dot.finditer(text):
                token = match.group(1)
                if not self._is_allowed_typeface(token):
                    self.fail(
                        f"{path.relative_to(REPO_ROOT)} uses Typeface.{token} "
                        f"which forces a concrete font/style"
                    )


if __name__ == "__main__":
    unittest.main()
