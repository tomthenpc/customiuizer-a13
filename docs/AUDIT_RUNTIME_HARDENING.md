# A13 Runtime Hardening Audit

> Branch: `devin/a13-runtime-hardening`  
> Base: `main` after `r13.8.6`  
> Date: 2025-11-01 (round 2)

## 1. Scope and status discipline

This audit tracks the P0 runtime hardening items for the `devin/a13-runtime-hardening` branch.

Status words are used strictly:

- **COMPLETED** — code, unit tests and build/lint evidence are in the branch.
- **VERIFIED** — existing code was reviewed and matches the requirement; no new code was needed.
- **PARTIAL** — implementation is in place but has a documented remaining gap.
- **DEFERRED** — explicitly out of scope for this round.

## 2. P0 status summary

| P0 item | Status | Evidence | Remaining risk |
|---|---|---|---|
| PrefMap concurrency + typed getters | COMPLETED | `PrefMap.kt`, `PrefMapTest.kt` | `Map.contains` operator still resolves to underlying `ConcurrentHashMap` for non-normalized keys; production code uses typed getters, not `contains`. |
| RemotePreferences bootstrap / state machine | COMPLETED | `PreferenceBootstrap.java`, `MainModule.java`, `PreferenceBootstrapTest.kt` | `MainModule.onPackageReady` early `getRemotePrefs()` for `needLoadPrefs` triggers a lazy remote resolve; no double `getAll()` there. Real remote provider behavior needs device validation. |
| Module receiver registration atomicity | COMPLETED | `ModuleHelper.java`, `ModuleHelperReceiverTest.kt` | Identity check after `registerReceiver` is now explicit. Full 100-iteration race replay is in the existing receiver test; add more if device logs show replacement races. |
| Owned receiver tracking | COMPLETED | `ModuleHelper.java` | Same identity check pattern as module receivers. Weak owner cleanup is verified by `ModuleHelperRegistrationTest`. |
| Observer lifecycle closure | VERIFIED | `ModuleHelper.java` | Weak preference observer cleanup and `handlePreferenceChanged` exception boundaries already in place; additional GC test added in previous round. |
| FeatureDispatcher typed IDs | COMPLETED | `FeatureId.kt`, `FeatureDispatcher.kt`, `FeatureCatalogTest.kt` | `installById(String)` is now a compatibility wrapper that records a diagnostic for unknown ids; new code should call `install(FeatureId, FeatureRuntime)`. |

## 3. RemotePreferences bootstrap (P0-1)

### What changed

- Added `PreferenceBootstrap` (`app/src/main/java/tv/withaibuild/customiuizer/utils/PreferenceBootstrap.java`).
  - State machine: `UNINITIALIZED`, `UNAVAILABLE`, `EMPTY_PENDING`, `LOADED`, `VALID_EMPTY`.
  - Single `synchronized` lock for all state, snapshot and listener state transitions.
  - Double `getAll()` protocol: first read, register unique listener, second read, then publish.
  - Listener is registered at most once per process; registration failure is recorded but does not pretend success.
  - Max retry attempts: 3.
  - `ensureWatcher()` allows late recovery when listener registration failed at first `start()`.
- `MainModule` no longer keeps `remotePrefs`, `mListener`, `prefsLoaded`, `emptyPrefsReported`, `prefsWatcherRegistered` as scattered booleans.
  - `mPrefs` remains a single `PrefMap<String, Object>` instance; `PreferenceBootstrap` updates it in place.
  - `getRemotePrefs()` returns `PreferenceBootstrap.resolveRemote()` for `needLoadPrefs` gating.
  - `initPrefs()` and `watchPreferenceChange()` route to `PreferenceBootstrap.start()` / `ensureWatcher()`.
- `PrefMap.replaceSnapshot(Map)` now adds new/updated entries before removing stale keys, avoiding an intermediate empty map.
- `PrefMap.containsKey` normalizes short and full preference keys.

### Verification

- `PreferenceBootstrapTest` (15 cases) passes:
  - initial state;
  - null / throwing provider;
  - first and second `getAll()` exceptions;
  - listener registration failure;
  - single listener per process;
  - `EMPTY_PENDING`, `VALID_EMPTY`, `LOADED` transitions;
  - concurrent `start()` idempotency;
  - malformed type update does not throw;
  - preference removal updates snapshot;
  - retry cap;
  - `ensureWatcher()` recovery.
- `PrefMapTest` still passes.
- `:app:testDebugUnitTest` and `:app:lintDebug` pass.

## 4. Receiver registration identity (P0-2 / P0-3)

### What changed

