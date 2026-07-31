"""UI text inheritance and About attribution display invariants for A13."""

import importlib.util
import sys
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
PREFS_DIR = (
    REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "prefs"
)
ABOUT_HEAD_PATH = REPO_ROOT / "app" / "src" / "main" / "res" / "layout" / "fragment_about_head.xml"
INVARIANTS_PATH = REPO_ROOT / "tools" / "check-invariants.py"

_spec = importlib.util.spec_from_file_location("check_invariants", INVARIANTS_PATH)
check_invariants = importlib.util.module_from_spec(_spec)
sys.modules["check_invariants"] = check_invariants
_spec.loader.exec_module(check_invariants)

ANDROID_NS = "http://schemas.android.com/apk/res/android"


def _make_about_xml(
    maintainer_extra: str = "",
    based_on_extra: str = "",
    version_extra: str = "",
) -> str:
    return f"""<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="{ANDROID_NS}"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <TextView
        android:id="@+id/about_maintainer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        {maintainer_extra} />

    <TextView
        android:id="@+id/about_based_on"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        {based_on_extra} />

    <TextView
        android:id="@+id/about_version"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        {version_extra} />
</LinearLayout>
"""


def _make_pref_class(
    name: str,
    super_call: str,
    defstyle: str | None = "androidx.preference.R.attr.preferenceStyle",
    extra_body: str = "",
) -> str:
    params = "    context: Context,\n    attrs: AttributeSet? = null"
    if defstyle is not None:
        params += f",\n    defStyleAttr: Int = {defstyle}"
    body = "\n" + extra_body if extra_body else ""
    return (
        f"class {name} @JvmOverloads constructor(\n"
        f"{params}\n"
        f") : {super_call} {{{body}}}\n"
    )


def _about_findings(xml: str) -> list:
    return check_invariants.check_about_text_wrapping(
        Path("fragment_about_head.xml"),
        xml,
    )


def _pref_findings(name: str, text: str) -> list:
    return check_invariants.check_preference_style_attr(Path(name), text)


class AboutTextViewInvariant(unittest.TestCase):
    """About page attribution TextViews must not truncate."""

    def test_real_about_layout_passes(self) -> None:
        xml = ABOUT_HEAD_PATH.read_text(encoding="utf-8")
        findings = _about_findings(xml)
        self.assertEqual(findings, [], f"real layout findings: {findings}")

    def test_about_maintainer_max_lines_fails(self) -> None:
        xml = _make_about_xml(maintainer_extra='android:maxLines="1"')
        self.assertTrue(_about_findings(xml))

    def test_about_based_on_ellipsize_fails(self) -> None:
        xml = _make_about_xml(based_on_extra='android:ellipsize="end"')
        self.assertTrue(_about_findings(xml))

    def test_about_version_single_line_fails(self) -> None:
        xml = _make_about_xml(version_extra='android:singleLine="true"')
        self.assertTrue(_about_findings(xml))

    def test_missing_about_id_fails(self) -> None:
        xml = _make_about_xml().replace("about_maintainer", "about_maintainer_missing")
        self.assertTrue(_about_findings(xml))

    def test_fixed_layout_height_fails(self) -> None:
        xml = _make_about_xml(version_extra='android:layout_height="48dp"')
        self.assertTrue(_about_findings(xml))


class PreferenceStyleInvariant(unittest.TestCase):
    """Custom Preference classes must use the correct defStyleAttr."""

    def test_expected_set_is_complete(self) -> None:
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
            path = PREFS_DIR / name
            self.assertTrue(path.is_file(), f"{name} must exist")
            findings = _pref_findings(name, path.read_text(encoding="utf-8"))
            self.assertEqual(findings, [], f"{name}: {findings}")

    def test_defstyle_zero_fails(self) -> None:
        text = _make_pref_class(
            "PreferenceEx",
            "Preference(context, attrs, defStyleAttr)",
            defstyle="0",
        )
        self.assertTrue(_pref_findings("PreferenceEx.kt", text), "defStyleAttr=0")

    def test_wrong_attr_fails(self) -> None:
        text = _make_pref_class(
            "PreferenceEx",
            "Preference(context, attrs, defStyleAttr)",
            defstyle="androidx.preference.R.attr.switchPreferenceStyle",
        )
        self.assertTrue(_pref_findings("PreferenceEx.kt", text), "wrong attr")

    def test_missing_defstyle_fails(self) -> None:
        text = _make_pref_class(
            "ListPreferenceEx",
            "ListPreference(context, attrs)",
            defstyle=None,
        )
        self.assertTrue(_pref_findings("ListPreferenceEx.kt", text), "missing defStyleAttr")

    def test_duplicate_defstyle_fails(self) -> None:
        text = _make_pref_class(
            "DropDownPreferenceEx",
            "DropDownPreference(context, attrs, defStyleAttr)",
            defstyle="androidx.preference.R.attr.dropdownPreferenceStyle",
            extra_body="    private val defStyleAttr: Int = 0",
        )
        self.assertTrue(_pref_findings("DropDownPreferenceEx.kt", text), "duplicate defStyleAttr")

    def test_super_third_arg_zero_fails(self) -> None:
        text = _make_pref_class(
            "SeekBarPreference",
            "Preference(context, attrs, 0)",
        )
        self.assertTrue(_pref_findings("SeekBarPreference.kt", text), "super(..., 0)")

    def test_super_omits_defstyle_fails(self) -> None:
        text = _make_pref_class(
            "SeekBarPreference",
            "Preference(context, attrs)",
        )
        self.assertTrue(_pref_findings("SeekBarPreference.kt", text), "super omits defStyleAttr")

    def test_completeness_finds_missing_file(self) -> None:
        original = dict(check_invariants.EXPECTED_DEFSTYLE)
        try:
            check_invariants.EXPECTED_DEFSTYLE = {
                "__MissingPreference.kt": "androidx.preference.R.attr.preferenceStyle"
            }
            findings = check_invariants.check_preference_style_attr_completeness()
            self.assertTrue(findings, "missing file")
        finally:
            check_invariants.EXPECTED_DEFSTYLE = original


if __name__ == "__main__":
    unittest.main()
