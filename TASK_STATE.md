# A13 最终自治任务状态

## 0. 控制状态

```text
OverallState: BASELINE_LOCKED
CompletionTarget: PROJECT_COMPLETE
Repository: tomthenpc/customiuizer-a13
AuthorizedBranch: devin/a13-rom-intelligence-audit
BranchMode: EXACT_LOCK
MainMergeAllowed: false
NewBranchAllowed: false
ReleaseAllowed: false
DeviceEvidence: NOT_EXERCISED
HardBlocker: NONE
```

本文件是唯一动态执行台账。Agent 可以持续修改，但不得删除未满足的最终验收项。

---

## 1. 初始评估快照

Agent 必须在 P0 使用当前 HEAD 重新验证，不能把本节当最终事实。

当前外部观察：

- 产品面向 MIUI 14 / Android 13，并对 HyperOS 1 / Android 13 使用 Contract/Resolver 保护。
- 当前分支已经具备 process scope、dedicated installers、Feature identity、Registry/legacy catalog split、compatibility contracts、diagnostics、ROM intelligence tools 和大量架构测试。
- typed Feature catalog 当前包含 25 个 `FeatureId`。
- Registry 与 legacy catalog/dispatcher 路径仍并存。
- 部分架构文档落后于当前实现。
- 现有完整验证入口为 `tools/verify.py full`。
- 实机验证仍属于外部证据。

---

## 2. 状态值

```text
TODO
IN_PROGRESS
BLOCKED_INTERNAL
BLOCKED_EXTERNAL
VERIFIED_STATIC
VERIFIED_BUILD
VERIFIED_CI
VERIFIED_DEVICE
COMPLETE
```

优先级：

```text
P0：系统进程崩溃、数据损坏、错误发布、分支污染
P1：功能失效、兼容错误、生命周期泄漏、明显性能回退
P2：架构债务、测试缺口、文档不一致、可维护性问题
P3：低风险清理与体验改进
```

---

## 3. 每个闭环证据模板

```text
Task:
Priority:
State:
Baseline commit:
Files:
Original behavior:
Invariant:
Implementation:
Commands:
Exit codes:
Tests:
CI:
Device evidence:
Commit:
Push:
Risks:
Next:
```

---

# P0 — 锁定真实基线

## P0.1 仓库、分支与 Git

State: `COMPLETE`

记录：

```text
git rev-parse --show-toplevel           -> C:/Users/tv/Downloads/Peengeek/customiuizer-a13-forDevin
git remote get-url origin               -> https://github.com/tomthenpc/customiuizer-a13.git
git symbolic-ref --short HEAD           -> devin/a13-rom-intelligence-audit
git rev-parse HEAD                      -> 2e4e6fad56be816d2165f0135a08b93a8b949e90
git status --short                      -> (clean)
git log -5 --oneline                    -> 2e4e6fa chore: install final A13 autonomous control plane
                                            eac3d0f Harden FeatureInstallRegistry: atomic claims, isolated conditions, explicit compatibility policies, split catalog lists and mechanical single-path invariants.
                                            c62de4c refactor(catalog): migrate second batch canaries to FeatureInstallRegistry
                                            789b3cc fix(runtime): harden registry boundaries before second canary batch
                                            649b4c0 refactor(catalog): migrate statusBarClockTweak and autoBrightnessRange through registry
git rev-parse --abbrev-ref --symbolic-full-name '@{u}'
                                        -> origin/devin/a13-rom-intelligence-audit
```

 unfinished Git markers: 无

验收：

- 仓库规范化后完全一致；
- 分支精确匹配；
- upstream 精确匹配；
- 无 detached HEAD；
- 无 unfinished merge/rebase/cherry-pick/revert；
- 当前本地修改全部分类且不丢失。

## P0.2 工具链

State: `COMPLETE`

记录：

