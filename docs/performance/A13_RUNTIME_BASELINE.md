# A13 Runtime Baseline Preparation

> Branch: `devin/a13-memory-performance-optimization`
> Application ID: `tv.withaibuild.customiuizer.r13`
> Status: `READY_FOR_DEVICE_EXECUTION`（工具层）/ `BLOCKED_DEVICE_NOT_CONNECTED`（执行层）
> Related: `docs/audit/A13_PERF_BASELINE.md`（通用测量框架）、`tools/a13_perf_probe.py`（基础采集）、`tools/a13_runtime_baseline.py`（场景编排 R1）

> **重要：P0 tooling != runtime evidence。** 工具准备完成不代表运行时基线已建立。只有真实 A13 设备完成采集后才能得出性能结论。

本文件定义 A13 优化前/阶段性真实设备基线的采集方案，聚焦 P1B-2（AudioService hot-path）和 P1B-4A（Notification Menu hot-path）相关的 system_server 与 SystemUI 指标。本阶段只准备采集方案和工具，不修改任何生产代码。

## 1. 采集目标

| 进程 | 包名/标识 | 关注指标 | 关联优化 |
|------|-----------|----------|----------|
| system_server | `system_server` | PSS、Private Dirty、CPU time | P1B-2 `VolumeStepsHook`、`NotificationVolumeServiceHook` |
| SystemUI | `com.android.systemui` | PSS、Java heap、Native heap、Threads | P1B-4A `MiuiNotificationMenuRow#createMenuViews` |

## 2. 采集指标定义

### 2.1 system_server

| 指标 | 单位 | adb 命令 | 说明 |
|------|------|----------|------|
| `total_pss_kb` | KiB | `adb shell dumpsys meminfo <pid>` | 总 PSS，反映 system_server 内存占用 |
| `private_dirty_kb` | KiB | `adb shell dumpsys meminfo <pid>` | 私有脏页，模块注入后的增量主要体现于此 |
| `cpu_time_ms` | ms | `adb shell cat /proc/<pid>/stat` | 累计 CPU 时间，反映 hot-path 回调开销 |

### 2.2 SystemUI

| 指标 | 单位 | adb 命令 | 说明 |
|------|------|----------|------|
| `total_pss_kb` | KiB | `adb shell dumpsys meminfo <pid>` | 总 PSS |
| `java_heap_kb` | KiB | `adb shell dumpsys meminfo <pid>` | Java 堆，通知菜单 View 分配主要在此 |
| `native_heap_kb` | KiB | `adb shell dumpsys meminfo <pid>` | Native 堆 |
| `thread_count` | 个 | `adb shell cat /proc/<pid>/status` 或 `/proc/<pid>/stat` | 线程数 |

### 2.3 不可用指标处理

- 不允许填 0、猜测值或主观挑选样本。
- 必须在 `unavailable_reason` 字段写明原因。
- 在 `notes` 中记录尝试过的命令与错误信息。

## 3. 模块状态

每次采集必须覆盖三种状态，用于区分基础注入成本、功能注册成本和功能运行成本：

| 状态 | 含义 | 配置方式 |
|------|------|----------|
| `module_disabled` | LSPosed 中完全禁用模块 | LSPosed 模块开关关闭 |
| `enabled_features_off` | 模块启用，所有功能关闭 | 模块开关开启，应用内所有功能开关关闭 |
| `enabled_typical_features` | 模块启用，开启典型功能组合 | 开启 `system_volumesteps=150`、`system_separatevolume=true`、通知菜单相关功能 |

## 4. 应用场景

### 4.1 场景列表

| 场景 ID | 操作 | 目标进程 | 重复次数 | 预热 | 关联 |
|---------|------|----------|----------|------|------|
| `boot_stable` | 开机到桌面后静置 5 分钟 | system_server, SystemUI | 1 | 300s | 全部 |
| `notification_panel_open_close` | 下拉并收起通知栏 10 次 | SystemUI, system_server | 5 | 10s | P1B-4A |
| `volume_adjust_10` | 音量上 5 次 + 音量下 5 次 | system_server | 5 | 10s | P1B-2 |
| `qs_panel_expand_collapse` | 展开 QS 面板并收起 10 次 | SystemUI, system_server | 5 | 10s | P1B-4A 间接 |
| `notification_menu_create_delete` | 每轮迭代：post 唯一 tag 通知→verify→open shade→long-press→BACK close→swipe-dismiss→close shade。重复 10 次/轮 × 5 轮 | SystemUI, system_server | 5 | 10s | P1B-4A |

