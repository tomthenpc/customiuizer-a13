#!/usr/bin/env python3
"""SystemUI startup gate differential audit (R1-B2).

Compares the SystemUiInstaller runtime conditions, FeatureCatalog feature
conditions, and the hasAnySystemUiStartupFeature startup gate.  Produces a
canonical structural diff with provenance rather than a simple key-set diff.
"""

import argparse
import copy
import importlib
import shutil
import tempfile
import json
import os
import re
import sys
from collections.abc import Callable
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parent.parent

SCHEMA_VERSION = "1.0"

PREF_METHODS = {"getBoolean", "getInt", "getStringAsInt", "getString", "getStringSet"}

# Known PrefMap implicit defaults.  These match the documented contract used by
# R1-B1; they are only used when default_kind is IMPLICIT or when R1-B1 left a
# method as UNKNOWN_DEFAULT because the source omitted an explicit default.
IMPLICIT_DEFAULTS: dict[str, Any] = {
    "getBoolean": False,
    "getInt": 0,
    "getStringAsInt": 0,
    "getString": "",
    "getStringSet": [],
}

UNKNOWN_SENTINEL = "__UNKNOWN__"

# ---------------------------------------------------------------------------
# Canonical condition AST
# ---------------------------------------------------------------------------


@dataclass(frozen=True, eq=False)
class Node:
    """Base class for canonical condition AST nodes."""

    def to_dict(self) -> dict[str, Any]:
        return node_to_dict(self)


@dataclass(frozen=True, eq=False)
class PREF_ACCESS(Node):
    method: str
    key: str
    default: Any
    default_kind: str

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, PREF_ACCESS):
            return NotImplemented
        if self.method != other.method or self.key != other.key:
            return False
        if self.default_kind == UNKNOWN_SENTINEL or other.default_kind == UNKNOWN_SENTINEL:
            return False
        return self.default == other.default

    def __hash__(self) -> int:
        return hash((self.method, self.key, self.default))


@dataclass(frozen=True)
class LITERAL(Node):
    value: Any


@dataclass(frozen=True)
class ID(Node):
    name: str


@dataclass(frozen=True)
class CALL(Node):
    receiver: Node | None
    method: str
    args: tuple[Any, ...]


@dataclass(frozen=True)
class FIELD(Node):
    receiver: Node
    name: str


@dataclass(frozen=True)
class COMPARE(Node):
    op: str
    left: Node
    right: Node


@dataclass(frozen=True)
class NOT(Node):
    child: Node


@dataclass(frozen=True)
class AND(Node):
    children: tuple[Any, ...]


@dataclass(frozen=True)
class OR(Node):
    children: tuple[Any, ...]


@dataclass(frozen=True)
class IS_EMPTY(Node):
    child: Node


@dataclass(frozen=True)
class IS_NOT_EMPTY(Node):
    child: Node


@dataclass(frozen=True)
class EQUALS(Node):
    left: Node
    right: Node


@dataclass(frozen=True)
class OPAQUE(Node):
    expr: str


@dataclass(frozen=True)
class UNKNOWN(Node):
    expr: str


# ---------------------------------------------------------------------------
# Node utilities
# ---------------------------------------------------------------------------


def _freeze(v: Any) -> Any:
    if isinstance(v, list):
        return tuple(_freeze(x) for x in v)
    if isinstance(v, tuple):
        return tuple(_freeze(x) for x in v)
    return v


def resolve_default(method: str, default_value: Any, default_kind: str) -> tuple[Any, str]:
    """Return the effective default value and a resolved/default kind."""
    if default_kind in ("EXPLICIT", "IMPLICIT_PREFMAP_DEFAULT"):
        return _freeze(default_value), default_kind
    if default_kind == "UNKNOWN_DEFAULT":
        known = IMPLICIT_DEFAULTS.get(method)
        if known is not None:
            return _freeze(known), "RESOLVED_IMPLICIT"
        return _freeze(default_value), UNKNOWN_SENTINEL
    return _freeze(default_value), default_kind


def node_sort_key(n: Node) -> str:
    """Stable string key for sorting canonical nodes."""
    return json.dumps(node_to_dict(n), sort_keys=True, default=str)


def node_to_dict(n: Node) -> dict[str, Any]:
    """Convert an AST node to a JSON-serializable dict."""
    name = type(n).__name__
    if isinstance(n, PREF_ACCESS):
        return {"kind": name, "method": n.method, "key": n.key, "default": n.default, "default_kind": n.default_kind}
    if isinstance(n, LITERAL):
        return {"kind": name, "value": n.value}
    if isinstance(n, ID):
        return {"kind": name, "name": n.name}
    if isinstance(n, CALL):
        return {"kind": name, "method": n.method, "args": [node_to_dict(a) for a in n.args], "receiver": node_to_dict(n.receiver) if n.receiver is not None else None}
    if isinstance(n, FIELD):
        return {"kind": name, "name": n.name, "receiver": node_to_dict(n.receiver)}
    if isinstance(n, COMPARE):
        return {"kind": name, "op": n.op, "left": node_to_dict(n.left), "right": node_to_dict(n.right)}
    if isinstance(n, NOT):
        return {"kind": name, "child": node_to_dict(n.child)}
    if isinstance(n, (AND, OR)):
        return {"kind": name, "children": [node_to_dict(c) for c in n.children]}
    if isinstance(n, (IS_EMPTY, IS_NOT_EMPTY)):
        return {"kind": name, "child": node_to_dict(n.child)}
    if isinstance(n, EQUALS):
        return {"kind": name, "left": node_to_dict(n.left), "right": node_to_dict(n.right)}
    if isinstance(n, (OPAQUE, UNKNOWN)):
        return {"kind": name, "expr": n.expr}
    return {"kind": name}


def canonical(node: Node) -> Node:
    """Return the canonical form of a node (flatten, sort, simplify NOT)."""
    if isinstance(node, AND):
        flat: list[Node] = []
        for c in node.children:
            cc = canonical(c)
            if isinstance(cc, AND):
                flat.extend(cc.children)
            else:
                flat.append(cc)
        flat = [canonical(c) for c in flat]
        # Remove duplicate nodes
        seen: set[str] = set()
        unique: list[Node] = []
        for c in flat:
            k = node_sort_key(c)
            if k not in seen:
                seen.add(k)
                unique.append(c)
        unique.sort(key=node_sort_key)
        return AND(tuple(unique))
    if isinstance(node, OR):
        flat: list[Node] = []  # type: ignore[no-redef]
        for c in node.children:
            cc = canonical(c)
            if isinstance(cc, OR):
                flat.extend(cc.children)
            else:
                flat.append(cc)
        flat = [canonical(c) for c in flat]
        seen = set()  # type: ignore[no-redef]
        unique = []  # type: ignore[no-redef]
        for c in flat:
            k = node_sort_key(c)
            if k not in seen:
                seen.add(k)
                unique.append(c)
        unique.sort(key=node_sort_key)
        return OR(tuple(unique))
    if isinstance(node, NOT):
        c = canonical(node.child)
        if isinstance(c, NOT):
            return c.child
        if isinstance(c, IS_EMPTY):
            return IS_NOT_EMPTY(c.child)
        if isinstance(c, IS_NOT_EMPTY):
            return IS_EMPTY(c.child)
        if isinstance(c, EQUALS):
            if isinstance(c.left, PREF_ACCESS) and isinstance(c.right, LITERAL) and c.right.value == "":
                return IS_NOT_EMPTY(c.left)
            if isinstance(c.right, PREF_ACCESS) and isinstance(c.left, LITERAL) and c.left.value == "":
                return IS_NOT_EMPTY(c.right)
        return NOT(c)
    if isinstance(node, EQUALS):
        left = canonical(node.left)
        right = canonical(node.right)
        if isinstance(left, PREF_ACCESS) and isinstance(right, LITERAL) and right.value == "":
            return IS_EMPTY(left)
        if isinstance(right, PREF_ACCESS) and isinstance(left, LITERAL) and left.value == "":
            return IS_EMPTY(right)
        return EQUALS(left, right)
    if isinstance(node, PREF_ACCESS):
        return PREF_ACCESS(node.method, node.key, _freeze(node.default), node.default_kind)
    if isinstance(node, LITERAL):
        return LITERAL(_freeze(node.value))
    if isinstance(node, CALL):
        r = canonical(node.receiver) if node.receiver is not None else None
        return CALL(r, node.method, tuple(canonical(a) for a in node.args))
    if isinstance(node, FIELD):
        return FIELD(canonical(node.receiver), node.name)
    if isinstance(node, COMPARE):
        return COMPARE(node.op, canonical(node.left), canonical(node.right))
    if isinstance(node, (IS_EMPTY, IS_NOT_EMPTY)):
        child = canonical(node.child)
        if isinstance(node, IS_EMPTY) and isinstance(child, PREF_ACCESS):
            return IS_EMPTY(child)
        return IS_NOT_EMPTY(child) if isinstance(node, IS_NOT_EMPTY) else IS_EMPTY(child)
    if isinstance(node, (OPAQUE, UNKNOWN, ID)):
        return node
    return node


# ---------------------------------------------------------------------------
# Tokenizer / parser for Java/Kotlin boolean expressions
# ---------------------------------------------------------------------------


@dataclass
class Token:
    type: str
    value: Any
    pos: int


