# A13 P2 Post-P2-1/P2-2 Lateral Memory/Lifecycle Checkpoint

## Base HEAD

`4c964c85b4fac3d63bda24092549fd8e7aa5012d`

## P2 Cumulative Production Scope

From BUILD-1A integration point (`dbf655870137f8c4003d2521d434fe8cf6a48dc5`) to current HEAD:

| File | Task |
|------|------|
| `app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt` | P2-1 |
| `app/src/main/java/tv/withaibuild/customiuizer/subs/AppSelector.kt` | P2-2 |

No third production file. No scope drift.

## P2-1 Regression Audit

Source review of `SubFragment.kt` at current HEAD:

| Contract | Present |
|----------|---------|
| `pendingHighlightScroll: Runnable?` field | YES (line 50) |
| `removeCallbacks` in onDestroyView | YES (line 259-260) |
| `onDestroyView` override | YES (line 258) |
| Runnable self-clear via `=== this` | YES (line 244-245) |
| post failure clear | YES (line 251-254) |
| 380ms delay | YES (line 251) |
| `highlightKey = null` one-shot | YES (line 227) |

**P2-1 REGRESSION = NO**

P2-2 did not break the parent cleanup chain. `AppSelector.onDestroyView()` calls `super.onDestroyView()` which chains through `SubFragmentWithSearch.onDestroyView()` → `SubFragment.onDestroyView()` (P2-1 highlight-scroll cleanup).

## P2-2 Regression Audit

Source review of `AppSelector.kt` at current HEAD:

| Contract | Present |
|----------|---------|
| `pendingAppLoadStart: Runnable?` field | YES (line 39) |
| `appLoadInFlight` field | YES (line 40) |
| `retryAppLoadAfterInFlight` field | YES (line 41) |
| `applicationContext` used (not Activity) | YES (line 286) |
| `WeakReference<AppSelector>` used | YES (line 287) |
| `scheduleAppLoad()` method | YES (line 281) |
| `onAppLoadFinished()` method | YES (line 323) |
| `companion object` with `startAppLoadWorker` | YES (line 355-391) |
| Thread body no Activity capture | YES |
| Thread body no `this@AppSelector` | YES |
| Thread body no view/listView/process | YES |
| `onDestroyView` clears pending + retry | YES (line 337-343) |

Bytecode evidence (javap on `AppSelector$Companion.class`):
- `startAppLoadWorker` signature: `(Context, WeakReference<AppSelector>, boolean×6, String?)` — no bare AppSelector, no Activity, no View
- Thread Runnable created via `invokedynamic` (indy) — no capture class generated
- `startAppLoadWorker$lambda$1` (Thread body) signature: `(boolean, boolean, boolean, boolean, String, boolean, Context, boolean, WeakReference)` — only primitives + Context + WeakReference
- `startAppLoadWorker$lambda$1$lambda$0` (completion) accesses AppSelector only via `WeakReference.get()` → `access$onAppLoadFinished`

**P2-2 REGRESSION = NO**

## Launcher Scope Guard

`tools/tests/test_launcher_gesture_state_cache.py` `ScopeProtectionTest`:
- Uses `git diff HEAD -- app/src/main/java` to detect uncommitted production changes
- Allowed set does NOT include `SubFragment.kt` or `AppSelector.kt`
- Classification: **A — protects fixed historical slice**
- After commit (clean worktree), test passes because `git diff HEAD` is empty
- Could produce false failures during future production tasks that modify `app/src/main/java` files outside the allowed set, but this is by design — the test guards against scope creep on that specific branch context

No other historical scope guard tests found that use `git diff HEAD` against production paths.

## Evidence-Pending Candidate Downgrades

### 1. SystemChargingAndWallpaperHooks — Wallpaper Handler/Runnable

- Handler stored as `setAdditionalInstanceField` on `WallpaperManagerService` (long-lived system service)
- `removeCallbacks(oldWallpaperRunnable)` before posting new (line 114)
- 1800ms bounded callback
- Captures: `wallpaper` (File), `lpparam`, `mContext` (Context)

