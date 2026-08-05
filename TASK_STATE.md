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
  - [x] `system_hidestatusbar_whenscreenshot` → `hideStatusBarBeforeScreenshot` FeatureSpec (systemui, PACKAGE_READY)
  - [x] `controls_hidenavbar_whenscreenshot` → `hideNavBarBeforeScreenshot` FeatureSpec (systemui, PACKAGE_READY)
  - [x] `system_cleanshare` → `cleanShareMenu` + `cleanShareMenuService` FeatureSpec (android PACKAGE_READY + system_server SYSTEM_SERVER_STARTING)
  - [x] `system_cleanopenwith` → `cleanOpenWithMenu` + `cleanOpenWithMenuService` FeatureSpec (android PACKAGE_READY + system_server SYSTEM_SERVER_STARTING)
  - [x] `system_charginginfo` → `chargingInfo` FeatureSpec (systemui, PACKAGE_READY)
  - [x] `system_lswallpaper` → `setLockscreenWallpaper` FeatureSpec (system_server, SYSTEM_SERVER_STARTING)
  - [x] batch 9: `EnhancedSecurity`, `AppLock`, `SkipAppLock`, `NoCallInterruption` (system_server, SYSTEM_SERVER_STARTING) — contract corrected, all hard/silent criticality verified
  - [x] batch 10: `RemoveSecure`, `NoSignatureVerify`, `NoDarkForce`, `StickyFloatingWindows` (system_server, SYSTEM_SERVER_STARTING) — contract corrected to match production hook calls; `stickyFloatingWindows` expanded from 1 to 7 targets after parity audit
  - [x] batch 11: `AppsDisableService`, `NoAccessDeviceLogsRequest`, `AutoGroupNotifications`, `AppLockTimeout` (system_server, SYSTEM_SERVER_STARTING) — migrated with focused behavior tests
  - [x] P3.2.1A 合同—生产 EXACT_METHOD/EXACT_CONSTRUCTOR `parameterTypes` 表面门禁：新增 `tools/check_hook_contract_parity.py` + `tools/tests/test_check_hook_contract_parity.py`，覆盖 batch 9/10/11/12，已接入 `tools/verify.py` 和 CI；R1 修复 FQ/nested class JVM 名称规范化、disjoint parameterTypes 诊断、duplicate `TargetKey` 检测、unresolved `parameterTypes` 退化为空；含 37 个 focused tests 与 4 个 mutation tests
  - [x] P3.2.1B `AnyOfRequirement` 组级语义：解析 `AnyOfRequirement` 候选组，支持至少一个候选匹配即满足、检测空 group/duplicate candidate/candidate parameterTypes mismatch/错误候选/缺失候选/unparseable candidate，保留 P3.2.1A 全部能力；新增 `TestAnyOfRequirement` 与 `TestAnyOfMutations`（5 个 mutation tests）
  - [~] batch 12: 剩余 system_server SYSTEM_SERVER_STARTING 直接调用：
    口径：`SystemServerInstaller.install` 内非 `FeatureDispatcher.installById` 的直接调用，不含 `GlobalActions`/`Controls`/`Various`/`USBConfig`/`AlarmCompatService` 等已声明暂缓项，含一个偏好对应一个入口、每个入口只计一次：
    - [x] `TempHideOverlayAppHook` (system_screenshot_overlay) → `tempHideOverlayApp` FeatureSpec
    - [x] `OpenAppInFreeFormHook` (system_notify_openinfw / system_fw_forcein_actionsend / system_betterpopups_allowfloat) → `openAppInFreeForm` FeatureSpec
    - [x] `NavBarActionsHook` / `PowerDoubleTapActionHook` (controls_backlong_action / controls_powerdt_action) → `navBarActions` + `powerDoubleTapAction` FeatureSpec (P3.2.3 完成)
    - [x] `SelectiveToastsHook` (system_blocktoasts) → `selectiveToasts` FeatureSpec (P3.2.2 完成)
    - [x] `MultiWindowPlusHook` / `NoFloatingWindowBlacklistHook` (system_fw_splitscreen / system_fw_noblacklist) → `multiWindowPlus` + `noFloatingWindowBlacklist` FeatureSpec (P3.2.4 完成)
  - [x] `system_separatevolume`、`system_defaultusb` 等首批跨 process 项已按 LEGACY_EXCEPTION 登记（4 条 curated records，CROSS_PROCESS 原因）
- [x] P3.5 A13 Devin Local 控制面迁移：安装本地 Skill、采用原子 Task Slice、独立 Reviewer 流程、更新控制文档与 checker/mutation tests
- [~] P3.3 登记不可迁移项为 LEGACY_EXCEPTION 并补充原因/owner/test；
  - [x] P3.3A 机器可读 registry schema/validator/首批 4 条 curated records 完成，R1 完整性修复完成，state: `APPROVED / COMPLETE`
  - [-] P3.3B/C/D/E 继续登记剩余 logical owners (P3.3B: `READY_NOT_STARTED`)
  - [ ] P3.3 整体完成需全部 205 logical owner groups 已登记并验证
- [ ] P3.4 增加 inventory 机械门禁，防止 UNKNOWN/重复 ownership。

完成条件：

```text
UNKNOWN production hook = 0
orphan preference = 0
unreachable installer = 0
duplicate hook ownership = 0
```

批次 9/10/11 验证记录：

