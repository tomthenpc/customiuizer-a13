# A13 P3 Stage Closure

## Closure base

`af2971f0ff9d160422e08b10a77adaf5075cf7be`

## P3 starting stable base

`cf648c59085be570813fdb25b31176ca0866de8a`

## P3 cumulative production scope

`git diff --name-only cf648c59085be570813fdb25b31176ca0866de8a..HEAD -- app/src/main`

Result:

```
app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt
```

Only one production file changed during P3.

## P3-0 status

`QA_ACCEPTED / FROZEN`

Historical audit: `docs/audit/A13_P3_CPU_GC_HOT_PATH_BASELINE.md`
- Base HEAD: `cf648c59085be570813fdb25b31176ca0866de8a`
- Production change: 0
- P3-CAND-01: `statusBarClockTweak` format-resolution hot path
- P3-1: `SELECTED / NOT_STARTED` (at time of P3-0 audit)
- P3-0 historical baseline intact.

## P3-1 final production contract

File: `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`

### Cache topology

`StatusBarClockFormatCache` holds:
- `Int` (`unresolvedResId`, `resIdNoAmpm`, `resIdWithAmpm`)
- `Boolean` (`cachedShowSeconds`, `cachedIs24`, `cachedHourIn2d`)
- `String?` (`cachedRawFormat`, `cachedResolvedFormat`)

Does not hold:
- `Context`
- `Resources` (only as method parameter)
- `View`
- `TextView`
- `MiuiClock`
- `Calendar`
- `ClassLoader`

### Resource ID cache

`fmt_time_12hour_minute` and `fmt_time_12hour_minute_pm` are lazy-resolved
using `Resources.getIdentifier`. Sentinel: `Int.MIN_VALUE`. If
`getIdentifier()` returns `0`, the cache stores `0` and preserves the
original failure semantics for `getString(0)`.

### Format cache key

`resolveFormat` is keyed by:
- `rawFormat`
- `showSeconds`
- `is24`
- `hourIn2d`

### Configuration / locale correctness

Per tick, the code still calls:

```kotlin
val rawFormat = mContext.getString(fmtResId)
```

If the resource text changes due to locale or configuration, `rawFormat`
changes and the cache key misses, forcing recompute. No explicit
invalidation needed.

### Callback-local builders

```kotlin
val formatSb = StringBuilder(timeFmt)
val textSb = StringBuilder()
```

Both are callback-local. They are not promoted to reusable mutable state.

### ROM format call

```kotlin
XposedHelpers.callMethod(mCalendar, "format", mContext, textSb, formatSb)
```

Preserved.

### Frozen behaviors

- `ccClock` path uses `system_cc_clock_customformat`
- `ccDate` path uses `ccDateFormat`
- Custom format path uses `system_statusbar_clock_customformat`
- Ticker lifecycle and screen receiver unchanged

### P3-1 regression

No regression found.

## P3-1 JVM behavior proof

Test file: `app/src/test/java/tv/withaibuild/customiuizer/mods/StatusBarClockFormatCacheTest.kt`

Covers:
- Same input → cache hit
- `rawFormat` change → recompute
- `showSeconds` change → recompute
- `is24` change → recompute
- `hourIn2d` change → recompute
- 12h / 24h
- Leading zero
- Seconds
- Hour token behavior

JVM behavior proof is present and passes.

## P3-1 mutation proof

Test file: `tools/tests/test_a13_p3_statusbar_clock_format_hot_path.py`

`contract_violations(realSource)` returns `[]`.

Real mutation tests verify these violation codes:

- `RESOLVED_FORMAT_CACHE_MISSING`
- `RESOURCE_ID_LAZY_CACHE_MISSING`
- `CACHE_RETAINS_CONTEXT`
- `CACHE_RETAINS_RESOURCES`
- `RAW_FORMAT_KEY_MISSING`
- `SHOW_SECONDS_KEY_MISSING`
- `IS24_KEY_MISSING`
- `HOUR_2D_KEY_MISSING`
- `REUSABLE_BUILDER_FIELD`
- `CUSTOM_FORMAT_ROUTED_THROUGH_CACHE`
- `PER_TICK_GET_STRING_MISSING`
- `CC_CLOCK_DISPATCH_CHANGED`
- `CC_DATE_DISPATCH_CHANGED`
- `ROM_FORMAT_CALL_CHANGED`
- `DEFAULT_PATH_CACHE_MISSING`
- `CACHE_CLASS_MISSING`

No fake `assertNotIn` self-fulfilling assertions remain.

## P3-QA-1 status

`QA_ACCEPTED / FROZEN`

Audit: `docs/audit/A13_P3_POST_P3_1_CANDIDATE_GATE.md`

Final external closure adopts:

- `STATIC_ACTIONABLE` remaining candidates = 0
- `DEVICE_EVIDENCE_FIRST` remaining:
  - `HorizMarginHook` (low-value borderline)
  - `LockScreenAlarmHook`
  - Launcher `force_fsg_nav_bar` stack-trace hook
  - `DisableDockSuggestHook`
  - `TimerItem.getTimePos` (low-value borderline)

## Test-hardening debt

