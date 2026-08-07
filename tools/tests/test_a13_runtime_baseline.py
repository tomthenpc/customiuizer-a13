#!/usr/bin/env python3
"""Strong tests for tools/a13_runtime_baseline.py (R2).

Tests call actual runner/aggregation functions with mocked `_adb_shell` and
`time.sleep`, avoiding real device and delays.
"""
from __future__ import annotations

import json
import os
import re
import statistics
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import MagicMock, patch, call

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent))

import a13_runtime_baseline as rtb
import a13_perf_probe as probe

FIXTURES = Path(__file__).resolve().parent / "fixtures"


def _fixture(name: str) -> str:
    return (FIXTURES / name).read_text(encoding="utf-8", errors="replace")


MEMINFO = _fixture("dumpsys_meminfo.txt")
PROC_STATUS = _fixture("proc_status.txt")
PROC_STAT = "100 (process) S 1 100 100 0 -1 1077952832 100 0 0 0 5000 3000 0 0 20 0 1 0 100 0"


def _make_default_adb_response(command: str) -> rtb.AdbResult:
    """Return plausible fake output for any adb shell command."""
    if command == "pidof system_server":
        return rtb.AdbResult(command, 0, "100\n", "")
    if command == "pidof com.android.systemui":
        return rtb.AdbResult(command, 0, "200\n", "")
    if command == "getconf CLK_TCK":
        return rtb.AdbResult(command, 0, "100\n", "")
    if command == "cmd notification":
        return rtb.AdbResult(command, 0, "post\nlist\nremove", "")
    if command == "cmd notification post --help":
        return rtb.AdbResult(command, 0, "post", "")
    if "dumpsys meminfo" in command:
        return rtb.AdbResult(command, 0, MEMINFO, "")
    if "/proc/100/stat" in command or "/proc/200/stat" in command:
        return rtb.AdbResult(command, 0, PROC_STAT, "")
    if "/proc/100/status" in command or "/proc/200/status" in command:
        return rtb.AdbResult(command, 0, PROC_STATUS, "")
    if "cmd notification list" in command:
        tags = [f"customiuizer_a13_baseline_20260807T140000_r{rep}_i{i}" for rep in range(5) for i in range(10)]
        return rtb.AdbResult(command, 0, "\n".join(tags) + "\n", "")
    if "dumpsys notification" in command:
        tags = [f"customiuizer_a13_baseline_20260807T140000_r{rep}_i{i}" for rep in range(5) for i in range(10)]
        return rtb.AdbResult(command, 0, "\n".join(tags), "")
    if "uiautomator dump" in command:
        return rtb.AdbResult(command, 0, "", "")
    if "cat /sdcard/window_dump.xml" in command:
        return rtb.AdbResult(command, 0, "<hierarchy></hierarchy>", "")
    if "cmd notification post" in command:
        return rtb.AdbResult(command, 0, "", "")
    if command == "getprop sys.boot_completed":
        return rtb.AdbResult(command, 0, "1\n", "")
    if command == "cat /proc/sys/kernel/random/boot_id":
        return rtb.AdbResult(command, 0, "abcd-1234\n", "")
    if command == "cat /proc/uptime":
        return rtb.AdbResult(command, 0, "350.5 0.0\n", "")
    if "input swipe" in command or "input keyevent" in command or command == "input --help":
        return rtb.AdbResult(command, 0, "", "")
    if "wm size" in command:
        return rtb.AdbResult(command, 0, "Physical size: 1080x2400\n", "")
    if "wm density" in command:
        return rtb.AdbResult(command, 0, "Physical density: 440\n", "")
    if "dumpsys input" in command:
        return rtb.AdbResult(command, 0, "Orientation: 0", "")
    return rtb.AdbResult(command, 0, "", "")


# -----------------------------------------------------------------------------
# Helpers for patching _adb_shell across a whole test
# -----------------------------------------------------------------------------

