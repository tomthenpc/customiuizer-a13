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
| `tools/verify.py` | COMPLETED | `fast` / `full` modes; invariant and compatibility-contract fail-fast gates; no APK build |
| `tools/check-invariants.py` | COMPLETED | static source invariants |
| `tools/check-compat-contracts.py` | COMPLETED | selected-variant, callback-independence and OOM boundaries; invoked by `verify.py` |
| `tools/analyze_lsposed_log.py` | COMPLETED | offline log analyzer only |
| `.github/workflows/build.yml` | REMOVED | local `tools/verify.py` orchestrates invariant, compatibility, compile, unit-test and lint gates; no cloud CI |
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
| Compatibility contracts | `python tools/check-compat-contracts.py` | PASS |
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
| `ModuleHelper` module receivers | Cold | Zero | One active `ReceiverRegistration` per key; stale bounded at `MAX_STALE_RECEIVERS=3`; empty stale deque removed by identity | `moduleReceivers`, `staleModuleReceivers` | None; stale entries retried on next registration | Register-new-before-release-old ordering and failed-replacement preservation covered by deterministic tests; OOM rethrown |
| `ModuleHelper` owned receivers | Cold | Zero | `OwnedReceiverBucket` per key; per-bucket lock; re-check `ownedReceivers.get(key) == bucket` before adding; `WeakReference<owner>` + `OwnedReceiverRegistration` | `ownedReceivers`, `staleOwnedReceivers` | None; cleaned on next dispatch if owner GCed | Same-owner failed replacement preserves the active registration; detached-bucket/stale identity races covered by tests; OOM rethrown |
| `FeatureDispatcher` | Process start | Zero if features disabled | One install per enabled `FeatureId` | `FeatureCatalog` singleton | None | Need disabled-feature no-op proof |
| `StatusBar clock` | Per second | Needs audit | Needs audit | Needs audit | `secondTicker` `Runnable`? | Periodic UI refresh while screen off |
| `Network speed` | Periodic | Needs audit | Needs audit | Needs audit | `NetworkSpeed` callback | sysfs/network I/O schedule |
| `DeviceInfoMonitor` | Periodic while screen on | Zero when both master features are disabled | One sysfs pass per ROM-looper tick; failures back off from 2s to 60s | Two Handlers, one snapshot, two text states, one receiver | Reuses `NetworkSpeedController` looper; no module thread | ROM looper identity and sysfs availability need device-log confirmation |
| `Launcher` hooks | High | Needs audit | Needs audit | Needs audit | `launcher` events | Object allocation on every layout pass |
| `Album art` | Event driven | Zero without media art; worker starts only with attached owner and screen on | One serialized decode/transform per latest source/parameter/size generation | One-frame output cache; 512x512 blur input cap; private intermediates recycled | Media metadata + owner/screen lifecycle | Legacy MIUI installer targets and device peak RSS need confirmation |
| `BatteryIndicator` | Event driven; 20ms only during explicit test animation | Zero when feature is disabled | One draw update per state change; deferred View updates are coalesced | One View; lazy rainbow palettes; cached evaluator and shapes | SystemUI battery/power/dark/layout events | Duplicate `createAndAddWindows` installation still needs device-log confirmation |
| `AudioVisualizer` | FFT capture + display frames while actively playing | Zero when feature is disabled; capture and frame scheduler stop when hidden/detached | Fixed 31-band scan and array copy; one coalesced main-thread frame request | Fixed primitive arrays, one frame callback/Runnable, lazy worker threads with 15s timeout | Visualizer FFT callback + Choreographer | Legacy MIUI installer targets still need Contract/HyperOS evidence |
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
| High-frequency callback allocations | PARTIAL | `CLOCK_HOUR_PATTERN` precompiled; network-speed suffix removal uses a zero-allocation-on-miss character scan; `ResourceHooks` no hot `Executable.getName()` | `Regex`, `String.format`, `StringBuilder`, `ArrayList`, `lambda` in remaining hot paths need pass |
| Bitmap / large object budgets | VERIFIED_STATIC | album-art blur input is capped at 512x512; cache is capped to one output frame; cancelled generations remove queued publication, recycle private intermediates and cannot repopulate a cleared cache; module-owned View backgrounds are cleared by exact Drawable identity | Device peak RSS and ROM Bitmap ownership still need measurement |
| Disabled feature zero-cost | VERIFIED_STATIC | `FeatureDispatcher` checks `runtime.prefs` before `installWithContract` | `SystemServerInstaller` / `SystemUiInstaller` / `LauncherInstaller` already gate by process + prefs |
| Diagnostics bounded | COMPLETED | `DiagnosticRecorder` `MAX_SNAPSHOTS=32`, `MAX_DETAIL_LENGTH=512`, `THROTTLE_MS=60_000` | Already fixed capacity and throttling |

