# A13 Performance Findings

> Branch: `devin/a13-memory-performance-optimization`  
> Status: `STATIC_FINDINGS`（仅记录，未修复）

P0 阶段发现的潜在性能热点。本阶段只建立测量体系，不进行优化。

## 1. 管理应用重页面

依据 `res/xml/prefs_*.xml` 中 `tv.withaibuild.customiuizer.prefs.*` 元素数量：

| 页面 | 元素数 | 场景 ID | 风险 |
|------|--------|---------|------|
| `prefs_system.xml` | 223 | `manager_heavy_page_1` | 高：设置项最多，Preference 膨胀与重复反射可能最大 |
| `prefs_controls.xml` | 58 | `manager_heavy_page_2` | 中：控件与手势设置密集 |
| `prefs_launcher.xml` | 56 | `manager_heavy_page_3` | 中：Launcher 设置密集 |
| `prefs_various.xml` | 31 | — | 低 |

影响：首次进入重页面时可能触发大量 `Preference` 创建、`XposedHelpers` 初始化与资源加载。

建议优先级：P1 在 `manager_heavy_page_1` 上验证并减少不必要的反射/临时对象。

## 2. 管理应用冷启动链

`MainActivity.onCreate` 会：

1. 注册 `XposedServiceHelper.OnServiceListener`。
2. 初始化 `MainFragment`（加载 `prefs_main.xml`）。
3. 注册 `SharedPreferences.OnSharedPreferenceChangeListener`。

如果 LSPosed 服务未绑定，会延迟等待回调，导致首次启动可能额外持有监听器。P1 需测量 `manager_cold_start` 与 `manager_home_idle` 的差值。

## 3. system_server 增量归属

`Controls.kt`、`SystemUIStatusBarHooks.kt` 等文件在 `system_server` 中注册 Hook。`system_server` 总内存不能归因给模块，但 `module_disabled` 与 `enabled_features_off` 的差值可以反映基础注入成本。当前缺少稳定采集 `system_server` 中单一模块增量的方法。

## 4. SystemUI 持久对象

`SystemUIStatusBarHooks.kt`、`SystemUIBatteryHooks.kt` 涉及自定义 View 的 `addView`/`removeView`。历史上 Issue #660 模式（`IndexOutOfBoundsException`）说明 View 生命周期与所有者管理直接影响内存与稳定性。P1 需在 `systemui_idle` 场景下比较启用/禁用状态。

## 5. Native / Graphics 内存未单独验证

现有历史任务（如 `PERF-A13-DEVICEINFO-LOW-ALLOCATION-SYSFS-SAMPLER`）主要优化 Java 侧分配。`dumpsys meminfo` 中的 `Native Heap` 和 `Graphics` 尚未成为验收指标。P1 必须将 `native_heap_kb` 与 `graphics_kb` 纳入基线比较，避免只报告 Java Heap。

## 6. 测量工具已知限制

- `loaded_class_count` 在 `a13_perf_probe.py` 中标记为 `NOT_IMPLEMENTED_IN_PROBE`。
- `binder_proxy_count` 无法从 `/proc/<pid>/status` 稳定获取。
- `gc_count` / `gc_freed_objects` / `process_restart_count` 依赖 `dumpsys procstats`，在无 root 设备上可能不可用。

这些限制不是性能问题，但需要在 P1 中决定是否增加采样方法或保持不可用标记。

# P1A 新增发现

## A13-PERF-FINDING-007 — SystemUI 强制 `Application#onCreate` Hook

- **类型**：零功能启用成本 / 启动时注册
- **位置**：`app/src/main/java/tv/withaibuild/customiuizer/installers/SystemUiInstaller.java:59-70`
- **进程**：`com.android.systemui`
- **源码证据**：`ModuleHelper.findAndHookMethod("com.android.systemui.SystemUIApplication", ..., "onCreate", ...)` 在 `SystemUiInstaller.install` 开始处无条件注册。
- **静态风险**：SystemUI 启动时即注入 `Application.onCreate` 回调；回调内初始化 `SystemUIStatusBarHooks`、注册全局偏好监听器、调用 `GlobalActions.setupStatusBar`。即使所有功能关闭，这些工作仍会发生。
- **需要的运行时证据**：`systemui_cold_start` 与 `systemui_home_idle` 在 `system_statusbaricons_clock`、`system_albumartonlock` 等功能全关时的增量 PSS。
- **是否进入 Top 10**：是（A13_HOOK_COST_MAP.md #10）
- **后续建议阶段**：P1B

## A13-PERF-FINDING-008 — Launcher 强制 `Application#attach` Hook

- **类型**：零功能启用成本 / 启动时注册
- **位置**：`app/src/main/java/tv/withaibuild/customiuizer/installers/LauncherInstaller.java:101-109`
- **进程**：`com.miui.home` / `com.mi.android.globallauncher`
- **源码证据**：`ModuleHelper.findAndHookMethod(Application.class, ..., "attach", ..., new MethodHook() { after(...) { handleLoadLauncher(lpparam); } })`
- **静态风险**：启动器启动时无条件 Hook `Application.attach`，随后进入 `handleLoadLauncher` 并读取大量 `mPrefs` 判断。所有功能关闭时仍无法避免 `attach` Hook 与入口分支。
- **需要的运行时证据**：`launcher_cold_start` 与 `launcher_home_idle` 的增量。
- **是否进入 Top 10**：是
- **后续建议阶段**：P1B

