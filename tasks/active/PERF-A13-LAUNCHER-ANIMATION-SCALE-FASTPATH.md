# PERF-A13-LAUNCHER-ANIMATION-SCALE-FASTPATH

## Goal
Replace the reflection/binder-based `HookUtils.getAnimationScale(2)` used by
`LauncherAnimationHooks` with the Android 13 in-process fast path
`ValueAnimator.getDurationScale()`, while preserving all existing animation
semantics.

## Production changes
- `app/src/main/java/tv/withaibuild/customiuizer/mods/LauncherAnimationHooks.kt`
  - Add `import android.animation.ValueAnimator`
  - Add `effectiveAnimatorScale(rawScale)` and `currentAnimatorScale()`
  - Use `currentAnimatorScale()` in both `FixAnimHook` callback `before` methods
  - Keep `scaleStiffness`, all stiffness fields and `mRadioStiffness` fallback
  - Preserve `scale == 1.0f` early return and `scale == 0f -> 0.01f` mapping
- `app/src/main/java/tv/withaibuild/customiuizer/utils/HookUtils.kt`
  - Remove `getAnimationScale(type: Int)`
  - Remove unused `android.os.IBinder` import

## Test additions
- `app/src/test/java/tv/withaibuild/customiuizer/mods/LauncherAnimationScaleTest.kt`
- `tools/tests/test_launcher_animation_scale_fastpath.py`
- `tools/tests/test_hookutils_diagnostics.py` updated for removed method

## Verification
- `python -m compileall tools`
- `python -m unittest discover -s tools/tests -p "test_*.py"`
- `gradlew.bat :app:compileDebugKotlin`
- `gradlew.bat :app:testDebugUnitTest`
- `gradlew.bat :app:lintDebug`
- `gradlew.bat :app:testDebugUnitTest --dependency-verification=strict`
- `python tools/source_hazard_scan.py --path app/src/main/java`
- `python tools/verify.py fast --changed`
- `python tools/verify.py full`
- `git diff --check`
- `Get-ChildItem ... .printStackTrace(` returns no output
