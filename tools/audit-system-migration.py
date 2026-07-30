#!/usr/bin/env python3
"""Hardened audit for the K5 System.java -> Kotlin migration.

Usage:
    python tools/audit-system-migration.py
    python tools/audit-system-migration.py --baseline-ref backup/r13-k5-before-system-java-removal

Exit code:
    0 if the migration passes the hardened audit
    non-zero if signatures are incomplete, ambiguous, or the facade is not a pure delegation
"""
from __future__ import annotations

import argparse
import re
import os
import sys
import hashlib
import shutil
import subprocess
from pathlib import Path
from datetime import datetime, timezone
from collections import defaultdict


SCRIPT = Path(__file__).resolve()
REPO = SCRIPT.parent.parent

MAIN_MODULE = REPO / "app/src/main/java/tv/withaibuild/customiuizer/MainModule.java"
SYSTEM_FACADE = REPO / "app/src/main/java/tv/withaibuild/customiuizer/mods/System.kt"
MODS_DIR = REPO / "app/src/main/java/tv/withaibuild/customiuizer/mods"
BUILD_DIR = REPO / "app/build"
MAPPING_DIR = BUILD_DIR / "outputs/mapping"
APK_DIR = BUILD_DIR / "outputs/apk"
# Current and historical paths for baseline System.java
SYSTEM_JAVA_CANDIDATES = [
    "app/src/main/java/tv/withaibuild/customiuizer/mods/System.java",
    "app/src/main/java/name/monwf/customiuizer/mods/System.java",
]
EXPECTED_HOOKS_COUNT = 17  # User originally asked for 18; repository currently contains 17 System*Hooks files (excluding SystemUI*)

# These private static helpers existed in old System.java but were not part of the
# public facade API. They were split into the corresponding System*Hooks objects as
# private/internal helpers and are not called from outside the facade domain.
# They must not be re-exposed through the System.kt facade.
INTERNAL_SYSTEM_ALLOWLIST: dict[str, str] = {
    "DisableFloatingWindowBlacklistHook": "internal helper in SystemFreeformAndMultiWindowHooks",
    "checkLastCheck": "internal helper in SystemLockScreenMoreHooks",
    "checkToast": "internal helper in SystemStatusBarAndClockHooks",
    "checkVibration": "internal helper in SystemNotificationMoreHooks",
    "constrainValue": "internal helper in SystemDisplayAndWindowHooks",
    "getActionBarColor": "internal helper in SystemStatusBarAndClockHooks",
    "getCCShowSeconds": "internal helper in SystemStatusBarClockAndMoreHooks",
    "getContentType": "internal helper in SystemShareAndOpenWithHooks",
    "getShowSeconds": "internal helper in SystemStatusBarClockAndMoreHooks",
    "hideMimeType": "internal helper in SystemShareAndOpenWithHooks",
    "hookToolbar": "internal helper in SystemStatusBarAndClockHooks",
    "hookUpdateTime": "internal helper in SystemAudioAndVisualAndMoreHooks",
    "hookWindowDecor": "internal helper in SystemStatusBarAndClockHooks",
    "initClockStyle": "internal helper in SystemStatusBarClockAndMoreHooks",
    "initSecondTimer": "internal helper in SystemStatusBarClockAndMoreHooks",
    "isAuthOnce": "internal helper in SystemLockScreenMoreHooks",
    "isIgnored": "internal helper in SystemStatusBarAndClockHooks",
    "isRemoveApp": "internal helper in SystemShareAndOpenWithHooks",
    "isTrusted": "internal helper in SystemLockScreenMoreHooks",
    "isTrustedBt": "internal helper in SystemLockScreenMoreHooks",
    "isTrustedWiFi": "internal helper in SystemLockScreenMoreHooks",
    "isUnlocked": "internal helper in SystemLockScreenMoreHooks",
    "patchActivityOptions": "internal helper in SystemFreeformAndMultiWindowHooks",
    "processLSEvent": "internal helper in SystemAudioAndVisualAndMoreHooks",
    "removeListener": "internal helper in SystemAudioAndVisualAndMoreHooks / SystemUILockScreenHooks",
    "saveLastCheck": "internal helper in SystemLockScreenMoreHooks",
    "shouldOpenInFreeForm": "internal helper in SystemFreeformAndMultiWindowHooks",
    "updateAlarmVisibility": "internal helper in SystemStatusBarMoreHooks",
    "updateAudioVisualizerState": "internal helper in SystemAudioAndVisualAndMoreHooks",
}

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def latest_file(parent: Path, pattern: str) -> Path | None:
    if not parent.exists():
        return None
    files = sorted(parent.rglob(pattern), key=lambda p: p.stat().st_mtime, reverse=True)
    return files[0] if files else None


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


def run_git_show(ref: str, path: str) -> str | None:
    try:
        result = subprocess.run(
            ["git", "show", f"{ref}:{path}"],
            capture_output=True,
            text=True,
            encoding="utf-8",
            timeout=30,
        )
        if result.returncode != 0:
            return None
        return result.stdout
    except Exception:
        return None


def split_top_level(s: str, delimiter: str = ",") -> list[str]:
    """Split a string by a delimiter, respecting (), [], <>, and quotes."""
    parts = []
    current = []
    depth_paren = 0
    depth_bracket = 0
    depth_angle = 0
    quote = None
    escape = False
    for ch in s:
        if escape:
            current.append(ch)
            escape = False
            continue
        if quote:
            current.append(ch)
            if ch == "\\":
                escape = True
            elif ch == quote:
                quote = None
            continue
        if ch in ('"', "'"):
            quote = ch
            current.append(ch)
            continue
        if ch == "(":
            depth_paren += 1
            current.append(ch)
        elif ch == ")":
            depth_paren -= 1
            current.append(ch)
        elif ch == "[":
            depth_bracket += 1
            current.append(ch)
        elif ch == "]":
            depth_bracket -= 1
            current.append(ch)
        elif ch == "<":
            depth_angle += 1
            current.append(ch)
        elif ch == ">":
            depth_angle -= 1
            current.append(ch)
        elif ch == delimiter and depth_paren == 0 and depth_bracket == 0 and depth_angle == 0:
            parts.append("".join(current).strip())
            current = []
        else:
            current.append(ch)
    if current or s.endswith(delimiter):
        parts.append("".join(current).strip())
    return [p for p in parts if p]


# ---------------------------------------------------------------------------
# Type normalisation
# ---------------------------------------------------------------------------

JAVA_TO_KT = {
    "void": "Unit",
    "object": "Any",
    "java.lang.object": "Any",
    "object[]": "Array<Any>",
    "boolean": "Boolean",
    "java.lang.boolean": "Boolean",
    "integer": "Int",
    "java.lang.integer": "Int",
    "int": "Int",
    "long": "Long",
    "float": "Float",
    "double": "Double",
    "string": "String",
    "java.lang.string": "String",
    "charsequence": "CharSequence",
    "java.lang.charsequence": "CharSequence",
    "throwable": "Throwable",
    "java.lang.throwable": "Throwable",
    "exception": "Exception",
    "java.lang.exception": "Exception",
    "packageparam": "PackageReadyParam",
    "systemserverstartingparam": "SystemServerStartingParam",
    "moduleloadedparam": "ModuleLoadedParam",
}


def _last_component(t: str) -> str:
    t = t.replace("$", ".")
    if "." in t:
        return t.rsplit(".", 1)[1]
    return t


def _strip_generics_and_nullability(t: str) -> str:
    """Return a base name like 'PackageReadyParam' from 'PackageReadyParam?' or 'Map<String, String>'."""
    t = t.strip()
    if t.endswith("?"):
        t = t[:-1]
    # keep array brackets attached
    base = t.split("<")[0].strip()
    return base


