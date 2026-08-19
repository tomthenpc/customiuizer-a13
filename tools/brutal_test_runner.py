#!/usr/bin/env python3
"""Mutation, hermeticity and determinism tests for repository gates.

The tool never mutates the real worktree during mutation testing. Every
mutation is applied to a detached temporary git worktree and is expected to be
"killed" by a configured gate. A surviving mutation is a concrete test gap.
"""
from __future__ import annotations

import argparse
import contextlib
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Callable


def run(command: str, cwd: Path, timeout: int, env: dict[str, str] | None = None) -> subprocess.CompletedProcess:
    merged = os.environ.copy()
    if env:
        merged.update(env)
    return subprocess.run(
        command,
        cwd=cwd,
        shell=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        timeout=timeout,
        env=merged,
    )


def repo_root() -> Path:
    result = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        text=True,
        encoding="utf-8",
        errors="replace",
        capture_output=True,
        check=True,
    )
    return Path(result.stdout.strip()).resolve()


def tracked_hashes(root: Path) -> dict[str, str]:
    result = subprocess.run(
        ["git", "ls-files", "-z"],
        cwd=root,
        stdout=subprocess.PIPE,
        check=True,
    )
    hashes: dict[str, str] = {}
    for raw in result.stdout.split(b"\0"):
        if not raw:
            continue
        rel = raw.decode("utf-8", errors="surrogateescape")
        path = root / rel
        if path.is_file():
            hashes[rel] = hashlib.sha256(path.read_bytes()).hexdigest()
        else:
            hashes[rel] = "<missing>"
    return hashes


