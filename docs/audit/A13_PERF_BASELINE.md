# A13 Performance Baseline

> Branch: `devin/a13-memory-performance-optimization`  
> Base commit: `5ca30911e3fc41e80bfde7ac294218e9b855b8d3`  
> Application ID: `tv.withaibuild.customiuizer.r13`  
> Status: `RUNTIME_BASELINE_PENDING_DEVICE`（工具层已完成，真机数据待回填）

本文件定义 A13 模块性能的测量对象、场景、指标、统计方法与回归门禁。P0 只建立基线和工具，不修改业务代码。

## 1. 测量对象

必须区分以下四类对象，不得把整个 SystemUI 或 `system_server` 的总内存直接算作模块内存。

| 对象 | 包名/进程 | 说明 |
|------|-----------|------|
| 管理应用 | `tv.withaibuild.customiuizer.r13` | 模块设置应用本身 |
| SystemUI | `com.android.systemui` | 状态栏、通知、控制中心等 |
| system_server | `system_server` | 系统服务进程，只记录可稳定采集的增量趋势 |
| 普通被注入应用 | 例如 `com.miui.home`、`com.android.settings` | 至少一个轻量目标和一个复杂目标 |

## 2. 模块可归因成本

模块可归因成本必须使用差值表达：

```text
模块增量成本 = 开启模块后的测量结果 - 关闭模块后的相同场景测量结果
```

所有比较必须保证：

- 同一设备、同一 ROM、同一构建类型、同一 Android 版本。
- 同一模块版本。
- 同一测试场景与操作步骤。
- 环境状态一致（屏幕、后台、温度、充电器、待机时间）。

## 3. 模块状态

测量必须覆盖三种状态，用于区分基础注入成本、功能注册成本和功能运行成本：

| 状态 | 含义 | 配置方式 |
|------|------|----------|
| `module_disabled` | 在 LSPosed 中完全禁用模块 | LSPosed 模块开关关闭 |
| `enabled_features_off` | 模块启用，但所有目标功能关闭 | 模块开关开启，应用内所有功能开关关闭 |
| `enabled_typical_features` | 模块启用，并开启典型功能组合 | 模块开关开启，打开代表性功能 |

## 4. 指标定义

### 4.1 通用指标

| 字段 | 单位 | 来源 | 不可用时的原因示例 |
|------|------|------|---------------------|
| `total_pss_kb` | KiB | `dumpsys meminfo <pid>` | 非 root / 未获取到 pid |
| `java_heap_kb` | KiB | `dumpsys meminfo <pid>` | 输出格式不匹配 |
| `native_heap_kb` | KiB | `dumpsys meminfo <pid>` | 输出格式不匹配 |
| `graphics_kb` | KiB | `dumpsys meminfo <pid>` | 平台未报告 |
| `private_dirty_kb` | KiB | `dumpsys meminfo <pid>` | 输出格式不匹配 |
| `thread_count` | 个 | `/proc/<pid>/status` | 无权限读取 / 进程已退出 |
| `cpu_time_ms` | ms | `/proc/<pid>/stat` | 无法获取 clock tick |
| `startup_total_time_ms` | ms | `am start -W` | 用于启动场景，不用于 idle |
| `gc_count` | 次 | `dumpsys procstats` / meminfo | 未授权 / 未启用 |
| `gc_freed_objects` | 个 | `dumpsys procstats` / meminfo | 未授权 / 未启用 |
| `binder_proxy_count` | 个 | `/proc/<pid>/status` | 未报告 |
| `loaded_class_count` | 个 | 待实现 | 当前标记 `NOT_IMPLEMENTED_IN_PROBE` |
| `process_restart_count` | 次 | `dumpsys procstats --hours 0 <package>` | 无权限 |

### 4.2 无法获得指标的处理

- 不允许填 0、猜测值或"好看"的样本。
- 必须在 `unavailable_reason` 中写明原因。
- 在 `notes` 中说明尝试过的命令与错误信息。

## 5. 标准测试场景

### 5.1 管理应用

| 场景 ID | 操作 | 预热/等待 | 预期状态 |
|---------|------|-----------|----------|
| `manager_cold_start` | `am start -W tv.withaibuild.customiuizer.r13/.MainActivity` | 每次前 `am force-stop` | 冷启动 |
| `manager_home_idle` | 打开应用后静置 30s | 跳过 3 次 | 前台 idle |
| `manager_heavy_page_1` | 进入 `prefs_system.xml` | 跳过 3 次 | 重页面 1 |
| `manager_heavy_page_2` | 进入 `prefs_controls.xml` | 跳过 3 次 | 重页面 2 |
| `manager_heavy_page_3` | 进入 `prefs_launcher.xml` | 跳过 3 次 | 重页面 3 |
| `manager_page_cycle_20` | 在三个重页面间切换 20 次 | 跳过 1 次 | 页面切换 |
| `manager_background` | 按 Home 后静置 30s | 跳过 3 次 | 后台 |
| `manager_exit_reclaim` | `am force-stop` 后静置 60s | 单次 | 进程退出后 |

三个重页面依据 `res/xml/prefs_*.xml` 中 `tv.withaibuild.customiuizer.prefs.*` 元素数量选择：

