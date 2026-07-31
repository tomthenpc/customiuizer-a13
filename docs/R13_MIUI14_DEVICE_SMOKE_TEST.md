# R13 MIUI 14 / Android 13 人工实机冒烟测试清单

**重要声明**：

- 本文件仅为 **人工测试计划**。
- **本轮未连接、读取、修改或控制任何 Android 设备。**
- 未执行 `adb`、未安装 APK、未修改 LSPosed 作用域、未重启任何进程或手机。
- 所有测试项当前状态为 `NOT TESTED`。

## 1. 测试原则

1. 每次只启用一个新功能。
2. 先记录关闭状态的基线。
3. 启用后按 `activationRestartTarget` 手动重启对应进程。
4. 验证功能表现后，再验证关闭恢复。
5. 不同时启用多个可能互相影响的状态栏或 Launcher 功能。
6. `system_server` 相关功能按低风险到高风险排列。
7. 发生 SystemUI / Launcher 崩溃时，立即关闭对应功能并恢复。
8. 不清除用户数据、不恢复出厂设置、不修改系统关键设置。
9. 不测试安全绕过或签名类未迁移功能。
10. 不自动执行任何脚本或 adb 命令流水线。

## 2. 目标设备

- Android 13
- MIUI 14
- arm64-v8a
- LSPosed / libxposed 环境
- 模块包名：`tv.withaibuild.customiuizer.r13`

## 3. 通用前置条件

- 手机已完成 root / LSPosed 激活。
- 模块已在 LSPosed 中启用，作用域包含 `系统框架`、`SystemUI`、`Launcher`（`com.miui.home` 或 `com.mi.android.globallauncher`）。
- CustoMIUIzer 设置应用可正常打开并修改偏好。
- 手机处于稳定状态，未运行其他测试工具。
- 已记录 LSPosed 日志开关状态（建议关闭 verbose 后再开启，避免日志量过大）。

## 4. 通用恢复步骤

1. 打开 CustoMIUIzer 设置应用。
2. 进入对应设置页面。
3. 关闭刚刚启用的开关或恢复默认值。
4. 按 `activationRestartTarget` 执行对应重启：
   - `REBOOT`：手动重启手机。
   - `SYSTEMUI_RESTART`：在 LSPosed 中取消勾选 SystemUI 作用域并重新勾选，或从最近任务划掉并等待 SystemUI 重启；也可执行 `Settings → 应用 → 系统界面 → 强行停止`。
   - `LAUNCHER_RESTART`：从最近任务划掉 Launcher 或 `Settings → 应用 → 系统桌面 / Poco Launcher → 强行停止`。
5. 验证异常现象消失。

## 5. 检查清单

### 5.1 模块加载基线

| 检查项 | 方法 | 通过标准 |
|--------|------|----------|
| 模块在 LSPosed 中已启用 | 打开 LSPosed 管理器 | 模块开关为开，无红色错误 |
| 作用域正确 | LSPosed 管理器 → 模块 → 作用域 | 包含 系统框架、SystemUI、Launcher |
| 模块能输出日志 | LSPosed 管理器 → 日志 | 能搜索到 `tv.withaibuild.customiuizer.r13` 相关行 |
| SystemUI 未崩溃 | 下拉状态栏、打开控制中心 | 状态栏正常显示，无重启循环 |
| Launcher 未崩溃 | 返回桌面、滑动抽屉 | 桌面正常加载，无 ANR |

---

### 5.2 Canary Features

#### C1. packagePermissions

- **featureId**：`packagePermissions`
- **中文名称**：包权限放行
- **ProcessTarget**：`SystemServer`
- **偏好键**：无（始终启用）
- **测试前置条件**：LSPosed 作用域包含系统框架；模块已启用；需要安装一个普通第三方应用用于验证。
- **启用步骤**：无需在 CustoMIUIzer 中手动开启，保持模块启用即可。
- **需要重启的进程**：系统框架（需 `REBOOT`）。
- **正常结果**：
  - 模块自己的包被系统视为系统应用或平台签名；
  - 安装/升级模块相关测试 APK 时不再出现“安装来源未知”或签名冲突拦截。