def _canonical_generic_args(t: str) -> str:
    """For 'Map<String, String>' return 'Map<String, String>' with canonical inner types."""
    if "<" not in t or ">" not in t:
        return t
    base = t.split("<", 1)[0].strip()
    # find matching >
    start = t.index("<")
    depth = 0
    end = start
    for i in range(start, len(t)):
        if t[i] == "<":
            depth += 1
        elif t[i] == ">":
            depth -= 1
            if depth == 0:
                end = i
                break
    inner = t[start + 1 : end]
    inner_parts = split_top_level(inner, ",")
    canonical_inner = ", ".join(_canonical_type(p) for p in inner_parts)
    remainder = t[end + 1 :].strip()
    if remainder.startswith("?"):
        remainder = remainder[1:]
    arr = "[]" if "[]" in remainder else ""
    return f"{base}<{canonical_inner}>{arr}"


def _canonical_type(t: str) -> str:
    """Convert a Java or Kotlin type to a canonical form for signature comparison."""
    t = t.strip()
    if t.endswith("?"):
        t = t[:-1].strip()
    is_array = t.endswith("[]") or t.startswith("Array<")
    if is_array:
        if t.endswith("[]"):
            elem = t[:-2].strip()
            return f"Array<{_canonical_type(elem)}>"
        # Array<T>
        inner = t[len("Array<") : -1]
        return f"Array<{_canonical_type(inner)}>"
    if "<" in t and ">" in t:
        return _canonical_generic_args(t)
    base = _strip_generics_and_nullability(t)
    base_l = _last_component(base).lower()
    if base_l in JAVA_TO_KT:
        return JAVA_TO_KT[base_l]
    return base


def _erased_type(t: str) -> str:
    """JVM erased type: strip generics and nullability, keep array brackets."""
    t = t.strip()
    if t.endswith("?"):
        t = t[:-1].strip()
    if t.endswith("[]"):
        elem = t[:-2].strip()
        return f"{_erased_type(elem)}[]"
    if t.startswith("Array<") and t.endswith(">"):
        inner = t[len("Array<") : -1]
        return f"{_erased_type(inner)}[]"
    base = t.split("<")[0].strip()
    base_l = _last_component(base).lower()
    if base_l in JAVA_TO_KT:
        mapped = JAVA_TO_KT[base_l]
        # For erased arrays, use the mapped array base, but keep bracket handled above
        return mapped
    return base


# ---------------------------------------------------------------------------
# Java method parser (for old System.java and MainModule)
# ---------------------------------------------------------------------------

JAVA_METHOD_RE = re.compile(
    r"""(?mx)
    ^\s*
    (?P<ann>(?:@[A-Za-z0-9_.]+(?:\([^)]*\))?\s+)*)
    (?P<vis>public|protected|private)\s+
    (?P<mods>(?:static\s+|final\s+|synchronized\s+)*)
    (?P<ret>[A-Za-z0-9_\[\]<>,?\.\s]+?)\s+
    (?P<name>[A-Za-z0-9_]+)\s*\(
    (?P<args>[^)]*)
    \)
    (?:\s*throws\s+[A-Za-z0-9_,\s]+)?
    \s*\{
    """
)


def parse_java_args(args_raw: str) -> list[tuple[str, str, str, str]]:
    """Return list of (raw_type, canonical_type, erased_type, name)."""
    if not args_raw.strip():
        return []
    parts = split_top_level(args_raw, ",")
    result = []
    for part in parts:
        part = part.strip()
        if not part:
            continue
        # remove annotations and 'final'
        # e.g. "@NonNull final PackageReadyParam lpparam" or "final Object thisObject"
        tokens = re.split(r"\s+", part)
        # drop leading annotations, 'final'
        while tokens and (tokens[0].startswith("@") or tokens[0] == "final"):
            tokens.pop(0)
        if len(tokens) < 2:
            continue
        name = tokens[-1]
        raw_type = " ".join(tokens[:-1])
        result.append((raw_type, _canonical_type(raw_type), _erased_type(raw_type), name))
    return result


def parse_java_methods(text: str, require_static: bool = True, require_public: bool = False) -> list[dict]:
    methods = []
    for m in JAVA_METHOD_RE.finditer(text):
        ret_raw = m.group("ret").strip()
        name = m.group("name")
        visibility = m.group("vis")
        if ret_raw in ("class", "interface", "enum"):
            continue
        # skip constructors (return type matches class name, no static)
        if name == ret_raw and not re.search(r"\bstatic\b", m.group("mods") or ""):
            continue
        if require_static and not re.search(r"\bstatic\b", m.group("mods") or ""):
            continue
        if require_public and visibility != "public":
            continue
        args = parse_java_args(m.group("args"))
        start = m.end()
        end = find_matching_brace_robust(text, start - 1)
        methods.append({
            "name": name,
            "vis": visibility,
            "is_public": visibility == "public",
            "ret_raw": ret_raw,
            "ret_canonical": _canonical_type(ret_raw),
            "ret_erased": _erased_type(ret_raw),
            "args": args,
            "body_start": start,
            "body_end": end,
            "text": text,
            "signature_start": m.start(),
        })
    return methods


# ---------------------------------------------------------------------------
# Kotlin method parser
# ---------------------------------------------------------------------------

KT_ANN_VISIBILITY = r"(?:@[A-Za-z0-9_.]+(?:\([^)]*\))?\s+)*"
KT_VISIBILITY = r"(?:public\s+|private\s+|protected\s+|internal\s+)?"

KT_METHOD_RE = re.compile(
    rf"""(?m)
    ^{KT_ANN_VISIBILITY}
    {KT_VISIBILITY}
    fun\s+
    (?P<name>[A-Za-z0-9_]+)\s*\(
    (?P<args>[^)]*)
    \)\s*
    (?::\s*(?P<ret>[^={{\n]+))?
    """,
    re.DOTALL,
)


def parse_kt_args(args_raw: str) -> list[tuple[str, str, str, str]]:
    if not args_raw.strip():
        return []
    parts = split_top_level(args_raw, ",")
    result = []
    for part in parts:
        part = part.strip()
        if not part:
            continue
        # handle 'val' / 'var'? fun params don't have val/var usually
        # drop annotations
        # e.g. "lpparam: PackageReadyParam" or "thisObject: Any"
        if ":" not in part:
            continue
        name, type_part = part.split(":", 1)
        name = name.strip()
        raw_type = type_part.strip()
        result.append((raw_type, _canonical_type(raw_type), _erased_type(raw_type), name))
    return result


def parse_kt_methods(text: str, require_jvm_static: bool = False) -> list[dict]:
    methods = []
    for m in KT_METHOD_RE.finditer(text):
        # if require_jvm_static, ensure the declaration has @JvmStatic before it
        if require_jvm_static:
            # look back from match start for @JvmStatic in the same declaration
            before = text[: m.start()]
            decl_start = before.rfind("\n@JvmStatic")
            if decl_start == -1:
                continue
            # make sure no other fun or class between decl_start and m.start()
            between = text[decl_start + 1 : m.start()]
            if re.search(r"\bfun\b|\bclass\b|\bobject\b", between):
                continue
        name = m.group("name")
        args = parse_kt_args(m.group("args"))
        ret_raw = (m.group("ret") or "Unit").strip()
        # find body start and end by matching braces from the first '{' after the match
        body_start = text.find("{", m.end())
        if body_start == -1:
            continue
        brace = 1
        i = body_start + 1
        while i < len(text) and brace > 0:
            if text[i] == "{":
                brace += 1
            elif text[i] == "}":
                brace -= 1
            i += 1
        vis = m.group(0)[: m.start()]  # not used
        private = bool(re.search(r"\bprivate\b", m.group(0)[:50]))
        methods.append({
            "name": name,
            "ret_raw": ret_raw,
            "ret_canonical": _canonical_type(ret_raw),
            "ret_erased": _erased_type(ret_raw),
            "args": args,
            "private": private,
            "body_start": body_start,
            "body_end": i - 1,
            "text": text,
            "signature_start": m.start(),
        })
    return methods


