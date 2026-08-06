#!/usr/bin/env python3
"""Tests for tools/a13_perf_probe.py.

All tests use synthetic fixtures and mock subprocess; they do not require a
real Android device or ADB.
"""
from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import a13_perf_probe as probe


REPO_ROOT = Path(__file__).resolve().parent.parent.parent
FIXTURES = Path(__file__).resolve().parent / "fixtures"


def _fixture(name: str) -> str:
    return (FIXTURES / name).read_text(encoding="utf-8", errors="replace")


def _completed(args, returncode: int = 0, stdout: str = "", stderr: str = "") -> subprocess.CompletedProcess:
    return subprocess.CompletedProcess(args=args, returncode=returncode, stdout=stdout, stderr=stderr)


class ParseMeminfoTest(unittest.TestCase):
    def test_parse_meminfo_fixture(self):
        text = _fixture("dumpsys_meminfo.txt")
        result = probe.parse_meminfo(text)
        self.assertEqual(result["total_pss_kb"], 9876)
        self.assertEqual(result["java_heap_kb"], 5678)
        self.assertEqual(result["native_heap_kb"], 1234)
        self.assertEqual(result["graphics_kb"], 256)
        self.assertEqual(result["private_dirty_kb"], 8000)

    def test_parse_meminfo_missing_native_heap(self):
        text = "** MEMINFO in pid 1 [a] **\n  Dalvik Heap: 4000 4000\n        TOTAL: 5000 5000\n"
        result = probe.parse_meminfo(text)
        self.assertIsNone(result["native_heap_kb"])
        self.assertEqual(result["java_heap_kb"], 4000)
        self.assertEqual(result["total_pss_kb"], 5000)

    def test_parse_meminfo_crlf(self):
        base = _fixture("dumpsys_meminfo.txt").replace("\n", "\r\n")
        result = probe.parse_meminfo(base)
        self.assertEqual(result["total_pss_kb"], 9876)
        self.assertEqual(result["native_heap_kb"], 1234)

    def test_parse_meminfo_carriage_return_only(self):
        base = _fixture("dumpsys_meminfo.txt").replace("\n", "\r")
        result = probe.parse_meminfo(base)
        self.assertEqual(result["total_pss_kb"], 9876)

    def test_parse_meminfo_empty(self):
        result = probe.parse_meminfo("")
        self.assertTrue(all(v is None for v in result.values()))

    def test_parse_meminfo_graphics_variants(self):
        for keyword in ["Graphics", "GFX", "EGL", "GL"]:
            text = f"{keyword}: 1234 1234\n"
            with self.subTest(keyword=keyword):
                result = probe.parse_meminfo(text)
                self.assertEqual(result["graphics_kb"], 1234)


class ParseProcStatusTest(unittest.TestCase):
    def test_thread_count(self):
        text = _fixture("proc_status.txt")
        result = probe.parse_proc_status(text)
        self.assertEqual(result["thread_count"], 42)
        self.assertIsNone(result["binder_proxy_count"])


class ParseProcStatTest(unittest.TestCase):
    def test_cpu_and_threads(self):
        text = _fixture("proc_stat.txt")
        result = probe.parse_proc_stat(text, clock_tick=100)
        self.assertEqual(result["thread_count"], 42)
        # utime=1234, stime=567 -> (1234+567)*1000/100 = 18010
        self.assertEqual(result["cpu_time_ms"], 18010)

    def test_default_clock_tick(self):
        text = _fixture("proc_stat.txt")
        result = probe.parse_proc_stat(text)
        self.assertEqual(result["cpu_time_ms"], 18010)

    def test_malformed(self):
        result = probe.parse_proc_stat("not a stat line")
        self.assertIsNone(result["cpu_time_ms"])


class ParseAmStartWTest(unittest.TestCase):
    def test_total_time(self):
        text = _fixture("am_start_w.txt")
        result = probe.parse_am_start_w(text)
        self.assertEqual(result["startup_total_time_ms"], 487)

    def test_missing(self):
        result = probe.parse_am_start_w("Status: ok\nComplete\n")
        self.assertIsNone(result["startup_total_time_ms"])


