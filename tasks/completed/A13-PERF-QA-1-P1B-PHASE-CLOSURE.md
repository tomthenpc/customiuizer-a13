# A13-PERF-QA-1 — P1B Phase Closure, Correctness Audit and Release Verification

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-QA-1` |
| 分支 | `devin/a13-memory-performance-optimization` |
| QA 起点 commit | `b8eead54594bfbc850309a1ead2f617a8122b411` |
| 状态 | `COMPLETED_STATIC_DEVICE_EVIDENCE_PENDING` |
| QA 终点 commit | `THIS_COMMIT`（本文件最终封版 commit；exact SHA 见最终执行报告） |
| P0 真实运行时基线 | `RUNTIME_BASELINE_PENDING_DEVICE` |

## 审计对象

- P1B-0: `AndroidPackageInstaller` 零功能成本。
- P1B-1: `SystemUI` / `Launcher` 启动门控。
- P1B-2: `AudioService` 热路径。
- P1B-3: `Quick Settings Tile` 热路径。
- P1B-4A: `Notification Menu` 创建路径。
- P1B-4B: `Notification Intent` 启动路径。

## 核心原则

- 本轮不得声明具体内存、CPU、延迟或功耗收益。
- 编译、扫描器或自动生成 JSON 通过，不等同于行为正确。
- 结论必须同时具备源码证据、行为测试和构建证据。
- 不开始 P1B-5 及后续热路径优化、新功能、新 Hook、main 合并或 Release 发布。

## 阶段 QA 总表

见 `docs/audit/A13_P1B_PHASE_QA.md`。

## 最终阶段结论

- P1B-0 = `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`
- P1B-1 = `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`
- P1B-2 = `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`
- P1B-3 = `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`
- P1B-4A = `QA_CONDITIONAL` / `ROM_LIFECYCLE_EVIDENCE_REQUIRED`
- P1B-4B = `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING`

P0 真实运行时基线继续保持 `RUNTIME_BASELINE_PENDING_DEVICE`；P1B-4A ROM lifecycle evidence 继续独立 pending。

## 历史 Release 证据（来自 `4d1203be5f6b73238903c6c17a97256f3579d396`，prior signed Release，不作为新 metadata corrective HEAD 证据）

```text
PRIOR_SIGNED_RELEASE_LOCAL_EVIDENCE
HEAD = 4d1203be5f6b73238903c6c17a97256f3579d396
APK SHA-256 = 903C3E5443568328F6F9AFD8BA343A7572760B17D859F60B2AE265A92557F5A8

apksigner reported:
Verifies = true
Verified using v2 scheme = true
Number of signers = 1
certificate SHA-256 = 15CE32F03E4D8E62DF9390F77431862E59BF2CF95CD5A72F0C7330CDFCCA2934

artifact identity:
applicationId = tv.withaibuild.customiuizer.r13
versionCode = 135
versionName = r13.10.1
debuggable = false
testOnly = false
```

> 上述 prior artifact 不担保新的 metadata corrective HEAD；本轮从 `QA1_CORRECTED_FINAL_HEAD` 重新构建并重新验证后的 APK 见下文。

## 当前 corrective metadata 完成定义

- 所有 P1B Slice QA 状态已更新为 `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING` 或 `QA_CONDITIONAL` 并记录阻塞原因。
- P1B-1 / P1B-3 / P1B-4B / QA-1 内部错误 SHA 已修正为 `git rev-parse` 精确值。
- P1B-2 provenance 明确区分原始 closure、QA corrective production 与 QA corrective metadata。
- 新增 `tools/tests/test_a13_p1b_phase_provenance.py`，确保 phase ledger 中所有 authoritative full SHA 均能被 `git cat-file -e` 验证。
- P0 真机基线保持 `RUNTIME_BASELINE_PENDING_DEVICE`。
- 从最终 metadata corrective HEAD 重建正式签名 Release APK 并通过 `apksigner` 验证。