@contextlib.contextmanager
def detached_worktree(root: Path):
    temp_parent = Path(tempfile.mkdtemp(prefix="brutal-worktree-"))
    target = temp_parent / "repo"
    subprocess.run(
        ["git", "worktree", "add", "--detach", "--force", str(target), "HEAD"],
        cwd=root,
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    try:
        yield target
    finally:
        subprocess.run(
            ["git", "worktree", "remove", "--force", str(target)],
            cwd=root,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            encoding="utf-8",
            errors="replace",
            text=True,
        )
        shutil.rmtree(temp_parent, ignore_errors=True)


def replace_first(path: Path, pattern: str, replacement: str, flags: int = 0) -> None:
    text = path.read_text(encoding="utf-8")
    changed, count = re.subn(pattern, replacement, text, count=1, flags=flags)
    if count != 1:
        raise RuntimeError(f"mutation pattern not found in {path}: {pattern}")
    path.write_text(changed, encoding="utf-8")


def append_after_line(path: Path, prefix: str, lines: list[str]) -> None:
    text = path.read_text(encoding="utf-8")
    out = []
    done = False
    for line in text.splitlines():
        out.append(line)
        if not done and line.startswith(prefix):
            out.extend(lines)
            done = True
    if not done:
        raise RuntimeError(f"line prefix not found: {prefix}")
    path.write_text("\n".join(out) + "\n", encoding="utf-8")


def mutate_duplicate_smart(root: Path, cfg: dict) -> None:
    path = root / cfg["smart_state"]
    text = path.read_text(encoding="utf-8")
    m = re.search(r"(?m)^CurrentObjective:.*$", text)
    if not m:
        raise RuntimeError("CurrentObjective missing")
    path.write_text(text[:m.end()] + "\n" + m.group(0) + text[m.end():], encoding="utf-8")


def mutate_fake_ci_pass(root: Path, cfg: dict) -> None:
    path = root / cfg["smart_state"]
    replace_first(path, r"(?m)^LastCIState:.*$", "LastCIState: PASS")
    text = path.read_text(encoding="utf-8")
    if "LastCIRun:" not in text:
        append_after_line(
            path,
            "LastCIState:",
            [
                "LastCIRun: https://github.com/example/example/actions/runs/1",
                "LastCIJob: https://github.com/example/example/actions/runs/1/job/1",
                "LastCICommit: 0000000000000000000000000000000000000000",
            ],
        )


def mutate_symbolic_tree(root: Path, cfg: dict) -> None:
    replace_first(root / cfg["smart_state"], r"(?m)^LastVerifiedTree:.*$", "LastVerifiedTree: HEAD^{tree}")


def mutate_ambiguous_resume(root: Path, cfg: dict) -> None:
    replace_first(root / cfg["smart_state"], r"(?m)^ResumeTask:(.*)$", r"ResumeTask:\1 or skip to P99")


def mutate_task_drift(root: Path, cfg: dict) -> None:
    replace_first(root / cfg["task_state"], r"State:\s*`COMPLETE`", "State: `TODO`")


def mutate_progress_tamper(root: Path, cfg: dict) -> None:
    path = root / cfg["progress_json"]
    data = json.loads(path.read_text(encoding="utf-8"))
    data["__BRUTAL_MUTATION__"] = {"projectProgress": 999.0}
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def workflow_path(root: Path, cfg: dict, full: bool = False) -> Path:
    paths = cfg["full_workflows"] if full else cfg["fast_workflows"]
    if not paths:
        raise RuntimeError("workflow path not configured")
    return root / paths[0]


def mutate_remove_fetch_depth(root: Path, cfg: dict) -> None:
    path = workflow_path(root, cfg)
    text = path.read_text(encoding="utf-8")
    changed, count = re.subn(r"(?m)^\s*fetch-depth\s*:\s*0\s*\n", "", text, count=1)
    if count != 1:
        raise RuntimeError("fetch-depth: 0 not found")
    path.write_text(changed, encoding="utf-8")


def mutate_wrong_branch(root: Path, cfg: dict) -> None:
    path = workflow_path(root, cfg)
    text = path.read_text(encoding="utf-8")
    changed = re.sub(
        r"(?m)^(\s*-\s*)" + re.escape(cfg["expected_branch"]) + r"\s*$",
        r"\g<1>devin/stale-ci-branch",
        text,
        count=1,
    )
    if changed == text:
        raise RuntimeError("YAML branch entry not found for mutation")
    path.write_text(changed, encoding="utf-8")


def mutate_signing_leak(root: Path, cfg: dict) -> None:
    path = workflow_path(root, cfg)
    with path.open("a", encoding="utf-8") as f:
        f.write(
            "\n# BRUTAL MUTATION\n"
            "      - name: forbidden signing\n"
            "        run: ./gradlew -PofficialRelease=true :app:assembleDevelop\n"
        )


def mutate_hardcoded_api37(root: Path, cfg: dict) -> None:
    path = workflow_path(root, cfg)
    with path.open("a", encoding="utf-8") as f:
        f.write(
            "\n# BRUTAL MUTATION\n"
            "      - name: brittle sdk\n"
            '        run: sdkmanager "platforms;android-37" "build-tools;37.0.0"\n'
        )


def mutate_windows_path(root: Path, cfg: dict) -> None:
    path = root / "tools" / "__brutal_windows_path_mutation.py"
    drive = f"{chr(67)}{chr(58)}"
    sep = chr(92) * 2
    path.write_text(
        'from pathlib import Path\n'
        f'x = Path("{drive}\\\\temp\\\\repo")\n'
        f'y = "a/b".replace("/", "{sep}")\n',
        encoding="utf-8",
    )


def mutate_duplicate_feature_id(root: Path, cfg: dict) -> None:
    path = root / cfg["catalog_file"]
    text = path.read_text(encoding="utf-8")
    ids = list(re.finditer(r'\bid\s*=\s*"([^"]+)"', text))
    unique = []
    for m in ids:
        if m.group(1) not in [u.group(1) for u in unique]:
            unique.append(m)
        if len(unique) == 2:
            break
    if len(unique) < 2:
        raise RuntimeError("two FeatureSpec ids not found")
    second = unique[1]
    changed = text[:second.start(1)] + unique[0].group(1) + text[second.end(1):]
    path.write_text(changed, encoding="utf-8")


def mutate_preference_default(root: Path, cfg: dict) -> None:
    path = root / cfg["catalog_file"]
    text = path.read_text(encoding="utf-8")
    m = re.search(r"getBoolean\s*\(([^,\n]+),\s*(false|true)\s*\)", text)
    if not m:
        raise RuntimeError("literal getBoolean default not found")
    flipped = "true" if m.group(2) == "false" else "false"
    changed = text[:m.start(2)] + flipped + text[m.end(2):]
    path.write_text(changed, encoding="utf-8")


def mutate_process_scope(root: Path, cfg: dict) -> None:
    path = root / cfg["catalog_file"]
    text = path.read_text(encoding="utf-8")
    target_id = 'id = "packagePermissions"'
    idx = text.find(target_id)
    if idx < 0:
        raise RuntimeError("packagePermissions FeatureSpec not found")
    region_start = text.rfind("FeatureSpec(", 0, idx)
    region_end = text.find(")", idx) + 1
    region = text[region_start:region_end]
    mutated_region = region.replace(
        "ProcessScope.SYSTEM_SERVER",
        "ProcessScope.SYSTEM_UI",
        1,
    )
    if mutated_region == region:
        raise RuntimeError("ProcessScope.SYSTEM_SERVER not found in packagePermissions")
    changed = text[:region_start] + mutated_region + text[region_end:]
    path.write_text(changed, encoding="utf-8")


def mutate_install_phase(root: Path, cfg: dict) -> None:
    path = root / cfg["catalog_file"]
    text = path.read_text(encoding="utf-8")
    m = re.search(r"installPhase\s*=\s*InstallPhase\.([A-Z0-9_]+)", text)
    if not m:
        raise RuntimeError("installPhase not found")
    replacements = ["PACKAGE_READY", "APPLICATION_ATTACH", "ZYGOTE_READY"]
    target = next((x for x in replacements if x != m.group(1)), "PACKAGE_READY")
    changed = text[:m.start(1)] + target + text[m.end(1):]
    path.write_text(changed, encoding="utf-8")


def mutate_remove_dispatch(root: Path, cfg: dict) -> None:
    roots = [root / p for p in cfg.get("installer_roots", ["app/src/main/java"])]
    for base in roots:
        for path in sorted([*base.rglob("*.kt"), *base.rglob("*.java")]):
            if "SystemUiInstaller" not in path.name:
                continue
            text = path.read_text(encoding="utf-8")
            changed, count = re.subn(
                r"(?m)^.*FeatureDispatcher\.installById\([^\n]*\n",
                "",
                text,
                count=1,
            )
            if count:
                path.write_text(changed, encoding="utf-8")
                return
    raise RuntimeError("FeatureDispatcher.installById call not found in SystemUiInstaller")


def mutation_source_file(root: Path, body: str) -> None:
    path = root / "app/src/main/java/brutal_mutation/InjectedHazard.kt"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        "package brutal_mutation\n\n" + body + "\n",
        encoding="utf-8",
    )


