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

## 产品决策

灵动额头 / Dynamic Island 明确排除：

```text
PORT   = NO
REASON = PRODUCT_DECISION
```

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

## 示例记录：Issue #2 FSG 第三方桌面

```text
FEATURE_ID:               ISSUE_2_FSG_THIRD_PARTY_LAUNCHER
USER_VISIBLE_NAME:        Horizontal gestures / 水平手势
A14_STATUS:               PRESENT_EQUIVALENT
A13_STATUS:               MISSING_PORT_REQUIRED
A14_PREF_KEY:             controls_fsg_horiz
A13_PREF_KEY:             controls_fsg_horiz
PROCESS:                  com.miui.home
UPSTREAM_SEMANTIC_SOURCE: BaseRecentsImpl FSG lifecycle
A14_IMPLEMENTATION_SOURCE: A14 LauncherGestureHooks.FSGesturesHook
A13_TARGET:               com.miui.home.recents.BaseRecentsImpl
PORT_CLASS:               A13_VARIANT_REQUIRED
STATIC_EVIDENCE:          DONE (A13 launcher 4.39.14.8060)
BUILD_EVIDENCE:           PENDING
DEVICE_EVIDENCE:          PENDING
TEST_STATUS:              PENDING
FINAL_STATE:              SELECTED_STATIC_CANDIDATE DESIGN_E_BACK_STUB_ONLY_RECOVERY
```

Issue #2 的持久状态：

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

F0 不开始架构 parity 或功能移植。
