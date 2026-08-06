# A13 P1B Phase QA

> Branch: `devin/a13-memory-performance-optimization`  
> QA base: `b8eead54594bfbc850309a1ead2f617a8122b411`  
> P0 baseline: `RUNTIME_BASELINE_PENDING_DEVICE`

本文件记录 P1B-0 至 P1B-4B 各 Slice 的质量封板审计结果。状态只能是 `QA_ACCEPTED`、`QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`、`QA_CONDITIONAL`、`QA_REOPENED`、`QA_REJECTED`。

## 1. 阶段审计总表

| task_id | base_sha | engineering_sha | closure_sha | task_file | task_file_status | qa_status | blocking_findings | required_corrections |
|---|---|---|---|---|---|---|---|---|
| P1B-0 | `0b034f36a7810bcf2cc184a8b424330981ad390c` | `1a15dcb8f66b6d900d3c7504d7f06f3a1c898478` | `1a15dcb8f66b6d900d3c7504d7f06f3a1c898478` | `tasks/completed/A13-PERF-P1B-0-ZERO-FEATURE-COST.md` | completed | `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING` | 无 | 更新任务文件状态从 `COMPLETED` 到 `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING` |
| P1B-1 | `cd89c38f02834db5baf24e5ab08b345c187085fe` | `44b4c4c` | `44b4c4c` | `tasks/completed/A13-PERF-P1B-1-STARTUP-LOAD-GATING.md` | completed | `QA_CONDITIONAL` | `LauncherInstaller.hasAnyLauncherStartupFeature()` 已存在，但生产入口未可靠证明被用于控制 Launcher 安装链；缺少 `MainModule → LauncherInstaller` 集成测试 | 拆分 PackageReady / Application 门控并接入 `MainModule` 路由；新增集成测试覆盖 all-off/package-ready-only/application-only/both 场景 |
| P1B-2 | `3d38cdd53a6190c68187a803badaf201dfda25cd` | `74b54e5` | `5f780b8` | `tasks/active/A13-PERF-P1B-2-AUDIOSERVICE-HOT-PATH.md` | active but status `COMPLETED` | `QA_CONDITIONAL` | 任务文件终点 `fec4ee6` 不在当前分支历史；`system_volumesteps` 生效语义、createStreamStates 多次调用、readSettings 失败语义需复核 | 修正任务文件 base/engineering/closure SHA；复核 `system_volumesteps` 重启/运行时语义；补充 AudioService 生命周期与失败语义测试 |
| P1B-3 | `5f780b8a15727114bd29f01188191a2520ff2509` | `24053dc` | `ec05f5e` | `tasks/completed/A13-PERF-P1B-3-QS-TILE-HOT-PATH.md` | completed | `QA_CONDITIONAL` | 同一具体 Tile 类可能对应多个 custom(...) / intent(...) spec，Hook 闭包可能捕获首次 originalTileName | 新增 shared-class multi-spec 测试；若失败则将 spec 写入 Tile additional instance field |
| P1B-4A | `ec05f5e948167742da6520cdf64b9fd32d360b3e` | `b58ced9` | `69c1441` | `tasks/completed/A13-PERF-P1B-4A-NOTIFICATION-MENU-HOT-PATH.md` | completed | `QA_REOPENED` | 测试 `createMenuViews_after_doesNotRecreateMenuItemsOnSecondCall` 断言第二次调用后菜单项数为 6，与“不重复创建”矛盾 | 移回 `tasks/active` 并修正菜单生命周期合同；补充 ROM 类型兼容与幂等性测试 |
| P1B-4B | `7f0f37c` | `922df7a` | `b8eead5` | `tasks/completed/A13-PERF-P1B-4B-NOTIFICATION-INTENT-LAUNCH-HOT-PATH.md` | completed but status `IN_PROGRESS` | `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`（待正式签名 Release 后确认） | 任务文件内部状态不一致；`A13_HOOK_COST_MAP.json` 被手动修改过需重新生成 | 修正任务文件状态；重新生成扫描器地图；执行正式签名 Release 与 apksigner 验证 |

## 2. 变更范围核对

- Hook call site 总数：669（未新增）。
- 进程覆盖范围：未扩大。
- 未新增线程、Handler、listener、observer、锁、无界缓存或依赖（除 QA 任务要求的集成测试 fakes 外）。

## 3. 任务文件与 SHA 一致性

- P1B-0 状态 `COMPLETED` → 待更新为 `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`。
- P1B-1 状态 `COMPLETED` → 待更新为 `QA_CONDITIONAL`（需修复 Launcher 路由）。
- P1B-2 位于 `tasks/active/` 但状态写 `COMPLETED`；终点 `fec4ee6` 不在分支历史 → 需修正。
- P1B-3 状态 `ENGINEERING_COMPLETE_DEVICE_EVIDENCE_PENDING` → 待评估是否可直接 `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING` 或先补测试。
- P1B-4A 在 `completed/` 但 QA 判定 `QA_REOPENED` → 需移回 `active/` 并修正。
- P1B-4B 在 `completed/` 但状态 `IN_PROGRESS` → 需修正为 `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`（Release 后）。
