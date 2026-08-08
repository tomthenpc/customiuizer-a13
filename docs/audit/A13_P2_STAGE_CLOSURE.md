# A13 P2 Lifecycle Optimization Stage Closure

## Current exact HEAD

`aa93b993ecb6f979d6853bf5108df6d449962e16`

## BUILD-1A baseline

`dbf655870137f8c4003d2521d434fe8cf6a48dc5`

## P2 cumulative production scope

```
app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt
app/src/main/java/tv/withaibuild/customiuizer/subs/AppSelector.kt
app/src/main/java/tv/withaibuild/customiuizer/subs/ActivitySelector.kt
```

No fourth production file. No scope drift.

## P2-1 final contract (SubFragment.kt)

- `pendingHighlightScroll: Runnable?` single-slot field — present.
- Schedule-before-replacement removal (`removeCallbacks(previous)`) — present.
- Callback self-clear (`if (pendingHighlightScroll === this) pendingHighlightScroll = null`) — present.
- Post failure clear (`if (!postDelayed(...)) pendingHighlightScroll = null`) — present.
- `onDestroyView` removeCallbacks + null clear — present.
- Cleanup before `super.onDestroyView()` — present.
- 380ms delay unchanged — present.
- Position threshold 9 unchanged (`if (position < 9) return`) — present.
- `highlightKey` one-shot unchanged (`highlightKey = null` after read) — present.

```
P2-1 FINAL REGRESSION = NO
```

## P2-2 final contract (AppSelector.kt)

- `pendingAppLoadStart: Runnable?` single-slot — present.
- `appLoadInFlight` single-flight — present.
- `retryAppLoadAfterInFlight` retry demand — present.
- `applicationContext` worker — present.
- `WeakReference<AppSelector>` handoff — present.
- `companion object` `startAppLoadWorker` — present.
- Worker does not strongly capture Activity/View/AppSelector — confirmed (params: Context, WeakReference, Boolean/String snapshots).
- Thread creation remains outside instance delayed Runnable — confirmed (Thread in companion worker only).
- Failure + recreated View retry gap closed (`retry && isAdded && view != null`) — present.
- No unconditional retry — confirmed.
- `onDestroyView` pending cancellation — present.
- Retry demand View-scoped — confirmed.

```
P2-2 FINAL REGRESSION = NO
```

## P2-3 final contract (ActivitySelector.kt)

- `initialized` occurrences = 0 — confirmed.
- No Fragment-lifetime result cache — confirmed.
- Fresh View query semantics restored (`inFlight → retry; else → scheduleActivityLoad`) — present.
- `pendingActivityLoadStart: Runnable?` single-slot — present.
- `activityLoadInFlight` single-flight — present.
- `retryActivityLoadAfterInFlight` retry demand — present.
- `applicationContext` worker — present.
- `WeakReference<ActivitySelector>` handoff — present.
- `companion object` `startActivityLoadWorker` — present.
- Worker-local `loadedActivities` — present.
- Background Fragment.activities mutation = NO — confirmed.
- Main-thread result mutation only (`mainExecutor.execute`) — confirmed.
- Success + no live View → result discarded (`activities.clear/addAll` inside `isAdded && view != null` gate) — confirmed.
- `onDestroyView` → pending callback removed, retry cleared, `activities.clear()`, `activityLoadInFlight` NOT falsely cleared — confirmed.

```
P2-3 FINAL REGRESSION = NO
```

## Launcher scope guard state

`tools/tests/test_launcher_gesture_state_cache.py` allowed set:
- `SubFragment.kt` = absent
- `AppSelector.kt` = absent
- `ActivitySelector.kt` = absent

```
LAUNCHER_SCOPE_GUARD = RESTORED
```

## Test-governance debt

```
TEST_GOVERNANCE_DEBT:

test_launcher_gesture_state_cache.py
uses:

git diff --name-only HEAD -- app/src/main/java

This can false-fail during legitimate uncommitted future production tasks.

Do not solve by expanding historical whitelist.

Future cleanup should redesign the guard around:
- fixed historical task scope
or
- explicit source invariants
rather than current worktree production diff.
```

Status: `NON_BLOCKING`

## Remaining candidate classification

From P2-0 inventory (559 total records: 408 REVIEWED, 151 NEEDS_ROM_EVIDENCE)
re-sampled against current HEAD source.

### A. STATIC_ACTIONABLE

None.

No remaining candidate simultaneously satisfies all of:
proven owner mismatch, meaningful retention duration/cardinality,
small isolated scope, static behavior proof possible, no frozen slice
conflict, low regression risk.

The P2-1/P2-2/P2-3 batch already captured the three Fragment-lifecycle
delayed-callback and background-worker ownership sites that were
statically provable. All remaining MEDIUM-risk candidates are
`UNKNOWN_REQUIRES_MANUAL_REVIEW` with `NEEDS_ROM_EVIDENCE` — their
retention duration, cardinality, and release paths cannot be proven
from source alone.

### B. DEVICE_EVIDENCE_REQUIRED