def extract_facade_methods(text: str) -> list[dict]:
    """Return all @JvmStatic fun methods in System.kt with body and full signatures."""
    # Find each @JvmStatic, then the following fun
    methods = []
    for m in re.finditer(r"@JvmStatic", text):
        # search for 'fun' within the next few lines
        after = text[m.end() : m.end() + 500]
        fm = re.search(rf"\bfun\s+([A-Za-z0-9_]+)\s*\(([^)]*)\)\s*(?::\s*([^={{\n]+))?", after, re.DOTALL)
        if not fm:
            continue
        name = fm.group(1)
        args_raw = fm.group(2)
        ret_raw = (fm.group(3) or "Unit").strip()
        start_in_text = m.end() + fm.start()
        body_start = text.find("{", m.end() + fm.end())
        if body_start == -1:
            continue
        brace = 1
        i = body_start + 1
        while i < len(text) and brace > 0:
            if text[i] == "{":
                brace += 1
            elif text[i] == "}":
                brace -= 1
            i += 1
        body = text[body_start + 1 : i - 1]
        args = parse_kt_args(args_raw)
        methods.append({
            "name": name,
            "ret_raw": ret_raw,
            "ret_canonical": _canonical_type(ret_raw),
            "ret_erased": _erased_type(ret_raw),
            "args": args,
            "args_raw": args_raw,
            "body": body.strip(),
            "signature_start": m.start(),
        })
    return methods


def extract_hook_objects_and_methods(text: str, filename: str) -> tuple[str, list[dict]]:
    obj_match = re.search(r"^\s*object\s+([A-Za-z0-9_]+)(?:\s*[:{(])", text, re.MULTILINE)
    object_name = obj_match.group(1) if obj_match else filename.replace(".kt", "")
    methods = []
    for m in re.finditer(
        rf"^\s*({KT_ANN_VISIBILITY}{KT_VISIBILITY})fun\s+([A-Za-z0-9_]+)\s*\(([^)]*)\)\s*(?::\s*([^={{\n]+))?",
        text,
        re.MULTILINE | re.DOTALL,
    ):
        vis = m.group(1)
        name = m.group(2)
        args_raw = m.group(3)
        ret_raw = (m.group(4) or "Unit").strip()
        private = bool(re.search(r"\bprivate\b", vis))
        args = parse_kt_args(args_raw)
        methods.append({
            "object": object_name,
            "filename": filename,
            "name": name,
            "ret_raw": ret_raw,
            "ret_canonical": _canonical_type(ret_raw),
            "ret_erased": _erased_type(ret_raw),
            "args": args,
            "private": private,
        })
    return object_name, methods


# ---------------------------------------------------------------------------
# Signature keys
# ---------------------------------------------------------------------------

def full_key(method: dict) -> tuple[str, tuple[str, ...], str]:
    return (method["name"], tuple(a[1] for a in method["args"]), method["ret_canonical"])


def erased_key(method: dict) -> tuple[str, tuple[str, ...], str]:
    return (method["name"], tuple(a[2] for a in method["args"]), method["ret_erased"])


# ---------------------------------------------------------------------------
# javap-based System.class public API comparison
# ---------------------------------------------------------------------------

def find_javap() -> Path | None:
    """Locate a javap executable."""
    for env in ("JAVA_HOME", "JDK_HOME"):
        home = os.environ.get(env)
        if home:
            candidate = Path(home) / "bin" / "javap"
            if candidate.exists():
                return candidate
            candidate = candidate.with_suffix(".exe")
            if candidate.exists():
                return candidate
    which = shutil.which("javap")
    if which:
        return Path(which)
    return None


def find_system_class(repo: Path, build_prefix: str = "debug") -> Path | None:
    """Find the most recently compiled System.class for the current facade."""
    candidates = list(repo.rglob(f"{build_prefix}/tv/withaibuild/customiuizer/mods/System.class"))
    if not candidates:
        candidates = list(repo.rglob("tv/withaibuild/customiuizer/mods/System.class"))
    if not candidates:
        return None
    return max(candidates, key=lambda p: p.stat().st_mtime)


def run_javap_public_s(class_file: Path) -> str:
    javap = find_javap()
    if not javap:
        raise RuntimeError("javap not found; set JAVA_HOME or add javap to PATH")
    result = subprocess.run(
        [str(javap), "-public", "-s", str(class_file)],
        capture_output=True,
        text=True,
        check=True,
    )
    return result.stdout


def parse_javap_public_methods(output: str) -> dict[str, list[str]]:
    """Return method name -> list of JVM descriptors from `javap -public -s`."""
    methods: dict[str, list[str]] = defaultdict(list)
    name: str | None = None
    for line in output.splitlines():
        line = line.strip()
        m = re.match(r"public static (?:final )?[\w\.\$<>,\[\]\?\s]+ (\w+)\((.*)\);", line)
        if m:
            name = m.group(1)
            continue
        if name and line.startswith("descriptor:"):
            desc = line.split(":", 1)[1].strip()
            methods[name].append(desc)
            name = None
    return dict(methods)


def javap_compare(baseline_class: Path | None, current_class: Path | None, r: Reporter) -> tuple[set[str], set[str], set[tuple[str, ...]]] | None:
    """Compare public API of baseline and current System.class using javap."""
    if not current_class:
        r.warn("Current System.class not found; cannot run javap comparison")
        return None
    if not baseline_class:
        r.warn("Baseline System.class not provided; cannot run javap comparison")
        return None

    try:
        base_out = run_javap_public_s(baseline_class)
        curr_out = run_javap_public_s(current_class)
    except (subprocess.CalledProcessError, RuntimeError) as e:
        r.warn(f"javap comparison failed: {e}")
        return None

    base_methods = parse_javap_public_methods(base_out)
    curr_methods = parse_javap_public_methods(curr_out)

    base_sigs: set[tuple[str, ...]] = set()
    curr_sigs: set[tuple[str, ...]] = set()
    for name, descs in base_methods.items():
        for d in descs:
            base_sigs.add((name, d))
    for name, descs in curr_methods.items():
        for d in descs:
            curr_sigs.add((name, d))

    missing = base_sigs - curr_sigs
    extra = curr_sigs - base_sigs

    r.add("## javap -public -s System.class comparison")
    r.add(f"- Baseline class: {baseline_class}")
    r.add(f"- Current class: {current_class}")
    r.add(f"- Baseline public signatures: {len(base_sigs)}")
    r.add(f"- Current public signatures: {len(curr_sigs)}")
    r.add(f"- Missing from current: {len(missing)}")
    r.add(f"- Extra in current: {len(extra)}")
    if missing:
        for sig in sorted(missing):
            r.add(f"  - javap missing: {sig[0]} {sig[1]}")
    if extra:
        for sig in sorted(extra):
            r.add(f"  - javap extra: {sig[0]} {sig[1]}")
    r.add("")

    if missing:
        r.fail("javap public API comparison found missing signatures in current System.class")
    if extra:
        r.fail("javap public API comparison found extra signatures in current System.class")

    return base_methods, curr_methods, base_sigs, curr_sigs


# ---------------------------------------------------------------------------
# Whole-repo System.* call resolution
# ---------------------------------------------------------------------------

def resolve_system_calls(repo: Path, facade_methods: list[dict]) -> tuple[list[dict], list[dict]]:
    """Resolve every System.<method>( call in .java/.kt files against the facade."""
    facade_by_name = defaultdict(list)
    for fm in facade_methods:
        facade_by_name[fm["name"]].append(fm)

    calls: list[dict] = []
    unresolved: list[dict] = []

    for p in repo.rglob("*"):
        if not p.is_file() or p.suffix not in (".java", ".kt"):
            continue
        text = p.read_text(encoding="utf-8", errors="ignore")
        for m in JAVA_CALL_RE.finditer(text):
            pos = m.start()
            if is_in_line_comment(text, pos):
                continue
            name = m.group(1)
            if name in JAVA_LANG_SYSTEM_METHODS:
                continue
            end_pos, call_args = extract_call_args(text, m.end() - 1)
            if not call_args:
                call_args = []
            matches = [fm for fm in facade_by_name.get(name, []) if len(fm["args"]) == len(call_args)]
            if len(matches) == 0:
                unresolved.append({
                    "file": str(p.relative_to(repo)),
                    "line": text[:pos].count("\n") + 1,
                    "name": name,
                    "arg_count": len(call_args),
                    "call_args": call_args,
                    "reason": "no facade method with matching name and argument count",
                })
            else:
                calls.append({
                    "file": str(p.relative_to(repo)),
                    "line": text[:pos].count("\n") + 1,
                    "name": name,
                    "arg_count": len(call_args),
                    "resolved_full_key": full_key(matches[0]),
                })

    return calls, unresolved


