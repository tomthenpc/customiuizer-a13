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

INSTALLER_REL = "app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java"
CATALOG_REL = "app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt"
SCHEMA_VERSION = "1.0"

PREF_METHODS = ("getBoolean", "getInt", "getStringAsInt", "getString", "getStringSet")
PREF_RECEIVERS = {"prefs", "mPrefs", "MainModule.mPrefs"}

KNOWN_CONTROL_KEYWORDS = {"if", "while", "for", "do", "switch", "synchronized", "try"}


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
    default_kinds: list[str] = field(default_factory=list)
    comparators: list[str] = field(default_factory=list)
    boolean_operators: list[str] = field(default_factory=list)
    feature_id: str = ""
    install_target: str = ""
    phase: str = "UNKNOWN"
    parse_status: str = "PARSED"
    nesting_depth: int = 0
    parent_condition_id: str = ""
    branch_kind: str = "IF"
    # FeatureCatalog-specific fields
    declared_preference_keys: list[str] = field(default_factory=list)
    condition_preference_keys: list[str] = field(default_factory=list)
    preference_key_difference: list[str] = field(default_factory=list)
    # Internal: absolute source offsets and branch span for binding installById
    start_offset: int = 0
    end_offset: int = 0
    branch_start: int = 0
    branch_end: int = 0


@dataclass
class MethodBody:
    text: str
    start_offset: int
    open_brace: int
    close_brace: int

    def __bool__(self) -> bool:
        return bool(self.text)


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
    partial: list[ConditionEntry] = field(default_factory=list)


@dataclass
class ParsedExpr:
    preference_keys: list[str] = field(default_factory=list)
    accessors: list[str] = field(default_factory=list)
    default_values: list[Any] = field(default_factory=list)
    default_kinds: list[str] = field(default_factory=list)
    comparators: list[str] = field(default_factory=list)
    boolean_operators: list[str] = field(default_factory=list)
    unmodelled_pref_calls: int = 0
    parse_status: str = "PARSED"


# ---------------------------------------------------------------------------
# Low-level source scanning helpers
# ---------------------------------------------------------------------------


def get_line_number(text: str, offset: int) -> int:
    """Return the 1-based line number for an absolute source offset."""
    return text[:offset].count("\n") + 1


def skip_string(text: str, pos: int) -> int:
    """Skip a single or double quoted string starting at pos; return index after closing quote."""
    n = len(text)
    if pos >= n:
        return pos
    quote = text[pos]
    if quote not in ('"', "'"):
        return pos + 1
    i = pos + 1
    while i < n:
        ch = text[i]
        if ch == "\\" and i + 1 < n:
            i += 2
            continue
        if ch == quote:
            return i + 1
        i += 1
    return n


def skip_ws_comments(text: str, pos: int) -> int:
    """Advance past whitespace, line comments and block comments."""
    n = len(text)
    while pos < n:
        if text[pos].isspace():
            pos += 1
            continue
        if pos + 1 < n and text[pos] == "/":
            if text[pos + 1] == "/":
                # line comment: skip to after the newline
                nl = text.find("\n", pos)
                pos = nl + 1 if nl != -1 else n
                continue
            if text[pos + 1] == "*":
                # block comment: skip to after */
                end = text.find("*/", pos + 2)
                pos = end + 2 if end != -1 else n
                continue
        break
    return pos


def find_matching(text: str, start: int, open_ch: str, close_ch: str) -> int:
    """Find the matching close character for an open at start, skipping strings and comments."""
    n = len(text)
    if start >= n or text[start] != open_ch:
        return -1
    depth = 1
    i = start + 1
    while i < n:
        ch = text[i]
        if ch in ('"', "'"):
            i = skip_string(text, i)
            continue
        if i + 1 < n and ch == "/":
            if text[i + 1] == "/":
                i = text.find("\n", i) + 1
                if i == 0:
                    i = n
                continue
            if text[i + 1] == "*":
                end = text.find("*/", i + 2)
                i = end + 2 if end != -1 else n
                continue
        if ch == open_ch:
            depth += 1
        elif ch == close_ch:
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def read_identifier(text: str, pos: int) -> tuple[str, int]:
    """Read an identifier starting at pos."""
    n = len(text)
    start = pos
    while pos < n and (text[pos].isalnum() or text[pos] == "_"):
        pos += 1
    return text[start:pos], pos


# ---------------------------------------------------------------------------
# Statement and if-chain parsing
# ---------------------------------------------------------------------------


def find_simple_statement_end(text: str, pos: int) -> int:
    """Find the ';' ending a simple statement at top level, skipping nested braces/parens and strings."""
    n = len(text)
    brace = 0
    paren = 0
    i = pos
    while i < n:
        ch = text[i]
        if ch in ('"', "'"):
            i = skip_string(text, i)
            continue
        if i + 1 < n and ch == "/":
            if text[i + 1] == "/":
                i = text.find("\n", i) + 1
                if i == 0:
                    i = n
                continue
            if text[i + 1] == "*":
                end = text.find("*/", i + 2)
                i = end + 2 if end != -1 else n
                continue
        if ch == "{":
            brace += 1
        elif ch == "}":
            brace -= 1
        elif ch == "(":
            paren += 1
        elif ch == ")":
            paren -= 1
        if ch == ";" and brace == 0 and paren == 0:
            return i + 1
        i += 1
    return n


