# R13 Catalog Expansion Batch 1

This plan audits candidate features for migration into `FeatureCatalog` after the
`HookRequirement` / `HookEvidenceEvaluator` correctness work on
`devin/r13.8-install-evidence-correctness`.

## Constraints

- Maximum 6 new catalog features in this batch.
- Maximum 2 new features per process (`system_server`, `SystemUI`, `Launcher`).
- No security, signature, permission, integrity, AppLock, lock-screen-auth,
  album-art, bitmap, large cache, broadcast/Service/Foreground monitor, or
  resource-replacement features.
- Each selected feature must have a typed `HookTargetContract` and produce
  `INSTALLED` / `DEGRADED` / `FAILED` from real hook evidence.

## Candidate Audit

### system_server candidates

#### 1. `screenDimTime` — SELECTED

- **featureId**: `screenDimTime`
- **MainModule call**: `MainModule.java:249`
  ```java
  if (mPrefs.getInt("system_dimtime", 0) > 0) SystemAudioAndVisualAndMoreHooks.ScreenDimTimeHook(lpparam);
  ```
- **ProcessTarget**: `SystemServer`
- **preferenceKeys**: `system_dimtime`
- **condition**: `mPrefs.getInt("system_dimtime", 0) > 0`
- **installer**: `SystemAudioAndVisualAndMoreHooks.ScreenDimTimeHook(lpparam)`
- **ModuleHelper calls**:
  1. `findAndHookMethod("com.android.server.power.PowerManagerService", ..., "readConfigurationLocked", callback)`
  2. `findAndHookMethod("com.android.server.power.PowerManagerService", ..., "setStayOnSettingInternal", Int::class.javaPrimitiveType, callback)`
- **Hook target count**: 2
- **Requirement model**:
  - `SingleTargetRequirement` (REQUIRED) for `readConfigurationLocked`
  - `SingleTargetRequirement` (REQUIRED) for `setStayOnSettingInternal`
- **AnyOf fallback**: No
- **activationRestartTarget**: `REBOOT`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

#### 2. `firstVolumePress` — SELECTED

- **featureId**: `firstVolumePress`
- **MainModule call**: `MainModule.java:269`
  ```java
  if (mPrefs.getBoolean("system_firstpress")) SystemAudioAndVisualAndMoreHooks.FirstVolumePressHook(lpparam);
  ```
- **ProcessTarget**: `SystemServer`
- **preferenceKeys**: `system_firstpress`
- **condition**: `mPrefs.getBoolean("system_firstpress")`
- **installer**: `SystemAudioAndVisualAndMoreHooks.FirstVolumePressHook(lpparam)`
- **ModuleHelper calls**:
  1. `findAndHookMethod("com.android.server.audio.AudioService$VolumeController", ..., "suppressAdjustment", Int, Int, Boolean, callback)`
- **Hook target count**: 1
- **Requirement model**: `SingleTargetRequirement` (REQUIRED)
- **AnyOf fallback**: No
- **activationRestartTarget**: `REBOOT`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

#### 3. `clearAllTasks` — NOT SELECTED

- **featureId**: `clearAllTasks`
- **MainModule call**: `MainModule.java:273`
  ```java
  if (mPrefs.getBoolean("system_clearalltasks")) SystemAudioAndVisualAndMoreHooks.ClearAllTasksHook(lpparam);
  ```
- **ProcessTarget**: `SystemServer`
- **preferenceKeys**: `system_clearalltasks`
- **condition**: `mPrefs.getBoolean("system_clearalltasks")`
- **installer**: `SystemAudioAndVisualAndMoreHooks.ClearAllTasksHook(lpparam)`
- **ModuleHelper calls**:
  1. `hookAllMethods("com.android.server.wm.WindowProcessUtils", ..., "getPerceptibleRecentAppList", callback)`
- **Hook target count**: 1
- **Requirement model**: `SingleTargetRequirement` (REQUIRED)
- **AnyOf fallback**: No
- **activationRestartTarget**: `REBOOT`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: No (system_server quota filled by `screenDimTime` and `firstVolumePress`)

#### 4. `toastTime` — NOT SELECTED

- **featureId**: `toastTime`
- **MainModule call**: `MainModule.java:250`
  ```java
  if (mPrefs.getInt("system_toasttime", 0) > 0) SystemAudioAndVisualAndMoreHooks.ToastTimeHook(lpparam);
  ```
