#!/usr/bin/env python3
"""Audit the canary FeatureCatalog contracts.

Verifies that:
- each of the 8 canary features has a HookTargetContract;
- every contract declares at least one METHOD or CONSTRUCTOR target;
- no contract relies solely on class existence for compatibility;
- fallback groups contain at least a primary and a fallback target;
- each FeatureSpec in FeatureCatalog references the correct contract.
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


def extract_contracts(text: str) -> dict[str, dict]:
    """Return a map contractName -> list of spec dicts."""
    contracts: dict[str, dict] = {}
    # Match val <name> = HookTargetContract( ... )
    for m in re.finditer(r"val\s+(\w+)\s+=\s+HookTargetContract\(", text):
        name = m.group(1)
        start = m.end()
        # Find the matching closing ) by counting parens
        depth = 1
        i = start
        while i < len(text) and depth > 0:
            if text[i] == "(":
                depth += 1
            elif text[i] == ")":
                depth -= 1
            i += 1
        body = text[start : i - 1]

        feature_id = None
        fm = re.search(r"featureId\s*=\s*\"([^\"]+)\"", body)
        if fm:
            feature_id = fm.group(1)

        specs: list[dict] = []
        for sm in re.finditer(r"HookTargetSpec\(", body):
            s_start = sm.end()
            s_depth = 1
            j = s_start
            while j < len(body) and s_depth > 0:
                if body[j] == "(":
                    s_depth += 1
                elif body[j] == ")":
                    s_depth -= 1
                j += 1
            spec_body = body[s_start : j - 1]

            kind = None
            km = re.search(r"kind\s*=\s*HookTargetKind\.(\w+)", spec_body)
            if km:
                kind = km.group(1)

            required = True
            rm = re.search(r"required\s*=\s*(true|false)", spec_body)
            if rm:
                required = rm.group(1) == "true"

            fallback_group = None
            gm = re.search(r"fallbackGroup\s*=\s*\"([^\"]+)\"", spec_body)
            if gm:
                fallback_group = gm.group(1)

            fallback_order = 0
            om = re.search(r"fallbackOrder\s*=\s*(\d+)", spec_body)
            if om:
                fallback_order = int(om.group(1))

            specs.append(
                {
                    "kind": kind,
                    "required": required,
                    "fallback_group": fallback_group,
                    "fallback_order": fallback_order,
                }
            )

        contracts[name] = {"feature_id": feature_id, "specs": specs}
    return contracts


def extract_catalog_contracts(text: str) -> dict[str, str]:
    """Map feature id -> contract name from FeatureCatalog FeatureSpec blocks."""
    result: dict[str, str] = {}
    for m in re.finditer(r"FeatureSpec\(", text):
        start = m.end()
        depth = 1
        i = start
        while i < len(text) and depth > 0:
            if text[i] == "(":
                depth += 1
            elif text[i] == ")":
                depth -= 1
            i += 1
        block = text[start : i - 1]

        idm = re.search(r"id\s*=\s*\"([^\"]+)\"", block)
        cm = re.search(r"contract\s*=\s*CanaryContracts\.(\w+)", block)
        if idm and cm:
            result[idm.group(1)] = cm.group(1)
    return result


def main() -> int:
    contracts_text = CONTRACTS_FILE.read_text(encoding="utf-8")
    catalog_text = CATALOG_FILE.read_text(encoding="utf-8")

    contracts = extract_contracts(contracts_text)
    catalog_contracts = extract_catalog_contracts(catalog_text)

    errors: list[str] = []

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

        specs = contract["specs"]
        has_method_or_constructor = any(
            s["kind"] in ("METHOD", "CONSTRUCTOR") for s in specs
        )
        if not has_method_or_constructor:
            errors.append(
                f"CanaryContracts.{contract_name} has no METHOD or CONSTRUCTOR targets; "
                "compatibility must not rely on class existence alone"
            )

        class_only = [s for s in specs if s["kind"] == "CLASS"]
        if class_only:
            errors.append(
                f"CanaryContracts.{contract_name} contains CLASS targets: "
                "compatibility should be based on methods/constructors/fields"
            )

        # Validate fallback groups
        groups: dict[str, list[int]] = {}
        for s in specs:
            if s["fallback_group"]:
                groups.setdefault(s["fallback_group"], []).append(s["fallback_order"])
        for group, orders in groups.items():
            if len(orders) < 2:
                errors.append(
                    f"CanaryContracts.{contract_name} fallback group {group!r} has only one target"
                )
            elif len(set(orders)) != len(orders):
                errors.append(
                    f"CanaryContracts.{contract_name} fallback group {group!r} has duplicate orders"
                )

    if errors:
        print("Catalog contract audit failed:")
        for e in errors:
            print(f"  - {e}")
        return 1

    print("Catalog contract audit passed:")
    print(f"  - {len(CANARY_IDS)} canary contracts defined")
    print(f"  - each contract references methods or constructors")
    print(f"  - no class-only contracts detected")
    print(f"  - fallback groups have distinct primary/fallback order")
    return 0


if __name__ == "__main__":
    sys.exit(main())
