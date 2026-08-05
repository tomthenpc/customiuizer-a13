#!/usr/bin/env python3
"""Static hazard scanner for injected Android runtime code.

The scanner is intentionally strict. Existing reviewed findings can be frozen
in a JSON baseline. CI fails only on new fingerprints unless --strict-all is
used. Add `BRUTAL_ALLOW:<RULE>` on the same line for a narrow reviewed waiver.
"""
from __future__ import annotations

import argparse
import functools
import hashlib
import json
import re
import sys
from dataclasses import dataclass, asdict
from pathlib import Path


@dataclass(frozen=True)
class Finding:
    rule: str
    path: str
    line: int
    snippet: str

    @property
    def fingerprint(self) -> str:
        normalized = re.sub(r"\s+", " ", self.snippet.strip())
        raw = f"{self.rule}\0{self.path}\0{normalized}".encode()
        return hashlib.sha256(raw).hexdigest()[:20]


RULES: list[tuple[str, re.Pattern[str], str]] = [
    (
        "EMPTY_CATCH",
        re.compile(r"catch\s*\([^)]*\)\s*\{\s*(?://[^\n]*)?\s*\}", re.S),
        "empty catch hides runtime failures",
    ),
    (
        "GLOBAL_SCOPE",
        re.compile(r"\bGlobalScope\s*\."),
        "GlobalScope has no injectable lifecycle owner",
    ),
    (
        "THREAD_SLEEP",
        re.compile(r"\bThread\.sleep\s*\("),
        "blocking sleep in injected production source",
    ),
    (
        "RUN_BLOCKING",
        re.compile(r"\brunBlocking\s*\{"),
        "runBlocking can block SystemUI/system_server",
    ),
    (
        "PRINT_STACK_TRACE",
        re.compile(r"\.printStackTrace\s*\("),
        "printStackTrace is uncontrolled production diagnostics",
    ),
    (
        "SYSTEM_OUT",
        re.compile(r"\bSystem\.(?:out|err)\."),
        "System.out/System.err in injected production source",
    ),
    (
        "NATIVE_LOAD",
        re.compile(r"\bSystem\.load(?:Library)?\s*\("),
        "native loading requires explicit benchmark and ABI review",
    ),
    (
        "STATIC_STRONG_ANDROID_OWNER",
        re.compile(
            r"(?m)^[ \t]*(?:public|private|protected|internal)?[ \t]*(?:static[ \t]+|@JvmField[ \t]+)?"
            r"(?:"
            r"(?:var|val)[ \t]+[A-Za-z0-9_]+[ \t]*:[ \t]*\b(?:Context|Activity|View|Fragment|Window|Drawable)\??[ \t]*(?:=|;)"
            r"|"
            r"\b(?:Context|Activity|View|Fragment|Window|Drawable)(?:<[^>\n]*>)?[ \t]+[A-Za-z0-9_]+[ \t]*(?:=|;)"
            r")"
        ),
        "potential strong Android owner; require scoped owner or WeakReference",
    ),
    (
        "EAGER_HANDLER_THREAD",
        re.compile(r"HandlerThread\s*\([^)]*\)[\s\S]{0,160}?\.start\s*\("),
        "eager HandlerThread start; worker must be lazy and bounded",
    ),
    (
        "UNBOUNDED_GLOBAL_COLLECTION",
        re.compile(
            r"(?m)^\s*(?:static\s+|@JvmField\s+)?(?:val|var|final\s+\w+\s+)"
            r"\w+\s*(?::[^=\n]+)?=\s*(?:mutableListOf|mutableMapOf|mutableSetOf|"
            r"ArrayList|HashMap|HashSet|ConcurrentHashMap)\s*[<(]"
        ),
        "global mutable collection needs hard bound and lifecycle cleanup",
    ),
]


def line_number(text: str, offset: int) -> int:
    return text[:offset].count("\n") + 1