- **ProcessTarget**: `SystemServer`
- **preferenceKeys**: `system_toasttime`
- **condition**: `mPrefs.getInt("system_toasttime", 0) > 0`
- **installer**: `SystemAudioAndVisualAndMoreHooks.ToastTimeHook(lpparam)`
- **ModuleHelper calls**:
  1. `findAndHookMethod("com.android.server.notification.NotificationManagerService", ..., "showNextToastLocked", callback)`
  2. `hookAllMethods("com.android.server.wm.DisplayPolicy", ..., "adjustWindowParamsLw", callback)`
- **Hook target count**: 2
- **Requirement model**:
  - `SingleTargetRequirement` (REQUIRED) for `showNextToastLocked`
  - `SingleTargetRequirement` (REQUIRED) for `adjustWindowParamsLw`
- **AnyOf fallback**: No
- **activationRestartTarget**: `REBOOT`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: No (system_server quota filled; also two targets vs `firstVolumePress` one target)

### SystemUI candidates

#### 5. `networkIndicatorWifi` — SELECTED

- **featureId**: `networkIndicatorWifi`
- **MainModule call**: `MainModule.java:412`
  ```java
  if (mPrefs.getBoolean("system_networkindicator_wifi")) SystemStatusBarMoreHooks.NetworkIndicatorWifi(lpparam);
  ```
- **ProcessTarget**: `SystemUI`
- **preferenceKeys**: `system_networkindicator_wifi`
- **condition**: `mPrefs.getBoolean("system_networkindicator_wifi")`
- **installer**: `SystemStatusBarMoreHooks.NetworkIndicatorWifi(lpparam)`
- **ModuleHelper calls**:
  1. `hookAllMethods("com.android.systemui.statusbar.StatusBarWifiView", ..., "applyWifiState", callback)`
- **Hook target count**: 1
- **Requirement model**: `SingleTargetRequirement` (REQUIRED)
- **AnyOf fallback**: No
- **activationRestartTarget**: `SYSTEMUI_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

#### 6. `muteVisibleNotifications` — SELECTED

- **featureId**: `muteVisibleNotifications`
- **MainModule call**: `MainModule.java:545`
  ```java
  if (mPrefs.getBoolean("system_mutevisiblenotif")) SystemNotificationMoreHooks.MuteVisibleNotificationsHook(lpparam);
  ```
- **ProcessTarget**: `SystemUI`
- **preferenceKeys**: `system_mutevisiblenotif`
- **condition**: `mPrefs.getBoolean("system_mutevisiblenotif")`
- **installer**: `SystemNotificationMoreHooks.MuteVisibleNotificationsHook(lpparam)`
- **ModuleHelper calls**:
  1. `hookAllMethods("com.android.systemui.statusbar.notification.policy.NotificationAlertController", ..., "buzzBeepBlink", callback)`
- **Hook target count**: 1
- **Requirement model**: `SingleTargetRequirement` (REQUIRED)
- **AnyOf fallback**: No
- **activationRestartTarget**: `SYSTEMUI_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

#### 7. `noNetworkSpeedSeparator` — NOT SELECTED

- **featureId**: `noNetworkSpeedSeparator`
- **MainModule call**: `MainModule.java:516`
  ```java
  if (mPrefs.getBoolean("system_nonetspeedseparator")) SystemUIStatusBarHooks.NoNetworkSpeedSeparatorHook(lpparam);
  ```
- **ProcessTarget**: `SystemUI`
- **preferenceKeys**: `system_nonetspeedseparator`
- **condition**: `mPrefs.getBoolean("system_nonetspeedseparator")`
- **installer**: `SystemUIStatusBarHooks.NoNetworkSpeedSeparatorHook(lpparam)`
- **ModuleHelper calls**:
  1. `findAndHookMethod("com.android.systemui.statusbar.views.NetworkSpeedSplitter", ..., "onClockVisibilityChanged", Int, callback)`
  2. `findAndHookMethod("com.android.systemui.statusbar.views.NetworkSpeedSplitter", ..., "onNetworkSpeedVisibilityChanged", Int, callback)`
- **Hook target count**: 2
- **Requirement model**:
  - `SingleTargetRequirement` (REQUIRED) for `onClockVisibilityChanged`
  - `SingleTargetRequirement` (REQUIRED) for `onNetworkSpeedVisibilityChanged`