### 4.2 场景详细操作

#### boot_stable

> 脚本本身 **不会** 执行 `adb reboot` 或修改启动流程。操作者必须先完成模块状态设置并手动重启设备。

1. 操作者先设置模块状态并重启设备。
2. 等待开机完成并解锁到桌面。
3. 运行脚本；脚本会先验证：
   - `sys.boot_completed == 1`
   - `/proc/sys/kernel/random/boot_id`
   - `/proc/uptime`
4. 如果 `sys.boot_completed != 1`，标记 `BOOT_RECENCY_UNVERIFIED`。
5. 如果 uptime 超出约定窗口，标记 `BOOT_RECENCY_UNVERIFIED`。
6. 静置 5 分钟（由脚本执行 sleep），然后采集 system_server 和 SystemUI 的单次样本。

#### notification_panel_open_close

1. 预热 10 秒。
2. 采集 pre-sample。
3. 执行 10 次：`input swipe 540 0 540 800 300`（下拉打开），等待 1.5s，`input swipe 540 800 540 0 300`（上滑关闭），等待 1.5s。
4. 等待 2 秒稳定。
5. 采集 post-sample。

#### volume_adjust_10

1. 预热 10 秒。
2. 采集 pre-sample。
3. 执行 5 次 `input keyevent 24`（音量上），每次间隔 0.5s。
4. 执行 5 次 `input keyevent 25`（音量下），每次间隔 0.5s。
5. 等待 2 秒稳定。
6. 采集 post-sample。

#### qs_panel_expand_collapse

1. 预热 10 秒。
2. 采集 pre-sample。
3. 执行 10 次：下拉打开通知面板（`input swipe 540 0 540 800 300`），等待 1s，再次下拉展开 QS（`input swipe 540 200 540 800 300`），等待 1.5s，上滑关闭（`input swipe 540 800 540 0 300`），等待 1.5s。
4. 等待 2 秒稳定。
5. 采集 post-sample。

#### notification_menu_create_delete

每轮迭代执行以下操作，每轮 10 次迭代，共 5 轮：

1. 预热 10 秒。
2. 采集 pre-sample。
3. 每次迭代：
   - 使用 `cmd notification post` 发送测试通知，tag 格式：
     `customiuizer_a13_baseline_<run_id>_r<rep>_i<iteration>`
   - 检查 adb return code。
   - 调用 `_verify_notification_exists()` 验证当前 tag 存在。
   - 下拉打开通知面板。
   - 长按通知区域触发菜单（`LONG_PRESS_NOTIFICATION_ROW`）。
   - uiautomator dump BEFORE / AFTER 保存到 artifacts。
   - 使用 `KEYCODE_BACK` 关闭菜单（`menu_close_method = KEYCODE_BACK`）。
   - 在通知面板打开时 swipe-dismiss 目标通知。
   - （可选）验证通知已消失。
   - 上滑关闭面板。
4. 等待 2 秒稳定。
5. 采集 post-sample。

> **注意**：`notification_menu_create_delete` 使用 `NOTIFICATION_SOURCE = SHELL_TEST_NOTIFICATION`，不代表普通三方 app 通知。菜单触发证据标记为 `UNVERIFIED_COORDINATE_ACTION`（尚未在真实 xaga/MIUI14/HyperOS1 上完成 ROM-specific verifier），只能作为 `notification long-press workload`，不能证明 `MiuiNotificationMenuRow#createMenuViews` 必然执行。

## 5. 采集方案

### 5.1 工具

| 工具 | 用途 |
|------|------|
| `tools/a13_perf_probe.py` | 基础进程采样（meminfo、proc、startup） |
| `tools/a13_runtime_baseline.py` | 场景编排（pre-sample → 执行 adb input 操作 → post-sample） |

### 5.2 数据格式