- **异常表现**：
  - SystemUI/Launcher 无法启动；
  - 安装 APK 仍被拒绝；
  - `PermissionManagerServiceImpl` 或 `PackageManagerServiceUtils` 连续崩溃。
- **恢复步骤**：关闭整个模块或在 LSPosed 中禁用模块，重启手机。
- **检查日志关键字**：`Diagnostic[PACKAGE_PERMISSIONS]`、`PermissionManagerServiceImpl`。
- **状态**：NOT TESTED
- **备注**：高风险功能，涉及签名/权限系统，稳定后再测试其他功能。

#### C2. statusBarClockTweak

- **featureId**：`statusBarClockTweak`
- **中文名称**：状态栏时钟微调
- **ProcessTarget**：`SystemUI`
- **偏好键**：`system_statusbar_clocktweak`、`system_cc_clocktweak`、`system_cc_hidedate`、`system_cc_dateformat`
- **测试前置条件**：状态栏有默认时钟显示。
- **启用步骤**：
  1. 设置 → 状态栏 → 开启“显示秒” 或 “隐藏日期”。
  2. 或设置 → 控制中心 → 开启“隐藏日期”。
- **需要重启的进程**：`SYSTEMUI_RESTART`
- **正常结果**：
  - 状态栏时间显示秒数；
  - 控制中心日期被隐藏。
- **异常表现**：
  - 状态栏时钟空白或 FC；
  - SystemUI 崩溃循环。
- **恢复步骤**：关闭对应开关，重启 SystemUI。
- **检查日志关键字**：`Diagnostic[STATUSBAR_CLOCK_TWEAK]`、`MiuiStatusBarClockController`。
- **状态**：NOT TESTED

#### C3. autoBrightnessRange

- **featureId**：`autoBrightnessRange`
- **中文名称**：自动亮度范围
- **ProcessTarget**：`SystemServer`
- **偏好键**：`system_autobrightness`
- **测试前置条件**：手机支持自动亮度；环境光线可变。
- **启用步骤**：设置 → 显示 → 自动亮度 → 开启。
- **需要重启的进程**：`REBOOT`
- **正常结果**：
  - 自动亮度最低值/最高值被限制（不刺眼了）。
- **异常表现**：
  - 亮度卡在最低或最高；
  - 亮度调节失效。
- **恢复步骤**：关闭自动亮度或关闭模块中该功能，重启。
- **检查日志关键字**：`Diagnostic[AUTO_BRIGHTNESS_RANGE]`、`AutomaticBrightnessController`、`DisplayPowerController`。
- **状态**：NOT TESTED

#### C4. muffledVibration

- **featureId**：`muffledVibration`
- **中文名称**：减弱振动
- **ProcessTarget**：`SystemServer`
- **偏好键**：`system_vibration_amp`
- **测试前置条件**：手机支持振动。
- **启用步骤**：设置 → 声音与振动 → 开启“减弱振动”。
- **需要重启的进程**：`REBOOT`
- **正常结果**：
  - 通知/来电振动强度明显降低。
- **异常表现**：
  - 振动完全消失或异常；
  - SystemUI 无响应。
- **恢复步骤**：关闭开关，重启。
- **检查日志关键字**：`Diagnostic[MUFFLED_VIBRATION]`、`VibratorService`。
- **状态**：NOT TESTED

#### C5. noMoreIcon

- **featureId**：`noMoreIcon`
- **中文名称**：隐藏更多图标
- **ProcessTarget**：`SystemUI`
- **偏好键**：`system_hidemoreicon`
- **测试前置条件**：状态栏图标足够多，会出现“更多图标”小点。
- **启用步骤**：设置 → 状态栏 → 开启“隐藏更多图标”。
- **需要重启的进程**：`SYSTEMUI_RESTART`
- **正常结果**：
  - 状态栏右侧不再显示“...”更多图标提示。
