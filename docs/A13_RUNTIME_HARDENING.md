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
| `.github/workflows/build.yml` | REMOVED | local `tools/verify.py` + `tools/check-invariants.py` remain the only gates; no cloud CI |
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
| Stale receiver refill race | COMPLETED | `addToStale` / `drainStale` share `synchronized(deque)` and re-check `staleMap.get(key) == deque` | `ModuleHelperReceiverTest.staleModule*` drains and identity removes |
| Status-bar second ticker | COMPLETED | `SystemStatusBarClockAndMoreHooks` `ClockRunnable` per generation; `TickerScheduler` abstraction; `SecondTickerState` `scheduledGeneration` + `callbackPending`; `startOrRestartSecondTicker` / `stopSecondTimer` / `scheduleNextSecondTick` invariants | `SystemStatusBarClockAndMoreHooksTest` state machine + `FakeTickerScheduler`; old callback cannot repost after new generation; one pending callback per controller |
| High-frequency callback allocations | PARTIAL | `CLOCK_HOUR_PATTERN` precompiled; `ResourceHooks` no hot `Executable.getName()` | `Regex`, `String.format`, `StringBuilder`, `ArrayList`, `lambda` in remaining hot paths need pass |
| Bitmap / large object budgets | PARTIAL | `AlbumArtPolicy.kt` has `CACHE_BUDGET_FRAMES` and `BLUR_MAX_PIXELS`; `DiagnosticRecorder` bounded | In-flight task / View strong-reference audit pending |
| Disabled feature zero-cost | VERIFIED_STATIC | `FeatureDispatcher` checks `runtime.prefs` before `installWithContract` | `SystemServerInstaller` / `SystemUiInstaller` / `LauncherInstaller` already gate by process + prefs |
| Diagnostics bounded | COMPLETED | `DiagnosticRecorder` `MAX_SNAPSHOTS=32`, `MAX_DETAIL_LENGTH=512`, `THROTTLE_MS=60_000` | Already fixed capacity and throttling |

## Periodic-work inventory (P1-B)

| Component | Trigger | Start condition | Stop condition | Screen off | View detach | Feature off | Repeat init | Notes |
|---|---|---|---|---|---|---|---|---|
| `SystemStatusBarClockAndMoreHooks` second ticker | `SCREEN_ON` / `TIME_SET` / `TIMEZONE_CHANGED` | `isScreenOn() && (ccShowSeconds \|\| sbShowSeconds)` | `SCREEN_OFF` or seconds disabled | stops via `stopSecondTimer()` | controller GC releases `WeakReference`s | `FeatureDispatcher` gate for clock tweaks | `removeCallbacks` before each `postDelayed`; `ClockRunnable` reused | one `Handler` + one `ClockRunnable` per controller; `SecondTickerState` generation guards old ticks |
| `DeviceInfoMonitor` temperature/current tick | preference + screen state | `start(enabled, interactive)` | `stop()` / `snapshot.enabled = false` | `screenOn = enabled && interactive` | owner `WeakReference` | `DeviceInfoMonitor` feature pref | single `snapshot` per update | already gated by enabled flag and screen |
| `StepCounterController` step updates | `screenOn` lifecycle + `SensorManager` | `scheduleUpdate()` if `lifecycle.screenOn` | lifecycle off | stops | not applicable | step feature pref | pending flag / `removeCallbacks` | uses `StepCounterLifecycle` with `screenOn` |
| `AudioVisualizer` capture | visualizer / per frame | `onAttachedToWindow` if enabled | `onDetachedFromWindow` release | not applicable | `release()` on detach | visualizer feature pref | session token + `WeakReference` | audit not yet completed; Visualizer release lifecycle needs explicit test |
| `LockScreenAlbumArtController` decode | media change | enabled + active media | `stop()` / controller destroyed | not applicable | release `View`/Drawable | lock-screen album art pref | in-flight `Future` per cache key; `generation` token | `AlbumArtPolicy` budgets bytes; `inFlight` cleanup pending |
| `Network speed` | ROM `updateNetworkSpeed` / `postUpdateNetworkSpeedDelay` | MIUI `NetworkSpeedController` is already ticked by the system; module hooks `updateNetworkSpeed` and `updateText` | ROM stops its own updater; module does not add a second cycle | N/A (ROM owned) | `NetSpeedStyleHook` constructor attaches `OnAttachStateChangeListener` to remove the 200ms style init `Runnable` on detach | `FeatureDispatcher` gate for `NetSpeedStyleHook` / `DetailedNetSpeedHook` / `NetSpeedIntervalHook` | one-shot 200ms `postDelayed` per `NetworkSpeedView` | module only reformats the text produced by ROM; `updateText` reuses cached `unitSuffix` and `speedChars` per locale |
| `BatteryIndicator` | `observePreferenceChange` + `viewScope.launch` | enabled | view detached | needs audit | `viewScope` cleared | `BatteryIndicator` pref | single `viewScope` | lifecycle audited but needs explicit View detach test |

*Components marked `needs audit` are tracked in the remaining-risks list below.* |

## P1-B.2 clock scheduling design

