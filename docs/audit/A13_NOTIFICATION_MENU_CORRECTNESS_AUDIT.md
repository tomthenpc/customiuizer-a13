# A13 Notification Menu Hook Correctness Audit

> Task: `A13-PERF-P1B-4A-NOTIFICATION-MENU-HOT-PATH`  
> Branch: `devin/a13-memory-performance-optimization`  
> Scope: `SystemNotificationMoreHooks.NotificationRowMenuHook`

## 1. 审计目标

在 P1B-4A 性能优化过程中，`NotificationRowMenuHook` 出现实现漂移，导致三个正确性风险。本审计记录风险根因、修复方法和验证证据。

## 2. 发现的风险

| 风险 ID | 名称 | 根因 | 影响 |
|---|---|---|---|
| `STALE_ROW_BINDING_RISK` | 行绑定快照过期 | `after` 回调在菜单创建时读取 `pkgName`、`appUid`、`user`、`miniWindowPkg`、`notifyIntent` 并捕获到 `OnClickListener` lambda；ROM 的 `createMenuViews` 可能在行数据变更（重新绑定、滑动、展开/折叠）时重建 menu，导致点击时使用的是过期数据 | 点击应用信息/强制停止/浮窗打开时可能操作错误应用、错误用户或错误 Intent |
| `CONTEXT_SEMANTICS_DRIFT` | `mContext` 与 `view.context` 混用 | 点击分支改用 `view.context` 而非最初捕获的 `mContext` | `view.context` 可能为 `ThemedContext`、`DecorContext` 或 null，导致 `PackageManager`、`ActivityManager`、Toast、资源解析行为不一致 |
| `OPTIONAL_FIELD_NULL_SAFETY` | 可选字段缺失崩溃 | `mSbn`/`mParent` 在某些 ROM 或 transient 行上可能为 null；原实现未处理 | 抛出 `NullPointerException` 并可能拖垮 SystemUI 热路径 |
| `RUNTIME_SUBTYPE_RESOLUTION` | 基类声明缺失方法 | 部分 ROM 将 `mSbn`/`mParent` 声明为基类类型（如 `BaseNotification` / `BaseExpandableNotificationRow`），运行时才为具体子类；缓存的 `Method` 在基类上找不到 | `getPackageName` / `getAppUid` / `getMiniWindowTargetPkg` / `getPendingIntent` 调用失败，菜单项行为异常 |

## 3. 修复方案

### 3.1 点击时再读取动态数据

- `mSbnField?.get(menuRow)` 在每次点击时重新获取当前 `StatusBarNotification` 实例。
- `mParentField?.get(menuRow)` 在每次点击时重新获取当前 `ExpandableNotificationRow` 实例。
- 通过缓存的 `Method`（`getPackageNameMethod`、`getAppUidMethod`、`getMiniWindowTargetPkgMethod`、`getPendingIntentMethod`）调用动态数据。
- 当缓存的 `Method` 因为声明类型为基类而无法命中时，回退到 `XposedHelpers.callMethod` 运行时反射。

### 3.2 统一使用 `mContext`

- 所有点击分支统一使用安装阶段捕获的 `mContext`（`mContextField.get(param.thisObject)`）。
- 不再使用 `view.context`。
- `mContext` 为空时直接 return，避免 `ThemedContext` 或 null 导致的资源/服务不一致。

### 3.3 可选字段 null-safety

- `mSbn` 为 null 时：`mInfoBtn` / `mForceCloseBtn` 点击直接 return。
- `mParent` 为 null 时：`mOpenFwBtn` 点击直接 return。
- `mMenuMargin` / `mMenuContainer` 等可选字段在 `?:` 后提供安全默认值或 return。

### 3.4 运行时 subtype fallback

- 新增 `callMethodCompat(instance, cachedMethod, methodName, vararg args)` helper：
  - 优先使用安装阶段缓存的 `Method.invoke`。
  - 若 `cachedMethod` 不存在或 `invoke` 抛出 `NoSuchMethodError` / `IllegalArgumentException`，回退到 `XposedHelpers.callMethod(instance, methodName, *args)`。
