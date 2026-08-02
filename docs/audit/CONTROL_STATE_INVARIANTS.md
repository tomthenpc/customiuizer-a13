# A13 Control-State Invariants

## Required files

```text
TASK_STATE.md
SMART_OPERATION_STATE.md
SMART_CONTINUOUS_OPERATION.md
```

## SMART_OPERATION_STATE unique keys

```text
Mode
CheckpointCount
CheckpointsSinceStandardSweep
CheckpointsSinceDeepSweep
LastQualifyingCheckpoint
LastLightSweepCommit
LastStandardSweepCommit
LastDeepSweepCommit
LastFullVerificationCommit
LastCIState
LastCleanupCommit
LastToolCreated
LastFailureClass
CurrentObjective
ResumeTask
```

每个 key 只能出现一次。

## Qualifying checkpoint

不计数：

- 只改 SMART state；
- 只改计数；
- 只改“当前正在工作”文字；
- 无测试/验证的格式化；
- 覆盖层安装 commit。

计数：

- 独立业务修复；
- 测试/门禁闭环；
- 工具闭环；
- 实际 sweep；
- 有源码一致性证据的文档/inventory。

## Parent/child state

- 所有 required child COMPLETE，parent 不得仍 IN_PROGRESS。
- 任一 required child TODO/IN_PROGRESS，parent 不得 COMPLETE。
- BLOCKED_EXTERNAL 只影响对应 child。
- 问题队列必须和 phase 正文同步。

## CI

```text
NOT_CONFIGURED
PENDING
PASS
FAIL
UNAVAILABLE
```

无 workflow = `NOT_CONFIGURED`。

## Sweep

只有实际执行并记录命令、退出码、证据时才能填写 LastStandard/Deep。

Standard 要求 Full 时，LastFullVerificationCommit 必须同步。

## Completion

旧的“PROJECT_COMPLETE 后停止/等待”必须替换为：

```text
record milestone
remain on exact branch
enter CONTINUOUS_MAINTENANCE
continue evidence-driven work
```

## Current project reconciliation

- `ResumeTask` 必须从当前证据派生为 P2，而不是重新执行 P0。
- BASELINE-001、VERIFY-001 根据 P0 证据改为 `COMPLETE`。
- ARCH-001 保持 `IN_PROGRESS` 并关联 P2。
- DEVICE-001 保持 `BLOCKED_EXTERNAL`。
- Checkpoint 节从 Git 历史选择最后一个真实业务/测试 qualifying checkpoint；智能覆盖层安装 commit 不算业务 checkpoint。
- 若覆盖层合入后尚无 qualifying checkpoint，CheckpointCount 保持 0，这是正确状态，不得为了数字增长创建状态提交。
