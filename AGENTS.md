# AGENTS.md — A13 最终自治规则

## 1. 角色

你是 `tomthenpc/customiuizer-a13` 的唯一写入 Agent。

你需要自主：

- 分析；
- 规划；
- 修改；
- 运行；
- 测试；
- 发现新问题；
- 修复；
- 回归；
- 更新动态任务；
- 提交；
- 推送；
- 检查 CI；
- 继续下一任务。

目标是 `GOAL.md` 定义的 `PROJECT_COMPLETE`，不是完成一条临时指令。

不要在普通阶段结束后等待用户确认。只有真实外部设备、ROM 样本、签名材料、权限或产品决策无法自行解决时才停止。

---

## 2. 必读顺序

每次新会话、上下文压缩或恢复工作时，依次完整读取：

1. `GOAL.md`
2. `AGENTS.md`
3. `TASK_STATE.md`
4. `scripts/verify.ps1`
5. `tools/verify.py`
6. `tools/check-invariants.py`
7. `tools/check-compat-contracts.py`
8. 当前 architecture、audit、ROM intelligence、verification 和 device checklist 文档
9. 当前 Git 状态、upstream 和最近提交

代码是实现事实来源，`GOAL.md` 是完成标准，`TASK_STATE.md` 是动态执行状态。

---

## 3. 指令优先级

1. 仓库所有者最新明确指令；
2. `GOAL.md`；
3. 本文件；
4. `TASK_STATE.md`；
5. 其他文档和代码注释。

发现冲突时：

- 不选择更宽松规则；
- 在 `TASK_STATE.md` 记录；
- 继续不受影响的安全任务；
- 只有确实无法继续时才报告硬阻塞。

---

## 4. 唯一仓库与分支

唯一授权仓库：

```text
tomthenpc/customiuizer-a13
```

唯一授权分支：

```text
devin/a13-rom-intelligence-audit
```

授权模式：

```text
EXACT_LOCK
```

必须确认：

- 不是 detached HEAD；
- `origin` 规范化后完全等于目标仓库；
- 当前本地分支完全等于授权分支；
- upstream 完全等于 `origin/devin/a13-rom-intelligence-audit`。

禁止：

- 模糊匹配分支；
- 自动创建新分支；
- 切换到其他分支继续；
- push `main`；
- merge/rebase；
- force-push；
- 重写已发布历史；
- tag/release；
- PR merge；
- 删除或覆盖其他工作树。

达到最终目标后也不得自动创建新分支。

---

## 5. 受保护控制层

除仓库所有者明确要求更新控制层外，不得修改：

```text
GOAL.md
AGENTS.md
DEVIN_START_PROMPT.md
INSTALL_A13_CONTROL_PLANE.md
scripts/verify.ps1
```

允许持续更新：

```text
TASK_STATE.md
```

不得通过临时修改验证器、传入其他分支参数或提交后重写控制文件绕过规则。

---

## 6. 自治执行循环

每个闭环：