def _patch_adb_shell(test_case, responses=None):
    """Patch _adb_shell to a map of commands or default."""
    default_map = {}

    def make_side_effect(responses):
        def side_effect(adb, command, timeout=30):
            if responses and command in responses:
                return responses[command]
            return _make_default_adb_response(command)
        return side_effect

    patcher = patch("a13_runtime_baseline._adb_shell", side_effect=make_side_effect(responses))
    test_case.addCleanup(patcher.stop)
    return patcher.start()


# -----------------------------------------------------------------------------
# Clock tick provenance (mutation-resistant)
# -----------------------------------------------------------------------------

class ClockTickProvenanceTest(unittest.TestCase):

    def test_getconf_success_100hz(self):
        with patch("a13_runtime_baseline._adb_shell") as mock_shell:
            mock_shell.return_value = rtb.AdbResult("getconf CLK_TCK", 0, "100\n", "")
            info = rtb._resolve_clock_tick_with_provenance(MagicMock())
            self.assertEqual(info.value, 100)
            self.assertEqual(info.source, "DEVICE_GETCONF")

    def test_getconf_success_250hz(self):
        with patch("a13_runtime_baseline._adb_shell") as mock_shell:
            mock_shell.return_value = rtb.AdbResult("getconf CLK_TCK", 0, "250\n", "")
            info = rtb._resolve_clock_tick_with_provenance(MagicMock())
            self.assertEqual(info.value, 250)

    def test_getconf_failure_no_silent_fallback(self):
        # Mutation C: if getconf fails, code must NOT silently return 100
        with patch("a13_runtime_baseline._adb_shell") as mock_shell:
            mock_shell.return_value = rtb.AdbResult("getconf CLK_TCK", 1, "", "not found")
            info = rtb._resolve_clock_tick_with_provenance(MagicMock())
            self.assertIsNone(info.value)
            self.assertEqual(info.source, "UNAVAILABLE")
            self.assertIn("not found", info.raw_output)


# -----------------------------------------------------------------------------
# Notification scenario ordering and uniqueness
# -----------------------------------------------------------------------------

class NotificationOrderingTest(unittest.TestCase):

    def test_verify_after_post(self):
        # Mutation A: if verify is moved before post, the log must catch it
        with patch("a13_runtime_baseline._adb_shell") as mock_shell:
            log = []

            def side_effect(adb, command, timeout=30):
                log.append(command)
                if "cmd notification post" in command:
                    return rtb.AdbResult(command, 0, "", "")
                if "cmd notification list" in command:
                    return rtb.AdbResult(command, 0, "customiuizer_a13_baseline_20260807T140000_r0_i0\n", "")
                return _make_default_adb_response(command)

            mock_shell.side_effect = side_effect
            display = rtb.DisplayInfo(width=1080, height=2400, coordinate_confidence="1080P_PORTRAIT_ASSUMED")
            steps, _ = rtb._build_notification_menu_steps("20260807T140000", 0, display)
            cap = rtb.NotificationCapability(cmd_notification_available=True, post_available=True, list_available=True)

            # simulate one post + verify
            tag = "customiuizer_a13_baseline_20260807T140000_r0_i0"
            r = mock_shell(MagicMock(), steps[0].commands[0])
            self.assertTrue(r.ok)
            found, method = rtb._verify_notification_exists(MagicMock(), tag, cap)
            self.assertTrue(found)

            post_idx = log.index([c for c in log if "cmd notification post" in c][0])
            verify_idx = log.index([c for c in log if "cmd notification list" in c][0])
            self.assertLess(post_idx, verify_idx)

    def test_unique_tags_per_iteration(self):
        display = rtb.DisplayInfo(width=1080, height=2400)
        for rep in range(2):
            _, plan = rtb._build_notification_menu_steps("20260807T140000", rep, display)
            tags = [p["tag"] for p in plan]
            self.assertEqual(len(tags), 10)
            self.assertEqual(len(tags), len(set(tags)))
            for p in plan:
                self.assertIn(f"_r{rep}_", p["tag"])

    def test_unique_tags_across_repetitions(self):
        display = rtb.DisplayInfo(width=1080, height=2400)
        all_tags = []
        for rep in range(2):
            _, plan = rtb._build_notification_menu_steps("20260807T140000", rep, display)
            all_tags.extend([p["tag"] for p in plan])
        self.assertEqual(len(all_tags), 20)
        self.assertEqual(len(all_tags), len(set(all_tags)))

    def test_cleanup_before_close_shade(self):
        display = rtb.DisplayInfo(width=1080, height=2400)
        steps, _ = rtb._build_notification_menu_steps("20260807T140000", 0, display)
        descs = [s.description for s in steps]
        cleanup_idx = next(i for i, d in enumerate(descs) if "Swipe-dismiss" in d)
        close_idx = next(i for i, d in enumerate(descs) if "Swipe up to close panel" in d)
        self.assertLess(cleanup_idx, close_idx)

    def test_menu_close_uses_back_key(self):
        display = rtb.DisplayInfo(width=1080, height=2400)
        steps, _ = rtb._build_notification_menu_steps("20260807T140000", 0, display)
        close_steps = [s for s in steps if "Close notification menu" in s.description]
        self.assertTrue(close_steps)
        for s in close_steps:
            self.assertIn("input keyevent 4", s.commands)

    def test_no_tap_400_for_menu_close(self):
        display = rtb.DisplayInfo(width=1080, height=2400)
        steps, _ = rtb._build_notification_menu_steps("20260807T140000", 0, display)
        for s in steps:
            for c in s.commands:
                self.assertNotIn("input tap 540 400", c)

    def test_no_cmd_notification_remove_anywhere(self):
        import inspect
        source = inspect.getsource(rtb)
        self.assertNotIn("cmd notification remove", source)


