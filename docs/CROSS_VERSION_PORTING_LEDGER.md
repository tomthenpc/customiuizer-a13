# CustoMIUIzer A13 / A14 跨版本同步台账

> 记录 A14 工程方法与修复哪些可以/已经同步到 A13，哪些因 ROM 边界或架构差异不能机械移植，以及每项的对应提交、测试与实机状态。
> 本台账不替代 Git 历史、构建产物或实机日志；每次继续工作前应重新核对分支和测试证据。

## 基线

- A13 仓库：`tomthenpc/customiuizer-a13`
- A13 当前维护分支：`main` @ `66f22a7`
- A13 长期运行边界：MIUI 14 / Android 13，applicationId `tv.withaibuild.customiuizer.r13`
- A14 仓库：`tomthenpc/customiuizer-a14`
- A14 当前正式版本：`r14.13.6` / `be5191b5`
- 共享 libxposed 边界：`minApiVersion=101`、`targetApiVersion=102`、`staticScope=false`
- 共享 ABI：`arm64-v8a`
- 共享目标：功能关闭时接近零额外成本，高频路径避免不必要分配、反射、阻塞和日志。

## 可同步修复（已实现或本轮实现）

| 编号 | 主题 | A14 来源 | A13 实现 | 关键提交 | 平台差异 | 测试 | 实机状态 |
|---|---|---|---|---|---|---|---|
| C-01 | RemotePreferences 空快照不永久标记已加载 | Claude 审计 | `MainModule.java` 空快照返回且不置 `prefsLoaded` | `4b386cd` / `d578e80` | 无 | 单元测试 + lint | 已发布 |
| C-02 | 偏好监听器注册成功后置位 | Claude 审计 | `MainModule.java` 调整注册/状态顺序 | `4b386cd` / `d578e80` | 无 | 单元测试 + lint | 已发布 |
| C-03 | `PrefPair` 替代 `\|` Regex 解析 | Claude 审计 | `PrefPair.kt` + `PrefPairTest.kt` | `4b386cd` / `d578e80` | 无 | 单元测试 | 已发布 |
| C-04 | SystemUI 状态栏文本图标弱引用 | Claude 审计 | `SystemUI.java` 静态 `WeakReference<View>` 集合 | `4b386cd` / `d578e80` | A13 保留 `TextIcon` tag / 左右侧 / `setBlocked` 语义 | lint + 构建 | 待实机验证 |
| C-05 | Hook 回调统一异常边界 | A14 K11 加固 | `ModuleHelper.guarded` 覆盖 Receiver/Handler/Runnable/Observer | `b538d55` / `7c1b146` 等 | 无 | 单元测试 | 已发布 |
| C-06 | Receiver/Observer 所有权注册模型 | A14 K11 加固 | `registerModuleReceiver` / `registerOwnedReceiver` / `replaceModuleRegistration` | `31d60ba` / `a642994` 等 | 无 | 单元测试 + 负向验证 | 已发布 |
| C-07 | 热路径参数零复制 | A14 K11 加固 | 只读参数改用 `Chain.getArg(i)` / `chain.args` | `7c1b146` / `b538d55` 等 | 无 | 构建 + lint | 已发布 |
| C-08 | 反射缓存命中零分配 | A14 K12 加固 | 字段/无参方法 `Class -> name -> member` 两级嵌套 | `6080fa3` 之后 | 无 | `ReflectionCacheAllocationTest` | 已发布 |
| C-09 | Additional instance field 按 identity | A14 K13 加固 | 后端改为 identity 比较 | `eb03775` 之后 | 无 | 迁移审计 | 已发布 |
| C-10 | DeviceInfoMonitor 生命周期 | A14 K12 加固 | 按屏幕状态暂停/恢复、generation、有界退避 | `a1ca520` / `aae21d7` | A13 sysfs 路径/节点可能与 A14 不同 | 单元测试 | 待实机验证 |
| C-11 | 应用图标有界队列与去重 | A14 K13 加固 | 共享有界 executor、in-flight 去重、字节 LRU | `eef4fc8` / `6859029` | 无 | 单元测试 | 已发布 |
| C-12 | Locale 统一写入 `LocaleManager` | A14 K14 加固 | `AppLocaleController` 走 `LocaleManager.applicationLocales` | `6080fa3` 等 | Android 13 支持 `LocaleManager` | 单元测试 | 已发布 |
| C-13 | 搜索预计算索引 | A14 K14 加固 | 预计算 key、生成号防止旧结果覆盖 | `6080fa3` 等 | 无 | 单元测试 | 已发布 |
| C-14 | AudioVisualizer 单帧调度 | A14 K15 加固 | 31 个 `ValueAnimator` 收口为单调度器，generation latest-wins | `aae21d7` 等 | 无 | 单元测试 | 待实机验证 |
| C-15 | 锁屏专辑图有界 worker | A14 K16 加固 | `LockScreenAlbumArtController` + `AlbumArtPolicy`、单 worker、按字节缓存 | `eb03775` 等 | A13 锁屏布局与 A14 不同，保留 A13 target | 单元测试 | 待实机验证 |
| C-16 | 设置 UI 与 A14 对齐 | UI polish | 状态栏/导航栏、About、语言切换、搜索、弹窗样式 | `66f22a7` / `e0e2539` / `12d9173` 链 | A13 资源与 A14 不完全一致 | lint + 构建 | 待实机验证 |
| C-17 | `StepCounterController` 生命周期 | 本轮（K18 后续） | `StepCounterController.kt` 弱引用、熄屏停止、亮屏恢复、按需注册、后台查询；`StepCounterController.Lifecycle` 可单测 | `cbf2ad3` / `7cf3f1e` | 无 | 单元测试 + 构建 | 待实机验证 |
| C-18 | 状态栏秒针 `MiuiStatusBarClockController` 生命周期 | 本轮（K18 后续） | `SystemStatusBarClockAndMoreHooks` 屏幕 on/off 控制、避免重复任务；`SecondTickerState` 可单测 | `cbf2ad3` / `7cf3f1e` | 无 | 构建 + 单测 | 待实机验证 |
| C-19 | `HookUtils` 从 `Helpers` 机械拆分 | 本轮 | `HookUtils.kt` 承载 Hook 进程轻量工具，`Helpers` 不再被 `system_server`/SystemUI/Launcher 加载 | `d70727d` | A13 `getAppName`/`getAppIcon` 与 A14 归属不同，仍按 A13 调用链处理 | 构建 + lint + 单测 | 待实机验证 |
| C-20 | 统一 `release-manifest.json` | 本轮 | `docs/release-manifest.json` 记录版本、commit、APK、SHA-256、证书、SDK、ABI、libxposed API；区分 build-time / post-release 字段 | `d85c52a` / 本轮 | 无 | JSON 校验 | 无实机 |
| C-21 | `HookUtils`/`Helpers` 重复实现审计注释 | 本轮 | `Helpers.kt`/`HookUtils.kt` 顶部 KDoc 明确职责边界，防止 hook 代码再次引用 `Helpers` | 本轮 | 无 | lint + 文档审阅 | 无实机 |

