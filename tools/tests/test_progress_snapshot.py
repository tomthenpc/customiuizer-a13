import json
import shutil
import tempfile
import unittest
from pathlib import Path

from tools import progress_snapshot


class ProgressSnapshotV7Test(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls._orig_out_json = progress_snapshot.OUT_JSON
        cls._orig_out_md = progress_snapshot.OUT_MD
        cls._orig_repo_root = progress_snapshot.REPO_ROOT
        cls._orig_task_state = progress_snapshot.TASK_STATE
        cls._orig_smart_state = progress_snapshot.SMART_STATE

    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.snapshot_json = self.tmp / "A13_PROGRESS_CURRENT.json"
        self.snapshot_md = self.tmp / "A13_PROGRESS_CURRENT.md"
        progress_snapshot.OUT_JSON = self.snapshot_json
        progress_snapshot.OUT_MD = self.snapshot_md
        progress_snapshot.REPO_ROOT = self.tmp
        # Keep reading from the real repo for task/smart state.
        progress_snapshot.TASK_STATE = self._orig_task_state
        progress_snapshot.SMART_STATE = self._orig_smart_state

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)
        progress_snapshot.OUT_JSON = self._orig_out_json
        progress_snapshot.OUT_MD = self._orig_out_md
        progress_snapshot.REPO_ROOT = self._orig_repo_root
        progress_snapshot.TASK_STATE = self._orig_task_state
        progress_snapshot.SMART_STATE = self._orig_smart_state

    def test_no_args_prints_help_and_does_not_write(self):
        code = progress_snapshot.main([])
        self.assertEqual(2, code)
        self.assertFalse(self.snapshot_json.is_file())
        self.assertFalse(self.snapshot_md.is_file())

    def test_print_is_read_only(self):
        code = progress_snapshot.main(["--print"])
        self.assertEqual(0, code)
        self.assertFalse(self.snapshot_json.is_file())
        self.assertFalse(self.snapshot_md.is_file())

    def test_check_is_read_only(self):
        # First write a known snapshot, then run --check against it.
        progress_snapshot.main(["--write"])
        self.assertTrue(self.snapshot_json.is_file())
        before_json = self.snapshot_json.stat().st_mtime
        before_md = self.snapshot_md.stat().st_mtime
        code = progress_snapshot.main(["--check"])
        self.assertEqual(0, code)
        self.assertEqual(before_json, self.snapshot_json.stat().st_mtime)
        self.assertEqual(before_md, self.snapshot_md.stat().st_mtime)

    def test_check_detects_drift(self):
        progress_snapshot.main(["--write"])
        # Corrupt the JSON with a fake state to force semantic drift.
        existing = json.loads(self.snapshot_json.read_text(encoding="utf-8"))
        existing["projectProgress"] = -1.0
        self.snapshot_json.write_text(json.dumps(existing, indent=2), encoding="utf-8")
        code = progress_snapshot.main(["--check"])
        self.assertEqual(1, code)

    def test_parent_child_not_double_counted(self):
        # P5 is a parent with children P5.1-P5.4; P5 itself must not appear as a leaf.
        text = progress_snapshot.TASK_STATE.read_text(encoding="utf-8")
        leaves = progress_snapshot.parse_task_sections(text)
        ids = set(leaves)
        self.assertIn("P5.1", ids)
        self.assertNotIn("P5", ids)

    def test_blocked_external_accounted(self):
        text = progress_snapshot.TASK_STATE.read_text(encoding="utf-8")
        leaves = progress_snapshot.parse_task_sections(text)
        issues = progress_snapshot.parse_issue_table(text)
        items = progress_snapshot.build_capability_items(leaves, issues)
        buckets = {"complete": 0, "in_progress": 0, "not_started": 0, "blocked_internal": 0, "blocked_external": 0}
        for it in items:
            buckets[it.bucket] = buckets.get(it.bucket, 0) + 1
        self.assertEqual(sum(buckets.values()), progress_snapshot.compute_progress(items)["taskCounts"]["total"])
        self.assertGreaterEqual(buckets["blocked_external"], 0)

    def test_machine_progress_excludes_device(self):
        text = progress_snapshot.TASK_STATE.read_text(encoding="utf-8")
        leaves = progress_snapshot.parse_task_sections(text)
        issues = progress_snapshot.parse_issue_table(text)
        items = progress_snapshot.build_capability_items(leaves, issues)
        progress = progress_snapshot.compute_progress(items)
        non_device_total = 95.0
        non_device_earned = sum(it.earned for it in items if it.domain != "Device validation")
        expected = round(non_device_earned / non_device_total * 100, 1)
        self.assertEqual(expected, progress["machineProgressPercent"])

    def test_sum_of_buckets_equals_total(self):
        text = progress_snapshot.TASK_STATE.read_text(encoding="utf-8")
        leaves = progress_snapshot.parse_task_sections(text)
        issues = progress_snapshot.parse_issue_table(text)
        items = progress_snapshot.build_capability_items(leaves, issues)
        progress = progress_snapshot.compute_progress(items)
        counts = progress["taskCounts"]
        self.assertEqual(
            counts["total"],
            counts["complete"] + counts["in_progress"] + counts["not_started"] + counts["blocked_internal"] + counts["blocked_external"],
        )


if __name__ == "__main__":
    unittest.main()