- **异常表现**：
  - 状态栏图标区域空白或重叠；
  - SystemUI 崩溃。
- **恢复步骤**：关闭开关，重启 SystemUI。
- **检查日志关键字**：`Diagnostic[NO_MORE_ICON]`、`NotificationIconAreaController`。
- **状态**：NOT TESTED

#### C6. batteryIndicator

- **featureId**：`batteryIndicator`
- **中文名称**：电池指示器
- **ProcessTarget**：`SystemUI`
- **偏好键**：`system_batteryindicator`
- **测试前置条件**：电量不是 100%。
- **启用步骤**：设置 → 状态栏 → 电池 → 开启“电池指示器”。
- **需要重启的进程**：`SYSTEMUI_RESTART`
- **正常结果**：
  - 状态栏/锁屏出现额外电池百分比或环形指示器。
- **异常表现**：
  - 状态栏电池图标消失；
  - SystemUI 崩溃；
  - 充电动画异常。
- **恢复步骤**：关闭开关，重启 SystemUI。
- **检查日志关键字**：`Diagnostic[BATTERY_INDICATOR]`、`CentralSurfacesImpl`。
- **状态**：NOT TESTED

#### C7. noClockHide

- **featureId**：`noClockHide`
- **中文名称**：禁止时钟隐藏
- **ProcessTarget**：`Launcher`
- **偏好键**：`launcher_noclockhide`
- **测试前置条件**：默认 Launcher 在抽屉/桌面上有时会隐藏状态栏时钟。
- **启用步骤**：设置 → 桌面 → 开启“不隐藏时钟”。
- **需要重启的进程**：`LAUNCHER_RESTART`
- **正常结果**：
  - 进入抽屉或应用列表时，状态栏时钟保持显示。
- **异常表现**：
  - Launcher FC；
  - 状态栏时钟仍然被隐藏。
- **恢复步骤**：关闭开关，重启 Launcher。
- **检查日志关键字**：`Diagnostic[NO_CLOCK_HIDE]`、`com.miui.home.launcher.Launcher`。
- **状态**：NOT TESTED

#### C8. noWidgetOnly

- **featureId**：`noWidgetOnly`
- **中文名称**：禁止仅小部件页面
- **ProcessTarget**：`Launcher`
- **偏好键**：`launcher_nowidgetonly`
- **测试前置条件**：有空白桌面页面或仅小部件页面。
- **启用步骤**：设置 → 桌面 → 开启“禁止仅小部件页面”。
- **需要重启的进程**：`LAUNCHER_RESTART`
- **正常结果**：
  - 仅包含小部件的页面被禁止创建或自动合并。
- **异常表现**：
  - 桌面布局异常；
  - Launcher 崩溃。
- **恢复步骤**：关闭开关，重启 Launcher。
- **检查日志关键字**：`Diagnostic[NO_WIDGET_ONLY]`、`com.miui.home.launcher.CellLayout`。
- **状态**：NOT TESTED

---

### 5.3 Batch-1 Features

#### B1-1. screenDimTime

- **featureId**：`screenDimTime`
- **中文名称**：屏幕自动休眠时间
- **ProcessTarget**：`SystemServer`
- **偏好键**：`system_dimtime`
- **测试前置条件**：设置 → 省电 中可设置屏幕变暗时间。
- **启用步骤**：设置 → 显示 / 省电 → 设置屏幕变暗时间 > 0。
- **需要重启的进程**：`REBOOT`
- **正常结果**：
  - 无操作后屏幕按设定时间变暗/关闭。
- **异常表现**：
  - 屏幕不关闭或立即关闭；
  - 耗电异常。
- **恢复步骤**：恢复为 0 或默认值，重启。
- **检查日志关键字**：`Diagnostic[SCREEN_DIM_TIME]`、`PowerManagerService`。
- **状态**：NOT TESTED