The status-bar second ticker now uses these invariants (P0-1 complete):

1. `tickerGeneration` is an `AtomicLong`; `nextGeneration()` is a strict counter, not a time measurement.
2. `SecondTickerState.start(newGen)` increments `generation` and sets `scheduledGeneration`; every `ClockRunnable` is created with a fixed `scheduledGen`.
3. A running `ClockRunnable` enters `callbackPending = false` at the beginning of `run()`.
4. The UI update is wrapped in `ModuleHelper.guarded`; non-OOM exceptions are logged and the ticker continues to the next scheduling check.
5. After the update, the callback can only repost if `state.canRePost(scheduledGen)` is true, which requires `screenOn`, `running`, `!callbackPending` and `generation == scheduledGen`.
6. `startOrRestartSecondTicker` removes any pending old `ClockRunnable`, starts a new generation and posts exactly one new `ClockRunnable`; if `mContext` cannot be read it stops the ticker state instead of leaving it half-armed.
7. `stopSecondTimer` removes the pending callback and sets `running = false`, `callbackPending = false`.

This means:
- `SCREEN_OFF` sets `screenOn = false` and removes the pending callback; pending count becomes 0.
- `SCREEN_ON` / `TIME_SET` / `TIMEZONE_CHANGED` creates a new generation and posts one new callback; at most one pending callback per controller.
- A normal exception during the tick does not kill the loop; the next scheduling check still runs and, if generation matches, posts the next tick.
- `OutOfMemoryError` propagates through `ModuleHelper.guarded`.

The `ClockRunnable.doTick()` still uses `XposedHelpers.getObjectField` for `mCalendar`, `mIs24` and `mClockListeners`. These remain functional necessary reflection on the MIUI controller. No new `Runnable` is created per tick; one `ClockRunnable` is created per `startOrRestart` (lifecycle event), not per second.

## P1-B.2 step counter audit

`StepCounterController` (P0-3 VERIFIED_STATIC):

* `Lifecycle` uses a single `synchronized` lock to protect the combined state (`screenOn`, `hasViews`, `timeTickRegistered`, `generation`, `nextQueryId`, `activeTicket`, `latestValidQueryId`).
* `QueryTicket(generation, queryId)` identifies each query and its validity window.
* `PendingQuerySlot` provides identity-based replacement and clearing of the single pending `QueryRunnable`.
* `Lifecycle.canPublish(ticket)` is the single atomic gate for publication; it checks `queryId`, `generation`, `screenOn` and `hasViews` under one lock.
* All `QueryRunnable` paths enter `try/finally`, calling `finishQuery(ticket)` and `clearPendingQuery(thisRunnable)` on every exit.
* `scheduleUpdate()` calls `abortQueryStart()` on `queryHandler` null/false/exception/OOM, cleaning the pending slot, active ticket and result validity.
* `postResult(ticket, text)` handles `uiHandler` null/false/exception/OOM and calls `consumeResult(ticket)` on every non-success path.
* `updateViews(ticket, newText)` checks `canPublish` before and after taking the `liveViews()` snapshot; it consumes the result on every path and does not modify `TextView` or `stepsWithGoal` for stale tickets.
* `querySteps()` `Throwable` path rethrows `OutOfMemoryError` and logs throttled class names for other exceptions.
* `stepViews` is protected by `viewLock`; `liveViews()` returns a snapshot and is never called from the background query thread.
* The data source is `ContentResolver.query`, not `SensorManager`.

待验证：真实 `ContentResolver`、真实 `HandlerThread`、`SystemUI` 屏幕/View 并发、`WeakReference` GC、`LSPosed` 实机。

## P1-B.2 network speed audit

Network speed is driven by the ROM `NetworkSpeedController`.

* `NetSpeedIntervalHook` only changes the argument passed to `postUpdateNetworkSpeedDelay`; it does not create a module Handler/Runnable.
* `DetailedNetSpeedHook` no longer hooks `getTotalByte`. `getTotalByte` hook has been removed.
* `DetailedNetSpeedHook` hooks `updateNetworkSpeed` and `updateText` to reformat the speed text produced by the ROM once per tick.
* `updateText` uses `MainModule.resHooks` for `network_speed_suffix` and a per-locale cache (`cachedUnitSuffix`, `cachedSpeedChars`) to avoid re-reading resources on every tick.
* `NetSpeedStyleHook` adds an `OnAttachStateChangeListener` to the `NetworkSpeedView`. When the view is detached, the listener removes the 200ms one-shot style init `Runnable`, preventing a detached view from being restyled.
* `initNetSpeedStyle` runs inside `ModuleHelper.guarded` so a single style failure is logged once and the update finishes.
* `NetSpeedRuntimeState` is stored per controller via `AdditionalInstanceField`; there is no global `txBytes`/`rxBytes` state.
* Per-tick traffic is sampled once for each interface; `Pair` return values and global speed state have been removed.
* The first tick after a network reconnect only establishes the new baseline and does not publish a value.

The module does not add a periodic network-speed updater. The ROM still drives the tick; the module adds per-tick tx/rx sampling, connection state checks and display formatting. The one-shot delayed style callback is now cancellable on `View` detach. Locale/pref cache and strict style one-shot remain pending.

