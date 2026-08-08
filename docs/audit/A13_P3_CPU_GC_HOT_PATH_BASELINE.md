# A13 P3 CPU / GC / Hot-Path Baseline Audit

## Exact base HEAD

`cf648c59085be570813fdb25b31176ca0866de8a`

## Methodology

Audit-only pass over current A13 source. No production change.

Sources:
- `docs/audit/A13_HOOK_COST_MAP.json` (669 hook records)
- Manual source review of high-frequency hook callbacks
- Reflection, allocation, stack-trace, resource-lookup, and IPC pattern
  searches across `app/src/main/java`
- XposedHelpers.java internal cache analysis

## Hotness scale

| Level | Description | Examples |
|-------|-------------|----------|
| H4 | per-frame / animation / touch-move | onDraw, dispatchTouchEvent (ACTION_MOVE), layout, measure |
| H3 | frequent SystemUI state update | clock tick, status bar icon, notification row binding, QS refresh |
| H2 | event-driven repeated | screen state, broadcast, audio event, notification add/remove, settings observer |
| H1 | user-action / rare | settings click, long press, fragment opening |
| H0 | install / setup only | hook installation, class/method resolution, feature installer |

## XposedHelpers internal cache state

`mods/utils/XposedHelpers.java` already provides:
- Field cache: `ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Optional<Field>>>`
- No-arg method cache: `ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Optional<Method>>>`
- Method cache: `ConcurrentHashMap<MemberCacheKey.Method, Optional<Method>>`
- Constructor cache: `ConcurrentHashMap<MemberCacheKey.Constructor, Optional<Constructor<?>>>`

No class cache — `findClass()` re-resolves via `ClassUtils.getClass()` each call.
However, `findClass` calls are predominantly in H0 setup paths, not hot callbacks.

## Reflection findings

### Setup-only (H0) — already cached or one-time

All `findClass`, `findMethod`, `findField`, `findConstructor` calls in hook
installation functions are in H0 paths. Results are cached in local variables
or companion fields. No action needed.

### Runtime callback (H1-H4) — repeated resolution

| ID | File | Line | Function | Pattern | Frequency | Cached? | Actionable? |
|----|------|------|----------|---------|-----------|---------|-------------|
| R-01 | Helpers.kt | 452-471 | getAnimationScale | Class.forName + getDeclaredMethod × 3 + invoke × 3 | H1 (settings UI only) | No | Yes but low value — only called from System.kt settings sliders |
| R-02 | Helpers.kt | 477-496 | setAnimationScale | Same as R-01 | H1 | No | Same as R-01 |
| R-03 | GlobalActions.kt | 263-270 | SwitchToRecent handler | getDeclaredMethod × 2 + invoke × 2 | H1 (user gesture) | No | Yes but low frequency |
| R-04 | GlobalActions.kt | 295-304 | ScrollToTop handler | Same as R-03 | H1 | No | Same |
| R-05 | SystemUILockScreenHooks.kt | 592 | SecureQSTiles BroadcastReceiver | findMethodExact + invoke | H1 (tile click) | Via XposedHelpers cache | Low — XposedHelpers already caches Method |
| R-06 | AppSelector.kt | 192-196 | Privacy app toggle | getDeclaredMethod × 2 + invoke | H1 (user click) | No | Low — settings UI |
| R-07 | AppSelector.kt | 211-225 | Lock app toggle | Same as R-06 | H1 | No | Same |

**Conclusion**: No reflection candidate is in H3/H4. All runtime reflection
is H1 (user action) or lower. The highest-value reflection fix (R-01/R-02)
is only called from settings UI sliders — not steady-state.

## Allocation findings

### H3 — per-clock-tick allocations

