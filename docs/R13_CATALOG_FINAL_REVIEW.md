# R13 Catalog Final Code Review

## 1. 审查基线

- **仓库**：`tomthenpc/customiuizer-a13`
- **基线分支**：`origin/devin/r13.8-install-evidence-correctness`
- **最终分支**：`origin/devin/r13.8-catalog-expansion-batch-3`（审查时据此切出 `devin/r13.8-catalog-final-review`）
- **最终代码 HEAD**：以 `devin/r13.8-catalog-expansion-batch-3` 的 `b0fd3e9` 为起点，最终审查分支上追加文档与修复
- **审查提交范围**：

```text
b0fd3e9 fix(catalog): preserve package-specific hotseat hook behavior
8016915 tools: extend catalog audits for batch-3
0f58e56 tests: add batch-3 catalog test coverage
d322dc6 MainModule: migrate batch-3 features to FeatureCatalog
535f750 catalog: add batch-3 specs, contracts, schema and diagnostic IDs
96898f0 docs: plan catalog expansion batch 3
7514eb3 tools: extend catalog audits for batch-2
66eb134 tests: add batch-2 catalog test coverage
886a5fd MainModule: migrate batch-2 features to FeatureCatalog
67a9085 catalog: add batch-2 specs, contracts, schema and diagnostic IDs
89414f8 docs: plan catalog expansion batch 2
8b0fb87 tools: extend catalog audits for batch-1
1fb3dc7 tests: add batch-1 catalog test coverage
224b347 MainModule: migrate batch-1 features to FeatureCatalog
6b5524a catalog: add batch-1 feature specs, contracts, schema and diagnostic IDs
ccdac04 docs: plan first catalog expansion batch
```

- **审查方法**：纯代码 diff 审查、单元测试运行、审计脚本、人工逐 feature installer/contract/schema/MainModule 核对。
- **设备操作次数**：0。所有结论均来自代码、单测和静态审计，未连接任何 Android 设备。

## 2. Catalog 总体状态

| 批次 | 数量 | 说明 |
|------|------|------|
| Canary | 8 | packagePermissions / statusBarClockTweak / autoBrightnessRange / muffledVibration / noMoreIcon / batteryIndicator / noClockHide / noWidgetOnly |
| Batch-1 | 6 | screenDimTime / firstVolumePress / networkIndicatorWifi / muteVisibleNotifications / hideLauncherTitles / fixAppInfoLaunch |
| Batch-2 | 6 | hideProximityWarning / clearAllTasks / hideDismissView / hideLockScreenHint / folderColumns / titleTopMargin |
| Batch-3 | 5 | noLightUpOnCharge / allRotations / noNetworkSpeedSeparator / hideIconsClock / noUnlockAnimation |
| **合计** | **25** | `maxHotseatIconsCount` 已撤回，不计入 |

- `DiagnosticIds`：29 个，全部唯一。
- `PreferenceSchema`：27 个 entry，全部 ownerFeature 指向有效 featureId。
- `FeatureCatalog.specs()`：25 个 `FeatureSpec`，id 唯一。
- `MainModule` 中 25 个 `FeatureCatalog.installById(...)` 调用位于原 direct Hook 调用位置，相对顺序保持不变。

## 3. 25 个 Feature 行为一致性表格

### 3.1 Canary（8 个）

