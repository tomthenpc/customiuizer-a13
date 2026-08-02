#!/usr/bin/env python3
"""A13 control-state invariant checker.

Read-only by default. Non-zero exit on:
- duplicate keys in SMART_OPERATION_STATE.md
- missing required SMART keys
- stale Mode / false CI / false sweep / false checkpoint
- SMART/TASK checkpoint count mismatch
- ResumeTask not matching an active TASK_STATE section
- parent/child state inconsistency in TASK_STATE.md
- forbidden stop/wait language in GOAL.md / AGENTS.md
- issue queue with TODO for already-completed baseline/verify/arch
"""

import os
import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(os.environ.get("CHECK_AUTOMATION_REPO_ROOT", Path(__file__).resolve().parents[1]))

SMART_FILE = REPO_ROOT / "SMART_OPERATION_STATE.md"
TASK_FILE = REPO_ROOT / "TASK_STATE.md"
GOAL_FILE = REPO_ROOT / "GOAL.md"
AGENTS_FILE = REPO_ROOT / "AGENTS.md"

REQUIRED_SMART_KEYS = [
    "Mode",
    "CheckpointCount",
    "CheckpointsSinceStandardSweep",
    "CheckpointsSinceDeepSweep",
    "LastQualifyingCheckpoint",
    "LastLightSweepCommit",
    "LastStandardSweepCommit",
    "LastDeepSweepCommit",
    "LastFullVerificationCommit",
    "LastCIState",
    "LastCleanupCommit",
    "LastToolCreated",
    "LastFailureClass",
    "ResumeTask",
]

ALLOWED_CI_STATES = {"NOT_CONFIGURED", "PENDING", "PASS", "FAIL", "UNAVAILABLE"}

COMMIT_PLACEHOLDERS = {"none", "pending"}

FORBIDDEN_PHRASES = ["停止并等待", "等待仓库所有者", "Agent 停止"]


class Findings:
    def __init__(self) -> None:
        self.errors: list[str] = []

    def add(self, msg: str) -> None:
        self.errors.append(msg)

    def ok(self) -> bool:
        return not self.errors


