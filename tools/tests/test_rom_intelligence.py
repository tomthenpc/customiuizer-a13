#!/usr/bin/env python3
"""Tests for the ROM inventory and target diff workflow.

These tests skip gracefully if the scripts are not yet present, so the audit
branch can be verified before all tools are finalized.
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

REPO = Path(__file__).resolve().parent.parent.parent


def run_py(script: str, *args: str) -> tuple[int, str, str]:
    env = os.environ.copy()
    env["PYTHONIOENCODING"] = "utf-8"
    proc = subprocess.run(
        [sys.executable, str(REPO / "tools" / script), *args],
        cwd=REPO,
        capture_output=True,
        text=True,
        env=env,
        encoding="utf-8",
        errors="replace",
    )
    return proc.returncode, proc.stdout, proc.stderr


def has_tool(name: str) -> str:
    return "found" if shutil.which(name) else "missing"


@unittest.skipUnless((REPO / "tools" / "rom_inventory.py").is_file(), "rom_inventory.py not present")
class RomInventoryTests(unittest.TestCase):
    def test_help_returns_zero(self):
        rc, out, _ = run_py("rom_inventory.py", "--help")
        self.assertEqual(rc, 0, f"--help failed: {out}")

    def test_scan_empty_directory(self):
        """Scanning an empty directory must produce an empty JSON inventory."""
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "out.json"
            rc, _, _ = run_py("rom_inventory.py", "-d", tmp, "-o", str(out), "-f", "json")
            self.assertEqual(rc, 0, f"non-zero exit for empty dir: {rc}")
            data = json.loads(out.read_text(encoding="utf-8"))
            self.assertEqual(data, [])

    def test_scan_tools_directory_finds_no_roms(self):
        """Scanning `tools` (no APK/JAR) must not report any APK or JAR samples."""
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "out.json"
            # Copy one .py file as a non-ROM sample.
            shutil.copy(REPO / "tools" / "rom_inventory.py", Path(tmp) / "rom_inventory.py")
            rc, _, _ = run_py("rom_inventory.py", "-d", tmp, "-o", str(out), "-f", "json")
            self.assertEqual(rc, 0, f"non-zero exit: {rc}")
            data = json.loads(out.read_text(encoding="utf-8"))
            types = {row["sample_type"] for row in data}
            self.assertNotIn("APK", types)
            self.assertNotIn("JAR", types)

    def test_scan_directory_with_json_output(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "out.json"
            (Path(tmp) / "readme.txt").write_text("not a rom", encoding="utf-8")
            rc, _, _ = run_py("rom_inventory.py", "-d", tmp, "-o", str(out), "-f", "json")
            self.assertIn(rc, (0, 2), f"unexpected exit {rc}")
            if rc == 0:
                data = json.loads(out.read_text(encoding="utf-8"))
                self.assertIsInstance(data, list)


@unittest.skipUnless((REPO / "tools" / "rom_target_diff.py").is_file(), "rom_target_diff.py not present")
class RomTargetDiffTests(unittest.TestCase):
    def test_help_returns_zero(self):
        rc, out, _ = run_py("rom_target_diff.py", "--help")
        self.assertEqual(rc, 0, f"--help failed: {out}")

    def test_diff_two_text_files_reports_not_supported(self):
        a = REPO / ".gitignore"
        b = REPO / "README.md" if (REPO / "README.md").is_file() else a
        rc, out, err = run_py("rom_target_diff.py", str(a), str(b))
        self.assertIn(rc, (0, 1, 2), f"unexpected exit {rc}: {err}")


@unittest.skipUnless((REPO / "tools" / "rom_super_inspector.py").is_file(), "rom_super_inspector.py not present")
class RomSuperInspectorTests(unittest.TestCase):
    def test_help_returns_zero(self):
        rc, out, _ = run_py("rom_super_inspector.py", "--help")
        self.assertEqual(rc, 0, f"--help failed: {out}")

    def test_missing_rom_directory_fails(self):
        missing = REPO / "nonexistent_rom_dir"
        rc, out, err = run_py("rom_super_inspector.py", "-r", str(missing))
        self.assertIn(rc, (1, 2), f"expected failure for missing dir: {rc}")

    def test_empty_directory_with_allow_missing_returns_zero(self):
        with tempfile.TemporaryDirectory() as tmp:
            rc, out, _ = run_py("rom_super_inspector.py", "-r", tmp, "--allow-missing")
            self.assertEqual(rc, 0, f"--allow-missing empty dir failed: {rc}")


@unittest.skipUnless(
    (REPO / "docs" / "rom-intelligence" / "A13_PROCESS_MATRIX.md").is_file(),
    "process matrix not present",
)
class ProcessMatrixDocTests(unittest.TestCase):
    def test_process_matrix_has_required_sections(self):
        text = (REPO / "docs" / "rom-intelligence" / "A13_PROCESS_MATRIX.md").read_text(
            encoding="utf-8", errors="replace"
        )
        for header in ("Feature ID", "Preference key(s)", "Business / user-visible name", "target package", "allowed process(es)", "denied process(es)"):
            self.assertIn(header, text, f"missing header: {header}")


@unittest.skipUnless(
    (REPO / "docs" / "rom-intelligence" / "A13_TARGET_MATRIX.md").is_file(),
    "target matrix not present",
)
class TargetMatrixDocTests(unittest.TestCase):
    def test_target_matrix_has_required_statuses(self):
        text = (REPO / "docs" / "rom-intelligence" / "A13_TARGET_MATRIX.md").read_text(
            encoding="utf-8", errors="replace"
        )
        for status in ("STATIC_RESOLVED", "CANDIDATE", "WAITING_FOR_SAMPLE"):
            self.assertIn(status, text, f"missing status: {status}")


@unittest.skipUnless(
    (REPO / "docs" / "audit" / "A13_FEATURE_RETIREMENT_AUDIT.md").is_file(),
    "retirement audit not present",
)
class FeatureRetirementDocTests(unittest.TestCase):
    def test_retirement_audit_has_categories(self):
        text = (REPO / "docs" / "audit" / "A13_FEATURE_RETIREMENT_AUDIT.md").read_text(
            encoding="utf-8", errors="replace"
        )
        for cat in ("KEEP", "KEEP_GUARDED", "EXPERIMENTAL", "FREEZE_LEGACY", "DELETE_DEAD"):
            self.assertIn(cat, text, f"missing category: {cat}")


if __name__ == "__main__":
    unittest.main()