| # | featureId | 中文名称 | ProcessTarget | preferenceKeys | condition（默认值） | installer | 主要 HookOperation | 最低成功条件 | fallback | 激活重启 | 热重载 | Parity 结论 |
|---|-----------|----------|---------------|----------------|--------------------|-----------|---------------------|--------------|----------|----------|--------|-------------|
| 1 | packagePermissions | 包权限放行 | SystemServer | （无） | 始终 true | PackagePermissions.hook | 5 个 requirement（2 个 ALL_METHODS、2 个 EXACT_METHOD、1 个 ALL_METHODS optional） | 至少 `PermissionManagerServiceImpl.shouldGrantPermissionBySignature`、`PackageManagerServiceUtils.verifySignatures`、`ApplicationInfo.isSystemApp` 成功 | 无（`isSignedWithPlatformKey` 与 `canShowWhenLocked` 为 optional） | REBOOT | NONE | Parity maintained |
| 2 | statusBarClockTweak | 状态栏时钟微调 | SystemUI | 4 个 key | `system_statusbar_clocktweak` / `system_cc_clocktweak` / `system_cc_hidedate` / `system_cc_dateformat` | SystemStatusBarClockAndMoreHooks.StatusBarClockTweakHook | 6 个 SingleTargetRequirement（2 个 ALL_CONSTRUCTORS、2 个 EXACT_METHOD、2 个 EXACT_METHOD optional） | 全部 required target 找到并 hook | 无 | SYSTEMUI_RESTART | PARTIAL | Parity maintained（修复前 compatibilityCheck 与 contract 不一致，见 §6） |
| 3 | autoBrightnessRange | 自动亮度范围 | SystemServer | `system_autobrightness` | `false` | SystemDisplayAndWindowHooks.AutoBrightnessRangeHook | 2 个 AnyOfRequirement（clamp、constructor 各 2 个候选） | 每个 AnyOf 至少命中 1 个 candidate | `DisplayPowerController`（class 不存在时） | REBOOT | NONE | Parity maintained（修复前 compatibilityCheck 未覆盖 fallback，见 §6） |
| 4 | muffledVibration | 减弱振动 | SystemServer | `system_vibration_amp` | `false` | SystemAudioAndVisualAndMoreHooks.MuffledVibrationHook | 1 个 SingleTargetRequirement：ALL_METHODS `VibratorService.doVibratorOn` | target class/method 存在 | 无 | REBOOT | NONE | Parity maintained |
| 5 | noMoreIcon | 隐藏更多图标 | SystemUI | `system_hidemoreicon` | `false` | SystemNotificationMoreHooks.NoMoreIconHook | 1 个 SingleTargetRequirement：EXACT_METHOD `NotificationIconAreaController.setIconsVisibility` | target 存在 | 无 | SYSTEMUI_RESTART | NONE | Parity maintained |
| 6 | batteryIndicator | 电池指示器 | SystemUI | `system_batteryindicator` | `false` | SystemUIBatteryHooks.BatteryIndicatorHook | 7 个 SingleTargetRequirement | 7 个 required target 全部命中 | 无 | SYSTEMUI_RESTART | NONE | Parity maintained |
| 7 | noClockHide | 禁止时钟隐藏 | Launcher | `launcher_noclockhide` | `false` | LauncherSystemHooks.NoClockHideHook | 1 个 SingleTargetRequirement：EXACT_METHOD `Launcher.updateStatusBarClock(Long)` | target 存在 | 无 | LAUNCHER_RESTART | NONE | Parity maintained |
| 8 | noWidgetOnly | 禁止仅小部件页面 | Launcher | `launcher_nowidgetonly` | `false` | LauncherLayoutHooks.NoWidgetOnlyHook | 1 个 SingleTargetRequirement：EXACT_METHOD `CellLayout.setScreenType(Int)` | target 存在 | 无 | LAUNCHER_RESTART | NONE | Parity maintained |

### 3.2 Batch-1（6 个）

