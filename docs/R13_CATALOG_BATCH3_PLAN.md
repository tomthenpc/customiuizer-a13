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
- **Selected**: No (SystemUI quota filled)

### Launcher candidates

#### 6. `noUnlockAnimation` — SELECTED

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

#### 7. `maxHotseatIconsCount` — WITHDRAWN

- **featureId**: `maxHotseatIconsCount`
- **MainModule call**: `MainModule.java:800`
  ```java
  if (mPrefs.getBoolean("launcher_unlockhotseat")) LauncherLayoutHooks.MaxHotseatIconsCountHook(lpparam);
  ```
- **Original behavior**:
  - `com.mi.android.globallauncher`: hook `DeviceConfig.getHotseatCount`
  - all other Launcher packages: hook `DeviceConfig.getHotseatMaxCount`
- **Withdrawal reason**:
  Static catalog contracts currently cannot express package-specific target
  applicability without changing the legacy installer behavior.
  The original installer chooses one method at runtime based on `lpparam.packageName`;
  the catalog `AnyOfRequirement` model would require both candidates to be installed
  (or the installer to be rewritten), which would alter the observed runtime behavior.
  A runtime-applicability model will be designed separately before this feature is
  re-evaluated.
- **Final state**: Direct hook call is preserved in `MainModule.java` with the
  original condition and package-name branch in `MaxHotseatIconsCountHook`.
  Catalog artifacts (contract, feature spec, diagnostic ID, preference schema
  owner entry, tests and audit mappings) are removed.
  The `launcher_unlockhotseat` preference XML is unchanged.
- **Risk level**: LOW
- **Selected**: No (withdrawn from catalog migration)

#### 8. `hideLauncherSeekPoints` — NOT SELECTED

- **featureId**: `hideLauncherSeekPoints`
- **MainModule call**: `MainModule.java:813`
- **Selected**: No (Launcher quota filled; uses a Handler for animation)

## Final Selected Batch 3 Features (5 total)

| # | featureId | process | key | risk |
|---|-----------|---------|-----|------|
| 1 | `noLightUpOnCharge` | system_server | `system_nolightuponcharges` | LOW |
| 2 | `allRotations` | system_server | `system_allrotations2` | LOW |
| 3 | `noNetworkSpeedSeparator` | SystemUI | `system_nonetspeedseparator` | LOW |
| 4 | `hideIconsClock` | SystemUI | `system_statusbaricons_clock` | LOW |
| 5 | `noUnlockAnimation` | Launcher | `launcher_nounlockanim` | LOW |

## Catalog totals after batch 3

- Canary: 8
- Batch 1: 6
- Batch 2: 6
- Batch 3: 5
- **Total catalog features: 25**

## Implementation checklist

1. `DiagnosticIds.kt`: add 5 new diagnostic IDs.
2. `PreferenceSchema.kt`: add 5 preference entries (no owner for
   `launcher_unlockhotseat`; it remains a direct hook preference).
3. `CatalogContracts.kt`: add `HookTargetContract` for each selected feature.
4. `FeatureCatalog.kt`: add 5 `FeatureSpec` entries preserving MainModule call order.
5. `MainModule.java`: replace direct hook calls for the 5 selected features with
   `FeatureCatalog.installById`; preserve the original direct call for
   `maxHotseatIconsCount`.
6. Test stubs for the new target classes/methods.
7. `CatalogBatch3Test.kt` and `FeatureCatalogTest` update.
8. Audit scripts: extend sequence and contract audits to cover batch-3 and add
   regression checks for the withdrawn `maxHotseatIconsCount`.
