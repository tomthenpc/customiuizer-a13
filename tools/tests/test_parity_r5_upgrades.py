#!/usr/bin/env python3
"""Phase F-R5 production upgrade regressions."""
from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class PhaseFR5UpgradeTest(unittest.TestCase):
    def test_screen_dim_time_writes_aosp_dim_ratio_fields(self) -> None:
        text = (
            ROOT / "app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt"
        ).read_text(encoding="utf-8")
        start = text.index("fun ScreenDimTimeHook")
        body = text[start:text.index("fun NoOverscrollAppHook")]
        self.assertIn('getInt("system_dimtime"', body)
        self.assertIn("mMaximumScreenDimDurationConfig", body)
        self.assertIn("mMaximumScreenDimRatioConfig", body)
        self.assertNotIn("system_screendimtime", body)
        self.assertNotIn("mScreenOffTimeoutSetting", body)
        self.assertNotIn("setStayOnSettingInternal", body)

    def test_battery_indicator_reads_custom_colors(self) -> None:
        text = (
            ROOT / "app/src/main/java/tv/withaibuild/customiuizer/utils/BatteryIndicator.kt"
        ).read_text(encoding="utf-8")
        self.assertIn('getInt("system_batteryindicator_colorval1"', text)
        self.assertIn('getInt("system_batteryindicator_colorval2"', text)
        self.assertIn('getInt("system_batteryindicator_colorval3"', text)
        self.assertIn('getInt("system_batteryindicator_colorval4"', text)

    def test_r5_xml_proofs_are_acceptable(self) -> None:
        from tools.parity_r5_xml import r5_xml_owner_groups
        from tools.parity_phase_f import proof_is_acceptable

        groups = r5_xml_owner_groups()
        self.assertGreaterEqual(sum(len(m.preference_keys) for m in groups), 80)
        bad = [m.proof_id for m in groups if not proof_is_acceptable(m)]
        self.assertEqual(bad, [])

    def test_r5_reviewed_proofs_are_acceptable(self) -> None:
        from tools.parity_r5_reviews import r5_reviewed_owner_groups
        from tools.parity_phase_f import proof_is_acceptable

        groups = r5_reviewed_owner_groups()
        self.assertGreater(len(groups), 100)
        bad = [
            man.proof_id
            for man in groups
            if man.proof_conclusion in {"PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT"}
            and not proof_is_acceptable(man)
        ]
        self.assertEqual(bad, [])


if __name__ == "__main__":
    unittest.main()
