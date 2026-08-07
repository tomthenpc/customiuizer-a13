# A13-PERF-P1B-2 — AudioService Hot-Path Cost Reduction

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-P1B-2-AUDIOSERVICE-HOT-PATH` |
| 分支 | `devin/a13-memory-performance-optimization` |
| 起点 commit | `3d38cdd53a6190c68187a803badaf201dfda25cd` |
| 状态 | `QA_ACCEPTED_DEVICE_EVIDENCE_PENDING` |
| 终点 commit | `5f780b8a15727114bd29f01188191a2520ff2509` |
| Original closure | `5f780b8a15727114bd29f01188191a2520ff2509` |
| QA corrective production | `c661e4c33b913a6abcef08ae32e08977ec2b566c` |
| QA corrective metadata | `bc0363c8e10303b0dbfdca35ba5a7363f88a500d` |
| engineering provenance | `c661e4c` 后 production 无回退；`git diff c661e4c..HEAD -- app/src/main/java/tv/withaibuild/customiuizer/mods/SystemAudioAndVolumeHooks.kt` 为空 |
| note | `5f780b8a...` 是原始工程 closure；QA corrective 以 `c661e4c...`（production）和 `bc0363c...`（metadata）组成最终证据链 |
| P0 真实运行时基线 | `RUNTIME_BASELINE_PENDING_DEVICE` |
| 授权范围 | 仅 `AudioService$VolumeStreamState#readSettings` 与 `AudioService#createStreamStates` 两个目标路径的辅助逻辑 |

## 目标

在不改变 AudioService 音量行为、默认音量值、流映射、MIUI/HyperOS 1 兼容分支、失败降级路径和偏好生效语义的前提下，减少 `createStreamStates` 启动阶段和 `VolumeStreamState#readSettings` 高频回调中的重复反射、不必要的偏好读取与临时对象分配。

## 授权修改范围

- 回调期间重复的稳定反射查询。
- 回调期间不必要的偏好读取或重复解析。
- 回调期间可避免的临时对象分配。
- `createStreamStates` 启动阶段可提前完成的稳定元数据解析。
- `readSettings` 高频或重复调用路径中的不变量外提。
- 失败路径中的重复反射尝试或重复日志。

## 禁止项

- 其他 AudioService 方法。
- 音量 UI、SystemUI Tile、通知面板和通知菜单。
- Launcher、重复 Hook 合并、UI 页面、PackagePermissions、进程入口架构。
- 新功能、新偏好、新兼容范围、Android 14、HyperOS 2。
- 构建配置、签名配置。

## 缓存与状态约束

- 只允许缓存稳定反射元数据（Class / Method / Field / Constructor / 不可变方法签名 / 小型不可变标志）。
- 禁止缓存 AudioService 实例、VolumeStreamState 实例、Context、Handler、Binder、View、Activity、SystemUI 对象或任何短生命周期 Android owner。
- 禁止无上限 Map / List 或按对象实例增长的缓存。
- 不新增通用反射缓存框架。

## 验收标准

- `readSettings` 中可提前完成的稳定反射不再按回调重复执行。
- `createStreamStates` 中不重复解析相同稳定元数据。
- 不新增 Android owner 强引用、无上限缓存、线程、Handler、listener 或 observer。
- 不增加 Hook call site、不扩大进程覆盖范围。
- 偏好生效语义、默认音量行为、system_server 失败路径保持不变。
- 新增/扩展 JVM 单元测试覆盖给定场景。
- 更新 `a13_hook_cost_scan.py` 回归门禁与审计文档。
- 全部 Python 测试、Android 编译测试、lint、R8、正式 Release 打包签名验证通过。
- 工作区干净并推送。
- 不声明未经真机测量的性能收益。

## 验证结果

| 验证项 | 命令 | 状态 |
|--------|------|------|
| Android 单元测试 | `gradlew :app:testDebugUnitTest` | PASS |
| 快速验证（AudioService 相关测试） | `python tools/verify.py fast --tests AudioServiceHotPath` | PASS |
| 快速验证（变更） | `python tools/verify.py fast --changed` | PASS |
| 完整验证 | `python tools/verify.py full` | PASS |
| Python 工具编译 | `python -m compileall tools` | PASS |
| Python 单元测试 | `python -m unittest discover -s tools/tests -p "test_*.py"` | PASS |
| 构建 legacy exception registry | `tools/build_legacy_exception_registry.py --build` | PASS |
| Hook 成本扫描稳定性 | `tools/a13_hook_cost_scan.py --verify-stability` | PASS |
| 其他历史验证 | `git diff --check` | OK |
| 其他历史验证 | `gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| 真实运行时基线 | — | `RUNTIME_BASELINE_PENDING_DEVICE` |

## 证据

- `docs/audit/A13_AUDIOSERVICE_CORRECTNESS_AUDIT.md` 已创建，记录 A13-PERF-QA-1 / P1B-2 行为矩阵、修正点、测试证据与验证结果。
