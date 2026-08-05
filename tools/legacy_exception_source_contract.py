#!/usr/bin/env python3
"""Read-only source contract parser for A13 LEGACY_EXCEPTION registry.

This module derives activation contracts, call-site conditions and preference-key
roles directly from production Java/Kotlin source. It is intentionally not
allowed to generate registry expected values from `build_legacy_exception_registry`
or `LEGACY_EXCEPTION_SEEDS`.
"""

from __future__ import annotations

import re
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
SOURCE_ROOT = REPO_ROOT / "app" / "src" / "main" / "java"


class SourceContractError(Exception):
    """Raised when source-derived contract extraction fails."""


class SourceStructureError(Exception):
    """Raised when source code structure is not as expected."""


def read_source(rel: str) -> str:
    return (SOURCE_ROOT / rel).read_text(encoding="utf-8")


def _balanced_range(text: str, start: int, open_ch: str, close_ch: str) -> str | None:
    """Return the substring from `start` to the matching `close_ch`, inclusive.

    Handles string literals and both `//` and `/* */` comments.
    """
    if start >= len(text) or text[start] != open_ch:
        return None
    i = start
    depth = 0
    n = len(text)
    in_string: str | None = None
    in_line_comment = False
    in_block_comment = False
    while i < n:
        ch = text[i]
        if in_line_comment:
            if ch == "\n":
                in_line_comment = False
            i += 1
            continue
        if in_block_comment:
            if ch == "*" and i + 1 < n and text[i + 1] == "/":
                in_block_comment = False
                i += 2
                continue
            i += 1
            continue
        if in_string:
            if ch == "\\" and i + 1 < n:
                i += 2
                continue
            if ch == in_string:
                in_string = None
            i += 1
            continue
        if ch == "/" and i + 1 < n:
            if text[i + 1] == "/":
                in_line_comment = True
                i += 2
                continue
            if text[i + 1] == "*":
                in_block_comment = True
                i += 2
                continue
        if ch == '"' or ch == "'":
            in_string = ch
        elif ch == open_ch:
            depth += 1
        elif ch == close_ch:
            depth -= 1
            if depth == 0:
                return text[start : i + 1]
        i += 1
    return None


def _line_number(text: str, pos: int) -> int:
    return text[:pos].count("\n") + 1


def _find_next(text: str, start: int, chars: str) -> int:
    i = start
    n = len(text)
    while i < n:
        if text[i] in chars:
            return i
        i += 1
    return -1


def _skip_whitespace(text: str, start: int) -> int:
    n = len(text)
    i = start
    while i < n:
        if not text[i].isspace():
            return i
        i += 1
    return n


def _in_string_or_comment(text: str, pos: int) -> bool:
    """Return True if `pos` lies inside a string literal or Java/Kotlin comment."""
    in_string: str | None = None
    in_line_comment = False
    in_block_comment = False
    for i, ch in enumerate(text):
        if i > pos:
            return False
        if in_line_comment:
            if i == pos:
                return True
            if ch == "\n":
                in_line_comment = False
            continue
        if in_block_comment:
            if i == pos:
                return True
            if ch == "*" and i + 1 < len(text) and text[i + 1] == "/":
                in_block_comment = False
            continue
        if in_string:
            if i == pos:
                return True
            if ch == "\\" and i + 1 < len(text):
                continue
            if ch == in_string:
                in_string = None
            continue
        if ch == "/" and i + 1 < len(text):
            nxt = text[i + 1]
            if nxt == "/":
                in_line_comment = True
                continue
            if nxt == "*":
                in_block_comment = True
                continue
        if ch == '"' or ch == "'":
            in_string = ch
            if i == pos:
                return True
        if i == pos:
            return False
    return False


def _skip_whitespace_and_comments(text: str, start: int) -> int:
    """Return the first index >= start that is not whitespace or a comment."""
    n = len(text)
    i = start
    in_line_comment = False
    in_block_comment = False
    in_string: str | None = None
    while i < n:
        ch = text[i]
        if in_line_comment:
            if ch == "\n":
                in_line_comment = False
            i += 1
            continue
        if in_block_comment:
            if ch == "*" and i + 1 < n and text[i + 1] == "/":
                in_block_comment = False
                i += 2
                continue
            i += 1
            continue
        if in_string:
            if ch == "\\" and i + 1 < n:
                i += 2
                continue
            if ch == in_string:
                in_string = None
            i += 1
            continue
        if ch.isspace():
            i += 1
            continue
        if ch == "/" and i + 1 < n:
            nxt = text[i + 1]
            if nxt == "/":
                in_line_comment = True
                i += 2
                continue
            if nxt == "*":
                in_block_comment = True
                i += 2
                continue
        if ch == '"' or ch == "'":
            in_string = ch
            i += 1
            continue
        return i
    return n