| ID | File | Line | Function | Allocation | Frequency |
|----|------|------|----------|------------|-----------|
| A-01 | SystemStatusBarClockAndMoreHooks.kt | 196-197 | updateTime before() | 2× StringBuilder per tick | H3 (every second or minute) |
| A-02 | SystemStatusBarClockAndMoreHooks.kt | 183 | updateTime before() | 1× String.replaceFirst per tick (when seconds enabled) | H3 |
| A-03 | SystemStatusBarClockAndMoreHooks.kt | 41 | replaceClockHourToken | 1× StringBuilder per tick (when custom format path) | H3 |
| A-04 | SystemAudioAndVisualAndMoreHooks.kt | 365-378 | hookUpdateTime | 2× StringBuilder + SimpleDateFormat + Calendar + TimeZone per lock screen tick | H3 (lock screen visible) |

### H2 — event-driven allocations

| ID | File | Line | Function | Allocation | Frequency |
|----|------|------|----------|------------|-----------|
| A-05 | Various.kt | 792 | DisableDockSuggestHook before() | ArrayList<String>() per call | H2 (freeform suggestion query) |
| A-06 | SystemChargingAndWallpaperHooks.kt | 58-61 | ChargingInfoHook | 3× String.format per charging state change | H2 |
| A-07 | SystemUIStatusBarHooks.kt | 1482 | HorizMarginHook | 1× Pair per layout pass | H2/H3 (layout) |

### H1 — user-action allocations (low priority)

| ID | File | Line | Function | Allocation |
|----|------|------|----------|------------|
| A-08 | Various.kt | 1036-1047 | AppInfoDuringMiuiInstallHook | SpannableStringBuilder per install |
| A-09 | SystemAudioAndVisualAndMoreHooks.kt | 430,448 | ScreenshotConfigHook | String.replace chain per screenshot |

### H0 — setup allocations (no action)

Helpers.kt ArrayList allocations, HookTargetResolver Triple allocations,
AppHelper Pair allocations — all H0.

## Stack-trace findings

| ID | File | Line | Function | Pattern | Frequency | Gated? |
|----|------|------|----------|---------|-----------|--------|
| S-01 | LauncherGestureHooks.kt | 284 | getGlobalBoolean after() | Thread.currentThread().stackTrace iteration | H2 (only when key == "force_fsg_nav_bar") | Yes — early return for other keys |
| S-02 | Various.kt | 795 | DisableDockSuggestHook before() | Thread.currentThread().stackTrace iteration | H2 (freeform suggestion query) | No — always walks stack |

Both are H2, not H3/H4. S-01 is gated by key check before stack walk.
S-02 always walks but is gated by `various_disableapp` preference.

## Resource lookup findings

| ID | File | Line | Function | Lookup | Cached? | Frequency |
|----|------|------|----------|--------|---------|-----------|
| R-01 | SystemStatusBarClockAndMoreHooks.kt | 180-181 | updateTime before() | getIdentifier + getString | No | H3 (per tick) |
| R-02 | SystemUIControlCenterHooks.kt | 528-534 | TimerItem.getTimePos before() | getIdentifier × 2 + getDimension × 2 | No | H1/H2 (volume timer UI) |
| R-03 | SystemNotificationMoreHooks.kt | 686-688 | notification hook | getIdentifier + getDimensionPixelSize | Yes (lazy) | H2 |
| R-04 | SystemUIControlCenterHooks.kt | 617-618 | StatusBarGesturesHook | getIdentifier + getDimensionPixelSize | Yes (lazy) | H2 |
| R-05 | SystemUILockScreenHooks.kt | 263-401 | LockScreenShortcutHook | getDrawable × multiple | No | H2 (lock screen icon) |

R-01 is the most significant: uncached `Resources.getIdentifier` + `getString`
on every clock tick in the `statusbarClockTweak` default format path.

## IPC / Binder findings