def find_statement_end(text: str, pos: int) -> int:
    """Return the index just past the statement starting at pos."""
    n = len(text)
    pos = skip_ws_comments(text, pos)
    if pos >= n:
        return pos
    ch = text[pos]
    if ch == "{":
        close = find_matching(text, pos, "{", "}")
        return close + 1 if close >= 0 else n
    if ch.isalpha() or ch == "_":
        ident, end = read_identifier(text, pos)
        if ident == "if":
            pos = skip_ws_comments(text, end)
            if pos < n and text[pos] == "(":
                cond_close = find_matching(text, pos, "(", ")")
                if cond_close < 0:
                    return n
                then_end = find_statement_end(text, cond_close + 1)
                check = skip_ws_comments(text, then_end)
                if check < n and (text[check].isalpha() or text[check] == "_"):
                    nxt, nxt_end = read_identifier(text, check)
                    if nxt == "else":
                        else_end = find_statement_end(text, nxt_end)
                        return else_end
                return then_end
            return find_simple_statement_end(text, pos)
        if ident in ("while", "switch", "synchronized"):
            pos = skip_ws_comments(text, end)
            if pos < n and text[pos] == "(":
                cond_close = find_matching(text, pos, "(", ")")
                if cond_close >= 0:
                    return find_statement_end(text, cond_close + 1)
            return find_statement_end(text, end)
        if ident == "for":
            pos = skip_ws_comments(text, end)
            if pos < n and text[pos] == "(":
                cond_close = find_matching(text, pos, "(", ")")
                if cond_close >= 0:
                    return find_statement_end(text, cond_close + 1)
            return find_statement_end(text, end)
        if ident == "do":
            body_end = find_statement_end(text, end)
            pos = skip_ws_comments(text, body_end)
            if pos < n and (text[pos].isalpha() or text[pos] == "_"):
                nxt, nxt_end = read_identifier(text, pos)
                if nxt == "while":
                    pos = skip_ws_comments(text, nxt_end)
                    if pos < n and text[pos] == "(":
                        cond_close = find_matching(text, pos, "(", ")")
                        if cond_close >= 0:
                            pos = skip_ws_comments(text, cond_close + 1)
                            if pos < n and text[pos] == ";":
                                return pos + 1
            return body_end
        if ident == "try":
            pos = skip_ws_comments(text, end)
            if pos < n and text[pos] == "{":
                block_end = find_matching(text, pos, "{", "}")
                if block_end >= 0:
                    pos = block_end + 1
                    while True:
                        pos = skip_ws_comments(text, pos)
                        if pos >= n or not (text[pos].isalpha() or text[pos] == "_"):
                            break
                        nxt, nxt_end = read_identifier(text, pos)
                        if nxt == "catch":
                            pos = skip_ws_comments(text, nxt_end)
                            if pos < n and text[pos] == "(":
                                catch_close = find_matching(text, pos, "(", ")")
                                pos = find_statement_end(text, catch_close + 1) if catch_close >= 0 else n
                            else:
                                pos = find_statement_end(text, nxt_end)
                        elif nxt == "finally":
                            pos = find_statement_end(text, nxt_end)
                        else:
                            break
                    return pos
            return find_simple_statement_end(text, end)
        return find_simple_statement_end(text, pos)
    return find_simple_statement_end(text, pos)


# ---------------------------------------------------------------------------
# Condition extraction
# ---------------------------------------------------------------------------


def _condition_id(source_method: str, parent_id: str, branch_kind: str, counter: int) -> str:
    kind = branch_kind.lower().replace("_", "_")
    base = parent_id if parent_id else source_method
    return f"{base}_{kind}_{counter}"


def parse_if_at(
    text: str,
    if_pos: int,
    source_file: str,
    source_method: str,
    phase: str,
    full_text: str,
    body_start: int,
    parent_id: str,
    depth: int,
    branch_kind: str,
    counter: list[int],
) -> tuple[list[ConditionEntry], int]:
    n = len(text)
    if_start_abs = body_start + if_pos
    pos = if_pos + 2
    pos = skip_ws_comments(text, pos)
    if pos >= n or text[pos] != "(":
        return [], pos
    cond_open = pos
    cond_close = find_matching(text, cond_open, "(", ")")
    if cond_close < 0:
        return [], n

    raw_expr = text[cond_open + 1 : cond_close]
    cleaned_expr = strip_comments(raw_expr).strip()
    normalized_expr = normalize_whitespace(cleaned_expr)

    counter[0] += 1
    cond_id = _condition_id(source_method, parent_id, branch_kind, counter[0])

    parsed = parse_expression(cleaned_expr) if cleaned_expr else ParsedExpr()

    start_line = get_line_number(full_text, if_start_abs)
    end_line = get_line_number(full_text, body_start + cond_close)

    entry = ConditionEntry(
        id=cond_id,
        source_file=source_file,
        source_method=source_method,
        start_line=start_line,
        end_line=end_line,
        raw_expression=cleaned_expr,
        normalized_expression=normalized_expr,
        preference_keys=parsed.preference_keys,
        accessors=parsed.accessors,
        default_values=parsed.default_values,
        default_kinds=parsed.default_kinds,
        comparators=parsed.comparators,
        boolean_operators=parsed.boolean_operators,
        parse_status=parsed.parse_status,
        phase=phase,
        nesting_depth=depth,
        parent_condition_id=parent_id,
        branch_kind=branch_kind,
        start_offset=if_start_abs,
        end_offset=body_start + cond_close,
    )

    then_start = cond_close + 1
    then_end = find_statement_end(text, then_start)
    entry.branch_start = body_start + then_start
    entry.branch_end = body_start + then_end
    branch_text = text[then_start:then_end]
    entry.install_target = extract_install_target(branch_text)

    results: list[ConditionEntry] = [entry]
    nested = extract_conditions(
        branch_text,
        source_file,
        source_method,
        phase,
        full_text=full_text,
        body_start=body_start + then_start,
        parent_id=cond_id,
        depth=depth + 1,
        counter=counter,
    )
    results.extend(nested)

    pos = then_end
    while True:
        pos = skip_ws_comments(text, pos)
        if pos >= n or not (text[pos].isalpha() or text[pos] == "_"):
            break
        ident, end = read_identifier(text, pos)
        if ident != "else":
            break
        pos = end
        pos = skip_ws_comments(text, pos)
        if pos < n and (text[pos].isalpha() or text[pos] == "_"):
            nxt, nxt_end = read_identifier(text, pos)
            if nxt == "if":
                sub, next_pos = parse_if_at(
                    text,
                    pos,
                    source_file,
                    source_method,
                    phase,
                    full_text,
                    body_start,
                    parent_id=cond_id,
                    depth=depth + 1,
                    branch_kind="ELSE_IF",
                    counter=counter,
                )
                results.extend(sub)
                pos = next_pos
                continue
        # plain else branch
        else_start = pos
        else_end = find_statement_end(text, else_start)
        else_text = text[else_start:else_end]
        else_entry = ConditionEntry(
            id=f"{cond_id}_else_{counter[0] + 1}",
            source_file=source_file,
            source_method=source_method,
            start_line=get_line_number(full_text, body_start + else_start),
            end_line=get_line_number(full_text, body_start + else_start),
            raw_expression="",
            normalized_expression="",
            phase=phase,
            nesting_depth=depth + 1,
            parent_condition_id=cond_id,
            branch_kind="ELSE",
            start_offset=body_start + else_start,
            end_offset=body_start + else_start,
        )
        counter[0] += 1
        else_entry.branch_start = body_start + else_start
        else_entry.branch_end = body_start + else_end
        else_entry.install_target = extract_install_target(else_text)
        results.append(else_entry)
        nested_else = extract_conditions(
            else_text,
            source_file,
            source_method,
            phase,
            full_text=full_text,
            body_start=body_start + else_start,
            parent_id=cond_id,
            depth=depth + 1,
            counter=counter,
        )
        results.extend(nested_else)
        pos = else_end
    return results, pos