def mutate_fatal_swallow(root: Path, cfg: dict) -> None:
    mutation_source_file(
        root,
        'object InjectedHazard { fun run() { try { error("x") } catch (t: Throwable) { } } }',
    )


def mutate_static_context(root: Path, cfg: dict) -> None:
    mutation_source_file(
        root,
        "object InjectedHazard {\n    @JvmField\n    var context: android.content.Context? = null\n}",
    )


def mutate_eager_thread(root: Path, cfg: dict) -> None:
    mutation_source_file(
        root,
        'object InjectedHazard {\n    val worker = android.os.HandlerThread("bad").apply { start() }\n}',
    )


MUTATORS: dict[str, Callable[[Path, dict], None]] = {
    "duplicate_smart_key": mutate_duplicate_smart,
    "fake_ci_pass": mutate_fake_ci_pass,
    "symbolic_verified_tree": mutate_symbolic_tree,
    "ambiguous_resume": mutate_ambiguous_resume,
    "task_state_drift": mutate_task_drift,
    "progress_json_tamper": mutate_progress_tamper,
    "remove_fetch_depth": mutate_remove_fetch_depth,
    "wrong_ci_branch": mutate_wrong_branch,
    "signing_leak": mutate_signing_leak,
    "hardcoded_api37": mutate_hardcoded_api37,
    "windows_path": mutate_windows_path,
    "duplicate_feature_id": mutate_duplicate_feature_id,
    "preference_default_flip": mutate_preference_default,
    "process_scope_flip": mutate_process_scope,
    "install_phase_flip": mutate_install_phase,
    "remove_installer_dispatch": mutate_remove_dispatch,
    "fatal_swallow": mutate_fatal_swallow,
    "static_context_leak": mutate_static_context,
    "eager_handler_thread": mutate_eager_thread,
}


def hermeticity(root: Path, cfg: dict, timeout: int) -> int:
    before = tracked_hashes(root)
    failures = []
    for command in cfg["hermetic_commands"]:
        result = run(command, root, timeout)
        if result.returncode:
            failures.append(f"command failed: {command}\n{result.stdout[-3000:]}")
    after = tracked_hashes(root)
    dirty = sorted(k for k in set(before) | set(after) if before.get(k) != after.get(k))
    if dirty:
        failures.append(f"tracked files changed by read-only tests: {dirty}")
    if failures:
        print("Hermeticity FAILED")
        for f in failures:
            print(f"  {f}")
        return 1
    print(f"Hermeticity passed: {len(cfg['hermetic_commands'])} command(s), no tracked changes")
    return 0


