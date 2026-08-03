#!/usr/bin/env python3
"""Compare production hook calls in legacy hook functions to their CatalogContracts definitions.

Covers batches 9/10/11/12 by default. Detects MISSING_CONTRACT_TARGET, ORPHAN_CONTRACT_TARGET,
PARAMETER_TYPES_MISMATCH, UNRESOLVED_PARAMETER_TYPES, DUPLICATE_CONTRACT_TARGET, DUPLICATE_TARGET,
CRITICALITY_MISMATCH, UNPARSEABLE_HOOK_SURFACE, EMPTY_ANYOF_GROUP, DUPLICATE_ANYOF_CANDIDATE,
ANYOF_CANDIDATE_MISMATCH, ANYOF_GROUP_UNSATISFIED and UNPARSEABLE_ANYOF_CANDIDATE.
"""
from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class TargetKey:
    class_name: str
    member_name: str
    operation: str
    parameter_types: tuple[str, ...] = ()


@dataclass(frozen=True)
class ProductionTarget:
    key: TargetKey
    hard: bool
    source: str
    line: int


@dataclass(frozen=True)
class AnyOfGroup:
    group_id: str
    candidates: tuple[TargetKey, ...]
    optional: bool


class TypeResolutionError(Exception):
    """Raised when a Kotlin type expression cannot be normalized to a stable JVM name."""


BATCH_FUNCTIONS: dict[str, dict[str, tuple[str, str]]] = {
    "9": {
        "enhancedSecurity": ("tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt", "EnhancedSecurityHook"),
        "appLock": ("tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt", "AppLockHook"),
        "skipAppLock": ("tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt", "SkipAppLockHook"),
        "noCallInterruption": ("tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt", "NoCallInterruptionHook"),
    },
    "10": {
        "removeSecure": ("tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt", "RemoveSecureHook"),
        "noSignatureVerify": ("tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt", "NoSignatureVerifyServiceHook"),
        "noDarkForce": ("tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt", "NoDarkForceHook"),
        "stickyFloatingWindows": ("tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt", "StickyFloatingWindowsHook"),
    },
    "11": {
        "appsDisableService": ("tv/withaibuild/customiuizer/mods/Various.kt", "AppsDisableServiceHook"),
        "noAccessDeviceLogsRequest": ("tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt", "NoAccessDeviceLogsRequest"),
        "autoGroupNotifications": ("tv/withaibuild/customiuizer/mods/SystemNotificationAndShareHooks.kt", "AutoGroupNotificationsHook"),
        "appLockTimeout": ("tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt", "AppLockTimeoutHook"),
    },
    "12": {
        "tempHideOverlayApp": ("tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt", "TempHideOverlayAppHook"),
        "openAppInFreeForm": ("tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt", "OpenAppInFreeFormHook"),
    },
}


MODULE_HELPER_RE = re.compile(
    r"""(ModuleHelper\.findAndHookMethod(?:Silently)?|ModuleHelper\.hookAllMethods(?:Silently)?|ModuleHelper\.hookAllConstructors|ModuleHelper\.findAndHookConstructor)\s*\(""",
    re.DOTALL,
)


FUNCTION_PATTERN = re.compile(
    r"""@JvmStatic\s+fun\s+([A-Za-z0-9_]+)\s*\([^)]*\)(?:\s*:\s*[A-Za-z0-9_<>?\s]+)?\s*\{""",
    re.DOTALL,
)


CLASSLITERAL_RE = re.compile(
    r'"([^"]+)"(?:\s*,\s*(?:lpparam\.)?classLoader)?'
)


def extract_function_body(text: str, function_name: str) -> tuple[str, int] | None:
    pos = 0
    while True:
        m = FUNCTION_PATTERN.search(text, pos)
        if not m:
            return None
        name = m.group(1)
        if name != function_name:
            pos = m.end()
            continue
        start = m.start()
        open_pos = text.find("{", m.end() - 1)
        if open_pos < 0:
            return None
        depth = 0
        in_string = False
        escaped = False
        for i in range(open_pos, len(text)):
            ch = text[i]
            if in_string:
                if escaped:
                    escaped = False
                elif ch == "\\":
                    escaped = True
                elif ch == '"':
                    in_string = False
                continue
            if ch == '"':
                in_string = True
            elif ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
                if depth == 0:
                    return text[start : i + 1], text[:start].count("\n") + 1
        return None
    return None


