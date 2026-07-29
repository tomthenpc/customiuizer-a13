# A13 / A14 当前差距基线（K10）

> 本文冻结 `customiuizer-a13` 当前源码事实，并以 A14 `r14.13.7` 仅作工程方法对照。K10 不修改运行逻辑，不把静态验证当作实机证据。

## 1. 审计边界

| 项目 | 冻结基线 |
|---|---|
| A13 仓库 | `tomthenpc/customiuizer-a13` |
| A13 分支 | `codex/r13.7-gap-baseline` |
| A13 起始提交 | `5b7a59c49cae846decfc78ad200ed1c5f4ab127d` |
| A14 仓库 | `tomthenpc/customiuizer-a14` |
| A14 发布基线 | `r14.13.7` / `0b277609a79dfd36e8c35807bf90c5c9793c71d3` |
| 目标系统 | MIUI 14 / Android 13（`minSdk=33`、`targetSdk=34`） |
| Xposed 边界 | libxposed `minApiVersion=101`、`targetApiVersion=102`、`staticScope=false` |
| applicationId | `tv.withaibuild.customiuizer.r13` |
| 当前版本 | `versionCode=123`、`versionName=r13.2.4-devin` |
| 当前工具链 | Gradle 8.9、AGP 8.7.2、Kotlin 2.0.21、`compileSdk=36` |

本轮没有修改 SDK、工具链、签名、applicationId、Hook target、资源或运行逻辑。A14 的 Android 14 类名、方法签名、Hook target、资源和 ROM 行为不属于回移目标。

## 2. 当前 A13 结构结论

### 2.1 已拆分的功能域

旧 K9 文档中“A13 尚未拆分 `System/SystemUI/Launcher`”“仍使用大型 `System.java/Launcher.java`”“全部大文件仍需迁移”的结论已经过期，不能继续作为实施依据。

- `mods/System.kt`：635 行、124 个 `@JvmStatic` 方法，全部是对领域 Hook 的一对一转发。
- `mods/SystemUI.kt`：193 行、60 个 `@JvmStatic` 方法，全部是对领域 Hook 的一对一转发。
- `mods/Launcher.kt`：160 行、50 个 `@JvmStatic` 方法，全部是对领域 Hook 的一对一转发。
- 三个 facade 共 234 个转发入口；当前 `MainModule.java` 仍通过这些 facade 调用领域实现。
- `System` 迁移审计确认：当前 124 个公开 facade 签名与迁移前 Java 基线一致，`MainModule` 的 119 个调用点均可解析。

实际功能域文件如下。

**System（17 个）**

- `SystemAudioAndVisualAndMoreHooks.kt`
- `SystemAudioAndVolumeHooks.kt`
- `SystemChargingAndWallpaperHooks.kt`
- `SystemDisplayAndWindowHooks.kt`
- `SystemFreeformAndMultiWindowHooks.kt`
- `SystemLockScreenHooks.kt`
- `SystemLockScreenMoreHooks.kt`
- `SystemNotificationAndShareHooks.kt`
- `SystemNotificationMoreHooks.kt`
- `SystemNotificationPopupsHooks.kt`
- `SystemSecurityAndSystemHooks.kt`
- `SystemSettingsAndConnectivityHooks.kt`
- `SystemSettingsMoreHooks.kt`
- `SystemShareAndOpenWithHooks.kt`
- `SystemStatusBarAndClockHooks.kt`
- `SystemStatusBarClockAndMoreHooks.kt`
- `SystemStatusBarMoreHooks.kt`

**SystemUI（7 个）**

- `SystemUIBatteryHooks.kt`
- `SystemUIControlCenterHooks.kt`
- `SystemUILockScreenHooks.kt`
- `SystemUIMonitorAndTileHooks.kt`
- `SystemUINotificationHooks.kt`
- `SystemUIScreenshotHooks.kt`
- `SystemUIStatusBarHooks.kt`

**Launcher（6 个）**

- `LauncherAnimationHooks.kt`
- `LauncherFolderHooks.kt`
- `LauncherGestureHooks.kt`
- `LauncherIconHooks.kt`
- `LauncherLayoutHooks.kt`
- `LauncherSystemHooks.kt`

### 2.2 MainModule 与剩余 Java

`MainModule.java` 仍是 libxposed 入口和有序调度边界。它当前没有直接导入领域 Hook；调用路径是：

```text
MainModule.java -> System/SystemUI/Launcher facade -> 30 个领域 Hook 文件
```

当前主源码共 117 个 Kotlin 文件、6 个 Java 文件。剩余 Java 文件及用途：

1. `MainModule.java`：libxposed 入口、进程分派和 Hook 注册顺序边界。
2. `mods/utils/HookerClassHelper.java`：libxposed `Chain`/回调 JVM 边界。
3. `mods/utils/ModuleHelper.java`：公共 Hook、偏好观察和模块工具边界。
4. `mods/utils/ResourceHooks.java`：资源替换、动态入口和 R8 敏感边界。
5. `mods/utils/XposedHelpers.java`：反射、Hook 和 additional-field 兼容层。
6. `org/apache/commons/lang3/reflect/MemberUtilsX.java`：反射兼容 shim。

这些文件不是“迁移遗漏”。在 K11–K17 中可以按根因修改 Java 实现，但不为追求 Kotlin 数量或照搬 A14 而迁移。

### 2.3 当前静态门禁、测试与高风险点

