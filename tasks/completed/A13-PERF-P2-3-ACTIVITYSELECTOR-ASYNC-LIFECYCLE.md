# A13-PERF-P2-3 — ActivitySelector Async Activity-List Lifecycle / Worker Ownership Cleanup

## Status

P2-3 = QA_ACCEPTED / FROZEN

R1 reopen reasons (resolved):
- NEW_FRAGMENT_CACHE_SEMANTICS_NOT_PRESENT_IN_BASE
- VIEW_DESTROYED_METADATA_RETENTION_UNNECESSARILY_EXTENDED

RESULT =
ActivitySelector delayed kickoff is View-lifecycle cancellable;
PackageManager worker retains applicationContext/package snapshot rather than Activity;
background results are built in worker-local storage;
Fragment activities field is updated only on main thread;
completion applies UI only to a live current View;
view recreation does not start duplicate activity queries;
R1: results are view-scoped — no Fragment-lifetime cache, activities cleared onDestroyView.

## Base

- Base SHA: `18843359ec7290f325d4bccab3123ddf8c1103ed`
- Branch: `devin/a13-memory-performance-optimization`

## Context

- P2-QA-1 = QA_ACCEPTED / FROZEN
- P2-2 = QA_ACCEPTED / FROZEN
- BUILD-1A = QA_ACCEPTED / FROZEN

## initialized Symbol Provenance

ActivitySelector base had no initialized state.

R0 incorrectly introduced one: a `private var initialized = false` field set to
`true` on first successful load, with a cache fast-path in `onActivityCreated()`
that skipped the PackageManager query and called `renderActivities()` directly
when `initialized` was already true. This cache semantics was not present in the
base ActivitySelector and unnecessarily extended metadata retention beyond the
View lifetime.

R1 removes that cache behavior and retains only the lifecycle/worker ownership
correction:
- `initialized` field, reads, and writes removed.
- `onActivityCreated()` restored to historical query semantics: in-flight → retry
  demand; else → schedule a fresh activity metadata query.
- Success completion commits `loadedActivities` to the Fragment `activities`
  field only when a live View is present (`isAdded && view != null`); otherwise
  the result is discarded.
- `onDestroyView()` clears `activities` before `super.onDestroyView()`.
- `activityLoadInFlight` is NOT cleared in `onDestroyView()` (it reflects actual
  worker lifetime; clearing it would let a new View start a duplicate query).

## Problem

ActivitySelector.onActivityCreated() schedules an anonymous postDelayed callback
that captures the Fragment, starts a raw Thread that captures `activity` (Activity),
uses `act.packageManager` and `act.runOnUiThread(process)` to post results back.
The Thread directly mutates the Fragment `activities` field from background.
No onDestroyView cancellation, no lifecycle gate on completion.

## Fix

- Add `pendingActivityLoadStart: Runnable?` single-slot cancellable kickoff.
- Add `activityLoadInFlight` + `retryActivityLoadAfterInFlight` single-flight.
- Replace Activity capture with `applicationContext` + immutable package snapshot.
- Use `WeakReference<ActivitySelector>` for completion handoff.
- Extract `renderActivities()` instance method for main-thread UI.
- Extract `scheduleActivityLoad()` instance method for delayed kickoff.
- Extract `onActivityLoadFinished()` instance method for completion.
- Move Thread creation to `companion object` `startActivityLoadWorker`.
- Worker builds results in local `ArrayList<AppData>`, not Fragment field.
- Fragment `activities` field updated only on main thread.
- Add `onDestroyView()` cleanup before `super.onDestroyView()`.