```text
- python tools/check_hook_contract_parity.py  PASS
- targeted tests: :app:testDebugUnitTest --tests CatalogBatch9And10ContractTest --tests Batch11BehaviorTest  PASS
- powershell .\scripts\verify.ps1 -Mode Fast  PASS
- powershell .\scripts\verify.ps1 -Mode Full  PASS
- powershell .\scripts\verify.ps1 -Mode Final  待当前工程 checkpoint 提交后重跑
- GitHub CI A13 Fast CI run 30741425209  PASS (commit 66ad73b)
```

P3.2.1A R1 修复验证记录：

```text
- python -m unittest tools.tests.test_check_hook_contract_parity  PASS (37 tests)
- python tools/check_hook_contract_parity.py  PASS (batches 9/10/11/12)
- python -m unittest discover -s tools/tests -p "test_*.py"  PASS (204 tests)
- python tools/check_automation_state.py  PASS
- python tools/check-invariants.py  PASS
- python tools/check-compat-contracts.py  PASS
- powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Fast  PASS
- powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Full  PASS
- git diff --check  PASS
- GitHub CI A13 Fast CI run 30772075962  PASS (commit a34d0ef)
```

P3.2.1B AnyOfRequirement 组级语义验证记录（exact commit CI 待追加）：

```text
QualifyingCommit: cb70bca9a5a8a0852fc5931e5388ebcde757fd98
VerifiedTree: 042f8fcd4f928b6d0bf3e6f9d5959546d311ff55
- python -m unittest tools.tests.test_check_hook_contract_parity  PASS (54 tests)
- python tools/check_hook_contract_parity.py  PASS (batches 9/10/11/12)
- python -m unittest discover -s tools/tests -p "test_*.py"  PASS (221 tests)
- python tools/check_automation_state.py  PASS
- python tools/check-invariants.py  PASS
- python tools/check-compat-contracts.py  PASS
- powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Fast  PASS
- powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Full  PASS
- git diff --check  PASS
- GitHub CI A13 Fast CI run 30778144937 PASS (commit 9afec52)
```

P3.2.3 NavBarActions / PowerDoubleTapAction 迁移记录：

```text
Task: 迁移 NavBarActionsHook 与 PowerDoubleTapActionHook 到 FeatureCatalog
Priority: P2
State: COMPLETE
Baseline commit: 1495a1583b89ceaa33d6ff20e535c1be706b648c
Files:
  - app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureId.kt
  - app/src/main/java/tv/withaibuild/customiuizer/mods/diagnostics/DiagnosticIds.kt
  - app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/CatalogContracts.kt
  - app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt
  - app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java
  - app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt (kept unchanged)
  - app/src/test/java/tv/withaibuild/customiuizer/mods/Batch12BehaviorTest.kt
  - app/src/test/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalogTest.kt
  - app/src/test/java/com/android/server/policy/BaseMiuiPhoneWindowManager.java (new stub)
  - app/src/test/java/com/android/server/policy/MiuiKeyShortcutManager.java (new stub)
  - app/src/test/java/com/miui/server/input/util/ShortCutActionsUtils.java (new stub)
  - tools/check_hook_contract_parity.py (BUNDLE constant, guarded-optional logic, class-name literal variable resolution)
  - tools/tests/test_architecture_invariants.py
  - tools/tests/test_feature_inventory.py
  - docs/rom-intelligence/A13_PROCESS_MATRIX.md
Original behavior:
  - NavBarActionsHook 和 PowerDoubleTapActionHook 由 SystemServerInstaller 直接调用；
  - 无 FeatureId、DiagnosticId、FeatureSpec、Contract；
  - 无 typed Feature 安装路径。
Invariant:
  - 保持 Hook 方法体、启用条件、调用顺序、异常处理及 ROM fallback 不变；
  - disabled path 0 business object / 0 hook reflection。
Implementation:
  - 新增 FeatureId.NAV_BAR_ACTIONS / POWER_DOUBLE_TAP_ACTION；
  - 新增 DiagnosticIds 条目；
  - 新增 CatalogContracts.navBarActions（BaseMiuiPhoneWindowManager postKeyLongPress/removeKeyLongPress ALL_METHODS_BY_NAME）
    与 powerDoubleTapAction（ShortCutActionsUtils.triggerFunction EXACT_METHOD 4 参数 +
    MiuiKeyShortcutManager.getVolumeKeyLaunchCamera OPTIONAL 0 参数）；
  - 新增 FeatureSpec（system_server, SYSTEM_SERVER_STARTING, CONTRACT_REQUIRED）；
  - SystemServerInstaller 替换为 FeatureDispatcher.installById；
  - 保持 Controls.kt 原 hook body 不变；为让 contract parity 工具识别原
    `val className = "..."` 变量与条件安装的 OPTIONAL target，增强
    check_hook_contract_parity.py（字符串类名变量解析、guard 区间检测）。
Commands:
  - python tools/check_hook_contract_parity.py --batch 12  PASS
  - python -m unittest tools.tests.test_check_hook_contract_parity  PASS (54 tests)
  - .\gradlew.bat --no-daemon :app:testDebugUnitTest  PASS
  - python -m unittest discover -s tools/tests -p "test_*.py"  PASS (221 tests)
  - powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Fast  PASS
  - powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Full  PASS
  - git diff --check  PASS
Exit codes: 0
Tests: Batch12BehaviorTest navBarActions/powerDoubleTapAction disabled/any-enabled paths; FeatureCatalogTest catalog total=66, adapted=58
CI: GitHub Actions run 30786701862 PASS (commit c1dd408)
Device evidence: NOT_EXERCISED
Commit: b522f69 (migration) + c1dd408 (keep Controls.kt unchanged, strengthen parity tool)
Push: origin/devin/a13-rom-intelligence-audit
Risks:
  - PowerDoubleTapAction 的 getVolumeKeyLaunchCamera 为 OPTIONAL + 条件硬安装；
    若条件为 false，安装结果为 DEGRADED（仍 active），与原行为一致。
  - c1dd408 扩大了 check_hook_contract_parity.py 对局部变量/条件块的理解；
    后续若条件块解析误报需补充单元测试。
Next: 继续 batch 12 MultiWindowPlusHook / NoFloatingWindowBlacklistHook，然后 P3.3 / P3.4
```

