# A13 最终项目目标

## 0. 性质与权限

本文件定义 `tomthenpc/customiuizer-a13` 的最终完成条件，不是阶段性任务说明。

除仓库所有者明确下令外，自动 Agent 不得：

- 修改本文件；
- 缩小目标；
- 降低验收标准；
- 将静态验证描述成实机验证；
- 以“编译成功”代替“项目完成”。

动态任务、发现的问题、执行顺序和证据统一写入 `TASK_STATE.md`。

---

## 1. 仓库与分支锁

唯一授权仓库：

```text
tomthenpc/customiuizer-a13
```

允许识别以下等价远程地址：

```text
https://github.com/tomthenpc/customiuizer-a13
https://github.com/tomthenpc/customiuizer-a13.git
git@github.com:tomthenpc/customiuizer-a13.git
ssh://git@github.com/tomthenpc/customiuizer-a13.git
```

唯一授权写入分支：

```text
devin/a13-rom-intelligence-audit
```

授权模式：

```text
EXACT_LOCK
```

禁止用以下模糊规则授权写入：

```text
devin/a13-*
devin/*
*a13*
任何当前 devin 分支
```

在 `PROJECT_COMPLETE` 之前：

- 只允许在 `devin/a13-rom-intelligence-audit` 工作；
- 不得新建分支；
- 不得切换到其他分支继续任务；
- 不得合并或推送 `main`；
- 不得 merge、rebase、force-push 或重写历史；
- 不得创建 tag、GitHub Release 或自动合并 PR。

达到 `PROJECT_COMPLETE` 后，Agent 也不得自动创建新分支。它必须停止并等待仓库所有者决定下一阶段。

---

## 2. 最终产品定义

A13 的最终产品是：

> 面向 MIUI 14 / Android 13，并对 HyperOS 1 / Android 13 提供契约保护的稳定、可维护、可诊断、低开销 libxposed 模块。全部生产功能具有明确身份、目标进程、安装阶段、兼容契约、结构化安装结果、资源所有权和验证证据；项目可重复构建，并具备发布候选所需的自动化与实机证据。

固定产品边界：

```text
Android 主版本：13
主要 ROM：MIUI 14 / Android 13
次要 ROM：HyperOS 1 / Android 13
ABI：arm64-v8a
applicationId：tv.withaibuild.customiuizer.r13
JDK：17
libxposed：minApiVersion 101 / targetApiVersion 102
```

不扩展到：

```text
Android 14+
HyperOS 2+
其他 applicationId
其他 Android 大版本线
```

A14 仓库仅作为只读架构参考。不得机械复制 A14 文件，不得为了形式一致破坏 A13 的 ROM、ClassLoader、API 或历史行为。

---

## 3. 最终目标

### 3.1 功能完整与行为保持

- 保留当前有效功能、设置项、默认值、重启语义与 Hook 行为。
- 不得为了测试通过而删除功能、隐藏设置或降低兼容要求。
- 每个生产功能必须可以追溯到：
  - 用户偏好或系统条件；
  - 默认值；
  - Feature identity；
  - 目标进程；
  - 安装阶段；
  - Installer owner；
  - Hook 入口；
  - 兼容契约及 variant；
  - 安装结果；
  - diagnostics ID；
  - 生命周期 owner；
  - 自动测试或实机证据。
- 不允许存在不可到达、重复安装、无人负责或文档与实现冲突的生产 Hook。
- 历史失效功能必须先审计，再分类：
  - `KEEP`
  - `KEEP_GUARDED`
  - `REPAIR`
  - `EXPERIMENTAL`
  - `FREEZE_LEGACY`
  - `DELETE_CONFIRMED_DEAD`
- `DELETE_CONFIRMED_DEAD` 必须同时具有机械证据和仓库所有者明确批准。Agent 不得自行删除用户功能。

### 3.2 统一运行期架构

最终架构必须满足：

1. `MainModule` 只负责入口、偏好 bootstrap 和进程路由。
2. 每个受支持进程或包由明确 Installer 管理。
3. 每个 Feature 有稳定、唯一、可机械检查的 identity。
4. Feature 安装统一经过：

