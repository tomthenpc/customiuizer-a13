# A13 Hook Ownership Inventory

> Branch: `devin/a13-rom-intelligence-audit`
> Baseline commit: `3154fdd`
> Generated: 2026-08-02
> Repository: `tomthenpc/customiuizer-a13`
> Device evidence: `NOT_EXERCISED`

---

## 1. Classification

| Category | Meaning |
|---|---|
| `REGISTRY_FEATURE` | Hook implementation owned by a typed catalog `FeatureId` and installed through `FeatureInstallRegistry` (Canary batch). |
| `INSTALLER_INFRASTRUCTURE` | Bootstrap / utility hook used by an `Installer` or by shared runtime helpers, not a business Feature. |
| `LEGACY_EXCEPTION` | Business hook that is still invoked through `FeatureDispatcher` legacy install paths or by an untyped installer. |
| `DEAD_CANDIDATE` | Hook with no reachable owner (none found at baseline). |
| `UNKNOWN` | Hook that cannot be classified into the above (target: 0). |

---

## 2. Summary

| Category | Files | Direct `ModuleHelper.*` call sites | Share |
|---|---|---|---|
| `REGISTRY_FEATURE` | 8 | 163 | ~25.9 % |
| `INSTALLER_INFRASTRUCTURE` | 6 | 7 | ~1.1 % |
| `LEGACY_EXCEPTION` | 27 | 460 | ~73.0 % |
| `DEAD_CANDIDATE` | 0 | 0 | 0 % |
| `UNKNOWN` | 0 | 0 | 0 % |
| **Total** | **41** | **630** | **100 %** |

---

## 3. Per-file inventory

