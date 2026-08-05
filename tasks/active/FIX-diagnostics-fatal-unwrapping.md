# FIX-diagnostics-fatal-unwrapping

## Goal
Improve fatal-error propagation in A13 feature diagnostics so that best-effort
isolation logic:
- still swallows ordinary diagnostic record/log exceptions;
- propagates direct or cause-chain `OutOfMemoryError`, `ThreadDeath` and
  `VirtualMachineError` unchanged;
- inspects up to 8 cause-chain levels;
- rethrows the original fatal instance;
- does not widen `AssertionError`, `LinkageError` or ordinary `Error` into fatal.

## Production changes
- Add `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/RuntimeFatality.kt`
  as an internal shared helper.
- Update `app/src/main/java/tv/withaibuild/customiuizer/mods/diagnostics/DiagnosticRecorder.kt`
  to use `RuntimeFatality` for injected and fallback logger isolation.
- Update `app/src/main/java/tv/withaibuild/customiuizer/mods/compat/RomEnvironmentDiagnostics.kt`
  to use `RuntimeFatality` in `recordSafely`.
- Keep `FeatureDispatcher`, `ModuleHelper`, `XposedHelpers`, hook installer/resolver
  and baseline unchanged.

## Test additions
- `app/src/test/java/tv/withaibuild/customiuizer/mods/utils/RuntimeFatalityTest.kt`
- Extended `app/src/test/java/tv/withaibuild/customiuizer/mods/diagnostics/DiagnosticRecorderTest.kt`
- Extended `app/src/test/java/tv/withaibuild/customiuizer/mods/catalog/FeatureRuntimeTest.kt`
- `tools/tests/test_diagnostics_fatal_unwrapping.py`

## Verification
- `python -m compileall tools`
- `python -m unittest discover -s tools/tests -p "test_*.py"`
- `gradlew.bat :app:compileDebugKotlin`
- `gradlew.bat :app:compileDebugJavaWithJavac`
- `gradlew.bat :app:testDebugUnitTest`
- `gradlew.bat :app:lintDebug`
- `gradlew.bat :app:testDebugUnitTest --dependency-verification=strict`
- `python tools/source_hazard_scan.py --path app/src/main/java`
- `python tools/verify.py fast --changed`
- `python tools/verify.py full`
- `git diff --check`
- `Get-ChildItem ... .printStackTrace(` returns no output
