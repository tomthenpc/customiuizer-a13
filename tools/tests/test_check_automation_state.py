#!/usr/bin/env python3
"""Tests for tools/check_automation_state.py"""

import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

CHECKER = Path(__file__).resolve().parents[1] / "check_automation_state.py"


SMART_TEMPLATE = """# Smart operation state

```text
{body}
```
"""

TASK_TEMPLATE = """# TASK_STATE

# P0 — Base

State: `COMPLETE`

# P1 — One source

State: `COMPLETE`

# P2 — Registry

State: `COMPLETE`

## P2.1 Identity

State: `COMPLETE`

## P2.2 Inventory

State: `COMPLETE`

## P2.3 Hook ownership

State: `COMPLETE`

# P3 — Hooks

State: `TODO`

---

## 4. 发现的问题队列

| ID | Priority | Area | State | Evidence | Acceptance |
|---|---|---|---|---|---|
{issues}

---

## 5. Checkpoint

| # | Commit | Task | Verification | State |
|---|---|---|---|---|
{checkpoints}

---
"""

GOAL_TEMPLATE = """# GOAL

达到 `PROJECT_COMPLETE` 后：

- 记录最终证据报告；
- 不创建新分支；
- 不合并 `main`；
- 不 tag/release；
- 进入 CONTINUOUS_MAINTENANCE；
- 继续 evidence-driven maintenance。
"""

AGENTS_TEMPLATE = """# AGENTS

## 18. Professional autonomous stewardship

- `PROJECT_COMPLETE` 是证据里程碑，不是主动停止条件；
- 里程碑后留在当前精确分支进入 `CONTINUOUS_MAINTENANCE`。
"""


def init_git(tmp: Path) -> None:
    subprocess.run(["git", "init", str(tmp)], check=True, capture_output=True)
    subprocess.run(
        ["git", "-C", str(tmp), "config", "user.email", "test@example.com"],
        check=True,
        capture_output=True,
    )
    subprocess.run(
        ["git", "-C", str(tmp), "config", "user.name", "Test"],
        check=True,
        capture_output=True,
    )
    (tmp / "file.txt").write_text("x", encoding="utf-8")
    subprocess.run(["git", "-C", str(tmp), "add", "file.txt"], check=True, capture_output=True)
    subprocess.run(["git", "-C", str(tmp), "commit", "-m", "init"], check=True, capture_output=True)