| File | Direct calls | Primary process | Category | Notes |
|---|---|---|---|---|
| `installers/GenericAppInstaller.java` | 1 | per-app / mixed | `INSTALLER_INFRASTRUCTURE` | `Application.attach` bootstrap for generic apps |
| `installers/LauncherInstaller.java` | 1 | `com.miui.home` / `com.mi.android.globallauncher` | `INSTALLER_INFRASTRUCTURE` | `Application.attach` bootstrap for launcher |
| `installers/SystemUiInstaller.java` | 1 | `com.android.systemui` | `INSTALLER_INFRASTRUCTURE` | `SystemUIApplication.onCreate` bootstrap |
| `mods/utils/DeviceInfoMonitor.kt` | 1 | mixed | `INSTALLER_INFRASTRUCTURE` | Device info helper, not a catalog Feature |
| `mods/utils/HookInstaller.kt` | 2 | process-agnostic | `INSTALLER_INFRASTRUCTURE` | Internal `HookInstaller.withSession` implementation helper |
| `mods/utils/ResourceHooks.java` | 1 | mixed | `INSTALLER_INFRASTRUCTURE` | Resource replacement infrastructure |
| `mods/PackagePermissions.kt` | 5 | `android` / `system_server` | `REGISTRY_FEATURE` | `packagePermissions` (Canary) |
| `mods/SystemStatusBarClockAndMoreHooks.kt` | 10 | `com.android.systemui` | `REGISTRY_FEATURE` | `statusBarClockTweak` (Canary); may contain related helpers |
| `mods/SystemDisplayAndWindowHooks.kt` | 22 | mixed | `REGISTRY_FEATURE` | `autoBrightnessRange` (Canary); also display/window legacy helpers |
| `mods/SystemAudioAndVisualAndMoreHooks.kt` | 47 | `com.android.systemui` / `android` | `REGISTRY_FEATURE` | `muffledVibration` (Canary); audio/visual legacy helpers |
| `mods/SystemNotificationMoreHooks.kt` | 30 | `com.android.systemui` | `REGISTRY_FEATURE` | `noMoreIcon` (Canary); notification legacy helpers |
| `mods/SystemUIBatteryHooks.kt` | 8 | `com.android.systemui` | `REGISTRY_FEATURE` | `batteryIndicator` (Canary) |
| `mods/LauncherSystemHooks.kt` | 15 | `com.miui.home` / `com.mi.android.globallauncher` | `REGISTRY_FEATURE` | `noClockHide`, `noWidgetOnly` (Canary); launcher legacy helpers |
| `mods/SystemAudioAndVolumeHooks.kt` | 5 | `com.android.systemui` | `LEGACY_EXCEPTION` | Untyped audio / volume hooks |
| `mods/SystemChargingAndWallpaperHooks.kt` | 3 | `com.android.systemui` | `LEGACY_EXCEPTION` | Untyped charging / wallpaper hooks |
| `mods/SystemFreeformAndMultiWindowHooks.kt` | 27 | `com.android.systemui` | `LEGACY_EXCEPTION` | Freeform / multi-window hooks |
| `mods/SystemLockScreenHooks.kt` | 7 | `com.android.systemui` | `LEGACY_EXCEPTION` | Lock screen hooks |
| `mods/SystemLockScreenMoreHooks.kt` | 18 | `com.android.systemui` | `LEGACY_EXCEPTION` | Additional lock screen hooks |
| `mods/SystemNotificationAndShareHooks.kt` | 14 | `com.android.systemui` | `LEGACY_EXCEPTION` | Notification and share hooks |
| `mods/SystemNotificationPopupsHooks.kt` | 7 | `com.android.systemui` | `LEGACY_EXCEPTION` | Notification popup hooks |
| `mods/SystemSecurityAndSystemHooks.kt` | 19 | `android` / `system_server` / `com.android.systemui` | `LEGACY_EXCEPTION` | Security and system hooks |
| `mods/SystemSettingsAndConnectivityHooks.kt` | 13 | `com.android.systemui` / `com.android.settings` | `LEGACY_EXCEPTION` | Settings / connectivity hooks |
| `mods/SystemSettingsMoreHooks.kt` | 5 | `com.android.settings` / `com.android.systemui` | `LEGACY_EXCEPTION` | Additional settings hooks |
| `mods/SystemShareAndOpenWithHooks.kt` | 4 | `com.android.systemui` | `LEGACY_EXCEPTION` | Share / open-with hooks |
| `mods/SystemStatusBarAndClockHooks.kt` | 6 | `com.android.systemui` | `LEGACY_EXCEPTION` | Status bar / clock helpers |
| `mods/SystemStatusBarMoreHooks.kt` | 11 | `com.android.systemui` | `LEGACY_EXCEPTION` | Additional status bar hooks |
| `mods/SystemUIControlCenterHooks.kt` | 53 | `com.android.systemui` | `LEGACY_EXCEPTION` | Control center hooks |
| `mods/SystemUILockScreenHooks.kt` | 28 | `com.android.systemui` | `LEGACY_EXCEPTION` | SystemUI lock screen hooks |
| `mods/SystemUIMonitorAndTileHooks.kt` | 8 | `com.android.systemui` | `LEGACY_EXCEPTION` | Monitor / tile hooks |
| `mods/SystemUINotificationHooks.kt` | 13 | `com.android.systemui` | `LEGACY_EXCEPTION` | Notification hooks |
| `mods/SystemUIScreenshotHooks.kt` | 3 | `com.android.systemui` | `LEGACY_EXCEPTION` | Screenshot hooks |
| `mods/SystemUIStatusBarHooks.kt` | 54 | `com.android.systemui` | `LEGACY_EXCEPTION` | Status bar hooks (largest single owner) |
| `mods/Controls.kt` | 29 | `com.android.systemui` / `com.android.settings` | `LEGACY_EXCEPTION` | Control / input hooks |
| `mods/GlobalActions.kt` | 8 | `android` / `system_server` | `LEGACY_EXCEPTION` | Global actions / power menu hooks |
| `mods/Various.kt` | 48 | mixed | `LEGACY_EXCEPTION` | Various per-app / miscellaneous hooks |
| `mods/LauncherAnimationHooks.kt` | 14 | `com.miui.home` / `com.mi.android.globallauncher` | `LEGACY_EXCEPTION` | Launcher animation hooks |
| `mods/LauncherFolderHooks.kt` | 11 | `com.miui.home` / `com.mi.android.globallauncher` | `LEGACY_EXCEPTION` | Launcher folder hooks |
| `mods/LauncherGestureHooks.kt` | 32 | `com.miui.home` / `com.mi.android.globallauncher` | `LEGACY_EXCEPTION` | Launcher gesture hooks |
| `mods/LauncherIconHooks.kt` | 19 | `com.miui.home` / `com.mi.android.globallauncher` | `LEGACY_EXCEPTION` | Launcher icon hooks |
| `mods/LauncherLayoutHooks.kt` | 27 | `com.miui.home` / `com.mi.android.globallauncher` | `LEGACY_EXCEPTION` | Launcher layout hooks |
| `mods/LauncherSystemHooks.kt` | 15 | `com.miui.home` / `com.mi.android.globallauncher` | `REGISTRY_FEATURE` | See above |