def tokenize(expr: str) -> list[Token]:
    tokens: list[Token] = []
    i = 0
    n = len(expr)
    while i < n:
        c = expr[i]
        if c.isspace():
            i += 1
            continue
        if c == "(":
            tokens.append(Token("LPAREN", "(", i))
            i += 1
            continue
        if c == ")":
            tokens.append(Token("RPAREN", ")", i))
            i += 1
            continue
        if c == "{":
            tokens.append(Token("LBRACE", "{", i))
            i += 1
            continue
        if c == "}":
            tokens.append(Token("RBRACE", "}", i))
            i += 1
            continue
        if c == ".":
            tokens.append(Token("DOT", ".", i))
            i += 1
            continue
        if c == ",":
            tokens.append(Token("COMMA", ",", i))
            i += 1
            continue
        if c == ";":
            i += 1
            continue
        if c == "-" and i + 1 < n and expr[i + 1] == ">":
            tokens.append(Token("ARROW", "->", i))
            i += 2
            continue
        if c == "&" and i + 1 < n and expr[i + 1] == "&":
            tokens.append(Token("OP", "&&", i))
            i += 2
            continue
        if c == "|" and i + 1 < n and expr[i + 1] == "|":
            tokens.append(Token("OP", "||", i))
            i += 2
            continue
        if c == "!" and i + 1 < n and expr[i + 1] == "=":
            tokens.append(Token("OP", "!=", i))
            i += 2
            continue
        if c == "=" and i + 1 < n and expr[i + 1] == "=":
            tokens.append(Token("OP", "==", i))
            i += 2
            continue
        if c == ">" and i + 1 < n and expr[i + 1] == "=":
            tokens.append(Token("OP", ">=", i))
            i += 2
            continue
        if c == "<" and i + 1 < n and expr[i + 1] == "=":
            tokens.append(Token("OP", "<=", i))
            i += 2
            continue
        if c in "><":
            tokens.append(Token("OP", c, i))
            i += 1
            continue
        if c == "!":
            tokens.append(Token("OP", "!", i))
            i += 1
            continue
        if c == '"' or c == "'":
            start = i
            i += 1
            buf = ""
            while i < n:
                ch = expr[i]
                if ch == "\\" and i + 1 < n:
                    nxt = expr[i + 1]
                    if nxt == "n":
                        buf += "\n"
                    elif nxt == "t":
                        buf += "\t"
                    elif nxt == "r":
                        buf += "\r"
                    else:
                        buf += nxt
                    i += 2
                    continue
                if ch == c:
                    i += 1
                    break
                buf += ch
                i += 1
            tokens.append(Token("STRING", buf, start))
            continue
        if c.isdigit() or (c == "-" and i + 1 < n and expr[i + 1].isdigit()):
            start = i
            if c == "-":
                i += 1
            while i < n and (expr[i].isdigit() or expr[i] == "."):
                i += 1
            value = expr[start:i]
            if "." in value:
                tokens.append(Token("NUMBER", float(value), start))
            else:
                # Strip Java long/int suffixes.
                lit = value.rstrip("Ll")
                tokens.append(Token("NUMBER", int(lit), start))
            continue
        if c.isalpha() or c == "_":
            start = i
            while i < n and (expr[i].isalnum() or expr[i] == "_"):
                i += 1
            name = expr[start:i]
            if name == "true":
                tokens.append(Token("BOOL", True, start))
            elif name == "false":
                tokens.append(Token("BOOL", False, start))
            elif name == "null":
                tokens.append(Token("NULL", None, start))
            else:
                tokens.append(Token("ID", name, start))
            continue
        raise SyntaxError(f"Unexpected character {c!r} at position {i} in {expr!r}")
    tokens.append(Token("EOF", None, n))
    return tokens


class Parser:
    def __init__(
        self,
        tokens: list[Token],
        defaults: list[Any],
        default_kinds: list[str],
        local_vars: dict[str, str] | None = None,
        recursing: frozenset[str] | None = None,
    ):
        self.tokens = tokens
        self.pos = 0
        self.defaults = defaults
        self.default_kinds = default_kinds
        self.pref_idx = 0
        self.local_vars = local_vars or {}
        self.recursing = recursing or frozenset()

    def peek(self) -> Token:
        return self.tokens[self.pos]

    def consume(self, type: str | None = None, value: Any = None) -> Token:
        t = self.peek()
        if type is not None and t.type != type:
            raise SyntaxError(f"Expected token type {type}, got {t.type} at {t.pos}")
        if value is not None and t.value != value:
            raise SyntaxError(f"Expected token value {value!r}, got {t.value!r} at {t.pos}")
        self.pos += 1
        return t

    def match_op(self, value: str) -> bool:
        t = self.peek()
        if t.type == "OP" and t.value == value:
            self.pos += 1
            return True
        return False

    def parse(self) -> Node:
        node = self.parse_or()
        if self.peek().type != "EOF":
            raise SyntaxError(f"Unexpected trailing token {self.peek().value!r} at {self.peek().pos}")
        return canonical(node)

    def parse_or(self) -> Node:
        left = self.parse_and()
        while self.match_op("||"):
            right = self.parse_and()
            left = OR((left, right))
        return left

    def parse_and(self) -> Node:
        left = self.parse_not()
        while self.match_op("&&"):
            right = self.parse_not()
            left = AND((left, right))
        return left

    def parse_not(self) -> Node:
        if self.match_op("!"):
            return NOT(self.parse_not())
        return self.parse_comparison()

    def parse_comparison(self) -> Node:
        left = self.parse_additive()
        t = self.peek()
        if t.type == "OP" and t.value in ("==", "!=", ">=", "<=", ">", "<"):
            op = self.consume("OP").value
            right = self.parse_additive()
            return COMPARE(op, left, right)
        return left

    def parse_additive(self) -> Node:
        left = self.parse_multiplicative()
        while True:
            t = self.peek()
            if t.type == "OP" and t.value in ("+", "-"):
                op = self.consume("OP").value
                right = self.parse_multiplicative()
                left = OPAQUE(f"{to_expr(left)} {op} {to_expr(right)}")
            else:
                break
        return left

    def parse_multiplicative(self) -> Node:
        left = self.parse_unary()
        while True:
            t = self.peek()
            if t.type == "OP" and t.value in ("*", "/", "%"):
                op = self.consume("OP").value
                right = self.parse_unary()
                left = OPAQUE(f"{to_expr(left)} {op} {to_expr(right)}")
            else:
                break
        return left

    def parse_unary(self) -> Node:
        t = self.peek()
        if t.type == "OP" and t.value in ("+", "-"):
            op = self.consume("OP").value
            operand = self.parse_unary()
            if op == "-" and isinstance(operand, LITERAL) and isinstance(operand.value, (int, float)):
                return LITERAL(-operand.value)
            return OPAQUE(f"{op}{to_expr(operand)}")
        return self.parse_primary()

    def parse_primary(self) -> Node:
        t = self.peek()
        if t.type == "LPAREN":
            self.consume("LPAREN")
            e = self.parse_or()
            self.consume("RPAREN")
            return e
        if t.type == "STRING":
            self.consume("STRING")
            return LITERAL(t.value)
        if t.type == "NUMBER":
            self.consume("NUMBER")
            return LITERAL(t.value)
        if t.type == "BOOL":
            self.consume("BOOL")
            return LITERAL(t.value)
        if t.type == "NULL":
            self.consume("NULL")
            return LITERAL(None)
        if t.type == "ID":
            if t.value in self.local_vars and t.value not in self.recursing:
                self.consume("ID")
                return parse_expression(
                    self.local_vars[t.value],
                    [],
                    [],
                    local_vars=self.local_vars,
                    recursing=self.recursing | {t.value},
                )
            return self.parse_call_chain()
        if t.type == "LBRACE":
            # Lambda or block: try to extract the body.
            return self.parse_lambda_or_block()
        raise SyntaxError(f"Unexpected token {t.value!r} ({t.type}) at {t.pos}")

    def parse_call_chain(self) -> Node:
        name_tok = self.consume("ID")
        node: Node = ID(name_tok.value)
        # Function call without a receiver: setOf(...) / emptySet(...)
        if self.peek().type == "LPAREN":
            self.consume("LPAREN")
            args = self.parse_args()
            self.consume("RPAREN")
            node = CALL(None, name_tok.value, tuple(args))
        while self.peek().type == "DOT":
            self.consume("DOT")
            member = self.consume("ID").value
            if self.peek().type == "LPAREN":
                self.consume("LPAREN")
                args = self.parse_args()
                self.consume("RPAREN")
                node = CALL(node, member, tuple(args))
            else:
                node = FIELD(node, member)
        return self.normalize_pref_call(node)

    def parse_lambda_or_block(self) -> Node:
        self.consume("LBRACE")
        # skip parameters / arrow
        while self.peek().type != "ARROW" and self.peek().type != "EOF":
            self.pos += 1
        if self.peek().type == "ARROW":
            self.consume("ARROW")
        body = []
        depth = 1
        while depth > 0 and self.peek().type != "EOF":
            if self.peek().type == "LBRACE":
                depth += 1
            if self.peek().type == "RBRACE":
                depth -= 1
                if depth == 0:
                    self.consume("RBRACE")
                    break
            body.append(self.consume())
        body_text = ""
        for tok in body:
            if tok.type == "STRING":
                body_text += f'"{tok.value}"'
            else:
                body_text += str(tok.value)
        return OPAQUE(body_text)

    def parse_args(self) -> list[Node]:
        if self.peek().type == "RPAREN":
            return []
        args = [self.parse_or()]
        while self.peek().type == "COMMA":
            self.consume("COMMA")
            args.append(self.parse_or())
        return args

    def _pref_base(self, node: Node) -> bool:
        if isinstance(node, ID) and node.name in ("prefs", "mPrefs"):
            return True
        if isinstance(node, FIELD):
            if isinstance(node.receiver, ID) and node.receiver.name == "MainModule" and node.name == "mPrefs":
                return True
        return False

    def _literal_value(self, node: Node) -> Any:
        if isinstance(node, LITERAL):
            return node.value
        if isinstance(node, CALL):
            if node.method == "emptySet" and len(node.args) == 0:
                return []
            if node.method == "setOf":
                values = []
                for a in node.args:
                    if isinstance(a, LITERAL):
                        values.append(a.value)
                    else:
                        values.append(to_expr(a))
                return values
        if isinstance(node, UNKNOWN):
            return node.expr
        return to_expr(node)

    def normalize_pref_call(self, node: Node) -> Node:
        if isinstance(node, CALL):
            base = self._pref_base(node.receiver) if node.receiver is not None else False
            method = node.method
            if base and method in PREF_METHODS:
                if len(node.args) >= 1 and isinstance(node.args[0], LITERAL) and isinstance(node.args[0].value, str):
                    key = node.args[0].value
                    if len(node.args) > 1:
                        default_value = self._literal_value(node.args[1])
                        default_kind = "EXPLICIT"
                    else:
                        if self.pref_idx < len(self.defaults):
                            default_value = self.defaults[self.pref_idx]
                            default_kind = self.default_kinds[self.pref_idx] if self.pref_idx < len(self.default_kinds) else "UNKNOWN_DEFAULT"
                        else:
                            default_value = None
                            default_kind = "UNKNOWN_DEFAULT"
                    self.pref_idx += 1
                    effective, eff_kind = resolve_default(method, default_value, default_kind)
                    return PREF_ACCESS(method, key, effective, eff_kind)
                return OPAQUE(to_expr(node))
            if method == "isEmpty" and len(node.args) == 0:
                inner = self.normalize_pref_call(node.receiver) if node.receiver is not None else node.receiver
                if isinstance(inner, PREF_ACCESS):
                    return IS_EMPTY(inner)
                return OPAQUE(f"{to_expr(inner)}.isEmpty()")
            if method == "isNotEmpty" and len(node.args) == 0:
                inner = self.normalize_pref_call(node.receiver) if node.receiver is not None else node.receiver
                if isinstance(inner, PREF_ACCESS):
                    return IS_NOT_EMPTY(inner)
                return OPAQUE(f"{to_expr(inner)}.isNotEmpty()")
            if method == "equals" and len(node.args) == 1:
                inner = self.normalize_pref_call(node.receiver) if node.receiver is not None else node.receiver
                right = node.args[0]
                if isinstance(inner, PREF_ACCESS) and isinstance(right, LITERAL) and right.value == "":
                    return IS_EMPTY(inner)
                if isinstance(inner, PREF_ACCESS):
                    return EQUALS(inner, right)
                return OPAQUE(f"{to_expr(inner)}.equals({to_expr(right)})")
            if node.receiver is not None:
                return CALL(self.normalize_pref_call(node.receiver), method, tuple(self.normalize_pref_call(a) for a in node.args))
            return CALL(None, method, tuple(self.normalize_pref_call(a) for a in node.args))
        if isinstance(node, FIELD):
            r = self.normalize_pref_call(node.receiver) if isinstance(node.receiver, Node) else node.receiver
            if r is None or isinstance(r, OPAQUE):
                return OPAQUE(to_expr(node))
            return FIELD(r, node.name)
        return node