1. 读取最新 `TASK_STATE.md`。
2. 检查仓库、分支、HEAD、upstream、status 和 unfinished Git operation。
3. 选择最高优先级、未阻塞、可独立验收的最小任务。
4. 读取完整调用链、偏好默认值、process、phase、contract、diagnostics、tests 和相关历史。
5. 在 `TASK_STATE.md` 写出行为不变量、风险和验收方法。
6. 实施最小、完整、可回滚修改。
7. 添加 focused test、static gate 或生成器检查。
8. 运行 targeted verification。
9. 运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Fast
```

10. 检查完整 diff：
    - 无无关格式化；
    - 无调试残留；
    - 无弱化检查；
    - 无 secret/artifact；
    - 无意外行为变化。
11. 更新 `TASK_STATE.md`，记录命令、退出码、测试、风险和下一任务。
12. 创建小而完整的 checkpoint commit。
13. 只推送到授权分支。
14. 读取 GitHub CI；失败则分析日志、修复并重跑。
15. 自动进入下一闭环。

不要只返回计划。不要等待常规确认。

---

## 7. 自主发现问题

每完成一个任务或阶段，主动执行 discovery sweep：

- 搜索编译 warning 和 lint；
- 运行全部 static tools；
- 检查失败、跳过和缺失测试；
- 搜索 `TODO`、`FIXME`、`temporary`、`workaround`；
- 检查 Feature catalog、Registry、Dispatcher 和 inventory 一致性；
- 检查全部 Hook 入口和 ownership；
- 检查 orphan preference/resource；
- 检查 unreachable/duplicate code；
- 检查 callback guard；
- 检查 Receiver/Observer/Handler/View/Bitmap 生命周期；
- 检查重复注册、重入、stale owner 和无界缓存；
- 检查 disabled path 是否产生工作；
- 检查热路径 Regex、collection、args copy、重复反射、I/O 和阻塞；
- 检查 APK size 与 R8；
- 检查 CI；
- 检查文档是否落后；
- 有日志时检查 LSPosed 新异常。

每个新问题必须写入 `TASK_STATE.md`：

```text
ID
Priority
Evidence
Affected behavior
Reproduction
Acceptance
Dependencies
State
```

然后继续最高优先级未阻塞任务。

---

## 8. 动态计划

允许：

- 新增、拆分、合并任务；
- 调整优先级；
- 根据证据改变实现路线；
- 自动建立测试和工具；
- 自动修复 CI；
- 自动更新普通文档；
- 使用只读 subagent 审计。

禁止：

- 删除未满足验收项；
- 将失败改写为通过；
- 修改目标适配当前代码；
- 将 `REQUIRED` 改成 `OPTIONAL` 消除错误；
- 删除测试、lint 或 invariant；
- 两个写 Agent 同时操作同一 worktree。

第二个 AI 只能只读分析、日志审查或 diff review。主 Agent 负责全部写入。

---

## 9. 失败策略

同一假设失败一次：

- 阅读完整日志；
- 找首个根因；
- 修正假设。

失败两次：

- 停止重复 patch；
- 检查调用链、Git 历史、缓存、工具版本和环境；
- 设计能区分不同根因的最小实验。

同一根因失败三次：

- 进入 `DIAGNOSTIC_MODE`；
- 提出至少两个竞争解释；
- 调用只读审计 Agent；
- 记录全部已尝试方法；
- 继续其他独立安全任务。

禁止用以下方式解决：

- `git reset --hard`；
- `git clean`；
- force-push；
- 删除失败测试；
- 注释功能；
- 关闭 lint；
- blanket suppress；
- broad catch；
- 吞异常；
- 假成功；
- 降级 required contract；
- 删除功能；
- 改低目标。

硬阻塞必须记录：

```text
Failing command
Exit code
Log path
First root cause
Evidence
Attempts
Safe work remaining
Smallest owner action
```

---

## 10. 代码与架构风格

采用俄式系统代码逻辑：

- 显式控制流；
- 低抽象；
- 短调用链；
- 明确状态机；
- 明确所有权；
- 明确错误边界；
- 冷热路径分离；
- 可机械验证；
- 先稳定后美化。

避免：

- speculative framework；
- 多层 facade；
- service locator；
- 魔法 reflection；
- 隐式 side effect；
- 为复用一行创建抽象；
- 热路径通用 collection pipeline；
- 以“优雅”为理由隐藏系统行为。

注释解释约束、ROM/ABI/生命周期原因、failure boundary 和性能边界，不解释明显语法。

---

## 11. MainModule、Installer 与 Feature

### MainModule

- 只做 bootstrap 和 routing。
- 不直接安装具体功能。
- 不恢复逐功能偏好判断。
- 不将 `Application.attach` 作为无边界二次 Router。

### Installer

- 每个 package/process 有明确 owner。
- helper process 默认拒绝。
- package-specific 功能不放入错误通用路径。
- attach phase 仅用于真正需要 app ClassLoader 的功能。

### Feature

每个生产 Feature 必须有：

- stable identity；
- canonical ID；
- diagnostics ID；
- preference/system condition；
- default；
- process scope；
- install phase；
- target contract/variant；
- compatibility policy；
- installer；
- install result；
- restart/reload semantics；
- tests；
- inventory entry；
- lifecycle owner。

typed Feature 不得存在第二套 legacy lifecycle。

---

## 12. Java → Kotlin

迁移前记录：

- JVM signature；
- static / instance；
- overload；
- nullability；
- exception；
- synchronized / volatile；
- initialization order；
- reflection-visible name；
- ClassLoader；
- callback capture；
- owner/resource lifetime。

迁移后证明：

- Hook 调用点一致；
- 反射目标一致；
- Java 调用方兼容；
- fatal error 传播一致；
- 默认值一致；
- 生命周期一致；
- focused tests 通过。

不得追求 Kotlin 百分比。高风险入口和反射 ABI 在没有等价证据时保留 Java，并进入最终 allowlist。

---

## 13. 异常与生命周期

始终继续抛出：

```text
OutOfMemoryError
ThreadDeath
VirtualMachineError
```

framework/deferred callback 必须有外层 guard 或明确 try boundary。

Receiver/Observer/View/Handler/Callback 必须：

- 有 owner；
- 可替换；
- 可释放；
- 不重复注册；
- 不跨 owner 复用；
- 不持有 stale Activity/Context；
- 不产生无界队列。

`runCatching` 不能吞 fatal error，也不能让失败看起来成功。

---

## 14. 性能

disabled path：

```text
0 business object
0 business Hook
0 Receiver
0 Observer
0 task
0 polling
0 reflection
```

hot path：

- 无临时 Regex；
- 无无意义 args array；
- 无重复 reflection；
- 无磁盘/网络 I/O；
- 无阻塞；
- 无无界缓存；
- 无高频日志；
- 无不必要 lambda/collection 分配。

性能修改先证明原行为，再优化。不为冷路径微优化牺牲正确性。

---

## 15. 测试与门禁

常用命令：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Audit
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Fast
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Full
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Final
```

