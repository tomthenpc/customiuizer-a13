# A13 Full Review v4

```text
DocumentKind: SNAPSHOT
Product: A13
Repository: tomthenpc/customiuizer-a13
Branch: devin/a13-rom-intelligence-audit
EvidenceCommit: 182ee03a209da3405d723a8ce6bc7622b217df57
EvidenceState: REMOTE_STATIC_PLUS_RECORDED_BUILD
DeviceEvidence: NOT_EXERCISED
AuditTime: 2026-08-03T02:00:00+08:00
AheadOfMain: 0
```

## 完成项

| Phase | 台账 | 复核 |
|---|---|---|
| P0 | COMPLETE | 有分支、工具链、Full 记录 |
| P1 | COMPLETE | identity/inventory/Hook ownership 已有机械测试 |
| P2 | COMPLETE | Registry 路由改动真实；远程 HEAD 门禁通过 |
| P3 | IN_PROGRESS | Hook 所有权收口持续进行 |
| P4-P12 | TODO | 尚未完成 |
| P13 | BLOCKED_EXTERNAL | 正确 |
| P14 | TODO | 正确 |

## 控制状态

```text
Mode: PROFESSIONAL_AUTONOMOUS_STEWARDSHIP
CheckpointCount: 5
LastQualifyingCheckpoint: 182ee03a209da3405d723a8ce6bc7622b217df57
CurrentObjective: P3 — 全部生产 Hook 收口
ResumeTask: P3.2 继续迁移可归类 legacy hook；准备 P3.3 登记跨 process LEGACY_EXCEPTION
```

`TASK_STATE.md` 中 checkpoint 表已机械对齐，stale issue 已 reconcile 为 COMPLETE。

## 算法缺口

### A13-ALG-001：O(n) 查找和列表分配

`FeatureDispatcher.install()` 当前每次通过 `FeatureCatalog.specs()` 创建合并列表并线性查找。

目标：

```text
allSpecsInternal
specByCanonicalIdInternal
```

使用 O(1) 查找。

### A13-ALG-002：旧 dispatcher 路径仍存在

公共路径已走 Registry，但大量 private `installXxx`、`installWithContract`、`installWithLegacyCheck` 仍保留。

零引用证明后单独删除，并增加静态不变量。

### A13-ALG-003：catalog 命名与 P2 冲突

`legacySpecsInternal` / `registrySpecs()` 的命名与 P2 宣称的全量 Registry 路由语义不一致。

目标：重命名为事实名称，缓存 all specs，更新 probe/test/comment。

### A13-ALG-004：错误进程也会先评估 condition

Registry 当前先执行 preference condition，再验证 scope/phase/process。

建议顺序：

```text
lookup
→ scope/phase/process
→ condition
→ REQUESTED
→ claim
→ compatibility
→ install
```

### A13-ALG-005：registerAll 非批次事务

后续 spec 冲突时，前面已注册的 spec 会留下。

目标：整批 preflight、一次性 commit 和 late-collision rollback test。

### A13-ALG-006：异常分类器是装饰性分支

`classifyThrownException` 所有分支都返回 transient。应简化为显式保守策略，或建立真实分类矩阵。

## 文档不足

1. `A13_A14_ARCHITECTURE_PARITY.md` 仍可能引用旧 A14 分支。
2. `A13_DEAD_CODE_CANDIDATES.md` 需按当前真实不可达路径重新生成。
3. `A13_BASELINE_INVENTORY.md` 是 P0 snapshot，不是 current。
4. 缺少唯一 CURRENT runtime architecture。
5. 缺少 baseline-to-current delta 和项目健康快照。

## 推荐顺序

```text
状态与最终 Full
→ O(1) catalog lookup
→ 删除 dispatcher 旧路径
→ transactional registerAll
→ Registry pipeline 顺序
→ current architecture/inventory/dead code
→ P3 Hook 收口
→ CI
```