def to_expr(node: Node) -> str:
    """Best-effort reverse of an AST node for diagnostic messages."""
    if isinstance(node, PREF_ACCESS):
        return f"PREF.{node.method}(\"{node.key}\", default={node.default}, kind={node.default_kind})"
    if isinstance(node, LITERAL):
        return repr(node.value)
    if isinstance(node, ID):
        return node.name
    if isinstance(node, CALL):
        receiver = to_expr(node.receiver) if node.receiver is not None else ""
        return f"{receiver}.{node.method}({', '.join(to_expr(a) for a in node.args)})"
    if isinstance(node, FIELD):
        return f"{to_expr(node.receiver)}.{node.name}"
    if isinstance(node, COMPARE):
        return f"({to_expr(node.left)} {node.op} {to_expr(node.right)})"
    if isinstance(node, NOT):
        return f"!{to_expr(node.child)}"
    if isinstance(node, AND):
        return "(" + " && ".join(to_expr(c) for c in node.children) + ")"
    if isinstance(node, OR):
        return "(" + " || ".join(to_expr(c) for c in node.children) + ")"
    if isinstance(node, IS_EMPTY):
        return f"{to_expr(node.child)}.isEmpty()"
    if isinstance(node, IS_NOT_EMPTY):
        return f"{to_expr(node.child)}.isNotEmpty()"
    if isinstance(node, EQUALS):
        return f"{to_expr(node.left)}.equals({to_expr(node.right)})"
    if isinstance(node, OPAQUE):
        return node.expr
    if isinstance(node, UNKNOWN):
        return node.expr
    return str(node)


def parse_expression(
    expr: str,
    defaults: list[Any],
    default_kinds: list[str],
    local_vars: dict[str, str] | None = None,
    recursing: frozenset[str] | None = None,
) -> Node:
    """Parse a Java/Kotlin boolean expression into a canonical AST."""
    try:
        tokens = tokenize(expr)
        parser = Parser(tokens, defaults, default_kinds, local_vars=local_vars, recursing=recursing)
        return parser.parse()
    except SyntaxError as e:
        return UNKNOWN(expr)


def strip_catalog_lambda(expr: str) -> str:
    """Remove '{ prefs -> ... }' wrapper from a FeatureCatalog condition."""
    expr = expr.strip()
    if expr.startswith("{") and expr.endswith("}"):
        arrow = expr.find("->")
        if arrow != -1:
            body = expr[arrow + 2 : -1].strip()
            return body
    return expr


# ---------------------------------------------------------------------------
# Matching and classification
# ---------------------------------------------------------------------------


def preference_keys(node: Node) -> list[str]:
    """Collect all PREF keys in a node."""
    keys: list[str] = []

    def walk(n: Node) -> None:
        if isinstance(n, PREF_ACCESS):
            keys.append(n.key)
        for field_name, field_value in n.__dict__.items():
            if isinstance(field_value, Node):
                walk(field_value)
            elif isinstance(field_value, tuple):
                for c in field_value:
                    if isinstance(c, Node):
                        walk(c)

    walk(node)
    return sorted(set(keys))


def atomic_units(node: Node) -> list[Node]:
    """Decompose an AST into matching units.

    OR nodes are split into children because `A || B` activates on either.
    AND nodes remain whole.  OPAQUE / UNKNOWN nodes remain whole.
    """
    node = canonical(node)
    if isinstance(node, OR):
        units: list[Node] = []
        for c in node.children:
            units.extend(atomic_units(c))
        return units
    return [node]


def classify_mismatch(a: Node, b: Node) -> str:
    """Classify the structural mismatch between two canonical nodes."""
    a = canonical(a)
    b = canonical(b)
    if a == b:
        return "MATCH"
    if isinstance(a, PREF_ACCESS) and isinstance(b, PREF_ACCESS):
        if a.method == b.method and a.key == b.key:
            return "DEFAULT_MISMATCH"
        return "COMPOSITE_CONDITION_MISMATCH"
    if isinstance(a, COMPARE) and isinstance(b, COMPARE):
        if a.op != b.op:
            return "COMPARATOR_MISMATCH"
        if isinstance(a.left, PREF_ACCESS) and isinstance(b.left, PREF_ACCESS):
            if a.left.method == b.left.method and a.left.key == b.left.key:
                if a.left.default != b.left.default:
                    return "DEFAULT_MISMATCH"
                if a.right != b.right:
                    return "COMPARATOR_MISMATCH"
        if a.left == b.left:
            if a.right != b.right:
                return "COMPARATOR_MISMATCH"
        return "COMPOSITE_CONDITION_MISMATCH"
    if isinstance(a, AND) and isinstance(b, AND) or isinstance(a, OR) and isinstance(b, OR):
        return "COMPOSITE_CONDITION_MISMATCH"
    if isinstance(a, (AND, OR)) or isinstance(b, (AND, OR)):
        return "COMPOSITE_CONDITION_MISMATCH"
    if isinstance(a, NOT) or isinstance(b, NOT):
        return "COMPOSITE_CONDITION_MISMATCH"
    if type(a) != type(b):
        return "COMPOSITE_CONDITION_MISMATCH"
    return "COMPOSITE_CONDITION_MISMATCH"
def explanation_for(mismatch: str, a: Node, b: Node) -> str:
    if mismatch == "DEFAULT_MISMATCH":
        return f"same preference key but effective default differs: {to_expr(a)} vs {to_expr(b)}"
    if mismatch == "COMPARATOR_MISMATCH":
        return f"comparator or threshold differs: {to_expr(a)} vs {to_expr(b)}"
    if mismatch == "COMPOSITE_CONDITION_MISMATCH":
        return f"boolean structure differs: {to_expr(a)} vs {to_expr(b)}"
    return f"mismatch: {to_expr(a)} vs {to_expr(b)}"


# ---------------------------------------------------------------------------
# Global action domain detection
# ---------------------------------------------------------------------------



def _strip_comments(text: str) -> str:
    """Remove // and /* */ comments from a Java/Kotlin snippet."""
    text = re.sub(r"//.*", "", text)
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return text


def _replace_in_method(text: str, method_name: str, old: str, new: str) -> str:
    """Replace `old` with `new` only inside the first method definition named `method_name`."""
    pattern = re.compile(
        r"\b(?:public|private|protected)?\s*(?:static\s+)?(?:[\w\[\]<>]+\s+)?(?:fun\s+)?"
        + re.escape(method_name)
        + r"\s*\([^)]*\)\s*\{",
        re.S,
    )
    m = pattern.search(text)
    if not m:
        return text
    start = m.end() - 1
    depth = 0
    end = start
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                end = i
                break
    body = text[start + 1 : end]
    new_body = body.replace(old, new)
    return text[: start + 1] + new_body + text[end:]


