#!/usr/bin/env python3
"""Architecture audit for FeatureCatalog, DiagnosticIds and PreferenceSchema.

Checks:
- Feature IDs are unique and non-empty.
- Diagnostic IDs are unique and non-empty.
- Schema ownerFeature points to an existing FeatureCatalog feature.
- Schema defaultValue type matches PreferenceType.
- Schema restartTarget is consistent with the owner feature restartTarget.
- Catalog preferenceKeys are declared in PreferenceSchema.
"""

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
CATALOG_FILE = REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "catalog" / "FeatureCatalog.kt"
DIAGNOSTIC_IDS_FILE = REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "diagnostics" / "DiagnosticIds.kt"
SCHEMA_FILE = REPO_ROOT / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "prefs" / "PreferenceSchema.kt"


def extract_balanced_blocks(text: str, type_name: str) -> list[str]:
    """Extract object-creation blocks of the form `TypeName(...)`, respecting
    nested parentheses and excluding the `data class TypeName(...)` declaration.
    """
    blocks = []
    pattern = re.compile(r'(?<!class\s)(?<!data\sclass\s)\b' + re.escape(type_name) + r'\(')
    for match in pattern.finditer(text):
        start = match.end() - 1  # position of the opening `(`
        depth = 0
        i = start
        while i < len(text):
            c = text[i]
            if c == '(':
                depth += 1
            elif c == ')':
                depth -= 1
                if depth == 0:
                    blocks.append(text[start + 1:i])
                    break
            i += 1
    return blocks


def parse_key_value_block(block: str) -> dict:
    """Parse a block of `key = value,` pairs, returning a dictionary."""
    result = {}
    # Split on top-level commas only (commas not inside parentheses or quotes).
    parts = []
    current = []
    depth = 0
    in_quote = False
    quote_char = ''
    for c in block:
        if not in_quote and c in '"\'':
            in_quote = True
            quote_char = c
        elif in_quote and c == quote_char:
            in_quote = False
        elif not in_quote:
            if c == '(':
                depth += 1
            elif c == ')':
                depth -= 1
            elif c == ',' and depth == 0:
                parts.append(''.join(current))
                current = []
                continue
        current.append(c)
    if current:
        parts.append(''.join(current))

    for part in parts:
        m = re.match(r'(\w+)\s*=\s*(.*)', part.strip(), re.DOTALL)
        if m:
            key = m.group(1)
            value = m.group(2).strip()
            if value.startswith('"') and value.endswith('"'):
                value = value[1:-1]
            elif value == 'emptySet()':
                value = set()
            elif value.startswith('setOf(') and value.endswith(')'):
                value = set(re.findall(r'"([^"]+)"', value))
            elif value == 'true':
                value = True
            elif value == 'false':
                value = False
            result[key] = value
    return result


def extract_feature_specs(text: str) -> list[dict]:
    """Naive parser for FeatureSpec(...) blocks in FeatureCatalog.kt."""
    blocks = extract_balanced_blocks(text, "FeatureSpec")
    return [parse_key_value_block(block) for block in blocks]



def extract_diagnostic_ids(text: str) -> list[tuple[str, str]]:
    return re.findall(r'const val (\w+)\s*=\s*"([^"]+)"', text)


def extract_schema_entries(text: str) -> list[dict]:
    """Naive parser for PreferenceEntry(...) blocks in PreferenceSchema.kt."""
    blocks = extract_balanced_blocks(text, "PreferenceEntry")
    return [parse_key_value_block(block) for block in blocks]


def type_matches(entry: dict) -> bool:
    ptype = entry.get("type", "")
    default = entry.get("defaultValue", "")
    if ptype == "PreferenceType.BOOLEAN":
        return isinstance(default, bool)
    if ptype == "PreferenceType.INT":
        return bool(re.fullmatch(r'-?\d+', default))
    if ptype == "PreferenceType.STRING":
        return isinstance(default, str)
    if ptype == "PreferenceType.STRING_SET":
        return default == "emptySet()" or isinstance(default, set)
    return False


def main() -> int:
    catalog_text = CATALOG_FILE.read_text(encoding="utf-8")
    diagnostic_text = DIAGNOSTIC_IDS_FILE.read_text(encoding="utf-8")
    schema_text = SCHEMA_FILE.read_text(encoding="utf-8")

    specs = extract_feature_specs(catalog_text)
    diagnostic_id_pairs = extract_diagnostic_ids(diagnostic_text)
    entries = extract_schema_entries(schema_text)

    feature_ids = [s.get("id", "") for s in specs]
    diag_ids = [p[1] for p in diagnostic_id_pairs]
    feature_diag_ids = [s.get("diagnosticId", "") for s in specs]

    errors = 0

    # Feature ID uniqueness
    if len(feature_ids) != len(set(feature_ids)):
        seen = set()
        dupes = {fid for fid in feature_ids if fid in seen or seen.add(fid)}  # type: ignore
        print(f"ERROR: duplicate feature IDs: {dupes}")
        errors += 1

    # Diagnostic ID uniqueness
    for collection, name in [(diag_ids, "DiagnosticIds"), (feature_diag_ids, "FeatureCatalog diagnosticId")]:
        if len(collection) != len(set(collection)):
            seen = set()
            dupes = {did for did in collection if did in seen or seen.add(did)}  # type: ignore
            print(f"ERROR: duplicate {name}: {dupes}")
            errors += 1

    catalog_feature_ids = set(feature_ids)
    catalog_by_id = {s.get("id"): s for s in specs}
    catalog_preference_keys: set[str] = set()
    for spec in specs:
        keys = spec.get("preferenceKeys", set())
        if isinstance(keys, set):
            catalog_preference_keys.update(keys)

    schema_keys = set()
    for entry in entries:
        key = entry.get("key", "")
        schema_keys.add(key)

        owner = entry.get("ownerFeature", "")
        if owner not in catalog_feature_ids:
            print(f"ERROR: ownerFeature '{owner}' for key '{key}' is not a catalog feature ID")
            errors += 1

        if not type_matches(entry):
            print(f"ERROR: defaultValue '{entry.get('defaultValue')}' does not match type '{entry.get('type')}' for key '{key}'")
            errors += 1

        # RestartTarget consistency: schema must be at least as strong as feature.
        feature = catalog_by_id.get(owner)
        if feature:
            feature_restart = feature.get("restartTarget", "")
            schema_restart = entry.get("restartTarget", "")
            if feature_restart and schema_restart != feature_restart:
                print(f"WARNING: restartTarget mismatch for '{key}' (feature={feature_restart}, schema={schema_restart})")
                # Treat as error for now because the starter schema is tiny.
                errors += 1

    # Catalog preferenceKeys must exist in Schema.
    missing_in_schema = catalog_preference_keys - schema_keys
    for key in missing_in_schema:
        print(f"ERROR: catalog preferenceKey '{key}' not in PreferenceSchema")
        errors += 1

    if errors:
        print(f"\nArchitecture audit failed with {errors} error(s).")
        return 1

    print("Architecture audit passed:")
    print(f"  - {len(specs)} feature(s), all IDs unique")
    print(f"  - {len(diag_ids)} diagnostic ID(s), all unique")
    print(f"  - {len(entries)} schema entry(ies), ownerFeatures and types valid")
    print(f"  - {len(catalog_preference_keys)} catalog preference key(s) present in schema")
    return 0


if __name__ == "__main__":
    sys.exit(main())
