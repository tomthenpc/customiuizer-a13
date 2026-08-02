# Progress Scoring Model v6

```text
DocumentKind: CURRENT
Product: A13
Source: A13_A14_PROGRESS_CONTROL_FINAL_V6
```

## 1. 目的

进度不得按以下指标计算：

```text
commit 数
ahead main 数
文档行数
测试数量
checkpoint 数
Agent 工作时长
```

这些只能说明活动量，不能说明接近总目标。

## 2. 权重

| Capability domain | Weight |
|---|---:|
| Baseline and autonomous control | 8 |
| Runtime architecture and Feature/Hook ownership | 22 |
| Runtime safety, lifecycle and concurrency | 18 |
| Performance, memory, APK and R8 | 12 |
| ROM intelligence and compatibility evidence | 10 |
| Java/Kotlin boundary and maintainability | 8 |
| Build, CI, signing, artifact and release engineering | 12 |
| Current documentation and provenance | 5 |
| Device validation | 5 |
| Total | 100 |

## 3. 计分证据

每个 domain 只能按下列证据计分：

```text
STATIC_PROVEN
BUILD_VERIFIED
CI_VERIFIED
DEVICE_VERIFIED
```

仅添加规则、计划或文档，不代表规则已经落地。

### 不能计分

- PLAN 文档；
- TODO 列表；
- state-only commit；
- overlay/governance install；
- 未运行的 checker；
- 未配置的 CI；
- 未构建的 signed artifact；
- NOT_EXERCISED device checklist；
- “应该”“计划”“预计”。

### 部分计分

- 代码完成但只 static；
- 本地 Full 通过但无 CI；
- contract guarded 但无 ROM sample；
- lifecycle 有局部测试但 owner inventory 未闭环；
- APK baseline 存在但不可变性/delta 未完成。

## 4. 双进度

每次报告必须同时输出：

```text
MachineProgress
ProjectProgress
```

`MachineProgress` 排除 DEVICE_VALIDATED 的 5 分，但保留 signed RC、CI、ROM sample 等机器/外部交叉要求。

`ProjectProgress` 包含完整 100 分。

## 5. 状态分类

```text
0-19: FOUNDATION
20-39: CORE_RECONSTRUCTION
40-59: SYSTEM_HARDENING
60-74: INTEGRATION_AND_EVIDENCE
75-89: RELEASE_CANDIDATE_PREPARATION
90-99: EXTERNAL_VALIDATION
100: PROJECT_COMPLETE
```

长期 LTS/ARCHIVE 是 PROJECT_COMPLETE 后的生命周期，不用于把当前完成度无限延后。
