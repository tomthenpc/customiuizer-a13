# A13-PERF-P1B-4A — Notification Menu Creation Hot-Path Reduction

## 元数据

| 字段 | 值 |
|------|-----|
| 任务 | `A13-PERF-P1B-4A-NOTIFICATION-MENU-HOT-PATH` |
| 分支 | `devin/a13-memory-performance-optimization` |
| 起点 commit | `ec05f5e948167742da6520cdf64b9fd32d360b3e` |
| 状态 | `ENGINEERING_COMPLETE_DEVICE_EVIDENCE_PENDING` |
| 终点 commit | `395e09338d6a9b663847d91ebddb06cb2ee9c44e` |
| P0 真实运行时基线 | `RUNTIME_BASELINE_PENDING_DEVICE` |
| 授权范围 | 仅 `MiuiNotificationMenuRow#createMenuViews` 及该回调直接调用、属于本模块的通知菜单辅助逻辑 |

## 目标

在不改变 MIUI 14 / HyperOS 1 Android 13 现有通知菜单功能、项顺序、图标、文字、点击行为、异常回退和偏好生效语义的前提下，降低 `createMenuViews` 回调中的重复稳定反射、临时对象分配和重复状态查询。

## 授权修改范围

- `createMenuViews` `after` 回调中的稳定反射查询（`getObjectField`、`callMethod`、`findClass`）。
- 回调中的重复 `Dependency` / `ModalController` / `AppMiniWindowManager` 类/方法查找。
- 回调中的临时 `Constructor` 访问、参数数组和 `Iterator`。
- 功能关闭时仍执行的模块菜单处理（`system_notifrowmenu` 门控）。
- 失败目标在每次菜单创建时的重复反射与重复日志。

## 禁止范围

- 不处理 `MiuiStatusBarNotificationActivityStarter#startNotificationIntent`。
- 不处理通知点击、启动 Intent、解锁后启动。
- 不处理通知监听服务、排序、折叠、过滤、重要性。
- 不处理状态栏、锁屏、 Quick Settings、音量、Launcher。
- 不处理管理应用 UI。
- 不新增通知菜单功能、视觉或交互修改。
- 不支持 Android 14 或 HyperOS 2。
- 不合并 Hook，不做项目级架构重构。

## 缓存与状态约束

- 只允许缓存稳定反射元数据（Class / Field / Method / Constructor）和不可变常量。
- 禁止缓存 `Notification`、`StatusBarNotification`、`NotificationEntry`、`ExpandableNotificationRow`、`MiuiNotificationMenuRow`、`MenuItem`、`View`、`Drawable`、`Context`、`Handler`、`Binder` 或短生命周期 Android owner。
- 禁止无上限 Map / List / ThreadLocal / 对象池 / 全局状态。

## 验收标准

- 稳定反射元数据不在每次 `createMenuViews` 中重复解析。
- 不缓存 Notification、Row、MenuRow、View、Context 或菜单实例。
- 不新增线程、Handler、listener、observer、锁、依赖或无上限缓存。
- 菜单功能、项、顺序、文字、图标和点击行为保持。
- 原方法调用次数和返回行为保持。
- Hook call site 不增加，进程覆盖范围不扩大。
- 新增/扩展 JVM 测试覆盖指定场景。
- 更新 `a13_hook_cost_scan.py` 回归门禁并重新生成 `A13_HOOK_COST_MAP.*`。
- 全部 Python 测试、Android 编译测试、lint、R8 和正式 Release 签名通过。
- 工作区干净并推送。
- 不声明未经真机测量的性能收益。

## 本次修正内容

1. 修复 `STALE_ROW_BINDING_RISK`：将 `pkgName`、`appUid`、`user`、`miniWindowPkg`、`notifyIntent` 的动态读取从创建时移到点击时。
2. 修复 `CONTEXT_SEMANTICS_DRIFT`：所有分支统一使用安装阶段捕获的 `mContext`，不再混用 `view.context`。
3. 修复 `OPTIONAL_FIELD_NULL_SAFETY`：`mSbn` / `mParent` / `mMenuMargin` / `mMenuContainer` 等字段缺失或 null 时安全降级。
4. 修复 `RUNTIME_SUBTYPE_RESOLUTION`：当 `mSbn` / `mParent` 声明类型为基类、运行时为子类时，使用运行时 fallback 反射。
5. 新增 `rethrowFatal` / `callMethodCompat` / `resolveString` / `resolveInt` / `resolveUser` helper，统一错误处理和 nullable 处理。
6. 更新 `NotificationRowMenuHookTest` 与相关 fakes，覆盖 ROM clear/preserve 生命周期、创建时/点击时数据隔离、基类 fallback、主用户/多用户 force stop、浮窗启动、缺失字段降级。

## 工程验证

- `python tools/verify.py fast --tests NotificationRowMenuHookTest` 通过。
- `A13_HOOK_COST_MAP.*` 由 `a13_hook_cost_scan.py` 重新生成待 QA-1/P6 完成。
- P0 真实设备运行时基线仍保持 `RUNTIME_BASELINE_PENDING_DEVICE`。

## 最终状态约束

- 工程完成状态只能是 `ENGINEERING_COMPLETE_DEVICE_EVIDENCE_PENDING`。
- P0 继续留在 `RUNTIME_BASELINE_PENDING_DEVICE`。
- 不自动开始 P1B-4B。
