# A13 ← A14 Engineering Method Non-Port Decisions

> Branch: `devin/a13-runtime-hardening`  
> Final HEAD: `ef61d76f55300ca1ec8494b418e3996c3ba6824c`  
> Date: 2026-07-31

This document records A14 `customiuizer-a14` engineering changes that were evaluated and explicitly not ported to A13.

## Decision process

A13 is the shipping base for MIUI 14 / Android 13. A14 code can only be used as an engineering-method reference. We port a change only when:

1. A13 does not already have an equivalent implementation, **and**
2. The change closes a real A13 bug or hardening gap, **and**
3. The change does not introduce Android-14-specific class names, hook targets, resources, or SystemUI data structures.

## A14 changes evaluated

### 1. `FeatureInstallRegistry` with ordered `ArrayList<FeatureDefinition>`

**A14 change**: A generic `FeatureInstallRegistry` that keeps an ordered list of `FeatureDefinition` objects, a `ConcurrentHashMap` of definitions, and states. It installs by target/phase with deterministic order and detects duplicate `FeatureId` registrations.

**A13 equivalent**: `FeatureDispatcher` is an `object` with an exhaustive `when (feature)` over `enum class FeatureId`. There is no dynamic registration; the catalog is fixed at compile time. `FeatureDispatcher.installById(String)` is a compatibility wrapper that records a diagnostic for unknown ids and dispatches to the typed `install(FeatureId, FeatureRuntime)`.

**Why not ported**:
- A13 does not have a runtime `FeatureDefinition` registry, so the `FeatureInstallRegistry` model is unnecessary.
- `FeatureId` is an A13 `enum`; the compiler enforces exhaustive handling. Duplicate `FeatureId` entries are impossible.
- The ordered iteration problem (iteration order of `ConcurrentHashMap.values()`) does not exist because A13 uses a fixed `when` with explicit branch order, not a map iteration.
- Adding `FeatureInstallRegistry` would introduce a large new runtime abstraction, more allocations, and more state for no A13 benefit.

**Risk if ported**:
- Increased cold-path allocation and state management.
- Divergence from the verified A13 `FeatureDispatcher` / `FeatureCatalog` behavior.
- Potential `ClassLoader` / R8 issues if `FeatureDefinition` subtypes are not kept.

### 2. `FeatureId` as `interface` (removed `sealed`)

**A14 change**: `FeatureId` changed from `sealed interface` to plain `interface`.

**A13 equivalent**: `FeatureId` is `enum class FeatureId`.

**Why not ported**:
- A13 does not need an open `FeatureId` type. The `enum` is closed and exhaustive, which the compiler verifies.
- A plain `interface` would allow new `FeatureId` implementations at runtime, but A13 `FeatureDispatcher` cannot handle unknown feature IDs beyond the `when` branches.
- Keeping `enum` preserves the A13 R8 keep rules and exhaustive `when` diagnostics.

### 3. `HookDiagnostics` `DUPLICATE_FEATURE` and `Kind.FEATURE`

**A14 change**: New diagnostic enums for duplicate feature registration and a `FEATURE` diagnostic kind.

**A13 equivalent**: `DiagnosticRecorder` with `DiagnosticIds.UNKNOWN_FEATURE_ID` already records unknown feature IDs. Duplicate `FeatureId` cannot occur in the A13 `enum` model.

**Why not ported**:
- A13 has no duplicate feature ID risk, so `DUPLICATE_FEATURE` would be dead code.
- Adding `Kind.FEATURE` to A13 diagnostics would require touching `DiagnosticIds`, `DiagnosticRecorder`, and tests for a scenario that cannot happen.
- A13 already records unknown feature IDs; this is the only runtime diagnostic needed for the typed feature catalog.

### 4. `FeatureInstallRegistry` ordered snapshot replacement on preference change

**A14 change**: `onPreferenceChanged` iterates `orderedFeatures` and re-evaluates each feature that matches the changed preference.

**A13 equivalent**: `PreferenceBootstrap` updates `PrefMap` in place. `FeatureDispatcher` is not involved in per-preference re-evaluation in A13; `MainModule` / `MainModule` installers watch preferences and re-install as needed.

**Why not ported**:
- A13 does not have a central feature registry that needs ordered re-evaluation.
- The `FeatureDispatcher` in A13 is an install-time dispatcher, not a long-lived re-evaluation engine.

## Re-evaluate if

- A13 moves from `enum FeatureId` to a dynamic, plugin-style feature catalog.
- A real device trace shows feature installation ordering affecting SystemUI/Launcher correctness.
- A duplicate feature ID or re-registration bug is observed in A13 logs.

Until then, A13 keeps the verified `FeatureDispatcher` / `FeatureId` design.