def parse_call_args(block: str) -> list[str]:
    args: list[str] = []
    depth = 0
    in_string = False
    escaped = False
    current = ""
    for ch in block:
        if in_string:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            current += ch
            continue
        if ch == '"':
            in_string = True
            current += ch
            continue
        if ch == "(":
            depth += 1
            if depth == 1:
                continue
        elif ch == ")":
            if depth == 1:
                if current.strip():
                    args.append(current.strip())
                return args
            depth -= 1
        elif ch == "," and depth == 1:
            if current.strip():
                args.append(current.strip())
            current = ""
            continue
        current += ch
    return args


def resolve_operation(method: str) -> str:
    if method == "ModuleHelper.findAndHookMethod" or method == "ModuleHelper.findAndHookMethodSilently":
        return "EXACT_METHOD"
    if method == "ModuleHelper.findAndHookConstructor":
        return "EXACT_CONSTRUCTOR"
    if method == "ModuleHelper.hookAllMethods" or method == "ModuleHelper.hookAllMethodsSilently":
        return "ALL_METHODS_BY_NAME"
    if method == "ModuleHelper.hookAllConstructors":
        return "ALL_CONSTRUCTORS"
    return "UNKNOWN"


def is_hard(method: str) -> bool:
    return not method.endswith("Silently")


def looks_like_class(s: str) -> bool:
    return "." in s


def first_class_literal(args: list[str]) -> str | None:
    for a in args:
        a = a.strip()
        m = re.match(r'^"([^"]+)"$', a)
        if m and looks_like_class(m.group(1)):
            return unescape_string(m.group(1))
    return None


def first_method_literal(args: list[str], class_resolved_from_var: bool) -> str | None:
    found_class = class_resolved_from_var
    for a in args:
        a = a.strip()
        m = re.match(r'^"([^"]+)"$', a)
        if m:
            if not found_class:
                found_class = True
                continue
            return unescape_string(m.group(1))
    return None


def resolve_class_variables(body: str) -> dict[str, str]:
    """Map local variable names like 'SignDetails' to the string class name from XposedHelpers.findClass*."""
    mapping: dict[str, str] = {}
    pattern = re.compile(
        r"val\s+([A-Za-z0-9_]+)\s*=\s*XposedHelpers\.(?:findClass|findClassIfExists)\s*\(\s*\"([^\"]+)\"",
        re.DOTALL,
    )
    for m in pattern.finditer(body):
        mapping[m.group(1)] = unescape_string(m.group(2))
    return mapping


def extract_production_targets(source_root: Path, rel_path: str, function_name: str) -> tuple[list[ProductionTarget], list[str]]:
    path = source_root / rel_path
    text = path.read_text(encoding="utf-8")
    imports = parse_imports(text)
    extracted = extract_function_body(text, function_name)
    if not extracted:
        return [], [f"UNPARSEABLE_HOOK_SURFACE: cannot locate function {function_name} in {rel_path}"]
    body, line = extracted
    class_vars = resolve_class_variables(body)
    targets: list[ProductionTarget] = []
    errors: list[str] = []
    for m in MODULE_HELPER_RE.finditer(body):
        method = m.group(1)
        call_start = m.end() - 1
        args = parse_call_args(body[call_start:])
        if not args:
            errors.append(f"UNPARSEABLE_HOOK_SURFACE: {rel_path}:{line}: {method} argument parse failed")
            continue
        class_resolved = False
        class_name_from_literal = first_class_literal(args)
        class_name = class_name_from_literal
        first_is_string = class_name_from_literal is not None
        if not class_name:
            var = args[0].strip()
            if var in class_vars:
                class_name = class_vars[var]
                class_resolved = True
        member_name = first_method_literal(args, class_resolved)
        if not class_name:
            errors.append(f"UNPARSEABLE_HOOK_SURFACE: {rel_path}:{line}: {method} className not a literal string")
            continue
        op = resolve_operation(method)
        if method in ("ModuleHelper.hookAllConstructors", "ModuleHelper.findAndHookConstructor"):
            member_name = "<constructors>"
        if not member_name:
            errors.append(f"UNPARSEABLE_HOOK_SURFACE: {rel_path}:{line}: {method} memberName not a literal string")
            continue
        # Determine parameter type expressions from the call signature.
        if op in ("ALL_METHODS_BY_NAME", "ALL_CONSTRUCTORS"):
            param_exprs: list[str] = []
        elif first_is_string:
            if op == "EXACT_CONSTRUCTOR":
                # [className, classLoader, types..., callback]
                param_exprs = args[2:-1]
            else:
                # [className, classLoader, memberName, types..., callback]
                param_exprs = args[3:-1]
        else:
            # [classExpr/variable, memberName, types..., callback]
            param_exprs = args[2:-1]
        if not args[-1].strip():
            # Missing callback means the call is malformed or the parser stopped early.
            param_exprs = []
        parameter_types: list[str] = []
        unresolved = False
        for p in param_exprs:
            expr = p.strip()
            if not expr:
                continue
            try:
                parameter_types.append(resolve_type_expr(expr, imports))
            except TypeResolutionError as e:
                unresolved = True
                body_lines_before = body[:m.start()].count("\n")
                abs_line = line + body_lines_before
                errors.append(f"UNRESOLVED_PARAMETER_TYPES: {rel_path}:{abs_line}: {method} cannot resolve parameter type {expr}: {e}")
        if unresolved:
            continue
        key = TargetKey(
            class_name=unescape_string(class_name),
            member_name=member_name,
            operation=op,
            parameter_types=tuple(parameter_types),
        )
        # line is approximate; normalize to body-relative line
        body_lines_before = body[:m.start()].count("\n")
        abs_line = line + body_lines_before
        targets.append(ProductionTarget(key=key, hard=is_hard(method), source=f"{rel_path}:{abs_line}", line=abs_line))
    return targets, errors


