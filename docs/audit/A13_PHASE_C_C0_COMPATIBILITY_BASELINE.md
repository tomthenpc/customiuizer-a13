# A13 Phase C C0 - Compatibility Baseline Census

```text
BASE_SHA = a75d2423937d37b8f48e4688a6e9a0548b9154c5
SCOPE = read-only compatibility architecture census
PRODUCTION_CHANGED = NO
```

## Architecture map

- `RomEnvironmentDetector` + `RomEnvironmentDiagnostics`: cold-path ROM profiling, profile values are `MIUI14_A13`, `HYPEROS1_A13`, `UNKNOWN_A13`, `UNSUPPORTED_ANDROID`.
- `FeatureRuntime`: per-process runtime holder with lazy `environment` and lazy per-classloader `HookTargetResolver`.
- `HookTargetContract` / `FeatureTargetVariant` / `HookTargetSpec`: typed compatibility contract model with ordered variants and required/optional requirements.
- `HookTargetResolver` + `HookEvidenceEvaluator`: resolves required/optional targets, supports variant-first selection, and emits compatibility/install evidence.
- `HookInstaller.withSession`: enforces selected variant ownership and binds installer execution to resolver/classloader identity.
- `CanaryContracts` + `CatalogContracts` + `FeatureCatalog` + `FeatureInstallRegistry`: catalog feature declarations and process-scoped install state machine.

## C0 internal matrix (representative rows)

| FEATURE | PROCESS_SCOPE | PACKAGE | PROCESS | CLASSLOADER | CONTRACT | PRIMARY_VARIANT | FALLBACK_VARIANT | REQUIRED_TARGETS | OPTIONAL_TARGETS | INSTALLER_ACTUAL_TARGET | CACHE_OWNER | FAILURE_MODE | ROM_EVIDENCE | CURRENT_RISK |
|---|---|---|---|---|---|---|---|---:|---:|---|---|---|---|---|
| `rom.environment` | process-local metadata | N/A | all injected processes | host process loader | detector-only | `MIUI14_A13` | `HYPEROS1_A13` | 5 properties | 0 | N/A | lazy singleton in `FeatureRuntime` | fail-open on non-fatal property failures | `docs/rom-intelligence/*`, `A13_PROCESS_MATRIX.md` | fatal boundary drift fixed in C1 |
| `packagePermissions` | `SYSTEM_SERVER` | `android` | `system_server` | `SystemServerStartingParam.classLoader` | `CONTRACT_REQUIRED` | `primary` | none | 3 | 2 | session-locked to selected variant | per-runtime resolver cache (max 128) | required miss -> incompatible | static source evidence only | low |
| `statusBarClockTweak` | `SYSTEM_UI` | `com.android.systemui` | main process only | `PackageReadyParam.classLoader` | `CUSTOM` (dynamic contract) | `primary` (prefs-derived active requirements) | none | runtime-derived | runtime-derived | session-locked to compatibility result | per-runtime resolver cache (max 128) | optional miss -> degraded | prior B3B/B3C audit evidence | medium (hybrid installer debt) |
| `autoBrightnessRange` | `SYSTEM_SERVER` | `android` | `system_server` | `SystemServerStartingParam.classLoader` | `CONTRACT_REQUIRED` multi-variant | `automatic_brightness_controller` | `display_power_controller` | 2 per variant | 0 | installer maps selected variant id to enum before hook | per-runtime resolver cache (max 128) | no selectable variant -> incompatible | static contracts + source | low |
| `fixAppInfoLaunch` | `LAUNCHER` | `com.miui.home`/`com.mi.android.globallauncher` | launcher process | `PackageReadyParam.classLoader` | `CONTRACT_REQUIRED` (`AnyOf`) | `ShortcutMenuManager.startAppDetailsActivity` | `Utilities.startDetailsActivityForInfo` | 1 requirement | 0 | `withSession` contract lock prevents variant drift | per-runtime resolver cache (max 128) | fallback hit -> degraded | limited ROM evidence in `docs/rom-intelligence` | medium (ROM variant uncertainty) |

## System-wide invariants verified in C0

- Resolver selected variant and installer variant are explicitly coupled by `HookInstaller.withSession` contract checks.
- Compatibility evaluation stays on cold install path; no ROM detection in UI callbacks.
- Resolver cache is bounded (`MAX_CACHE_ENTRIES = 128`) and instance-scoped by classloader through `FeatureRuntime`.
- `ProcessTarget` matching remains explicit and bounded (no speculative class-name ROM branching).
- Existing ROM evidence is sufficient for architecture baseline; no new ROM extraction performed in C0.

## C0 classification output

```text
C0_RESULT = PASS
NEW_CONFIRMED_DEFECTS = 0 (baseline-only phase)
PHASE_B_REOPENED = NO
```

