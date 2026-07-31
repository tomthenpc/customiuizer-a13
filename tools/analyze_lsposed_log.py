#!/usr/bin/env python3
"""Offline LSPosed log analyzer for CustoMIUIzer A13.

Usage:
    python tools/analyze_lsposed_log.py log.txt
    python tools/analyze_lsposed_log.py logs/
    python tools/analyze_lsposed_log.py exported.zip
    python tools/analyze_lsposed_log.py exported.zip --format markdown --output build/log-analysis/k1

Input:
    - one or more .txt/.log files
    - one or more directories (recursively scanned for .txt/.log)
    - one or more .zip files (containing .txt/.log)

Does NOT call adb, su, logcat, network or any device command.
"""
from __future__ import annotations

import argparse
import io
import json
import os
import re
import sys
import zipfile
from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path
from typing import Iterable, Iterator, TextIO

PACKAGE = "tv.withaibuild.customiuizer.r13"
MODULE_PREFIX = "tv.withaibuild.customiuizer"

TARGET_PROCESSES = {
    "android",
    "system",
    "com.android.systemui",
    "com.miui.home",
    "com.mi.android.globallauncher",
    "com.android.settings",
    "com.miui.securitycenter",
    "com.android.settings:remote",
}

RE_EXCEPTION = re.compile(r"^(?:Caused by:\s+)?((?:[A-Za-z_$][\w$]*\.)*[A-Za-z_$][\w$]*(?:Exception|Error))(?::\s*(.*))?$")
RE_STACK = re.compile(r"^\s+at\s+([\w.]+(?:\$[\w]+)?)\.(<(?:cl)?init>|[\w$]+)\(([^:]+(?::\d+)?)\)")
RE_MODULE_AT = re.compile(r"\s+at\s+(tv\.withaibuild\.customiuizer\.[\w.]+)")
RE_PROCESS = re.compile(r"Loading modules for\s+(\S+)")
RE_A13 = re.compile(r"tv\.withaibuild\.customiuizer\.r13")
RE_VERSION = re.compile(r"CustoMIUIzer.*?\b(r\d+\.\d+(?:\.\d+)?|\d+\.\d+\.\d+)\b")
RE_TS = re.compile(r"^(\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}(?:\.\d{3})?(?:[+-]\d{2}:?\d{2})?)")
RE_SIMPLE_TS = re.compile(r"^(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d+)")
RE_FATAL = re.compile(r"FATAL EXCEPTION|AndroidRuntime|>>>\s*\S+\s*<<<")
RE_HOOK_DIAG = re.compile(r"HookDiagnostics")
RE_PREF_EMPTY = re.compile(r"Empty preferences!|EMPTY_PENDING")
RE_RECEIVER_FAIL = re.compile(r"(?:registerReceiver|unregisterReceiver).*?(?:failed|not registered)", re.I)
RE_STALE = re.compile(r"stale\s+.*receiver", re.I)
RE_DEXKIT = re.compile(r"DexKit")
RE_CLASS_NOT_FOUND = re.compile(r"ClassNotFound(?:Exception|Error)")
RE_NO_METHOD = re.compile(r"NoSuchMethod(?:Error|Exception)")
RE_NO_FIELD = re.compile(r"NoSuchField(?:Error|Exception)")
RE_INVOCATION = re.compile(r"InvocationTargetException")
RE_HOOK_FAILED = re.compile(r"Hook failed")

MAX_FINGERPRINTS = 5000