## Periodic-work inventory (P1-B)

| Component | Trigger | Start condition | Stop condition | Screen off | View detach | Feature off | Repeat init | Notes |
|---|---|---|---|---|---|---|---|---|
| `SystemStatusBarClockAndMoreHooks` second ticker | `SCREEN_ON` / `TIME_SET` / `TIMEZONE_CHANGED` | `isScreenOn() && (ccShowSeconds \|\| sbShowSeconds)` | `SCREEN_OFF` or seconds disabled | stops via `stopSecondTimer()` | controller GC releases `WeakReference`s | `FeatureDispatcher` gate for clock tweaks | `removeCallbacks` before each `postDelayed`; `ClockRunnable` reused | one `Handler` + one `ClockRunnable` per controller; `SecondTickerState` generation guards old ticks |
| `DeviceInfoMonitor` temperature/current tick | preference + screen state | `start(enabled, interactive)` | `stop()` / `snapshot.enabled = false` | `screenOn = enabled && interactive` | owner `WeakReference` | `DeviceInfoMonitor` feature pref | single `snapshot` per update | already gated by enabled flag and screen |
| `StepCounterController` step updates | `screenOn` lifecycle + `SensorManager` | `scheduleUpdate()` if `lifecycle.screenOn` | lifecycle off | stops | not applicable | step feature pref | pending flag / `removeCallbacks` | uses `StepCounterLifecycle` with `screenOn` |
| `AudioVisualizer` capture | visualizer FFT + Choreographer while displaying | attached + visible + window visible + playback | any display gate closes or View detaches | not applicable | frame request/callback removed; color/palette work cancelled; Visualizer detached and asynchronously released | visualizer feature pref | generation token; one atomic coalesced main-thread frame request; fixed primitive buffers | scheduling lifecycle is statically covered; Visualizer release and legacy target bundle need device-log confirmation |
| `LockScreenAlbumArtController` decode | media art/parameter/size change | enabled + non-null art + attached owner + screen on | media clear, screen off, owner detach, size/source generation change or missing ROM capability | not applicable | cancels queued work/publication; clears one-frame cache and the exact module-owned Drawable; private intermediate Bitmaps are recycled | lock-screen album art pref | one bounded worker queue; generation + interruption gate; tokenized main publication | static large-object lifecycle complete; legacy MIUI target bundle and peak RSS need device-log/device confirmation |
| `Network speed` | ROM `updateNetworkSpeed` / `postUpdateNetworkSpeedDelay` | MIUI `NetworkSpeedController` is already ticked by the system; module hooks `updateNetworkSpeed` and `updateText` | ROM stops its own updater; module does not add a second cycle | N/A (ROM owned) | `NetSpeedStyleHook` constructor attaches `OnAttachStateChangeListener` to remove the 200ms style init `Runnable` on detach | `FeatureDispatcher` gate for `NetSpeedStyleHook` / `DetailedNetSpeedHook` / `NetSpeedIntervalHook` | one-shot 200ms `postDelayed` per `NetworkSpeedView` | module only reformats the text produced by ROM; `updateText` reuses cached `unitSuffix` and `speedChars` per locale |
| `BatteryIndicator` | preference observer + battery/power/dark/layout events; explicit test uses one `View.postDelayed` chain | feature installed and View attached | View detached or test completes | not applicable | detach unregisters observer/receiver and removes every owned Runnable | `BatteryIndicator` pref | stable coalesced Runnables; weak `CentralSurfaces` reference | static lifecycle hardening complete; duplicate window installation and detach behavior need device-log confirmation |

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