def _replace_method_body(text: str, method_name: str, new_body: str) -> str:
    """Replace the whole body of the first method definition named `method_name`."""
    pattern = re.compile(
        r"\b(?:public|private|protected)?\s*(?:static\s+)?(?:[\w\[\]<>]+\s+)?(?:fun\s+)?"
        + re.escape(method_name)
        + r"\s*\([^)]*\)\s*\{",
        re.S,
    )
    m = pattern.search(text)
    if not m:
        return text
    start = m.end() - 1
    depth = 0
    end = start
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                end = i
                break
    signature = text[m.start() : start + 1]
    return text[: m.start()] + signature + "\n" + new_body.rstrip("\n") + "\n    }" + text[end + 1 :]


def _extract_method_return(text: str, method_name: str) -> str | None:
    """Extract the body of a boolean method from the installer source."""
    pattern = re.compile(r"\bstatic\s+boolean\s+" + re.escape(method_name) + r"\s*\([^)]*\)\s*\{", re.S)
    m = pattern.search(text)
    if not m:
        return None
    start = m.end() - 1
    brace = start
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[start + 1 : i]
    return None


def _extract_method_body(text: str, method_name: str) -> str | None:
    """Extract the body of a Java/Kotlin method, ignoring return type and modifiers."""
    pattern = re.compile(
        r"\b(?:public|private|protected)?\s*(?:static\s+)?(?:[\w\[\]<>]+\s+)?(?:fun\s+)?"
        + re.escape(method_name)
        + r"\s*\([^{]*\)\s*\{",
        re.S,
    )
    m = pattern.search(text)
    if not m:
        return None
    start = m.end() - 1
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[start + 1 : i]
    return None


def _extract_installer_local_vars(text: str) -> dict[str, str]:
    """Extract local boolean variables from SystemUiInstaller.install() that are built from prefs."""
    body = _extract_method_body(text, "install")
    if not body:
        return {}
    vars: dict[str, str] = {}
    for m in re.finditer(r"\bboolean\s+(\w+)\s*=\s*(.+?);", body, re.S):
        name = m.group(1)
        expr = m.group(2).strip()
        if re.search(r"\b(?:MainModule\.mPrefs|mPrefs|prefs)\b", expr):
            vars[name] = expr
    return vars


def _extract_resource_units(resource_text: str | None, source_file: str = "") -> list[tuple[Node, dict[str, Any]]]:
    """Extract resource hook conditions from SystemUIStatusBarHooks.setupStatusBar text.

    These are not in the install() method but run as part of SystemUI startup,
    so they are treated as additional installer conditions for coverage.
    """
    if not resource_text:
        return []
    body = _extract_method_body(resource_text, "setupStatusBar")
    if not body:
        return []

    # Collect local val/variables (Kotlin val x = ... or boolean x = ...).
    local_vars: dict[str, str] = {}
    for m in re.finditer(r"\b(?:val|var|boolean)\s+(\w+)\s*=\s*(.+?)(?:\n|;)", body, re.S):
        name = m.group(1)
        expr = m.group(2).strip()
        if re.search(r"\b(?:MainModule\.mPrefs|mPrefs|prefs)\b", expr):
            local_vars[name] = expr

    # Extract if conditions with balanced parentheses.
    def _if_conditions(text: str) -> list[str]:
        conds: list[str] = []
        i = 0
        while True:
            m = re.search(r"\bif\s*\(", text[i:])
            if not m:
                break
            start = i + m.end() - 1
            depth = 0
            end = start
            for j in range(start, len(text)):
                if text[j] == "(":
                    depth += 1
                elif text[j] == ")":
                    depth -= 1
                    if depth == 0:
                        end = j
                        break
            conds.append(text[start + 1 : end].strip())
            i = end + 1
        return conds

    units: list[tuple[Node, dict[str, Any]]] = []
    cond_id = 0
    for raw_expr in _if_conditions(body):
        expr = raw_expr.strip()
        if not expr:
            continue
        has_pref = "MainModule.mPrefs" in expr
        has_local = any(re.search(rf"\b{v}\b", expr) for v in local_vars)
        if not has_pref and not has_local:
            continue
        if "newStyle" in expr:
            continue
        cond_id += 1
        ast = parse_expression(expr, [], [], local_vars=local_vars)
        for u in atomic_units(ast):
            pseudo_entry = {
                "id": f"setupStatusBar_if_{cond_id}",
                "source_file": source_file,
                "source_method": "setupStatusBar",
                "start_line": 0,
                "end_line": 0,
                "phase": "RESOURCE_HOOK",
                "feature_id": "",
                "install_target": "",
                "normalized_expression": expr,
            }
            units.append((u, pseudo_entry))
    return units


def _parse_global_action_domain(method_body: str | None) -> dict[str, Any]:
    """Heuristic parse of isSystemUiGlobalActionKey.

    Fail-closed: only PARSED_SAFE when the method clearly requires both the
    _action suffix and a controls_/system_ prefix.  If the method only checks
    the _action suffix it is PARSED_CONTAMINATED.  Any other structure (missing
    method, missing predicate, or parser-unrecognized form) is UNKNOWN.
    """
    if method_body is None or not method_body.strip():
        return {
            "status": "UNKNOWN",
            "contaminated": False,
            "reason": "method body missing or empty",
        }
    body = _strip_comments(method_body)
    has_action = '.endsWith("_action")' in body or '.endsWith(\'_action\')' in body
    has_controls = '.startsWith("controls_")' in body
    has_system = '.startsWith("system_")' in body

    if not has_action:
        return {
            "status": "UNKNOWN",
            "contaminated": False,
            "reason": "no recognizable endsWith(\"_action\") predicate",
        }

    # Positive domain: requires _action and both recognized SystemUI prefixes.
    if has_controls and has_system:
        return {
            "status": "PARSED_SAFE",
            "contaminated": False,
            "reason": "positive domain requires _action suffix and (controls_|system_) prefix",
        }

    # Contaminated: it checks _action but lacks the full prefix filter.
    return {
        "status": "PARSED_CONTAMINATED",
        "contaminated": True,
        "reason": "domain only checks _action suffix, missing controls_/system_ prefix filter",
    }
def is_global_action_key(key: str, domain: dict[str, Any]) -> bool:
    if not key or not key.endswith("_action"):
        return False
    if domain.get("contaminated"):
        return True
    base = key
    if base.startswith("pref_key_"):
        base = base[len("pref_key_"):]
    return base.startswith("controls_") or base.startswith("system_")


def is_systemui_key(key: str, startup_keys: set[str], catalog_keys: set[str]) -> bool:
    """A preference key belongs to the SystemUI domain if it is known in the
    startup gate, catalog, or a standard system/controls prefix."""
    if key in startup_keys or key in catalog_keys:
        return True
    if key.startswith("system_") or key.startswith("controls_") or key.startswith("various_"):
        return True
    if key.startswith("pref_key_"):
        return is_systemui_key(key[len("pref_key_"):], startup_keys, catalog_keys)
    return False


# ---------------------------------------------------------------------------
# Differential audit
# ---------------------------------------------------------------------------


@dataclass
class DiffRecord:
    installer_condition_id: str = ""
    startup_condition_id: str = ""
    feature_id: str = ""
    installer_file: str = ""
    installer_lines: tuple[int, int] = (0, 0)
    startup_file: str = ""
    startup_lines: tuple[int, int] = (0, 0)
    catalog_file: str = ""
    catalog_lines: tuple[int, int] = (0, 0)
    installer_expression: str = ""
    startup_expression: str = ""
    catalog_expression: str = ""
    mismatch_type: str = ""
    severity: str = ""
    explanation: str = ""

    def to_dict(self) -> dict[str, Any]:
        return {
            "installer_condition_id": self.installer_condition_id,
            "startup_condition_id": self.startup_condition_id,
            "feature_id": self.feature_id,
            "installer_file": self.installer_file,
            "installer_lines": list(self.installer_lines),
            "startup_file": self.startup_file,
            "startup_lines": list(self.startup_lines),
            "catalog_file": self.catalog_file,
            "catalog_lines": list(self.catalog_lines),
            "installer_expression": self.installer_expression,
            "startup_expression": self.startup_expression,
            "catalog_expression": self.catalog_expression,
            "mismatch_type": self.mismatch_type,
            "severity": self.severity,
            "explanation": self.explanation,
        }


@dataclass
class DiffResult:
    install_conditions: int = 0
    startup_conditions: int = 0
    conditional_dispatchers: int = 0
    unconditional_dispatchers: int = 0
    catalog_systemui_entries: int = 0
    matched_atomic_units: int = 0
    matched_unique_installer_conditions: int = 0
    matched_unique_startup_conditions: int = 0
    matched_unique_feature_ids: int = 0
    total_installer_atomic_units: int = 0
    total_startup_atomic_units: int = 0
    records: list[DiffRecord] = field(default_factory=list)
    categories: dict[str, list[DiffRecord]] = field(default_factory=dict)
    counts: dict[str, int] = field(default_factory=dict)
    global_action_domain: dict[str, Any] = field(default_factory=dict)
    provenance: dict[str, Any] = field(default_factory=dict)

    def add(self, record: DiffRecord) -> None:
        self.records.append(record)
        self.categories.setdefault(record.mismatch_type, []).append(record)

    def finalize(self) -> None:
        all_types = {
            "MATCH",
            "INSTALLER_ONLY",
            "GATE_ONLY",
            "GATE_ONLY_REDUNDANT",
            "GATE_ONLY_DYNAMIC_DOMAIN",
            "GATE_ONLY_UNEXPLAINED",
            "DEFAULT_MISMATCH",
            "COMPARATOR_MISMATCH",
            "COMPOSITE_CONDITION_MISMATCH",
            "INSTALLER_CATALOG_MISMATCH",
            "INSTALLER_CATALOG_MATCH",
            "FEATURE_CATALOG_GATE_UNKNOWN",
            "DOMAIN_CONTAMINATION",
            "SEMANTIC_REVIEW_REQUIRED",
            "UNMATCHED_INFRASTRUCTURE",
            "DYNAMIC_GLOBAL_ACTION_GATE",
        }
        self.counts = {t: len(self.categories.get(t, [])) for t in all_types}

    def to_dict(self) -> dict[str, Any]:
        return {
            "schema_version": SCHEMA_VERSION,
            "provenance": self.provenance,
            "counts": self.counts,
            "global_action_domain": self.global_action_domain,
            "records": [r.to_dict() for r in self.records],
            "categories": {k: [r.to_dict() for r in v] for k, v in sorted(self.categories.items())},
            "summary": {
                "install_conditions": self.install_conditions,
                "startup_conditions": self.startup_conditions,
                "conditional_dispatchers": self.conditional_dispatchers,
                "unconditional_dispatchers": self.unconditional_dispatchers,
                "catalog_systemui_entries": self.catalog_systemui_entries,
                "matched_atomic_units": self.matched_atomic_units,
                "matched_unique_installer_conditions": self.matched_unique_installer_conditions,
                "matched_unique_startup_conditions": self.matched_unique_startup_conditions,
                "matched_unique_feature_ids": self.matched_unique_feature_ids,
                "total_installer_atomic_units": self.total_installer_atomic_units,
                "total_startup_atomic_units": self.total_startup_atomic_units,
            },
        }