**Classification: BOUNDED PROCESS-SERVICE REPLACEMENT**
Not a P2-3 candidate. The replacement pattern is intentional and bounded.

### 2. SystemDisplayAndWindowHooks — Touch/Blur State

- `currentTouchTime`: Long (primitive)
- `currentTouchX`: Float (primitive)
- `currentTouchY`: Float (primitive)
- `mCustomBlurModifier`: Int (primitive)

**Classification: NOT_MEMORY_OWNER_CANDIDATE**
No object references. Pure primitive state tracking.

### 3. SystemStatusBarMoreHooks — Alarm Observer/Receiver

- `alarmObserver`: ContentObserver with `replaceModuleRegistration` unregistration runnable
- `alarmReceiver`: registered via `registerModuleReceiver` (ModuleHelper infrastructure)

**Classification: OWNERSHIP-MANAGED (DOWNGRADE)**
Uses ModuleHelper centralized registration management. Not a P2-3 candidate.

### 4. SystemUIMonitorAndTileHooks — 5G Observer

- `handleSetListening(true)` → `registerContentObserver` + `replaceModuleRegistration`
- `handleSetListening(false)` → `clearModuleRegistration`
- Complete lifecycle tied to tile listening state

**Classification: OWNERSHIP-MANAGED (DOWNGRADE)**
Properly managed through tile listening lifecycle. Not a P2-3 candidate.

## ActivitySelector Review

File: `app/src/main/java/tv/withaibuild/customiuizer/subs/ActivitySelector.kt`

### Ownership Chain

```
Fragment View
→ view?.postDelayed({ Thread { ... }.start() }, animDur.toLong())
    → val act = activity ?: return@Thread     [STRONG Activity capture]
    → val pm = act.packageManager
    → query activities
    → initialized = true
    → act.runOnUiThread(process)              [STRONG Activity handoff]
        → process Runnable operates listView, view directly
            [NO isAdded check, NO view != null check]
```

### Findings

| Question | Answer |
|----------|--------|
| Uses `val act = activity`? | YES (line 84) |
| Delayed callback cancellable? | NO — no Runnable identity, no removeCallbacks |
| Thread strongly captures Activity? | YES — `val act = activity` |
| Thread strongly captures Fragment? | No direct `this@ActivitySelector`, but Activity → Fragment chain |
| `onDestroyView()` cancellation? | NO — no onDestroyView override |
| Background can use applicationContext? | YES — PackageManager available from applicationContext |
| Completion uses `act.runOnUiThread`? | YES (line 97) |
| Completion can operate on dead View? | YES — process Runnable has no lifecycle gate |
| Same class as AppSelector pre-P2-2? | YES — identical problem pattern |

### Final Risk: HIGH

ActivitySelector has the exact same owner-mismatch pattern that P2-2 fixed in AppSelector:
- Strong Activity capture in background Thread
- No cancellable delayed kickoff
- No onDestroyView cleanup
- Completion can operate on dead View

## Helpers App-List Cache Review

### Four App-List Caches

| Cache | Type | Element |
|-------|------|---------|
| `installedAppsList` | `ArrayList<AppData>?` | AppData (metadata only) |
| `launchableAppsList` | `ArrayList<AppData>?` | AppData (metadata only) |
| `openWithAppsList` | `ArrayList<AppData>?` | AppData (metadata only) |
| `shareAppsList` | `ArrayList<AppData>?` | AppData (metadata only) |

### AppData Retained Types

All fields are primitives or Strings:
- `String?`: label, pkgName, actName, iconKey, labelSearchKey, activitySearchKey, selectionKey, primaryUserSelectionKey, userSelectionKey, customTitlePrefKey, activitySummary
- `Boolean`: enabled
- `Int`: user

**No Drawable/Bitmap/Context/View references.** AppData is pure metadata.

### Cache Lifetime