- `tools/check-invariants.py` 当前检查 117 个主源码文件，覆盖 legacy Xposed API、无 Looper Handler、直接延迟 lambda、部分 Receiver 配对、字面量 Regex split、冗余 Hook 参数数组等规则。
- 当前共有 26 个 Kotlin 测试文件、184 个 `@Test`。重点覆盖 additional fields、preference mirror、soft reboot、设备监控内容快照、Locale、搜索状态机和 System 迁移证明。
- invariant 脚本能发现直接形状，不等于证明间接调用的回调安全、owner 生命周期、屏灭停止、失败退避、Bitmap 并发或实机 Hook 可用。
- 当前高风险代码集中在：延迟回调异常边界、Receiver/Observer owner、状态栏每 2 秒监控、31 路可视化动画、主线程锁屏图处理，以及设置图标任务排队。

## 3. A13 / A14 逐项矩阵

### 3.1 Hook deferred callback 异常隔离

- **分类：** `SAFE_ANDROID13_BACKPORT`
- **A13 当前文件：** `mods/utils/ModuleHelper.java`、`Controls.kt`、`SystemLockScreenMoreHooks.kt`、`SystemStatusBarClockAndMoreHooks.kt`、`SystemStatusBarMoreHooks.kt`、`SystemUIStatusBarHooks.kt`、`Various.kt`。
- **A13 当前实现：** `MethodHook.intercept/before/after` 自身有异常边界，`ModuleHelper.guarded` 也已存在；但注册给框架后才执行的 `BroadcastReceiver.onReceive`、`Handler.handleMessage` 和部分延迟回调不在该边界内。静态检查发现仍有入口未保护，例如手电筒 Receiver、Smart Lock Receiver、时间/闹钟 Receiver、设备监控 `handleMessage` 和侧边栏 Receiver。
- **A14 对应文件：** `mods/utils/ModuleHelper.kt` 及各领域 Hook；参考提交 `9d4d1d56`、`615180ba`。
- **A14 解决的问题：** 用统一 guard 阻止延迟回调异常逃逸到 `system_server`、SystemUI 或 Launcher，并为有返回值回调提供“不消费”兜底。
- **Android 13 是否适用：** 是。异常边界与 Android 版本无关，但必须保留 A13 原 Hook target、执行顺序和返回语义。
- **是否能静态验证：** 部分可以。可检查回调入口、测试 guard 行为并扩展 invariant；不能静态证明 ROM 回调时序。
- **是否需要实机：** 是，至少覆盖 SystemUI、Launcher、锁屏、时间变化和手电筒相关路径。
- **建议处理方式：** K11 只给框架延迟回调增加入口 guard，并扩展 invariant 识别间接危险调用；不改 Hook target、priority、参数或 `Chain.proceed()` 次数。
- **风险：** 中。错误兜底可能意外消费事件、重复执行或隐藏真实功能失败。
- **回滚点：** K11 单独提交；按文件撤回新增 guard 和对应 invariant/test。

### 3.2 Receiver / Observer 注册与释放

- **分类：** `SAFE_ANDROID13_BACKPORT`
- **A13 当前文件：** `mods/utils/ModuleHelper.java`、`SystemStatusBarClockAndMoreHooks.kt`、`SystemStatusBarMoreHooks.kt`、`SystemUILockScreenHooks.kt`、`SystemUIScreenshotHooks.kt`、`GlobalActions.kt`、`utils/AudioVisualizer.kt`、`utils/BatteryIndicator.kt`。
- **A13 当前实现：** Receiver 已显式使用 exported/not-exported 标志，部分文件也有注销代码；但 `prefObservers` 是强引用集合，界面型观察者可能长期保留。多个构造 Hook 把旧 Receiver 存进“本次新实例”的 additional field，再尝试清理，这无法清理上一个实例的注册。当前没有统一的 process/owner 注册表。
- **A14 对应文件：** `mods/utils/ModuleHelper.kt`；参考提交 `46734e0b`、`23c64035`。
- **A14 解决的问题：** process-keyed 注册去重、owner 弱引用、销毁时注销，以及替换旧注册时不依赖新 Hook 实例。
- **Android 13 是否适用：** 是；使用 A13 已有 Context/Receiver API 即可，不复制 A14 Hook target。
- **是否能静态验证：** 部分可以。可验证注册表状态、幂等注销、弱 owner 清理和重复构造；真实 owner 销毁顺序需运行时验证。
- **是否需要实机：** 是，重点观察 SystemUI 重建、锁屏、截图、控制中心和偏好变化。
- **建议处理方式：** K11 建立最小统一注册表，先迁移已证实存在 owner/key 错误的点；偏好观察者改为可清理的弱 owner 或显式 token。
- **风险：** 高。错误注销会让功能静默停止，错误复用 key 会串扰不同进程或实例。
- **回滚点：** 注册表实现和每批调用点分开提交；出现回归时先回滚调用点，不扩大兼容分支。

### 3.3 Coroutine failure handler