def _condition_from_entry(entry: dict[str, Any], local_vars: dict[str, str] | None = None) -> Node:
    expr = entry.get("normalized_expression", "")
    if entry.get("phase") == "FEATURE_CATALOG_GATE":
        expr = strip_catalog_lambda(expr)
    defaults = entry.get("default_values", [])
    default_kinds = entry.get("default_kinds", [])
    if not defaults:
        defaults = []
    if not default_kinds:
        default_kinds = []
    return parse_expression(expr, defaults, default_kinds, local_vars=local_vars)


def _is_real_feature_activation(entry: dict[str, Any]) -> bool:
    """Exclude package guard, restart guard and pure infrastructure branches."""
    phase = entry.get("phase", "")
    if phase in ("PACKAGE_GUARD", "PRE_RESTART_GUARD_INFRASTRUCTURE", "RESTART_GUARD"):
        return False
    return True


def _collect_startup_units(
    startup_conditions: list[dict[str, Any]],
    local_vars: dict[str, str] | None = None,
) -> list[tuple[Node, dict[str, Any]]]:
    units: list[tuple[Node, dict[str, Any]]] = []
    for e in startup_conditions:
        ast = _condition_from_entry(e, local_vars=local_vars)
        for u in atomic_units(ast):
            units.append((u, e))
    return units


def _collect_installer_units(
    install_conditions: list[dict[str, Any]],
    local_vars: dict[str, str] | None = None,
) -> list[tuple[Node, dict[str, Any]]]:
    units: list[tuple[Node, dict[str, Any]]] = []
    for e in install_conditions:
        if not _is_real_feature_activation(e):
            continue
        ast = _condition_from_entry(e, local_vars=local_vars)
        for u in atomic_units(ast):
            units.append((u, e))
    return units


def _find_match(unit: Node, units: list[tuple[Node, dict[str, Any]]], used: set[int]) -> tuple[int, Node, dict[str, Any]] | None:
    for i, (u, e) in enumerate(units):
        if i in used:
            continue
        if canonical(unit) == canonical(u):
            return (i, u, e)
    return None


def _find_candidate(unit: Node, units: list[tuple[Node, dict[str, Any]]], used: set[int]) -> tuple[int, Node, dict[str, Any]] | None:
    unit_keys = set(preference_keys(unit))
    if not unit_keys:
        return None
    for i, (u, e) in enumerate(units):
        if i in used:
            continue
        candidate_keys = set(preference_keys(u))
        if unit_keys & candidate_keys:
            return (i, u, e)
    return None


def _make_record(
    install_entry: dict[str, Any] | None,
    startup_entry: dict[str, Any] | None,
    catalog_entry: dict[str, Any] | None,
    feature_id: str,
    mismatch_type: str,
    severity: str,
    explanation: str,
) -> DiffRecord:
    r = DiffRecord()
    r.mismatch_type = mismatch_type
    r.severity = severity
    r.explanation = explanation
    r.feature_id = feature_id
    if install_entry:
        r.installer_condition_id = install_entry.get("id", "")
        r.installer_file = install_entry.get("source_file", "")
        r.installer_lines = (install_entry.get("start_line", 0), install_entry.get("end_line", 0))
        r.installer_expression = install_entry.get("normalized_expression", "")
    if startup_entry:
        r.startup_condition_id = startup_entry.get("id", "")
        r.startup_file = startup_entry.get("source_file", "")
        r.startup_lines = (startup_entry.get("start_line", 0), startup_entry.get("end_line", 0))
        r.startup_expression = startup_entry.get("normalized_expression", "")
    if catalog_entry:
        r.catalog_file = catalog_entry.get("source_file", "")
        r.catalog_lines = (catalog_entry.get("start_line", 0), catalog_entry.get("end_line", 0))
        r.catalog_expression = catalog_entry.get("normalized_expression", "")
    return r


def _key_node_sig(node: Node) -> str:
    return ",".join(preference_keys(node))


def _is_dynamic_action_condition(node: Node, domain: dict[str, Any]) -> bool:
    for k in preference_keys(node):
        if is_global_action_key(k, domain):
            return True
    return False