- 新增 `resolveString`、`resolveInt`、`resolveUser` 包装器，统一处理 nullable 结果和 fatal 异常重抛。

### 3.5 致命异常处理

- 新增 `rethrowFatal(t: Throwable)`：
  - `OutOfMemoryError`、`StackOverflowError`、`IncompatibleClassChangeError`、`NoSuchMethodError`、`NoSuchFieldError`、`VerifyError`、`InternalError`、`UnknownError`、`ThreadDeath` 直接抛出。
  - 其他非致命异常仅记录或返回，避免 `catch (Throwable)` 吞掉系统级错误。

## 4. 验证

### 4.1 新增/更新测试

- `NotificationRowMenuHookTest`（`app/src/test/java/tv/withaibuild/customiuizer/mods/NotificationRowMenuHookTest.kt`）：
  - 验证 `createMenuViews` 后新增 3 个模块菜单项，且 2 个系统菜单项不被覆盖。
  - 验证原方法先 clear 时不累积，原方法 preserve 时累积。
  - 验证 `mInfoBtn` 点击启动 activity 并发送 `ACTION_CLOSE_SYSTEM_DIALOGS` broadcast。
  - 验证 `mForceCloseBtn` 点击调用 `RecordingActivityManager#forceStopPackage`（主用户）或 `forceStopPackageAsUser`（多用户）。
  - 验证 `mOpenFwBtn` 点击调用 `ModalController#animExitModelCollapsePanels` 和 `AppMiniWindowManager#launchMiniWindowActivity`。
  - 验证重新绑定 `mSbn` / `mParent` 后点击使用的是当前数据，而非创建时的快照。
  - 验证 `mSbn` / `mParent` 声明为基类、运行时为子类时的 fallback。
  - 验证 `mSbn` / `mParent` 为 null 时点击安全降级。

### 4.2 新增/更新 Fakes

- `FakeContext`：支持 `ACTIVITY_SERVICE` 注入 `RecordingActivityManager`；`startActivity` / `sendBroadcast` 记录；`getResources` 返回非 null `Resources`。
- `RecordingActivityManager`（`android.app` 包）：记录 `forceStopPackage` / `forceStopPackageAsUser`。
- `RecordingMenuContainer` / `RecordingMenuItemView`：记录 `addView`、`removeAllViews`、`setOnClickListener`。
- `ModalController`（`com.android.systemui.statusbar.notification.modal`）：记录 `animExitModelCollapsePanels`。
- `AppMiniWindowManager`：记录 `launchMiniWindowActivity` 调用。
- `FakeStatusBarNotification` / `FakeExpandableNotificationRow`：增加 `BaseNotification` / `BaseExpandableNotificationRow` 基类与运行时子类 `RuntimeStatusBarNotification` / `RuntimeExpandableNotificationRow`，用于验证声明基类 fallback。
- `MiuiNotificationMenuRow`：模拟 ROM `createMenuViews` 先 clear 并添加 2 个系统 menu item，再执行 after 回调。

### 4.3 门禁状态

- `python tools/verify.py fast --tests NotificationRowMenuHookTest` 通过。
- `python tools/verify.py full` 待运行。
- `A13_HOOK_COST_MAP.*` 待重新生成（由 `a13_hook_cost_scan.py`）。
- 正式签名 Release 待 QA-1/P6 阶段处理。

## 5. 结论

- `STALE_ROW_BINDING_RISK`、`CONTEXT_SEMANTICS_DRIFT`、`OPTIONAL_FIELD_NULL_SAFETY`、`RUNTIME_SUBTYPE_RESOLUTION` 已修复。
- 稳定反射元数据仍保持在安装阶段缓存；点击时仅做 `Field.get` / `Method.invoke`，并保留运行时 fallback。
- 新增测试覆盖创建时/点击时数据隔离、ROM clear/preserve 生命周期、基类声明/运行时子类、主用户/多用户 force stop、浮窗启动、缺失字段降级。
