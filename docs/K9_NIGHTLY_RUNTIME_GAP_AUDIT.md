# K9 Nightly Runtime Gap Audit

> A13 夜间运行时与 A14 r14.13.7 差距收口审计。

## 审计基线

- A13 仓库：`tomthenpc/customiuizer-a13`
- A13 分支：`devin/r13.6-runtime-backport`
- A13 审计开始 HEAD：`d628d89`（K8.3.1 完成后）
- A13 备份：`backup/r13-before-night-run-20260729`（196fa9ab）
- A14 仓库：`tomthenpc/customiuizer-a14`
- A14 运行时修复合并：`a1929885`
- A14 发布：`0b277609 (tag: r14.13.7)`

## 审计方法

1. 在 A14 worktree (`a1929885`) 解析 `CHANGELOG_EN.md`、`CHANGELOG.md`、`RUNTIME_INVARIANTS.md`。
2. 在 A14 worktree 对比 `0b277609` 与 `a1929885` 的源码差异与日志变化。
3. 在 A13 使用 A14 `tools/analyze_lsposed_log.py --profile a13` 分析本地 LSPosed 日志 (`LSPosed_log/r13/r13.2.2/.../full.log`)。
4. 检查 A13 `p0p1_candidates.tsv` / `contexts.log` 的崩溃/异常栈。
5. 对每个候选缺陷与 A14 修复逐一对照源码和调用链，按 `ALREADY_PRESENT`、`SAFE_BACKPORT`、`A13_DIFFERENT_IMPLEMENTATION`、`A14_ONLY`、`NEEDS_DEVICE_TEST`、`LOW_VALUE` 分类。

## A14 r14.13.6/r14.13.7 运行时修复清单

### 1. 设置镜像（preference mirror）与服务 bind 可靠性

**A14 修复：**
- 未连接 LSPosed 服务期间，用户修改的设置不再被静默丢弃；建立连接后做一次全量对齐。
- `onServiceBind` 不再在 `remotePrefs == null` 时直接 `return`，而是只注册增量监听、从不补偿。
- 软重启/快速重启不再依赖设置应用的 binder 绑定状态；改用有序广播，只向 `com.android.systemui` 发送。
- 区分 "we stopped waiting" 与 "proven disconnected"，超时前多一次等待。

**A13 现状与差距：**
- A13 在 `devin/r13.6-runtime-backport` 已有三条提交：`4c4cf1b` / `93b4eb6` / `7152991` 修复 preference mirror 与软重启。
- K8.3.1 进一步修正了 device monitor 的快照行为。
- 代码路径：`MainModule.java` / `XposedProvider.kt` / `PrefMap.kt`。
- 基本语义已与 A14 对齐，但 `XposedProvider.kt` 中 `onServiceBind` 的具体逻辑仍有差异，需要源码级对照确认。

**分类：** `SAFE_BACKPORT`（确认 `onServiceBind` 与 `remotePrefs` 空处理，补充缺失的边界）。

### 2. `PrefMap.getStringAsInt()` 不再抛异常

**A14 修复：**
- 存储类型变化可能抛 `ClassCastException`；非法字符串抛 `NumberFormatException`；调用点在 SystemUI / system_server hook 启动路径，已统一回退到 `defValue`。

**A13 现状与差距：**
- `PrefMap.kt` 已修复，返回默认值且缓存失败结果（`getStringAsInt`）。
- 已补充 `PrefMapTest.kt` 覆盖。

**分类：** `ALREADY_PRESENT`。

### 3. 设备监控热更新与内容依赖

**A14 修复：**
- 状态栏电池/温度格式与单位无需重启 SystemUI 即可生效；ticker 每次使用最新快照。
- 图标槽位（主开关、左右侧）固定，不在无槽位时动态启用。

**A13 现状与差距：**
- K8.3.1 已重构 `SystemUIStatusBarHooks.kt`：`DeviceMonitorSnapshot` 移除主开关，`buildDeviceInfo` 支持模式 1/2/3，非法值回退到 1。
- 已添加 `SystemUIStatusBarHooksDeviceMonitorTest`。

**分类：** `ALREADY_PRESENT`。

### 4. system_server / SystemUI Hook 回调未加 try/catch（23 处加固）

**A14 修复：**
- `MethodHook` 回调内的异常会导致 system_server 重启；A14 对 23 个站点加了 `ModuleHelper.guarded { }` 或改用 `XposedInterface.Chain.intercept` 并在 `catch` 后 `chain.proceed()`。