P3.2.4 MultiWindowPlus / NoFloatingWindowBlacklist 迁移记录：

```text
Task: 迁移 MultiWindowPlusHook 与 NoFloatingWindowBlacklistHook 到 FeatureCatalog
Priority: P2
State: COMPLETE
Baseline commit: e98709b6d2b01de9e1be5522484f1a6bb09e69fc
Files:
  - app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureId.kt
  - app/src/main/java/tv/withaibuild/customiuizer/mods/diagnostics/DiagnosticIds.kt
  - app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/CatalogContracts.kt
  - app/src/main/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalog.kt
  - app/src/main/java/tv/withaibuild/customiuizer/installers/SystemServerInstaller.java
  - app/src/main/java/tv/withaibuild/customiuizer/mods/SystemFreeformAndMultiWindowHooks.kt (kept unchanged)
  - app/src/test/java/tv/withaibuild/customiuizer/mods/Batch12BehaviorTest.kt
  - app/src/test/java/tv/withaibuild/customiuizer/mods/catalog/FeatureCatalogTest.kt
  - app/src/test/java/com/android/server/wm/ActivityTaskManagerServiceImpl.java (new stub)
  - app/src/test/java/com/android/server/wm/MiuiFreeformServicesUtils.java (new stub)
  - app/src/test/java/android/util/MiuiMultiWindowAdapter.java (new stub)
  - app/src/test/java/android/util/MiuiMultiWindowUtils.java (new stub)
  - tools/check_hook_contract_parity.py (multi-function feature extraction, private fun pattern)
  - tools/tests/test_check_hook_contract_parity.py (multi-function extraction tests)
  - tools/tests/test_architecture_invariants.py
  - tools/tests/test_feature_inventory.py
  - docs/rom-intelligence/A13_PROCESS_MATRIX.md
Original behavior:
  - MultiWindowPlusHook 和 NoFloatingWindowBlacklistHook 由 SystemServerInstaller 直接调用；
  - 无 FeatureId、DiagnosticId、FeatureSpec、Contract；
  - 无 typed Feature 安装路径；
  - NoFloatingWindowBlacklistHook 通过 DisableFloatingWindowBlacklistHook 私有 helper 安装大部分 hook。
Invariant:
  - 保持 Hook 方法体、启用条件、调用顺序、异常处理及 ROM fallback 不变；
  - disabled path 0 business object / 0 hook reflection；
  - 未迁移 com.miui.home 的 MultiWindowPlusHook(PackageReadyParam) 与 LauncherInstaller 直接调用；
  - 未修改 DisableFloatingWindowBlacklistHook 实现。
Implementation:
  - 新增 FeatureId.MULTI_WINDOW_PLUS / NO_FLOATING_WINDOW_BLACKLIST；
  - 新增 DiagnosticIds 条目；
  - 新增 CatalogContracts.multiWindowPlus（ActivityTaskManagerServiceImpl.updateResizeBlackList(Context) EXACT_METHOD、
    getSplitScreenBlackListFromXml() EXACT_METHOD、inResizeBlackList ALL_METHODS_BY_NAME）
    与 noFloatingWindowBlacklist（MiuiMultiWindowAdapter/Utils 全部目标 + MiuiFreeformServicesUtils.supportsFreeform，
    其中 getListFromCloudData、getStartFromFreeformBlackListFromCloud、isPkgMainActivityResizeable 为 OPTIONAL）；
  - 新增 FeatureSpec（system_server, SYSTEM_SERVER_STARTING, CONTRACT_REQUIRED）；
  - SystemServerInstaller 替换为 FeatureDispatcher.installById；
  - 移除 SystemServerInstaller 中未使用的 SystemFreeformAndMultiWindowHooks 导入；
  - 为让 contract parity 工具识别 NoFloatingWindowBlacklistHook 经 DisableFloatingWindowBlacklistHook 安装的辅助目标，
    增强 check_hook_contract_parity.py：支持一个 feature 对应多个 function name（主函数 + 私有 helper），并扩展
    FUNCTION_PATTERN 以匹配 private fun；
  - 更新 FeatureCatalogTest / architecture / feature inventory 计数 66→68、58→60；
  - 更新 A13_PROCESS_MATRIX 增加 multiWindowPlus、noFloatingWindowBlacklist 两行。
Commands:
  - python -m unittest tools.tests.test_check_hook_contract_parity  PASS (56 tests)
  - python tools/check_hook_contract_parity.py --batch 12  PASS
  - python tools/check_hook_contract_parity.py  PASS
  - .\gradlew.bat --no-daemon :app:testDebugUnitTest --tests Batch12BehaviorTest --tests FeatureCatalogTest  PASS
  - .\gradlew.bat --no-daemon :app:testDebugUnitTest  PASS
  - python -m unittest discover -s tools/tests -p "test_*.py"  PASS (223 tests)
  - python tools/check_automation_state.py  PASS
  - python tools/check-invariants.py  PASS
  - python tools/check-compat-contracts.py  PASS
  - powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Fast  PASS
  - powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Full  PASS
  - git diff --check  PASS
Exit codes: 0
Tests: Batch12BehaviorTest multiWindowPlus/noFloatingWindowBlacklist disabled/enabled paths; FeatureCatalogTest catalog total=68, adapted=60; Batch12 contract parity; multi-function extraction unit tests
CI: GitHub Actions run 30790210095 PASS (commit 17ff77ce40b47c0b4fb335c597ca262869ef846a, job 91611921609)
Device evidence: NOT_EXERCISED
Commit: 17ff77ce40b47c0b4fb335c597ca262869ef846a
Push: origin/devin/a13-rom-intelligence-audit
Risks:
  - MultiWindowPlus 仍保留 com.miui.home 的 PackageReadyParam 直接调用（LauncherInstaller），未在 catalog 中注册；
    该路径仍在同一 preference system_fw_splitscreen 下，不视为 orphan，但进入 P3.3/P3.4 需统一 launcher 路由。
  - 本次增强 contract parity 工具支持多 function 合并；后续 helper 跨文件调用不会被追踪。
Next: 继续 P3.3 登记 LEGACY_EXCEPTION 与 P3.4 inventory 门禁
```

