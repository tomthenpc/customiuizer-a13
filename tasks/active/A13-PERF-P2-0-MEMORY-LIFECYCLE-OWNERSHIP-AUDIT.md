# A13-PERF-P2-0 — Memory & Lifecycle Ownership / Retention Topology Audit

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-P2-0` |
| 分支 | `devin/a13-memory-performance-optimization` |
| Base SHA | `283e731b9f998c4fe188d919e3bddae1c0a5648c` |
| 状态 | `AUDIT_COMPLETE` |
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

- Scanner 候选：282
- 风险：HIGH=4, MEDIUM=96, LOW=57, INFO=117, UNKNOWN=8
- 手动补充计数：0 个新的 HIGH/CRITICAL（手动 grep 已确认覆盖）
- 误报 / 良性计数：122（117 SAFE_STABLE_METADATA + 5 PROCESS_LIFETIME_INTENTIONAL）
- 推荐 P2-1：`SubFragment.kt` 中 `view?.postDelayed` smooth-scroller 延迟回调清理

## P2-1 推荐

```text
RECOMMENDED_P2_1 = SubFragment.kt smooth-scroller delayed callback cleanup
```

- 生命周期失配：Fragment/View 向 Handler 投递延迟 Runnable，捕获 `smoothScroller` 与 `mList`。
- 未证明释放：SubFragment 未在 `onDestroyView`/`onPause` 中 `removeCallbacks`。
- 可静态验证：添加 Runnable 字段并在视图销毁路径中移除即可，无需真机即可证明正确性。
- 范围小、回归风险低、不触及 P1B 冻结切片。