def _closing_for_open_paren(text: str, open_pos: int) -> int:
    """Find the index of the `)` matching the `(` at open_pos."""
    block = _balanced_range(text, open_pos, "(", ")")
    if block is None:
        return -1
    return open_pos + len(block) - 1


def _closing_for_open_brace(text: str, open_pos: int) -> int:
    block = _balanced_range(text, open_pos, "{", "}")
    if block is None:
        return -1
    return open_pos + len(block) - 1


def _strip_outer_parens(expr: str) -> str:
    expr = expr.strip()
    while len(expr) >= 2 and expr[0] == "(" and expr[-1] == ")":
        close = _closing_for_open_paren(expr, 0)
        if close == len(expr) - 1:
            expr = expr[1:-1].strip()
        else:
            break
    return expr


def _find_function_definition(text: str, func_name: str, language: str) -> tuple[int, int] | None:
    """Return (match_start, brace_start) for the unique function definition.

    The match must be a real declaration, not a call or a string/comment.
    Multiple matching definitions, Kotlin expression bodies, or unsupported
    declaration forms are treated as errors and raise SourceStructureError.
    """
    if language == "kt":
        pattern = re.compile(rf"\bfun\s+{re.escape(func_name)}\s*\(")
    else:
        pattern = re.compile(rf"(?<![(\w])\b{re.escape(func_name)}\s*\(")

    candidates: list[tuple[int, int]] = []
    for match in pattern.finditer(text):
        # Locate the actual opening parenthesis of the parameter list.
        paren = _find_next(text, match.start(), "(")
        if paren == -1:
            continue
        if _in_string_or_comment(text, match.start()):
            continue
        params = _balanced_range(text, paren, "(", ")")
        if params is None:
            continue
        params_end = paren + len(params) - 1
        # Skip Java throws/annotations and Kotlin return-type annotations.
        i = _skip_whitespace_and_comments(text, params_end + 1)
        while i < len(text):
            # Java: `throws Exception, ...`
            if text.startswith("throws", i):
                i += 6
                while i < len(text):
                    if text[i] in ",":
                        i += 1
                        i = _skip_whitespace_and_comments(text, i)
                        continue
                    if text[i].isspace():
                        i = _skip_whitespace_and_comments(text, i)
                        continue
                    if text[i] == "{" or text[i] == ";" or text[i] == "=" or text[i] == "(":
                        break
                    i += 1
                continue
            break
        # Kotlin return-type annotation `fun f(): Type {` or `fun f(): Type = ...`
        if i < len(text) and text[i] == ":":
            i += 1
            i = _skip_whitespace_and_comments(text, i)
            while i < len(text):
                ch = text[i]
                if ch.isspace():
                    i = _skip_whitespace_and_comments(text, i)
                    continue
                if ch.isalnum() or ch == "_" or ch == "." or ch == "?":
                    i += 1
                    continue
                if ch == "<":
                    block = _balanced_range(text, i, "<", ">")
                    if block is not None:
                        i += len(block)
                        continue
                break
        if i >= len(text):
            continue
        nxt = text[i]
        if nxt == "{":
            candidates.append((match.start(), i))
            continue
        if nxt == "=":
            raise SourceStructureError(
                f"{func_name} has an expression body; that is not supported by the source contract parser"
            )
        # Any other next token is not a block-body definition (call, type cast,
        # statement, etc.): ignore this candidate.
    if not candidates:
        return None
    if len(candidates) > 1:
        raise SourceStructureError(
            f"{func_name} has multiple definitions or overloads; source contract extraction is ambiguous"
        )
    return candidates[0]


def extract_function_body(text: str, func_name: str, language: str = "java") -> str | None:
    loc = _find_function_definition(text, func_name, language)
    if loc is None:
        return None
    _, brace = loc
    return _balanced_range(text, brace, "{", "}")


