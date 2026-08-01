#!/usr/bin/env python3
"""Static R8/ProGuard/dependency audit for A13.

This tool does not execute Gradle or R8. It reads the committed
`proguard-rules.pro` and `build.gradle.kts` to surface:

- keep rules
- minification/shrinkResources settings per build type
- `implementation` dependencies
- packaging excludes (resource filters)
- reflection/Xposed entry retention

Output is deterministic JSON/Markdown.
"""
from __future__ import annotations

import argparse
import json
import re
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Optional


@dataclass
class R8AuditReport:
    build_file: str
    proguard_file: str
    build_types: dict[str, dict[str, object]]
    keep_rules: list[str]
    implementation_dependencies: list[str]
    packaging_excludes: list[str]
    packaging_merges: list[str]
    xposed_entry_retained: bool
    hooker_retained: bool
    gateway_launcher_retained: bool

    def to_json(self, indent: int = 2) -> str:
        return json.dumps(asdict(self), indent=indent, ensure_ascii=False, sort_keys=True)

    def to_markdown(self) -> str:
        lines = ["# R8 / ProGuard Audit", ""]
        lines += ["## Build types", "| Type | minify | shrinkResources | debuggable |", "|---|---|---|---|"]
        for name, cfg in sorted(self.build_types.items()):
            lines.append(
                f"| {name} | {cfg.get('minify', '?')} | {cfg.get('shrink', '?')} | {cfg.get('debuggable', '?')} |"
            )
        lines += ["", "## Implementation dependencies", ""]
        for dep in self.implementation_dependencies:
            lines.append(f"- `{dep}`")
        lines += ["", "## Keep rules", ""]
        for rule in self.keep_rules:
            lines.append(f"- `{rule}`")
        lines += ["", "## Packaging excludes", ""]
        for e in self.packaging_excludes:
            lines.append(f"- `{e}`")
        lines += ["", "## Packaging merges", ""]
        for e in self.packaging_merges:
            lines.append(f"- `{e}`")
        lines += ["", "## Reflection / Xposed entry", "", f"- XposedModule retained: {self.xposed_entry_retained}"]
        lines.append(f"- Hooker retained: {self.hooker_retained}")
        lines.append(f"- GatewayLauncher retained: {self.gateway_launcher_retained}")
        return "\n".join(lines)


def _strip_comment(line: str) -> str:
    if "#" in line:
        return line[: line.index("#")].strip()
    return line.strip()


def _parse_proguard(path: Path) -> list[str]:
    rules: list[str] = []
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = _strip_comment(raw)
        if line:
            rules.append(line)
    return rules


def _parse_build_types(text: str) -> dict[str, dict[str, object]]:
    types: dict[str, dict[str, object]] = {}
    for m in re.finditer(r'getByName\("(.*?)"\)\s*\{|create\("(.*?)"\)\s*\{', text):
        name = m.group(1) or m.group(2)
        start = m.end()
        brace = 1
        i = start
        while brace > 0 and i < len(text):
            if text[i] == "{":
                brace += 1
            elif text[i] == "}":
                brace -= 1
            i += 1
        block = text[start:i]
        types[name] = {
            "minify": "isMinifyEnabled = true" in block,
            "shrink": "isShrinkResources = true" in block,
            "debuggable": "isDebuggable = true" in block,
            "crunchPngs": "isCrunchPngs = true" in block,
        }
    return types


def _parse_dependencies(text: str) -> list[str]:
    deps: list[str] = []
    for line in text.splitlines():
        m = re.search(r'implementation\(([^)]+)\)', line)
        if m:
            deps.append(m.group(1).strip().strip('"').strip('"').strip('`').strip('"'))
    return deps


def _parse_packaging(text: str) -> tuple[list[str], list[str]]:
    excludes: list[str] = []
    merges: list[str] = []
    # Merge rules
    for m in re.finditer(r'merges\s*\+?=\s*setOf\((.*?)\)', text, re.S):
        merges += [s.strip().strip('"') for s in m.group(1).split(",") if s.strip()]
    for m in re.finditer(r'merges\s*\+?=\s*"([^"]+)"', text):
        merges.append(m.group(1))
    # Exclude rules
    for m in re.finditer(r'excludes\s*\+?=\s*setOf\((.*?)\)', text, re.S):
        excludes += [s.strip().strip('"') for s in m.group(1).split(",") if s.strip()]
    for m in re.finditer(r'excludes\s*\+?=\s*"([^"]+)"', text):
        excludes.append(m.group(1))
    return excludes, merges


def audit(build_gradle: Path, proguard: Path) -> R8AuditReport:
    gradle_text = build_gradle.read_text(encoding="utf-8")
    proguard_rules = _parse_proguard(proguard)
    build_types = _parse_build_types(gradle_text)
    deps = _parse_dependencies(gradle_text)
    excludes, merges = _parse_packaging(gradle_text)

    return R8AuditReport(
        build_file=str(build_gradle),
        proguard_file=str(proguard),
        build_types=build_types,
        keep_rules=proguard_rules,
        implementation_dependencies=deps,
        packaging_excludes=excludes,
        packaging_merges=merges,
        xposed_entry_retained=any(
            "extends io.github.libxposed.api.XposedModule" in r for r in proguard_rules
        ),
        hooker_retained=any(
            "XposedInterface$Hooker" in r for r in proguard_rules
        ),
        gateway_launcher_retained=any(
            "tv.withaibuild.customiuizer.GateWayLauncher" in r for r in proguard_rules
        ),
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="R8 / ProGuard static audit")
    parser.add_argument("--build-gradle", type=Path, default=Path("app/build.gradle.kts"))
    parser.add_argument("--proguard", type=Path, default=Path("app/proguard-rules.pro"))
    parser.add_argument("--json", type=Path, default=None)
    parser.add_argument("--md", type=Path, default=None)
    args = parser.parse_args()

    result = audit(args.build_gradle, args.proguard)
    out = result.to_json()
    if args.json:
        args.json.write_text(out, encoding="utf-8")
    else:
        print(out)

    if args.md:
        args.md.write_text(result.to_markdown(), encoding="utf-8")


if __name__ == "__main__":
    main()