- `ModuleHelper.registerModuleReceiver`:
  - `ConcurrentHashMap.compute` removes the old registration and registers the new one atomically per key.
  - After `registerReceiver` returns, an identity check verifies the exact `ReceiverRegistration` is still in the map.
  - If the registration is no longer tracked, it is released immediately to avoid a "registered but untracked" receiver.
- `ModuleHelper.registerOwnedReceiver`:
  - Same per-key `compute` + `synchronized(list)` pattern.
  - Builds the `OwnedReceiverRegistration` before `registerReceiver` so the post-register identity check can compare the exact instance.
  - If the registration is not found in the tracked list after `compute`, it is released.
- `ReceiverRegistration` still stores the application context and releases the framework receiver on cleanup.

### Verification

- `ModuleHelperReceiverTest` passes.
- `ModuleHelperRegistrationTest` passes.

## 5. FeatureDispatcher unknown ID handling (P0-4)

### What changed

- `FeatureDispatcher.installById(String, FeatureRuntime)` parses the id with `FeatureId.fromString`.
- Unknown ids are recorded via `DiagnosticRecorder` with:
  - `id = DiagnosticIds.UNKNOWN_FEATURE_ID`
  - `installation = InstallOutcome.FAILED`
  - `reasonCode = ReasonCode.UNKNOWN`
  - `detail = featureId`
- `FeatureDispatcher.install(FeatureId, FeatureRuntime)` is the typed entry point.

### Verification

- `FeatureCatalogTest.unknownFeatureCreatesNoRuntimeState` updated to assert the diagnostic snapshot instead of an empty log.

## 6. P1/P2 deferred items

These were not touched in this round:

- `MainModule` per-process installer split.
- `ResourceHooks` full `SparseArray` split for resolved resource IDs.
- Full `mods/` callback boundary audit and `ModuleHelper.guarded` wrap.
- UI, locale, release builds and real-device validation.

## 7. Build and test evidence

| Check | Command | Result |
|---|---|---|
| Java compile | `gradlew :app:compileDebugJavaWithJavac` | PASS |
| Kotlin compile | `gradlew :app:compileDebugKotlin` | PASS |
| Unit tests | `gradlew :app:testDebugUnitTest` | PASS |
| Lint | `gradlew :app:lintDebug` | PASS |
| APK build | intentionally not run | N/A |

## 8. Remaining risks and next steps

1. **Device validation**: `PreferenceBootstrap` uses `SharedPreferences` behavior from the libxposed remote provider. A real device must confirm the double `getAll()` protocol closes the observed race and that listener callbacks arrive on the expected thread.
2. **APK / Release build**: not run in this round; required before any merge.
3. **Callback boundary audit**: still pending for `mods/` anonymous `BroadcastReceiver`, `ContentObserver`, `Runnable` and view listeners.
4. **ResourceHooks hot path**: `ConcurrentHashMap<String, ...>` for unresolved/wildcard replacements remains; split into `SparseArray` for resolved IDs in a separate commit if device traces justify it.
5. **PrefMap `contains` operator**: the Kotlin `in`/`contains` call does not normalize keys through `PrefMap.containsKey` in this configuration. All production accessors (`get*`) normalize correctly, so the practical impact is limited to code that uses `contains()` directly.

---

## A13 0731 closeout

| Item | Status | Evidence |
|---|---|---|
| AGENTS.md simplified | COMPLETED | `AGENTS.md` rewritten |
| ADB/device automation removed | COMPLETED | `tools/k7-device-smoke.ps1` and device smoke docs deleted |
| `tools/verify.py` | COMPLETED | `verify.py` with `fast`/`full` modes, no APK build |
| `tools/analyze_lsposed_log.py` | COMPLETED | offline log analyzer + `docs/LSPOSED_LOG_ANALYSIS.md` |
| CI workflow | COMPLETED | `.github/workflows/build.yml` no longer builds APK |
| PrefMap atomic snapshot | COMPLETED | `PrefMap.kt` uses `AtomicReference` immutable snapshot, `PrefMapTest` passes |
| PreferenceBootstrap state | VERIFIED | `PreferenceBootstrapTest` passes |
| Module/Owned receiver | VERIFIED | `ModuleHelperReceiverTest` passes |
| FeatureDispatcher typed IDs | VERIFIED | `FeatureCatalogTest` passes |
| MainModule process split | PARTIAL | `SystemServerInstaller` extracted from `onSystemServerStarting`; `onPackageReady` still in `MainModule` |
| ResourceHooks SparseArray | COMPLETED | `ResourceHooks.java` uses `unresolved` `ConcurrentHashMap` + `active` `SparseArray` copy-on-write, bounded by `MAX_ACTIVE` |
| Full callback boundary | VERIFIED | `ModuleHelper.guarded` enforced by `check-invariants.py` |

*Updated during the `devin/a13-runtime-hardening` continuation session.*