```text
enabled 判断
→ process scope
→ install phase
→ contract / variant 解析
→ 单一安装事务
→ FeatureInstallResult
→ 单一 diagnostics owner
→ process-local idempotent state
→ bounded lifecycle cleanup
```

5. 同一 typed Feature 不得同时拥有 Registry 和旧 Dispatcher 两套生命周期。
6. 当前及后续所有 typed `FeatureId` 必须进入统一生产 Registry。
7. typed catalog 之外的全部生产 Hook 必须进入 inventory，并归类为：
   - `REGISTRY_FEATURE`
   - `INSTALLER_INFRASTRUCTURE`
   - 有理由、有测试、有退出条件的 `LEGACY_EXCEPTION`
8. 最终不允许 `UNKNOWN`、orphan preference、unreachable installer 或 duplicate owner。
9. 兼容逻辑限制在 ROM / ClassLoader / target resolver 边界，不向业务代码扩散。
10. A13 不要求与 A14 类名完全一致，但必须具备闭环能力：
    - process routing；
    - Feature identity；
    - install state；
    - callback guard；
    - receiver/observer lifecycle；
    - bounded reflection cache；
    - contract/variant resolution；
    - diagnostics；
    - lazy construction；
    - static invariants。

### 3.3 Agent 自主发现与持续修复

Agent 不能只执行固定清单。每个阶段结束后必须主动发现下一批问题，至少检查：

- 编译 warning；
- lint；
- unit test 缺口；
- static invariant；
- contract/inventory 不一致；
- `TODO`、`FIXME`、temporary workaround；
- 重复实现；
- unreachable code；
- orphan resource/preference；
- 反射失败边界；
- stale callback/owner；
- Receiver/Observer/View/Handler 生命周期；
- 重复注册；
- disabled path 成本；
- 热路径分配、Regex、重复反射和阻塞；
- APK size 变化；
- GitHub CI；
- 文档过期；
- LSPosed/实机日志中的新错误。

新发现的问题必须写入 `TASK_STATE.md`，设置优先级、证据、验证方法和依赖，然后自动进入最高优先级未阻塞任务。

### 3.4 安全 Java → Kotlin 收口

最终目标不是强制 100% Kotlin，而是：

> 所有适合迁移的业务逻辑安全迁移；所有保留 Java 的文件都有明确、可审计的边界理由。

全部生产 Java 文件必须分类：

```text
MIGRATE_TO_KOTLIN
KEEP_JAVA_JVM_BOUNDARY
KEEP_JAVA_REFLECTION_ABI
KEEP_JAVA_FRAMEWORK_ENTRY
KEEP_JAVA_GENERATED_OR_VENDOR
KEEP_JAVA_TEMPORARY_BLOCKER
```

最终禁止存在：

```text
UNCLASSIFIED
KEEP_JAVA_TEMPORARY_BLOCKER
```

迁移必须保持：

- JVM signature；
- static / companion 语义；
- overload 选择；
- visibility；
- reflection-visible name；
- ClassLoader；
- nullability；
- exception propagation；
- synchronization / volatile；
- 初始化顺序；
- callback capture；
- resource lifetime；
- Hook 时机和结果。

Kotlin 只能用于降低真实复杂度、状态错误或不必要开销。禁止为了缩短代码引入隐式控制流、过度抽象、热路径 lambda/collection 分配或隐式全局状态。

最终生成：

```text
docs/JAVA_BOUNDARY_ALLOWLIST.md
```

### 3.5 运行期安全

必须满足：

- `OutOfMemoryError`、`ThreadDeath`、`VirtualMachineError` 始终继续抛出。
- 非 fatal Feature 失败只影响对应 Feature。
- framework callback 和 deferred callback 具有外层 failure boundary。
- Receiver、Observer、Handler、Runnable、Listener、View、Bitmap、Drawable、Activity、Context、Controller 和 owner reference 有明确注册、替换、失效与释放闭环。
- 不允许 stale owner、重复 Receiver、重复 Observer、重复任务或跨 owner 重用 View。
- 不允许无显式 `Looper` 的 `Handler()`。
- 不允许 runtime path 使用 legacy `de.robv.android.xposed` API。
- API 102-only 能力不得污染 API 101 必经路径。
- 不允许 broad catch 将未知错误转换成成功或静默吞掉根因。
- fallback 必须可诊断，不能伪装成完整安装。
- Reflection cache 按 ClassLoader 隔离、有界、可安全失效。
- OOM 失败不得写入错误的 negative cache 或永久失败状态。
- 并发安装、重入、process recreation 和 owner replacement 必须有测试或结构性证明。