- `SystemChargingAndWallpaperHooks` — `mWallpaperHandler` (Handler) and
  `mWallpaperRunnable` (Runnable) additional instance fields on
  system_server wallpaper objects. Owner lifetime and release path
  depend on MIUI wallpaper service lifecycle. Cannot prove retention
  or leak from source.
- `SystemDisplayAndWindowHooks` — `currentTouchTime`, `currentTouchX`,
  `currentTouchY` (primitive long/float additional instance fields on
  View), `mCustomBlurModifier` (int additional instance fields on
  notificationShadeDepthController / mControlPanelWindowManager /
  mBlurUtils). Owner lifetime depends on MIUI SystemUI object graph.
  Primitive fields are low-retention but owner lifetime is ROM-dependent.
- `SystemStatusBarMoreHooks` — additional instance fields and Handler
  on system_server status bar objects. ContentObserver is
  LIFECYCLE_MANAGED but additional fields need ROM evidence.
- `SystemUIMonitorAndTileHooks` — additional instance fields and
  Handler on SystemUI tile/monitor objects. ContentObservers are
  LIFECYCLE_MANAGED but additional fields need ROM evidence.
- `XposedHelpers` instance fields (mlp-00439, mlp-00440, mlp-00444,
  mlp-00445) — owner lifetime depends on hooked framework object
  lifecycle.

### C. DEFERRED_LOW_VALUE

- `Helpers` app-list caches (`installedAppsList`, `launchableAppsList`,
  `openWithAppsList`, `shareAppsList`) — Kotlin object fields,
  PROCESS_LIFETIME_METADATA_COLLECTION, LOW risk. Process-lifetime
  metadata cache with `invalidateAppCaches()` eviction path. Intentional
  process-lifetime retention. Not a leak.
- `Helpers` `memoryCache` (LruCache<String, Bitmap>) — bounded LruCache
  with `coerceIn(512, 8*1024)` KB limit and `evictAll()` eviction path.
  Already registration-managed and bounded. Not a leak.
- `SubFragment` `pendingHighlightScroll` (mlp-00011) — already fixed
  in P2-1. BOUNDED_DELAYED_CALLBACK_RETENTION, now View-lifecycle
  cancellable.
- `Controls` / `GlobalActions` broadcast receivers (mlp-00025, 00050,
  00051) — BOUNDED_REPLACEMENT_RETENTION, LOW risk. Process-lifetime
  intentional broadcast receivers with replacement semantics.
- `LauncherIconHooks` listener (mlp-00064) — VIEW_LIFETIME_OWNED_LISTENER,
  LOW risk. Listener owned by View lifetime.
- `SystemUILockScreenHooks` listener (mlp-00170) —
  BOUNDED_REPLACEMENT_RETENTION, MEDIUM risk but LIFECYCLE_MANAGED.
  Already reviewed.
- `LockScreenAlbumArtController` LruCache (mlp-00380) —
  SAFE_STABLE_METADATA, INFO risk. Bounded cache.

## P2 continuation decision

```
IS THERE ANY REMAINING STATIC_ACTIONABLE P2 CANDIDATE
WITH MATERIAL BENEFIT?
```

Answer: **NO**

```
P2 = STATIC_STAGE_COMPLETE
P2-4 = DEFERRED / NOT_STARTED
```

No P2-4 will be created. The remaining MEDIUM-risk candidates all
require device/ROM evidence to prove owner mismatch and retention
duration. Static source analysis has exhausted its actionable surface.

## Device evidence still pending

- P0 runtime baseline = pending device
- P1B device smoke evidence = pending
- P1B-4A ROM lifecycle evidence = pending
- P2 device/runtime memory evidence = pending

No RAM/PSS/GC improvement claims are made. Static closure does not
equate to device-verified memory leak elimination.

## Recommended next stage

Not started. Candidates for future prioritization:

- A. Device smoke/runtime evidence campaign
- B. P3 CPU/GC/hot-path audit
- C. P6 modularization boundary audit
- D. BUILD-1B AGP 9.4 compatibility bump

Priority to be determined by closure results and user direction.

## Validation evidence

- Python tests: 1226 OK (skipped=2)
- memory lifecycle scan: Deterministic PASS
- source hazard scan: 0 findings
- hook cost scan: Stability check passed (no drift)
- verify.py full: PASS (check-invariants, check-compat-contracts,
  check-hook-contract-parity, compileDebugKotlin, compileDebugJavaWithJavac,
  testDebugUnitTest, lintDebug)
- Debug compile + JVM tests: BUILD SUCCESSFUL
- Release compile + lintRelease + minifyReleaseWithR8: BUILD SUCCESSFUL
- lintVitalAnalyzeRelease + lintVitalReportRelease: BUILD SUCCESSFUL
- analyzeReleaseR8Config: BUILD SUCCESSFUL
- R8 output: mapping.txt, usage.txt, seeds.txt, configuration.txt present;
  no missing_rules.txt (no missing rules); no fatal R8 warnings
- git diff --check: PASS

## Toolchain

- Gradle: 9.6.1
- AGP: 9.3.1
- JVM: 25 (OpenJDK 25.0.4, Eclipse Adoptium)
