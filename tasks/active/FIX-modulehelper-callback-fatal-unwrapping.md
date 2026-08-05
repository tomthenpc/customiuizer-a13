# FIX-modulehelper-callback-fatal-unwrapping

## Goal
Unify `ModuleHelper` callback isolation boundaries so that all observer and `guarded` catch blocks use the existing cause-chain aware `throwIfFatal` helper for fatal-error propagation.

## Scope
- `handlePreferenceChanged`:
  - Unkeyed observer loop catch
  - Keyed observer loop catch
  - Owned observer loop catch
- `guarded(Runnable)`
- `guarded(String, Runnable, CallbackFailureLogger)` (package-private execution overload)
- `guarded(T, Callable<T>)`
- `guarded(String, T, Callable<T>, CallbackFailureLogger)` (package-private execution overload)

The two public forwarding overloads `guarded(String, Runnable)` and `guarded(String, T, Callable<T>)` must remain pure delegates.

## Contract
1. Each target catch block begins with `throwIfFatal(t);` (or `throwIfFatal(t2)` if applicable, none in this task).
2. `throwIfFatal` must be called with the full caught `Throwable`, never `t.getCause()`.
3. Fatal errors (`OutOfMemoryError`, `ThreadDeath`, `VirtualMachineError`) are rethrown as the original instance found in the cause chain.
4. Ordinary callback failures remain isolated:
   - Observers log and continue.
   - `guarded(Runnable)` logs.
   - Named `guarded` logs once per process via `logGuardedFailure`.
   - Returning variants return the caller-supplied fallback.
5. No direct `instanceof OutOfMemoryError` / `ThreadDeath` / `VirtualMachineError` checks in the seven target catch blocks.
6. Observer loops, weak-reference/cleared behavior, `sawCleared` cleanup, log-once map, and `CallbackFailureLogger` contract must remain unchanged.

## Verification
- New Python contract test: `tools/tests/test_module_helper_callback_fatality.py`.
- Updated JVM test: `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelperGuardedTest.kt`.
- `python tools/verify.py full`
- `python -m compileall tools`
- `python -m unittest discover -s tools/tests -p "test_*.py"`
- `python tools/source_hazard_scan.py --path app/src/main/java`
- `python tools/verify.py fast --changed`
- `git diff --check`

## Status
In progress.
