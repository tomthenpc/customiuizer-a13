# A13-PERF-P0 — Performance Baseline and Measurement Infrastructure

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-P0-BASELINE-AND-MEASUREMENT` |
| 分支 | `devin/a13-memory-performance-optimization` |
| 基线 commit | `5ca30911e3fc41e80bfde7ac294218e9b855b8d3` |
| 应用 ID | `tv.withaibuild.customiuizer.r13` |
| 主 Activity | `tv.withaibuild.customiuizer.MainActivity` |
| 状态 | `ACTIVE — 测量基础设施已完成，真实 A13 设备基线待回填` |
| 运行时基线 | `RUNTIME_BASELINE_PENDING_DEVICE` |
| 下游任务 | `A13-PERF-P1A` 可静态并行推进；`A13-PERF-P1B` 源码优化须等待真实运行时基线或明确授权 |

## 背景

A13 性能优化需要建立可重复、可审计的测量体系。没有真实基线就无法证明"模块可归因内存成本降低 50%"，也无法区分基础注入成本、功能注册成本和功能运行成本。P0 只负责建立工具和文档，不修改业务代码。

## 固定范围

- 在 `tasks/active/` 建立本任务文件。
- 在 `docs/audit/` 建立 `A13_PERF_BASELINE.md`。
- 在 `docs/audit/` 建立 `A13_PERF_BASELINE.json`。
- 在 `tools/` 实现 `a13_perf_probe.py`。
- 在 `tools/tests/` 实现 `test_a13_perf_probe.py` 及 fixture。
- 在 `docs/audit/` 建立 `A13_PERF_FINDINGS.md`，仅记录不修复。

## 非目标

- 不修改任何 Hook 实现、功能注册、UI 页面、偏好行为或默认值。
- 不增加缓存、后台任务、调试 Service、埋点框架或 Benchmark 依赖。
- 不通过关闭功能或改变 APK 大小来声称运行内存下降。
- 不合并或删除 `devin/a13-rom-intelligence-audit` 分支。
- 不修改 `main` 分支。

## 测量对象

按四类进程分别测量，避免把整进程内存直接归因给模块：

1. **管理应用进程** — `tv.withaibuild.customiuizer.r13`。
2. **SystemUI 进程** — `com.android.systemui`。
3. **system_server 进程** — `system_server`。
4. **普通被注入应用进程** — 至少一个轻量目标（如 `com.android.settings`）和一个复杂目标（如 `com.miui.home`）。

模块可归因成本以**差值**表达：

```text
模块增量成本 = 开启模块后的测量结果 - 关闭模块后的相同场景测量结果
```

## 测量场景

### 管理应用

- `manager_cold_start`
- `manager_home_idle`
- `manager_heavy_page_1`（`prefs_system.xml`）
- `manager_heavy_page_2`（`prefs_controls.xml`）
- `manager_heavy_page_3`（`prefs_launcher.xml`）
- `manager_page_cycle_20`
- `manager_background`
- `manager_exit_reclaim`

### 普通注入应用

- `target_light_idle`
- `target_heavy_idle`
- `target_cold_start`

### SystemUI

- `systemui_idle`
- `notification_shade_open_close`
- `quick_settings_open_close`
- `lock_unlock_cycle`

### system_server

- `system_server_idle`（仅记录可稳定采集的增量趋势，不归因整进程内存）

## 指标定义

| 指标 | 含义 | 主要来源 |
|------|------|----------|
| `total_pss_kb` | 进程总 PSS | `dumpsys meminfo <pid>` |
| `java_heap_kb` | Java 堆 | `dumpsys meminfo <pid>` |
| `native_heap_kb` | Native 堆 | `dumpsys meminfo <pid>` |
| `graphics_kb` | Graphics 内存 | `dumpsys meminfo <pid>` |
| `private_dirty_kb` | Private Dirty | `dumpsys meminfo <pid>` |
| `thread_count` | 线程数 | `/proc/<pid>/status` |
| `cpu_time_ms` | CPU 累计时间 | `/proc/<pid>/stat` |
| `startup_total_time_ms` | 启动总时间 | `am start -W` |
| `gc_count` | GC 次数 | `dumpsys meminfo` / `procstats` |
| `gc_freed_objects` | GC 释放对象数 | `dumpsys meminfo` / `procstats` |
| `binder_proxy_count` | Binder 代理数 | `/proc/<pid>/status` / 解析 `Binder` 段 |
| `loaded_class_count` | 加载类数 | 待实现，当前标记不可用 |
| `process_restart_count` | 进程重启次数 | `dumpsys procstats --hours 0 <package>` |

无法可靠获得的指标必须写 `unavailable_reason`，不允许填 0 或猜测值。

## 执行步骤

1. 确认工作区在 `devin/a13-memory-performance-optimization` 且干净。
2. 复用仓库任务模板建立 `tasks/active/A13-PERF-P0-BASELINE-AND-MEASUREMENT.md`。
3. 建立 `docs/audit/A13_PERF_BASELINE.md`，明确四类对象、三态模块、场景与回归门禁。
4. 建立 `docs/audit/A13_PERF_BASELINE.json`，提供机器可读 schema 与示例。
5. 实现 `tools/a13_perf_probe.py`：
   - `doctor` 检查 adb、设备、权限；
   - `process` 重复采样指定进程并输出 JSON；
   - `startup` 重复测量 `am start -W` 并输出 JSON。
6. 实现 `tools/tests/test_a13_perf_probe.py`：
   - `dumpsys meminfo` 解析；
   - 缺失字段；
   - 不同换行；
   - Windows 中文错误；
   - adb 不存在、设备未连接、进程不存在；
   - 中位数计算；
   - JSON 结构；
   - 非零退出码。
7. 建立 `docs/audit/A13_PERF_FINDINGS.md`，记录观察到的风险但不修复。
8. 运行验证：`python -m compileall tools`、`python -m unittest discover -s tools/tests -p "test_*.py"`、`python tools/verify.py fast --changed`、`git diff --check`。
9. 按两个原子提交提交并推送。

## 验收标准

- [ ] `A13_PERF_BASELINE.md` 已建立并区分四类进程与三态模块。
- [ ] `A13_PERF_BASELINE.json` 已建立且结构包含所有要求字段。
- [ ] `a13_perf_probe.py` 仅使用标准库、支持 Windows PowerShell、错误明确。
- [ ] 解析器离线测试全部通过。
- [ ] 文档中列出至少五种标准场景与统计方法。
- [ ] 后续性能回归门禁明确。
- [ ] 未修改业务功能代码或引入第三方依赖。
- [ ] 工作区干净且已推送。

## 设备不足时的处理方式

若当前环境没有可用的 A13 真机、ADB 或权限：

- 不伪造运行时基线。
- 不根据源码估算具体 PSS 数字。
- 继续完成脚本、schema、文档和测试。
- 在基线文档和 JSON 中明确标记 `RUNTIME_BASELINE_PENDING_DEVICE`。
- 列出真机执行命令。
- 将"工具完成"与"真机基线完成"分开报告。

## 风险与回滚方式

| 风险 | 影响 | 回滚 |
|------|------|------|
| `a13_perf_probe.py` 解析错误导致数据不可用 | P0 阶段无运行数据 | 修复解析器，重新采集 |
| 文档或 schema 字段遗漏 | 后续比较无法对齐 | 更新 `A13_PERF_BASELINE.md` / `.json` |
| 误改业务代码 | 破坏功能 | 立即 `git checkout --` 恢复相关文件 |

## P0 完成情况

- 测量基础设施已完成：`tools/a13_perf_probe.py`、JSON schema、单元测试、基线文档均到位。
- 真实 A13 设备数据尚未获得，状态保持 `RUNTIME_BASELINE_PENDING_DEVICE`。
- 未伪造任何设备数据或推测 PSS 数值。
- 本任务保留在 `tasks/active/`，不移动到 `tasks/completed`。

## 与 P1A/P1B 的关系

- `A13-PERF-P1A`（静态 Hook 与进程成本拓扑）可在无真机数据时并行推进。
- `A13-PERF-P1B`（实际源码优化）必须基于真实运行时基线，除非收到明确授权。

## 最终证据位置

- 任务文件：`tasks/active/A13-PERF-P0-BASELINE-AND-MEASUREMENT.md`
- 基线文档：`docs/audit/A13_PERF_BASELINE.md`
- 基线数据：`docs/audit/A13_PERF_BASELINE.json`
- 采集脚本：`tools/a13_perf_probe.py`
- 测试结果：`tools/tests/test_a13_perf_probe.py`
- 发现记录：`docs/audit/A13_PERF_FINDINGS.md`
