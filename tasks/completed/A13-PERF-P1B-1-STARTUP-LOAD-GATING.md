# A13-PERF-P1B-1 — SystemUI and Launcher Startup Load Gating

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-P1B-1-STARTUP-LOAD-GATING` |
| 分支 | `devin/a13-memory-performance-optimization` |
| 起点 commit | `cd89c38f02834db5baf24e5ab08b345c187085fe` |
| 状态 | `COMPLETED` |
| 终点 commit | 44b4c4c |
| P0 真实运行时基线 | `RUNTIME_BASELINE_PENDING_DEVICE` |
| 授权范围 | 仅启动加载路径优化；禁止进入一般 Hook 热路径、AudioService、Tile、通知菜单、Launcher 手势、重复 Hook 合并、生命周期监听注销、UI 页面、PackagePermissions、构建系统、R8、新功能 |

## 目标

在不改变现有功能效果、默认行为和偏好生效语义的前提下，减少以下启动路径在无相关功能启用时产生的无效工作：

1. `SystemUIApplication#onCreate` 对应的 SystemUI 功能加载路径。
2. `Launcher Application#attach` 对应的 Launcher 功能加载路径。

本任务只处理启动阶段的功能族门控、门控前不必要的类引用/注册器/dispatcher/反射/监听器，以及功能族全部关闭时仍执行的初始化。

## 禁止项

- 一般 Hook 回调热路径优化。
- AudioService。
- Quick Settings Tile 创建热路径。
- 通知菜单构建热路径。
- Launcher 手势点击或滑动回调。
- 重复 Hook 合并。
- 生命周期监听注销。
- UI 页面性能。
- PackagePermissions 修改（仅可标记为 `MANDATORY_ALWAYS_ON_FEATURE`）。
- 构建系统、签名配置、R8 规则修改。
- 新功能或新设置。

## 验收标准

- SystemUI 相关功能全部关闭时，可避免的功能初始化不再执行。
- Launcher 相关功能全部关闭时，可避免的功能初始化不再执行。
- 功能开启时原 Hook 和初始化路径保持完整。
- 默认行为不变。
- 偏好动态生效语义不变（需要重启的才允许启动时门控；支持即时开启的不得破坏）。
- 不相关功能类不在门控前引用。
- 不新增 Hook。
- 不扩大进程或包覆盖范围。
- 不新增线程、Handler、监听器、observer、缓存或依赖。
- 静态扫描稳定。
- 全部单元测试和 `python tools/verify.py full` 通过。
- Release 编译、R8 和 `lintVitalRelease` 通过；`packageRelease` 因签名配置缺失失败需单独报告。
- 工作区干净，提交已推送。
- 不声称未经真机测量的性能收益。
