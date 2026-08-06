# A13 Notification Intent Launch Hot-Path Cost Audit

> Branch: `devin/a13-memory-performance-optimization`
> Scope: `SystemUINotificationHooks.OpenNotifyInFloatingWindowHook` -> `MiuiStatusBarNotificationActivityStarter#startNotificationIntent`

P0 真实设备基线为 `RUNTIME_BASELINE_PENDING_DEVICE`，任何性能收益数字均为静态推断，不声称具体 KB、MB、百分比、延迟或耗电变化。

## 1. 目标与范围

- **Hook 目标**：`com.android.systemui.statusbar.phone.MiuiStatusBarNotificationActivityStarter#startNotificationIntent(...)`。
- **所在进程**：`com.android.systemui`（`SYSTEM_UI`）。
- **模块入口**：`SystemUiInstaller.install` 在 `MainModule.mPrefs.getBoolean("system_notify_openinfw")` 为 `true` 时调用 `SystemUINotificationHooks.OpenNotifyInFloatingWindowHook(lpparam)`。
- **Hook 类型**：`ALL_METHODS`，`before` 回调；满足条件时 `returnAndSkip(null)`，原方法不执行；否则原方法正常执行。
- **直接辅助逻辑**：点击后通过 `Dependency` 获取 `AppMiniWindowManager` 并调用 `launchMiniWindowActivity(pkgName, pendingIntent)`。

## 2. 安装路径（冷路径）

```text
MainModule.onPackageReady (com.android.systemui)
  └─ SystemUiInstaller.install()
       └─ if (mPrefs.getBoolean("system_notify_openinfw")) SystemUINotificationHooks.OpenNotifyInFloatingWindowHook(lpparam)
            └─ ModuleHelper.hookAllMethods("com.android.systemui.statusbar.phone.MiuiStatusBarNotificationActivityStarter", ..., "startNotificationIntent", beforeHook)
```

`system_notify_openinfw` 为 `false` 时不安装 Hook，不产生回调开销。

## 3. 修改前调用链

### 3.1 startNotificationIntent before 回调

```text
MiuiStatusBarNotificationActivityStarter.startNotificationIntent(PendingIntent, ...)
  └─ before(param)
       1. param.getArg(0) as? PendingIntent ?: return
       2. XposedHelpers.getObjectField(param.getArg(2), "mSbn") ?: return       // CALLBACK_TIME_REFLECTION
       3. XposedHelpers.callMethod(mSbn, "isSubstituteNotification") as? Boolean // CALLBACK_TIME_REFLECTION
       4. if (true)  XposedHelpers.getObjectField(mSbn, "mPkgName") as? String   // CALLBACK_TIME_REFLECTION
          else       pendingIntent.creatorPackage ?: ""
       5. ProcessManager.getForegroundInfo()
       6. foregroundInfo.mForegroundPackageName
       7. if (pkgName == topPackage || "com.miui.home" == topPackage) return
       8. MainModule.mPrefs.getBoolean("system_notify_openinfw_in_whitelist")
       9. MainModule.mPrefs.getStringSet("system_notify_openinfw_apps").contains(pkgName)
      10. if (whitelist xor appInList) return
      11. XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader)            // CALLBACK_TIME_REFLECTION
      12. XposedHelpers.findClassIfExists("com.android.systemui.statusbar.notification.policy.AppMiniWindowManager", lpparam.classLoader) // CALLBACK_TIME_REFLECTION
      13. XposedHelpers.callStaticMethod(Dependency, "get", AppMiniWindowManagerClass)             // 使用反射
      14. XposedHelpers.callMethod(AppMiniWindowManager, "launchMiniWindowActivity", pkgName, pendingIntent) // 使用反射
      15. param.returnAndSkip(null)
```

### 3.2 涉及系统服务/Binder

- `ProcessManager.getForegroundInfo()`：查询当前前台应用包名。
- `PendingIntent.creatorPackage`：读取 PendingIntent 的创建者包名。

## 4. 修改前成本清单

| 类别 | 数量（startNotificationIntent before） | 说明 |
|---|---|---|
| `CALLBACK_TIME_REFLECTION` | 4+ | `getObjectField` x 2、`callMethod` x 1、`findClass` x 1、`findClassIfExists` x 1 |
| `MainModule.mPrefs.get*` | 3 | `system_notify_openinfw_in_whitelist`、`system_notify_openinfw_apps`、`getBoolean`/`getStringSet` |
| 复合偏好解析 | 0 | 无 split / set / map 解析，但 `getStringSet` 每次新建一个 `Set<String>` |
| 临时 List/Set/Map | 1 | `getStringSet` 的 `toSet()` 副本 |
| 临时数组 | 0 | `callMethod`/`callStaticMethod` 内部 varargs 数组 |
| 字符串拼接/格式化 | 0 | 无 |
| lambda / 匿名对象 | 0 | 同一 `MethodHook` 实例复用 |
| Intent / Bundle | 0 | 未创建新 Intent/Bundle |
| PendingIntent 发送 | 0 | 由 `AppMiniWindowManager.launchMiniWindowActivity` 后续处理 |
| 日志 | 0 | 正常路径无日志；反射失败时由 `XposedHelpers` 内部记录 |
| 系统服务 / Binder | 1 | `ProcessManager.getForegroundInfo()` |
| catch 后重复重试 | 0 | 失败即返回或跳过 |

