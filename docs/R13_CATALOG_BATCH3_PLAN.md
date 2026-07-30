# R13 Catalog Expansion Batch 3

This plan audits the next set of low-risk features for migration into
`FeatureCatalog` after batch 2.

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

#### 1. `noLightUpOnCharge` — SELECTED

- **featureId**: `noLightUpOnCharge`
- **MainModule call**: `MainModule.java:284`
  ```java
  if (mPrefs.getStringAsInt("system_nolightuponcharges", 1) > 1) SystemDisplayAndWindowHooks.NoLightUpOnChargeHook(lpparam);
  ```
- **ProcessTarget**: `SystemServer`
- **preferenceKeys**: `system_nolightuponcharges`
- **condition**: `mPrefs.getStringAsInt("system_nolightuponcharges", 1) > 1`
- **installer**: `SystemDisplayAndWindowHooks.NoLightUpOnChargeHook(lpparam)`
- **ModuleHelper calls**:
  1. `hookAllMethods("com.android.server.power.PowerManagerService", ..., "wakePowerGroupLocked", callback)`
- **Hook target count**: 1
- **Requirement model**: `SingleTargetRequirement` (REQUIRED)
- **AnyOf fallback**: No
- **activationRestartTarget**: `REBOOT`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

#### 2. `allRotations` — SELECTED

- **featureId**: `allRotations`
- **MainModule call**: `MainModule.java:283`
  ```java
  if (mPrefs.getStringAsInt("system_allrotations2", 1) > 1) SystemAudioAndVisualAndMoreHooks.AllRotationsHook(lpparam);
  ```
- **ProcessTarget**: `SystemServer`
- **preferenceKeys**: `system_allrotations2`
- **condition**: `mPrefs.getStringAsInt("system_allrotations2", 1) > 1`
- **installer**: `SystemAudioAndVisualAndMoreHooks.AllRotationsHook(lpparam)`
- **ModuleHelper calls**:
  1. `hookAllConstructors("com.android.server.wm.DisplayRotation", ..., callback)`
- **Hook target count**: 1
- **Requirement model**: `SingleTargetRequirement` (REQUIRED)
- **AnyOf fallback**: No
- **activationRestartTarget**: `REBOOT`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

### SystemUI candidates

#### 3. `noNetworkSpeedSeparator` — SELECTED

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
- **Selected**: Yes

#### 4. `hideIconsClock` — SELECTED

- **featureId**: `hideIconsClock`
- **MainModule call**: `MainModule.java:517`
  ```java
  if (mPrefs.getBoolean("system_statusbaricons_clock")) SystemUIStatusBarHooks.HideIconsClockHook(lpparam);
  ```
- **ProcessTarget**: `SystemUI`
- **preferenceKeys**: `system_statusbaricons_clock`
- **condition**: `mPrefs.getBoolean("system_statusbaricons_clock")`
- **installer**: `SystemUIStatusBarHooks.HideIconsClockHook(lpparam)`
- **ModuleHelper calls**:
  1. `findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", ..., "showClock", Boolean, callback)`
- **Hook target count**: 1
- **Requirement model**: `SingleTargetRequirement` (REQUIRED)
- **AnyOf fallback**: No
- **activationRestartTarget**: `SYSTEMUI_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

#### 5. `hideCCOperatorDelimiter` — NOT SELECTED

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

#### 6. `maxHotseatIconsCount` — SELECTED

- **featureId**: `maxHotseatIconsCount`
- **MainModule call**: `MainModule.java:800`
  ```java
  if (mPrefs.getBoolean("launcher_unlockhotseat")) LauncherLayoutHooks.MaxHotseatIconsCountHook(lpparam);
  ```
- **ProcessTarget**: `Launcher`
- **preferenceKeys**: `launcher_unlockhotseat`
- **condition**: `mPrefs.getBoolean("launcher_unlockhotseat")`
- **installer**: `LauncherLayoutHooks.MaxHotseatIconsCountHook(lpparam)`
- **ModuleHelper calls**:
  1. `findAndHookMethodSilently("com.miui.home.launcher.DeviceConfig", ..., "getHotseatCount", returnConstant(666))`
  2. `findAndHookMethodSilently("com.miui.home.launcher.DeviceConfig", ..., "getHotseatMaxCount", returnConstant(666))`
- **Hook target count**: 1 (the method that exists on the running launcher variant)
- **Requirement model**: `AnyOfRequirement` (REQUIRED) with two candidates:
  - `DeviceConfig.getHotseatCount`
  - `DeviceConfig.getHotseatMaxCount`
- **AnyOf fallback**: Yes (package-dependent)
- **activationRestartTarget**: `LAUNCHER_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