| # | featureId | 中文名称 | ProcessTarget | preferenceKeys | condition（默认值） | installer | 主要 HookOperation | 最低成功条件 | fallback | 激活重启 | 热重载 | Parity 结论 |
|---|-----------|----------|---------------|----------------|--------------------|-----------|---------------------|--------------|----------|----------|--------|-------------|
| 9 | screenDimTime | 屏幕自动休眠时间 | SystemServer | `system_dimtime` | `0` | SystemAudioAndVisualAndMoreHooks.ScreenDimTimeHook | 2 个 EXACT_METHOD on `PowerManagerService` | 两个 target 都命中 | 无 | REBOOT | NONE | Parity maintained |
| 10 | firstVolumePress | 首次按音量键静音 | SystemServer | `system_firstpress` | `false` | SystemAudioAndVisualAndMoreHooks.FirstVolumePressHook | 1 个 EXACT_METHOD `AudioService$VolumeController.suppressAdjustment(Int, Int, Boolean)` | target 存在 | 无 | REBOOT | NONE | Parity maintained |
| 11 | networkIndicatorWifi | 网络指示器 WiFi | SystemUI | `system_networkindicator_wifi` | `false` | SystemStatusBarMoreHooks.NetworkIndicatorWifi | 1 个 ALL_METHODS `StatusBarWifiView.applyWifiState` | target 存在 | 无 | SYSTEMUI_RESTART | NONE | Parity maintained |
| 12 | muteVisibleNotifications | 静音可见通知 | SystemUI | `system_mutevisiblenotif` | `false` | SystemNotificationMoreHooks.MuteVisibleNotificationsHook | 1 个 ALL_METHODS `NotificationAlertController.buzzBeepBlink` | target 存在 | 无 | SYSTEMUI_RESTART | NONE | Parity maintained |
| 13 | hideLauncherTitles | 隐藏启动器标题 | Launcher | `launcher_hidetitles` | `false` | LauncherIconHooks.HideTitlesHook | 1 个 EXACT_METHOD `ItemIcon.onFinishInflate` | target 存在 | 无 | LAUNCHER_RESTART | NONE | Parity maintained |
| 14 | fixAppInfoLaunch | 修复应用信息启动 | Launcher | `launcher_fixlaunch` | `false` | LauncherSystemHooks.FixAppInfoLaunchHook | 1 个 AnyOfRequirement（`ShortcutMenuManager.startAppDetailsActivity` primary，`Utilities.startDetailsActivityForInfo` fallback） | 任一 candidate 命中并实际 hook | `Utilities` 路径（global launcher） | LAUNCHER_RESTART | NONE | Parity maintained；installer 仍保留 `packageName` 分支，需注意 package-specific 语义（见 §7） |

### 3.3 Batch-2（6 个）

| # | featureId | 中文名称 | ProcessTarget | preferenceKeys | condition（默认值） | installer | 主要 HookOperation | 最低成功条件 | fallback | 激活重启 | 热重载 | Parity 结论 |
|---|-----------|----------|---------------|----------------|--------------------|-----------|---------------------|--------------|----------|----------|--------|-------------|
| 15 | hideProximityWarning | 隐藏接近感应提示 | SystemServer | `system_hideproxywarn` | `false` | SystemDisplayAndWindowHooks.HideProximityWarningHook | 2 个 EXACT_METHOD `MiuiScreenOnProximityLock.showHint`、`prepareHintWindow` | 两个 target 都命中 | 无 | REBOOT | NONE | Parity maintained |
| 16 | clearAllTasks | 清除所有任务 | SystemServer | `system_clearalltasks` | `false` | SystemAudioAndVisualAndMoreHooks.ClearAllTasksHook | 1 个 ALL_METHODS `WindowProcessUtils.getPerceptibleRecentAppList` | target 存在 | 无 | REBOOT | NONE | Parity maintained |
| 17 | hideDismissView | 隐藏清除全部按钮 | SystemUI | `system_removedismiss` | `false` | SystemUINotificationHooks.HideDismissViewHook | 1 个 EXACT_METHOD `MiuiNotificationPanelViewController.updateDismissView` | target 存在 | 无 | SYSTEMUI_RESTART | NONE | Parity maintained |
| 18 | hideLockScreenHint | 隐藏锁屏提示 | SystemUI | `system_hidelshint` | `false` | SystemLockScreenMoreHooks.HideLockScreenHintHook | 1 个 EXACT_METHOD `KeyguardIndicationRotateTextViewController.hasIndicationsExceptResting` | target 存在 | 无 | SYSTEMUI_RESTART | NONE | Parity maintained |
| 19 | folderColumns | 文件夹列数 | Launcher | `launcher_folder_cols` | `1` | LauncherFolderHooks.FolderColumnsHook | 2 个 SingleTargetRequirement（EXACT `onFinishInflate` + ALL_METHODS `onLayout`） | 两个 target 都命中 | 无 | LAUNCHER_RESTART | NONE | Parity maintained |
| 20 | titleTopMargin | 标题上边距 | Launcher | `launcher_titletopmargin` | `0` | LauncherIconHooks.TitleTopMarginHook | 1 个 EXACT_METHOD `ItemIcon.onFinishInflate` | target 存在 | 无 | LAUNCHER_RESTART | NONE | Parity maintained |

### 3.4 Batch-3（5 个）