- **分类：** `ALIGNED`
- **A13 当前文件：** 当前主源码和测试中没有 `CoroutineScope`、`SupervisorJob`、`Dispatchers` 或 `launch` 运行路径；`tools/check-invariants.py` 保留未来协程规则。
- **A13 当前实现：** 主要异步边界仍是 Handler、Runnable、Receiver、Executor 和 AsyncTask，因此当前没有未处理的 CoroutineScope 异常面。
- **A14 对应文件：** `mods/utils/ModuleHelper.kt` 的 `coroutineFailureHandler`，以及使用协程的 Controller。
- **A14 解决的问题：** `SupervisorJob` 不吞异常，统一处理 scope 中未捕获异常，避免进程级崩溃。
- **Android 13 是否适用：** 原理适用，但当前没有需要回移的 A13 协程调用点。
- **是否能静态验证：** 是，可通过搜索和 invariant 确认当前无 scope。
- **是否需要实机：** 当前项不需要；若 K16 引入协程则对应功能必须实机。
- **建议处理方式：** K10/K11 不新增空闲 scope 或抽象。未来引入第一个 CoroutineScope 时，在同一提交加入 failure handler 和测试。
- **风险：** 低；提前引入会增加无用状态和依赖面。
- **回滚点：** 当前无代码回滚；未来 handler 与首个 scope 同提交回滚。

### 3.4 反射缓存和 Hook 参数读取

- **分类：** `SAFE_ANDROID13_BACKPORT`
- **A13 当前文件：** `mods/utils/HookerClassHelper.java`、`mods/utils/XposedHelpers.java`，以及 `mods/` 下 Hook 调用点。
- **A13 当前实现：** Helper 已提供 `getArg(index)` 零复制读取，但现有调用仍约有 166 处 `getArgs()`、158 处 Kotlin `args[index]`，仅 2 处显式 `getArg`；只读调用仍可能物化数组并在 `chain.proceed(args)` 重编组。反射字段/方法缓存命中时仍会创建复合 key。
- **A14 对应文件：** `mods/utils/HookerClassHelper.kt`、`mods/utils/XposedHelpers.java`；参考提交 `eea03f36`、`25c64542`。
- **A14 解决的问题：** 单参数只读路径直接 `chain.getArg`，多参数只读使用不复制 List；字段和无参方法缓存改为分层索引，缓存命中不分配 key。
- **Android 13 是否适用：** 是；`Chain.getArg(int)` 属于 API 101，可保持最低运行边界。
- **是否能静态验证：** 是，可做调用点清单、编译、单测和分配形状审计；实际收益需测量。
- **是否需要实机：** 高风险/高频 Hook 需要，不能仅凭代码宣称性能提升。
- **建议处理方式：** K11 先处理系统进程只读参数点，按调用点证明 `proceed` 语义不变；反射缓存作为独立提交并补并发测试。
- **风险：** 中高。误判参数可写性会改变 Hook 行为；缓存同步错误会产生竞态。
- **回滚点：** 参数调用点和反射缓存分别提交，可单独回退。

### 3.5 additional instance fields

- **分类：** `ALIGNED`
- **A13 当前文件：** `mods/utils/XposedHelpers.java`、`test/.../AdditionalInstanceFieldTest.kt`。
- **A13 当前实现：** 已使用弱 identity key、`ReferenceQueue`、并发 map、null sentinel 和可复用查询 probe；不会因目标对象 `equals/hashCode` 相等或变化而串项。
- **A14 对应文件：** `mods/utils/XposedHelpers.java` 及缓存测试；参考提交 `0ea8fc92`、`2989e915`。
- **A14 解决的问题：** 修复等值但非同一对象共享字段、可变 hash 导致条目丢失、owner 无法回收及并发问题。
- **Android 13 是否适用：** 是，且 A13 已完成等价根因修复。
- **是否能静态验证：** 是；当前单测覆盖 identity、mutable hash、弱回收、并发和 probe 释放。
- **是否需要实机：** 此数据结构本身不需要新增专项实机；整体 Hook 仍受 K7 实机边界约束。
- **建议处理方式：** 保持现状，不为与 A14 文件形状一致而重写。
- **风险：** 低；无收益重构反而可能破坏弱引用和并发语义。
- **回滚点：** 不产生新改动；以当前 `main` 实现为基线。

### 3.6 preference mirror

- **分类：** `ALIGNED`
- **A13 当前文件：** `MainModule.java`、`AppHelper.kt`、`MainActivity.kt`、`PrefMap.kt` 及相应测试。
- **A13 当前实现：** 本地修改会标记 mirror dirty；服务绑定后执行全量 reconcile，并继续增量同步。空配置读取不会永久标记已加载，监听注册状态只在成功后设置。
- **A14 对应文件：** A14 的 preference mirror / service manager 路径。
- **A14 解决的问题：** 未连接服务期间的设置不再丢失，重连后补偿全量状态，注册失败可重试。
- **Android 13 是否适用：** 是，A13 已按自身 service/API 101 边界实现等价语义。
- **是否能静态验证：** 是，状态转换和失败重试有单测覆盖。
- **是否需要实机：** 是，仍需验证真实 LSPosed bind、进程重启和跨进程偏好传播。
- **建议处理方式：** 保持 A13 架构；K11–K17 不顺带替换成 A14 service 类。
- **风险：** 中；跨进程时序无法由 JVM 测试完全覆盖。
- **回滚点：** 不产生新改动；以 K8/K9 已合并实现为基线。

### 3.7 soft reboot

