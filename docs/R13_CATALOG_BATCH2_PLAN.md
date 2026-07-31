# R13 Catalog Expansion Batch 2

This plan audits the next set of low-risk features for migration into
`FeatureCatalog` after batch 1.

## Constraints

- Maximum 6 new catalog features.
- Maximum 2 new features per process (`system_server`, `SystemUI`, `Launcher`).
- No security, signature, permission, integrity, AppLock, lock-screen-auth,
  album-art, bitmap, large cache, broadcast/Service/Foreground monitor, or
  resource-replacement features.
- Each selected feature must have a typed `HookTargetContract` and produce
  `INSTALLED` / `DEGRADED` / `FAILED` from real hook evidence.

## Candidate Audit

### system_server candidates

#### 1. `hideProximityWarning` — SELECTED

- **featureId**: `hideProximityWarning`
- **MainModule call**: `MainModule.java:268`
  ```java
  if (mPrefs.getBoolean("system_hideproxywarn")) SystemDisplayAndWindowHooks.HideProximityWarningHook(lpparam);
  ```
- **ProcessTarget**: `SystemServer`
- **preferenceKeys**: `system_hideproxywarn`
- **condition**: `mPrefs.getBoolean("system_hideproxywarn")`
- **installer**: `SystemDisplayAndWindowHooks.HideProximityWarningHook(lpparam)`
- **ModuleHelper calls**:
  1. `findAndHookMethod("com.android.server.policy.MiuiScreenOnProximityLock", ..., "showHint", DO_NOTING)`
  2. `findAndHookMethod("com.android.server.policy.MiuiScreenOnProximityLock", ..., "prepareHintWindow", DO_NOTING)`
- **Hook target count**: 2
- **Requirement model**:
  - `SingleTargetRequirement` (REQUIRED) for `showHint`
  - `SingleTargetRequirement` (REQUIRED) for `prepareHintWindow`
- **AnyOf fallback**: No
- **activationRestartTarget**: `REBOOT`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

#### 2. `clearAllTasks` — SELECTED

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
- **Selected**: Yes

#### 3. `toastTime` — NOT SELECTED

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
- **Selected**: No (system_server quota filled; uses additional instance state)

### SystemUI candidates

#### 4. `hideDismissView` — SELECTED

- **featureId**: `hideDismissView`
- **MainModule call**: `MainModule.java:444`
  ```java
  if (mPrefs.getBoolean("system_removedismiss")) SystemUINotificationHooks.HideDismissViewHook(lpparam);
  ```
- **ProcessTarget**: `SystemUI`
- **preferenceKeys**: `system_removedismiss`
- **condition**: `mPrefs.getBoolean("system_removedismiss")`
- **installer**: `SystemUINotificationHooks.HideDismissViewHook(lpparam)`
- **ModuleHelper calls**:
  1. `findAndHookMethod("com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController", ..., "updateDismissView", callback)`
- **Hook target count**: 1
- **Requirement model**: `SingleTargetRequirement` (REQUIRED)
- **AnyOf fallback**: No
- **activationRestartTarget**: `SYSTEMUI_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

#### 5. `hideLockScreenHint` — SELECTED

- **featureId**: `hideLockScreenHint`
- **MainModule call**: `MainModule.java:509`
  ```java
  if (mPrefs.getBoolean("system_hidelshint")) SystemLockScreenMoreHooks.HideLockScreenHintHook(lpparam);
  ```
- **ProcessTarget**: `SystemUI`
- **preferenceKeys**: `system_hidelshint`
- **condition**: `mPrefs.getBoolean("system_hidelshint")`
- **installer**: `SystemLockScreenMoreHooks.HideLockScreenHintHook(lpparam)`
- **ModuleHelper calls**:
  1. `findAndHookMethod("com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController", ..., "hasIndicationsExceptResting", HookerClassHelper.returnConstant(true))`
- **Hook target count**: 1
- **Requirement model**: `SingleTargetRequirement` (REQUIRED)
- **AnyOf fallback**: No
- **activationRestartTarget**: `SYSTEMUI_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