#### B1-2. firstVolumePress

- **featureId**：`firstVolumePress`
- **中文名称**：首次按音量键静音
- **ProcessTarget**：`SystemServer`
- **偏好键**：`system_firstpress`
- **测试前置条件**：正在播放媒体。
- **启用步骤**：设置 → 声音 → 开启“首次按下音量键即静音”。
- **需要重启的进程**：`REBOOT`
- **正常结果**：
  - 第一次按音量减键，媒体直接静音而不是逐渐降低。
- **异常表现**：
  - 音量调节无效；
  - 音量直接降到最低。
- **恢复步骤**：关闭开关，重启。
- **检查日志关键字**：`Diagnostic[FIRST_VOLUME_PRESS]`、`AudioService`。
- **状态**：NOT TESTED

#### B1-3. networkIndicatorWifi

- **featureId**：`networkIndicatorWifi`
- **中文名称**：网络指示器 WiFi
- **ProcessTarget**：`SystemUI`
- **偏好键**：`system_networkindicator_wifi`
- **测试前置条件**：连接 WiFi 并有数据传输。
- **启用步骤**：设置 → 状态栏 → 开启“网络速度指示器 / WiFi 指示”。
- **需要重启的进程**：`SYSTEMUI_RESTART`
- **正常结果**：
  - 状态栏 WiFi 图标右侧出现实时上传/下载箭头或数字。
- **异常表现**：
  - 状态栏空白；
  - 网络速度数字不更新。
- **恢复步骤**：关闭开关，重启 SystemUI。
- **检查日志关键字**：`Diagnostic[NETWORK_INDICATOR_WIFI]`、`StatusBarWifiView`。
- **状态**：NOT TESTED

#### B1-4. muteVisibleNotifications

- **featureId**：`muteVisibleNotifications`
- **中文名称**：静音可见通知
- **ProcessTarget**：`SystemUI`
- **偏好键**：`system_mutevisiblenotif`
- **测试前置条件**：有应用在屏幕开启时发送通知。
- **启用步骤**：设置 → 通知 → 开启“屏幕开启时静音可见通知”。
- **需要重启的进程**：`SYSTEMUI_RESTART`
- **正常结果**：
  - 屏幕点亮时，通知不振动/不发声，但仍显示。
- **异常表现**：
  - 所有通知都不响；
  - SystemUI 崩溃。
- **恢复步骤**：关闭开关，重启 SystemUI。
- **检查日志关键字**：`Diagnostic[MUTE_VISIBLE_NOTIFICATIONS]`、`NotificationAlertController`。
- **状态**：NOT TESTED

#### B1-5. hideLauncherTitles

- **featureId**：`hideLauncherTitles`
- **中文名称**：隐藏桌面图标标题
- **ProcessTarget**：`Launcher`
- **偏好键**：`launcher_hidetitles`
- **测试前置条件**：桌面有应用图标。
- **启用步骤**：设置 → 桌面 → 开启“隐藏图标标题”。
- **需要重启的进程**：`LAUNCHER_RESTART`
- **正常结果**：
  - 桌面图标下方文字消失。
- **异常表现**：
  - 图标文字仍显示；
  - Launcher 崩溃。
- **恢复步骤**：关闭开关，重启 Launcher。
- **检查日志关键字**：`Diagnostic[HIDE_LAUNCHER_TITLES]`、`ItemIcon`。
- **状态**：NOT TESTED

#### B1-6. fixAppInfoLaunch

- **featureId**：`fixAppInfoLaunch`
- **中文名称**：修复应用信息启动
- **ProcessTarget**：`Launcher`
- **偏好键**：`launcher_fixlaunch`
- **测试前置条件**：桌面有图标；长按图标可进入应用信息。
- **启用步骤**：设置 → 桌面 → 开启“修复应用信息启动”。
- **需要重启的进程**：`LAUNCHER_RESTART`
- **正常结果**：
  - 长按图标 → 应用信息 能正常跳转到系统应用详情。
