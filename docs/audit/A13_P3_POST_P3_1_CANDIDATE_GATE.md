# A13 P3 Post-P3-1 Hot-Path Candidate Gate

## Base HEAD

`4317311555d9bb0f53f0e8c8e94f0b5aa55c4fbf`

## P3-1 steady-state revalidation

P3-1 scope = `SystemStatusBarClockAndMoreHooks.kt` only.

Current primary cached path:

```kotlin
val formatCache = XposedHelpers.getAdditionalInstanceField(clock, "statusBarClockFormatCache") as? StatusBarClockFormatCache
if (formatCache != null) {
    val fmtResId = formatCache.resolveResourceId(mContext.resources, showAmpm)
    val rawFormat = mContext.getString(fmtResId)
    timeFmt = formatCache.resolveFormat(rawFormat, showSeconds, is24, hourIn2d)
} else {
    // fallback old path
}
```

| Aspect | State |
|--------|-------|
| Resource ID | Lazy cached in `StatusBarClockFormatCache` |
| Raw resource String | Per-tick `mContext.getString(fmtResId)` retained |
| Format transformation | Cache hit avoids `replaceFirst` + `replaceClockHourToken` |
| formatSb | Callback-local `StringBuilder` |
| textSb | Callback-local `StringBuilder` |
| mCalendar.format | Preserved via `XposedHelpers.callMethod` |
| P3-1 regression | None confirmed |

## P3-1 test-hardening debt

```
NON_BLOCKING_TEST_HARDENING_DEBT:

contract_violations() in test_a13_p3_statusbar_clock_format_hot_path.py
does not explicitly reject a primary cached path that simultaneously
contains both:

  formatCache.resolveResourceId(...)

and

  resources.getIdentifier("fmt_time_12hour_minute", ...)

The current real source is valid, but a future mutation could leave
both forms in the primary branch and still pass. Not solved in P3-QA-1.
```

## Remaining candidate revalidation

### Candidate A — HorizMarginHook (SystemUIStatusBarHooks.kt:1475)

```kotlin
override fun before(param: BeforeHookCallback) {
    val leftMargin = MainModule.mPrefs.getInt("system_statusbar_horizmargin_left", 16)
    val marginLeft = HookUtils.dp2px(leftMargin.toFloat())
    val rightMargin = MainModule.mPrefs.getInt("system_statusbar_horizmargin_right", 16)
    val marginRight = HookUtils.dp2px(rightMargin.toFloat())
    param.returnAndSkip(android.util.Pair(marginLeft.toInt(), marginRight.toInt()))
}
```

Targets:
- `StatusBarWindowView.paddingNeededForCutoutAndRoundedCorner`
- `StatusBarContentInsetsProvider.getStatusBarContentInsetsForCurrentRotation`

**Callback nature:**
Both target names refer to layout/insets computation, not explicit
per-frame rendering. `getStatusBarContentInsetsForCurrentRotation` is
expected to be called when the status bar content insets are computed
for the current display rotation. `paddingNeededForCutoutAndRoundedCorner`
computes display cutout / rounded corner padding. Neither is an
`onDraw`, `onLayout`, or `onMeasure` callback.

**Per-frame?** No static proof. ROM likely calls these during
configuration/rotation and possibly every layout pass, but that is
ROM-specific.

**Orientation / density / configuration changes:** Yes. `dp2px` uses
`Resources.getSystem().displayMetrics`. Any density or display
configuration change can change the pixel values.

**Preference live reload:** The module settings UI can change the
margin preferences. The code reads them on every callback, which is
the live-reload contract.

**ROM mutation of returned Pair:** `android.util.Pair` fields are
final, so the caller cannot mutate the returned object. That is safe.

**Pair reuse safety:** If cached, the cache key must include both
margins and the current density. A single cached Pair could be shared
across callbacks with identical inputs.

**Owner / invalidation:** A `Pair` is only useful when both margins
and density are stable. It would need invalidation on preference
change and on configuration change. The per-callback cost is small:
two `getInt` + two `dp2px` + one `Pair` allocation.

**Worth caching?** Even at H3/H4 frequency, the allocation is one
Pair and two boxed Floats. Adding cache state to avoid this is not
obviously worthwhile.

| Score | Value |
|-------|-------|
| FREQUENCY | 1 (likely configuration/rotation-driven, not provably per-frame) |
| WORK_PER_CALLBACK | 1 (small: 2 prefs + 2 dp2px + 1 Pair) |
| ALLOCATION_PRESSURE | 1 (1 Pair + 2 Float boxes) |
| REFLECTION_PRESSURE | 0 |
| IPC_PRESSURE | 0 |
| STATIC_CONFIDENCE | 1 (frequency unproven; cache key trivial but invalidation needed) |
| OPTIMIZATION_ISOLATION | 2 (single file, single hook) |
| REGRESSION_SAFETY | 2 (cache key must include density and both margins) |
| DEVICE_EVIDENCE_DEPENDENCY | HIGH |

