# A13 Runtime Hardening

> Branch: `devin/a13-runtime-hardening`
> Reviewed code baseline: `devin/a13-runtime-hardening` tip
> Date: 2026-08-01


## Status words

| Word | Meaning |
|---|---|
| `COMPLETED` | Code, unit tests and `verify.py full` / `lintDebug` pass in the branch. |
| `VERIFIED_STATIC` | Code and static evidence (unit tests / `check-invariants.py`) are in place; runtime evidence is static only. |
| `PARTIAL` | Implementation is in place with a documented, acceptable remaining gap. |
| `DEFERRED_EXTERNAL` | Requires a real device, release build or other external validation; not possible in this round. |

## Current implementation summary

| Area | Status | Evidence | Notes |
|---|---|---|---|
| `AGENTS.md` current rules | COMPLETED | `AGENTS.md` updated for `devin/a13-runtime-hardening`, no ADB/APK |
| `tools/verify.py` | COMPLETED | `fast` / `full` modes; no APK build |
| `tools/check-invariants.py` | COMPLETED | static source invariants |
| `tools/analyze_lsposed_log.py` | COMPLETED | offline log analyzer only |
| `.github/workflows/build.yml` | COMPLETED | `check-invariants.py` + `verify.py full`; no APK |
| `PrefMap` atomic snapshot + typed getters | COMPLETED | `PrefMap.kt`, `PrefMapTest.kt`; map-style `get` and `in` normalize keys |
| `PreferenceBootstrap` state machine | COMPLETED | `PreferenceBootstrap.java`, `PreferenceBootstrapTest.kt` |
| `ModuleHelper` receiver lifecycle | COMPLETED | two-phase module/owned registration, stale tracking with bound, identity checks; `ModuleHelperReceiverTest.kt` |
| `ModuleHelper.guarded` | COMPLETED | rethrows `OutOfMemoryError`; `check-invariants.py` enforces callback guarding |
| `FeatureDispatcher` typed IDs | COMPLETED | `FeatureId.kt`, `FeatureDispatcher.kt`, `FeatureCatalogTest.kt` |
| `ResourceHooks` active cache | COMPLETED | `ResourceHooks.java`; active cache checked before Context lookup; `applyHooks` idempotent |
| `MainModule` per-process installer split | VERIFIED_STATIC | `SystemServerInstaller`, `SystemUiInstaller`, `LauncherInstaller`, `PackageInstallerRouter` |
| `PackageInstallerRouter` return semantics | VERIFIED_STATIC | input method block returned to `MainModule` so `various_alarmcompat` runs for non-input packages |
| `mods/` callback boundaries | VERIFIED_STATIC | `ModuleHelper.guarded` present; manual pass over anonymous callbacks still recommended |
| Hot paths (network, clock, notification, launcher) | VERIFIED_STATIC | no `getArgsArray` without rewrite, no regex split on literal, callbacks guarded |
| Release build | DEFERRED_EXTERNAL | requires out-of-tree keystore and real device |
| Real-device LSPosed validation | DEFERRED_EXTERNAL | `PreferenceBootstrap` listener protocol, `ResourceHooks` active cache, `PackageInstallerRouter` routing need logs |

## Local verification

| Check | Command | Result |
|---|---|---|
| Invariants | `python tools/check-invariants.py` | PASS |
| Kotlin compile | `gradlew :app:compileDebugKotlin` | PASS |
| Java compile | `gradlew :app:compileDebugJavaWithJavac` | PASS |
| Unit tests | `gradlew :app:testDebugUnitTest` | PASS |
| Lint | `gradlew :app:lintDebug` | PASS |
| APK build | intentionally not run | N/A |

## Runtime inventory

Source-level steady-state cost checklist. Each row states the current evidence; items marked `PENDING` are still being audited.

