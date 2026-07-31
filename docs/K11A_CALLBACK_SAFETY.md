# K11A Deferred Callback Safety

## Scope and method

K11A audits callbacks that execute after a libxposed `MethodHook` has returned.
The scan covers `BroadcastReceiver`, `Handler`, `Runnable`, posted lambdas,
`ContentObserver`, framework listeners, and the SystemUI `BatteryIndicator`.
Settings-only UI callbacks are recorded as out of the system-process boundary
and are not wrapped merely to increase the change count.

- Frozen base: `505c97e5d12b237a7022f77c0392a9771cff6cb6`
- Processes: `system_server`, `com.android.systemui`, `com.miui.home`, and the
  ordinary target process used by the Security Center sidebar hook.
- Runtime-scope candidates: 76
- Changed entries: 21
- Existing adequate boundary or no throwing work: 55
- Hook targets, priorities, registration order, delays, actions, preference
  keys, and `Chain.proceed()` counts: unchanged

## `ModuleHelper.guarded` assessment

`ModuleHelper` is a retained Java/libxposed boundary, so its functions are not
Kotlin source-level `inline`. The existing overloads already catch `Throwable`,
log only on failure, execute the body once, and let each return-valued caller
choose its fallback. K11A keeps those ABI-compatible overloads and adds named
variants. A single static logger instance avoids allocating a logger on the
normal path, and failure logs carry a stable callback name.

The named return overload does not choose a global fallback. For example,
Launcher `Handler.Callback` uses `false` so the framework treats a failed
callback as not consumed, while long-click actions preserve the action
handler's normal Boolean result.

## Changed callbacks

| File and callback | Process / thread | Throwing operations | Previous boundary | Normal result | Failure fallback | Decision |
|---|---|---|---|---|---|---|
| `Controls.kt` `screenOnReceiver` | `system_server` / receiver thread | settings, broadcast, wakelock | none | `Unit` | log and end this receive | named guard |
| `Controls.kt` left navigation click | `system_server` / main | action dispatch | inner partial guard | `Unit` | log and end click | outer named guard |
| `Controls.kt` left navigation long-click | `system_server` / main | action dispatch | inner partial guard | action Boolean | `false`, not consumed | named return guard |
| `Controls.kt` right navigation click | `system_server` / main | action dispatch | inner partial guard | `Unit` | log and end click | outer named guard |
| `Controls.kt` right navigation long-click | `system_server` / main | action dispatch | inner partial guard | action Boolean | `false`, not consumed | named return guard |
| `GlobalActions.kt` foreground fullscreen writer | SystemUI / background handler | `Settings.Global.putInt` | none | `Unit` | log and end posted write | named guard |
| `GlobalActions.kt` window receiver | `system_server` / receiver thread | reflection, settings, handler message | per-branch `try` only | `Unit` | log and end this receive | outer named guard |
| `LauncherIconHooks.kt` message `TextWatcher` | `com.miui.home` / main | resource lookup, measure/layout | none | `Unit` | log and end text update | named guard |
| `LauncherLayoutHooks.kt` seek-bar handler callback | `com.miui.home` / main | animation and view access | only animation end action guarded | `true` | `false`, not consumed | named return guard |
| `SystemChargingAndWallpaperHooks.kt` wallpaper runnable | `system_server` / main handler | file copy, reflection, JSON, broadcast | JSON section only | `Unit` | log and end runnable | outer named guard |
| `SystemLockScreenMoreHooks.kt` Smart Lock receiver | SystemUI / receiver thread | reflection, trust checks, broadcast | only final fallback guarded | `Unit` | log and end receive | outer named guard |
| `SystemStatusBarClockAndMoreHooks.kt` time-set receiver | SystemUI / main | reflection and timer replacement | none | `Unit` | log and end receive | named guard |
| `SystemStatusBarClockAndMoreHooks.kt` second ticker | SystemUI / main | reflection and repost | unnamed inner guard after local work | `Unit` | log and stop this tick | outer named guard |
| `SystemStatusBarMoreHooks.kt` alarm receiver | SystemUI / main | reflection and icon update | none | `Unit` | log and end receive | named guard |
| `SystemUILockScreenHooks.kt` album-art receiver | SystemUI / main | two ROM reflection fallbacks | second fallback could escape | `Unit` | log if both ROM methods fail | outer named guard |
| `SystemUIScreenshotHooks.kt` status-bar receiver | SystemUI / main | view state access | none | `Unit` | log and keep last state | named guard |
| `SystemUIScreenshotHooks.kt` navigation-bar receiver | SystemUI / main | view state access | none | `Unit` | log and keep last state | named guard |
| `SystemUIStatusBarHooks.kt` device-monitor handler | SystemUI / background looper | preferences, sysfs, reflection, messages | only sysfs reads caught | `Unit` | log and end this tick | outer named guard |
| `StepCounterController.kt` time-tick receiver | SystemUI / main | settings and view update | update helper caught internally, entry unguarded | `Unit` | log and end tick | named guard |
| `Various.kt` sidebar receiver | Security Center / receiver thread | preferences, synthetic touch dispatch | none | `Unit` | log and end receive | named guard |
| `BatteryIndicator.kt` test/screenshot receiver | SystemUI / main | view callbacks and test scheduling | none | `Unit` | log and end receive | named guard |

## Reviewed without code changes

