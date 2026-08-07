# A13 Runtime Baseline Execution Result

> Branch: `devin/a13-memory-performance-optimization`
> HEAD: 见当前 Final SHA
> Application ID: `tv.withaibuild.customiuizer.r13`
> Status: `DEVICE_NOT_CONNECTED`
> Date: 2026-08-07

## 1. 状态

```text
P0_TOOLING = READY_FOR_DEVICE_SMOKE_TEST
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
| 模块版本 | 见当前 Final SHA |

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
- `notification_menu_create_delete` 的 ROM UI 自动化尚未在真实 xaga/MIUI14/HyperOS1 设备上验证。

### 8.2 升级到 `READY_FOR_DEVICE_EXECUTION` 的条件

1. 连接 A13 设备并启用 USB 调试。
2. 运行 `python tools/a13_runtime_baseline.py --adb <path> doctor` 确认 15 项检查通过。
3. 执行一次 `notification_menu_create_delete` smoke test：
   - 确认 `cmd notification post` 后 `cmd notification list`/`dumpsys notification` 验证通过。
   - 确认 uiautomator BEFORE/AFTER 文件写入。
   - 确认 `KEYCODE_BACK` 关闭菜单、swipe-dismiss 清除通知。
   - 确认 PRE/POST PID 一致、artifact 完整、manifest 字段正确。
4. 如菜单触发无法被验证，标记 `MENU_TRIGGER_EVIDENCE = UNVERIFIED_COORDINATE_ACTION`。
5. smoke test 通过后，`P0_TOOLING` 可升级为 `READY_FOR_DEVICE_EXECUTION`。

### 8.3 完整采集

1. 安装当前模块 APK。
2. 在 LSPosed 中启用模块。
3. 对三种模块状态分别执行：

```powershell
python tools/a13_runtime_baseline.py --adb <path> run --scenario all --module-state <state>
```

4. 将 `artifacts/runtime-baseline/` 数据回填到本文档和 `docs/audit/A13_PERF_BASELINE.json`。
5. 更新状态为 `DEVICE_EVIDENCE_COLLECTED`。