**Classification:** `DEVICE_EVIDENCE_FIRST` / LOW_VALUE borderline.

---

### Candidate B — LockScreenAlarmHook (SystemAudioAndVisualAndMoreHooks.kt:405)

```kotlin
ModuleHelper.findAndHookMethod("com.android.keyguard.clock.MiuiKeyguardSingleClock", ..., "updateTime", ...)
ModuleHelper.findAndHookMethod("com.android.keyguard.clock.MiuiKeyguardDualClock", ..., "updateTime", ...)
```

`hookUpdateTime` cost breakdown:

| Step | Dynamic dependency | Cacheable? | Invalidation needed |
|------|-------------------|------------|---------------------|
| A. `ModuleHelper.getNextMIUIAlarmTime` | next_alarm_clock_formatted Settings | Yes, but alarm changes | ContentObserver or alarm broadcast |
| B. Module resource title lookup | `R.string.system_statusbaricons_alarm_title` | No change | N/A |
| C. `system_lsalarm_format` | User preference | Yes | Preference change |
| D. `SimpleDateFormat` | Pattern from `DateFormat.getBestDateTimePattern` | Yes with locale/24h | Locale, 24h format, configuration |
| E. `Locale.getDefault` | Locale | Yes | Locale change |
| F. `TimeZone.getDefault` | Time zone | Yes | Time zone change |
| G. `Calendar.getInstance` | Time/now | No | Current time |
| H. `DateUtils.getRelativeTimeSpanString` | Current time - alarm time | No | Every minute at least |
| I. `Settings.System.getInt` selected_keyguard_clock_position | Setting | Yes | Setting change |
| J. `getIdentifier` + `getDimensionPixelSize` | Resource/density | Yes | Configuration |
| K. `SpannableString` + spans | Final text + font size | No | All of the above |

**Frequency confidence:** The hook is on `updateTime` of the keyguard
clock. The exact ticker frequency is ROM-dependent. It may be once per
minute or once per second if the lock screen clock shows seconds.
`FREQUENCY = ROM_DEPENDENT`.

**formatterAlarm:** `private val formatterAlarm = SimpleDateFormat("H:m", Locale.ENGLISH)`
is declared but unused in `hookUpdateTime`. It is historical dead state.
Do not remove in this P3-QA-1 pass (no production change).

**Static confidence:** Very low. Multiple dynamic dependencies require
an invalidation design. The allocation chain is heavy, but the cache
benefit is unclear because the rendered text changes with time.

| Score | Value |
|-------|-------|
| FREQUENCY | 2 (H3 possible, only on lock screen, frequency ROM-dependent) |
| WORK_PER_CALLBACK | 3 (SimpleDateFormat, Calendar, SpannableString, spans, Settings read) |
| ALLOCATION_PRESSURE | 3 (6+ objects + SpannableString + spans) |
| REFLECTION_PRESSURE | 0 |
| IPC_PRESSURE | 1 (Settings.System.getString for alarm) |
| STATIC_CONFIDENCE | 0 (too many dynamic dependencies) |
| OPTIMIZATION_ISOLATION | 1 (spans AlarmHook, ModuleHelper, formatting, resources) |
| REGRESSION_SAFETY | 1 (complex invalidation matrix) |
| DEVICE_EVIDENCE_DEPENDENCY | HIGH |

**Classification:** `DEVICE_EVIDENCE_FIRST`.

---

### Candidate C — Launcher stack-trace (LauncherGestureHooks.kt:280)

```kotlin
ModuleHelper.findAndHookMethodSilently("com.miui.launcher.utils.MiuiSettingsUtils", ..., "getGlobalBoolean", ...,
    object : MethodHook() {
        override fun after(param: AfterHookCallback) {
            if (param.getArg(1) != "force_fsg_nav_bar") return
            for (el in Thread.currentThread().stackTrace) {
                if (el.className == "com.miui.home.recents.BaseRecentsImpl") {
                    XposedHelpers.setAdditionalStaticField(baseRecentsClass, "REAL_FORCE_FSG_NAV_BAR", param.getResult())
                    param.setResult(true)
                    return
                }
            }
        }
    })
```

**Key gate:** The `if (param.getArg(1) != "force_fsg_nav_bar") return`
guards the stack trace walk. The walk only happens when the queried
key is `force_fsg_nav_bar`.