`invalidateAppCaches()` is called on:
- `onTrimMemory()` (level >= `TRIM_MEMORY_UI_HIDDEN`)
- `onLowMemory()`
- Package change broadcast (`ACTION_PACKAGE_ADDED/REMOVED/REPLACED`)

### Duplicate Representation

The four lists may share `AppData` instances or contain separate copies with overlapping entries. However, since AppData holds only metadata (no bitmaps, no contexts), the memory impact of any duplication is bounded by string metadata cardinality, not by graphic resources.

**Classification: CACHE_OPTIMIZATION_CANDIDATE (LOW PRIORITY)**
Not a lifecycle/owner mismatch. No evidence of memory leak. Optimization would require device evidence of actual memory pressure from list duplication.

## Icon LruCache Review

| Property | Value |
|----------|-------|
| Type | `LruCache<String, Bitmap>` |
| Bound | `maxMemory / 16`, coerced to `[512 KB, 8 MB]` |
| sizeOf | `Bitmap.allocationByteCount / 1024` (actual bytes) |
| Eviction | `evictAll()` in `invalidateAppCaches()` |
| Duplicate cache? | NO — single `Helpers.memoryCache`, all loaders use it |
| BitmapCachedLoader | Uses `Helpers.memoryCache` only, no independent cache |

**Classification: PROPERLY_BOUNDED**
LruCache is bounded, uses actual byte count, has clear eviction path. Not a P2-3 candidate.

## Candidate Comparison

| Dimension | A. ActivitySelector | B. App-list cache | C. Icon cache | D. system_server candidates |
|-----------|-------------------|-------------------|---------------|---------------------------|
| PROVEN_OWNER_MISMATCH | 3 (HIGH) | 0 | 0 | 1 |
| RETENTION_DURATION | 3 (View destroy → Thread complete) | 1 (session) | 0 (bounded LRU) | 2 (process) |
| CARDINALITY | 1 (single Fragment) | 2 (hundreds of AppData) | 2 (hundreds of icons) | 1 |
| REPEATED_EXECUTION | 3 (every ActivitySelector open) | 1 (cache hit) | 1 (cache hit) | 2 |
| PROCESS_LIFETIME | 2 (settings process) | 2 (settings process) | 2 (settings process) | 3 (system_server) |
| STATIC_VERIFIABILITY | 3 (source-proven) | 2 | 3 | 1 |
| PRODUCTION_SCOPE | 3 (one file) | 2 (Helpers + callers) | 2 (Helpers) | 2 |
| REGRESSION_RISK | 1 (LOW — same pattern as P2-2) | 2 | 2 | 3 |
| FROZEN_SLICE_INTERSECTION | 0 (none) | 1 (Helpers frozen) | 1 (Helpers frozen) | 0 |
| DEVICE_EVIDENCE_DEPENDENCY | 1 (low — source-proven) | 3 (high) | 3 (high) | 2 |
| **Total** | **20** | **14** | **14** | **15** |

## RECOMMENDED_P2_3

**ActivitySelector async load lifecycle cleanup**

Rationale:
- Same proven owner-mismatch pattern as AppSelector (P2-2)
- Source-proven: `val act = activity`, `act.runOnUiThread(process)`, no onDestroyView, no cancellable callback
- One production file, low regression risk (can reuse P2-2 design principles without shared abstraction)
- Static regression test possible (same contract test approach as P2-2)
- No frozen-slice intersection
- No device evidence required — the lifecycle gap is structurally proven

## DEFERRED Candidates

| Candidate | Reason |
|-----------|--------|
| Helpers app-list cache duplication | AppData is metadata-only; no leak; optimization needs device evidence |
| Icon LruCache | Properly bounded; single cache; no duplication; not a leak |
| SystemChargingAndWallpaperHooks | Bounded process-service replacement; intentional pattern |
| SystemDisplayAndWindowHooks | Primitive state only; not a memory owner |
| SystemStatusBarMoreHooks | Ownership-managed via ModuleHelper |
| SystemUIMonitorAndTileHooks | Ownership-managed via tile listening lifecycle |
