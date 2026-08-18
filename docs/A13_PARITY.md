# A13 / A14 功能移植 Parity 控制文档

## 基线

```text
A13 foundation SHA: 4c6d2b10d6037f8d9741927d1ab76731dc8171ba
A14 reference SHA:  d20d96b543a49a584970e312da7d704958a155aa
```

不使用移动 A14 `main` 做 parity 决策。A14 参考刷新必须显式更新本文件并记录新 SHA。

## 平台边界

- A13 主支持：MIUI 14 / Android 13。
- A13 实验兼容：HyperOS 1 / Android 13。
- A13 不支持：Android 14+。
- A14 仅作为工程 / 产品语义参考。

## 工程授权方向

```text
ARCHITECTURE_COMPATIBILITY_PROGRAM = AUTHORIZED
```

授权范围：

- A13 架构加固
- A13 兼容架构

不授权：

- 无关功能语义重开
- 主动 ROM 猎取
- 未经明确授权的功能实现

阶段顺序：

```text
A Foundation Correctness
B Runtime Architecture Convergence
C Compatibility Architecture
D A14 Feature Parity
```

A14 是参考，不是自动代码权威。只有经过 A13 语义、API、ABI、生命周期、ClassLoader
和资源映射审计的功能才进入 A13。

ROM 证据不被主动猎取；兼容性工作确实需要时才按现有证据等级补充设备/日志证据。

## 产品决策

灵动额头 / Dynamic Island 是唯一当前已授权排除的功能：

```text
PORT   = NO
REASON = PRODUCT_DECISION
```

灵动岛专属基础设施不自动移植。

## Parity 不变式

A14 参考 SHA 中存在的每个**用户可见**功能，都必须出现在 F2 A13 功能库存中，并收到上述状态之一。

不允许因为以下原因省略：

- 看起来难以移植
- A13 target 未知
- Android 13 与 Android 14 不同
- 当前没有设备连接

若不支持，明确分类为：

- `A13_VARIANT_REQUIRED`（A13 需要独立实现）
- `BLOCKED_BY_A13_PLATFORM`（A13 平台/ROM/生命周期不支持）
- `EXCLUDED_DYNAMIC_ISLAND`（仅灵动额头）

基础设施独占于灵动岛的不单独进入库存。

## 功能状态

| 状态 | 含义 |
|---|---|
| PRESENT_EQUIVALENT | A13 已有等价实现，语义一致 |
| PRESENT_DRIFTED | A13 有实现，但语义已偏离或需修复 |
| MISSING_PORT_REQUIRED | A13 缺失，且产品明确要求移植 |
| A13_VARIANT_REQUIRED | A13 需要独立变体，不能直接回移 |
| BLOCKED_BY_A13_PLATFORM | A13 平台（API/ROM/生命周期）不支持 |
| EXCLUDED_DYNAMIC_ISLAND | 产品决策排除 |

## 功能记录 Schema

每个功能必须具备以下字段：

```text
FEATURE_ID
USER_VISIBLE_NAME
A14_STATUS
A13_STATUS
A14_PREF_KEY
A13_PREF_KEY
PROCESS
UPSTREAM_SEMANTIC_SOURCE
A14_IMPLEMENTATION_SOURCE
A13_TARGET
PORT_CLASS
STATIC_EVIDENCE
BUILD_EVIDENCE
DEVICE_EVIDENCE
TEST_STATUS
FINAL_STATE
```

未开始的功能库存不在 F0 中填充；F2 进行完整盘点。

## 上游语义审计

修改已有 A13 功能语义前检查：

1. MonwF/customiuizer 当前上游（相关部分）
2. A13 初始 / 历史工作实现
3. 当前 A13 实现
4. A14 参考实现

分类：

```text
UPSTREAM_EXACT
UPSTREAM_INTENT_EQUIVALENT
A13_COMPAT_VARIANT
SEMANTIC_DRIFT
A14_NEW_FEATURE
DEAD_UPSTREAM_PATH
```

对已有功能：已有 A13 上游用户意图 > A14 实现形状。
对全新 A14 功能：A14 用户可见合约为产品参考，A13 target 必须独立证明。

## Issue #2

Issue #2 是**已有 A13 功能**（`controls_fsg_horiz` / `LauncherGestureHooks.FSGesturesHook`）
在第三方默认桌面下的兼容缺口，不是缺失功能。它不提前被赋予 `A13_STATUS` 或最终 parity
分类；实际分类在 F2/F3 完成。