---

## P3.3A LEGACY_EXCEPTION 登记基础

State: `APPROVED / COMPLETE`
R2 Decision: `APPROVE`
R2 review target HEAD: `3b1fd330ad4f4e1d46fd926c6ca3b2d781fb538d`
Engineering checkpoint: `f72baa77fe8a8a6c3e2a2ba2ca9cabd90048e419`
Verified tree: `d0667253d17972c926fa43cfe38f031450819635`

文件：

```text
- docs/audit/A13_LEGACY_EXCEPTION_REGISTRY.json
- tools/build_legacy_exception_registry.py
- tools/validate_legacy_exception_registry.py
- tools/tests/test_legacy_exception_registry.py
- docs/process/tasks/A13-P3.3A-LEGACY-EXCEPTION-REGISTRY.md
```

原始行为：

```text
- A13_HOOK_OWNERSHIP_INVENTORY.md 仅有 per-file 分类，缺少稳定 call-site 身份；
- 无机器可读 LEGACY_EXCEPTION 登记；
- 无 validator / focused tests；
- 首批 exception 未按 cross-process / owner / reason 精确定义。
```

不变量：

```text
- 不修改 app/src/main/** 生产代码或已有 typed Feature；
- 不一次登记所有 514 legacy calls；
- 每条 exception 必须有明确 owner、process、phase、reasonCode、hookTargets、testEvidence 和 exitCondition；
- reason 必须为具体技术解释，不得仅写 "legacy"；
- registry、inventory、build tool 的 legacy 计数一致（514）。
```

实现：

```text
- 定义 A13_LEGACY_EXCEPTION_REGISTRY.json schema v1（schemaVersion、sourceCommit、totalLegacyCallSites、
  totalLegacyGroups、firstBatchSize、records[]）；
- 定义 controlled reasonCode taxonomy（CROSS_PROCESS / LIFECYCLE_BOOTSTRAP / RESOURCE_HOOK / ... / OTHER_REVIEW_REQUIRED）；
- build_legacy_exception_registry.py：
  - scan_legacy_call_sites 复用 audit_hook_ownership.py 分类，保证与 inventory 一致；
  - build_census 可生成稳定 per-call A13_HOOK_CALL_SITE_CENSUS.json；
  - build_registry 以 FIRST_BATCH_SEEDS 生成首批 4 条 curated records；
  - validate 检查 schema、taxonomy、文件/entrypoint 存在性、重复 id、call site 重叠、typed/infra 越界；
- validate_legacy_exception_registry.py：独立入口调用同一 validate；
- test_legacy_exception_registry.py：24 个 focused + mutation 测试；
- 首批 4 条记录：
  - legacy-separatevolume-systemui (MIUIVolumeDialogHook + SingleNotificationSliderHook, process=system_ui, phase=PACKAGE_READY, CROSS_PROCESS)
  - legacy-separatevolume-settings (NotificationVolumeSettingsHook, process=per_app, phase=PACKAGE_READY, CROSS_PROCESS)
  - legacy-usbconfig-system (USBConfigHook, process=system_server, phase=SYSTEM_SERVER_STARTING, CROSS_PROCESS)
  - legacy-usbconfig-settings (USBConfigSettingsHook, process=per_app, phase=PACKAGE_READY, CROSS_PROCESS)
- A13_HOOK_OWNERSHIP_INVENTORY.md 经 audit_hook_ownership.py 重新生成为 676 total / 514 LEGACY_EXCEPTION / 133 REGISTRY_FEATURE / 6 INSTALLER_INFRASTRUCTURE / 23 API_BRIDGE / 0 UNKNOWN。
- R1 完整性修复：`--check` canonical stale detection（排除 `generatedAt` 与 `sourceCommit`），`WHOLE_FILE_LEGACY_EXCEPTION_FORBIDDEN` 与 `ALL_LEGACY_CALLS_BATCH_FORBIDDEN` 动态 census 门控，`hookTargets` / `coveredCallSites` / `sourceFile` 结构化校验，稳定 provenance（`inputDigest`、`sourceTree`、`generatorVersion`）；`test_legacy_exception_registry.py` 更新为 24 个 focused/mutation tests。
```