def extract_balanced_expr(text: str, start: int) -> tuple[str, int] | None:
    """Extract a Kotlin expression starting at `start` until the matching top-level comma or closing paren."""
    i = start
    while i < len(text) and text[i].isspace():
        i += 1
    if i >= len(text):
        return None
    depth = 0
    in_string = False
    escaped = False
    start_i = i
    while i < len(text):
        ch = text[i]
        if in_string:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            i += 1
            continue
        if ch == '"':
            in_string = True
            i += 1
            continue
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            if depth == 0:
                # top-level closing paren; expression ends before this
                break
            depth -= 1
        elif ch == "," and depth == 0:
            break
        i += 1
    return text[start_i:i].strip(), i


def _extract_named_arg(inner: list[str], name: str) -> str | None:
    """Return the value expression for `name = ...` inside a list of call arguments."""
    pattern = re.compile(rf"^\s*{re.escape(name)}\s*=\s*", re.DOTALL)
    for arg in inner:
        m = pattern.match(arg)
        if m:
            return arg[m.end() :].strip()
    return None


def _parse_hook_target_spec_args(
    spec_args: list[str],
    imports: dict[str, str],
    errors: list[str],
    feature_id: str,
    group_id: str | None = None,
) -> TargetKey | None:
    """Parse a HookTargetSpec argument list into a stable TargetKey."""
    full = ", ".join(spec_args)
    class_name = field(full, "className")
    member_name = field(full, "memberName")
    operation = symbol_field(full, "operation")
    if operation == "ALL_CONSTRUCTORS" and not member_name:
        member_name = "<constructors>"

    parameter_types_expr = "emptyList()"
    pt_pos = full.find("parameterTypes")
    if pt_pos >= 0:
        eq_pos = full.find("=", pt_pos)
        if eq_pos >= 0:
            extracted = extract_balanced_expr(full, eq_pos + 1)
            if extracted:
                parameter_types_expr, _ = extracted

    if class_name and member_name and operation:
        parameter_types, pt_errors = resolve_parameter_list(parameter_types_expr, imports)
        errors.extend(pt_errors)
        if pt_errors:
            return None
        return TargetKey(
            class_name=class_name,
            member_name=member_name,
            operation=operation,
            parameter_types=parameter_types,
        )
    ctx = f" {group_id}" if group_id else ""
    errors.append(f"UNPARSEABLE_ANYOF_CANDIDATE: {feature_id}{ctx} HookTargetSpec missing className/memberName/operation")
    return None