每次场景运行生成一个 JSON 文件，结构如下：

```json
[
  {
    "scenario_id": "volume_adjust_10",
    "description": "...",
    "module_state": "enabled_features_off",
    "device": "...",
    "rom": "...",
    "android_version": "13",
    "build_variant": "...",
    "measurement_time": "2026-08-07T...",
    "repetitions": 5,
    "warmup_s": 10.0,
    "aggregation_method": "median",
    "target_processes": ["system_server"],
    "median_pre": {
      "system_server": {
        "total_pss_kb": {"unit": "KiB", "source": "...", "value": 123456, "unavailable_reason": ""},
        "private_dirty_kb": {"unit": "KiB", "source": "...", "value": 50000, "unavailable_reason": ""},
        "cpu_time_ms": {"unit": "ms", "source": "...", "value": 120000, "unavailable_reason": ""}
      }
    },
    "median_post": { "system_server": { "..." } },
    "delta": {
      "system_server": {
        "total_pss_kb": {"unit": "KiB", "pre": 123456, "post": 123500, "delta": 44},
        "private_dirty_kb": {"unit": "KiB", "pre": 50000, "post": 50010, "delta": 10},
        "cpu_time_ms": {"unit": "ms", "pre": 120000, "post": 120500, "delta": 500}
      }
    },
    "raw_repetitions": [ { "..." } ],
    "notes": "aggregated 5 repetitions"
  }
]
```

### 5.3 输出路径

```
artifacts/runtime-baseline/
└── <run_id>_<scenario>/
    ├── manifest.json
    ├── samples.json
    ├── raw/
    │   ├── 001_PRE_system_server_meminfo.txt
    │   ├── 001_PRE_system_server_proc_stat.txt
    │   ├── 001_PRE_system_server_proc_status.txt
    │   ├── 002_POST_systemui_meminfo.txt
    │   └── ...
    └── uiautomator/
        ├── r0_i2_BEFORE.xml
        ├── r0_i2_AFTER.xml
        └── ...
```

## 6. adb 命令参考

### 6.1 环境检查

```powershell
# 确认 ADB 和设备
adb devices
adb shell getprop ro.build.version.release
adb shell getprop ro.miui.ui.version.name
adb shell getprop ro.product.device

# 确认 system_server 和 SystemUI 进程
adb shell pidof system_server
adb shell pidof com.android.systemui
```

### 6.2 手动采集 system_server

```powershell
# 获取 PID
adb shell pidof system_server

# PSS / Private Dirty
adb shell dumpsys meminfo <pid>

# CPU time
adb shell cat /proc/<pid>/stat
# 或使用工具解析
adb shell getconf CLK_TCK
```

### 6.3 手动采集 SystemUI

```powershell
# 获取 PID
adb shell pidof com.android.systemui

# PSS / Java heap / Native heap
adb shell dumpsys meminfo <pid>

# 线程数
adb shell cat /proc/<pid>/status | findstr Threads
# 或
adb shell cat /proc/<pid>/stat
```

### 6.4 场景操作命令

坐标由脚本根据 `wm size` 自动计算或 fallback 到 1080p portrait。非 1080p 设备标记 `NON_1080P_REQUIRES_CALIBRATION`。

```powershell
# 下拉通知面板
adb shell input swipe <center_x> 0 <center_x> <height*0.67> 300

# 上滑关闭通知面板
adb shell input swipe <center_x> <height*0.67> <center_x> 0 300

# 展开 QS 面板（第二次下拉）
adb shell input swipe <center_x> <height*0.08> <center_x> <height*0.67> 300

# 音量上
adb shell input keyevent 24

# 音量下
adb shell input keyevent 25

# 长按通知区域（通知行大约在高度 25% 处）
adb shell input swipe <center_x> <height*0.25> <center_x> <height*0.25> 800

# 关闭通知菜单（优先）
adb shell input keyevent 4

# 在 shade 打开时 swipe-dismiss 目标通知
adb shell input swipe <center_x> <height*0.25> <width*0.1> <height*0.25> 300
```

### 6.5 使用编排脚本

