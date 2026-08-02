import textwrap
import tempfile
import unittest
from pathlib import Path

from tools import check_automation_state as checker


class SmartStateTests(unittest.TestCase):
    def test_parse_detects_duplicate_keys(self):
        text = textwrap.dedent(
            """
            ```text
            Mode: A
            LastFailureClass: one
            LastFailureClass: two
            ```
            """
        )
        block = checker.parse_smart_state(text)
        _, errors = checker.smart_state_dict(block)
        self.assertIn("SMART_OPERATION_STATE duplicate key: LastFailureClass", errors)

    def test_unknown_key_reported(self):
        text = textwrap.dedent(
            """
            ```text
            Mode: A
            UnknownKey: value
            ```
            """
        )
        block = checker.parse_smart_state(text)
        _, errors = checker.smart_state_dict(block)
        self.assertIn("SMART_OPERATION_STATE unknown key: UnknownKey", errors)

    def test_missing_text_block(self):
        text = "No fenced block here."
        with self.assertRaises(ValueError):
            checker.parse_smart_state(text)


class TaskStateTests(unittest.TestCase):
    def test_parent_complete_with_incomplete_child(self):
        text = """
# P5 — Gesture/Control Center

State: `COMPLETE`

## P5.1 生产状态机

State: `TODO`

## P5.2 事件模型

State: `COMPLETE`
"""
        sections = checker.parse_task_sections(text)
        errors = checker.build_parent_child(sections)
        self.assertTrue(any("P5 is COMPLETE but child P5.1 is TODO" in e for e in errors))

    def test_todo_parent_with_complete_child(self):
        text = """
# P5 — Gesture/Control Center

State: `TODO`

## P5.1 生产状态机

State: `COMPLETE`
"""
        sections = checker.parse_task_sections(text)
        errors = checker.build_parent_child(sections)
        self.assertTrue(any("P5 is TODO but has COMPLETE children" in e for e in errors))

    def test_stale_complete_evidence(self):
        text = """
## 4. 发现的问题队列

| ID | Priority | Area | State | Evidence | Acceptance |
|---|---|---|---|---|---|---|
| GESTURE-001 | P1 | Gesture | COMPLETE | 尚未由本地 Agent 盘点 | P5 完成 |
"""
        errors = checker.check_issue_queue(text)
        self.assertIn(
            "TASK_STATE issue GESTURE-001 is COMPLETE but evidence is stale: 尚未由本地 Agent 盘点",
            errors,
        )

    def test_todo_with_complete_acceptance(self):
        text = """
# P5 — Gesture/Control Center

State: `COMPLETE`

## 4. 发现的问题队列

| ID | Priority | Area | State | Evidence | Acceptance |
|---|---|---|---|---|---|---|
| GESTURE-001 | P1 | Gesture | TODO | 多状态机需盘点 | P5 完成 |
"""
        errors = checker.check_issue_queue(text)
        self.assertIn(
            "TASK_STATE issue GESTURE-001 is TODO but acceptance implies complete: P5 完成",
            errors,
        )

    def test_todo_acceptance_references_incomplete_parent(self):
        text = """
# P5 — Gesture/Control Center

State: `TODO`

## 4. 发现的问题队列

| ID | Priority | Area | State | Evidence | Acceptance |
|---|---|---|---|---|---|---|
| GESTURE-001 | P1 | Gesture | TODO | 多状态机需盘点 | P5 完成 |
"""
        errors = checker.check_issue_queue(text)
        self.assertEqual(errors, [])

    def test_empty_checkpoint(self):
        text = """
## 5. Checkpoint

尚无。

---

## 6. 最终报告
"""
        errors = checker.check_checkpoint_section(text)
        self.assertTrue(any("Checkpoint section is empty" in e for e in errors))


class StopConflictTests(unittest.TestCase):
    def test_detects_stop_and_wait(self):
        texts = {
            "GOAL.md": "达到 PROJECT_COMPLETE 后，... 停止并等待仓库所有者。",
            "AGENTS.md": "",
            "SMART_CONTINUOUS_OPERATION.md": "",
        }
        errors = checker.check_stop_conflicts(texts)
        self.assertIn("GOAL.md still contains '停止...等待仓库所有者' post-completion action", errors)


class FixtureRegressionTests(unittest.TestCase):
    def test_current_audit_finding_reproduced_then_fixed(self):
        """Simulate the state captured in CURRENT_AUDIT_FINDINGS.md."""
        text = textwrap.dedent(
            """
            # Smart operation state

            ```text
            Mode: SMART_CONTINUOUS_OPERATION
            CheckpointCount: 3
            CheckpointsSinceStandardSweep: 0
            CheckpointsSinceDeepSweep: 3
            LastLightSweepCommit: pending
            LastStandardSweepCommit: pending
            LastDeepSweepCommit: pending
            LastFullVerificationCommit: pending
            LastStandardSweepCommit: pending
            LastCIState: pending
            LastCleanupCommit: pending
            LastToolCreated: none
            LastFailureClass: none
            ResumeTask: derive from TASK_STATE.md
            ```
            """
        )
        block = checker.parse_smart_state(text)
        _, errors = checker.smart_state_dict(block)
        self.assertIn("SMART_OPERATION_STATE duplicate key: LastStandardSweepCommit", errors)