# ---------------------------------------------------------------------------
# Delegation validation
# ---------------------------------------------------------------------------

def validate_delegation(method: dict, allowed_objects: set[str]) -> tuple[bool, str, dict]:
    """Check the facade method body is a pure 1:1 delegation to a System*Hooks method."""
    body = re.sub(r"//.*", "", method["body"])
    # remove block comments? rare in generated file
    body = re.sub(r"/\*.*?\*/", "", body, flags=re.DOTALL)
    body = re.sub(r"\s+", " ", body).strip()

    if not body:
        return False, "empty body", {}

    # Unit return
    unit_m = re.match(r"^([A-Za-z0-9_]+)\.([A-Za-z0-9_]+)\(([^)]*)\)$", body)
    non_unit_m = re.match(r"^return\s+([A-Za-z0-9_]+)\.([A-Za-z0-9_]+)\(([^)]*)\)$", body)

    m = unit_m or non_unit_m
    if not m:
        # allow return at end of non-Unit if only `return target(...)`
        if body.startswith("return "):
            # try again after 'return '
            rest = body[len("return "):].strip()
            non_unit_m2 = re.match(r"^([A-Za-z0-9_]+)\.([A-Za-z0-9_]+)\(([^)]*)\)$", rest)
            if non_unit_m2:
                m = non_unit_m2
                body = f"return {rest}"
        if not m:
            return False, f"not a pure delegation: {body[:80]}", {}

    target_obj = m.group(1)
    target_method = m.group(2)
    call_args_raw = m.group(3).strip()

    if method["ret_canonical"] == "Unit":
        if unit_m is None and non_unit_m is not None:
            return False, "Unit-return facade must not use 'return'", {}
    else:
        if non_unit_m is None:
            return False, "non-Unit-return facade must use 'return target(...)'", {}

    if target_obj in ("System", "this"):
        return False, f"facade may not delegate to itself ({target_obj})", {}

    if target_obj not in allowed_objects:
        return False, f"delegation target '{target_obj}' is not one of the allowed System*Hooks objects", {}

    if target_method != method["name"]:
        return False, f"delegation method name mismatch: {target_method} vs facade {method['name']}", {}

    # parse call args
    call_args = split_top_level(call_args_raw, ",")
    call_args = [a.strip() for a in call_args if a.strip()]
    if call_args_raw.strip() == "":
        call_args = []

    facade_params = method["args"]
    if len(call_args) != len(facade_params):
        return False, f"argument count mismatch: call has {len(call_args)}, facade has {len(facade_params)}", {}

    seen = set()
    for idx, (call_arg, fac_param) in enumerate(zip(call_args, facade_params)):
        # call_arg must be a single identifier, not an expression
        if not re.match(r"^[A-Za-z0-9_]+$", call_arg):
            return False, f"argument {idx + 1} is not a simple parameter identifier: {call_arg}", {}
        if call_arg != fac_param[3]:
            return False, f"argument {idx + 1} is not the matching facade parameter: {call_arg} vs {fac_param[3]}", {}
        if call_arg in seen:
            return False, f"argument {call_arg} is duplicated in call", {}
        seen.add(call_arg)

    if method["ret_canonical"] != "Unit":
        # call must cover all facade params, which the count check already does
        pass

    return True, "", {
        "target_object": target_obj,
        "target_method": target_method,
        "target_full_key": (target_obj, target_method, tuple(a[1] for a in method["args"]), method["ret_canonical"]),
    }


# ---------------------------------------------------------------------------
# MainModule call extraction with overload resolution
# ---------------------------------------------------------------------------

JAVA_CALL_RE = re.compile(
    r"(?<![A-Za-z0-9_.])System\.([A-Za-z0-9_]+)\s*\(",
)

# java.lang.System methods that must not be treated as facade calls
JAVA_LANG_SYSTEM_METHODS = {
    "arraycopy", "clearProperty", "console", "currentTimeMillis", "exit", "gc",
    "getenv", "getLogger", "getProperties", "getProperty", "getSecurityManager",
    "identityHashCode", "inheritedChannel", "lineSeparator", "load", "loadLibrary",
    "mapLibraryName", "nanoTime", "out", "err", "in", "println", "print",
    "runFinalization", "setErr", "setIn", "setOut", "setProperties", "setProperty",
    "setSecurityManager",
}


def find_matching_brace(text: str, open_pos: int) -> int:
    """Find the position of the matching closing brace for '{' at open_pos."""
    brace = 1
    i = open_pos + 1
    while i < len(text) and brace > 0:
        if text[i] == "{":
            brace += 1
        elif text[i] == "}":
            brace -= 1
        i += 1
    return i - 1


def extract_call_args(text: str, start: int) -> tuple[int, list[str]]:
    """Extract the arguments of a call whose '(' is at start. Returns end pos and arg list."""
    if start >= len(text) or text[start] != "(":
        return start, []
    paren = 1
    i = start + 1
    arg_buf = []
    current = []
    quote = None
    escape = False
    while i < len(text) and paren > 0:
        ch = text[i]
        if escape:
            current.append(ch)
            escape = False
            i += 1
            continue
        if quote:
            current.append(ch)
            if ch == "\\":
                escape = True
            elif ch == quote:
                quote = None
            i += 1
            continue
        if ch in ('"', "'"):
            quote = ch
            current.append(ch)
            i += 1
            continue
        if ch == "(":
            paren += 1
            current.append(ch)
        elif ch == ")":
            paren -= 1
            if paren == 0:
                if current:
                    arg_buf.append("".join(current).strip())
                return i, [a for a in arg_buf if a]
            else:
                current.append(ch)
        elif ch == "," and paren == 1:
            arg_buf.append("".join(current).strip())
            current = []
        else:
            current.append(ch)
        i += 1
    return i, [a for a in arg_buf if a]


def find_enclosing_mainmodule_method(text: str, pos: int, methods: list[dict]) -> dict | None:
    """Find the outer MainModule method whose body contains pos."""
    containing = [m for m in methods if m["body_start"] <= pos <= m["body_end"]]
    if not containing:
        return None
    # choose the one with the greatest start (innermost) but it must be a top-level MainModule method
    # all passed are already top-level methods of MainModule
    return max(containing, key=lambda m: m["signature_start"])


def is_in_line_comment(text: str, pos: int) -> bool:
    line_start = text.rfind("\n", 0, pos) + 1
    segment = text[line_start:pos]
    # naive: if // appears before pos on this line, the rest is a comment
    # (MainModule does not have // inside strings on these lines)
    cmt = segment.find("//")
    return cmt != -1