- **异常表现**：
  - 应用信息无法打开；
  - Launcher 崩溃。
- **恢复步骤**：关闭开关，重启 Launcher。
- **检查日志关键字**：`Diagnostic[FIX_APP_INFO_LAUNCH]`、`ShortcutMenuManager`。
- **状态**：NOT TESTED
- **备注**：global launcher（`com.mi.android.globallauncher`）与国内版桌面可能行为不同，应分别测试。

---

### 5.4 Batch-2 Features

#### B2-1. hideProximityWarning

- **featureId**：`hideProximityWarning`
- **中文名称**：隐藏接近感应提示
- **ProcessTarget**：`SystemServer`
- **偏好键**：`system_hideproxywarn`
- **测试前置条件**：支持接近传感器；通话时贴近耳朵会触发提示。
- **启用步骤**：设置 → 通话 / 系统 → 开启“隐藏接近感应提示”。
- **需要重启的进程**：`REBOOT`
- **正常结果**：
  - 通话贴近屏幕时不再显示“请勿遮挡听筒”等提示。
- **异常表现**：
  - 提示仍然出现；
  - 通话功能异常。
- **恢复步骤**：关闭开关，重启。
- **检查日志关键字**：`Diagnostic[HIDE_PROXIMITY_WARNING]`、`MiuiScreenOnProximityLock`。
- **状态**：NOT TESTED

#### B2-2. clearAllTasks

- **featureId**：`clearAllTasks`
- **中文名称**：清除所有任务
- **ProcessTarget**：`SystemServer`
- **偏好键**：`system_clearalltasks`
- **测试前置条件**：最近任务中有多个应用。
- **启用步骤**：设置 → 系统 → 开启“允许清除所有任务”。
- **需要重启的进程**：`REBOOT`
- **正常结果**：
  - 最近任务界面的“清除全部”按钮可用，点击后所有任务被清理。
- **异常表现**：
  - 最近任务无法清除；
  - 清理后应用立即重建。
- **恢复步骤**：关闭开关，重启。
- **检查日志关键字**：`Diagnostic[CLEAR_ALL_TASKS]`、`WindowProcessUtils`。
- **状态**：NOT TESTED

#### B2-3. hideDismissView

- **featureId**：`hideDismissView`
- **中文名称**：隐藏清除全部按钮
- **ProcessTarget**：`SystemUI`
- **偏好键**：`system_removedismiss`
- **测试前置条件**：通知栏有通知，底部有“清除全部”按钮。
- **启用步骤**：设置 → 通知 → 开启“隐藏清除全部按钮”。
- **需要重启的进程**：`SYSTEMUI_RESTART`
- **正常结果**：
  - 通知栏底部“清除全部”按钮消失。
- **异常表现**：
  - 按钮仍然显示；
  - 通知栏无法下拉。
- **恢复步骤**：关闭开关，重启 SystemUI。
- **检查日志关键字**：`Diagnostic[HIDE_DISMISS_VIEW]`、`MiuiNotificationPanelViewController`。
- **状态**：NOT TESTED

#### B2-4. hideLockScreenHint

- **featureId**：`hideLockScreenHint`
- **中文名称**：隐藏锁屏提示
- **ProcessTarget**：`SystemUI`
- **偏好键**：`system_hidelshint`
- **测试前置条件**：锁屏界面有“向上滑动解锁”等提示文字。
- **启用步骤**：设置 → 锁屏 → 开启“隐藏锁屏提示”。
- **需要重启的进程**：`SYSTEMUI_RESTART`
- **正常结果**：
  - 锁屏底部提示文字消失。
- **异常表现**：
  - 锁屏黑屏；
  - 文字仍显示。
- **恢复步骤**：关闭开关，重启 SystemUI。
- **检查日志关键字**：`Diagnostic[HIDE_LOCK_SCREEN_HINT]`、`KeyguardIndicationRotateTextViewController`。
- **状态**：NOT TESTED

