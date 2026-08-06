# A13 Notification Menu Creation Hot-Path Cost Audit

> Branch: `devin/a13-memory-performance-optimization`
> Scope: `SystemNotificationMoreHooks.NotificationRowMenuHook` -> `MiuiNotificationMenuRow#createMenuViews`

P0 真实设备基线为 `RUNTIME_BASELINE_PENDING_DEVICE`，任何性能收益数字均为静态推断，不声称具体 KB、MB、百分比、延迟或耗电变化。

## 1. 目标与范围

- **Hook 目标**：`com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow#createMenuViews(boolean, boolean)`。
- **所在进程**：`com.android.systemui`（`SYSTEM_UI`）。
- **模块入口**：`SystemUiInstaller.install` 在 `MainModule.mPrefs.getBoolean("system_notifrowmenu")` 为 `true` 时调用 `SystemNotificationMoreHooks.NotificationRowMenuHook(lpparam)`。
- **Hook 类型**：`METHOD_HOOK`，`after` 回调，原方法始终执行。
- **直接辅助逻辑**：`createMenuViews` `after` 回调内创建的 `View.OnClickListener`（应用信息、强制停止、浮窗打开）。

## 2. 安装路径（冷路径）

```text
MainModule.onPackageReady (com.android.systemui)
  └─ SystemUiInstaller.install()
       └─ if (mPrefs.getBoolean("system_notifrowmenu")) SystemNotificationMoreHooks.NotificationRowMenuHook(lpparam)
            ├─ 注册 6 个资源替换/新增（icon、title、dimen、drawable）
            ├─ XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow$MiuiNotificationMenuItem", lpparam.classLoader)
            └─ ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", lpparam.classLoader, "createMenuViews", boolean, boolean, afterHook)
```

## 3. 修改前调用链

### 3.1 createMenuViews after 回调

```text
MiuiNotificationMenuRow.createMenuViews(boolean, boolean)
  └─ after(param)
       1. XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return       // CALLBACK_TIME_REFLECTION
       2. XposedHelpers.getObjectField(param.thisObject, "mMenuItems") as? ArrayList<Any> ?: return // CALLBACK_TIME_REFLECTION
       3. val menuItem: Constructor<*> = MiuiNotificationMenuItem.constructors[0]                  // 反射访问 Constructor
       4. menuItem.newInstance(param.thisObject, mContext, appInfoDescId, null, appInfoIconResId)   // 创建 MenuItem 实例（+ Object[] varargs）
       5. menuItem.newInstance(param.thisObject, mContext, forceCloseDescId, null, forceCloseIconResId)
       6. menuItem.newInstance(param.thisObject, mContext, openInFwDescId, null, openInFwIconResId)
       7. if (any null) return
       8. XposedHelpers.getObjectField(param.thisObject, "mSbn")                                    // CALLBACK_TIME_REFLECTION
       9. XposedHelpers.getObjectField(param.thisObject, "mParent")                                 // CALLBACK_TIME_REFLECTION
       10. mMenuItems.add(infoBtn); mMenuItems.add(forceCloseBtn); mMenuItems.add(openFwBtn)
       11. XposedHelpers.setObjectField(param.thisObject, "mMenuItems", mMenuItems)                 // 冗余 setObjectField
       12. XposedHelpers.getObjectField(param.thisObject, "mMenuMargin") as? Int ?: 0              // CALLBACK_TIME_REFLECTION
       13. XposedHelpers.getObjectField(param.thisObject, "mMenuContainer") as? LinearLayout ?: return // CALLBACK_TIME_REFLECTION
       14. XposedHelpers.callMethod(infoBtn, "getMenuView") as? View ?: return                    // CALLBACK_TIME_REFLECTION
       15. XposedHelpers.callMethod(forceCloseBtn, "getMenuView") as? View ?: return
       16. XposedHelpers.callMethod(openFwBtn, "getMenuView") as? View ?: return
       17. 创建 View.OnClickListener lambda，捕获 notification / expandNotifyRow / mContext / lpparam
       18. mInfoBtn.setOnClickListener(itemClick); ...
       19. 创建 LinearLayout.LayoutParams(-2, -2) 并设置 margin
       20. mMenuContainer.addView(mInfoBtn, layoutParams); ...
       21. val menuWidth = TypedValue.applyDimension(COMPLEX_UNIT_DIP, 52f, mContext.resources.displayMetrics).toInt()
       22. val titleId = mContext.resources.getIdentifier("modal_menu_title", "id", lpparam.packageName)  // 字符串资源查找
       23. for (obj in mMenuItems) {                                                                  // Iterator 分配
              val menuView = XposedHelpers.callMethod(obj, "getMenuView") as? View ?: continue      // 每条 menu item 反射 getMenuView
              (menuView.findViewById<TextView>(titleId))?.maxWidth = menuWidth
           }
```