**Static replacement?** The hook target is a generic `MiuiSettingsUtils.getGlobalBoolean`.
To avoid stack inspection, one would need a different hook target that
is only called by `BaseRecentsImpl` or that has the caller in the
arguments. No such target is statically obvious. Re-targeting is a
compatibility / ROM-dependent research problem.

**Frequency:** `force_fsg_nav_bar` query frequency is ROM-dependent,
but the key gate makes it much lower than the total `getGlobalBoolean`
call volume.

**Do not use StackWalker:** A different stack API would not solve the
caller-identity problem.

| Score | Value |
|-------|-------|
| FREQUENCY | 1 (gated by specific key; frequency ROM-dependent) |
| WORK_PER_CALLBACK | 2 (stack trace array allocation + walk) |
| ALLOCATION_PRESSURE | 1 (StackTraceElement[] on gated path) |
| STATIC_CONFIDENCE | 0 (caller detection is inherently dynamic) |
| OPTIMIZATION_ISOLATION | 1 (re-targeting needed) |
| REGRESSION_SAFETY | 0 (re-targeting could break recents behavior) |
| DEVICE_EVIDENCE_DEPENDENCY | HIGH |

**Classification:** `DEVICE_EVIDENCE_FIRST`.

---

### Candidate D — DisableDockSuggestHook (Various.kt:789)

```kotlin
override fun before(param: BeforeHookCallback) {
    val blackList = ArrayList<String>()
    blackList.add("xx.yy.zz")
    var topMethod = 10
    val stackTrace = Thread.currentThread().stackTrace
    for (el in stackTrace) {
        if (el != null && topMethod < 20 &&
            (el.className.contains("edit.DockAppEditActivity") || el.className.contains("BubblesSettings"))) {
            return
        }
        topMethod++
    }
    param.returnAndSkip(blackList)
}
```

Target: `android.util.MiuiMultiWindowUtils.getFreeformSuggestionList`

**Callback frequency:** Freeform suggestion list queries are
ROM-dependent. Not H4, not status-bar tick. Likely H2 (triggered by
freeform UI events).

**Stack walk purpose:** Detect if the call originates from the dock
app edit UI or bubble settings.

**Static replacement?** No obvious static caller parameter. Re-targeting
to a more specific method in those two caller classes would be
ROM-dependent.

**ArrayList:** The list is always created and returned to ROM. If the
ROM caller modifies the list (e.g., adds suggestions), a shared
immutable singleton would break that expectation. Caching is not safe
without proving the caller does not mutate the returned list.

| Score | Value |
|-------|-------|
| FREQUENCY | 1 (freeform suggestion events, likely H2) |
| WORK_PER_CALLBACK | 1 (ArrayList + stack walk of bounded depth) |
| ALLOCATION_PRESSURE | 1 (1 ArrayList) |
| STATIC_CONFIDENCE | 0 (caller detection is dynamic) |
| OPTIMIZATION_ISOLATION | 1 (re-targeting or list-mutation proof needed) |
| REGRESSION_SAFETY | 0 (mutable return list risk) |
| DEVICE_EVIDENCE_DEPENDENCY | HIGH |

**Classification:** `DEVICE_EVIDENCE_FIRST`.

---

### Candidate E — TimerItem getTimePos (SystemUIControlCenterHooks.kt:521)

```kotlin
override fun before(param: BeforeHookCallback) {
    ...
    val seekbarWidthResId = if (ModuleHelper.NOT_EXIST_SYMBOL.equals(mTimerSeekbarWidth)) {
        mContext.resources.getIdentifier("miui_volume_timer_seelbar_width", "dimen", "miui.systemui.plugin")
    } else {
        mTimerSeekbarWidth as? Int ?: 0
    }
    val mTimerSeekbarMarginLeft = mContext.resources.getIdentifier("miui_volume_timer_seekbar_margin_left", "dimen", "miui.systemui.plugin")
    val seekWidth = mContext.resources.getDimension(seekbarWidthResId)
    val marginLeft = mContext.resources.getDimensionPixelSize(mTimerSeekbarMarginLeft)
    ...
    param.returnAndSkip(seekWidth / 10 * seg + marginLeft - halfTimerWidth)
}
```

Target: `com.android.systemui.miui.volume.TimerItem.getTimePos`

**Callback nature:** Called when the volume timer position is computed
(e.g., on user drag or timer change). Not per-frame rendering, not
per-volume-animation. Likely H1/H2 (user interaction with the timer
UI).

**Configuration sensitivity:** The dimens are stable for a given
SystemUI plugin configuration. They could be cached, but the frequency
is low enough that the benefit is small.

