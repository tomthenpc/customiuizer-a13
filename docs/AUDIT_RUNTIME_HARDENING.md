# A13 Runtime Hardening Audit

> Branch: `devin/a13-runtime-hardening`  
> Base: `main` after `r13.8.6` release  
> Date: 2025-11-01 (session continuation)

## 1. Scope and principles

This audit focuses on the runtime and architecture hardening items tracked in the `devin/a13-runtime-hardening` branch.

Principles applied:

- Features disabled at runtime impose near-zero cost.
- Event-driven responses only when a real event occurs.
- Hot paths avoid unnecessary allocation, reflection, blocking, and logging.
- Compatibility logic is scoped to verifiable boundaries (Android 13 / MIUI 14, API 101).

## 2. Completed changes

### 2.1 PrefMap concurrency and typed getters (P0)

**Files:**

- `app/src/main/java/tv/withaibuild/customiuizer/utils/PrefMap.kt`
- `app/src/test/java/tv/withaibuild/customiuizer/utils/PrefMapTest.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureDispatcher.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureRuntime.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureSpec.kt`
- `app/src/test/java/tv/withaibuild/customiuizer/mods/catalog/*Test.kt`

**What changed:**

- Replaced `HashMap` backing with `ConcurrentHashMap`, removing the need for external synchronization on `MainModule.mPrefs`.
- Added type-safe `getBoolean`, `getInt`, `getLong`, `getString`, `getStringAsInt`, and `getStringSet` getters with non-throwing fallbacks.
- `getStringAsInt` now caches parse results and invalidates the cache on `put`, `remove`, `clear`, and `putAll`.
- Short (`foo`) and full (`pref_key_foo`) keys now produce consistent read/write/lookup results.
- Updated all callers and tests from `PrefMap<String, Any?>` to `PrefMap<String, Any>` because `ConcurrentHashMap` cannot hold null keys or values.

**Verification:**

- `PrefMapTest` passes with concurrency stress tests (8 threads, 1000 iterations each).
- All catalog tests pass after the type change.

### 2.2 Receiver registration atomicity (P0)

**Files:**

- `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java`
- `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelperReceiverTest.kt`

**What changed:**

- `registerModuleReceiver` now uses `ConcurrentHashMap.compute` to ensure the map and framework registration are updated under a single key lock. If the framework registration fails, the in-flight entry is removed and `releaseReceiver` is called.
- `registerOwnedReceiver` now uses `ConcurrentHashMap.compute` with a per-key `ArrayList` guarded by `synchronized`. Stale or same-owner registrations are released before the new one is registered.
- `unregisterOwnedReceiver` synchronizes on the per-key list before mutating it.

**Verification:**

- New `ModuleHelperReceiverTest` covers single-owner replacement, concurrent same-key racing, different-owner coexistence, and failure paths.

### 2.3 Observer lifecycle closure (P0)

**Files:**

- `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelperRegistrationTest.kt`

**What changed:**

- Added a weak-owner garbage-collection test: when an owned preference observer's owner is no longer reachable, `handlePreferenceChanged` skips it and `dropOwnedObserver` removes the dead registration.

**Existing protection already in place:**

- `ModuleHelper` already stores owned observers with `WeakReference<Object>`.
- `handlePreferenceChanged` detects `owner == null` or `observer == null` and triggers `dropOwnedObserver(null, null)` to purge dead entries.

### 2.4 FeatureDispatcher typed IDs (P1)

**Files:**

- `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureId.kt`
- `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureDispatcher.kt`

**What changed:**

- Added a `FeatureId` enum that mirrors the 25 catalog feature IDs.
- Added `FeatureDispatcher.install(FeatureId, FeatureRuntime)` as the primary entry point.
- `FeatureDispatcher.installById(String, FeatureRuntime)` now parses to `FeatureId` and delegates. Unknown IDs are logged without crashing.

**Verification:**

- All `mods.catalog.*` unit tests pass.

### 2.5 Build and test verification

| Check | Command | Result |
|-------|---------|--------|
| Debug unit tests | `gradlew :app:testDebugUnitTest` | PASS |
| Debug assemble | `gradlew :app:assembleDebug` | PASS |
| Lint | `gradlew :app:lintDebug` | PASS |

## 3. Items reviewed but not changed in this session

### 3.1 RemotePreferences startup state (P0)

The current `MainModule.java` already implements the two critical fixes from the A13 Claude audit:

- `initPrefs(Map)` returns early when the snapshot is `null` or empty and does **not** set `prefsLoaded = true`.
- `watchPreferenceChange()` sets `prefsWatcherRegistered = true` **after** `registerOnSharedPreferenceChangeListener` returns successfully.