def extract_conditions(
    body_text: str,
    source_file: str,
    source_method: str,
    phase: str,
    full_text: str | None = None,
    body_start: int = 0,
    parent_id: str = "",
    depth: int = 0,
    counter: list[int] | None = None,
) -> list[ConditionEntry]:
    """Recursively extract if/else-if/else blocks from a method body.

    The returned ConditionEntry objects have absolute start_line/end_line values
    because offsets are computed against full_text (or body_text if no full_text
    is supplied).
    """
    if full_text is None:
        full_text = body_text
    if counter is None:
        counter = [0]
    results: list[ConditionEntry] = []
    n = len(body_text)
    pos = 0
    while pos < n:
        pos = skip_ws_comments(body_text, pos)
        if pos >= n:
            break
        ch = body_text[pos]
        if ch in ('"', "'"):
            pos = skip_string(body_text, pos)
            continue
        if ch.isalpha() or ch == "_":
            ident, end = read_identifier(body_text, pos)
            if ident == "if":
                if pos == 0 or not (
                    body_text[pos - 1].isalnum()
                    or body_text[pos - 1] == "_"
                    or body_text[pos - 1] == "."
                ):
                    entries, next_pos = parse_if_at(
                        body_text,
                        pos,
                        source_file,
                        source_method,
                        phase,
                        full_text,
                        body_start,
                        parent_id,
                        depth,
                        "IF",
                        counter,
                    )
                    results.extend(entries)
                    pos = next_pos
                    continue
            pos = end
            continue
        pos += 1
    return results


def extract_install_target(branch_text: str) -> str:
    """Return the first top-level method call in a branch statement, or ''."""
    pos = skip_ws_comments(branch_text, 0)
    n = len(branch_text)
    if pos >= n:
        return ""
    if branch_text[pos] == "{":
        pos = skip_ws_comments(branch_text, pos + 1)
    if pos >= n:
        return ""
    if branch_text[pos].isalpha() or branch_text[pos] == "_":
        ident, end = read_identifier(branch_text, pos)
        if ident in KNOWN_CONTROL_KEYWORDS:
            return ""
    # find first method call of the form SomeClass[.member]*.methodName(
    m = re.search(r"\b([A-Z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)*)\s*\(", branch_text[pos:])
    if not m:
        return ""
    call_open = pos + m.end() - 1
    call_close = find_matching(branch_text, call_open, "(", ")")
    if call_close < 0:
        return ""
    return branch_text[pos + m.start() : call_close + 1]


# ---------------------------------------------------------------------------
# Method body extraction
# ---------------------------------------------------------------------------


def find_method_body(text: str, signature: str) -> MethodBody | None:
    """Locate a method by its signature and return its body plus source offsets."""
    pattern = re.compile(re.escape(signature) + r"\s*\(", re.S)
    m = pattern.search(text)
    if not m:
        return None
    param_open = m.end() - 1
    param_close = find_matching(text, param_open, "(", ")")
    if param_close < 0:
        return None
    pos = skip_ws_comments(text, param_close + 1)
    if pos >= len(text) or text[pos] != "{":
        return None
    open_brace = pos
    close_brace = find_matching(text, open_brace, "{", "}")
    if close_brace < 0:
        return None
    body_text = text[open_brace : close_brace + 1]
    return MethodBody(
        text=body_text,
        start_offset=open_brace,
        open_brace=open_brace,
        close_brace=close_brace,
    )


# ---------------------------------------------------------------------------
# Expression parsing
# ---------------------------------------------------------------------------


def strip_comments(text: str) -> str:
    """Remove line and block comments while preserving string literals."""
    out: list[str] = []
    i = 0
    n = len(text)
    in_string: str | None = None
    while i < n:
        ch = text[i]
        if in_string:
            if ch == in_string:
                if i == 0 or text[i - 1] != "\\":
                    in_string = None
            out.append(ch)
            i += 1
            continue
        if ch in ('"', "'"):
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


def mask_strings(text: str) -> str:
    """Return a copy of text where the content of string/char literals is replaced by spaces.

    Quote characters are preserved so that positions remain aligned with the original.
    """
    n = len(text)
    chars = list(text)
    i = 0
    while i < n:
        ch = text[i]
        if ch in ('"', "'"):
            quote = ch
            i += 1
            while i < n:
                if text[i] == "\\" and i + 1 < n:
                    chars[i] = " "
                    chars[i + 1] = " "
                    i += 2
                    continue
                if text[i] == quote:
                    i += 1
                    break
                chars[i] = " "
                i += 1
            continue
        if i + 1 < n and ch == "/" and text[i + 1] in ("/", "*"):
            # comments are not normally present in expressions, but mask them too
            if text[i + 1] == "/":
                nl = text.find("\n", i)
                end = nl if nl != -1 else n
                for j in range(i, end):
                    chars[j] = " "
                i = end
                continue
            else:
                end = text.find("*/", i + 2)
                end = end + 2 if end != -1 else n
                for j in range(i, end):
                    chars[j] = " "
                i = end
                continue
        i += 1
    return "".join(chars)


