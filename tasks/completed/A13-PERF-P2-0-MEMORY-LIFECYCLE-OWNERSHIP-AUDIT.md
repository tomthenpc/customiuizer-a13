# A13-PERF-P2-0 — Memory & Lifecycle Ownership / Retention Topology Audit

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-P2-0` |
| 分支 | `devin/a13-memory-performance-optimization` |
| Base SHA | `f28c3373a0b8a518788d84c3054274c609247444` |
| 状态 | `QA_ACCEPTED` |
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
- 风险：HIGH=0, MEDIUM=122, LOW=66, INFO=303, UNKNOWN=68
- raw HIGH：17（全部 source 复核，0 个保留为 final HIGH）
- raw CRITICAL：0
- final HIGH：0
- final CRITICAL：0
- NEEDS_MANUAL_REVIEW：0
- NEEDS_ROM_EVIDENCE：151
- 手动补充计数：0 个新的 HIGH/CRITICAL（手动 grep 已确认覆盖）
- 误报 / 良性计数：348（303 SAFE_STABLE_METADATA + 45 PROCESS_LIFETIME_INTENTIONAL）
- 推荐 P2-1：`SubFragment.kt` 中 `view?.postDelayed` smooth-scroller 延迟回调清理

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
P2-0 = QA_ACCEPTED
P2-1 = NOT_STARTED
P2-2 = NOT_STARTED
JDK25 = no
```