## P1-B.3.1 closeout progress

| Item | Status | Notes |
|---|---|---|
| P0-1 Clock timezone / generation / mContext | COMPLETED | `TIMEZONE_CHANGED` / `TIME_CHANGED` consult real screen state; `ClockLifecycleAction`; receiver registration failure stops ticker |
| P0-2 Network speed per-controller / one-sample | PARTIAL | `getTotalByte` hook removed; per-controller `NetSpeedRuntimeState` via `AdditionalInstanceField`; one `getTrafficBytes` per tick; no `Pair`; reconnect baseline reset pending; locale/pref cache and strict style one-shot still pending |
| P0-3 StepCounter query token / lifecycle | VERIFIED_STATIC | `QueryTicket(generation, queryId)`; `Lifecycle.canPublish` single atomic check; `PendingQuerySlot` identity-based; all `QueryRunnable` paths enter `finally`; terminal `queryHandler`/`uiHandler` posts cleaned; `Lifecycle`/`View` thread races covered by tests |
| P0-4 DeviceInfo lifecycle | NOT_STARTED | dedicated I/O thread and stale generation pending |
| P1-1 BatteryIndicator lifecycle | NOT_STARTED | pending |
| P1-2 AudioVisualizer scheduling | NOT_STARTED | pending |
| P1-3 Album Art large-object lifecycle | NOT_STARTED | pending |
| P1-4 Receiver deterministic ordering test | NOT_STARTED | pending |
| P1-5 Final hot-path scan and docs | IN_PROGRESS | this section |

## A13-H1 HyperOS 1 / Android 13 兼容基线

- H1.1b 静态闭环：VERIFIED_STATIC
- H1.1c 严格版本解析与诊断失败隔离：VERIFIED_STATIC
- H1.2 A14 静态对照：PENDING

- `RomEnvironment` 数据结构：COMPLETED
- `SystemPropertyReader` 异常隔离：VERIFIED_STATIC
  - 缓存 `android.os.SystemProperties.get(String)` Method 一次
  - 普通反射异常降级为 `null`
  - 直接 / 包装 `OutOfMemoryError` 重新抛出
  - `API != 33` 时零 property 读取
- ROM 分类规则：VERIFIED_STATIC
  - HyperOS 1 证据优先于 MIUI V14 证据
  - 支持 `OS1` / `OS1.0.10.0` / `V14` / `V14.0.10.0` 等格式
  - 拒绝 `OS10` / `OS` / `V140` / `V13`
  - `UNSUPPORTED_ANDROID` 使用 `ANDROID_VERSION_UNSUPPORTED` reason
- `FeatureRuntime` cold-path 触发：VERIFIED_STATIC
  - `FeatureDispatcher.installWithContract` 首次读取 `runtime.environment`
  - disabled Feature 不触发检测
  - 同一 `FeatureRuntime` 只检测和记录一次
- 诊断测试隔离：VERIFIED_STATIC
  - `RomEnvironmentDetector` 不再调用 `DiagnosticRecorder`
  - `RomEnvironmentDiagnostics` 与分类分离
  - `FeatureCatalogTest` 不再依赖可变 `recordDiagnostics` 开关
- LSPosed ROM 日志解析：VERIFIED_STATIC
  - 保存 `state` / `compatibility` / `reason` / `detail`
  - 去重 key 包含 `compatibility`
  - Markdown/Text 摘要增加 ROM Environments 明细
- H1.2 A14 静态对照：PENDING
  - 需要以只读方式读取 A14 `mods/` 中对应 Hook 文件
  - 对 8 个 Canary 完成 S0–S3 分级
- HyperOS 生产 fallback：NOT_IMPLEMENTED
- HyperOS 实机验证：DEFERRED_EXTERNAL

> 当前仅能在运行时识别 `HYPEROS1_A13` 并输出诊断；没有任何 Feature 因 `HYPEROS1_A13` profile 而选择不同 Hook target。检测到 HyperOS 不等于功能兼容。`MIUI14_A13` 原路径没有改变。

| Profile             |  API | 当前行为    | Hook状态          | 实机状态              |
| ------------------- | ---: | ------- | --------------- | ----------------- |
| MIUI14_A13          |   33 | 原路径     | primary         | existing baseline |
| HYPEROS1_A13        |   33 | 环境识别与诊断 | no fallback yet | not verified      |
| UNKNOWN_A13         |   33 | 继续能力探测  | primary only    | unknown           |
| UNSUPPORTED_ANDROID | !=33 | 不支持     | none            | not applicable    |

## Remaining risks

1. **Device validation** — `PreferenceBootstrap` listener protocol and `ResourceHooks` active cache need real LSPosed logs.
2. **APK / Release build** — not run; out-of-tree signature required.
3. **Manual `mods/` callback audit** — `ModuleHelper.guarded` and `check-invariants.py` cover structure; a manual pass over anonymous `BroadcastReceiver`, `ContentObserver`, `Runnable` and view listeners is still recommended before a major release.