| 项 | 值 |
|---|---|
| OS | Windows (PowerShell) |
| JDK | 17.0.12 (Oracle) |
| Python | 3.14.3 |
| Gradle | 8.9 (wrapper) |
| Android SDK | C:\\Users\\tv\\Downloads\\Peengeek\\.tools\\android-sdk |
| Git | 2.55.0.windows.3 |
| RAM | ~13.7 GB |
| 磁盘 | 113.9 GB / 511.0 GB 剩余 |
| 网络依赖 | Gradle 依赖已解析，GitHub push 成功 |
| GitHub/CI | origin 可达，分支存在并可推送 |

## P0.3 完整基线验证

State: `COMPLETE`

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Full
```

退出码：0

结果：

```text
- check-invariants: OK
- check-compat-contracts: OK
- compileDebugKotlin: OK
- compileDebugJavaWithJavac: OK
- testDebugUnitTest-all: OK (131 tests)
- lintDebug: OK
- assembleDebug: BUILD SUCCESSFUL
- A13 VERIFICATION PASSED
```

失败分类：无（全部通过）。

未知基线：无。

## P0.4 生成完整基线 inventory

State: `COMPLETE`

生成：

| 项 | 来源/文件 | 状态 |
|---|---|---|
| production Java/Kotlin | `docs/audit/A13_BASELINE_INVENTORY.md` | 94 文件（22 Java / 72 Kotlin） |
| production Hook entries | `docs/audit/A13_BASELINE_INVENTORY.md` | 627 `ModuleHelper.*` hook-helper matches；25 `FeatureDispatcher.install*`；16 `FeatureInstallRegistry` |
| Feature IDs / canonical IDs / diagnostics IDs | `FeatureCatalog.kt` / `FeatureId.kt` / `DiagnosticIds.kt` | 25 typed `FeatureId` |
| Registry / legacy / infrastructure ownership | `docs/audit/A13_BASELINE_INVENTORY.md` | 8 registry / 17 legacy / 13 installer-infrastructure |
| process / phase / Installer | `docs/rom-intelligence/A13_PROCESS_MATRIX.md` | 已存在 |
| contract / variant | `CanaryContracts.kt` / `CatalogContracts.kt` | 8 Canary / 17 Catalog；`autoBrightnessRange` 有 variant |
| preference keys | `app/src/main/res/xml/prefs_*.xml` | 628 unique / 659 total |
| tests | `app/src/test` / `tools/tests` | 57 Java/Kotlin tests + 16 Python tool tests |
| static tools | `tools/` | `verify.py`, `check-invariants.py`, `check-compat-contracts.py` 等 |
| docs freshness | `docs/` | 18 doc files；无过期 |
| APK size | `app/build/outputs/apk/debug/CustoMIUIzer-A13-r13.9.2-debug.apk` | 12,336,006 bytes |
| device evidence | N/A | `NOT_EXERCISED` |

基线 inventory 文件：

```text
docs/audit/A13_BASELINE_INVENTORY.md
```

命令与证据：

```text
- powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Fast
  -> A13 VERIFICATION PASSED (exit 0)
- git diff --check
  -> exit 0
- git commit -m "docs: baseline inventory and P0.4 completion"
  -> 8e9d500
- git push origin devin/a13-rom-intelligence-audit
  -> pushed 8e9d500
- CI: 无仓库级 CI workflow；仅本地 verify
```

完成后将 `OverallState` 更新为 `BASELINE_LOCKED`。

---

# P1 — 单一事实源

State: `COMPLETE`

## P1.1 Feature identity

State: `COMPLETE`

不变量：

- `FeatureId` enum canonical ID 与 `FeatureCatalog.specs().id` 双向一一对应；
- `FeatureId.fromString` 可解析所有 canonical/alias 形式；
- canonical ID 和 `FeatureIdentity.normalizeLookupId` 后 ID 均唯一；
- alias 不与其它 canonical ID 或 alias 冲突；
- `diagnosticId` 非空、唯一，且已在 `DiagnosticIds` 中声明；
- `CONTRACT_REQUIRED` 必须有 `contract`；
- `FeatureCatalog` registry/legacy 并集无重叠；
- `FeatureInstallRegistry.registerAll(FeatureCatalog.specs())` 不抛出冲突。

实现：

- 新增 `FeatureIdentityCompletenessTest.kt`（`app/src/test/.../mods/catalog/`），覆盖上述不变量。

验证：

```text
- .\gradlew.bat :app:testDebugUnitTest --tests "tv.withaibuild.customiuizer.mods.catalog.FeatureIdentityCompletenessTest"
  -> BUILD SUCCESSFUL (10 tests, 0 failed, 0 skipped)
- powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Fast
  -> A13 VERIFICATION PASSED (exit 0)
- powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Full
  -> A13 VERIFICATION PASSED (exit 0)
- CI: 无仓库级 CI workflow；仅本地 verify
- Device evidence: NOT_EXERCISED
```

验收：

- canonical ID 唯一；
- normalized ID 唯一；
- alias 无冲突；
- diagnostics ID 无未解释冲突；
- enum/catalog/registry/dispatcher/inventory 一致；
- 新 Feature 缺字段时测试失败。

## P1.2 完整 Feature inventory

State: `COMPLETE`

字段：

```text
Feature identity
Preference/system condition
Default
Process
Phase
Installer
Hook entry
Contract
Variant
Diagnostics
Restart/reload
Registry state
Tests
Device evidence
```

不变量：

- `A13_PROCESS_MATRIX.md` 必须包含 `FeatureCatalog` 中全部 25 个 catalog feature canonical id；
- 过程矩阵每一行必须保持 15 列；
- 新增 catalog feature 必须同步更新过程矩阵，否则 `test_feature_inventory.py` 失败。

实现：

- 新增 `tools/tests/test_feature_inventory.py`，机械校验 `docs/rom-intelligence/A13_PROCESS_MATRIX.md` 与 `FeatureCatalog.kt` 一致性。

验证：

```text
- python -m unittest tools.tests.test_feature_inventory
  -> OK (1 test)
- powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Full
  -> A13 VERIFICATION PASSED (exit 0), Python tool tests 132
- CI: 无仓库级 CI workflow；仅本地 verify
- Device evidence: NOT_EXERCISED
```

## P1.3 Hook ownership inventory

State: `COMPLETE`

所有生产 Hook 归类：

```text
REGISTRY_FEATURE
INSTALLER_INFRASTRUCTURE
LEGACY_EXCEPTION
DEAD_CANDIDATE
UNKNOWN
```

不变量：

- 所有 `ModuleHelper.findAndHookMethod` / `hookAllConstructors` / `hookAllMethods` 调用点必须落入 `A13_HOOK_OWNERSHIP_INVENTORY.md`；
- `UNKNOWN` 必须为 0；
- source 与 inventory 总调用数一致；
- inventory 中文件必须在 source 中存在，反之亦然。

实现：

- 新增 `docs/audit/A13_HOOK_OWNERSHIP_INVENTORY.md`，包含 41 个文件、630 个调用点、分类、进程和说明；
- 新增 `tools/tests/test_hook_ownership_inventory.py` 机械校验 source 与 inventory 一致性。

分类结果：

```text
- REGISTRY_FEATURE:     8 files / 163 calls (~25.9 %)
- INSTALLER_INFRASTRUCTURE: 6 files / 7 calls (~1.1 %)
- LEGACY_EXCEPTION:    27 files / 460 calls (~73.0 %)
- DEAD_CANDIDATE:      0
- UNKNOWN:             0
```

验证：

```text
- python -m unittest tools.tests.test_hook_ownership_inventory
  -> OK (2 tests)
- powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Full
  -> A13 VERIFICATION PASSED (exit 0), Python tool tests 134