| # | featureId | 中文名称 | ProcessTarget | preferenceKeys | condition（默认值） | installer | 主要 HookOperation | 最低成功条件 | fallback | 激活重启 | 热重载 | Parity 结论 |
|---|-----------|----------|---------------|----------------|--------------------|-----------|---------------------|--------------|----------|----------|--------|-------------|
| 21 | noLightUpOnCharge | 充电不亮屏 | SystemServer | `system_nolightuponcharges` | `"1"` | SystemDisplayAndWindowHooks.NoLightUpOnChargeHook | 1 个 ALL_METHODS `PowerManagerService.wakePowerGroupLocked` | target 存在 | 无 | REBOOT | NONE | Parity maintained |
| 22 | allRotations | 允许所有旋转 | SystemServer | `system_allrotations2` | `"1"` | SystemAudioAndVisualAndMoreHooks.AllRotationsHook | 1 个 ALL_CONSTRUCTORS `DisplayRotation.<init>` | target 存在 | 无 | REBOOT | NONE | Parity maintained |
| 23 | noNetworkSpeedSeparator | 隐藏网络速度分隔符 | SystemUI | `system_nonetspeedseparator` | `false` | SystemUIStatusBarHooks.NoNetworkSpeedSeparatorHook | 2 个 EXACT_METHOD `NetworkSpeedSplitter.onClockVisibilityChanged(Int)`、`onNetworkSpeedVisibilityChanged(Int)` | 两个 target 都命中 | 无 | SYSTEMUI_RESTART | NONE | Parity maintained |
| 24 | hideIconsClock | 隐藏状态栏图标时钟 | SystemUI | `system_statusbaricons_clock` | `false` | SystemUIStatusBarHooks.HideIconsClockHook | 1 个 EXACT_METHOD `MiuiCollapsedStatusBarFragment.showClock(Boolean)` | target 存在 | 无 | SYSTEMUI_RESTART | NONE | Parity maintained |
| 25 | noUnlockAnimation | 无解锁动画 | Launcher | `launcher_nounlockanim` | `false` | LauncherAnimationHooks.NoUnlockAnimationHook | 1 个 ALL_METHODS `MiuiSettingsUtils.isSystemAnimationOpen` | target 存在 | 无 | LAUNCHER_RESTART | NONE | Parity maintained；`MainModule` 仅在 `com.miui.home` 分支调用，ProcessTarget 标注为 `Launcher` 实际受 MainModule 位置限制 |

## 4. MainModule 顺序与条件审计

- 25 个 catalog feature 的 `FeatureCatalog.installById` 调用顺序与原 direct Hook 顺序一致。
- 条件由 `FeatureSpec.condition` 承担；`MainModule` 中部分 catalog 调用前仍保留 `if`（如 `screenDimTime`、`firstVolumePress` 等），这是为了向后兼容且与原 `if` 位置一致；`FeatureCatalog.installById` 内部会再次判断 condition。
- 无 `direct call` 与 `Catalog` 重复执行同一功能。
- `maxHotseatIconsCount` 已恢复为直接调用 `LauncherLayoutHooks.MaxHotseatIconsCountHook(lpparam)`，并保留 package-specific `methodName` 分支。

## 5. Contract / Installer 一致性审计

- 每个 contract 的 operation、className、memberName、parameterTypes 与 installer 中实际 `ModuleHelper` 调用一致。
- `EXACT_METHOD` 与 `ALL_METHODS_BY_NAME` 未混淆。
- 已撤回 feature 无 catalog 残留。
- 审查发现的 P1 兼容性问题并修复，见 §6。

## 6. 发现的问题与修复

### 6.1 修复： Canary `statusBarClockTweak` 与 `autoBrightnessRange` 的 `compatibilityCheck` 与 Contract 不匹配

**问题描述**

- `statusBarClockTweak` 的 `compatibilityCheck` 使用 `resolveFirstClass` 探测 `MiuiStatusBarClockController` 与 `StatusBarClockController`。
- 但 contract 中并不存在 `StatusBarClockController`；contract 只包含 `MiuiStatusBarClockController` 与 `MiuiClock` 相关 target。
- 当 `StatusBarClockController` 存在而 `MiuiStatusBarClockController` 不存在时，`compatibilityCheck` 返回 `DEGRADED`，随后 `installWithContract` 会基于 contract 判断为 `INCOMPATIBLE` 并失败。这导致一个虚假的 `DEGRADED` 后立即失败的诊断。
- `autoBrightnessRange` 的 `compatibilityCheck` 仅探测 `AutomaticBrightnessController`，但 contract 使用 `AnyOfRequirement` 同时把 `DisplayPowerController` 作为 fallback。当 `AutomaticBrightnessController` 不存在而 `DisplayPowerController` 存在时，原 `compatibilityCheck` 直接返回 `INCOMPATIBLE`，阻止了 contract 已声明的 fallback 路径生效。