- **分类：** `ALIGNED`
- **A13 当前文件：** `PreferenceFragmentBase.kt`、`mods/GlobalActions.kt`、`GlobalActionsFastRebootTest.kt`。
- **A13 当前实现：** 软重启使用显式目标为 `com.android.systemui` 的有序广播，不以设置进程当前 binder 状态作为发送门槛；SystemUI 处理成功时才设置结果。
- **A14 对应文件：** A14 preference/global actions 的 soft reboot 路径。
- **A14 解决的问题：** 消除“设置应用停止等待”被误判为模块离线，以及广播被无关进程消费。
- **Android 13 是否适用：** 是，A13 已保留自身广播 action 和 SystemUI target。
- **是否能静态验证：** 是，可验证显式包、结果协议和 gate；真实重启仍非静态证据。
- **是否需要实机：** 是，需验证 MIUI 14 SystemUI 实际接收和重启。
- **建议处理方式：** 保持现状，不复制 A14 action/target。
- **风险：** 中；错误目标会使操作静默失败。
- **回滚点：** 不产生新改动；以当前 soft reboot 测试为回归门禁。

### 3.8 DeviceInfoMonitor

- **分类：** `NEEDS_DEVICE_BASELINE`
- **A13 当前文件：** `mods/SystemUIStatusBarHooks.kt`、`SystemUIStatusBarHooksDeviceMonitorTest.kt`。
- **A13 当前实现：** 电池/温度内容已按每次 tick 的最新快照生成；但 Handler 每 2 秒再次排队。屏灭时只跳过 sysfs 读取，仍唤醒和重排；连续读取失败没有退避；固定关闭两个显示项时也可能继续排队；缺少统一完整 stop，且可能重复创建 Handler。
- **A14 对应文件：** `mods/utils/DeviceInfoMonitor.kt`；参考提交 `17423649`。
- **A14 解决的问题：** 屏灭移除消息、亮屏立即恢复、失败指数退避（带上限）、内容不变时不提交 UI、stop 时释放 Handler/Receiver/Context。
- **Android 13 是否适用：** 工程方法适用；sysfs 路径、状态栏 View 和 Hook target 必须保持 A13 实现。
- **是否能静态验证：** 部分可以。调度/退避状态可用纯 JVM 测试，真实屏幕广播、文件节点和 View 生命周期不行。
- **是否需要实机：** 必须。需记录亮屏/息屏唤醒、更新恢复、失败节点、SystemUI 重建和内存。
- **建议处理方式：** K12 提取最小 controller，只有功能启用且屏幕亮时排队；失败退避；owner 销毁完整 stop。分支停止等待实机，不自动合并。
- **风险：** 高。漏恢复会导致状态栏不更新，重复调度会增加唤醒，错误 owner 会泄漏 SystemUI。
- **回滚点：** K12 独立分支和提交；保留原内联实现直至 controller 通过实机。

### 3.9 ScreenStateController

- **分类：** `NEEDS_DEVICE_BASELINE`
- **A13 当前文件：** `StepCounterController.kt`、`SystemStatusBarClockAndMoreHooks.kt`、`SystemUIStatusBarHooks.kt`、`utils/AudioVisualizer.kt`。
- **A13 当前实现：** 屏幕状态分散处理。Step Counter 长期注册 `TIME_TICK` 并强持有 View 列表；秒钟 Runnable 每秒递归排队且没有统一 screen listener；设备监控屏灭仍重排。A13 当前没有天气功能，不存在对应迁移目标。
- **A14 对应文件：** `mods/utils/ScreenStateController.kt`；参考提交 `f8cd2`。
- **A14 解决的问题：** 只在有 listener 时注册单一屏幕 Receiver，统一通知亮灭并在无 listener 时释放。
- **Android 13 是否适用：** 对 A13 已存在的秒钟、步数、设备监控适用；A14 天气路径不适用。
- **是否能静态验证：** 部分可以。listener 去重/清理和状态转换可测，ROM 屏幕事件及时序需实机。
- **是否需要实机：** 必须，与 K12 一起验证息屏、亮屏、锁屏、SystemUI 重建和耗电。
- **建议处理方式：** K12 只统一 A13 现有消费者，不新增天气、不改变时钟 Hook target。
- **风险：** 高。单例 owner 或初始状态错误会让多个功能停止或永久唤醒。
- **回滚点：** controller 和每个消费者迁移分别提交；可逐消费者退回原屏幕判断。

### 3.10 AudioVisualizer

- **分类：** `NEEDS_DEVICE_BASELINE`
- **A13 当前文件：** `utils/AudioVisualizer.kt`。
- **A13 当前实现：** 仍创建 31 个 `ValueAnimator`，每个 FFT band 独立取消/启动动画，各 animator 更新时写点并 `postInvalidate`；Palette 使用 `AsyncTask`，没有 generation/latest-wins。`updatePlaying` 会依据屏幕/可见性释放 Visualizer，因此“息屏后仍必然采样”不成立；但 detach 只清 bitmap，没有统一取消 animator/Palette/Visualizer，且绑定默认 audio session 0。
- **A14 对应文件：** `utils/AudioVisualizer.kt`；参考提交 `00bd1685`、`40ef26`、`483a6`、`243da`。
- **A14 解决的问题：** 单 Choreographer 帧调度、预计算 FFT bins、只应用最新 Palette、绑定真实媒体 session、串行 create/release、隐藏/暂停立即清空并保证跨线程可见性。
- **Android 13 是否适用：** 调度、生命周期和 latest-wins 方法适用；真实 session 获取必须按 MIUI 14 / Android 13 媒体栈重新确认。
- **是否能静态验证：** 部分可以。纯 FFT 映射、帧状态和 generation 可测；音频 session、屏幕时序、帧率与功耗不可静态证明。
- **是否需要实机：** 必须，覆盖播放/暂停/切歌/息屏/解锁/通知面板/蓝牙和 SystemUI 重建。
- **建议处理方式：** K15 先把 31 路 animator 收敛到单帧调度，再分提交处理 Palette 和 session；分支停止等待实机。
- **风险：** 很高。可造成 SystemUI 崩溃、Visualizer 资源占用、动画错误或音频行为回归。
- **回滚点：** scheduler、Palette、session 三个独立提交；任一失败只回滚对应层。