No regression was found. A full state-machine refactor with `VALID_EMPTY`, `RETRY_PENDING`, etc. was considered but rejected as over-engineering for a branch that already has the required safe behavior.

### 3.2 PrefPair hot-path optimization

`tv.withaibuild.customiuizer.utils.PrefPair` already exists and uses `indexOf('|')` / `regionMatches` without `Regex` allocation. `HookUtils.containsStringPair` delegates to `PrefPair.containsFirst`. A search of `app/src/main` found **no** remaining `split("...".toRegex())` or `substringBefore("|")` patterns.

This item is effectively complete in the current tree.

### 3.3 ResourceHooks hot path

`mods/utils/ResourceHooks.java` already follows the A13 audit recommendations to a large degree:

- `fakes` is a `SparseIntArray`.
- The miss path short-circuits before `findContext()` or `chain.getExecutable().getName()` when both `fakes` and `replacements` are empty.
- `getDimensionPixelOffset` / `getDimensionPixelSize` float-to-int conversion is preserved.

**Remaining risk:**

- `replacements` is still a `ConcurrentHashMap<String, Pair<...>>`. For the next hardening pass, resolve resource IDs at registration time and split into:
  - `volatile SparseArray<Pair<ReplacementType, Object>> resourceIdReplacements` for resolved IDs.
  - `ConcurrentHashMap<String, Pair<ReplacementType, Object>> unresolvedReplacements` for `*:` wildcards or unresolved entries.
- Use copy-on-write under a `replacementsLock` when updating `SparseArray` / `SparseIntArray` because they are not thread-safe for readers during a write.

This is a high-risk change and must be a separate commit with its own `ResourceHooksTest` and release R8 verification.

### 3.4 MainModule installer split (P1)

`MainModule.java` remains a single large dispatcher for `onSystemServerStarting` and `onPackageReady`. Splitting it into per-process installers was scoped out because:

- It is a large, mechanical refactor that does not fix a known runtime defect in this branch.
- It carries high regression risk for feature gating and hook registration order.
- A prerequisite is stabilizing `FeatureDispatcher` and adding per-process runtime factories, which was started but not completed.

Recommended future path:

1. Introduce `SystemServerInstaller`, `SystemUIInstaller`, `LauncherInstaller`, and `PackageInstaller` objects.
2. Move each `if (mPrefs.get...()) { ... }` block to the appropriate installer.
3. Keep `MainModule` as a thin router that calls `Installer.install(runtime)`.
4. Add process-gate unit tests with `FakeXposedInterface`.

### 3.5 Exception boundary review (P1)

- `ModuleHelper.guarded` and named variants already exist for `Runnable` and `Callable<T>`.
- `handlePreferenceChanged` already catches `Throwable` around each observer call and logs it.
- `XposedHelpers.log` is used rather than silently swallowing exceptions.

**Remaining risk:**

- Some anonymous `BroadcastReceiver`, `ContentObserver`, and `Runnable` callbacks outside `ModuleHelper.guarded` were not exhaustively audited. The AGENTS.md rule requires every framework callback to be wrapped.
- A follow-up pass should grep for `new BroadcastReceiver`, `new ContentObserver`, `new Handler`, `post {`, `runOnUiThread`, and `setOnXxxListener` in `mods/` and ensure each has a `ModuleHelper.guarded` entry point.

## 4. Summary and recommendations

| Priority | Item | Status |
|----------|------|--------|
| P0 | PrefMap concurrency / typed getters | Done |
| P0 | RemotePreferences startup state | Already safe |
| P0 | Receiver registration atomicity | Done |
| P0 | Observer lifecycle closure | Verified, test added |
| P1 | FeatureDispatcher typed IDs | Done |
| P1 | MainModule installer split | Deferred |
| P1 | Hot path / allocation reduction | Partially done; ResourceHooks next |
| P1 | Exception boundary review | Partially done; callback audit next |

**Next steps:**

1. Complete `ResourceHooks` split into `SparseArray` + `unresolvedReplacements` (separate commit, own tests).
2. Split `MainModule` into per-process installers.
3. Run a full `mods/` callback boundary audit and wrap remaining anonymous callbacks in `ModuleHelper.guarded`.
4. Run `:app:test`, `:app:lintDebug`, `:app:assembleRelease`, and release R8 / zipalign / signing verification before any merge.

## 5. Evidence

- Unit test reports: `app/build/reports/tests/testDebugUnitTest/index.html`
- Lint report: `app/build/reports/lint-results-debug.html`
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

---

*Generated during the `devin/a13-runtime-hardening` continuation session.*