#### 6. `hideCCOperatorDelimiter` — NOT SELECTED

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

#### 7. `folderColumns` — SELECTED

- **featureId**: `folderColumns`
- **MainModule call**: `MainModule.java:787`
  ```java
  if (mPrefs.getInt("launcher_folder_cols", 1) > 1) LauncherFolderHooks.FolderColumnsHook(lpparam);
  ```
- **ProcessTarget**: `Launcher`
- **preferenceKeys**: `launcher_folder_cols`
- **condition**: `mPrefs.getInt("launcher_folder_cols", 1) > 1`
- **installer**: `LauncherFolderHooks.FolderColumnsHook(lpparam)`
- **ModuleHelper calls**:
  1. `findAndHookMethod("com.miui.home.launcher.Folder", ..., "onFinishInflate", callback)`
  2. `hookAllMethods("com.miui.home.launcher.Folder", ..., "onLayout", callback)`
- **Hook target count**: 2
- **Requirement model**:
  - `SingleTargetRequirement` (REQUIRED) for `onFinishInflate`
  - `SingleTargetRequirement` (REQUIRED) for `onLayout`
- **AnyOf fallback**: No
- **activationRestartTarget**: `LAUNCHER_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

#### 8. `titleTopMargin` — SELECTED

- **featureId**: `titleTopMargin`
- **MainModule call**: `MainModule.java:790`
  ```java
  if (mPrefs.getInt("launcher_titletopmargin", 0) > 0) LauncherIconHooks.TitleTopMarginHook(lpparam);
  ```
- **ProcessTarget**: `Launcher`
- **preferenceKeys**: `launcher_titletopmargin`
- **condition**: `mPrefs.getInt("launcher_titletopmargin", 0) > 0`
- **installer**: `LauncherIconHooks.TitleTopMarginHook(lpparam)`
- **ModuleHelper calls**:
  1. `findAndHookMethod("com.miui.home.launcher.ItemIcon", ..., "onFinishInflate", callback)`
- **Hook target count**: 1
- **Requirement model**: `SingleTargetRequirement` (REQUIRED)
- **AnyOf fallback**: No
- **activationRestartTarget**: `LAUNCHER_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

#### 9. `hideLauncherSeekPoints` — NOT SELECTED

- **featureId**: `hideLauncherSeekPoints`
- **MainModule call**: `MainModule.java:813`
  ```java
  if (mPrefs.getBoolean("launcher_hideseekpoints")) LauncherLayoutHooks.HideSeekPointsHook(lpparam);
  ```
- **ProcessTarget**: `Launcher`
- **preferenceKeys**: `launcher_hideseekpoints`
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
- **Selected**: No (Launcher quota filled; uses Handler for animation)

## Selected Batch 2 Features (6 total)

| # | featureId | process | key | risk |
|---|-----------|---------|-----|------|
| 1 | `hideProximityWarning` | system_server | `system_hideproxywarn` | LOW |
| 2 | `clearAllTasks` | system_server | `system_clearalltasks` | LOW |
| 3 | `hideDismissView` | SystemUI | `system_removedismiss` | LOW |
| 4 | `hideLockScreenHint` | SystemUI | `system_hidelshint` | LOW |
| 5 | `folderColumns` | Launcher | `launcher_folder_cols` | LOW |
| 6 | `titleTopMargin` | Launcher | `launcher_titletopmargin` | LOW |

## Implementation checklist

1. `DiagnosticIds.kt`: add six new diagnostic IDs.
2. `PreferenceSchema.kt`: add entries for the six preference keys.
3. `CatalogContracts.kt`: add `HookTargetContract` for each selected feature.
4. `FeatureCatalog.kt`: add six `FeatureSpec` entries preserving MainModule call order.
5. `MainModule.java`: replace direct hook calls with `FeatureCatalog.installById`.
6. Test stubs for the new target classes/methods.
7. `CatalogBatch2Test.kt` and `FeatureCatalogTest` update.
8. Audit scripts: extend sequence and contract audits to cover batch-2.