def _audit(
    inventory: dict[str, Any],
    installer_text: str,
    resource_text: str | None = None,
    resource_source_file: str = "",
) -> DiffResult:
    result = DiffResult()
    result.provenance = {
        "inventory": "docs/audit/A13_SYSTEMUI_GATE_INVENTORY.json",
        "installer_source": "app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java",
    }
    if resource_source_file:
        result.provenance["resource_source"] = resource_source_file

    install_conditions = inventory.get("INSTALL_CONDITIONS", [])
    startup_conditions = inventory.get("STARTUP_GATE_CONDITIONS", [])
    catalog_entries = inventory.get("FEATURE_CATALOG_GATES", [])
    dispatch_calls = inventory.get("FEATURE_DISPATCH_CALLS", [])

    result.install_conditions = len(install_conditions)
    result.startup_conditions = len(startup_conditions)
    result.conditional_dispatchers = len([e for e in install_conditions if e.get("feature_id")])
    result.unconditional_dispatchers = len(dispatch_calls)
    result.catalog_systemui_entries = len(catalog_entries)

    installer_local_vars = _extract_installer_local_vars(installer_text)
    global_action_body = _extract_method_return(installer_text, "isSystemUiGlobalActionKey")
    result.global_action_domain = _parse_global_action_domain(global_action_body)
    ga_status = result.global_action_domain.get("status", "UNKNOWN")

    startup_keys: set[str] = set()
    for e in startup_conditions:
        startup_keys.update(e.get("preference_keys", []))
    catalog_keys: set[str] = set()
    for e in catalog_entries:
        catalog_keys.update(e.get("declared_preference_keys", []))
        catalog_keys.update(e.get("condition_preference_keys", []))

    startup_units = _collect_startup_units(startup_conditions, local_vars=installer_local_vars)
    startup_used: set[int] = set()
    startup_by_id = {e.get("id", ""): e for e in startup_conditions}

    global_action_startup = [e for e in startup_conditions if "hasAnyGlobalAction" in e.get("normalized_expression", "")]
    for ga in global_action_startup:
        if ga_status == "PARSED_CONTAMINATED":
            result.add(
                _make_record(
                    None,
                    ga,
                    None,
                    "",
                    "DOMAIN_CONTAMINATION",
                    "BLOCKER",
                    "Global action domain is contaminated (accepts non-SystemUI _action keys): "
                    + result.global_action_domain.get("reason", ""),
                )
            )
        elif ga_status == "UNKNOWN":
            result.add(
                _make_record(
                    None,
                    ga,
                    None,
                    "",
                    "SEMANTIC_REVIEW_REQUIRED",
                    "BLOCKER",
                    "Global action domain parser could not recognize the method body: "
                    + result.global_action_domain.get("reason", ""),
                )
            )
        else:
            result.add(
                _make_record(
                    None,
                    ga,
                    None,
                    "",
                    "DYNAMIC_GLOBAL_ACTION_GATE",
                    "INFO",
                    "Dynamic global action gate covers SystemUI _action keys.",
                )
            )

    installer_units = _collect_installer_units(install_conditions, local_vars=installer_local_vars)
    install_used: set[int] = set()
    resource_units = _extract_resource_units(resource_text, source_file=resource_source_file)
    resource_used: set[int] = set()

    for iu, install_entry in installer_units:
        action_keys = [k for k in preference_keys(iu) if k.endswith("_action")]
        if ga_status == "UNKNOWN" and action_keys:
            result.add(
                _make_record(
                    install_entry,
                    None,
                    None,
                    install_entry.get("feature_id", ""),
                    "SEMANTIC_REVIEW_REQUIRED",
                    "BLOCKER",
                    f"Dynamic action key(s) {action_keys} require semantic review because the global action domain parser is in an unrecognized state.",
                )
            )
            install_used.add(id(install_entry))
            continue
        if _is_dynamic_action_condition(iu, result.global_action_domain):
            all_valid = all(
                is_global_action_key(k, result.global_action_domain) or is_systemui_key(k, startup_keys, catalog_keys)
                for k in preference_keys(iu)
            )
            if not all_valid:
                result.add(
                    _make_record(
                        install_entry,
                        None,
                        None,
                        install_entry.get("feature_id", ""),
                        "DOMAIN_CONTAMINATION",
                        "BLOCKER",
                        f"Installer uses non-SystemUI action key(s): {preference_keys(iu)}",
                    )
                )
            else:
                result.add(
                    _make_record(
                        install_entry,
                        None,
                        None,
                        install_entry.get("feature_id", ""),
                        "MATCH",
                        "OK",
                        "Covered by the dynamic global action gate.",
                    )
                )
            install_used.add(id(install_entry))
            continue

        match = _find_match(iu, startup_units, startup_used)
        if match:
            startup_idx, _, startup_entry = match
            startup_used.add(startup_idx)
            result.add(
                _make_record(
                    install_entry,
                    startup_entry,
                    None,
                    install_entry.get("feature_id", ""),
                    "MATCH",
                    "OK",
                    "Startup gate covers this installer condition.",
                )
            )
            install_used.add(id(install_entry))
            continue

        candidate = _find_candidate(iu, startup_units, startup_used)
        if candidate:
            _, _, startup_entry = candidate
            mtype = classify_mismatch(iu, _condition_from_entry(startup_entry))
            if mtype == "MATCH":
                mtype = "COMPOSITE_CONDITION_MISMATCH"
            result.add(
                _make_record(
                    install_entry,
                    startup_entry,
                    None,
                    install_entry.get("feature_id", ""),
                    mtype,
                    "BLOCKER" if mtype != "MATCH" else "OK",
                    explanation_for(mtype, iu, _condition_from_entry(startup_entry)),
                )
            )
            install_used.add(id(install_entry))
            continue

        for k in preference_keys(iu):
            if not is_systemui_key(k, startup_keys, catalog_keys):
                result.add(
                    _make_record(
                        install_entry,
                        None,
                        None,
                        install_entry.get("feature_id", ""),
                        "DOMAIN_CONTAMINATION",
                        "BLOCKER",
                        f"Preference key {k!r} is not known in the SystemUI startup/catalog domain.",
                    )
                )
                break
        else:
            result.add(
                _make_record(
                    install_entry,
                    None,
                    None,
                    install_entry.get("feature_id", ""),
                    "INSTALLER_ONLY",
                    "BLOCKER",
                    "No matching startup gate condition found.",
                )
            )
        install_used.add(id(install_entry))

    catalog_by_feature = {e.get("feature_id", ""): e for e in catalog_entries}
    for dispatch in dispatch_calls:
        feature_id = dispatch.get("feature_id", "")
        catalog = catalog_by_feature.get(feature_id)
        if not catalog:
            result.add(
                _make_record(
                    dispatch,
                    None,
                    None,
                    feature_id,
                    "FEATURE_CATALOG_GATE_UNKNOWN",
                    "BLOCKER",
                    f"No FeatureCatalog entry for unconditional dispatcher {feature_id!r}.",
                )
            )
            continue
        catalog_ast = _condition_from_entry(catalog)
        catalog_units = atomic_units(catalog_ast)
        for cu in catalog_units:
            match = _find_match(cu, startup_units, startup_used)
            if match:
                startup_idx, _, startup_entry = match
                startup_used.add(startup_idx)
                result.add(
                    _make_record(
                        dispatch,
                        startup_entry,
                        catalog,
                        feature_id,
                        "INSTALLER_CATALOG_MATCH",
                        "OK",
                        f"Catalog condition for {feature_id} is covered by startup gate.",
                    )
                )
            else:
                candidate = _find_candidate(cu, startup_units, startup_used)
                if candidate:
                    _, _, startup_entry = candidate
                    mtype = classify_mismatch(cu, _condition_from_entry(startup_entry))
                    result.add(
                        _make_record(
                            dispatch,
                            startup_entry,
                            catalog,
                            feature_id,
                            mtype,
                            "BLOCKER" if mtype != "MATCH" else "OK",
                            explanation_for(mtype, cu, _condition_from_entry(startup_entry)),
                        )
                    )
                else:
                    result.add(
                        _make_record(
                            dispatch,
                            None,
                            catalog,
                            feature_id,
                            "FEATURE_CATALOG_GATE_UNKNOWN",
                            "BLOCKER",
                            f"Catalog condition for {feature_id} has no matching startup gate.",
                        )
                    )

    for install_entry in install_conditions:
        feature_id = install_entry.get("feature_id", "")
        if not feature_id:
            continue
        catalog = catalog_by_feature.get(feature_id)
        if not catalog:
            result.add(
                _make_record(
                    install_entry,
                    None,
                    None,
                    feature_id,
                    "FEATURE_CATALOG_GATE_UNKNOWN",
                    "BLOCKER",
                    f"No FeatureCatalog entry for conditional dispatcher {feature_id!r}.",
                )
            )
            continue
        install_ast = _condition_from_entry(install_entry)
        catalog_ast = _condition_from_entry(catalog)
        if canonical(install_ast) == canonical(catalog_ast):
            continue
        mtype = classify_mismatch(install_ast, catalog_ast)
        result.add(
            _make_record(
                install_entry,
                None,
                catalog,
                feature_id,
                "INSTALLER_CATALOG_MISMATCH",
                "BLOCKER",
                explanation_for(mtype, install_ast, catalog_ast),
            )
        )

    for i, (su, startup_entry) in enumerate(startup_units):
        if i in startup_used:
            continue
        match = _find_match(su, resource_units, resource_used)
        if match:
            resource_idx, _, resource_entry = match
            resource_used.add(resource_idx)
            startup_used.add(i)
            result.add(
                _make_record(
                    resource_entry,
                    startup_entry,
                    None,
                    "",
                    "MATCH",
                    "OK",
                    "Startup gate covered by SystemUIStatusBarHooks.setupStatusBar resource hook condition.",
                )
            )

    for i, (su, startup_entry) in enumerate(startup_units):
        if i in startup_used:
            continue
        if "hasAnyGlobalAction" in startup_entry.get("normalized_expression", ""):
            continue
        keys = preference_keys(su)
        if ga_status == "UNKNOWN" and any(k.endswith("_action") for k in keys):
            result.add(
                _make_record(
                    None,
                    startup_entry,
                    None,
                    "",
                    "SEMANTIC_REVIEW_REQUIRED",
                    "BLOCKER",
                    f"Startup action gate for {keys} cannot be verified because the global action domain parser is in an unrecognized state.",
                )
            )
        elif _is_dynamic_action_condition(su, result.global_action_domain):
            result.add(
                _make_record(
                    None,
                    startup_entry,
                    None,
                    "",
                    "GATE_ONLY_DYNAMIC_DOMAIN",
                    "INFO",
                    f"Startup action gate for {keys} has no individual installer condition; covered by dynamic global action gate.",
                )
            )
        elif not keys:
            result.add(
                _make_record(
                    None,
                    startup_entry,
                    None,
                    "",
                    "GATE_ONLY_REDUNDANT",
                    "INFO",
                    "Startup condition has no preference keys and is treated as redundant.",
                )
            )
        else:
            result.add(
                _make_record(
                    None,
                    startup_entry,
                    None,
                    "",
                    "GATE_ONLY_UNEXPLAINED",
                    "INFO",
                    f"Startup condition with keys {keys} has no matching installer.",
                )
            )

    for install_entry in install_conditions:
        if install_entry.get("phase") in ("PACKAGE_GUARD", "PRE_RESTART_GUARD_INFRASTRUCTURE", "RESTART_GUARD"):
            result.add(
                _make_record(
                    install_entry,
                    None,
                    None,
                    install_entry.get("feature_id", ""),
                    "UNMATCHED_INFRASTRUCTURE",
                    "INFO",
                    f"Excluded from startup coverage: phase={install_entry.get('phase')}, id={install_entry.get('id')}",
                )
            )

    result.total_installer_atomic_units = len(installer_units)
    result.total_startup_atomic_units = len(startup_units)
    match_records = [r for r in result.records if r.mismatch_type == "MATCH"]
    result.matched_atomic_units = len(match_records)
    result.matched_unique_installer_conditions = len({r.installer_condition_id for r in match_records if r.installer_condition_id})
    result.matched_unique_startup_conditions = len({r.startup_condition_id for r in match_records if r.startup_condition_id})
    result.matched_unique_feature_ids = len({r.feature_id for r in match_records if r.feature_id})

    result.finalize()
    return result
def diff_from_inventory(
    inventory: dict[str, Any],
    installer_text: str,
    resource_text: str | None = None,
    resource_source_file: str = "",
) -> DiffResult:
    return _audit(inventory, installer_text, resource_text, resource_source_file)


def diff_from_repo(repo_root: Path) -> DiffResult:
    """Read real repo sources and run the differential audit."""
    inventory_path = repo_root / "docs/audit/A13_SYSTEMUI_GATE_INVENTORY.json"
    installer_path = repo_root / "app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java"
    resource_path = repo_root / "app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt"

    inventory = json.loads(inventory_path.read_text(encoding="utf-8"))
    installer_text = installer_path.read_text(encoding="utf-8")
    resource_text = resource_path.read_text(encoding="utf-8") if resource_path.exists() else None
    resource_source_file = (
        resource_path.relative_to(repo_root).as_posix()
        if resource_path.exists()
        else ""
    )
    result = diff_from_inventory(inventory, installer_text, resource_text, resource_source_file)
    result.provenance = {
        "inventory": inventory_path.relative_to(repo_root).as_posix(),
        "installer_source": installer_path.relative_to(repo_root).as_posix(),
        "resource_source": resource_source_file,
    }
    return result



# ---------------------------------------------------------------------------
# Output generation
# ---------------------------------------------------------------------------