| Score | Value |
|-------|-------|
| FREQUENCY | 1 (timer UI interaction, not H4) |
| WORK_PER_CALLBACK | 1 (2 getIdentifier + 2 dimension reads) |
| ALLOCATION_PRESSURE | 0 |
| STATIC_CONFIDENCE | 2 (cacheable if frequency proven) |
| OPTIMIZATION_ISOLATION | 2 (single hook) |
| REGRESSION_SAFETY | 2 (cache by resource ID, stable for config) |
| DEVICE_EVIDENCE_DEPENDENCY | MEDIUM |

**Classification:** `DEVICE_EVIDENCE_FIRST` / LOW_VALUE borderline.

---

## H4 revalidation

`A13_HOOK_COST_MAP.json` records with `callback_frequency_class == "FRAME_OR_LAYOUT_HOT"` were
spot-checked. The following targets exist but have negligible per-callback work:

- `LauncherGestureHooks` touch dispatchers: use already-cached `HotSeatGestureState` per view.
- `appsDisableService` `draw` / `onTouch`: `returnAndSkip(null/false)` only.
- `LauncherFolderHooks.Folder.onLayout`: single preference + reflection.
- `SystemUIControlCenterHooks.Resources.getDrawable`: setup-time hook installed inside `PluginComponentFactory.create`; per-callback only checks `resId == enabledTileBackgroundResId`.
- `SystemAudioAndVisualAndMoreHooks.MiuiKeyguardSingleClock.updateTime`: actually belongs to `LockScreenAlarmHook`, not `tempHideOverlayApp` (map mis-classification noted).

No `onDraw`, `dispatchDraw`, `onMeasure`, `ACTION_MOVE`, or animation
callback was found with repeated allocation, reflection, or IPC cost
that is both statically provable and isolated.

**H4 STATIC_ACTIONABLE candidate count = 0** remains credible.

---

## Candidate scoring summary

| Candidate | FREQ | WORK | ALLOC | REFL | IPC | STAT_CONF | ISOL | SAFE | DEVICE_DEP | Class |
|-----------|------|------|-------|------|-----|-----------|------|------|-----------|-------|
| A HorizMargin | 1 | 1 | 1 | 0 | 0 | 1 | 2 | 2 | HIGH | DEVICE_EVIDENCE_FIRST / LOW |
| B LockScreenAlarm | 2 | 3 | 3 | 0 | 1 | 0 | 1 | 1 | HIGH | DEVICE_EVIDENCE_FIRST |
| C Launcher stack trace | 1 | 2 | 1 | 0 | 0 | 0 | 1 | 0 | HIGH | DEVICE_EVIDENCE_FIRST |
| D DisableDockSuggest | 1 | 1 | 1 | 0 | 0 | 0 | 1 | 0 | HIGH | DEVICE_EVIDENCE_FIRST |
| E TimerItem getTimePos | 1 | 1 | 0 | 0 | 0 | 2 | 2 | 2 | MEDIUM | DEVICE_EVIDENCE_FIRST / LOW |

---

## STATIC_ACTIONABLE candidates

0.

No remaining candidate satisfies:
- proven repeated cost
- not setup-only / not user-action-only
- small isolated production scope
- static proof possible
- controlled configuration semantics
- meaningful expected direction
- low/controlled regression risk

P3-1 eliminated the only confirmed H3 STATIC_ACTIONABLE format-resolution cost.

## DEVICE_EVIDENCE_FIRST candidates

- A. HorizMarginHook
- B. LockScreenAlarmHook
- C. Launcher stack-trace hook
- D. DisableDockSuggestHook
- E. TimerItem getTimePos

## LOW_VALUE candidates

None that are provably low. HorizMargin and TimerItem are at the
low-value / device-evidence borderline.

## ALREADY_OPTIMIZED

- Launcher touch dispatchers use per-view `HotSeatGestureState`.
- Clock format resolution uses `StatusBarClockFormatCache`.
- Most XposedHelpers reflection is cached.
- `Resources.getDrawable` hook has minimal per-callback guard.

## P3-2 selection Gate

No P3-2 candidate passes the required gate. Therefore:

```
P3-2 = DEFERRED
P3 STATIC STAGE = READY_FOR_CLOSURE
```

No further P3 production implementation is recommended until device
evidence identifies a specific repeated cost that justifies an
optimization.

## P3-1 test-hardening debt repeat

```
NON_BLOCKING_TEST_HARDENING_DEBT:

The Python contract checker does not reject a primary cached path that
contains both:
  formatCache.resolveResourceId(...)
and
  resources.getIdentifier("fmt_time_12hour_minute", ...)
This could be tightened in a future governance pass.
```