def extract_balanced_range(text: str, start: int) -> str | None:
    return _balanced_range(text, start, "{", "}")


def parse_boolean_expression(expr: str) -> tuple[list[str], list[str]]:
    """Split a boolean expression into top-level operands and operators.

    Returns (operands, operators) where operators are drawn from {'&&', '||'}.
    Operands preserve their original text (with whitespace collapsed to a single
    space and outer parentheses stripped once).
    """
    expr = _strip_outer_parens(expr.strip())
    operands: list[str] = []
    operators: list[str] = []
    n = len(expr)
    i = 0
    depth = 0
    cur_start = 0
    in_string: str | None = None
    in_line_comment = False
    in_block_comment = False

    while i < n:
        ch = expr[i]
        if in_line_comment:
            if ch == "\n":
                in_line_comment = False
            i += 1
            continue
        if in_block_comment:
            if i + 1 < n and ch == "*" and expr[i + 1] == "/":
                in_block_comment = False
                i += 2
                continue
            i += 1
            continue
        if in_string:
            if ch == "\\" and i + 1 < n:
                i += 2
                continue
            if ch == in_string:
                in_string = None
            i += 1
            continue
        if ch == "/" and i + 1 < n:
            if expr[i + 1] == "/":
                in_line_comment = True
                i += 2
                continue
            if expr[i + 1] == "*":
                in_block_comment = True
                i += 2
                continue
        if ch == '"' or ch == "'":
            in_string = ch
            i += 1
            continue
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        elif depth == 0:
            if i + 1 < n and expr[i] == "&" and expr[i + 1] == "&":
                op = expr[cur_start:i].strip()
                if op:
                    operands.append(_strip_outer_parens(op))
                operators.append("&&")
                i += 2
                cur_start = i
                continue
            if i + 1 < n and expr[i] == "|" and expr[i + 1] == "|":
                op = expr[cur_start:i].strip()
                if op:
                    operands.append(_strip_outer_parens(op))
                operators.append("||")
                i += 2
                cur_start = i
                continue
        i += 1

    trailing = expr[cur_start:].strip()
    if trailing:
        operands.append(_strip_outer_parens(trailing))

    # Normalize whitespace within each operand.
    operands = [re.sub(r"\s+", " ", op).strip() for op in operands]
    return operands, operators


def _normalize_expr(expr: str) -> str:
    return re.sub(r"\s+", " ", _strip_outer_parens(expr).strip())


def _contains_token(expr: str, token: str) -> bool:
    return _normalize_expr(expr).find(_normalize_expr(token)) != -1


def _find_next_at_depth(text: str, start: int, char: str) -> int:
    """Return the next `char` at brace/comment/string-aware depth 0."""
    n = len(text)
    i = start
    depth = 0
    in_string: str | None = None
    in_line_comment = False
    in_block_comment = False
    while i < n:
        ch = text[i]
        if in_line_comment:
            if ch == "\n":
                in_line_comment = False
            i += 1
            continue
        if in_block_comment:
            if ch == "*" and i + 1 < n and text[i + 1] == "/":
                in_block_comment = False
                i += 2
                continue
            i += 1
            continue
        if in_string:
            if ch == "\\" and i + 1 < n:
                i += 2
                continue
            if ch == in_string:
                in_string = None
            i += 1
            continue
        if ch == "/" and i + 1 < n:
            nxt = text[i + 1]
            if nxt == "/":
                in_line_comment = True
                i += 2
                continue
            if nxt == "*":
                in_block_comment = True
                i += 2
                continue
        if ch == '"' or ch == "'":
            in_string = ch
            i += 1
            continue
        if ch in "({[":
            depth += 1
        elif ch in ")]}":
            depth -= 1
        elif ch == char and depth == 0:
            return i
        i += 1
    return -1


