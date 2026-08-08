# A13-PERF-P2-1 — SubFragment Delayed Highlight Scroll Lifecycle Cleanup

## Status

P2-1 = QA_ACCEPTED
DEVICE_EVIDENCE = NOT_REQUIRED_FOR_THIS_BOUNDED_UI_LIFECYCLE_FIX

## Result

Bounded delayed callback now explicitly cancelled at View lifecycle teardown.
The 380ms highlight-scroll Runnable is stored in a single-slot field, self-clears
after execution, is cancelled on duplicate schedule, handles post failure, and
is removed in onDestroyView() before super.onDestroyView().

## Base

- Base SHA: `dbf655870137f8c4003d2521d434fe8cf6a48dc5`
- Branch: `devin/a13-memory-performance-optimization`

## Context

- P2-0 = QA_ACCEPTED / FROZEN
- BUILD-1A = QA_ACCEPTED / INTEGRATED

## Problem

`SubFragment.onStart()` schedules a 380ms delayed highlight-scroll callback via
`view?.postDelayed(..., 380)`. If the Fragment View is destroyed before the
callback fires (e.g. user navigates away quickly), the callback retains a
reference to the View hierarchy via the captured `mList` (RecyclerView) and
`smoothScroller` until the delayed message is dispatched or the View is detached.

## Fix

- Add `private var pendingHighlightScroll: Runnable? = null` single-slot field.
- Replace anonymous lambda with a named `Runnable` that self-clears the slot
  after execution.
- Cancel any previous pending callback before scheduling a new one.
- Handle `postDelayed` failure by clearing the slot.
- Add `onDestroyView()` that calls `removeCallbacks` and clears the slot
  **before** `super.onDestroyView()`.

## Preserved behavior

- No `highlightKey` → no scroll.
- No `listView` → no scroll.
- Adapter not `PreferencePositionCallback` → no scroll.
- position < 9 → no scroll.
- position >= 9 → delayed ~380ms scroll.
- `highlightKey = null` one-shot semantics preserved.
- `SNAP_TO_START` preserved.
- `targetPosition` preserved.
- 380ms delay preserved.
