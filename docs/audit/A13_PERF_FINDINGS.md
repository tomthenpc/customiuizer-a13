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