def _parse_hook_target_spec(
    spec_text: str,
    imports: dict[str, str],
    errors: list[str],
    feature_id: str,
    group_id: str | None = None,
) -> TargetKey | None:
    """Parse a standalone `HookTargetSpec(...)` expression into a TargetKey."""
    spec_text = spec_text.strip()
    if not spec_text.startswith("HookTargetSpec"):
        ctx = f" {group_id}" if group_id else ""
        errors.append(f"UNPARSEABLE_ANYOF_CANDIDATE: {feature_id}{ctx} candidate is not a HookTargetSpec: {spec_text[:80]}")
        return None
    open_pos = spec_text.find("(")
    if open_pos < 0:
        ctx = f" {group_id}" if group_id else ""
        errors.append(f"UNPARSEABLE_ANYOF_CANDIDATE: {feature_id}{ctx} cannot find HookTargetSpec args: {spec_text[:80]}")
        return None
    inner = balanced_call_args(spec_text, open_pos)
    if not inner:
        ctx = f" {group_id}" if group_id else ""
        errors.append(f"UNPARSEABLE_ANYOF_CANDIDATE: {feature_id}{ctx} cannot parse HookTargetSpec args: {spec_text[:80]}")
        return None
    return _parse_hook_target_spec_args(inner, imports, errors, feature_id, group_id)


def parse_contract_targets(contracts_text: str, feature_id: str) -> tuple[dict[TargetKey, tuple[str, bool]], list[AnyOfGroup], list[str]]:
    """Extract SingleTargetRequirement and AnyOfRequirement blocks for the named contract.

    Returns the single-target map, the AnyOf groups, and a list of diagnostic strings.
    """
    results: dict[TargetKey, tuple[str, bool]] = {}
    anyof_groups: list[AnyOfGroup] = []
    errors: list[str] = []
    imports = parse_imports(contracts_text)
    # find the val <featureId>: HookTargetContract block
    block_re = re.compile(
        rf"val\s+{re.escape(feature_id)}\s*:\s*HookTargetContract.*?\{{",
        re.DOTALL,
    )
    m = block_re.search(contracts_text)
    if not m:
        return results, anyof_groups, errors
    start = m.start()
    open_pos = contracts_text.find("{", m.end() - 1)
    depth = 0
    in_string = False
    escaped = False
    end = -1
    for i in range(open_pos, len(contracts_text)):
        ch = contracts_text[i]
        if in_string:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            continue
        if ch == '"':
            in_string = True
        elif ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                end = i
                break
    if end < 0:
        return results, anyof_groups, errors
    block = contracts_text[open_pos + 1 : end]

    # parse top-level SingleTargetRequirement and AnyOfRequirement blocks
    req_pattern = re.compile(r"(SingleTargetRequirement|AnyOfRequirement)\s*\(", re.DOTALL)
    for rm in req_pattern.finditer(block):
        req_type = rm.group(1)
        inner = balanced_call_args(block, rm.end() - 1)
        if not inner:
            errors.append(f"UNPARSEABLE_CONTRACT_REQUIREMENT: {feature_id} cannot parse {req_type}")
            continue
        full = ", ".join(inner)
        criticality = symbol_field(full, "criticality") or "REQUIRED"
        optional = criticality == "OPTIONAL"

        if req_type == "SingleTargetRequirement":
            target_arg = _extract_named_arg(inner, "target")
            if not target_arg:
                errors.append(f"UNPARSEABLE_CONTRACT_REQUIREMENT: {feature_id} SingleTargetRequirement missing target")
                continue
            key = _parse_hook_target_spec(target_arg, imports, errors, feature_id)
            if key is None:
                continue
            _record_contract_target(results, key, (criticality, optional), feature_id, errors)

        elif req_type == "AnyOfRequirement":
            group_id = field(full, "id")
            if not group_id:
                errors.append(f"UNPARSEABLE_CONTRACT_REQUIREMENT: {feature_id} AnyOfRequirement missing id")
                continue
            candidates_expr = _extract_named_arg(inner, "candidates")
            if not candidates_expr:
                errors.append(f"UNPARSEABLE_CONTRACT_REQUIREMENT: {feature_id} AnyOfRequirement {group_id} missing candidates")
                continue
            list_of_m = re.match(r"^listOf\s*\((.*)\)\s*$", candidates_expr, re.DOTALL)
            if not list_of_m:
                errors.append(f"UNPARSEABLE_ANYOF_CANDIDATE: {feature_id} {group_id} candidates value is not a listOf: {candidates_expr[:80]}")
                continue
            inner_text = list_of_m.group(1).strip()

            candidates: list[TargetKey] = []
            seen_ids: set[str] = set()
            seen_keys: set[TargetKey] = set()
            for cm in re.finditer(r"HookTargetSpec\s*\(", candidates_expr):
                spec_args = balanced_call_args(candidates_expr, cm.end() - 1)
                if not spec_args:
                    errors.append(f"UNPARSEABLE_ANYOF_CANDIDATE: {feature_id} {group_id} cannot parse candidate")
                    continue
                spec_full = ", ".join(spec_args)
                candidate_id = field(spec_full, "id") or ""
                if candidate_id:
                    if candidate_id in seen_ids:
                        errors.append(f"DUPLICATE_ANYOF_CANDIDATE: {feature_id} {group_id} has duplicate id {candidate_id}")
                    seen_ids.add(candidate_id)
                key = _parse_hook_target_spec_args(spec_args, imports, errors, feature_id, group_id)
                if key is None:
                    continue
                if key in seen_keys:
                    errors.append(f"DUPLICATE_ANYOF_CANDIDATE: {feature_id} {group_id} has duplicate candidate {key}")
                seen_keys.add(key)
                candidates.append(key)

            if not candidates:
                if inner_text:
                    errors.append(f"UNPARSEABLE_ANYOF_CANDIDATE: {feature_id} {group_id} cannot parse any candidate from: {inner_text[:80]}")
                else:
                    errors.append(f"EMPTY_ANYOF_GROUP: {feature_id} {group_id} has no candidates")
                continue
            anyof_groups.append(AnyOfGroup(group_id=group_id, candidates=tuple(candidates), optional=optional))
    return results, anyof_groups, errors