验证：

```text
- python tools/build_legacy_exception_registry.py --build                -> 0
- python tools/build_legacy_exception_registry.py --check                -> 0
- python tools/validate_legacy_exception_registry.py                     -> 0
- python -m unittest tools.tests.test_legacy_exception_registry          -> 24/24 pass
- python -m unittest tools.tests.test_hook_ownership_inventory           -> 2/2 pass
- python -m unittest discover -s tools/tests -p "test_*.py"              -> 271 pass
- python tools/audit_hook_ownership.py                                   -> 0, totals 676/133/6/23/514/0
- python tools/check-invariants.py                                       -> 0, no violations
- python tools/check-compat-contracts.py                                 -> 0
- python tools/check_automation_state.py                                 -> 0
- python tools/check_document_contracts.py                               -> 0
- python tools/check_goal_constitution.py                                -> 0
- python tools/check_hook_contract_parity.py                             -> 0
- python tools/progress_snapshot.py --check                              -> 0
- git diff --check                                                       -> 0
- powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Fast -> A13 VERIFICATION PASSED
- powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Full -> A13 VERIFICATION PASSED
```

CI:

```text
- GitHub Actions A13 Fast CI run 30797856821, job 91635432116, result PASS (commit f72baa77fe8a8a6c3e2a2ba2ca9cabd90048e419)
```

Device evidence: `NOT_EXERCISED`

Commit: `f72baa77fe8a8a6c3e2a2ba2ca9cabd90048e419`

Tree: `d0667253d17972c926fa43cfe38f031450819635`

Push: `origin/devin/a13-rom-intelligence-audit`

风险：

```text
- audit_hook_ownership.py 的 typed-function 识别基于函数名；同名的多态重载（如 MultiWindowPlusHook 的 PackageReadyParam 变体）
  会被 REGISTRY 或 LEGACY 同时归类，具体取决于所在文件；P3.4 的 inventory 门禁可进一步细化签名级 owner；
- 首批 exception 仅覆盖 2 个 cross-process preference，剩余 ~501 legacy calls 留给 P3.3B/C；
- registry 中的 hookTargets 为手工摘录，需随 ROM 版本变化由 contract parity 工具持续校验。
```

Next: P3.3A approved; P3.3B engineering complete and in R2 review; P3.3C may start only after R2 APPROVE; P3.3D/E and P3.4 remain planned but not started; toolchain upgrades remain blocked.

---

## P3.3B GlobalActions and AlarmCompat LEGACY_EXCEPTION 登记

State: `R4_REVIEW_REQUIRED`

R2 修复目标 HEAD: `fa74d4f3c1d4b2f10c519ac154e5172bd2fa39d7`

R3 修复目标 HEAD: `33ce1ab5969d61c94c0ce3f7942c8dac02a5c579`

R4 修复目标 HEAD: `6f06e6df9a05d5b50f1c314c1f92f2c9a3ccb248`

EngineeringCommit: `372c59fd3515036b348baf7a19d6443e1993b8e7`

EngineeringTree: `2d820119e7d839a08795b49b69882f933e9ac7db`

EngineeringCIRun: `30870297127`

EngineeringCIJob: `91870748474`

EngineeringCIResult: `PASS`

R3EngineeringCommit: `23cf2e86309df4168db24e1d57719c9be1fe36a6`

R3EngineeringTree: `2b1f703215717502f65bb8d608920db9c67bc504`

R3EngineeringCIRun: `30958584324`

R3EngineeringCIJob: `92157229529`

R3EngineeringCIResult: `PASS`

R4EngineeringCommit: `6f06e6df9a05d5b50f1c314c1f92f2c9a3ccb248`

R4EngineeringTree: `aacdce71b908356e369bf843867f1785905095b6`

R4EngineeringCIRun: `30968706163`

R4EngineeringCIJob: `92188177489`

R4EngineeringCIResult: `PASS`

文件：

```text
- docs/audit/A13_LEGACY_EXCEPTION_REGISTRY.json
- tools/build_legacy_exception_registry.py
- tools/validate_legacy_exception_registry.py
- tools/legacy_exception_source_contract.py（新增）
- tools/tests/test_legacy_exception_registry.py
- tools/tests/test_p33b_legacy_exception_routes.py
- tools/tests/test_legacy_exception_source_contract.py（新增）
- docs/process/tasks/A13-P3.3B-GLOBALACTIONS-ALARMCOMPAT-EXCEPTIONS.md
- docs/process/tasks/A13-P3.3B-R1-ACTIVATION-CONTRACT-REPAIR.md
- docs/process/tasks/A13-P3.3B-R2-SOURCE-LOGIC-VALIDATOR-REPAIR.md
- docs/process/tasks/A13-P3.3B-R3-INDEPENDENT-TRUTH-EVIDENCE-REPAIR.md
- docs/process/tasks/A13-P3.3B-R4-GATE-COVERAGE-COMPLETION-EVIDENCE.md
- docs/process/handoffs/A13-HANDOFF-2026-08-03-P3.3B.md
- docs/process/handoffs/A13-HANDOFF-2026-08-04-P3.3B-R2.md
- docs/process/handoffs/A13-HANDOFF-2026-08-04-P3.3B-R3.md
- docs/process/handoffs/A13-HANDOFF-2026-08-05-P3.3B-R4.md
- TASK_STATE.md
- SMART_OPERATION_STATE.md
```