- CI: 无仓库级 CI workflow；仅本地 verify
- Device evidence: NOT_EXERCISED
```

最终 `UNKNOWN = 0`。

---

# P2 — typed Feature Registry 全量收口

State: `COMPLETE`

目标：

- 全部 typed `FeatureId` 进入生产 Registry；
- typed legacy lifecycle 为零；
- Registry 为唯一安装入口；
- diagnostics 单一 owner；
- process-local idempotent state；
- fatal error 继续抛出。

实现：

- `FeatureDispatcher.install` 全部路由到 `FeatureInstallRegistry`；
- `FeatureCatalog` 为 17 个 legacy `FeatureSpec` 补齐 `processScope` / `installPhase`；
- 新增 `legacyInstall` helper，将 legacy installer 包裹在 `HookInstaller.withSession` 中，生成 `InstallSummary`；
- `FeatureInstallRegistry.installationOutcome` 从 `FeatureInstallResult.Installed.installSummary` 读取真实 `InstallOutcome` / `ReasonCode`，保留 `DEGRADED` / `DISPATCHED`；
- `CatalogBatch1/2/3`、`FeatureDispatcherRegressionTest` 的 `setUp` 增加 `FeatureInstallRegistry.clearStatesForTesting()`，保证测试隔离。

文件：

```text
app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt
app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureDispatcher.kt
app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureInstallRegistry.kt
app/src/test/java/tv/withaibuild/customiuizer/mods/catalog/CatalogBatch1Test.kt
app/src/test/java/tv/withaibuild/customiuizer/mods/catalog/CatalogBatch2Test.kt
app/src/test/java/tv/withaibuild/customiuizer/mods/catalog/CatalogBatch3Test.kt
app/src/test/java/tv/withaibuild/customiuizer/mods/catalog/FeatureDispatcherRegressionTest.kt
```

验证：

```text
- .\gradlew.bat :app:compileDebugKotlin
  -> BUILD SUCCESSFUL
- .\gradlew.bat :app:testDebugUnitTest
  -> BUILD SUCCESSFUL
- powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Fast
  -> A13 VERIFICATION PASSED (exit 0)
- powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Full
  -> A13 VERIFICATION PASSED (exit 0)：134 Python tests、unit tests、lint、assembleDebug 全通过
