# A13 P1B Phase QA

> Branch: `devin/a13-memory-performance-optimization`  
> QA base: `b8eead54594bfbc850309a1ead2f617a8122b411`  
> P0 baseline: `RUNTIME_BASELINE_PENDING_DEVICE`

本文件记录 P1B-0 至 P1B-4B 各 Slice 的质量封板审计结果。状态只能是 `QA_ACCEPTED`、`QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`、`QA_CONDITIONAL`、`QA_REOPENED`、`QA_REJECTED`。

## 1. 阶段审计总表

| task_id | base_sha | engineering_sha | closure_sha | task_file | task_file_status | qa_status | blocking_findings | required_corrections |
|---|---|---|---|---|---|---|---|---|
| P1B-0 | `0b034f36a7810bcf2cc184a8b424330981ad390c` | `1a15dcb8f66b6d900d3c7504d7f06f3a1c898478` | `1a15dcb8f66b6d900d3c7504d7f06f3a1c898478` | `tasks/completed/A13-PERF-P1B-0-ZERO-FEATURE-COST.md` | completed | `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING` | 无 | 更新任务文件状态从 `COMPLETED` 到 `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING` |
| P1B-1 | `cd89c38f02834db5baf24e5ab08b345c187085fe` | `44b4c4c7dd642bbe3de9e2ba3735872c1c445115` | `a36b5a2f23e0f7e04d86abde179fdaaf7dbb115f` | `tasks/completed/A13-PERF-P1B-1-STARTUP-LOAD-GATING.md` | `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING` | `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING` | 无静态 blocker；P0 真机基线与运行时性能量化证据待 QA-1/P6 收集；正式签名 Release 由 QA-1 总封板统一处理 | 无 P1B-1 静态修正；剩余为阶段级/设备级证据收集 |
| P1B-2 | `3d38cdd53a6190c68187a803badaf201dfda25cd` | `74b54e5` | `5f780b8` | `tasks/active/A13-PERF-P1B-2-AUDIOSERVICE-HOT-PATH.md` | active but status `COMPLETED` | `QA_CONDITIONAL` | 任务文件终点 `fec4ee6` 不在当前分支历史；`system_volumesteps` 生效语义、createStreamStates 多次调用、readSettings 失败语义需复核 | 修正任务文件 base/engineering/closure SHA；复核 `system_volumesteps` 重启/运行时语义；补充 AudioService 生命周期与失败语义测试 |
| P1B-3 | `5f780b8a15727114bd29f01188191a2520ff2509` | `24053dc` | `ec05f5e` | `tasks/completed/A13-PERF-P1B-3-QS-TILE-HOT-PATH.md` | completed | `QA_CONDITIONAL` | 同一具体 Tile 类可能对应多个 custom(...) / intent(...) spec，Hook 闭包可能捕获首次 originalTileName | 新增 shared-class multi-spec 测试；若失败则将 spec 写入 Tile additional instance field |
| P1B-4A | `ec05f5e948167742da6520cdf64b9fd32d360b3e` | `395e09338d6a9b663847d91ebddb06cb2ee9c44e` | `395e09338d6a9b663847d91ebddb06cb2ee9c44e` | `tasks/active/A13-PERF-P1B-4A-NOTIFICATION-MENU-HOT-PATH.md` | active | `ENGINEERING_COMPLETE_DEVICE_EVIDENCE_PENDING` | `STALE_ROW_BINDING_RISK`、`CONTEXT_SEMANTICS_DRIFT`、`OPTIONAL_FIELD_NULL_SAFETY`、`RUNTIME_SUBTYPE_RESOLUTION` 已修复 | `A13_HOOK_COST_MAP.*` 由 `a13_hook_cost_scan.py` 重新生成；P0 真机基线待 QA-1/P6 收集；更新任务文件终点 commit |
| P1B-4B | `7f0f37c` | `922df7a` | `b8eead5` | `tasks/completed/A13-PERF-P1B-4B-NOTIFICATION-INTENT-LAUNCH-HOT-PATH.md` | completed but status `IN_PROGRESS` | `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`（待正式签名 Release 后确认） | 任务文件内部状态不一致；`A13_HOOK_COST_MAP.json` 被手动修改过需重新生成 | 修正任务文件状态；重新生成扫描器地图；执行正式签名 Release 与 apksigner 验证 |

## 2. P1B-1 状态与修复记录

- 原缺陷：`LauncherInstaller.hasAnyLauncherStartupFeature()` 已存在，但生产入口 `MainModule.onPackageReady()` 未可靠接入 Launcher 安装链；`hasAnyGlobalAction()` 把 `launcher_*_action` 键误判为 SystemUI 全局动作。
- 修复提交：
  - `584032d` fix(a13): complete P1B-1 launcher gating tests and fix SystemUI cross-contamination
  - `79f1610` fix(a13): publish baseline preference snapshot when listener registration is delayed
  - `afd3d15` test(a13): harden launcher routing assertions and isolate Build stub
  - `d1c19d2` fix(a13): correctly isolate SystemUI global action preferences
  - `41d72a9` test(a13): strengthen SystemUI routing lifecycle assertions
  - `4040f6f` fix(a13): remove production test probes and extract SystemUI restart guard
  - `f7eeba8` test(a13): execute SystemUI onCreate routing lifecycle
  - `7503b57` test(a13): inventory SystemUI startup gate conditions
  - `dc80d96` docs(a13): record raw SystemUI gate inventory
