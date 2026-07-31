# A13 运行期不变量

> 本文件记录使模块不会在 MIUI 14 / Android 13 设备上崩溃的运行期契约。
> 每条规则都对应一个在本仓库或 A14 仓库中真实出现过的缺陷，且该缺陷在编译、lint、单元测试均通过的情况下仍会导致设备不可用。

---

## 1. framework callback 必须 guarded

### 缺陷
`MethodHook.intercept` / `before` / `after` 内部有 try/catch，但模块在 hook 里注册给 Android framework 的回调没有。

### 影响进程
`system_server`、`com.android.systemui`、`com.miui.home`。

### 真实风险
`BroadcastReceiver.onReceive`、`ContentObserver.onChange`、`Handler.handleMessage`、`Runnable.run`、`setOnXxxListener {}` 等回调中一次反射打不中（ROM 改字段名、对象已 detach），异常直接冒到 system_server / SystemUI / Launcher 主线程，导致设备重启或黑屏。

### 错误示例
```kotlin
val strongAuthReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cb = XposedHelpers.getObjectField(controller, "mKeyguardSecurityCallback")
        XposedHelpers.callMethod(cb, "reportUnlockAttempt", 0, true, 0, 0) // 可能抛异常
    }
}
```

### 正确契约
```kotlin
override fun onReceive(context: Context, intent: Intent) = ModuleHelper.guarded {
    val cb = XposedHelpers.getObjectField(controller, "mKeyguardSecurityCallback")
    XposedHelpers.callMethod(cb, "reportUnlockAttempt", 0, true, 0, 0)
}
```

### 静态检查方法
- 正则匹配 `override fun (onReceive|onChange|handleMessage|run)\b` 且同作用域无 `try` / `ModuleHelper.guarded`。
- `mods/` 下 `postDelayed { }`、`setOnXxxListener { }` 等 lambda 需被 guard 包住。

### 允许的例外
- `MainApplication.kt`、`tasker/UnlockReceiver.kt` 等设置应用进程回调。
- `PreferenceObserver.onChange` 分发处已逐个隔离。

### 验证方法
`tools/check-invariants.py` 规则 `guard-framework-callbacks` + 实机 LSPosed 日志。

---

## 2. deferred callback 必须 guarded

### 缺陷
第一轮只匹配 `override fun run()`，漏掉 `mHandler.postDelayed(Runnable { ... })` 等 lambda 形状。

### 影响进程
`system_server`（`MiuiPhoneWindowManager` handler）、SystemUI。

### 真实风险
这类 Runnable 跑在 `system_server` 的 handler 上，里面有反射按键注入、`newWakeLock`、`sendBroadcast`、`getStringAsInt.toInt()` 等可能抛异常的操作。在 `system_server` 抛异常不是应用崩溃，是设备重启。

### 错误示例
```kotlin
mHandler.postDelayed(Runnable {
    sendSomeEvent(context)
    XposedHelpers.callMethod(phoneWin, "injectKeyEvent", key)
}, delay)
```

### 正确契约
```kotlin
mHandler.postDelayed(Runnable {
    ModuleHelper.guarded { sendSomeEvent(context) }
    ModuleHelper.guarded { XposedHelpers.callMethod(phoneWin, "injectKeyEvent", key) }
}, delay)
```

### 静态检查方法
- 匹配 `postDelayed`、`postAtTime`、`postOnAnimation`、`runOnUiThread`、`Thread {}`、`withEndAction`、`doOnLayout`、`addUpdateListener` 等调用中的 lambda/Runnable 是否被 `ModuleHelper.guarded` 包裹。

### 允许的例外
空 lambda 不可能抛异常，无需 guard。

### 验证方法
`tools/check-invariants.py` 规则 `guard-deferred-callbacks`。

---

## 3. Receiver 注册必须绑定所有者

### 缺陷
旧逻辑把上一个 receiver 存在被 hook 实例的 additional instance field 上，但 `hookAllConstructors` 每次 `thisObject` 都是新实例，清理从未生效。主题/密度/折叠态变化后泄漏多个 Receiver。

### 影响进程
SystemUI / system_server。