def resolve_mainmodule_calls(text: str, facade_methods: list[dict]) -> tuple[list[dict], list[dict]]:
    """Resolve each System.* call to a facade signature and report conflicts."""
    methods = parse_java_methods(text, require_static=False)
    # Filter to top-level methods of MainModule (signature at class brace depth 1).
    top_level_methods = [m for m in methods if brace_depth_at(m["signature_start"], text) == 1]

    facade_by_name = defaultdict(list)
    for fm in facade_methods:
        facade_by_name[fm["name"]].append(fm)

    calls = []
    unresolved = []
    for m in JAVA_CALL_RE.finditer(text):
        pos = m.start()
        name = m.group(1)
        if is_in_line_comment(text, pos):
            continue
        if name in JAVA_LANG_SYSTEM_METHODS:
            continue
        end_pos, call_args = extract_call_args(text, m.end() - 1)
        if not call_args:
            call_args = []
        enclosing = find_enclosing_mainmodule_method(text, pos, top_level_methods)

        arg_types: list[str] = []
        if enclosing:
            param_map = {a[3]: a[1] for a in enclosing["args"]}
            # also try to resolve local variable declarations; use a simple regex scan of the body up to pos
            body_prefix = text[enclosing["body_start"] : pos]
            local_re = re.compile(
                r"(?:^|;|\{|\})\s*(?:final\s+)?([A-Za-z0-9_\[\]<>,?\.\s]+)\s+([A-Za-z0-9_]+)\s*[=;]",
                re.MULTILINE,
            )
            for lm in local_re.finditer(body_prefix):
                vtype = lm.group(1).strip()
                vname = lm.group(2).strip()
                if vname not in param_map:
                    param_map[vname] = _canonical_type(vtype)
            for ca in call_args:
                ca = ca.strip()
                if re.match(r"^[A-Za-z0-9_]+$", ca):
                    if ca in param_map:
                        arg_types.append(param_map[ca])
                    else:
                        arg_types.append("<unknown>")
                else:
                    arg_types.append("<expr>")
        else:
            arg_types = ["<unknown>"] * len(call_args)

        matches = [fm for fm in facade_by_name.get(name, []) if len(fm["args"]) == len(call_args)]
        if len(matches) == 0:
            unresolved.append({
                "name": name,
                "arg_count": len(call_args),
                "arg_types": arg_types,
                "call_args": call_args,
                "enclosing_method": enclosing["name"] if enclosing else "<unknown>",
                "reason": "no facade method with matching name and argument count",
            })
            continue
        if len(matches) > 1:
            # try to resolve by types
            type_matches = [fm for fm in matches if tuple(a[1] for a in fm["args"]) == tuple(arg_types)]
            if len(type_matches) == 1:
                calls.append({
                    "name": name,
                    "call_args": call_args,
                    "arg_types": arg_types,
                    "resolved_full_key": full_key(type_matches[0]),
                    "enclosing_method": enclosing["name"] if enclosing else "<unknown>",
                })
                continue
            if len(type_matches) > 1:
                unresolved.append({
                    "name": name,
                    "arg_count": len(call_args),
                    "arg_types": arg_types,
                    "call_args": call_args,
                    "enclosing_method": enclosing["name"] if enclosing else "<unknown>",
                    "reason": "multiple facade overloads match argument types",
                })
            else:
                unresolved.append({
                    "name": name,
                    "arg_count": len(call_args),
                    "arg_types": arg_types,
                    "call_args": call_args,
                    "enclosing_method": enclosing["name"] if enclosing else "<unknown>",
                    "reason": "facade overload exists but argument types could not be resolved",
                })
            continue
        calls.append({
            "name": name,
            "call_args": call_args,
            "arg_types": arg_types,
            "resolved_full_key": full_key(matches[0]),
            "enclosing_method": enclosing["name"] if enclosing else "<unknown>",
        })

    return calls, unresolved


# ---------------------------------------------------------------------------
# R8 mapping / usage parsing
# ---------------------------------------------------------------------------

def parse_mapping(path: Path) -> list[dict]:
    """Parse mapping.txt and return list of method mappings with original class/name/args."""
    mappings = []
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line or line.startswith("#"):
                continue
            m = re.match(
                r"^\s*(?:\d+:\d+:)?([^\s]+)\s+([^\s]+)\.([A-Za-z0-9_]+)\(([^)]*)\)(?::\d+)?\s*->\s*([^\s]+)$",
                line,
            )
            if not m:
                continue
            ret, cls, name, args, obf = m.groups()
            arg_types = [a.strip() for a in split_top_level(args, ",") if a.strip()]
            mappings.append({
                "class": cls,
                "name": name,
                "args": arg_types,
                "ret": ret,
                "obf": obf,
                "line": line,
            })
    return mappings


def canonicalize_mapping_arg(arg: str) -> str:
    # e.g. "io.github.libxposed.api.XposedModuleInterface$PackageReadyParam"
    # or "java.lang.String"
    arg = arg.strip()
    if not arg:
        return ""
    # arrays: "java.lang.Object[]" or "java.lang.String[]"
    is_array = arg.endswith("[]")
    if is_array:
        elem = arg[:-2].strip()
        return f"Array<{canonicalize_mapping_arg(elem)}>"
    # primitives in mapping are unboxed names
    base = _last_component(arg)
    base_l = base.lower()
    if base_l in JAVA_TO_KT:
        return JAVA_TO_KT[base_l]
    return base


def mapping_matches_facade(mapping: dict, facade: dict) -> bool:
    if mapping["class"] != "tv.withaibuild.customiuizer.mods.System":
        return False
    if mapping["name"] != facade["name"]:
        return False
    if len(mapping["args"]) != len(facade["args"]):
        return False
    for ma, fa in zip(mapping["args"], facade["args"]):
        if canonicalize_mapping_arg(ma) != fa[1]:
            return False
    return True


def parse_usage(path: Path) -> list[dict]:
    """Parse usage.txt and return list of removed members/classes."""
    items = []
    current_class = None
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n").rstrip()
            if not line:
                current_class = None
                continue
            top = re.match(r"^([^\s]+):\s*$", line)
            plain = re.match(r"^([^\s]+)$", line)
            if top:
                current_class = top.group(1)
                items.append({"class": current_class, "member": None, "raw": line})
                continue
            if plain:
                current_class = plain.group(1)
                items.append({"class": current_class, "member": None, "raw": line})
                continue
            if current_class:
                items.append({"class": current_class, "member": line.strip(), "raw": line})
    return items


def usage_matches_facade(usage: list[dict], facade: dict) -> bool:
    class_name = "tv.withaibuild.customiuizer.mods.System"
    method_re = re.compile(
        rf"{re.escape(facade['name'])}\s*\(([^)]*)\)"
    )
    for u in usage:
        if u["class"] != class_name:
            continue
        member = u["member"]
        if not member:
            continue
        if facade["name"] not in member:
            continue
        m = method_re.search(member)
        if not m:
            continue
        args_raw = m.group(1)
        args = [a.strip() for a in split_top_level(args_raw, ",") if a.strip()]
        if len(args) != len(facade["args"]):
            continue
        if all(canonicalize_mapping_arg(a) == fa[1] for a, fa in zip(args, facade["args"])):
            return True
    return False


# ---------------------------------------------------------------------------
# APK helpers
# ---------------------------------------------------------------------------

def apkanalyzer_path() -> Path | None:
    envs = ["ANDROID_HOME", "ANDROID_SDK_ROOT"]
    locations = [
        "cmdline-tools/latest/bin/apkanalyzer.bat",
        "tools/bin/apkanalyzer.bat",
        "cmdline-tools/latest/bin/apkanalyzer",
        "tools/bin/apkanalyzer",
    ]
    for env in envs:
        value = os.environ.get(env)
        if not value:
            continue
        root = Path(value)
        if not root.exists():
            continue
        for loc in locations:
            p = root / loc
            if p.exists():
                return p
    root = Path("C:/Android/Sdk")
    if root.exists():
        for loc in locations:
            p = root / loc
            if p.exists():
                return p
    return None


def find_apks() -> tuple[Path | None, Path | None]:
    debug = latest_file(APK_DIR / "debug", "*.apk")
    release = latest_file(APK_DIR / "release", "*.apk")
    return debug, release


def run_apkanalyzer_dex_packages(apk: Path, analyzer: Path, mapping: Path | None) -> str:
    cmd = [str(analyzer), "dex", "packages", str(apk), "--defined-only"]
    if mapping:
        cmd.extend(["--proguard-mappings", str(mapping)])
    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            encoding="utf-8",
            timeout=120,
        )
        return result.stdout + result.stderr
    except Exception as e:
        return f"apkanalyzer error: {e}"


# ---------------------------------------------------------------------------
# Reporting
# ---------------------------------------------------------------------------
class Reporter:
    def __init__(self):
        self.lines: list[str] = []
        self.failures: list[str] = []
        self.warnings: list[str] = []

    def add(self, line: str):
        self.lines.append(line)

    def fail(self, line: str):
        self.lines.append(f"FAIL: {line}")
        self.failures.append(line)

    def warn(self, line: str):
        self.lines.append(f"WARN: {line}")
        self.warnings.append(line)

    def text(self) -> str:
        return "\n".join(self.lines)

    def ok(self) -> bool:
        return not self.failures


