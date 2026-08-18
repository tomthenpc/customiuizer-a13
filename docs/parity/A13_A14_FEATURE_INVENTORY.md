# A13/A14 Full User-Visible Feature Inventory (Phase D)

```text
A13_BASE_SHA = eeb2361bb55ba367fe6563eadee8d030049958eb
A14_REFERENCE_SHA = d20d96b543a49a584970e312da7d704958a155aa
MODE = INVENTORY_ONLY (PRODUCTION_AUTHORIZATION=NO)
```

## D0 Method and coverage

Inventory was built with three independent sources (not README/CHANGELOG-only):

1. **UI/preferences source**: all `prefs_*.xml` reachable preference entries.
2. **Implementation source**:
   - A14: `mods/utils/feature/*.kt` `LazyFeatureSpec` + hook/installer references.
   - A13: `PreferenceSchema.kt`, `FeatureCatalog.kt`, installer/hook ownership.
3. **Metadata source**: restart/contract/backup/locale/search structures.

Generated baseline artifact:
- `docs/parity/A13_A14_FEATURE_MATRIX.csv` (machine-readable, stable schema)
- generator: `tools/parity_inventory.py`

## D1 A14 inventory result

```text
A14_VISIBLE_FEATURE_COUNT = 642
```

- Full row-level inventory is in `A13_A14_FEATURE_MATRIX.csv` (`a14_*` columns).
- Each row carries domain, A14 feature ID/name, preference key, parity state, source relationship, risk, priority, and Phase E batch.
- Dynamic Island appears as a dedicated excluded row and is not expanded into helper-only pseudo-gaps.

## D2 A13 inventory result

```text
A13_VISIBLE_FEATURE_COUNT = 628
A13_ONLY_FEATURE_COUNT = 66
```

- A13 inventory rows are represented in the same matrix (`a13_*` columns).
- A13-only rows are preserved as `A13_COMPAT_VARIANT` / keep candidates, not removal candidates.
- Same-key presence is treated as **evidence-only**; semantic parity is classified conservatively (variant by default unless proven exact).

## D3 A14 ↔ A13 semantic parity map (current freeze)

```text
PRESENT_EQUIVALENT_COUNT = 0
PRESENT_A13_VARIANT_COUNT = 628
PARTIAL_PARITY_COUNT = 0
MISSING_IN_A13_COUNT = 79
INTENTIONAL_EXCLUDED_COUNT = 1
INSUFFICIENT_EVIDENCE_COUNT = 0
DYNAMIC_ISLAND_EXCLUDED_COUNT = 1
```

- `PRESENT_A13_VARIANT` dominates: same or near-same user capability with A13 implementation shape differences.
- `MISSING_IN_A13` rows are Phase E candidates (not Phase D implementation targets).
- `INTENTIONAL_EXCLUDED` is only Dynamic Island by product rule.

## D4/D5 discrepancy ledger from multi-source cross-check

```text
UI_WITHOUT_IMPLEMENTATION = 27
IMPLEMENTATION_WITHOUT_UI = 56
DEAD_PREFERENCES = 27 (candidate set, overlaps with UI_WITHOUT_IMPLEMENTATION)
```

Representative UI-without-implementation candidates:
- `system_cc_tile_enabled_color_usemonet`
- `system_hidestatusbar_whenscreenrecord`
- `system_netspeed_prerequisite`
- several status-bar typography keys for temperature/mobile-signal blocks

Representative implementation-without-UI candidates:
- gesture/nav action keys (`controls_backlong_action`, `controls_homelong_action`, etc.)
- bridge/internal keys (`actions`, `activities`, `invocation_type`, etc.)

These are carried as audit findings for Phase E triage, not auto-fixed in Phase D.

## Domain summary snapshot

High-level domain totals derived from A14 rows:

- `system*`: 393 rows (`PRESENT_A13_VARIANT` heavy, primary source of gaps)
- `launcher`: 56 rows (`3` missing)
- `controls`: 41 rows (`1` missing)
- `various*`: 38 rows (`13` missing)
- `main`: 9 rows (all present variant)

Detailed per-row evidence remains authoritative in `A13_A14_FEATURE_MATRIX.csv`.

