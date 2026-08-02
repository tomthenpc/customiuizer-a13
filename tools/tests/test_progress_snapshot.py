#!/usr/bin/env python3
"""Tests for tools/progress_snapshot.py."""
from __future__ import annotations

import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

# Import the module under test with a configurable repo root.
import tools.progress_snapshot as ps


class ProgressSnapshotTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tmpdir = Path(tempfile.mkdtemp())
        self._orig_root = ps.REPO_ROOT
        ps.REPO_ROOT = self.tmpdir
        ps.SMART_FILE = self.tmpdir / "SMART_OPERATION_STATE.md"
        ps.TASK_FILE = self.tmpdir / "TASK_STATE.md"
        ps.JSON_FILE = self.tmpdir / "docs" / "progress" / "A13_PROGRESS_CURRENT.json"
        ps.MD_FILE = self.tmpdir / "docs" / "progress" / "A13_PROGRESS_CURRENT.md"

    def tearDown(self) -> None:
        ps.REPO_ROOT = self._orig_root
        ps.SMART_FILE = self._orig_root / "SMART_OPERATION_STATE.md"
        ps.TASK_FILE = self._orig_root / "TASK_STATE.md"
        ps.JSON_FILE = self._orig_root / "docs" / "progress" / "A13_PROGRESS_CURRENT.json"
        ps.MD_FILE = self._orig_root / "docs" / "progress" / "A13_PROGRESS_CURRENT.md"

    def _write_files(self, smart: str, task: str) -> None:
        ps.SMART_FILE.write_text(smart, encoding="utf-8")
        ps.TASK_FILE.write_text(task, encoding="utf-8")

    def test_parse_task_state_major_sections(self) -> None:
        task = """# P0 — baseline

State: `COMPLETE`

## P0.1 sub

State: `COMPLETE`

# P1 — arch

State: `COMPLETE`

Verification text.

# P2 — registry

State: `IN_PROGRESS`
"""
        smart = """# Smart\n\n```text\nCheckpointCount: 1\n```\n"""
        self._write_files(smart, task)
        sections = ps.parse_task_state()
        ids = [s.id for s in sections]
        self.assertIn("P0", ids)
        self.assertIn("P1", ids)
        self.assertIn("P2", ids)
        self.assertNotIn("P0.1", ids, "subsection should not be counted when major has state")

    def test_aggregate_from_subsections(self) -> None:
        task = """# P0 — baseline

## P0.1 sub

State: `COMPLETE`

## P0.2 sub

State: `TODO`
"""
        smart = """# Smart\n\n```text\nCheckpointCount: 1\n```\n"""
        self._write_files(smart, task)
        sections = ps.parse_task_state()
        p0 = next(s for s in sections if s.id == "P0")
        self.assertEqual("IN_PROGRESS", p0.state)

    def test_machine_progress_excludes_device(self) -> None:
        task = """# P0 — baseline

State: `COMPLETE`

- powershell -NoProfile -ExecutionPolicy Bypass -File .\\scripts\\verify.ps1 -Mode Full

# P13 — device

State: `COMPLETE`

Evidence: `BUILD_VERIFIED`

# P9 — build

State: `TODO`
"""
        smart = """# Smart

```text
CheckpointCount: 1
```
"""
        self._write_files(smart, task)
        snapshot = ps.compute_progress(ps.parse_task_state(), ps.parse_smart())
        self.assertLess(snapshot.machine_progress, snapshot.project_progress)
        self.assertEqual("BUILD_VERIFIED", ps.evidence_for(next(s for s in snapshot.sections if s.id == "P0")))

    def test_progress_is_deterministic_and_written(self) -> None:
        task = """# P0 — baseline

State: `COMPLETE`

# P1 — arch

State: `COMPLETE`

# P3 — hook

State: `IN_PROGRESS`
"""
        smart = """# Smart

```text
CheckpointCount: 1
```
"""
        self._write_files(smart, task)
        os.chdir(self.tmpdir)
        snapshot1 = ps.compute_progress(ps.parse_task_state(), ps.parse_smart())
        ps.write_snapshot(snapshot1)
        json_text = ps.JSON_FILE.read_text(encoding="utf-8")
        data = json.loads(json_text)
        self.assertEqual(snapshot1.project_progress, data["project_progress"])
        self.assertEqual(snapshot1.machine_progress, data["machine_progress"])

    def _baseline_task(self) -> str:
        return """# P0 — baseline

State: `COMPLETE`

# P1 — arch

State: `COMPLETE`

# P3 — hook

State: `IN_PROGRESS`
"""

    def _baseline_smart(self) -> str:
        return """# Smart

```text
CheckpointCount: 5
```
"""

    def test_check_snapshot_ignores_volatile_fields(self) -> None:
        """Simulate a no-op commit: only provenance metadata changes."""
        self._write_files(self._baseline_smart(), self._baseline_task())
        os.chdir(self.tmpdir)
        snapshot = ps.compute_progress(ps.parse_task_state(), ps.parse_smart())
        ps.write_snapshot(snapshot)
        with patch.object(ps, "ahead_of_main", return_value=99), patch.object(ps, "head_sha", return_value="deadbeef" * 5):
            self.assertTrue(ps.check_snapshot())

    def test_check_snapshot_fails_on_task_state_change(self) -> None:
        self._write_files(self._baseline_smart(), self._baseline_task())
        os.chdir(self.tmpdir)
        snapshot = ps.compute_progress(ps.parse_task_state(), ps.parse_smart())
        ps.write_snapshot(snapshot)
        new_task = self._baseline_task().replace("State: `IN_PROGRESS`", "State: `COMPLETE`", 1)
        ps.TASK_FILE.write_text(new_task, encoding="utf-8")
        self.assertFalse(ps.check_snapshot())

    def test_check_snapshot_fails_on_checkpoint_count_change(self) -> None:
        self._write_files(self._baseline_smart(), self._baseline_task())
        os.chdir(self.tmpdir)
        snapshot = ps.compute_progress(ps.parse_task_state(), ps.parse_smart())
        ps.write_snapshot(snapshot)
        ps.SMART_FILE.write_text("""# Smart\n\n```text\nCheckpointCount: 99\n```\n""", encoding="utf-8")
        self.assertFalse(ps.check_snapshot())

    def test_check_snapshot_fails_on_domain_evidence_change(self) -> None:
        self._write_files(self._baseline_smart(), self._baseline_task())
        os.chdir(self.tmpdir)
        snapshot = ps.compute_progress(ps.parse_task_state(), ps.parse_smart())
        ps.write_snapshot(snapshot)
        new_task = self._baseline_task().replace("# P0 — baseline", "# P0 — baseline\n\nEvidence: `CI_VERIFIED`")
        ps.TASK_FILE.write_text(new_task, encoding="utf-8")
        self.assertFalse(ps.check_snapshot())

    def test_markdown_check_snapshot_ignores_volatile_fields(self) -> None:
        self._write_files(self._baseline_smart(), self._baseline_task())
        os.chdir(self.tmpdir)
        snapshot = ps.compute_progress(ps.parse_task_state(), ps.parse_smart())
        ps.write_snapshot(snapshot)
        md = ps.MD_FILE.read_text(encoding="utf-8")
        ps.MD_FILE.write_text(
            md.replace(f"AheadOfMain: {snapshot.ahead_of_main}", "AheadOfMain: 9999")
            .replace(f"HEAD: {snapshot.head}", "HEAD: 0000000000000000000000000000000000000000"),
            encoding="utf-8",
        )
        self.assertTrue(ps.check_snapshot())

    def test_markdown_check_snapshot_fails_on_manual_body_drift(self) -> None:
        self._write_files(self._baseline_smart(), self._baseline_task())
        os.chdir(self.tmpdir)
        snapshot = ps.compute_progress(ps.parse_task_state(), ps.parse_smart())
        ps.write_snapshot(snapshot)
        md = ps.MD_FILE.read_text(encoding="utf-8")
        # Tamper with the section state and the progress percentage.
        ps.MD_FILE.write_text(
            md.replace("`IN_PROGRESS`", "`COMPLETE`", 1)
            .replace(f"ProjectProgress: {snapshot.project_progress}%", "ProjectProgress: 99.99%"),
            encoding="utf-8",
        )
        self.assertFalse(ps.check_snapshot())


if __name__ == "__main__":
    unittest.main()