def _extract_all_if_blocks(text: str, min_depth: int = 1) -> list[dict]:
    """Extract all `if (condition) { body }` blocks in `text`.

    Returns a list of dicts with `condition`, `body`, `start`, `end`,
    `body_start`, `body_end`.  Candidates inside strings or comments are
    ignored, and candidate `if` tokens are checked to avoid matches inside
    identifiers.
    """
    blocks: list[dict] = []
    for m in re.finditer(r"\bif\s*\(", text):
        # Skip `if` tokens that are inside string literals or comments, or that
        # are part of a larger identifier (e.g. `notifyif(` would not match).
        if _in_string_or_comment(text, m.start()):
            continue
        open_paren = text.find("(", m.start())
        if open_paren == -1:
            continue
        close_paren = _closing_for_open_paren(text, open_paren)
        if close_paren == -1:
            continue
        condition = text[open_paren + 1 : close_paren].strip()
        after = _skip_whitespace_and_comments(text, close_paren + 1)
        if after >= len(text) or text[after] != "{":
            # one-statement if: find the next `;` at the same nesting level,
            # skipping string/comment boundaries and nested braces.
            stmt_end = _find_next_at_depth(text, after, ";")
            if stmt_end == -1:
                continue
            body = text[after : stmt_end + 1]
            blocks.append(
                {
                    "condition": condition,
                    "body": body,
                    "start": m.start(),
                    "end": stmt_end,
                    "body_start": after,
                    "body_end": stmt_end,
                }
            )
        else:
            body_end = _closing_for_open_brace(text, after)
            if body_end == -1:
                continue
            body = text[after : body_end + 1]
            blocks.append(
                {
                    "condition": condition,
                    "body": body,
                    "start": m.start(),
                    "end": body_end,
                    "body_start": after,
                    "body_end": body_end,
                }
            )
    return blocks


def _find_top_level_returns(body: str) -> list[dict]:
    """Find return statements that are not nested inside inner blocks/lambdas.

    Returns a list of dicts with `expr` (the expression text) and `start`.
    """
    # We scan the function body at depth 1 (directly inside the outermost
    # braces) and also at the statement level inside try/catch blocks.  To keep
    # this simple and robust, we look for `return` tokens that are followed by
    # either `;` or a `{`/`}` boundary, and are not inside nested braces.
    results: list[dict] = []
    n = len(body)
    i = 0
    depth = 0
    in_string: str | None = None
    in_line_comment = False
    in_block_comment = False
    while i < n:
        ch = body[i]
        if in_line_comment:
            if ch == "\n":
                in_line_comment = False
            i += 1
            continue
        if in_block_comment:
            if i + 1 < n and ch == "*" and body[i + 1] == "}":
                pass
            if i + 1 < n and ch == "*" and body[i + 1] == "/":
                in_block_comment = False
                i += 2
                continue
            i += 1
            continue
        if in_string:
            if ch == "\\" and i + 1 < n:
                i += 2
                continue
            if ch == in_string:
                in_string = None
            i += 1
            continue
        if ch == "/" and i + 1 < n:
            if body[i + 1] == "/":
                in_line_comment = True
                i += 2
                continue
            if body[i + 1] == "*":
                in_block_comment = True
                i += 2
                continue
        if ch == '"' or ch == "'":
            in_string = ch
        elif ch == "{" or ch == "(" or ch == "[":
            depth += 1
        elif ch == "}" or ch == ")" or ch == "]":
            depth -= 1
        elif depth == 1 and body.startswith("return", i):
            # Ensure it is a real `return` token.
            if i + 6 < n and body[i + 6].isalnum():
                i += 1
                continue
            start = i
            i += 6
            while i < n and body[i].isspace():
                i += 1
            expr_start = i
            # Find the end of the expression: the next `;` at depth 1.
            expr_depth = 0
            while i < n:
                c = body[i]
                if c == ";" and expr_depth == 0:
                    expr = body[expr_start:i].strip()
                    results.append({"expr": expr, "start": start, "end": i})
                    i += 1
                    break
                if c in "({[":
                    expr_depth += 1
                elif c in ")]}":
                    expr_depth -= 1
                elif c == '"' or c == "'":
                    j = i + 1
                    while j < n:
                        if body[j] == "\\":
                            j += 2
                            continue
                        if body[j] == c:
                            break
                        j += 1
                    i = j
                i += 1
            continue
        i += 1
    return results


def _extract_preference_keys(text: str) -> set[str]:
    """Extract literal preference key strings used in `MainModule.mPrefs` calls."""
    pattern = re.compile(
        r"MainModule\.mPrefs\."
        r"(?:getStringSet|getStringAsInt|getBoolean|getInt|getString|getLong)"
        r"\s*\(\s*\"([^\"]+)\""
    )
    return {m.group(1) for m in pattern.finditer(text)}