def run_checker(tmp: Path, extra_env: dict[str, str] | None = None) -> tuple[int, str, str]:
    env = os.environ.copy()
    env["CHECK_AUTOMATION_REPO_ROOT"] = str(tmp)
    if extra_env:
        env.update(extra_env)
    proc = subprocess.run(
        [sys.executable, str(CHECKER)],
        cwd=str(tmp),
        env=env,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    return proc.returncode, proc.stdout or "", proc.stderr or ""


def write_fixtures(tmp: Path, smart_body: str, issues: str, checkpoints: str) -> None:
    (tmp / "SMART_OPERATION_STATE.md").write_text(SMART_TEMPLATE.format(body=smart_body), encoding="utf-8")
    (tmp / "TASK_STATE.md").write_text(TASK_TEMPLATE.format(issues=issues, checkpoints=checkpoints), encoding="utf-8")
    (tmp / "GOAL.md").write_text(GOAL_TEMPLATE, encoding="utf-8")
    (tmp / "AGENTS.md").write_text(AGENTS_TEMPLATE, encoding="utf-8")


VALID_SMART = """Mode: PROFESSIONAL_AUTONOMOUS_STEWARDSHIP
CheckpointCount: 0
CheckpointsSinceStandardSweep: 0
CheckpointsSinceDeepSweep: 0
LastQualifyingCheckpoint: none
LastLightSweepCommit: none
LastStandardSweepCommit: none
LastDeepSweepCommit: none
LastFullVerificationCommit: none
LastCIState: NOT_CONFIGURED
LastCleanupCommit: none
LastToolCreated: none
LastFailureClass: none
ResumeTask: P3 — Hooks
"""

COMMIT_SMART = """Mode: PROFESSIONAL_AUTONOMOUS_STEWARDSHIP
CheckpointCount: 1
CheckpointsSinceStandardSweep: 0
CheckpointsSinceDeepSweep: 0
LastQualifyingCheckpoint: HEAD
LastLightSweepCommit: none
LastStandardSweepCommit: none
LastDeepSweepCommit: none
LastFullVerificationCommit: HEAD
LastCIState: NOT_CONFIGURED
LastCleanupCommit: none
LastToolCreated: none
LastFailureClass: none
ResumeTask: P3 — Hooks
"""

VALID_ISSUES = """| BASELINE-001 | P0 | Git | COMPLETE | done | done |
| VERIFY-001 | P0 | Build | COMPLETE | done | done |
| ARCH-001 | P1 | Feature | COMPLETE | done | done |
| DOC-001 | P2 | Docs | TODO | stale | refresh |
| DEVICE-001 | P1 | Device | BLOCKED_EXTERNAL | none | later |"""

VALID_CHECKPOINTS = """"""

COMMIT_CHECKPOINTS = """| 1 | HEAD | P2 | `verify.ps1 -Mode Full` | qualifying |"""


class TestCheckAutomationState(unittest.TestCase):
    def test_valid_state_passes(self):
        with tempfile.TemporaryDirectory() as td:
            tmp = Path(td)
            write_fixtures(tmp, VALID_SMART, VALID_ISSUES, VALID_CHECKPOINTS)
            code, out, _ = run_checker(tmp)
            self.assertEqual(code, 0, out)
            self.assertIn("A13 automation state OK", out)

    def test_duplicate_smart_key_fails(self):
        body = VALID_SMART + "\nMode: SMART_CONTINUOUS_OPERATION\n"
        with tempfile.TemporaryDirectory() as td:
            tmp = Path(td)
            write_fixtures(tmp, body, VALID_ISSUES, VALID_CHECKPOINTS)
            code, out, _ = run_checker(tmp)
            self.assertNotEqual(code, 0, out)
            self.assertIn("duplicate key", out)

    def test_missing_smart_key_fails(self):
        body = VALID_SMART.replace("LastCIState:", "OldLastCIState:")
        with tempfile.TemporaryDirectory() as td:
            tmp = Path(td)
            write_fixtures(tmp, body, VALID_ISSUES, VALID_CHECKPOINTS)
            code, out, _ = run_checker(tmp)
            self.assertNotEqual(code, 0, out)
            self.assertIn("missing required key", out)

    def test_old_mode_fails(self):
        body = VALID_SMART.replace(
            "Mode: PROFESSIONAL_AUTONOMOUS_STEWARDSHIP",
            "Mode: SMART_CONTINUOUS_OPERATION",
        )
        with tempfile.TemporaryDirectory() as td:
            tmp = Path(td)
            write_fixtures(tmp, body, VALID_ISSUES, VALID_CHECKPOINTS)
            code, out, _ = run_checker(tmp)
            self.assertNotEqual(code, 0, out)
            self.assertIn("PROFESSIONAL_AUTONOMOUS_STEWARDSHIP", out)

    def test_false_sweep_pending_fails(self):
        body = VALID_SMART.replace("LastFullVerificationCommit: none", "LastFullVerificationCommit: pending")
        with tempfile.TemporaryDirectory() as td:
            tmp = Path(td)
            write_fixtures(tmp, body, VALID_ISSUES, VALID_CHECKPOINTS)
            code, out, _ = run_checker(tmp)
            self.assertNotEqual(code, 0, out)
            self.assertIn("false", out.lower())

    def test_invalid_ci_state_fails(self):
        body = VALID_SMART.replace("LastCIState: NOT_CONFIGURED", "LastCIState: pending")
        with tempfile.TemporaryDirectory() as td:
            tmp = Path(td)
            write_fixtures(tmp, body, VALID_ISSUES, VALID_CHECKPOINTS)
            code, out, _ = run_checker(tmp)
            self.assertNotEqual(code, 0, out)
            self.assertIn("LastCIState", out)

    def test_resume_task_undetermined_fails(self):
        body = VALID_SMART.replace("ResumeTask: P3 — Hooks", "ResumeTask: derive from TASK_STATE.md")
        with tempfile.TemporaryDirectory() as td:
            tmp = Path(td)
            write_fixtures(tmp, body, VALID_ISSUES, VALID_CHECKPOINTS)
            code, out, _ = run_checker(tmp)
            self.assertNotEqual(code, 0, out)
            self.assertIn("ResumeTask", out)

    def test_stale_todo_issue_fails(self):
        issues = VALID_ISSUES.replace("| BASELINE-001 | P0 | Git | COMPLETE", "| BASELINE-001 | P0 | Git | TODO")
        with tempfile.TemporaryDirectory() as td:
            tmp = Path(td)
            write_fixtures(tmp, VALID_SMART, issues, VALID_CHECKPOINTS)
            code, out, _ = run_checker(tmp)
            self.assertNotEqual(code, 0, out)
            self.assertIn("BASELINE-001", out)

    def test_parent_complete_child_in_progress_fails(self):
        task = TASK_TEMPLATE.format(issues=VALID_ISSUES, checkpoints=VALID_CHECKPOINTS).replace(
            "# P0 — Base\n\nState: `COMPLETE`",
            "# P0 — Base\n\nState: `COMPLETE`\n\n## P0.1 Git\n\nState: `IN_PROGRESS`",
        )
        with tempfile.TemporaryDirectory() as td:
            tmp = Path(td)
            (tmp / "SMART_OPERATION_STATE.md").write_text(SMART_TEMPLATE.format(body=VALID_SMART), encoding="utf-8")
            (tmp / "TASK_STATE.md").write_text(task, encoding="utf-8")
            (tmp / "GOAL.md").write_text(GOAL_TEMPLATE, encoding="utf-8")
            (tmp / "AGENTS.md").write_text(AGENTS_TEMPLATE, encoding="utf-8")
            code, out, _ = run_checker(tmp)
            self.assertNotEqual(code, 0, out)
            self.assertIn("P0", out)
            self.assertIn("P0.1", out)

    def test_checkpoint_count_matches_commits(self):
        with tempfile.TemporaryDirectory() as td:
            tmp = Path(td)
            init_git(tmp)
            write_fixtures(tmp, COMMIT_SMART, VALID_ISSUES, COMMIT_CHECKPOINTS)
            code, out, _ = run_checker(tmp)
            self.assertEqual(code, 0, out)
            self.assertIn("A13 automation state OK", out)

    def test_checkpoint_count_mismatch_fails(self):
        with tempfile.TemporaryDirectory() as td:
            tmp = Path(td)
            init_git(tmp)
            write_fixtures(tmp, COMMIT_SMART, VALID_ISSUES, VALID_CHECKPOINTS)
            code, out, _ = run_checker(tmp)
            self.assertNotEqual(code, 0, out)
            self.assertIn("mismatch", out.lower())

    def test_stop_phrase_in_goal_fails(self):
        with tempfile.TemporaryDirectory() as td:
            tmp = Path(td)
            write_fixtures(tmp, VALID_SMART, VALID_ISSUES, VALID_CHECKPOINTS)
            (tmp / "GOAL.md").write_text("Agent 停止并等待仓库所有者。", encoding="utf-8")
            code, out, _ = run_checker(tmp)
            self.assertNotEqual(code, 0, out)
            self.assertIn("GOAL.md", out)


if __name__ == "__main__":
    unittest.main()