def git_exists(commitish: str) -> bool:
    try:
        subprocess.run(
            ["git", "-C", str(REPO_ROOT), "cat-file", "-t", commitish],
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        return False


def parse_smart_state() -> dict[str, str]:
    text = SMART_FILE.read_text(encoding="utf-8", errors="replace")
    block_match = re.search(r"```text\n(.*?)\n```", text, re.DOTALL)
    if not block_match:
        raise ValueError("SMART_OPERATION_STATE.md has no ```text block")

    raw = block_match.group(1)
    seen: dict[str, int] = {}
    values: dict[str, str] = {}
    for line in raw.splitlines():
        if ":" not in line:
            continue
        key, _, value = line.partition(":")
        key = key.strip()
        value = value.strip()
        if not key:
            continue
        seen[key] = seen.get(key, 0) + 1
        if seen[key] > 1:
            raise ValueError(f"SMART_OPERATION_STATE.md duplicate key: {key}")
        values[key] = value
    return values


def parse_task_checkpoints() -> list[dict[str, str]]:
    text = TASK_FILE.read_text(encoding="utf-8", errors="replace")
    checkpoints: list[dict[str, str]] = []
    # Find the checkpoint table
    match = re.search(r"## 5\. Checkpoint\n\n(.*?)(?:\n---|\n# )", text, re.DOTALL)
    if not match:
        return checkpoints
    table = match.group(1).strip()
    lines = [ln for ln in table.splitlines() if ln.startswith("|")]
    if len(lines) < 2:
        return checkpoints
    for ln in lines[2:]:
        cells = [c.strip() for c in ln.strip("|").split("|")]
        if len(cells) >= 3:
            checkpoints.append({
                "commit": cells[1],
                "task": cells[2],
                "state": cells[-1] if cells[-1] else "",
            })
    return checkpoints


def parse_task_issues() -> dict[str, dict[str, str]]:
    text = TASK_FILE.read_text(encoding="utf-8", errors="replace")
    issues: dict[str, dict[str, str]] = {}
    match = re.search(r"## 4\. 发现的问题队列\n\n(.*?)(?:\n---|\n# )", text, re.DOTALL)
    if not match:
        return issues
    table = match.group(1).strip()
    lines = [ln for ln in table.splitlines() if ln.startswith("|")]
    if len(lines) < 2:
        return issues
    for ln in lines[2:]:
        cells = [c.strip() for c in ln.strip("|").split("|")]
        if len(cells) >= 4:
            issues[cells[0]] = {
                "priority": cells[1],
                "area": cells[2],
                "state": cells[3],
                "evidence": cells[4] if len(cells) > 4 else "",
                "acceptance": cells[5] if len(cells) > 5 else "",
            }
    return issues


def parse_task_parent_states() -> dict[str, tuple[str, list[tuple[str, str]]]]:
    """Return {parent_id: (state, [(child_id, child_state), ...])}."""
    text = TASK_FILE.read_text(encoding="utf-8", errors="replace")
    parents: dict[str, tuple[str, list[tuple[str, str]]]] = {}
    top_pattern = re.compile(r"^# P(\d+)[ \—].*?\nState: `([A-Z_]+)`", re.MULTILINE | re.DOTALL)
    for m in top_pattern.finditer(text):
        parent_id = f"P{m.group(1)}"
        state = m.group(2)
        # find children until next top-level P section or end
        start = m.end()
        next_m = top_pattern.search(text, start)
        end = next_m.start() if next_m else len(text)
        segment = text[start:end]
        children: list[tuple[str, str]] = []
        child_pattern = re.compile(r"^## P(\d+(?:\.\d+)?).*?\nState: `([A-Z_]+)`", re.MULTILINE | re.DOTALL)
        for cm in child_pattern.finditer(segment):
            children.append((f"P{cm.group(1)}", cm.group(2)))
        parents[parent_id] = (state, children)
    return parents


def check_smart_state(findings: Findings) -> dict[str, str]:
    try:
        values = parse_smart_state()
    except ValueError as e:
        findings.add(str(e))
        return {}

    for key in REQUIRED_SMART_KEYS:
        if key not in values:
            findings.add(f"SMART_OPERATION_STATE.md missing required key: {key}")

    if values.get("Mode") != "PROFESSIONAL_AUTONOMOUS_STEWARDSHIP":
        findings.add(f"SMART_OPERATION_STATE.md Mode is not PROFESSIONAL_AUTONOMOUS_STEWARDSHIP: {values.get('Mode')}")

    ci = values.get("LastCIState", "")
    if ci not in ALLOWED_CI_STATES:
        findings.add(f"SMART_OPERATION_STATE.md LastCIState invalid: {ci}")

    for key in (
        "LastLightSweepCommit",
        "LastStandardSweepCommit",
        "LastDeepSweepCommit",
        "LastFullVerificationCommit",
        "LastQualifyingCheckpoint",
        "LastCleanupCommit",
    ):
        val = values.get(key, "")
        if val in COMMIT_PLACEHOLDERS:
            if key in ("LastFullVerificationCommit", "LastQualifyingCheckpoint") and val == "pending":
                findings.add(f"SMART_OPERATION_STATE.md {key} is pending (false sweep/checkpoint)")
            continue
        if not git_exists(val):
            findings.add(f"SMART_OPERATION_STATE.md {key} references non-existent commit: {val}")

    try:
        count = int(values.get("CheckpointCount", "0"))
    except ValueError:
        findings.add(f"SMART_OPERATION_STATE.md CheckpointCount is not an integer: {values.get('CheckpointCount')}")
        count = 0

    checkpoints = parse_task_checkpoints()
    qualifying = [c for c in checkpoints if c.get("state") == "qualifying"]
    if count != len(qualifying):
        findings.add(
            f"CheckpointCount mismatch: SMART says {count}, TASK_STATE.md has {len(qualifying)} qualifying checkpoints"
        )

    resume = values.get("ResumeTask", "")
    if resume in ("", "derive from TASK_STATE.md"):
        findings.add("SMART_OPERATION_STATE.md ResumeTask is not derived")
    else:
        text = TASK_FILE.read_text(encoding="utf-8", errors="replace")
        resume_id = resume.split()[0] if resume.split() else resume
        if not re.search(rf"^# {re.escape(resume_id)}[ \—]", text, re.MULTILINE):
            findings.add(f"SMART_OPERATION_STATE.md ResumeTask '{resume}' does not match a TASK_STATE.md section")

    return values


def check_task_state(findings: Findings) -> None:
    issues = parse_task_issues()
    for issue_id, issue in issues.items():
        state = issue["state"]
        if state not in {"TODO", "IN_PROGRESS", "COMPLETE", "BLOCKED_EXTERNAL", "BLOCKED_INTERNAL", "DEFERRED", "REJECTED"}:
            findings.add(f"TASK_STATE.md issue {issue_id} has unknown state: {state}")

    # Known stale issues from the v3 audit
    if issues.get("BASELINE-001", {}).get("state") == "TODO":
        findings.add("TASK_STATE.md BASELINE-001 is still TODO after P0.1")
    if issues.get("VERIFY-001", {}).get("state") == "TODO":
        findings.add("TASK_STATE.md VERIFY-001 is still TODO after P0.3")
    if issues.get("ARCH-001", {}).get("state") == "TODO":
        findings.add("TASK_STATE.md ARCH-001 is still TODO after P2")

    text = TASK_FILE.read_text(encoding="utf-8", errors="replace")
    if "## 5. Checkpoint" not in text:
        findings.add("TASK_STATE.md is missing ## 5. Checkpoint section")

    checkpoints = parse_task_checkpoints()

    parents = parse_task_parent_states()
    for parent_id, (state, children) in parents.items():
        if state == "COMPLETE":
            for child_id, child_state in children:
                if child_state in ("TODO", "IN_PROGRESS"):
                    findings.add(
                        f"TASK_STATE.md parent {parent_id} is COMPLETE but child {child_id} is {child_state}"
                    )
        elif state in ("TODO", "IN_PROGRESS"):
            # all required children must not be COMPLETE if parent is TODO? Not necessarily.
            pass


def check_control_files(findings: Findings) -> None:
    for path in (GOAL_FILE, AGENTS_FILE):
        text = path.read_text(encoding="utf-8", errors="replace")
        for phrase in FORBIDDEN_PHRASES:
            if phrase in text:
                findings.add(f"{path.name} contains forbidden stop/wait phrase: {phrase}")


def main() -> int:
    findings = Findings()
    check_smart_state(findings)
    check_task_state(findings)
    check_control_files(findings)

    if findings.ok():
        print("A13 automation state OK")
        return 0

    print("A13 automation state errors:")
    for err in findings.errors:
        print(f"  - {err}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