class AdbTest(unittest.TestCase):
    def test_adb_missing_raises(self):
        with patch.object(shutil, "which", return_value=None):
            with self.assertRaises(probe.ProbeError) as cm:
                probe.Adb()
            self.assertEqual(cm.exception.reason, "adb_missing")

    def test_build_cmd_includes_device(self):
        with patch.object(shutil, "which", return_value="adb"):
            adb = probe.Adb(device="K7X7X7X7")
            cmd = adb.build_cmd(["version"])
            self.assertIn("-s", cmd)
            self.assertIn("K7X7X7X7", cmd)

    def test_run_forwards_subprocess_error(self):
        with patch.object(shutil, "which", return_value="adb"):
            with patch.object(probe.subprocess, "run") as mock_run:
                mock_run.return_value = _completed(["adb", "shell", "bad"], returncode=1, stderr="Permission denied")
                adb = probe.Adb()
                with self.assertRaises(probe.ProbeError) as cm:
                    adb.run(["shell", "bad"])
                self.assertEqual(cm.exception.reason, "adb_error")

    def test_run_timeout(self):
        with patch.object(shutil, "which", return_value="adb"):
            with patch.object(probe.subprocess, "run", side_effect=subprocess.TimeoutExpired(cmd=["adb"], timeout=5)):
                adb = probe.Adb()
                with self.assertRaises(probe.ProbeError) as cm:
                    adb.run(["shell", "sleep 10"], timeout=5)
                self.assertEqual(cm.exception.reason, "adb_timeout")


class MedianTest(unittest.TestCase):
    def test_median_odd(self):
        self.assertEqual(probe._median([1, 3, 5, 7, 9]), 5)

    def test_median_even(self):
        self.assertEqual(probe._median([1, 2, 3, 4]), 2.5)

    def test_median_ignores_none(self):
        self.assertEqual(probe._median([1, None, 3, None, 5]), 3)

    def test_median_all_none(self):
        self.assertIsNone(probe._median([None, None]))


class JsonOutputTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="a13_perf_probe_test_"))

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def _make_adb(self, device: str = "K7X7X7X7") -> probe.Adb:
        with patch.object(shutil, "which", return_value="adb"):
            return probe.Adb(device=device)

    def _mock_run(self, cmd: list[str], **kwargs) -> subprocess.CompletedProcess:
        joined = " ".join(cmd)
        if "version" in joined:
            return _completed(cmd, stdout="Android Debug Bridge version 1.0.41")
        if "devices -l" in joined:
            return _completed(cmd, stdout="List of devices attached\nK7X7X7X7 device product:xaga model:Redmi_Note_11T_Pro")
        if "pidof" in joined:
            return _completed(cmd, stdout="12345")
        if "dumpsys meminfo 12345" in joined:
            return _completed(cmd, stdout=_fixture("dumpsys_meminfo.txt"))
        if "/proc/12345/status" in joined:
            return _completed(cmd, stdout=_fixture("proc_status.txt"))
        if "/proc/12345/stat" in joined:
            return _completed(cmd, stdout=_fixture("proc_stat.txt"))
        if "getconf CLK_TCK" in joined:
            return _completed(cmd, stdout="100")
        if "getprop" in joined:
            return _completed(cmd, stdout="xaga")
        if "am start -W" in joined:
            return _completed(cmd, stdout=_fixture("am_start_w.txt"))
        if "am force-stop" in joined:
            return _completed(cmd, stdout="")
        return _completed(cmd, stdout="")

    @patch.object(probe.time, "sleep")
    @patch.object(probe.subprocess, "run")
    def test_process_command_output_structure(self, mock_run, _mock_sleep):
        mock_run.side_effect = self._mock_run
        out = self.tmp / "process.json"
        adb = self._make_adb()
        exit_code = probe.cmd_process(
            adb,
            package="tv.withaibuild.customiuizer.r13",
            scenario="manager_idle",
            module_state="enabled_features_off",
            repeat=2,
            output=out,
            warmup=0,
        )
        self.assertEqual(exit_code, 0)
        data = json.loads(out.read_text(encoding="utf-8"))
        self.assertEqual(len(data), 1)
        record = data[0]
        self.assertEqual(record["scenario"], "manager_idle")
        self.assertEqual(record["repetitions"], 2)
        self.assertEqual(record["aggregation_method"], "median")
        self.assertIn("metrics", record)
        self.assertIn("raw_samples", record)
        self.assertIn("median", record)

        metrics = record["metrics"]
        for key in probe.METRIC_UNITS:
            self.assertIn(key, metrics)
            self.assertIn("value", metrics[key])
            self.assertIn("unavailable_reason", metrics[key])

        self.assertEqual(metrics["total_pss_kb"]["value"], 9876)
        self.assertEqual(metrics["native_heap_kb"]["value"], 1234)
        self.assertEqual(metrics["java_heap_kb"]["value"], 5678)
        self.assertEqual(metrics["thread_count"]["value"], 42)

    @patch.object(probe.time, "sleep")
    @patch.object(probe.subprocess, "run")
    def test_startup_command_output(self, mock_run, _mock_sleep):
        mock_run.side_effect = self._mock_run
        out = self.tmp / "startup.json"
        adb = self._make_adb()
        exit_code = probe.cmd_startup(
            adb,
            package="tv.withaibuild.customiuizer.r13",
            activity="tv.withaibuild.customiuizer.MainActivity",
            repeat=2,
            output=out,
        )
        self.assertEqual(exit_code, 0)
        data = json.loads(out.read_text(encoding="utf-8"))
        self.assertEqual(data[0]["metrics"]["startup_total_time_ms"]["value"], 487)

    @patch.object(probe.time, "sleep")
    @patch.object(probe.subprocess, "run")
    def test_process_not_found(self, mock_run, _mock_sleep):
        def runner(cmd, **kwargs):
            joined = " ".join(cmd)
            if "pidof" in joined:
                return _completed(cmd, stdout="")
            if "ps -A" in joined:
                return _completed(cmd, stdout="USER PID PPID VSZ RSS WCHAN PC NAME\nshell 9000 1 12345 1234 futex 0 S ps")
            return self._mock_run(cmd)

        mock_run.side_effect = runner
        out = self.tmp / "process.json"
        adb = self._make_adb()
        exit_code = probe.cmd_process(
            adb,
            package="tv.withaibuild.customiuizer.r13",
            scenario="manager_idle",
            module_state="enabled_features_off",
            repeat=2,
            output=out,
            warmup=0,
        )
        self.assertEqual(exit_code, 1)
        data = json.loads(out.read_text(encoding="utf-8"))
        self.assertEqual(data[0]["metrics"]["total_pss_kb"]["value"], None)