def split_top_level(text: str, sep: str) -> list[str]:
    """Split text by a separator at the top level (outside of strings and nested parens/braces/brackets)."""
    n = len(text)
    i = 0
    start = 0
    parts: list[str] = []
    depth_paren = 0
    depth_brace = 0
    depth_brack = 0
    sep_len = len(sep)
    while i < n:
        ch = text[i]
        if ch in ('"', "'"):
            i = skip_string(text, i)
            continue
        if i + 1 < n and ch == "/" and text[i + 1] in ("/", "*"):
            if text[i + 1] == "/":
                i = text.find("\n", i) + 1
                if i == 0:
                    i = n
                continue
            else:
                end = text.find("*/", i + 2)
                i = end + 2 if end != -1 else n
                continue
        if ch == "(":
            depth_paren += 1
        elif ch == ")":
            depth_paren -= 1
        elif ch == "{":
            depth_brace += 1
        elif ch == "}":
            depth_brace -= 1
        elif ch == "[":
            depth_brack += 1
        elif ch == "]":
            depth_brack -= 1
        if depth_paren == 0 and depth_brace == 0 and depth_brack == 0 and text.startswith(sep, i):
            parts.append(text[start:i])
            i += sep_len
            start = i
            continue
        i += 1
    parts.append(text[start:])
    return parts


def parse_string_literal(text: str) -> str | None:
    """Parse a string or char literal and return its content, or None if not a literal."""
    text = text.strip()
    if len(text) < 2:
        return None
    quote = text[0]
    if quote not in ('"', "'") or text[-1] != quote:
        return None
    inner = text[1:-1]
    return inner.replace("\\\"", "\"").replace("\\\\", "\\")


def default_value_and_kind(method: str, arg_text: str | None) -> tuple[Any, str]:
    """Compute default value and default kind for a PrefMap accessor call."""
    if arg_text is None:
        if method == "getBoolean":
            return False, "IMPLICIT_PREFMAP_DEFAULT"
        if method == "getStringSet":
            return [], "IMPLICIT_PREFMAP_DEFAULT"
        return None, "UNKNOWN_DEFAULT"
    arg = arg_text.strip()
    if method == "getBoolean":
        if arg == "true":
            return True, "EXPLICIT"
        if arg == "false":
            return False, "EXPLICIT"
        return arg, "EXPLICIT"
    if method in ("getInt", "getStringAsInt"):
        try:
            return int(arg), "EXPLICIT"
        except ValueError:
            return arg, "EXPLICIT"
    if method == "getString":
        lit = parse_string_literal(arg)
        if lit is not None:
            return lit, "EXPLICIT"
        return arg, "EXPLICIT"
    if method == "getStringSet":
        if arg == "emptySet()":
            return [], "EXPLICIT"
        if arg.startswith("setOf("):
            inner = arg[6:-1]
            values: list[Any] = []
            for part in split_top_level(inner, ","):
                part = part.strip()
                if not part:
                    continue
                lit = parse_string_literal(part)
                values.append(lit if lit is not None else part)
            return values, "EXPLICIT"
        return arg, "EXPLICIT"
    return arg, "UNKNOWN_DEFAULT"


def _build_accessor_call(method: str, args_text: str) -> dict[str, Any] | None:
    args = split_top_level(args_text, ",")
    if not args:
        return None
    key_lit = parse_string_literal(args[0].strip())
    if key_lit is None:
        return None
    default_text = args[1].strip() if len(args) > 1 else None
    default_value, default_kind = default_value_and_kind(method, default_text)
    return {
        "accessor": method,
        "key": key_lit,
        "default_value": default_value,
        "default_kind": default_kind,
    }


PREF_ACCESSOR_RE = re.compile(
    r"\b(?:prefs|mPrefs|MainModule\.mPrefs)\.(getBoolean|getInt|getStringAsInt|getString|getStringSet)\s*\("
)
PREF_ANY_RE = re.compile(r"\b(?:prefs|mPrefs|MainModule\.mPrefs)\.(\w+)\s*\(")


def _parse_accessor_calls(expr: str, masked: str) -> tuple[list[dict[str, Any]], int]:
    calls: list[dict[str, Any]] = []
    unmodelled = 0
    for m in PREF_ACCESSOR_RE.finditer(masked):
        call_open = m.end() - 1
        call_close = find_matching(expr, call_open, "(", ")")
        if call_close < 0:
            continue
        call = _build_accessor_call(m.group(1), expr[call_open + 1 : call_close])
        if call:
            calls.append(call)
    for m in PREF_ANY_RE.finditer(masked):
        method = m.group(1)
        if method not in PREF_METHODS:
            unmodelled += 1
    return calls, unmodelled


def _split_boolean_atoms(expr: str) -> tuple[list[str], list[str]]:
    """Split an expression by top-level &&/||. Return (atoms, operators)."""
    n = len(expr)
    i = 0
    start = 0
    atoms: list[str] = []
    ops: list[str] = []
    depth_paren = 0
    depth_brace = 0
    depth_brack = 0
    while i < n:
        ch = expr[i]
        if ch in ('"', "'"):
            i = skip_string(expr, i)
            continue
        if i + 1 < n and ch == "/" and expr[i + 1] in ("/", "*"):
            if expr[i + 1] == "/":
                i = expr.find("\n", i) + 1
                if i == 0:
                    i = n
                continue
            else:
                end = expr.find("*/", i + 2)
                i = end + 2 if end != -1 else n
                continue
        if ch == "(":
            depth_paren += 1
        elif ch == ")":
            depth_paren -= 1
        elif ch == "{":
            depth_brace += 1
        elif ch == "}":
            depth_brace -= 1
        elif ch == "[":
            depth_brack += 1
        elif ch == "]":
            depth_brack -= 1
        if depth_paren == 0 and depth_brace == 0 and depth_brack == 0:
            if i + 1 < n and expr[i] == "&" and expr[i + 1] == "&":
                atoms.append(expr[start:i])
                ops.append("AND")
                i += 2
                start = i
                continue
            if i + 1 < n and expr[i] == "|" and expr[i + 1] == "|":
                atoms.append(expr[start:i])
                ops.append("OR")
                i += 2
                start = i
                continue
        i += 1
    atoms.append(expr[start:])
    return atoms, ops