### 真实风险
监听 `TIME_TICK` 的 Receiver 每多泄漏一个，就是每分钟一次无用唤醒 + 对已 detach 对象做反射。

### 错误示例
```kotlin
val old = XposedHelpers.getAdditionalInstanceField(thisObject, "myReceiver")
if (old is BroadcastReceiver) context.unregisterReceiver(old)
context.registerReceiver(receiver, filter)
XposedHelpers.setAdditionalInstanceField(thisObject, "myReceiver", receiver)
```

### 正确契约
按目标存活语义三选一：
- 进程单例：`ModuleHelper.registerModuleReceiver(context, key, receiver, filter, flags)`
- 多实例合法共存：`ModuleHelper.registerOwnedReceiver(context, owner, key, receiver, filter, flags)`（owner 弱引用，新注册时顺带清理已 GC owner 的 receiver）
- 非 Receiver 注册：`ModuleHelper.replaceModuleRegistration(key, cleanup)`

### 静态检查方法
- `check-invariants.py` 规则 `no-raw-register-receiver`。
- 匿名 receiver 一律要走注册表；具名字段 + 同文件有 `unregisterReceiver` 才允许豁免。

### 允许的例外
`tv/withaibuild/customiuizer/mods/utils/ModuleHelper` 本身实现该机制。

### 验证方法
实机反复重建/主题切换/折叠态变化后检查 Receiver 数量不再增长。

---

## 4. 只读 Hook 参数不复制数组

### 缺陷
`XposedHelpers.getArgsArray(chain)` 每次分配数组，`chain.proceed(args)` 又让框架重新 marshal 全部参数。只读参数 hook 不需要这些。

### 影响进程
所有高频绘制/滚动 hook（状态栏背景、图标、通知着色等）。

### 真实风险
单次两次分配，乘以每天几十万次调用，造成 GC 压力。

### 错误示例
```kotlin
val args = XposedHelpers.getArgsArray(chain)
val view = args[0] as View
return chain.proceed(args)
```

### 正确契约
```kotlin
val view = chain.getArg(0) as View
return chain.proceed()
```

确实要改写参数时才用数组：
```kotlin
val args = XposedHelpers.getArgsArray(chain)
args[0] = newValue
return chain.proceed(args)
```

### 静态检查方法
- 检测 `getArgsArray` 后未修改即 `proceed(args)`。
- `Chain.getArg(int)` 在 API 101 已存在，不影响最低运行基线。

### 允许的例外
需要修改参数时。

### 验证方法
热路径代码审查 + ReflectionCacheAllocationTest（迁移后）。

---

## 5. Handler 显式指定 Looper

### 缺陷
`Handler()` 无参构造绑定当前线程 Looper。在 hook 中当前线程不可预测，没有 Looper 就抛异常。

### 影响进程
SystemUI / system_server。

### 真实风险
已在锁屏手电筒的 `ContentObserver` 上出现。

### 错误示例
```kotlin
val h = Handler()
```

### 正确契约
```kotlin
val h = Handler(context.mainLooper)
```

### 静态检查方法
匹配 `Handler()` 无参构造，排除 `Handler(Looper.myLooper())` 等显式形式。

### 允许的例外
注释中示例、测试代码。

### 验证方法
`tools/check-invariants.py` 规则 `no-looperless-handler`。

---

## 6. 禁止 Legacy Xposed

### 缺陷
模块运行在 libxposed API 101/102 上，`de.robv.android.xposed` 在运行期不存在。

### 影响进程
所有模块进程。

### 真实风险
引用 Legacy Xposed 类会导致 `ClassNotFoundError`，崩溃。

### 正确契约
仅使用 `io.github.libxposed.api` / `io.github.libxposed.service`。

### 静态检查方法
- 源码扫描 `de.robv.android.xposed` 字符串。
- R8 mapping / DEX 扫描 Legacy API。

### 允许的例外
无。

### 验证方法
Release build `apktool` / `d2j-dex2jar` 扫描。

---

## 7. 单字符分隔符禁止 Regex

### 缺陷
Java `String.split("\\|")` 走单字符快路径；机械翻译成 `split("\\|".toRegex())` 每次调用都 `Pattern.compile` + `Matcher`。

### 影响进程
设置应用列表适配器、锁屏可信网络判断（逐行调用）。