def _record_contract_target(
    results: dict[TargetKey, tuple[str, bool]],
    key: TargetKey,
    value: tuple[str, bool],
    feature_id: str,
    errors: list[str],
) -> None:
    """Add a contract target, emitting DUPLICATE_CONTRACT_TARGET if it already exists."""
    if key in results:
        errors.append(f"DUPLICATE_CONTRACT_TARGET: {feature_id} has duplicate {key}")
        return
    results[key] = value


def balanced_call_args(text: str, open_paren_pos: int) -> list[str] | None:
    args: list[str] = []
    depth = 0
    in_string = False
    escaped = False
    current = ""
    for i in range(open_paren_pos, len(text)):
        ch = text[i]
        if in_string:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            current += ch
            continue
        if ch == '"':
            in_string = True
            current += ch
            continue
        if ch == "(":
            depth += 1
            if depth == 1:
                continue
        elif ch == ")":
            if depth == 1:
                if current.strip():
                    args.append(current.strip())
                return args
            depth -= 1
        elif ch == "," and depth == 1:
            if current.strip():
                args.append(current.strip())
            current = ""
            continue
        current += ch
    return None


def unescape_string(s: str) -> str:
    return s.replace("\\$", "$").replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\")


# JVM canonical names for Kotlin built-in / common types.
BUILTINS: dict[str, str] = {
    "Int": "java.lang.Integer",
    "Boolean": "java.lang.Boolean",
    "Long": "java.lang.Long",
    "Float": "java.lang.Float",
    "Double": "java.lang.Double",
    "Char": "java.lang.Character",
    "Byte": "java.lang.Byte",
    "Short": "java.lang.Short",
    "String": "java.lang.String",
    "Any": "java.lang.Object",
    "List": "java.util.List",
    "Map": "java.util.Map",
    "Set": "java.util.Set",
    "Runnable": "java.lang.Runnable",
}


PRIMITIVE_BY_WRAPPER: dict[str, str] = {
    "Int": "int",
    "Boolean": "boolean",
    "Long": "long",
    "Float": "float",
    "Double": "double",
    "Char": "char",
    "Byte": "byte",
    "Short": "short",
}


# Local constants used inside CatalogContracts.
CONTRACT_CONSTANTS: dict[str, str] = {
    "INT": "int",
    "BOOLEAN": "boolean",
    "LONG": "long",
    "STRING": "java.lang.String",
}


def parse_imports(text: str) -> dict[str, str]:
    """Build simple-name -> fully-qualified mapping from import statements."""
    imports: dict[str, str] = {}
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped.startswith("import "):
            continue
        if stripped.startswith("import static "):
            continue
        # Drop trailing semicolon.
        fq = stripped[7:].rstrip(";").strip()
        if " as " in fq:
            fq, alias = fq.rsplit(" as ", 1)
            simple = alias.strip()
        else:
            simple = fq.split(".")[-1]
        imports[simple] = fq
    return imports


