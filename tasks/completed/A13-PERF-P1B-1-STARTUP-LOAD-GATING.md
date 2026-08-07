# A13-PERF-P1B-1 — SystemUI and Launcher Startup Load Gating

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-P1B-1-STARTUP-LOAD-GATING` |
| 分支 | `devin/a13-memory-performance-optimization` |
| P0 真实运行时基线 | `RUNTIME_BASELINE_PENDING_DEVICE` |
| 授权范围 | 仅启动加载路径优化；禁止进入一般 Hook 热路径、AudioService、Tile、通知菜单、Launcher 手势、重复 Hook 合并、生命周期监听注销、UI 页面、PackagePermissions、构建系统、R8、新功能 |
| 状态 | `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING` |
| Base SHA | `cd89c38f02834db5baf24e5ab08b345c187085fe` |
| Original engineering SHA | `44b4c4c7dd642bbe3de9e2ba3735872c1c445115` |
| Original closure SHA | `3d38cdd53a6190c68187a803badaf201dfda25cd` |
| R1-B2 audit SHA | `442ade1c5b780aead006b542775111f61499cf0b` |
| R1-B2 proof-hardening SHA | `a36b5a2f23e0f7e04d86abde179fdaaf7dbb115f` |
| QA documentation checkpoint | `0722912e6bc7c7f7bb7e45172ac56355a591ebeb` |
| Final QA closure SHA | `58d0defeada15ef50d5c12d3933aa5fbcf7fbbb4` |

## 目标

在不改变现有功能效果、默认行为和偏好生效语义的前提下，减少以下启动路径在无相关功能启用时产生的无效工作：

1. `SystemUIApplication#onCreate` 对应的 SystemUI 功能加载路径。
2. `Launcher Application#attach` 对应的 Launcher 功能加载路径。

本任务只处理启动阶段的功能族门控、门控前不必要的类引用/注册器/dispatcher/反射/监听器，以及功能族全部关闭时仍执行的初始化。

## R1-B2 差异审计强化

R1-B2 为 P1B-1 启动门控提供结构对账与反证能力：

- `442ade1c5b780aead006b542775111f61499cf0b` 创建原始 `tools/a13_systemui_gate_diff.py`。
- `a36b5a2f23e0f7e04d86abde179fdaaf7dbb115f` 完成第一轮 proof hardening：
  - 核心审计入口 `diff_from_repo(repo_root)` 化，删除全局 `REPO_ROOT` 依赖，
    支持临时仓库和单元测试。
  - `isSystemUiGlobalActionKey()` 解析失败关闭：`status` 必为
    `PARSED_SAFE` / `PARSED_CONTAMINATED` / `UNKNOWN`；后两者产生
    `SEMANTIC_REVIEW_REQUIRED` (`BLOCKER`)。
  - `DiffResult` 新增 `matched_atomic_units`、`matched_unique_installer_conditions`、
    `matched_unique_startup_conditions`、`matched_unique_feature_ids` 统计。
  - 新增 A-F 清单级 mutation 与 A-G 源码级 mutation。
  - 新增 `tools/tests/test_a13_systemui_gate_diff.py`。
- 本轮最终 QA closure 额外完成：
  - `diff_from_repo` provenance 彻底移除 `repo_root` 绝对路径，产物不再包含
    Windows drive、用户名、checkout 目录等机器相关路径。
  - 新增 DeterministicOutputTests：两份内容完全相同的源码放在不同绝对路径下
    生成的 `to_dict()` 与 `render_markdown()` 完全一致。
  - Markdown 新增 `## Match Coverage` 章节。
  - 新增 `MutationIntegrationTests` 让 unittest 直接驱动 A-F/A-G mutation suites。

当前差异判定基线（最终 closure 工作区）：

- `MATCH=197`, `GATE_ONLY_UNEXPLAINED=0`, `INSTALLER_ONLY=0`
- `DEFAULT_MISMATCH=0`, `COMPARATOR_MISMATCH=0`, `COMPOSITE_CONDITION_MISMATCH=0`
- `DOMAIN_CONTAMINATION=0`, `SEMANTIC_REVIEW_REQUIRED=0`, `FEATURE_CATALOG_GATE_UNKNOWN=0`
- `global_action_domain.status=PARSED_SAFE`
- `matched_atomic_units=197`, `matched_unique_installer_conditions=113`,
  `matched_unique_startup_conditions=111`, `matched_unique_feature_ids=8`

## 禁止项

- 一般 Hook 回调热路径优化。
- AudioService。
- Quick Settings Tile 创建热路径。
- 通知菜单构建热路径。
- Launcher 手势点击或滑动回调。
- 重复 Hook 合并。
- 生命周期监听注销。
- UI 页面性能。
- PackagePermissions 修改（仅可标记为 `MANDATORY_ALWAYS_ON_FEATURE`）。
- 构建系统、签名配置、R8 规则修改。
- 新功能或新设置。

## 验收标准

- SystemUI 相关功能全部关闭时，可避免的功能初始化不再执行。
- Launcher 相关功能全部关闭时，可避免的功能初始化不再执行。
- 功能开启时原 Hook 和初始化路径保持完整。
- 默认行为不变。
- 偏好动态生效语义不变（需要重启的才允许启动时门控；支持即时开启的不得破坏）。
- 不相关功能类不在门控前引用。
- 不新增 Hook。
- 不扩大进程或包覆盖范围。
- 不新增线程、Handler、监听器、observer、缓存或依赖。
- 静态扫描稳定。
- 全部单元测试和 `python tools/verify.py full` 通过。
- Release 编译、R8 和 `lintVitalRelease` 通过；`packageRelease` 因签名配置缺失失败需单独报告。
- 工作区干净，提交已推送。
- 不声称未经真机测量的性能收益。
