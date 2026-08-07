# A13 Notification Menu Creation Hot-Path Cost Audit

> Branch: `devin/a13-memory-performance-optimization`<br>
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

## 3. 修改后调用链

### 3.1 createMenuViews after 回调

```text
MiuiNotificationMenuRow.createMenuViews(boolean, boolean)
  └─ after(param)
       1. mContextField.get(param.thisObject) as? Context ?: return          // 安装阶段缓存的 Field 读取
       2. mMenuItemsField.get(param.thisObject) as? ArrayList<Any> ?: return // 安装阶段缓存的 Field 读取
       3. mMenuContainerField.get(param.thisObject) as? LinearLayout ?: return // 安装阶段缓存的 Field 读取
       4. 使用安装阶段缓存的 menuItemConstructor 创建三个 MiuiNotificationMenuItem
       5. getMenuViewSafe(infoBtn, getMenuViewMethod) ?: return              // 安全包装：普通异常记录并返回 null，fatal 重抛
       6. getMenuViewSafe(forceCloseBtn, getMenuViewMethod) ?: return
       7. getMenuViewSafe(openFwBtn, getMenuViewMethod) ?: return
       8. 创建 View.OnClickListener lambda，捕获 mContext / mMenuRow / 缓存的 Field 与 Method
       9. mInfoBtn.setOnClickListener(itemClick); ...
       10. mMenuMarginField?.get(param.thisObject) as? Int ?: 0              // 安装阶段缓存的可选 Field 读取
       11. 创建 LinearLayout.LayoutParams(-2, -2) 并设置 margin
       12. mMenuItems.add(infoBtn); mMenuItems.add(forceCloseBtn); mMenuItems.add(openFwBtn)   // 全部前置成功后首次 list 修改
       13. mMenuContainer.addView(mInfoBtn, layoutParams); ...              // 与 list 修改原子提交
       14. val menuWidth = TypedValue.applyDimension(COMPLEX_UNIT_DIP, 52f, mContext.resources.displayMetrics).toInt()
       15. val realTitleId = if (titleId != 0) titleId else mContext.resources.getIdentifier("modal_menu_title", "id", lpparam.packageName)
       16. for (i in 0 until mMenuItems.size) {                                    // 索引循环，无 Iterator
              val menuView = getMenuViewSafe(mMenuItems[i], getMenuViewMethod) ?: continue
              (menuView.findViewById<TextView>(realTitleId))?.maxWidth = menuWidth
           }
```

### 3.2 点击监听辅助逻辑

`View.OnClickListener` 被触发时执行（**所有实例数据均在点击时通过缓存的 Field 重新读取，不使用创建时的快照**）：

- 应用信息 / 强制停止分支：
  - `mSbnField?.get(menuRow)` 获取当前 notification 实例
  - `getPackageNameMethod.invoke(notification)` 或 `XposedHelpers.callMethod(notification, "getPackageName")` fallback（仅在 cachedMethod 不存在时）
  - `getAppUidMethod.invoke(notification)` 或 `XposedHelpers.callMethod(notification, "getAppUid")` fallback
  - `UserHandle#getUserId` 或 `appUid / ANDROID_PER_USER_RANGE` fallback
- 强制停止分支：
  - `mContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager`
  - `forceStopPackageMethod.invoke(am, pkgName)` 或 `XposedHelpers.callMethod(am, "forceStopPackage", pkgName)` fallback
  - `forceStopPackageAsUserMethod.invoke(am, pkgName, user)` 或 `XposedHelpers.callMethod(am, "forceStopPackageAsUser", pkgName, user)` fallback
  - `mContext.packageManager.getApplicationInfo` / `getApplicationLabel`（失败降级，不阻断 force stop）
  - `Toast.makeText(..., ModuleHelper.getModuleRes(mContext).getString(R.string.force_closed, appName), ...)`（失败降级）
- 浮窗打开分支：
  - `mParentField?.get(menuRow)` 获取当前 expandNotifyRow 实例
  - `getMiniWindowTargetPkgMethod.invoke(currentParent)` 或 `XposedHelpers.callMethod(currentParent, "getMiniWindowTargetPkg")` fallback
  - `getPendingIntentMethod.invoke(currentParent)` 或 `XposedHelpers.callMethod(currentParent, "getPendingIntent")` fallback
  - `dependencyGetMethod.invoke(null, appMiniWindowManagerClass)` 获取 `AppMiniWindowManager`
  - `dependencyGetMethod.invoke(null, modalControllerClass)` 获取 `ModalController`
  - `animExitModelCollapsePanelsMethod.invoke(modalController)` 或 `XposedHelpers.callMethod(modalController, "animExitModelCollapsePanels")` fallback
  - `launchMiniWindowActivityMethod.invoke(appMiniWindowManager, miniWindowPkg, notifyIntent)` 或 `XposedHelpers.callMethod(appMiniWindowManager, "launchMiniWindowActivity", miniWindowPkg, notifyIntent)` fallback

## 4. 修改后成本清单