原始行为：

```text
- P3.3A 首批 exception 未覆盖 GlobalActions.kt 和 Various.kt 中的 cross-process / lifecycle-bootstrap entrypoint；
- registry schema v1 无法表达多批次登记；
- 无 P3.3B 路由 evidence、process/phase/preference 校验、或 mutation 测试。
```

不变量：

```text
- 不修改 app/src/main/** 生产代码或已有 typed Feature；
- 不一次登记所有 514 legacy calls；
- 每条 exception 必须有明确 owner、process、phase、reasonCode、hookTargets、testEvidence 和 exitCondition；
- reason 必须为具体技术解释，不得仅写 "legacy"；
- P3.3A 4 条记录必须原样保留，不得强制添加 activationContract / callSiteConditions；
- schema v3 中 firstBatchSize 必须保持为 4；
- batchCounts 和 registeredRecordCount 必须由 records 动态计算；
- activationContract 和 callSiteConditions 必须受控枚举，不允许自由文本 expression；
- preferenceKeys 只保存固定 literal keys，动态 key domain 由 activationContract 表达；
- 删除/修改/移动 P3.3B batch 或合约时 --check 必须报告 stale。
```

实现：

```text
- P3.3B 首次实现：FIRST_BATCH_SEEDS 重命名为 LEGACY_EXCEPTION_SEEDS，每条 seed 增加 batch 字段，新增 4 条 P3.3B 记录，schema 升级到 v2；
- P3.3B-R1 修复（见 docs/process/tasks/A13-P3.3B-R1-ACTIVATION-CONTRACT-REPAIR.md）：
  - schema 升级到 v3；
  - 为 P3.3B records 新增 activationContract 和 callSiteConditions；
  - setupGlobalActions preferenceKeys 只保留固定 literal keys（controls_volumemedia_up / controls_volumemedia_down / controls_mediaplayer_apps），动态 _action domain 由 DYNAMIC_SUFFIX_INT_GT 表达；
  - setupForegroundMonitor 第三个 call 增加 per-call-site condition（various_showcallui > 0）；
  - setupStatusBar 标记为 UNCONDITIONAL；
  - AlarmCompatServiceHook 区分 various_alarmcompat（activation）与 various_alarmcompat_apps（runtime allowlist 配置）；
  - route evidence 测试改为从生产源码和 committed registry 独立解析，不使用 LEGACY_EXCEPTION_SEEDS 作为 expected；
- P3.3B-R2 修复（见 docs/process/tasks/A13-P3.3B-R2-SOURCE-LOGIC-VALIDATOR-REPAIR.md）：
  - 新增 tools/legacy_exception_source_contract.py 只读 source contract parser，从 Java/Kotlin 源码结构化推导 activation、call-site condition、runtimeConfigKeys、preferenceKeys；
  - needGlobalActions() 证据改由 balanced-brace function extraction、boolean expression 拆析、if/return 结构验证，能区分 OR/AND、证明 app-set 非空属于 media 分支并受 ! 取反；
  - setupForegroundMonitor 证据从 SystemUiInstaller 与 GlobalActions 源码分别推导，第三个 call 必须位于 various_showcallui > 0 分支；
  - build_legacy_exception_registry.py 升级到 schema v4；
  - 新增 runtimeConfigKeys（可选），AlarmCompatServiceHook 区分 various_alarmcompat（activation）与 various_alarmcompat_apps（runtime）；
  - validator fail-closed：非 object 的 activationContract 不报 AttributeError/KeyError，threshold 拒绝 string/bool/float/null，精确 allowed/required 字段集、canonical 排序、重复/空 predicate、UNCONDITIONAL 携带 predicate、unknown field 等；
  - callSiteConditions 必须 canonical 等于 activationContract 中某个 predicate，否则 CALL_SITE_CONDITION_NOT_ACTIVATION_BRANCH；
  - preferenceKeys 双向不变量：等于 sorted(fixedActivationKeys ∪ callConditionKeys ∪ runtimeKeys)；
  - test_p33b 重构为 generator + source contract + fail-closed + mutation + source mutation 五类测试；
  - 新增 test_legacy_exception_source_contract.py 覆盖 source parser 与 mutation；
  - A13_LEGACY_EXCEPTION_REGISTRY.json 重新生成，schema v4，provenance 刷新，8 records / 19 covered calls / firstBatchSize 4 保持不变。
- P3.3B-R4 修复（见 docs/process/tasks/A13-P3.3B-R4-GATE-COVERAGE-COMPLETION-EVIDENCE.md）与 handoff A13-HANDOFF-2026-08-05-P3.3B-R4.md：
  - 统一 build_registry AST gate 覆盖 test_p33b_legacy_exception_routes.py 与 test_legacy_exception_source_contract.py 全模块；
  - 唯一 allowlisted class 为 P3_3B_GeneratorConsistencyTest；
  - 识别并拒绝 alias / getattr / partial / callback / 嵌套引用等绕过形式；
  - completion evidence 改为使用 unittest.defaultTestLoader 动态计算 focused counts 与 discover total；
  - 机械核对 TASK_STATE.md / SMART_OPERATION_STATE.md / R3/R4 handoff 与 task slice 的 active verification 段；
  - 不再保留任何手写测试数量；
  - 新增 P2 parser 边界 subtests 与最小 Kotlin return-type fail-closed 修复；
  - 所有正式文档数字更新为 57/57 source contract、441/441 discover。
```

