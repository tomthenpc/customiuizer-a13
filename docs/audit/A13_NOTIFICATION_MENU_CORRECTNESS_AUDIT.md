# A13 Notification Menu Hook Correctness Audit

> Task: `A13-PERF-P1B-4A-NOTIFICATION-MENU-HOT-PATH`<br>
> Branch: `devin/a13-memory-performance-optimization`<br>
> Scope: `SystemNotificationMoreHooks.NotificationRowMenuHook`

## 1. 审计目标

在 P1B-4A 性能优化过程中，`NotificationRowMenuHook` 出现实现漂移，导致多个正确性风险。本审计记录风险根因、修复方法和验证证据。

## 2. 发现的风险

| 风险 ID | 名称 | 根因 | 影响 |
|---|---|---|---|
| `STALE_ROW_BINDING_RISK` | 行绑定快照过期 | `after` 回调在菜单创建时读取 `pkgName`、`appUid`、`user`、`miniWindowPkg`、`notifyIntent` 并捕获到 `OnClickListener` lambda；ROM 的 `createMenuViews` 可能在行数据变更（重新绑定、滑动、展开/折叠）时重建 menu，导致点击时使用的是过期数据 | 点击应用信息/强制停止/浮窗打开时可能操作错误应用、错误用户或错误 Intent |
| `CONTEXT_SEMANTICS_DRIFT` | `mContext` 与 `view.context` 混用 | 点击分支改用 `view.context` 而非最初捕获的 `mContext` | `view.context` 可能为 `ThemedContext`、`DecorContext` 或 null，导致 `PackageManager`、`ActivityManager`、Toast、资源解析行为不一致 |
| `OPTIONAL_FIELD_NULL_SAFETY` | 可选字段缺失崩溃 | `mSbn`/`mParent` 在某些 ROM 或 transient 行上可能为 null；原实现未处理 | 抛出 `NullPointerException` 并可能拖垮 SystemUI 热路径 |
| `RUNTIME_SUBTYPE_RESOLUTION` | 基类声明缺失方法 | 部分 ROM 将 `mSbn`/`mParent` 声明为基类类型（如 `BaseNotification` / `BaseExpandableNotificationRow`），运行时才为具体子类；缓存的 `Method` 在基类上找不到 | `getPackageName` / `getAppUid` / `getMiniWindowTargetPkg` / `getPendingIntent` 调用失败，菜单项行为异常 |
| `PARTIAL_MENU_MUTATION` | 菜单事务不完整 | `after` 回调先 `mMenuItems.add` 三项，再 `getMenuView` / `mMenuContainer.addView`；`getMenuView` 失败或 `mMenuContainer` 缺失时列表已脏，且 `getMenuView` 普通异常直接抛出 | 部分菜单项残留或异常逃逸，破坏 ROM 菜单状态 |

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
- `mMenuMargin` 在缺失时使用 0；`mMenuContainer` 在缺失时直接 return，不修改 `mMenuItems`。

### 3.4 运行时 subtype fallback

- 新增 `callMethodCompat(instance, cachedMethod, methodName, vararg args)` helper：
  - `cachedMethod` 存在时调用一次 `Method.invoke`。
  - `cachedMethod` 不存在时回退到 `XposedHelpers.callMethod(instance, methodName, *args)`。
  - `cachedMethod.invoke` 失败后仅记录并返回 `null`，不再二次调用，避免 target 已执行副作用。
- 新增 `resolveString`、`resolveInt`、`resolveUser` 包装器，统一处理 nullable 结果和 fatal 异常重抛。

### 3.5 致命异常处理

- 新增 `rethrowFatal(t: Throwable)`：
  - `VirtualMachineError`（包含 `OutOfMemoryError`、`StackOverflowError`、`InternalError` 等）直接抛出。
  - `ThreadDeath` 直接抛出。
  - 对 cause chain 深度 8 层内的 `VirtualMachineError` / `ThreadDeath` 同样抛出。
  - 其他非致命异常（包括 `NoSuchMethodError`、`NoSuchFieldError`、`VerifyError`、`IncompatibleClassChangeError`）仅记录或返回，避免 `catch (Throwable)` 吞掉系统级错误。

### 3.6 菜单构建事务化

- `after` 回调在首次修改 `mMenuItems` / `mMenuContainer` 前完成以下步骤：
  1. 读取 `mContext`、`mMenuItems`、`mMenuContainer`。
  2. 构造三个 `MiuiNotificationMenuItem`。
  3. 通过 `getMenuViewSafe` 安全获取三个 `View`（普通异常不逃逸，fatal 重抛）。
  4. 准备 `View.OnClickListener` 和 `LinearLayout.LayoutParams`。
- 任何前置步骤失败时直接 `return`，不修改 `mMenuItems` 和 `mMenuContainer`。
- `mMenuContainer == null`、`getMenuView == null` 或 `getMenuView` 抛非 fatal 异常时保持 zero mutation。

## 4. ROM 生命周期证据