### 3.2 点击监听辅助逻辑

`View.OnClickListener` 被触发时执行：

- 应用信息 / 强制停止分支：
  - `XposedHelpers.callMethod(notification, "getPackageName")`
  - `XposedHelpers.callMethod(notification, "getAppUid")`
  - `XposedHelpers.callStaticMethod(UserHandle::class.java, "getUserId", uid)`
- 强制停止分支：
  - `mContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager`
  - `XposedHelpers.callMethod(am, "forceStopPackage" / "forceStopPackageAsUser", ...)`
  - `mContext.packageManager.getApplicationInfo(pkgName, 0)`
  - `mContext.packageManager.getApplicationLabel(...)`
  - `Toast.makeText(..., mContext.getString(R.string.forcestop_toast, appName), ...)`
- 浮窗打开分支：
  - `XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader)`            // CALLBACK_TIME_REFLECTION
  - `XposedHelpers.findClassIfExists("com.android.systemui.statusbar.notification.policy.AppMiniWindowManager", lpparam.classLoader)` // CALLBACK_TIME_REFLECTION
  - `XposedHelpers.findClass("com.android.systemui.statusbar.notification.modal.ModalController", lpparam.classLoader)` // CALLBACK_TIME_REFLECTION
  - `XposedHelpers.callStaticMethod(Dependency, "get", AppMiniWindowManagerClass)`
  - `XposedHelpers.callMethod(expandNotifyRow, "getMiniWindowTargetPkg")`
  - `XposedHelpers.callMethod(expandNotifyRow, "getPendingIntent") as? PendingIntent`
  - `XposedHelpers.callStaticMethod(Dependency, "get", ModalControllerClass)`
  - `XposedHelpers.callMethod(ModalController, "animExitModelCollapsePanels")`
  - `XposedHelpers.callMethod(AppMiniWindowManager, "launchMiniWindowActivity", miniWindowPkg, notifyIntent)`

## 4. 修改前成本清单

| 类别 | 数量（createMenuViews after） | 说明 |
|---|---|---|
| `CALLBACK_TIME_REFLECTION` | `getObjectField` x 6、`callMethod` x 3（new items `getMenuView`）+ `mMenuItems.size`（loop）+ `getIdentifier` x 1 | 稳定字段/方法反复查询 |
| `MainModule.mPrefs.get*` | 0 | 回调内无直接偏好读取，功能由 installer 门控 |
| 复合偏好解析 | 0 | 无 split / set / map 解析 |
| 临时 List/Set/Map | 0 | 复用 `mMenuItems`，未新建集合；Iterator 由 `for` 循环分配 |
| 临时数组 | 3+ | 每个 `Constructor.newInstance` 产生 varargs 数组；后续 `callMethod`/`callStaticMethod` 也产生 `Object[]` |
| 字符串拼接/格式化 | 1 | `mContext.resources.getIdentifier` 字符串查找；点击时 `Toast` 格式化字符串 |
| lambda / 匿名对象 | 1 | 每个 `createMenuViews` 创建一个 `View.OnClickListener` lambda |
| Drawable / Intent / Bundle | 1 `Intent` 每次点击 | `ACTION_CLOSE_SYSTEM_DIALOGS` 在 appInfo / forceClose 分支各一次 |
| View | 3 | 模块新增的 `MiuiNotificationMenuItem` 视图 |
| 系统服务 / Binder | `ActivityManager` / `PackageManager` 在 forceClose 分支 | 按点击按需查询 |
| 日志 | 3 | `infoBtn`/`forceCloseBtn`/`openFwBtn` 构造失败时 `XposedHelpers.log(t)`；`UserHandle.getUserId` 失败时记录 |
| catch 后重复重试 | 0 | 失败即返回 null，不重复反射 |