def render_markdown(result: DiffResult) -> str:
    lines: list[str] = [
        "# A13 SystemUI Gate Differential Audit",
        "",
        f"Schema version: {SCHEMA_VERSION}",
        "",
        "## Summary",
        "",
        f"- Install conditions: {result.install_conditions}",
        f"- Startup conditions: {result.startup_conditions}",
        f"- Conditional dispatchers: {result.conditional_dispatchers}",
        f"- Unconditional dispatchers: {result.unconditional_dispatchers}",
        f"- Catalog SystemUI entries: {result.catalog_systemui_entries}",
        "",
        "## Match Coverage",
        "",
        f"- Matched atomic units: {result.matched_atomic_units}",
        f"- Matched unique installer conditions: {result.matched_unique_installer_conditions}",
        f"- Matched unique startup conditions: {result.matched_unique_startup_conditions}",
        f"- Matched unique feature IDs: {result.matched_unique_feature_ids}",
        f"- Total installer atomic units: {result.total_installer_atomic_units}",
        f"- Total startup atomic units: {result.total_startup_atomic_units}",
        "",
        "## Counts",
        "",
    ]
    for k in sorted(result.counts):
        lines.append(f"- {k}: {result.counts[k]}")
    lines.append("")
    lines.append("## Global Action Domain")
    lines.append("")
    lines.append(f"- contaminated: {result.global_action_domain.get('contaminated')}")
    lines.append(f"- reason: {result.global_action_domain.get('reason')}")
    lines.append("")
    lines.append("## Records")
    lines.append("")
    for record in result.records:
        lines.append(f"### {record.mismatch_type} — {record.feature_id or record.installer_condition_id or record.startup_condition_id}")
        lines.append(f"- severity: {record.severity}")
        if record.feature_id:
            lines.append(f"- feature_id: {record.feature_id}")
        if record.installer_file:
            lines.append(f"- installer: {record.installer_file} lines {record.installer_lines[0]}-{record.installer_lines[1]}")
        if record.startup_file:
            lines.append(f"- startup: {record.startup_file} lines {record.startup_lines[0]}-{record.startup_lines[1]}")
        if record.catalog_file:
            lines.append(f"- catalog: {record.catalog_file} lines {record.catalog_lines[0]}-{record.catalog_lines[1]}")
        lines.append(f"- explanation: {record.explanation}")
        lines.append("")
    return "\n".join(lines)


def write_diff(repo_root: Path, verify: bool = False) -> tuple[Path, Path, str, str]:
    json_path = repo_root / "docs/audit/A13_SYSTEMUI_GATE_DIFF.json"
    md_path = repo_root / "docs/audit/A13_SYSTEMUI_GATE_DIFF.md"

    result = diff_from_repo(repo_root)

    json_data = result.to_dict()
    json_text = json.dumps(json_data, indent=2, sort_keys=True, ensure_ascii=False)
    md_text = render_markdown(result)

    if verify:
        if not json_path.exists() or not md_path.exists():
            raise RuntimeError("Diff output files do not exist; run without --verify to generate.")
        existing_json = json_path.read_text(encoding="utf-8")
        existing_md = md_path.read_text(encoding="utf-8")
        if existing_json != json_text:
            raise RuntimeError("JSON diff is not deterministic or source changed.")
        if existing_md != md_text:
            raise RuntimeError("Markdown diff is not deterministic or source changed.")
        return json_path, md_path, json_text, md_text

    json_path.parent.mkdir(parents=True, exist_ok=True)
    json_path.write_text(json_text, encoding="utf-8", newline="\n")
    md_path.write_text(md_text, encoding="utf-8", newline="\n")
    return json_path, md_path, json_text, md_text


# ---------------------------------------------------------------------------
# Mutation-style counter-proof tests.
# Each mutation deliberately breaks one aspect of the inventory or source
# contract and asserts that the diff auditor flags the expected category.
# ---------------------------------------------------------------------------


def _find_condition(inventory: dict[str, Any], section: str, cond_id: str) -> dict[str, Any] | None:
    for e in inventory.get(section, []):
        if e.get("id") == cond_id:
            return e
    return None


def _mut_a_remove_startup_lockscreen(inventory: dict[str, Any]) -> None:
    cond = _find_condition(inventory, "STARTUP_GATE_CONDITIONS", "hasAnySystemUiStartupFeature_if_37")
    if cond is not None:
        inventory["STARTUP_GATE_CONDITIONS"].remove(cond)


def _mut_b_fake_install_key(inventory: dict[str, Any]) -> None:
    cond = _find_condition(inventory, "INSTALL_CONDITIONS", "install_if_40")
    if cond is not None:
        cond["normalized_expression"] = 'MainModule.mPrefs.getBoolean("fake_unknown_key")'
        cond["raw_expression"] = 'MainModule.mPrefs.getBoolean("fake_unknown_key")'
        cond["preference_keys"] = ["fake_unknown_key"]
        cond["default_values"] = [False]
        cond["default_kinds"] = ["IMPLICIT_PREFMAP_DEFAULT"]


def _mut_c_startup_default_true(inventory: dict[str, Any]) -> None:
    cond = _find_condition(inventory, "STARTUP_GATE_CONDITIONS", "hasAnySystemUiStartupFeature_if_37")
    if cond is not None:
        cond["default_values"] = [True]
        cond["default_kinds"] = ["EXPLICIT"]


def _mut_d_comparator_flip(inventory: dict[str, Any]) -> None:
    cond = _find_condition(inventory, "STARTUP_GATE_CONDITIONS", "hasAnySystemUiStartupFeature_if_2")
    if cond is not None:
        cond["normalized_expression"] = cond["normalized_expression"].replace(
            'prefs.getInt("system_statusbarheight", 19) > 19',
            'prefs.getInt("system_statusbarheight", 19) < 19',
        )
        cond["raw_expression"] = cond["raw_expression"].replace(
            'prefs.getInt("system_statusbarheight", 19) > 19',
            'prefs.getInt("system_statusbarheight", 19) < 19',
        )


def _mut_e_remove_catalog_overlay(inventory: dict[str, Any]) -> None:
    cond = _find_condition(inventory, "FEATURE_CATALOG_GATES", "FeatureCatalog_tempHideOverlaySystemUI")
    if cond is not None:
        inventory["FEATURE_CATALOG_GATES"].remove(cond)


def _mut_f_startup_composite(inventory: dict[str, Any]) -> None:
    cond = _find_condition(inventory, "STARTUP_GATE_CONDITIONS", "hasAnySystemUiStartupFeature_if_37")
    if cond is not None:
        cond["normalized_expression"] = (
            'prefs.getBoolean("system_disableanynotif") && '
            'prefs.getBoolean("system_nosilentvibrate")'
        )
        cond["raw_expression"] = cond["normalized_expression"]
        cond["preference_keys"] = ["system_disableanynotif", "system_nosilentvibrate"]
        cond["accessors"] = ["getBoolean", "getBoolean"]
        cond["comparators"] = []
        cond["boolean_operators"] = ["AND"]
        cond["default_values"] = [False, False]
        cond["default_kinds"] = ["IMPLICIT_PREFMAP_DEFAULT", "IMPLICIT_PREFMAP_DEFAULT"]


_MUTATIONS: list[tuple[str, Callable[[dict[str, Any]], None], dict[str, int]]] = [
    ("A: remove startup lockscreen gate", _mut_a_remove_startup_lockscreen, {"INSTALLER_ONLY": 1}),
    ("B: fake installer preference key", _mut_b_fake_install_key, {"DOMAIN_CONTAMINATION": 1}),
    ("C: startup default flipped to true", _mut_c_startup_default_true, {"DEFAULT_MISMATCH": 1}),
    ("D: startup comparator flipped", _mut_d_comparator_flip, {"COMPARATOR_MISMATCH": 1}),
    ("E: remove catalog overlay gate", _mut_e_remove_catalog_overlay, {"FEATURE_CATALOG_GATE_UNKNOWN": 1}),
    ("F: startup gate made composite", _mut_f_startup_composite, {"COMPOSITE_CONDITION_MISMATCH": 1}),
]


def run_mutations(repo_root: Path) -> int:
    inventory_path = repo_root / "docs/audit/A13_SYSTEMUI_GATE_INVENTORY.json"
    installer_path = repo_root / "app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java"
    resource_path = repo_root / "app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt"

    inventory = json.loads(inventory_path.read_text(encoding="utf-8"))
    installer_text = installer_path.read_text(encoding="utf-8")
    resource_source = str(resource_path.relative_to(repo_root))
    resource_text = resource_path.read_text(encoding="utf-8") if resource_path.exists() else None

    baseline = diff_from_inventory(inventory, installer_text, resource_text=resource_text, resource_source_file=resource_source).counts
    print(f"Baseline: INSTALLER_ONLY={baseline['INSTALLER_ONLY']}, "
          f"GATE_ONLY_UNEXPLAINED={baseline['GATE_ONLY_UNEXPLAINED']}, "
          f"DOMAIN_CONTAMINATION={baseline['DOMAIN_CONTAMINATION']}, "
          f"DEFAULT_MISMATCH={baseline['DEFAULT_MISMATCH']}, "
          f"COMPARATOR_MISMATCH={baseline['COMPARATOR_MISMATCH']}, "
          f"COMPOSITE_CONDITION_MISMATCH={baseline['COMPOSITE_CONDITION_MISMATCH']}, "
          f"FEATURE_CATALOG_GATE_UNKNOWN={baseline['FEATURE_CATALOG_GATE_UNKNOWN']}")

    all_ok = True
    for name, mutate, expected in _MUTATIONS:
        mutant = copy.deepcopy(inventory)
        mutate(mutant)
        counts = diff_from_inventory(mutant, installer_text, resource_text=resource_text, resource_source_file=resource_source).counts
        ok = True
        details = []
        for key, delta in expected.items():
            actual = counts.get(key, 0)
            if actual <= baseline.get(key, 0):
                ok = False
                details.append(f"{key} expected > {baseline.get(key, 0)}, got {actual}")
        if ok:
            print(f"PASS {name}")
        else:
            print(f"FAIL {name}: {', '.join(details)}")
            all_ok = False

    return 0 if all_ok else 1