The module does not add a periodic network-speed updater. The ROM still drives the tick; the module adds per-tick tx/rx sampling, connection state checks and display formatting. The delayed style fallback is cancellable on `View` detach, and a per-View completion tag prevents `onFinishInflate` plus the delayed fallback from applying the full style twice. Locale/pref caching remains pending.

## P1-B.3.1 closeout progress

| Item | Status | Notes |
|---|---|---|
| P0-1 Clock timezone / generation / mContext | COMPLETED | `TIMEZONE_CHANGED` / `TIME_CHANGED` consult real screen state; `ClockLifecycleAction`; receiver registration failure stops ticker |
| P0-2 Network speed per-controller / one-sample | PARTIAL | `getTotalByte` hook removed; per-controller `NetSpeedRuntimeState` via `AdditionalInstanceField`; one `getTrafficBytes` per tick; no `Pair`; disconnect resets the full sampling baseline; style initialization commits once per View; locale/pref caching remains pending |
| P0-3 StepCounter query token / lifecycle | VERIFIED_STATIC | `QueryTicket(generation, queryId)`; `Lifecycle.canPublish` single atomic check; `PendingQuerySlot` identity-based; all `QueryRunnable` paths enter `finally`; terminal `queryHandler`/`uiHandler` posts cleaned; `Lifecycle`/`View` thread races covered by tests |
| P0-4 DeviceInfo lifecycle | VERIFIED_STATIC | reuses the ROM-provided `NetworkSpeedController` looper; screen-off removes monitor/UI messages; generation + snapshot identity reject stale I/O publication and duplicate scheduling; bounded failure backoff; OOM rethrown |
| P1-1 BatteryIndicator lifecycle | VERIFIED_STATIC | `CentralSurfaces` reference is weak; preference/layout/test Runnables are stable and cancelled on detach; detached Views reject draw work; observer and receiver remain owner-scoped; rainbow palettes, evaluator and shapes are reused; OOM is rethrown; duplicate window installation still needs device-log confirmation |
| P1-2 AudioVisualizer scheduling | VERIFIED_STATIC | FFT capture reuses fixed buffers and posts one stable, atomically coalesced frame request; detach cancels both queued request and Choreographer callback; detached state is cross-thread visible; rainbow/random HSV buffers and lockscreen path are reused; OOM is rethrown; lifecycle tests cover the single callback/request structure |
| P1-3 Album Art large-object lifecycle | VERIFIED_STATIC | cache budget reduced from two full output frames to one; blur input remains capped at 512x512; cache access/clear is serialized; stale/interrupted work cannot publish or repopulate cache; queued main publication is removable; module-owned BitmapDrawable is weakly tracked and cleared by identity; private downsample/blur/result intermediates are recycled when not retained |
| P1-4 Receiver deterministic ordering test | VERIFIED_STATIC | module and owned receiver tests cover register-before-replace ordering, failed replacement preserving the active receiver, concurrent same-key replacement/unregister, detached bucket retry, stale unregister drain, empty-bucket removal and OOM propagation |
| P1-5 Final hot-path scan and docs | IN_PROGRESS | this section |

## A13-H1 HyperOS 1 / Android 13 兼容基线

- H1.1b 静态闭环：VERIFIED_STATIC
- H1.1c 严格版本解析与诊断失败隔离：VERIFIED_STATIC
- H1.2a A14 静态对照：部分完成
  - 完成 A14参考分支 `devin/a14-runtime-hardening` SHA `f4ef55f034dee52625d7496a4cd51093ad113bb3` 的只读审计
  - 完成 3 个 `system_server` Canary 静态矩阵：`PACKAGE_PERMISSIONS`、`AUTO_BRIGHTNESS_RANGE`、`MUFFLED_VIBRATION`
  - 三个 Feature 均评为 S2（REFERENCE_ONLY），`deviceVerified=false`
  - 未引入任何生产 Hook fallback