def _is_action_chain(condition: str) -> bool:
    """Check whether `condition` is the exact dynamic _action guard chain."""
    operands, operators = parse_boolean_expression(condition)
    if len(operands) != 4 or operators != ["&&", "&&", "&&"]:
        return False
    norm = [_normalize_expr(op) for op in operands]
    # Each required sub-expression must appear once in order.
    # Patterns allow Java-style whitespace; `_action` suffix, `Integer` type
    # guard and `> 1` threshold are all mandatory.
    required = [
        (r"key\s*!=\s*null|null\s*!=\s*key",),
        (r"key\.endsWith\s*\(\s*\"_action\"\s*\)",),
        (r"value\s+instanceof\s+Integer",),
        (r"(?:\(\s*Integer\s*\)\s*)?value\s*>\s*1",),
    ]
    for req in required:
        found = False
        for op in norm:
            for r in req:
                if re.search(r, op):
                    found = True
                    break
            if found:
                break
        if not found:
            return False
    return True


def _is_media_condition(condition: str) -> bool:
    """Check whether `condition` is the media up/down OR condition."""
    operands, operators = parse_boolean_expression(condition)
    if len(operands) != 2 or operators != ["||"]:
        return False
    up_found = False
    down_found = False
    for op in operands:
        n = _normalize_expr(op)
        if "controls_volumemedia_up" in n and re.search(r">\s*0", n):
            up_found = True
        if "controls_volumemedia_down" in n and re.search(r">\s*0", n):
            down_found = True
    return up_found and down_found


def _is_app_set_nonempty_expr(expr: str) -> bool:
    """Check whether `expr` is `!getStringSet("controls_mediaplayer_apps").isEmpty()`."""
    n = _normalize_expr(expr)
    if not n.startswith("!"):
        return False
    n = n[1:].strip()
    if not n.startswith("("):
        n = "(" + n
    if not n.endswith(")"):
        n = n + ")"
    return (
        "MainModule.mPrefs.getStringSet(\"controls_mediaplayer_apps\").isEmpty()" in n
    )


def _is_media_return_expr(expr: str) -> bool:
    """Check whether `expr` is `(up||down) && !appSet.isEmpty()` or the
    semantically equivalent `if`-form.  The latter is handled by callers."""
    operands, operators = parse_boolean_expression(expr)
    if len(operands) != 2 or operators != ["&&"]:
        return False
    left, right = operands
    l_ops, l_ors = parse_boolean_expression(left)
    if len(l_ops) != 2 or l_ors != ["||"]:
        return False
    up_found = False
    down_found = False
    for op in l_ops:
        n = _normalize_expr(op)
        if "controls_volumemedia_up" in n and re.search(r">\s*0", n):
            up_found = True
        if "controls_volumemedia_down" in n and re.search(r">\s*0", n):
            down_found = True
    return up_found and down_found and _is_app_set_nonempty_expr(right)


def _find_if_by_condition(body: str, matcher: callable) -> dict | None:
    for block in _extract_all_if_blocks(body):
        if matcher(block["condition"]):
            return block
    return None


def derive_setup_global_actions_activation() -> dict:
    """Derive the activation contract for GlobalActions.setupGlobalActions."""
    text = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
    body = extract_function_body(text, "needGlobalActions", "java")
    if body is None:
        raise SourceContractError("needGlobalActions body not extractable")

    # Branch A: dynamic _action predicate
    if not _find_if_by_condition(body, _is_action_chain):
        raise SourceContractError("needGlobalActions action chain not found")

    # Branch B: media keys + app-set non-empty
    media_branch = _find_if_by_condition(body, _is_media_condition)
    if media_branch is not None:
        # The branch must contain a return of the app-set negation.
        if not _is_app_set_nonempty_expr(_extract_return_in_block(media_branch["body"])):
            raise SourceContractError("media branch app-set negation not found")
    else:
        # Alternatively the function may return the whole combined expression.
        returns = _find_top_level_returns(body)
        found = False
        for ret in returns:
            if _is_media_return_expr(ret["expr"]):
                found = True
                break
        if not found:
            raise SourceContractError("media branch not found")

    return {
        "mode": "ANY_OF",
        "predicates": [
            {
                "kind": "DYNAMIC_SUFFIX_INT_GT",
                "keySuffix": "_action",
                "thresholdExclusive": 1,
                "valueType": "INTEGER",
            },
            {
                "kind": "FIXED_INT_ANY_GT_AND_NONEMPTY_SET",
                "integerKeys": sorted(["controls_volumemedia_up", "controls_volumemedia_down"]),
                "thresholdExclusive": 0,
                "requiredNonEmptySetKey": "controls_mediaplayer_apps",
            },
        ],
    }