**A13 现状与差距：**
- `ModuleHelper.guarded(...)` 已存在，但调用不统一；部分 `before`/`after` 仍直接执行可能抛异常的逻辑。
- LSPosed 日志 `r13.2.2` 出现 P0：
  - `PackageManagerServiceImpl.canBeDisabled` hook（`Various.kt`）触发异常，栈顶为 `HookerClassHelper$MethodHook.intercept`。
  - 该异常出现在 `system_server` 进程，若不吞掉可能导致 watchdog/重启。

**分类：** `SAFE_BACKPORT`（加固 `AppsDisableServiceHook` 等 system_server/systemui 高频 hook）。

### 5. 注册泄漏与 `TIME_TICK` 等 receiver

**A14 修复：**
- 清理逻辑以 hook 实例为 key（每次新实例，导致清理永远没跑）。
- 一个泄漏的 receiver 每分钟监听 `TIME_TICK`，造成无效唤醒。

**A13 现状与差距：**
- 尚未审计全部 receiver 生命周期；`SystemClockHooks` / `SystemNotificationHooks` 等存在 `BroadcastReceiver` 注册。
- 需要逐一对照 `registerReceiver` / `unregisterReceiver` 的 key 与清理路径。

**分类：** `NEEDS_DEVICE_TEST`（静态可发现风险，但需实机确认具体注册点）。

### 6. 实例附加字段以 `equals` 为 key

**A14 修复：**
- `setAdditionalInstanceField` 使用 `WeakHashMap`/`IdentityHashMap`，两个 `equals` 相等但非同一对象共享一个 map，修改 hash 依赖的字段会导致条目永久丢失。
- 改为弱引用并按 identity 比较。

**A13 现状与差距：**
- `XposedHelpers.java` 仍使用 `HashMap<AdditionalFieldKey, Object>`，`AdditionalFieldKey` 由 `hashCode` / `equals` 决定。
- 若 hook 的目标对象重写了 `equals` / `hashCode`（如 `Notification`/`UserHandle`），存在相同的 key 冲突风险。

**分类：** `SAFE_BACKPORT`（替换为 `IdentityHashMap` 或 weak identity keys，风险低、可静态验证）。

### 7. 性能热点

**A14 修复：**
- 117 个只读 hook 不再拷贝参数数组并重 marshalling。
- 反射缓存命中不再分配（616 个字段查找，137 个无参方法查找）。
- 主屏搜索单次无分配扫描，排序只在构建索引时执行一次。

**A13 现状与差距：**
- `HookerClassHelper.BeforeHookCallback.getArgs()` 仍会把 `List` 转为数组并触发 R8/xposed 重 marshalling。
- `XposedHelpers.java` 的反射缓存使用 `HashMap` 且无 `synchronized`，且缓存命中会 new 对象？需要看实现。
- `MainFragment` 搜索使用 `toRegex()` / `split` 等临时对象，存在热点，但属于 P3/P4。

**分类：** `SAFE_BACKPORT`（`getArgs()` 只读路径避免物化数组；反射缓存避免分配）；部分为 `P3` 搜索性能可在后续批次处理。

### 8. 大文件拆分（A14 专有）

**A14 修复：**
- `System.kt` 4898 -> 593 行，`SystemUI.kt` 3682 -> 205，`Launcher.kt` 2960 -> 405，拆分为 18 个域文件。

**A13 现状与差距：**
- A13 仍使用大型 Java/Kotlin 文件：`System.kt`、`SystemUI.kt`、`Launcher.java`、`Various.kt`。
- 这是结构性重构，不属于运行时修复，且会破坏 `MainModule` 调用序列，风险高。

**分类：** `A14_ONLY`（不直接复制拆分结构）。

### 9. Lock screen editor 并发与缓存 / 图标加载队列

**A14 修复（根据 CHANGELOG 中文摘要）：**
- 单例调度器第一句 `withContext(Dispatchers.Default)` 把工作交给无界线程池，快速切换时可能并行生成多张 ARGB 8888 全屏图；缓存按张数计数、blur key 用 identity hash，导致永远不可能命中。
- 改为逐项校验、按 `allocationByteCount` 限额、按源图与真实参数建 key；不支持主题/目标尺寸变化时释放缓存。
- 图标加载队列 `DiscardOldestPolicy` 会丢弃已入队任务但对应图标记录不释放，导致后续每个加载者判定 "已有人在加载" 后直接返回；改为 `AbortPolicy` 并在提交处显式处理拒绝。

**A13 现状与差距：**
- A13 存在 `ResourceHooks.kt` / `SystemLockScreenMoreHooks.kt` / `SystemDisplayAndWindowHooks.kt` 等涉及 Bitmap、Drawable、Executor 的代码。
- `LauncherIconHooks` 等图标加载使用 `ThreadPoolExecutor`？需要源码对照。

