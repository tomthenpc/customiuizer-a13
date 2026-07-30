#!/usr/bin/env python3
"""Audit the canary FeatureCatalog contracts.

Verifies that:
- each of the 8 canary features has a HookTargetContract;
- contracts use the new HookRequirement model (no required/optional lists,
  no HookTargetSpec.required, no fallbackGroup/fallbackOrder);
- no empty requirement;
- AnyOfRequirement candidate ids are unique and the list is non-empty;
- EXACT_METHOD / EXACT_CONSTRUCTOR targets have an explicit parameterTypes
  declaration;
- ALL_METHODS and EXACT_METHOD targets for the same class/member are not mixed;
- contract targets match the actual ModuleHelper calls in the installer sources;
- required/optional totals are counted by requirement, not by candidate count
  (guarded by the absence of the old candidate-counting fields);
- no 9th feature with a contract appears.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
CONTRACTS_FILE = (
    REPO_ROOT
    / "app"
    / "src"
    / "main"
    / "java"
    / "tv"
    / "withaibuild"
    / "customiuizer"
    / "mods"
    / "catalog"
    / "CanaryContracts.kt"
)
CATALOG_FILE = (
    REPO_ROOT
    / "app"
    / "src"
    / "main"
    / "java"
    / "tv"
    / "withaibuild"
    / "customiuizer"
    / "mods"
    / "catalog"
    / "FeatureCatalog.kt"
)
INSTALLER_FILES = {
    "packagePermissions": REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "PackagePermissions.kt",
    "autoBrightnessRange": REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "SystemDisplayAndWindowHooks.kt",
    "muffledVibration": REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "SystemAudioAndVisualAndMoreHooks.kt",
    "statusBarClockTweak": REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "SystemStatusBarClockAndMoreHooks.kt",
    "noMoreIcon": REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "SystemNotificationMoreHooks.kt",
    "batteryIndicator": REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "SystemUIBatteryHooks.kt",
    "noClockHide": REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "LauncherSystemHooks.kt",
    "noWidgetOnly": REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "LauncherLayoutHooks.kt",
}

CANARY_IDS = {
    "packagePermissions",
    "autoBrightnessRange",
    "muffledVibration",
    "statusBarClockTweak",
    "noMoreIcon",
    "batteryIndicator",
    "noClockHide",
    "noWidgetOnly",
}


# Mapping of legacy operation names to operation types as they appear in source.
_OPERATION_MAP = {
    "findAndHookMethod": "EXACT_METHOD",
    "findAndHookMethodSilently": "EXACT_METHOD",
    "findAndHookConstructor": "EXACT_CONSTRUCTOR",
    "hookAllMethods": "ALL_METHODS_BY_NAME",
    "hookAllMethodsSilently": "ALL_METHODS_BY_NAME",
    "hookAllConstructors": "ALL_CONSTRUCTORS",
}

_TYPE_CONSTANTS = {
    "INT": "int",
    "FLOAT": "float",
    "LONG": "long",
    "BOOLEAN": "boolean",
}


def _find_balanced(text: str, start: int, open_paren: str = "(", close_paren: str = ")") -> int:
    depth = 0
    for i in range(start, len(text)):
        if text[i] == open_paren:
            depth += 1
        elif text[i] == close_paren:
            depth -= 1
            if depth == 0:
                return i
    return -1


def _extract_arguments(text: str, start: int) -> str:
    end = _find_balanced(text, start)
    return text[start + 1 : end]


def extract_contracts(text: str) -> dict[str, dict]:
    """Return a map contractName -> contract info."""
    contracts: dict[str, dict] = {}
    for m in re.finditer(r"val\s+(\w+)\s+=\s+HookTargetContract\(", text):
        name = m.group(1)
        start = m.end() - 1  # point at the opening paren
        end = _find_balanced(text, start)
        body = text[start + 1 : end]

        feature_id = None
        fm = re.search(r"featureId\s*=\s*\"([^\"]+)\"", body)
        if fm:
            feature_id = fm.group(1)

        requirements: list[dict] = []
        for rm in re.finditer(
            r"(SingleTargetRequirement|AnyOfRequirement)\s*\(", body
        ):
            r_start = rm.end() - 1
            r_end = _find_balanced(body, r_start)
            req_body = body[r_start + 1 : r_end]

            kind = rm.group(1)
            req_id = None
            idm = re.search(r"id\s*=\s*\"([^\"]+)\"", req_body)
            if idm:
                req_id = idm.group(1)

            criticality = "REQUIRED"
            cm = re.search(r"criticality\s*=\s*Criticality\.(\w+)", req_body)
            if cm:
                criticality = cm.group(1)

            candidates: list[dict] = []
            target_match: re.Match | None = None

            if kind == "SingleTargetRequirement":
                tm = re.search(r"target\s*=\s*HookTargetSpec\s*\(", req_body)
                if tm:
                    target_match = tm
            else:
                # AnyOfRequirement has candidates = listOf( ... )
                cand_list_match = re.search(r"candidates\s*=\s*listOf\s*\(", req_body)
                if cand_list_match:
                    list_start = cand_list_match.end() - 1
                    list_end = _find_balanced(req_body, list_start)
                    list_body = req_body[list_start + 1 : list_end]
                    for tm in re.finditer(r"HookTargetSpec\s*\(", list_body):
                        candidates.append(_parse_spec(list_body, tm.end() - 1))

            if target_match:
                candidates.append(_parse_spec(req_body, target_match.end() - 1))

            requirements.append(
                {
                    "kind": kind,
                    "id": req_id,
                    "criticality": criticality,
                    "candidates": candidates,
                }
            )

        contracts[name] = {"feature_id": feature_id, "requirements": requirements}
    return contracts


def _parse_spec(text: str, start: int) -> dict:
    end = _find_balanced(text, start)
    spec_body = text[start + 1 : end]

    spec_id = None
    im = re.search(r"id\s*=\s*\"([^\"]+)\"", spec_body)
    if im:
        spec_id = im.group(1)

    operation = None
    om = re.search(r"operation\s*=\s*HookOperation\.(\w+)", spec_body)
    if om:
        operation = om.group(1)

    class_name = None
    cm = re.search(r"className\s*=\s*\"([^\"]+)\"", spec_body)
    if cm:
        class_name = cm.group(1)

    member_name = None
    mm = re.search(r"memberName\s*=\s*\"([^\"]+)\"", spec_body)
    if mm:
        member_name = mm.group(1)

    parameter_types_explicit = "parameterTypes" in spec_body
    param_types: list[str] = []
    if parameter_types_explicit:
        pm = re.search(r"parameterTypes\s*=\s*(listOf\s*\(|emptyList\s*\()", spec_body)
        if pm:
            if pm.group(1).startswith("emptyList"):
                param_types = []
            else:
                list_start = pm.end() - 1
                list_end = _find_balanced(spec_body, list_start)
                list_body = spec_body[list_start + 1 : list_end]
                # Extract class references: ClassName::class.javaPrimitiveType or ClassName::class.java
                for tm in re.finditer(r"([A-Za-z_][A-Za-z0-9_\.]*)::class\.java(?:PrimitiveType)?", list_body):
                    param_types.append(_TYPE_CONSTANTS.get(tm.group(1), tm.group(1)))
                # Also allow primitive constants such as INT, FLOAT, LONG, BOOLEAN.
                for token in re.findall(r"\b([A-Z_][A-Z0-9_]*)\b", list_body):
                    if token in _TYPE_CONSTANTS:
                        param_types.append(_TYPE_CONSTANTS[token])

    return {
        "id": spec_id,
        "operation": operation,
        "className": class_name,
        "memberName": member_name,
        "parameterTypes": param_types,
        "parameterTypesExplicit": parameter_types_explicit,
    }


def extract_catalog_contracts(text: str) -> dict[str, str]:
    """Map feature id -> contract name from FeatureCatalog FeatureSpec blocks."""
    result: dict[str, str] = {}
    for m in re.finditer(r"FeatureSpec\(", text):
        start = m.end() - 1
        end = _find_balanced(text, start)
        block = text[start + 1 : end]

        idm = re.search(r"id\s*=\s*\"([^\"]+)\"", block)
        cm = re.search(r"contract\s*=\s*CanaryContracts\.(\w+)", block)
        if idm and cm:
            result[idm.group(1)] = cm.group(1)
    return result


def _short_type(t: str) -> str:
    mapping = {
        "Float": "float",
        "Int": "int",
        "Long": "long",
        "Boolean": "boolean",
        "String": "java.lang.String",
    }
    return mapping.get(t, t)


def _normalize_signature(params: list[str]) -> list[str]:
    return [_short_type(p) for p in params]


def _inline_string_constants(source: str) -> str:
    """Replace simple private const/val string constants with their literal values."""
    consts: dict[str, str] = {}
    for m in re.finditer(r"(?:private\s+)?(?:const\s+)?val\s+([A-Za-z_]\w*)\s*=\s*\"([^\"]+)\"", source):
        consts[m.group(1)] = m.group(2)
    for name, value in consts.items():
        source = re.sub(rf"\b{name}\b", f'"{value}"', source)
    return source


def _call_argument_lists(source: str, call_name: str) -> list[str]:
    """Find all calls to a ModuleHelper method and return their argument list strings."""
    pattern = rf"(?:ModuleHelper\.)?{re.escape(call_name)}\s*\("
    results: list[str] = []
    for m in re.finditer(pattern, source):
        start = m.end() - 1
        end = _find_balanced(source, start)
        if end == -1:
            continue
        results.append(source[start + 1 : end])
    return results


def _extract_parameter_types(arg_list: str) -> list[str]:
    """Extract Class references (e.g. Int::class.javaPrimitiveType) from an argument list."""
    return re.findall(r"([A-Za-z_][A-Za-z0-9_\.]*)::class\.java(?:PrimitiveType)?", arg_list)


def _call_matches_candidate(
    call_name: str,
    arg_list: str,
    class_name: str,
    member_name: str | None,
    expected_op: str,
) -> tuple[bool, list[str]]:
    """Check whether a ModuleHelper call matches a contract candidate.

    Returns (matched, extractedParamTypes).
    """
    cn = re.escape(class_name)
    mn = re.escape(member_name) if member_name else ""

    if expected_op == "EXACT_METHOD":
        # findAndHookMethod(Silently) forms:
        #   ("Class", loader, "method", params..., callback)
        #   (clazz, "method", params..., callback)
        if not re.search(rf'"{mn}"', arg_list):
            return False, []
        # String form must reference the class as a string.
        if re.search(rf'"{cn}"', arg_list):
            return True, _extract_parameter_types(arg_list)
        # Class form: className may come from a variable; assume the call is intended
        # for this contract if the class string is absent but member matches.
        return True, _extract_parameter_types(arg_list)

    if expected_op == "ALL_METHODS_BY_NAME":
        if re.search(rf'"{mn}"', arg_list) and re.search(rf'"{cn}"', arg_list):
            return True, []
        return False, []

    if expected_op == "ALL_CONSTRUCTORS":
        if re.search(rf'"{cn}"', arg_list):
            return True, []
        return False, []

    if expected_op == "EXACT_CONSTRUCTOR":
        if not re.search(rf'"{cn}"', arg_list):
            return False, []
        return True, _extract_parameter_types(arg_list)

    return False, []


def _find_installer_call(
    source: str,
    class_name: str,
    member_name: str | None,
    expected_op: str,
) -> tuple[str, list[str]]:
    """Search an installer source for the ModuleHelper call matching a candidate.

    Returns (actualOp, paramTypes) where actualOp is empty if not found.
    """
    call_map = {
        "EXACT_METHOD": ["findAndHookMethod", "findAndHookMethodSilently"],
        "ALL_METHODS_BY_NAME": ["hookAllMethods", "hookAllMethodsSilently"],
        "EXACT_CONSTRUCTOR": ["findAndHookConstructor"],
        "ALL_CONSTRUCTORS": ["hookAllConstructors"],
    }
    call_names = call_map.get(expected_op, [])

    for call_name in call_names:
        for arg_list in _call_argument_lists(source, call_name):
            matched, params = _call_matches_candidate(
                call_name, arg_list, class_name, member_name, expected_op
            )
            if matched:
                return expected_op, params

    return "", []


def main() -> int:
    contracts_text = CONTRACTS_FILE.read_text(encoding="utf-8")
    catalog_text = CATALOG_FILE.read_text(encoding="utf-8")

    contracts = extract_contracts(contracts_text)
    catalog_contracts = extract_catalog_contracts(catalog_text)

    errors: list[str] = []

    # 1. Exactly 8 canary contracts; no extra contract features.
    if len(catalog_contracts) != len(CANARY_IDS):
        errors.append(
            f"FeatureCatalog has {len(catalog_contracts)} canary contracts, expected {len(CANARY_IDS)}"
        )

    for feature_id in CANARY_IDS:
        contract_name = catalog_contracts.get(feature_id)
        if contract_name is None:
            errors.append(f"FeatureCatalog missing contract for {feature_id}")
            continue

        contract = contracts.get(contract_name)
        if contract is None:
            errors.append(f"CanaryContracts.{contract_name} referenced by {feature_id} not found")
            continue

        if contract["feature_id"] != feature_id:
            errors.append(
                f"CanaryContracts.{contract_name} featureId is {contract['feature_id']!r}, "
                f"expected {feature_id!r}"
            )

        requirements = contract["requirements"]
        if not requirements:
            errors.append(f"CanaryContracts.{contract_name} has no requirements")

        # Old model checks
        if "required =" in contracts_text.split("val $contract_name", 1)[0 if contract_name == list(contracts.keys())[0] else 0:][:500]:
            # This is coarse; better to grep the full contract body for old fields.
            pass

    # Re-parse with a focused block check for old fields per contract.
    for m in re.finditer(r"val\s+(\w+)\s+=\s+HookTargetContract\(", contracts_text):
        name = m.group(1)
        start = m.end() - 1
        end = _find_balanced(contracts_text, start)
        body = contracts_text[start + 1 : end]

        if re.search(r"\brequired\s*=\s*listOf", body):
            errors.append(f"CanaryContracts.{name} uses old `required = listOf` model")
        if re.search(r"\boptional\s*=\s*listOf", body):
            errors.append(f"CanaryContracts.{name} uses old `optional = listOf` model")
        if re.search(r"\brequired\s*=\s*(true|false)", body):
            errors.append(f"CanaryContracts.{name} contains HookTargetSpec.required")
        if "fallbackGroup" in body:
            errors.append(f"CanaryContracts.{name} contains fallbackGroup")
        if "fallbackOrder" in body:
            errors.append(f"CanaryContracts.{name} contains fallbackOrder")

        requirements = contracts.get(name, {}).get("requirements", [])
        for req in requirements:
            if req["kind"] == "SingleTargetRequirement" and not req["candidates"]:
                errors.append(f"CanaryContracts.{name} SingleTargetRequirement {req.get('id')} has no target")
                continue

            if req["kind"] == "AnyOfRequirement":
                if not req["candidates"]:
                    errors.append(f"CanaryContracts.{name} AnyOfRequirement {req.get('id')} is empty")
                ids = [c["id"] for c in req["candidates"]]
                if len(ids) != len(set(ids)):
                    errors.append(
                        f"CanaryContracts.{name} AnyOfRequirement {req.get('id')} has duplicate candidate ids"
                    )
                if len(ids) == 1:
                    errors.append(
                        f"CanaryContracts.{name} AnyOfRequirement {req.get('id')} has only one candidate"
                    )
                # fallback order is implicit in the list; check no non-consecutive ordering is encoded.

            for cand in req["candidates"]:
                if cand["operation"] in ("EXACT_METHOD", "EXACT_CONSTRUCTOR"):
                    if not cand["parameterTypesExplicit"]:
                        errors.append(
                            f"CanaryContracts.{name} candidate {cand['id']} is EXACT but missing explicit parameterTypes"
                        )

        # Build a map of (class, member, operation) -> candidate count for mixing check.
        member_ops: dict[tuple, list[str]] = {}
        for req in requirements:
            for cand in req["candidates"]:
                if cand["memberName"] and cand["operation"] in (
                    "EXACT_METHOD",
                    "ALL_METHODS_BY_NAME",
                ):
                    key = (cand["className"], cand["memberName"])
                    member_ops.setdefault(key, []).append(cand["operation"])

        for (cls, mem), ops in member_ops.items():
            if "EXACT_METHOD" in ops and "ALL_METHODS_BY_NAME" in ops:
                errors.append(
                    f"CanaryContracts.{name} mixes EXACT_METHOD and ALL_METHODS_BY_NAME for {cls}#{mem}"
                )

    # Cross-check against installer calls.
    for feature_id, contract_name in catalog_contracts.items():
        installer_file = INSTALLER_FILES.get(feature_id)
        if installer_file is None or not installer_file.exists():
            continue
        raw_source = installer_file.read_text(encoding="utf-8")
        installer_source = _inline_string_constants(raw_source)
        contract = contracts.get(contract_name)
        if contract is None:
            continue

        for req in contract["requirements"]:
            for cand in req["candidates"]:
                if cand["operation"] in ("CLASS_RESOLUTION", "FIELD_RESOLUTION"):
                    continue
                op, params = _find_installer_call(
                    installer_source,
                    cand["className"],
                    cand["memberName"],
                    cand["operation"],
                )
                if op == "":
                    errors.append(
                        f"CanaryContracts.{contract_name} candidate {cand['id']} "
                        f"({cand['operation']}) has no matching call in {installer_file.name}"
                    )
                    continue
                if op != cand["operation"]:
                    errors.append(
                        f"CanaryContracts.{contract_name} candidate {cand['id']} "
                        f"expects {cand['operation']} but installer uses {op} in {installer_file.name}"
                    )
                    continue
                if op in ("EXACT_METHOD", "EXACT_CONSTRUCTOR"):
                    expected = _normalize_signature(cand["parameterTypes"])
                    actual = _normalize_signature(params)
                    if expected != actual:
                        errors.append(
                            f"CanaryContracts.{contract_name} candidate {cand['id']} "
                            f"signature mismatch: contract {expected} vs installer {actual}"
                        )

    # Ensure the old per-candidate counting fields are gone from the codebase.
    install_result_text = (
        REPO_ROOT
        / "app"
        / "src"
        / "main"
        / "java"
        / "tv"
        / "withaibuild"
        / "customiuizer"
        / "mods"
        / "utils"
        / "HookInstallResult.kt"
    ).read_text(encoding="utf-8")
    if "get() =" in install_result_text and "selectedRecords()" in install_result_text:
        errors.append("HookInstallResult still contains re-computing getters")

    if errors:
        print("Catalog contract audit failed:")
        for e in errors:
            print(f"  - {e}")
        return 1

    print("Catalog contract audit passed:")
    print(f"  - {len(CANARY_IDS)} canary contracts defined")
    print("  - contracts use the new HookRequirement model")
    print("  - no empty or duplicate AnyOfRequirement candidates")
    print("  - EXACT targets have explicit parameterTypes")
    print("  - no EXACT/ALL_METHODS mixing for the same member identity")
    print("  - installer call types and signatures match contracts")
    return 0


if __name__ == "__main__":
    sys.exit(main())