def _extract_return_in_block(block: str) -> str:
    """Return the expression of the first top-level `return` inside `block`."""
    returns = _find_top_level_returns(block)
    if not returns:
        return ""
    return returns[0]["expr"]


def _is_foreground_activation_condition(condition: str) -> tuple[bool, list[dict] | None]:
    """Check whether condition is the installer OR for setupForegroundMonitor.
    Return (ok, predicates) if it can be derived."""
    operands, operators = parse_boolean_expression(condition)
    if len(operands) != 2 or operators != ["||"]:
        return False, None
    predicates: list[dict] = []
    for op in operands:
        n = _normalize_expr(op)
        keys = _extract_preference_keys(op)
        if "various_showcallui" in keys and re.search(r"getStringAsInt.*>\s*0", n):
            predicates.append({"kind": "INT_KEY_GT", "key": "various_showcallui", "thresholdExclusive": 0})
        elif "controls_volumecursor" in keys and "getBoolean" in n:
            predicates.append({"kind": "BOOLEAN_KEY_TRUE", "key": "controls_volumecursor"})
        else:
            return False, None
    return True, predicates


def _get_call_pos(text: str, call: str) -> int:
    m = re.search(re.escape(call), text)
    if not m:
        raise SourceContractError(f"call {call!r} not found")
    return m.start()


def get_enclosing_if_condition(text: str, call_pos: int) -> str | None:
    """Return the condition of the innermost `if` that encloses the call at
    `call_pos`.  Return None if the call is not inside an `if`."""
    # Find all `if (` before the call, nearest first.
    positions = [m.start() for m in re.finditer(r"\bif\s*\(", text) if m.start() < call_pos]
    for if_start in reversed(positions):
        open_paren = text.find("(", if_start)
        if open_paren == -1:
            continue
        close_paren = _closing_for_open_paren(text, open_paren)
        if close_paren == -1 or close_paren > call_pos:
            continue
        condition = text[open_paren + 1 : close_paren].strip()
        after = _skip_whitespace(text, close_paren + 1)
        if after >= len(text):
            continue
        if text[after] == "{":
            body_end = _closing_for_open_brace(text, after)
            if body_end == -1:
                continue
            if after < call_pos < body_end:
                return condition
        else:
            # one-statement if: body extends to the next `;` at depth 0
            stmt_end = text.find(";", after)
            if stmt_end == -1:
                continue
            if after <= call_pos < stmt_end:
                return condition
    return None


def derive_setup_foreground_monitor_activation() -> dict:
    text = read_source("tv/withaibuild/customiuizer/installers/SystemUiInstaller.java")
    call = "GlobalActions.setupForegroundMonitor(lpparam);"
    call_pos = _get_call_pos(text, call)
    condition = get_enclosing_if_condition(text, call_pos)
    if condition is None:
        raise SourceContractError("setupForegroundMonitor installer condition not found")
    ok, predicates = _is_foreground_activation_condition(condition)
    if not ok:
        raise SourceContractError(f"setupForegroundMonitor installer condition not derivable: {condition}")
    return {"mode": "ANY_OF", "predicates": sorted(predicates, key=lambda p: (p["kind"], p.get("key", "")))}


def _is_showcallui_condition(condition: str) -> tuple[bool, dict | None]:
    operands, operators = parse_boolean_expression(condition)
    if len(operands) != 1 or operators:
        return False, None
    n = _normalize_expr(operands[0])
    if "various_showcallui" in n and re.search(r"getStringAsInt.*>\s*0", n):
        return True, {"kind": "INT_KEY_GT", "key": "various_showcallui", "thresholdExclusive": 0}
    return False, None


HOOK_CALL_RE = re.compile(
    r"(?:ModuleHelper|XposedHelpers|XposedBridge|HookerClassHelper)\s*\.\s*"
    r"(hookAllConstructors|hookAllMethods|findAndHookMethod|hookAll)"
)