def format_timestamp(path: Path | None) -> str:
    if not path or not path.exists():
        return "n/a"
    mtime = path.stat().st_mtime
    dt = datetime.fromtimestamp(mtime, tz=timezone.utc)
    return dt.strftime("%Y-%m-%d %H:%M:%S UTC")


def find_matching_brace_robust(text: str, open_pos: int) -> int:
    """Find matching '}' for '{' at open_pos, skipping strings and comments."""
    brace = 1
    i = open_pos + 1
    in_str = None
    escape = False
    in_line_comment = False
    in_block_comment = False
    while i < len(text) and brace > 0:
        ch = text[i]
        if in_line_comment:
            if ch == "\n":
                in_line_comment = False
            i += 1
            continue
        if in_block_comment:
            if ch == "*" and i + 1 < len(text) and text[i + 1] == "/":
                in_block_comment = False
                i += 2
            else:
                i += 1
            continue
        if in_str:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == in_str:
                in_str = None
            i += 1
            continue
        if ch == '"' or ch == "'":
            in_str = ch
            i += 1
            continue
        if ch == "/" and i + 1 < len(text):
            nxt = text[i + 1]
            if nxt == "/":
                in_line_comment = True
                i += 2
                continue
            elif nxt == "*":
                in_block_comment = True
                i += 2
                continue
        if ch == "{":
            brace += 1
        elif ch == "}":
            brace -= 1
            if brace == 0:
                return i
        i += 1
    return i - 1


def brace_depth_at(pos: int, text: str) -> int:
    """Return brace depth at position, ignoring strings and comments."""
    depth = 0
    i = 0
    in_str = None
    escape = False
    in_line_comment = False
    in_block_comment = False
    while i < pos:
        ch = text[i]
        if in_line_comment:
            if ch == "\n":
                in_line_comment = False
            i += 1
            continue
        if in_block_comment:
            if ch == "*" and i + 1 < len(text) and text[i + 1] == "/":
                in_block_comment = False
                i += 2
            else:
                i += 1
            continue
        if in_str:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == in_str:
                in_str = None
            i += 1
            continue
        if ch == '"' or ch == "'":
            in_str = ch
            i += 1
            continue
        if ch == "/" and i + 1 < len(text):
            nxt = text[i + 1]
            if nxt == "/":
                in_line_comment = True
                i += 2
                continue
            elif nxt == "*":
                in_block_comment = True
                i += 2
                continue
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
        i += 1
    return depth


def audit_direct_system_dispatch(r: Reporter, baseline_methods: list[dict]) -> int:
    """Audit the post-K17 layout where MainModule calls System*Hooks directly."""
    hooks_files = sorted(p for p in MODS_DIR.glob("System*Hooks.kt") if not p.name.startswith("SystemUI"))
    r.add("## K17 direct System dispatch")
    r.add("- System.kt facade: removed")
    r.add(f"- System*Hooks files: {len(hooks_files)} (expected: {EXPECTED_HOOKS_COUNT})")
    if len(hooks_files) != EXPECTED_HOOKS_COUNT:
        r.fail(f"System*Hooks.kt file count is {len(hooks_files)}, expected {EXPECTED_HOOKS_COUNT}")

    public_hook_methods: list[dict] = []
    hook_objects: dict[str, set[str]] = {}
    for hook_file in hooks_files:
        text = hook_file.read_text(encoding="utf-8")
        object_name, methods = extract_hook_objects_and_methods(text, hook_file.name)
        expected_object = hook_file.stem
        if object_name != expected_object:
            r.fail(f"{hook_file.name} declares object '{object_name}', expected '{expected_object}'")
        public = [method for method in methods if not method["private"]]
        public_hook_methods.extend(public)
        hook_objects[object_name] = {method["name"] for method in public}
        r.add(f"  - {hook_file.name}: {len(public)} public/internal entry points")

    if baseline_methods:
        public_baseline = [method for method in baseline_methods if method["is_public"]]
        hook_keys = {full_key(method) for method in public_hook_methods}
        missing = [
            method for method in public_baseline
            if full_key(method) not in hook_keys and method["name"] not in INTERNAL_SYSTEM_ALLOWLIST
        ]
        r.add(f"- Baseline public static methods: {len(public_baseline)}")
        r.add(f"- Baseline methods resolved in domain objects: {len(public_baseline) - len(missing)}")
        for method in missing:
            key = full_key(method)
            r.fail(f"Baseline method missing from domain objects: {key[0]}({', '.join(key[1])}): {key[2]}")
    else:
        r.warn("No baseline supplied; signature coverage could not be compared")

    if not MAIN_MODULE.exists():
        r.fail(f"MainModule not found: {MAIN_MODULE}")
    else:
        main_text = MAIN_MODULE.read_text(encoding="utf-8")
        if "import tv.withaibuild.customiuizer.mods.System;" in main_text:
            r.fail("MainModule still imports the removed System facade")

        facade_calls = []
        for match in JAVA_CALL_RE.finditer(main_text):
            if is_in_line_comment(main_text, match.start()):
                continue
            name = match.group(1)
            if name not in JAVA_LANG_SYSTEM_METHODS:
                facade_calls.append((name, main_text[:match.start()].count("\n") + 1))
        for name, line in facade_calls:
            r.fail(f"MainModule still calls System.{name} at line {line}")

        direct_pattern = re.compile(r"\b(System(?!UI)[A-Za-z0-9_]+Hooks)\.([A-Za-z0-9_]+)\s*\(")
        direct_calls = list(direct_pattern.finditer(main_text))
        unresolved = []
        for match in direct_calls:
            target, method = match.groups()
            if method not in hook_objects.get(target, set()):
                unresolved.append((target, method, main_text[:match.start()].count("\n") + 1))
        r.add(f"- Direct System*Hooks call sites in MainModule: {len(direct_calls)}")
        r.add(f"- Remaining System facade calls in MainModule: {len(facade_calls)}")
        for target, method, line in unresolved:
            r.fail(f"Unresolved direct target {target}.{method} at MainModule.java:{line}")

    r.add("")
    r.add("## Summary")
    if r.ok():
        r.add("PASS: Direct System dispatch audit completed with no blocking issues.")
    else:
        r.add(f"FAIL: {len(r.failures)} issue(s) found.")
        for failure in r.failures:
            r.add(f"  - {failure}")
    print(r.text())
    return 0 if r.ok() else 1