# -----------------------------------------------------------------------------
# PID pair validity and aggregation
# -----------------------------------------------------------------------------

class PIDPairValidityTest(unittest.TestCase):

    def _sample(self, pid, pss):
        return {"sample_index": 1, "process": "system_server", "repeat_index": 0, "pid": pid, "metrics": {"total_pss_kb": pss}}

    def test_pair_valid_same_pid(self):
        pairs = rtb._build_valid_pairs([self._sample(100, 1000)], [self._sample(100, 1100)])
        self.assertTrue(pairs[0]["pair_valid"])

    def test_pair_invalid_pid_changed(self):
        pairs = rtb._build_valid_pairs([self._sample(100, 1000)], [self._sample(200, 1100)])
        self.assertFalse(pairs[0]["pair_valid"])

    def test_pair_invalid_post_null(self):
        pairs = rtb._build_valid_pairs([self._sample(100, 1000)], [self._sample(None, 1100)])
        self.assertFalse(pairs[0]["pair_valid"])

    def test_pair_invalid_pre_null(self):
        pairs = rtb._build_valid_pairs([self._sample(None, 1000)], [self._sample(100, 1100)])
        self.assertFalse(pairs[0]["pair_valid"])

    def test_pair_invalid_missing_metric(self):
        pre = {"sample_index": 1, "process": "system_server", "repeat_index": 0, "pid": 100, "metrics": {"total_pss_kb": 1000}}
        post = {"sample_index": 1, "process": "system_server", "repeat_index": 0, "pid": 100, "metrics": {"total_pss_kb": None}}
        pairs = rtb._build_valid_pairs([pre], [post])
        self.assertFalse(pairs[0]["pair_valid"])


class AggregationExcludesInvalidPairsTest(unittest.TestCase):

    def _pair(self, valid, pre_pss, post_pss):
        return {
            "pair_valid": valid,
            "pre": {"metrics": {"total_pss_kb": pre_pss}},
            "post": {"metrics": {"total_pss_kb": post_pss}},
        }

    def test_median_uses_only_valid_pairs(self):
        pairs = [self._pair(True, 1000, 1100), self._pair(False, 900, 950), self._pair(True, 1200, 1300)]
        pre_med = rtb._build_median_from_pairs(pairs, {"total_pss_kb"}, "pre")
        post_med = rtb._build_median_from_pairs(pairs, {"total_pss_kb"}, "post")
        self.assertEqual(pre_med["total_pss_kb"]["value"], 1100)
        self.assertEqual(post_med["total_pss_kb"]["value"], 1200)

    def test_failed_repetition_excluded(self):
        pairs = [self._pair(i != 2, 1000 + i * 10, 1100 + i * 10) for i in range(5)]
        pre_med = rtb._build_median_from_pairs(pairs, {"total_pss_kb"}, "pre")
        valid = [1000, 1010, 1030, 1040]
        self.assertEqual(pre_med["total_pss_kb"]["value"], statistics.median(valid))