#### B2-5. folderColumns

- **featureId**：`folderColumns`
- **中文名称**：文件夹列数
- **ProcessTarget**：`Launcher`
- **偏好键**：`launcher_folder_cols`
- **测试前置条件**：桌面有一个应用文件夹。
- **启用步骤**：设置 → 桌面 → 文件夹列数 > 1。
- **需要重启的进程**：`LAUNCHER_RESTART`
- **正常结果**：
  - 打开文件夹，内部图标按设定列数排列。
- **异常表现**：
  - 文件夹布局错乱；
  - Launcher 崩溃。
- **恢复步骤**：恢复为 1，重启 Launcher。
- **检查日志关键字**：`Diagnostic[FOLDER_COLUMNS]`、`Folder`。
- **状态**：NOT TESTED

#### B2-6. titleTopMargin

- **featureId**：`titleTopMargin`
- **中文名称**：图标标题上边距
- **ProcessTarget**：`Launcher`
- **偏好键**：`launcher_titletopmargin`
- **测试前置条件**：桌面图标标题可见。
- **启用步骤**：设置 → 桌面 → 设置标题上边距 > 0。
- **需要重启的进程**：`LAUNCHER_RESTART`
- **正常结果**：
  - 图标标题与图标之间距离增大。
- **异常表现**：
  - 标题重叠或消失；
  - Launcher 崩溃。
- **恢复步骤**：恢复为 0，重启 Launcher。
- **检查日志关键字**：`Diagnostic[TITLE_TOP_MARGIN]`、`ItemIcon`。
- **状态**：NOT TESTED

---

### 5.5 Batch-3 Features

#### B3-1. noLightUpOnCharge

- **featureId**：`noLightUpOnCharge`
- **中文名称**：充电不亮屏
- **ProcessTarget**：`SystemServer`
- **偏好键**：`system_nolightuponcharges`
- **测试前置条件**：可插电充电。
- **启用步骤**：设置 → 省电 → 设置“充电时不亮屏”为 2 或 3。
- **需要重启的进程**：`REBOOT`
- **正常结果**：
  - 插上充电器时屏幕不亮起（依选项不同，普通充电/无线充电/快充均不亮）。
- **异常表现**：
  - 充电时仍然亮屏；
  - 无法唤醒屏幕。
- **恢复步骤**：恢复为 1，重启。
- **检查日志关键字**：`Diagnostic[NO_LIGHT_UP_ON_CHARGE]`、`PowerManagerService`。
- **状态**：NOT TESTED

#### B3-2. allRotations

- **featureId**：`allRotations`
- **中文名称**：允许所有旋转
- **ProcessTarget**：`SystemServer`
- **偏好键**：`system_allrotations2`
- **测试前置条件**：自动旋转已开启。
- **启用步骤**：设置 → 显示 → 设置“旋转”为“所有方向”。
- **需要重启的进程**：`REBOOT`
- **正常结果**：
  - 桌面/应用可在 0°、90°、180°、270° 之间自由旋转。
- **异常表现**：
  - 旋转不生效；
  - 屏幕卡在倒置方向。
- **恢复步骤**：恢复为 1，重启。
- **检查日志关键字**：`Diagnostic[ALL_ROTATIONS]`、`DisplayRotation`。
- **状态**：NOT TESTED

#### B3-3. noNetworkSpeedSeparator

- **featureId**：`noNetworkSpeedSeparator`
- **中文名称**：隐藏网络速度分隔符
- **ProcessTarget**：`SystemUI`
- **偏好键**：`system_nonetspeedseparator`
- **测试前置条件**：状态栏已启用网络速度显示。
- **启用步骤**：设置 → 状态栏 → 网络速度 → 开启“隐藏分隔符”。
- **需要重启的进程**：`SYSTEMUI_RESTART`
- **正常结果**：
  - 状态栏网络速度数字与上下行箭头之间的分隔线/多余空白消失。