def _extract_hook_target(call_text: str) -> str | None:
    """Extract a `class#method` string from a single hook call text."""
    call = call_text.strip()
    if "hookAllConstructors" in call:
        m = re.search(r'hookAllConstructors\s*\(\s*"([^"]+)"', call)
        if m:
            return f"{m.group(1)}#<init>"
    if "hookAllMethods" in call:
        # ModuleHelper.hookAllMethods("class", classLoader, "method", callback)
        m = re.search(r'hookAllMethods\s*\(\s*"([^"]+)"(?:[^,]*,){2}\s*"([^"]+)"', call)
        if m:
            return f"{m.group(1)}#{m.group(2)}"
    if "findAndHookMethod" in call:
        # findAndHookMethod(class, classLoader, "method", ...)
        m = re.search(r'findAndHookMethod\s*\([^,]*,[^,]*,\s*"([^"]+)"', call)
        if m:
            # Try to locate the class string before the method.
            cls = re.search(r'"([^"]+)"', call)
            method = m.group(1)
            if cls and cls.group(1) != method:
                return f"{cls.group(1)}#{method}"
    return None


def find_hook_calls_in_function(rel: str, func_name: str) -> list[dict]:
    """Return all hook calls in `func_name`, each with `line`, `text`, `target`."""
    text = read_source(rel)
    body = extract_function_body(text, func_name, "kt")
    if body is None:
        raise SourceContractError(f"{func_name} body not extractable from {rel}")
    func_match = _find_function_definition(text, func_name, "kt")
    if func_match is None:
        raise SourceContractError(f"{func_name} definition not found in {rel}")
    base_line = _line_number(text, func_match[0])
    calls: list[dict] = []
    for m in HOOK_CALL_RE.finditer(body):
        line = base_line + body[: m.start()].count("\n")
        call_text = body[m.start() : m.end()]
        # extend to the end of the call (next `)` matching the opening `(`)
        paren = body.find("(", m.start())
        if paren != -1:
            close = _closing_for_open_paren(body, paren)
            if close != -1:
                call_text = body[m.start() : close + 1]
        calls.append({
            "line": line,
            "text": call_text,
            "target": _extract_hook_target(call_text),
        })
    return calls


def _call_in_block(body: str, block: dict, call_text: str) -> bool:
    """Check whether a hook call string occurs inside a balanced block."""
    block_body = block["body"]
    return call_text in block_body


def derive_setup_foreground_monitor_call_site_conditions() -> dict[int, dict]:
    """Derive call-site conditions for setupForegroundMonitor hook calls.

    Returns a dict mapping line number to condition predicate.  Calls with no
    additional condition are omitted.
    """
    body = extract_function_body(
        read_source("tv/withaibuild/customiuizer/mods/GlobalActions.kt"),
        "setupForegroundMonitor",
        "kt",
    )
    if body is None:
        raise SourceContractError("setupForegroundMonitor body not extractable")
    calls = find_hook_calls_in_function(
        "tv/withaibuild/customiuizer/mods/GlobalActions.kt",
        "setupForegroundMonitor",
    )
    conditions: dict[int, dict] = {}
    for call in calls:
        call_text = call["text"]
        # Find the innermost if block containing this call.
        enclosing: dict | None = None
        for block in _extract_all_if_blocks(body):
            if block["body_start"] < body.find(call_text) < block["body_end"]:
                if enclosing is None or block["body_start"] > enclosing["body_start"]:
                    enclosing = block
        if enclosing is not None:
            ok, cond = _is_showcallui_condition(enclosing["condition"])
            if ok:
                conditions[call["line"]] = cond
    return conditions


def derive_alarm_compat_activation() -> dict:
    text = read_source("tv/withaibuild/customiuizer/installers/SystemServerInstaller.java")
    call = "Various.AlarmCompatServiceHook(lpparam);"
    call_pos = _get_call_pos(text, call)
    condition = get_enclosing_if_condition(text, call_pos)
    if condition is None:
        return {"mode": "UNCONDITIONAL"}
    keys = _extract_preference_keys(condition)
    if keys == {"various_alarmcompat"}:
        return {"mode": "ANY_OF", "predicates": [{"kind": "BOOLEAN_KEY_TRUE", "key": "various_alarmcompat"}]}
    raise SourceContractError(f"AlarmCompatServiceHook activation not derivable: {condition}")


