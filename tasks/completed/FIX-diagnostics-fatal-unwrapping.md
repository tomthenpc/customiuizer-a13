# FIX-diagnostics-fatal-unwrapping

## Base SHA
2a9fa8422167c6061a110a7d9ef45a48489bb772

## Implementation SHA
0e40637576f7fada97278c74537a3f3586492ebe

## Summary
- Added `RuntimeFatality` shared helper for direct/cause-chain fatal unwrapping
  (`OutOfMemoryError`, `ThreadDeath`, `VirtualMachineError`) up to 8 levels.
- Converged `DiagnosticRecorder` injected/default/fallback logger isolation to use
  `RuntimeFatality` and `logFallbackSafely`.
- Converged `RomEnvironmentDiagnostics.recordSafely` to use `RuntimeFatality`.
- Added JVM tests for `RuntimeFatality`, `DiagnosticRecorder`, and `FeatureRuntime`.
- Added `tools/tests/test_diagnostics_fatal_unwrapping.py` with 45 static contract
  tests.

## Python test count
- New static contract tests: 45
- Full `python -m unittest discover -s tools/tests -p "test_*.py"`: 786 passed

## JVM test result
- `:app:testDebugUnitTest` BUILD SUCCESSFUL
- Targeted `RuntimeFatalityTest`, `DiagnosticRecorderTest`, `FeatureRuntimeTest`
  passed

## Mutation results
All 10 temporary mutations failed as expected and were restored:
1. Delete `RuntimeFatality` `ThreadDeath` branch
2. Delete cause-chain traversal (outermost-only)
3. Expand fatal check to `current is Error`
4. Reorder injected logger catch (fatal helper after fallback call)
5. Delete `logFallbackSafely` fatal helper
6. `logger == null` path uses direct `XposedHelpers.log(logLine)`
7. `recordSafely` checks `t.cause` instead of `t`
8. Delete `reset()` `fallbackLogger` restore
9. Rethrow ordinary fallback logger failure
10. Rename `rom.environment` diagnostic id

## Verification results
- `python -m compileall tools`: OK
- `python -m unittest discover -s tools/tests -p "test_*.py"`: OK (786)
- `gradlew.bat :app:compileDebugKotlin`: OK
- `gradlew.bat :app:compileDebugJavaWithJavac`: OK
- `gradlew.bat :app:testDebugUnitTest`: OK
- `gradlew.bat :app:lintDebug`: OK
- `gradlew.bat :app:testDebugUnitTest --dependency-verification=strict`: OK
- `python tools/source_hazard_scan.py --path app/src/main/java`: 0 reviewed, 0 new
- `python tools/verify.py fast --changed`: OK
- `python tools/verify.py full`: OK
- `git diff --check`: OK
- `Get-ChildItem ... .printStackTrace(`: no output