def _load_inventory_tool():
    """Load the inventory generator from the sibling tools directory."""
    tools_dir = Path(__file__).resolve().parent
    if str(tools_dir) not in sys.path:
        sys.path.insert(0, str(tools_dir))
    return importlib.import_module("a13_systemui_gate_inventory")


# ---------------------------------------------------------------------------
# End-to-end source mutation harness.
#
# Each mutation copies the minimum source files to a temporary repo, mutates
# one source file, regenerates the inventory, and runs the diff audit.  This
# proves the full source -> inventory -> diff chain and the fail-closed global
# action parser.
# ---------------------------------------------------------------------------


def _copy_source_skeleton(repo_root: Path, temp_root: Path) -> None:
    """Copy just enough source for the inventory/diff tools to run."""
    rels = [
        Path("app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java"),
        Path("app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt"),
        Path("app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt"),
        Path("app/src/main/java/tv/withaibuild/customiuizer/installers/AndroidPackageInstaller.java"),
        Path("app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java"),
    ]
    for rel in rels:
        src = repo_root / rel
        if not src.exists():
            continue
        dst = temp_root / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)


def _remove_feature_spec(catalog_text: str, spec_id: str) -> str:
    """Remove the first FeatureSpec block containing id = <spec_id>."""
    marker = f'id = "{spec_id}"'
    pos = catalog_text.find(marker)
    if pos == -1:
        return catalog_text
    # Find the opening "FeatureSpec(" that starts this block.
    open_pos = catalog_text.rfind("FeatureSpec(", 0, pos)
    if open_pos == -1:
        return catalog_text
    close_pos = _find_balanced(catalog_text, open_pos + len("FeatureSpec(") - 1)
    if close_pos == -1:
        return catalog_text
    # Consume the following comma, if present.
    end = close_pos + 1
    while end < len(catalog_text) and catalog_text[end] in " \t\r\n":
        end += 1
    if end < len(catalog_text) and catalog_text[end] == ",":
        end += 1
    return catalog_text[:open_pos] + catalog_text[end:]


def _find_balanced(text: str, open_pos: int) -> int:
    """Find the matching ')' for '(' at open_pos, skipping strings/comments."""
    if text[open_pos] != "(":
        return -1
    depth = 1
    i = open_pos + 1
    n = len(text)
    while i < n:
        ch = text[i]
        if ch in ('"', "'"):
            j = i + 1
            while j < n and text[j] != ch:
                if text[j] == "\\":
                    j += 2
                else:
                    j += 1
            i = j + 1
            continue
        if ch == "(" and i + 1 < n and text[i + 1] == "*":
            end = text.find("*/", i + 2)
            i = end + 2 if end != -1 else n
            continue
        if i + 1 < n and text[i] == "/" and text[i + 1] == "/":
            nl = text.find("\n", i)
            i = nl + 1 if nl != -1 else n
            continue
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


_SOURCE_MUTATIONS: list[tuple[str, dict[str, Any]]] = [
    {
        "name": "A: remove startup system_statusbarheight gate",
        "target": {"INSTALLER_ONLY": 1},
        "mutate": lambda text, catalog: (
            _replace_in_method(
                text,
                "hasAnySystemUiStartupFeature",
                '        if (prefs.getInt("system_statusbarheight", 19) > 19) return true;\n',
                '',
            ),
            catalog,
        ),
    },
    {
        "name": "B: change system_statusbarheight default to 18",
        "target": {"DEFAULT_MISMATCH": 1},
        "mutate": lambda text, catalog: (
            _replace_in_method(
                text,
                "hasAnySystemUiStartupFeature",
                'if (prefs.getInt("system_statusbarheight", 19) > 19) return true;',
                'if (prefs.getInt("system_statusbarheight", 18) > 19) return true;',
            ),
            catalog,
        ),
    },
    {
        "name": "C: change betterpopups_delay && to ||",
        "target": {"COMPOSITE_CONDITION_MISMATCH": 1},
        "mutate": lambda text, catalog: (
            _replace_in_method(
                text,
                "hasAnySystemUiStartupFeature",
                'if (prefs.getInt("system_betterpopups_delay", 0) > 0 && !prefs.getBoolean("system_betterpopups_nohide")) return true;',
                'if (prefs.getInt("system_betterpopups_delay", 0) > 0 || !prefs.getBoolean("system_betterpopups_nohide")) return true;',
            ),
            catalog,
        ),
    },
    {
        "name": "D: change system_statusbarheight comparator to ==",
        "target": {"COMPARATOR_MISMATCH": 1},
        "mutate": lambda text, catalog: (
            _replace_in_method(
                text,
                "hasAnySystemUiStartupFeature",
                'if (prefs.getInt("system_statusbarheight", 19) > 19) return true;',
                'if (prefs.getInt("system_statusbarheight", 19) == 19) return true;',
            ),
            catalog,
        ),
    },
    {
        "name": "E: remove noMoreIcon FeatureSpec from catalog",
        "target": {"FEATURE_CATALOG_GATE_UNKNOWN": 1},
        "mutate": lambda text, catalog: (text, _remove_feature_spec(catalog, "noMoreIcon")),
    },
    {
        "name": "F: make isSystemUiGlobalActionKey accept any _action",
        "target": {"DOMAIN_CONTAMINATION": 1},
        "mutate": lambda text, catalog: (
            _replace_method_body(
                text,
                "isSystemUiGlobalActionKey",
                '''        return key != null && key.endsWith("_action");''',
            ),
            catalog,
        ),
    },
    {
        "name": "G: rewrite isSystemUiGlobalActionKey with parser-unrecognized predicates",
        "target": {"SEMANTIC_REVIEW_REQUIRED": 1},
        "mutate": lambda text, catalog: (
            _replace_method_body(
                text,
                "isSystemUiGlobalActionKey",
                '''        if (key == null) return false;
        String tail = key.substring(key.length() - 7);
        if (!tail.equals("_action")) return false;
        return key.indexOf("controls_") == 0 || key.indexOf("system_") == 0;''',
            ),
            catalog,
        ),
    },
]


def run_source_mutations(repo_root: Path) -> int:
    """Run source-level mutations in isolated temp repos."""
    baseline_result = diff_from_repo(repo_root)
    baseline = baseline_result.counts
    baseline_status = baseline_result.global_action_domain.get("status", "UNKNOWN")
    print(f"Baseline: status={baseline_status}, "
          f"INSTALLER_ONLY={baseline['INSTALLER_ONLY']}, "
          f"GATE_ONLY_UNEXPLAINED={baseline['GATE_ONLY_UNEXPLAINED']}, "
          f"DOMAIN_CONTAMINATION={baseline['DOMAIN_CONTAMINATION']}, "
          f"DEFAULT_MISMATCH={baseline['DEFAULT_MISMATCH']}, "
          f"COMPARATOR_MISMATCH={baseline['COMPARATOR_MISMATCH']}, "
          f"COMPOSITE_CONDITION_MISMATCH={baseline['COMPOSITE_CONDITION_MISMATCH']}, "
          f"FEATURE_CATALOG_GATE_UNKNOWN={baseline['FEATURE_CATALOG_GATE_UNKNOWN']}, "
          f"SEMANTIC_REVIEW_REQUIRED={baseline['SEMANTIC_REVIEW_REQUIRED']}")

    inv_tool = _load_inventory_tool()
    all_ok = True

    for mut in _SOURCE_MUTATIONS:
        name = mut["name"]
        target = mut["target"]
        with tempfile.TemporaryDirectory() as td:
            temp_root = Path(td)
            _copy_source_skeleton(repo_root, temp_root)
            installer_path = temp_root / "app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java"
            catalog_path = temp_root / "app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt"

            installer_text = installer_path.read_text(encoding="utf-8")
            catalog_text = catalog_path.read_text(encoding="utf-8")
            new_installer, new_catalog = mut["mutate"](installer_text, catalog_text)
            installer_path.write_text(new_installer, encoding="utf-8", newline="\n")
            catalog_path.write_text(new_catalog, encoding="utf-8", newline="\n")

            inv_tool.write_inventory(temp_root, verify=False)
            result = diff_from_repo(temp_root)
            counts = result.counts

        ok = True
        details = []
        for key, want in target.items():
            if counts.get(key, 0) <= baseline.get(key, 0):
                ok = False
                details.append(f"{key} expected > {baseline.get(key, 0)}, got {counts.get(key, 0)}")
        # Source mutation G additionally checks the domain status.
        if name.startswith("G:") and result.global_action_domain.get("status") != "UNKNOWN":
            ok = False
            details.append(f"global_action_domain.status expected UNKNOWN, got {result.global_action_domain.get('status')}")

        if ok:
            print(f"PASS {name}")
        else:
            print(f"FAIL {name}: {', '.join(details)}")
            all_ok = False

    return 0 if all_ok else 1


def main() -> int:
    parser = argparse.ArgumentParser(description="SystemUI startup gate differential audit")
    parser.add_argument("--output", type=Path, default=REPO_ROOT, help="repo root path")
    parser.add_argument("--verify", action="store_true", help="verify existing diff files are up to date")
    parser.add_argument("--mutations", action="store_true", help="run inventory-level mutation-style counter-proof tests")
    parser.add_argument("--source-mutations", action="store_true", help="run source-level mutation-style counter-proof tests")
    args = parser.parse_args()

    if args.mutations:
        return run_mutations(args.output)
    if args.source_mutations:
        return run_source_mutations(args.output)

    try:
        path, md_path, _, _ = write_diff(args.output, verify=args.verify)
        if args.verify:
            print(f"Verified {path}\nVerified {md_path}")
        else:
            print(f"Wrote {path}\nWrote {md_path}")
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