def _resolve_simple_name(simple: str, imports: dict[str, str]) -> str:
    if simple in CONTRACT_CONSTANTS:
        return CONTRACT_CONSTANTS[simple]
    if simple in imports:
        return imports[simple]
    if simple in BUILTINS:
        return BUILTINS[simple]
    if "." in simple or "$" in simple:
        return simple
    return simple


def _class_like(segment: str) -> bool:
    """Heuristic: a Java type name usually starts with an uppercase letter.

    This is intentionally conservative. Package names in the target ROM are
    lower-case; a capitalized segment is treated as a class/outer-class name.
    """
    if not segment:
        return False
    # Treat segments containing '$' as already class-like.
    if "$" in segment:
        return True
    return segment[0].isupper()


def _to_jvm_canonical(name: str, imports: dict[str, str]) -> str:
    """Convert a dot-separated Kotlin/Java class reference into a stable JVM name.

    Resolution order:
      1. explicit $ nested class separator
      2. direct import/alias or builtin simple name
      3. imported outer class plus nested suffix
      4. unambiguous fully qualified package/class boundary

    Package dots are preserved; only nested-class separators become '$'.
    Ambiguous or unsupported references raise TypeResolutionError.
    """
    if not name:
        raise TypeResolutionError("empty type reference")

    # 1. explicit nested class separator
    if "$" in name:
        return name

    # 2. direct import/alias or builtin simple name
    if name in imports:
        resolved = imports[name]
        # The import fq may itself contain dots that need canonicalization
        # (e.g. import android.provider.Settings.System).
        return _to_jvm_canonical(resolved, imports) if "." in resolved and "$" not in resolved else resolved
    if name in BUILTINS:
        return BUILTINS[name]
    if name in CONTRACT_CONSTANTS:
        return CONTRACT_CONSTANTS[name]

    # 3. imported outer class plus nested suffix
    if "." in name:
        base, _, rest = name.partition(".")
        base_fq = _resolve_simple_name(base, imports)
        if base_fq != base:
            # base is an imported class; rest is a nested suffix
            return base_fq + "$" + rest.replace(".", "$").replace("$$", "$")

    # 4. unambiguous fully qualified package/class boundary
    if "." in name:
        segments = name.split(".")
        n = len(segments)
        # Find the leftmost index of the rightmost contiguous class-like suffix.
        class_start = n - 1
        while class_start >= 0 and _class_like(segments[class_start]):
            class_start -= 1
        class_start += 1
        if class_start == n:
            raise TypeResolutionError(f"no class-like segment in type reference: {name}")
        # The package part must look like a package (all lower-case starts).
        package_segments = segments[:class_start]
        for seg in package_segments:
            if _class_like(seg):
                raise TypeResolutionError(f"ambiguous package segment in type reference: {name}")
        if not package_segments:
            # No package and no import; e.g. "Settings.System" without an import.
            raise TypeResolutionError(f"ambiguous nested class without package or import: {name}")
        class_chain = "$".join(segments[class_start:])
        return ".".join(package_segments) + "." + class_chain

    # simple name not resolvable
    raise TypeResolutionError(f"unresolvable simple type reference: {name}")


def resolve_type_expr(expr: str, imports: dict[str, str]) -> str:
    """Turn a Kotlin parameter type expression into a stable JVM canonical name."""
    expr = expr.strip().rstrip("!!").rstrip(".!!").strip()
    if not expr:
        raise TypeResolutionError("empty type expression")

    # Contract local constant.
    if expr in CONTRACT_CONSTANTS:
        return CONTRACT_CONSTANTS[expr]

    # Array<T>::class.java -> T[]
    array_m = re.match(r"^Array<(.+)>::class\.java$", expr)
    if array_m:
        inner = resolve_type_expr(array_m.group(1), imports)
        return f"{inner}[]"

    # T::class.java or T::class.javaPrimitiveType
    class_m = re.match(r"^([A-Za-z0-9_$.]+)::class\.java(PrimitiveType)?$", expr)
    if class_m:
        body = class_m.group(1)
        primitive = class_m.group(2)
        if primitive:
            return PRIMITIVE_BY_WRAPPER.get(body, body.lower())
        return _to_jvm_canonical(body, imports)

    # Fully qualified or simple name used directly without ::class.java.
    return _to_jvm_canonical(expr, imports)