**修复方式**

将两个 feature 的 `compatibilityCheck` 改为 `{ _ -> CompatibilityState.COMPATIBLE }`，把兼容性判断完全交给 `HookTargetResolver.evaluateContract(contract)`。

- 好处：contract 是唯一的 compatibility 真实来源，避免 `compatibilityCheck` 与 contract 目标集合不一致。
- 影响：对于完全不存在目标的情况，contract 评估后仍会返回 `INCOMPATIBLE` 并记录 `TARGET_NOT_FOUND`，行为不变；对于存在 fallback 的情况，fallback 现在可以正确降级为 `DEGRADED` 并尝试安装。

**提交示例**

```text
fix(catalog): align canary compatibility checks with contracts

- statusBarClockTweak compatibilityCheck no longer probes
  StatusBarClockController, which is not in the contract.
- autoBrightnessRange compatibilityCheck no longer short-circuits
  on AutomaticBrightnessController alone, allowing the contract's
  DisplayPowerController fallback to work.
```

**验证**

- `./gradlew :app:testDebugUnitTest`：通过
- 新增/既有单测 `StatusBarClockTweakClosedLoopTest`、`FeatureCatalogTest` 仍期望 `INCOMPATIBLE/FAILED`（目标不存在时）与 `COMPATIBLE/INSTALLED`（测试 stub 全命中时），全部通过。

### 6.2 已处理：Batch-3 `maxHotseatIconsCount` 撤回

- 原因：installer 根据 `lpparam.packageName == "com.mi.android.globallauncher"` 选择 `getHotseatCount` 或 `getHotseatMaxCount`。静态 catalog contract 无法表达“package-specific 二选一”而不改变原行为或强行安装两个 hook。
- 处理：恢复 `MainModule` 直接调用 `LauncherLayoutHooks.MaxHotseatIconsCountHook(lpparam)`，删除 catalog 相关 contract/spec/diagnostic/schema/test/audit 映射。

### 6.3 待观察：Batch-1 `fixAppInfoLaunch` 的 package-specific 分支

- installer 仍保留 `if (lpparam.packageName == "com.mi.android.globallauncher")` else 分支：
  - global launcher → hook `com.miui.home.launcher.util.Utilities.startDetailsActivityForInfo`
  - 其他 launcher → hook `com.miui.home.launcher.shortcuts.ShortcutMenuManager.startAppDetailsActivity`
- catalog contract 使用 `AnyOfRequirement` 把两个类列为 primary/fallback。
- 当前实现不会同时安装两个 hook，因为 `if/else` 只进入一支；最终 evidence 会根据实际 hook 的是哪一支标记 `fallbackUsed`。
- 该 feature 未出现 `maxHotseatIconsCount` 级别的静态 Applicability 冲突，因此本轮**不撤回**。下一轮在增加 runtime package-specific applicability 模型时应优先重新评估此 feature。

## 7. Package-Specific / 互斥 Hook 问题清单

| featureId | 类型 | 当前状态 | 说明 |
|-----------|------|----------|------|
| `maxHotseatIconsCount` | package-specific method | 已撤回 | 原实现按 package 选择 `getHotseatCount` / `getHotseatMaxCount`，静态 contract 不能表达。 |
| `fixAppInfoLaunch` | package-specific class | 保留，待观察 | installer 按 package 二选一；contract 用 AnyOf 覆盖两个候选。功能未变，但语义不完全等价。 |

未发现其他 feature 存在 package-specific 分支或原本互斥但迁移后同时安装的 Hook。

## 8. 基础设施复核结论

### 8.1 HookInstaller

- `withSession` 使用 try/finally 清理 `ThreadLocal`。
- 嵌套 session 抛出 `IllegalStateException`。
- 异常后 `session.remove()` 保证 ThreadLocal 清空。
- `isRecording()` 为非 catalog 路径只增加一次 boolean 检查，零额外反射。
- 不保存 Context、View、callback 或 Throwable；只保存稳定 target id、resolution/hook 状态和简短 failure reason。