| ID | File | Line | Function | IPC | Frequency |
|----|------|------|----------|-----|-----------|
| I-01 | Various.kt | 904,911 | ShowCallUIHook after() | Settings.Global.getString + getInt | H2 (incoming call) |
| I-02 | GlobalActions.kt | 315,330 | Volume cursor control | Settings.Global.getString | H2 (volume key press) |
| I-03 | GlobalActions.kt | 760,772 | Task stack listener | Settings.Global.putString + putInt | H2 (task change) |
| I-04 | Controls.kt | multiple | Various control hooks | Settings.Global/System getInt/putInt | H2 |
| I-05 | GlobalActions.kt | 576-662 | Toggle handlers | Settings.Secure/System getInt/putInt | H1 (user toggle) |
| I-06 | SystemShareAndOpenWithHooks.kt | 72 | Share hook | getPackageInfoAsUser | H2 (share sheet) |

All IPC findings are H1/H2. None are H3/H4 steady-state. Most Settings
reads are inherently current-state queries that cannot be cached without
invalidation design. No IPC candidate meets the STATIC_ACTIONABLE bar.

## Logging findings

Logging in `mods/` is exclusively on error paths via `XposedHelpers.log(t)`.
No steady-state logging in successful callback paths. `SettingsDiagnostics`
is only used in settings app UI code, not runtime hooks.

No logging-related hot-path issue.

## Already-optimized findings

- `SystemNotificationMoreHooks.kt:686-688` — lazy-cached dimension lookup ✅
- `SystemUIControlCenterHooks.kt:617-618` — lazy-cached status bar height ✅
- `ModuleHelper.java:95-108` — static-variable-cached ActivityThread reflection ✅
- `XposedHelpers.java` — field/method/constructor caches ✅
- Most `Field.get`/`Field.set` in callbacks use H0-cached Field objects ✅

## Frozen-slice findings

No frozen P1B/P2 slice was found to have remaining hot-path issues.

P2-1 (SubFragment pendingHighlightScroll) — fixed, no hot-path issue remains.
P2-2 (AppSelector async load) — fixed, no hot-path issue remains.
P2-3 (ActivitySelector async load) — fixed, no hot-path issue remains.

## Candidate evidence sheet

### Candidate P3-CAND-01: statusBarClockTweak updateTime hot-path

| Field | Value |
|-------|-------|
| File | `SystemStatusBarClockAndMoreHooks.kt` |
| Line | 153-202 |
| Function | `updateTime` before() hook |
| Process | SYSTEM_UI (com.android.systemui) |
| Hotness | H3 — per clock tick (every second when seconds enabled, every minute otherwise) |
| Trigger | `MiuiClock.updateTime` called by system clock ticker |
| Feature gate | `system_statusbar_clocktweak` preference (preference-dependent) |

**Steady-state operations per tick (default format path):**
- 4-5× `MainModule.mPrefs.getBoolean/getString` — in-memory snapshot read, low cost
- 1× `Resources.getIdentifier(fmt, "string", "com.android.systemui")` — uncached resource lookup
- 1× `mContext.getString(fmtResId)` — string resource load
- 1× `String.replaceFirst(":mm", ":mm:ss")` — new String allocation (when seconds enabled)
- 1× `replaceClockHourToken(fmtString, hourStr)` — 1× StringBuilder allocation
- 2× `StringBuilder` allocation (formatSb + textSb) — lines 196-197
- 1× `XposedHelpers.callMethod(mCalendar, "format", ...)` — reflection call (via XposedHelpers cache)
- 1× `textSb.toString()` — String allocation

**Total per-tick allocation**: ~4-5 short-lived objects + 1-2 String allocations.
**Total per-tick resource lookup**: 1 uncached `getIdentifier` + 1 `getString`.

| Score dimension | Score (0-3) |
|-----------------|-------------|
| FREQUENCY | 3 (H3, per-second when seconds enabled) |
| WORK_PER_CALLBACK | 2 (moderate: format resolution + StringBuilder + resource lookup) |
| ALLOCATION_PRESSURE | 2 (4-5 short-lived objects per tick) |
| REFLECTION_PRESSURE | 1 (callMethod via cache, low) |
| IPC_PRESSURE | 0 (no Binder IPC) |
| STATIC_CONFIDENCE | 3 (format string is deterministic from preferences, cacheable) |
| OPTIMIZATION_ISOLATION | 3 (single hook, single file, self-contained) |
| REGRESSION_SAFETY | 2 (must preserve format correctness, but logic is deterministic) |