#### 7. `noUnlockAnimation` — SELECTED

- **featureId**: `noUnlockAnimation`
- **MainModule call**: `MainModule.java:818`
  ```java
  if (mPrefs.getBoolean("launcher_nounlockanim")) LauncherAnimationHooks.NoUnlockAnimationHook(lpparam);
  ```
- **ProcessTarget**: `Launcher`
- **preferenceKeys**: `launcher_nounlockanim`
- **condition**: `mPrefs.getBoolean("launcher_nounlockanim")`
- **installer**: `LauncherAnimationHooks.NoUnlockAnimationHook(lpparam)`
- **ModuleHelper calls**:
  1. `hookAllMethods("com.miui.launcher.utils.MiuiSettingsUtils", ..., "isSystemAnimationOpen", returnConstant(false))`
- **Hook target count**: 1
- **Requirement model**: `SingleTargetRequirement` (REQUIRED)
- **AnyOf fallback**: No
- **activationRestartTarget**: `LAUNCHER_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: Yes

#### 8. `hideLauncherSeekPoints` — NOT SELECTED

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
  1. `findAndHookMethod("com.miui.home.launcher.pageindicators.AllAppsIndicator", ..., "shouldHide", returnConstant(true))`
  2. `findAndHookMethod("com.miui.home.launcher.pageindicators.AllAppsIndicator", ..., "hideAllAppsArrow", callback)`
- **Hook target count**: 2
- **Requirement model**:
  - `SingleTargetRequirement` (REQUIRED) for `shouldHide`
  - `SingleTargetRequirement` (REQUIRED) for `hideAllAppsArrow`
- **AnyOf fallback**: No
- **activationRestartTarget**: `LAUNCHER_RESTART`
- **configReloadMode**: `NONE`
- **Risk level**: LOW
- **Selected**: No (Launcher quota filled; uses a Handler for animation)

## Selected Batch 3 Features (6 total)

| # | featureId | process | key | risk |
|---|-----------|---------|-----|------|
| 1 | `noLightUpOnCharge` | system_server | `system_nolightuponcharges` | LOW |
| 2 | `allRotations` | system_server | `system_allrotations2` | LOW |
| 3 | `noNetworkSpeedSeparator` | SystemUI | `system_nonetspeedseparator` | LOW |
| 4 | `hideIconsClock` | SystemUI | `system_statusbaricons_clock` | LOW |
| 5 | `maxHotseatIconsCount` | Launcher | `launcher_unlockhotseat` | LOW |
| 6 | `noUnlockAnimation` | Launcher | `launcher_nounlockanim` | LOW |

## Implementation checklist

1. `DiagnosticIds.kt`: add six new diagnostic IDs.
2. `PreferenceSchema.kt`: add entries for the six preference keys.
3. `CatalogContracts.kt`: add `HookTargetContract` for each selected feature.
4. `FeatureCatalog.kt`: add six `FeatureSpec` entries preserving MainModule call order.
5. `MainModule.java`: replace direct hook calls with `FeatureCatalog.installById`.
6. Test stubs for the new target classes/methods.
7. `CatalogBatch3Test.kt` and `FeatureCatalogTest` update.
8. Audit scripts: extend sequence and contract audits to cover batch-3.