- **异常表现**：
  - 网络速度不显示；
  - 分隔符仍存在。
- **恢复步骤**：关闭开关，重启 SystemUI。
- **检查日志关键字**：`Diagnostic[NO_NETWORK_SPEED_SEPARATOR]`、`NetworkSpeedSplitter`。
- **状态**：NOT TESTED

#### B3-4. hideIconsClock

- **featureId**：`hideIconsClock`
- **中文名称**：隐藏状态栏图标时钟
- **ProcessTarget**：`SystemUI`
- **偏好键**：`system_statusbaricons_clock`
- **测试前置条件**：状态栏左侧显示时钟图标。
- **启用步骤**：设置 → 状态栏 → 图标 → 开启“隐藏时钟图标”。
- **需要重启的进程**：`SYSTEMUI_RESTART`
- **正常结果**：
  - 状态栏左侧时钟消失。
- **异常表现**：
  - 时钟仍然显示；
  - SystemUI 崩溃。
- **恢复步骤**：关闭开关，重启 SystemUI。
- **检查日志关键字**：`Diagnostic[HIDE_ICONS_CLOCK]`、`MiuiCollapsedStatusBarFragment`。
- **状态**：NOT TESTED

#### B3-5. noUnlockAnimation

- **featureId**：`noUnlockAnimation`
- **中文名称**：无解锁动画
- **ProcessTarget**：`Launcher`
- **偏好键**：`launcher_nounlockanim`
- **测试前置条件**：默认 `com.miui.home` 桌面。
- **启用步骤**：设置 → 桌面 → 动画 → 开启“无解锁动画”。
- **需要重启的进程**：`LAUNCHER_RESTART`
- **正常结果**：
  - 从锁屏进入桌面时没有解锁动画，直接显示桌面。
- **异常表现**：
  - 解锁动画仍存在；
  - Launcher 崩溃或黑屏。
- **恢复步骤**：关闭开关，重启 Launcher。
- **检查日志关键字**：`Diagnostic[NO_UNLOCK_ANIMATION]`、`MiuiSettingsUtils`。
- **状态**：NOT TESTED
- **备注**：`MainModule` 中仅在 `com.miui.home` 分支调用，global launcher 用户不应期待生效。

---

## 6. 测试执行顺序建议

按以下顺序执行，风险由低到高、由少到多：

1. 模块加载基线（C1 相关但先不验证功能）
2. Launcher 低风险：`noClockHide` → `noWidgetOnly` → `hideLauncherTitles` → `titleTopMargin` → `folderColumns` → `noUnlockAnimation`
3. SystemUI 低风险：`hideIconsClock` → `noMoreIcon` → `networkIndicatorWifi` → `noNetworkSpeedSeparator` → `muteVisibleNotifications` → `hideDismissView` → `hideLockScreenHint` → `statusBarClockTweak`
4. system_server 低风险：`firstVolumePress` → `screenDimTime` → `allRotations` → `noLightUpOnCharge` → `muffledVibration` → `clearAllTasks` → `autoBrightnessRange` → `hideProximityWarning`
5. 高风险/签名相关：`packagePermissions`（最后）

每次测试后恢复默认值再测下一项。

## 7. 结果统计表

| 状态 | 含义 | 当前计数 |
|------|------|----------|
| PASS | 实机验证通过 | 0 |
| FAIL | 实机验证失败 | 0 |
| BLOCKED | 设备/环境不满足 | 0 |
| NOT TESTED | 未进行实机测试 | 25 |

## 8. 安全约束

- 不自动执行 adb、logcat、Broadcast Probe、Tasker。
- 不修改 `scope.list`。
- 不恢复 ADB regression 框架。
- 不修改 LSPosed 作用域，除非按“恢复步骤”手动勾选。
- 不处理 keystore、不安装正式签名 APK。
- 不创建 PR、tag、Release，不合并 main。