- 新增集成测试：`MainModuleLauncherRoutingTest` 覆盖 all-off/package-ready-only/application-only/both/non-launcher 五个场景；`watcher` 注册次数按场景断言（0 或 1）。
- 新增测试桩：`android.app.Application`、`android.os.Build`、`FakeSharedPreferences.registerCount`、`FakeXposedInterface.remotePreferences`。
- 交叉污染根因：`SystemUiInstaller.hasAnyGlobalAction()` 扫描所有 `*_action` 偏好，未排除 Launcher 手势域。
- R1-A 排除规则：抽取 `isSystemUiGlobalActionKey(key)`，按正向 SystemUI 域识别；先去掉 `pref_key_` 前缀，再要求基础键以 `controls_` 或 `system_` 开头并以 `_action` 结尾；`launcher_*_action`、`pref_key_launcher_*_action` 以及其他未知 action 键被拒绝。
- 新增直接门控测试：`SystemUiGateTest` 覆盖 6 个真实 `launcher_*_action` 键、`pref_key_launcher_*_action`、合法 `controls_*_action`、`system_lockscreenshortcuts_right_action`、未知 action 和 all-defaults。
- 新增 SystemUI 路由测试：`MainModuleSystemUiRoutingTest` 覆盖 `com.android.systemui` 进程在仅含 `launcher_swipedown_action=2` 时不会安装 `SystemUIApplication#onCreate` hook；在仅含 `controls_backlong_action=2` 时会安装 `SystemUIApplication#onCreate` hook。注意：R1-A 阶段尚未手动触发 onCreate callback、未观测 `setupStatusBar` 执行次数、未统计 `FeatureRuntime` 创建次数，也未区分 PreferenceBootstrap 基线监听器与 SystemUI `watchPreferenceChange` 监听器。
- 当前 QA 状态：`QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`。原因：R1-A/B 静态正确性、SystemUI/Launcher 启动门控、callback、inventory、structural diff、mutation 反证均已封板；P0 真机基线、运行时性能量化与正式签名 Release 由 QA-1/P6 统一处理。

## 2.1 P1B-1 R1-B2 审计硬化

- 关键提交：
  - `442ade1c5b780aead006b542775111f61499cf0b` feat(a13): add SystemUI startup gate differential audit (R1-B2)
  - `a36b5a2f23e0f7e04d86abde179fdaaf7dbb115f` qa(a13): harden SystemUI startup gate differential audit (R1-B2 proof hardening)
  - 本轮最终 closure commit（见 Git history `docs(a13): close P1B-1 static QA`）完成 provenance 去绝对路径、DeterministicOutputTests、Match Coverage markdown。
- 关键改动：
  - `tools/a13_systemui_gate_diff.py` 核心审计入口统一为 `diff_from_repo(repo_root)`，provenance 不再包含本地绝对路径。
  - `global_action_domain` 状态必为 `PARSED_SAFE` / `PARSED_CONTAMINATED` / `UNKNOWN`，后两者触发 `SEMANTIC_REVIEW_REQUIRED`。
  - `DiffResult` 新增 `matched_atomic_units` / `matched_unique_installer_conditions` / `matched_unique_startup_conditions` / `matched_unique_feature_ids`。
  - 新增 A-F inventory-level 与 A-G source-level mutation 反证，全部通过。
  - `tools/tests/test_a13_systemui_gate_diff.py` 28 个单元测试全过，新增 DeterministicOutputTests 证明同一份源码在不同 checkout 路径生成完全一致产物。
- 当前差异判定基线：
  - `MATCH=197`, `INSTALLER_ONLY=0`, `GATE_ONLY_UNEXPLAINED=0`, `GATE_ONLY_DYNAMIC_DOMAIN=6`, `DYNAMIC_GLOBAL_ACTION_GATE=1`
  - `DEFAULT_MISMATCH=0`, `COMPARATOR_MISMATCH=0`, `COMPOSITE_CONDITION_MISMATCH=0`
  - `DOMAIN_CONTAMINATION=0`, `SEMANTIC_REVIEW_REQUIRED=0`, `FEATURE_CATALOG_GATE_UNKNOWN=0`
  - `global_action_domain.status=PARSED_SAFE`
  - `matched_atomic_units=197`, `matched_unique_installer_conditions=113`, `matched_unique_startup_conditions=111`, `matched_unique_feature_ids=8`

## 3. 变更范围核对

- Hook call site 总数：669（未新增）。
- 进程覆盖范围：未扩大。
- 未新增线程、Handler、listener、observer、锁、无界缓存或依赖（除 QA 任务要求的集成测试 fakes 外）。

## 3. 任务文件与 SHA 一致性

- P1B-0 状态 `COMPLETED` → 待更新为 `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`。
- P1B-1 已移动到 `tasks/completed/`，状态 `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`。
- P1B-2 位于 `tasks/active/` 但状态写 `COMPLETED`；终点 `fec4ee6` 不在分支历史 → 需修正。
- P1B-3 状态 `ENGINEERING_COMPLETE_DEVICE_EVIDENCE_PENDING` → 待评估是否可直接 `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING` 或先补测试。
- P1B-4A 在 `completed/` 但 QA 判定 `QA_REOPENED` → 需移回 `active/` 并修正。
- P1B-4B 在 `completed/` 但状态 `IN_PROGRESS` → 需修正为 `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`（Release 后）。
