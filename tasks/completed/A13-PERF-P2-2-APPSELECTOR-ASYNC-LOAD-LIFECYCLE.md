# A13-PERF-P2-2 — AppSelector Async App-List Load Lifecycle / Owner Decoupling

## Status

P2-2 = QA_ACCEPTED

RESULT =
AppSelector delayed load kickoff is View-lifecycle cancellable;
background package queries retain applicationContext rather than Activity;
completion uses weak Fragment ownership and only applies to a live current View.

## Base

- Base SHA: `6e95d73129617abd9028bf441defa6311ea7a973`
- Branch: `devin/a13-memory-performance-optimization`

## Context

- P2-1 = QA_ACCEPTED / FROZEN
- P2-0 = QA_ACCEPTED / FROZEN
- BUILD-1A = QA_ACCEPTED / INTEGRATED / FROZEN

## Problem

`AppSelector.onActivityCreated()` schedules a delayed anonymous callback via
`view?.postDelayed(..., animDur)` that captures the Activity, starts a raw
`Thread` that captures the Activity, and uses `act.runOnUiThread(process)` to
post results back. If the Fragment View is destroyed before the callback fires
or the background Thread completes, the Activity and View hierarchy are retained
by the Thread and the delayed message.

## Fix

- Add `pendingAppLoadStart: Runnable?` single-slot field for cancellable kickoff.
- Add `appLoadInFlight` single-flight boolean (main-thread only).
- Replace Activity capture with `applicationContext` in background Thread.
- Use `WeakReference<AppSelector>` for completion handoff.
- Use `appContext.mainExecutor.execute` instead of `act.runOnUiThread`.
- Completion clears `appLoadInFlight`, sets `initialized=true` on success,
  and only calls `process?.run()` when `isAdded && view != null`.
- Failure path always clears `appLoadInFlight`.
- Add `onDestroyView()` that cancels pending kickoff before `super.onDestroyView()`.

## Preserved behavior

- `initialized == true` → `process?.run()` (no reload).
- `animDur` delay preserved.
- Cache check (`Helpers.xxxList == null`) preserved.
- Selection dispatch (openwith/share/installed/launchable) preserved.
- Adapter types, onItemClick, progressBar behavior preserved.