验证：

```text
- python tools/build_legacy_exception_registry.py --build                 -> 0
- python tools/build_legacy_exception_registry.py --check                 -> 0
- python tools/validate_legacy_exception_registry.py                      -> 0
- python -m unittest tools.tests.test_legacy_exception_registry           -> 72/72 pass
- python -m unittest tools.tests.test_p33b_legacy_exception_routes        -> 88/88 pass
- python -m unittest tools.tests.test_legacy_exception_source_contract    -> 57/57 pass
- python -m unittest tools.tests.test_hook_ownership_inventory            -> 2/2 pass
- python -m unittest tools.tests.test_p33b_completion_evidence            -> 1/1 pass
- python -m unittest discover -s tools/tests -p "test_*.py"               -> 441/441 pass
- python tools/check-invariants.py                                        -> 0, no violations
- python tools/check-compat-contracts.py                                  -> 0
- python tools/check_automation_state.py                                  -> 0
- python tools/check_document_contracts.py                                -> 0
- python tools/check_goal_constitution.py                                 -> 0
- python tools/check_hook_contract_parity.py                              -> 0
- python tools/progress_snapshot.py --check                               -> 0
- .\gradlew.bat --no-daemon :app:compileDebugKotlin                       -> BUILD SUCCESSFUL
- .\gradlew.bat --no-daemon :app:compileDebugJavaWithJavac                -> BUILD SUCCESSFUL
- .\gradlew.bat --no-daemon :app:testDebugUnitTest                        -> BUILD SUCCESSFUL
- .\gradlew.bat --no-daemon :app:lintDebug                                -> BUILD SUCCESSFUL
- .\gradlew.bat --no-daemon :app:assembleDebug                            -> BUILD SUCCESSFUL
- powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Fast -> A13 VERIFICATION PASSED
- powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Full  -> A13 VERIFICATION PASSED
- git diff --check                                                        -> 0
- 连续两次 --build 的 canonical 输出相同（仅 generatedAt 不同）
```

CI：

```text
- GitHub Actions A13 Fast CI run 30862747188, job 91848049006, result PASS (commit 219c49659cf575ea7b0dc5c6b3e455ddf1ef3ac5)
- GitHub Actions A13 Fast CI run 30958584324, job 92157229529, result PASS (commit 23cf2e86309df4168db24e1d57719c9be1fe36a6)
- GitHub Actions A13 Fast CI run 30968706163, job 92188177489, result PASS (commit 6f06e6df9a05d5b50f1c314c1f92f2c9a3ccb248)
```

Device evidence: `NOT_EXERCISED`

Commit: `6f06e6df9a05d5b50f1c314c1f92f2c9a3ccb248`

Tree: `aacdce71b908356e369bf843867f1785905095b6`

Push: `origin/devin/a13-rom-intelligence-audit`

风险：

```text
- setupGlobalActions 的 preferenceKeys 只包含固定 literal key；动态 `_action` domain 不属于 preferenceKeys 的子集或枚举范围，而是由 activationContract.DYNAMIC_SUFFIX_INT_GT 完整表达；
- setupStatusBar 的 preferenceKeys 为空，表示其触发条件是 SystemUI package ready 而非用户偏好；validator 和 tests 已接受空列表；
- GlobalActions 内仍存在 miuizerSettingsHook 等 legacy 函数未在 P3.3B 登记，留给后续 batch；
- Various.kt 中仍有大量 legacy 函数，仅 AlarmCompatServiceHook 已登记；
- hookTargets 仍为手工摘录，需随 ROM 版本持续校验。
```

StateCommit: `c555b31cc28df05906f666e6a38ce4007c0ff993`

HandoffCommit: `ae4f2d4e628f8e6748dd23d3a7267f4345663ca5`

R3EngineeringCommit: `23cf2e86309df4168db24e1d57719c9be1fe36a6`

R3EngineeringTree: `2b1f703215717502f65bb8d608920db9c67bc504`

R3StateCommit: `a34ce3349ccfa6e79160890b6ba0fde73838688a`

R3HandoffCommit: `a34ce3349ccfa6e79160890b6ba0fde73838688a`

R3EvidenceCorrectionCommit: `a153b930dbe557770af8dab1a9881f08a94dd547`

R3ReviewTarget: `a153b930dbe557770af8dab1a9881f08a94dd547`

R3ReviewTargetCIRun: `30959213005`

R3ReviewTargetCIJob: `92159224416`

R3ReviewTargetCIResult: `PASS`

R3Push: `origin/devin/a13-rom-intelligence-audit`

R3CI: GitHub Actions A13 Fast CI run `30958584324`, job `92157229529`, result **PASS** (commit `23cf2e86309df4168db24e1d57719c9be1fe36a6`)

Push: `origin/devin/a13-rom-intelligence-audit`

CI: GitHub Actions A13 Fast CI run 30870297127, job 91870748474, result PASS (commit 372c59fd3515036b348baf7a19d6443e1993b8e7)

Next: P3.3B R3 修复完成，进入独立 R3 review；P3.3C 仍 blocked 直到 R3 APPROVE；P3.3D/E 和 P3.4 remain planned but not started；toolchain upgrades remain blocked。