def resolve_parameter_list(text: str, imports: dict[str, str]) -> tuple[tuple[str, ...], list[str]]:
    """Parse listOf(...) / emptyList() contents and return canonical types plus errors."""
    errors: list[str] = []
    text = text.strip()
    if text == "emptyList()" or text == "listOf()":
        return (), errors
    m = re.match(r"^listOf\s*\((.*)\)\s*$", text, re.DOTALL)
    if not m:
        return (), [f"UNRESOLVED_PARAMETER_TYPES: unsupported parameterTypes expression: {text}"]
    inner = m.group(1).strip()
    if not inner:
        return (), errors
    # Split by commas at top level (ignoring nested generics).
    parts: list[str] = []
    current = ""
    depth = 0
    for ch in inner:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth -= 1
        elif ch == "," and depth == 0:
            if current.strip():
                parts.append(current.strip())
            current = ""
            continue
        current += ch
    if current.strip():
        parts.append(current.strip())

    types: list[str] = []
    for p in parts:
        if not p:
            continue
        try:
            types.append(resolve_type_expr(p, imports))
        except TypeResolutionError as e:
            errors.append(f"UNRESOLVED_PARAMETER_TYPES: cannot resolve {p}: {e}")
    return tuple(types), errors


def field(block: str, name: str) -> str | None:
    m = re.search(rf"\b{re.escape(name)}\s*=\s*\"([^\"]+)\"", block)
    return unescape_string(m.group(1)) if m else None


def symbol_field(block: str, name: str) -> str | None:
    m = re.search(rf"\b{re.escape(name)}\s*=\s*([A-Za-z0-9_.]+)", block)
    if not m:
        return None
    value = m.group(1)
    if value.startswith("HookOperation."):
        value = value[len("HookOperation."):]
    if value.startswith("Criticality."):
        value = value[len("Criticality."):]
    return value


