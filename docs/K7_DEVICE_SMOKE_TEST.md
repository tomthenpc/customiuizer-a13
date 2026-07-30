# K7 实机冒烟测试报告

## 1. 测试身份

| 字段 | 值 |
|---|---|
| 测试日期 | |
| 测试人员 | |
| 仓库 | `tomthenpc/customiuizer-a13` |
| 分支 | `codex/r13.7-final-audit` |
| 测试 commit | |
| APK SHA-256 | |
| 签名证书 SHA-256 | |
| applicationId | `tv.withaibuild.customiuizer.r13` |
| versionName / versionCode | |

## 2. 设备与环境

| 字段 | 值 |
|---|---|
| 设备型号 | |
| Android / SDK | |
| MIUI / ROM build | |
| LSPosed/Vector 版本 | |
| libxposed API | |
| 模块作用域 | |
| 用户配置来源 | |

## 3. 采集方式

只读采集命令：

```powershell
.\tools\k7-device-smoke.ps1 `
  -Serial "<adb serial>" `
  -OutputRoot "<仓库外证据目录>" `
  -ObservationMinutes 30
```

脚本不会清除 logcat，也不会安装 APK、修改 SharedPreferences、启用功能、改变作用域、重启进程或重启设备。测试动作由测试人员按照验收清单手工执行。

| 字段 | 值 |
|---|---|
| 证据目录 | |
| 采集开始时间 | |
| 采集结束时间 | |
| `adb-devices.txt` | |
| `device-properties.txt` | |
| `package-version.txt` | |
| `pids-start.txt` / `pids-end.txt` | |
| `module-load-filtered.txt` | |
| `crash-anr-watchdog-summary.txt` | |
| `logcat.txt` | |

## 4. A–J 结果摘要

状态只能填写 `PASS`、`FAIL`、`NOT TESTED` 或 `NOT APPLICABLE`。

| 分组 | 状态 | 失败项/未测项 | 证据 |
|---|---|---|---|
| A. 基础加载 | `<状态>` | | |
| B. K11 回调与生命周期 | `<状态>` | | |
| C. K12 设备监控 | `<状态>` | | |
| D. K13 设置列表与图标 | `<状态>` | | |
| E. K14 语言与搜索 | `<状态>` | | |
| F. K15 AudioVisualizer | `<状态>` | | |
| G. K16 锁屏专辑图 | `<状态>` | | |
| H. K17 综合冒烟 | `<状态>` | | |
| I. 30 分钟稳定性观察 | `<状态>` | | |
| J. 日志结论 | `<状态>` | | |

详细逐项结果填写在 [R13_7_DEVICE_ACCEPTANCE_CHECKLIST.md](R13_7_DEVICE_ACCEPTANCE_CHECKLIST.md)。

## 5. 异常记录

每个异常单独记录，不以包名出现作为因果证明。

| 时间 | 进程/PID | 操作 | 现象 | 模块加载证据 | 因果堆栈/Hook 失败 | 严重度 | 结论 |
|---|---|---|---|---|---|---|---|
| | | | | | | | |

## 6. 最终结论

```text
K7：PASS / FAIL / NOT TESTED
K12：PASS / FAIL / NOT TESTED
K15：PASS / FAIL / NOT TESTED
K16：PASS / FAIL / NOT TESTED

是否为实机可合并候选：是 / 否
是否允许合并 main：否（需用户另行授权）
是否允许发布：否（需用户另行授权）
```

未测项、阻塞与下一步：

```text

```