def determinism(root: Path, cfg: dict, timeout: int) -> int:
    command = cfg["determinism_command"]
    outputs = [Path(p) for p in cfg["determinism_outputs"]]
    snapshots = []
    with detached_worktree(root) as work:
        for env in (
            {"PYTHONHASHSEED": "1", "TZ": "UTC", "LC_ALL": "C"},
            {"PYTHONHASHSEED": "777", "TZ": "Asia/Tokyo", "LC_ALL": "C"},
        ):
            result = run(command, work, timeout, env)
            if result.returncode:
                print(result.stdout)
                return 1
            current = {}
            for rel in outputs:
                path = work / rel
                if not path.exists():
                    print(f"Determinism output missing: {rel}")
                    return 1
                text = path.read_text(encoding="utf-8")
                text = re.sub(r'(?m)^.*(?:GeneratedAt|generatedAt|AuditTime|audit_time).*$\n?', "", text)
                current[rel.as_posix()] = hashlib.sha256(text.encode()).hexdigest()
            snapshots.append(current)
    if snapshots[0] != snapshots[1]:
        print("Determinism FAILED")
        print(json.dumps({"run1": snapshots[0], "run2": snapshots[1]}, indent=2))
        return 1
    print(f"Determinism passed for {len(outputs)} output(s)")
    return 0


def mutation_test(root: Path, cfg: dict, timeout: int, selected: set[str] | None) -> int:
    results = []
    for case in cfg["mutations"]:
        name = case["name"]
        if selected and name not in selected:
            continue
        mutator = MUTATORS.get(case["mutator"])
        if mutator is None:
            results.append((name, "ERROR", "unknown mutator"))
            continue
        gate = cfg["gates"][case["gate"]]
        try:
            with detached_worktree(root) as work:
                before_tracked = tracked_hashes(work)
                before_all = set(
                    p.relative_to(work).as_posix()
                    for p in work.rglob("*") if p.is_file() and ".git" not in p.parts
                )
                mutator(work, cfg)
                after_tracked = tracked_hashes(work)
                after_all = set(
                    p.relative_to(work).as_posix()
                    for p in work.rglob("*") if p.is_file() and ".git" not in p.parts
                )
                new_files = (set(after_tracked) - set(before_tracked)) | (after_all - before_all)
                changed_files = {k for k in set(before_tracked) & set(after_tracked) if before_tracked[k] != after_tracked[k]}
                if not new_files and not changed_files:
                    results.append((name, "MUTATION_NOT_APPLIED", "mutator produced no file changes"))
                    continue
                result = run(gate, work, timeout)
                status = "KILLED" if result.returncode != 0 else "SURVIVED"
                tail = result.stdout[-2500:]
                results.append((name, status, tail))
        except Exception as exc:
            results.append((name, "ERROR", repr(exc)))

    failed = False
    for name, status, tail in results:
        print(f"{status:9} {name}")
        if status != "KILLED":
            failed = True
            if tail:
                print("  " + tail.replace("\n", "\n  "))
    killed = sum(1 for _, s, _ in results if s == "KILLED")
    print(f"Mutation score: {killed}/{len(results)} killed")
    if failed:
        print("SURVIVED_MUTATION means the current test suite did not detect an injected defect.")
        return 1
    return 0


def main(argv=None) -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--config", required=True)
    p.add_argument("--timeout", type=int, default=900)
    sub = p.add_subparsers(dest="command", required=True)
    sub.add_parser("hermeticity")
    sub.add_parser("determinism")
    m = sub.add_parser("mutate")
    m.add_argument("--case", action="append")
    sub.add_parser("all")
    args = p.parse_args(argv)

    root = repo_root()
    cfg = json.loads((root / args.config).read_text(encoding="utf-8"))

    if args.command == "hermeticity":
        return hermeticity(root, cfg, args.timeout)
    if args.command == "determinism":
        return determinism(root, cfg, args.timeout)
    if args.command == "mutate":
        return mutation_test(root, cfg, args.timeout, set(args.case or []) or None)

    code = hermeticity(root, cfg, args.timeout)
    if code:
        return code
    code = determinism(root, cfg, args.timeout)
    if code:
        return code
    return mutation_test(root, cfg, args.timeout, None)


if __name__ == "__main__":
    raise SystemExit(main())