## 不能机械移植的项目（按 ROM 或架构差异）

| 编号 | 项目 | 原因 | A13 处理 | A14 参考 |
|---|---|---|---|---|
| D-01 | Hook target 类名 | Android 14 / HyperOS 1 的 SystemUI、Launcher、Settings 类结构不同 | 保持 A13 MIUI 14 类名和 method/field 签名，仅提取工程方法 | `HookUtils` 定位，不复制 Hook 目标 |
| D-02 | 状态栏布局 / 控制中心 XML 资源 | A14 控制中心插件拆分、状态栏第二行逻辑不同 | UI 只对齐日间/夜间颜色和 About 文本，不动布局Hook | `r14.13.6` 资源 |
| D-03 | Android 14 权限与 `RECEIVER_NOT_EXPORTED` 语义 | 部分广播在 A14 需要 `RECEIVER_EXPORTED` 或显式 user | A13 维持现有 flag，仅在未指明处补 `RECEIVER_NOT_EXPORTED` | `registerOwnedReceiver` 实现 |
| D-04 | A14 `getResId` 缓存 | A13 当前调用点直接使用 `resources.getIdentifier`；引入缓存需先确认所有 hook 进程共享同一 Resources 语义 | 本轮不引入，留待后续审计 | `HookUtils.getResId` |
| D-05 | A14 `constrain`/`lerp` 等数学函数 | A13 当前调用点集中在 `convertGammaToLinearFloat` 与亮度曲线；可直接平移，但需保持原精度 | 本轮随 `HookUtils` 平移，不修改调用语义 | `HookUtils.constrain` / `lerp` |
| D-06 | `AppDataAdapter` / 搜索 UI 架构 | A14 已全面 Kotlin，A13 仍有 Java 遗留部分；UI 拆分策略不同 | 保持 A13 现有 UI 迁移节奏，不重构整体架构 | A14 K13-K14 |

## 本轮新增（r13.7.1-maintenance-foundation）

1. `docs/CROSS_VERSION_PORTING_LEDGER.md` 本身（过程资产）。
2. `docs/release-manifest.json`：统一发布资产描述。
3. `tv.withaibuild.customiuizer.utils.HookUtils`：从 `Helpers` 拆分 Hook 进程工具。
4. `StepCounterController` 与状态栏秒针生命周期优化。
5. `MainModule`/偏好 Schema/Hook 目标能力/诊断体系审计实施计划（仅输出计划，不落地）。

## 测试与实机状态图例

- `已发布`：已随正式 Release 发布且至少通过发布门禁。
- `待实机验证`：代码/构建/静态测试通过，尚未在 MIUI 14 / Android 13 设备上验证。
- `无实机`：纯文档/工程资产，无需实机。
- `代码层面确认`：通过单测、lint、diff review 确认，但未构建或未跑完整 R8。

## 更新记录

- `2026-07-30`：新建台账，合并 K11-K18 已发布项、本轮维护项和 A14/A13 差异项。
- `2026-07-30`：补充 C-17..C-20 关键提交哈希、增加 C-21 审计注释行，同步 release-manifest 元数据。