def derive_setup_status_bar_activation() -> dict:
    text = read_source("tv/withaibuild/customiuizer/installers/SystemUiInstaller.java")
    call = "GlobalActions.setupStatusBar(lpparam);"
    call_pos = _get_call_pos(text, call)
    condition = get_enclosing_if_condition(text, call_pos)
    if condition is not None:
        raise SourceContractError("setupStatusBar installer unexpectedly has a condition")
    return {"mode": "UNCONDITIONAL"}


def _extract_runtime_config_keys_from_body(body: str, activation: dict, call_conditions: dict) -> list[str]:
    """Return the literal preference keys used in the function body that are not
    already part of the activation or call-site conditions."""
    body_keys = _extract_preference_keys(body)
    fixed = set()
    for p in activation.get("predicates", []):
        if p["kind"] == "BOOLEAN_KEY_TRUE":
            fixed.add(p["key"])
        elif p["kind"] == "INT_KEY_GT":
            fixed.add(p["key"])
        elif p["kind"] == "FIXED_INT_ANY_GT_AND_NONEMPTY_SET":
            fixed.update(p.get("integerKeys", []))
            fixed.add(p.get("requiredNonEmptySetKey", ""))
    for cond in call_conditions.values():
        if cond["kind"] == "BOOLEAN_KEY_TRUE":
            fixed.add(cond["key"])
        elif cond["kind"] == "INT_KEY_GT":
            fixed.add(cond["key"])
    runtime = body_keys - fixed
    return sorted(runtime)


def derive_runtime_config_keys(record_owner: str) -> list[str]:
    if record_owner == "AlarmCompatServiceHook":
        rel = "tv/withaibuild/customiuizer/mods/Various.kt"
        func = "AlarmCompatServiceHook"
        body = extract_function_body(read_source(rel), func, "kt")
        if body is None:
            raise SourceContractError("AlarmCompatServiceHook body not extractable")
        activation = derive_alarm_compat_activation()
        body_keys = _extract_preference_keys(body)
        fixed = {p["key"] for p in activation.get("predicates", []) if p["kind"] == "BOOLEAN_KEY_TRUE"}
        runtime = body_keys - fixed
        if "various_alarmcompat_apps" not in runtime:
            raise SourceContractError("various_alarmcompat_apps runtime key not found in AlarmCompatServiceHook body")
        return sorted(runtime)
    return []


def derive_record_preference_keys(record_owner: str) -> list[str]:
    """Derive the canonical preferenceKeys for a P3.3B record from source."""
    if record_owner == "GlobalActions.setupGlobalActions":
        activation = derive_setup_global_actions_activation()
        runtime: list[str] = []
    elif record_owner == "GlobalActions.setupForegroundMonitor":
        activation = derive_setup_foreground_monitor_activation()
        call_conditions = derive_setup_foreground_monitor_call_site_conditions()
        body = extract_function_body(
            read_source("tv/withaibuild/customiuizer/mods/GlobalActions.kt"),
            "setupForegroundMonitor",
            "kt",
        )
        runtime = _extract_runtime_config_keys_from_body(body, activation, call_conditions)
    elif record_owner == "GlobalActions.setupStatusBar":
        activation = derive_setup_status_bar_activation()
        runtime = []
    elif record_owner == "AlarmCompatServiceHook":
        activation = derive_alarm_compat_activation()
        runtime = derive_runtime_config_keys(record_owner)
    else:
        raise SourceContractError(f"unknown owner {record_owner}")

    fixed: set[str] = set()
    for p in activation.get("predicates", []):
        if p["kind"] == "BOOLEAN_KEY_TRUE":
            fixed.add(p["key"])
        elif p["kind"] == "INT_KEY_GT":
            fixed.add(p["key"])
        elif p["kind"] == "FIXED_INT_ANY_GT_AND_NONEMPTY_SET":
            fixed.update(p.get("integerKeys", []))
            fixed.add(p.get("requiredNonEmptySetKey", ""))
    for cond in derive_record_call_site_conditions(record_owner).values():
        if cond["kind"] == "BOOLEAN_KEY_TRUE":
            fixed.add(cond["key"])
        elif cond["kind"] == "INT_KEY_GT":
            fixed.add(cond["key"])
    return sorted((fixed | set(runtime)) - {""})


def derive_record_call_site_conditions(record_owner: str) -> dict[int, dict]:
    if record_owner == "GlobalActions.setupForegroundMonitor":
        return derive_setup_foreground_monitor_call_site_conditions()
    return {}