class DeltaFromValidPairsTest(unittest.TestCase):

    def test_delta_uses_valid_only(self):
        pairs = [
            {"pair_valid": True, "pre": {"metrics": {"total_pss_kb": 1000}}, "post": {"metrics": {"total_pss_kb": 1100}}},
            {"pair_valid": False, "pre": {"metrics": {"total_pss_kb": 500}}, "post": {"metrics": {"total_pss_kb": 600}}},
        ]
        delta = rtb._build_delta_from_pairs(pairs)
        self.assertEqual(delta["total_pss_kb"]["delta"], 100)


# -----------------------------------------------------------------------------
# meminfo fail-visible
# -----------------------------------------------------------------------------

class MeminfoParseFailVisibleTest(unittest.TestCase):

    def test_parse_ok(self):
        result = rtb._parse_meminfo_fail_visible(MEMINFO)
        self.assertEqual(result["_parse_status"], "OK")
        self.assertEqual(result["total_pss_kb"], 9876)

    def test_missing_fields_null_not_zero(self):
        text = "** MEMINFO in pid 1 [a] **\n  Dalvik Heap: 4000 4000\n        TOTAL: 5000 5000\n"
        result = rtb._parse_meminfo_fail_visible(text)
        self.assertIsNone(result["native_heap_kb"])
        self.assertNotEqual(result["native_heap_kb"], 0)

    def test_unparseable_format(self):
        result = rtb._parse_meminfo_fail_visible("weird ROM\nFoo: bar\n")
        self.assertEqual(result["_parse_status"], "PARSE_FAILED")

    def test_empty_output(self):
        result = rtb._parse_meminfo_fail_visible("")
        self.assertEqual(result["_parse_status"], "EMPTY_OUTPUT")


# -----------------------------------------------------------------------------
# CLI exit semantics
# -----------------------------------------------------------------------------

class CLIExitCodeTest(unittest.TestCase):

    @patch("a13_runtime_baseline._run_scenario")
    @patch("a13_runtime_baseline._probe_display")
    @patch("a13_runtime_baseline._probe_notification_capability")
    @patch("a13_runtime_baseline._resolve_clock_tick_with_provenance")
    @patch("a13_runtime_baseline._ensure_single_device")
    def test_scenario_failed_returns_exit_scenario(
        self, mock_ensure, mock_tick, mock_cap, mock_disp, mock_run
    ):
        mock_ensure.return_value = (True, [{"serial": "abc", "state": "device"}])
        mock_tick.return_value = rtb.ClockTickInfo(100, "DEVICE_GETCONF", "100")
        mock_cap.return_value = rtb.NotificationCapability(cmd_notification_available=True, post_available=True)
        mock_disp.return_value = rtb.DisplayInfo()
        mock_run.return_value = (rtb.EXIT_SCENARIO, {"scenario_status": "FAILED"})
        adb = MagicMock(spec=probe.Adb)
        adb.device = "abc"
        code = rtb.cmd_run(adb, ["volume_adjust_10"], "enabled_features_off", 1, Path("/tmp"))
        self.assertEqual(code, rtb.EXIT_SCENARIO)

    @patch("a13_runtime_baseline._ensure_single_device")
    def test_no_device_returns_exit_device(self, mock_ensure):
        mock_ensure.return_value = (False, [])
        adb = MagicMock(spec=probe.Adb)
        code = rtb.cmd_run(adb, ["all"], "enabled_features_off", None, Path("/tmp"))
        self.assertEqual(code, rtb.EXIT_DEVICE)

    @patch("a13_runtime_baseline._ensure_single_device")
    def test_multiple_devices_returns_exit_device(self, mock_ensure):
        mock_ensure.return_value = (False, [{"serial": "a"}, {"serial": "b"}])
        adb = MagicMock(spec=probe.Adb)
        code = rtb.cmd_run(adb, ["all"], "enabled_features_off", None, Path("/tmp"))
        self.assertEqual(code, rtb.EXIT_DEVICE)

    def test_unknown_scenario_exit_cli(self):
        with patch("a13_runtime_baseline._ensure_single_device") as mock_ensure, \
             patch("a13_runtime_baseline._resolve_clock_tick_with_provenance") as mock_tick, \
             patch("a13_runtime_baseline._probe_notification_capability") as mock_cap, \
             patch("a13_runtime_baseline._probe_display") as mock_disp:
            mock_ensure.return_value = (True, [{"serial": "abc", "state": "device"}])
            mock_tick.return_value = rtb.ClockTickInfo(100, "DEVICE_GETCONF", "100")
            mock_cap.return_value = rtb.NotificationCapability(cmd_notification_available=True, post_available=True)
            mock_disp.return_value = rtb.DisplayInfo()
            adb = MagicMock(spec=probe.Adb)
            adb.device = "abc"
            code = rtb.cmd_run(adb, ["nonsense"], "enabled_features_off", None, Path("/tmp"))
            self.assertEqual(code, rtb.EXIT_CLI)


