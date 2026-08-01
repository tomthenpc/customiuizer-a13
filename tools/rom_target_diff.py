#!/usr/bin/env python3
"""Compare two ROM samples (APK/JAR) and report class/member differences.

Uses `javap` when available, otherwise `apkanalyzer` or a plain zip entry
listing. No files are uploaded or submitted.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


@dataclass
class Sample:
    path: Path
    sha256: str
    sample_type: str
    classes: dict[str, dict[str, Any]] = field(default_factory=dict)
    warnings: list[str] = field(default_factory=list)
    fallback_entries: set[str] = field(default_factory=set)


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


def sample_type(path: Path) -> str:
    ext = path.suffix.lower()
    if ext == ".apk":
        return "APK"
    if ext == ".jar":
        return "JAR"
    return "unknown"


def tool_available(name: str) -> bool:
    return shutil.which(name) is not None


def zip_entries(path: Path) -> set[str]:
    try:
        with zipfile.ZipFile(path) as zf:
            return set(zf.namelist())
    except (zipfile.BadZipFile, OSError):
        return set()


def class_names_from_zip(path: Path) -> list[str]:
    classes = []
    try:
        with zipfile.ZipFile(path) as zf:
            for n in zf.namelist():
                if n.endswith(".class") and not n.startswith("META-INF"):
                    classes.append(n[:-6].replace("/", "."))
    except (zipfile.BadZipFile, OSError):
        pass
    return classes


def parse_javap(output: str) -> dict[str, Any]:
    members = {"methods": set(), "fields": set()}
    # Skip the class declaration line and braces
    in_class = False
    for line in output.splitlines():
        line = line.rstrip()
        if "{" in line:
            in_class = True
            continue
        if in_class and line.strip() == "}":
            break
        if not in_class:
            continue
        # Strip modifiers and types/signature tail heuristically
        stripped = re.sub(r"^\s*(?:public|private|protected|static|final|abstract|native|synchronized|transient|volatile|strictfp)*\s*", "", line)
        stripped = re.sub(r"\s+;\s*$", "", stripped).strip()
        if "(" in stripped and ")" in stripped:
            # method
            m = re.match(r"^(?:[\w\[\]$_.<>]+\s+)*([\w$<>]+)\((.*)\)", stripped)
            if m:
                members["methods"].add(f"{m.group(1)}({m.group(2).strip()})")
        elif stripped and not stripped.startswith("class ") and not stripped.startswith("interface "):
            # field: last token is field name
            parts = stripped.split()
            if parts:
                members["fields"].add(parts[-1])
    return members


def javap_class_members(path: Path, class_name: str) -> dict[str, Any]:
    try:
        out = subprocess.check_output(
            ["javap", "-p", "-cp", str(path), class_name],
            text=True,
            stderr=subprocess.DEVNULL,
            timeout=30,
        )
        return parse_javap(out)
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired, OSError):
        return {"methods": set(), "fields": set()}


def javap_load(sample: Sample) -> None:
    class_names = class_names_from_zip(sample.path)
    if not class_names:
        sample.warnings.append("No .class entries found; javap analysis unavailable")
        return
    for i, c in enumerate(class_names):
        if i % 500 == 0 and i > 0:
            sample.warnings.append(f"Analyzed {i}/{len(class_names)} classes with javap")
        sample.classes[c] = javap_class_members(sample.path, c)


def apkanalyzer_dex_classes(path: Path) -> list[str]:
    # apkanalyzer dex packages <apk> prints package list, not class list.
    # We use the lighter 'dex list' output if the installed version supports it.
    try:
        out = subprocess.check_output(
            ["apkanalyzer", "dex", "list", str(path)],
            text=True,
            stderr=subprocess.DEVNULL,
            timeout=120,
        )
        return [line.strip().replace("/", ".") for line in out.splitlines() if line.strip()]
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired, OSError):
        return []


def apkanalyzer_load(sample: Sample) -> None:
    class_names = apkanalyzer_dex_classes(sample.path)
    if not class_names:
        sample.warnings.append("apkanalyzer dex list produced no output or is unavailable")
        return
    for c in class_names:
        # apkanalyzer lists class names; we cannot get members without javap on classes.dex
        sample.classes[c] = {"methods": set(), "fields": set()}


def zip_load(sample: Sample) -> None:
    entries = zip_entries(sample.path)
    sample.fallback_entries = entries
    sample.warnings.append("Used zip entry listing as fallback (no javap/apkanalyzer)")


def load_sample(path: Path) -> Sample:
    s = Sample(path=path, sha256=sha256(path), sample_type=sample_type(path))
    if s.sample_type not in ("APK", "JAR"):
        s.warnings.append(f"Unknown sample type for {path}; treating as zip archive")

    if tool_available("javap") and s.sample_type in ("JAR", "unknown"):
        javap_load(s)
    elif tool_available("apkanalyzer") and s.sample_type == "APK":
        apkanalyzer_load(s)
    else:
        zip_load(s)
        # Still try javap if jar and not found? already covered
    return s


def diff_classes(left: Sample, right: Sample) -> dict[str, Any]:
    if left.fallback_entries or right.fallback_entries:
        # fall-back to zip entry diff
        added = right.fallback_entries - left.fallback_entries
        removed = left.fallback_entries - right.fallback_entries
        common = left.fallback_entries & right.fallback_entries
        return {
            "mode": "zip_fallback",
            "added_entries": sorted(added),
            "removed_entries": sorted(removed),
            "common_entries_count": len(common),
        }

    lclasses = set(left.classes.keys())
    rclasses = set(right.classes.keys())
    added_classes = sorted(rclasses - lclasses)
    removed_classes = sorted(lclasses - rclasses)
    common = lclasses & rclasses
    changed: list[dict[str, Any]] = []
    identical: list[str] = []
    for c in sorted(common):
        lm = left.classes[c]
        rm = right.classes[c]
        added_methods = sorted(rm["methods"] - lm["methods"])
        removed_methods = sorted(lm["methods"] - rm["methods"])
        added_fields = sorted(rm["fields"] - lm["fields"])
        removed_fields = sorted(lm["fields"] - rm["fields"])
        if added_methods or removed_methods or added_fields or removed_fields:
            changed.append({
                "class": c,
                "added_methods": added_methods,
                "removed_methods": removed_methods,
                "added_fields": added_fields,
                "removed_fields": removed_fields,
            })
        else:
            identical.append(c)

    return {
        "mode": "class_member",
        "added_classes": added_classes,
        "removed_classes": removed_classes,
        "changed_classes": changed,
        "identical_classes": identical,
        "total_common_classes": len(common),
    }


def write_markdown(report: dict[str, Any], out: Path, left: Sample, right: Sample) -> None:
    lines = [
        "# ROM Target Diff",
        "",
        f"- Left: `{left.path}`",
        f"  - SHA-256: `{left.sha256}`",
        f"  - Type: `{left.sample_type}`",
        f"- Right: `{right.path}`",
        f"  - SHA-256: `{right.sha256}`",
        f"  - Type: `{right.sample_type}`",
        "",
    ]

    for s in (left, right):
        if s.warnings:
            lines.append(f"## {'Left' if s is left else 'Right'} warnings")
            for w in s.warnings:
                lines.append(f"- {w}")
            lines.append("")

    if report["mode"] == "zip_fallback":
        lines += [
            "## Zip entry diff (fallback)",
            "",
            f"- Added entries: {len(report['added_entries'])}",
            f"- Removed entries: {len(report['removed_entries'])}",
            f"- Common entries: {report['common_entries_count']}",
            "",
            "### Added entries",
            "" if not report["added_entries"] else "\n".join(f"- `{e}`" for e in report["added_entries"]),
            "",
            "### Removed entries",
            "" if not report["removed_entries"] else "\n".join(f"- `{e}`" for e in report["removed_entries"]),
            "",
        ]
    else:
        lines += [
            "## Class/member diff",
            "",
            f"- Classes added in right: {len(report['added_classes'])}",
            f"- Classes removed in right: {len(report['removed_classes'])}",
            f"- Classes changed: {len(report['changed_classes'])}",
            f"- Identical classes: {len(report['identical_classes'])}",
            "",
            "### Added classes",
            "" if not report["added_classes"] else "\n".join(f"- `{c}`" for c in report["added_classes"]),
            "",
            "### Removed classes",
            "" if not report["removed_classes"] else "\n".join(f"- `{c}`" for c in report["removed_classes"]),
            "",
            "### Changed classes",
        ]
        for ch in report["changed_classes"]:
            lines.append(f"\n#### `{ch['class']}`")
            for k in ("added_methods", "removed_methods", "added_fields", "removed_fields"):
                if ch[k]:
                    lines.append(f"\n**{k.replace('_', ' ').title()}:**")
                    for it in ch[k]:
                        lines.append(f"- `{it}`")
        lines.append("")

    out.write_text("\n".join(lines), encoding="utf-8")


def write_json(report: dict[str, Any], out: Path, left: Sample, right: Sample) -> None:
    data = {
        "left": {"path": str(left.path), "sha256": left.sha256, "type": left.sample_type, "warnings": left.warnings},
        "right": {"path": str(right.path), "sha256": right.sha256, "type": right.sample_type, "warnings": right.warnings},
        "diff": report,
    }
    out.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Diff two ROM samples without uploading them.")
    parser.add_argument("left", type=Path, help="Path to the first APK/JAR sample.")
    parser.add_argument("right", type=Path, help="Path to the second APK/JAR sample.")
    parser.add_argument("-o", "--out", type=Path, default=Path("rom-target-diff.md"), help="Output file.")
    parser.add_argument("-f", "--format", choices=("md", "json"), default="md", help="Output format.")
    args = parser.parse_args(argv)

    if not args.left.exists():
        print(f"ERROR: left sample not found: {args.left}", file=sys.stderr)
        return 1
    if not args.right.exists():
        print(f"ERROR: right sample not found: {args.right}", file=sys.stderr)
        return 1

    left = load_sample(args.left)
    right = load_sample(args.right)
    report = diff_classes(left, right)

    if args.format == "md":
        write_markdown(report, args.out, left, right)
    else:
        write_json(report, args.out, left, right)

    print(f"Wrote diff report: {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