## 5. 行为合同矩阵

| 场景 | 原方法调用 | 参数/result | 折叠面板 | 解除锁屏 | 发送 PendingIntent | 吞点击 | 异常降级 |
|---|---|---|---|---|---|---|---|
| `system_notify_openinfw` 关闭 | 不安装 Hook | - | - | - | - | - | - |
| `system_notify_openinfw` 开启 | 取决于条件 | - | 保持系统原行为 | 保持 | 保持 | 可能 | 按原方式 |
| PendingIntent 为 null | `return`（调用原方法） | - | 原方法决定 | 原方法决定 | 原方法决定 | 否 | 无 |
| mSbn 缺失 | `return`（调用原方法） | - | 同上 | 同上 | 同上 | 否 | 无 |
| 当前前台是 `pkgName` | `return`（调用原方法） | - | 同上 | 同上 | 同上 | 否 | 无 |
| 当前前台是 `com.miui.home` | `return`（调用原方法） | - | 同上 | 同上 | 同上 | 否 | 无 |
| `whitelist=true` 且 pkg 在白名单 | `return`（调用原方法） | - | 同上 | 同上 | 同上 | 否 | 无 |
| `whitelist=false` 且 pkg 在白名单 | `returnAndSkip` | 原方法不再调用 | 不折叠/不启动 | 不 | 不发送 | 是 | 已拦截到浮窗 |
| `whitelist=true` 且 pkg 不在白名单 | `returnAndSkip` | 原方法不再调用 | 同上 | 同上 | 同上 | 是 | 同上 |
| 需要认证的锁屏 | `returnAndSkip` 后系统可能按 `AppMiniWindowManager` 处理 | - | 由 `launchMiniWindowActivity` 决定 | 由 `launchMiniWindowActivity` 决定 | 由后续 Hook 决定 | 是 | 保持 |
| contentIntent 缺失 | 通常不会进入 `startNotificationIntent` | - | - | - | - | - | - |
| PendingIntent 已取消 | `launchMiniWindowActivity` 内部处理；本 Hook 不直接 send | - | 后续可能抛 `CanceledException` | - | 后续处理 | - | 后续降级 |
| Entry 缺失 | `param.getArg(2)` 可能为 null；`getObjectField` 失败并返回 | 原方法调用 | 原方法决定 | 原方法决定 | 原方法决定 | 否 | 无 |
| Row 缺失 | 同上 | 同上 | 同上 | 同上 | 同上 | 否 | 同上 |
| ROM 目标 `AppMiniWindowManager` 缺失 | `findClassIfExists` 返回 null；`callStaticMethod`/`callMethod` 失败 | 原方法调用 | 同上 | 同上 | 同上 | 否 | 反射异常被 `XposedHelpers` 捕获并记录 |

**不变更事项**：

- 原方法调用次数不变：满足条件时 `returnAndSkip`（原方法被跳过一次），否则 `return`（原方法执行一次）。
- 不修改 PendingIntent flags、Intent extras、component 或包名。
- 不自行发送 `PendingIntent`。
- 不绕过锁屏认证。

## 6. 功能门控

### `system_notify_openinfw`

- **preference_key**：`system_notify_openinfw`
- **default_value**：`false`
- **runtime_enable_semantics**：`SystemUiInstaller.install` 中读取一次；为 `true` 时安装 `OpenNotifyInFloatingWindowHook`，需重启 SystemUI 生效。
- **runtime_disable_semantics**：为 `false` 时不安装 Hook；已安装则保持到下次 SystemUI 重启。
- **requires_systemui_restart**：是
- **affects_original_method**：是（满足条件时阻止原方法）
- **affects_pending_intent**：是（改变启动方式）
- **affects_unlock_flow**：否（不修改解锁认证）
- **can_skip_when_disabled**：是（installer 层已跳过）

### `system_notify_openinfw_in_whitelist`

- **preference_key**：`system_notify_openinfw_in_whitelist`
- **default_value**：`false`
- **runtime_enable_semantics**：内存快照读取；决定“白名单模式”语义。
- **runtime_disable_semantics**：非白名单模式。
- **requires_systemui_restart**：否（随快照更新）
- **affects_original_method**：是
- **affects_pending_intent**：是（影响是否拦截）
- **affects_unlock_flow**：否
- **can_skip_when_disabled**：不能（子配置项）

### `system_notify_openinfw_apps`

- **preference_key**：`system_notify_openinfw_apps`
- **default_value**：空 `Set<String>`
- **runtime_enable_semantics**：内存快照读取；与 `in_whitelist` 做 XOR 判断是否拦截。
- **runtime_disable_semantics**：空集合。
- **requires_systemui_restart**：否
- **affects_original_method**：是
- **affects_pending_intent**：是
- **affects_unlock_flow**：否
- **can_skip_when_disabled**：不能

## 7. 反射分类

当前回调中的反射：

