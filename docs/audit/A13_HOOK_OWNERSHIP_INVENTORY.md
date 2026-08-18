# A13 Hook Ownership Inventory

> Branch: `devin/a13-rom-intelligence-audit`
> Baseline commit: `HEAD`
> Generated: auto
> Repository: `tomthenpc/customiuizer-a13`
> Device evidence: `NOT_EXERCISED`

---

## 1. Classification

| Category | Meaning |
|---|---|
| `REGISTRY_FEATURE` | Hook implementation owned by a typed catalog `FeatureId` and installed through `FeatureInstallRegistry`. |
| `INSTALLER_INFRASTRUCTURE` | Bootstrap / utility hook used by an `Installer` or shared runtime helper, not a business Feature. |
| `LEGACY_EXCEPTION` | Business hook with at least one call site not yet owned by a typed catalog Feature. |
| `UNKNOWN` | Hook with no reachable owner (target: 0). |

---

## 2. Summary

| Category | Files | Direct `ModuleHelper.*` call sites | Share |
|---|---|---|---|
| `REGISTRY_FEATURE` | 3 | 10 | 1.6 % |
| `INSTALLER_INFRASTRUCTURE` | 0 | 0 | 0.0 % |
| `LEGACY_EXCEPTION` | 37 | 618 | 98.4 % |
| `UNKNOWN` | 0 | 0 | 0.0 % |
| **Total** | **40** | **628** | **100 %** |

---

## 3. Per-file inventory

| File | Direct calls | Primary process | Category | Notes |
|---|---|---|---|---|
| `installers/GenericAppInstaller.java` | 1 | per-app / mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `installers/LauncherInstaller.java` | 1 | com.miui.home / com.mi.android.globallauncher | `LEGACY_EXCEPTION` | no typed catalog owner |
| `installers/SystemUiInstaller.java` | 1 | com.android.systemui | `LEGACY_EXCEPTION` | no typed catalog owner |
| `mods/Controls.kt` | 29 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `mods/GlobalActions.kt` | 8 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `mods/LauncherAnimationHooks.kt` | 14 | com.miui.home / com.mi.android.globallauncher | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=7 |
| `mods/LauncherFolderHooks.kt` | 11 | com.miui.home / com.mi.android.globallauncher | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=4 |
| `mods/LauncherGestureHooks.kt` | 32 | com.miui.home / com.mi.android.globallauncher | `LEGACY_EXCEPTION` | no typed catalog owner |
| `mods/LauncherIconHooks.kt` | 19 | com.miui.home / com.mi.android.globallauncher | `LEGACY_EXCEPTION` | mixed: typed=2, legacy=4 |
| `mods/LauncherLayoutHooks.kt` | 27 | com.miui.home / com.mi.android.globallauncher | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=11 |
| `mods/LauncherSystemHooks.kt` | 15 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=2, legacy=5 |
| `mods/PackagePermissions.kt` | 5 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `mods/SystemAudioAndVisualAndMoreHooks.kt` | 47 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=8, legacy=13 |
| `mods/SystemAudioAndVolumeHooks.kt` | 5 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=2, legacy=1 |
| `mods/SystemChargingAndWallpaperHooks.kt` | 3 | com.android.systemui | `REGISTRY_FEATURE` | all hook calls owned by typed catalog |
| `mods/SystemDisplayAndWindowHooks.kt` | 22 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=5, legacy=3 |
| `mods/SystemFreeformAndMultiWindowHooks.kt` | 27 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=4, legacy=3 |
| `mods/SystemLockScreenHooks.kt` | 7 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=2 |
| `mods/SystemLockScreenMoreHooks.kt` | 18 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=4, legacy=3 |
| `mods/SystemNotificationAndShareHooks.kt` | 14 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=3 |
| `mods/SystemNotificationMoreHooks.kt` | 30 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=8, legacy=10 |
| `mods/SystemNotificationPopupsHooks.kt` | 7 | com.android.systemui | `LEGACY_EXCEPTION` | no typed catalog owner |
| `mods/SystemSecurityAndSystemHooks.kt` | 19 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=7, legacy=1 |
| `mods/SystemSettingsAndConnectivityHooks.kt` | 13 | com.android.systemui | `LEGACY_EXCEPTION` | no typed catalog owner |
| `mods/SystemSettingsMoreHooks.kt` | 5 | com.android.systemui | `LEGACY_EXCEPTION` | no typed catalog owner |
| `mods/SystemShareAndOpenWithHooks.kt` | 4 | com.android.systemui | `REGISTRY_FEATURE` | all hook calls owned by typed catalog |
| `mods/SystemStatusBarAndClockHooks.kt` | 6 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=2 |
| `mods/SystemStatusBarClockAndMoreHooks.kt` | 10 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=2 |
| `mods/SystemStatusBarMoreHooks.kt` | 11 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=6 |
| `mods/SystemUIBatteryHooks.kt` | 8 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=1 |
| `mods/SystemUIControlCenterHooks.kt` | 53 | com.android.systemui | `LEGACY_EXCEPTION` | no typed catalog owner |
| `mods/SystemUILockScreenHooks.kt` | 28 | com.android.systemui | `LEGACY_EXCEPTION` | no typed catalog owner |
| `mods/SystemUIMonitorAndTileHooks.kt` | 8 | com.android.systemui | `LEGACY_EXCEPTION` | no typed catalog owner |
| `mods/SystemUINotificationHooks.kt` | 13 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=1, legacy=6 |
| `mods/SystemUIScreenshotHooks.kt` | 3 | com.android.systemui | `REGISTRY_FEATURE` | all hook calls owned by typed catalog |
| `mods/SystemUIStatusBarHooks.kt` | 54 | com.android.systemui | `LEGACY_EXCEPTION` | mixed: typed=2, legacy=17 |
| `mods/Various.kt` | 46 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `mods/utils/DeviceInfoMonitor.kt` | 1 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `mods/utils/HookInstaller.kt` | 2 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
| `mods/utils/ResourceHooks.java` | 1 | mixed | `LEGACY_EXCEPTION` | no typed catalog owner |