### 3.11 LockScreenAlbumArtController

- **分类：** `NEEDS_DEVICE_BASELINE`
- **A13 当前文件：** `mods/SystemUILockScreenHooks.kt`。
- **A13 当前实现：** `processAlbumArt`、blur 和 `WallpaperColors.fromBitmap` 在媒体 Hook 路径同步执行；可能分配全屏 ARGB_8888 bitmap。只按 source `sameAs` 跳过，没有有界处理缓存、请求 generation、latest-wins 或任务取消；Receiver owner 也存在新实例清理旧注册的问题。
- **A14 对应文件：** `mods/utils/LockScreenAlbumArtController.kt`；参考提交 `7892a`、`813ba`、`488187`、`064ba`。
- **A14 解决的问题：** 重处理离开主线程、限制并行度、latest-wins、降采样 blur、有界字节缓存、弱引用、配置变化失效和异步颜色提取。
- **Android 13 是否适用：** 方法适用，但 A14 锁屏类、字段、主题和目标尺寸不可复制。
- **是否能静态验证：** 部分可以。cache key/字节上限/generation 可测，真实媒体通知和锁屏 View 生命周期不行。
- **是否需要实机：** 必须，覆盖快速切歌、横竖屏、主题、模糊等级、锁屏开关、内存压力和 SystemUI 重建。
- **建议处理方式：** K16 在 A13 Hook 外建立最小 controller，先保持输出像素语义，再加入 downsample/cache；分支停止等待实机。
- **风险：** 很高。可能出现旧专辑图覆盖新图、OOM、主线程卡顿或锁屏显示错误。
- **回滚点：** controller 接入、异步化、缓存分别提交；保留同步旧路径供完整回滚。

### 3.12 AppDataAdapter

- **分类：** `SAFE_ANDROID13_BACKPORT`
- **A13 当前文件：** `utils/AppDataAdapter.kt`、其他设置列表 adapter。
- **A13 当前实现：** filtered 数据使用 `CopyOnWriteArrayList`；每个 adapter 创建 CPU 数相关的 `ThreadPoolExecutor`，队列是无界 `LinkedBlockingQueue`；`getView` 每次图标缓存未命中创建一个 `BitmapCachedLoader` AsyncTask，没有同 key in-flight 去重。
- **A14 对应文件：** `utils/AppDataAdapter.kt`；参考提交 `296ee`。
- **A14 解决的问题：** 主线程普通列表、ViewHolder/选择集合、共享有界图标执行器、相同 key 请求合并和结果 fan-out。
- **Android 13 是否适用：** 是；设置应用自身运行在 SDK 33，适配器优化不依赖 Android 14。
- **是否能静态验证：** 是，可用 adapter/loader 单测验证排序、筛选、拒绝清理、去重和 stale-view 防护。
- **是否需要实机：** 是，需检查滚动、快速搜索、多选、应用卸载/安装、低内存和日夜主题。
- **建议处理方式：** K13 先建立共享有界 loader 和 in-flight 去重，再把只在主线程更新的列表换为 `ArrayList`；不改变排序和选择语义。
- **风险：** 中。View 复用错误会显示错图，拒绝处理不完整会永久卡在 in-flight。
- **回滚点：** loader 与 adapter 容器分开提交，可恢复旧 loader。

### 3.13 Bitmap / icon loading

- **分类：** `SAFE_ANDROID13_BACKPORT`
- **A13 当前文件：** `utils/BitmapCachedLoader.kt`、`Helpers.kt`、`MainApplication.kt`。
- **A13 当前实现：** 每个 miss 使用 AsyncTask；完成后按剩余 heap 判断是否缓存，低于阈值时显式调用 `Runtime.getRuntime().gc()`；没有相同 key in-flight 去重，也没有包变更/trim 驱动的统一失效。需要纠正旧说法：现有 `Helpers` 的 bitmap `LruCache` 已按 `allocationByteCount` 以 KiB 计量，并非“不按字节限制”。
- **A14 对应文件：** `utils/AppDataAdapter.kt` 及应用缓存失效路径；参考提交 `296ee`。
- **A14 解决的问题：** 有界队列、显式拒绝清理、in-flight 合并、无主动 GC、包变化和内存压力时失效。
- **Android 13 是否适用：** 是。
- **是否能静态验证：** 是，可注入 executor/cache 并测试排队、拒绝、fan-out 和失效。
- **是否需要实机：** 是，验证真实图标、滚动复用、包变化和内存压力。
- **建议处理方式：** K13 与 AppDataAdapter 同分支，删除主动 GC；保持现有字节计量并补统一 trim/package invalidation。
- **风险：** 中。缓存失效不足会显示旧图标，过度失效会增加解码。
- **回滚点：** executor、in-flight、invalidation 各自独立提交。

### 3.14 AppLocaleController