class LogProfile:
    def __init__(self) -> None:
        self.a13_marker = False
        self.module_version: str | None = None
        self.processes: set[str] = set()
        self.first_time: str | None = None
        self.last_time: str | None = None
        self.hook_diagnostics = 0
        self.preference_empty = 0
        self.receiver_fail = 0
        self.stale_receiver = 0
        self.class_not_found = 0
        self.no_such_method = 0
        self.no_such_field = 0
        self.invocation_target = 0
        self.dexkit = 0
        self.hook_failed = 0
        self.crashes: list[dict] = []
        self.fingerprints: Counter = Counter()
        self.fingerprint_overflow = 0
        self.source_classes: set[str] = set()
        self.top_fingerprints: list[tuple[str, int]] = []

    def record_exception(self, exc_type: str, frames: list[str], process: str) -> None:
        module_frames = [f for f in frames if f.startswith(MODULE_PREFIX)]
        if not module_frames:
            return
        self.source_classes.update(module_frames)
        first = module_frames[0]
        short = exc_type if len(exc_type) < 60 else exc_type[:60]
        key = f"{process or '?'} | {short} | {first}"
        if len(self.fingerprints) < MAX_FINGERPRINTS:
            self.fingerprints[key] += 1
        else:
            self.fingerprint_overflow += 1

    def bump_kind(self, line: str) -> None:
        if RE_PREF_EMPTY.search(line):
            self.preference_empty += 1
        if RE_RECEIVER_FAIL.search(line):
            self.receiver_fail += 1
        if RE_STALE.search(line):
            self.stale_receiver += 1
        if RE_CLASS_NOT_FOUND.search(line):
            self.class_not_found += 1
        if RE_NO_METHOD.search(line):
            self.no_such_method += 1
        if RE_NO_FIELD.search(line):
            self.no_such_field += 1
        if RE_INVOCATION.search(line):
            self.invocation_target += 1
        if RE_DEXKIT.search(line):
            self.dexkit += 1
        if RE_HOOK_FAILED.search(line):
            self.hook_failed += 1


def pick_process(line: str, current: str | None) -> str | None:
    m = RE_PROCESS.search(line)
    if m:
        return m.group(1).strip()
    return current


def record_time(line: str, profile: LogProfile) -> None:
    m = RE_TS.search(line) or RE_SIMPLE_TS.search(line)
    if m:
        ts = m.group(1)
        if profile.first_time is None:
            profile.first_time = ts
        profile.last_time = ts


def process_stream(stream: Iterable[str], profile: LogProfile) -> None:
    current_process: str | None = None
    exc_lines: list[str] = []
    exc_type: str | None = None
    exc_frames: list[str] = []
    crash_lines: list[str] | None = None
    crash_process: str | None = None

    for raw in stream:
        line = raw.rstrip("\n\r")
        record_time(line, profile)
        if RE_A13.search(line):
            profile.a13_marker = True
        m_version = RE_VERSION.search(line)
        if m_version:
            profile.module_version = m_version.group(1)

        current_process = pick_process(line, current_process)
        if current_process:
            profile.processes.add(current_process)

        profile.bump_kind(line)
        if RE_HOOK_DIAG.search(line):
            profile.hook_diagnostics += 1

        # exception block
        em = RE_EXCEPTION.search(line)
        if em:
            if exc_type is not None and exc_frames:
                profile.record_exception(exc_type, exc_frames, current_process or "")
            exc_type = em.group(1)
            exc_frames = []
            exc_lines = [line]
            continue

        if exc_type is not None:
            sm = RE_STACK.search(line)
            if sm:
                cls = sm.group(1)
                exc_frames.append(cls)
            elif line.strip() and not line.startswith("\t") and not line.startswith(" "):
                # end of exception block
                profile.record_exception(exc_type, exc_frames, current_process or "")
                exc_type = None
                exc_frames = []
            else:
                exc_lines.append(line)
                m = RE_MODULE_AT.search(line)
                if m:
                    exc_frames.append(m.group(1))

        # crash block (simplified: gather a window)
        if RE_FATAL.search(line):
            if crash_lines is not None and crash_lines:
                # flush previous crash if it contains our module
                _save_crash(crash_lines, profile, crash_process)
            crash_process = current_process
            crash_lines = [line]
            continue
        if crash_lines is not None:
            if len(crash_lines) < 120:
                crash_lines.append(line)
            else:
                _save_crash(crash_lines, profile, crash_process)
                crash_lines = None
                crash_process = None

    if exc_type is not None and exc_frames:
        profile.record_exception(exc_type, exc_frames, current_process or "")
    if crash_lines is not None and crash_lines:
        _save_crash(crash_lines, profile, crash_process)