| 页面 | 元素数 | 场景 ID |
|------|--------|---------|
| `prefs_system.xml` | 223 | `manager_heavy_page_1` |
| `prefs_controls.xml` | 58 | `manager_heavy_page_2` |
| `prefs_launcher.xml` | 56 | `manager_heavy_page_3` |

P0 不优化页面，仅识别候选。

### 5.2 普通被注入应用

| 场景 ID | 目标 | 状态 | 操作 |
|---------|------|------|------|
| `target_light_idle` | `com.android.settings` | `module_disabled` | 静置 30s |
| `target_light_idle` | `com.android.settings` | `enabled_features_off` | 静置 30s |
| `target_light_idle` | `com.android.settings` | `enabled_typical_features` | 开启少量功能后静置 30s |
| `target_heavy_idle` | `com.miui.home` | `module_disabled` | 静置 30s |
| `target_heavy_idle` | `com.miui.home` | `enabled_features_off` | 静置 30s |
| `target_heavy_idle` | `com.miui.home` | `enabled_typical_features` | 开启 Launcher 相关功能后静置 30s |
| `target_cold_start` | `com.miui.home` | 三种状态 | 每次 `am force-stop` 后 `am start -W` |

### 5.3 SystemUI

| 场景 ID | 操作 | 重复次数 |
|---------|------|----------|
| `systemui_idle` | 静置 30s | 5 |
| `notification_shade_open_close` | 下拉并收起通知栏 10 次 | 5 |
| `quick_settings_open_close` | 打开并关闭快速设置 10 次 | 5 |
| `lock_unlock_cycle` | 锁屏并解锁 5 次 | 5 |

### 5.4 system_server

| 场景 ID | 操作 | 重复次数 |
|---------|------|----------|
| `system_server_idle` | 静置 30s | 5 |

## 6. 统计方法

- 每个正式场景至少重复 **5 次**。
- 默认使用 **中位数**，不使用最好值、最低值或主观挑选。
- 报告时必须包含 `raw_samples`。
- 预热样本不计入中位数。

## 7. 测量环境变量

每次测量必须记录：

- 是否冷启动
- 是否清除进程（`am force-stop`）
- 是否清除应用数据
- 屏幕亮灭状态
- 测试前后台状态
- 手机温度或热状态
- 是否连接充电器
- 后台应用控制方式
- ROM 版本
- 模块版本

**禁止默认清除用户数据**。需要清除数据的场景必须作为独立可选操作，由操作者显式执行。

## 8. 测量命令

### 8.1 工具层命令

```powershell
python tools/a13_perf_probe.py doctor

python tools/a13_perf_probe.py process `
  --package tv.withaibuild.customiuizer.r13 `
  --scenario manager_idle `
  --module-state enabled_features_off `
  --repeat 5 `
  --output out/a13-perf/manager-idle.json

python tools/a13_perf_probe.py startup `
  --package tv.withaibuild.customiuizer.r13 `
  --activity tv.withaibuild.customiuizer.MainActivity `
  --repeat 5 `
  --output out/a13-perf/manager-startup.json
```

### 8.2 真机操作参考

```powershell
# 1. 确认 ADB 和设备
adb devices
adb shell getprop ro.build.version.release
adb shell getprop ro.miui.ui.version.name

# 2. 采集管理应用内存
adb shell pidof tv.withaibuild.customiuizer.r13
adb shell dumpsys meminfo <pid>
adb shell cat /proc/<pid>/status
adb shell cat /proc/<pid>/stat

# 3. 采集启动时间
adb shell am force-stop tv.withaibuild.customiuizer.r13
adb shell am start -W tv.withaibuild.customiuizer.r13/.MainActivity

# 4. 模块状态切换
# module_disabled：在 LSPosed 中关闭模块
# enabled_features_off：LSPosed 中开启，应用内关闭所有功能
# enabled_typical_features：开启代表性功能组合

# 5. 使用脚本自动重复
python tools/a13_perf_probe.py process --package <package> --scenario <scenario> --module-state <state> --repeat 5 --output out/a13-perf/<file>.json
```

## 9. 数据回填流程

1. 在真机上运行 `python tools/a13_perf_probe.py ...`。
2. 将生成的 JSON 保存到 `out/a13-perf/`。
3. 将 `out/a13-perf/*.json` 中的 `records` 合并到 `docs/audit/A13_PERF_BASELINE.json`。
4. 移除 `RUNTIME_BASELINE_PENDING_DEVICE` 标记，改为 `RUNTIME_BASELINE_POPULATED`。
5. 验证中位数、raw_samples、环境变量完整。

## 10. 后续性能回归门禁

1. 每个优化必须与固定基线比较。
2. 测试设备、ROM、构建类型和场景必须一致。
3. 结果至少五次取中位数。
4. 内存收益小于 5% 且明显增加复杂度的改动原则上不接受。
5. 冷启动或关键交互回退超过 5% 时视为阻断。
6. Hook 数量、覆盖进程数量和常驻线程数量不得增加。
7. 不允许通过关闭现有功能获得优化结论。
8. 不允许只报告 APK 大小而声称运行内存下降。
9. 不允许只报告 Java Heap 而忽略 Native、Graphics 和 PSS。
10. 无法稳定复现的数据不得作为验收证据。