### 3.6 性能与资源成本

总原则：

> 功能关闭时接近零成本；功能开启时只响应真实事件；高频路径无重复反射、无阻塞、无不必要分配；兼容代码只存在于边界。

要求：

- disabled Feature 不创建业务 Hook、Receiver、Observer、Controller、线程、定时任务、循环或反射工作。
- enabled 判断在冷路径完成，不在事件热路径反复读取远程偏好。
- 热路径禁止：
  - 临时 Regex；
  - 只读参数的 args array copy；
  - 临时 List/Map/Set；
  - 重复 class/member lookup；
  - 磁盘或网络 I/O；
  - 阻塞锁；
  - 高频格式化；
  - 无节制日志；
  - 重复 Handler/Runnable；
  - 无界缓存。
- Bitmap、Drawable、View 和大型对象必须有生命周期和释放策略。
- 周期任务必须可取消、可去重，并与 owner/process 生命周期绑定。
- 性能修改必须保持行为一致，并有基线或结构性证据。
- 建立 APK size 基线；明显增长必须解释。

### 3.7 ROM intelligence 与兼容性

MIUI 14 / Android 13 是必须验证的主要目标。

HyperOS 1 / Android 13 是正式契约保护目标，但没有样本或日志时不得声称已验证。

要求：

- package/process/class/member/variant 形成可生成 inventory。
- required、optional、candidate 语义明确。
- required target 不得为了通过测试降级为 optional。
- variant 必须整体匹配，不能拼接不同 ROM 结构中的部分成员。
- target 缺失时产生结构化 compatibility result，并安全跳过。
- 样本缺失标记为 `NOT_EXERCISED` 或 `EXTERNAL_EVIDENCE_REQUIRED`。
- ROM inventory、target matrix、process matrix、Feature catalog 与实际安装路径一致。

### 3.8 测试、CI 与构建

必须建立并持续通过：

- static invariants；
- compatibility-contract checks；
- Feature identity uniqueness；
- inventory consistency；
- process/phase mismatch tests；
- disabled path tests；
- idempotency tests；
- transient/permanent failure tests；
- fatal propagation tests；
- callback/lifecycle regression tests；
- Kotlin compile；
- Java compile；
- Android unit tests；
- lint；
- Python tool tests；
- debug APK assemble；
- R8/shrinker audit；
- dead-code evidence gate；
- GitHub Actions CI。

CI 必须在唯一授权分支每次 push 后运行核心门禁。CI 失败必须被 Agent 自动读取、归因、修复并重跑，不能忽略。

正式签名配置必须位于仓库外。不得提交：

- keystore；
- password；
- token；
- `keystore.properties`；
- `local.properties`；
- `.env`；
- APK/AAB；
- 私人日志；
- 本机构建缓存。

### 3.9 文档与维护闭环

最终文档至少包括：

- 当前 runtime architecture；
- runtime invariants；
- Feature inventory；
- Hook ownership inventory；
- process matrix；
- target/contract matrix；
- ROM sample catalog；
- Java boundary allowlist；
- dead-code audit；
- performance audit；
- verification matrix；
- device regression checklist；
- release candidate report；
- known limitations；
- external evidence gaps。

可机械统计的内容优先生成，手工文档只解释无法机械推断的设计约束。禁止长期保留与当前代码冲突的旧阶段描述。

### 3.10 发布候选与实机验证

静态完成不等于发布完成。

发布候选必须具备：

- clean commit；
- 精确授权分支；
- 全部自动门禁通过；
- GitHub CI 通过；
- debug APK 可构建；
- 外部签名存在时 signed develop/release APK 可构建；
- artifact hash、version、commit SHA 可追溯；
- MIUI 14 / Android 13 实机回归；
- LSPosed 详细日志；
- SystemUI、Launcher、system_server 无新增 fatal crash；
- 关键 Feature 行为检查；
- Receiver/Observer/View lifecycle 检查；
- 性能和内存无明显回退；
- 已知限制明确。