def _save_crash(lines: list[str], profile: LogProfile, process: str | None) -> None:
    module_frames = []
    for ln in lines:
        if not process:
            m = RE_PROCESS.search(ln)
            if m:
                process = m.group(1)
        for sm in RE_STACK.finditer(ln):
            cls = sm.group(1)
            if cls.startswith(MODULE_PREFIX):
                module_frames.append(cls)
    if module_frames:
        profile.crashes.append({
            "process": process or "unknown",
            "module_frames": list(set(module_frames))[:20],
            "sample": lines[0][:160],
        })


def open_inputs(paths: list[Path]) -> Iterator[tuple[Path, TextIO]]:
    for p in paths:
        if p.is_dir():
            for child in sorted(p.rglob("*")):
                if child.suffix.lower() in (".txt", ".log"):
                    yield child, child.open("r", encoding="utf-8", errors="replace")
        elif p.suffix.lower() == ".zip":
            with zipfile.ZipFile(p, "r") as zf:
                for name in sorted(zf.namelist()):
                    if name.lower().endswith((".txt", ".log")):
                        data = zf.read(name)
                        yield Path(p.name) / name, io.StringIO(data.decode("utf-8", errors="replace"))
        elif p.is_file():
            yield p, p.open("r", encoding="utf-8", errors="replace")
        else:
            print(f"[warn] not a file, directory, or zip: {p}", file=sys.stderr)


def build_profile(paths: list[Path]) -> LogProfile:
    profile = LogProfile()
    for source, handle in open_inputs(paths):
        try:
            process_stream(handle, profile)
        finally:
            handle.close()
    profile.top_fingerprints = profile.fingerprints.most_common(20)
    return profile


def p0_count(profile: LogProfile) -> int:
    bad = set(["android", "com.android.systemui", "com.miui.home", "com.mi.android.globallauncher"])
    return sum(
        1 for c in profile.crashes if c["process"] in bad
    ) + profile.hook_failed


def p1_count(profile: LogProfile) -> int:
    return (
        sum(profile.fingerprints.values())
        + profile.preference_empty
        + profile.receiver_fail
        + profile.stale_receiver
        + profile.dexkit
    )


def text_summary(profile: LogProfile) -> str:
    lines = [
        "=== A13 LSPosed Log Summary ===",
        f"A13 marker:      {'yes' if profile.a13_marker else 'NO'}",
        f"Module version:  {profile.module_version or 'unknown'}",
        f"Processes:       {len(profile.processes)}",
        f"Time range:      {profile.first_time or 'N/A'} -> {profile.last_time or 'N/A'}",
        f"P0:              {p0_count(profile)}",
        f"P1:              {p1_count(profile)}",
        f"Hook diagnostics: {profile.hook_diagnostics}",
        f"Empty prefs:     {profile.preference_empty}",
        f"Receiver fails:  {profile.receiver_fail}",
        f"Stale receivers: {profile.stale_receiver}",
        f"ClassNotFound:   {profile.class_not_found}",
        f"NoSuchMethod:    {profile.no_such_method}",
        f"NoSuchField:     {profile.no_such_field}",
        f"InvocationTarget: {profile.invocation_target}",
        f"DexKit hits:     {profile.dexkit}",
        f"Hook failed:     {profile.hook_failed}",
        f"Crashes:         {len(profile.crashes)}",
        f"Unique fingerprints: {len(profile.fingerprints)} ({profile.fingerprint_overflow} overflow)",
        "",
        "Top 10 fingerprints:",
    ]
    for fp, cnt in profile.top_fingerprints[:10]:
        lines.append(f"  {cnt:5d}  {fp}")
    if profile.crashes:
        lines.append("")
        lines.append("Crashes with module frames:")
        for c in profile.crashes[:5]:
            lines.append(f"  {c['process']}: {', '.join(c['module_frames'][:3])}")
    if profile.source_classes:
        lines.append("")
        lines.append("Likely source classes (sample):")
        for cls in sorted(profile.source_classes)[:15]:
            lines.append(f"  {cls}")
    return "\n".join(lines)