- **分类：** `SAFE_ANDROID13_BACKPORT`
- **A13 当前文件：** `utils/AppLocaleController.kt`、`MainApplication.kt`、`AppLocaleControllerTest.kt`。
- **A13 当前实现：** Application 启动时调用 `AppCompatDelegate.setApplicationLocales`。如果此时没有 live AppCompat delegate，生产路径可能不触发期望的配置更新；测试通过 seam 验证状态，但没有证明 framework locale 已应用。
- **A14 对应文件：** `utils/AppLocaleController.kt`；参考提交 `5a9489`、`eab4`、`14faae`。
- **A14 解决的问题：** Android 13+ 直接通过 Application 的 `android.app.LocaleManager` 设置/清除 app locales，启动快速路径避免无意义重写，并修正偏好与 framework 状态对齐。
- **Android 13 是否适用：** 是。A13 `minSdk=33`，可直接使用 framework `LocaleManager`，但必须保留当前语言值和 follow-system 语义。
- **是否能静态验证：** 是，可 mock LocaleManager/状态并覆盖启动、切换、清除和重复应用。
- **是否需要实机：** 是，覆盖中文/英文/跟随系统、冷启动、旋转、返回栈、日夜主题和进程重启。
- **建议处理方式：** K14 用 framework LocaleManager 替换生产 apply 边界；不改资源、不升级 SDK、不顺带重写 Activity。
- **风险：** 中。可能导致循环重建、偏好与 framework 不一致或语言不能恢复。
- **回滚点：** Locale controller 单独提交，可恢复 AppCompatDelegate 路径。

### 3.15 MainFragment 搜索状态机

- **分类：** `ALIGNED`
- **A13 当前文件：** `MainFragment.kt`、`utils/SearchNavigation.kt`、`utils/ModSearchAdapter.kt`、`SearchStateMachineTest.kt`、`SearchRouteResolverTest.kt`。
- **A13 当前实现：** 已有显式 0/1/2 搜索状态、nullable 子路由、恢复与返回行为测试。旧 K9 所称“搜索状态机缺失”已过期。剩余性能差距在相邻的 `ModSearchAdapter`：使用 `CopyOnWriteArrayList`，每次查询/绑定重复 lowercase，并在每次查询后排序。
- **A14 对应文件：** `MainFragment.kt`、`utils/ModSearchAdapter.kt`；参考提交 `bcc6284`、`411536`。
- **A14 解决的问题：** 状态/路由一致性，以及索引建立时预计算 lowercase、主线程单次扫描和避免每次查询重排。
- **Android 13 是否适用：** 状态机已适用并完成；adapter 热点优化同样适用。
- **是否能静态验证：** 是，状态转换、恢复、路由、结果顺序和 query 变化可单测。
- **是否需要实机：** 状态机本身可静态确认；Locale 与实际 Fragment 重建仍需 K14 实机。
- **建议处理方式：** 保持状态机；K14 只优化 adapter 索引/扫描，不重写导航。
- **风险：** 低到中。排序或路由轻微变化会影响搜索结果和返回行为。
- **回滚点：** adapter 性能提交与 Locale 提交分开；保留现有状态机。

### 3.16 facade 和 MainModule dispatch

- **分类：** `SAFE_ANDROID13_BACKPORT`
- **A13 当前文件：** `MainModule.java`、`mods/System.kt`、`mods/SystemUI.kt`、`mods/Launcher.kt`、30 个领域 Hook 文件、`tools/audit-system-migration.py`。
- **A13 当前实现：** `MainModule` 通过 234 个纯一对一 facade 方法间接进入领域 Hook。facade 不读取偏好、不维护状态、不做兼容路由；它们目前主要是迁移期 ABI/验证边界。
- **A14 对应文件：** A14 `MainModule.java` 和领域 Hook；A14 `r14.13.7` 已大部分直接分派，只保留仍有真实实现的少量 facade 方法。
- **A14 解决的问题：** 消除无意义转发层，使注册入口直接指向唯一领域实现，同时保持有序调用序列。
- **Android 13 是否适用：** 是，但只能机械替换接收者类型，不能复制 A14 领域划分或 Hook target。
- **是否能静态验证：** 是。必须证明 MainModule 有序调用序列、签名解析、R8 可达性和 `Chain.proceed()` 次数不变。
- **是否需要实机：** 是，影响面覆盖全部进程；静态通过后也只能是可合并候选。
- **建议处理方式：** K17 先把 MainModule import/call 改为直接领域调用，再独立删除三个 facade；每批使用迁移审计和 Release/R8 验证。
- **风险：** 高。漏调用、重排或误解析会使功能静默缺失。
- **回滚点：** 每个 facade 单独迁移/删除；可恢复 MainModule 接收者而无需恢复领域实现。

### 3.17 Java / Kotlin 边界

- **分类：** `DO_NOT_PORT`
- **A13 当前文件：** 当前 6 个 Java 文件及其 Java/Kotlin 调用点、R8 规则和 `META-INF/xposed`。
- **A13 当前实现：** 核心 JVM/libxposed/反射/资源边界保留 Java，业务实现主体已 Kotlin 化。`MainModule.java` 和 `XposedHelpers.java` 是明确的稳定边界。
- **A14 对应文件：** A14 只剩 `MainModule.java`、`XposedHelpers.java`、`MemberUtilsX.java`，其 `ModuleHelper/HookerClassHelper/ResourceHooks` 已 Kotlin 化。
- **A14 解决的问题：** A14 自身的维护和结构一致性，不构成 A13 运行缺陷。
- **Android 13 是否适用：** 不适合作为直接回移目标。只有具体根因修复可以落在现有 Java 文件中。
- **是否能静态验证：** 可以确认 ABI/入口，但无法证明一次无收益语言迁移安全。
- **是否需要实机：** 若未来单独迁移任何核心边界，必须 Release/R8 和多进程实机。
- **建议处理方式：** K11–K17 保留全部 6 个 Java 文件；不以 Kotlin 覆盖率作为验收指标。
- **风险：** 很高。语言迁移可能改变 descriptor、静态初始化、同步、反射、R8 或 libxposed 入口。
- **回滚点：** 本轮无改动；未来必须一文件一阶段独立回滚。

