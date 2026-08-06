# A13-PERF-QA-1 — P1B Phase Closure, Correctness Audit and Release Verification

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-QA-1` |
| 分支 | `devin/a13-memory-performance-optimization` |
| QA 起点 commit | `b8eead54594bfbc850309a1ead2f617a8122b411` |
| 状态 | `IN_PROGRESS` |
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

## 重点事项

1. 修正 `tasks/active/` 与 `tasks/completed/` 路径与内部状态不一致。
2. 修复 P1B-1 `LauncherInstaller` 启动门控未接入 `MainModule` 路由的问题。
3. 建立 `SystemUI` / `Launcher` 启动门控完整性门禁。
4. 重新打开并纠正 P1B-4A 菜单生命周期合同与测试。
5. 复核 P1B-2 `AudioService` 偏好生效语义与异常降级。
6. 复核 P1B-3 `Tile` 多 spec 共享类绑定。
7. 独立审计 P1B-4B。
8. 正式签名 Release 构建与 `apksigner` 验证。
9. mutation-style 反证测试。

## 完成定义

- 所有 P1B Slice QA 状态更新为 `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING` 或 `QA_CONDITIONAL`/`QA_REJECTED` 并记录阻塞原因。
- QA 任务文件移动到 `tasks/completed/`。
- P0 真机基线保持 `RUNTIME_BASELINE_PENDING_DEVICE`。