| 类别 | 数量（createMenuViews after） | 说明 |
|---|---|---|
| `CALLBACK_TIME_REFLECTION_LOOKUP` | 0 | 所有 `findClass` / `findMethodBestMatch` / `findConstructorBestMatch` / `findFieldIfExists` 均在安装阶段完成 |
| `CALLBACK_TIME_REFLECTION_INVOKE` | 少量 | 点击时通过缓存的 `Field.get` 重新读取 `mSbn` / `mParent`；通过缓存的 `Method.invoke` 调用 `getPackageName` / `getAppUid` / `getMiniWindowTargetPkg` / `getPendingIntent` / force stop 等；稳定元数据已缓存 |
| `MainModule.mPrefs.get*` | 0 | 回调内无直接偏好读取，功能由 installer 门控 |
| 复合偏好解析 | 0 | 无 split / set / map 解析 |
| 临时 List/Set/Map | 0 | 复用 `mMenuItems`，未新建集合；`for` 改为索引循环 |
| 临时数组 | 3+ | 每个 `Constructor.newInstance` 产生 varargs 数组；必要的 `Method.invoke` 调用仍存在 |
| 字符串拼接/格式化 | 1 | `getIdentifier("modal_menu_title")` 退居 fallback（`com.android.systemui.R$id` 存在时直接取静态 int）；点击时 Toast 格式化保留 |
| lambda / 匿名对象 | 1 | 每个 `createMenuViews` 创建一个 `View.OnClickListener` lambda；捕获 `mContext`、`mMenuRow` 与缓存的反射元数据 |
| Drawable / Intent / Bundle | 1 `Intent` 每次点击 | `ACTION_CLOSE_SYSTEM_DIALOGS` 在 appInfo / forceClose 分支各一次 |
| View | 3 | 模块新增的 `MiuiNotificationMenuItem` 视图 |
| 系统服务 / Binder | `ActivityManager` / `PackageManager` 在 forceClose 分支 | 按点击按需查询 |
| 日志 | 0（正常路径） | 安装阶段失败或构造异常时仍记录；热路径正常无日志；`getMenuViewSafe` 普通异常记录并返回 |
| catch 后重复重试 | 0 | 失败即返回，不重复反射；`getMenuView` 失败不触发 fallback 到其他方法 |

## 5. 功能门控

- **preference_key**：`system_notifrowmenu`
- **default_value**：`false`（未显式设置时按 `PrefMap` 默认）
- **runtime_enable_semantics**：`SystemUiInstaller.install` 中读取一次；功能启用时安装 Hook，需重启 SystemUI 生效。
- **runtime_disable_semantics**：禁用时不安装 Hook；已安装则保持到下次 SystemUI 重启。
- **requires_systemui_restart**：是
- **affects_menu_structure**：是（新增三个菜单项）
- **affects_menu_click_behavior**：是
- **can_skip_when_disabled**： installer 层已跳过；回调内无需再次判断。

## 6. 已实施关键修复

1. 在 `NotificationRowMenuHook` 安装阶段解析 `MiuiNotificationMenuRow` 的 `mContext` / `mMenuItems` / `mSbn` / `mParent` / `mMenuMargin` / `mMenuContainer` 为 `Field`。
2. 缓存 `MiuiNotificationMenuItem` 构造器、`getMenuView` 方法、`modal_menu_title` 资源 id。
3. 缓存 `StatusBarNotification#getPackageName`、`#getAppUid`，`UserHandle#getUserId`（静态），`ExpandableNotificationRow#getMiniWindowTargetPkg`、`#getPendingIntent`；缺失时使用运行时 fallback 反射。
4. 将 `Dependency`、`AppMiniWindowManager`、`ModalController` 类及方法解析移到安装阶段；点击时只使用 `Method.invoke` 或 fallback。
5. 移除 `XposedHelpers.setObjectField(thisObject, "mMenuItems", mMenuItems)` 冗余调用。
6. 将 `for (obj in mMenuItems)` 改为索引循环，减少 `Iterator`。
7. 修复 `STALE_ROW_BINDING_RISK`：点击时重新读取 `mSbn` / `mParent`，确保使用用户点击时的最新 notification / parent binding。
8. 修复 `CONTEXT_SEMANTICS_DRIFT`：所有分支统一使用安装阶段捕获的 `mContext`，不再混用 `view.context`。
9. 修复可选字段 null-safety：`mSbn` / `mParent` / `mMenuMargin` / `mMenuContainer` 缺失时安全降级。
10. 修复 `PARTIAL_MENU_MUTATION`：`mMenuItems` 和 `mMenuContainer` 修改推迟到容器、三个 menu item、三个 menu view、listener 均准备完成后；`mMenuContainer == null`、`getMenuView == null` 或 `getMenuView` 普通异常时 zero mutation。
11. 增加 `rethrowFatal` helper：`VirtualMachineError` 与 `ThreadDeath` 继续抛出（含 wrapped cause chain），避免误吞致命异常；`NoSuchMethodError` / `NoSuchFieldError` / `VerifyError` / `IncompatibleClassChangeError` 不自动 fatal。
12. `callMethodCompat` 合同明确：cached 存在时调用一次；cached 不存在时运行时 fallback；cached 调用失败后不回退，避免副作用重复。
13. `UserHandle.getUserId` 不可用时，使用 `appUid / ANDROID_PER_USER_RANGE` 作为安全 fallback。

## 7. 保留动态反射原因

- `MiuiNotificationMenuItem` 构造器和 `getMenuView` 方法在目标 ROM 上必须存在，属于稳定元数据，全部提前缓存。
- 点击分支中 `notification.getPackageName()`、`getAppUid()` 等调用依赖具体 `StatusBarNotification` 实例；方法已缓存，调用为 `Method.invoke`。
- 回调内唯一需要在运行时使用反射 lookup 的原因是目标类/方法无法在安装阶段以跨 ROM 确定的形式获取；本路径中稳定的类/方法均可在安装阶段解析。
- 为兼容 ROM 中将 `mSbn` / `mParent` 声明为基类、运行时为子类的情形，保留 `XposedHelpers.callMethod` 运行时 fallback（在 cached Method 缺失时）。
- `getMenuView` 调用失败后不尝试运行时 fallback，避免 target 可能已执行的副作用被重复。
