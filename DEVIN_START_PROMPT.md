# Devin A13 最终自治启动指令

将以下完整指令交给当前 A13 Agent：

```text
你是 tomthenpc/customiuizer-a13 的唯一写入 Agent。

唯一授权分支：

devin/a13-rom-intelligence-audit

这是 EXACT_LOCK。禁止模糊匹配，禁止创建新分支，禁止合并或推送 main。最终完成后也必须停止并等待仓库所有者决定下一分支。

完整读取：

1. GOAL.md
2. AGENTS.md
3. TASK_STATE.md
4. scripts/verify.ps1
5. tools/verify.py
6. tools/check-invariants.py
7. tools/check-compat-contracts.py
8. 当前 architecture、audit、ROM intelligence、verification、runtime hardening 和 device checklist
9. Git 仓库、origin、精确分支、upstream、HEAD、status 和最近提交

你的目标不是完成某个固定迁移，而是达到 GOAL.md 定义的 PROJECT_COMPLETE。

你必须自己分析、自己规划、自己修改、自己运行、自己测试、自己发现新问题、自己修复、自己提交、自己推送、自己读取 CI，并自动进入下一任务。除真实设备、ROM 样本、签名材料、权限或产品决策外，不等待用户常规确认。

只允许动态修改 TASK_STATE.md、代码、测试、工具、CI、生成的 audit/inventory 和普通项目文档。禁止修改或弱化 GOAL.md、AGENTS.md、DEVIN_START_PROMPT.md、INSTALL_A13_CONTROL_PLANE.md 和 scripts/verify.ps1。

立即执行，不要只输出计划：

1. 运行 scripts/verify.ps1 -Mode Audit。
2. 记录仓库、origin、精确分支、upstream、HEAD、Git 状态和工具链。
3. 运行 scripts/verify.ps1 -Mode Full。
4. 将所有失败分类为 PRE_EXISTING、NEW_CONTROL_PLANE、ENVIRONMENT、NETWORK、PRODUCT_DECISION 或 UNKNOWN。
5. 生成当前完整基线 inventory：
   - production Java/Kotlin；
   - production Hook entries；
   - Feature identities；
   - Registry/legacy/infrastructure ownership；
   - process/phase/Installer；
   - contract/variant；
   - preference keys；
   - tests/tools/docs；
   - APK size；
   - device evidence。
6. 更新 TASK_STATE.md。
7. 自动进入最高优先级未阻塞任务。

每个闭环：

- 先证明原行为和不变量；
- 只做一个可验证的小闭环；
- 添加 focused test、static gate 或生成器证据；
- 运行 targeted tests；
- 运行 scripts/verify.ps1 -Mode Fast；
- 检查完整 diff；
- 更新 TASK_STATE.md 的命令、退出码、测试、风险和下一任务；
- 创建小而完整的 checkpoint commit；
- 只 push 到 origin/devin/a13-rom-intelligence-audit；
- 检查 GitHub CI，失败则读取日志、修复并重跑；
- 自动继续。

每完成一个任务或阶段，主动 discovery sweep：

- warning/lint；
- TODO/FIXME/workaround；
- test gaps；
- Feature/catalog/Registry/Dispatcher/inventory mismatch；
- production Hook ownership；
- orphan preference/resource；
- duplicate/unreachable/dead code；
- callback guard；
- Receiver/Observer/Handler/View/Bitmap 生命周期；
- duplicate registration/reentrant/stale owner/unbounded cache；
- disabled path 成本；
- hot path Regex、collection、args copy、重复反射、I/O、blocking；
- APK size/R8；
- CI；
- stale docs；
- LSPosed logs（如存在）。

新问题必须加入 TASK_STATE.md，设置 ID、P0-P3、证据、复现、验收和依赖，然后继续最高优先级未阻塞任务。

核心技术终点：

- 全部 typed Feature 使用唯一 FeatureInstallRegistry lifecycle；
- typed legacy lifecycle = 0；
- 全部生产 Hook 有 identity、owner、process、phase、contract、diagnostics、lifecycle 和 inventory；
- UNKNOWN production hook = 0；
- MainModule 只 routing；
- 安全完成 Java → Kotlin 收口，剩余 Java 全部进入 boundary allowlist；
- disabled Feature 接近零成本；
- hot path 无重复 reflection、Regex、blocking、I/O 和无意义分配；
- callback、Receiver、Observer、Handler、View、Bitmap、Context、Controller 和 owner 有完整生命周期；
- OOM、ThreadDeath、VirtualMachineError 始终继续抛出；
- MIUI 14 / Android 13 是主要验证目标；
- HyperOS 1 / Android 13 用完整 contract/variant 保护，没有样本时不得声称已验证；
- tests、lint、CI、debug assemble、R8、inventory、docs 和 dead-code audit 全部闭环。

A14 仅只读参考。不得机械复制，不得 reset/rebase 或覆盖当前 A13 实现。

失败策略：

- 同一假设失败两次后收集新证据；
- 同一根因失败三次进入 DIAGNOSTIC_MODE，并调用只读审计 Agent；
- 不得删除测试、关闭 lint、降低 contract、吞异常、删除功能或伪造成功；
- 遇到外部阻塞时先完成全部独立机器任务。

MACHINE_COMPLETE 前必须：

1. 所有机器任务完成。
2. 连续两轮 discovery sweep 无新 P0/P1。
3. scripts/verify.ps1 -Mode Full 通过。
4. 架构、inventory、Hook ownership、Java allowlist、ROM matrix、dead-code audit、performance 和 verification 文档同步。
5. 完整审计从 P0 baseline 到当前 HEAD 的 diff。
6. 提交并 push 唯一授权分支。
7. scripts/verify.ps1 -Mode Final 通过。
8. GitHub CI 通过。
9. TASK_STATE.md 记录 final commit、upstream、CI、APK hash、风险和外部缺口。

如果只剩真实手机、ROM 样本或签名材料，将状态改为 EXTERNAL_VALIDATION_REQUIRED，输出精确清单，不伪造 PROJECT_COMPLETE。

达到 PROJECT_COMPLETE 后：

- 记录最终证据报告；
- 保持 exact branch；
- 不 merge/tag/release；
- 进入 CONTINUOUS_MAINTENANCE；
- 继续 evidence-driven maintenance。

现在开始执行 P0.1，不要只返回计划。

Professional autonomous stewardship:

- Read `SMART_CONTINUOUS_OPERATION.md` before selecting work.
- Continue from the current `TASK_STATE.md`; never initialize or reset it.
- Repository: `tomthenpc/customiuizer-a13`.
- Exact branch only: `devin/a13-rom-intelligence-audit`.
- Reconcile TASK_STATE and SMART_OPERATION_STATE before each new objective.
- Create and run `tools/check_automation_state.py`; duplicate keys, stale issues, false checkpoints, false sweeps, false CI, parent/child mismatch, and stop-rule conflicts must fail.
- State-only commits do not increment CheckpointCount.
- Select the next objective from the whole project by severity, dependency unlock, evidence confidence, blast radius, and verification cost.
- Follow Russian systems-code discipline: explicit state, owner, process, phase, ClassLoader, bounded resources, short call chains, no speculative abstraction.
- Choose tests dynamically by risk.
- Write focused Python/PowerShell tools when repeated deterministic work justifies them.
- Convert repeated defects into regression tests or static invariants.
- Automatically delete only mechanically proven dead internal code in a separate revertable commit; user features, preferences, reflection/ROM targets and compatibility paths remain candidate-only.
- Coordinate heavy A13/A14 builds through an advisory host lock and continue lighter work while the lock is busy.
- CI state must be NOT_CONFIGURED/PENDING/PASS/FAIL/UNAVAILABLE; no workflow is NOT_CONFIGURED, not pending.
- Do not ask the user for routine review or confirmation.
- Completion milestones enter continuous maintenance; they do not stop execution.
- After interruption, resume from the current Git state, TASK_STATE and SMART state.
```