### 3.18 Gradle、R8、lint 和单元测试

- **分类：** `ALIGNED`
- **A13 当前文件：** `app/build.gradle.kts`、根 `build.gradle.kts`、`gradle.properties`、`gradle/wrapper/gradle-wrapper.properties`、`proguard-rules.pro`、`META-INF/xposed/*`、`app/src/test`、`tools/check-invariants.py`。
- **A13 当前实现：** configuration cache/build cache 已启用；Release minify/resource shrink 和正式签名缺失失败边界已配置；当前 184 项单测。实时版本为 Gradle 8.9、AGP 8.7.2、Kotlin 2.0.21、`compileSdk=36`、`targetSdk=34`；Xposed 元数据为 101/102。
- **A14 对应文件：** A14 对应构建、R8、测试和 invariant 门禁。
- **A14 解决的问题：** 建立测试、lint、Debug/Release、R8、入口和静态 invariant 的持续门禁。
- **Android 13 是否适用：** 门禁方法适用；A14 工具链版本、SDK、依赖和 keep 规则不应同步复制。
- **是否能静态验证：** 是，可复现执行测试、lint、构建、APK 元数据和迁移审计。
- **是否需要实机：** 构建门禁不需要；所有运行结论仍需要 K7/各阶段实机。
- **建议处理方式：** 保持当前工具链和 API；每阶段执行针对测试、全单测、invariants、lintDebug、Debug/Release 和 R8，按风险检查 APK。
- **风险：** 低；无关升级会把工具链风险混入运行修复。
- **回滚点：** K11–K17 禁止顺带升级；任何必要构建改动单独提交。

## 4. 真实候选复核摘要

| 候选 | 当前源码事实 | 结论 |
|---|---|---|
| 延迟回调 | invariant 通过，但仍有直接或间接危险逻辑未在回调入口 guard | K11 P0 |
| Receiver/Observer | 有显式 flag 和局部 unregister；仍有新实例清理旧 receiver、强观察者集合 | K11 P1 |
| Coroutine | 当前无 CoroutineScope/launch 路径 | 不引入空抽象 |
| AudioVisualizer | 31 个 animator、每 band invalidate、AsyncTask Palette；屏灭通常会 release，但 detach 不完整 | K15，必须实机 |
| 设置列表 | CopyOnWriteArrayList、每 miss 一个 AsyncTask、无界队列、无 in-flight、主动 GC | K13 |
| Bitmap cache | 已按 allocationByteCount 计量 | 纠正旧 K9 误判 |
| Locale | Application 启动仍走 AppCompatDelegate | K14 |
| Device monitor | 屏灭跳过 I/O 但仍 2 秒重排；无失败退避和完整 stop | K12，必须实机 |
| Lock screen art | 主 Hook 路径同步 blur/colors；无 latest-wins 和有界处理缓存 | K16，必须实机 |
| facade | 234 个纯转发；System 审计已证明 124 个 facade ABI | K17 机械删除候选 |

## 5. 执行优先级

### P0

- `system_server`、SystemUI、Launcher 的框架延迟回调异常逃逸。

### P1

- Receiver/Observer owner、幂等注销和重复注册。
- DeviceInfoMonitor 屏灭停止、失败退避、完整 stop。
- ScreenStateController 对 A13 现有秒钟/步数/监控消费者的统一。
- AudioVisualizer 单帧调度与完整释放。

### P2

- 设置列表和图标加载的有界队列、in-flight 去重、无主动 GC。
- Android 13 LocaleManager。
- 搜索 adapter 分配/排序热点（保留现有状态机）。
- 锁屏专辑图异步、latest-wins、降采样和有界缓存。

### P3

- `MainModule -> facade -> domain` 纯转发删除。
- 有证据、可独立回滚的低风险结构清理。

## 6. 建议分支与合并门禁

### `codex/r13.7-callback-safety`（K11）

- **修改范围：** deferred callback guard、Receiver/Observer 最小注册表、只读 Hook 参数点、必要 invariant/test。
- **不允许修改：** Hook target/priority/order、返回语义、SDK/依赖、功能 UI。
- **测试：** guard/注册表/参数读取单测，全单测、invariants、lintDebug、Debug/Release/R8。
- **实机要求：** SystemUI、Launcher、system_server 相关功能冒烟和 LSPosed 日志。
- **合并条件：** 静态门禁全部通过后只报告“可合并候选”，等待用户决定。
- **回滚方法：** guard、注册表、参数点按独立提交回滚。

### `codex/r13.7-device-monitor-controller`（K12）

- **修改范围：** DeviceInfoMonitor、ScreenStateController、A13 现有秒钟/步数/监控消费者。
- **不允许修改：** A14 天气功能、sysfs 节点语义、状态栏 Hook target、显示格式。
- **测试：** 调度、亮灭、退避、重复 start/stop、弱 owner；全静态门禁。
- **实机要求：** 强制，含亮灭屏、SystemUI 重建、失败节点和内存/唤醒 A/B。
- **合并条件：** 停在分支，只有实机证据完整才可提出合并。
- **回滚方法：** controller 与消费者分提交，逐消费者恢复原实现。

