# A13 Runtime Baseline Execution Result

> Branch: `devin/a13-memory-performance-optimization`
> HEAD: `bc0363c`（R1 修正前）
> Application ID: `tv.withaibuild.customiuizer.r13`
> Status: `DEVICE_NOT_CONNECTED`
> Date: 2026-08-07

## 1. 状态

```text
P0_TOOLING = READY_FOR_DEVICE_EXECUTION
P0_EXECUTION = BLOCKED_DEVICE_NOT_CONNECTED
P0_DEVICE_EVIDENCE = RUNTIME_BASELINE_PENDING_DEVICE
```

## 2. 执行结果

```text
executed_samples = 0
performance_conclusion = NONE
```

**禁止以下任何运行时结论**（无真机数据支撑）：

- memory improved
- CPU improved
- lower usage
- regression free

## 3. 设备连接状态

| 项 | 值 |
|----|-----|
| ADB 路径 | `C:\Android\platform-tools\adb.exe` |
| ADB 版本 | `1.0.41, Version 37.0.0-14910828` |
| 平台 | Windows 10.0.26100 |
| 在线设备数 | **0** |

```
$ adb devices -l
List of devices attached
```

## 4. 设备信息

| 项 | 值 |
|----|-----|
| 设备型号 | `N/A`（无设备连接） |
| ROM 版本 | `N/A` |
| Android 版本 | `N/A` |
| LSPosed 版本 | `N/A` |
| 模块版本 | `bc0363c` → R1 修正后见最终 SHA |

## 5. 基线数据

### 5.1 enabled_features_off

| 指标 | 值 |
|------|-----|
| system_server PSS | `N/A` |
| system_server Private Dirty | `N/A` |
| system_server CPU time | `N/A` |
| SystemUI PSS | `N/A` |
| SystemUI Java heap | `N/A` |
| SystemUI Native heap | `N/A` |
| SystemUI Threads | `N/A` |

### 5.2 enabled_typical_features

| 指标 | 值 |
|------|-----|
| system_server PSS | `N/A` |
| system_server Private Dirty | `N/A` |
| system_server CPU time | `N/A` |
| SystemUI PSS | `N/A` |
| SystemUI Java heap | `N/A` |
| SystemUI Native heap | `N/A` |
| SystemUI Threads | `N/A` |

## 6. PSS/CPU/Thread 变化

无数据。所有场景因无设备连接而未执行。

## 7. 场景执行状态

| 场景 | 状态 | 原因 |
|------|------|------|
| `boot_stable` | `NOT_EXECUTED` | 无设备 |
| `notification_panel_open_close` | `NOT_EXECUTED` | 无设备 |
| `volume_adjust_10` | `NOT_EXECUTED` | 无设备 |
| `qs_panel_expand_collapse` | `NOT_EXECUTED` | 无设备 |
| `notification_menu_create_delete` | `NOT_EXECUTED` | 无设备 |

## 8. 阻塞原因与后续步骤

### 8.1 阻塞原因

- **无 A13 设备连接**。ADB 已正确安装并运行，但 `adb devices -l` 返回空列表。

### 8.2 后续步骤

1. 连接 A13 设备并启用 USB 调试。
2. 运行 `python tools/a13_runtime_baseline.py --adb <path> doctor` 确认 15 项检查通过。
3. 安装当前模块 APK。
4. 在 LSPosed 中启用模块。
5. 对三种模块状态分别执行采集。
6. 将 `artifacts/runtime-baseline/` 数据回填到本文档和 `docs/audit/A13_PERF_BASELINE.json`。
7. 更新状态为 `DEVICE_EVIDENCE_COLLECTED`。