def parse_expression(expr: str) -> ParsedExpr:
    """Best-effort parse of a Java/Kotlin boolean condition."""
    parsed = ParsedExpr()
    if not expr:
        parsed.parse_status = "UNPARSED"
        return parsed

    expr = strip_comments(expr)
    masked = mask_strings(expr)

    calls, unmodelled = _parse_accessor_calls(expr, masked)
    for call in calls:
        parsed.accessors.append(call["accessor"])
        parsed.preference_keys.append(call["key"])
        parsed.default_values.append(call["default_value"])
        parsed.default_kinds.append(call["default_kind"])
    parsed.unmodelled_pref_calls = unmodelled

    atoms, ops = _split_boolean_atoms(masked)
    parsed.boolean_operators.extend(ops)

    for atom in atoms:
        a = atom.strip()
        neg = a.startswith("!") or (a.startswith("(") and a[1:].lstrip().startswith("!"))
        if neg:
            parsed.boolean_operators.append("NOT")

        if re.search(r"(?<!\w)isNotEmpty\s*\(", atom):
            parsed.boolean_operators.append("notIsEmpty")
        elif re.search(r"(?<!\w)isEmpty\s*\(", atom):
            if neg:
                parsed.boolean_operators.append("notIsEmpty")
            else:
                parsed.boolean_operators.append("isEmpty")

        # treat .equals(...) as an equality/inequality comparator
        eq_match = re.search(r"(?<!\w)equals\s*\(", atom)
        if eq_match:
            if neg:
                parsed.comparators.append("!=")
            else:
                parsed.comparators.append("==")

        for cm in re.finditer(r">=|<=|==|!=|>|<", atom):
            parsed.comparators.append(cm.group(0))

    if unmodelled > 0:
        if parsed.accessors:
            parsed.parse_status = "PARTIAL"
        else:
            parsed.parse_status = "UNPARSED"
    elif not parsed.accessors:
        # constant or non-preference expression
        parsed.parse_status = "PARSED"
    else:
        parsed.parse_status = "PARSED"

    return parsed


def normalize_whitespace(text: str) -> str:
    """Collapse all whitespace into single spaces and trim."""
    return re.sub(r"\s+", " ", text).strip()


# ---------------------------------------------------------------------------
# FeatureCatalog parsing
# ---------------------------------------------------------------------------


def parse_feature_catalog(repo_root: Path) -> list[ConditionEntry]:
    """Extract FeatureSpec objects for SystemUI scope from FeatureCatalog.kt."""
    catalog_path = repo_root / CATALOG_REL
    source_file = catalog_path.relative_to(repo_root).as_posix()
    text = catalog_path.read_text(encoding="utf-8")
    results: list[ConditionEntry] = []

    for m in re.finditer(r"FeatureSpec\s*\(", text):
        spec_open = m.end() - 1
        spec_close = find_matching(text, spec_open, "(", ")")
        if spec_close < 0:
            continue
        spec_text = text[spec_open:spec_close + 1]

        id_match = re.search(r'\bid\s*=\s*"([^"]+)"', spec_text)
        scope_match = re.search(r'processScope\s*=\s*ProcessScope\.([A-Z_]+)', spec_text)
        if not id_match or not scope_match or scope_match.group(1) != "SYSTEM_UI":
            continue

        feature_id = id_match.group(1)

        declared_keys: list[str] = []
        pref_match = re.search(r"preferenceKeys\s*=\s*", spec_text)
        if pref_match:
            pos = skip_ws_comments(spec_text, pref_match.end())
            if pos < len(spec_text) and (spec_text[pos].isalpha() or spec_text[pos] == "_"):
                _, after_ident = read_identifier(spec_text, pos)
                pos2 = skip_ws_comments(spec_text, after_ident)
                if pos2 < len(spec_text) and spec_text[pos2] == "(":
                    set_close = find_matching(spec_text, pos2, "(", ")")
                    if set_close >= 0:
                        set_content = spec_text[pos2 + 1 : set_close]
                        declared_keys = re.findall(r'"([^"]+)"', set_content)

        condition_text = ""
        cond_match = re.search(r"condition\s*=\s*", spec_text)
        if cond_match:
            pos = skip_ws_comments(spec_text, cond_match.end())
            if pos < len(spec_text) and spec_text[pos] == "{":
                cond_close = find_matching(spec_text, pos, "{", "}")
                if cond_close >= 0:
                    condition_text = spec_text[pos : cond_close + 1]
            else:
                # expression-body condition (rare); consume until top-level comma
                i = pos
                depth = 0
                while i < len(spec_text):
                    if spec_text[i] in ('"', "'"):
                        i = skip_string(spec_text, i)
                        continue
                    if spec_text[i] == "(":
                        depth += 1
                    elif spec_text[i] == ")":
                        depth -= 1
                    elif spec_text[i] == "," and depth == 0:
                        break
                    i += 1
                condition_text = spec_text[pos:i]

        raw_condition = condition_text.strip()
        inner = strip_comments(raw_condition).strip()
        if inner.startswith("{"):
            inner = inner[1:]
            if inner.endswith("}"):
                inner = inner[:-1]
            inner = inner.strip()
        # remove optional lambda parameters (prefs ->)
        m2 = re.match(r"^(?:[\w\s,]+\s*->\s*)?(.*)$", inner, re.S)
        expr = m2.group(1).strip() if m2 else inner

        if expr:
            parsed = parse_expression(expr)
        else:
            parsed = ParsedExpr(parse_status="UNPARSED")

        condition_keys = parsed.preference_keys
        declared_set = set(declared_keys)
        condition_set = set(condition_keys)
        diff = sorted((declared_set ^ condition_set)) if declared_set != condition_set else []

        start_line = get_line_number(text, spec_open)
        end_line = get_line_number(text, spec_close)

        entry = ConditionEntry(
            id=f"FeatureCatalog_{feature_id}",
            source_file=source_file,
            source_method="FeatureCatalog",
            start_line=start_line,
            end_line=end_line,
            raw_expression=raw_condition,
            normalized_expression=normalize_whitespace(expr),
            feature_id=feature_id,
            install_target="FeatureDispatcher",
            phase="FEATURE_CATALOG_GATE",
            declared_preference_keys=sorted(declared_keys),
            condition_preference_keys=condition_keys,
            preference_key_difference=diff,
            parse_status=parsed.parse_status,
            preference_keys=condition_keys,
            accessors=parsed.accessors,
            default_values=parsed.default_values,
            default_kinds=parsed.default_kinds,
            comparators=parsed.comparators,
            boolean_operators=parsed.boolean_operators,
        )
        results.append(entry)

    return results