### `codex/r13.7-settings-performance`（K13）

- **修改范围：** AppDataAdapter、Bitmap/icon loader、共享有界 executor、in-flight、cache invalidation。
- **不允许修改：** 列表排序/筛选/选择语义、应用管理 Hook、资源和主题。
- **测试：** 队列拒绝、去重/fan-out、View 复用、包失效、trim；全静态门禁。
- **实机要求：** 设置列表滚动、快速搜索、多选、包变化、低内存。
- **合并条件：** 静态通过后只报告“可合并候选”。
- **回滚方法：** loader、adapter 容器、invalidation 独立提交。

### `codex/r13.7-settings-state-machine`（K14）

- **修改范围：** AppLocaleController 的 framework LocaleManager 边界、ModSearchAdapter 预计算索引；保持 MainFragment 状态机。
- **不允许修改：** SDK/资源、导航层级、搜索排序语义、Activity 架构。
- **测试：** Locale set/clear/reconcile、冷启动、搜索状态/路由/顺序；全静态门禁。
- **实机要求：** 语言、跟随系统、旋转、日夜主题、重启和返回栈。
- **合并条件：** 静态通过后只报告“可合并候选”。
- **回滚方法：** Locale 与搜索性能分提交，各自恢复。

### `codex/r13.7-audio-visualizer`（K15）

- **修改范围：** 单帧调度、FFT 预计算、Palette latest-wins、Visualizer create/release 和 session。
- **不允许修改：** 视觉输出含义、偏好 key、媒体 Hook target、无关状态栏功能。
- **测试：** FFT/bounds、帧状态、generation、重复 start/stop、并发释放；全静态门禁。
- **实机要求：** 强制，覆盖音频、蓝牙、屏幕、面板和 SystemUI 重建，并观察内存/帧/功耗。
- **合并条件：** 停在分支，实机通过前不合并。
- **回滚方法：** scheduler、Palette、session 三层独立提交。

### `codex/r13.7-lockscreen-art`（K16）

- **修改范围：** A13 专辑图 controller、异步处理、latest-wins、降采样、字节缓存和 owner 清理。
- **不允许修改：** A14 锁屏类/字段/主题、A13 Hook target、专辑图偏好语义。
- **测试：** generation、cache key/上限/失效、取消、颜色结果；全静态门禁。
- **实机要求：** 强制，覆盖快速切歌、主题、旋转、模糊、锁屏和内存压力。
- **合并条件：** 停在分支，实机通过前不合并。
- **回滚方法：** 接入、异步化、缓存分提交，完整保留旧输出路径回退。

### `codex/r13.8-direct-domain-dispatch`（K17）

- **修改范围：** MainModule 直接导入/调用领域 Hook，最后删除三个纯 facade。
- **不允许修改：** 领域实现文本、调用顺序、偏好读取、Hook target/条件、任何运行优化。
- **测试：** 调用序列机械证明、签名/迁移审计、R8 mapping、全单测、lint、Debug/Release。
- **实机要求：** 多进程广覆盖冒烟；静态通过不能冒充实机。
- **合并条件：** 只报告“可合并候选”，等待用户决定。
- **回滚方法：** System、SystemUI、Launcher 分批提交；恢复接收者类型即可回退。

## 7. 连续执行顺序与停止规则

顺序固定为：

```text
K11 callback safety
K12 device monitor controller
K13 settings performance
K14 locale and search state
K15 audio visualizer
K16 lockscreen art
K17 facade removal
```

进入下一阶段前，前一阶段必须独立提交，单元测试、invariants、lintDebug、assembleDebug、assembleRelease 通过，且没有扩大 API 边界、修改 Hook target 或留下未解释行为变化。

K12、K15、K16 必须停在各自分支等待实机，不自动合并。K11、K13、K14、K17 即使静态通过也仅为“可合并候选”，不得直接更新 `main`。

## 8. 明确不应回移

1. A14 的 Android 14 类名、字段、Hook target、资源、Manifest、SystemUI tag 结构和版本闸门。
2. A14 的天气消费者及其他 A13 当前不存在的功能路径。
3. 为对齐文件数量而迁移 `MainModule.java`、`XposedHelpers.java` 等稳定 Java 边界。
4. A14 的 SDK、Gradle、AGP、Kotlin、依赖、签名或 keep 规则。
5. 未经 A13 控制流、R8 和实机证明的大文件复制或架构替换。

## 9. K10 验证记录

K10 只改本文档。2026-07-30 在上述冻结基线上执行结果如下：

| 验证 | 结果 |
|---|---|
| `git diff --check` | PASS |
| `python tools\check-invariants.py` | PASS；117 个主源码文件，无违规 |
| `python tools\audit-system-migration.py --baseline-ref backup/r13-k5-before-system-java-removal` | PASS；124/124 public facade 签名、119/119 `MainModule` 调用、17/17 System 领域文件、Debug/Release APK 与 R8 证据均通过 |
| `:app:testDebugUnitTest` | PASS；184 tests、0 failures、0 errors、0 skipped |
| `:app:lintDebug` | PASS |
| `:app:assembleDebug` | PASS；`CustoMIUIzer-A13-r13.2.4-devin.apk`，11,825,207 bytes，SHA-256 `413d4b519dbe3b3dbfba139c3c37dfde7df7b7b0e3914ae376e93843beccb894` |

所有结果均属于源码、JVM、lint 或构建层证据。K7 和 K12/K15/K16 的运行结论仍为：

```text
待实机验证
```
