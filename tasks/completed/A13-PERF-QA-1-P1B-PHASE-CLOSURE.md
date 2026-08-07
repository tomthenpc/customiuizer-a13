# A13-PERF-QA-1 — P1B Phase Closure, Correctness Audit and Release Verification

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-QA-1` |
| 分支 | `devin/a13-memory-performance-optimization` |
| QA 起点 commit | `b8eead54594bfbc850309a1ead2f617a8122b411` |
| 状态 | `STATIC_CLOSURE_READY_FOR_SIGNED_RELEASE` |
| QA 终点 commit | 待填入 |
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

## 静态/Release 验证

- `python -m compileall tools`：PASS
- `python -m unittest discover -s tools/tests -p "test_*.py"`：PASS（1043 项，2 项跳过）
- `python tools/a13_hook_cost_scan.py --verify`：PASS
- `python tools/source_hazard_scan.py`：PASS
- `python tools/verify.py full`：PASS
- `gradlew :app:testDebugUnitTest --rerun-tasks --no-build-cache --no-daemon`：PASS（1046 JVM 测试）
- `gradlew :app:compileReleaseKotlin :app:compileReleaseJavaWithJavac :app:testReleaseUnitTest :app:lintRelease :app:minifyReleaseWithR8`：PASS
- `gradlew :app:lintVitalAnalyzeRelease :app:lintVitalReportRelease`：PASS
- `gradlew :app:signingReport`：release signing resolved
- `gradlew :app:assembleRelease`：BUILD SUCCESSFUL
- `apksigner verify --verbose --print-certs`：Verifies=true，v2=true，signers=1，cert SHA-256=15CE32F03E4D8E62DF9390F77431862E59BF2CF95CD5A72F0C7330CDFCCA2934
- artifact identity：package=`tv.withaibuild.customiuizer.r13`，versionCode=135，versionName=`r13.10.1`，debuggable=false，testOnly=false
- `git diff --check`：PASS

## 完成定义

- 所有 P1B Slice QA 状态已更新为 `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING` 或 `QA_CONDITIONAL` 并记录阻塞原因。
- P0 真机基线保持 `RUNTIME_BASELINE_PENDING_DEVICE`。
- 正式签名 Release APK 构建并通过 `apksigner` 验证。
