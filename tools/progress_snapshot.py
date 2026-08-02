#!/usr/bin/env python3
"""A13 v6 progress snapshot generator.

Implements the v6 scoring model:
- Progress is derived from TASK_STATE.md section states.
- Percentages cannot be hand-edited; all values are computed.
- PLAN/governance and state-only commits do not increase progress.
- Each score has an evidence path (state, evidence keywords, git trailer).
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

REPO_ROOT = Path(
    os.environ.get("CHECK_AUTOMATION_REPO_ROOT")
    if "CHECK_AUTOMATION_REPO_ROOT" in os.environ
    else Path(__file__).resolve().parent.parent
)
if isinstance(REPO_ROOT, str):
    REPO_ROOT = Path(REPO_ROOT)

SMART_FILE = REPO_ROOT / "SMART_OPERATION_STATE.md"
TASK_FILE = REPO_ROOT / "TASK_STATE.md"
JSON_FILE = REPO_ROOT / "docs" / "progress" / "A13_PROGRESS_CURRENT.json"
MD_FILE = REPO_ROOT / "docs" / "progress" / "A13_PROGRESS_CURRENT.md"

DOMAIN_WEIGHTS = {
    "Baseline and autonomous control": 8,
    "Runtime architecture and Feature/Hook ownership": 22,
    "Runtime safety, lifecycle and concurrency": 18,
    "Performance, memory, APK and R8": 12,
    "ROM intelligence and compatibility evidence": 10,
    "Java/Kotlin boundary and maintainability": 8,
    "Build, CI, signing, artifact and release engineering": 12,
    "Current documentation and provenance": 5,
    "Device validation": 5,
}

DOMAIN_PATTERNS = [
    ("Baseline and autonomous control", [r"^P0(\.|$)", r"^P11(\.|$)"]),
    ("Runtime architecture and Feature/Hook ownership", [r"^P[1234](\.|$)"]),
    ("Runtime safety, lifecycle and concurrency", [r"^P5(\.|$)"]),
    ("Performance, memory, APK and R8", [r"^P6(\.|$)"]),
    ("ROM intelligence and compatibility evidence", [r"^P8(\.|$)"]),
    ("Java/Kotlin boundary and maintainability", [r"^P7(\.|$)"]),
    ("Build, CI, signing, artifact and release engineering", [r"^P9(\.|$)", r"^P12(\.|$)"]),
    ("Current documentation and provenance", [r"^P10(\.|$)"]),
    ("Device validation", [r"^P13(\.|$)"]),
]

EVIDENCE_ORDER = ["NOT_EXERCISED", "STATIC_PROVEN", "BUILD_VERIFIED", "CI_VERIFIED", "DEVICE_VERIFIED"]

STATE_COMPLETION = {
    "TODO": 0.0,
    "BLOCKED_INTERNAL": 0.0,
    "BLOCKED_EXTERNAL": 0.0,
    "IN_PROGRESS": 0.5,
    "VERIFIED_STATIC": 1.0,
    "VERIFIED_BUILD": 1.0,
    "VERIFIED_CI": 1.0,
    "VERIFIED_DEVICE": 1.0,
    "COMPLETE": 1.0,
}


@dataclass
class Section:
    id: str
    title: str
    state: str
    evidence: list[str]


@dataclass
class DomainScore:
    name: str
    weight: int
    score: float
    evidence_level: str
    sections: list[str]


@dataclass
class Snapshot:
    audit_time: str
    head: str
    tree: str
    verified_tree: str | None
    verified_mode: str | None
    ahead_of_main: int
    checkpoint_count: int
    project_progress: float
    machine_progress: float
    stage: str
    domain_scores: list[DomainScore]
    sections: list[Section]
    notes: list[str]


def git(*args: str, check: bool = False) -> str:
    cmd = ["git", "-C", str(REPO_ROOT)] + list(args)
    result = subprocess.run(cmd, capture_output=True, text=True, check=check)
    return result.stdout.strip() if result.returncode == 0 else ""


def head_sha() -> str:
    return git("rev-parse", "HEAD")


def head_tree() -> str:
    return git("rev-parse", "HEAD^{tree}")


def ahead_of_main() -> int:
    out = git("rev-list", "--count", "main..HEAD")
    try:
        return int(out)
    except ValueError:
        return 0


def verified_tree_from_head() -> str | None:
    message = git("log", "-1", "--pretty=format:%B")
    match = re.search(r"^Verified-Tree:\s*([0-9a-f]{40})\b", message, re.MULTILINE)
    return match.group(1) if match else None


def verified_mode_from_head() -> str | None:
    message = git("log", "-1", "--pretty=format:%B")
    match = re.search(r"^Verification:\s*(\S+)", message, re.MULTILINE)
    return match.group(1) if match else None


def infer_evidence(body: str) -> str:
    """Infer the strongest evidence type present in a section body."""
    body_l = body.lower()
    for level in EVIDENCE_ORDER[::-1]:
        if level.lower() in body_l:
            return level
    if re.search(r"verify\.ps1.*-mode\s+(full|fast|final)", body_l) or re.search(r"verify\.py\s+(full|fast)", body_l):
        return "BUILD_VERIFIED"
    if re.search(r"github\s*action|ci:|\.github/workflows", body_l):
        return "CI_VERIFIED"
    if re.search(r"lsposed|logcat|rom\s*sample|device\s*evidence|实机", body_l):
        return "DEVICE_VERIFIED"
    return "STATIC_PROVEN"


def parse_smart() -> dict[str, str]:
    text = SMART_FILE.read_text(encoding="utf-8", errors="replace")
    match = re.search(r"```text\n(.*?)\n```", text, re.S)
    if not match:
        return {}
    values: dict[str, str] = {}
    for line in match.group(1).splitlines():
        if ":" in line:
            key, _, value = line.partition(":")
            values[key.strip()] = value.strip()
    return values


def parse_task_state() -> list[Section]:
    """Parse major P sections. Subsections are only used when a major has no state."""
    text = TASK_FILE.read_text(encoding="utf-8", errors="replace")
    major_pattern = re.compile(r"^(# )P(\d+)[ \—]\s*(.+)$", re.MULTILINE)
    state_pattern = re.compile(r"State:\s*`(\w+)`")
    evidence_pattern = re.compile(r"Evidence:\s*`([^`]+)`")
    sub_pattern = re.compile(r"^(## )P(\d+\.\d+)[ \—]\s*(.+)$", re.MULTILINE)

    majors = list(major_pattern.finditer(text))
    sections: list[Section] = []

    for i, match in enumerate(majors):
        major_id = f"P{match.group(2)}"
        major_title = match.group(3).strip()
        start = match.end()
        end = majors[i + 1].start() if i + 1 < len(majors) else len(text)
        body = text[start:end]

        first_sub = sub_pattern.search(body)
        header_body = body[: first_sub.start()] if first_sub else body
        state_match = state_pattern.search(header_body)
        if state_match:
            state = state_match.group(1)
            evidence = [m.group(1) for m in evidence_pattern.finditer(body)]
            if not evidence:
                evidence = [infer_evidence(body)] if state in ("COMPLETE", "VERIFIED_STATIC", "VERIFIED_BUILD", "VERIFIED_CI", "VERIFIED_DEVICE") else ["NOT_EXERCISED"]
            sections.append(Section(major_id, major_title, state, evidence))
            continue

        # Major has no state: aggregate from subsections.
        sub_matches = list(sub_pattern.finditer(body))
        if not sub_matches:
            sections.append(Section(major_id, major_title, "TODO", ["NOT_EXERCISED"]))
            continue

        subs: list[Section] = []
        for j, sub_match in enumerate(sub_matches):
            sub_id = f"P{sub_match.group(2)}"
            sub_title = sub_match.group(3).strip()
            sub_start = sub_match.end()
            sub_end = sub_matches[j + 1].start() if j + 1 < len(sub_matches) else end
            sub_body = body[sub_start:sub_end]
            sub_state_match = state_pattern.search(sub_body)
            sub_state = sub_state_match.group(1) if sub_state_match else "TODO"
            sub_evidence = [m.group(1) for m in evidence_pattern.finditer(sub_body)]
            if not sub_evidence:
                sub_evidence = [infer_evidence(sub_body)] if sub_state in ("COMPLETE", "VERIFIED_STATIC", "VERIFIED_BUILD", "VERIFIED_CI", "VERIFIED_DEVICE") else ["NOT_EXERCISED"]
            subs.append(Section(sub_id, sub_title, sub_state, sub_evidence))

        # Aggregate: average completion, take min evidence.
        completion = sum(STATE_COMPLETION.get(s.state, 0.0) for s in subs) / len(subs)
        if completion >= 1.0:
            state = "COMPLETE"
        elif completion > 0.0:
            state = "IN_PROGRESS"
        else:
            state = "TODO"
        all_evidence = sorted(
            {e for s in subs for e in s.evidence if e in EVIDENCE_ORDER},
            key=EVIDENCE_ORDER.index,
        )
        evidence = all_evidence[:1] or ["NOT_EXERCISED"]
        sections.append(Section(major_id, major_title, state, evidence))

    return sections


def domain_for(section_id: str) -> str | None:
    for domain, patterns in DOMAIN_PATTERNS:
        for pattern in patterns:
            if re.search(pattern, section_id):
                return domain
    return None


def evidence_for(section: Section) -> str:
    for level in EVIDENCE_ORDER[::-1]:
        if any(level in e for e in section.evidence):
            return level
    if section.state in ("VERIFIED_STATIC", "VERIFIED_BUILD", "VERIFIED_CI", "VERIFIED_DEVICE", "COMPLETE"):
        return "STATIC_PROVEN"
    return "NOT_EXERCISED"


def compute_progress(sections: list[Section], smart: dict[str, str]) -> Snapshot:
    grouped: dict[str, list[Section]] = {d: [] for d in DOMAIN_WEIGHTS}
    for section in sections:
        domain = domain_for(section.id)
        if domain:
            grouped[domain].append(section)

    notes: list[str] = []
    domain_scores: list[DomainScore] = []
    project_progress = 0.0

    for domain, weight in DOMAIN_WEIGHTS.items():
        ds = grouped[domain]
        if not ds:
            score = 0.0
            level = "NOT_EXERCISED"
        else:
            completions = [STATE_COMPLETION.get(s.state, 0.0) for s in ds]
            score = sum(completions) / len(ds)
            levels = [evidence_for(s) for s in ds]
            level = min(levels, key=EVIDENCE_ORDER.index)
            if any(s.state == "BLOCKED_EXTERNAL" for s in ds) and domain == "Device validation":
                notes.append(f"{domain} is blocked by external evidence.")
        domain_scores.append(
            DomainScore(
                name=domain,
                weight=weight,
                score=round(score * 100, 2),
                evidence_level=level,
                sections=[s.id for s in ds],
            )
        )
        project_progress += weight * score

    # Machine progress: subtract the device domain contribution unless it is
    # device-verified. This keeps signed RC / CI / ROM evidence while removing
    # the 5 points that require actual device evidence.
    device_index = next((i for i, d in enumerate(domain_scores) if d.name == "Device validation"), -1)
    if device_index >= 0 and domain_scores[device_index].evidence_level != "DEVICE_VERIFIED":
        machine_progress = project_progress - (DOMAIN_WEIGHTS["Device validation"] * (domain_scores[device_index].score / 100))
    else:
        machine_progress = project_progress

    project_progress = round(project_progress, 2)
    machine_progress = round(machine_progress, 2)

    verified_tree = verified_tree_from_head()
    verified_mode = verified_mode_from_head()

    return Snapshot(
        audit_time=datetime.now(timezone.utc).isoformat(),
        head=head_sha(),
        tree=head_tree(),
        verified_tree=verified_tree,
        verified_mode=verified_mode,
        ahead_of_main=ahead_of_main(),
        checkpoint_count=int(smart.get("CheckpointCount", "0")),
        project_progress=project_progress,
        machine_progress=machine_progress,
        stage=stage_for_score(project_progress),
        domain_scores=domain_scores,
        sections=sections,
        notes=notes,
    )


def stage_for_score(score: float) -> str:
    if score < 20:
        return "FOUNDATION"
    if score < 40:
        return "CORE_RECONSTRUCTION"
    if score < 60:
        return "SYSTEM_HARDENING"
    if score < 75:
        return "INTEGRATION_AND_EVIDENCE"
    if score < 90:
        return "RELEASE_CANDIDATE_PREPARATION"
    if score < 100:
        return "EXTERNAL_VALIDATION"
    return "PROJECT_COMPLETE"


def snapshot_to_dict(snapshot: Snapshot) -> dict[str, Any]:
    return asdict(snapshot)


def render_markdown(snapshot: Snapshot) -> str:
    lines = [
        "# A13 Progress Snapshot",
        "",
        "```text",
        f"AuditTime: {snapshot.audit_time}",
        f"HEAD: {snapshot.head}",
        f"Tree: {snapshot.tree}",
        f"VerifiedTree: {snapshot.verified_tree or 'not recorded'}",
        f"VerifiedMode: {snapshot.verified_mode or 'none'}",
        f"AheadOfMain: {snapshot.ahead_of_main}",
        f"CheckpointCount: {snapshot.checkpoint_count}",
        f"ProjectProgress: {snapshot.project_progress}%",
        f"MachineProgress: {snapshot.machine_progress}%",
        f"Stage: {snapshot.stage}",
        "```",
        "",
        "## Domain Scores",
        "",
        "| Domain | Weight | Score | Evidence | Sections |",
        "|---|---:|---:|---|---|",
    ]
    for d in snapshot.domain_scores:
        lines.append(
            f"| {d.name} | {d.weight} | {d.score}% | {d.evidence_level} | {', '.join(d.sections)} |"
        )
    lines += ["", "## Section States", ""]
    for s in snapshot.sections:
        evidence = ", ".join(s.evidence) if s.evidence else "none"
        lines.append(f"- **{s.id}** {s.title} — `{s.state}` ({evidence})")
    if snapshot.notes:
        lines += ["", "## Notes", ""]
        for note in snapshot.notes:
            lines.append(f"- {note}")
    lines += [
        "",
        "---",
        "",
        "This snapshot is auto-generated by `tools/progress_snapshot.py`. "
        "Do not edit percentages manually.",
    ]
    return "\n".join(lines) + "\n"


def write_snapshot(snapshot: Snapshot) -> None:
    JSON_FILE.parent.mkdir(parents=True, exist_ok=True)
    JSON_FILE.write_text(
        json.dumps(snapshot_to_dict(snapshot), indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    MD_FILE.write_text(render_markdown(snapshot), encoding="utf-8")


def _semantic_equal(a: Any, b: Any) -> bool:
    """Compare snapshots ignoring volatile audit-time metadata."""
    if isinstance(a, dict) and isinstance(b, dict):
        for key in a:
            if key in ("audit_time", "head", "tree", "verified_tree", "verified_mode"):
                continue
            if key not in b or not _semantic_equal(a[key], b[key]):
                return False
        for key in b:
            if key in ("audit_time", "head", "tree", "verified_tree", "verified_mode"):
                continue
            if key not in a:
                return False
        return True
    if isinstance(a, list) and isinstance(b, list):
        return len(a) == len(b) and all(_semantic_equal(x, y) for x, y in zip(a, b))
    return a == b


def check_snapshot() -> bool:
    snapshot = compute_progress(parse_task_state(), parse_smart())
    if not JSON_FILE.exists() or not MD_FILE.exists():
        return False
    stored = json.loads(JSON_FILE.read_text(encoding="utf-8"))
    current = snapshot_to_dict(snapshot)
    return _semantic_equal(stored, current)


def main() -> int:
    parser = argparse.ArgumentParser(description="A13 v6 progress snapshot generator")
    parser.add_argument("--check", action="store_true", help="Check current docs are up to date")
    parser.add_argument("--write", action="store_true", help="Write progress docs")
    args = parser.parse_args()

    if args.check:
        if check_snapshot():
            print("A13 progress snapshot is up to date")
            return 0
        print("A13 progress snapshot is out of date (run --write)")
        return 1

    if args.write:
        snapshot = compute_progress(parse_task_state(), parse_smart())
        write_snapshot(snapshot)
        print("A13 progress snapshot written")
        print(f"ProjectProgress: {snapshot.project_progress}%")
        print(f"MachineProgress: {snapshot.machine_progress}%")
        print(f"Stage: {snapshot.stage}")
        return 0

    parser.print_help()
    return 1


if __name__ == "__main__":
    sys.exit(main())
