# A13 Document Update Plan v4

```text
DocumentKind: PLAN
Product: A13
Repository: tomthenpc/customiuizer-a13
Branch: devin/a13-rom-intelligence-audit
EvidenceCommit: 182ee03a209da3405d723a8ce6bc7622b217df57
EvidenceState: STATIC
GeneratedBy: agent
SourceOfTruth: A13_HOT_MERGE_PROMPT_V4.md
DeviceEvidence: NOT_EXERCISED
```

## 更新

### `docs/audit/A13_A14_ARCHITECTURE_PARITY.md`

- 当前 A14 分支；
- 当前 evidence commits；
- Registry/InstallPhase/diagnostics 事实；
- intentional difference 与 missing 分离。

### `docs/audit/A13_DEAD_CODE_CANDIDATES.md`

- P2 后重新生成；
- 用户功能、内部 adapter、不可达 private helper 分开；
- 删除必须有 Full 证据。

### `A13_BASELINE_INVENTORY.md`

标记 `SNAPSHOT`，冻结 P0 commit，不再写 current 数字。

## 新建

```text
docs/DOCUMENT_INDEX.md
docs/architecture/A13_RUNTIME_ARCHITECTURE_CURRENT.md
docs/audit/A13_CURRENT_INVENTORY.md
docs/audit/A13_CURRENT_INVENTORY.json
docs/audit/A13_BASELINE_TO_CURRENT_DELTA.md
docs/audit/A13_BASELINE_TO_CURRENT_DELTA.json
docs/audit/A13_PROJECT_HEALTH_CURRENT.md
docs/audit/A13_PROJECT_HEALTH_CURRENT.json
```

## TASK_STATE

- 基于最终 Full 决定 P2；
- 修正 stale issues；
- 机器可解析 checkpoint 表；
- P14 改为 continuous maintenance；
- CurrentObjective=P3。

## SMART state

```text
Mode: PROFESSIONAL_AUTONOMOUS_STEWARDSHIP
LastQualifyingCheckpoint:
CurrentObjective:
ResumeTask:
LastCIState: NOT_CONFIGURED
```

历史 checkpoint count 必须机械重算，不能猜。
