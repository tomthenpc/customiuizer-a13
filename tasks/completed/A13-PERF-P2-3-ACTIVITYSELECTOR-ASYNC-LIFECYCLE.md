# A13-PERF-P2-3 — ActivitySelector Async Activity-List Lifecycle / Worker Ownership Cleanup

## Status

P2-3 = QA_ACCEPTED

RESULT =
ActivitySelector delayed kickoff is View-lifecycle cancellable;
PackageManager worker retains applicationContext/package snapshot rather than Activity;
background results are built in worker-local storage;
Fragment activities field is updated only on main thread;
completion applies UI only to a live current View;
view recreation does not start duplicate activity queries.

## Base

- Base SHA: `18843359ec7290f325d4bccab3123ddf8c1103ed`
- Branch: `devin/a13-memory-performance-optimization`

## Context

- P2-QA-1 = QA_ACCEPTED / FROZEN
- P2-2 = QA_ACCEPTED / FROZEN
- BUILD-1A = QA_ACCEPTED / FROZEN

## initialized Symbol Provenance

ActivitySelector has NO `initialized` field. Source search confirms `initialized`
only exists in AppSelector.kt (P2-2) and ColorCircle.kt. ActivitySelector currently
re-queries PackageManager on every onActivityCreated call with no caching.

P2-3 introduces a new `initialized` field specific to ActivitySelector, with
semantics: once a successful activity-list load completes, the results are cached
in the Fragment `activities` field and `initialized = true` is set. Subsequent
onActivityCreated calls skip the query and call renderActivities() directly.

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