## 5. 功能门控

- **preference_key**：`system_notifrowmenu`
- **default_value**：`false`（未显式设置时按 `PrefMap` 默认）
- **runtime_enable_semantics**：`SystemUiInstaller.install` 中读取一次；功能启用时安装 Hook，需重启 SystemUI 生效。
- **runtime_disable_semantics**：禁用时不安装 Hook；已安装则保持到下次 SystemUI 重启。
- **requires_systemui_restart**：是
- **affects_menu_structure**：是（新增三个菜单项）
- **affects_menu_click_behavior**：是
- **can_skip_when_disabled**： installer 层已跳过；回调内无需再次判断。

## 6. 已实施优化

1. 在 `NotificationRowMenuHook` 安装阶段解析 `MiuiNotificationMenuRow` 的 `mContext` / `mMenuItems` / `mSbn` / `mParent` / `mMenuMargin` / `mMenuContainer` 为 `Field`。
2. 缓存 `MiuiNotificationMenuItem` 构造器、`getMenuView` 方法、`modal_menu_title` 资源 id。
3. 缓存 `StatusBarNotification#getPackageName`、`#getAppUid`，`UserHandle#getUserId`（静态），`ExpandableNotificationRow#getMiniWindowTargetPkg`、`#getPendingIntent`。
4. 将 `Dependency`、`AppMiniWindowManager`、`ModalController` 类及方法解析移到安装阶段；点击时只使用 `Method.invoke`。
5. 移除 `XposedHelpers.setObjectField(thisObject, "mMenuItems", mMenuItems)` 冗余调用。
6. 将 `for (obj in mMenuItems)` 改为索引循环，减少 `Iterator`。
7. 保留 `Constructor.newInstance` 创建三个菜单项；保留 `View.OnClickListener` 创建；保留 `Intent` 创建（必要点击行为）；保留 `Toast` 格式化（点击分支）。

## 7. 修改后成本清单

| 类别 | 数量（createMenuViews after） | 说明 |
|---|---|---|
| `CALLBACK_TIME_REFLECTION` | 0 | 字段/方法查找全部移至安装阶段；回调使用 `Field.get` / `Method.invoke` / `Constructor.newInstance` |
| `MainModule.mPrefs.get*` | 0 | 回调内无偏好读取，功能由 installer 门控 |
| 复合偏好解析 | 0 | 无 split / set / map 解析 |
| 临时 List/Set/Map | 0 | 复用 `mMenuItems`，未新建集合；`for` 改为索引循环 |
| 临时数组 | 3+ | 每个 `Constructor.newInstance` 产生 varargs 数组；必要的 `Method.invoke` 静态/实例调用仍存在 |
| 字符串拼接/格式化 | 1 | `getIdentifier("modal_menu_title")` 退居 fallback（`com.android.systemui.R$id` 存在时直接取静态 int）；点击时 Toast 格式化保留 |
| lambda / 匿名对象 | 1 | 每个 `createMenuViews` 创建一个 `View.OnClickListener` lambda；捕获 `pkgName` / `user` / `miniWindowPkg` / `notifyIntent` 等稳定数据 |
| Drawable / Intent / Bundle | 1 `Intent` 每次点击 | `ACTION_CLOSE_SYSTEM_DIALOGS` 在 appInfo / forceClose 分支各一次 |
| View | 3 | 模块新增的 `MiuiNotificationMenuItem` 视图 |
| 系统服务 / Binder | `ActivityManager` / `PackageManager` 在 forceClose 分支 | 按点击按需查询 |
| 日志 | 0（正常路径） | 安装阶段失败或构造异常时仍记录；热路径正常无日志 |
| catch 后重复重试 | 0 | 失败即返回，不重复反射 |

## 8. 保留动态反射原因

- `MiuiNotificationMenuItem` 构造器和 `getMenuView` 方法在目标 ROM 上必须存在，属于稳定元数据，全部提前缓存。
- 点击分支中 `notification.getPackageName()`、`getAppUid()` 等调用依赖具体 `StatusBarNotification` 实例，但这些是 Method.invoke 调用，不是反射查找。
- 回调内唯一需要在运行时使用反射的原因是目标类/方法无法在安装阶段以跨 ROM 确定的形式获取；本路径中稳定的类/方法均可在安装阶段解析。