```powershell
# 检查设备和 ADB
python tools/a13_runtime_baseline.py doctor

# 运行所有场景（enabled_features_off 状态）
python tools/a13_runtime_baseline.py run --scenario all --module-state enabled_features_off

# 只运行 volume_adjust_10 场景，重复 10 次
python tools/a13_runtime_baseline.py run --scenario volume_adjust_10 --module-state enabled_typical_features --repeat 10

# 只运行 boot_stable
python tools/a13_runtime_baseline.py run --scenario boot_stable --module-state module_disabled
```

## 7. 采集流程

### 7.1 准备

1. 连接 A13 设备，启用 USB 调试。
2. 运行 `python tools/a13_runtime_baseline.py doctor` 确认 ADB 和设备。
3. 安装当前模块 APK（`.\gradlew.bat :app:assembleDebug` 产物）。
4. 在 LSPosed 中启用模块，选择目标进程（system_server、SystemUI、Settings 等）。
5. 重启设备使模块生效。

### 7.2 采集（每种模块状态）

对 `module_disabled`、`enabled_features_off`、`enabled_typical_features` 三种状态分别执行：

1. 配置模块状态（LSPosed 开关 + 应用内功能开关）。
2. 重启设备。
3. 等待开机到桌面。
4. 运行场景：

```powershell
python tools/a13_runtime_baseline.py run --scenario all --module-state <state>
```

5. 将生成的 `artifacts/runtime-baseline/<run_id>_<scenario>/` 目录保留。

### 7.3 回填

1. 将 `artifacts/runtime-baseline/<run_id>_<scenario>/manifest.json` 和 `samples.json` 合并到 `docs/audit/A13_PERF_BASELINE.json`。
2. 更新 `A13_PERF_BASELINE.md` 中的 `RUNTIME_BASELINE_PENDING_DEVICE` 标记为 `RUNTIME_BASELINE_POPULATED`。
3. 验证中位数、raw_samples、环境变量完整。

## 8. 环境变量记录

每次采集必须记录：

- 设备型号（`ro.product.device`）
- ROM 版本（`ro.miui.ui.version.name`）
- Android 版本（`ro.build.version.release`）
- 构建类型（`ro.build.type`）
- 模块版本（当前 commit SHA）
- 模块状态（`module_disabled` / `enabled_features_off` / `enabled_typical_features`）
- 屏幕亮灭状态
- 是否连接充电器
- 后台应用状态
- 采集时间（UTC ISO 8601）

## 9. 统计方法

- 每个场景至少重复 5 次（`boot_stable` 除外，仅 1 次）。
- 默认使用中位数，不使用最好值或主观挑选。
- 报告必须包含 `raw_repetitions` 原始数据。
- 预热样本不计入中位数。
- delta = median_post - median_pre。

## 10. 回归门禁

1. 每个优化阶段必须与固定基线比较。
2. 测试设备、ROM、构建类型和场景必须一致。
3. 结果至少 5 次取中位数。
4. 内存收益小于 5% 且明显增加复杂度的改动原则上不接受。
5. CPU time 增量超过 5% 时视为阻断。
6. Hook 数量、覆盖进程数量和常驻线程数量不得增加。
7. 不允许通过关闭现有功能获得优化结论。
8. 无法稳定复现的数据不得作为验收证据。

## 11. 状态

```text
P0_TOOLING = READY_FOR_DEVICE_SMOKE_TEST
P0_EXECUTION = BLOCKED_DEVICE_NOT_CONNECTED
P0_DEVICE_EVIDENCE = RUNTIME_BASELINE_PENDING_DEVICE
```

- 工具层已完成 R2 修正。
- `notification_menu_create_delete` 的 ROM UI 自动化尚未在真实 A13/xaga 设备上验证。
- 真实 A13 设备完成至少一次 tooling smoke test（post verified、long-press executed、artifacts written、PID logic correct、command statuses correct）后，才允许升级为 `READY_FOR_DEVICE_EXECUTION`。
- 执行层因无设备连接而阻塞。
- 真机数据回填后改为 `DEVICE_EVIDENCE_COLLECTED`。
- P0 真实运行时基线状态：`RUNTIME_BASELINE_PENDING_DEVICE`。
- **禁止在无真机数据时声称任何运行时性能结论。**

## 12. R2 修正记录