# ---------------------------------------------------------------------------
# SystemUiInstaller analysis
# ---------------------------------------------------------------------------


def _find_install_by_id_calls(
    body_text: str,
    body_start: int,
    full_text: str,
    source_file: str,
    source_method: str,
    phase: str,
) -> list[ConditionEntry]:
    """Find all unconditional FeatureDispatcher.installById(...) calls in a body."""
    results: list[ConditionEntry] = []
    pattern = re.compile(r"FeatureDispatcher\s*\.\s*installById\s*\(")
    for m in pattern.finditer(body_text):
        call_open = m.end() - 1
        call_close = find_matching(body_text, call_open, "(", ")")
        if call_close < 0:
            continue
        call_text = body_text[m.start() : call_close + 1]
        argm = re.search(r'\(\s*"([^"]+)"', call_text)
        feature_id = argm.group(1) if argm else ""
        results.append(
            ConditionEntry(
                id=f"{source_method}_installById_{len(results) + 1}",
                source_file=source_file,
                source_method=source_method,
                start_line=get_line_number(full_text, body_start + m.start()),
                end_line=get_line_number(full_text, body_start + call_close),
                raw_expression=call_text,
                normalized_expression=call_text,
                feature_id=feature_id,
                install_target="FeatureDispatcher",
                phase=phase,
            )
        )
    return results


def _bind_install_by_id(
    body_text: str,
    body_start: int,
    full_text: str,
    conditions: list[ConditionEntry],
    source_file: str,
    source_method: str,
) -> list[ConditionEntry]:
    """Bind installById calls to the innermost condition branch that contains them.

    Returns the calls that were not inside any condition branch (unconditional).
    """
    pattern = re.compile(r"FeatureDispatcher\s*\.\s*installById\s*\(")
    unconditional: list[ConditionEntry] = []
    for m in pattern.finditer(body_text):
        call_open = m.end() - 1
        call_close = find_matching(body_text, call_open, "(", ")")
        if call_close < 0:
            continue
        call_start = body_start + m.start()
        call_end = body_start + call_close
        call_text = body_text[m.start() : call_close + 1]
        argm = re.search(r'\(\s*"([^"]+)"', call_text)
        feature_id = argm.group(1) if argm else ""

        # find the deepest (most nested) condition whose branch contains this call
        candidates = [
            c
            for c in conditions
            if c.branch_start <= call_start and call_end <= c.branch_end
        ]
        if candidates:
            candidates.sort(key=lambda c: c.nesting_depth, reverse=True)
            chosen = candidates[0]
            if not chosen.feature_id:
                chosen.feature_id = feature_id
                chosen.install_target = call_text
            continue

        # unconditional call
        unconditional.append(
            ConditionEntry(
                id=f"{source_method}_installById_{len(unconditional) + 1}",
                source_file=source_file,
                source_method=source_method,
                start_line=get_line_number(full_text, call_start),
                end_line=get_line_number(full_text, call_end),
                raw_expression=call_text,
                normalized_expression=call_text,
                feature_id=feature_id,
                install_target="FeatureDispatcher",
                phase="POST_RESTART_GUARD_RUNTIME",
            )
        )
    return unconditional


def _classify_install_phases(conditions: list[ConditionEntry]) -> None:
    """Assign phase labels to the conditions inside SystemUiInstaller.install()."""
    guard = None
    guard_block_end = None
    for c in conditions:
        if "isWithinSystemUiRestartGuard" in c.raw_expression:
            guard = c
            c.phase = "RESTART_GUARD"
            guard_block_end = c.branch_end
            break

    if guard is None:
        for c in conditions:
            c.phase = "POST_RESTART_GUARD_RUNTIME"
        return

    for c in conditions:
        if c is guard:
            continue
        if c.start_offset < guard.start_offset:
            if "pkg" in c.raw_expression and "com.android.systemui" in c.raw_expression:
                c.phase = "PACKAGE_GUARD"
            elif "StatusBarHeightRes" in c.install_target or "NavbarHeightRes" in c.install_target:
                c.phase = "PRE_RESTART_GUARD_RESOURCE"
            elif c.raw_expression and (
                "statusbarheight" in c.raw_expression or "navbarheight" in c.raw_expression
            ):
                c.phase = "PRE_RESTART_GUARD_RESOURCE"
            else:
                c.phase = "PRE_RESTART_GUARD_INFRASTRUCTURE"
        elif c.start_offset > guard_block_end:
            c.phase = "POST_RESTART_GUARD_RUNTIME"
        else:
            c.phase = "PRE_RESTART_GUARD_INFRASTRUCTURE"


