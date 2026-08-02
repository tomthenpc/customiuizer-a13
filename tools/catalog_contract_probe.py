#!/usr/bin/env python3
"""Relationship-based Feature Catalog contract checker.

Unlike fixed-count tests, this derives the current set and validates one-to-one
relationships among FeatureSpec IDs, FeatureId enum values, contracts,
diagnostics, installer dispatches and the process matrix.
"""
from __future__ import annotations

import argparse
import re
import sys
from collections import Counter
from pathlib import Path


def balanced_blocks(text: str, token: str) -> list[str]:
    blocks: list[str] = []
    pos = 0
    # Match the token immediately followed by '(' to avoid comment/KDoc matches.
    while True:
        start = text.find(token, pos)
        if start < 0:
            return blocks
        if start + len(token) >= len(text) or text[start + len(token)] != "(":
            pos = start + len(token)
            continue
        open_pos = start + len(token)
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
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    blocks.append(text[start : i + 1])
                    pos = i + 1
                    break
        else:
            return blocks


def field(block: str, name: str) -> str | None:
    m = re.search(rf"\b{re.escape(name)}\s*=\s*\"([^\"]+)\"", block)
    return m.group(1) if m else None


def symbol_field(block: str, name: str) -> str | None:
    m = re.search(rf"\b{re.escape(name)}\s*=\s*([A-Za-z0-9_.]+)", block)
    return m.group(1) if m else None


def parse_matrix_ids(path: Path) -> set[str]:
    ids: set[str] = set()
    if not path.exists():
        return ids
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.startswith("|"):
            continue
        cells = [c.strip() for c in line.strip("|").split("|")]
        if not cells:
            continue
        value = cells[0].strip("` ")
        if re.fullmatch(r"[A-Za-z][A-Za-z0-9_]+", value):
            ids.add(value)
    return ids


def duplicate_messages(label: str, values: list[str]) -> list[str]:
    return [f"{label} duplicate: {v}" for v, count in Counter(values).items() if count > 1]


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--catalog", required=True)
    p.add_argument("--feature-id", required=True)
    p.add_argument("--diagnostic-ids", default="")
    p.add_argument("--matrix", required=True)
    p.add_argument("--source-root", default="app/src/main/java")
    args = p.parse_args(argv)

    catalog_path = Path(args.catalog)
    feature_id_path = Path(args.feature_id)
    matrix_path = Path(args.matrix)
    source_root = Path(args.source_root)

    catalog = catalog_path.read_text(encoding="utf-8")
    feature_id_text = feature_id_path.read_text(encoding="utf-8")

    diagnostic_path = Path(args.diagnostic_ids) if args.diagnostic_ids else feature_id_path
    diagnostic_text = diagnostic_path.read_text(encoding="utf-8")

    blocks = balanced_blocks(catalog, "FeatureSpec")
    specs: list[dict[str, str | None]] = []
    for block in blocks:
        specs.append(
            {
                "id": field(block, "id"),
                "diagnostic": symbol_field(block, "diagnosticId"),
                "scope": symbol_field(block, "processScope"),
                "phase": symbol_field(block, "installPhase"),
                "target": symbol_field(block, "processTarget"),
                "contract": symbol_field(block, "contract"),
            }
        )

    spec_ids = [s["id"] for s in specs if s["id"]]
    enum_ids = re.findall(r'\b[A-Z][A-Z0-9_]*\s*\(\s*"([^"]+)"\s*\)', feature_id_text)
    diagnostics = set(re.findall(r'\bconst\s+val\s+([A-Z][A-Z0-9_]*)\s*=', diagnostic_text))
    contract_ids = re.findall(r'\bfeatureId\s*=\s*"([^"]+)"', catalog)

    dispatch_ids: list[str] = []
    for path in [*source_root.rglob("*.kt"), *source_root.rglob("*.java")]:
        text = path.read_text(encoding="utf-8")
        dispatch_ids.extend(re.findall(r'installById\s*\(\s*"([^"]+)"', text))

    matrix_ids = parse_matrix_ids(matrix_path)
    errors: list[str] = []
    errors += duplicate_messages("FeatureSpec id", spec_ids)
    errors += duplicate_messages("FeatureId canonical id", enum_ids)

    missing_id = sorted(set(spec_ids) - set(enum_ids))
    orphan_enum = sorted(set(enum_ids) - set(spec_ids))
    if missing_id:
        errors.append(f"FeatureSpec ids missing from FeatureId: {missing_id}")
    if orphan_enum:
        errors.append(f"FeatureId ids without FeatureSpec: {orphan_enum}")

    for spec in specs:
        sid = spec["id"] or "<missing-id>"
        diagnostic = spec["diagnostic"]
        if not diagnostic:
            errors.append(f"{sid}: missing diagnosticId")
        elif diagnostic.split(".")[-1] not in diagnostics:
            errors.append(f"{sid}: diagnostic constant does not exist: {diagnostic}")
        if not spec["scope"]:
            errors.append(f"{sid}: missing processScope")
        if not spec["phase"]:
            errors.append(f"{sid}: missing installPhase")
        if not spec["target"]:
            errors.append(f"{sid}: missing processTarget")
        if not spec["contract"]:
            errors.append(f"{sid}: missing contract")

    unknown_contracts = sorted(set(contract_ids) - set(spec_ids))
    if unknown_contracts:
        errors.append(f"contract featureIds without FeatureSpec: {unknown_contracts}")

    unknown_dispatch = sorted(set(dispatch_ids) - set(spec_ids))
    if unknown_dispatch:
        errors.append(f"installer dispatch ids without FeatureSpec: {unknown_dispatch}")

    missing_matrix = sorted(set(spec_ids) - matrix_ids)
    extra_matrix = sorted(matrix_ids - set(spec_ids))
    if missing_matrix:
        errors.append(f"process matrix missing ids: {missing_matrix}")
    if extra_matrix:
        errors.append(f"process matrix has orphan ids: {extra_matrix}")

    if len(blocks) != len(spec_ids):
        errors.append(f"{len(blocks)} FeatureSpec blocks but only {len(spec_ids)} contain literal id")

    if errors:
        print("Catalog contract violations:")
        for e in errors:
            print(f"  - {e}")
        return 1

    print(
        "Catalog contract probe passed: "
        f"{len(spec_ids)} specs, {len(dispatch_ids)} literal dispatches, {len(matrix_ids)} matrix rows"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