HyperOS 1 / Android 13 无设备或 ROM 样本时，可以保留外部验证缺口，但必须明确写成 contract-guarded、尚未实机确认。

---

## 4. 完成状态

### `BASELINE_LOCKED`

- 仓库、origin、精确分支、upstream、HEAD 已记录。
- 完整基线验证已运行。
- 功能、Hook、Java/Kotlin、测试、文档、ROM 和 APK size 已盘点。
- 初始失败已分为 pre-existing、environment、network、product decision 或 new regression。

### `ARCHITECTURE_COMPLETE`

- 所有 typed Feature 使用单一 Registry 生命周期。
- 全部生产 Hook 有 identity、owner、process、phase、contract、diagnostics、lifecycle 和 inventory。
- `MainModule` 只负责路由。
- typed legacy lifecycle 为零。
- 不存在 `UNKNOWN` production hook。
- 架构文档与代码一致。

### `MACHINE_COMPLETE`

必须满足：

- `ARCHITECTURE_COMPLETE`；
- Java/Kotlin 收口完成，剩余 Java 全部进入 allowlist；
- invariants、contracts、编译、unit tests、lint、tool tests、debug assemble、R8 audit 全部通过；
- GitHub CI 通过；
- inventory/docs 与代码一致；
- 无未解释 dead code；
- 无 tracked secret/artifact；
- working tree clean；
- 本地 HEAD、upstream HEAD 一致；
- HEAD 已推送到唯一授权分支；
- 无 unfinished Git operation；
- `TASK_STATE.md` 包含最终机器证据。

### `DEVICE_VALIDATED`

- MIUI 14 / Android 13 完成真实设备回归。
- 提供真实 LSPosed 日志和行为证据。
- 关键 SystemUI、Launcher、system_server 功能通过。
- 无新增 fatal crash、重复注册或明显性能回退。
- signed RC 可追溯。

### `PROJECT_COMPLETE`

必须同时满足：

```text
MACHINE_COMPLETE
DEVICE_VALIDATED
RELEASE_CANDIDATE_RECORDED
NO_OPEN_P0
NO_OPEN_P1
DOCUMENTATION_CURRENT
```

HyperOS 1 / Android 13 若缺少外部样本，可以保留：

```text
EXTERNAL_EVIDENCE_REQUIRED
```

但不得宣传为已验证。

达到 `PROJECT_COMPLETE` 后：

- Agent 输出最终证据报告；
- Agent 停止；
- Agent 不创建新分支；
- Agent 不合并 `main`；
- 等待仓库所有者下一条指令。

---

## 5. 允许动态调整的内容

Agent 可以动态调整：

- 任务顺序；
- 批次大小；
- 实现方式；
- 测试策略；
- 诊断假设；
- 优先级；
- 新发现问题；
- `TASK_STATE.md`；
- 生成的 audit/inventory 文档；
- 代码、测试、工具、CI 和普通项目文档。

Agent 不得动态调整：

- 唯一仓库；
- 唯一分支；
- Android 13 产品边界；
- 功能保持原则；
- fatal error 规则；
- required target 规则；
- 测试/CI门禁；
- `MACHINE_COMPLETE` / `PROJECT_COMPLETE` 定义；
- secret、签名与 Git 安全规则；
- 本文件；
- `AGENTS.md`；
- `scripts/verify.ps1`。

---

## 6. 非目标

未经仓库所有者明确指令，不做：

- Android 14/15/16 支持；
- 新 UI 大改；
- 大规模新功能扩张；
- 与 A14 机械同构；
- 强制 100% Kotlin；
- 上游 reset/rebase；
- `main` 合并；
- tag/release；
- 将签名材料放入仓库；
- 无实机证据的兼容性宣传。

---

## 7. 最终验证命令

审计：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Audit
```

内循环：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Fast
```

完整机器验证：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Full
```

最终提交并推送后的严格验证：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Final
```