Every remaining runtime candidate is listed below. Entries on one row share the
same owner, process, boundary, return semantics, and decision.

| Candidates | Process / thread | Current boundary and throwing work | Normal / failure semantics | Decision |
|---|---|---|---|---|
| `Controls.kt`: power/volume/fingerprint runnables; back/home/menu long-press runnables | `system_server` / policy handler | outer `ModuleHelper.guarded` or whole-body `try`; action/reflection work contained | `Unit`; log and end run | keep |
| `GlobalActions.kt`: status-bar receiver, global receiver, delayed home/scroll/volume runnables | SystemUI / main | whole receiver or runnable reflection sections already caught | `Unit`; log and end current callback | keep |
| `LauncherFolderHooks.kt`: secret-code receiver | `com.miui.home` / receiver | whole reflective body in `try` | `Unit`; log and end receive | keep |
| `LauncherIconHooks.kt`: posted icon update, message-animation runnable | `com.miui.home` / main | posted path enters guard; animation body has whole `try` | `Unit`; log and end | keep |
| `LauncherLayoutHooks.kt`: animation end action | `com.miui.home` / main | existing guard around visibility change | `Unit`; log and end | keep |
| `LauncherSystemHooks.kt`: dismiss-recents receiver | `com.miui.home` / receiver | whole reflective body in `try` | `Unit`; log and end | keep |
| `SystemAudioAndVisualAndMoreHooks.kt`: posted QS update and touch listener | SystemUI / main | existing guards; touch fallback is call-site `true` | original Boolean / chosen fallback | keep |
| `SystemDisplayAndWindowHooks.kt`: two preference observers, touch listener, wakelock release runnable | system/SystemUI | preference dispatch isolates each observer; listener/runnable guarded | original Boolean or `Unit`; per-call fallback | keep |
| `SystemFreeformAndMultiWindowHooks.kt`: two receivers and posted update | system/SystemUI | existing whole-body guards | `Unit`; log and end | keep |
| `SystemLockScreenMoreHooks.kt`: strong-auth and Bluetooth receivers | SystemUI / receiver | whole-body `try` or guard | `Unit`; log and end | keep |
| `SystemNotificationMoreHooks.kt`: three preference observers | `system_server` | `handlePreferenceChanged` isolates observers independently | `Unit`; one failure does not stop later observers | keep |
| `SystemNotificationPopupsHooks.kt`: preference observer and empty replacement runnable | SystemUI | observer dispatch isolated; runnable intentionally empty | `Unit`; no throwing work | keep |
| `SystemSettingsMoreHooks.kt`: update receiver | ordinary target process | whole reflective body in `try` | `Unit`; log and end | keep |
| `SystemStatusBarClockAndMoreHooks.kt`: delayed notification expansion | SystemUI / main | existing guard | `Unit`; log and end | keep |
| `SystemStatusBarMoreHooks.kt`: alarm observer | SystemUI / main | existing guard | `Unit`; log and end | keep |
| `SystemUIControlCenterHooks.kt`: preference observer and two touch listeners | SystemUI / main | observer whole `try`; listeners use explicit guarded return values | original Boolean / call-site fallback | keep |
| `SystemUILockScreenHooks.kt`: shortcut listener, camera reset runnable, secure-QS receiver/post/runnable | SystemUI / main | existing guards or whole-body `try` | original Boolean or `Unit`; call-site fallback | keep |
| `SystemUIMonitorAndTileHooks.kt`: 5G observer | SystemUI / main | existing guard | `Unit`; log and end | keep |
| `SystemUIScreenshotHooks.kt`: PiP screenshot receiver | SystemUI / receiver | existing guard around reflection and transaction | `Unit`; log and end | keep |
| `SystemUIStatusBarHooks.kt`: UI message handler and posted icon update | SystemUI / main | whole handler `try` or existing guard | `Unit`; log and end | keep |
| `StepCounterController.kt`: update runnable | SystemUI / handler | existing guard | `Unit`; log and end | keep |
| `Various.kt`: nested AppInfo posts, menu invalidation runnable, sidebar cleanup runnable, alarm observer | target app / main or `system_server` | whole reflective work already caught or guarded | `Unit`; log and end | keep |
| `BatteryIndicator.kt`: preference observer and step runnable | SystemUI / main | observer dispatch and update helper catch failures; runnable performs local state/update only | `Unit`; existing update failure containment | keep |

## Static gate

`tools/check-invariants.py` now:

- requires an outer guard/`try` for runtime-scope `onReceive`,
  `handleMessage`, and Launcher `afterTextChanged`;
- distinguishes object callbacks from lambda bodies;
- recognizes lambda parameters before the outer guard;
- checks `Handler.Callback` lambdas;
- excludes settings-only UI callbacks from the system-process rule.

Negative validation used a detached temporary worktree at K11A HEAD:

1. removing `Controls.screenOnReceiver`'s guard produced one
   `guard-framework-callbacks` violation and exit code 1;
2. restoring the guard returned 117 files / zero violations;
3. the temporary worktree was removed and was never committed.

## Verification boundary

- `ModuleHelperGuardedTest`: normal path, chosen return result, runtime
  exception, non-`Exception` throwable, no duplicate fallback execution,
  per-call fallback semantics, one failure log, and observer isolation.
- K11A targeted test and Kotlin/Java compilation: PASS.
- Full device behavior, LSPosed callback timing, and ROM reflection targets:
  **pending device verification**.