def markdown_summary(profile: LogProfile) -> str:
    lines = [
        "# A13 LSPosed Log Summary",
        "",
        "| Item | Value |",
        "|---|---|",
        f"| A13 marker | {'yes' if profile.a13_marker else 'NO'} |",
        f"| Module version | {profile.module_version or 'unknown'} |",
        f"| Processes | {len(profile.processes)} |",
        f"| Time range | {profile.first_time or 'N/A'} -> {profile.last_time or 'N/A'} |",
        f"| P0 | {p0_count(profile)} |",
        f"| P1 | {p1_count(profile)} |",
        f"| Hook diagnostics | {profile.hook_diagnostics} |",
        f"| Empty prefs | {profile.preference_empty} |",
        f"| Receiver fails | {profile.receiver_fail} |",
        f"| Stale receivers | {profile.stale_receiver} |",
        f"| ClassNotFound | {profile.class_not_found} |",
        f"| NoSuchMethod | {profile.no_such_method} |",
        f"| NoSuchField | {profile.no_such_field} |",
        f"| InvocationTarget | {profile.invocation_target} |",
        f"| DexKit hits | {profile.dexkit} |",
        f"| Hook failed | {profile.hook_failed} |",
        f"| Crashes | {len(profile.crashes)} |",
        f"| Unique fingerprints | {len(profile.fingerprints)} ({profile.fingerprint_overflow} overflow) |",
        "",
        "## Top 10 fingerprints",
        "",
        "| Count | Fingerprint |",
        "|---|---|",
    ]
    for fp, cnt in profile.top_fingerprints[:10]:
        lines.append(f"| {cnt} | `{fp}` |")
    if profile.crashes:
        lines += ["", "## Crashes with module frames", ""]
        for c in profile.crashes[:5]:
            frames = ", ".join(f"`{f}`" for f in c["module_frames"][:3])
            lines.append(f"- **{c['process']}**: {frames}")
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description="A13 offline LSPosed log analyzer")
    parser.add_argument("inputs", nargs="+", help="log files, directories, or zip files")
    parser.add_argument("--format", choices=["text", "markdown", "json"], default="text")
    parser.add_argument("--output", default="build/log-analysis", help="output directory for markdown/json")
    args = parser.parse_args()

    paths = [Path(p) for p in args.inputs]
    profile = build_profile(paths)

    if args.format == "text":
        print(text_summary(profile))
    else:
        out_dir = Path(args.output)
        out_dir.mkdir(parents=True, exist_ok=True)
        if args.format == "markdown":
            out = out_dir / "summary.md"
            out.write_text(markdown_summary(profile), encoding="utf-8")
            print(f"[analyze] wrote {out}")
        else:
            out = out_dir / "summary.json"
            out.write_text(
                json.dumps(
                    {
                        "a13_marker": profile.a13_marker,
                        "module_version": profile.module_version,
                        "processes": sorted(profile.processes),
                        "first_time": profile.first_time,
                        "last_time": profile.last_time,
                        "p0": p0_count(profile),
                        "p1": p1_count(profile),
                        "counters": {
                            "hook_diagnostics": profile.hook_diagnostics,
                            "preference_empty": profile.preference_empty,
                            "receiver_fail": profile.receiver_fail,
                            "stale_receiver": profile.stale_receiver,
                            "class_not_found": profile.class_not_found,
                            "no_such_method": profile.no_such_method,
                            "no_such_field": profile.no_such_field,
                            "invocation_target": profile.invocation_target,
                            "dexkit": profile.dexkit,
                            "hook_failed": profile.hook_failed,
                            "crashes": len(profile.crashes),
                        },
                        "fingerprints": dict(profile.top_fingerprints),
                        "crashes": profile.crashes,
                        "source_classes": sorted(profile.source_classes)[:100],
                    },
                    ensure_ascii=False,
                    indent=2,
                ),
                encoding="utf-8",
            )
            print(f"[analyze] wrote {out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