- **AnyOf fallback**: No
- **activationRestartTarget**: `SYSTEMUI_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: No (SystemUI quota filled by one-target features)

#### 8. `hideCCOperatorDelimiter` — NOT SELECTED

- **featureId**: `hideCCOperatorDelimiter`
- **MainModule call**: `MainModule.java:566`
  ```java
  if (mPrefs.getBoolean("system_cc_hideoperator_delimiter")) SystemSettingsAndConnectivityHooks.HideCCOperatorDelimiterHook(lpparam);
  ```
- **ProcessTarget**: `SystemUI`
- **preferenceKeys**: `system_cc_hideoperator_delimiter`
- **condition**: `mPrefs.getBoolean("system_cc_hideoperator_delimiter")`
- **installer**: `SystemSettingsAndConnectivityHooks.HideCCOperatorDelimiterHook(lpparam)`
- **ModuleHelper calls**:
  1. `findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextController", ..., "fireCarrierTextChanged", String, callback)`
- **Hook target count**: 1
- **Requirement model**: `SingleTargetRequirement` (REQUIRED)
- **AnyOf fallback**: No
- **activationRestartTarget**: `SYSTEMUI_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: No (SystemUI quota filled)

### Launcher candidates

#### 9. `hideLauncherTitles` — SELECTED

- **featureId**: `hideLauncherTitles`
- **MainModule call**: `MainModule.java:796`
  ```java
  if (mPrefs.getBoolean("launcher_hidetitles")) LauncherIconHooks.HideTitlesHook(lpparam);
  ```
- **ProcessTarget**: `Launcher`
- **preferenceKeys**: `launcher_hidetitles`
- **condition**: `mPrefs.getBoolean("launcher_hidetitles")`
- **installer**: `LauncherIconHooks.HideTitlesHook(lpparam)`
- **ModuleHelper calls**:
  1. `findAndHookMethod("com.miui.home.launcher.ItemIcon", ..., "onFinishInflate", callback)`
- **Hook target count**: 1
- **Requirement model**: `SingleTargetRequirement` (REQUIRED)
- **AnyOf fallback**: No
- **activationRestartTarget**: `LAUNCHER_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

#### 10. `fixAppInfoLaunch` — SELECTED

- **featureId**: `fixAppInfoLaunch`
- **MainModule call**: `MainModule.java:797`
  ```java
  if (mPrefs.getBoolean("launcher_fixlaunch")) LauncherSystemHooks.FixAppInfoLaunchHook(lpparam);
  ```
- **ProcessTarget**: `Launcher`
- **preferenceKeys**: `launcher_fixlaunch`
- **condition**: `mPrefs.getBoolean("launcher_fixlaunch")`
- **installer**: `LauncherSystemHooks.FixAppInfoLaunchHook(lpparam)`
- **ModuleHelper calls**:
  1. `hookAllMethods("com.miui.home.launcher.util.Utilities", ..., "startDetailsActivityForInfo", callback)`
  2. `hookAllMethods("com.miui.home.launcher.shortcuts.ShortcutMenuManager", ..., "startAppDetailsActivity", callback)`
- **Hook target count**: 2
- **Requirement model**:
  - `AnyOfRequirement` (REQUIRED) with two candidates:
    1. `com.miui.home.launcher.shortcuts.ShortcutMenuManager.startAppDetailsActivity` (primary, for `com.miui.home`)
    2. `com.miui.home.launcher.util.Utilities.startDetailsActivityForInfo` (fallback, for `com.mi.android.globallauncher`)