- H1.2b 剩余 Canary 与完整 variant机制：PENDING
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
- H1.2a `system_server` Canary 静态对照：VERIFIED_STATIC
  - A14参考分支 `devin/a14-runtime-hardening` SHA `f4ef55f034dee52625d7496a4cd51093ad113bb3` 只读审计
  - `PACKAGE_PERMISSIONS`、`AUTO_BRIGHTNESS_RANGE`、`MUFFLED_VIBRATION` 均评为 S2
  - `deviceVerified=false` 保留
  - 未引入任何生产 Hook fallback
- H1.2b 剩余 SystemUI Canary 静态对照：VERIFIED_STATIC
  - A14 当前远程 HEAD `9302ae552c9b3c3e5bd6ef16ecbdfd76f54b9799`；审计固定引用 `f4ef55f034dee52625d7496a4cd51093ad113bb3`
  - 完成 3 个 SystemUI Canary 静态矩阵：`STATUS_BAR_CLOCK_TWEAK`、`NO_MORE_ICON`、`BATTERY_INDICATOR`
  - 三个 Feature 均评为 S2（REFERENCE_ONLY），`deviceVerified=false`，无 S1 证据
  - `NO_MORE_ICON` 在 A14 当前参考中缺失（`A14_CURRENTLY_ABSENT`）
  - `DiagnosticRecorder` logger 异常隔离：普通 `RuntimeException` 不阻断 `FeatureDispatcher.installById`；`OutOfMemoryError` 重新抛出
  - 新增 `FeatureDispatcherRegressionTest` 与 `CanaryContractAuditTest`
- H1.2c Launcher Canary 静态对照：VERIFIED_STATIC
  - A14 参考固定 `9302ae552c9b3c3e5bd6ef16ecbdfd76f54b9799` 只读审计
  - `NO_CLOCK_HIDE` 与 `NO_WIDGET_ONLY` 均评为 S0（A13/A14 target 完全相同）
  - 生产 fallback：无
- 原子 `FeatureTargetVariant` 机制：VERIFIED_STATIC
  - `HookTargetContract` 支持 `FeatureTargetVariant`
  - `HookTargetResolver` 按固定优先级评估完整 variant，选中即停止，禁止跨 variant 混合
  - `HookInstaller.withSession` 仅记录选中 variant 的 target
  - `AUTO_BRIGHTNESS_RANGE` 拆为 `automatic_brightness_controller` 与 `display_power_controller` 两个原子 variant
- `HookTargetResolver` fatal/OOM 边界：VERIFIED_STATIC
  - 直接 `OutOfMemoryError` 重新抛出
  - `InvocationTargetException` / `ExceptionInInitializerError` 包装 OOM 解包后重新抛出
  - OOM 不写 negative cache
  - 缓存硬上限 128
- H1.2d 生产 Variant 执行绑定：VERIFIED_STATIC
  - `FeatureDispatcher.installWithContractVariant` 显式传入 `selectedVariant`
  - `AUTO_BRIGHTNESS_RANGE` 真实 Hook 只安装 resolver 选中的完整 variant
  - 未选 variant 不发生 `ModuleHelper` 调用
  - `installWithContract` / `installWithLegacyCheck` 重新抛出 `OutOfMemoryError`
  - `DiagnosticRecorder` fallback logger 在 OOM 时重新抛出，普通异常不阻断安装
- H1.2e `HookInstaller` 选择结果严格边界：VERIFIED_STATIC
  - 多 variant contract 缺 `compatibilityResult.selectedVariant` 显式失败
  - 单 variant legacy 路径仍可用
  - 校验 selected variant 属于当前 contract 且 target 数量一致
- H1.2f 真实生产入口 anti-mixing 测试：VERIFIED_STATIC
  - `AutoBrightnessVariantExecutionTest` 覆盖 ABC/DPC 独占安装、部分 bundle 零安装、失败后不切换、OOM 传播与 Session 清理
- H1.2g 兼容框架静态检查：VERIFIED_STATIC
  - `tools/check-compat-contracts.py` 检查 variant 参数传递、callback 独立性与 OOM 边界
- H1.2 总状态：VERIFIED_STATIC
  - 8 个 Canary 静态审计矩阵完整
  - 无生产 HyperOS fallback
  - 所有 Canary 保持 `deviceVerified=false`
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
