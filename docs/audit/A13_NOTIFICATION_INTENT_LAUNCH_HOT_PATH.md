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

## 3. 修改后调用链

### 3.1 startNotificationIntent before 回调

```text
MiuiStatusBarNotificationActivityStarter.startNotificationIntent(PendingIntent, ...)
  └─ before(param)
       1. param.getArg(0) as? PendingIntent ?: return
       2. mSbnFieldsByMethod[member].get(param.getArg(2)) ?: return       // CACHED_METADATA_USE
       3. isSubstituteNotificationMethod.invoke(mSbn) as? Boolean                 // CACHED_METADATA_USE
       4. if (true)  mPkgNameField.get(mSbn) as? String                           // CACHED_METADATA_USE
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
| `XposedHelpers.findClass("com.android.systemui.Dependency", ...)` | `REGISTRATION_TIME_REFLECTION` | `REGISTRATION_TIME_REFLECTION` | 安装阶段缓存 |
| `XposedHelpers.findClassIfExists("...AppMiniWindowManager", ...)` | `REGISTRATION_TIME_REFLECTION` | `REGISTRATION_TIME_REFLECTION` | 安装阶段缓存；缺失时安全跳过 |
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

- `python tools/verify.py fast --tests OpenNotifyInFloatingWindowHookTest`：通过（35 个 JVM 场景，R2 新增 7 个异常边界场景）。
- `python tools/verify.py full`：通过。
- `python -m compileall tools`：通过。
- `python -m unittest discover -s tools/tests -p "test_*.py"`：通过（1043 项，2 项跳过）。
- `.\gradlew.bat :app:minifyReleaseWithR8`：通过。
- `.\gradlew.bat :app:assembleDebug`：通过。
- `OpenNotifyInFloatingWindowHookTest` 覆盖：
  - 功能开关关闭时不注册；
  - PendingIntent 为空 / arg2 为空 / mSbn 为空 的安全返回；
  - substitute 通知使用 `StatusBarNotification#mPkgName` 字段，而非 `getPackageName()`；
  - 非 substitute 通知使用 `PendingIntent.creatorPackage`；
  - 前台是 `pkgName` 或 `com.miui.home` 时返回；
  - 白名单/黑名单 XOR 语义；
  - `NotificationEntry` 与 `ExpandableNotificationRow` 两种 `startNotificationIntent` 重载映射各自的 `mSbn` 字段；
  - 无 `mSbn` 字段的未知重载 fail-open；
  - `Dependency#get` 返回 null 或普通异常时安全返回；
  - `Dependency#get` 抛出 `OutOfMemoryError`/`ThreadDeath`/`VirtualMachineError`（含包装）时继续抛出；
  - `launchMiniWindowActivity` 普通异常时不 skip；
  - `launchMiniWindowActivity` 抛出 fatal 错误时继续抛出；
  - `returnAndSkip` 仅在 `launchMiniWindowActivity` 成功后调用。

## 13. R1 QA 关键结论

- 已修复 `SUBSTITUTE_NOTIFICATION_PACKAGE_SEMANTICS`：substitute 包名从 `getPackageName()` 改为安装阶段解析的 `mPkgName` 字段，与 P1B-3 之前基线一致。
- 已引入 `rethrowNotificationFatal`：在 install 阶段与回调 `Field.get` / `Method.invoke` 中重新抛出 `OutOfMemoryError` / `ThreadDeath` / `VirtualMachineError`（含 cause 链），避免在反射失败路径上静默吞掉 fatal 错误。
- `returnAndSkip(null)` 严格位于 `launchMiniWindowActivity` 成功后，保证普通 launch 失败不会错误地跳过原方法。
- 多 overload `mSbn` 映射已支持 `NotificationEntry` 与 `ExpandableNotificationRow`，对无 `mSbn` 字段的 overload 安全 fail-open。
- 白名单 XOR 语义与 `ProcessManager.getForegroundInfo()` 实时查询保持不变。

## 14. R2 异常语义与 fail-open 边界

R2 在 R1 基础上收紧异常传播语义，建立与 `XposedHelpers` legacy 调用（`callMethod` / `callStaticMethod`）等价的异常 oracle。

### 14.1 致命错误传播

保持 R1 的 `rethrowNotificationFatal`：遇到 `OutOfMemoryError`、`ThreadDeath`、`VirtualMachineError`（含 `InvocationTargetException`/wrapper cause 链）时直接抛出，不允许吞掉。这包括 `mSbn Field.get`、`isSubstituteNotification Method.invoke`、`mPkgName Field.get`、`Dependency#get Method.invoke`、`launchMiniWindowActivity Method.invoke` 的目标异常链。

### 14.2 安装阶段 fail-closed

以下 symbol 缺失或无法解析时，`OpenNotifyInFloatingWindowHook` 直接返回，**不安装** `startNotificationIntent` Hook：

- `android.service.notification.StatusBarNotification` 类不存在；
- `StatusBarNotification#isSubstituteNotification()` 方法不存在；
- `StatusBarNotification#mPkgName` 字段不存在。

这是 DEFENSIVE_FAIL_CLOSED：缺少必要 ROM 元数据时，宁可不生效，也不让 miniwindow 以空/错误的包名启动。

### 14.3 回调阶段 pre-side-effect fail-open

`isSubstituteNotification` 或 `mPkgName` 读取发生 **ordinary** 异常（非 fatal）时，回调立即 `return`，让原 `startNotificationIntent` 继续执行。这些操作发生在实际副作用（启动 miniwindow）之前，因此安全 fail-open。

- `mSbn Field.get` 失败 → `return`；
- `isSubstituteNotification()` 返回/抛出 ordinary 异常 → `return`；
- `mPkgName Field.get` 失败 → `return`（注意：字段值为 `null` 时按 legacy 使用空字符串 `""`，不算异常）。

### 14.4 副作用后异常 legacy 传播

`Dependency#get` 返回 `null` 时 fail-open；ordinary 目标异常时 fail-open。

`AppMiniWindowManager#launchMiniWindowActivity` 属于实际副作用点。调用失败后不再 catch 并 `return`，而是使用 `invokeNotificationCompat` 将目标异常转换为与 `XposedHelpers.InvocationTargetError` 等价的 `Error` 并调用 `param.throwAndSkip(e)`：

- 普通异常：抛出 `XposedHelpers.InvocationTargetError(cause)`，`returnAndSkip` 不发生，原方法被跳过且异常保持可观测；
- 致命错误：直接抛出，不包装；
- 异常前已发生 side effect（如 `launchMiniWindowActivity` 内部部分执行）会保留，测试验证 `calls.size == 1` 且异常传播。

### 14.5 新增/更新的 JVM 测试覆盖

R2 新增/修改 `OpenNotifyInFloatingWindowHookTest` 场景：

- `isSubstitute` 普通方法失败 → fail-open（原方法继续）；
- `isSubstitute` 包装致命错误 → 继续抛出；
- `mPkgName` 字段值为 `null` → 使用空字符串启动；
- `mPkgName Field.get` 普通失败 → fail-open；
- 安装阶段 `isSubstituteNotification` 缺失 → 零 Hook；
- 安装阶段 `mPkgName` 缺失 → 零 Hook；
- `launchMiniWindowActivity` 普通异常 → 传播 `XposedHelpers.InvocationTargetError`；
- `launchMiniWindowActivity` side-effect-then-throw → 传播且 side effect 保留；
- `launchMiniWindowActivity` 包装致命错误 → 继续抛出；
- `ProcessManager.getForegroundInfo()` 普通异常 → 直接传播到 `MethodHook` 边界。
