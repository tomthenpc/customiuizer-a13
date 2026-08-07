# A13-PERF-P2-0 — Memory & Lifecycle Ownership / Retention Topology Audit

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-P2-0` |
| 分支 | `devin/a13-memory-performance-optimization` |
| Base SHA | `4dbe02599bfe09ea7efb5b0d94c2f35cb614d72a` |
| 状态 | `QA_ACCEPTED / FROZEN` |
| Production changes | `FORBIDDEN` |
| P1B / QA-1 | `SEALED` |

## 目标

建立 A13 当前生产代码完整的 ROOT → RETAINED OBJECT → REGISTRATION/CALLBACK EDGE → OWNER → LIFETIME → RELEASE/REPLACEMENT PATH → CARDINALITY → RISK 保留拓扑，为 P2-1 / P2-2 的真实内存生命周期整改选择目标。

本轮只进行审计、库存和优先级排序，禁止修改 `app/src/main/**` 任何生产代码。

## 审计范围

- `app/src/main/java/**`
- 覆盖进程：`android / system_server`、`com.android.systemui`、`com.miui.home / Launcher`、模块自身 app process
- 排除：`app/src/test`、`build/`、`generated/`、`docs/`、`tools test fixtures`

## 产出

- `tools/a13_memory_lifecycle_scan.py`：候选发现扫描器
- `tools/tests/test_a13_memory_lifecycle_scan.py`：扫描器回归测试
- `docs/audit/A13_MEMORY_LIFECYCLE_INVENTORY.json`：保留库存
- `docs/audit/A13_MEMORY_LIFECYCLE_TOPOLOGY.md`：拓扑与 Top 10 候选

## 完成定义

- 扫描器稳定、确定性、路径无关
- 覆盖 static root、receiver、observer、listener、callback、Handler、Thread/Executor、WeakReference、AdditionalInstanceField、collection
- 所有 HIGH/CRITICAL 候选手工复核
- Top 10 按生命周期失配、缺失释放、基数排序
- 推荐一个明确的 P2-1 首个整改 slice
- production diff = 0

## P2-0 结果

- Scanner 候选：559
- 风险：HIGH=0, MEDIUM=96, LOW=92, INFO=303, UNKNOWN=68
- raw HIGH：26（R1 raw HIGH = 17；R2/R3 final scanner raw HIGH = 26；全部 source 复核，0 个保留为 final HIGH）
- raw CRITICAL：0
- final HIGH：0
- final CRITICAL：0
- NEEDS_MANUAL_REVIEW：0
- NEEDS_ROM_EVIDENCE：151
- 手动补充计数：0 个新的 HIGH/CRITICAL（手动 grep 已确认覆盖）
- 误报 / 良性计数：348（303 SAFE_STABLE_METADATA + 45 PROCESS_LIFETIME_INTENTIONAL）
- Collection 复核结果：
  - `UNBOUNDED_OWNER_COLLECTION`：0（原 26 个全部按 element/value 类型重新分类）
  - `PROCESS_LIFETIME_METADATA_COLLECTION`：25（XposedHelpers 反射缓存、FeatureInstallRegistry、Helpers AppData/ModData、DiagnosticRecorder、ModuleHelper registries、StepCounterController stepViews 等）
  - `PROCESS_LIFETIME_CONFIG_COLLECTION`：1（`SystemFreeformAndMultiWindowHooks.fwApps`）
  - `UNKNOWN_COLLECTION_CARDINALITY`：0
- 推荐 P2-1：`SubFragment.kt` 中 `view?.postDelayed` smooth-scroller 延迟回调清理

## R3 修正点

- 修正 `UNBOUNDED_OWNER_COLLECTION` 泛化逻辑：不再把 `ArrayList<String>` / `Map<String, String>` / reflection cache 等 blanket 标为 owner collection。
- 新增 collection 分类：`PROCESS_LIFETIME_METADATA_COLLECTION`、`PROCESS_LIFETIME_CONFIG_COLLECTION`、`UNKNOWN_COLLECTION_CARDINALITY`。
- 26 个原 `UNBOUNDED_OWNER_COLLECTION` 逐条按 element/value 类型复核：25 个降为 metadata/state，1 个 `fwApps` 归为 config。
- 修正 raw HIGH ledger：`raw HIGH = 26`（R1 17 → R2/R3 26），`final HIGH = 0`。
- 修正 review 计数：区分 `RAW HIGH/CRITICAL SOURCE-REVIEWED` 与 `FINAL HIGH/CRITICAL`。
- 修正 Top10 / Top3 排序：证据不确定性作为优先级因素，`NEEDS_ROM_EVIDENCE` 候选可进入 Top10；新增 `Top evidence-pending` 列表。
- P2-1 推荐保持为 `SubFragment.kt` smooth-scroller，独立于 Top1。
- 新增 6 个 collection 语义回归测试。

## P2-1 推荐

```text
RECOMMENDED_P2_1 = SubFragment.kt smooth-scroller delayed callback cleanup
```

- 生命周期失配：Fragment/View 向 Handler 投递 380ms 延迟 Runnable。
- 释放窗口有限：单次 `highlightKey = null` 触发，延迟固定且较短；未证明无限重入或生命周期结束后仍持有。
- 改进价值：在 `onDestroyView` / `onPause` 中添加 `removeCallbacks` 可消除最后的不确定性，属于生命周期卫生改进。
- 可静态验证：添加 `Runnable?` 字段并在视图销毁路径中 `removeCallbacks` 即可通过单测验证。
- 范围小、回归风险低、不触及 P1B 冻结切片。

## 最终状态

```text
P2-0 = QA_ACCEPTED / FROZEN
P2-1 = NOT_STARTED
P2-2 = NOT_STARTED
P1B = SEALED
QA-1 = SEALED
P1B-4A = ROM_LIFECYCLE_EVIDENCE_PENDING
JDK25_MIGRATION = READY_TO_BRANCH
A13_PERFORMANCE_STABLE_BASE = <R3 FINAL SHA>
```
