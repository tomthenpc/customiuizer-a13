# A13-PERF-P1B-0 — Zero-Feature Enabled Cost Reduction

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-P1B-0-ZERO-FEATURE-COST` |
| 分支 | `devin/a13-memory-performance-optimization` |
| 起点 commit | `0b034f36a7810bcf2cc184a8b424330981ad390c` |
| 状态 | `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING` |
| 终点 commit | `1a15dcb8f66b6d900d3c7504d7f06f3a1c898478` |
| 验证 | `python -m compileall tools`、`python -m unittest discover -s tools/tests -p "test_*.py"`、`python tools/a13_hook_cost_scan.py --verify-stability`、`python tools/source_hazard_scan.py`、`python tools/verify.py full`、`git diff --check` 通过；Release 构建编译/R8/lint 通过，打包因缺少签名配置失败 |
| 授权 | 已获得业务源码修改授权，但仅限零功能成本范围 |
| 依赖 | `A13-PERF-P0`（基础设施），`A13-PERF-P1A`（静态地图） |

## 背景

P1A 已识别模块在启用状态下、相关功能关闭时仍产生的 Hook 注册、类加载、偏好解析和反射成本。P1B-0 只处理其中静态证据最明确、行为等价可保证的项目，不进入一般热路径优化。

## 允许范围

- 功能关闭时仍注册的 Hook。
- 进程或包门控过晚。
- 门控前不必要加载的重型功能类。
- 模块启用但功能全部关闭时仍产生的可避免初始化成本。

## 非目标

- 一般热路径偏好读取优化。
- 反射缓存。
- 重复 Hook 合并。
- UI/布局/资源优化。
- 生命周期监听注销重构。
- 构建或混淆配置改动。
- 未经验证的性能收益声明。

## 实施原则

1. 先确认目标进程/包，再读取最小开关，功能关闭立即返回。
2. 不得先构建完整功能列表再过滤。
3. 保持运行时即时开关语义；若不能改为关闭不注册，则标记 `RUNTIME_TOGGLE_SEMANTICS_BLOCKED`。
4. 不得新增常驻状态：全局 Map、事件总线、监听器、线程、Handler、广播、Observer、缓存框架等。

## 允许修改候选类别

- `FEATURE_OFF_NO_REGISTRATION`
- `EARLIER_PROCESS_GATE` / `EARLIER_PACKAGE_GATE`
- `LAZY_FEATURE_CLASS_LOAD`

## 必须停止条件

- 需要改变运行时即时开关行为。
- 无法证明目标进程范围。
- 需要新增监听器/线程/缓存。
- 功能开启路径测试失败。
- 静态证据不足。

## 验收标准

- [ ] P1A 可疑进程归属已核实并修正。
- [ ] 启动路径与真正高频回调已分开。
- [ ] 偏好读取、反射和重复 Hook 统计已去误报。
- [ ] 功能关闭不注册，功能开启仍注册且行为不变。
- [ ] Hook 总数不增加，覆盖进程范围不扩大。
- [ ] 不新增线程、监听器、后台任务或第三方依赖。
- [ ] 完整验证与 Release 构建通过。
- [ ] P0 仍保留在 `tasks/active`，状态保持 `RUNTIME_BASELINE_PENDING_DEVICE`。

## 输出文件

- `docs/audit/A13_HOOK_COST_MAP.md`
- `docs/audit/A13_HOOK_COST_MAP.json`
- `docs/audit/A13_PROCESS_LOAD_MAP.md`
- `docs/audit/A13_PERF_FINDINGS.md`
- `tools/a13_hook_cost_scan.py`（回归门禁扩展）
- 新增/更新测试

## 停止点

P1B-0 完成后立即停止。不进入 P1B-1 或热路径优化。
