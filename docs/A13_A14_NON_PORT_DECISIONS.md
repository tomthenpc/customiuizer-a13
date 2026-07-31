# A13 ← A14 Engineering Method Non-Port Decisions

> Branch: `devin/a13-runtime-hardening`

This document records A14 `customiuizer-a14` engineering changes that were evaluated and explicitly not ported to A13. A14 code can only be used as an engineering-method reference.

## Decision process

Port a change only when:

1. A13 does not already have an equivalent implementation.
2. The change closes a real A13 bug or hardening gap.
3. The change does not introduce Android-14-specific class names, hook targets, resources, or SystemUI data structures.

## Decisions

| A14 change | A13 equivalent | Why not ported |
|---|---|---|
| `FeatureInstallRegistry` with ordered `ArrayList<FeatureDefinition>` | `FeatureDispatcher` uses an exhaustive `when` over `enum class FeatureId`; no dynamic registry, fixed catalog | A13 has no runtime feature registry; adding the model would be unused complexity, allocations and R8 risk |
| `FeatureId` as plain `interface` (removed `sealed`) | `FeatureId` is `enum class` | A13 needs the closed, compiler-exhaustive `enum`; a plain interface would allow unknown IDs the A13 `when` cannot handle |
| `HookDiagnostics` `DUPLICATE_FEATURE` and `Kind.FEATURE` | `DiagnosticRecorder` with `DiagnosticIds.UNKNOWN_FEATURE_ID`; duplicate `FeatureId` impossible in an enum | The scenario cannot happen in A13; would be dead code |
| `FeatureInstallRegistry` ordered re-evaluation on preference change | `PreferenceBootstrap` updates `PrefMap` in place; `MainModule` installers react directly | A13 `FeatureDispatcher` is install-time only, not a long-lived re-evaluation engine |

## Re-evaluate if

- A13 moves from `enum FeatureId` to a dynamic, plugin-style feature catalog.
- A real device trace shows feature installation ordering affecting SystemUI/Launcher correctness.
- A duplicate feature ID or re-registration bug is observed in A13 logs.