### 真实风险
高频列表滚动时重复编译正则。

### 错误示例
```kotlin
val parts = value.split("|".toRegex())
```

### 正确契约
```kotlin
val parts = value.split("|")        // Kotlin 标准库单字符快路径
// 或：
val sep = value.indexOf("|")
val first = if (sep == -1) value else value.substring(0, sep)
```

A13 已有 `PrefPair` 工具，优先使用。

### 静态检查方法
`tools/check-invariants.py` 规则 `no-regex-split-on-literal`。

### 允许的例外
真正的多字符模式如 `"\\s+".toRegex()`。

### 验证方法
热路径审查 + 单元测试。

---

## 8. 热路径禁止重复偏好读取

### 缺陷
高频 hook 中反复调用 `MainModule.mPrefs.getString(...)`，每次触发 `ConcurrentHashMap` 查找 + 类型转换。

### 影响进程
状态栏、控制中心、通知绑定等高频进程。

### 真实风险
单次不大，但乘以每天几十万次调用，积少成多。

### 错误示例
```kotlin
override fun after(param: AfterHookCallback) {
    if (MainModule.mPrefs.getBoolean("system_some_switch")) { ... }
    val value = MainModule.mPrefs.getInt("system_some_value", 0)
    // 同一 hook 中再次读取同一 key
}
```

### 正确契约
- 功能开关在 hook 注册阶段判断，不注册则关闭。
- 必须读取的值在回调入口一次性读到局部不可变变量。
- 用 `PreferenceObserver` 在值变化时更新已准备的缓存状态，而不是每次回调重读。

### 静态检查方法
- 检查高频 `before`/`after` 中 `mPrefs.get*` 调用次数。
- 对读取多于一次的 key 提取到局部 val。

### 允许的例外
Preference 值本身作为返回结果，且只有一处读取。

### 验证方法
Profile / 火焰图 + 代码审查。

---

## 9. 热路径禁止普通 HashMap primitive key 装箱

### 缺陷
`Map<Int, *>` / `Map<Long, *>` 在 `Integer` / `Long` 缓存范围外会每次分配包装对象。

### 影响进程
`ResourceHooks` 每次资源读取都会命中替换表查找。

### 真实风险
`ResourceHooks.mReplaceHook` 是进程最热代码之一，每次 `getString` / `getText` / `getDrawable` 都经过。

### 错误示例
```kotlin
val replacements = HashMap<Int, ResourceValue>()
val v = replacements[resId]
```

### 正确契约
- 资源 ID 映射优先用 `SparseArray` / `SparseIntArray`。
- 跨线程读取时用 copy-on-write 或锁内更新 + volatile 安全发布。

### 静态检查方法
- 检查热路径 `Map<Int,` / `Map<Long,`。
- 检查 `SparseArray` 是否被多线程读且无双锁/安全发布。

### 允许的例外
非热路径、读写均在初始化阶段且线程安全。

### 验证方法
构建通过 + 实机资源替换功能正常。

---

## 10. 息屏停止周期任务

### 缺陷
息屏后 2s 周期 ticker 仍然重投消息，不做任何工作但持续唤醒 CPU。

### 影响进程
SystemUI。

### 真实风险
待机耗电。

### 错误示例
```kotlin
handler.postDelayed(tickRunnable, 2000)
```

### 正确契约
- 监听 `ACTION_SCREEN_ON` / `ACTION_SCREEN_OFF`。
- 息屏时停止 post，亮屏后立即恢复。
- 首次亮屏后刷新一次当前状态，避免用户看到旧值。

### 静态检查方法
- 查找 `postDelayed` 固定间隔任务。
- 检查是否有 `SCREEN_ON` / `SCREEN_OFF` 反注册/重注册。

### 允许的例外
需要持续后台监控且耗电极低的功能（需记录）。

### 验证方法
实机电量日志 / `dumpsys alarm`。

---

## 11. ROM 目标缺失时只失败一次

### 缺陷
某个类/方法在目标 ROM 不存在时，模块可能高频重试注册或反射，导致日志刷屏、持续分配。

### 影响进程
所有模块进程。

### 真实风险
无关进程也初始化对应功能，拖慢 SystemUI / system_server。

