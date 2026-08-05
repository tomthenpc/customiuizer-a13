# A13 Upstream Issue Regression Audit

> Branch: `devin/a13-rom-intelligence-audit`
> Base: `ac49cae8deb4fe24df2621c0a2f2aae9d510ba86`
> Upstream: `https://github.com/MonwF/customiuizer`
> Date: 2026-08-01
> Scope: A13 (MIUI 14 / HyperOS 1) risk patterns only — no ADB, no real-device confirmation.

## Legend

| Classification | Meaning |
|---|---|
| `DIRECTLY_APPLICABLE` | Same feature, same failure mode, likely reproducible on A13 if the same target bundle is present. |
| `PATTERN_APPLICABLE` | Failure pattern matches A13 code, but the exact ROM / target / version differs. |
| `NOT_APPLICABLE` | Feature not present in A13 branch, or upstream OS version out of scope. |
| `NEEDS_DEVICE` | Cannot be confirmed without an exported LSPosed log or manual reproduction. |
| `INSUFFICIENT_INFORMATION` | Upstream report lacks stack, target version, or repro steps. |

## Issue inventory

| # | URL | Device / ROM | Upstream version | Classification | Fault | Key stack / symptom | A13 feature / files | Same code mode? | Needs device? | Risk |
|---|---|---|---|---|---|---|---|---|---|---|
| 660 | https://github.com/MonwF/customiuizer/issues/660 | Xiaomi 17 Pro, HyperOS 3.0.306 | 26.05.04-test | PATTERN_APPLICABLE | Status bar battery temp/current display causes `SystemUI` crash with `IndexOutOfBoundsException: index=8 count=7` in `ViewGroup.addInArray` via `IconManager.addHolder`. | `IconManager.addHolder` → `StatusBarIconControllerImpl.addIconGroup` → `KeyguardStatusBarViewController.onViewAttached` | `batteryIndicator`, `SystemUIBatteryHooks.kt`, `SystemUIStatusBarHooks.kt` (dual-row icon area moves) | YES — A13 inserts/moves `NotificationIconArea` views when dual-row / battery features are enabled. | YES | HIGH |
| 624 | https://github.com/MonwF/customiuizer/issues/624 | Redmi K70, HyperOS 2.0.217.0 | 25.09.25 | PATTERN_APPLICABLE | Status bar "show seconds" is enabled but the clock stays still. | No stack; UI symptom only | `STATUSBAR_CLOCK_TWEAK`, `SystemStatusBarClockAndMoreHooks.kt`, `CanaryContracts.statusBarClockTweak` | POSSIBLE — A13 uses `MiuiStatusBarClockController.fireTimeChange` and `MiuiClock.updateTime`; a single-shot or stale callback could produce the same symptom. | YES | MEDIUM |
| 665 | https://github.com/MonwF/customiuizer/issues/665 | Pocopad, HyperOS 3 | 25.09.25 | PATTERN_APPLICABLE | Notification "open in separate window" causes black screen / crash. | No stack provided | `SystemUINotificationHooks`, `SystemNotificationPopupsHooks` | POSSIBLE — A13 has notification / floating-window hooks; wrong `Activity` / `Window` routing in a new ROM version could crash. | YES | MEDIUM |
| 646 | https://github.com/MonwF/customiuizer/issues/646 | Various | 26.04.05 | PATTERN_APPLICABLE | "Switch to previous app" via navigation buttons stopped working, has 1s delay. | Launcher / System gesture hook mismatch | `LauncherGestureHooks`, `SystemLockScreenMoreHooks` | POSSIBLE — gesture targets differ between MIUI/HyperOS versions. | YES | MEDIUM |
| 649 | https://github.com/MonwF/customiuizer/issues/649 | Various | latest | DIRECTLY_APPLICABLE | Double-tap power to camera broken when "long press power for flashlight" is enabled. | Interference between lockscreen gesture hooks | `SystemLockScreenHooks`, `Controls` | YES — same feature and same `KeyEvent` intercept path on A13. | YES | LOW |
| 592 | https://github.com/MonwF/customiuizer/issues/592 | Xiaomi 15 Ultra, HyperOS 2.0.x | 20.04.30-test | PATTERN_APPLICABLE | Launcher custom gestures do not work on newer HyperOS / Launcher builds. | Gesture target method missing in newer Launcher | `LauncherGestureHooks` | POSSIBLE — A13 contracts not yet hardened for H1 variants. | YES | MEDIUM |
| 7 | https://github.com/MonwF/customiuizer/issues/7 | Redmi K40, MIUI 13 / Android 12 | old | NOT_APPLICABLE | Multiple hooks not working on Android 12 / MIUI 13. | Various `Failed to hook` LSPosed logs | N/A | NO — A13 branch is Android 13 / MIUI 14 only. | — | N/A |
| 661 | https://github.com/MonwF/customiuizer/issues/661 | Various | latest | PATTERN_APPLICABLE | Status bar height minimum 12 vs 0; visual artifact request. | UI layout / top mask | `SystemUIStatusBarHooks`, `SystemStatusBarMoreHooks` | POSSIBLE — A13 has status bar height / padding hooks, but the requested min value is a UI policy, not a crash. | YES | LOW |
| 477 | https://github.com/MonwF/customiuizer/issues/477 | Xiaomi 14 Ultra | ? | PATTERN_APPLICABLE | Screenshot format `png` ignored, always saved as `jpg`. | `com.miui.screenshot` hook target changed | `SystemAudioAndVisualAndMoreHooks.ScreenshotConfigHook` | POSSIBLE — A13 hooks `com.miui.screenshot`; target method may differ in newer ROM. | YES | LOW |
| 395 | https://github.com/MonwF/customiuizer/issues/395 | MIUI 14 | ? | DIRECTLY_APPLICABLE | NFC tag notification cannot be disabled. | System notification policy | `SystemNotificationAndShareHooks` or `Various` | YES — A13 has notification / system hooks that could be mapped to this. | YES | LOW |
| 570 | https://github.com/MonwF/customiuizer/issues/570 | MIUI / HyperOS | ? | PATTERN_APPLICABLE | Weekday text too long in status bar clock. | Date format string `EEEEE` is a workaround | `SystemStatusBarClockAndMoreHooks`, `SystemUIStatusBarHooks` | POSSIBLE — clock format hooks depend on `MiuiClock` date format. | YES | LOW |