class ControlPlaneInvariantTests(unittest.TestCase):
    def _make_repo(
        self,
        smart_text: str,
        safe_skill_text: str,
        review_skill_text: str,
        devin_text: str,
    ) -> Path:
        tmp = Path(tempfile.mkdtemp())
        (tmp / ".agents" / "skills" / "a13-safe-implementation").mkdir(parents=True)
        (tmp / ".agents" / "skills" / "a13-independent-review").mkdir(parents=True)
        (tmp / ".agents" / "skills" / "a13-safe-implementation" / "SKILL.md").write_text(
            safe_skill_text, encoding="utf-8"
        )
        (tmp / ".agents" / "skills" / "a13-independent-review" / "SKILL.md").write_text(
            review_skill_text, encoding="utf-8"
        )
        (tmp / "AGENTS.md").write_text(
            "一个 Task Slice\n结束当前 Implementer 会话\nR2、R3、R4\na13-independent-review\n",
            encoding="utf-8",
        )
        (tmp / "SMART_CONTINUOUS_OPERATION.md").write_text(smart_text, encoding="utf-8")
        (tmp / "DEVIN_START_PROMPT.md").write_text(devin_text, encoding="utf-8")
        return tmp

    def test_control_plane_passes(self):
        smart = textwrap.dedent(
            """
            SessionMode: ATOMIC_TASK_SLICE
            IndependentReviewRequired: R2_R3_R4
            AutoResumeWithinSlice: true
            AutoStartNextSlice: false
            ProjectContinuity: MULTI_SESSION
            ContextHandoffThreshold: 70_PERCENT
            完成 Task Slice、qualifying checkpoint、exact CI 检查和 handoff 后，
            结束当前会话是成功边界，不是项目停止。
            """
        )
        safe = textwrap.dedent(
            """
            ---
            name: a13-safe-implementation
            description: Implement exactly one approved atomic slice in tomthenpc/customiuizer-a13.
            argument-hint: <task-slice-path>
            triggers: ["user"]
            ---
            """
        )
        review = textwrap.dedent(
            """
            ---
            name: a13-independent-review
            description: Independently red-team one completed A13 atomic slice.
            argument-hint: <base-sha> <head-sha> <task-slice-path>
            triggers: ["user"]
            ---
            """
        )
        devin = textwrap.dedent(
            """
            # A13 Devin Local 启动入口

            @skills:a13-safe-implementation docs/process/tasks/<task-file>.md
            @skills:a13-independent-review <base-sha> <head-sha> docs/process/tasks/<task-file>.md
            """
        )
        repo = self._make_repo(smart, safe, review, devin)
        errors = checker.check_control_plane_invariants(repo)
        self.assertEqual(errors, [], f"Unexpected control-plane errors: {errors}")

    def test_autostart_next_slice_true_fails(self):
        """Mutation: AutoStartNextSlice: false -> true must fail."""
        smart = textwrap.dedent(
            """
            SessionMode: ATOMIC_TASK_SLICE
            AutoStartNextSlice: true
            完成 Task Slice、qualifying checkpoint、exact CI 检查和 handoff 后，
            结束当前会话是成功边界，不是项目停止。
            """
        )
        safe = textwrap.dedent(
            """
            ---
            name: a13-safe-implementation
            triggers: ["user"]
            ---
            """
        )
        review = textwrap.dedent(
            """
            ---
            name: a13-independent-review
            triggers: ["user"]
            ---
            """
        )
        devin = "@skills:a13-safe-implementation\n@skills:a13-independent-review\n"
        repo = self._make_repo(smart, safe, review, devin)
        errors = checker.check_control_plane_invariants(repo)
        self.assertTrue(
            any("AutoStartNextSlice: true" in e for e in errors),
            f"Expected AutoStartNextSlice: true violation, got: {errors}",
        )

    def test_missing_triggers_fails(self):
        """Mutation: delete triggers: [\"user\"] from a skill must fail."""
        smart = textwrap.dedent(
            """
            SessionMode: ATOMIC_TASK_SLICE
            AutoStartNextSlice: false
            完成 Task Slice、qualifying checkpoint、exact CI 检查和 handoff 后，
            结束当前会话是成功边界，不是项目停止。
            """
        )
        safe = textwrap.dedent(
            """
            ---
            name: a13-safe-implementation
            ---
            """
        )
        review = textwrap.dedent(
            """
            ---
            name: a13-independent-review
            triggers: ["user"]
            ---
            """
        )
        devin = "@skills:a13-safe-implementation\n@skills:a13-independent-review\n"
        repo = self._make_repo(smart, safe, review, devin)
        errors = checker.check_control_plane_invariants(repo)
        self.assertTrue(
            any('missing triggers: ["user"]' in e for e in errors),
            f"Expected missing triggers violation, got: {errors}",
        )

    def test_a14_reference_in_skill_fails(self):
        """A14 skill name in an A13 skill file must fail."""
        smart = textwrap.dedent(
            """
            SessionMode: ATOMIC_TASK_SLICE
            AutoStartNextSlice: false
            完成 Task Slice、qualifying checkpoint、exact CI 检查和 handoff 后，
            结束当前会话是成功边界，不是项目停止。
            """
        )
        safe = textwrap.dedent(
            """
            ---
            name: a13-safe-implementation
            triggers: ["user"]
            description: Also applies to a14-safe-implementation.
            ---
            """
        )
        review = textwrap.dedent(
            """
            ---
            name: a13-independent-review
            triggers: ["user"]
            ---
            """
        )
        devin = "@skills:a13-safe-implementation\n@skills:a13-independent-review\n"
        repo = self._make_repo(smart, safe, review, devin)
        errors = checker.check_control_plane_invariants(repo)
        self.assertTrue(
            any("A14 reference" in e for e in errors),
            f"Expected A14 reference violation, got: {errors}",
        )


if __name__ == "__main__":
    unittest.main()
