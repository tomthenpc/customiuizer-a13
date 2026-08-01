# A13 Process Exceptions

> Branch: `devin/a13-rom-intelligence-audit`
> Base: `ac49cae8deb4fe24df2621c0a2f2aae9d510ba86`

This document records explicit process/package routing decisions and the gaps found during the audit.

## Static findings

1. **Package vs process distinction** — `MainModule.needLoadPrefs()` is a package allowlist; it does not distinguish `com.android.systemui` main process from plugin processes. Most catalog `FeatureSpec`s use `ProcessTarget` which is process-based.
2. **Boolean precedence** — No obvious `&&`/`||` precedence bugs were detected in `MainModule.java`, but the large `if` chains in `PackageInstallerRouter.java` rely on short-circuit evaluation.
3. **Settings main vs `:remote`** — `MainModule.onPackageReady` explicitly returns when `pkg.equals("com.android.settings") && !"com.android.settings".equals(processName)`. Guard present: True.
4. **SecurityCenter `bootaware`** — `MainModule.onPackageReady` returns when `com.miui.securitycenter` and `processName` is `com.miui.securitycenter.bootaware`. Guard present: True.
5. **SystemUI plugin ClassLoader** — Catalog features use `runtime.classLoader` / `lpparam.getClassLoader()`; legacy installers also use `lpparam.getClassLoader()`. No evidence of using the host ClassLoader in a plugin context.
6. **`isFirstPackage` gate** — `MainModule.onPackageReady` returns if `!lpparam.isFirstPackage()`. Present: True.
7. **Dual install entry points** — Some legacy features are called both from `SystemUiInstaller` and `PackageInstallerRouter` (e.g. `system_separatevolume` in Settings and SystemUI). Catalog features are dispatched through `FeatureDispatcher` only.
8. **Disabled feature skip** — Catalog `FeatureSpec.condition` is evaluated before `compatibilityCheck` and `installer`; legacy installers check `mPrefs.getBoolean`/`getInt` directly before calling the hook.
9. **Auxiliary process default** — NetworkStack, `com.android.location.fused`, Settings `:remote`, SecurityCenter `bootaware`, and input methods have explicit early returns or dedicated blocks.

## Catalog process mapping

| Feature ID | Process target | Evidence |
|---|---|---|
| "packagePermissions" | system_server / android | STATIC_RESOLVED |
| "statusBarClockTweak" | com.android.systemui | STATIC_RESOLVED |
| "autoBrightnessRange" | system_server / android | STATIC_RESOLVED |
| "muffledVibration" | system_server / android | STATIC_RESOLVED |
| "noMoreIcon" | com.android.systemui | STATIC_RESOLVED |
| "batteryIndicator" | com.android.systemui | STATIC_RESOLVED |
| "noClockHide" | com.miui.home / com.mi.android.globallauncher | STATIC_RESOLVED |
| "noWidgetOnly" | com.miui.home / com.mi.android.globallauncher | STATIC_RESOLVED |
| "screenDimTime" | system_server / android | STATIC_RESOLVED |
| "firstVolumePress" | system_server / android | STATIC_RESOLVED |
| "networkIndicatorWifi" | com.android.systemui | STATIC_RESOLVED |
| "muteVisibleNotifications" | com.android.systemui | STATIC_RESOLVED |
| "hideLauncherTitles" | com.miui.home / com.mi.android.globallauncher | STATIC_RESOLVED |
| "fixAppInfoLaunch" | com.miui.home / com.mi.android.globallauncher | STATIC_RESOLVED |
| "hideProximityWarning" | system_server / android | STATIC_RESOLVED |
| "clearAllTasks" | system_server / android | STATIC_RESOLVED |
| "hideDismissView" | com.android.systemui | STATIC_RESOLVED |
| "hideLockScreenHint" | com.android.systemui | STATIC_RESOLVED |
| "folderColumns" | com.miui.home / com.mi.android.globallauncher | STATIC_RESOLVED |
| "titleTopMargin" | com.miui.home / com.mi.android.globallauncher | STATIC_RESOLVED |
| "noLightUpOnCharge" | system_server / android | STATIC_RESOLVED |
| "allRotations" | system_server / android | STATIC_RESOLVED |
| "noNetworkSpeedSeparator" | com.android.systemui | STATIC_RESOLVED |
| "hideIconsClock" | com.android.systemui | STATIC_RESOLVED |
| "noUnlockAnimation" | com.miui.home / com.mi.android.globallauncher | STATIC_RESOLVED |

## Process target definitions

```text
ProcessTarget.SystemServer  -> processName == "android" || "system_server"
ProcessTarget.SystemUI      -> processName == "com.android.systemui"
ProcessTarget.Launcher      -> processName == "com.miui.home" || "com.mi.android.globallauncher"
ProcessTarget.Package(name) -> processName == name
ProcessTarget.Any           -> any process
```