| 操作 | 当前分类 | 优化后分类 | 说明 |
|---|---|---|---|
| `XposedHelpers.findClass("com.android.systemui.Dependency", ...)` | `CALLBACK_TIME_REFLECTION` | `REGISTRATION_TIME_REFLECTION` | 稳定类，可在安装阶段缓存 |
| `XposedHelpers.findClassIfExists("...AppMiniWindowManager", ...)` | `CALLBACK_TIME_REFLECTION` | `REGISTRATION_TIME_REFLECTION` | 稳定类，可在安装阶段缓存；缺失时安全跳过 |
| `XposedHelpers.getObjectField(arg2, "mSbn")` | `CALLBACK_TIME_REFLECTION` | `CACHED_METADATA_USE` | `arg2` 的具体参数类型取决于 ROM 重载，可用 `Method.parameterTypes` 在安装阶段或第一次回调确定 |
| `XposedHelpers.callMethod(mSbn, "isSubstituteNotification")` | `CALLBACK_TIME_REFLECTION` | `CACHED_METADATA_USE` | `StatusBarNotification` 稳定方法 |
| `XposedHelpers.getObjectField(mSbn, "mPkgName")` | `CALLBACK_TIME_REFLECTION` | `CACHED_METADATA_USE` | `StatusBarNotification` 稳定字段 |
| `XposedHelpers.callStaticMethod(Dependency, "get", ...)` | `CALLBACK_TIME_REFLECTION` | `CACHED_METADATA_USE` | 稳定静态方法 |
| `XposedHelpers.callMethod(AppMiniWindowManager, "launchMiniWindowActivity", ...)` | `CALLBACK_TIME_REFLECTION` | `CACHED_METADATA_USE` | 稳定方法 |
| `ProcessManager.getForegroundInfo()` | 系统服务调用 | 系统服务调用 | 必须动态查询，不能缓存 |

## 8. 偏好读取分类

- `MainModule.mPrefs.getBoolean("system_notify_openinfw_in_whitelist")`：`IN_MEMORY_SNAPSHOT_READ`
- `MainModule.mPrefs.getStringSet("system_notify_openinfw_apps")`：`IN_MEMORY_SNAPSHOT_READ`，但每次新建 `Set<String>` 副本。

## 9. 优化方向

1. 安装阶段解析 `MiuiStatusBarNotificationActivityStarter` 全部 `startNotificationIntent` 重载，为每个 `arg2` 参数类型缓存 `mSbn` `Field`。
2. 缓存 `StatusBarNotification` 的 `isSubstituteNotification` `Method` 和 `mPkgName` `Field`。
3. 缓存 `Dependency` 类、`get(Class)` 静态方法、`AppMiniWindowManager` 类、`launchMiniWindowActivity(String, PendingIntent)` 方法。
4. 回调中仅使用 `Field.get()` / `Method.invoke()` / `Constructor.newInstance()`。
5. `MainModule.mPrefs.getStringSet` 返回的临时集合副本仍然存在；可考虑用局部 `val` 复用同一次点击中的结果，避免多次读取。
6. 保留 `ProcessManager.getForegroundInfo()` 动态查询，不缓存前台状态。
7. 不缓存 `PendingIntent`、`Intent`、`NotificationEntry`、`Row`、`SBN` 或 `Context`。

## 10. 保留动态反射原因

- `startNotificationIntent` 在 MIUI 上可能有不同参数重载，导致 `arg2` 类型为 `NotificationEntry` 或 `ExpandableNotificationRow`；但 `mSbn` `Field` 在发现后固定，可按参数类型缓存。
- `StatusBarNotification` 的 `isSubstituteNotification` 和 `mPkgName` 是 ROM 扩展，但属于稳定元数据。
- `AppMiniWindowManager` 类在某些 ROM 上可能不存在，安装阶段 `findClassIfExists` 即可判断；缺少时直接不安装 Hook。
- `ProcessManager.getForegroundInfo()` 必须按点击实时查询，不能缓存。

## 11. 元数据所有权

- 回调不持有 `NotificationEntry`、`ExpandableNotificationRow`、`StatusBarNotification`、`Notification`、`PendingIntent`、`Intent`、`Context`、`ActivityStarter`、`Handler`、`View` 或 `Binder` 实例。
- 安装阶段仅持有 `Class`、`Field`、`Method` 引用，以及一个由 `Method` → `Field` 组成的有界映射，大小不超过 `startNotificationIntent` 重载数量。

## 12. 实现后验证

- `python tools/verify.py full`：通过。
- `python -m unittest discover -s tools/tests -p "test_*.py"`：944 项通过，2 项跳过。
- `python -m compileall tools`：通过。
- `python tools/a13_hook_cost_scan.py --output docs/audit/A13_HOOK_COST_MAP.json`：已重新生成，回归检查全部通过。
- `.\gradlew.bat :app:minifyReleaseWithR8`：通过。
- `.\gradlew.bat :app:assembleDebug`：通过。
- 正式 Release 签名构建：本地未配置 A13 签名（`CUSTOMIUIZER_A13_KEYSTORE_PROPERTIES` 未设置），未执行；等签名配置就位后可按既有流程构建。