# -----------------------------------------------------------------------------
# End-to-end runner with mocked _adb_shell and time.sleep
# -----------------------------------------------------------------------------

class EndToEndRunnerTest(unittest.TestCase):

    def setUp(self):
        _patch_adb_shell(self)
        self.sleep_patcher = patch("a13_runtime_baseline.time.sleep")
        self.sleep_patcher.start()
        self.addCleanup(self.sleep_patcher.stop)

    def test_runner_creates_manifest(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            adb = MagicMock(spec=probe.Adb)
            adb.device = "abc"
            clock = rtb.ClockTickInfo(100, "DEVICE_GETCONF", "100")
            cap = rtb.NotificationCapability(cmd_notification_available=True, post_available=True)
            display = rtb.DisplayInfo(width=1080, height=2400, coordinate_confidence="1080P_PORTRAIT_ASSUMED")
            device_info = {"device": "test", "rom": "test", "android_version": "13", "build_variant": "user"}

            status, manifest = rtb._run_scenario(
                adb, rtb.SCENARIOS["volume_adjust_10"], "enabled_features_off", 2,
                Path(tmpdir), "20260807T140000", clock, cap, display, device_info, "abc"
            )
            self.assertEqual(status, rtb.EXIT_OK)
            self.assertEqual(manifest["repetitions_completed"], 2)
            self.assertEqual(manifest["repetitions_valid_for_aggregation"], 2)
            self.assertTrue((Path(tmpdir) / "20260807T140000_volume_adjust_10" / "manifest.json").exists())

    def test_unique_tags_in_manifest(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            adb = MagicMock(spec=probe.Adb)
            adb.device = "abc"
            clock = rtb.ClockTickInfo(100, "DEVICE_GETCONF", "100")
            cap = rtb.NotificationCapability(cmd_notification_available=True, post_available=True, list_available=True)
            display = rtb.DisplayInfo(width=1080, height=2400, coordinate_confidence="1080P_PORTRAIT_ASSUMED")
            device_info = {"device": "test", "rom": "test", "android_version": "13", "build_variant": "user"}

            status, manifest = rtb._run_scenario(
                adb, rtb.SCENARIOS["notification_menu_create_delete"], "enabled_features_off", 1,
                Path(tmpdir), "20260807T140000", clock, cap, display, device_info, "abc"
            )
            tags = set()
            for rep in manifest["repetitions"]:
                for p in rep["iteration_plan"]:
                    tags.add(p["tag"])
            self.assertEqual(len(tags), 10)
            self.assertEqual(status, rtb.EXIT_OK)

    def test_pid_change_excludes_from_aggregate(self):
        call_count = [0]

        with patch("a13_runtime_baseline._adb_shell") as mock_shell:
            def side_effect(adb, command, timeout=30):
                if "pidof system_server" in command:
                    call_count[0] += 1
                    if call_count[0] <= 1:
                        return rtb.AdbResult(command, 0, "100\n", "")
                    return rtb.AdbResult(command, 0, "200\n", "")  # PID changes after first
                return _make_default_adb_response(command)

            mock_shell.side_effect = side_effect
            with tempfile.TemporaryDirectory() as tmpdir:
                adb = MagicMock(spec=probe.Adb)
                adb.device = "abc"
                clock = rtb.ClockTickInfo(100, "DEVICE_GETCONF", "100")
                cap = rtb.NotificationCapability(cmd_notification_available=True, post_available=True)
                display = rtb.DisplayInfo(width=1080, height=2400, coordinate_confidence="1080P_PORTRAIT_ASSUMED")
                device_info = {"device": "test", "rom": "test", "android_version": "13", "build_variant": "user"}

                status, manifest = rtb._run_scenario(
                    adb, rtb.SCENARIOS["volume_adjust_10"], "enabled_features_off", 1,
                    Path(tmpdir), "20260807T140000", clock, cap, display, device_info, "abc"
                )
                self.assertEqual(status, rtb.EXIT_SCENARIO)
                self.assertEqual(manifest["repetitions_valid_for_aggregation"], 0)

    def test_notification_post_failure_invalidates_repetition(self):
        with patch("a13_runtime_baseline._adb_shell") as mock_shell:
            def side_effect(adb, command, timeout=30):
                if "cmd notification post" in command:
                    return rtb.AdbResult(command, 1, "", "post failed")
                return _make_default_adb_response(command)

            mock_shell.side_effect = side_effect
            with tempfile.TemporaryDirectory() as tmpdir:
                adb = MagicMock(spec=probe.Adb)
                adb.device = "abc"
                clock = rtb.ClockTickInfo(100, "DEVICE_GETCONF", "100")
                cap = rtb.NotificationCapability(cmd_notification_available=True, post_available=True, list_available=True)
                display = rtb.DisplayInfo(width=1080, height=2400, coordinate_confidence="1080P_PORTRAIT_ASSUMED")
                device_info = {"device": "test", "rom": "test", "android_version": "13", "build_variant": "user"}

                status, manifest = rtb._run_scenario(
                    adb, rtb.SCENARIOS["notification_menu_create_delete"], "enabled_features_off", 1,
                    Path(tmpdir), "20260807T140000", clock, cap, display, device_info, "abc"
                )
                self.assertEqual(status, rtb.EXIT_SCENARIO)
                self.assertEqual(manifest["repetitions_completed"], 1)
                self.assertEqual(manifest["repetitions_valid_for_aggregation"], 0)

    def test_module_state_source_operator_declared(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            adb = MagicMock(spec=probe.Adb)
            adb.device = "abc"
            clock = rtb.ClockTickInfo(100, "DEVICE_GETCONF", "100")
            cap = rtb.NotificationCapability(cmd_notification_available=True, post_available=True)
            display = rtb.DisplayInfo(width=1080, height=2400, coordinate_confidence="1080P_PORTRAIT_ASSUMED")
            device_info = {"device": "test", "rom": "test", "android_version": "13", "build_variant": "user"}

            _, manifest = rtb._run_scenario(
                adb, rtb.SCENARIOS["volume_adjust_10"], "enabled_typical_features", 1,
                Path(tmpdir), "20260807T140000", clock, cap, display, device_info, "abc"
            )
            self.assertEqual(manifest["module_state"], "enabled_typical_features")
            self.assertEqual(manifest["module_state_source"], "OPERATOR_DECLARED")

    def test_boot_stable_records_precondition(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            adb = MagicMock(spec=probe.Adb)
            adb.device = "abc"
            clock = rtb.ClockTickInfo(100, "DEVICE_GETCONF", "100")
            cap = rtb.NotificationCapability()
            display = rtb.DisplayInfo(width=1080, height=2400, coordinate_confidence="1080P_PORTRAIT_ASSUMED")
            device_info = {"device": "test", "rom": "test", "android_version": "13", "build_variant": "user"}

            _, manifest = rtb._run_scenario(
                adb, rtb.SCENARIOS["boot_stable"], "enabled_features_off", 1,
                Path(tmpdir), "20260807T140000", clock, cap, display, device_info, "abc"
            )
            self.assertIsNotNone(manifest["boot_info"])
            self.assertEqual(manifest["boot_info"]["sys_boot_completed"], "1")


# -----------------------------------------------------------------------------
# Multi-device guard
# -----------------------------------------------------------------------------

class MultiDeviceGuardTest(unittest.TestCase):

    def test_single_device_allowed(self):
        adb = MagicMock(spec=probe.Adb)
        adb.devices.return_value = [{"serial": "abc", "state": "device"}]
        adb.device = "abc"
        ok, _ = rtb._ensure_single_device(adb)
        self.assertTrue(ok)

    def test_multiple_devices_blocked(self):
        adb = MagicMock(spec=probe.Adb)
        adb.devices.return_value = [
            {"serial": "a", "state": "device"},
            {"serial": "b", "state": "device"},
        ]
        adb.device = None
        ok, _ = rtb._ensure_single_device(adb)
        self.assertFalse(ok)

    def test_specified_device_allows_multiple(self):
        adb = MagicMock(spec=probe.Adb)
        adb.devices.return_value = [
            {"serial": "a", "state": "device"},
            {"serial": "b", "state": "device"},
        ]
        adb.device = "a"
        ok, _ = rtb._ensure_single_device(adb)
        self.assertTrue(ok)


# -----------------------------------------------------------------------------
# Coordinate adaptation
# -----------------------------------------------------------------------------

class CoordinateAdaptationTest(unittest.TestCase):

    def test_swipe_coords_1080p(self):
        display = rtb.DisplayInfo(width=1080, height=2400)
        x1, _, _, y2 = rtb._swipe_coords(display, from_top=True)
        self.assertEqual(x1, 540)
        self.assertEqual(y2, 1608)

    def test_long_press_1080p(self):
        display = rtb.DisplayInfo(width=1080, height=2400)
        cmd = rtb._make_long_press(display)
        self.assertIn("540", cmd)
        self.assertIn("600", cmd)  # 2400 * 0.25


# -----------------------------------------------------------------------------
# Doctor
# -----------------------------------------------------------------------------

class DoctorTests(unittest.TestCase):

    def test_doctor_device_not_connected(self):
        adb = MagicMock(spec=probe.Adb)
        adb.adb = "adb"
        adb.version.return_value = "1.0.41"
        adb.devices.return_value = []
        with patch("a13_runtime_baseline._print"):
            code = rtb.cmd_doctor(adb)
        self.assertEqual(code, rtb.EXIT_DEVICE)

    def test_doctor_no_keycode_menu(self):
        import inspect
        source = inspect.getsource(rtb.cmd_doctor)
        self.assertNotIn("input keyevent 82", source)


# -----------------------------------------------------------------------------
# Scenario status codes
# -----------------------------------------------------------------------------

class ScenarioStatusCodeTest(unittest.TestCase):

    def test_codes(self):
        self.assertEqual(rtb.STATUS_OK, "OK")
        self.assertEqual(rtb.STATUS_FAILED_PRECONDITION, "FAILED_PRECONDITION")
        self.assertEqual(rtb.STATUS_FAILED, "FAILED")
        self.assertEqual(rtb.EXIT_OK, 0)
        self.assertEqual(rtb.EXIT_DEVICE, 1)
        self.assertEqual(rtb.EXIT_CLI, 2)
        self.assertEqual(rtb.EXIT_SCENARIO, 3)


# -----------------------------------------------------------------------------
# Artifact storage
# -----------------------------------------------------------------------------

class ArtifactStorageTest(unittest.TestCase):

    def test_run_dir_structure(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            ap = rtb.ArtifactPaths.for_run(Path(tmpdir), "RID", "volume_adjust_10")
            ap.mkdirs()
            self.assertTrue(ap.run_dir.exists())
            self.assertTrue(ap.raw_dir.exists())
            self.assertTrue(ap.uiautomator_dir.exists())

    def test_raw_filename(self):
        self.assertEqual(rtb._raw_filename(5, "com.android.systemui", "proc_stat", "POST"),
                         "005_POST_systemui_proc_stat.txt")


if __name__ == "__main__":
    unittest.main()