# Match type / function declaration lines, accounting for `companion object`.
_TYPE_DECL_RE = re.compile(
    r"^(?:\s*(?:abstract\s+|data\s+|sealed\s+|open\s+|public\s+|private\s+|"
    r"protected\s+|internal\s+|final\s+))*"
    r"(class|object|companion\s+object|interface|enum\s+class|enum)\b"
)
_FUN_DECL_RE = re.compile(
    r"^(?:\s*(?:public|private|protected|internal|override|abstract|open|final|"
    r"inline|crossinline|noinline|operator|infix|suspend|tailrec|external|"
    r"expect|actual)\s+)*fun\b"
    r"|"
    r"(?:public|private|protected|static|final|abstract|synchronized|"
    r"native|strictfp|\s)+[A-Za-z0-9_<>,\[\].\s]+\s+[A-Za-z0-9_]+\s*\("
)


def _declaration_kind(line: str) -> str | None:
    """Return 'type', 'fun', or None based on whether the line starts a declaration."""
    if _TYPE_DECL_RE.match(line):
        return "type"
    if _FUN_DECL_RE.match(line):
        return "fun"
    return None


def _detect_type_kind(line: str) -> str:
    """Return a coarse kind for a type declaration line: object, class, etc."""
    m = re.match(
        r"\s*(?:abstract\s+|data\s+|sealed\s+|open\s+|public\s+|private\s+|"
        r"protected\s+|internal\s+|final\s+)*"
        r"(class|object|companion\s+object|interface|enum\s+class|enum)\b",
        line,
    )
    if not m:
        return "other"
    kind = m.group(1)
    if kind == "companion object":
        return "companion"
    if kind == "object":
        return "object"
    return "class"  # class, interface, enum, enum class


def _has_static_keyword(line: str, prev_line: str) -> bool:
    """True if the field line or previous line indicates a static field."""
    # Java `static` keyword.
    if re.search(r"\bstatic\b", line):
        return True
    # Kotlin `@JvmField` on the field line or previous line.
    if "@JvmField" in line or "@JvmField" in prev_line:
        return True
    return False


@functools.lru_cache(maxsize=512)
def _scope_state_for(text: str) -> dict[int, tuple[bool, bool, bool, bool]]:
    """Return a map from 1-indexed line numbers to (in_fun, in_object, in_class, is_static_field).

    in_object is also true for `companion object` because those fields are static
    at the JVM level.  in_class is the opposite of in_object for type scopes, but
    note that object members can also appear inside nested classes/objects.
    """
    lines = text.splitlines()
    line_count = len(lines)
    info: dict[int, tuple[bool, bool, bool, bool]] = {}

    stack: list[tuple[str, str]] = []  # (block_kind, type_kind)
    pending: list[str] = []  # type/fun declarations waiting for their opening brace

    for i, line in enumerate(lines, 1):
        prev_line = lines[i - 2] if i > 1 else ""
        kind = _declaration_kind(line)
        if kind:
            pending.append(kind)

        # Process braces in order on this line.
        for ch in line:
            if ch == "{":
                block_kind = pending.pop(0) if pending else "other"
                type_kind = _detect_type_kind(line) if block_kind == "type" else "other"
                # For `companion object`, the type kind is 'companion' and we treat it like 'object'.
                if type_kind == "companion":
                    type_kind = "object"
                stack.append((block_kind, type_kind))
            elif ch == "}":
                if stack:
                    stack.pop()

        # Determine state for this line *after* processing braces.  This means the
        # body of a declaration that opened on this line is considered entered.
        in_fun = any(b == "fun" for b, _ in stack)
        in_object = any(t == "object" for _, t in stack)
        in_class = any(t == "class" for _, t in stack)
        is_static = _has_static_keyword(line, prev_line)
        info[i] = (in_fun, in_object, in_class, is_static)

    return info


def _is_static_owner_scope(text: str, line: int, source_line: str) -> bool:
    """Return True only for static/object fields, not locals or class instance fields."""
    info = _scope_state_for(text)
    in_fun, in_object, in_class, is_static = info.get(line, (False, False, False, False))
    # Locals inside functions are never the target of this rule.
    if in_fun:
        return False
    # Object / companion object fields are static at the JVM level.
    if in_object:
        return True
    # Java static fields.
    if is_static:
        return True
    # Instance fields in classes are ordinary instance fields, not static owners.
    return False