**Risk**: LOW — format string depends only on preferences (showSeconds, is24,
showAmpm, hourIn2d) which are stable between ticks. Caching the resolved
format string and reusing StringBuilders is safe with preference-change
invalidation.

**Static testability**: HIGH — format resolution is pure function of
preferences, no ROM-dependent behavior.

**Device evidence dependency**: LOW — the allocation and resource lookup
cost is statically provable. No ROM-specific behavior needed.

### Candidate P3-CAND-02: lockScreenAlarm hookUpdateTime

| Field | Value |
|-------|-------|
| File | `SystemAudioAndVisualAndMoreHooks.kt` |
| Line | 340-398 |
| Function | `hookUpdateTime` (lock screen alarm display) |
| Process | SYSTEM_UI |
| Hotness | H3 (per lock screen clock tick, only when lock screen visible) |
| Trigger | Lock screen clock update |

**Steady-state operations**: 2× StringBuilder + SimpleDateFormat + Calendar +
TimeZone + SpannableString per tick. Also calls `ModuleHelper.getNextMIUIAlarmTime`
(Settings.System.getString Binder IPC) and `Settings.System.getInt`.

| Score dimension | Score (0-3) |
|-----------------|-------------|
| FREQUENCY | 2 (H3 but only when lock screen visible) |
| WORK_PER_CALLBACK | 3 (heavy: SimpleDateFormat + Calendar + TimeZone + IPC) |
| ALLOCATION_PRESSURE | 3 (6+ objects per tick) |
| REFLECTION_PRESSURE | 0 |
| IPC_PRESSURE | 1 (Settings.System.getString for alarm time) |
| STATIC_CONFIDENCE | 2 (format depends on locale + preference, cacheable but more complex) |
| OPTIMIZATION_ISOLATION | 2 (spans alarm time lookup + format) |
| REGRESSION_SAFETY | 1 (locale-sensitive, alarm time is dynamic) |

**Risk**: MEDIUM — SimpleDateFormat is locale-sensitive, alarm time is
dynamic. Caching is more complex. Higher regression risk.

### Candidate P3-CAND-03: LauncherGestureHooks getGlobalBoolean stack trace

| Field | Value |
|-------|-------|
| File | `LauncherGestureHooks.kt` |
| Line | 280-292 |
| Function | `MiuiSettingsUtils.getGlobalBoolean` after() |
| Process | LAUNCHER |
| Hotness | H2 (only when key == "force_fsg_nav_bar") |
| Trigger | Launcher settings query for force_fsg_nav_bar |

**Steady-state operations**: `Thread.currentThread().stackTrace` array
allocation + iteration. Gated by key check.

| Score dimension | Score (0-3) |
|-----------------|-------------|
| FREQUENCY | 1 (H2, gated by key) |
| WORK_PER_CALLBACK | 2 (stack trace array allocation + walk) |
| ALLOCATION_PRESSURE | 1 (StackTraceElement[] array) |
| STATIC_CONFIDENCE | 1 (stack trace is inherently dynamic) |
| OPTIMIZATION_ISOLATION | 1 (stack trace inspection is fragile to replace) |
| REGRESSION_SAFETY | 1 (changing detection mechanism could break recents behavior) |

**Risk**: HIGH — stack trace inspection is used to detect caller context.
Replacing it requires an alternative detection mechanism that may not
exist statically.

## Classification

### STATIC_ACTIONABLE

**P3-CAND-01: statusBarClockTweak updateTime hot-path**

Satisfies all criteria:
- H3 hotness (per-second/per-minute clock tick) ✅
- Proven repeated cost (4-5 allocations + uncached resource lookup per tick) ✅
- Bounded change (single hook, single file, cache format string + reuse StringBuilder) ✅
- Static behavior proof possible (format is pure function of preferences) ✅
- No frozen slice conflict ✅
- Low regression risk (deterministic format, preference-change invalidation) ✅