```

完成条件：

```text
typed legacy lifecycle = 0
duplicate typed install route = 0
unknown typed identity = 0
```

---

# P3 — 全部生产 Hook 收口

State: `IN_PROGRESS`

typed catalog 之外的 Hook 同样必须处理。

任务：

- [ ] 审计 HookBuilder、Hooker、resource hook 和反射安装入口；
- [ ] 业务 Feature 迁入 Registry；
- [ ] bootstrap/lifecycle 归入 Installer infrastructure；
- [ ] 无法迁移项登记 `LEGACY_EXCEPTION`；
- [ ] exception 必须有原因、owner、process、phase、test 和退出条件；
- [ ] dead code 仅在机械证据和所有者批准后删除。

子任务：

- [x] P3.1 刷新 Hook ownership inventory（根据 FeatureCatalog 调用关系重新分类）；
- [~] P3.2 迁移可直接归类的 legacy hook 到 Registry（按 process/phase 分批）；
  - [x] `system_volumesteps` → `volumeSteps` FeatureSpec (system_server, SYSTEM_SERVER_STARTING)
  - [x] `system_toasttime` → `toastTime` FeatureSpec (system_server, SYSTEM_SERVER_STARTING)
  - [ ] `system_separatevolume` 等跨 process 项按 LEGACY_EXCEPTION 登记
- [ ] P3.3 登记不可迁移项为 LEGACY_EXCEPTION 并补充原因/owner/test；
- [ ] P3.4 增加 inventory 机械门禁，防止 UNKNOWN/重复 ownership。

完成条件：

```text
UNKNOWN production hook = 0
orphan preference = 0
unreachable installer = 0
duplicate hook ownership = 0
```

---

# P4 — 进程与 Installer

State: `TODO`

检查：

- MainModule 仅 routing；
- dedicated installers 完整；
- helper process 默认拒绝；
- package/process matrix 与 scope list 一致；
- package-specific Feature 不进入错误通用路径；
- attach phase 只用于需要 app ClassLoader 的功能；
- reflection cache 在安全生命周期失效；
- 不同 process 不共享错误状态。

---

# P5 — Runtime safety

State: `TODO`

## P5.1 Callback boundary

覆盖 framework callback、delayed work、listener、animation、Handler、Observer、Receiver、thread entry。

## P5.2 Fatal boundary

验证 OOM、ThreadDeath、VirtualMachineError 在 registry、installer、reflection、cache、receiver 和 resource path 继续抛出。

## P5.3 Lifecycle ownership

验证 Receiver、Observer、Handler/Runnable、View、Bitmap/Drawable、Activity/Context、Controller、cached owner、process recreation、theme/reload。

## P5.4 Concurrency

验证 double install、reentrant install、stale state、lock ordering、concurrent callback、bounded queue/cache。

---

# P6 — 性能与内存

State: `TODO`

## P6.1 Disabled path

```text
0 business object
0 business Hook
0 Receiver
0 Observer
0 task
0 polling
0 reflection
```

## P6.2 Hot path

检查 Regex、collection pipeline、args copy、重复 reflection、重复 preference parsing、I/O、blocking、高频日志、重复 Handler/Runnable、无界 cache。

## P6.3 Resource lifetime

重点检查 status bar custom view、battery/temperature/current、notification view、Launcher view、album art/bitmap、周期 SystemUI 工作。

## P6.4 APK 与 R8

记录 baseline/final debug APK、signed RC（如可用）、SHA-256、size delta 和 R8 audit。

---

# P7 — Java → Kotlin 最终收口

State: `TODO`

## P7.1 分类

全部生产 Java 文件分类。

最终禁止：

```text
UNCLASSIFIED
KEEP_JAVA_TEMPORARY_BLOCKER
```

## P7.2 迁移

优先 pure model、utility、Installer glue、cold-path business logic 和 tests。

高风险入口逐文件验证 JVM signature、reflection ABI、static、overload、synchronization、callback capture 和 class initialization。

## P7.3 Allowlist

生成：

```text
docs/JAVA_BOUNDARY_ALLOWLIST.md
```

每个保留文件记录 technical reason、risk、JVM boundary、tests 和 re-evaluation condition。

---

# P8 — ROM intelligence

State: `TODO`

## P8.1 MIUI 14 / Android 13

完成 target inventory、process matrix、required target、variant、known device baseline 和 LSPosed log map。

## P8.2 HyperOS 1 / Android 13

完成 sample acquisition、package/version inventory、complete variant resolution、safe degradation 和 evidence gap。

## P8.3 Contract consistency

required 不降级；optional 有理由；candidate 不等于 verified；generated matrix 与 runtime 一致；diagnostics reason 精确。

---

# P9 — 测试、CI 与构建

State: `TODO`

## P9.1 Local

稳定通过：

```text
tools/verify.py full
compileall
Python unit tests
Kotlin compile
Java compile
Android unit tests
lint
assembleDebug
```

## P9.2 CI

GitHub Actions：

- push 到唯一授权分支；
- JDK 17；
- Python；
- Gradle cache；
- full verifier；
- debug assemble；
- 保存失败日志；
- 不发布 Release；
- Agent 自动读取失败并修复。

## P9.3 RC

外部签名存在时：

- signed develop/release；
- artifact SHA-256；
- version/commit mapping；
- manifest/signature check；
- 只记录 RC，不公开 Release。

---

# P10 — 文档与 dead code

State: `TODO`

更新：

- runtime architecture；
- invariants；
- architecture parity；
- Feature inventory；
- Hook ownership；
- process matrix；
- target matrix；
- ROM samples；
- Java allowlist；
- dead-code audit；
- performance audit；
- verification；
- device checklist；
- RC report；
- known limitations。

完成条件：

- 无 stale architecture status；
- 自动数字不重复手工维护；
- dead code 删除有证据和批准；
- device 状态不伪造。

---

# P11 — 自主 discovery sweep

State: `TODO`

在 P2-P10 各阶段后重复执行：

- warning/lint；
- TODO/FIXME；
- test gaps；
- inventory mismatch；
- duplicate/unreachable/orphan；
- callback/lifecycle；
- performance；
- APK/R8；
- CI；
- stale docs；
- LSPosed logs（如有）。

新增问题写入“发现的问题队列”，继续最高优先级任务。只有连续两轮 discovery sweep 无新 P0/P1，才可进入机器完成。

---

# P12 — MACHINE_COMPLETE

State: `TODO`

执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Full
```

完整审计从 P0 baseline 到当前 HEAD 的 diff。

提交全部机器完成内容，只推送：