def scan_file(path: Path, repo_root: Path) -> list[Finding]:
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return []
    rel = path.relative_to(repo_root).as_posix()
    findings: list[Finding] = []
    for rule, pattern, _ in RULES:
        for match in pattern.finditer(text):
            line = line_number(text, match.start())
            source_line = text.splitlines()[line - 1] if text.splitlines() else ""
            if f"BRUTAL_ALLOW:{rule}" in source_line:
                continue
            if rule == "STATIC_STRONG_ANDROID_OWNER" and not _is_static_owner_scope(text, line, source_line):
                continue
            snippet = re.sub(r"\s+", " ", match.group(0))[:220]
            findings.append(Finding(rule, rel, line, snippet))

    # Catch(Throwable) requires visible fatal propagation in the local block.
    catch_pattern = re.compile(r"catch\s*\(\s*(\w+)\s*:\s*Throwable\s*\)\s*\{", re.M)
    for match in catch_pattern.finditer(text):
        start = match.end()
        window = text[start : start + 700]
        end = window.find("}")
        block = window if end < 0 else window[:end]
        if not re.search(
            r"\bthrow\b|rethrow|fatal|OutOfMemoryError|ThreadDeath|VirtualMachineError",
            block,
            re.I,
        ):
            line = line_number(text, match.start())
            source_line = text.splitlines()[line - 1]
            if "BRUTAL_ALLOW:CATCH_THROWABLE_NO_FATAL" not in source_line:
                findings.append(
                    Finding(
                        "CATCH_THROWABLE_NO_FATAL",
                        rel,
                        line,
                        re.sub(r"\s+", " ", text[match.start() : start + min(len(block), 220)]),
                    )
                )
    return findings


def collect(root: Path, paths: list[str]) -> list[Finding]:
    findings: list[Finding] = []
    for raw in paths:
        path = root / raw
        candidates = [path] if path.is_file() else [
            *path.rglob("*.kt"),
            *path.rglob("*.java"),
        ] if path.exists() else []
        for candidate in sorted(set(candidates)):
            findings.extend(scan_file(candidate, root))
    return findings


def load_baseline(path: Path) -> set[str]:
    if not path.exists():
        return set()
    data = json.loads(path.read_text(encoding="utf-8"))
    return set(data.get("fingerprints", []))


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--repo-root", default=".")
    p.add_argument(
        "--path",
        action="append",
        default=[],
        help="file or directory relative to repo; repeatable",
    )
    p.add_argument("--baseline", default="docs/audit/SOURCE_HAZARD_BASELINE.json")
    p.add_argument("--write-baseline", action="store_true")
    p.add_argument("--strict-all", action="store_true")
    p.add_argument("--json-output")
    args = p.parse_args(argv)

    root = Path(args.repo_root).resolve()
    paths = args.path or ["app/src/main/java"]
    findings = collect(root, paths)
    baseline_path = root / args.baseline

    if args.write_baseline:
        baseline_path.parent.mkdir(parents=True, exist_ok=True)
        payload = {
            "schema": 1,
            "fingerprints": sorted(f.fingerprint for f in findings),
            "findings": [dict(asdict(f), fingerprint=f.fingerprint) for f in findings],
        }
        baseline_path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
        print(f"Wrote baseline with {len(findings)} finding(s): {baseline_path}")
        return 0

    baseline = set() if args.strict_all else load_baseline(baseline_path)
    new_findings = [f for f in findings if f.fingerprint not in baseline]
    payload = {
        "total": len(findings),
        "baseline": len(baseline),
        "new": len(new_findings),
        "findings": [dict(asdict(f), fingerprint=f.fingerprint) for f in new_findings],
    }
    if args.json_output:
        out = Path(args.json_output)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")

    if new_findings:
        print(f"New source hazards: {len(new_findings)}")
        for f in new_findings:
            print(f"  {f.path}:{f.line}: {f.rule}: {f.snippet}")
        return 1
    print(f"Source hazard scan passed: {len(findings)} reviewed finding(s), 0 new")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