- `ROM_LIFECYCLE_EVIDENCE = MISSING`
- 本地仓库未找到目标 MIUI14 / HyperOS1 Android 13 的 `MiuiNotificationMenuRow#createMenuViews(boolean, boolean)` 反编译产物、JADX 输出或 SystemUI.apk 提取。
- 现有测试使用 JVM fixture 模拟两种生命周期行为：
  - `CLEAR_REBUILD`：每一轮 `createMenuViews` 都会 `mMenuItems.clear()` 并重新添加系统 item，模块 item 不会累积。
  - `PRESERVE`：原方法不清除列表，模块 item 会在多轮 `after` 中累积。
- 该 fixture 不是 ROM 生命周期证据，仅用于证明当前模块在两种模型下的行为差异。

## 5. 验证

### 5.1 新增/更新测试

- `NotificationRowMenuHookTest`（`app/src/test/java/tv/withaibuild/customiuizer/mods/NotificationRowMenuHookTest.kt`）：
  - 验证 `createMenuViews` 后新增 3 个模块菜单项，且 2 个系统菜单项不被覆盖。
  - 验证 CLEAR_REBUILD fixture 每轮固定为 2 个系统 + 3 个模块 item。
  - 验证 PRESERVE fixture 第二轮会累积到 7 个 item（1 个 preserve + 6 个模块）。
  - 验证 `mMenuContainer == null` 时 `mMenuItems` 和 container 不被污染。
  - 验证 `getMenuView` 失败时 `mMenuItems` 和 container 不被污染。
  - 验证 `mInfoBtn` 点击启动 activity 并发送 `ACTION_CLOSE_SYSTEM_DIALOGS` broadcast。
  - 验证 `mForceCloseBtn` 点击调用 `RecordingActivityManager#forceStopPackage`（主用户）或 `forceStopPackageAsUser`（多用户）。
  - 验证 `mOpenFwBtn` 点击调用 `ModalController#animExitModelCollapsePanels` 和 `AppMiniWindowManager#launchMiniWindowActivity`。
  - 验证重新绑定 `mSbn` / `mParent` 后点击使用的是当前数据，而非创建时的快照。
  - 验证 `mSbn` / `mParent` 声明为基类、运行时为子类时的 fallback。
  - 验证 `mSbn` / `mParent` 为 null 时点击安全降级。
  - 验证 `rethrowFatal` 对 `VirtualMachineError`、`ThreadDeath`、wrapped fatal 重抛，对普通 `RuntimeException`、`NoSuchMethodError` 等不抛出。
  - 验证 `callMethodCompat` 在 cached `Method.invoke` 失败后不回退到运行时反射。

### 5.2 新增/更新 Fakes

- `FakeContext`：支持 `ACTIVITY_SERVICE` 注入 `RecordingActivityManager`；`startActivity` / `sendBroadcast` 记录；`getResources` 返回非 null `Resources`。
- `RecordingActivityManager`（`android.app` 包）：记录 `forceStopPackage` / `forceStopPackageAsUser`。
- `RecordingMenuContainer` / `RecordingMenuItemView`：记录 `addView`、`removeAllViews`、`setOnClickListener`。
- `ModalController`（`com.android.systemui.statusbar.notification.modal`）：记录 `animExitModelCollapsePanels`。
- `AppMiniWindowManager`：记录 `launchMiniWindowActivity` 调用。
- `FakeStatusBarNotification` / `FakeExpandableNotificationRow`：增加 `BaseNotification` / `BaseExpandableNotificationRow` 基类与运行时子类 `RuntimeStatusBarNotification` / `RuntimeExpandableNotificationRow`，用于验证声明基类 fallback。
- `MiuiNotificationMenuRow`：模拟 ROM `createMenuViews` 两种 fixture，支持可控 `getMenuView` 失败。

### 5.3 门禁状态

- `python tools/verify.py full` 待运行。
- `A13_HOOK_COST_MAP.*` 由 `a13_hook_cost_scan.py` 生成或验证。
- 正式签名 Release 待 QA-1/P6 阶段处理。

## 6. 结论

- `STALE_ROW_BINDING_RISK`、`CONTEXT_SEMANTICS_DRIFT`、`OPTIONAL_FIELD_NULL_SAFETY`、`RUNTIME_SUBTYPE_RESOLUTION`、`PARTIAL_MENU_MUTATION` 已修复。
- `callMethodCompat` 合同为：cached 存在时调用一次；cached 不存在时回退到运行时反射；cached 调用失败后不回退，避免副作用重复。
- `rethrowFatal` 合同为：`VirtualMachineError` / `ThreadDeath` 直接重抛，包括 wrapped cause chain；`NoSuchMethodError` / `NoSuchFieldError` / `VerifyError` / `IncompatibleClassChangeError` 不自动 fatal。
- 菜单构建已事务化：任何前置失败均 zero mutation。
- `ROM_LIFECYCLE_EVIDENCE` 缺失，状态保持 `QA_CONDITIONAL_ROM_EVIDENCE_REQUIRED`。