## A13-PERF-FINDING-009 — `PreferenceBootstrap` 偏好监听器无注销路径

- **类型**：生命周期释放风险
- **位置**：`app/src/main/java/tv/withaibuild/customiuizer/utils/PreferenceBootstrap.java:225-239`
- **进程**：所有被注入的 installable 进程
- **源码证据**：`remote.registerOnSharedPreferenceChangeListener(l)` 被调用，但未找到对应的 `unregisterOnSharedPreferenceChangeListener`。
- **静态风险**：每个被注入且启用偏好的进程都会长期持有 `OnSharedPreferenceChangeListener`，进程结束时由 GC 回收，但期间强持有可能增加 `binder_proxy_count`。
- **需要的运行时证据**：多进程同时运行时 `binder_proxy_count`、`gc_count` 与 `total_pss` 趋势。
- **是否进入 Top 10**：否
- **后续建议阶段**：P1B / P2

## A13-PERF-FINDING-010 — 多进程复用类 `GlobalActions` / `Controls` 跨进程加载

- **类型**：无关进程类加载 / 多进程共享
- **位置**：`mods/GlobalActions.kt`、`mods/Controls.kt`
- **进程**：`SYSTEM_SERVER`、`SYSTEM_UI`、`LAUNCHER`、`GENERIC_APP` 等
- **源码证据**：`a13_hook_cost_scan.py` installer map 显示 `GlobalActions` 被 `SettingsInstaller`、`SystemServerInstaller`、`SystemUiInstaller` 引用；`Controls` 被 6 个 installer 引用。
- **静态风险**：同一功能类在不同进程按需加载，但 `GlobalActions` 的部分行为可能只在某个进程需要；当前按 installer 分散判断，无法统一门控。
- **需要的运行时证据**：各进程启用/关闭 `GlobalActions` 相关功能时的类加载差异。
- **是否进入 Top 10**：是（A13_HOOK_COST_MAP.md #5）
- **后续建议阶段**：P1B

## A13-PERF-FINDING-011 — 热路径偏好读取与反射集中在高频回调

- **类型**：热路径分配 / 高频回调
- **位置**：A13_HOOK_COST_MAP.json 中 `preference_read_in_callback = true` 175 处，`reflection_lookup_in_callback = true` 43 处。
- **进程**：`SYSTEM_SERVER`、`SYSTEM_UI`、`LAUNCHER` 为主
- **源码证据**：Top 10 中 #1 `SystemAudioAndVolumeHooks.readSettings`、#3 `SystemUILockScreenHooks.tileHostCls` 等回调体均含 `mPrefs.get...`、`XposedHelpers.findClass`。
- **静态风险**：用户操作/系统事件触发回调时反复读取 SharedPreferences 与反射查找目标类，导致高频临时对象与 CPU 开销。
- **需要的运行时证据**：`systemui_notification_shade`、`system_server_idle`、`manager_heavy_page_1` 等场景的 systrace/CPU 样本。
- **是否进入 Top 10**：是
- **后续建议阶段**：P1B

## A13-PERF-FINDING-012 — 重复目标 Hook 组

- **类型**：重复 Hook / 重复监听
- **位置**：A13_HOOK_COST_MAP.json 中 `duplicate_target_group` 非空 159 处。
- **进程**：多个
- **源码证据**：同一目标类/方法在多个 Hook 调用或不同 feature 中被注册。
- **静态风险**：同一方法被多个回调拦截，增加 `XposedBridge` 内部回调链表长度和每次调用的分发开销。
- **需要的运行时证据**：具体重复目标在真机上的 `cpu_time_ms` 增量。
- **是否进入 Top 10**：否
- **后续建议阶段**：P1B

## A13-PERF-FINDING-013 — 管理应用重页面偏好膨胀

- **类型**：UI 加载成本
- **位置**：`res/xml/prefs_system.xml` 等
- **进程**：`tv.withaibuild.customiuizer.r13`
- **源码证据**：`prefs_system.xml` 含 223 个 `Preference` 元素；`MainFragment` 加载对应 XML。
- **静态风险**：进入重页面时创建大量 `Preference` 对象和反射初始化。
- **需要的运行时证据**：`manager_heavy_page_1` 场景的启动时间。
- **是否进入 Top 10**：否（已在 P0 发现，现补充编号）
- **后续建议阶段**：P1B

## A13-PERF-FINDING-014 — `FeatureCatalog`/`FeatureInstallRegistry` 全局单例初始化

- **类型**：类加载 / 静态初始化
- **位置**：`mods/catalog/FeatureDispatcher.kt:23-25`
- **进程**：首次调用 `FeatureDispatcher` 的进程
- **源码证据**：`init { FeatureInstallRegistry.registerAll(FeatureCatalog.specs()) }`
- **静态风险**：`FeatureDispatcher` 对象在 `SystemServerInstaller`/`SystemUiInstaller`/`LauncherInstaller` 等调用 `createRuntime` 时触发 `FeatureCatalog.specs()` 构建并注册所有 `FeatureSpec`，可能一次性加载大量 catalog 与 contract 类。
- **需要的运行时证据**：`system_server` / `SystemUI` 首次 `createRuntime` 时的类加载数量。
- **是否进入 Top 10**：否
- **后续建议阶段**：P1B / P2
