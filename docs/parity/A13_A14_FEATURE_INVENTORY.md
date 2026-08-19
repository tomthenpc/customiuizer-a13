# A13/A14 Product Feature Inventory (Phase D-R3)

```text
A13_BASE_SHA = 15b26ee485c8de344cb543031f163c655c53c995
A14_REFERENCE_SHA = d20d96b543a49a584970e312da7d704958a155aa
OLD_INVENTORY_REJECTED = YES
```

## Method correction summary (R3)

R3 keeps D-R2 structure and performs missing-row semantic alias reconciliation:

- Node types: `ACTIONABLE_FEATURE`, `NAVIGATION_ENTRY`, `CATEGORY`, `DEPENDENCY_HELPER`, `SUBOPTION`, `INTERNAL_STATE`, `UNKNOWN`.
- Product counts include only actionable/suboption rows plus explicit infrastructure rows.
- Key match no longer implies semantic parity; default is `INSUFFICIENT_EVIDENCE`.
- Matrix now uses `evidence_level`:
  - `MECHANICAL_ONLY`
  - `IMPLEMENTATION_PRESENCE`
  - `STRUCTURAL_SEMANTIC_PROOF`
  - `INDIVIDUAL_SEMANTIC_PROOF`
- Only structural/individual proof can produce `PRESENT_*` or `PARTIAL_PARITY`.
- `MISSING_IN_A13` rows are audited by semantic identity (legacy keys, hook aliases, migrated feature IDs), not key equality.
- Added `implementation_mode`: `NEW_PORT`, `UPGRADE_EXISTING_A13`, `NO_IMPLEMENTATION`, `EVIDENCE_HOLD`.
- A13-only rows are explicit `A13_ONLY_KEEP` and excluded from A14 parity RHS counts.

## Corrected counts

```text
A14_PRODUCT_FEATURE_COUNT = 566
A13_PRODUCT_FEATURE_COUNT = 552
A13_ONLY_KEEP_COUNT = 64

PRESENT_EQUIVALENT_COUNT = 0
PRESENT_A13_VARIANT_COUNT = 85
PARTIAL_PARITY_COUNT = 2
MISSING_IN_A13_COUNT = 75
INTENTIONAL_EXCLUDED_COUNT = 1
INSUFFICIENT_EVIDENCE_COUNT = 403
```

## Discovery coverage

- UI topology scanned from A14/A13 `prefs_*.xml`.
- Implementation scan covers **both** `*.kt` and `*.java` for preference reads and feature registration patterns.
- A14 feature-spec discovery supports multi-key specs and reports unknown constructs.

```text
UI_TOPOLOGY_NODE_COUNT_A14 = 712
UI_TOPOLOGY_NODE_COUNT_A13 = 728
A14_SPEC_DISCOVERED = 252
A14_SPEC_UNKNOWN = 1
CURRENT_MISSING_ROWS_AUDITED = 77
FALSE_MISSING_RECLASSIFIED = 2
TRUE_MISSING_REMAINING = 75
```

## Discrepancy confidence classes

```text
CONFIRMED_UI_WITHOUT_IMPLEMENTATION = 0
CANDIDATE_UI_WITHOUT_IMPLEMENTATION = 105
CONFIRMED_IMPLEMENTATION_WITHOUT_UI = 0
CANDIDATE_IMPLEMENTATION_WITHOUT_UI = 59
```

Discrepancy outputs remain conservative (`CANDIDATE`) unless source-reviewed.

## Infrastructure feature rows (explicit, non-toggle)

Included as dedicated product capabilities:

- backup/restore
- language/about
- search navigation
- restart UX
- locale reconcile
- launcher reconcile
- app-selection sanitizer

Dynamic Island is represented once:

```text
DYNAMIC_ISLAND_EXCLUDED_EXACTLY_ONCE = YES
```

Key false-missing corrections:

- `system_usb_default_function` -> A13 `system_defaultusb` family (`PARTIAL_PARITY`, `UPGRADE_EXISTING_A13`)
- `system_detailednetspeed_style` -> A13 `system_detailednetspeed` family (`PARTIAL_PARITY`, `UPGRADE_EXISTING_A13`)

