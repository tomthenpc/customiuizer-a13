#!/usr/bin/env python3
"""Raw-condition inventory for SystemUI startup gating.

This tool does NOT decide whether the gate is correct; it only records the
conditions that exist in:

- SystemUiInstaller.install()
- SystemUiInstaller.hasAnySystemUiStartupFeature()
- SystemUiInstaller.hasAnyGlobalAction()
- FeatureCatalog (SystemUI scoped specs)

Output:
- docs/audit/A13_SYSTEMUI_GATE_INVENTORY.json
- docs/audit/A13_SYSTEMUI_GATE_INVENTORY.md
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parent.parent

# Shared Java/Kotlin preference accessors.
ACCESSOR_PATTERNS = [
    ("getBoolean", re.compile(r'(?:prefs|mPrefs|MainModule\.mPrefs)\.getBoolean\s*\(\s*"([^"]+)"\s*\)'), None),
    ("getBoolean", re.compile(r'(?:prefs|mPrefs|MainModule\.mPrefs)\.getBoolean\s*\(\s*"([^"]+)"\s*,\s*(true|false)\s*\)'), 2),
    ("getInt", re.compile(r'(?:prefs|mPrefs|MainModule\.mPrefs)\.getInt\s*\(\s*"([^"]+)"\s*,\s*(\d+)\s*\)'), 2),
    ("getStringAsInt", re.compile(r'(?:prefs|mPrefs|MainModule\.mPrefs)\.getStringAsInt\s*\(\s*"([^"]+)"\s*,\s*(\d+)\s*\)'), 2),
    ("getString", re.compile(r'(?:prefs|mPrefs|MainModule\.mPrefs)\.getString\s*\(\s*"([^"]+)"\s*,\s*"([^"]*)"\s*\)'), 2),
    ("getStringSet", re.compile(r'(?:prefs|mPrefs|MainModule\.mPrefs)\.getStringSet\s*\(\s*"([^"]+)"\s*\)'), None),
    ("getStringSet", re.compile(r'(?:prefs|mPrefs|MainModule\.mPrefs)\.getStringSet\s*\(\s*"([^"]+)"\s*,\s*([^)]+)\s*\)'), 2),
]

# Comparison and logical operators.
OPERATORS = {
    "||": "OR",
    "&&": "AND",
    "!": "NOT",
    "==": "EQ",
    "!=": "NE",
    ">": "GT",
    "<": "LT",
    ">=": "GE",
    "<=": "LE",
    "isEmpty": "isEmpty",
    "!isEmpty": "notIsEmpty",
}


@dataclass
class ConditionEntry:
    id: str
    source_file: str
    source_method: str
    start_line: int
    end_line: int
    raw_expression: str
    normalized_expression: str = ""
    preference_keys: list[str] = field(default_factory=list)
    accessors: list[str] = field(default_factory=list)
    default_values: list[Any] = field(default_factory=list)
    comparators: list[str] = field(default_factory=list)
    boolean_operators: list[str] = field(default_factory=list)
    feature_id: str = ""
    install_target: str = ""
    phase: str = "UNKNOWN"
    parse_status: str = "PARSED"


@dataclass
class Inventory:
    install_conditions: list[ConditionEntry] = field(default_factory=list)
    startup_gate_conditions: list[ConditionEntry] = field(default_factory=list)
    global_action_rules: list[ConditionEntry] = field(default_factory=list)
    feature_dispatch_calls: list[ConditionEntry] = field(default_factory=list)
    feature_catalog_gates: list[ConditionEntry] = field(default_factory=list)
    resource_phase_conditions: list[ConditionEntry] = field(default_factory=list)
    restart_guard: list[ConditionEntry] = field(default_factory=list)
    unparsed: list[ConditionEntry] = field(default_factory=list)


def strip_comments(text: str) -> str:
    """Remove line and block comments while preserving string literals."""
    out: list[str] = []
    i = 0
    n = len(text)
    in_string: str | None = None
    while i < n:
        ch = text[i]
        if in_string:
            if ch == in_string and text[i - 1] != "\\":
                in_string = None
            out.append(ch)
            i += 1
            continue
        if ch in ('"', "'", "`"):
            in_string = ch
            out.append(ch)
            i += 1
            continue
        if ch == "/" and i + 1 < n:
            if text[i + 1] == "/":
                while i < n and text[i] != "\n":
                    i += 1
                continue
            if text[i + 1] == "*":
                i += 2
                while i < n - 1:
                    if text[i] == "*" and text[i + 1] == "/":
                        i += 2
                        break
                    i += 1
                continue
        out.append(ch)
        i += 1
    return "".join(out)


def find_block_end(text: str, start: int) -> int:
    """Return the index of the matching brace starting at [start]."""
    depth = 0
    in_string: str | None = None
    for i in range(start, len(text)):
        ch = text[i]
        if in_string:
            if ch == in_string and text[i - 1] != "\\":
                in_string = None
            continue
        if ch in ('"', "'", "`"):
            in_string = ch
            continue
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return i
    return -1


def get_line_number(text: str, offset: int) -> int:
    return text[:offset].count("\n") + 1


def find_method_body(text: str, signature: str) -> str | None:
    """Locate a method by its signature and return its body including braces."""
    pattern = re.compile(re.escape(signature) + r"\s*\([^)]*\)\s*\{", re.S)
    match = pattern.search(text)
    if not match:
        return None
    open_brace = text.find("{", match.end() - 1)
    close_brace = find_block_end(text, open_brace)
    if close_brace < 0:
        return None
    return text[open_brace:close_brace + 1]


def extract_top_level_ifs(body: str, source_file: str, source_method: str, phase_prefix: str) -> list[ConditionEntry]:
    """Extract top-level `if (condition)` blocks from a method body.

    The scan is string/comment aware so that `if` tokens inside string literals
    or comments are not treated as real conditions.
    """
    results: list[ConditionEntry] = []
    text = strip_comments(body)
    counter = 0
    in_string: str | None = None
    i = 0
    n = len(text)
    while i < n:
        ch = text[i]
        if in_string:
            if ch == in_string and text[i - 1] != "\\":
                in_string = None
            i += 1
            continue
        if ch in ('"', "'", "`"):
            in_string = ch
            i += 1
            continue
        if text.startswith("if", i):
            # Make sure it is a real `if` keyword, not an identifier suffix.
            before = i - 1
            if before >= 0 and (text[before].isalnum() or text[before] == "_"):
                i += 1
                continue
            after_if = i + 2
            # Skip whitespace up to opening paren.
            while after_if < n and text[after_if] in " \t\r\n":
                after_if += 1
            if after_if >= n or text[after_if] != "(":
                i += 2
                continue

            cond_start = after_if + 1
            paren_depth = 1
            cond_end = -1
            inner_string: str | None = None
            for j in range(cond_start, n):
                c = text[j]
                if inner_string:
                    if c == inner_string and text[j - 1] != "\\":
                        inner_string = None
                    continue
                if c in ('"', "'", "`"):
                    inner_string = c
                    continue
                if c == "(":
                    paren_depth += 1
                elif c == ")":
                    paren_depth -= 1
                    if paren_depth == 0:
                        cond_end = j
                        break
            if cond_end < 0:
                break

            condition = text[cond_start:cond_end].strip()
            end_line = get_line_number(text, cond_end)
            start_line = get_line_number(text, i)

            counter += 1
            entry = ConditionEntry(
                id=f"{source_method}_if_{counter}",
                source_file=source_file,
                source_method=source_method,
                start_line=start_line,
                end_line=end_line,
                raw_expression=condition,
                phase=phase_prefix,
            )
            parse_condition(entry)

            # Look for an installById call in the following statement/block.
            after = text[cond_end:]
            install_match = re.search(r'FeatureDispatcher\s*\.\s*installById\s*\(\s*"([^"]+)"\s*(?:,\s*[^)]+)?\)', after, re.S)
            if install_match:
                entry.feature_id = install_match.group(1)
                entry.install_target = "FeatureDispatcher"

            results.append(entry)
            i = cond_end
        else:
            i += 1
    return results


def parse_condition(entry: ConditionEntry) -> None:
    """Best-effort parse of a Java/Kotlin boolean condition."""
    expr = entry.raw_expression
    entry.normalized_expression = expr
    entry.parse_status = "PARSED"

    # Extract accessors and default values, keeping source order.
    matches: list[tuple[int, str, re.Match]] = []
    for accessor_name, pattern, default_group in ACCESSOR_PATTERNS:
        for match in pattern.finditer(expr):
            matches.append((match.start(), accessor_name, match, default_group))
    matches.sort(key=lambda x: x[0])

    for _, accessor_name, match, default_group in matches:
        key = match.group(1)
        entry.preference_keys.append(key)
        entry.accessors.append(accessor_name)
        if default_group is not None:
            raw = match.group(default_group)
            if raw == "true":
                entry.default_values.append(True)
            elif raw == "false":
                entry.default_values.append(False)
            else:
                try:
                    entry.default_values.append(int(raw))
                except ValueError:
                    entry.default_values.append(raw)
        else:
            entry.default_values.append(None)

    # Record boolean / logical operators.
    if re.search(r'(?<![=!])!(?![=])', expr):
        entry.boolean_operators.append("NOT")
    if re.search(r'(?<!&)&&(?!&)', expr):
        entry.boolean_operators.append("AND")
    if re.search(r'(?<!\|)\|\|(?!\|)', expr):
        entry.boolean_operators.append("OR")
    if re.search(r'\bisEmpty\s*\(', expr):
        entry.boolean_operators.append("isEmpty")
    if re.search(r'!\s*\w+\.\s*isEmpty\s*\(', expr):
        entry.boolean_operators.append("notIsEmpty")

    # Record comparison operators using a single regex (longest matches first).
    for match in re.finditer(r'(?:>=|<=|==|!=|>|<)', expr):
        entry.comparators.append(match.group(0))

    if not entry.preference_keys and has_unparsed_tokens(expr):
        entry.parse_status = "UNPARSED"
    elif has_unparsed_method_call(expr):
        # Known accessor(s) found but the expression still contains unmodelled
        # preference method calls -> best-effort only.
        entry.parse_status = "PARTIAL"


def has_unparsed_tokens(expr: str) -> bool:
    """Returns True when the expression contains a PrefMap reference but no known accessor."""
    if not re.search(r'(?:prefs|mPrefs|MainModule\.mPrefs)', expr):
        return False
    return not bool(re.search(
        r'(?:prefs|mPrefs|MainModule\.mPrefs)\.(?:getBoolean|getInt|getStringAsInt|getString|getStringSet)\s*\(',
        expr,
    ))


def has_unparsed_method_call(expr: str) -> bool:
    """Returns True when an unmodelled PrefMap method call remains in the expression."""
    for match in re.finditer(r'(?:prefs|mPrefs|MainModule\.mPrefs)\.(\w+)\s*\(', expr):
        if match.group(1) not in ("getBoolean", "getInt", "getStringAsInt", "getString", "getStringSet"):
            return True
    return False


def find_install_by_id_calls(body: str, source_file: str, source_method: str, phase: str) -> list[ConditionEntry]:
    results: list[ConditionEntry] = []
    pattern = re.compile(r'FeatureDispatcher\s*\.\s*installById\s*\(\s*"([^"]+)"\s*(?:,\s*[^)]+)?\)', re.S)
    for counter, match in enumerate(pattern.finditer(body), start=1):
        start_line = get_line_number(body, match.start())
        end_line = get_line_number(body, match.end())
        results.append(ConditionEntry(
            id=f"{source_method}_installById_{counter}",
            source_file=source_file,
            source_method=source_method,
            start_line=start_line,
            end_line=end_line,
            raw_expression=match.group(0),
            normalized_expression=match.group(0),
            feature_id=match.group(1),
            install_target="FeatureDispatcher",
            phase=phase,
        ))
    return results


def parse_feature_catalog(kt_path: Path) -> list[ConditionEntry]:
    """Extract FeatureSpec objects for SystemUI scope from FeatureCatalog.kt."""
    text = strip_comments(kt_path.read_text(encoding="utf-8"))
    results: list[ConditionEntry] = []

    # Match FeatureSpec(...) including trailing , or ) with balanced parens.
    pattern = re.compile(r'FeatureSpec\s*\(')
    counter = 0
    for match in pattern.finditer(text):
        open_p = match.end() - 1
        close_p = find_matching_paren(text, open_p)
        if close_p < 0:
            continue
        spec_text = text[match.start():close_p + 1]
        counter += 1

        # Extract id, processScope, and isEnabled lambda.
        id_match = re.search(r'\bid\s*=\s*"([^"]+)"', spec_text)
        scope_match = re.search(r'processScope\s*=\s*ProcessScope\.([A-Z_]+)', spec_text)
        enabled_match = re.search(r'isEnabled\s*=\s*\{\s*([^}]+)\s*\}', spec_text, re.S)

        if not id_match:
            continue

        feature_id = id_match.group(1)
        scope = scope_match.group(1) if scope_match else "UNKNOWN"

        if scope != "SYSTEM_UI":
            continue

        start_line = get_line_number(text, match.start())
        end_line = get_line_number(text, close_p)
        raw = enabled_match.group(1).strip() if enabled_match else ""

        entry = ConditionEntry(
            id=f"FeatureCatalog_{feature_id}",
            source_file="app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt",
            source_method="FeatureCatalog",
            start_line=start_line,
            end_line=end_line,
            raw_expression=spec_text,
            normalized_expression=raw,
            feature_id=feature_id,
            install_target="FeatureDispatcher",
            phase="FEATURE_DISPATCHER_INTERNAL_GATE",
        )

        if not raw:
            entry.parse_status = "UNPARSED"
        else:
            parse_condition(entry)
            if entry.parse_status == "UNPARSED" and entry.preference_keys:
                entry.parse_status = "PARTIAL"

        results.append(entry)

    return results


def find_matching_paren(text: str, open_idx: int) -> int:
    """Find the matching closing paren for an opening paren at open_idx."""
    depth = 0
    in_string: str | None = None
    for i in range(open_idx, len(text)):
        ch = text[i]
        if in_string:
            if ch == in_string and text[i - 1] != "\\":
                in_string = None
            continue
        if ch in ('"', "'", "`"):
            in_string = ch
            continue
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return i
    return -1


def inventory_from_sources(repo_root: Path) -> Inventory:
    installer = repo_root / "app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java"
    catalog = repo_root / "app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt"

    installer_text = strip_comments(installer.read_text(encoding="utf-8"))

    inv = Inventory()

    # Install conditions.
    install_body = find_method_body(installer_text, "public static void install")
    if install_body:
        inv.install_conditions.extend(extract_top_level_ifs(install_body, str(installer), "install", "PACKAGE_READY_RESOURCE"))
        inv.feature_dispatch_calls.extend(find_install_by_id_calls(install_body, str(installer), "install", "FEATURE_DISPATCHER_INTERNAL_GATE"))
    else:
        inv.unparsed.append(ConditionEntry(
            id="install_body_not_found",
            source_file=str(installer),
            source_method="install",
            start_line=0,
            end_line=0,
            raw_expression="",
            parse_status="UNPARSED",
        ))

    # Startup gate conditions.
    gate_body = find_method_body(installer_text, "public static boolean hasAnySystemUiStartupFeature")
    if gate_body:
        inv.startup_gate_conditions.extend(extract_top_level_ifs(gate_body, str(installer), "hasAnySystemUiStartupFeature", "STARTUP_GATE"))
    else:
        inv.unparsed.append(ConditionEntry(
            id="startup_gate_body_not_found",
            source_file=str(installer),
            source_method="hasAnySystemUiStartupFeature",
            start_line=0,
            end_line=0,
            raw_expression="",
            parse_status="UNPARSED",
        ))

    # Global action rule.
    ga_body = find_method_body(installer_text, "static boolean hasAnyGlobalAction")
    if ga_body:
        inv.global_action_rules.extend(extract_top_level_ifs(ga_body, str(installer), "hasAnyGlobalAction", "GLOBAL_ACTION_DYNAMIC_SCAN"))
    else:
        inv.unparsed.append(ConditionEntry(
            id="global_action_body_not_found",
            source_file=str(installer),
            source_method="hasAnyGlobalAction",
            start_line=0,
            end_line=0,
            raw_expression="",
            parse_status="UNPARSED",
        ))

    # Restart guard. The predicate is a single return expression, not an if.
    guard_body = find_method_body(installer_text, "static boolean isWithinSystemUiRestartGuard")
    if guard_body:
        return_match = re.search(r'return\s+(.+?);', guard_body, re.S)
        if return_match:
            raw = return_match.group(1).strip()
            entry = ConditionEntry(
                id="isWithinSystemUiRestartGuard_return",
                source_file=str(installer),
                source_method="isWithinSystemUiRestartGuard",
                start_line=get_line_number(installer_text, installer_text.find("isWithinSystemUiRestartGuard")),
                end_line=get_line_number(installer_text, installer_text.find("isWithinSystemUiRestartGuard")),
                raw_expression=raw,
                phase="RESTART_GUARD",
            )
            parse_condition(entry)
            inv.restart_guard.append(entry)
        else:
            inv.unparsed.append(ConditionEntry(
                id="restart_guard_return_not_found",
                source_file=str(installer),
                source_method="isWithinSystemUiRestartGuard",
                start_line=0,
                end_line=0,
                raw_expression=guard_body,
                parse_status="UNPARSED",
            ))
    else:
        inv.unparsed.append(ConditionEntry(
            id="restart_guard_body_not_found",
            source_file=str(installer),
            source_method="isWithinSystemUiRestartGuard",
            start_line=0,
            end_line=0,
            raw_expression="",
            parse_status="UNPARSED",
        ))

    # Feature catalog gates.
    inv.feature_catalog_gates.extend(parse_feature_catalog(catalog))

    # Resource phase conditions are all conditions in install() that occur before
    # the restart guard (we conservatively label top-level install ifs that only
    # call resource hooks or installById with resource identifiers as such).
    for cond in inv.install_conditions:
        if cond.phase == "PACKAGE_READY_RESOURCE" and (
            "Res" in cond.raw_expression or "Resource" in cond.raw_expression or cond.feature_id
        ):
            cond.phase = "PACKAGE_READY_RESOURCE"

    return inv


def entry_to_dict(entry: ConditionEntry) -> dict[str, Any]:
    return {
        "id": entry.id,
        "source_file": entry.source_file,
        "source_method": entry.source_method,
        "start_line": entry.start_line,
        "end_line": entry.end_line,
        "raw_expression": entry.raw_expression,
        "normalized_expression": entry.normalized_expression,
        "preference_keys": entry.preference_keys,
        "accessors": entry.accessors,
        "default_values": entry.default_values,
        "comparators": entry.comparators,
        "boolean_operators": entry.boolean_operators,
        "feature_id": entry.feature_id,
        "install_target": entry.install_target,
        "phase": entry.phase,
        "parse_status": entry.parse_status,
    }


def to_json(inv: Inventory) -> dict[str, Any]:
    return {
        "INSTALL_CONDITIONS": [entry_to_dict(e) for e in inv.install_conditions],
        "STARTUP_GATE_CONDITIONS": [entry_to_dict(e) for e in inv.startup_gate_conditions],
        "GLOBAL_ACTION_DOMAIN_RULES": [entry_to_dict(e) for e in inv.global_action_rules],
        "FEATURE_DISPATCH_CALLS": [entry_to_dict(e) for e in inv.feature_dispatch_calls],
        "FEATURE_CATALOG_GATES": [entry_to_dict(e) for e in inv.feature_catalog_gates],
        "RESOURCE_PHASE_CONDITIONS": [entry_to_dict(e) for e in inv.resource_phase_conditions],
        "RESTART_GUARD": [entry_to_dict(e) for e in inv.restart_guard],
        "UNPARSED": [entry_to_dict(e) for e in inv.unparsed],
    }


def render_markdown(inv: Inventory) -> str:
    lines: list[str] = [
        "# A13 SystemUI Gate Inventory",
        "",
        "This document records the raw conditions found in SystemUI startup gating.",
        "It does not judge INSTALLER_ONLY, GATE_ONLY, DEFAULT_MISMATCH, etc.",
        "",
    ]
    total = (
        len(inv.install_conditions)
        + len(inv.startup_gate_conditions)
        + len(inv.global_action_rules)
        + len(inv.feature_dispatch_calls)
        + len(inv.feature_catalog_gates)
        + len(inv.resource_phase_conditions)
        + len(inv.restart_guard)
        + len(inv.unparsed)
    )
    lines.append(f"Total entries: {total}")
    lines.append("")

    parsed = sum(
        1
        for group in [
            inv.install_conditions,
            inv.startup_gate_conditions,
            inv.global_action_rules,
            inv.feature_dispatch_calls,
            inv.feature_catalog_gates,
            inv.resource_phase_conditions,
            inv.restart_guard,
        ]
        for e in group
        if e.parse_status == "PARSED"
    )
    partial = sum(
        1
        for group in [
            inv.install_conditions,
            inv.startup_gate_conditions,
            inv.global_action_rules,
            inv.feature_dispatch_calls,
            inv.feature_catalog_gates,
            inv.resource_phase_conditions,
            inv.restart_guard,
        ]
        for e in group
        if e.parse_status == "PARTIAL"
    )
    unparsed = sum(
        1
        for group in [
            inv.install_conditions,
            inv.startup_gate_conditions,
            inv.global_action_rules,
            inv.feature_dispatch_calls,
            inv.feature_catalog_gates,
            inv.resource_phase_conditions,
            inv.restart_guard,
            inv.unparsed,
        ]
        for e in group
        if e.parse_status == "UNPARSED"
    )
    lines.append(f"| parse_status | count |")
    lines.append(f"|--------------|-------|")
    lines.append(f"| PARSED | {parsed} |")
    lines.append(f"| PARTIAL | {partial} |")
    lines.append(f"| UNPARSED | {unparsed} |")
    lines.append("")

    categories = [
        ("INSTALL_CONDITIONS", inv.install_conditions),
        ("STARTUP_GATE_CONDITIONS", inv.startup_gate_conditions),
        ("GLOBAL_ACTION_DOMAIN_RULES", inv.global_action_rules),
        ("FEATURE_DISPATCH_CALLS", inv.feature_dispatch_calls),
        ("FEATURE_CATALOG_GATES", inv.feature_catalog_gates),
        ("RESOURCE_PHASE_CONDITIONS", inv.resource_phase_conditions),
        ("RESTART_GUARD", inv.restart_guard),
        ("UNPARSED", inv.unparsed),
    ]

    for name, entries in categories:
        lines.append(f"## {name} ({len(entries)})")
        lines.append("")
        for e in entries:
            lines.append(f"### {e.id}")
            lines.append(f"- source_file: `{e.source_file}`")
            lines.append(f"- source_method: `{e.source_method}`")
            lines.append(f"- lines: {e.start_line}-{e.end_line}")
            lines.append(f"- phase: `{e.phase}`")
            lines.append(f"- parse_status: `{e.parse_status}`")
            if e.feature_id:
                lines.append(f"- feature_id: `{e.feature_id}`")
            if e.preference_keys:
                lines.append(f"- preference_keys: {e.preference_keys}")
            if e.accessors:
                lines.append(f"- accessors: {e.accessors}")
            if e.default_values:
                lines.append(f"- default_values: {e.default_values}")
            if e.comparators:
                lines.append(f"- comparators: {e.comparators}")
            if e.boolean_operators:
                lines.append(f"- boolean_operators: {e.boolean_operators}")
            lines.append(f"- raw_expression: `{e.raw_expression}`")
            if e.normalized_expression and e.normalized_expression != e.raw_expression:
                lines.append(f"- normalized_expression: `{e.normalized_expression}`")
            lines.append("")

    return "\n".join(lines)


def write_inventory(repo_root: Path, verify: bool = False) -> tuple[Path, Path, str, str]:
    inv = inventory_from_sources(repo_root)
    json_data = to_json(inv)

    json_path = repo_root / "docs/audit/A13_SYSTEMUI_GATE_INVENTORY.json"
    md_path = repo_root / "docs/audit/A13_SYSTEMUI_GATE_INVENTORY.md"

    json_text = json.dumps(json_data, indent=2, sort_keys=True, ensure_ascii=False)
    md_text = render_markdown(inv)

    if verify:
        if not json_path.exists() or not md_path.exists():
            raise RuntimeError("Inventory output files do not exist; run without --verify to generate.")
        existing_json = json_path.read_text(encoding="utf-8")
        existing_md = md_path.read_text(encoding="utf-8")
        if existing_json != json_text:
            raise RuntimeError("JSON inventory is not deterministic or source changed.")
        if existing_md != md_text:
            raise RuntimeError("Markdown inventory is not deterministic or source changed.")
        return json_path, md_path, json_text, md_text

    json_path.parent.mkdir(parents=True, exist_ok=True)
    json_path.write_text(json_text, encoding="utf-8")
    md_path.write_text(md_text, encoding="utf-8")

    return json_path, md_path, json_text, md_text


def main() -> int:
    parser = argparse.ArgumentParser(description="SystemUI gate condition inventory")
    parser.add_argument("--output", type=Path, default=REPO_ROOT, help="repo root path")
    parser.add_argument("--verify", action="store_true", help="verify existing inventory matches")
    parser.add_argument("--sha256", action="store_true", help="print sha256 of JSON output")
    args = parser.parse_args()

    json_path, _, json_text, _ = write_inventory(args.output, verify=args.verify)

    if args.sha256:
        print(hashlib.sha256(json_text.encode("utf-8")).hexdigest())
    else:
        print(f"Wrote {json_path}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
