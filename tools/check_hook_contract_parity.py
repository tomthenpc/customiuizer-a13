#!/usr/bin/env python3
"""Compare production hook calls in legacy hook functions to their CatalogContracts definitions.

Covers batches 9/10/11/12 by default. Detects MISSING_CONTRACT_TARGET, ORPHAN_CONTRACT_TARGET,
CRITICALITY_MISMATCH, DUPLICATE_TARGET and UNPARSEABLE_HOOK_SURFACE.
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


@dataclass(frozen=True)
class ProductionTarget:
    key: TargetKey
    hard: bool
    source: str
    line: int


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
        class_name = first_class_literal(args)
        if not class_name:
            var = args[0].strip()
            if var in class_vars:
                class_name = class_vars[var]
                class_resolved = True
        member_name = first_method_literal(args, class_resolved)
        if not class_name:
            errors.append(f"UNPARSEABLE_HOOK_SURFACE: {rel_path}:{line}: {method} className not a literal string")
            continue
        if method in ("ModuleHelper.hookAllConstructors", "ModuleHelper.findAndHookConstructor"):
            member_name = "<constructors>"
        if not member_name:
            errors.append(f"UNPARSEABLE_HOOK_SURFACE: {rel_path}:{line}: {method} memberName not a literal string")
            continue
        op = resolve_operation(method)
        key = TargetKey(class_name=unescape_string(class_name), member_name=member_name, operation=op)
        # line is approximate; normalize to body-relative line
        body_lines_before = body[:m.start()].count("\n")
        abs_line = line + body_lines_before
        targets.append(ProductionTarget(key=key, hard=is_hard(method), source=f"{rel_path}:{abs_line}", line=abs_line))
    return targets, errors


def parse_contract_targets(contracts_text: str, feature_id: str) -> dict[TargetKey, tuple[str, bool]]:
    """Extract SingleTargetRequirement/AnyOfRequirement blocks for the named contract."""
    results: dict[TargetKey, tuple[str, bool]] = {}
    # find the val <featureId>: HookTargetContract block
    block_re = re.compile(
        rf"val\s+{re.escape(feature_id)}\s*:\s*HookTargetContract.*?\{{",
        re.DOTALL,
    )
    m = block_re.search(contracts_text)
    if not m:
        return results
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
        return results
    block = contracts_text[open_pos + 1 : end]

    # parse SingleTargetRequirement blocks
    req_pattern = re.compile(r"SingleTargetRequirement\s*\(", re.DOTALL)
    for rm in req_pattern.finditer(block):
        inner = balanced_call_args(block, rm.end() - 1)
        if not inner:
            continue
        full = ", ".join(inner)
        target_block = inner[0]
        class_name = field(target_block, "className")
        member_name = field(target_block, "memberName")
        operation = symbol_field(target_block, "operation")
        criticality = symbol_field(full, "criticality") or "REQUIRED"
        if operation == "ALL_CONSTRUCTORS" and not member_name:
            member_name = "<constructors>"
        optional = criticality == "OPTIONAL"
        if class_name and member_name and operation:
            key = TargetKey(class_name=class_name, member_name=member_name, operation=operation)
            results[key] = (criticality, optional)
    return results


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
        contract_targets = parse_contract_targets(contracts_text, feature_id)
        prod_keys = set()
        for pt in prod_targets:
            if pt.key in prod_keys:
                issues.append(f"DUPLICATE_TARGET: {feature_id} production has duplicate {pt.key}")
            prod_keys.add(pt.key)

        contract_keys = set(contract_targets.keys())

        missing = prod_keys - contract_keys
        for k in missing:
            pt = next(t for t in prod_targets if t.key == k)
            issues.append(f"MISSING_CONTRACT_TARGET: {feature_id} {pt.source}: {k}")

        orphan = contract_keys - prod_keys
        for k in orphan:
            issues.append(f"ORPHAN_CONTRACT_TARGET: {feature_id} contract has {k} not found in production")

        for pt in prod_targets:
            if pt.key in contract_targets:
                crit, optional = contract_targets[pt.key]
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