```text
origin/devin/a13-rom-intelligence-audit
```

再执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Final
```

记录 final commit、upstream、CI、debug APK hash、tests、docs 和外部缺口。

---

# P13 — DEVICE_VALIDATED

State: `BLOCKED_EXTERNAL`

至少验证 MIUI 14 / Android 13：

- module activation；
- system_server；
- SystemUI；
- Launcher；
- status bar clock/seconds；
- battery/custom view；
- notification/floating window；
- receiver/observer lifecycle；
- process isolation；
- reboot/restart；
- LSPosed fatal log；
- memory/performance observation。

HyperOS 1 / Android 13：

- 有样本则验证；
- 无样本保持 `EXTERNAL_EVIDENCE_REQUIRED`。

---

# P14 — PROJECT_COMPLETE

State: `TODO`

必须：

```text
MACHINE_COMPLETE
DEVICE_VALIDATED
RELEASE_CANDIDATE_RECORDED
NO_OPEN_P0
NO_OPEN_P1
DOCUMENTATION_CURRENT
```

完成后：

- 将 `OverallState` 改为 `PROJECT_COMPLETE`；
- 记录最终证据报告；
- 不新建分支；
- 不合并 main；
- 不 tag/release；
- 进入 `LTS`（参见 `docs/governance/LONG_HORIZON_CONSTITUTION.md`）；
- 继续 evidence-driven maintenance。

---

## 4. 发现的问题队列

P0 后重建，但不得删除未解决问题。

| ID | Priority | Area | State | Evidence | Acceptance |
|---|---|---|---|---|---|
| BASELINE-001 | P0 | Git/branch | COMPLETE | P0.1 已记录仓库/分支/HEAD/upstream/Git 状态 | 完成 P0.1 |
| VERIFY-001 | P0 | Build | COMPLETE | P0.3 运行 `scripts/verify.ps1 -Mode Full` 通过 | 完成 P0.3 |
| ARCH-001 | P1 | Feature lifecycle | COMPLETE | P2 已完成，全部 typed Feature 通过 `FeatureInstallRegistry` | inventory 后全量收口 |
| DOC-001 | P2 | Docs | IN_PROGRESS | v4 文档契约检查器与 audit docs 已建立 | 代码与生成数据一致 |
| GOAL-001 | P2 | Governance | TODO | v5 长期治理宪章已融合，但 SBOM/ROM packs/artifact provenance 尚未实现 | 随 P8-P14 逐步落地 |
| DEVICE-001 | P1 | Device | BLOCKED_EXTERNAL | 无本轮真实日志 | 完成 P13 |

---

## 5. Checkpoint

| # | Commit | Task | Verification | State |
|---|---|---|---|---|
| 1 | `354e239` | P2 — typed Feature Registry 全量收口 | `scripts/verify.ps1 -Mode Full` 通过 | qualifying |
| 2 | `9d2fa23` | v3 governance: automation state checker + stewardship refresh | `scripts/verify.ps1 -Mode Fast` 通过 | qualifying |
| 3 | `37e8f1a` | P3.1 — refresh hook ownership inventory from typed catalog | `scripts/verify.ps1 -Mode Fast` 通过 | qualifying |
| 4 | `1cbc4db` | P3.2 — migrate `system_volumesteps` to typed `volumeSteps` | `scripts/verify.ps1 -Mode Fast` 通过 | qualifying |
| 5 | `182ee03` | P3.2 — migrate `system_toasttime` to typed `toastTime` | `scripts/verify.ps1 -Mode Fast` 通过 | qualifying |
| 6 | `44602f7` | v4 审计融合：文档契约检查器 + v4 audit docs | `scripts/verify.ps1 -Mode Fast` 通过 | qualifying |
| 7 | `d939868` | A13-ALG-001：FeatureCatalog O(1) index 与命名修正 | `scripts/verify.ps1 -Mode Fast` 通过 | qualifying |
| 8 | `pending` | v5 长期治理宪章融合：GOAL/AGENTS/constitution + checker | `scripts/verify.ps1 -Mode Fast` 通过 | qualifying |

---

## 6. 最终报告

尚未生成。
