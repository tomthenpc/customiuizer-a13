# PERF-A13-LAUNCHER-ANIMATION-SCALE-FASTPATH

## Base SHA
9b7b695e18c765a24db51d310aa5a21af9718b2f

## Implementation SHA
b9619ad9507e38851b29d4d8f7915461976aac13

## Summary
- Replaced `HookUtils.getAnimationScale(2)` in `LauncherAnimationHooks` with
  `ValueAnimator.getDurationScale()`.
- Added `effectiveAnimatorScale(rawScale)` to map `0f` to `0.01f` while leaving
  all other values unchanged.
- Added `currentAnimatorScale()` fast path.
- Preserved `scale == 1.0f` early return, all stiffness field updates and the
  `mRadioStiffness` fallback.
- Removed `getAnimationScale(type: Int)` from `HookUtils` along with the
  `android.os.IBinder` import.
- Added JVM test `LauncherAnimationScaleTest` (7 tests).
- Added 24 static contract tests in
  `tools/tests/test_launcher_animation_scale_fastpath.py`.
- Updated `tools/tests/test_hookutils_diagnostics.py` and
  regenerated `docs/audit/A13_LEGACY_EXCEPTION_REGISTRY.json` to reflect the
  source tree change.

## git grep caller result
- `HookUtils.getAnimationScale(2)` now appears in no production caller.
- `Helpers.getAnimationScale(0/1/2)` remains in `System.kt` settings UI and was
  not within this task's scope (different helper, not removed).
- Stale `HookUtils.getAnimationScale(0)` test reference removed from
  `HookUtilsDiagnosticsTest.kt`.

## Python test count
- New static contract tests: 24
- Full `python -m unittest discover -s tools/tests -p "test_*.py"`: 810 passed

## JVM result
- `:app:testDebugUnitTest` BUILD SUCCESSFUL
- `LauncherAnimationScaleTest` (7 tests) passed

## Mutation results
All 8 temporary mutations failed as expected and were restored:
1. Revert to `HookUtils.getAnimationScale(2)`
2. Call `ValueAnimator.getDurationScale()` twice in a callback
3. Delete `scale == 1.0f` early return
4. Delete `0f -> 0.01f` mapping
5. Fix `0.5f` to `0.99f`
6. Add `Settings.Global.getFloat` to fast path
7. Add `Thread.currentThread().stackTrace` to fast path
8. Delete `mRadioStiffness` fallback

## Verification results
- `python -m compileall tools`: OK
- `python -m unittest discover -s tools/tests -p "test_*.py"`: OK (810)
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