def _extract_guard_predicate(body: MethodBody, full_text: str, source_file: str) -> list[ConditionEntry]:
    """Parse the predicate inside isWithinSystemUiRestartGuard(...)."""
    text = body.text
    m = re.search(r"return\s+([^;]+);", text)
    if not m:
        return []
    expr = m.group(1).strip()
    parsed = parse_expression(expr)
    start = body.start_offset + m.start(1)
    end = body.start_offset + m.end(1)
    return [
        ConditionEntry(
            id="isWithinSystemUiRestartGuard_predicate",
            source_file=source_file,
            source_method="isWithinSystemUiRestartGuard",
            start_line=get_line_number(full_text, start),
            end_line=get_line_number(full_text, end),
            raw_expression=expr,
            normalized_expression=expr,
            comparators=parsed.comparators,
            boolean_operators=parsed.boolean_operators,
            parse_status=parsed.parse_status,
            phase="RESTART_GUARD",
        )
    ]


# ---------------------------------------------------------------------------
# Inventory assembly
# ---------------------------------------------------------------------------


def inventory_from_sources(repo_root: Path) -> Inventory:
    inv = Inventory()

    installer_path = repo_root / INSTALLER_REL
    catalog_path = repo_root / CATALOG_REL
    source_file_installer = installer_path.relative_to(repo_root).as_posix()
    source_file_catalog = catalog_path.relative_to(repo_root).as_posix()

    installer_text = installer_path.read_text(encoding="utf-8")

    # install()
    install_body = find_method_body(installer_text, "public static void install")
    if install_body:
        install_conditions = extract_conditions(
            install_body.text,
            source_file_installer,
            "install",
            "UNKNOWN",
            full_text=installer_text,
            body_start=install_body.start_offset,
        )
        _classify_install_phases(install_conditions)
        inv.feature_dispatch_calls.extend(
            _bind_install_by_id(
                install_body.text,
                install_body.start_offset,
                installer_text,
                install_conditions,
                source_file_installer,
                "install",
            )
        )
        inv.install_conditions.extend(install_conditions)
    else:
        inv.unparsed.append(
            ConditionEntry(
                id="install_body_not_found",
                source_file=source_file_installer,
                source_method="install",
                start_line=0,
                end_line=0,
                raw_expression="",
                parse_status="UNPARSED",
            )
        )

    # hasAnySystemUiStartupFeature()
    startup_body = find_method_body(installer_text, "public static boolean hasAnySystemUiStartupFeature")
    if startup_body:
        inv.startup_gate_conditions.extend(
            extract_conditions(
                startup_body.text,
                source_file_installer,
                "hasAnySystemUiStartupFeature",
                "STARTUP_GATE",
                full_text=installer_text,
                body_start=startup_body.start_offset,
            )
        )
    else:
        inv.unparsed.append(
            ConditionEntry(
                id="startup_gate_body_not_found",
                source_file=source_file_installer,
                source_method="hasAnySystemUiStartupFeature",
                start_line=0,
                end_line=0,
                raw_expression="",
                parse_status="UNPARSED",
            )
        )

    # hasAnyGlobalAction()
    global_body = find_method_body(installer_text, "private static boolean hasAnyGlobalAction")
    if global_body:
        inv.global_action_rules.extend(
            extract_conditions(
                global_body.text,
                source_file_installer,
                "hasAnyGlobalAction",
                "GLOBAL_ACTION_DOMAIN",
                full_text=installer_text,
                body_start=global_body.start_offset,
            )
        )
    else:
        inv.unparsed.append(
            ConditionEntry(
                id="global_action_body_not_found",
                source_file=source_file_installer,
                source_method="hasAnyGlobalAction",
                start_line=0,
                end_line=0,
                raw_expression="",
                parse_status="UNPARSED",
            )
        )

    # restart guard predicate
    guard_body = find_method_body(installer_text, "static boolean isWithinSystemUiRestartGuard")
    if guard_body:
        inv.restart_guard.extend(_extract_guard_predicate(guard_body, installer_text, source_file_installer))
    else:
        inv.unparsed.append(
            ConditionEntry(
                id="restart_guard_body_not_found",
                source_file=source_file_installer,
                source_method="isWithinSystemUiRestartGuard",
                start_line=0,
                end_line=0,
                raw_expression="",
                parse_status="UNPARSED",
            )
        )

    # FeatureCatalog
    inv.feature_catalog_gates.extend(parse_feature_catalog(repo_root))

    # resource phase conditions are a copy of install conditions with that phase
    inv.resource_phase_conditions = [
        c for c in inv.install_conditions if c.phase == "PRE_RESTART_GUARD_RESOURCE"
    ]

    # aggregate UNPARSED and PARTIAL entries from the functional categories
    functional = (
        inv.install_conditions
        + inv.startup_gate_conditions
        + inv.global_action_rules
        + inv.feature_dispatch_calls
        + inv.feature_catalog_gates
        + inv.resource_phase_conditions
        + inv.restart_guard
    )
    for e in functional:
        if e.parse_status == "UNPARSED":
            inv.unparsed.append(e)
        elif e.parse_status == "PARTIAL":
            inv.partial.append(e)

    return inv


# ---------------------------------------------------------------------------
# JSON and Markdown rendering
# ---------------------------------------------------------------------------


def _clean_entry_dict(d: dict[str, Any]) -> dict[str, Any]:
    """Remove internal-only fields from a serialized entry."""
    d.pop("start_offset", None)
    d.pop("end_offset", None)
    d.pop("branch_start", None)
    d.pop("branch_end", None)
    return d


def entry_to_dict(entry: ConditionEntry) -> dict[str, Any]:
    d = {
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
        "default_kinds": entry.default_kinds,
        "comparators": entry.comparators,
        "boolean_operators": entry.boolean_operators,
        "feature_id": entry.feature_id,
        "install_target": entry.install_target,
        "phase": entry.phase,
        "parse_status": entry.parse_status,
        "nesting_depth": entry.nesting_depth,
        "parent_condition_id": entry.parent_condition_id,
        "branch_kind": entry.branch_kind,
    }
    if entry.declared_preference_keys or entry.condition_preference_keys or entry.preference_key_difference:
        d["declared_preference_keys"] = entry.declared_preference_keys
        d["condition_preference_keys"] = entry.condition_preference_keys
        d["preference_key_difference"] = entry.preference_key_difference
    return _clean_entry_dict(d)


