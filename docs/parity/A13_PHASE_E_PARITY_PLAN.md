# A13 Phase E Parity Plan (from Phase D-R2 candidate inventory)

```text
INPUT = docs/parity/A13_A14_FEATURE_MATRIX.csv + D4 settings parity audit
PHASE_D_POLICY = planning only, no production ports here
```

## Planning principles

- Port by **user-visible semantic contract**, not by code-shape similarity.
- Preserve A13 MIUI14/API33 + HyperOS1/A13 compatibility constraints.
- Keep Dynamic Island as `INTENTIONAL_EXCLUDED` (`PORT=NO`).
- Treat `UI_WITHOUT_IMPLEMENTATION` and `IMPLEMENTATION_WITHOUT_UI` as first-class parity debt.

## Batch plan (row-traceable from matrix)

### E1 - Settings/Maintenance foundation (P0/P1)

- Backup/restore typed format parity (versioning, integrity, bounded decode, migration, rollback).
- Search/destination reconciliation for dead/unreachable entries.
- Restart target UX normalization and metadata consistency.
- Language/about flow consolidation and locale-restore reconciliation.

```text
PHASE_E_E1_COUNT = 1
```

### E2 - Low-risk independent features (P1/P2)

- Gaps with isolated process scope and low cross-feature coupling.
- Preference-visible toggles where A13 lacks equivalent user-facing behavior.
- Includes mismatch cleanup where implementation risk is low and testability high.

```text
PHASE_E_E2_COUNT = 2
```

### E3 - Launcher + SystemUI user-visible parity (P1)

- Launcher and SystemUI visible behavior gaps from matrix `MISSING_IN_A13`.
- Status bar/control center/notification/lockscreen visual and interaction parity.
- Keep hot-path constraints and avoid callback-time ROM detection.

```text
PHASE_E_E3_COUNT = 57
```

### E4 - SecurityCenter/permission/installer/app-process parity (P1/P2)

- Security Center / package installer / app-process behavior parity.
- Permission/privacy and service cleanup options requiring process-aware routing.

```text
PHASE_E_E4_COUNT = 9
```

### E5 - system_server high-risk parity (P0/P1)

- Boot-critical, cross-process, or crash-sensitive missing semantics.
- Requires strongest preflight static proof + targeted regression plan + staged rollout.

```text
PHASE_E_E5_COUNT = 4
```

Total planned gaps:

```text
TOTAL_PHASE_E_PARITY_GAPS = 77
HOLD_EVIDENCE_COUNT = 4
PHASE_E_READY_GAPS = 73
```

Counts are derived from `A13_A14_FEATURE_MATRIX.csv` rows where
`parity_state in {MISSING_IN_A13, PARTIAL_PARITY}`.
Rows with unresolved design evidence are routed to `HOLD_EVIDENCE`
and excluded from E1-E5 ready counts.

## Per-feature implementation template (to be used in E phase execution)

For each selected feature row, Phase E tickets must include:

- `A14_REFERENCE`
- `A13_CURRENT_STATE`
- `EXPECTED_USER_BEHAVIOR`
- `A13_API33_DESIGN_DIRECTION`
- `PROCESS`
- `CLASSLOADER`
- `RISK`
- `DEPENDENCIES`
- `TEST_STRATEGY`
- `ROM_EVIDENCE_NEEDED`
- `PORT_PRIORITY (P0-P3)`

## Dynamic Island policy carry-forward

```text
INTENTIONAL_EXCLUDED = Dynamic Island only
PHASE_E = NEVER
```

