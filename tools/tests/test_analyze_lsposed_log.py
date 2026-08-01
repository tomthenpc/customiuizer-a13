"""LSPosed log analyzer invariants for ROM environment and HyperOS fallback."""

import importlib.util
import io
import sys
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
ANALYZER_PATH = REPO_ROOT / "tools" / "analyze_lsposed_log.py"

_spec = importlib.util.spec_from_file_location("analyze_lsposed_log", ANALYZER_PATH)
analyze = importlib.util.module_from_spec(_spec)
sys.modules["analyze_lsposed_log"] = analyze
_spec.loader.exec_module(analyze)


class RomEnvironmentParsingTest(unittest.TestCase):

    def test_parse_rom_environment_detail(self) -> None:
        profile = analyze.LogProfile()
        line = "Diagnostic[rom.environment] COMPATIBLE compat=COMPATIBLE reason=ROM_PROFILE_DETECTED detail=MIUI14_A13; sdk=33; evidence=[display=TKQ1, miui=V14]"
        profile.bump_kind(line)
        self.assertEqual(1, len(profile.rom_environment))
        self.assertEqual("COMPATIBLE", profile.rom_environment[0]["state"])
        self.assertEqual("COMPATIBLE", profile.rom_environment[0]["compatibility"])
        self.assertEqual("ROM_PROFILE_DETECTED", profile.rom_environment[0]["reason"])
        self.assertIn("MIUI14_A13", profile.rom_environment[0]["detail"])

    def test_parse_falls_back_to_state_for_compatibility(self) -> None:
        profile = analyze.LogProfile()
        profile.bump_kind("Diagnostic[rom.environment] COMPATIBLE reason=ROM_PROFILE_UNKNOWN detail=UNKNOWN_A13")
        self.assertEqual(1, len(profile.rom_environment))
        self.assertEqual("COMPATIBLE", profile.rom_environment[0]["state"])
        self.assertEqual("COMPATIBLE", profile.rom_environment[0]["compatibility"])

    def test_rom_environment_dedup_and_bound(self) -> None:
        profile = analyze.LogProfile()
        for _ in range(40):
            profile.bump_kind("Diagnostic[rom.environment] COMPATIBLE compat=COMPATIBLE reason=ROM_PROFILE_DETECTED detail=MIUI14_A13; sdk=33")
        self.assertEqual(1, len(profile.rom_environment))
        self.assertEqual(0, profile.rom_environment_overflow)

    def test_rom_environment_overflow(self) -> None:
        profile = analyze.LogProfile()
        for i in range(40):
            profile.bump_kind(
                f"Diagnostic[rom.environment] COMPATIBLE compat=COMPATIBLE reason=ROM_PROFILE_DETECTED detail=profile={i}"
            )
        self.assertEqual(32, len(profile.rom_environment))
        self.assertEqual(8, profile.rom_environment_overflow)

    def test_compatibility_is_part_of_dedup_key(self) -> None:
        profile = analyze.LogProfile()
        profile.bump_kind("Diagnostic[rom.environment] COMPATIBLE compat=COMPATIBLE reason=ROM_PROFILE_DETECTED detail=same")
        profile.bump_kind("Diagnostic[rom.environment] COMPATIBLE compat=DEGRADED reason=ROM_PROFILE_DETECTED detail=same")
        self.assertEqual(2, len(profile.rom_environment))

    def test_hyperos_fallback_counted(self) -> None:
        profile = analyze.LogProfile()
        profile.bump_kind("Diagnostic[statusBarClockTweak] DEGRADED compat=DEGRADED fallback=true reason=HYPEROS_FALLBACK_FOUND")
        self.assertEqual(1, profile.hyperos_fallback)

    def test_hyperos_target_not_found_counted(self) -> None:
        profile = analyze.LogProfile()
        profile.bump_kind("Diagnostic[statusBarClockTweak] INCOMPATIBLE compat=INCOMPATIBLE reason=HYPEROS_TARGET_NOT_FOUND")
        self.assertEqual(1, profile.hyperos_target_not_found)

    def test_markdown_summary_contains_rom_table(self) -> None:
        profile = analyze.LogProfile()
        profile.bump_kind("Diagnostic[rom.environment] COMPATIBLE compat=COMPATIBLE reason=ROM_PROFILE_DETECTED detail=HYPEROS1_A13")
        summary = analyze.markdown_summary(profile)
        self.assertIn("## ROM Environments", summary)
        self.assertIn("| State | Compatibility | Reason | Detail |", summary)
        self.assertIn("COMPATIBLE", summary)
        self.assertIn("HYPEROS1_A13", summary)

    def test_markdown_summary_escapes_pipe(self) -> None:
        profile = analyze.LogProfile()
        profile.bump_kind("Diagnostic[rom.environment] COMPATIBLE compat=COMPATIBLE reason=ROM_PROFILE_DETECTED detail=has|pipe")
        summary = analyze.markdown_summary(profile)
        self.assertIn("has\\|pipe", summary)

    def test_markdown_summary_handles_newline_in_detail(self) -> None:
        profile = analyze.LogProfile()
        profile.bump_kind("Diagnostic[rom.environment] COMPATIBLE compat=COMPATIBLE reason=ROM_PROFILE_DETECTED detail=line1\nline2")
        summary = analyze.markdown_summary(profile)
        self.assertNotIn("\nline2", summary)

    def test_text_summary_contains_rom_environment(self) -> None:
        profile = analyze.LogProfile()
        profile.bump_kind("Diagnostic[rom.environment] COMPATIBLE compat=COMPATIBLE reason=ROM_PROFILE_DETECTED detail=MIUI14_A13")
        summary = analyze.text_summary(profile)
        self.assertIn("ROM environments:", summary)
        self.assertIn("compat=COMPATIBLE", summary)

    def test_malformed_line_does_not_crash(self) -> None:
        profile = analyze.LogProfile()
        profile.bump_kind("Diagnostic[rom.environment] not well formed")
        self.assertEqual(0, len(profile.rom_environment))

    def test_empty_profile_markdown(self) -> None:
        profile = analyze.LogProfile()
        summary = analyze.markdown_summary(profile)
        self.assertIn("## ROM Environments", summary)
        self.assertIn("None", summary)


if __name__ == "__main__":
    unittest.main()