## Required upstream regression tests

### 1. Status-bar temperature / current / battery custom view attach (Issue #660 pattern)

Failure: `IndexOutOfBoundsException` when a custom View is added to the icon area and the owner / child index becomes stale.

Static proof required:

* Before any `addView` in a SystemUI hook, the code must `removeView` the same child from any prior parent, or create a brand-new child.
* The target parent is discovered by reflection; a `null` or wrong-type parent must early-return, not fall through to `addView`.
* No hook installs `addHolder` / `setIconsVisibility` without a `ModuleHelper.guarded` outer boundary.
* `BatteryIndicator` installation is wrapped by `FeatureDispatcher` and `InstallSummary`, so a failed install is not silently converted to success.

A13 files to inspect:

* `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt`
* `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt`
* `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/CanaryContracts.kt` (`batteryIndicator` contract)

### 2. Status-bar seconds / clock update (Issue #624 pattern)

Failure: The clock is static when "show seconds" is enabled because the update callback only fires once or the old handler is not replaced on re-attach.

Static proof required:

* `MiuiStatusBarClockController.fireTimeChange` is hooked and calls the original / a valid update source.
* `MiuiClock.updateTime` is present and used.
* The update Runnable / handler is owner-bound and is stopped/removed when the clock View detaches, then re-created on attach.
* `STATUSBAR_CLOCK_TWEAK` cannot return `INSTALLER_FAILED` and be logged as `DISPATCHED` (see `InstallOutcome`, `DiagnosticRecorder`).
* No REQUIRED target has been downgraded to OPTIONAL.

A13 files to inspect:

* `app/src/main/java/tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt`
* `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/CanaryContracts.kt` (`statusBarClockTweak` contract)
* `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt`

### 3. Launcher / receiver / observer lifecycle (Issues #649, #592, #646 patterns)

Failure: Multiple attach, configuration change, or process rebuild causes duplicate receivers, duplicate business objects, or use of stale owners.

Static proof required:

* `LauncherInstaller.handleLoadLauncher` only runs inside `Application.attach` and is protected by `lpparam.isFirstPackage()`.
* `ModuleHelper` owned/module receivers track `owner` identity; re-registering the same key does not create a second broadcast.
* Feature hooks check `FeatureDispatcher` state and do not install a second `Xposed` hook if the first is still registered.
* `MainModule.onPackageReady` early-returns on `!lpparam.isFirstPackage()`.

A13 files to inspect:

* `app/src/main/java/tv/withaibuild/customiuizer/MainModule.java`
* `app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java`
* `app/src/main/java/tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java`
* `app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureDispatcher.kt`

## How this audit was produced

* Upstream issues were collected from `https://github.com/MonwF/customiuizer/issues` using public web search.
* Only open/closed issues matching the keywords `Android 13`, `MIUI 14`, `HyperOS 1`, `SystemUI crash`, `status bar`, `clock / seconds`, `temperature / current`, `battery indicator`, `network speed`, `Launcher`, `folder`, `recent tasks`, `lockscreen`, `notification`, `receiver`, `duplicate view`, `process`, `ClassLoader` were retained.
* Issue #660 and #624 are from newer HyperOS versions and are therefore treated as **risk patterns**, not as A13 already-reproduced.
* No ADB, logcat, dumpsys, or device-side script was run.