class MainCommandTest(unittest.TestCase):
    @patch.object(shutil, "which", return_value=None)
    def test_main_no_adb(self, _mock):
        exit_code = probe.main(["doctor"])
        self.assertEqual(exit_code, 1)

    @patch.object(probe.subprocess, "run")
    @patch.object(shutil, "which", return_value="adb")
    def test_main_doctor_success(self, _which, mock_run):
        def runner(cmd, **kwargs):
            joined = " ".join(cmd)
            if "version" in joined:
                return _completed(cmd, stdout="Android Debug Bridge version 1.0.41")
            if "devices -l" in joined:
                return _completed(cmd, stdout="List of devices attached\nK7X7X7X7 device product:xaga")
            if "whoami" in joined:
                return _completed(cmd, stdout="shell\n---\nu:r:shell:s0")
            return _completed(cmd, stdout="")
        mock_run.side_effect = runner
        exit_code = probe.main(["doctor"])
        self.assertEqual(exit_code, 0)

    @patch.object(probe.subprocess, "run")
    @patch.object(shutil, "which", return_value="adb")
    def test_main_doctor_no_devices(self, _which, mock_run):
        def runner(cmd, **kwargs):
            if "devices" in " ".join(cmd):
                return _completed(cmd, stdout="List of devices attached\n")
            return _completed(cmd, stdout="")
        mock_run.side_effect = runner
        exit_code = probe.main(["doctor"])
        self.assertEqual(exit_code, 1)


class NonAsciiAndNewlineTest(unittest.TestCase):
    def test_chinese_output_does_not_crash_parsing(self):
        text = _fixture("dumpsys_meminfo.txt").replace(
            "TOTAL", "总计 TOTAL"
        )
        result = probe.parse_meminfo(text)
        self.assertEqual(result["total_pss_kb"], 9876)

    def test_replacement_character_tolerated(self):
        text = "Native Heap: 1234\nDalvik Heap: \udcff\n"
        result = probe.parse_meminfo(text)
        self.assertEqual(result["native_heap_kb"], 1234)


if __name__ == "__main__":
    unittest.main()