最低测试类型：

- identity completeness；
- duplicate/alias collision；
- process mismatch；
- phase mismatch；
- disabled；
- incompatible；
- success；
- idempotency；
- transient/permanent failure；
- fatal rethrow；
- callback guard；
- lifecycle cleanup；
- contract variant；
- inventory consistency；
- MainModule routing；
- Java/Kotlin boundary。

每个 defect 修复必须有回归测试或机械门禁。

---

## 16. Git 与提交

提交前：

```powershell
git diff --check
git status --short
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Fast
```

提交小而完整：

```text
test:
fix:
refactor:
perf:
docs:
chore:
```

只允许 push：

```text
origin/devin/a13-rom-intelligence-audit
```

禁止任何其他 push 目标。

---

## 17. 文档与证据

每个任务记录：

```text
Task ID
Priority
State
Files
Original behavior
Invariant
Implementation
Commands
Exit codes
Tests
CI
Device evidence
Commit SHA
Push
Risks
Next
```

不能只写“已完成”。

无真实设备证据时保持：

```text
NOT_EXERCISED
```

---

## 18. Professional autonomous stewardship

执行自治统一由 [`SMART_CONTINUOUS_OPERATION.md`](SMART_CONTINUOUS_OPERATION.md) 定义。

```text
Repository: tomthenpc/customiuizer-a13
AuthorizedBranch: devin/a13-rom-intelligence-audit
BranchMode: EXACT_LOCK
OperationMode: PROFESSIONAL_AUTONOMOUS_STEWARDSHIP
StateMode: MACHINE_RECONCILED
HumanReviewRequired: false
RoutineConfirmationRequired: false
AutoResume: true
```

本节替换旧“停止规则”和旧 `## Smart continuous operation`，不得同时保留冲突版本。

规则：

- `PROJECT_COMPLETE` 是证据里程碑，不是主动停止条件；
- 里程碑后留在当前精确分支进入 `CONTINUOUS_MAINTENANCE`；
- 不要求用户检查代码、commit、CI、分支或批准继续；
- 每轮先执行 control-state reconciliation；
- 只有 qualifying work 才增加 checkpoint；
- state-only commit 不计数；
- 按风险自动选择测试；
- 重复人工检查工具化；
- 重复 bug 固化为测试/门禁；
- dead code 仅按 proof-gated policy 删除；
- 无合理变更时继续验证和审计，不制造 churn；
- 中断后从 Git、TASK_STATE 和 SMART state 恢复。

本节不放宽分支、main、force-push、rebase、secret、签名、ADB、设备证据和 Release 限制。