---

## P3.5 A13 Devin Local 控制面迁移

State: `COMPLETE`

文件：

```text
AGENTS.md
SMART_CONTINUOUS_OPERATION.md
DEVIN_START_PROMPT.md
INSTALL_A13_CONTROL_PLANE.md
tools/check_automation_state.py
tools/tests/test_check_automation_state.py
.agents/skills/a13-safe-implementation/SKILL.md
.agents/skills/a13-independent-review/SKILL.md
docs/process/A13_CONTROL_PLANE_MIGRATION.md
docs/process/A13_DEVIN_LOCAL_SKILLS.md
docs/process/A13_RISK_GATE_MATRIX.md
docs/process/templates/A13_SESSION_HANDOFF_TEMPLATE.md
docs/process/templates/A13_TASK_SLICE_TEMPLATE.md
```

原始行为：

```text
AGENTS.md / SMART_CONTINUOUS_OPERATION.md 要求同一会话长时连续自治，
DEVIN_START_PROMPT.md 为巨型启动指令，
无显式 repository Skill 边界，
无独立 Reviewer 会话，
无 A13 技能文件保护检查。
```

不变量：

```text
仓库/分支 EXACT_LOCK 不变；
技术规则（fatal、Hook、ABI、俄式系统代码、disabled-path、验证设备证据）不变；
GOAL.md、scripts/verify.ps1 不改；
app/src/main 生产代码不改；
控制面迁移后恢复保护，后续不得自行重写控制层。
```

实现：

```text
安装 .agents/skills/a13-safe-implementation 与 a13-independent-review（triggers: ["user"]）；
AGENTS.md 改为单会话一个原子 Task Slice、显式 Skill 调用优先级、handoff 后结束会话；
SMART_CONTINUOUS_OPERATION.md 改为 SessionMode: ATOMIC_TASK_SLICE、AutoStartNextSlice: false、MULTI_SESSION 连续性；
DEVIN_START_PROMPT.md 改为简短 Skill 启动入口；
INSTALL_A13_CONTROL_PLANE.md 增加 Skill 文件路径与调用方式；
tools/check_automation_state.py 增加 control-plane invariants 与 A14 引用检查；
tools/tests/test_check_automation_state.py 增加 mutation 测试（AutoStartNextSlice true、missing triggers）。
```

验证：

```text
python -m unittest discover -s tools/tests -p "test_*.py"  -> OK
python tools/check_automation_state.py                       -> pass
python tools/check-invariants.py                             -> pass
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Mode Fast -> pass
```

风险：

```text
- 未继续 parity/AnyOf/callback/progress/hazard/普通 P3 任务，留给后续 a13-safe-implementation 会话；
- 受保护文档规则已按本次所有者授权临时更新，完成后恢复保护；
- 新 checkpoint 的 CI 状态将在 handoff 中记录。
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

State: `IN_PROGRESS`

## P9.1 Local

State: `VERIFIED_BUILD`

Evidence: `BUILD_VERIFIED`

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

State: `VERIFIED_CI`

Evidence: `CI_VERIFIED`

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
| A13-ARCH-TG-001 | P2 | Runtime | DEFERRED | A13_TELEGRAM_INSPIRED_LTS_RUNTIME_PROMPT_V1.md 已下发 | 完成 P3 核心收口后做现状审计，不直接写框架 |
| DEVICE-001 | P1 | Device | BLOCKED_EXTERNAL | 无本轮真实日志 | 完成 P13 |
| REPAIR-V2-001 | P1 | Contract parity | IN_PROGRESS | A13_CODE_ERROR_REPAIR_PROMPT_V2 要求参数类型、AnyOf、错误类和变异测试 | 完成参数类型/AnyOf 校验强化（父 P3）|
| REPAIR-V2-002 | P2 | Tool contract | TODO | SHARED_TOOL_REPAIR_CONTRACT_V2 要求 progress_snapshot.py 与 source_hazard_scan.py 同步 | 两工具通过工具契约验收 |
| REPAIR-V2-003 | P2 | CI governance | TODO | 状态文件 CI 接受清单收紧，防止仅状态提交误报 PASS | 更新 check_automation_state.py 通过新清单 |

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
| 8 | `590421d` | v5 长期治理宪章融合：GOAL/AGENTS/constitution + checker | `scripts/verify.ps1 -Mode Fast` 通过 | qualifying |
| 9 | `9fd9e10` | v6 控制状态修复：签名 source of truth + A13-ALG-002 + v6 governance docs | `scripts/verify.ps1 -Mode Final` 通过 | qualifying |
| 10 | `7c3b4fe` | Fast CI 修复：跨平台路径 + full checkout + progress snapshot 忽略 volatile metadata | `python tools/verify.py full` + Fast CI 通过 | qualifying |
| 11 | `eb0ba15` | 紧急修复 v2：fatal 边界、batch 12 args 安全与 focused callback 测试 | `gradlew :app:testDebugUnitTest` + Fast CI 通过 | qualifying |
| 12 | `0894e99` | 修正 `LastVerifiedTree` 为已推送 commit 的真实 tree | Fast CI 通过 | bookkeeping |
| 13 | `8a5fa11` | 修正 `LastVerifiedTree` 为 `0894e99` 的 tree 对象（避免不存在 tree） | Fast CI 通过 | bookkeeping |

---

## 6. 最终报告

尚未生成。