### DEVICE_EVIDENCE_FIRST

- **P3-CAND-02** (lockScreenAlarm): H3 but only when lock screen visible.
  SimpleDateFormat locale sensitivity and dynamic alarm time make static
  caching more complex. Device evidence needed to confirm actual tick
  frequency and allocation significance on ROM.
- **LauncherGestureHooks stack trace (S-01)**: H2, gated by key. Stack
  trace replacement requires alternative caller detection mechanism.
  Device evidence needed to confirm actual frequency of `force_fsg_nav_bar`
  queries.
- **Various.kt DisableDockSuggestHook (S-02)**: H2, always walks stack.
  Device evidence needed to confirm `getFreeformSuggestionList` call
  frequency.

### ALREADY_OPTIMIZED

- SystemNotificationMoreHooks lazy-cached dimension lookup
- SystemUIControlCenterHooks lazy-cached status bar height
- ModuleHelper static-variable-cached ActivityThread reflection
- XposedHelpers field/method/constructor caches
- Most Field.get/Field.set in callbacks use H0-cached Field objects

### LOW_VALUE

- Helpers.getAnimationScale/setAnimationScale reflection (H1, settings UI only)
- GlobalActions input injection reflection (H1, user gesture)
- AppSelector privacy/lock app toggle reflection (H1, settings UI)
- PrivacyAppAdapter/LockedAppAdapter constructor reflection (H1)
- SortableList/SpinnerEx field resolution (H1, UI init)
- Various.kt AppInfoDuringMiuiInstall SpannableStringBuilder (H1, per install)
- ScreenshotConfigHook String.replace chain (H1, per screenshot)
- All H0 setup allocations

### FALSE_POSITIVE

- None. All scanner hits were verified against actual call context.

## Recommended P3-1

**P3-CAND-01: statusBarClockTweak updateTime hot-path optimization**

This is the only candidate that simultaneously satisfies:
- H3 hotness with proven per-tick cost
- Statically provable optimization (format string is deterministic)
- Small isolated scope (single hook in single file)
- Low regression risk (deterministic format, preference invalidation)
- No device evidence required to prove the optimization is correct

**Expected production scope**: `SystemStatusBarClockAndMoreHooks.kt` only.

**Optimization direction** (not implemented in P3-0):
- Cache resolved format string keyed by preference snapshot
  (showSeconds, is24, showAmpm, hourIn2d). Invalidate on preference change.
- Pre-resolve `getIdentifier` + `getString` result once, cache resource ID.
- Reuse StringBuilder instances via per-clock-instance fields or ThreadLocal.
- Eliminate `replaceFirst` allocation by pre-computing the seconds-appended
  format string.

## Deferred candidates

- P3-CAND-02 (lockScreenAlarm): DEVICE_EVIDENCE_FIRST
- LauncherGestureHooks stack trace (S-01): DEVICE_EVIDENCE_FIRST
- Various.kt DisableDockSuggestHook stack trace (S-02): DEVICE_EVIDENCE_FIRST
- All H1/H0 reflection and allocation findings: LOW_VALUE

## Test-governance debt

```
NON_BLOCKING_TEST_GOVERNANCE_DEBT:

test_launcher_gesture_state_cache.py uses git diff HEAD -- app/src/main/java
which can false-fail during legitimate uncommitted future production tasks.
Future cleanup should redesign around fixed historical scope or explicit
source invariants. Not solved in P3-0.
```

## Device evidence still pending

- P0 runtime baseline = pending device
- P1B device smoke evidence = pending
- P1B-4A ROM lifecycle evidence = pending
- P2 device/runtime memory evidence = pending
- P3 device/runtime CPU/GC evidence = pending

No CPU/GC/allocation improvement claims are made. Static audit identifies
candidates; device evidence is required to quantify actual impact.