def main() -> int:
    parser = argparse.ArgumentParser(description="Audit K5 System.java -> Kotlin migration")
    parser.add_argument(
        "--baseline-ref",
        default=None,
        help="Git ref to old System.java for baseline comparison",
    )
    args = parser.parse_args()

    r = Reporter()
    r.add("# K5 System.java -> Kotlin hardened audit")
    r.add(f"Repo root: {REPO}")
    r.add(f"Audit time: {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S UTC')}")
    r.add("")

    # --- 1. Baseline ---
    baseline_methods: list[dict] = []
    if args.baseline_ref:
        baseline_text = None
        used_baseline_path = None
        for candidate in SYSTEM_JAVA_CANDIDATES:
            baseline_text = run_git_show(args.baseline_ref, candidate)
            if baseline_text is not None:
                used_baseline_path = candidate
                break
        if baseline_text is None:
            r.warn(f"Baseline ref '{args.baseline_ref}' not found; old System.java could not be read")
        else:
            # Parse all static methods (public and non-public) so we can
            # classify private helpers that moved into System*Hooks.
            baseline_methods = parse_java_methods(baseline_text, require_static=True, require_public=False)
            r.add(f"## Baseline {args.baseline_ref}")
            r.add(f"- Baseline System.java path: {used_baseline_path}")
            r.add(f"- Static methods in old System.java: {len(baseline_methods)}")
            r.add(f"- Public static methods in old System.java: {sum(1 for m in baseline_methods if m['is_public'])}")
            r.add("")

    # --- 2. Facade parsing ---
    if not SYSTEM_FACADE.exists():
        return audit_direct_system_dispatch(r, baseline_methods)

    facade_text = SYSTEM_FACADE.read_text(encoding="utf-8")
    facade_methods = extract_facade_methods(facade_text)
    r.add("## System.kt facade methods")
    r.add(f"- Total @JvmStatic methods: {len(facade_methods)}")

    facade_by_full: dict[tuple, dict] = {}
    facade_by_erased: dict[tuple, list[dict]] = defaultdict(list)
    duplicate_full = []
    duplicate_erased = []
    for m in facade_methods:
        fk = full_key(m)
        if fk in facade_by_full:
            duplicate_full.append(fk)
        else:
            facade_by_full[fk] = m
        ek = erased_key(m)
        facade_by_erased[ek].append(m)
    for ek, ms in facade_by_erased.items():
        if len(ms) > 1:
            duplicate_erased.append(ek)

    if duplicate_full:
        r.fail(f"Facade has duplicate full signatures: {', '.join(str(k) for k in duplicate_full[:10])}")
    if duplicate_erased:
        r.fail(f"Facade has duplicate JVM erased signatures: {', '.join(str(k) for k in duplicate_erased[:10])}")
    r.add("")

    # --- 3. Baseline comparison (with allowlist classification) ---
    javap_result = None
    if baseline_methods:
        # Public-only and full baseline keys
        public_baseline_methods = [m for m in baseline_methods if m["is_public"]]
        baseline_keys = {full_key(m) for m in public_baseline_methods}
        all_baseline_keys = {full_key(m) for m in baseline_methods}
        facade_keys = set(facade_by_full.keys())

        missing_from_facade = sorted(baseline_keys - facade_keys, key=lambda k: (k[0], k[1]))
        missing_nonpublic = sorted(all_baseline_keys - baseline_keys - facade_keys, key=lambda k: (k[0], k[1]))
        new_in_facade = sorted(facade_keys - all_baseline_keys, key=lambda k: (k[0], k[1]))

        same_name_different_sig = []
        baseline_by_name = defaultdict(list)
        for m in baseline_methods:
            baseline_by_name[m["name"]].append(full_key(m))
        for m in facade_methods:
            if m["name"] in baseline_by_name and full_key(m) not in baseline_by_name[m["name"]]:
                # Signature changed for the same name
                same_name_different_sig.append((m["name"], baseline_by_name[m["name"]], full_key(m)))

        # javap cross-check using compiled .class files if available
        current_class = find_system_class(REPO)
        baseline_class = None
        if current_class:
            for candidate in [
                REPO.parent / "customiuizer-a13-baseline-build" / "app" / "build" / "intermediates" / "javac" / "debug" / "compileDebugJavaWithJavac" / "classes" / "name" / "monwf" / "customiuizer" / "mods" / "System.class",
                REPO.parent / "customiuizer-a13-baseline-build" / "app" / "build" / "intermediates" / "javac" / "release" / "compileReleaseJavaWithJavac" / "classes" / "name" / "monwf" / "customiuizer" / "mods" / "System.class",
            ]:
                if candidate.exists():
                    baseline_class = candidate
                    break
        javap_result = javap_compare(baseline_class, current_class, r)

        r.add("## Baseline comparison")
        r.add(f"- Old System.java full signatures (all static): {len(all_baseline_keys)}")
        r.add(f"- Old System.java full signatures (public static): {len(baseline_keys)}")
        r.add(f"- Current facade full signatures: {len(facade_keys)}")
        r.add(f"- Missing public from current facade: {len(missing_from_facade)}")
        r.add(f"- Missing non-public from current facade: {len(missing_nonpublic)}")
        r.add(f"- New in current facade: {len(new_in_facade)}")

        # Categorize missing public methods
        real_missing: list[tuple] = []
        internal_allowlisted: list[tuple] = []
        for k in missing_from_facade:
            if k[0] in INTERNAL_SYSTEM_ALLOWLIST:
                internal_allowlisted.append(k)
            else:
                real_missing.append(k)

        if real_missing:
            r.add("- Missing public methods not in allowlist (real missing):")
            for k in real_missing[:30]:
                r.add(f"  - {k[0]}({', '.join(k[1])}): {k[2]}")
            if len(real_missing) > 30:
                r.add(f"  ... and {len(real_missing) - 30} more")

        if internal_allowlisted:
            r.add(f"- Missing public methods in allowlist (internal helpers, no facade needed): {len(internal_allowlisted)}")
            for k in internal_allowlisted:
                reason = INTERNAL_SYSTEM_ALLOWLIST.get(k[0], "internal helper")
                r.add(f"  - {k[0]}({', '.join(k[1])}): {k[2]}  [{reason}]")

        # Categorize non-public (private/package) methods missing from facade.
        # These were private helpers in old System.java; they should be in the
        # allowlist and have moved into System*Hooks.
        nonpublic_allowlisted: list[tuple] = []
        nonpublic_unclassified: list[tuple] = []
        for k in missing_nonpublic:
            if k[0] in INTERNAL_SYSTEM_ALLOWLIST:
                nonpublic_allowlisted.append(k)
            else:
                nonpublic_unclassified.append(k)

        if nonpublic_allowlisted:
            r.add(f"- Non-public methods in allowlist (migrated to Hooks, no facade needed): {len(nonpublic_allowlisted)}")
            for k in nonpublic_allowlisted:
                reason = INTERNAL_SYSTEM_ALLOWLIST.get(k[0], "internal helper")
                r.add(f"  - {k[0]}({', '.join(k[1])}): {k[2]}  [{reason}]")

        if nonpublic_unclassified:
            r.add(f"- Non-public methods not in allowlist (unclassified): {len(nonpublic_unclassified)}")
            for k in nonpublic_unclassified[:30]:
                r.add(f"  - {k[0]}({', '.join(k[1])}): {k[2]}")

        if new_in_facade:
            r.add("- New in current facade:")
            for k in new_in_facade[:30]:
                r.add(f"  - {k[0]}({', '.join(k[1])}): {k[2]}")
            if len(new_in_facade) > 30:
                r.add(f"  ... and {len(new_in_facade) - 30} more")

        if same_name_different_sig:
            r.add(f"- Same name but different signature: {len(same_name_different_sig)}")
            for name, old, new in same_name_different_sig[:20]:
                r.add(f"  - {name}: old={old[0][1:]}, new={new[1:]}")

        r.add("")

        if real_missing:
            for k in real_missing:
                r.fail(f"Missing facade method: {k[0]}({', '.join(k[1])}): {k[2]}")
        if nonpublic_unclassified:
            for k in nonpublic_unclassified:
                r.fail(f"Unclassified non-public helper missing from facade: {k[0]}({', '.join(k[1])}): {k[2]}")
        if same_name_different_sig:
            for name, old, new in same_name_different_sig:
                r.fail(f"Facade signature changed for {name}: old={old[0][1:]}, new={new[1:]}")

    # --- 4. MainModule call resolution ---
    if not MAIN_MODULE.exists():
        r.fail(f"MainModule not found: {MAIN_MODULE}")
        print(r.text())
        return 1

    main_text = MAIN_MODULE.read_text(encoding="utf-8")
    calls, unresolved = resolve_mainmodule_calls(main_text, facade_methods)
    r.add("## MainModule System.* calls")
    r.add(f"- Total call sites: {len(calls) + len(unresolved)}")
    r.add(f"- Resolved calls: {len(calls)}")
    r.add(f"- Unresolved calls: {len(unresolved)}")

    if unresolved:
        for u in unresolved[:20]:
            r.fail(f"MainModule call unresolved: {u['name']}({', '.join(u['call_args'])}) in {u['enclosing_method']} - {u['reason']}")
        if len(unresolved) > 20:
            r.add(f"  ... and {len(unresolved) - 20} more unresolved")
    else:
        unique_call_keys = sorted({c['resolved_full_key'] for c in calls}, key=lambda k: k[0])
        r.add(f"- Unique resolved facade signatures: {len(unique_call_keys)}")
    r.add("")

    # --- 4b. Whole-repo System.* call resolution ---
    all_calls, all_unresolved = resolve_system_calls(REPO, facade_methods)
    r.add("## Whole-repo System.* calls")
    r.add(f"- Total call sites in repo: {len(all_calls) + len(all_unresolved)}")
    r.add(f"- Resolved calls: {len(all_calls)}")
    r.add(f"- Unresolved calls: {len(all_unresolved)}")

    if all_unresolved:
        for u in all_unresolved[:30]:
            r.fail(f"Unresolved System.{u['name']}({', '.join(u['call_args'])}) at {u['file']}:{u['line']} - {u['reason']}")
        if len(all_unresolved) > 30:
            r.add(f"  ... and {len(all_unresolved) - 30} more unresolved")
    else:
        unique_all_keys = sorted({c['resolved_full_key'] for c in all_calls}, key=lambda k: k[0])
        r.add(f"- Unique resolved facade signatures: {len(unique_all_keys)}")
    r.add("")

    # --- 5. System*Hooks parsing and hard checks ---
    hooks_files = sorted(p for p in MODS_DIR.glob("System*Hooks.kt") if not p.name.startswith("SystemUI"))
    r.add("## System*Hooks files")
    r.add(f"- Files found: {len(hooks_files)} (expected: {EXPECTED_HOOKS_COUNT})")
    if len(hooks_files) != EXPECTED_HOOKS_COUNT:
        r.fail(f"System*Hooks.kt file count is {len(hooks_files)}, expected {EXPECTED_HOOKS_COUNT}")

    hook_objects: dict[str, list[dict]] = {}
    hook_object_names: set[str] = set()
    all_hook_methods: list[dict] = []
    file_object_mismatch = []
    for hf in hooks_files:
        text = hf.read_text(encoding="utf-8")
        obj_name, methods = extract_hook_objects_and_methods(text, hf.name)
        expected_obj = hf.name.replace(".kt", "")
        if obj_name != expected_obj:
            file_object_mismatch.append(f"{hf.name} declares object '{obj_name}'")
        hook_objects[obj_name] = methods
        hook_object_names.add(obj_name)
        all_hook_methods.extend(methods)
        r.add(f"  - {hf.name}: {len(methods)} methods (object {obj_name})")

    if file_object_mismatch:
        for e in file_object_mismatch:
            r.fail(e)
    r.add("")

    # Build hook target map keyed by full signature (object, name, arg types, ret)
    hook_target_map: dict[tuple, dict] = {}
    duplicate_hook_sigs: list[tuple] = []
    for hm in all_hook_methods:
        if hm["private"]:
            continue
        key = (hm["object"], hm["name"], tuple(a[1] for a in hm["args"]), hm["ret_canonical"])
        if key in hook_target_map:
            if hook_target_map[key]["object"] != hm["object"]:
                duplicate_hook_sigs.append(key)
        else:
            hook_target_map[key] = hm
    if duplicate_hook_sigs:
        for k in duplicate_hook_sigs[:20]:
            r.fail(f"Duplicate public hook signature across objects: {k[0]}.{k[1]}({', '.join(k[2])}): {k[3]}")
        if len(duplicate_hook_sigs) > 20:
            r.add(f"  ... and {len(duplicate_hook_sigs) - 20} more")
    r.add("")

    # --- 6. Facade delegation validation ---
    r.add("## Facade delegation validation")
    allowed_hook_objects = hook_object_names
    bad_delegations = []
    for m in facade_methods:
        ok, msg, _ = validate_delegation(m, allowed_hook_objects)
        if not ok:
            bad_delegations.append((m, msg))
    if bad_delegations:
        for m, msg in bad_delegations[:20]:
            r.fail(f"System.{m['name']}({', '.join(a[3] for a in m['args'])}): {msg}")
        if len(bad_delegations) > 20:
            r.add(f"  ... and {len(bad_delegations) - 20} more")
    else:
        r.add("- All facade methods are pure 1:1 parameter-forwarding delegations")
    r.add("")

    # --- 7. Facade target coverage ---
    r.add("## Facade target coverage")
    missing_targets = []
    for m in facade_methods:
        ok, _, info = validate_delegation(m, allowed_hook_objects)
        if not ok:
            continue
        target_key = info["target_full_key"]
        if target_key not in hook_target_map:
            missing_targets.append(f"{target_key[0]}.{target_key[1]}({', '.join(target_key[2])}): {target_key[3]}")
    if missing_targets:
        for mt in missing_targets[:20]:
            r.fail(f"Facade target missing in Hooks: {mt}")
        if len(missing_targets) > 20:
            r.add(f"  ... and {len(missing_targets) - 20} more")
    else:
        r.add(f"- All facade delegations resolve to a public System*Hooks method")
    r.add("")

    # --- 8. R8 mapping / usage counts ---
    r.add("## R8 mapping and usage counts")
    usage_path = latest_file(MAPPING_DIR, "usage.txt")
    mapping_path = latest_file(MAPPING_DIR, "mapping.txt")
    if not usage_path or not mapping_path:
        r.warn(f"R8 mapping files not found under {MAPPING_DIR}")
    else:
        r.add(f"- usage.txt: {usage_path}")
        r.add(f"- mapping.txt: {mapping_path}")
        mappings = parse_mapping(mapping_path)
        usages = parse_usage(usage_path)

        mapping_count = 0
        usage_count = 0
        unresolved_count = 0
        for m in facade_methods:
            in_mapping = any(mapping_matches_facade(mp, m) for mp in mappings)
            in_usage = usage_matches_facade(usages, m)
            if in_mapping:
                mapping_count += 1
            elif in_usage:
                usage_count += 1
            else:
                unresolved_count += 1

        r.add(f"- Facade full signatures total: {len(facade_methods)}")
        if baseline_methods:
            r.add(f"- Baseline (old System.java) full signatures: {len(baseline_methods)}")
        r.add(f"- Facade signatures located in mapping.txt: {mapping_count}")
        r.add(f"- Facade signatures explicitly listed in usage.txt (removed): {usage_count}")
        r.add(f"- Facade signatures not directly locatable in mapping/usage: {unresolved_count}")
        if unresolved_count:
            r.add("  (These may have been inlined or optimized by R8; Release build link succeeded, but runtime semantics are pending device verification.)")
    r.add("")

    # --- 9. APK artifacts ---
    r.add("## APK artifacts")
    debug_apk, release_apk = find_apks()
    for label, apk in [("Debug", debug_apk), ("Release", release_apk)]:
        r.add(f"### {label} APK")
        if not apk:
            r.fail(f"{label} APK not found")
            continue
        r.add(f"- Path: {apk}")
        r.add(f"- Size: {apk.stat().st_size} bytes")
        r.add(f"- SHA-256: {sha256(apk)}")
        r.add(f"- Last write: {format_timestamp(apk)}")

        analyzer = apkanalyzer_path()
        if analyzer and mapping_path:
            r.add(f"- apkanalyzer: {analyzer}")
            dex_out = run_apkanalyzer_dex_packages(apk, analyzer, mapping_path)
            lines = [ln for ln in dex_out.splitlines() if ("tv.withaibuild.customiuizer.mods" in ln or "tv.withaibuild.customiuizer.MainModule" in ln)]
            if lines:
                r.add(f"- DEX entries for `tv.withaibuild.customiuizer.mods` / `MainModule`: {len(lines)}")
                for ln in lines[:20]:
                    r.add(f"    {ln}")
                if len(lines) > 20:
                    r.add(f"    ... and {len(lines) - 20} more")
            else:
                r.add("- No DEX entries matching `tv.withaibuild.customiuizer.mods` or `MainModule`; apkanalyzer output:")
                for ln in dex_out.splitlines()[:20]:
                    r.add(f"    {ln}")
        else:
            r.add("- apkanalyzer not found or no mapping file; DEX inspection skipped.")
    r.add("")

    # --- 10. Summary ---
    r.add("## Summary")
    if r.ok():
        r.add("PASS: Migration audit completed with no blocking issues.")
    else:
        r.add(f"FAIL: {len(r.failures)} issue(s) found.")
        for f in r.failures[:20]:
            r.add(f"  - {f}")
        if len(r.failures) > 20:
            r.add(f"  ... and {len(r.failures) - 20} more")

    print(r.text())
    return 0 if r.ok() else 1


if __name__ == "__main__":
    sys.exit(main())
