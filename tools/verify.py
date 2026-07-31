#!/usr/bin/env python3
"""Unified local verification for CustoMIUIzer A13.

Only the Python standard library is used.  No ADB, no APK build, no release
signing, no artifact upload.

Usage:
    python tools/verify.py fast
    python tools/verify.py fast --tests PreferenceBootstrapTest
    python tools/verify.py fast --tests ModuleHelperReceiverTest
    python tools/verify.py full
"""
from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
BUILD_LOG_DIR = REPO_ROOT / "build" / "verify-logs"
GRADLEW = REPO_ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")

FORBIDDEN_TASKS = {
    "assemble",
    "package",
    "bundle",
    "install",
    "sign",
    "publish",
    "officialRelease",
    "lintVitalRelease",
}


def log_file(task: str) -> Path:
    BUILD_LOG_DIR.mkdir(parents=True, exist_ok=True)
    stamp = time.strftime("%Y%m%d-%H%M%S")
    return BUILD_LOG_DIR / f"{stamp}-{task}.log"


def _safe_print(msg: str) -> None:
    if hasattr(sys.stdout, "buffer"):
        encoded = msg.encode(sys.stdout.encoding, errors="replace")
        sys.stdout.buffer.write(encoded + b"\n")
    else:
        print(msg)


def print_tail(path: Path, lines: int = 40) -> None:
    try:
        text = path.read_text(encoding="utf-8", errors="replace")
    except OSError as exc:
        _safe_print(f"  could not read log: {exc}")
        return
    for line in text.splitlines()[-lines:]:
        _safe_print(f"  {line}")


def guard_task(task: str) -> None:
    if any(forbidden in task for forbidden in FORBIDDEN_TASKS):
        print(f"[verify] refuse to run forbidden task: {task}")
        sys.exit(2)


def run(cmd: list[str], task: str) -> int:
    guard_task(task)
    log = log_file(task)
    cmd = [str(c) for c in cmd]
    print(f"[verify] {task} -> {' '.join(cmd)}")
    with open(log, "w", encoding="utf-8") as f:
        result = subprocess.run(cmd, cwd=REPO_ROOT, stdout=f, stderr=subprocess.STDOUT)
    if result.returncode == 0:
        print(f"[verify] {task} OK")
    else:
        print(f"[verify] {task} FAILED (exit {result.returncode}); log: {log}")
        print_tail(log)
    return result.returncode


def changed_files() -> list[str]:
    names: list[str] = []
    for git_cmd in ("diff --name-only", "diff --cached --name-only"):
        result = subprocess.run(
            ["git"] + git_cmd.split(),
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
        )
        if result.returncode == 0:
            names.extend(result.stdout.splitlines())
    return names


def relevant_to_android(names: list[str]) -> bool:
    if not names:
        return True  # no git diff available, be conservative
    for name in names:
        if name.startswith("app/src") or name.endswith(".gradle.kts") or name.startswith("gradle/"):
            return True
    return False


def gradle_cmd() -> str:
    if GRADLEW.is_file():
        return str(GRADLEW)
    if os.name == "nt":
        return "gradlew"
    return "./gradlew"


def find_python() -> str:
    python = shutil.which("python3") or shutil.which("python")
    return python or sys.executable


def check_invariants() -> int:
    return run([find_python(), str(REPO_ROOT / "tools" / "check-invariants.py")], "check-invariants")


def compile_debug() -> int:
    gradle = gradle_cmd()
    return run([gradle, ":app:compileDebugKotlin"], "compileDebugKotlin")


def compile_debug_java() -> int:
    gradle = gradle_cmd()
    return run([gradle, ":app:compileDebugJavaWithJavac"], "compileDebugJavaWithJavac")


def run_tests(pattern: str | None = None) -> int:
    gradle = gradle_cmd()
    cmd = [gradle, ":app:testDebugUnitTest"]
    if pattern:
        cmd.append(f"--tests=*{pattern}*")
    return run(cmd, f"testDebugUnitTest-{pattern or 'all'}")


def run_lint() -> int:
    gradle = gradle_cmd()
    return run([gradle, ":app:lintDebug"], "lintDebug")


def fast_mode(pattern: str | None, skip_android: bool) -> int:
    if check_invariants() != 0:
        return 1
    if skip_android:
        print("[verify] no relevant source changes; skipping Android build tasks")
        return 0
    if compile_debug() != 0:
        return 1
    if compile_debug_java() != 0:
        return 1
    if pattern and run_tests(pattern) != 0:
        return 1
    return 0


def full_mode() -> int:
    if check_invariants() != 0:
        return 1
    if compile_debug() != 0:
        return 1
    if compile_debug_java() != 0:
        return 1
    if run_tests(None) != 0:
        return 1
    if run_lint() != 0:
        return 1
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="A13 local verification")
    parser.add_argument("mode", choices=["fast", "full"], help="verification mode")
    parser.add_argument("--tests", default=None, help="targeted test class name substring")
    parser.add_argument("--changed", action="store_true", help="skip Android build tasks when only non-source files changed")
    args = parser.parse_args()

    skip_android = False
    if args.changed:
        skip_android = not relevant_to_android(changed_files())

    if args.mode == "fast":
        return fast_mode(args.tests, skip_android)
    return full_mode()


if __name__ == "__main__":
    sys.exit(main())
