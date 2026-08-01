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
        self.assertEqual("ROM_PROFILE_DETECTED", profile.rom_environment[0]["reason"])
        self.assertIn("MIUI14_A13", profile.rom_environment[0]["detail"])

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

    def test_hyperos_fallback_counted(self) -> None:
        profile = analyze.LogProfile()
        profile.bump_kind("Diagnostic[statusBarClockTweak] DEGRADED compat=DEGRADED fallback=true reason=HYPEROS_FALLBACK_FOUND")
        self.assertEqual(1, profile.hyperos_fallback)

    def test_hyperos_target_not_found_counted(self) -> None:
        profile = analyze.LogProfile()
        profile.bump_kind("Diagnostic[statusBarClockTweak] INCOMPATIBLE compat=INCOMPATIBLE reason=HYPEROS_TARGET_NOT_FOUND")
        self.assertEqual(1, profile.hyperos_target_not_found)

    def test_markdown_summary_contains_rom_environment(self) -> None:
        profile = analyze.LogProfile()
        profile.bump_kind("Diagnostic[rom.environment] COMPATIBLE compat=COMPATIBLE reason=ROM_PROFILE_DETECTED detail=HYPEROS1_A13")
        summary = analyze.markdown_summary(profile)
        self.assertIn("ROM environments", summary)
        self.assertIn("HYPEROS1_A13", summary)


if __name__ == "__main__":
    unittest.main()