### 正确契约
- 入口层尽早按包名、进程、Android 版本退出无关路径。
- 注册失败只记录一次日志，安全禁用当前单项功能。
- 使用 `findClassIfExists` / `findMethodExactIfExists` 后判空，不要反复调用。

### 静态检查方法
- 查找循环内注册 hook 或反复 `Class.forName`。

### 允许的例外
需要等待服务就绪的延迟注册必须有一次性成功标志。

### 验证方法
在不支持该类/方法的 ROM 上安装运行，确认只出现一次日志。

---

## 12. `Chain.proceed()` 只能按原语义执行

### 缺陷
迁移后容易在 `before` 中重复调用 `chain.proceed()`，或在 `after` 中错误 `setResult`。

### 影响进程
所有被 hook 的进程。

### 真实风险
原方法被调用 0 次或 2 次，导致功能异常或崩溃。

### 正确契约
- `before`：需要修改参数或阻止时才 `returnAndSkip` / `chain.proceed(args)`。
- `after`：只读取或 `setResult`（一次）。
- `intercept`：只读参数时 `chain.proceed()`；改写参数时 `chain.proceed(args)`。

### 静态检查方法
- 人工审计 `before` / `intercept` / `after` 中 `proceed` / `setResult` / `returnAndSkip` 次数。

### 允许的例外
无。

### 验证方法
Java→Kotlin 迁移时与原 Java 逐行比对。

---

## 13. additional instance field 使用身份语义

### 缺陷
按 `equals` / `hashCode` 存字段时，两个不同但 `equals` 的对象共用字段表；修改参与 `hashCode` 的字段后条目丢失。

### 影响进程
SystemUI / Launcher。

### 真实风险
`ShortcutInfo` 上存 `mLabelOrig`、改 `mLabel` 后再读可能读到 `null` 或错值。

### 正确契约
- `XposedHelpers` 实现按身份比较。
- 模块代码不把 `WeakHashMap` / `HashMap` / `HashSet` 用于“每个 ROM 实例一份”的状态。

### 静态检查方法
- 检查对 ROM 可变对象使用 `HashMap` / `HashSet` 作为实例级缓存的用法。

### 允许的例外
不可变或比较后不会修改的对象。

### 验证方法
构建/测试 + 实机。

---

## 14. Java → Kotlin 循环控制流必须核对

### 缺陷
Java `break` / `continue` 在 Kotlin `when` 中消失正常，但在循环体中消失属于回归；`use {}`、`forEach {}` 中不能直接用 Java 非局部跳出。

### 影响进程
所有迁移后的 mod。

### 真实风险
Thermal zone 扫描丢弃 `break` 后，每次调用打开 19 个 sysfs 文件且 `thermalId` 不重置。

### 正确契约
- 迁移后必须核对 `break` / `continue` 数量与位置。
- 不能机械把 Java 循环翻译成 `forEach` 后丢失非局部跳出。

### 静态检查方法
```powershell
git show "<迁移commit>^:app/src/main/java/<path>.java" |
    Select-String -Pattern '\bbreak\b|\bcontinue\b' -Context 6,1
```

### 允许的例外
Java `switch` 中的 `break` 在 Kotlin `when` 中消失正常。

### 验证方法
每迁移一个文件都执行上述命令并人工判定。

---

## 15. Bitmap 和 View 静态缓存必须有释放边界

### 缺陷
`Bitmap` 长期缓存或静态持有 `View` / `Context` 无法释放。

### 影响进程
SystemUI / Launcher。

### 真实风险
主题/密度/折叠态变化后创建大量新 View 和 Bitmap，旧资源永久占用。

### 正确契约
- 缓存必须有容量、范围、生命周期或失效规则。
- `View`、`Context`、`ClassLoader` 不静态持有。
- 使用弱引用时必须在注册和遍历阶段清理失效引用。

### 静态检查方法
- 扫描 `static` 字段中的 `View`、`Context`、`Bitmap`、`ArrayList<View>`。
- 检查 `WeakReference` 是否只替换强引用但不清理空壳条目。

### 允许的例外
进程内全局单例且容量受限的缓存（需记录）。

### 验证方法
内存基线脚本 + 实机长时间运行。
