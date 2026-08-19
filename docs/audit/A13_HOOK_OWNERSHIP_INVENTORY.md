# A13 Hook Ownership Inventory

Total direct hook call sites: 634

| Category | Files | Direct calls | Share |
|---|---|---:|---:|
| `REGISTRY_FEATURE` | 26 | 131 | 20.7 % |
| `INSTALLER_INFRASTRUCTURE` | 5 | 6 | 0.9 % |
| `API_BRIDGE` | 0 | 0 | 0.0 % |
| `LEGACY_EXCEPTION` | 31 | 497 | 78.4 % |
| `UNKNOWN` | 0 | 0 | 0.0 % |

## Per-file summary

| File | Direct calls | Primary process | Category | Notes |
|---|---|---|---|---|
| `tv/withaibuild/customiuizer/installers/GenericAppInstaller.java` | 1 | mixed | `INSTALLER_INFRASTRUCTURE` | infrastructure |
| `tv/withaibuild/customiuizer/installers/LauncherInstaller.java` | 1 | mixed | `INSTALLER_INFRASTRUCTURE` | infrastructure |
| `tv/withaibuild/customiuizer/installers/SystemUiInstaller.java` | 1 | mixed | `INSTALLER_INFRASTRUCTURE` | infrastructure |
| `tv/withaibuild/customiuizer/mods/utils/HookInstaller.kt` | 2 | mixed | `INSTALLER_INFRASTRUCTURE` | infrastructure |
| `tv/withaibuild/customiuizer/mods/utils/ResourceHooks.java` | 1 | mixed | `INSTALLER_INFRASTRUCTURE` | infrastructure |
| `tv/withaibuild/customiuizer/mods/Controls.kt` | 30 | mixed | `LEGACY_EXCEPTION` | mixed: typed=4, legacy=26 |
| `tv/withaibuild/customiuizer/mods/GlobalActions.kt` | 8 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/LauncherAnimationHooks.kt` | 14 | mixed | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=13 |
| `tv/withaibuild/customiuizer/mods/LauncherFolderHooks.kt` | 11 | mixed | `LEGACY_EXCEPTION` | mixed: typed=3, legacy=8 |
| `tv/withaibuild/customiuizer/mods/LauncherGestureHooks.kt` | 32 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt` | 19 | mixed | `LEGACY_EXCEPTION` | mixed: typed=2, legacy=17 |
| `tv/withaibuild/customiuizer/mods/LauncherLayoutHooks.kt` | 28 | mixed | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=27 |
| `tv/withaibuild/customiuizer/mods/LauncherSystemHooks.kt` | 15 | mixed | `LEGACY_EXCEPTION` | mixed: typed=3, legacy=12 |
| `tv/withaibuild/customiuizer/mods/SystemAudioAndVisualAndMoreHooks.kt` | 46 | mixed | `LEGACY_EXCEPTION` | mixed: typed=11, legacy=35 |
| `tv/withaibuild/customiuizer/mods/SystemAudioAndVolumeHooks.kt` | 5 | mixed | `LEGACY_EXCEPTION` | mixed: typed=3, legacy=2 |
| `tv/withaibuild/customiuizer/mods/SystemDisplayAndWindowHooks.kt` | 22 | mixed | `LEGACY_EXCEPTION` | mixed: typed=10, legacy=12 |
| `tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt` | 27 | mixed | `LEGACY_EXCEPTION` | mixed: typed=15, legacy=12 |
| `tv/withaibuild/customiuizer/mods/SystemLockScreenHooks.kt` | 7 | mixed | `LEGACY_EXCEPTION` | mixed: typed=4, legacy=3 |
| `tv/withaibuild/customiuizer/mods/SystemLockScreenMoreHooks.kt` | 18 | mixed | `LEGACY_EXCEPTION` | mixed: typed=5, legacy=13 |
| `tv/withaibuild/customiuizer/mods/SystemNotificationAndShareHooks.kt` | 14 | mixed | `LEGACY_EXCEPTION` | mixed: typed=2, legacy=12 |
| `tv/withaibuild/customiuizer/mods/SystemNotificationMoreHooks.kt` | 30 | mixed | `LEGACY_EXCEPTION` | mixed: typed=12, legacy=18 |
| `tv/withaibuild/customiuizer/mods/SystemNotificationPopupsHooks.kt` | 7 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt` | 19 | mixed | `LEGACY_EXCEPTION` | mixed: typed=18, legacy=1 |
| `tv/withaibuild/customiuizer/mods/SystemSettingsAndConnectivityHooks.kt` | 13 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/SystemSettingsMoreHooks.kt` | 5 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/SystemStatusBarAndClockHooks.kt` | 6 | mixed | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=5 |
| `tv/withaibuild/customiuizer/mods/SystemStatusBarClockAndMoreHooks.kt` | 10 | mixed | `LEGACY_EXCEPTION` | mixed: typed=8, legacy=2 |
| `tv/withaibuild/customiuizer/mods/SystemStatusBarMoreHooks.kt` | 11 | mixed | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=10 |
| `tv/withaibuild/customiuizer/mods/SystemUIBatteryHooks.kt` | 8 | mixed | `LEGACY_EXCEPTION` | mixed: typed=7, legacy=1 |
| `tv/withaibuild/customiuizer/mods/SystemUIControlCenterHooks.kt` | 53 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/SystemUILockScreenHooks.kt` | 28 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/SystemUIMonitorAndTileHooks.kt` | 8 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/SystemUINotificationHooks.kt` | 13 | mixed | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=12 |
| `tv/withaibuild/customiuizer/mods/SystemUIStatusBarHooks.kt` | 54 | mixed | `LEGACY_EXCEPTION` | mixed: typed=3, legacy=51 |
| `tv/withaibuild/customiuizer/mods/Various.kt` | 51 | mixed | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=50 |
| `tv/withaibuild/customiuizer/mods/utils/DeviceInfoMonitor.kt` | 1 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `tv/withaibuild/customiuizer/mods/PackagePermissions.kt` | 5 | mixed | `REGISTRY_FEATURE` | typed catalog |
| `tv/withaibuild/customiuizer/mods/SystemChargingAndWallpaperHooks.kt` | 3 | mixed | `REGISTRY_FEATURE` | typed catalog |
| `tv/withaibuild/customiuizer/mods/SystemShareAndOpenWithHooks.kt` | 4 | mixed | `REGISTRY_FEATURE` | typed catalog |
| `tv/withaibuild/customiuizer/mods/SystemUIScreenshotHooks.kt` | 3 | mixed | `REGISTRY_FEATURE` | typed catalog |