### 8.2 HookEvidenceEvaluator

- compatibility 与 installation 使用同一 `evaluate` 逻辑，仅 phase 不同。
- `requiredTotal` 按 Requirement 数计算。
- `AnyOfRequirement` 命中任一 candidate 即算该 requirement 成功；非 primary 命中标记 `fallbackUsed`。
- optional 失败不导致 `FAILED`。
- `HookInstallResult` 一次性计算完成，不可变，无二次计算差异。

### 8.3 HookTargetResolver

- 每个 `FeatureRuntime` 持有独立 resolver；cache 按 `ClassLoader` 隔离。
- exact method、all methods、constructor、field 使用不同 key，不会混淆。
- 参数签名使用 `Class.name` 规范化缓存。
- 不在静态集合中长期持有 ROM ClassLoader。

### 8.4 DiagnosticRecorder

- enabled、compatibility、installation 三个维度独立存储。
- snapshot 覆盖策略让 installation 结果不覆盖 compatibility 结论。
- reasonCode 稳定；detail 可能包含类名但不参与限流 key。
- `ConcurrentHashMap` 有界，大小最多为 feature/diagnostic 数量。
- 限流 60s，仅 FAILED/DEGRADED 重复时限制；状态升级立即记录。
- throwable 仅记录类名与前 80 字符消息，不保存 stack trace。

### 8.5 FeatureCatalog

- `MainModule` 仍按顺序逐个调用 `installById`，无统一循环批量安装。
- `FeatureRuntime` 在同一进程复用，`resolver` 不重复创建。
- 单个 feature 失败被 catch 并记录，不影响后续 feature。
- `byId` 查找失败安全返回 false。
- `ProcessTarget.matches` 支持 `SystemServer`、`SystemUI`、`Launcher` 及任意 `Package`。

## 9. PreferenceSchema 审查

- 27 个 `PreferenceEntry` 均存在合法 ownerFeature。
- 默认值与原代码中 `mPrefs.get...` 的默认值一致。
- 每个 feature 的 `preferenceKeys` 集合覆盖 condition 中读取的所有 key。
- 未把子参数错误提升为 feature 启用条件；未遗漏组合 condition 中的 key。
- 未补录非 Catalog 的数百个 preference key。

## 10. 测试覆盖

- 单元测试：包含 `FeatureCatalogTest`、`StatusBarClockTweakClosedLoopTest`、`CatalogBatch1Test`、`CatalogBatch2Test`、`CatalogBatch3Test`、`HookEvidenceEvaluatorTest`、`HookTargetResolverTest`。
- 覆盖点：
  - disabled 不反射
  - 目标不存在时 `INCOMPATIBLE/FAILED`
  - 目标命中时 `COMPATIBLE/INSTALLED`
  - `AnyOf` fallback 命中时 `DEGRADED`
  - installer 异常隔离
  - `processTarget.matches` 按包名正确过滤
  - `maxHotseatIconsCount` 无 catalog 残留

## 11. 代码级验证边界

- 已通过：单元测试、lint、Release R8 构建、本地审计脚本。
- 尚未验证：MIUI 14 / Android 13 实机启动、SystemUI/Launcher 稳定性、25 个 feature 的实际 UI 行为。
- 不能由代码验证的项：不同 MIUI 14 小版本/国际版/印度版 ROM 中 class 存在性差异、Launcher 包名差异、系统资源 ID 变化、Hook 运行时抛出与恢复行为。

## 12. 实机验证状态

- **未执行任何 ADB / 真机 / LSPosed 操作。**
- SystemUI、Launcher、system_server 均未在实机上启动验证。
- 所有功能标记为 `待实机验证`（见 `docs/R13_MIUI14_DEVICE_SMOKE_TEST.md`）。

## 13. 是否建议继续 batch-4

**不建议。**

当前 25 个 feature 已覆盖 Canary + 3 个 batch，且刚完成一轮大规模 catalog 迁移和修复。batch-4 应在以下事项完成后考虑：

1. 当前 25 个 feature 在 MIUI 14 / Android 13 实机上通过冒烟测试；
2. 明确 package-specific applicability 的通用模型（否则会出现第二个 `maxHotseatIconsCount` 类型问题）；
3. 用户确认需要继续扩张 catalog。