| Component | Trigger frequency | Disabled cost | Enabled cost | Resident objects | Periodic work | Key risk |
|---|---|---|---|---|---|---|
| `ResourceHooks` | Very high | Zero if not created | Fixed `kind` per `Resources` method; no `Executable.getName()`; no method-name string comparison; active `SparseArray` bounded at `MAX_ACTIVE=256`; install bit mask (`14` methods, `0x3fff`) | `fakes`, `unresolved`, `active` | None | ID replacement still needs Module Resources; device log needed |
| `PreferenceBootstrap` | Low | Zero if `initPrefs` not called | One `OnSharedPreferenceChangeListener`; snapshot published once | `snapshot` `PrefMap` | No polling; event driven | Listener `getAll()` may rethrow on remote error; device log needed |
| `ModuleHelper` module receivers | Cold | Zero | One active `ReceiverRegistration` per key; stale bounded at `MAX_STALE_RECEIVERS=3`; empty stale deque removed by identity | `moduleReceivers`, `staleModuleReceivers` | None; `stale` retried on next registration | Concurrent identity race under per-`moduleReceivers` lock |
| `ModuleHelper` owned receivers | Cold | Zero | `OwnedReceiverBucket` per key; per-bucket lock; re-check `ownedReceivers.get(key) == bucket` before adding; `WeakReference<owner>` + `OwnedReceiverRegistration` | `ownedReceivers`, `staleOwnedReceivers` | None; cleaned on next dispatch if owner GCed | Detached-bucket race fixed; stale deque identity removal |
| `FeatureDispatcher` | Process start | Zero if features disabled | One install per enabled `FeatureId` | `FeatureCatalog` singleton | None | Need disabled-feature no-op proof |
| `StatusBar clock` | Per second | Needs audit | Needs audit | Needs audit | `secondTicker` `Runnable`? | Periodic UI refresh while screen off |
| `Network speed` | Periodic | Needs audit | Needs audit | Needs audit | `NetworkSpeed` callback | sysfs/network I/O schedule |
| `DeviceInfoMonitor` | Periodic | Needs audit | Needs audit | Needs audit | Temperature/current tick | Wake/refresh alignment |
| `Launcher` hooks | High | Needs audit | Needs audit | Needs audit | `launcher` events | Object allocation on every layout pass |
| `Album art` | Event driven | Needs audit | Needs audit | Needs audit | Media broadcast | Bitmap unbounded / full-screen ARGB |
| `BatteryIndicator` | Event/periodic | Needs audit | Needs audit | Needs audit | Level/tick | Listener lifecycle |
| `AudioVisualizer` | Per frame | Needs audit | Needs audit | Needs audit | Visualizer callback | Per-frame allocation |
| `Diagnostics` | Error events | Zero if no errors | `CopyOnWriteArraySet` / `Map` | diagnostic ids | None | Need explicit capacity bound |

*Items marked `Needs audit` are not yet measured and are tracked in the remaining-risks list below.*

## P1 extreme-performance closeout

| Item | Status | Evidence | Notes |
|---|---|---|---|
| ResourceHooks fixed method kind | COMPLETED | `ResourceHooks.java` uses `int kind`; no `Executable.getName()`; bit-mask install tracking (`installedMask`); `ResourceHooksTest` | OBJECT/DENSITY/ID routing by kind; active hit avoids Context lookup and resource-name parsing |
| ResourceHooks partial install recovery | COMPLETED | `applyHooks()` `PARTIAL_FAILED` / `INSTALLED` state; `installedMask` CAS per method; `ResourceHooksTest` idempotence + concurrent | Retry only missing bits; no double-hook |
| Owned receiver detached-bucket race | COMPLETED | `OwnedReceiverBucket`; `synchronized(bucket)`; re-check `ownedReceivers.get(key) == bucket`; identity remove on empty | `ModuleHelperReceiverTest` concurrency test |
| Stale receiver identity remove | COMPLETED | `drainModuleStale` / `drainOwnedStale` use `stale*.remove(key, deque)` after draining | Prevents leaving empty stale containers |
| Periodic tasks (clock, network, device info, battery, audio, album art) | PENDING | Not yet audited | `postDelayed` / `Handler` instances need stop-on-screen-off and disabled-zero-start review |
| High-frequency callback allocations | PENDING | Not yet audited | `Regex`, `String.format`, `StringBuilder`, `ArrayList`, `lambda` in hot paths need pass |
| Bitmap / large object budgets | PENDING | `AlbumArtPolicy.kt` has `CACHE_BUDGET_FRAMES` | Need in-flight task and View strong-reference audit |
| Disabled feature zero-cost | VERIFIED_STATIC | `FeatureDispatcher` checks `runtime.prefs` before `installWithContract` | `SystemServerInstaller` / `SystemUiInstaller` / `LauncherInstaller` already gate by process + prefs |
| Diagnostics bounded | COMPLETED | `DiagnosticRecorder` `MAX_SNAPSHOTS=32`, `MAX_DETAIL_LENGTH=512`, `THROTTLE_MS=60_000` | Already fixed capacity and throttling |

## Remaining risks

1. **Device validation** — `PreferenceBootstrap` listener protocol and `ResourceHooks` active cache need real LSPosed logs.
2. **APK / Release build** — not run; out-of-tree signature required.
3. **Manual `mods/` callback audit** — `ModuleHelper.guarded` and `check-invariants.py` cover structure; a manual pass over anonymous `BroadcastReceiver`, `ContentObserver`, `Runnable` and view listeners is still recommended before a major release.