```
NON_BLOCKING_TEST_HARDENING_DEBT:

`contract_violations()` in test_a13_p3_statusbar_clock_format_hot_path.py
does not explicitly reject a primary cached path that simultaneously
contains both:

  formatCache.resolveResourceId(...)

and

  resources.getIdentifier("fmt_time_12hour_minute", ...)

The current production source does not have this pattern. This is
recorded for future governance, not fixed in this closure.
```

## Launcher historical guard

File: `tools/tests/test_launcher_gesture_state_cache.py`

`SystemStatusBarClockAndMoreHooks.kt` is not in the launcher's allowed
main-source change set. `SubFragment.kt`, `AppSelector.kt`,
`ActivitySelector.kt` are also not present.

Status: `LAUNCHER HISTORICAL GUARD = RESTORED`

## H4 revalidation

`A13_HOOK_COST_MAP.json` `FRAME_OR_LAYOUT_HOT` records were spot-checked.
No statically proven isolated H4 candidate with repeated allocation,
reflection, or IPC cost was found. Existing H4 targets either do
negligible work, are setup-only, or already use per-instance cached
state.

`H4 STATIC_ACTIONABLE = 0` remains credible.

Wording: "No statically proven isolated H4 candidate found." Not
"A13 has no H4 hot paths."

## Candidate scoring summary

| Candidate | FREQ | WORK | ALLOC | REFL | IPC | STAT_CONF | ISOL | SAFE | DEVICE_DEP | Class |
|-----------|------|------|-------|------|-----|-----------|------|------|-----------|-------|
| HorizMargin | 1 | 1 | 1 | 0 | 0 | 1 | 2 | 2 | HIGH | DEVICE_EVIDENCE_FIRST / LIKELY_LOW_VALUE |
| LockScreenAlarm | 2 | 3 | 3 | 0 | 1 | 0 | 1 | 1 | HIGH | DEVICE_EVIDENCE_FIRST |
| Launcher stack trace | 1 | 2 | 1 | 0 | 0 | 0 | 1 | 0 | HIGH | DEVICE_EVIDENCE_FIRST |
| DisableDockSuggest | 1 | 1 | 1 | 0 | 0 | 0 | 1 | 0 | HIGH | DEVICE_EVIDENCE_FIRST |
| TimerItem getTimePos | 1 | 1 | 0 | 0 | 0 | 2 | 2 | 2 | MEDIUM | DEVICE_EVIDENCE_FIRST / LIKELY_LOW_VALUE |

## P3-2 decision

`P3-2 = DEFERRED / NOT_STARTED`

No remaining candidate passes the P3-2 selection gate:
- Proven repeated cost
- Not setup-only
- Not user-action-only
- Small isolated production scope
- Static proof possible
- Controlled configuration semantics
- Meaningful expected direction
- Low/controlled regression risk

## P3 static-stage conclusion

```
P3 = STATIC_QA_ACCEPTED_PENDING_EXACT_HEAD_ARTIFACT
```

P3-1 is the only confirmed and implemented H3 hot-path optimization.

## Remaining evidence dependencies

- `P0 runtime baseline` = `PENDING_DEVICE`
- `P1B runtime/ROM evidence` = `PENDING_DEVICE`
- `P2 runtime memory evidence` = `PENDING_DEVICE`
- `P3 runtime CPU/GC evidence` = `PENDING_DEVICE`

Static closure does not equal device verified.

## Next-stage options (not implemented)

- A. Device profiling / runtime evidence campaign
- B. P6 modularization boundary audit
- C. BUILD-1B AGP 9.4 compatibility evaluation

No automatic selection or implementation.

## Build gate summary

- `compileDebugKotlin`: OK
- `compileDebugJavaWithJavac`: OK
- `testDebugUnitTest`: OK
- `compileReleaseKotlin`: OK
- `compileReleaseJavaWithJavac`: OK
- `lintRelease`: OK
- `minifyReleaseWithR8`: OK
- `lintVitalAnalyzeRelease`: OK
- `lintVitalReportRelease`: OK
- `analyzeReleaseR8Config`: OK
- R8 outputs: `mapping.txt`, `usage.txt`, `seeds.txt`, `configuration.txt`, `resources.txt`
- No missing-class blocker, no unresolved keep-rule blocker, no new fatal R8 warning

## Python gate summary

- `python -m compileall tools`: OK
- `python -m unittest discover -s tools/tests -p "test_*.py"`: 1252 tests OK (skipped=2)
- `python tools/source_hazard_scan.py`: 0 findings
- `python tools/a13_hook_cost_scan.py --verify`: 669 records, stability OK
- `python tools/verify.py full`: OK
- `git diff --check`: OK

## Audit artifacts

`A13_HOOK_COST_MAP.json` and `A13_LEGACY_EXCEPTION_REGISTRY.json` not
regenerated. No drift during closure.

## Production invariant

`git diff af2971f..HEAD -- app/src/main` = empty at closure start.

## Final repository state

```
P3-CLOSURE = STATIC_QA_ACCEPTED_PENDING_EXACT_HEAD_ARTIFACT
P3 = STATIC_QA_ACCEPTED_PENDING_EXACT_HEAD_ARTIFACT
P3-0 = QA_ACCEPTED / FROZEN
P3-1 = QA_ACCEPTED / FROZEN
P3-QA-1 = QA_ACCEPTED / FROZEN
P3-2 = DEFERRED / NOT_STARTED
AGP_9_4 = DEFERRED
P6 = NOT_STARTED
```
