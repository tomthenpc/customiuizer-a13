# A13-PERF-P3-1 — Status Bar Clock Format-Resolution Hot-Path Cache

## Status

P3-0 = QA_ACCEPTED / FROZEN
P3-1 = QA_ACCEPTED

BASE =
10b8ecfd4c4d6376c07079c2057f8635fff9bee7

P2 = STATIC_QA_ACCEPTED / FROZEN
AGP_9_4 = DEFERRED
P6 = NOT_STARTED

RESULT =
status-bar default clock format resource IDs are lazily resolved;
derived format transformation is cached by current raw resource format
and format preferences;
configuration/resource text changes invalidate naturally;
callback-local ROM format StringBuilders remain uncached.

## Scope

Cache status-bar clock default-format resolution in
`SystemStatusBarClockAndMoreHooks.kt` only.

Eliminate steady-state per-tick:
- `Resources.getIdentifier(...)` (lazy resource ID cache)
- `String.replaceFirst(":mm", ":mm:ss")` (cached format transformation)
- `replaceClockHourToken(...)` (cached format transformation)

Retain per-tick:
- 4 preference reads (live preference behavior)
- `getString(fmtResId)` (configuration/locale correctness)
- 2 callback-local StringBuilders (ROM mCalendar.format mutable builders)
- `XposedHelpers.callMethod(mCalendar, "format", ...)` (existing cache)

## External QA implementation constraints

1. Cache key includes raw resource format string, not prefs only.
   This ensures locale/configuration changes naturally invalidate.
2. Callback StringBuilders intentionally remain local — they are handed
   to ROM mutable method; P3-1 does not prove ROM retained/reentrant
   behavior.