以下修正已应用于 `tools/a13_runtime_baseline.py`，不涉及任何生产代码：

1. **`cmd notification remove` 已删除**：AOSP Android 13 不保证该子命令存在。清理改为 swipe-dismiss。
2. **通知 capability probe**：运行前探测 `cmd notification` 可用子命令（post/list/remove）。
3. **通知 post 验证**：post 后通过 `cmd notification list` 或 `dumpsys notification` 验证通知存在，验证失败标记 `FAILED_PRECONDITION`。
4. **通知来源标记**：`NOTIFICATION_SOURCE = SHELL_TEST_NOTIFICATION`，不写 `REAL_APP_NOTIFICATION`。
5. **菜单触发方式记录**：`menu_trigger_method = LONG_PRESS_NOTIFICATION_ROW`，`ui_automation_confidence = COORDINATE_BASED`。
6. **Raw artifact 分层存储**：raw dumpsys/proc 输出存入 `artifacts/runtime-baseline/<run_id>/raw/`，不内嵌 JSON。
7. **PID 稳定性检查**：PRE/POST 之间 PID 变化标记 `pid_changed = true`，不计算 delta。
8. **meminfo parse-fail-visible**：解析失败返回 `null`，不返回 `0`。
9. **CLK_TCK 记录**：manifest 中记录 `clk_tck` 值。
10. **module_state_source**：`OPERATOR_DECLARED`，不暗示脚本验证了实际模块状态。
11. **doctor 15 项检查**：设备、ROM、PID、meminfo、proc、通知能力、display、input 等。
12. **所有 adb command 检查 return code**：关键命令非 0 标记 scenario fail。

## 13. R2 额外修正记录

1. **通知 scenario 控制流修正**：每轮迭代先 post 唯一 tag 通知，再验证该 tag，然后 open shade、long-press、BACK close、swipe-dismiss、close shade。禁止在 post 前验证通知。
2. **唯一 tag 策略**：tag 格式 `customiuizer_a13_baseline_<run_id>_r<rep>_i<iteration>`，不同 repetition/iteration 不重复。
3. **Cleanup 顺序修正**：先关闭菜单（BACK），再 swipe-dismiss 通知（shade 仍打开），最后 close shade。不使用 `cmd notification remove`。
4. **菜单触发证据**：uiautomator dump BEFORE/AFTER long-press，保存 UI hierarchy 到 `uiautomator/`。菜单触发证据标记为 `UNVERIFIED_COORDINATE_ACTION`，不能证明 `createMenuViews` 必然执行。
5. **菜单关闭方法**：`menu_close_method = KEYCODE_BACK`，不使用任意坐标 tap。
6. **PID pair 有效性**：`pre_pid != null && post_pid != null && pre_pid == post_pid` 才为有效 pair。PID 变化或缺失的 pair 不参与 aggregate。
7. **有效 pair 聚合**：仅 valid pair 进入 `median_pre`/`median_post`/`delta`。无效 repetition 不得污染 aggregate。
8. **CLI 退出码**：`0=OK`、`1=DEVICE`、 `2=CLI`、`3=SCENARIO`。
9. **端到端 mock runner tests**：实际调用 `_run_scenario`/`cmd_run`/`_build_valid_pairs`，覆盖 command order、tag 唯一性、cleanup 顺序、PID 变化 exclusion、post failure invalidation、scenario 失败退出码。
10. **Clock tick provenance**：`getconf CLK_TCK` 失败时 `clk_tck = null`，`cpu_time_ms = null`，不静默 fallback 100。保留 raw utime/stime ticks。
11. **boot_stable 语义**：脚本不自动 reboot，记录 `sys.boot_completed`、`boot_id`、`/proc/uptime`。
12. **多设备 guard**：多台在线设备且未指定 `--device` 立即失败。
13. **doctor 无 KEYCODE_MENU**：`input --help` 替代实体按键测试，避免改变 UI。
14. **坐标适配**：解析 `wm size` 计算 center_x / swipe y；非 1080p 标记 `NON_1080P_REQUIRES_CALIBRATION`。
15. **通知 capability precondition**：`cmd_notification_available && post_available` 是必需前置条件。