**分类：** `SAFE_BACKPORT`（若 A13 使用相同 Executor policy 则替换为 `AbortPolicy` 并释放 placeholder）；`NEEDS_DEVICE_TEST`（blur 缓存并发需要实机确认路径）。

### 10. Search / Locale 状态机

**A14 修复：**
- 搜索状态机 `0/1/2` 显式化，`ModData.sub` nullable，`MainFragment.openModCat()` 返回一致。
- Locale 直接调用 `android.app.LocaleManager`。

**A13 现状与差距：**
- A13 `MainFragment.kt` 仍使用 Java 迁移来的 Kotlin，存在 `toRegex()` 和 `Consumer.forEach`。
- `AppHelper.kt` / `AppLocaleController` 可能存在与 A14 类似的 `AppCompatDelegate.setApplicationLocales()` no-op 问题。

**分类：** `A13_DIFFERENT_IMPLEMENTATION`（需在 A13 实体验证后再移植）。

## A13 LSPosed 日志 P0/P1 候选

分析日志：`LSPosed_log/r13/r13.2.2/LSPosed_2026-07-27T10_23_21.929827/full.log`（32.7 MB）。

| P | 数量 | 类型 | 描述 | 模块相关 | 归类 |
|---|---|---|---|---|---|
| P0 | 1 | `System.err` 栈异常 | `PackageManagerServiceImpl.canBeDisabled` hook 抛出，栈顶穿过 `HookerClassHelper$MethodHook.intercept` | 是 | SAFE_BACKPORT |
| P1 | 1 | `ClassNotFoundError` | `DATETIME NUM NUM W System : ClassLoader referenced unknown path:` | 否（外部反诈/系统） | A14_ONLY / 外部噪音 |
| P1 | 17 | `IllegalArgumentException` | `Parcel.createExceptionOrNull` | 否 | 外部 ROM 行为 |
| P1 | 8 | `ClassCastException` | `eb.y.i` 等系统/第三方代码 | 否 | 外部 ROM 行为 |
| P2 | 多个 | Hook 失败 | `MiuiSavedAccessPointsWifiSettings` / `SavedAccessPointPreference` 的 `onBindViewHolder` 找不到 | 是 | SAFE_BACKPORT / NEEDS_DEVICE_TEST |

## 固定与动态配置最终边界（K8.3.1 复盘点）

- **固定（需 SystemUI 重启）：** 主开关、左右侧槽位、字体、边距、加粗、固定宽度等 `initStatusbarTextIcon` 参数。
- **动态（每 tick 快照）：** `batteryContentOpt`、`batteryTempDecimal`、`batteryFixCurrentRatio`、`batteryPositive`、`batterySingleRow`、`batteryReverseOrder`、`batteryHideUnit`、`deviceTempContentOpt`、`deviceTempHideUnit`、`deviceTempSingleRow`、`deviceTempReverseOrder`、`batteryInCharge`。
- **三种温度模式文件读取：**
  - 模式 1：`battery/uevent` + `thermal/thermal_zone0/temp`。
  - 模式 2：仅 `battery/uevent`。
  - 模式 3：仅 `thermal/thermal_zone0/temp`。

## Java 文件剩余清单（粗略）

`mods/utils/XposedHelpers.java`、`mods/utils/ModuleHelper.java`、`mods/utils/HookerClassHelper.java`、`mods/utils/ResourceHooks.java`、`mods/System.java`、`mods/Launcher.java`、`MainModule.java`、`org/apache/commons/lang3/reflect/MemberUtilsX.java` 等。

按用户规则中 `KEEP_JAVA` / 高风险边界分类，本次不强行迁移大型 Hook 文件。

## 结论

- K8.3.1 已完成，A13 设备监控内容依赖与固定开关边界已修正。
- A14 与 A13 主要运行时差距集中在：
  1. `AppsDisableServiceHook` 等 `system_server` hook 异常直接上抛（P0）。
  2. `XposedHelpers` 附加字段 key 冲突风险（P2 转 P1 可能）。
  3. 反射缓存与 hook 参数只读热点分配。
  4. 图标加载队列 `DiscardOldestPolicy` 与资源泄漏（NEEDS_DEVICE_TEST）。
  5. 锁屏 editor 并发缓存（NEEDS_DEVICE_TEST / A14_ONLY 依赖 Android 14 模糊路径）。
- 建议下一阶段先处理 `SAFE_BACKPORT` 中可静态验证、不需要实机的项。
