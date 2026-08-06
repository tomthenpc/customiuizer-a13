# A13-PERF-P1A — Static Hook Cost Topology and Process Load Map

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-P1A-HOOK-COST-TOPOLOGY` |
| 分支 | `devin/a13-memory-performance-optimization` |
| 起点 commit | `6f2d4374932e14a192b19358a2c53aab0c623d14` |
| 状态 | `COMPLETED` |
| 依赖 | `A13-PERF-P0`（基础设施，仍待真实设备基线） |
| 终点 commit | `待填入` |
| 验证 | `python -m compileall tools`、`python -m unittest discover -s tools/tests -p "test_*.py"`、`python tools/verify.py full`、`git diff --check` 全部通过 |

## 背景

A13 性能优化需要先完整识别模块的 Hook 注册拓扑、进程加载路径、功能开关门控位置及潜在常驻成本，才能在 P1B 中按真实证据优化。P1A 只做静态地图，不动业务代码。

## 范围

- 更新 `tasks/active/A13-PERF-P0-BASELINE-AND-MEASUREMENT.md` 的 P0 完成状态。
- 新建本任务文件 `tasks/active/A13-PERF-P1A-HOOK-COST-TOPOLOGY.md`。
- 新建 `docs/audit/A13_HOOK_COST_MAP.md` 与 `docs/audit/A13_HOOK_COST_MAP.json`。
- 新建 `docs/audit/A13_PROCESS_LOAD_MAP.md`。
- 如果可靠，新建 `tools/a13_hook_cost_scan.py` 与 `tools/tests/test_a13_hook_cost_scan.py`。
- 更新 `docs/audit/A13_PERF_FINDINGS.md`，编号记录新发现。

## 非目标

- 不修改任何 Hook 实现、模块入口、功能注册条件、UI、偏好逻辑。
- 不合并、删除或移动 Hook。
- 不增加缓存、线程、调度、后台任务。
- 不修改构建配置或默认值。
- 不伪造运行时数据或声称未经测量的内存/百分比收益。

## Hook 统计定义

| 统计项 | 定义 |
|--------|------|
| Hook call site | 源码中调用 Hook API（`findAndHookMethod`、`hookAllMethods` 等）的位置数量 |
| Logical hook target | 去重后的目标类、方法、签名组合数量 |
| Feature registration path | 功能从入口到 Hook 注册的逻辑路径数量 |
| Runtime registration estimate | 根据进程、包范围和门控推导的潜在运行时注册范围 |

## 进程统计定义

| 统计项 | 定义 |
|--------|------|
| 入口函数 | 包加载或 Application/SystemUI 的入口方法 |
| 最早门控 | 第一个按进程/包名过滤的位置 |
| 门控前加载 | 在过滤前已初始化的类/object/static 集合 |
| 无关加载 | 当前进程不应加载的功能类被初始化 |

## 分析方法

1. 使用 `tools/source_hazard_scan.py` 等现有工具识别高危模式。
2. 使用 `git grep` / Python 标准库扫描定位 `findAndHookMethod`、`hookAllMethods`、`hookAllConstructors`、`findAndHookConstructor`、`hookMethod`、`hookBefore`、`hookAfter`、`registerReceiver`、`registerContentObserver`、`postDelayed` 等注册点。
3. 阅读 `MainModule.java`、` installers/`、`mods/`、`mods/catalog/`、`mods/utils/` 等目录，确认进程、包、功能门控顺序。
4. 人工审计动态目标、反射目标、ROM 条件分支，记录置信度。

## 风险分级

- `HIGH`：功能关闭仍注册/初始化、高频回调、热路径分配、跨进程无关加载。
- `MEDIUM`：偏好读取、反射查找、可合并重复目标、缺少注销路径。
- `LOW`：仅静态开销、一次性初始化、明确按进程隔离且无热路径成本。

## 输出文件

- `docs/audit/A13_HOOK_COST_MAP.md`
- `docs/audit/A13_HOOK_COST_MAP.json`
- `docs/audit/A13_PROCESS_LOAD_MAP.md`
- `docs/audit/A13_PERF_FINDINGS.md`
- `tools/a13_hook_cost_scan.py`（如果可靠实现）
- `tools/tests/test_a13_hook_cost_scan.py`（如果新增扫描工具）

## 验收门槛

- [ ] Hook 成本地图覆盖所有模块入口及长期注册点。
- [ ] 记录了 `hook_id`、`feature_id`、`target_process`、`target_package_scope`、`preference_key`、`default_enabled_state`、`registered_when_feature_disabled` 等字段。
- [ ] 进程加载图覆盖管理应用、SystemUI、system_server、设置、启动器、框架相关进程及普通应用。
- [ ] 统计出 Hook call site、logical hook target、feature registration path 总数。
- [ ] 输出 Top 10 静态优化候选并给出源码证据与建议方向。
- [ ] 单独分析了 `Zero-feature enabled cost`。
- [ ] 未修改业务源码。

## 停止条件

静态地图完整、验证通过、文档更新后即完成。禁止自动实施 Top 10 中的任何优化。
