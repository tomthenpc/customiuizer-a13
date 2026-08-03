# A13 Hook Ownership Inventory

Total direct hook call sites: 676

| Category | Files | Direct calls | Share |
|---|---|---:|---:|
| `REGISTRY_FEATURE` | 26 | 133 | 19.7 % |
| `INSTALLER_INFRASTRUCTURE` | 5 | 6 | 0.9 % |
| `API_BRIDGE` | 1 | 23 | 3.4 % |
| `LEGACY_EXCEPTION` | 32 | 514 | 76.0 % |
| `UNKNOWN` | 0 | 0 | 0.0 % |

## Per-file summary

| File | Direct calls | Registry calls | Legacy calls | Category | Notes |
|---|---|---:|---:|---|---|---|
| `tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java` | 23 | 0 | 23 | `API_BRIDGE` | infrastructure |
| `tv/withaibuild/customiuizer/installers/GenericAppInstaller.java` | 1 | 0 | 1 | `INSTALLER_INFRASTRUCTURE` | infrastructure |
| `tv/withaibuild/customiuizer/installers/LauncherInstaller.java` | 1 | 0 | 1 | `INSTALLER_INFRASTRUCTURE` | infrastructure |
| `tv/withaibuild/customiuizer/installers/SystemUiInstaller.java` | 1 | 0 | 1 | `INSTALLER_INFRASTRUCTURE` | infrastructure |
| `tv/withaibuild/customiuizer/mods/utils/HookInstaller.kt` | 2 | 0 | 2 | `INSTALLER_INFRASTRUCTURE` | infrastructure |
| `tv/withaibuild/customiuizer/mods/utils/ResourceHooks.java` | 1 | 0 | 1 | `INSTALLER_INFRASTRUCTURE` | infrastructure |
| `tv/withaibuild/customiuizer/mods/Controls.kt` | 29 | 4 | 25 | `LEGACY_EXCEPTION` | mixed: typed=4, legacy=25 |
| `tv/withaibuild/customiuizer/mods/GlobalActions.kt` | 8 | 0 | 8 | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/LauncherAnimationHooks.kt` | 14 | 1 | 13 | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=13 |
| `tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt` | 11 | 3 | 8 | `LEGACY_EXCEPTION` | mixed: typed=3, legacy=8 |
| `tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt` | 32 | 0 | 32 | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt` | 19 | 2 | 17 | `LEGACY_EXCEPTION` | mixed: typed=2, legacy=17 |
| `tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt` | 27 | 1 | 26 | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=26 |
| `tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt` | 15 | 3 | 12 | `LEGACY_EXCEPTION` | mixed: typed=3, legacy=12 |
| `tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt` | 47 | 12 | 35 | `LEGACY_EXCEPTION` | mixed: typed=12, legacy=35 |
| `tv/withaibuild/customiuizer/mods/SystemAudioAndVolumeHooks.kt` | 5 | 4 | 1 | `LEGACY_EXCEPTION` | mixed: typed=4, legacy=1 |
| `tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt` | 22 | 10 | 12 | `LEGACY_EXCEPTION` | mixed: typed=10, legacy=12 |
| `tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt` | 27 | 15 | 12 | `LEGACY_EXCEPTION` | mixed: typed=15, legacy=12 |
| `tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt` | 7 | 4 | 3 | `LEGACY_EXCEPTION` | mixed: typed=4, legacy=3 |
| `tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt` | 18 | 5 | 13 | `LEGACY_EXCEPTION` | mixed: typed=5, legacy=13 |
| `tv/withaibuild/customiuizer/mods/SystemNotificationAndShareHooks.kt` | 15 | 2 | 13 | `LEGACY_EXCEPTION` | mixed: typed=2, legacy=13 |
| `tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt` | 30 | 12 | 18 | `LEGACY_EXCEPTION` | mixed: typed=12, legacy=18 |
| `tv/withaibuild/customiuizer/mods/SystemNotificationPopupsHooks.kt` | 7 | 0 | 7 | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt` | 19 | 18 | 1 | `LEGACY_EXCEPTION` | mixed: typed=18, legacy=1 |
| `tv/withaibuild/customiuizer/mods/SystemSettingsAndConnectivityHooks.kt` | 13 | 0 | 13 | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/SystemSettingsMoreHooks.kt` | 5 | 0 | 5 | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/SystemStatusBarAndClockHooks.kt` | 10 | 1 | 9 | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=9 |
| `tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt` | 10 | 8 | 2 | `LEGACY_EXCEPTION` | mixed: typed=8, legacy=2 |
| `tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt` | 11 | 1 | 10 | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=10 |
| `tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt` | 8 | 7 | 1 | `LEGACY_EXCEPTION` | mixed: typed=7, legacy=1 |
| `tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt` | 54 | 0 | 54 | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt` | 28 | 0 | 28 | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt` | 8 | 0 | 8 | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt` | 14 | 1 | 13 | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=13 |
| `tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt` | 55 | 3 | 52 | `LEGACY_EXCEPTION` | mixed: typed=3, legacy=52 |
| `tv/withaibuild/customiuizer/mods/Various.kt` | 54 | 1 | 53 | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=53 |
| `tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt` | 1 | 0 | 1 | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/utils/XposedHelpers.java` | 9 | 0 | 9 | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/PackagePermissions.kt` | 5 | 5 | 0 | `REGISTRY_FEATURE` | typed catalog |
| `tv/withaibuild/customiuizer/mods/SystemChargingAndWallpaperHooks.kt` | 3 | 3 | 0 | `REGISTRY_FEATURE` | typed catalog |
| `tv/withaibuild/customiuizer/mods/SystemShareAndOpenWithHooks.kt` | 4 | 4 | 0 | `REGISTRY_FEATURE` | typed catalog |
| `tv/withaibuild/customiuizer/mods/SystemUIScreenshotHooks.kt` | 3 | 3 | 0 | `REGISTRY_FEATURE` | typed catalog |