持久状态记录于
`docs/rom-intelligence/A13_STAGE_F1_R1_FSG_TARGET_CORRECTIVE_REPORT.txt`
的 `ISSUE #2 DURABLE STATE` 段，本文件不复制。

不重新打开技术分析。不实现 Design E。


```text
ISSUE_2_TARGET_SELECTION    = PASS
SELECTED_STATIC_CANDIDATE   = DESIGN_E_BACK_STUB_ONLY_RECOVERY
PRODUCTION_IMPLEMENTATION   = DEFERRED
DEVICE_ROOT_CAUSE           = UNVERIFIED
DEVICE_VALIDATION           = PENDING
ACTUAL_BASE_SHA             = cc200778ec90285a638015fb037b3a919471c0ad
ACTUAL_FINAL_SHA            = 4c6d2b10d6037f8d9741927d1ab76731dc8171ba
ACTUAL_MERGE_BASE           = cc200778ec90285a638015fb037b3a919471c0ad
```

## 阶段计划

| 阶段 | 目标 |
|---|---|
| F0 FOUNDATION | 重建 AGENTS.md、ARCHITECTURE.md、COMPATIBILITY.md、A13_PARITY.md；清理过时控制文档 |
| F1 ARCHITECTURE BASELINE | 完成当前 A13 运行时架构基线；确认 Hook 安装链、ClassLoader 边界、生命周期 |
| F2 FEATURE INVENTORY | 完整盘点 A14→A13 功能清单，填充 Feature Schema |
| F3 SEMANTIC / ABI AUDIT | 逐个功能审计上游语义、A13 target ABI、变体需求 |
| F4 FEATURE PORT BATCHES | 按优先级分批移植或标记为 A13 独立变体 |
| F5 RELEASE CLOSURE | 验证、构建、发布 r13.x |

## 阶段状态

```text
F0 FOUNDATION           = DONE
F1 ARCHITECTURE BASELINE = DONE
F2 FEATURE INVENTORY    = READY
F4 PRODUCTION PORT      = BLOCKED
```

架构兼容工程阶段：

```text
A Foundation Correctness  = IN_PROGRESS
  A1 PreferenceBootstrap fatal boundary = DONE
B Runtime Architecture Convergence = PENDING
C Compatibility Architecture       = PENDING
D A14 Feature Parity              = PENDING
```

F1 结果与完整 A13/A14 结构对比见
`docs/audit/A13_F1_ARCHITECTURE_BASELINE.md`。

F4 被阻塞的原因（F1 findings，A1 关闭 P0-1）：

```text
P1-1  Application.attach after 回调无幂等守卫（Launcher / GenericApp）
P1-2  install-once 仅覆盖 catalog 路径
```

P0-1 已在 A1 关闭；`docs/audit/A13_F1_ARCHITECTURE_BASELINE.md` 已更新为
`CLOSED_BY_A1`。

进入 F4 前需要的最小修正批次：

```text
A1  PreferenceBootstrap 致命边界 + 不变式覆盖扩展              DONE
C2  Application.attach 回调幂等守卫
C3  HookTargetResolver 致命判定补全 + 致命助手收敛
```

`C4`（FeatureRuntime 进程键语义）与 `C5`（删除 migration residue）可与 F2 并行。

F2 是功能库存，不是功能实现。

## Phase F-R3 remaining A14-only holds

These two A14 product keys have no A13 counterpart. They are **not** ports;
they are ROM/device evidence holds recorded in `docs/parity/A13_PHASE_F_HOLD_EVIDENCE.md`.

```text
various_clear_update_state            = HOLD_EVIDENCE
  PORT_CLASS                          = BLOCKED_BY_A13_PLATFORM
  WHY                                 = HyperOS updater-services bridge + Settings.Global keys
  STATIC_VERIFIED                     = YES (A14 owner + A13 absence)
  BUILD_VERIFIED                      = NO (no production)
  DEVICE_EVIDENCE                     = UNVERIFIED

various_disable_defraud_apps_detect   = HOLD_EVIDENCE
  PORT_CLASS                          = BLOCKED_BY_A13_PLATFORM
  WHY                                 = GuardProvider DexKit AntiDefraudAppManager unproven on MIUI 14
  STATIC_VERIFIED                     = YES (A14 owner + A13 absence)
  BUILD_VERIFIED                      = NO (no production)
  DEVICE_EVIDENCE                     = UNVERIFIED
```
