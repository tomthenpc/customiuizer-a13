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
1. Each target catch block begins with `throwIfFatal(t);`.
2. `throwIfFatal` is called with the full caught `Throwable`, never `t.getCause()`.
3. Fatal errors (`OutOfMemoryError`, `ThreadDeath`, `VirtualMachineError`) are rethrown as the original instance found in the cause chain.
4. Ordinary callback failures remain isolated:
   - Observers log and continue.
   - `guarded(Runnable)` logs.
   - Named `guarded` logs once per process via `logGuardedFailure`.
   - Returning variants return the caller-supplied fallback.
5. No direct `instanceof OutOfMemoryError` / `ThreadDeath` / `VirtualMachineError` checks in the seven target catch blocks.
6. Observer loops, weak-reference/cleared behavior, `sawCleared` cleanup, log-once map, and `CallbackFailureLogger` contract remain unchanged.

## Verification
- New Python contract test: `tools/tests/test_module_helper_callback_fatality.py` (42 tests).
- Updated JVM test: `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelperGuardedTest.kt` (32 tests total).
- `python -m compileall tools` passed.
- `python -m unittest discover -s tools/tests -p "test_*.py"` passed (741 tests).
- `python tools/source_hazard_scan.py --path app/src/main/java` passed (0 reviewed, 0 new).
- `python tools/verify.py fast --changed` passed.
- `python tools/verify.py full` passed.
- `\gradlew.bat :app:testDebugUnitTest --dependency-verification=strict` passed.
- `\gradlew.bat :app:lintDebug` passed.
- `git diff --check` passed.
- 9 mutations applied and restored; new tests correctly failed each time.

## SHAs
- Base SHA: `4fde0e26379090db81bbca8ac85e777c30560a0a`
- Implementation SHA: `a359cb8a264bbb656d195d136f5c692433f96a52`

## Status
Completed.