- **AnyOf fallback**: Yes
- **activationRestartTarget**: `LAUNCHER_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

#### 11. `hideLauncherSeekPoints` — NOT SELECTED

- **featureId**: `hideLauncherSeekPoints`
- **MainModule call**: `MainModule.java:813`
  ```java
  if (mPrefs.getBoolean("launcher_hideseekpoints")) LauncherLayoutHooks.HideSeekPointsHook(lpparam);
  ```
- **ProcessTarget**: `Launcher`
- **preferenceKeys**: `launcher_hideseekpoints`, `launcher_hideseekpoints_edit`
- **condition**: `mPrefs.getBoolean("launcher_hideseekpoints")`
- **installer**: `LauncherLayoutHooks.HideSeekPointsHook(lpparam)`
- **ModuleHelper calls**:
  1. `findAndHookMethod("com.miui.home.launcher.pageindicators.AllAppsIndicator", ..., "shouldHide", HookerClassHelper.returnConstant(true))`
  2. `findAndHookMethod("com.miui.home.launcher.pageindicators.AllAppsIndicator", ..., "hideAllAppsArrow", callback)`
- **Hook target count**: 2
- **Requirement model**:
  - `SingleTargetRequirement` (REQUIRED) for `shouldHide`
  - `SingleTargetRequirement` (REQUIRED) for `hideAllAppsArrow`
- **AnyOf fallback**: No
- **activationRestartTarget**: `LAUNCHER_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: No (Launcher quota filled; also uses a Handler for animation, slightly more complex)

#### 12. `disableLauncherLog` — NOT SELECTED

- **featureId**: `disableLauncherLog`
- **MainModule call**: `MainModule.java:746`
  ```java
  if (mPrefs.getBoolean("launcher_disable_log")) LauncherSystemHooks.DisableLauncherLogHook(lpparam);
  ```
- **ProcessTarget**: `Launcher`
- **preferenceKeys**: `launcher_disable_log`
- **condition**: `mPrefs.getBoolean("launcher_disable_log")`
- **installer**: `LauncherSystemHooks.DisableLauncherLogHook(lpparam)`
- **ModuleHelper calls**:
  1. `hookAllMethods("com.miui.home.launcher.AnalyticalDataCollectorJobService", ..., "onStartJob", callback)`
  2. `findAndHookMethod("com.miui.home.launcher.AnalyticalDataCollector", ..., "canTrackLaunchAppEvent", callback)`
- **Hook target count**: 2 (plus a non-recorded `XposedHelpers.setStaticObjectField` call that cannot be captured by the current evidence system)
- **Requirement model**:
  - `SingleTargetRequirement` (REQUIRED) for `onStartJob`
  - `SingleTargetRequirement` (REQUIRED) for `canTrackLaunchAppEvent`
- **AnyOf fallback**: No
- **activationRestartTarget**: `LAUNCHER_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: No (contains a non-recorded static field mutation, which would make the contract under-report the installer; also Launcher quota filled)

## Selected Batch 1 Features (6 total)

| # | featureId | process | key | risk |
|---|-----------|---------|-----|------|
| 1 | `screenDimTime` | system_server | `system_dimtime` | LOW |
| 2 | `firstVolumePress` | system_server | `system_firstpress` | LOW |
| 3 | `networkIndicatorWifi` | SystemUI | `system_networkindicator_wifi` | LOW |
| 4 | `muteVisibleNotifications` | SystemUI | `system_mutevisiblenotif` | LOW |
| 5 | `hideLauncherTitles` | Launcher | `launcher_hidetitles` | LOW |
| 6 | `fixAppInfoLaunch` | Launcher | `launcher_fixlaunch` | LOW |

## Implementation checklist

1. `DiagnosticIds.kt`: add `SCREEN_DIM_TIME`, `FIRST_VOLUME_PRESS`, `NETWORK_INDICATOR_WIFI`, `MUTE_VISIBLE_NOTIFICATIONS`, `HIDE_LAUNCHER_TITLES`, `FIX_APP_INFO_LAUNCH`.
2. `PreferenceSchema.kt`: add entries for the 6 preference keys.
3. `CatalogContracts.kt` (new): define `HookTargetContract` for each selected feature.
4. `FeatureCatalog.kt`: add 6 `FeatureSpec` entries in the existing `features` list, preserving MainModule call order relative to canary features.
5. `MainModule.java`: replace the 6 direct `if (...) XxxHook(lpparam)` calls with `FeatureCatalog.installById("featureId", runtime)` in the same positions.
6. `app/src/test/java` stubs: add minimal stub classes for the target classes/methods so unit tests can resolve them.
7. `FeatureCatalogTest` / new test file: add tests for disabled, compatible, missing target, fallback (for `fixAppInfoLaunch`), and installer failure.
8. Audit scripts: extend `audit-architecture.py`, `audit-prefs.py`, `audit-canary-sequence.py` / `audit-catalog-sequence.py`, and `audit-catalog-contracts.py` to cover the new feature IDs, diagnostic IDs, schema keys, and MainModule call-sequence.