def to_json(inv: Inventory) -> dict[str, Any]:
    data: dict[str, Any] = {
        "schema_version": SCHEMA_VERSION,
        "generated_from": [INSTALLER_REL, CATALOG_REL],
        "INSTALL_CONDITIONS": [entry_to_dict(e) for e in inv.install_conditions],
        "STARTUP_GATE_CONDITIONS": [entry_to_dict(e) for e in inv.startup_gate_conditions],
        "GLOBAL_ACTION_DOMAIN_RULES": [entry_to_dict(e) for e in inv.global_action_rules],
        "FEATURE_DISPATCH_CALLS": [entry_to_dict(e) for e in inv.feature_dispatch_calls],
        "FEATURE_CATALOG_GATES": [entry_to_dict(e) for e in inv.feature_catalog_gates],
        "RESOURCE_PHASE_CONDITIONS": [entry_to_dict(e) for e in inv.resource_phase_conditions],
        "RESTART_GUARD": [entry_to_dict(e) for e in inv.restart_guard],
        "UNPARSED": [entry_to_dict(e) for e in inv.unparsed],
        "PARTIAL": [entry_to_dict(e) for e in inv.partial],
    }

    # parse_status_counts are computed from functional categories (excluding the aggregate UNPARSED/PARTIAL arrays)
    functional_keys = {
        "INSTALL_CONDITIONS",
        "STARTUP_GATE_CONDITIONS",
        "GLOBAL_ACTION_DOMAIN_RULES",
        "FEATURE_DISPATCH_CALLS",
        "FEATURE_CATALOG_GATES",
        "RESOURCE_PHASE_CONDITIONS",
        "RESTART_GUARD",
    }
    data["category_counts"] = {k: len(data[k]) for k in functional_keys | {"UNPARSED", "PARTIAL"}}
    counts: dict[str, int] = {}
    for key in functional_keys:
        for e in data.get(key, []):
            counts[e["parse_status"]] = counts.get(e["parse_status"], 0) + 1
    for status in ("PARSED", "PARTIAL", "UNPARSED"):
        if status not in counts:
            counts[status] = 0
    data["parse_status_counts"] = counts

    return data


def render_markdown(inv: Inventory) -> str:
    data = to_json(inv)
    lines: list[str] = [
        "# A13 SystemUI Gate Inventory",
        "",
        f"**Schema version:** {SCHEMA_VERSION}",
        f"**Generated from:** {', '.join(data['generated_from'])}",
        "",
        "This document records the raw conditions found in SystemUI startup gating.",
        "It does not judge INSTALLER_ONLY, GATE_ONLY, DEFAULT_MISMATCH, etc.",
        "",
    ]

    lines.append("## Category counts")
    lines.append("")
    lines.append("| category | count |")
    lines.append("|----------|-------|")
    for cat in sorted(data["category_counts"]):
        lines.append(f"| {cat} | {data['category_counts'][cat]} |")
    lines.append("")

    lines.append("## Parse status counts")
    lines.append("")
    lines.append("| parse_status | count |")
    lines.append("|--------------|-------|")
    for status in ("PARSED", "PARTIAL", "UNPARSED"):
        lines.append(f"| {status} | {data['parse_status_counts'].get(status, 0)} |")
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
        ("PARTIAL", inv.partial),
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
            if e.nesting_depth:
                lines.append(f"- nesting_depth: {e.nesting_depth}")
            if e.parent_condition_id:
                lines.append(f"- parent_condition_id: `{e.parent_condition_id}`")
            if e.branch_kind:
                lines.append(f"- branch_kind: `{e.branch_kind}`")
            if e.feature_id:
                lines.append(f"- feature_id: `{e.feature_id}`")
            if e.declared_preference_keys:
                lines.append(f"- declared_preference_keys: {e.declared_preference_keys}")
            if e.condition_preference_keys:
                lines.append(f"- condition_preference_keys: {e.condition_preference_keys}")
            if e.preference_key_difference:
                lines.append(f"- preference_key_difference: {e.preference_key_difference}")
            if e.preference_keys:
                lines.append(f"- preference_keys: {e.preference_keys}")
            if e.accessors:
                lines.append(f"- accessors: {e.accessors}")
            if e.default_values:
                lines.append(f"- default_values: {e.default_values}")
            if e.default_kinds:
                lines.append(f"- default_kinds: {e.default_kinds}")
            if e.comparators:
                lines.append(f"- comparators: {e.comparators}")
            if e.boolean_operators:
                lines.append(f"- boolean_operators: {e.boolean_operators}")
            lines.append(f"- raw_expression: `{e.raw_expression}`")
            if e.normalized_expression and e.normalized_expression != e.raw_expression:
                lines.append(f"- normalized_expression: `{e.normalized_expression}`")
            lines.append("")

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# CLI and verification
# ---------------------------------------------------------------------------


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
    json_path.write_text(json_text, encoding="utf-8", newline="\n")
    md_path.write_text(md_text, encoding="utf-8", newline="\n")

    return json_path, md_path, json_text, md_text


def main() -> int:
    parser = argparse.ArgumentParser(description="SystemUI gate condition inventory")
    parser.add_argument("--output", type=Path, default=REPO_ROOT, help="repo root path")
    parser.add_argument("--verify", action="store_true", help="verify existing output is up to date")
    parser.add_argument("--sha256", action="store_true", help="print SHA-256 of the generated outputs")
    args = parser.parse_args()

    repo_root = args.output.resolve()
    json_path, md_path, json_text, md_text = write_inventory(repo_root, verify=args.verify)

    if args.sha256:
        j = hashlib.sha256(json_text.encode("utf-8")).hexdigest()
        m = hashlib.sha256(md_text.encode("utf-8")).hexdigest()
        print(f"JSON  {json_path}: {j}")
        print(f"MD    {md_path}: {m}")
    elif args.verify:
        print(f"Verified {json_path}")
        print(f"Verified {md_path}")
    else:
        print(f"Wrote {json_path}")
        print(f"Wrote {md_path}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