def check_batch(
    batch: dict[str, tuple[str, str]],
    contracts_text: str,
    source_root: Path,
) -> list[str]:
    issues: list[str] = []
    for feature_id, (rel_path, function_name) in batch.items():
        prod_targets, parse_errors = extract_production_targets(source_root, rel_path, function_name)
        issues.extend(parse_errors)
        single_targets, anyof_groups, contract_parse_errors = parse_contract_targets(contracts_text, feature_id)
        issues.extend(contract_parse_errors)

        prod_target_by_key: dict[TargetKey, ProductionTarget] = {}
        prod_keys: set[TargetKey] = set()
        for pt in prod_targets:
            if pt.key in prod_keys:
                issues.append(f"DUPLICATE_TARGET: {feature_id} production has duplicate {pt.key}")
            prod_keys.add(pt.key)
            prod_target_by_key[pt.key] = pt

        def base(k: TargetKey) -> tuple[str, str, str]:
            return (k.class_name, k.member_name, k.operation)

        # AnyOf group matching: any candidate can satisfy the group.
        anyof_covered: set[TargetKey] = set()
        for group in anyof_groups:
            if not group.candidates:
                issues.append(f"EMPTY_ANYOF_GROUP: {feature_id} {group.group_id}")
                continue

            exact_matches: set[TargetKey] = set()
            for c in group.candidates:
                if c in prod_keys:
                    exact_matches.add(c)
                    anyof_covered.add(c)

            if exact_matches:
                # Group is satisfied; criticality applies to the matched production targets.
                for c in exact_matches:
                    # If the same key is also a single contract target, the single-target
                    # criticality check below handles it; avoid double reporting.
                    if c in single_targets:
                        continue
                    pt = prod_target_by_key[c]
                    if group.optional and pt.hard:
                        issues.append(f"CRITICALITY_MISMATCH: {feature_id} {pt.source}: {group.group_id} candidate {c} contract OPTIONAL but production hard install")
                    elif not group.optional and not pt.hard:
                        issues.append(f"CRITICALITY_MISMATCH: {feature_id} {pt.source}: {group.group_id} candidate {c} contract REQUIRED but production silent install")
                continue

            # No exact match: look for same-base candidates with wrong parameter types.
            mismatches: list[tuple[TargetKey, TargetKey]] = []
            for c in group.candidates:
                for pk in prod_keys:
                    if base(c) == base(pk) and c.parameter_types != pk.parameter_types:
                        mismatches.append((c, pk))
            if mismatches:
                for c, pk in mismatches:
                    pt = prod_target_by_key[pk]
                    issues.append(f"ANYOF_CANDIDATE_MISMATCH: {feature_id} {group.group_id}: candidate {c} does not match production {pt.source} types {pk.parameter_types}")
            else:
                issues.append(f"ANYOF_GROUP_UNSATISFIED: {feature_id} {group.group_id}: no candidate matched production")

        # Single-target comparison.  A production target already covered by an AnyOf
        # group, or that only matches an AnyOf candidate by base, is not reported as
        # missing; it has already generated ANYOF_CANDIDATE_MISMATCH or satisfied the
        # group.  It can still participate in base-level mismatch comparison so that a
        # single contract for the same base is checked.
        single_contract_keys = set(single_targets.keys())
        anyof_base_matched: set[TargetKey] = set()
        for group in anyof_groups:
            for c in group.candidates:
                for pk in prod_keys:
                    if base(c) == base(pk):
                        anyof_base_matched.add(pk)
        prod_keys_for_missing = prod_keys - anyof_covered - anyof_base_matched

        contract_by_base: dict[tuple[str, str, str], list[TargetKey]] = {}
        for k in single_contract_keys:
            contract_by_base.setdefault(base(k), []).append(k)
        prod_by_base_all: dict[tuple[str, str, str], list[TargetKey]] = {}
        for k in prod_keys:
            prod_by_base_all.setdefault(base(k), []).append(k)
        prod_by_base_missing: dict[tuple[str, str, str], list[TargetKey]] = {}
        for k in prod_keys_for_missing:
            prod_by_base_missing.setdefault(base(k), []).append(k)

        # Compare overload signature sets per base. This keeps mismatch and orphan
        # diagnostics disjoint: a same-base overload difference is exactly one
        # PARAMETER_TYPES_MISMATCH, never an orphan variant.
        all_bases = set(contract_by_base.keys()) | set(prod_by_base_all.keys())
        for b in all_bases:
            contract_versions = {c.parameter_types for c in contract_by_base.get(b, [])}
            prod_versions = {p.parameter_types for p in prod_by_base_all.get(b, [])}
            in_contract = b in contract_by_base
            in_prod = b in prod_by_base_all
            in_prod_missing = b in prod_by_base_missing
            if in_contract and in_prod:
                if contract_versions != prod_versions:
                    issues.append(
                        f"PARAMETER_TYPES_MISMATCH: {feature_id}: {b} production types {sorted(prod_versions)} do not match contract types {sorted(contract_versions)}"
                    )
            elif in_prod_missing:
                for k in prod_by_base_missing[b]:
                    pt = prod_target_by_key[k]
                    issues.append(f"MISSING_CONTRACT_TARGET: {feature_id} {pt.source}: {k}")
            elif in_contract:
                for k in contract_by_base[b]:
                    issues.append(f"ORPHAN_CONTRACT_TARGET: {feature_id} contract has {k} not found in production")

        for pt in prod_targets:
            if pt.key in single_targets:
                crit, optional = single_targets[pt.key]
                hard = pt.hard
                if optional and hard:
                    issues.append(f"CRITICALITY_MISMATCH: {feature_id} {pt.source}: {pt.key} contract OPTIONAL but production hard install")
                elif not optional and not hard:
                    issues.append(f"CRITICALITY_MISMATCH: {feature_id} {pt.source}: {pt.key} contract REQUIRED but production silent install")
    return issues


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--batch", default="9,10,11,12", help="comma-separated batch numbers")
    p.add_argument("--contracts", default="app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/CatalogContracts.kt")
    p.add_argument("--source-root", default="app/src/main/java")
    args = p.parse_args(argv)

    contracts_path = Path(args.contracts)
    if not contracts_path.exists():
        print(f"Missing contracts file: {contracts_path}")
        return 1
    contracts_text = contracts_path.read_text(encoding="utf-8")
    source_root = Path(args.source_root)

    selected = [b.strip() for b in args.batch.split(",")]
    issues: list[str] = []
    for b in selected:
        batch = BATCH_FUNCTIONS.get(b)
        if not batch:
            print(f"Unknown batch: {b}")
            return 1
        issues.extend(check_batch(batch, contracts_text, source_root))

    if issues:
        for i in sorted(set(issues)):
            print(i)
        return 1

    print("Contract-production parity OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
